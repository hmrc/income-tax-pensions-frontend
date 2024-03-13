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

import cats.implicits.catsSyntaxOptionId
import config.AppConfig
import controllers.predicates.actions.ActionsProvider
import forms.Countries
import forms.overseas.PensionSchemeForm.OverseasOnlyPensionSchemeFormModel.{emptySchemeModel, fromRefundPensionScheme}
import forms.overseas.PensionSchemeForm.{OverseasOnlyPensionSchemeFormModel, toOverseasPensionSchemeForm}
import models.User
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.pension.charges.OverseasRefundPensionScheme
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import services.redirects.ShortServiceRefundsPages.SchemeDetailsPage
import services.redirects.ShortServiceRefundsRedirects.refundSummaryRedirect
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.EitherTUtils.ResultMergersOps
import utils.SessionHelper
import validation.pensions.shortServiceRefunds.ShortServiceRefundsValidator.{validateFlow, validateIndex}
import views.html.pensions.shortServiceRefunds.ShortServicePensionsSchemeView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@Singleton
class ShortServicePensionsSchemeController @Inject() (actionsProvider: ActionsProvider,
                                                      service: PensionSessionService,
                                                      view: ShortServicePensionsSchemeView,
                                                      mcc: MessagesControllerComponents)(implicit appConfig: AppConfig, ec: ExecutionContext)
    extends FrontendController(mcc)
    with I18nSupport
    with SessionHelper {

  def show(taxYear: Int, index: Option[Int]): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async { implicit request =>
    val answers = request.pensionsUserData.pensions.shortServiceRefunds

    validateIndex[SchemeDetailsPage](index, answers, taxYear) { validIndex =>
      validateFlow(answers, SchemeDetailsPage(), taxYear, validIndex.some) {
        val schemeModel = Try(answers.refundPensionScheme(validIndex)).fold(_ => emptySchemeModel, fromRefundPensionScheme)
        val filledForm  = formProvider(request.user).fill(schemeModel)

        Future.successful(Ok(view(filledForm, taxYear, validIndex)))
      }
    }

  }

  def submit(taxYear: Int, index: Option[Int]): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async { implicit request =>
    val journey = request.pensionsUserData.pensions.shortServiceRefunds

    validateIndex[SchemeDetailsPage](index, journey, taxYear) { validIndex =>
      validateFlow(journey, SchemeDetailsPage(), taxYear, validIndex.some) {
        formProvider(request.user)
          .bindFromRequest()
          .fold(
            formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear, validIndex))),
            scheme => {
              val updatedSession = updateSessionModel(request.pensionsUserData, scheme, validIndex)
              val userData       = request.pensionsUserData.copy(pensions = updatedSession)
              service
                .upsertSession(userData)
                .onSuccess(refundSummaryRedirect(taxYear))
            }
          )
      }
    }
  }

  private def formProvider(user: User): Form[OverseasOnlyPensionSchemeFormModel] =
    toOverseasPensionSchemeForm(agentOrIndividual = if (user.isAgent) "agent" else "individual")

  private def updateSessionModel(userData: PensionsUserData, formModel: OverseasOnlyPensionSchemeFormModel, index: Int): PensionsCYAModel = {
    val schemeAnswers = userData.pensions.shortServiceRefunds.refundPensionScheme

    val updatedSchemeModel = OverseasRefundPensionScheme(
      name = formModel.providerName.some,
      providerAddress = formModel.providerAddress.some,
      qualifyingRecognisedOverseasPensionScheme = formModel.schemeReference.some,
      alphaTwoCountryCode = formModel.countryId,
      alphaThreeCountryCode = Countries.maybeGet3AlphaCodeFrom2AlphaCode(formModel.countryId)
    )

    val updatedSchemes = Try(schemeAnswers(index)) match {
      case Success(_) => schemeAnswers.updated(index, updatedSchemeModel)
      case Failure(_) => schemeAnswers :+ updatedSchemeModel
    }

    val updatedJourney = userData.pensions.shortServiceRefunds.copy(refundPensionScheme = updatedSchemes)

    userData.pensions.copy(shortServiceRefunds = updatedJourney)
  }

}
