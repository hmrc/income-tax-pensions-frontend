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

package controllers.pensions.unauthorisedPayments

import config.{AppConfig, ErrorHandler}
import controllers.predicates.AuthorisedAction
import controllers.predicates.TaxYearAction.taxYearAction
import forms.{AmountForm, FormUtils}
import models.mongo.PensionsCYAModel
import play.api.data.Form
import controllers.pensions.unauthorisedPayments.routes.NonUKTaxOnAmountResultedInSurchargeController
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import services.redirects.SimpleRedirectService.redirectBasedOnCurrentAnswers
import services.redirects.UnauthorisedPaymentsPages.SurchargedAmountPage
import services.redirects.UnauthorisedPaymentsRedirects.{cyaPageCall, isFinishedCheck, journeyCheck}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import views.html.pensions.unauthorisedPayments.SurchargeAmountView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SurchargeAmountController @Inject()(authAction: AuthorisedAction,
                                          view: SurchargeAmountView,
                                          pensionSessionService: PensionSessionService,
                                          errorHandler: ErrorHandler)
                                         (implicit val mcc: MessagesControllerComponents,
                                          appConfig: AppConfig, clock: Clock,
                                          ec: ExecutionContext) extends FrontendController(mcc) with I18nSupport with FormUtils {


  val amountForm: Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = "unauthorisedPayments.surchargeAmount.error.noEntry",
    wrongFormatKey = "common.error.incorrectFormat"
  )


  def show(taxYear: Int): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async { implicit request =>
    pensionSessionService.getPensionSessionData(taxYear, request.user).flatMap {
      case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
      case Right(optData) =>
        val checkRedirect = journeyCheck(SurchargedAmountPage, _, taxYear)
        redirectBasedOnCurrentAnswers(taxYear, optData, cyaPageCall(taxYear))(checkRedirect) { data =>
          data.pensions.unauthorisedPayments.surchargeAmount
            .map(value => Future.successful(Ok(view(amountForm.fill(value), taxYear))))
            .getOrElse(Future.successful(Ok(view(amountForm, taxYear))))
        }
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    amountForm.bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear))),
      amount => {
        pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) { optData =>
          val checkRedirect = journeyCheck(SurchargedAmountPage, _, taxYear)
          redirectBasedOnCurrentAnswers(taxYear, optData, cyaPageCall(taxYear))(checkRedirect) { data =>

            val pensionsCYAModel: PensionsCYAModel = data.pensions
            val viewModel = pensionsCYAModel.unauthorisedPayments
            val updatedCyaModel: PensionsCYAModel = pensionsCYAModel.copy(unauthorisedPayments = viewModel.copy(surchargeAmount = Some(amount)))

            pensionSessionService.createOrUpdateSessionData(request.user,
              updatedCyaModel, taxYear, data.isPriorSubmission)(errorHandler.internalServerError()) {
              isFinishedCheck(updatedCyaModel, taxYear, NonUKTaxOnAmountResultedInSurchargeController.show(taxYear))
            }
          }
        }
      }
    )
  }
}
