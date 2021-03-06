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

package controllers.pensions.annualAllowance

import config.{AppConfig, ErrorHandler}
import controllers.predicates.AuthorisedAction
import forms.PensionSchemeTaxReferenceForm
import models.mongo.PensionsCYAModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import views.html.pensions.annualAllowance.PensionSchemeTaxReferenceView
import controllers.pensions.routes.PensionsSummaryController
import controllers.pensions.annualAllowance.routes.PstrSummaryController
import controllers.predicates.TaxYearAction.taxYearAction
import models.User

import javax.inject.Inject
import scala.concurrent.Future

class PensionSchemeTaxReferenceController @Inject()(implicit val cc: MessagesControllerComponents,
                                                    authAction: AuthorisedAction,
                                                    pensionSchemeTaxReferenceView: PensionSchemeTaxReferenceView,
                                                    appConfig: AppConfig,
                                                    pensionSessionService: PensionSessionService,
                                                    errorHandler: ErrorHandler,
                                                    clock: Clock) extends FrontendController(cc) with I18nSupport {

  private def prefillValue(pstrListOpt: Option[Seq[String]], pensionSchemeTaxReference: Option[Int], user: User): Form[String] = {
    val emptyForm: Form[String] = PensionSchemeTaxReferenceForm.pensionSchemeTaxReferenceForm(user.isAgent)
    (pstrListOpt, pensionSchemeTaxReference) match {
      case (Some(pstrList), Some(pstrIndex)) => emptyForm.fill(pstrList(pstrIndex))
      case (_, _) => emptyForm
    }
  }

  def show(taxYear: Int, pensionSchemeTaxReferenceIndex: Option[Int]): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async {
    implicit request =>
      pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
        case Some(data) =>
          val pstrList: Option[Seq[String]] = data.pensions.pensionsAnnualAllowances.pensionSchemeTaxReference

          if (validatePstr(pensionSchemeTaxReferenceIndex, pstrList.getOrElse(Seq.empty))) {
            val form = prefillValue(
              pstrList,
              pensionSchemeTaxReferenceIndex,
              request.user
            )
            Future.successful(Ok(pensionSchemeTaxReferenceView(form, taxYear, pensionSchemeTaxReferenceIndex)))
          } else {
            Future.successful(Redirect(PstrSummaryController.show(taxYear)))
          }
        case _ =>
          //TODO: redirect to the annual allowances CYA page
          Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
      }
  }

  def submit(taxYear: Int, pensionSchemeTaxReferenceIndex: Option[Int]): Action[AnyContent] = authAction.async {
    implicit request =>
      PensionSchemeTaxReferenceForm.pensionSchemeTaxReferenceForm(request.user.isAgent).bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(pensionSchemeTaxReferenceView(formWithErrors, taxYear, pensionSchemeTaxReferenceIndex))),
        pensionScheme => {
          pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) {
            case Some(data) => {

              val pstrList: Option[Seq[String]] = data.pensions.pensionsAnnualAllowances.pensionSchemeTaxReference

              if (validatePstr(pensionSchemeTaxReferenceIndex, pstrList.getOrElse(Seq.empty))) {
                val pensionsCYAModel: PensionsCYAModel = data.pensions
                val viewModel = pensionsCYAModel.pensionsAnnualAllowances
                val newPensionSchemeTaxRef = Seq(pensionScheme)

                val updatedPstrList: Seq[String] = (viewModel.pensionSchemeTaxReference, pensionSchemeTaxReferenceIndex) match {
                  case (Some(pstrList), Some(pstrIndex)) =>
                    pstrList.updated(pstrIndex, newPensionSchemeTaxRef.head)
                  case (Some(pstrList), None) =>
                    pstrList ++ newPensionSchemeTaxRef
                  case (None, None) =>
                    newPensionSchemeTaxRef
                }

                val updatedCyaModel = pensionsCYAModel.copy(
                  pensionsAnnualAllowances = viewModel.copy(
                    pensionSchemeTaxReference = Some(updatedPstrList)
                  ))

                pensionSessionService.createOrUpdateSessionData(request.user,
                  updatedCyaModel, taxYear, data.isPriorSubmission)(errorHandler.internalServerError()) {
                  Redirect(PstrSummaryController.show(taxYear))
                }
              } else {
                Future.successful(Redirect(PstrSummaryController.show(taxYear)))
              }
            }
            //TODO: redirect to the annual allowances CYA page
            case _ => Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
          }
        }
      )
  }

  private def validatePstr(pstrIndex: Option[Int], pstrList: Seq[String]): Boolean = {
    pstrIndex match {
      case Some(index) =>
        pstrList.size > index
      case _ =>
        true
    }
  }
}
