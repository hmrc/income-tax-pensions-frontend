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
import controllers.pensions.routes.OverseasPensionsSummaryController
import controllers.predicates.{ActionsProvider, AuthorisedAction}
import models.mongo.PensionsCYAModel
import models.pension.AllPensionsData
import models.pension.AllPensionsData.generateCyaFromPrior
import models.pension.charges.PaymentsIntoOverseasPensionsViewModel
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.redirects.PaymentsIntoOverseasPensionsPages.{PaymentsIntoOverseasPensionsCYAPage, TaxEmployerPaymentsPage}
import services.redirects.PaymentsIntoOverseasPensionsRedirects.{cyaPageCall, journeyCheck}
import services.redirects.SimpleRedirectService.redirectBasedOnCurrentAnswers
import services.{NrsService, PensionOverseasPaymentService, PensionSessionService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.pensions.paymentsIntoOverseasPensions.PaymentsIntoOverseasPensionsCYAView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PaymentsIntoOverseasPensionsCYAController @Inject()(authAction: AuthorisedAction,
                                                          view: PaymentsIntoOverseasPensionsCYAView,
                                                          pensionSessionService: PensionSessionService,
                                                          errorHandler: ErrorHandler,
                                                          pensionOverseasPaymentService: PensionOverseasPaymentService,
                                                          nrsService: NrsService,
                                                          actionsProvider: ActionsProvider)
                                                         (implicit val mcc: MessagesControllerComponents,
                                                          appConfig: AppConfig, clock: Clock, ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport with SessionHelper {


  def show(taxYear: Int): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async { implicit request =>

    def cyaDataIsEmpty(priorData: AllPensionsData): Future[Result] = {
      val cyaModel = generateCyaFromPrior(priorData)
      pensionSessionService.createOrUpdateSessionData(request.user,
        cyaModel, taxYear, isPriorSubmission = false)(
        errorHandler.internalServerError())(
        Ok(view(taxYear, cyaModel.paymentsIntoOverseasPensions)))
    }


    pensionSessionService.getAndHandle(taxYear, request.user) { (cya, prior) =>
      (cya, prior) match {
        case (Some(data), Some(priorData: AllPensionsData)) if data.pensions.paymentsIntoOverseasPensions.isEmpty =>
          cyaDataIsEmpty(priorData)
        case (Some(_), _) =>
          val checkRedirect = journeyCheck(PaymentsIntoOverseasPensionsCYAPage, _: PensionsCYAModel, taxYear)
          redirectBasedOnCurrentAnswers(taxYear, cya, cyaPageCall(taxYear))(checkRedirect) { data =>
            Future.successful(Ok(view(taxYear, data.pensions.paymentsIntoOverseasPensions)))
          }
        case (None, Some(priorData)) =>
          cyaDataIsEmpty(priorData)
        case (None, None) =>
          val emptyPaymentsIntoOverseasPensions = PaymentsIntoOverseasPensionsViewModel()
          Future.successful(Ok(view(taxYear, emptyPaymentsIntoOverseasPensions)))
        case _ => Future.successful(Redirect(controllers.pensions.routes.OverseasPensionsSummaryController.show(taxYear)))
      }
    }
  }


  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    pensionSessionService.getAndHandle(taxYear, request.user) { (cya, prior) =>
      cya.fold(
        Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
      ) { model =>
        if (sessionDataDifferentThanPriorData(model.pensions, prior)) {
          val pIOPCopy = model.pensions.paymentsIntoOverseasPensions.copy()

          pensionOverseasPaymentService.savePaymentsFromOverseasPensionsViewModel(request.user, taxYear).map {
            case Left(_) => errorHandler.internalServerError()
            case Right(_) =>
              nrsService.submit(request.user.nino, pIOPCopy, request.user.mtditid)
              Redirect(OverseasPensionsSummaryController.show(taxYear))
          }
        } else {
          Future.successful(Redirect(OverseasPensionsSummaryController.show(taxYear)))
        }
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
