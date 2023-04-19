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

package controllers.pensions.shortServiceRefunds

import config.{AppConfig, ErrorHandler}
import controllers.predicates.ActionsProvider
import controllers.validatedIndex
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.pension.charges.OverseasRefundPensionScheme
import models.requests.UserSessionDataRequest
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import routes._
import views.html.pensions.shortServiceRefunds.RemoveRefundSchemeView
import utils.{Clock, SessionHelper}

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class RemoveRefundSchemeController @Inject()(actionsProvider: ActionsProvider,
                                             pensionSessionService: PensionSessionService,
                                             view: RemoveRefundSchemeView,
                                             errorHandler: ErrorHandler)
                                            (implicit val mcc: MessagesControllerComponents,
                                                     appConfig: AppConfig, clock: Clock)
  extends FrontendController(mcc) with I18nSupport with SessionHelper {

  def show(taxYear: Int, index: Option[Int]): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async { implicit sessionUserData =>
    val refundChargeScheme = sessionUserData.pensionsUserData.pensions.shortServiceRefunds.refundPensionScheme
    validatedIndex(index, refundChargeScheme.size).fold(Future.successful(Redirect(RefundSummaryController.show(taxYear)))) {
      i =>
        refundChargeScheme(i).name.fold(Future.successful(Redirect(RefundSummaryController.show(taxYear)))){
          name => Future.successful(Ok(view(taxYear, name, index)))
        }
    }
  }

  def submit(taxYear: Int, index: Option[Int]): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async { implicit sessionUserData =>
    val refundChargeScheme = sessionUserData.pensionsUserData.pensions.shortServiceRefunds.refundPensionScheme
    validatedIndex(index, refundChargeScheme.size)
      .fold(Future.successful(Redirect(RefundSummaryController.show(taxYear)))) {
        i =>
          val updatedRefundScheme = refundChargeScheme.patch(i, Nil, 1)
          updateSessionData(sessionUserData.pensionsUserData, updatedRefundScheme, taxYear)
      }
  }

  private def updateSessionData[T](pensionUserData: PensionsUserData,
                                   refundChargeScheme: Seq[OverseasRefundPensionScheme],
                                   taxYear: Int)(implicit request: UserSessionDataRequest[T]) = {
    val updatedCyaModel: PensionsCYAModel = pensionUserData.pensions.copy(
      shortServiceRefunds = pensionUserData.pensions.shortServiceRefunds.copy(
        refundPensionScheme = refundChargeScheme))

    pensionSessionService.createOrUpdateSessionData(request.user,
      updatedCyaModel, taxYear, pensionUserData.isPriorSubmission)(errorHandler.internalServerError()) {
      Redirect(RefundSummaryController.show(taxYear))
    }
  }


}
