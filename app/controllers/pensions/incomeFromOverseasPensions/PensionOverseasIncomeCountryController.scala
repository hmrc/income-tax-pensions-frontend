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

package controllers.pensions.incomeFromOverseasPensions

import config.{AppConfig, ErrorHandler}
import controllers.pensions.incomeFromOverseasPensions.routes._
import controllers.pensions.routes.OverseasPensionsSummaryController
import controllers.predicates.actions.AuthorisedAction
import forms.{Countries, CountryForm}
import models.User
import models.mongo.PensionsCYAModel
import models.pension.charges.PensionScheme
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import services.redirects.IncomeFromOverseasPensionsPages.WhatCountryIsSchemeRegisteredInPage
import services.redirects.IncomeFromOverseasPensionsRedirects.{cyaPageCall, journeyCheck, redirectForSchemeLoop, schemeIsFinishedCheck}
import services.redirects.SimpleRedirectService.redirectBasedOnCurrentAnswers
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import views.html.pensions.incomeFromOverseasPensions.PensionOverseasIncomeCountryView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PensionOverseasIncomeCountryController @Inject()(authAction: AuthorisedAction,
                                                       pensionOverseasIncomeCountryView: PensionOverseasIncomeCountryView,
                                                       pensionSessionService: PensionSessionService,
                                                       errorHandler: ErrorHandler
                                                      )(implicit val mcc: MessagesControllerComponents,
                                                        appConfig: AppConfig,
                                                        clock: Clock,
                                                        ec: ExecutionContext) extends FrontendController(mcc) with I18nSupport {

  private def countryForm(user: User): Form[String] = CountryForm.countryForm(
    agentOrIndividual = if (user.isAgent) "agent" else "individual"
  )

  def show(taxYear: Int, index: Option[Int]): Action[AnyContent] = authAction.async { implicit request =>
    pensionSessionService.getPensionSessionData(taxYear, request.user).flatMap {
      case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
      case Right(optData) =>
        val countriesToInclude = Countries.overseasCountries
        val form = countryForm(request.user)

        index.fold {
          if (optData.exists(data => data.pensions.incomeFromOverseasPensions.paymentsFromOverseasPensionsQuestion.exists(isTrue => (isTrue))))
            Future.successful(Ok(pensionOverseasIncomeCountryView(form, countriesToInclude, taxYear, None)))
          else Future.successful(Redirect(PensionOverseasIncomeStatus.show(taxYear)))
        } { countryIndex =>

          val checkRedirect = journeyCheck(WhatCountryIsSchemeRegisteredInPage, _, taxYear, Some(countryIndex))
          redirectBasedOnCurrentAnswers(taxYear, optData, cyaPageCall(taxYear))(checkRedirect) { data =>

            val prefillValue = data.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes.lift(countryIndex)
            prefillValue match {

              case Some(_) =>
                Future.successful(Ok(pensionOverseasIncomeCountryView(form, countriesToInclude, taxYear, Some(countryIndex))))
              case None =>
                Future.successful(Redirect(redirectForSchemeLoop(data.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes, taxYear)))
            }
          }
        }

      case _ => Future.successful(Redirect(OverseasPensionsSummaryController.show(taxYear)))
    }
  }

  def submit(taxYear: Int, index: Option[Int]): Action[AnyContent] = authAction.async { implicit request =>
    val countriesToInclude = Countries.overseasCountries

    countryForm(request.user).bindFromRequest().fold(
      formWithErrors =>
        Future.successful(BadRequest(pensionOverseasIncomeCountryView(formWithErrors, countriesToInclude, taxYear, index))),
      country => {
        pensionSessionService.getPensionSessionData(taxYear, request.user).flatMap {
          case Right(Some(data)) =>
            val pensionSchemeList: Seq[PensionScheme] = data.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes

            if (validateCountryIndex(index, pensionSchemeList)) {

              val checkRedirect = journeyCheck(WhatCountryIsSchemeRegisteredInPage, _, taxYear, index)
              redirectBasedOnCurrentAnswers(taxYear, Some(data), cyaPageCall(taxYear))(checkRedirect) { data =>

                val updatedSchemes: Seq[PensionScheme] = index match {
                  case Some(value) =>
                    val scheme = pensionSchemeList(value).copy(alphaTwoCode = Some(country))
                    pensionSchemeList.updated(value, scheme)
                  case None =>
                    val currentSchemes = pensionSchemeList
                    currentSchemes ++ Seq(PensionScheme(alphaTwoCode = Some(country)))
                }
                val updatedCyaModel: PensionsCYAModel = data.pensions.copy(incomeFromOverseasPensions = data.pensions.incomeFromOverseasPensions.copy(
                  overseasIncomePensionSchemes = updatedSchemes))

                pensionSessionService.createOrUpdateSessionData(request.user, updatedCyaModel, taxYear, data.isPriorSubmission)(
                  errorHandler.internalServerError()) {
                  val currentIndex = index.fold(updatedSchemes.size - 1)(index => index)

                  schemeIsFinishedCheck(updatedSchemes, currentIndex, taxYear, PensionPaymentsController.show(taxYear, Some(currentIndex)))
                }
              }
            }
            else {
              Future.successful(Redirect(redirectForSchemeLoop(pensionSchemeList, taxYear)))
            }

          case _ => Future.successful(Redirect(OverseasPensionsSummaryController.show(taxYear)))
        }
      }
    )
  }

  private def validateCountryIndex(countryIndex: Option[Int], pensionSchemeList: Seq[PensionScheme]): Boolean = {
    countryIndex match {
      case Some(index) if index < 0 =>
        false
      case Some(index) =>
        pensionSchemeList.size > index
      case _ =>
        true
    }
  }
}
