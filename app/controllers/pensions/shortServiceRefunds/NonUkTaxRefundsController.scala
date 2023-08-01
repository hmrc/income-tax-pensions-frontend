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
import controllers.predicates.actions.ActionsProvider
import forms.FormsProvider
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.requests.UserSessionDataRequest
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.PensionSessionService
import services.redirects.ShortServiceRefundsPages.NonUkTaxRefundsAmountPage
import services.redirects.ShortServiceRefundsRedirects.{cyaPageCall, journeyCheck, redirectForSchemeLoop}
import services.redirects.SimpleRedirectService.redirectBasedOnCurrentAnswers
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.pensions.shortServiceRefunds.NonUkTaxRefundsView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class NonUkTaxRefundsController @Inject()(
                                           actionsProvider: ActionsProvider,
                                           pensionSessionService: PensionSessionService,
                                           view: NonUkTaxRefundsView,
                                           formsProvider: FormsProvider,
                                           errorHandler: ErrorHandler
                                         )(implicit val mcc: MessagesControllerComponents, appConfig: AppConfig, clock: Clock)
  extends FrontendController(mcc) with I18nSupport with SessionHelper {

  def show(taxYear: Int): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async {
    implicit sessionData =>

      val checkRedirect = journeyCheck(NonUkTaxRefundsAmountPage, _: PensionsCYAModel, taxYear)
      redirectBasedOnCurrentAnswers(taxYear, Some(sessionData.pensionsUserData), cyaPageCall(taxYear))(checkRedirect) { data =>

        val refundTaxPaid: Option[Boolean] = data.pensions.shortServiceRefunds.shortServiceRefundTaxPaid
        val refundTaxPaidCharge: Option[BigDecimal] = data.pensions.shortServiceRefunds.shortServiceRefundTaxPaidCharge

        (refundTaxPaid, refundTaxPaidCharge) match {
          case (Some(a), refundTaxPaidCharge) => Future.successful(
            Ok(view(formsProvider.nonUkTaxRefundsForm(sessionData.user).fill((a, refundTaxPaidCharge)), taxYear)))
          case _ => Future.successful(Ok(view(formsProvider.nonUkTaxRefundsForm(sessionData.user), taxYear)))
        }
      }
  }

  def submit(taxYear: Int): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async {
    implicit sessionUserData =>

      formsProvider.nonUkTaxRefundsForm(sessionUserData.user).bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear))),
        yesNoAmount => {

          val checkRedirect = journeyCheck(NonUkTaxRefundsAmountPage, _: PensionsCYAModel, taxYear)
          redirectBasedOnCurrentAnswers(taxYear, Some(sessionUserData.pensionsUserData), cyaPageCall(taxYear))(checkRedirect) { data =>

            (yesNoAmount._1, yesNoAmount._2) match {
              case (true, amount) => updateSessionData(data, yesNo = true, amount, taxYear)
              case (false, _) => updateSessionData(data, yesNo = false, None, taxYear)
            }
          }
        }
      )
  }

  private def updateSessionData[T](pensionUserData: PensionsUserData,
                                   yesNo: Boolean,
                                   amount: Option[BigDecimal],
                                   taxYear: Int)(implicit request: UserSessionDataRequest[T]): Future[Result] = {
    val updatedCyaModel: PensionsCYAModel = pensionUserData.pensions.copy(
      shortServiceRefunds = pensionUserData.pensions.shortServiceRefunds.copy(
        shortServiceRefundTaxPaid = Some(yesNo),
        shortServiceRefundTaxPaidCharge = amount))

    pensionSessionService.createOrUpdateSessionData(request.user,
      updatedCyaModel, taxYear, pensionUserData.isPriorSubmission)(errorHandler.internalServerError()) {
      Redirect(redirectForSchemeLoop(updatedCyaModel.shortServiceRefunds.refundPensionScheme, taxYear))
    }
  }
}
