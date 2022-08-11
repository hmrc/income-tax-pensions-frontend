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

package controllers.pensions.paymentsIntoOverseasPensions

import config.{AppConfig, ErrorHandler}
import controllers.pensions.routes.PensionsSummaryController
import controllers.predicates.AuthorisedAction
import forms.RadioButtonAmountForm
import models.User
import models.mongo.PensionsCYAModel
import models.pension.charges.PaymentsIntoOverseasPensionsViewModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import views.html.pensions.paymentsIntoOverseasPensions.PaymentIntoPensionSchemeView
import controllers.pensions.paymentsIntoOverseasPensions.routes.PaymentIntoPensionScheme
import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PaymentIntoPensionScheme @Inject()(implicit val cc: MessagesControllerComponents,
                                         authAction: AuthorisedAction,
                                         paymentIntoPensionSchemeView: PaymentIntoPensionSchemeView,
                                         appConfig: AppConfig,
                                         pensionSessionService: PensionSessionService,
                                         errorHandler: ErrorHandler,
                                         clock: Clock) extends FrontendController(cc) with I18nSupport {

  def form(user: User): Form[(Boolean, Option[BigDecimal])] = RadioButtonAmountForm.radioButtonAndAmountForm(
    missingInputError = s"overseasPension.paymentIntoOverseasPensionScheme.radio.error.${if(user.isAgent) "agent" else "individual"}",
    emptyFieldKey = s"overseasPension.paymentIntoOverseasPensionScheme.no.entry.error.${if(user.isAgent) "agent" else "individual"}",
    wrongFormatKey = s"overseasPension.paymentIntoOverseasPensionScheme.invalid.format.error.${if(user.isAgent) "agent" else "individual"}",
    exceedsMaxAmountKey = s"overseasPension.paymentIntoOverseasPensionScheme.maximum.error.${if(user.isAgent) "agent" else "individual"}"
  )

  def show(taxYear: Int): Action[AnyContent] = authAction.async { implicit request => {
    pensionSessionService.getPensionSessionData(taxYear, request.user).flatMap {
      case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
      case Right(optPensionUserData) => optPensionUserData match {
        case Some(data) =>
          data.pensions.paymentsIntoOverseasPensions.paymentsIntoOverseasPensionsAmount match {
            case Some(value) =>
              Future.successful(Ok(paymentIntoPensionSchemeView(form(request.user).fill((true, Some(value))), taxYear)))

            case None =>
              Future.successful(Ok(paymentIntoPensionSchemeView(form(request.user), taxYear)))
          }
        case _ =>
          //todo - redirect to overseas pension cya page
          Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
      }
    }
  }

  }

  def submit (taxYear: Int): Action[AnyContent] = authAction.async { implicit request => {
    pensionSessionService.getPensionSessionData(taxYear, request.user).flatMap  {
      case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
      case Right(Some(data)) =>
          form(request.user).bindFromRequest.fold(
            formWithErrors => Future.successful(BadRequest(paymentIntoPensionSchemeView(formWithErrors, taxYear))),
            amounts => {
              val pensionsCYAModel: PensionsCYAModel = data.pensions
              val viewModel: PaymentsIntoOverseasPensionsViewModel = pensionsCYAModel.paymentsIntoOverseasPensions
              val updatedCyaModel: PensionsCYAModel = amounts match {
                case (yesSelected, amountOpt) =>
                  pensionsCYAModel.copy(
                    paymentsIntoOverseasPensions = viewModel.copy(
                      paymentsIntoOverseasPensionsQuestions = Some(yesSelected),
                      paymentsIntoOverseasPensionsAmount = if (yesSelected) amountOpt else None
                    )
                  )
              }
              pensionSessionService.createOrUpdateSessionData(request.user,
                updatedCyaModel, taxYear, data.isPriorSubmission)(errorHandler.internalServerError()) {
                //todo - did your employers pay into your overseas pension scheme page
                 Redirect(PaymentIntoPensionScheme.show(taxYear))
              }
            }
          )

      case _ =>
        //TODO: redirect to overseas cya page
        Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
    }
  }

  }

}
