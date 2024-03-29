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
import controllers.pensions.unauthorisedPayments.routes._
import controllers.predicates.actions.AuthorisedAction
import controllers.predicates.actions.TaxYearAction.taxYearAction
import forms.PensionSchemeTaxReferenceForm
import models.mongo.PensionsCYAModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import services.redirects.SimpleRedirectService.redirectBasedOnCurrentAnswers
import services.redirects.UnauthorisedPaymentsPages.PSTRPage
import services.redirects.UnauthorisedPaymentsRedirects.{cyaPageCall, journeyCheck}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.pensions.unauthorisedPayments.PensionSchemeTaxReferenceView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UnauthorisedPensionSchemeTaxReferenceController @Inject() (cc: MessagesControllerComponents,
                                                                 authAction: AuthorisedAction,
                                                                 view: PensionSchemeTaxReferenceView,
                                                                 pensionSessionService: PensionSessionService,
                                                                 errorHandler: ErrorHandler)(implicit appConfig: AppConfig, ec: ExecutionContext)
    extends FrontendController(cc)
    with I18nSupport {

  def show(taxYear: Int, pensionSchemeIndex: Option[Int]): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async {
    implicit request =>
      val errorMsgDetails = (
        s"common.pensionSchemeTaxReference.error.noEntry.${if (request.user.isAgent) "agent" else "individual"}",
        "common.pensionSchemeTaxReference.error.incorrectFormat"
      )
      val emptyForm: Form[String] = PensionSchemeTaxReferenceForm.pensionSchemeTaxReferenceForm(errorMsgDetails._1, errorMsgDetails._2)

      pensionSessionService.loadSessionData(taxYear, request.user).flatMap {
        case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
        case Right(optData) =>
          val checkRedirect = journeyCheck(PSTRPage, _: PensionsCYAModel, taxYear, pensionSchemeIndex)
          redirectBasedOnCurrentAnswers(taxYear, optData, cyaPageCall(taxYear))(checkRedirect) { data =>
            val pstrList: Seq[String] = data.pensions.unauthorisedPayments.pensionSchemeTaxReference.getOrElse(Seq.empty)
            checkIndexScheme(pensionSchemeIndex, pstrList) match {
              case Some(scheme) =>
                Future.successful(Ok(view(emptyForm.fill(scheme), pensionSchemeIndex, taxYear)))
              case None =>
                Future.successful(Ok(view(emptyForm, pensionSchemeIndex, taxYear)))
            }
          }
      }
  }

  private def checkIndexScheme(pensionSchemeIndex: Option[Int], pensionSchemesList: Seq[String]): Option[String] =
    pensionSchemeIndex match {
      case Some(index) if pensionSchemesList.size > index =>
        Some(pensionSchemesList(index))
      case _ =>
        None
    }

  def submit(taxYear: Int, pensionSchemeIndex: Option[Int]): Action[AnyContent] = authAction.async { implicit request =>
    val errorMsgDetails = (
      s"common.pensionSchemeTaxReference.error.noEntry.${if (request.user.isAgent) "agent" else "individual"}",
      "common.pensionSchemeTaxReference.error.incorrectFormat"
    )

    PensionSchemeTaxReferenceForm
      .pensionSchemeTaxReferenceForm(errorMsgDetails._1, errorMsgDetails._2)
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, pensionSchemeIndex, taxYear))),
        pstr =>
          pensionSessionService.loadSessionData(taxYear, request.user).flatMap {
            case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
            case Right(optData) =>
              val checkRedirect = journeyCheck(PSTRPage, _: PensionsCYAModel, taxYear, pensionSchemeIndex)
              redirectBasedOnCurrentAnswers(taxYear, optData, cyaPageCall(taxYear))(checkRedirect) { data =>
                val viewModel = data.pensions.unauthorisedPayments
                val updatedList =
                  (viewModel.pensionSchemeTaxReference, pensionSchemeIndex) match {
                    case (Some(pstrList), Some(pensionSchemeIndex)) =>
                      pstrList.updated(pensionSchemeIndex, pstr)
                    case (Some(pstrList), None) =>
                      pstrList ++ Seq(pstr)
                    case (None, _) =>
                      Seq(pstr)
                  }

                val updatedCyaModel = data.pensions.copy(
                  unauthorisedPayments = viewModel.copy(
                    pensionSchemeTaxReference = Some(updatedList)
                  ))
                pensionSessionService.createOrUpdateSessionData(request.user, updatedCyaModel, taxYear, data.isPriorSubmission)(
                  errorHandler.internalServerError()) {
                  Redirect(UkPensionSchemeDetailsController.show(taxYear))
                }
              }
          }
      )
  }
}
