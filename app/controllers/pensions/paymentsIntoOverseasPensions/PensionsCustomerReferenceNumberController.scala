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
import controllers.predicates.ActionsProvider
import controllers.validatedIndex
import forms.PensionCustomerReferenceNumberForm
import models.User
import models.mongo.PensionsCYAModel
import models.pension.charges.Relief
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.PensionSessionService
import services.redirects.PaymentsIntoOverseasPensionsRedirects.redirectOnBadIndexInSchemeLoop
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import views.html.pensions.paymentsIntoOverseasPensions.PensionsCustomerReferenceNumberView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class PensionsCustomerReferenceNumberController @Inject()(actionsProvider: ActionsProvider,
                                                          pensionsCustomerReferenceNumberView: PensionsCustomerReferenceNumberView,
                                                          pensionSessionService: PensionSessionService,
                                                          errorHandler: ErrorHandler
                                                         )(implicit val mcc: MessagesControllerComponents,
                                                           appConfig: AppConfig,
                                                           clock: Clock) extends FrontendController(mcc) with I18nSupport {

  def referenceForm(user: User): Form[String] = PensionCustomerReferenceNumberForm.pensionCustomerReferenceNumberForm(
    incorrectFormatMsg = s"overseasPension.pensionsCustomerReferenceNumber.error.noEntry.${if (user.isAgent) "agent" else "individual"}"
  )

  def show(taxYear: Int, index: Option[Int]): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async {
    implicit sessionDataRequest =>
      val piopReliefs = sessionDataRequest.pensionsUserData.pensions.paymentsIntoOverseasPensions.reliefs

      index match {
        case Some(_) => validatedIndex(index, piopReliefs.size) match {
          case Some(idx) => piopReliefs(idx).customerReference match {
            case None => Future.successful(Ok(pensionsCustomerReferenceNumberView(referenceForm(sessionDataRequest.user), taxYear, index)))
            case Some(customerReferenceNumber) =>
              Future.successful(Ok(pensionsCustomerReferenceNumberView(referenceForm(sessionDataRequest.user)
                .fill(customerReferenceNumber),
                taxYear, index)))
          }
          case None => Future.successful(Redirect(redirectOnBadIndexInSchemeLoop(piopReliefs, taxYear)))
        }
        case None => Future.successful(Ok(pensionsCustomerReferenceNumberView(referenceForm(sessionDataRequest.user), taxYear, None)))
      }
  }

  def submit(taxYear: Int, index: Option[Int]): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async {
    implicit sessionDataRequest =>

      val piop = sessionDataRequest.pensionsUserData.pensions.paymentsIntoOverseasPensions

      def createOrUpdateSessionData(updatedCyaModel: PensionsCYAModel, newIndex: Some[Int]): Future[Result] =
        pensionSessionService.createOrUpdateSessionData(sessionDataRequest.user, updatedCyaModel, taxYear,
          sessionDataRequest.pensionsUserData.isPriorSubmission)(errorHandler.internalServerError()) {
          Redirect(UntaxedEmployerPaymentsController.show(taxYear, newIndex))
        }

      index match {
        case None => referenceForm(sessionDataRequest.user).bindFromRequest().fold(
          formWithErrors =>
            Future.successful(BadRequest(pensionsCustomerReferenceNumberView(formWithErrors, taxYear, index))),
          pensionCustomerReferenceNumber => {
            val updatedCyaModel: PensionsCYAModel = sessionDataRequest.pensionsUserData.pensions.copy(
              paymentsIntoOverseasPensions = piop.copy(
                reliefs = piop.reliefs :+ Relief(customerReference = Some(pensionCustomerReferenceNumber))
              ))
            createOrUpdateSessionData(updatedCyaModel, Some(updatedCyaModel.paymentsIntoOverseasPensions.reliefs.size - 1))
          })
        case Some(_) =>
          validatedIndex(index, piop.reliefs.size) match {
            case Some(validIndex) =>
              referenceForm(sessionDataRequest.user).bindFromRequest().fold(
                formWithErrors =>
                  Future.successful(BadRequest(pensionsCustomerReferenceNumberView(formWithErrors, taxYear, Some(validIndex)))),
                pensionCustomerReferenceNumber => {
                  val updatedCyaModel: PensionsCYAModel = sessionDataRequest.pensionsUserData.pensions.copy(
                    paymentsIntoOverseasPensions = piop.copy(
                      reliefs = piop.reliefs.updated(
                        validIndex,
                        piop.reliefs(validIndex).copy(customerReference = Some(pensionCustomerReferenceNumber))
                      )))
                  val currentIndex = index.fold(
                    Some(updatedCyaModel.paymentsIntoOverseasPensions.reliefs.size - 1))(Some(_))
                  createOrUpdateSessionData(updatedCyaModel, currentIndex)
                })
            case _ => Future.successful(Redirect(redirectOnBadIndexInSchemeLoop(piop.reliefs, taxYear)))
          }
      }
  }
}
