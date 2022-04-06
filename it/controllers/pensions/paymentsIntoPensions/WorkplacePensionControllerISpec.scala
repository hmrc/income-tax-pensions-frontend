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
import builders.UserBuilder._
import controllers.pensions.paymentsIntoPension.routes._
import forms.YesNoForm
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.PaymentIntoPensions.workplacePensionUrl
import utils.PageUrls.fullUrl
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

// scalastyle:off magic.number
class WorkplacePensionControllerISpec extends IntegrationTest with ViewHelpers with BeforeAndAfterEach with PensionsDatabaseHelper {

  private val taxYearEOY: Int = taxYear - 1

  object Selectors {
    val captionSelector: String = "#main-content > div > div > header > p"
    val continueButtonSelector: String = "#continue"
    val formSelector: String = "#main-content > div > div > form"
    val yesSelector = "#value"
    val noSelector = "#value-no"
    val h2Selector: String = s"#main-content > div > div > form > div > fieldset > legend > h2"
    val findOutMoreSelector: String = s"#findOutMore-link"

    def paragraphSelector(index: Int): String = s"#main-content > div > div > p:nth-of-type($index)"
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val yesText: String
    val noText: String
    val buttonText: String
    val expectedFindOutMoreText: String
  }

  trait SpecificExpectedResults {
    val expectedHeading: String
    val expectedTitle: String
    val expectedInfoText: String
    val expectedTheseCases: String
    val expectedWhereToCheck: String
    val expectedErrorMessage: String
    val expectedErrorTitle: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Payments into pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val yesText = "Yes"
    val noText = "No"
    val buttonText = "Continue"
    val expectedFindOutMoreText = "Find out more about tax relief (opens in new tab)"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Payments into pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val yesText = "Yes"
    val noText = "No"
    val buttonText = "Continue"
    val expectedFindOutMoreText = "Find out more about tax relief (opens in new tab)"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedHeading = "Did you pay into a workplace pension and not receive tax relief?"
    val expectedTitle = "Did you pay into a workplace pension and not receive tax relief?"
    val expectedInfoText = "You would have made your payments after your pay was taxed."
    val expectedTheseCases = "These cases are unusual as most workplace pensions are set up to give you tax relief at the time of your payment."

    val expectedWhereToCheck = "Check with your employer or pension provider which arrangement you have."
    val expectedErrorMessage = "Select yes if you paid into a workplace pension and did not receive tax relief"
    val expectedErrorTitle = s"Error: $expectedTitle"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedHeading = "Did you pay into a workplace pension and not receive tax relief?"
    val expectedTitle = "Did you pay into a workplace pension and not receive tax relief?"
    val expectedInfoText = "You would have made your payments after your pay was taxed."
    val expectedTheseCases = "These cases are unusual as most workplace pensions are set up to give you tax relief at the time of your payment."
    val expectedWhereToCheck = "Check with your employer or pension provider which arrangement you have."
    val expectedErrorMessage = "Select yes if you paid into a workplace pension and did not receive tax relief"
    val expectedErrorTitle = s"Error: $expectedTitle"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedHeading = "Did your client pay into a workplace pension and not receive tax relief?"
    val expectedTitle = "Did your client pay into a workplace pension and not receive tax relief?"
    val expectedInfoText = "Your client would have made their payments after their pay was taxed."
    val expectedTheseCases = "These cases are unusual as most workplace pensions are set up to give your client tax relief at the time of their payment."

    val expectedWhereToCheck = "Check with your client’s employer or pension provider which arrangement they have."
    val expectedErrorMessage = "Select yes if your client paid into a workplace pension and did not receive tax relief"
    val expectedErrorTitle = s"Error: $expectedTitle"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedHeading = "Did your client pay into a workplace pension and not receive tax relief?"
    val expectedTitle = "Did your client pay into a workplace pension and not receive tax relief?"
    val expectedInfoText = "Your client would have made their payments after their pay was taxed."
    val expectedTheseCases = "These cases are unusual as most workplace pensions are set up to give your client tax relief at the time of their payment."

    val expectedWhereToCheck = "Check with your client’s employer or pension provider which arrangement they have."
    val expectedErrorMessage = "Select yes if your client paid into a workplace pension and did not receive tax relief"
    val expectedErrorTitle = s"Error: $expectedTitle"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  ".show" should {
    userScenarios.foreach { user =>

      import Selectors._
      import user.commonExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        "render the 'Workplace pension and not receive tax relief' question page with no pre-filled radio buttons when no CYA data for this item" which {
          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(anPensionsUserDataEmptyCya, aUserRequest)
            urlGet(fullUrl(workplacePensionUrl(taxYearEOY)), user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedInfoText, paragraphSelector(1))
          textOnPageCheck(user.specificExpectedResults.get.expectedTheseCases, paragraphSelector(2))
          textOnPageCheck(user.specificExpectedResults.get.expectedWhereToCheck, paragraphSelector(3))
          textOnPageCheck(expectedFindOutMoreText, findOutMoreSelector)
          radioButtonCheck(yesText, 1, checked = Some(false))
          radioButtonCheck(noText, 2, checked = Some(false))
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(workplacePensionUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the 'Workplace pension and not receive tax relief' question page with 'Yes' pre-filled when CYA data exists" which {
          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            val viewModel = aPaymentsIntoPensionViewModel.copy(workplacePensionPaymentsQuestion = Some(true))
            insertCyaData(pensionsUserDataWithPaymentsIntoPensions(viewModel), aUserRequest)
            urlGet(fullUrl(workplacePensionUrl(taxYearEOY)), user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedInfoText, paragraphSelector(1))
          textOnPageCheck(user.specificExpectedResults.get.expectedTheseCases, paragraphSelector(2))
          textOnPageCheck(user.specificExpectedResults.get.expectedWhereToCheck, paragraphSelector(3))
          textOnPageCheck(expectedFindOutMoreText, findOutMoreSelector)
          radioButtonCheck(yesText, 1, checked = Some(true))
          radioButtonCheck(noText, 2, checked = Some(false))
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(workplacePensionUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)

        }

        "render the 'Workplace pension and not receive tax relief' question page with 'No' pre-filled when CYA data exists" which {
          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            val viewModel = aPaymentsIntoPensionViewModel.copy(workplacePensionPaymentsQuestion = Some(false), totalWorkplacePensionPayments = None)
            insertCyaData(pensionsUserDataWithPaymentsIntoPensions(viewModel), aUserRequest)
            urlGet(fullUrl(workplacePensionUrl(taxYearEOY)), user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedInfoText, paragraphSelector(1))
          textOnPageCheck(user.specificExpectedResults.get.expectedTheseCases, paragraphSelector(2))
          textOnPageCheck(user.specificExpectedResults.get.expectedWhereToCheck, paragraphSelector(3))
          textOnPageCheck(expectedFindOutMoreText, findOutMoreSelector)
          radioButtonCheck(yesText, 1, checked = Some(false))
          radioButtonCheck(noText, 2, checked = Some(true))
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(workplacePensionUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)

        }
      }
    }

    "redirect to Payments into Pension CYA page when there is no CYA session data" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(workplacePensionUrl(taxYearEOY)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an 303 SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(PaymentsIntoPensionsCYAController.show(taxYearEOY).url)
      }
    }
  }

  ".submit" should {
    userScenarios.foreach { user =>
      import Selectors._
      import user.commonExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "return an error when form is submitted with no entry" which {
          lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> "")

          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            val viewModel = aPaymentsIntoPensionViewModel.copy(workplacePensionPaymentsQuestion = None)
            insertCyaData(pensionsUserDataWithPaymentsIntoPensions(viewModel), aUserRequest)
            urlPost(fullUrl(workplacePensionUrl(taxYearEOY)), body = form, welsh = user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedInfoText, paragraphSelector(1))
          textOnPageCheck(user.specificExpectedResults.get.expectedTheseCases, paragraphSelector(2))
          textOnPageCheck(user.specificExpectedResults.get.expectedWhereToCheck, paragraphSelector(3))
          radioButtonCheck(yesText, 1, checked = Some(false))
          radioButtonCheck(noText, 2, checked = Some(false))
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(workplacePensionUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
          errorSummaryCheck(user.specificExpectedResults.get.expectedErrorMessage, Selectors.yesSelector)
          errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorMessage, Some("value"))

        }
      }
    }

    "redirect to Workplace Pension Amount page when user submits 'Yes' and updates the session value to yes" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val viewModel = aPaymentsIntoPensionViewModel.copy(workplacePensionPaymentsQuestion = Some(false), totalWorkplacePensionPayments = None)
        insertCyaData(pensionsUserDataWithPaymentsIntoPensions(viewModel), aUserRequest)
        urlPost(fullUrl(workplacePensionUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
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
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
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
