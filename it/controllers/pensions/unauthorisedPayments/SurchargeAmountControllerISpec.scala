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

import builders.PensionsUserDataBuilder.pensionsUserDataWithUnauthorisedPayments
import builders.UnauthorisedPaymentsViewModelBuilder.{anUnauthorisedPaymentsEmptyViewModel, anUnauthorisedPaymentsViewModel}
import builders.UserBuilder.aUserRequest
import forms.AmountForm
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.UnauthorisedPaymentsPages.{
  checkUnauthorisedPaymentsCyaUrl,
  surchargeAmountUrl,
  taxOnAmountSurchargedUrl,
  unauthorisedPaymentsUrl
}
import utils.PageUrls.fullUrl
import utils.{CommonUtils, IntegrationTest, PensionsDatabaseHelper}

class SurchargeAmountControllerISpec extends IntegrationTest with CommonUtils with BeforeAndAfterEach with PensionsDatabaseHelper {

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] =
    Seq(
      UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
    )
  private val poundPrefixText             = "£"
  private val amountInputName             = "amount"
  private implicit val url: Int => String = surchargeAmountUrl

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedTitle: String
    val expectedHeading: String
    val expectedErrorTitle: String
    val noEntryErrorMessage: String
    val invalidFormatErrorText: String
    val maxAmountErrorText: String
    val expectedParagraphTwo: String
    val hintText: String
    val buttonText: String
  }

  trait SpecificExpectedResults {
    val expectedParagraph: String
  }

  object Selectors {
    val captionSelector: String        = "#main-content > div > div > header > p"
    val continueButtonSelector: String = "#continue"
    val formSelector: String           = "#main-content > div > div > form"
    val hintTextSelector               = "#amount-hint"
    val poundPrefixSelector            = ".govuk-input__prefix"
    val inputSelector                  = "#amount"
    val expectedErrorHref              = "#amount"

    def paragraphSelector(index: Int): String = s"#main-content > div > div > p:nth-of-type($index)"
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Unauthorised payments from pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedTitle                  = "Amount that resulted in a surcharge"
    val expectedHeading                = "Amount that resulted in a surcharge"
    val expectedErrorTitle             = s"Error: $expectedTitle"
    val hintText                       = "For example, £193.52"
    val noEntryErrorMessage            = "Enter the total amount of unauthorised payment that resulted in a surcharge"
    val invalidFormatErrorText         = "Enter the total amount in pounds"
    val maxAmountErrorText             = "The amount entered must be less than £100,000,000,000"
    val buttonText                     = "Continue"
    val expectedParagraph              = "Give a total of unauthorised payments that resulted in surcharges from all your client’s pension schemes."
    val expectedParagraphTwo           = "You can tell us about unauthorised payments that did not result in a surcharge later."
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Taliadau heb awdurdod o bensiynau ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val expectedTitle                  = "Y swm a wnaeth arwain at ordal"
    val expectedHeading                = "Y swm a wnaeth arwain at ordal"
    val expectedErrorTitle             = s"Gwall: $expectedTitle"
    val hintText                       = "Er enghraifft, £193.52"
    val noEntryErrorMessage            = "Nodwch gyfanswm y taliadau heb awdurdod a wnaeth arwain at ordal"
    val invalidFormatErrorText         = "Nodwch y cyfanswm yn y fformat cywir"
    val maxAmountErrorText             = "The amount entered must be less than £100,000,000,000"
    val buttonText                     = "Yn eich blaen"
    val expectedParagraph    = "Rhowch gyfanswm y taliadau heb awdurdod o bob un o gynlluniau pensiwn eich cleient a wnaeth arwain at ordaliadau."
    val expectedParagraphTwo = "Gallwch roi gwybod i ni am daliadau heb awdurdod na wnaethant arwain at ordal yn nes ymlaen."
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedParagraph = "Give a total of unauthorised payments that resulted in surcharges from all your pension schemes."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedParagraph = "Rhowch gyfanswm y taliadau heb awdurdod o bob un o’ch cynlluniau pensiwn a wnaeth arwain at ordaliadau."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedParagraph = "Give a total of unauthorised payments that resulted in surcharges from all your client’s pension schemes."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedParagraph = "Rhowch gyfanswm y taliadau heb awdurdod o bob un o gynlluniau pensiwn eich cleient a wnaeth arwain at ordaliadau."
  }

  ".show" should {
    userScenarios.foreach { user =>
      import Selectors._
      import user.commonExpectedResults._
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        "render unauthorised payments that resulted in surcharges page with no pre filling" which {
          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            val viewModel = anUnauthorisedPaymentsViewModel.copy(surchargeAmount = None)
            insertCyaData(pensionsUserDataWithUnauthorisedPayments(viewModel, isPriorSubmission = false))
            urlGet(
              fullUrl(surchargeAmountUrl(taxYearEOY)),
              user.isWelsh,
              follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(expectedTitle, user.isWelsh)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph, paragraphSelector(1))
          textOnPageCheck(expectedParagraphTwo, paragraphSelector(2))
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, inputSelector, "")
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(surchargeAmountUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render into your surcharge amount page when cya data" which {
          val existingAmount: BigDecimal = 999.88
          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            val pensionsViewModel = anUnauthorisedPaymentsViewModel.copy(surchargeAmount = Some(existingAmount))
            insertCyaData(pensionsUserDataWithUnauthorisedPayments(pensionsViewModel, isPriorSubmission = false))
            urlGet(
              fullUrl(surchargeAmountUrl(taxYearEOY)),
              user.isWelsh,
              follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }
          titleCheck(expectedTitle, user.isWelsh)
          h1Check(user.commonExpectedResults.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph, paragraphSelector(1))
          textOnPageCheck(expectedParagraphTwo, paragraphSelector(2))
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, inputSelector, s"$existingAmount")
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(surchargeAmountUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "redirect to the first page in journey when current page is invalid in journey" in {
      val viewModel               = anUnauthorisedPaymentsViewModel.copy(surchargeQuestion = None, noSurchargeQuestion = None)
      val pensionUserData         = pensionsUserDataWithUnauthorisedPayments(viewModel, isPriorSubmission = false)
      lazy val result: WSResponse = showPage(pensionUserData)

      result.status shouldBe SEE_OTHER
      result.header("location").contains(unauthorisedPaymentsUrl(taxYearEOY))
    }

    "redirect to the CYA page if there is no session data" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        urlGet(
          fullUrl(surchargeAmountUrl(taxYearEOY)),
          follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkUnauthorisedPaymentsCyaUrl(taxYearEOY)) shouldBe true
      }

    }
  }

  ".submit" should {
    userScenarios.foreach { user =>
      import Selectors._
      import user.commonExpectedResults._
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        "return an error when form is submitted with no input entry" which {
          val amountEmpty                    = ""
          val emptyForm: Map[String, String] = Map(AmountForm.amount -> amountEmpty)

          lazy val result: WSResponse = {
            dropPensionsDB()
            val pensionsViewModel = anUnauthorisedPaymentsViewModel.copy(surchargeAmount = None)
            insertCyaData(pensionsUserDataWithUnauthorisedPayments(pensionsViewModel, isPriorSubmission = false))
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(
              fullUrl(surchargeAmountUrl(taxYearEOY)),
              body = emptyForm,
              welsh = user.isWelsh,
              follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
            )
          }

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(expectedErrorTitle, user.isWelsh)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph, paragraphSelector(1))
          textOnPageCheck(expectedParagraphTwo, paragraphSelector(2))
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, inputSelector, amountEmpty)
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(surchargeAmountUrl(taxYearEOY), formSelector)
          errorSummaryCheck(noEntryErrorMessage, expectedErrorHref)
          errorAboveElementCheck(noEntryErrorMessage)
          welshToggleCheck(user.isWelsh)
        }

        "return an error when form is submitted with an invalid format input" which {
          val amountInvalidFormat                    = "invalid"
          val invalidFormatForm: Map[String, String] = Map(AmountForm.amount -> amountInvalidFormat)

          lazy val result: WSResponse = {
            dropPensionsDB()
            val pensionsViewModel = anUnauthorisedPaymentsViewModel.copy(surchargeAmount = None)
            insertCyaData(pensionsUserDataWithUnauthorisedPayments(pensionsViewModel, isPriorSubmission = false))
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(
              fullUrl(surchargeAmountUrl(taxYearEOY)),
              body = invalidFormatForm,
              welsh = user.isWelsh,
              follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
            )
          }

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(expectedErrorTitle, user.isWelsh)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph, paragraphSelector(1))
          textOnPageCheck(expectedParagraphTwo, paragraphSelector(2))
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, inputSelector, amountInvalidFormat)
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(surchargeAmountUrl(taxYearEOY), formSelector)
          errorSummaryCheck(invalidFormatErrorText, expectedErrorHref)
          errorAboveElementCheck(invalidFormatErrorText)
          welshToggleCheck(user.isWelsh)
        }

        "return an error when form is submitted with input over maximum allowed value" which {
          val amountOverMaximum                    = "100,000,000,000"
          val overMaximumForm: Map[String, String] = Map(AmountForm.amount -> amountOverMaximum)

          lazy val result: WSResponse = {
            dropPensionsDB()
            val pensionsViewModel = anUnauthorisedPaymentsViewModel.copy(surchargeAmount = None)
            insertCyaData(pensionsUserDataWithUnauthorisedPayments(pensionsViewModel, isPriorSubmission = false))
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(
              fullUrl(surchargeAmountUrl(taxYearEOY)),
              body = overMaximumForm,
              welsh = user.isWelsh,
              follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
            )
          }

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(expectedErrorTitle, user.isWelsh)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph, paragraphSelector(1))
          textOnPageCheck(expectedParagraphTwo, paragraphSelector(2))
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, inputSelector, amountOverMaximum)
          formPostLinkCheck(surchargeAmountUrl(taxYearEOY), formSelector)
          buttonCheck(buttonText, continueButtonSelector)
          errorSummaryCheck(maxAmountErrorText, expectedErrorHref)
          errorAboveElementCheck(maxAmountErrorText)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "redirect to the Non-UK Tax on Surcharged Amount page when a valid amount is submitted and update the session data" which {
      val validAmount                    = "100.22"
      val validForm: Map[String, String] = Map(AmountForm.amount -> validAmount)

      lazy val result: WSResponse = {
        dropPensionsDB()
        val pensionsViewModel = anUnauthorisedPaymentsEmptyViewModel.copy(
          surchargeQuestion = Some(true),
          surchargeAmount = None
        )
        insertCyaData(pensionsUserDataWithUnauthorisedPayments(pensionsViewModel, isPriorSubmission = false))
        authoriseAgentOrIndividual()
        urlPost(
          fullUrl(surchargeAmountUrl(taxYearEOY)),
          body = validForm,
          follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has a SEE_OTHER(303) status" in {

        result.status shouldBe SEE_OTHER
        result.header("location").contains(taxOnAmountSurchargedUrl(taxYearEOY)) shouldBe true

      }

      "updates surcharge amount" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.unauthorisedPayments.surchargeAmount shouldBe Some(BigDecimal(validAmount))
      }
    }

    "redirect to the CYA page when a valid amount is submitted and update the session amount, completing the journey" which {
      val validAmount                    = "100.22"
      val validForm: Map[String, String] = Map(AmountForm.amount -> validAmount)

      lazy val result: WSResponse = {
        dropPensionsDB()
        val pensionsViewModel = anUnauthorisedPaymentsViewModel.copy(surchargeAmount = None)
        insertCyaData(pensionsUserDataWithUnauthorisedPayments(pensionsViewModel, isPriorSubmission = false))
        authoriseAgentOrIndividual()
        urlPost(
          fullUrl(surchargeAmountUrl(taxYearEOY)),
          body = validForm,
          follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has a SEE_OTHER(303) status" in {

        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkUnauthorisedPaymentsCyaUrl(taxYearEOY)) shouldBe true

      }

      "updates surcharge amount" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.unauthorisedPayments.surchargeAmount shouldBe Some(BigDecimal(validAmount))
      }
    }

    "redirect to the first page in journey if submission is invalid in journey" in {
      val viewModel                      = anUnauthorisedPaymentsViewModel.copy(noSurchargeQuestion = None)
      val pensionUserData                = pensionsUserDataWithUnauthorisedPayments(viewModel, isPriorSubmission = false)
      val validAmount                    = "100.22"
      val validForm: Map[String, String] = Map(AmountForm.amount -> validAmount)
      lazy val result: WSResponse        = submitPage(pensionUserData, validForm)

      result.status shouldBe SEE_OTHER
      result.header("location").contains(unauthorisedPaymentsUrl(taxYearEOY))
    }

  }
} // scalastyle:on magic.number
