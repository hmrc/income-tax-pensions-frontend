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
import controllers.predicates.AuthorisedAction
import forms.QOPSReferenceNumberForm
import models.User
import models.mongo.{PensionsCYAModel, PensionsUserData}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import views.html.pensions.paymentsIntoOverseasPensions.QOPSReferenceView
import javax.inject.{Inject, Singleton}
import controllers._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class QOPSReferenceController @Inject()(authAction: AuthorisedAction,
                                        qopsReferenceView: QOPSReferenceView,
                                        pensionSessionService: PensionSessionService,
                                        errorHandler: ErrorHandler
                                       )(implicit val mcc: MessagesControllerComponents,
                                         appConfig: AppConfig,
                                         clock: Clock,
                                         ec: ExecutionContext) extends FrontendController(mcc) with I18nSupport {
  def referenceForm(): Form[String] = QOPSReferenceNumberForm.qopsReferenceNumberForm(
    incorrectFormatMsg = "common.overseasPensions.qops.error.incorrectFormat"
  )

  def show(taxYear: Int, index : Option[Int]): Action[AnyContent] = authAction.async { implicit request =>
    pensionSessionService.getPensionSessionData(taxYear, request.user).map {
      case Left(_) => errorHandler.handleError(INTERNAL_SERVER_ERROR)
      case Right(optPensionUserData) =>
        optPensionUserData match {
          case Some(pensionsUserData) =>
            validateIndex(index, pensionsUserData.pensions.paymentsIntoOverseasPensions.reliefs.size) match {
              case Some(index) => Ok(qopsReferenceView(referenceForm(request.user, pensionsUserData, index), taxYear, Some(index)))
              case None => Redirect(PensionsSummaryController.show(taxYear))
            }
          case None => Redirect(PensionsSummaryController.show(taxYear))
        }
    }
  }

  private def referenceForm(user: User, pensionUserData: PensionsUserData, index: Int): Form[String] = {
    val qopsNumber = pensionUserData.pensions.paymentsIntoOverseasPensions.reliefs(index).qualifyingOverseasPensionSchemeReferenceNumber
    qopsNumber match {
      case Some(qopsNumber) => referenceForm().fill(removePrefix(qopsNumber))
      case None => referenceForm()
    }
  }


  def submit(taxYear: Int, index : Option[Int]): Action[AnyContent] = authAction.async { implicit request =>
    referenceForm().bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(qopsReferenceView(formWithErrors, taxYear, index))),
      referenceNumber => {
        pensionSessionService.getPensionSessionData(taxYear, request.user).flatMap {
          case Right(Some(data)) =>
            validateIndex(index, data.pensions.paymentsIntoOverseasPensions.reliefs.size) match {
              case Some(index) => {
                val relief = data.pensions.paymentsIntoOverseasPensions.reliefs(index)
                val updatedCyaModel: PensionsCYAModel = data.pensions.copy(
                  paymentsIntoOverseasPensions = data.pensions.paymentsIntoOverseasPensions.copy(
                    reliefs = data.pensions.paymentsIntoOverseasPensions.reliefs.updated(
                      index, relief.copy(qualifyingOverseasPensionSchemeReferenceNumber = Some(referenceNumber))
                    )))
                pensionSessionService.createOrUpdateSessionData(request.user,
                  updatedCyaModel, taxYear, data.isPriorSubmission)(errorHandler.internalServerError()) {
                  Redirect(QOPSReferenceController.show(taxYear, Some(index))) //TODO - redirect to pensions-overseas-details-summary
                }
              }
              case None => Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
            }
          case _ => Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
        }
      }
    )
  }

  def removePrefix(qopsReference: String): String = if (qopsReference.length == 10) qopsReference.substring(4, 10) else qopsReference
}