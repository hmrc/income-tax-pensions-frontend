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

package controllers.pensions.shortServiceRefunds

import config.{AppConfig, ErrorHandler}
import controllers.pensions.routes.{OverseasPensionsSummaryController, PensionsSummaryController}
import controllers.pensions.shortServiceRefunds.routes.TaxableRefundAmountController
import controllers.predicates.actions.{ActionsProvider, AuthorisedAction}
import models.mongo.PensionsCYAModel
import models.pension.AllPensionsData
import models.pension.AllPensionsData.generateCyaFromPrior
import models.pension.charges.ShortServiceRefundsViewModel
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.redirects.ShortServiceRefundsPages.CYAPage
import services.redirects.ShortServiceRefundsRedirects.journeyCheck
import services.redirects.SimpleRedirectService.redirectBasedOnCurrentAnswers
import services.{PensionChargesService, PensionSessionService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.pensions.shortServiceRefunds.ShortServiceRefundsCYAView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ShortServiceRefundsCYAController @Inject()(authAction: AuthorisedAction,
                                                 view: ShortServiceRefundsCYAView,
                                                 pensionSessionService: PensionSessionService,
                                                 pensionChargesService: PensionChargesService,
                                                 errorHandler: ErrorHandler,
                                                 actionsProvider: ActionsProvider)
                                                (implicit val mcc: MessagesControllerComponents, appConfig: AppConfig,
                                                 clock: Clock, ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport with SessionHelper {


  def show(taxYear: Int): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async { implicit request =>
    pensionSessionService.getAndHandle(taxYear, request.user) { (cya, prior) =>
      (cya, prior) match {
        case (Some(data), _) =>
          val checkRedirect = journeyCheck(CYAPage, _: PensionsCYAModel, taxYear)
          redirectBasedOnCurrentAnswers(taxYear, cya, TaxableRefundAmountController.show(taxYear))(checkRedirect) { _ =>
            Future.successful(Ok(view(taxYear, data.pensions.shortServiceRefunds)))
          }
        case (None, Some(priorData)) =>
          val cyaModel = generateCyaFromPrior(priorData)
          pensionSessionService.createOrUpdateSessionData(request.user,
            cyaModel, taxYear, isPriorSubmission = true)(
            errorHandler.internalServerError())(
            Ok(view(taxYear, cyaModel.shortServiceRefunds)))
        case (None, None) =>
          val emptyShortServiceRefunds = ShortServiceRefundsViewModel()
          Future.successful(Ok(view(taxYear, emptyShortServiceRefunds)))
        case _ => Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
      }
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    pensionSessionService.getAndHandle(taxYear, request.user) { (cya, prior) =>
      cya.fold(
        Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
      ) { model =>
        val checkRedirect = journeyCheck(CYAPage, _: PensionsCYAModel, taxYear)
        redirectBasedOnCurrentAnswers(taxYear, cya, TaxableRefundAmountController.show(taxYear))(checkRedirect) { _ =>
          if (sessionDataDifferentThanPriorData(model.pensions, prior)) {
            pensionChargesService.saveShortServiceRefundsViewModel(request.user, taxYear).map {
              case Left(_) => errorHandler.internalServerError()
              case Right(_) => Redirect(OverseasPensionsSummaryController.show(taxYear))
            }
          } else {
            Future.successful(Redirect(OverseasPensionsSummaryController.show(taxYear)))
          }
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
