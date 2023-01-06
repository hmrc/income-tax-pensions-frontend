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
import builders.PensionsUserDataBuilder.{anPensionsUserDataEmptyCya, pensionsUserDataWithPaymentsIntoPensions}
import builders.UserBuilder.aUserRequest
import controllers.pensions.paymentsIntoPension.routes.{PaymentsIntoPensionsCYAController, PensionsTaxReliefNotClaimedController, ReliefAtSourcePaymentsAndTaxReliefAmountController}
import forms.YesNoForm
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.PaymentIntoPensions.reliefAtSourcePensionsUrl
import utils.PageUrls.fullUrl
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}
import views.ReliefAtSourcePensionsTestSupport.Selectors._
import views.ReliefAtSourcePensionsTestSupport.CommonExpectedEN._
import views.ReliefAtSourcePensionsTestSupport.ExpectedIndividualEN._

// scalastyle:off magic.number
class ReliefAtSourcePensionsControllerISpec extends IntegrationTest with BeforeAndAfterEach with ViewHelpers with PensionsDatabaseHelper {

  val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  ".show" should {
    "render 'Relief at source (RAS) pensions' page with correct content and no pre-filling" which {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropPensionsDB()
        insertCyaData(anPensionsUserDataEmptyCya, aUserRequest)
        urlGet(fullUrl(reliefAtSourcePensionsUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has the correct status" in {
        result.status shouldBe OK
      }

      implicit def document: () => Document = () => Jsoup.parse(result.body)

      titleCheck(expectedTitle)
      h1Check(expectedHeading)
      textOnPageCheck(expectedH2, h2Selector)
      radioButtonCheck(yesText, 1, checked = Some(false))
      radioButtonCheck(noText, 2, checked = Some(false))
      buttonCheck(expectedButtonText, continueButtonSelector)
      formPostLinkCheck(reliefAtSourcePensionsUrl(taxYearEOY), formSelector)
      welshToggleCheck(isWelsh = false)

      captionCheck(expectedCaption(taxYearEOY), captionSelector)
      textOnPageCheck(expectedParagraph, paragraphSelector(1))
      textOnPageCheck(expectedExample1, example1TextSelector)
      textOnPageCheck(expectedExample2, example2TextSelector)
      textOnPageCheck(expectedPensionProviderText, paragraphSelector(2))
      textOnPageCheck(expectedCheckProviderText, paragraphSelector(3))
    }

    "render 'Relief at source (RAS) pensions' page with correct content and yes pre-filled" which {

      implicit lazy val result: WSResponse = {
        dropPensionsDB()
        val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(rasPensionPaymentQuestion = Some(true))
        insertCyaData(pensionsUserDataWithPaymentsIntoPensions(pensionsViewModel), aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(reliefAtSourcePensionsUrl(taxYearEOY)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an OK status" in {
        result.status shouldBe OK
      }

      implicit def document: () => Document = () => Jsoup.parse(result.body)

      titleCheck(expectedTitle)
      h1Check(expectedHeading)
      textOnPageCheck(expectedH2, h2Selector)
      radioButtonCheck(yesText, 1, checked = Some(true))
      radioButtonCheck(noText, 2, checked = Some(false))
      buttonCheck(expectedButtonText, continueButtonSelector)
      formPostLinkCheck(reliefAtSourcePensionsUrl(taxYearEOY), formSelector)
      welshToggleCheck(isWelsh = false)

      captionCheck(expectedCaption(taxYearEOY), captionSelector)
      textOnPageCheck(expectedParagraph, paragraphSelector(1))
      textOnPageCheck(expectedExample1, example1TextSelector)
      textOnPageCheck(expectedExample2, example2TextSelector)
      textOnPageCheck(expectedPensionProviderText, paragraphSelector(2))
      textOnPageCheck(expectedCheckProviderText, paragraphSelector(3))
    }

    "render 'Relief at source (RAS) pensions' page with correct content and no pre-filled" which {

      implicit lazy val result: WSResponse = {
        dropPensionsDB()
        val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(rasPensionPaymentQuestion = Some(false))
        insertCyaData(pensionsUserDataWithPaymentsIntoPensions(pensionsViewModel), aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(reliefAtSourcePensionsUrl(taxYearEOY)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an OK status" in {
        result.status shouldBe OK
      }

      implicit def document: () => Document = () => Jsoup.parse(result.body)

      titleCheck(expectedTitle)
      h1Check(expectedHeading)
      textOnPageCheck(expectedH2, h2Selector)
      radioButtonCheck(yesText, 1, checked = Some(false))
      radioButtonCheck(noText, 2, checked = Some(true))
      buttonCheck(expectedButtonText, continueButtonSelector)
      formPostLinkCheck(reliefAtSourcePensionsUrl(taxYearEOY), formSelector)
      welshToggleCheck(isWelsh = false)

      captionCheck(expectedCaption(taxYearEOY), captionSelector)
      textOnPageCheck(expectedParagraph, paragraphSelector(1))
      textOnPageCheck(expectedExample1, example1TextSelector)
      textOnPageCheck(expectedExample2, example2TextSelector)
      textOnPageCheck(expectedPensionProviderText, paragraphSelector(2))
      textOnPageCheck(expectedCheckProviderText, paragraphSelector(3))
    }

  }
  ".submit" should {

    s"return $BAD_REQUEST error when no value is submitted" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> "")

      lazy val result: WSResponse = {
        dropPensionsDB()
        insertCyaData(anPensionsUserDataEmptyCya, aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(reliefAtSourcePensionsUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }
      "has the correct status" in {
        result.status shouldBe BAD_REQUEST
      }

      implicit def document: () => Document = () => Jsoup.parse(result.body)

      titleCheck(expectedErrorTitle)
      h1Check(expectedHeading)
      textOnPageCheck(expectedH2, h2Selector)
      radioButtonCheck(yesText, 1, checked = Some(false))
      radioButtonCheck(noText, 2, checked = Some(false))
      buttonCheck(expectedButtonText, continueButtonSelector)
      formPostLinkCheck(reliefAtSourcePensionsUrl(taxYearEOY), formSelector)
      welshToggleCheck(isWelsh = false)

      captionCheck(expectedCaption(taxYearEOY), captionSelector)
      textOnPageCheck(expectedParagraph, paragraphSelector(1))
      textOnPageCheck(expectedExample1, example1TextSelector)
      textOnPageCheck(expectedExample2, example2TextSelector)
      textOnPageCheck(expectedPensionProviderText, paragraphSelector(2))
      textOnPageCheck(expectedCheckProviderText, paragraphSelector(3))
      errorSummaryCheck(expectedError, yesSelector)
      errorAboveElementCheck(expectedError, Some("value"))
    }

    "redirect and update question to 'Yes' when user selects yes when there is no cya data" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

      lazy val result: WSResponse = {
        dropPensionsDB()
        insertCyaData(anPensionsUserDataEmptyCya, aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(reliefAtSourcePensionsUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(ReliefAtSourcePaymentsAndTaxReliefAmountController.show(taxYearEOY).url)
      }

      "updates rasPensionPaymentQuestion to Some(true)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.paymentsIntoPension.rasPensionPaymentQuestion shouldBe Some(true)
      }
    }

    "redirect and update question to 'Yes' when user selects yes and cya data exists" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

      lazy val result: WSResponse = {
        dropPensionsDB()
        insertCyaData(pensionsUserDataWithPaymentsIntoPensions(aPaymentsIntoPensionViewModel.copy(
          rasPensionPaymentQuestion = Some(false), totalRASPaymentsAndTaxRelief = None)), aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(reliefAtSourcePensionsUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(ReliefAtSourcePaymentsAndTaxReliefAmountController.show(taxYearEOY).url)
      }

      "updates rasPensionPaymentQuestion to Some(true)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.paymentsIntoPension.rasPensionPaymentQuestion shouldBe Some(true)
      }
    }

    "redirect and update question to 'No' when user selects no and there is no cya data" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)

      lazy val result: WSResponse = {
        dropPensionsDB()
        insertCyaData(anPensionsUserDataEmptyCya, aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(reliefAtSourcePensionsUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(PensionsTaxReliefNotClaimedController.show(taxYearEOY).url)
      }

      "updates rasPensionPaymentQuestion to Some(true)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.paymentsIntoPension.rasPensionPaymentQuestion shouldBe Some(false)
        cyaModel.pensions.paymentsIntoPension.totalRASPaymentsAndTaxRelief shouldBe None
      }
    }

    "redirect to Payment Into Pensions CYA page when user selects No which completes the CYA model" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)

      lazy val result: WSResponse = {
        dropPensionsDB()
        val paymentsIntoPensionsViewModel = aPaymentsIntoPensionViewModel.copy(rasPensionPaymentQuestion = Some(true),
          totalRASPaymentsAndTaxRelief = Some(123.12))
        insertCyaData(pensionsUserDataWithPaymentsIntoPensions(paymentsIntoPensionsViewModel), aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(reliefAtSourcePensionsUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(PaymentsIntoPensionsCYAController.show(taxYearEOY).url)
      }

      "updates rasPensionPaymentQuestion to Some(false) and remove totalRASPaymentsAndTaxRelief" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.paymentsIntoPension.rasPensionPaymentQuestion shouldBe Some(false)
        cyaModel.pensions.paymentsIntoPension.totalRASPaymentsAndTaxRelief shouldBe None
        cyaModel.pensions.paymentsIntoPension.oneOffRasPaymentPlusTaxReliefQuestion shouldBe None
        cyaModel.pensions.paymentsIntoPension.totalOneOffRasPaymentPlusTaxRelief shouldBe None
        cyaModel.pensions.paymentsIntoPension.totalRASPaymentsAndTaxRelief shouldBe None
      }
    }
  }
}
// scalastyle:on magic.number
