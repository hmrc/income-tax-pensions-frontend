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

package controllers.pensions.paymentsIntoOverseasPensions

import config.{AppConfig, ErrorHandler}
import controllers.pensions.paymentsIntoOverseasPensions.routes.PaymentsIntoOverseasPensionsCYAController
import controllers.pensions.routes.OverseasPensionsSummaryController
import controllers.predicates.AuthorisedAction
import forms.YesNoForm
import models.User
import models.mongo.PensionsCYAModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import services.redirects.PaymentsIntoOverseasPensionsRedirects.redirectForSchemeLoop
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import views.html.pensions.paymentsIntoOverseasPensions.TaxEmployerPaymentsView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TaxEmployerPaymentsController @Inject()(authAction: AuthorisedAction,
                                              taxEmployerPaymentsView: TaxEmployerPaymentsView,
                                              pensionSessionService: PensionSessionService,
                                              errorHandler: ErrorHandler
                                             )(implicit val mcc: MessagesControllerComponents,
                                               appConfig: AppConfig,
                                               clock: Clock,
                                               ec: ExecutionContext) extends FrontendController(mcc) with I18nSupport {

  def yesNoForm(user: User): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"overseasPension.taxEmployerPayments.error.noEntry.${if (user.isAgent) "agent" else "individual"}"
  )

  def show(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    pensionSessionService.getPensionSessionData(taxYear, request.user).flatMap {
      case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
      case Right(optPensionUserData) => optPensionUserData match {
        case Some(data) =>
          data.pensions.paymentsIntoOverseasPensions.taxPaidOnEmployerPaymentsQuestion match {
            case Some(value) => Future.successful(Ok(taxEmployerPaymentsView(
              yesNoForm(request.user).fill(value), taxYear)))
            case None => Future.successful(Ok(taxEmployerPaymentsView(yesNoForm(request.user), taxYear)))
          }
        case None =>
          Future.successful(Redirect(OverseasPensionsSummaryController.show(taxYear)))
      }
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    yesNoForm(request.user).bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(taxEmployerPaymentsView(formWithErrors, taxYear))),
      yesNo => {
        pensionSessionService.getPensionSessionData(taxYear, request.user).flatMap {
          case Right(Some(data)) =>
            val updatedCyaModel: PensionsCYAModel = data.pensions.copy(
              paymentsIntoOverseasPensions = data.pensions.paymentsIntoOverseasPensions.copy(
                taxPaidOnEmployerPaymentsQuestion = Some(yesNo)))

            pensionSessionService.createOrUpdateSessionData(request.user,
              updatedCyaModel, taxYear, data.isPriorSubmission)(errorHandler.internalServerError()) {

              Redirect(
                if (yesNo) redirectForSchemeLoop(updatedCyaModel.paymentsIntoOverseasPensions.reliefs, taxYear)
                else PaymentsIntoOverseasPensionsCYAController.show(taxYear)
              )
            }
          case _ =>
            Future.successful(Redirect(OverseasPensionsSummaryController.show(taxYear)))
        }
      }
    )
  }
}
