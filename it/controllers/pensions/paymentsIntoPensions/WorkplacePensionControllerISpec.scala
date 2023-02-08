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

package controllers.pensions.paymentsIntoPensions

import builders.PaymentsIntoPensionVewModelBuilder.aPaymentsIntoPensionViewModel
import builders.PensionsCYAModelBuilder.aPensionsCYAModel
import builders.PensionsUserDataBuilder.{aPensionsUserData, pensionsUserDataWithPaymentsIntoPensions}
import builders.UserBuilder._
import controllers.pensions.paymentsIntoPensions.routes._
import forms.YesNoForm
import models.mongo.PensionsUserData
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.PaymentIntoPensions.workplacePensionUrl
import utils.PageUrls.fullUrl
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}
import views.WorkplacePensionControllerSpec.Selectors._
import views.WorkplacePensionControllerSpec._
import views.WorkplacePensionControllerSpec.CommonExpectedEN._
import views.WorkplacePensionControllerSpec.ExpectedIndividualEN._

// scalastyle:off magic.number
class WorkplacePensionControllerISpec extends IntegrationTest with ViewHelpers with BeforeAndAfterEach with PensionsDatabaseHelper {

  val noWorkplaceCYAModel: PensionsUserData = aPensionsUserData.copy(
    pensions = aPensionsCYAModel.copy(
      paymentsIntoPension = aPaymentsIntoPensionViewModel.copy(
        workplacePensionPaymentsQuestion = None
      )))

  val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  ".show" should {
    "render the 'Workplace pension and not receive tax relief' question page with no pre-filled radio buttons when no CYA data for this item" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(noWorkplaceCYAModel, aUserRequest)
        urlGet(fullUrl(workplacePensionUrl(taxYearEOY)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an OK status" in {
        result.status shouldBe OK
      }
      implicit def document: () => Document = () => Jsoup.parse(result.body)

      titleCheck(expectedTitle)
      h1Check(expectedHeading)
      captionCheck(expectedCaption(taxYearEOY), captionSelector)
      textOnPageCheck(expectedInfoText, paragraphSelector(1))
      textOnPageCheck(expectedTheseCases, paragraphSelector(2))
      textOnPageCheck(expectedWhereToCheck, paragraphSelector(3))
      textOnPageCheck(expectedFindOutMoreText, findOutMoreSelector)
      radioButtonCheck(yesText, 1, checked = Some(false))
      radioButtonCheck(noText, 2, checked = Some(false))
      buttonCheck(buttonText, continueButtonSelector)
      formPostLinkCheck(workplacePensionUrl(taxYearEOY), formSelector)
      welshToggleCheck(isWelsh = false)
    }

    "render the 'Workplace pension and not receive tax relief' question page with 'Yes' pre-filled when CYA data exists" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val viewModel = aPaymentsIntoPensionViewModel.copy(workplacePensionPaymentsQuestion = Some(true))
        insertCyaData(pensionsUserDataWithPaymentsIntoPensions(viewModel), aUserRequest)
        urlGet(fullUrl(workplacePensionUrl(taxYearEOY)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an OK status" in {
        result.status shouldBe OK
      }
      implicit def document: () => Document = () => Jsoup.parse(result.body)

      titleCheck(expectedTitle)
      h1Check(expectedHeading)
      captionCheck(expectedCaption(taxYearEOY), captionSelector)
      textOnPageCheck(expectedInfoText, paragraphSelector(1))
      textOnPageCheck(expectedTheseCases, paragraphSelector(2))
      textOnPageCheck(expectedWhereToCheck, paragraphSelector(3))
      textOnPageCheck(expectedFindOutMoreText, findOutMoreSelector)
      radioButtonCheck(yesText, 1, checked = Some(true))
      radioButtonCheck(noText, 2, checked = Some(false))
      buttonCheck(buttonText, continueButtonSelector)
      formPostLinkCheck(workplacePensionUrl(taxYearEOY), formSelector)
      welshToggleCheck(isWelsh = false)
    }

    "render the 'Workplace pension and not receive tax relief' question page with 'No' pre-filled when CYA data exists" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val viewModel = aPaymentsIntoPensionViewModel.copy(workplacePensionPaymentsQuestion = Some(false), totalWorkplacePensionPayments = None)
        insertCyaData(pensionsUserDataWithPaymentsIntoPensions(viewModel), aUserRequest)
        urlGet(fullUrl(workplacePensionUrl(taxYearEOY)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an OK status" in {
        result.status shouldBe OK
      }
      implicit def document: () => Document = () => Jsoup.parse(result.body)

      titleCheck(expectedTitle)
      h1Check(expectedHeading)
      captionCheck(expectedCaption(taxYearEOY), captionSelector)
      textOnPageCheck(expectedInfoText, paragraphSelector(1))
      textOnPageCheck(expectedTheseCases, paragraphSelector(2))
      textOnPageCheck(expectedWhereToCheck, paragraphSelector(3))
      textOnPageCheck(expectedFindOutMoreText, findOutMoreSelector)
      radioButtonCheck(yesText, 1, checked = Some(false))
      radioButtonCheck(noText, 2, checked = Some(true))
      buttonCheck(buttonText, continueButtonSelector)
      formPostLinkCheck(workplacePensionUrl(taxYearEOY), formSelector)
      welshToggleCheck(isWelsh = false)
    }
    "redirect to Payments into Pension CYA page when there is no CYA session data" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(workplacePensionUrl(taxYearEOY)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an 303 SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(PaymentsIntoPensionsCYAController.show(taxYearEOY).url)
      }
    }
  }

  ".submit" should {
    "return an error when form is submitted with no entry" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> "")

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val viewModel = aPaymentsIntoPensionViewModel.copy(workplacePensionPaymentsQuestion = None)
        insertCyaData(pensionsUserDataWithPaymentsIntoPensions(viewModel), aUserRequest)
        urlPost(fullUrl(workplacePensionUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has the correct status" in {
        result.status shouldBe BAD_REQUEST
      }
      implicit def document: () => Document = () => Jsoup.parse(result.body)

      titleCheck(expectedErrorTitle)
      h1Check(expectedHeading)
      captionCheck(expectedCaption(taxYearEOY), captionSelector)
      textOnPageCheck(expectedInfoText, paragraphSelector(1))
      textOnPageCheck(expectedTheseCases, paragraphSelector(2))
      textOnPageCheck(expectedWhereToCheck, paragraphSelector(3))
      radioButtonCheck(yesText, 1, checked = Some(false))
      radioButtonCheck(noText, 2, checked = Some(false))
      buttonCheck(buttonText, continueButtonSelector)
      formPostLinkCheck(workplacePensionUrl(taxYearEOY), formSelector)
      welshToggleCheck(isWelsh = false)
      errorSummaryCheck(expectedErrorMessage, Selectors.yesSelector)
      errorAboveElementCheck(expectedErrorMessage, Some("value"))
    }


    "redirect to Workplace Pension Amount page when user submits 'Yes' and updates the session value to yes" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val viewModel = aPaymentsIntoPensionViewModel.copy(workplacePensionPaymentsQuestion = Some(false), totalWorkplacePensionPayments = None)
        insertCyaData(pensionsUserDataWithPaymentsIntoPensions(viewModel), aUserRequest)
        urlPost(fullUrl(workplacePensionUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(WorkplaceAmountController.show(taxYearEOY).url)
      }

      "updates workplacePensionPayments question to Some(true)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.paymentsIntoPension.workplacePensionPaymentsQuestion shouldBe Some(true)
      }
    }

    "redirect to Payment into Pensions CYA page when user submits 'No' and updates the session value to no" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val viewModel = aPaymentsIntoPensionViewModel.copy(workplacePensionPaymentsQuestion = Some(true), totalWorkplacePensionPayments = Some(123.12))
        insertCyaData(pensionsUserDataWithPaymentsIntoPensions(viewModel), aUserRequest)
        urlPost(fullUrl(workplacePensionUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(PaymentsIntoPensionsCYAController.show(taxYearEOY).url)
      }

      "updates workplacePensionPayments question to Some(false) and remove totalWorkplacePensionPayments amount" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.paymentsIntoPension.workplacePensionPaymentsQuestion shouldBe Some(false)
        cyaModel.pensions.paymentsIntoPension.totalWorkplacePensionPayments shouldBe None
      }
    }
  }
}
// scalastyle:on magic.number
