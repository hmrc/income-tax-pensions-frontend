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

package controllers.pensions.incomeFromOverseasPensions

import config.{AppConfig, ErrorHandler}
import controllers.predicates.AuthorisedAction
import forms.{Countries, CountryForm, YesNoForm}
import models.User
import models.mongo.PensionsCYAModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import views.html.pensions.incomeFromOverseasPensions.PensionOverseasIncomeCountryView
import controllers.pensions.incomeFromOverseasPensions.routes.PensionOverseasIncomeCountryController
import controllers.pensions.routes.PensionsSummaryController
import models.pension.charges.PensionScheme

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PensionOverseasIncomeCountryController @Inject()(authAction: AuthorisedAction,
                                                       pensionOverseasIncomeCountryView: PensionOverseasIncomeCountryView,
                                                       pensionSessionService: PensionSessionService,
                                                       errorHandler: ErrorHandler
                                                      )(implicit val mcc: MessagesControllerComponents,
                                                        appConfig: AppConfig,
                                                        clock: Clock,
                                                        ec: ExecutionContext) extends FrontendController(mcc) with I18nSupport {

  def countryForm(user: User): Form[String] = CountryForm.countryForm(
    noEntryMsg = s"incomeFromOverseasPensions.pensionOverseasIncomeCountry.error.noEntry.${if (user.isAgent) "agent" else "individual"}"
  )

  def show(taxYear: Int, countryIndex: Option[Int]): Action[AnyContent] = authAction.async { implicit request =>
    pensionSessionService.getPensionSessionData(taxYear, request.user).flatMap {
      case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
      case Right(optPensionUserData) => optPensionUserData match {
        case Some(data) =>
          val countriesToInclude = Countries.getCountryParametersForAllCountries()
          val form = countryForm(request.user)
          countryIndex.fold {
            Future.successful(Ok(
              pensionOverseasIncomeCountryView(
                form, None, countriesToInclude, taxYear, None)))
          } {
            countryIndex =>
              val prefillValue = data.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes.lift(countryIndex)
              prefillValue match {
                case Some(value) =>
                  val countryName = Countries.getCountryFromCode(value.countryCode)
                  countryName match {
                    case countryNameValue@Some(_) =>
                      Future.successful(Ok(
                        pensionOverseasIncomeCountryView(
                          form, countryNameValue.map(_.countryName), countriesToInclude, taxYear, Some(countryIndex))))
                    case None => // TODO - resolve case where country code could not be mapped to country value by the country helper object
                      Future.successful(Ok(
                        pensionOverseasIncomeCountryView(
                          form, None, countriesToInclude, taxYear, Some(countryIndex))))
                  }
                case None =>
                  Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
              }
          }
        case None =>
//        TODO - redirect to CYA page once implemented
                  Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
      }
    }
  }

  def submit(taxYear: Int, countryIndex: Option[Int]): Action[AnyContent] = authAction.async { implicit request =>
    val (countriesToInclude) =
      Countries.getCountryParametersForAllCountries()

    countryForm(request.user).bindFromRequest.fold(
      formWithErrors =>
        Future.successful(BadRequest(pensionOverseasIncomeCountryView(formWithErrors, None, countriesToInclude, taxYear, countryIndex))),
      country => {
        {
          pensionSessionService.getPensionSessionData(taxYear, request.user).flatMap {
            case Right(Some(data)) => {
              val pensionSchemeList: Seq[PensionScheme] = data.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes

              if (validateCountryIndex(countryIndex, pensionSchemeList)) {
                val updatedCyaModel = countryIndex match {
                  case Some(value) =>
                    val scheme = data.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes(value).copy(countryCode = Some(country))
                    data.pensions.copy(
                      incomeFromOverseasPensions = data.pensions.incomeFromOverseasPensions.copy(
                      overseasIncomePensionSchemes = data.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes.updated(value, scheme)
                    ))
                  case None =>
                    val currentSchemes = data.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes
                    data.pensions.copy(
                      incomeFromOverseasPensions = data.pensions.incomeFromOverseasPensions.copy(
                        overseasIncomePensionSchemes = currentSchemes ++ Seq(PensionScheme(Some(country)))
                      ))
                }
                pensionSessionService.createOrUpdateSessionData(request.user,
                  updatedCyaModel, taxYear, data.isPriorSubmission)(errorHandler.internalServerError()) {
                  Redirect(PensionOverseasIncomeCountryController.show(taxYear, countryIndex))
                }
              } else {
                //TODO Redirect to pstr summary controller for overseas pensions
                Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
              }
            }
            //TODO: redirect to CYA page
            case _ => Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
          }
        }
      }
    )
  }

  def validateCountryIndex(countryIndex: Option[Int], pensionSchemeLIst: Seq[PensionScheme]): Boolean = {
    countryIndex match {
      case Some(index) if index < 0 =>
        false
      case Some(index) =>
        pensionSchemeLIst.size > index
      case _ =>
        true
    }
  }
}
