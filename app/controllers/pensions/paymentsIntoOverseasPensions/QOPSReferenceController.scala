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
import controllers.pensions.paymentsIntoOverseasPensions.routes.{PensionsCustomerReferenceNumberController, QOPSReferenceController}
import controllers.pensions.routes.PensionsSummaryController
import controllers.predicates.{ActionsProvider, AuthorisedAction}
import forms.QOPSReferenceNumberForm
import models.User
import models.mongo.{PensionsCYAModel, PensionsUserData}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.pensions.paymentsIntoOverseasPensions.QOPSReferenceView

import javax.inject.{Inject, Singleton}
import controllers._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class QOPSReferenceController @Inject()(actionsProvider: ActionsProvider,
                                        qopsReferenceView: QOPSReferenceView,
                                        pensionSessionService: PensionSessionService,
                                        errorHandler: ErrorHandler,
                                        ec: ExecutionContext
                                       )(implicit val mcc: MessagesControllerComponents,
                                         appConfig: AppConfig,
                                         clock: Clock) extends FrontendController(mcc) with I18nSupport with SessionHelper {
  def referenceForm(): Form[String] = QOPSReferenceNumberForm.qopsReferenceNumberForm(
    incorrectFormatMsg = "common.overseasPensions.qops.error.incorrectFormat"
  )

  def show(taxYear: Int, index : Option[Int]): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async {
    implicit sessionData =>
      validateIndex(index, sessionData.pensionsUserData.pensions.paymentsIntoOverseasPensions.reliefs.size) match {
        case Some(idx) => Future.successful(Ok(qopsReferenceView(referenceForm(sessionData.user, sessionData.pensionsUserData, idx), taxYear, Some(idx))))
        case _ => Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
      }
  }

  private def referenceForm(user: User, pensionUserData: PensionsUserData, index: Int): Form[String] = {
    val qopsNumber = pensionUserData.pensions.paymentsIntoOverseasPensions.reliefs(index).qualifyingOverseasPensionSchemeReferenceNumber
    qopsNumber match {
      case Some(qopsNumber) => referenceForm().fill(removePrefix(qopsNumber))
      case None => referenceForm()
    }
  }

  def submit(taxYear: Int, index : Option[Int]): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async {
    implicit request =>
      validateIndex(index, request.pensionsUserData.pensions.paymentsIntoOverseasPensions.reliefs.size) match {
        case Some(idx) =>
          referenceForm().bindFromRequest().fold(
            formWithErrors => Future.successful(BadRequest(qopsReferenceView(formWithErrors, taxYear, index))),
            referenceNumber => {
              val relief = request.pensionsUserData.pensions.paymentsIntoOverseasPensions.reliefs(idx)
              val updatedCyaModel: PensionsCYAModel = request.pensionsUserData.pensions.copy(
                paymentsIntoOverseasPensions = request.pensionsUserData.pensions.paymentsIntoOverseasPensions.copy(
                  reliefs = request.pensionsUserData.pensions.paymentsIntoOverseasPensions.reliefs.updated(
                    idx, relief.copy(qualifyingOverseasPensionSchemeReferenceNumber = Some(referenceNumber))
                  )))
              pensionSessionService.createOrUpdateSessionData(request.user,
                updatedCyaModel, taxYear, request.pensionsUserData.isPriorSubmission)(errorHandler.internalServerError()) {
                Redirect(QOPSReferenceController.show(taxYear, Some(idx))) //TODO - redirect to pensions-overseas-details-summary
              }
            }
          )
        case _ => Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
      }
  }

  def removePrefix(qopsReference: String): String = if (qopsReference.length == 10) qopsReference.substring(4, 10) else qopsReference
}