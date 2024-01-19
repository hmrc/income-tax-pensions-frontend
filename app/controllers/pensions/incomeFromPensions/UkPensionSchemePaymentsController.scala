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

package controllers.pensions.incomeFromPensions

import config.{AppConfig, ErrorHandler}
import controllers.pensions.incomeFromPensions.routes._
import controllers.predicates.actions.TaxYearAction.taxYearAction
import controllers.predicates.actions.AuthorisedAction
import forms.YesNoForm
import models.User
import models.mongo.{DatabaseError, PensionsCYAModel, PensionsUserData}
import models.pension.statebenefits.IncomeFromPensionsViewModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import services.redirects.IncomeFromOtherUkPensionsPages.DoYouGetUkPensionSchemePaymentsPage
import services.redirects.IncomeFromOtherUkPensionsRedirects.{cyaPageCall, journeyCheck, redirectForSchemeLoop}
import services.redirects.SimpleRedirectService.redirectBasedOnCurrentAnswers
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import views.html.pensions.incomeFromPensions.UkPensionSchemePaymentsView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UkPensionSchemePaymentsController @Inject()(implicit val mcc: MessagesControllerComponents,
                                                  appConfig: AppConfig,
                                                  authAction: AuthorisedAction,
                                                  pensionSessionService: PensionSessionService,
                                                  errorHandler: ErrorHandler,
                                                  view: UkPensionSchemePaymentsView,
                                                  clock: Clock, ec: ExecutionContext) extends FrontendController(mcc) with I18nSupport {

  private def yesNoForm(user: User): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"pensions.ukPensionSchemePayments.error.noEntry.${if (user.isAgent) "agent" else "individual"}"
  )

  def show(taxYear: Int): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async { implicit request =>
    pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
      case Some(data) =>
        cleanUpSchemes(data).flatMap({
          case Right(updatedUserData) =>
            val checkRedirect = journeyCheck(DoYouGetUkPensionSchemePaymentsPage, _: PensionsCYAModel, taxYear)
            redirectBasedOnCurrentAnswers(taxYear, Some(updatedUserData), cyaPageCall(taxYear))(checkRedirect) {
              data =>
                data.pensions.incomeFromPensions.uKPensionIncomesQuestion match {
                  case Some(value) => Future.successful(Ok(view(yesNoForm(request.user).fill(value), taxYear)))
                  case _ => Future.successful(Ok(view(yesNoForm(request.user), taxYear)))
                }
            }
        })

      case _ => Future.successful(Ok(view(yesNoForm(request.user), taxYear)))
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    yesNoForm(request.user).bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear))),
      yesNo => {
        pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
          optData =>
            val checkRedirect = journeyCheck(DoYouGetUkPensionSchemePaymentsPage, _: PensionsCYAModel, taxYear)
            redirectBasedOnCurrentAnswers(taxYear, optData, cyaPageCall(taxYear))(checkRedirect) { _ =>
              val pensionsCYAModel: PensionsCYAModel = optData.map(_.pensions).getOrElse(PensionsCYAModel.emptyModels)
              val viewModel: IncomeFromPensionsViewModel = pensionsCYAModel.incomeFromPensions
              val updatedCyaModel: PensionsCYAModel = {
                pensionsCYAModel.copy(
                  incomeFromPensions = viewModel.copy(uKPensionIncomesQuestion = Some(yesNo),
                    uKPensionIncomes = if (yesNo) viewModel.uKPensionIncomes else Seq.empty))
              }
              pensionSessionService.createOrUpdateSessionData(request.user,
                updatedCyaModel, taxYear, optData.exists(_.isPriorSubmission))(errorHandler.internalServerError()) {
                Redirect(
                  if (yesNo) redirectForSchemeLoop(schemes = updatedCyaModel.incomeFromPensions.uKPensionIncomes, taxYear)
                  else UkPensionIncomeCYAController.show(taxYear)
                )
              }
            }
        }
      }
    )
  }

  private def cleanUpSchemes(pensionsUserData: PensionsUserData)(implicit ec: ExecutionContext): Future[Either[DatabaseError, PensionsUserData]] = {
    val schemes = pensionsUserData.pensions.incomeFromPensions.uKPensionIncomes
    val filteredSchemes = if (schemes.nonEmpty) schemes.filter(scheme => scheme.isFinished) else schemes
    val updatedViewModel = pensionsUserData.pensions.incomeFromPensions.copy(uKPensionIncomes = filteredSchemes)
    val updatedPensionData = pensionsUserData.pensions.copy(incomeFromPensions = updatedViewModel)
    val updatedUserData = pensionsUserData.copy(pensions = updatedPensionData)
    pensionSessionService.createOrUpdateSessionData(updatedUserData).map(_.map(_ => updatedUserData))
  }
}
