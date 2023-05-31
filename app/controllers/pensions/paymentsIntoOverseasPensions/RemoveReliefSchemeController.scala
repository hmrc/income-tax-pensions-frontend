/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.pensions.paymentsIntoOverseasPensions

import config.{AppConfig, ErrorHandler}
import controllers.predicates.ActionsProvider
import controllers.validatedIndex
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.pension.charges.Relief
import models.requests.UserSessionDataRequest
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import routes._
import views.html.pensions.paymentsIntoOverseasPensions.RemoveReliefSchemeView
import utils.{Clock, SessionHelper}


import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class RemoveReliefSchemeController @Inject()(actionsProvider: ActionsProvider,
                                             pensionSessionService: PensionSessionService,
                                             view: RemoveReliefSchemeView,
                                             errorHandler: ErrorHandler)
                                            (implicit val mcc: MessagesControllerComponents,
                                             appConfig: AppConfig, clock: Clock)
  extends FrontendController(mcc) with I18nSupport with SessionHelper {

  def show(taxYear: Int, index: Option[Int]): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async { implicit sessionUserData =>
    val pensionReliefSchemes: Seq[Relief] = sessionUserData.pensionsUserData.pensions.paymentsIntoOverseasPensions.reliefs
    getElementIndex(index, pensionReliefSchemes) match {
      case Some(relief) => Future.successful(Ok(view(taxYear = taxYear, reliefSchemeList = List(relief), index = index)))
      case None => Future.successful(Redirect(ReliefsSchemeDetailsController.show(taxYear, Some(1)))): Future[Result]
    }
  }

  private def getElementIndex[A](maybeIndex: Option[Int], as: Seq[A]): Option[A] = {
    maybeIndex.flatMap(index => as.lift(index))
  }

  def submit(taxYear: Int, index: Option[Int]): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async { implicit sessionUserData =>
    val pensionReliefScheme = sessionUserData.pensionsUserData.pensions.paymentsIntoOverseasPensions.reliefs
    validatedIndex(index, pensionReliefScheme.size)
      .fold(Future.successful(Redirect(ReliefsSchemeSummaryController.show(taxYear)))) {
        i =>
          val updatedPensionReliefScheme = pensionReliefScheme.patch(i, Nil, 1)
          updateSessionData(sessionUserData.pensionsUserData, updatedPensionReliefScheme, taxYear)
      }
  }

  private def updateSessionData[T](pensionUserData: PensionsUserData,
                                   reliefScheme: Seq[Relief],
                                   taxYear: Int)(implicit request: UserSessionDataRequest[T], r: Request[_]) = {
    val updatedCyaModel: PensionsCYAModel = pensionUserData.pensions.copy(
      paymentsIntoOverseasPensions = pensionUserData.pensions.paymentsIntoOverseasPensions.copy(
        reliefs = reliefScheme))

    pensionSessionService.createOrUpdateSessionData[Result](request.user,
      updatedCyaModel, taxYear, pensionUserData.isPriorSubmission)(errorHandler.internalServerError()) {
      Redirect(ReliefsSchemeSummaryController.show(taxYear))
    }
  }}
