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
import controllers.pensions.paymentsIntoOverseasPensions.routes.ReliefsSchemeDetailsController
import controllers.predicates.actions.ActionsProvider
import forms.Countries
import forms.overseas.DoubleTaxationAgreementForm.{DoubleTaxationAgreementFormModel, doubleTaxationAgreementForm}
import models.User
import models.mongo.PensionsUserData
import models.pension.charges.OverseasPensionScheme
import models.requests.UserSessionDataRequest
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.PensionSessionService
import services.redirects.PaymentsIntoOverseasPensionsPages.DoubleTaxationAgreementPage
import services.redirects.PaymentsIntoOverseasPensionsRedirects.indexCheckThenJourneyCheck
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.pensions.paymentsIntoOverseasPensions.DoubleTaxationAgreementView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class DoubleTaxationAgreementController @Inject() (actionsProvider: ActionsProvider,
                                                   pensionSessionService: PensionSessionService,
                                                   view: DoubleTaxationAgreementView,
                                                   errorHandler: ErrorHandler,
                                                   mcc: MessagesControllerComponents)(implicit val appConfig: AppConfig)
    extends FrontendController(mcc)
    with I18nSupport
    with SessionHelper {

  def show(taxYear: Int, index: Option[Int]): Action[AnyContent] = actionsProvider.authoriseWithSession(taxYear) async { implicit request =>
    indexCheckThenJourneyCheck(request.sessionData, index, DoubleTaxationAgreementPage, taxYear) { relief: OverseasPensionScheme =>
      val form: Form[DoubleTaxationAgreementFormModel] = dblTaxationAgreementForm(request.user).fill(updateViewModel(relief))
      Future.successful(Ok(view(form, taxYear, index)))
    }
  }

  def submit(taxYear: Int, index: Option[Int]): Action[AnyContent] = actionsProvider.authoriseWithSession(taxYear) async { implicit request =>
    indexCheckThenJourneyCheck(request.sessionData, index, DoubleTaxationAgreementPage, taxYear) { _ =>
      dblTaxationAgreementForm(request.user)
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear, index))),
          doubleTaxationAgreement => updateSessionData(request.sessionData, doubleTaxationAgreement, taxYear, index.get)
        )
    }
  }

  private def dblTaxationAgreementForm(user: User): Form[DoubleTaxationAgreementFormModel] =
    doubleTaxationAgreementForm(agentOrIndividual = if (user.isAgent) "agent" else "individual")

  private def updateViewModel(relief: OverseasPensionScheme): DoubleTaxationAgreementFormModel =
    DoubleTaxationAgreementFormModel(
      countryId = relief.alphaTwoCountryCode.fold(Countries.get2AlphaCodeFrom3AlphaCode(relief.alphaThreeCountryCode))(Some(_)),
      article = relief.doubleTaxationArticle,
      treaty = relief.doubleTaxationTreaty,
      reliefAmount = relief.doubleTaxationReliefAmount
    )

  private def updateSessionData[T](pensionsUserData: PensionsUserData,
                                   doubleTaxationAgreementFormModel: DoubleTaxationAgreementFormModel,
                                   taxYear: Int,
                                   idx: Int)(implicit request: UserSessionDataRequest[T]): Future[Result] = {

    val piopUserData = pensionsUserData.pensions.paymentsIntoOverseasPensions
    val updatedCyaModel = pensionsUserData.pensions.copy(
      paymentsIntoOverseasPensions = piopUserData.copy(
        schemes = piopUserData.schemes.updated(
          idx,
          piopUserData
            .schemes(idx)
            .copy(
              alphaTwoCountryCode = doubleTaxationAgreementFormModel.countryId,
              alphaThreeCountryCode = Countries.maybeGet3AlphaCodeFrom2AlphaCode(doubleTaxationAgreementFormModel.countryId),
              doubleTaxationArticle = doubleTaxationAgreementFormModel.article,
              doubleTaxationTreaty = doubleTaxationAgreementFormModel.treaty,
              doubleTaxationReliefAmount = doubleTaxationAgreementFormModel.reliefAmount
            )
        )
      )
    )

    pensionSessionService.createOrUpdateSessionData(request.user, updatedCyaModel, taxYear, pensionsUserData.isPriorSubmission)(
      errorHandler.internalServerError()) {
      Redirect(ReliefsSchemeDetailsController.show(taxYear, Some(idx)))
    }
  }
}
