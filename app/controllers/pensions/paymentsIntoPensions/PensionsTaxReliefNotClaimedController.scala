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

package controllers.pensions.paymentsIntoPensions

import config.{AppConfig, ErrorHandler}
import controllers.predicates.AuthorisedAction
import controllers.predicates.TaxYearAction.taxYearAction
import models.mongo.PensionsCYAModel
import models.pension.reliefs.PaymentsIntoPensionViewModel
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.PensionSessionService
import services.RedirectService.{PaymentsIntoPensionsRedirects, isFinishedCheck, redirectBasedOnCurrentAnswers}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import utils.PaymentsIntoPensionPages.{RasAmountPage, TaxReliefNotClaimedPage}
import views.html.pensions.paymentsIntoPensions.PensionsTaxReliefNotClaimedView

import javax.inject.{Inject, Singleton}
import models.redirects.ConditionalRedirect

import scala.concurrent.Future


@Singleton
class PensionsTaxReliefNotClaimedController @Inject()(authAction: AuthorisedAction,
                                                      pensionSessionService: PensionSessionService,
                                                      errorHandler: ErrorHandler,
                                                      pensionsTaxReliefNotClaimedView: PensionsTaxReliefNotClaimedView,
                                                      formProvider: PaymentsIntoPensionFormProvider)
                                                     (implicit val mcc: MessagesControllerComponents, appConfig: AppConfig, clock: Clock)
  extends FrontendController(mcc) with I18nSupport {

  def show(taxYear: Int): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async {
    implicit request =>
      pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
        optData =>
          redirectBasedOnCurrentAnswers(taxYear, optData)(redirects(_, taxYear)) {
            data =>

              val form = formProvider.pensionsTaxReliefNotClaimedForm(request.user.isAgent)
              data.pensions.paymentsIntoPension.pensionTaxReliefNotClaimedQuestion match {
                case Some(question) => Future.successful(Ok(pensionsTaxReliefNotClaimedView(form.fill(question), taxYear)))
                case None => Future.successful(Ok(pensionsTaxReliefNotClaimedView(form, taxYear)))
              }
          }
      }
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async {
    implicit request =>
      formProvider.pensionsTaxReliefNotClaimedForm(request.user.isAgent).bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(pensionsTaxReliefNotClaimedView(formWithErrors, taxYear))),
        yesNo => {
          pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
            optData =>
              redirectBasedOnCurrentAnswers(taxYear, optData)(redirects(_, taxYear)) {
                data =>

                  val pensionsCYAModel: PensionsCYAModel = data.pensions
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
                    controllers.pensions.paymentsIntoPensions.routes.RetirementAnnuityController.show(taxYear)
                  } else {
                    controllers.pensions.paymentsIntoPensions.routes.PaymentsIntoPensionsCYAController.show(taxYear)
                  }
                  pensionSessionService.createOrUpdateSessionData(request.user,
                    updatedCyaModel, taxYear, data.isPriorSubmission)(errorHandler.internalServerError()) {
                      isFinishedCheck(updatedCyaModel, taxYear, redirectLocation)
                  }
              }
          }
        }
      )
  }

  private def redirects(cya: PensionsCYAModel, taxYear: Int): Either[Result, Unit] = {
    PaymentsIntoPensionsRedirects.journeyCheck(RasAmountPage, cya, taxYear)
  }

}
