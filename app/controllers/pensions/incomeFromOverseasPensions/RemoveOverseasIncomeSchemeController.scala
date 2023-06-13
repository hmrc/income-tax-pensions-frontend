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

package controllers.pensions.incomeFromOverseasPensions

import config.{AppConfig, ErrorHandler}
import controllers.pensions.incomeFromOverseasPensions.routes._
import controllers.predicates.ActionsProvider
import controllers.validatedIndex
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.pension.charges.PensionScheme
import models.requests.UserSessionDataRequest
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.pensions.incomeFromOverseasPensions.RemoveOverseasIncomeSchemeView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class RemoveOverseasIncomeSchemeController @Inject()(actionsProvider: ActionsProvider,
                                                     pensionSessionService: PensionSessionService,
                                                     view: RemoveOverseasIncomeSchemeView,
                                                     errorHandler: ErrorHandler)
                                                    (implicit val mcc: MessagesControllerComponents,
                                                     appConfig: AppConfig, clock: Clock)
  extends FrontendController(mcc) with I18nSupport with SessionHelper {

  def show(taxYear: Int, index: Option[Int]): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async { implicit sessionUserData =>
    val overseasIncomeSchemes = sessionUserData.pensionsUserData.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes
    validatedIndex(index, overseasIncomeSchemes.size).fold(Future.successful(Redirect(CountrySummaryListController.show(taxYear)))) {
      i =>
        overseasIncomeSchemes(i).alphaTwoCode.fold(Future.successful(Redirect(CountrySummaryListController.show(taxYear)))) {
          country => Future.successful(Ok(view(taxYear, country, index)))
        }
    }
  }

  def submit(taxYear: Int, index: Option[Int]): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async { implicit sessionUserData =>
    val overseasIncomeSchemes = sessionUserData.pensionsUserData.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes
    validatedIndex(index, overseasIncomeSchemes.size)
      .fold(Future.successful(Redirect(CountrySummaryListController.show(taxYear)))) {
        i =>
          val updatedRefundScheme = overseasIncomeSchemes.patch(i, Nil, 1)
          updateSessionData(sessionUserData.pensionsUserData, updatedRefundScheme, taxYear)
      }
  }

  private def updateSessionData[T](pensionUserData: PensionsUserData,
                                   overseasIncomeSchemes: Seq[PensionScheme],
                                   taxYear: Int)(implicit request: UserSessionDataRequest[T]): Future[Result] = {
    val updatedCyaModel: PensionsCYAModel = pensionUserData.pensions.copy(
      incomeFromOverseasPensions = pensionUserData.pensions.incomeFromOverseasPensions.copy(
        overseasIncomePensionSchemes = overseasIncomeSchemes))

    pensionSessionService.createOrUpdateSessionData(request.user,
      updatedCyaModel, taxYear, pensionUserData.isPriorSubmission)(errorHandler.internalServerError()) {
      Redirect(CountrySummaryListController.show(taxYear))
    }
  }


}
