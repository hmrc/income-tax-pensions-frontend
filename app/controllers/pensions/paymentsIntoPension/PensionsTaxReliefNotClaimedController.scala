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

package controllers.pensions.paymentsIntoPension

import config.{AppConfig, ErrorHandler}
import controllers.pensions.paymentsIntoPension.routes._
import controllers.predicates.AuthorisedAction
import forms.YesNoForm
import models.User
import models.mongo.PensionsCYAModel
import models.pension.charges.PensionAnnualAllowancesViewModel
import models.pension.reliefs.PaymentsIntoPensionViewModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import views.html.pensions.PensionsTaxReliefNotClaimedView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future


@Singleton
class PensionsTaxReliefNotClaimedController @Inject()(implicit val mcc: MessagesControllerComponents,
                                                      appConfig: AppConfig,
                                                      authAction: AuthorisedAction,
                                                      pensionSessionService: PensionSessionService,
                                                      errorHandler: ErrorHandler,
                                                      pensionsTaxReliefNotClaimedView: PensionsTaxReliefNotClaimedView,
                                                      clock: Clock) extends FrontendController(mcc) with I18nSupport {

  def show(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
      case Some(data) =>
        data.pensions.paymentsIntoPension.pensionTaxReliefNotClaimedQuestion match {
          case Some(question) => Future.successful(Ok(pensionsTaxReliefNotClaimedView(yesNoForm(request.user).fill(question), taxYear)))
          case None => Future.successful(Ok(pensionsTaxReliefNotClaimedView(yesNoForm(request.user), taxYear)))
        }
      case _ =>
        Future.successful(Redirect(PaymentsIntoPensionsCYAController.show(taxYear)))
    }

  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    yesNoForm(request.user).bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(pensionsTaxReliefNotClaimedView(formWithErrors, taxYear))),
      yesNo => {
        pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
          data =>
            val pensionsCYAModel: PensionsCYAModel = data.map(_.pensions).getOrElse(PensionsCYAModel.emptyModels)
            val viewModel: PaymentsIntoPensionViewModel = pensionsCYAModel.paymentsIntoPension
            val updatedCyaModel: PensionsCYAModel = {
              pensionsCYAModel.copy(
                paymentsIntoPension = viewModel.copy(
                  pensionTaxReliefNotClaimedQuestion = Some(yesNo),
                  retirementAnnuityContractPaymentsQuestion = if (yesNo) viewModel.retirementAnnuityContractPaymentsQuestion else None,
                  totalRetirementAnnuityContractPayments = if (yesNo) viewModel.totalRetirementAnnuityContractPayments else None,
                  workplacePensionPaymentsQuestion = if (yesNo) viewModel.workplacePensionPaymentsQuestion else None,
                  totalWorkplacePensionPayments = if (yesNo) viewModel.totalWorkplacePensionPayments else None
                )
              )
            }
            val redirectLocation = if (yesNo) {
              controllers.pensions.paymentsIntoPension.routes.RetirementAnnuityController.show(taxYear)
            } else {
              controllers.pensions.paymentsIntoPension.routes.PaymentsIntoPensionsCYAController.show(taxYear)
            }
            pensionSessionService.createOrUpdateSessionData(request.user,
              updatedCyaModel, taxYear, data.exists(_.isPriorSubmission))(errorHandler.internalServerError()) {
              Redirect(redirectLocation)
            }
        }

      }
    )
  }

  private def yesNoForm(user: User): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"pensions.pensionsTaxReliefNotClaimed.error.noEntry.${if (user.isAgent) "agent" else "individual"}"
  )

}
