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
import forms.QOPSReferenceNumberForm
import models.mongo.PensionsCYAModel
import models.pension.charges.OverseasPensionScheme
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import services.redirects.PaymentsIntoOverseasPensionsPages.QOPSReferencePage
import services.redirects.PaymentsIntoOverseasPensionsRedirects.indexCheckThenJourneyCheck
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.pensions.paymentsIntoOverseasPensions.QOPSReferenceView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class QOPSReferenceController @Inject() (actionsProvider: ActionsProvider,
                                         view: QOPSReferenceView,
                                         pensionSessionService: PensionSessionService,
                                         errorHandler: ErrorHandler,
                                         mcc: MessagesControllerComponents)(implicit val appConfig: AppConfig)
    extends FrontendController(mcc)
    with I18nSupport
    with SessionHelper {

  def referenceForm: Form[String] = QOPSReferenceNumberForm.qopsReferenceNumberForm(
    incorrectFormatMsg = "common.overseasPensions.qops.error.incorrectFormat"
  )

  def show(taxYear: Int, index: Option[Int]): Action[AnyContent] = actionsProvider.authoriseWithSession(taxYear) async { implicit request =>
    indexCheckThenJourneyCheck(request.sessionData, index, QOPSReferencePage, taxYear) { relief: OverseasPensionScheme =>
      relief.qopsReference match {
        case Some(qopsNumber) => Future.successful(Ok(view(referenceForm.fill(removePrefix(qopsNumber)), taxYear, index)))
        case None             => Future.successful(Ok(view(referenceForm, taxYear, index)))
      }
    }
  }

  def submit(taxYear: Int, index: Option[Int]): Action[AnyContent] = actionsProvider.authoriseWithSession(taxYear) async { implicit request =>
    val piops = request.sessionData.pensions.paymentsIntoOverseasPensions

    indexCheckThenJourneyCheck(request.sessionData, index, QOPSReferencePage, taxYear) { relief: OverseasPensionScheme =>
      referenceForm
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear, index))),
          referenceNumber => {
            val maybeRef = if (referenceNumber.isEmpty) None else Some(referenceNumber)

            val updatedCyaModel: PensionsCYAModel = request.sessionData.pensions.copy(
              paymentsIntoOverseasPensions = piops.copy(schemes = piops.schemes.updated(index.get, relief.copy(qopsReference = maybeRef)))
            )
            pensionSessionService.createOrUpdateSessionData(request.user, updatedCyaModel, taxYear, request.sessionData.isPriorSubmission)(
              errorHandler.internalServerError()) {
              Redirect(ReliefsSchemeDetailsController.show(taxYear, index))
            }
          }
        )
    }
  }

  private def removePrefix(qopsReference: String): String = // TODO: check qops ref length
    if (qopsReference.length == 10) qopsReference.substring(4, 10) else qopsReference

}
