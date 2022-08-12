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

package controllers.pensions.unauthorisedPayments

import config.{AppConfig, ErrorHandler}
import controllers.pensions.routes.PensionsSummaryController
import controllers.pensions.unauthorisedPayments.routes.{NonUkTaxOnAmountNotSurchargeController, WhereAnyOfTheUnauthorisedPaymentsController, NoSurchargeAmountController}
import controllers.predicates.AuthorisedAction
import controllers.predicates.TaxYearAction.taxYearAction
import forms.{AmountForm, FormUtils}
import models.mongo.PensionsCYAModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.pensions.unauthorisedPayments.NoSurchargeAmountView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class NoSurchargeAmountController @Inject()(authAction: AuthorisedAction,
                                            view: NoSurchargeAmountView,
                                            pensionSessionService: PensionSessionService,
                                            errorHandler: ErrorHandler)
                                           (implicit val mcc: MessagesControllerComponents, appConfig: AppConfig, clock: Clock, ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport with SessionHelper with FormUtils {

  val amountForm: Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = "unauthorisedPayments.noSurchargeAmount.error.noEntry",
    wrongFormatKey = "common.error.incorrectFormat" //changed this to common to avoid duplicate messages error on jenkins SASS-3240 unauthorisedPayments.noSurchargeAmount.error.incorrectFormat
  )

  def show(taxYear: Int): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async { implicit request =>
    pensionSessionService.getPensionSessionData(taxYear, request.user).flatMap {
      case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))

      case Right(Some(data)) if data.pensions.unauthorisedPayments.noSurchargeQuestion.contains(true) =>
        data.pensions.unauthorisedPayments.noSurchargeAmount
          .map(value => Future.successful(Ok(view(amountForm.fill(value), taxYear))))
          .getOrElse(Future.successful(Ok(view(amountForm, taxYear))))

      //Todo Is this a necessary check? as opposed to caller DidYouPayNonUkTaxController.submit()
      case Right(Some(data)) if data.pensions.unauthorisedPayments.noSurchargeQuestion.contains(false) =>
        Future.successful(Redirect(WhereAnyOfTheUnauthorisedPaymentsController.show(taxYear)))

      case _ =>
        //Todo redirect to CYA page when implemented
        Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    amountForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear))),
      amount => {
        pensionSessionService.getPensionSessionData(taxYear, request.user).flatMap {
          case Right(Some(data)) if data.pensions.unauthorisedPayments.noSurchargeQuestion.contains(true) =>
            val updatedCyaModel: PensionsCYAModel = data.pensions.copy(unauthorisedPayments = data.pensions.unauthorisedPayments.copy(noSurchargeAmount = Some(amount)))

            pensionSessionService.createOrUpdateSessionData(request.user,
              updatedCyaModel, taxYear, data.isPriorSubmission)(errorHandler.internalServerError()) {

              Redirect(NonUkTaxOnAmountNotSurchargeController.show(taxYear))
            }
          //Todo strange case that may need to be looked at
          case Right(Some(data)) if data.pensions.unauthorisedPayments.noSurchargeQuestion.contains(false) =>
            Future.successful(Redirect(WhereAnyOfTheUnauthorisedPaymentsController.show(taxYear)))
          case _ =>
            //TODO - redirect to CYA page once implemented#
            Future.successful(Redirect(NoSurchargeAmountController.show(taxYear)))
        }
      }
    )
  }
}
