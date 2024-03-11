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
import controllers.pensions.paymentsIntoOverseasPensions.routes._
import controllers.predicates.actions.ActionsProvider
import forms.PensionCustomerReferenceNumberForm
import models.User
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.pension.charges.Relief
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.PensionSessionService
import services.redirects.PaymentsIntoOverseasPensionsPages.PensionsCustomerReferenceNumberPage
import services.redirects.PaymentsIntoOverseasPensionsRedirects.{cyaPageCall, indexCheckThenJourneyCheck, journeyCheck, schemeIsFinishedCheck}
import services.redirects.SimpleRedirectService.redirectBasedOnCurrentAnswers
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.pensions.paymentsIntoOverseasPensions.PensionsCustomerReferenceNumberView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class PensionsCustomerReferenceNumberController @Inject() (actionsProvider: ActionsProvider,
                                                           view: PensionsCustomerReferenceNumberView,
                                                           pensionSessionService: PensionSessionService,
                                                           errorHandler: ErrorHandler,
                                                           mcc: MessagesControllerComponents)(implicit appConfig: AppConfig)
    extends FrontendController(mcc)
    with I18nSupport {

  def referenceForm(user: User): Form[String] = PensionCustomerReferenceNumberForm.pensionCustomerReferenceNumberForm(
    incorrectFormatMsg = s"overseasPension.pensionsCustomerReferenceNumber.error.noEntry.${if (user.isAgent) "agent" else "individual"}"
  )

  def show(taxYear: Int, index: Option[Int]): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async { implicit sessionDataRequest =>
    index match {
      case Some(_) =>
        indexCheckThenJourneyCheck(sessionDataRequest.pensionsUserData, index, PensionsCustomerReferenceNumberPage, taxYear) { relief =>
          relief.customerReference match {
            case Some(customerReferenceNumber) =>
              Future.successful(
                Ok(
                  view(
                    referenceForm(sessionDataRequest.user)
                      .fill(customerReferenceNumber),
                    taxYear,
                    index)))
            case None =>
              Future.successful(Ok(view(referenceForm(sessionDataRequest.user), taxYear, index)))
          }
        }
      case None =>
        val checkRedirect = journeyCheck(PensionsCustomerReferenceNumberPage, _: PensionsCYAModel, taxYear)
        redirectBasedOnCurrentAnswers(taxYear, Some(sessionDataRequest.pensionsUserData), cyaPageCall(taxYear))(checkRedirect) {
          data: PensionsUserData =>
            Future.successful(Ok(view(referenceForm(sessionDataRequest.user), taxYear, index)))
        }
    }
  }

  def submit(taxYear: Int, optIndex: Option[Int]): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async {
    implicit sessionDataRequest =>
      val piop = sessionDataRequest.pensionsUserData.pensions.paymentsIntoOverseasPensions

      def createOrUpdateSessionData(updatedCyaModel: PensionsCYAModel, newIndex: Some[Int]): Future[Result] =
        pensionSessionService.createOrUpdateSessionData(
          sessionDataRequest.user,
          updatedCyaModel,
          taxYear,
          sessionDataRequest.pensionsUserData.isPriorSubmission)(errorHandler.internalServerError()) {
          schemeIsFinishedCheck(
            updatedCyaModel.paymentsIntoOverseasPensions.reliefs,
            newIndex.getOrElse(0),
            taxYear,
            UntaxedEmployerPaymentsController.show(taxYear, newIndex))
        }

      optIndex match {
        case None =>
          val checkRedirect = journeyCheck(PensionsCustomerReferenceNumberPage, _: PensionsCYAModel, taxYear)
          redirectBasedOnCurrentAnswers(taxYear, Some(sessionDataRequest.pensionsUserData), cyaPageCall(taxYear))(checkRedirect) {
            data: PensionsUserData =>
              referenceForm(sessionDataRequest.user)
                .bindFromRequest()
                .fold(
                  formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear, optIndex))),
                  pensionCustomerReferenceNumber => {
                    val updatedCyaModel: PensionsCYAModel = data.pensions.copy(
                      paymentsIntoOverseasPensions = piop.copy(
                        reliefs = piop.reliefs :+ Relief(customerReference = Some(pensionCustomerReferenceNumber))
                      ))
                    createOrUpdateSessionData(updatedCyaModel, Some(updatedCyaModel.paymentsIntoOverseasPensions.reliefs.size - 1))
                  }
                )
          }
        case Some(index) =>
          indexCheckThenJourneyCheck(sessionDataRequest.pensionsUserData, Some(index), PensionsCustomerReferenceNumberPage, taxYear) { _: Relief =>
            referenceForm(sessionDataRequest.user)
              .bindFromRequest()
              .fold(
                formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear, Some(index)))),
                pensionCustomerReferenceNumber => {
                  val updatedCyaModel: PensionsCYAModel = sessionDataRequest.pensionsUserData.pensions.copy(
                    paymentsIntoOverseasPensions = piop.copy(
                      reliefs = piop.reliefs.updated(
                        index,
                        piop.reliefs(index).copy(customerReference = Some(pensionCustomerReferenceNumber))
                      )))
                  val currentIndex = optIndex.fold(Some(updatedCyaModel.paymentsIntoOverseasPensions.reliefs.size - 1))(Some(_))
                  createOrUpdateSessionData(updatedCyaModel, currentIndex)
                }
              )
          }
      }
  }
}
