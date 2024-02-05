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

package controllers.pensions.incomeFromOverseasPensions

import config.{AppConfig, ErrorHandler}
import controllers.pensions.incomeFromOverseasPensions.routes._
import controllers.pensions.routes.OverseasPensionsSummaryController
import controllers.predicates.actions.AuthorisedAction
import forms.YesNoForm
import models.User
import models.mongo.{DatabaseError, PensionsCYAModel, PensionsUserData}
import models.pension.charges.{IncomeFromOverseasPensionsViewModel, PensionScheme}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import services.redirects.IncomeFromOverseasPensionsRedirects.redirectForSchemeLoop
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import views.html.pensions.incomeFromOverseasPensions.IncomeFromOverseasPensionsView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PensionOverseasIncomeStatus @Inject() (
    authAction: AuthorisedAction,
    incomeFromOverseasPensionsView: IncomeFromOverseasPensionsView,
    pensionSessionService: PensionSessionService,
    errorHandler: ErrorHandler)(implicit val mcc: MessagesControllerComponents, appConfig: AppConfig, clock: Clock, ec: ExecutionContext)
    extends FrontendController(mcc)
    with I18nSupport {

  def yesNoForm(user: User): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"incomeFromOverseasPensions.incomeFromOverseasPension.error.noEntry.${if (user.isAgent) "agent" else "individual"}"
  )

  def show(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    pensionSessionService
      .getPensionSessionData(taxYear, request.user)
      .flatMap {
        case Left(_) => Future.successful(errorHandler.internalServerError())
        case Right(maybeData) =>
          maybeData match {
            case Some(data) =>
              cleanUpSchemes(data).map {
                case Right(_) =>
                  val form =
                    data.pensions.incomeFromOverseasPensions.paymentsFromOverseasPensionsQuestion
                      .fold(yesNoForm(request.user))(yesNoForm(request.user).fill(_))
                  Ok(incomeFromOverseasPensionsView(form, taxYear))

                case Left(_) => errorHandler.internalServerError()
              }

            case None =>
              // TODO - redirect to CYA page once implemented <- I think to this page with empty form
              Future.successful(Ok(incomeFromOverseasPensionsView(yesNoForm(request.user), taxYear)))
          }
      }
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    yesNoForm(request.user)
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(incomeFromOverseasPensionsView(formWithErrors, taxYear))),
        yesNo =>
          pensionSessionService.getPensionSessionData(taxYear, request.user).flatMap {
            case Right(optPensionsUserData) =>
              val updatedCyaModel = optPensionsUserData match {

                case Some(data) =>
                  data.pensions.copy(incomeFromOverseasPensions = if (yesNo) {
                    data.pensions.incomeFromOverseasPensions.copy(paymentsFromOverseasPensionsQuestion = Some(yesNo))
                  } else {
                    IncomeFromOverseasPensionsViewModel(paymentsFromOverseasPensionsQuestion = Some(yesNo))
                  })

                case None =>
                  PensionsCYAModel.emptyModels.copy(incomeFromOverseasPensions =
                    IncomeFromOverseasPensionsViewModel(paymentsFromOverseasPensionsQuestion = Some(yesNo)))
              }

              val isPriorSubmission = optPensionsUserData.fold(false)(_.isPriorSubmission)

              pensionSessionService.createOrUpdateSessionData(request.user, updatedCyaModel, taxYear, isPriorSubmission)(
                errorHandler.internalServerError()) {
                Redirect(
                  if (yesNo) {
                    redirectForSchemeLoop(schemes = updatedCyaModel.incomeFromOverseasPensions.overseasIncomePensionSchemes, taxYear)
                  } else {
                    IncomeFromOverseasPensionsCYAController.show(taxYear)
                  }
                )
              }
            case _ =>
              Future.successful(Redirect(OverseasPensionsSummaryController.show(taxYear)))
          }
      )
  }

  private def cleanUpSchemes(pensionsUserData: PensionsUserData): Future[Either[DatabaseError, Seq[PensionScheme]]] = {
    val schemes            = pensionsUserData.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes
    val filteredSchemes    = if (schemes.nonEmpty) schemes.filter(scheme => scheme.isFinished) else schemes
    val updatedViewModel   = pensionsUserData.pensions.incomeFromOverseasPensions.copy(overseasIncomePensionSchemes = filteredSchemes)
    val updatedPensionData = pensionsUserData.pensions.copy(incomeFromOverseasPensions = updatedViewModel)
    val updatedUserData    = pensionsUserData.copy(pensions = updatedPensionData)
    pensionSessionService.createOrUpdateSessionData(updatedUserData).map(_.map(_ => filteredSchemes))
  }
}
