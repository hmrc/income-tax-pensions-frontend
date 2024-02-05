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
import controllers.pensions.unauthorisedPayments.routes.UkPensionSchemeDetailsController
import controllers.predicates.actions.AuthorisedAction
import controllers.predicates.actions.TaxYearAction.taxYearAction
import models.mongo.PensionsCYAModel
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import services.redirects.SimpleRedirectService.redirectBasedOnCurrentAnswers
import services.redirects.UnauthorisedPaymentsPages.RemovePSTRPage
import services.redirects.UnauthorisedPaymentsRedirects.{cyaPageCall, journeyCheck}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.pensions.unauthorisedPayments.RemovePSTRView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RemovePSTRController @Inject() (implicit
    val mcc: MessagesControllerComponents,
    authAction: AuthorisedAction,
    view: RemovePSTRView,
    appConfig: AppConfig,
    pensionSessionService: PensionSessionService,
    errorHandler: ErrorHandler,
    clock: Clock,
    ec: ExecutionContext)
    extends FrontendController(mcc)
    with I18nSupport
    with SessionHelper {

  def show(taxYear: Int, pensionSchemeIndex: Option[Int]): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async {
    implicit request =>
      pensionSessionService.loadSessionData(taxYear, request.user).flatMap {
        case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))

        case Right(optData) =>
          val checkRedirect = journeyCheck(RemovePSTRPage, _: PensionsCYAModel, taxYear, pensionSchemeIndex)
          redirectBasedOnCurrentAnswers(taxYear, optData, cyaPageCall(taxYear))(checkRedirect) { data =>
            val pstrList: Seq[String] = data.pensions.unauthorisedPayments.pensionSchemeTaxReference.getOrElse(Seq.empty)
            checkIndexScheme(pensionSchemeIndex, pstrList) match {
              case Some(scheme) =>
                Future.successful(Ok(view(taxYear, scheme, pensionSchemeIndex)))
              case _ =>
                Future.successful(Redirect(UkPensionSchemeDetailsController.show(taxYear)))
            }
          }
      }
  }

  def submit(taxYear: Int, pensionSchemeIndex: Option[Int]): Action[AnyContent] = authAction.async { implicit request =>
    pensionSessionService.loadSessionData(taxYear, request.user).flatMap {
      case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
      case Right(optData) =>
        val checkRedirect = journeyCheck(RemovePSTRPage, _: PensionsCYAModel, taxYear, pensionSchemeIndex)
        redirectBasedOnCurrentAnswers(taxYear, optData, cyaPageCall(taxYear))(checkRedirect) { data =>
          val pensionsCYAModel      = data.pensions
          val viewModel             = pensionsCYAModel.unauthorisedPayments
          val pstrList: Seq[String] = viewModel.pensionSchemeTaxReference.getOrElse(Seq.empty)

          checkIndexScheme(pensionSchemeIndex, pstrList) match {
            case Some(_) =>
              val rawPstrList: Seq[String] = pstrList.patch(pensionSchemeIndex.get, Nil, 1)

              val updatedPstrList = if (rawPstrList.isEmpty) None else Some(rawPstrList)

              val updatedCyaModel = pensionsCYAModel.copy(unauthorisedPayments = viewModel.copy(pensionSchemeTaxReference = updatedPstrList))

              // TODO - call API to remove pension scheme
              pensionSessionService.createOrUpdateSessionData(request.user, updatedCyaModel, taxYear, data.isPriorSubmission)(
                errorHandler.internalServerError()) {
                Redirect(UkPensionSchemeDetailsController.show(taxYear))
              }
            case _ =>
              Future.successful(Redirect(UkPensionSchemeDetailsController.show(taxYear)))
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

}
