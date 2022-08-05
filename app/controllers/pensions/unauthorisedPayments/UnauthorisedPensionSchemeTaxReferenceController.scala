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
import controllers.predicates.AuthorisedAction
import forms.PensionSchemeTaxReferenceForm
import models.mongo.PensionsCYAModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import views.html.pensions.unauthorisedPayments.PensionSchemeTaxReferenceView
import controllers.pensions.routes.PensionsSummaryController
import controllers.predicates.TaxYearAction.taxYearAction

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UnauthorisedPensionSchemeTaxReferenceController @Inject()(implicit val cc: MessagesControllerComponents,
                                                                authAction: AuthorisedAction,
                                                                pensionSchemeTaxReferenceView: PensionSchemeTaxReferenceView,
                                                                appConfig: AppConfig,
                                                                pensionSessionService: PensionSessionService,
                                                                errorHandler: ErrorHandler,
                                                                clock: Clock,
                                                                ec: ExecutionContext) extends FrontendController(cc) with I18nSupport {


  val isAgent = (isAgent : Boolean) => if (isAgent) "agent" else "individual"

  def show(taxYear: Int): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async{ implicit request =>
    pensionSessionService.getPensionSessionData(taxYear, request.user).flatMap{
      case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
      case Right(Some(_)) => {
        val errorMsgDetails = (
          s"unauthorisedPayments.pension.pensionSchemeTaxReference.error.noEntry.${isAgent(request.user.isAgent)}",
          s"unauthorisedPayments.pension.pensionSchemeTaxReference.error.incorrectFormat.${isAgent(request.user.isAgent)}",
        )
        val emptyForm: Form[String] = PensionSchemeTaxReferenceForm.pensionSchemeTaxReferenceForm(errorMsgDetails._1, errorMsgDetails._2)
          //TODO: capability to add or reference a particular pension scheme tax reference
          Future.successful(Ok(pensionSchemeTaxReferenceView(emptyForm, taxYear)))
        }
      case Right(None) => Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
    }
  }


  def submit(taxYear: Int): Action[AnyContent] = authAction.async {
    implicit request =>
      val errorMsgDetails = (
        s"unauthorisedPayments.pension.pensionSchemeTaxReference.error.noEntry.${isAgent(request.user.isAgent)}",
        s"unauthorisedPayments.pension.pensionSchemeTaxReference.error.incorrectFormat.${isAgent(request.user.isAgent)}",
      )
      PensionSchemeTaxReferenceForm.pensionSchemeTaxReferenceForm(errorMsgDetails._1, errorMsgDetails._2).bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(pensionSchemeTaxReferenceView(formWithErrors, taxYear))),
        pstr => {
          pensionSessionService.getPensionSessionData(taxYear, request.user).flatMap {
            case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
            case Right(Some(optData)) => {

              val pstrList: Option[Seq[String]] = optData.pensions.unauthorisedPayments.pensionSchemeTaxReference
                val pensionsCYAModel: PensionsCYAModel = optData.pensions
                val viewModelUnauthorisedPaymentsPstr = pensionsCYAModel.unauthorisedPayments
                val newPensionSchemeTaxRef = Seq(pstr)

                val updatedPstrList: Seq[String] = pstrList match {
                  case Some(list) => list ++ newPensionSchemeTaxRef
                  case None => newPensionSchemeTaxRef
                }

                val updatedCyaModel = pensionsCYAModel.copy(
                  unauthorisedPayments = viewModelUnauthorisedPaymentsPstr.copy(
                    pensionSchemeTaxReference = Some(updatedPstrList)
                  ))

                pensionSessionService.createOrUpdateSessionData(request.user,
                  updatedCyaModel, taxYear, optData.isPriorSubmission)(errorHandler.internalServerError()) {
                  //TODO: redirect to the annual allowances CYA page
                  Redirect(PensionsSummaryController.show(taxYear))
                }
            }
            //TODO: redirect to the annual allowances CYA page
            case _ => Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
          }
        }
      )
  }
}
