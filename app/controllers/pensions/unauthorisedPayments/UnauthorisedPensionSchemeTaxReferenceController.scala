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
import controllers.predicates.AuthorisedAction
import controllers.predicates.TaxYearAction.taxYearAction
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
import utils.Clock
import views.html.pensions.unauthorisedPayments.PensionSchemeTaxReferenceView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UnauthorisedPensionSchemeTaxReferenceController @Inject()(implicit val cc: MessagesControllerComponents,
                                                                authAction: AuthorisedAction,
                                                                pensionSchemeTaxReferenceView: PensionSchemeTaxReferenceView,
                                                                appConfig: AppConfig,
                                                                pensionSessionService: PensionSessionService,
                                                                errorHandler: ErrorHandler,
                                                                clock: Clock,
                                                                ec: ExecutionContext) extends FrontendController(cc) with I18nSupport {

  def show(taxYear: Int, pensionSchemeIndex: Option[Int]): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async { implicit request =>

    val errorMsgDetails = (
      s"common.pensionSchemeTaxReference.error.noEntry.${if (request.user.isAgent) "agent" else "individual"}",
      s"unauthorisedPayments.pension.pensionSchemeTaxReference.error.incorrectFormat.${if (request.user.isAgent) "agent" else "individual"}"
    )
    val emptyForm: Form[String] = PensionSchemeTaxReferenceForm.pensionSchemeTaxReferenceForm(errorMsgDetails._1, errorMsgDetails._2)

    pensionSessionService.getPensionSessionData(taxYear, request.user).flatMap {
      case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
      case Right(optData) =>
        val checkRedirect = journeyCheck(PSTRPage, _, taxYear)
        redirectBasedOnCurrentAnswers(taxYear, optData, cyaPageCall(taxYear))(checkRedirect) { data =>

          val pstrList: Seq[String] = data.pensions.unauthorisedPayments.pensionSchemeTaxReference.getOrElse(Seq.empty)
          checkIndexScheme(pensionSchemeIndex, pstrList) match {
            case Some(scheme) =>
              Future.successful(Ok(pensionSchemeTaxReferenceView(emptyForm.fill(scheme), pensionSchemeIndex, taxYear)))
            case None =>
              Future.successful(Ok(pensionSchemeTaxReferenceView(emptyForm, pensionSchemeIndex, taxYear)))
          }
        }
    }
  }

  def submit(taxYear: Int, pensionSchemeIndex: Option[Int]): Action[AnyContent] = authAction.async { implicit request =>
    val errorMsgDetails = (
      s"common.pensionSchemeTaxReference.error.noEntry.${if (request.user.isAgent) "agent" else "individual"}",
      s"unauthorisedPayments.pension.pensionSchemeTaxReference.error.incorrectFormat.${if (request.user.isAgent) "agent" else "individual"}"
    )

    PensionSchemeTaxReferenceForm.pensionSchemeTaxReferenceForm(
      errorMsgDetails._1, errorMsgDetails._2
    ).bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(pensionSchemeTaxReferenceView(formWithErrors, pensionSchemeIndex, taxYear))),
      pstr => {
        pensionSessionService.getPensionSessionData(taxYear, request.user).flatMap {
          case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
          case Right(optData) =>
            val checkRedirect = journeyCheck(PSTRPage, _, taxYear)
            redirectBasedOnCurrentAnswers(taxYear, optData, cyaPageCall(taxYear))(checkRedirect) { data =>

              val pensionsCYAModel: PensionsCYAModel = data.pensions
              val viewModel = pensionsCYAModel.unauthorisedPayments
              val pstrList: Seq[String] = data.pensions.unauthorisedPayments.pensionSchemeTaxReference.getOrElse(Seq.empty)

              if (validateIndexScheme(pensionSchemeIndex, pstrList)) {
                val updatedList =
                  (viewModel.pensionSchemeTaxReference, pensionSchemeIndex) match {
                    case (Some(pstrList), Some(pensionSchemeIndex)) =>
                      pstrList.updated(pensionSchemeIndex, pstr)
                    case (Some(pstrList), None) =>
                      pstrList ++ Seq(pstr)
                    case (None, _) =>
                      Seq(pstr)
                  }

                val updatedCyaModel = pensionsCYAModel.copy(
                  unauthorisedPayments = viewModel.copy(
                    pensionSchemeTaxReference = Some(updatedList)
                  ))
                pensionSessionService.createOrUpdateSessionData(request.user,
                  updatedCyaModel, taxYear, data.isPriorSubmission)(errorHandler.internalServerError()) {
                  Redirect(UkPensionSchemeDetailsController.show(taxYear))
                }
              } else {
                Future.successful(Redirect(UkPensionSchemeDetailsController.show(taxYear)))
              }
            }
        }
      }
    )
  }

  private def checkIndexScheme(pensionSchemeIndex: Option[Int], pensionSchemesList: Seq[String]): Option[String] = {
    pensionSchemeIndex match {
      case Some(index) if pensionSchemesList.size > index =>
        Some(pensionSchemesList(index))
      case _ =>
        None
    }
  }

  private def validateIndexScheme(pensionSchemeIndex: Option[Int], pensionSchemesList: Seq[String]): Boolean = {
    pensionSchemeIndex match {
      case Some(index) if pensionSchemesList.size > index =>
        true
      case None =>
        true
      case _ =>
        false
    }
  }
}
