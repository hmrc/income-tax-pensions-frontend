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
import controllers.predicates.ActionsProvider
import controllers.validateIndex
import forms.Countries
import forms.overseas.DoubleTaxationAgreementForm.{DoubleTaxationAgreementFormModel, doubleTaxationAgreementForm}
import models.User
import models.mongo.PensionsUserData
import models.pension.charges.Relief
import models.requests.UserSessionDataRequest
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.pensions.paymentsIntoOverseasPensions.DoubleTaxationAgreementView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DoubleTaxationAgreementController @Inject() (actionsProvider: ActionsProvider,
                                                   pensionSessionService: PensionSessionService,
                                                   view: DoubleTaxationAgreementView,
                                                   errorHandler: ErrorHandler)
                                                  (implicit val mcc: MessagesControllerComponents, appConfig: AppConfig, clock: Clock, ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport with SessionHelper {

  def show(taxYear: Int, index: Option[Int]): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async {
    implicit request =>
      pensionSessionService.getPensionSessionData(taxYear, request.user).map {
        case Left(_) => errorHandler.handleError(INTERNAL_SERVER_ERROR)
        case Right(optPensionUserData) =>
          val relief = request.pensionsUserData.pensions.paymentsIntoOverseasPensions.reliefs
          validateIndex(index, relief.size) match {
            case Some(idx) =>
              val form = dblTaxationAgreementForm(request.user, false).fill(updateViewModel(relief(idx)))
              Ok(view(form, taxYear, index))
            case _ =>
              Redirect(OverseasPensionsSummaryController.show(taxYear))
          }
      }
  }

  def submit(taxYear: Int, index: Option[Int]): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async {
    implicit request  =>
      dblTaxationAgreementForm(request.user, false).bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear, index))),
        doubleTaxationAgreement => updateSessionData(request.pensionsUserData, doubleTaxationAgreement, taxYear, index)
      )
  }

  private def dblTaxationAgreementForm(user: User, isUKScheme: Boolean): Form[DoubleTaxationAgreementFormModel] =
    doubleTaxationAgreementForm(
      agentOrIndividual = if (user.isAgent) "agent" else "individual", isUKScheme
    )

  private def updateViewModel(relief: Relief): DoubleTaxationAgreementFormModel =
    DoubleTaxationAgreementFormModel(
      countryId = relief.alphaTwoCountryCode.fold {
        Countries.get2AlphaCodeFrom3AlphaCode(relief.alphaThreeCountryCode)
      } {
        alpha2 => Some(alpha2)
      },
      article = relief.doubleTaxationCountryArticle,
      treaty = relief.doubleTaxationCountryTreaty,
      reliefAmount = relief.doubleTaxationReliefAmount
    )

  private def updateSessionData[T](pensionsUserData: PensionsUserData,
                                   doubleTaxationAgreementFormModel: DoubleTaxationAgreementFormModel,
                                   taxYear: Int,
                                   index: Option[Int])(implicit request: UserSessionDataRequest[T]): Future[Result] = {
    val reliefs = pensionsUserData.pensions.paymentsIntoOverseasPensions.reliefs
    validateIndex(index, reliefs.size) match {
      case Some(idx) =>
        val updatedCyaModel = pensionsUserData.pensions.copy(
          paymentsIntoOverseasPensions = pensionsUserData.pensions.paymentsIntoOverseasPensions.copy(
            reliefs = Seq(pensionsUserData.pensions.paymentsIntoOverseasPensions.reliefs(idx).copy(
              alphaTwoCountryCode = doubleTaxationAgreementFormModel.countryId,
              alphaThreeCountryCode = Countries.get3AlphaCodeFrom2AlphaCode(doubleTaxationAgreementFormModel.countryId),
              doubleTaxationCountryArticle = doubleTaxationAgreementFormModel.article,
              doubleTaxationCountryTreaty = doubleTaxationAgreementFormModel.treaty,
              doubleTaxationReliefAmount = doubleTaxationAgreementFormModel.reliefAmount
            ))
          )
        )

        pensionSessionService.createOrUpdateSessionData(request.user,
          updatedCyaModel, taxYear, pensionsUserData.isPriorSubmission)(errorHandler.internalServerError()) {
          //TODO: Redirect to the pension scheme details page
          Redirect(controllers.pensions.paymentsIntoOverseasPensions.routes.DoubleTaxationAgreementController.show(taxYear, index))
        }
      case _ =>
        Future.successful(Redirect(OverseasPensionsSummaryController.show(taxYear)))
    }
  }
}
