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
import controllers.predicates.{AuthorisedAction, InYearAction}
import forms.YesNoForm
import models.User
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.pension.reliefs.PaymentsIntoPensionViewModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.pensions.PayIntoRetirementAnnuityContract

import javax.inject.Inject
import scala.concurrent.Future


class RetirementAnnuityController @Inject()(implicit val cc: MessagesControllerComponents,
                                            authAction: AuthorisedAction,
                                            payIntoRetirementAnnuityContract: PayIntoRetirementAnnuityContract,
                                            appConfig: AppConfig,
                                            pensionSessionService: PensionSessionService,
                                            errorHandler: ErrorHandler,
                                            clock: Clock) extends FrontendController(cc) with I18nSupport with SessionHelper {


  def yesNoForm(implicit user: User[_]): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"pensions.retirementAnnuityContract.error.noEntry.${if (user.isAgent) "agent" else "individual"}"
  )

  def show(taxYear: Int): Action[AnyContent] = authAction.async { implicit user =>
    pensionSessionService.getPensionsSessionDataResult(taxYear) {
      case Some(data) =>
        data.pensions.paymentsIntoPension.retirementAnnuityContractPaymentsQuestion match {
          case Some(value) => Future.successful(Ok(payIntoRetirementAnnuityContract(
            yesNoForm.fill(value), taxYear)))
          case None => Future.successful(Ok(payIntoRetirementAnnuityContract(yesNoForm, taxYear)))
        }
      case None =>
        Future.successful(Ok(payIntoRetirementAnnuityContract(yesNoForm, taxYear)))
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit user =>
    yesNoForm.bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(payIntoRetirementAnnuityContract(formWithErrors, taxYear))),
      yesNo => {
        pensionSessionService.getPensionsSessionDataResult(taxYear) {
          data =>
            val pensionsCYAModel: PensionsCYAModel = data.map(_.pensions).getOrElse(PensionsCYAModel(PaymentsIntoPensionViewModel()))
            val viewModel: PaymentsIntoPensionViewModel = pensionsCYAModel.paymentsIntoPension
            val updatedCyaModel: PensionsCYAModel = {
              pensionsCYAModel.copy(paymentsIntoPension = viewModel.copy(retirementAnnuityContractPaymentsQuestion = Some(yesNo)))
            }
            pensionSessionService.createOrUpdateSessionData(
              updatedCyaModel, taxYear, data.exists(_.isPriorSubmission))(errorHandler.internalServerError()) {
              if (yesNo) {
                //TODO redirect to retirement annuity amount page
                Redirect(controllers.pensions.routes.PensionsSummaryController.show(taxYear))
              } else {
                //TODO redirect to workplace pensions page
                Redirect(controllers.pensions.routes.PensionsSummaryController.show(taxYear))
              }
            }
        }
      }
    )
  }
}
