/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.pensions.unauthorisedPayments

import config.{AppConfig, ErrorHandler}
import controllers.predicates.actions.AuthorisedAction
import controllers.predicates.actions.TaxYearAction.taxYearAction
import models.mongo.PensionsCYAModel
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import services.redirects.SimpleRedirectService.redirectBasedOnCurrentAnswers
import services.redirects.UnauthorisedPaymentsPages.PSTRSummaryPage
import services.redirects.UnauthorisedPaymentsRedirects.{cyaPageCall, journeyCheck}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.pensions.unauthorisedPayments.UkPensionSchemeDetails

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class UkPensionSchemeDetailsController @Inject()(implicit val cc: MessagesControllerComponents,
                                                 authAction: AuthorisedAction,
                                                 ukPensionSchemeDetails: UkPensionSchemeDetails,
                                                 appConfig: AppConfig,
                                                 pensionSessionService: PensionSessionService,
                                                 errorHandler: ErrorHandler)
  extends FrontendController(cc) with I18nSupport {

  def show(taxYear: Int): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async { implicit request =>
    pensionSessionService.getPensionSessionData(taxYear, request.user).flatMap {
      case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
      case Right(optData) =>
        val checkRedirect = journeyCheck(PSTRSummaryPage, _: PensionsCYAModel, taxYear)
        redirectBasedOnCurrentAnswers(taxYear, optData, cyaPageCall(taxYear))(checkRedirect) { data =>

          val pensionSchemeTaxReferenceList: Seq[String] = data.pensions.unauthorisedPayments.pensionSchemeTaxReference.getOrElse(Seq.empty)
          Future(Ok(ukPensionSchemeDetails(taxYear, pensionSchemeTaxReferenceList)))
        }
    }
  }
}
