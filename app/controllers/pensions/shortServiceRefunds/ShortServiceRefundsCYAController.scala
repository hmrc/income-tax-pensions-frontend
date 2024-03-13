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

import cats.data.EitherT
import common.TaxYear
import config.{AppConfig, ErrorHandler}
import controllers.predicates.auditActions.AuditActionsProvider
import models.IncomeTaxUserData
import models.mongo.{PensionsCYAModel, PensionsUserData, ServiceError}
import models.pension.AllPensionsData
import models.pension.AllPensionsData.generateSessionModelFromPrior
import models.requests.UserRequestWithSessionAndPrior
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.redirects.ShortServiceRefundsPages.CYAPage
import services.redirects.ShortServiceRefundsRedirects.taskListRedirect
import services.{PensionSessionService, ShortServiceRefundsService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import validation.pensions.shortServiceRefunds.ShortServiceRefundsValidator.validateFlow
import views.html.pensions.shortServiceRefunds.ShortServiceRefundsCYAView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ShortServiceRefundsCYAController @Inject() (auditProvider: AuditActionsProvider,
                                                  view: ShortServiceRefundsCYAView,
                                                  sessionService: PensionSessionService,
                                                  service: ShortServiceRefundsService,
                                                  errorHandler: ErrorHandler,
                                                  mcc: MessagesControllerComponents)(implicit appConfig: AppConfig, ec: ExecutionContext)
    extends FrontendController(mcc)
    with I18nSupport
    with SessionHelper {

  def show(taxYear: Int): Action[AnyContent] = auditProvider.shortServiceRefundsViewAuditing(taxYear) async { implicit request =>
    val answers = request.sessionData.pensions.shortServiceRefunds

    validateFlow(answers, CYAPage, taxYear) {
      Future.successful(Ok(view(taxYear, answers)))
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = auditProvider.shortServiceRefundsUpdateAuditing(taxYear) async { implicit request =>
    val answers = request.sessionData.pensions.shortServiceRefunds

    validateFlow(answers, CYAPage, taxYear) {
      val resultOrError: EitherT[Future, ServiceError, Result] =
        for {
          data <- sessionService.loadPriorAndSession(request.user, TaxYear(taxYear))
          (prior, session) = data
          _ <- processSubmission(session, prior, taxYear)
        } yield taskListRedirect(taxYear)

      resultOrError
        .leftMap(_ => errorHandler.internalServerError())
        .merge
    }
  }

  private def processSubmission(session: PensionsUserData, prior: IncomeTaxUserData, taxYear: Int)(implicit
      request: UserRequestWithSessionAndPrior[AnyContent]): EitherT[Future, ServiceError, Unit] =
    if (sessionDeviatesFromPrior(session.pensions, prior.pensions))
      EitherT(service.saveAnswers(request.user, TaxYear(taxYear)))
    else EitherT.pure[Future, ServiceError](())

  private def sessionDeviatesFromPrior(session: PensionsCYAModel, maybePrior: Option[AllPensionsData]): Boolean =
    maybePrior.fold(ifEmpty = true)(prior => !session.equals(generateSessionModelFromPrior(prior)))
}
