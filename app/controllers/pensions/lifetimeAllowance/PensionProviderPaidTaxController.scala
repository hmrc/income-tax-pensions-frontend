/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers.pensions.lifetimeAllowance

import config.{AppConfig, ErrorHandler}
import controllers.pensions.lifetimeAllowance.routes.PensionProviderPaidTaxController
import controllers.pensions.routes.PensionsSummaryController
import controllers.predicates.AuthorisedAction
import controllers.predicates.TaxYearAction.taxYearAction
import forms.RadioButtonAmountForm
import models.User
import models.mongo.PensionsCYAModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import views.html.pensions.lifetimeAllowance.PensionProviderPaidTaxView

import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class PensionProviderPaidTaxController @Inject()(implicit val cc: MessagesControllerComponents,
                                                 authAction: AuthorisedAction,
                                                 pensionProviderPaidTaxView: PensionProviderPaidTaxView,
                                                 appConfig: AppConfig,
                                                 pensionSessionService: PensionSessionService,
                                                 errorHandler: ErrorHandler,
                                                 clock: Clock) extends FrontendController(cc) with I18nSupport {

  def providerPaidTaxForm(implicit user: User): Form[(Boolean, Option[BigDecimal])] = RadioButtonAmountForm.radioButtonAndAmountForm(
    missingInputError = s"${if (user.isAgent) "pensions.pensionProviderPaidTax.error.noEntry.agent"
    else "common.pensions.selectYesifYourPensionProvider.noEntry"}",
    emptyFieldKey = s"pensions.pensionsProviderPaidTax.error.noAmount.${if (user.isAgent) "agent" else "individual"}",
    wrongFormatKey = s"pensions.pensionProviderPaidTax.error.incorrectFormat.${if (user.isAgent) "agent" else "individual"}",
    exceedsMaxAmountKey = s"common.pensions.error.amountMaxLimit.${if (user.isAgent) "agent" else "individual"}"
  )

  def show(taxYear: Int): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async { implicit request =>
    pensionSessionService.getPensionSessionData(taxYear, request.user).flatMap {
      case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
      case Right(Some(data)) =>
        implicit val user: User = request.user
        val amount = data.pensions.pensionsAnnualAllowances.taxPaidByPensionProvider
        data.pensions.pensionsAnnualAllowances.pensionProvidePaidAnnualAllowanceQuestion
          .map({
            case true =>
              Future.successful(Ok(pensionProviderPaidTaxView(providerPaidTaxForm.fill((true, amount)), taxYear)))
            case false =>
              Future.successful(Ok(pensionProviderPaidTaxView(providerPaidTaxForm.fill((false, None)), taxYear)))
          })
          .getOrElse(Future.successful(Ok(pensionProviderPaidTaxView(providerPaidTaxForm, taxYear))))

      case _ =>
        //TODO" navigate to CYA controller
        Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
    }
  }


  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    def createOrUpdateSessionDataWithRedirect(updatedCyaModel: PensionsCYAModel,
                                              isPriorSubmission: Boolean,
                                              page: Call)
                                             (implicit request: Request[_], user: User) = {

      pensionSessionService.createOrUpdateSessionData(user,
        updatedCyaModel, taxYear, isPriorSubmission)(errorHandler.internalServerError()) {
        Redirect(page)
      }
    }

    def updateCyaModel(pensionsCyaModel: PensionsCYAModel, yesNo: Boolean, amount: Option[BigDecimal]): PensionsCYAModel = {
      val viewModel = pensionsCyaModel.pensionsAnnualAllowances
      pensionsCyaModel.copy(pensionsAnnualAllowances = viewModel.copy(
        pensionProvidePaidAnnualAllowanceQuestion = Some(yesNo),
        taxPaidByPensionProvider = amount
      ))
    }

    implicit val user: User = request.user

    providerPaidTaxForm.bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(pensionProviderPaidTaxView(formWithErrors, taxYear))),
      yesNoAmount => {
        pensionSessionService.getPensionSessionData(taxYear, request.user).flatMap {
          case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
          case Right(Some(userData)) =>

            yesNoAmount match {
              case (true, amount) =>
                createOrUpdateSessionDataWithRedirect(
                  updateCyaModel(userData.pensions, yesNo = true, amount),
                  userData.isPriorSubmission,
                  PensionProviderPaidTaxController.show(taxYear))

              case (false, _) =>
                createOrUpdateSessionDataWithRedirect(
                  updateCyaModel(userData.pensions, yesNo = false, None),
                  userData.isPriorSubmission,
                  PensionProviderPaidTaxController.show(taxYear))
            }


          case _ =>
            //TODO" navigate to CYA controller
            Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
        }
      })
  }
}
