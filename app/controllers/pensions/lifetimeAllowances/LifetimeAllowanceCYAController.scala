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

package controllers.pensions.lifetimeAllowances

import config.{AppConfig, ErrorHandler}
import controllers.predicates.actions.ActionsProvider
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.pension.AllPensionsData
import models.pension.AllPensionsData.generateCyaFromPrior
import models.pension.charges.PensionLifetimeAllowancesViewModel
import models.requests.UserSessionDataRequest
import models.{APIErrorBodyModel, APIErrorModel, User}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.redirects.LifetimeAllowancesPages.CYAPage
import services.redirects.LifetimeAllowancesRedirects.{cyaPageCall, journeyCheck}
import services.redirects.SimpleRedirectService.redirectBasedOnCurrentAnswers
import services.{PensionChargesService, PensionSessionService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import views.html.pensions.lifetimeAllowances.LifetimeAllowanceCYAView
import controllers.pensions.routes.PensionsSummaryController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class LifetimeAllowanceCYAController @Inject()(actionsProvider: ActionsProvider,
                                               view: LifetimeAllowanceCYAView,
                                               pensionSessionService: PensionSessionService,
                                               pensionChargesService: PensionChargesService,
                                               errorHandler: ErrorHandler)
                                              (implicit val mcc: MessagesControllerComponents,
                                               appConfig: AppConfig, clock: Clock, ec: ExecutionContext) extends FrontendController(mcc) with I18nSupport {

  lazy val logger: Logger = Logger(this.getClass.getName)


  def show(taxYear: Int): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async { implicit request =>
    pensionSessionService.getAndHandle(taxYear, request.user) { (cya, prior) =>
      (cya, prior) match {
        case (Some(data), _) =>
          val checkRedirect = journeyCheck(CYAPage, _: PensionsCYAModel, taxYear)
          redirectBasedOnCurrentAnswers(taxYear, Some(data), cyaPageCall(taxYear))(checkRedirect) {
            data =>
              Future.successful(Ok(view(taxYear, data.pensions.pensionLifetimeAllowances)))
          }

        case (None, Some(priorData)) =>
          val cyaModel = generateCyaFromPrior(priorData)
          pensionSessionService.createOrUpdateSessionData(request.user, cyaModel, taxYear, isPriorSubmission = true)(
            errorHandler.internalServerError())(
            Ok(view(taxYear, cyaModel.pensionLifetimeAllowances)))

        case (None, None) =>
          val emptyLifetimeAllowances = PensionLifetimeAllowancesViewModel()
          Future.successful(Ok(view(taxYear, emptyLifetimeAllowances)))

        case _ =>
          Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
      }
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async { implicit request =>
    pensionSessionService.getAndHandle(taxYear, request.user) { (cya, prior) =>
      cya.fold(Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))) {
        model =>
          if (sessionDataDifferentThanPriorData(model.pensions, prior)) {
            val checkRedirect = journeyCheck(CYAPage, _: PensionsCYAModel, taxYear)
            redirectBasedOnCurrentAnswers(taxYear, Some(model), cyaPageCall(taxYear))(checkRedirect) {
              _ => performSubmission(taxYear, cya)(request.user, request, hc, clock)
            }
          } else {
            Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
          }
      }
    }
  }

  private def performSubmission(taxYear: Int, cya: Option[PensionsUserData])
                               (implicit user: User,
                                request: UserSessionDataRequest[AnyContent],
                                hc: HeaderCarrier,
                                clock: Clock
                               ): Future[Result] = {
    (cya match {
      case Some(_) =>
        pensionChargesService.saveLifetimeAllowancesViewModel(user, taxYear) map {
          case Left(_) =>
            logger.info("[submit] Failed to create or update session")
            Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel(BAD_REQUEST.toString, "Unable to createOrUpdate pension service")))
          case Right(_) =>
            Right(Ok)
        }
      case _ =>
        logger.info("[submit] CYA data or NINO missing from session.")
        Future.successful(Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel("MISSING_DATA", "CYA data or NINO missing from session."))))
    }).flatMap {
      case Right(_) =>
        //TODO: investigate  the use of the previously used pensionSessionService.clear
        Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
      case Left(error) => Future.successful(errorHandler.handleError(error.status))
    }
  }

  private def sessionDataDifferentThanPriorData(cyaData: PensionsCYAModel, priorData: Option[AllPensionsData]): Boolean = {
    priorData match {
      case None => true
      case Some(prior) => !cyaData.equals(generateCyaFromPrior(prior))
    }
  }

}
