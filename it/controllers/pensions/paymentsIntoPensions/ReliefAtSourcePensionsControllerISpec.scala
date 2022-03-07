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

package controllers.pensions.paymentsIntoPensions

import builders.PaymentsIntoPensionVewModelBuilder.aPaymentsIntoPensionViewModel
import builders.PensionsUserDataBuilder.{anPensionsUserDataEmptyCya, pensionsUserDataWithPaymentsIntoPensions}
import builders.UserBuilder.aUserRequest
import controllers.pensions.paymentsIntoPension.routes.{PensionsTaxReliefNotClaimedController, ReliefAtSourcePaymentsAndTaxReliefAmountController}
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

// scalastyle:off magic.number
class ReliefAtSourcePensionsControllerISpec extends IntegrationTest with BeforeAndAfterEach with ViewHelpers with PensionsDatabaseHelper {

  private val taxYearEOY: Int = taxYear - 1

  object Selectors {
    val captionSelector: String = "#main-content > div > div > form > div > fieldset > legend > header > p"
    val continueButtonSelector: String = "#continue"
    val formSelector: String = "#main-content > div > div > form"
    val yesSelector = "#value"
    val noSelector = "#value-no"
    val h2Selector = "#main-content > div > div > form > div > fieldset > legend > h2"
    val example1TextSelector = "#main-content > div > div > form > div > fieldset > legend > ul > li:nth-child(1)"
    val example2TextSelector = "#main-content > div > div > form > div > fieldset > legend > ul > li:nth-child(2)"

    def paragraphSelector(index: Int): String = s"#main-content > div > div > form > div > fieldset > legend > p:nth-child($index)"
  }

  trait SpecificExpectedResults {
    val expectedH2: String
    val expectedError: String
    val expectedParagraph: String
    val expectedExample1: String
    val expectedExample2: String
    val expectedPensionProviderText: String
    val expectedCheckProviderText: String
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedTitle: String
    val expectedErrorTitle: String
    val expectedHeading: String
    val expectedButtonText: String
    val yesText: String
    val noText: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedH2 = "Did you pay into a RAS pension?"
    val expectedError = "Select yes if you paid into a RAS pension"
    val expectedParagraph = "These are pensions you pay into from:"
    val expectedExample1 = "your net income (after tax has been deducted), if you’re employed"
    val expectedExample2 = "your taxable income, if you’re self-employed"
    val expectedPensionProviderText = "Your pension provider then claims tax relief for you."
    val expectedCheckProviderText = "You can check with your pension provider whether this applies to you."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedH2 = "Did you pay into a RAS pension?"
    val expectedError = "Select yes if you paid into a RAS pension"
    val expectedParagraph = "These are pensions you pay into from:"
    val expectedExample1 = "your net income (after tax has been deducted), if you’re employed"
    val expectedExample2 = "your taxable income, if you’re self-employed"
    val expectedPensionProviderText = "Your pension provider then claims tax relief for you."
    val expectedCheckProviderText = "You can check with your pension provider whether this applies to you."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedH2 = "Did your client pay into a RAS pension?"
    val expectedError = "Select yes if your client paid into a RAS pension"
    val expectedParagraph = "These are pensions your client pays into from:"
    val expectedExample1 = "your client’s net income (after tax has been deducted), if they’re employed"
    val expectedExample2 = "your client’s taxable income, if they’re self-employed"
    val expectedPensionProviderText = "Your pension provider then claims tax relief for your client."
    val expectedCheckProviderText = "You can check with your pension provider whether this applies to your client."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedH2 = "Did your client pay into a RAS pension?"
    val expectedError = "Select yes if your client paid into a RAS pension"
    val expectedParagraph = "These are pensions your client pays into from:"
    val expectedExample1 = "your client’s net income (after tax has been deducted), if they’re employed"
    val expectedExample2 = "your client’s taxable income, if they’re self-employed"
    val expectedPensionProviderText = "Your pension provider then claims tax relief for your client."
    val expectedCheckProviderText = "You can check with your pension provider whether this applies to your client."
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Payments into pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedTitle = "Relief at source (RAS) pensions"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedHeading = "Relief at source (RAS) pensions"
    val expectedButtonText = "Continue"
    val yesText = "Yes"
    val noText = "No"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Payments into pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedTitle = "Relief at source (RAS) pensions"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedHeading = "Relief at source (RAS) pensions"
    val expectedButtonText = "Continue"
    val yesText = "Yes"
    val noText = "No"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  ".show" should {
    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        import Selectors._
        import user.commonExpectedResults._

        "render 'Relief at source (RAS) pensions' page with correct content and no pre-filling" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropPensionsDB()
            insertCyaData(anPensionsUserDataEmptyCya, aUserRequest)
            urlGet(fullUrl(reliefAtSourcePensionsUrl(taxYearEOY)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(expectedTitle)
          h1Check(expectedHeading)
          textOnPageCheck(user.specificExpectedResults.get.expectedH2, h2Selector)
          radioButtonCheck(yesText, 1, checked = Some(false))
          radioButtonCheck(noText, 2, checked = Some(false))
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(reliefAtSourcePensionsUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)

          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph, paragraphSelector(2))
          textOnPageCheck(user.specificExpectedResults.get.expectedExample1, example1TextSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedExample2, example2TextSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedPensionProviderText, paragraphSelector(4))
          textOnPageCheck(user.specificExpectedResults.get.expectedCheckProviderText, paragraphSelector(5))
        }

        "render 'Relief at source (RAS) pensions' page with correct content and yes pre-filled" which {

          implicit lazy val result: WSResponse = {
            dropPensionsDB()
            val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(rasPensionPaymentQuestion = Some(true))
            insertCyaData(pensionsUserDataWithPaymentsIntoPensions(pensionsViewModel), aUserRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(fullUrl(reliefAtSourcePensionsUrl(taxYearEOY)), user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(expectedTitle)
          h1Check(expectedHeading)
          radioButtonCheck(yesText, 1, checked = Some(true))
          radioButtonCheck(noText, 2, checked = Some(false))
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(reliefAtSourcePensionsUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)

          textOnPageCheck(user.specificExpectedResults.get.expectedH2, h2Selector)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph, paragraphSelector(2))
          textOnPageCheck(user.specificExpectedResults.get.expectedExample1, example1TextSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedExample2, example2TextSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedPensionProviderText, paragraphSelector(4))
          textOnPageCheck(user.specificExpectedResults.get.expectedCheckProviderText, paragraphSelector(5))
        }

        "render 'Relief at source (RAS) pensions' page with correct content and no pre-filled" which {

          implicit lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(rasPensionPaymentQuestion = Some(false))
            insertCyaData(pensionsUserDataWithPaymentsIntoPensions(pensionsViewModel), aUserRequest)
            urlGet(fullUrl(reliefAtSourcePensionsUrl(taxYearEOY)), user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(expectedTitle)
          h1Check(expectedHeading)
          radioButtonCheck(yesText, 1, checked = Some(false))
          radioButtonCheck(noText, 2, checked = Some(true))
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(reliefAtSourcePensionsUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)

          textOnPageCheck(user.specificExpectedResults.get.expectedH2, h2Selector)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph, paragraphSelector(2))
          textOnPageCheck(user.specificExpectedResults.get.expectedExample1, example1TextSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedExample2, example2TextSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedPensionProviderText, paragraphSelector(4))
          textOnPageCheck(user.specificExpectedResults.get.expectedCheckProviderText, paragraphSelector(5))

        }
      }
    }
  }

  ".submit" should {
    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        s"return $BAD_REQUEST error when no value is submitted" which {
          lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> "")

          lazy val result: WSResponse = {
            dropPensionsDB()
            insertCyaData(anPensionsUserDataEmptyCya, aUserRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(fullUrl(reliefAtSourcePensionsUrl(taxYearEOY)), body = form, welsh = user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }
          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)
          import Selectors._
          import user.commonExpectedResults._
          titleCheck(expectedErrorTitle)
          h1Check(expectedHeading)
          textOnPageCheck(user.specificExpectedResults.get.expectedH2, h2Selector)
          radioButtonCheck(yesText, 1, checked = Some(false))
          radioButtonCheck(noText, 2, checked = Some(false))
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(reliefAtSourcePensionsUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)

          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph, paragraphSelector(2))
          textOnPageCheck(user.specificExpectedResults.get.expectedExample1, example1TextSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedExample2, example2TextSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedPensionProviderText, paragraphSelector(4))
          textOnPageCheck(user.specificExpectedResults.get.expectedCheckProviderText, paragraphSelector(5))
          errorSummaryCheck(user.specificExpectedResults.get.expectedError, yesSelector)
          errorAboveElementCheck(user.specificExpectedResults.get.expectedError, Some("value"))
        }
      }
    }

    "redirect and update question to 'Yes' when user selects yes when there is no cya data" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

      lazy val result: WSResponse = {
        dropPensionsDB()
        insertCyaData(anPensionsUserDataEmptyCya, aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(reliefAtSourcePensionsUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
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
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
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
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
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

    "redirect to Payment Into Pensions CYA page when user selects No and there is cya data" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)

      lazy val result: WSResponse = {
        dropPensionsDB()
        val paymentsIntoPensionsViewModel = aPaymentsIntoPensionViewModel.copy(rasPensionPaymentQuestion = Some(true),
          totalRASPaymentsAndTaxRelief = Some(123.12))
        insertCyaData(pensionsUserDataWithPaymentsIntoPensions(paymentsIntoPensionsViewModel), aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(reliefAtSourcePensionsUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        //TODO redirect to payment into pensions cya page
        result.header("location") shouldBe Some(PensionsTaxReliefNotClaimedController.show(taxYearEOY).url)
      }

      "updates rasPensionPaymentQuestion to Some(false) and remove totalRASPaymentsAndTaxRelief" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.paymentsIntoPension.rasPensionPaymentQuestion shouldBe Some(false)
        cyaModel.pensions.paymentsIntoPension.totalRASPaymentsAndTaxRelief shouldBe None
      }
    }
  }
}
// scalastyle:on magic.number
