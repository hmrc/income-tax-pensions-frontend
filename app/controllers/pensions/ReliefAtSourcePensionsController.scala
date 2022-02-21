/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers.pensions

import config.{AppConfig, ErrorHandler}
import controllers.pensions.routes.{ReliefAtSourcePaymentsAndTaxReliefAmountController, PensionsTaxReliefNotClaimedController}
import controllers.predicates.AuthorisedAction
import forms.YesNoForm
import models.User
import models.mongo.PensionsCYAModel
import models.pension.reliefs.PaymentsIntoPensionViewModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import views.html.pensions.ReliefAtSourcePensionsView
import javax.inject.Inject
import scala.concurrent.Future

class ReliefAtSourcePensionsController @Inject()(implicit val cc: MessagesControllerComponents,
                                                 auth: AuthorisedAction,
                                                 rasPensionView: ReliefAtSourcePensionsView,
                                                 appConfig: AppConfig,
                                                 pensionSessionService: PensionSessionService,
                                                 errorHandler: ErrorHandler,
                                                 clock: Clock) extends FrontendController(cc) with I18nSupport {

  def show(taxYear: Int): Action[AnyContent] = auth.async { implicit user =>
    pensionSessionService.getPensionsSessionDataResult(taxYear) {
      case Some(data) =>
        data.pensions.paymentsIntoPension.rasPensionPaymentQuestion match {
          case Some(value) => Future.successful(Ok(rasPensionView(yesNoForm.fill(value), taxYear)))
          case None => Future.successful(Ok(rasPensionView(yesNoForm, taxYear)))
        }
      case _ => Future.successful(Ok(rasPensionView(yesNoForm, taxYear)))

    }
  }

  def submit(taxYear: Int): Action[AnyContent] = auth.async { implicit user =>
    yesNoForm.bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(rasPensionView(formWithErrors, taxYear))),
      yesNo => {
        pensionSessionService.getPensionsSessionDataResult(taxYear) { optData =>

          val pensionsCya = optData.map(_.pensions).getOrElse(PensionsCYAModel(PaymentsIntoPensionViewModel()))
          val viewModel = pensionsCya.paymentsIntoPension

          val updatedCyaModel = {
            pensionsCya.copy(
              paymentsIntoPension = viewModel.copy(
                rasPensionPaymentQuestion = Some(yesNo),
                totalRASPaymentsAndTaxRelief = if(yesNo) viewModel.totalRASPaymentsAndTaxRelief else None
              )
            )
          }

          pensionSessionService.createOrUpdateSessionData(
            updatedCyaModel, taxYear, optData.exists(_.isPriorSubmission))(errorHandler.internalServerError()) {
            if (yesNo) {
              Redirect(ReliefAtSourcePaymentsAndTaxReliefAmountController.show(taxYear))
            } else {
              Redirect(PensionsTaxReliefNotClaimedController.show(taxYear))
            }
          }
        }
      }
    )
  }

  private def yesNoForm(implicit user: User[_]): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"pensions.reliefAtSource.error.noEntry.${if (user.isAgent) "agent" else "individual"}"
  )

}
