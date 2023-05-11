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
import controllers.pensions.paymentsIntoPensions.routes.{PensionsTaxReliefNotClaimedController, ReliefAtSourcePaymentsAndTaxReliefAmountController}
import controllers.predicates.AuthorisedAction
import controllers.predicates.TaxYearAction.taxYearAction
import models.mongo.PensionsCYAModel
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import services.SimpleRedirectService.isFinishedCheck
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import views.html.pensions.paymentsIntoPensions.ReliefAtSourcePensionsView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ReliefAtSourcePensionsController @Inject()(authAction: AuthorisedAction,
                                                 pageView: ReliefAtSourcePensionsView,
                                                 pensionSessionService: PensionSessionService,
                                                 errorHandler: ErrorHandler,
                                                 formsProvider: PaymentsIntoPensionFormProvider)
                                                (implicit val cc: MessagesControllerComponents,
                                                 appConfig: AppConfig, clock: Clock, ec: ExecutionContext)
  extends FrontendController(cc) with I18nSupport {

  def show(taxYear: Int): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async { implicit request =>
    val yesNoForm = formsProvider.reliefAtSourcePensionsForm(request.user.isAgent)

    pensionSessionService.getPensionSessionData(taxYear, request.user).flatMap {
      case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
      case Right(optPensionUserDate) => optPensionUserDate match {
        case Some(data) =>
          data.pensions.paymentsIntoPension.rasPensionPaymentQuestion match {
            case Some(value) => Future.successful(Ok(pageView(yesNoForm.fill(value), taxYear)))
            case None => Future.successful(Ok(pageView(yesNoForm, taxYear)))
          }
        case None => Future.successful(Ok(pageView(yesNoForm, taxYear)))
      }
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    formsProvider.reliefAtSourcePensionsForm(request.user.isAgent).bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(pageView(formWithErrors, taxYear))),
      yesNo => {
        pensionSessionService.getPensionSessionData(taxYear, request.user).flatMap {
          case Right(optData) =>

            val pensionsCya = optData.map(_.pensions).getOrElse(PensionsCYAModel.emptyModels)
            val viewModel = pensionsCya.paymentsIntoPension

            val updatedCyaModel = {
              pensionsCya.copy(
                paymentsIntoPension = viewModel.copy(
                  rasPensionPaymentQuestion = Some(yesNo),
                  totalRASPaymentsAndTaxRelief = if (yesNo) viewModel.totalRASPaymentsAndTaxRelief else None,
                  oneOffRasPaymentPlusTaxReliefQuestion = if (yesNo) viewModel.oneOffRasPaymentPlusTaxReliefQuestion else None,
                  totalOneOffRasPaymentPlusTaxRelief = if (yesNo) viewModel.totalOneOffRasPaymentPlusTaxRelief else None,
                  totalPaymentsIntoRASQuestion = if (yesNo) viewModel.totalPaymentsIntoRASQuestion else None
                )
              )
            }
            val redirectLocation = if (yesNo) {
              ReliefAtSourcePaymentsAndTaxReliefAmountController.show(taxYear)
            } else {
              PensionsTaxReliefNotClaimedController.show(taxYear)
            }

            pensionSessionService.createOrUpdateSessionData(request.user,
              updatedCyaModel, taxYear, optData.exists(_.isPriorSubmission))(errorHandler.internalServerError()) {
              isFinishedCheck(updatedCyaModel, taxYear, redirectLocation)
            }
        }
      }
    )
  }

}
