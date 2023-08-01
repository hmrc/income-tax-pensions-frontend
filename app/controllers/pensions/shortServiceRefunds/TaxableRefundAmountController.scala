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
import controllers.pensions.shortServiceRefunds.routes.{NonUkTaxRefundsController, ShortServiceRefundsCYAController}
import controllers.predicates.actions.ActionsProvider
import forms.FormsProvider
import models.mongo.{DatabaseError, PensionsCYAModel, PensionsUserData}
import models.pension.charges.OverseasRefundPensionScheme
import models.requests.UserSessionDataRequest
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{PensionSessionService, ShortServiceRefundsService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.pensions.shortServiceRefunds.TaxableRefundAmountView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TaxableRefundAmountController @Inject()(actionsProvider: ActionsProvider,
                                              pensionSessionService: PensionSessionService,
                                              shortServiceRefundsService: ShortServiceRefundsService,
                                              view: TaxableRefundAmountView,
                                              formsProvider: FormsProvider,
                                              errorHandler: ErrorHandler)
                                             (implicit val mcc: MessagesControllerComponents, appConfig: AppConfig,
                                              clock: Clock, ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport with SessionHelper {

  def show(taxYear: Int): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async {
    implicit sessionData =>
      cleanUpSchemes(sessionData.pensionsUserData).map({
        case Right(_) =>
          val shortServiceRefundCharge: Option[BigDecimal] = sessionData.pensionsUserData.pensions.shortServiceRefunds.shortServiceRefundCharge
          val refundOpt: Option[Boolean] = sessionData.pensionsUserData.pensions.shortServiceRefunds.shortServiceRefund
          (refundOpt, shortServiceRefundCharge) match {
            case (Some(a), amount) => Ok(view(formsProvider.shortServiceTaxableRefundForm(sessionData.user).fill((a, amount)), taxYear))
            case _ => Ok(view(formsProvider.shortServiceTaxableRefundForm(sessionData.user), taxYear))
          }
      })
  }

  def submit(taxYear: Int): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async {
    implicit sessionUserData =>
      formsProvider.shortServiceTaxableRefundForm(sessionUserData.user).bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear))),
        yesNoAmount => {
          (yesNoAmount._1, yesNoAmount._2) match {
            case (true, amount) => updateSessionData(sessionUserData.pensionsUserData, yesNo = true, amount, taxYear)
            case (false, _) => updateSessionData(sessionUserData.pensionsUserData, yesNo = false, None, taxYear)
          }
        }
      )
  }

  private def updateSessionData[T](pensionUserData: PensionsUserData,
                                   yesNo: Boolean,
                                   amount: Option[BigDecimal],
                                   taxYear: Int)(implicit request: UserSessionDataRequest[T]): Future[Result] = {

    val updatedCyaModel: PensionsCYAModel = shortServiceRefundsService.updateCyaWithShortServiceRefundGatewayQuestion(
      pensionUserData, yesNo, amount)

    pensionSessionService.createOrUpdateSessionData(request.user,
      updatedCyaModel, taxYear, pensionUserData.isPriorSubmission)(errorHandler.internalServerError()) {
      if (yesNo) {
        Redirect(NonUkTaxRefundsController.show(taxYear))
      } else {
        Redirect(ShortServiceRefundsCYAController.show(taxYear))
      }
    }
  }

  private def cleanUpSchemes(pensionsUserData: PensionsUserData)
                            (implicit ec: ExecutionContext): Future[Either[DatabaseError, Seq[OverseasRefundPensionScheme]]] = {
    val schemes = pensionsUserData.pensions.shortServiceRefunds.refundPensionScheme
    val filteredSchemes = if (schemes.nonEmpty) schemes.filter(scheme => scheme.isFinished) else schemes
    val updatedViewModel = pensionsUserData.pensions.shortServiceRefunds.copy(refundPensionScheme = filteredSchemes)
    val updatedPensionData = pensionsUserData.pensions.copy(shortServiceRefunds = updatedViewModel)
    val updatedUserData = pensionsUserData.copy(pensions = updatedPensionData)
    pensionSessionService.createOrUpdateSessionData(updatedUserData).map(_.map(_ => filteredSchemes))
  }
}
