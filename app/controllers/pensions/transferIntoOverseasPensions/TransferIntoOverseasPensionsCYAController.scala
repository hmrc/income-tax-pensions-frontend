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

package controllers.pensions.transferIntoOverseasPensions

import config.{AppConfig, ErrorHandler}
import controllers.pensions.routes.OverseasPensionsSummaryController
import controllers.predicates.auditActions.AuditActionsProvider
import models.mongo.PensionsCYAModel
import models.pension.AllPensionsData
import models.pension.AllPensionsData.generateCyaFromPrior
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.redirects.SimpleRedirectService.redirectBasedOnCurrentAnswers
import services.redirects.TransfersIntoOverseasPensionsPages.TransferIntoOverseasPensionsCYA
import services.redirects.TransfersIntoOverseasPensionsRedirects.{cyaPageCall, journeyCheck}
import services.{PensionChargesService, PensionSessionService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.pensions.transferIntoOverseasPensions.TransferIntoOverseasPensionsCYAView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TransferIntoOverseasPensionsCYAController @Inject()(auditProvider: AuditActionsProvider,
                                                          view: TransferIntoOverseasPensionsCYAView,
                                                          pensionSessionService: PensionSessionService,
                                                          pensionChargesService: PensionChargesService,
                                                          errorHandler: ErrorHandler)
                                                         (implicit val mcc: MessagesControllerComponents,
                                                          appConfig: AppConfig, clock: Clock, ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport with SessionHelper {


  def show(taxYear: Int): Action[AnyContent] = auditProvider.transfersIntoOverseasPensionsViewAuditing(taxYear) async { implicit sessionDataRequest =>
    val cyaData = sessionDataRequest.pensionsUserData
    if (!cyaData.pensions.transfersIntoOverseasPensions.isFinished) {
      val checkRedirect = journeyCheck(TransferIntoOverseasPensionsCYA, _: PensionsCYAModel, taxYear)
      redirectBasedOnCurrentAnswers(taxYear, Some(cyaData), cyaPageCall(taxYear))(checkRedirect) { data =>
          Future.successful(Ok(view(taxYear, data.pensions.transfersIntoOverseasPensions)))
      }
    } else {
      pensionSessionService.createOrUpdateSessionData(sessionDataRequest.user, cyaData.pensions, taxYear, isPriorSubmission = false)(
        errorHandler.internalServerError())(Ok(view(taxYear, cyaData.pensions.transfersIntoOverseasPensions)))
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = auditProvider.transfersIntoOverseasPensionsUpdateAuditing(taxYear) async {
    implicit request =>
      val checkRedirect = journeyCheck(TransferIntoOverseasPensionsCYA, _: PensionsCYAModel, taxYear)
      redirectBasedOnCurrentAnswers(taxYear, Some(request.pensionsUserData), cyaPageCall(taxYear))(checkRedirect) {
        sessionData =>
          if (sessionDataDifferentThanPriorData(sessionData.pensions, request.pensions)) {
            pensionChargesService.saveTransfersIntoOverseasPensionsViewModel(request.user, taxYear).map {
              case Left(_) => errorHandler.internalServerError()
              case Right(_) => Redirect(OverseasPensionsSummaryController.show(taxYear))
            }
          } else {
            Future.successful(Redirect(OverseasPensionsSummaryController.show(taxYear)))
          }
      }
  }


  private def sessionDataDifferentThanPriorData(cyaData: PensionsCYAModel, priorData: Option[AllPensionsData]): Boolean = {
    priorData match {
      case None => true
      case Some(prior) => !cyaData.equals(generateCyaFromPrior(prior))
    }
  }

}
