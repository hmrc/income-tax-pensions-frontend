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

package controllers.pensions.unauthorisedPayments

import builders.PensionsUserDataBuilder.{aPensionsUserData, pensionsUserDataWithUnauthorisedPayments}
import builders.UnauthorisedPaymentsViewModelBuilder.{anUnauthorisedPaymentsEmptySchemesViewModel, anUnauthorisedPaymentsViewModel}
import builders.UserBuilder.aUserRequest
import forms.YesNoForm
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.UnauthorisedPaymentsPages._
import utils.PageUrls.fullUrl
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

class WereAnyOfTheUnauthorisedPaymentsControllerISpec extends IntegrationTest with BeforeAndAfterEach with ViewHelpers with PensionsDatabaseHelper {

  object Selectors {
    val captionSelector: String = "#main-content > div > div > header > p"
    val continueButtonSelector: String = "#continue"
    val formSelector: String = "#main-content > div > div > form"
    val yesSelector = "#value"
    val noSelector = "#value-no"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedErrorTitle: String
    val noEntryErrorMessage: String
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedButtonText: String
    val yesText: String
    val noText: String
    val noEntryErrorMessage: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Were any of the unauthorised payments from a UK pension scheme?"
    val expectedHeading = "Were any of the unauthorised payments from a UK pension scheme?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val noEntryErrorMessage = "Select yes if you got an unauthorised payment from a pension scheme"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "A ddaeth unrhyw rai o’r taliadau heb awdurdod o gynllun pensiwn yn y DU?"
    val expectedHeading = "A ddaeth unrhyw rai o’r taliadau heb awdurdod o gynllun pensiwn yn y DU?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val noEntryErrorMessage = "Dewiswch ‘Iawn’ os cawsoch daliad heb awdurdod o gynllun pensiwn"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Were any of the unauthorised payments from a UK pension scheme?"
    val expectedHeading = "Were any of the unauthorised payments from a UK pension scheme?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val noEntryErrorMessage = "Select yes if you got an unauthorised payment from a pension scheme" //TODO need an agent equiv of this
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "A ddaeth unrhyw rai o’r taliadau heb awdurdod o gynllun pensiwn yn y DU?"
    val expectedHeading = "A ddaeth unrhyw rai o’r taliadau heb awdurdod o gynllun pensiwn yn y DU?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val noEntryErrorMessage = "Dewiswch ‘Iawn’ os cawsoch daliad heb awdurdod o gynllun pensiwn" //TODO need an agent equiv of this
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Unauthorised payments from pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedButtonText = "Continue"
    val expectedTitle = "Were any of the unauthorised payments from a UK pension scheme?"
    val expectedHeading = "Were any of the unauthorised payments from a UK pension scheme?"
    val yesText = "Yes"
    val noText = "No"
    val noEntryErrorMessage = "Select yes if you got an unauthorised payment from a pension scheme"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Taliadau heb awdurdod o bensiynau ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val expectedButtonText = "Yn eich blaen"
    val expectedTitle = "A ddaeth unrhyw rai o’r taliadau heb awdurdod o gynllun pensiwn yn y DU?"
    val expectedHeading = "A ddaeth unrhyw rai o’r taliadau heb awdurdod o gynllun pensiwn yn y DU?"
    val yesText = "Iawn"
    val noText = "Na"
    val noEntryErrorMessage = "Select yes if you got an unauthorised payment from a pension scheme"
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

        "render the Were any of the Unauthorised Payments controller page with no pre filling" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropPensionsDB()
            insertCyaData(pensionsUserDataWithUnauthorisedPayments(anUnauthorisedPaymentsViewModel.copy(ukPensionSchemesQuestion = None)))
            urlGet(fullUrl(wereAnyOfTheUnauthorisedPaymentsUrl(taxYearEOY)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          radioButtonCheck(yesText, 1, checked = Some(false))
          radioButtonCheck(noText, 2, checked = Some(false))
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(wereAnyOfTheUnauthorisedPaymentsUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the Were any of the Unauthorised Payments controller page with correct content and with pre-filled data" which {

          implicit lazy val result: WSResponse = {
            dropPensionsDB()

            val viewModel = anUnauthorisedPaymentsViewModel.copy()
            insertCyaData(pensionsUserDataWithUnauthorisedPayments(viewModel))
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(fullUrl(wereAnyOfTheUnauthorisedPaymentsUrl(taxYearEOY)), user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          radioButtonCheck(yesText, 1, checked = Some(true))
          radioButtonCheck(noText, 2, checked = Some(false))
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(wereAnyOfTheUnauthorisedPaymentsUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "redirect to Pensions Summary page if there is no session data" should {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        urlGet(fullUrl(wereAnyOfTheUnauthorisedPaymentsUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(checkUnauthorisedPaymentsCyaUrl(taxYearEOY))
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
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(aPensionsUserData)
            urlPost(fullUrl(wereAnyOfTheUnauthorisedPaymentsUrl(taxYearEOY)), body = form, follow = false, welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)
          import Selectors._
          import user.commonExpectedResults._
          titleCheck(user.specificExpectedResults.get.expectedErrorTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          radioButtonCheck(yesText, 1, checked = Some(false))
          radioButtonCheck(noText, 2, checked = Some(false))
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(wereAnyOfTheUnauthorisedPaymentsUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)

          errorSummaryCheck(user.specificExpectedResults.get.noEntryErrorMessage, Selectors.yesSelector)
          errorAboveElementCheck(user.specificExpectedResults.get.noEntryErrorMessage, Some("value"))
        }
      }
    }

    "redirect and update question to 'Yes' when there is currently no radio button value selected and the user selects 'Yes'" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

      lazy val result: WSResponse = {
        dropPensionsDB()
        val viewModel = anUnauthorisedPaymentsViewModel.copy(
          ukPensionSchemesQuestion = None, pensionSchemeTaxReference = None)
        insertCyaData(pensionsUserDataWithUnauthorisedPayments(viewModel))
        authoriseAgentOrIndividual()
        urlPost(fullUrl(wereAnyOfTheUnauthorisedPaymentsUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has a SEE_OTHER(303) status and redirect to Pension Scheme Tax Reference (PSTR) Page" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionSchemeTaxReferenceUrl(taxYearEOY))
      }

      "updates ukPensionSchemesQuestion to Some(true)" in {
        val expectedViewModel = anUnauthorisedPaymentsEmptySchemesViewModel.copy(ukPensionSchemesQuestion = Some(true))
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.unauthorisedPayments shouldBe expectedViewModel
      }
    }

    "redirect to the CYA page and persist question as 'Yes' when CYA data is now complete" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

      lazy val result: WSResponse = {
        dropPensionsDB()
        insertCyaData(pensionsUserDataWithUnauthorisedPayments(anUnauthorisedPaymentsViewModel))
        authoriseAgentOrIndividual()
        urlPost(fullUrl(wereAnyOfTheUnauthorisedPaymentsUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has a SEE_OTHER(303) status and redirect to Pension Scheme Tax Reference (PSTR) Page" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(checkUnauthorisedPaymentsCyaUrl(taxYearEOY))
      }

      "updates ukPensionSchemesQuestion to Some(true)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.unauthorisedPayments shouldBe anUnauthorisedPaymentsViewModel
      }
    }

    "redirect and update question to 'No' when there is currently no radio button value selected and the user selects 'No'" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)

      lazy val result: WSResponse = {
        dropPensionsDB()
        val viewModel = anUnauthorisedPaymentsViewModel.copy(
          ukPensionSchemesQuestion = None, pensionSchemeTaxReference = None)
        insertCyaData(pensionsUserDataWithUnauthorisedPayments(viewModel))
        authoriseAgentOrIndividual()
        urlPost(fullUrl(wereAnyOfTheUnauthorisedPaymentsUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has a SEE_OTHER(303) status and redirect to Check your unauthorised payments page" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(checkUnauthorisedPaymentsCyaUrl(taxYearEOY))
      }

      "updates ukPensionSchemesQuestion to Some(false)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.unauthorisedPayments shouldBe anUnauthorisedPaymentsEmptySchemesViewModel
      }
    }

    "redirect to CYA page, updating question to 'No' and clearing PSTR collection when prior session data was 'Yes'" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)

      lazy val result: WSResponse = {
        dropPensionsDB()
        insertCyaData(pensionsUserDataWithUnauthorisedPayments(anUnauthorisedPaymentsViewModel))
        authoriseAgentOrIndividual()
        urlPost(fullUrl(wereAnyOfTheUnauthorisedPaymentsUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has a SEE_OTHER(303) status and redirect to Check your unauthorised payments page" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(checkUnauthorisedPaymentsCyaUrl(taxYearEOY))
      }

      "updates ukPensionSchemesQuestion to Some(false)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.unauthorisedPayments shouldBe anUnauthorisedPaymentsEmptySchemesViewModel
      }
    }

    "redirect to Pensions Summary page if there is no session data" should {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        urlPost(fullUrl(wereAnyOfTheUnauthorisedPaymentsUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))

      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(checkUnauthorisedPaymentsCyaUrl(taxYearEOY))
      }
    }
  }
}
