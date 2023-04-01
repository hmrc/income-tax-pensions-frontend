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

  def show(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    pensionSessionService.getPensionSessionData(taxYear, request.user).map {
      case Left(_) => errorHandler.handleError(INTERNAL_SERVER_ERROR)
      case Right(optPensionUserData) =>
        optPensionUserData
          .map(pensionUserData => Ok(qopsReferenceView(referenceForm(request.user, pensionUserData), taxYear)))
          .getOrElse(Redirect(PensionsSummaryController.show(taxYear)))
    }
  }

  private def referenceForm(user: User, pensionUserData: PensionsUserData): Form[String] =
    pensionUserData.pensions.paymentsIntoOverseasPensions.reliefs.headOption.fold {
      referenceForm()
    } {
      relief =>
        relief.qualifyingOverseasPensionSchemeReferenceNumber.fold {
          referenceForm()
        } {
          value => referenceForm().fill(removePrefix(value))
        }
    }

    def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
          referenceForm().bindFromRequest().fold(
            formWithErrors => Future.successful(BadRequest(qopsReferenceView(formWithErrors, taxYear))),
            referenceNumber => {
              pensionSessionService.getPensionSessionData(taxYear, request.user).flatMap {
                case Right(Some(data)) =>
                  data.pensions.paymentsIntoOverseasPensions.reliefs.headOption match { // todo currently only updates first in list - upgrade ticket
                    case Some(relief) => val updatedCyaModel: PensionsCYAModel = data.pensions.copy(
                      paymentsIntoOverseasPensions = data.pensions.paymentsIntoOverseasPensions.copy(
                        reliefs = data.pensions.paymentsIntoOverseasPensions.reliefs.updated(
                          0, relief.copy(qualifyingOverseasPensionSchemeReferenceNumber = Some(referenceNumber))
                        )))
                      pensionSessionService.createOrUpdateSessionData(request.user,
                        updatedCyaModel, taxYear, data.isPriorSubmission)(errorHandler.internalServerError()) {
                        Redirect(QOPSReferenceController.show(taxYear)) //TODO - redirect to pensions-overseas-details-summary
                      }
                    case _ => Future.successful(Redirect(PensionsCustomerReferenceNumberController.show(taxYear)))
                  }

                case _ =>
                  Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
              }
            }
          )
        }

        def removePrefix(qopsReference: String): String = if (qopsReference.length == 10) qopsReference.substring(4, 10) else qopsReference
}
