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
import models.pension.charges.Relief
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import services.redirects.PaymentsIntoOverseasPensionsPages.QOPSReferencePage
import services.redirects.PaymentsIntoOverseasPensionsRedirects.indexCheckThenJourneyCheck
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.pensions.paymentsIntoOverseasPensions.QOPSReferenceView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class QOPSReferenceController @Inject()(actionsProvider: ActionsProvider,
                                        qopsReferenceView: QOPSReferenceView,
                                        pensionSessionService: PensionSessionService,
                                        errorHandler: ErrorHandler)
                                       (implicit val mcc: MessagesControllerComponents, appConfig: AppConfig, clock: Clock)
  extends FrontendController(mcc) with I18nSupport with SessionHelper {

  def referenceForm: Form[String] = QOPSReferenceNumberForm.qopsReferenceNumberForm(
    incorrectFormatMsg = "common.overseasPensions.qops.error.incorrectFormat"
  )

  def show(taxYear: Int, index: Option[Int]): Action[AnyContent] = actionsProvider.userSessionDataForInYear(taxYear) async {
    implicit sessionData =>
      indexCheckThenJourneyCheck(sessionData.pensionsUserData, index, QOPSReferencePage, taxYear) { relief: Relief =>
        relief.qopsReference match {
          case Some(qopsNumber) => Future.successful(Ok(qopsReferenceView(referenceForm.fill(removePrefix(qopsNumber)), taxYear, index)))
          case None => Future.successful(Ok(qopsReferenceView(referenceForm, taxYear, index)))
        }
      }
  }

  def submit(taxYear: Int, index: Option[Int]): Action[AnyContent] = actionsProvider.userSessionDataForInYear(taxYear) async {
    implicit request =>
      val piops = request.pensionsUserData.pensions.paymentsIntoOverseasPensions

      indexCheckThenJourneyCheck(request.pensionsUserData, index, QOPSReferencePage, taxYear) { relief: Relief =>
        referenceForm.bindFromRequest().fold(
          formWithErrors => Future.successful(BadRequest(qopsReferenceView(formWithErrors, taxYear, index))),
          referenceNumber => {
            val updatedCyaModel: PensionsCYAModel = request.pensionsUserData.pensions.copy(
              paymentsIntoOverseasPensions = piops.copy(
                reliefs = piops.reliefs.updated(index.get, relief.copy(qopsReference = Some(referenceNumber))))
            )
            pensionSessionService.createOrUpdateSessionData(request.user,
              updatedCyaModel, taxYear, request.pensionsUserData.isPriorSubmission)(errorHandler.internalServerError()) {
              Redirect(ReliefsSchemeDetailsController.show(taxYear, index))
            }
          }
        )
      }
  }

  private def removePrefix(qopsReference: String): String = //TODO: check qops ref length
    if (qopsReference.length == 10) qopsReference.substring(4, 10) else qopsReference

}
