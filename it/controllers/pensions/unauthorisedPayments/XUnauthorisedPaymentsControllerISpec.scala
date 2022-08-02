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

package controllers.pensions.unauthorisedPayments

import builders.PensionAnnualAllowanceViewModelBuilder.aPensionAnnualAllowanceViewModel
import builders.PensionsUserDataBuilder.{aPensionsUserData, anPensionsUserDataEmptyCya, pensionsUserDataWithAnnualAllowances, pensionsUserDataWithUnauthorisedPayments}
import builders.UnauthorisedPaymentsViewModelBuilder.anUnauthorisedPaymentsViewModel
import builders.UserBuilder.aUserRequest
import forms.UnAuthorisedPaymentsForm.{noValue, yesNotSurchargeValue, yesSurchargeValue}
import forms.YesNoForm
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.PensionAnnualAllowancePages.reducedAnnualAllowanceTypeUrl
import utils.PageUrls.PensionLifetimeAllowance.pensionAboveAnnualLifetimeAllowanceUrl
import utils.PageUrls.unauthorisedPaymentsPages.{unauthorisedPaymentsUrl, whereAnyOfTheUnauthorisedPaymentsUrl}
import utils.PageUrls.{fullUrl, pensionSummaryUrl}
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

class XUnauthorisedPaymentsControllerISpec extends IntegrationTest with BeforeAndAfterEach with ViewHelpers with PensionsDatabaseHelper {

  private val externalHref = "https://www.gov.uk/guidance/pension-schemes-and-unauthorised-payments"

  object Selectors {
    val checkboxHintSelector = "#unauthorisedPayments-hint"
    val captionSelector: String = "#main-content > div > div > header > p"
    val paragraphTextSelector: String = "#paymentsOutside"
    val paragraphText1Selector: String = "#moreThanOneUnauthorisedPayment"
    val continueButtonSelector: String = "#continue"
    val formSelector: String = "#main-content > div > div > form"
    val yesSurchargeSelector = "#unauthorisedPayments"
    val yesNotSurchargeSelector = "#unauthorisedPayments-2"
    val noSelector = "#unauthorisedPayments-4"
    val expectedDetailsLinkSelector = "#unauthorised-find-out-more-link"
    val subHeadingSelector = "#didYouGetAnUnauthorisedPayment"

    def labelIndex(index: Int): String = s"#main-content > div > div > form > div:nth-child($index) > label"

    def bulletSelector(index: Int): String = s"#main-content > div > div > form > details > div > ul > li:nth-child($index)"

    def detailsParagraphSelector(index: Int): String = s"#main-content > div > div > form > details > div > p:nth-child($index)"
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedButtonText: String
    val expectedHeading: String
    val expectedTitle: String
    val expectedError: String
    val expectedErrorTitle: String
    val expectedSubHeading: String
    val checkboxHint: String
    val expectedParagraphText: String
    val expectedParagraphText1: String
    val expectedYesSurchargeCheckboxText: String
    val expectedYesNotSurchargeCheckboxText: String
    val expectedNoSurchargeCheckboxText: String
    val noEntryErrorMessage: String
    val expectedDetailsExternalLinkText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Unauthorised payments from pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedButtonText = "Continue"
    val expectedHeading = "Unauthorised payments"
    val expectedTitle = "Unauthorised payments"
    val expectedError = "Select yes if you got an unauthorised payment from a pension scheme"
    val expectedErrorTitle = "Select yes if you got an unauthorised payment from a pension scheme"
    val expectedSubHeading = "Did you get an unauthorised payment from a pension scheme?"
    val expectedParagraphText = "Unauthorised payments are made outside the tax rules"
    val expectedParagraphText1: String = "If you got more than one unauthorised payment, you might " +
      "have paid a surcharge (an extra fee) on some of them. It all " +
      "depends on if you’ve taken 25% or more of your pension pots."
    val expectedYesSurchargeCheckboxText = "Yes, unauthorised payments that resulted in a surcharge"
    val expectedYesNotSurchargeCheckboxText = "Yes, unauthorised payments that did not result in a surcharge"
    val expectedNoSurchargeCheckboxText = "No"
    val noEntryErrorMessage = "Select yes if you got an unauthorised payment from a pension scheme"
    val checkboxHint = "Select all that apply."
    val expectedDetailsExternalLinkText = "Find out more about unauthorised payments (opens in new tab)"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Unauthorised payments from pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedButtonText = "Continue"
    val expectedHeading = "Unauthorised payments"
    val expectedTitle = "Unauthorised payments"
    val expectedError = "Select yes if you got an unauthorised payment from a pension scheme"
    val expectedErrorTitle = "Select yes if you got an unauthorised payment from a pension scheme"
    val expectedSubHeading = "Did you get an unauthorised payment from a pension scheme?"
    val expectedParagraphText = "Unauthorised payments are made outside the tax rules"
    val expectedParagraphText1: String = "If you got more than one unauthorised payment, you might " +
      "have paid a surcharge (an extra fee) on some of them. It all " +
      "depends on if you’ve taken 25% or more of your pension pots."
    val expectedYesSurchargeCheckboxText = "Yes, unauthorised payments that resulted in a surcharge"
    val expectedYesNotSurchargeCheckboxText = "Yes, unauthorised payments that did not result in a surcharge"
    val expectedNoSurchargeCheckboxText = "No"
    val noEntryErrorMessage = "Select yes if you got an unauthorised payment from a pension scheme"
    val checkboxHint = "Select all that apply."
    val expectedDetailsExternalLinkText = "Find out more about unauthorised payments (opens in new tab)"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, _]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY)
  )

  ".show" should {
    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        import Selectors._
        import user.commonExpectedResults._

        /*"render the 'unauthorised payments page' with no pre filling" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropPensionsDB()

            val pensionsViewModel = anUnauthorisedPaymentsViewModel.copy()

            insertCyaData(pensionsUserDataWithUnauthorisedPayments(pensionsViewModel, isPriorSubmission = false), aUserRequest)
            urlGet(fullUrl(unauthorisedPaymentsUrl(taxYearEOY)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.commonExpectedResults.expectedTitle)
          h1Check(user.commonExpectedResults.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          hintTextCheck(checkboxHint, Selectors.checkboxHintSelector)
          checkBoxCheck(expectedYesSurchargeCheckboxText, 1, checked = Some(false))
          checkBoxCheck(expectedYesNotSurchargeCheckboxText, 2, checked = Some(false))
          checkBoxCheck(expectedNoSurchargeCheckboxText, 3, checked = Some(false))
          inputFieldValueCheck("unauthorisedPayments[]", Selectors.yesSurchargeSelector, yesSurchargeValue)
          inputFieldValueCheck("unauthorisedPayments[]", Selectors.yesNotSurchargeSelector, yesNotSurchargeValue)
          inputFieldValueCheck("unauthorisedPayments[]", Selectors.noSelector, noValue)
          textOnPageCheck(expectedParagraphText, Selectors.paragraphTextSelector)
          textOnPageCheck(expectedParagraphText1, Selectors.paragraphText1Selector)
          linkCheck(expectedDetailsExternalLinkText, expectedDetailsLinkSelector, externalHref)
          textOnPageCheck(expectedSubHeading, Selectors.subHeadingSelector)
          buttonCheck(expectedButtonText, continueButtonSelector)
          welshToggleCheck(user.isWelsh)
          formPostLinkCheck(unauthorisedPaymentsUrl(taxYearEOY), formSelector)
        }
      }
    }

        "redirect to Pensions Summary page if there is no session data" should {
          lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual(isAgent = false)
            urlGet(fullUrl(unauthorisedPaymentsUrl(taxYearEOY)), follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        "has an SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          //TODO - redirect to CYA page once implemented
          result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
        }
      }
    }*/

        "render unauthorised payments page with 'Yes, unauthorised payments that resulted in a surcharge' checkbox checked" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropPensionsDB()
            val pensionsViewModel = anUnauthorisedPaymentsViewModel.copy(
              surchargeQuestion = Some(true)
            )

            insertCyaData(pensionsUserDataWithUnauthorisedPayments(pensionsViewModel, isPriorSubmission = false), aUserRequest)
            urlGet(fullUrl(unauthorisedPaymentsUrl(taxYearEOY)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.commonExpectedResults.expectedTitle)
          h1Check(user.commonExpectedResults.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          hintTextCheck(checkboxHint, Selectors.checkboxHintSelector)
          checkBoxCheck(expectedYesSurchargeCheckboxText, 1, checked = Some(false))
          checkBoxCheck(expectedYesNotSurchargeCheckboxText, 2, checked = Some(false))
          checkBoxCheck(expectedNoSurchargeCheckboxText, 3, checked = Some(false))
          inputFieldValueCheck("unauthorisedPayments[]", Selectors.yesSurchargeSelector, yesSurchargeValue)
          inputFieldValueCheck("unauthorisedPayments[]", Selectors.yesNotSurchargeSelector, yesNotSurchargeValue)
          inputFieldValueCheck("unauthorisedPayments[]", Selectors.noSelector, noValue)
          textOnPageCheck(expectedParagraphText, Selectors.paragraphTextSelector)
          textOnPageCheck(expectedParagraphText1, Selectors.paragraphText1Selector)
          linkCheck(expectedDetailsExternalLinkText, expectedDetailsLinkSelector, externalHref)
          textOnPageCheck(expectedSubHeading, Selectors.subHeadingSelector)
          buttonCheck(expectedButtonText, continueButtonSelector)
          welshToggleCheck(user.isWelsh)
          formPostLinkCheck(unauthorisedPaymentsUrl(taxYearEOY), formSelector)
        }

        /* ".submit" should {
   userScenarios.foreach { user =>
     s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

       s"return $BAD_REQUEST error when no value is submitted" which {
         lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> "")

         lazy val result: WSResponse = {
           dropPensionsDB()
           authoriseAgentOrIndividual(user.isAgent)
           insertCyaData(aPensionsUserData, aUserRequest)
           urlPost(fullUrl(whereAnyOfTheUnauthorisedPaymentsUrl(taxYearEOY)), body = form, follow = false, welsh = user.isWelsh,
             headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
         }

         "has the correct status" in {
           result.status shouldBe BAD_REQUEST
         }

         implicit def document: () => Document = () => Jsoup.parse(result.body)
         import Selectors._
         import user.commonExpectedResults._
         titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
         h1Check(user.specificExpectedResults.get.expectedHeading)
         captionCheck(expectedCaption(taxYearEOY), captionSelector)
         radioButtonCheck(yesText, 1, checked = Some(false))
         radioButtonCheck(noText, 2, checked = Some(false))
         buttonCheck(expectedButtonText, continueButtonSelector)
         formPostLinkCheck(whereAnyOfTheUnauthorisedPaymentsUrl(taxYearEOY), formSelector)
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
       val viewModel = anUnauthorisedPaymentsViewModel.copy(unauthorisedPaymentsQuestion = None)
       insertCyaData(pensionsUserDataWithUnauthorisedPayments(viewModel), aUserRequest)

       insertCyaData(pensionsUserDataWithUnauthorisedPayments(viewModel), aUserRequest)

       authoriseAgentOrIndividual(isAgent = false)
       urlPost(fullUrl(whereAnyOfTheUnauthorisedPaymentsUrl(taxYearEOY)), body = form, follow = false,
         headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
     }

     "has a SEE_OTHER(303) status and redirect to Pension Scheme Tax Reference (PSTR) Page" in {
       result.status shouldBe SEE_OTHER
       //TODO - redirect to "Pension Scheme Tax Reference PSTR" page once implemented
       result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
     }

     "updates ukPensionSchemesQuestion to Some(true)" in {
       lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
       cyaModel.pensions.unauthorisedPayments.ukPensionSchemesQuestion shouldBe Some(true)
     }
   }

  "redirect and update question to 'No' when there is currently no radio button value selected and the user selects 'No'" which {
     lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)

     lazy val result: WSResponse = {
       dropPensionsDB()
       val viewModel = anUnauthorisedPaymentsViewModel.copy(unauthorisedPaymentsQuestion = None)
       insertCyaData(pensionsUserDataWithUnauthorisedPayments(viewModel), aUserRequest)

       insertCyaData(pensionsUserDataWithUnauthorisedPayments(viewModel), aUserRequest)

       authoriseAgentOrIndividual(isAgent = false)
       urlPost(fullUrl(whereAnyOfTheUnauthorisedPaymentsUrl(taxYearEOY)), body = form, follow = false,
         headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
     }

     "has a SEE_OTHER(303) status and redirect to Check your unauthorised payments page" in {
       result.status shouldBe SEE_OTHER
       //TODO redirect to "Check your unauthorised payments" page
       result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
     }

     "updates ukPensionSchemesQuestion to Some(false)" in {
       lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
       cyaModel.pensions.unauthorisedPayments.ukPensionSchemesQuestion shouldBe Some(false)
     }
   }

   "redirect to Pensions Summary page if there is no session data" should {
    lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
    lazy val result: WSResponse = {
      dropPensionsDB()
      authoriseAgentOrIndividual(isAgent = false)
      urlPost(fullUrl(whereAnyOfTheUnauthorisedPaymentsUrl(taxYearEOY)), body = form, follow = false,
        headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))

    }

    "has an SEE_OTHER status" in {
      result.status shouldBe SEE_OTHER
      //TODO - redirect to CYA page once implemented
      result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
    }
  }*/
      }
    }
  }
}
