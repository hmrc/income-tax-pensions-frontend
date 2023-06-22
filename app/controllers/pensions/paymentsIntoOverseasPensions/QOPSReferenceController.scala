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
import controllers._
import controllers.pensions.paymentsIntoOverseasPensions.routes._
import controllers.predicates.ActionsProvider
import forms.QOPSReferenceNumberForm
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.pension.charges.Relief
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import services.PensionSessionService
import services.redirects.SimpleRedirectService.checkForExistingSchemes
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

  def referenceForm(): Form[String] = QOPSReferenceNumberForm.qopsReferenceNumberForm(
    incorrectFormatMsg = "common.overseasPensions.qops.error.incorrectFormat"
  )

  def show(taxYear: Int, index: Option[Int]): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async {
    implicit sessionData =>
      val reliefs = sessionData.pensionsUserData.pensions.paymentsIntoOverseasPensions.reliefs
      validatedIndex(index, reliefs.size) match {
        case Some(idx) => Future.successful(Ok(qopsReferenceView(referenceForm(sessionData.pensionsUserData, idx), taxYear, Some(idx))))
        case _ => Future.successful(Redirect(redirectOnBadIndex(reliefs, taxYear)))
      }
  }

  private def referenceForm(pensionUserData: PensionsUserData, index: Int): Form[String] = {
    val qopsNumber = pensionUserData.pensions.paymentsIntoOverseasPensions.reliefs(index).qopsReference
    qopsNumber match {
      case Some(qopsNumber) => referenceForm().fill(removePrefix(qopsNumber))
      case None => referenceForm()
    }
  }

  def submit(taxYear: Int, index: Option[Int]): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async {
    implicit request =>
      val piops = request.pensionsUserData.pensions.paymentsIntoOverseasPensions
      val reliefSize = piops.reliefs.size

      validatedIndex(index, reliefSize) match {
        case Some(idx) =>
          referenceForm().bindFromRequest().fold(
            formWithErrors => Future.successful(BadRequest(qopsReferenceView(formWithErrors, taxYear, index))),
            referenceNumber => {
              val relief = piops.reliefs(idx)
              val updatedCyaModel: PensionsCYAModel = request.pensionsUserData.pensions.copy(
                paymentsIntoOverseasPensions = piops.copy(
                  reliefs = piops.reliefs.updated(idx, relief.copy(qopsReference = Some(referenceNumber))))
              )
              pensionSessionService.createOrUpdateSessionData(request.user,
                updatedCyaModel, taxYear, request.pensionsUserData.isPriorSubmission)(errorHandler.internalServerError()) {
                Redirect(ReliefsSchemeDetailsController.show(taxYear, index))
              }
            }
          )
        case _ => Future.successful(Redirect(redirectOnBadIndex(piops.reliefs, taxYear)))
      }
  }

  private def removePrefix(qopsReference: String): String = //TODO: check qops ref length
    if (qopsReference.length == 10) qopsReference.substring(4, 10) else qopsReference

  private def redirectOnBadIndex(reliefs: Seq[Relief], taxYear: Int): Call = checkForExistingSchemes(
    nextPage = PensionsCustomerReferenceNumberController.show(taxYear, None),
    summaryPage = ReliefsSchemeSummaryController.show(taxYear),
    schemes = reliefs
  )
}