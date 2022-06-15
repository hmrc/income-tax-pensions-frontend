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

package controllers.pensions.lifetimeAllowance

import builders.PensionLifetimeAllowanceViewModelBuilder.{aPensionLifetimeAllowanceViewModel, aPensionLifetimeAllowancesEmptyViewModel}
import builders.PensionsUserDataBuilder.{aPensionsUserData, pensionsUserDataWithLifetimeAllowance}
import builders.UserBuilder.aUserRequest
import forms.TupleAmountForm
import models.pension.charges.LifetimeAllowance
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.IncomeFromPensionsPages.pensionAmountUrl
import utils.PageUrls.PensionLifetimeAllowance.pensionLumpSumDetails
import utils.PageUrls.{fullUrl, pensionSummaryUrl}
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

class PensionLumpSumDetailsControllerISpec extends IntegrationTest with ViewHelpers with BeforeAndAfterEach with PensionsDatabaseHelper {

  private val newAmount = 25
  private val newAmount2 = 30
  private val poundPrefixText = "£"
  private val amount1InputName = "amount-1"
  private val amount2InputName = "amount-2"
  private val index = 0
  private val amountInvalidFormat = "invalid"
  private val amountEmpty = ""
  private val amountOverMaximum = "100,000,000,000"


  def amountForm(totalAmount: String, taxPaid: String): Map[String, String] = Map(
    TupleAmountForm.amount -> totalAmount,
    TupleAmountForm.amount2 -> taxPaid
  )

  object Selectors {
    val captionSelector: String = "#main-content > div > div > header > p"
    val continueButtonSelector: String = "#continue"
    val formSelector: String = "#main-content > div > div > form"
    val amount1hintTextSelector = "#amount-1-hint"
    val amount2hintTextSelector = "#amount-2-hint"
    val amount1inputSelector = "#amount-1"
    val amount2inputSelector = "#amount-2"
    val expectedAmount1ErrorHref = "#amount-1"
    val expectedAmount2ErrorHref = "#amount-2"
    val poundPrefixSelector1 = ".govuk-input__prefix"
    val beforeTaxLabel = "#main-content > div > div > p.govuk-label.govuk-label--m"
    val taxPaidLabel = "#main-content > div > div > form > p.govuk-label.govuk-label--m"
    val taxPaidParagraph = "#main-content > div > div > form > p.govuk-body"

    def mainParagraph(index: Int): String = s"#main-content > div > div > p:nth-child($index)"

    def paragraphSelector(index: Int): String = s"#main-content > div > div > form > div:nth-child(1) > div:nth-child($index) > p"

    def beforeTaxParagraph(index: Int): String = s"#main-content > div > div > p:nth-child($index)"

    def poundPrefixSelector(index: Int): String = s"#main-content > div > div > form > div:nth-child($index) > div.govuk-input__wrapper > div"

  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val hintText: String
    val buttonText: String
    val beforeTax: String
    val taxPaid: String
    val beforeTaxErrorIncorrectFormat: String
    val beforeTaxErrorOverMaximum: String
    val taxPaidErrorIncorrectFormat: String
    val taxPaidErrorOverMaximum: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedErrorTitle: String
    val beforeTaxErrorNoEntry: String
    val taxPaidErrorNoEntry: String
    val checkThisWithProviderParagraph: String
    val beforeTaxParagraph: String
    val taxPaidParagraph: String
  }


  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Annual and lifetime allowances for 6 April ${taxYear - 1} to 5 April $taxYear"
    val hintText = "For example, £193.52"
    val buttonText = "Continue"
    val beforeTax: String = "Total amount before tax"
    val taxPaid: String = "Total tax paid"
    val beforeTaxErrorIncorrectFormat: String = "Enter the amount of lump sum in the correct format"
    val beforeTaxErrorOverMaximum: String = "The amount of lifetime allowance must be less than 100,000,000,000"
    val taxPaidErrorIncorrectFormat: String = "Enter the amount of lifetime allowance tax in the correct format"
    val taxPaidErrorOverMaximum: String = "The amount of lifetime allowance tax must be less than 100,000,000,000"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Annual and lifetime allowances for 6 April ${taxYear - 1} to 5 April $taxYear"
    val hintText = "For example, £193.52"
    val buttonText = "Continue"
    val beforeTax: String = "Total amount before tax"
    val taxPaid: String = "Total tax paid"
    val beforeTaxErrorIncorrectFormat: String = "Enter the amount of lump sum in the correct format"
    val beforeTaxErrorOverMaximum: String = "The amount of lifetime allowance must be less than 100,000,000,000"
    val taxPaidErrorIncorrectFormat: String = "Enter the amount of lifetime allowance tax in the correct format"
    val taxPaidErrorOverMaximum: String = "The amount of lifetime allowance tax must be less than 100,000,000,000"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Your pension lump sum"
    val expectedHeading = "Your pension lump sum"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val beforeTaxErrorNoEntry = "Enter the amount you took above your lifetime allowance as a lump sum"
    val taxPaidErrorNoEntry = "Enter the amount of lifetime allowance tax your pension provider paid or agreed to pay on the lump sum"
    val checkThisWithProviderParagraph = "Check with your pension providers if you’re unsure."
    val beforeTaxParagraph = "If you got a lump sum payment from more than one pension scheme, give the total."
    val taxPaidParagraph = "If more than one of your pension schemes paid lifetime allowance tax, give the total."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Your pension lump sum"
    val expectedHeading = "Your pension lump sum"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val beforeTaxErrorNoEntry = "Enter the amount you took above your lifetime allowance as a lump sum"
    val taxPaidErrorNoEntry = "Enter the amount of lifetime allowance tax your pension provider paid or agreed to pay on the lump sum"
    val checkThisWithProviderParagraph = "Check with your pension providers if you’re unsure."
    val beforeTaxParagraph = "If you got a lump sum payment from more than one pension scheme, give the total."
    val taxPaidParagraph = "If more than one of your pension schemes paid lifetime allowance tax, give the total."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Your client’s pensions lump sum"
    val expectedHeading = "Your client’s pensions lump sum"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val beforeTaxErrorNoEntry = "Enter the amount your client took above their lifetime allowance as a lump sum"
    val taxPaidErrorNoEntry = "Enter the amount of lifetime allowance tax your client’s pension provider paid or agreed to pay on the lump sum"
    val checkThisWithProviderParagraph = "Your client can check with their pension provider if you’re unsure."
    val beforeTaxParagraph = "If your client got a lump sum payment from more than one pension scheme, give the total."
    val taxPaidParagraph = "If more than one of your client’s pension schemes paid lifetime allowance tax, give the total."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Your client’s pensions lump sum"
    val expectedHeading = "Your client’s pensions lump sum"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val beforeTaxErrorNoEntry = "Enter the amount your client took above their lifetime allowance as a lump sum"
    val taxPaidErrorNoEntry = "Enter the amount of lifetime allowance tax your client’s pension provider paid or agreed to pay on the lump sum"
    val checkThisWithProviderParagraph = "Your client can check with their pension provider if you’re unsure."
    val beforeTaxParagraph = "If your client got a lump sum payment from more than one pension scheme, give the total."
    val taxPaidParagraph = "If more than one of your client’s pension schemes paid lifetime allowance tax, give the total."
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

        "render Your pension lump sum details page with no prefilled value for tax paid and before tax" which {

          implicit lazy val result: WSResponse = {
            dropPensionsDB()
            val pensionsViewModel = aPensionLifetimeAllowanceViewModel.copy(
              pensionAsLumpSum = None
            )
            insertCyaData(pensionsUserDataWithLifetimeAllowance(pensionsViewModel), aUserRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(
              fullUrl(pensionLumpSumDetails(taxYearEOY)),
              user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.checkThisWithProviderParagraph, mainParagraph(2))
          textOnPageCheck(beforeTax, beforeTaxLabel)
          textOnPageCheck(user.specificExpectedResults.get.beforeTaxParagraph, beforeTaxParagraph(4))
          textOnPageCheck(poundPrefixText, poundPrefixSelector(2), "for amount 1")
          textOnPageCheck(poundPrefixText, poundPrefixSelector(5), "for amount 2")
          textOnPageCheck(hintText, amount1hintTextSelector, "for amount 1")
          textOnPageCheck(taxPaid, taxPaidLabel)
          textOnPageCheck(user.specificExpectedResults.get.taxPaidParagraph, taxPaidParagraph)
          textOnPageCheck(hintText, amount2hintTextSelector, "for amount 2")
          inputFieldValueCheck(amount1InputName, amount1inputSelector, "")
          inputFieldValueCheck(amount2InputName, amount2inputSelector, "")
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(pensionLumpSumDetails(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render render Your pension lump sum details page with prefilled value for before tax and tax paid" which {
          implicit lazy val result: WSResponse = {
            dropPensionsDB()
            val pensionsViewModel = aPensionLifetimeAllowanceViewModel.copy(
              pensionAsLumpSum = Some(LifetimeAllowance(newAmount, newAmount2))
            )
            insertCyaData(pensionsUserDataWithLifetimeAllowance(pensionsViewModel), aUserRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(
              fullUrl(pensionLumpSumDetails(taxYearEOY)),
              user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }
          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.checkThisWithProviderParagraph, mainParagraph(2))
          textOnPageCheck(beforeTax, beforeTaxLabel)
          textOnPageCheck(user.specificExpectedResults.get.beforeTaxParagraph, beforeTaxParagraph(4))
          textOnPageCheck(poundPrefixText, poundPrefixSelector(2), "for amount 1")
          textOnPageCheck(poundPrefixText, poundPrefixSelector(5), "for amount 2")
          textOnPageCheck(hintText, amount1hintTextSelector, "for amount 1")
          textOnPageCheck(taxPaid, taxPaidLabel)
          textOnPageCheck(user.specificExpectedResults.get.taxPaidParagraph, taxPaidParagraph)
          textOnPageCheck(hintText, amount2hintTextSelector, "for amount 2")
          inputFieldValueCheck(amount1InputName, amount1inputSelector, newAmount.toString)
          inputFieldValueCheck(amount2InputName, amount2inputSelector, newAmount2.toString)
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(pensionLumpSumDetails(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }


    "redirect to the CYA page if there is no session data" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(pensionAmountUrl(taxYearEOY, index)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }
      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        //TODO: - Redirect to Annual Lifetime allowances cya page
        result.header("location").contains(pensionSummaryUrl(taxYearEOY)) shouldBe true
      }
    }
  }


  ".submit" should {

    userScenarios.foreach { user =>

      import Selectors._
      import user.commonExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "return an error when 'before tax' and 'tax paid' are submitted with no input entry" which {

          lazy val emptyForm: Map[String, String] = amountForm(amountEmpty, amountEmpty)
          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(aPensionsUserData, aUserRequest)
            urlPost(fullUrl(pensionLumpSumDetails(taxYearEOY)), body = emptyForm, welsh = user.isWelsh,
              follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }
          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.checkThisWithProviderParagraph, mainParagraph(3))
          textOnPageCheck(beforeTax, beforeTaxLabel)
          textOnPageCheck(user.specificExpectedResults.get.beforeTaxParagraph, beforeTaxParagraph(5))
          textOnPageCheck(poundPrefixText, poundPrefixSelector(1), "for amount 1")
          textOnPageCheck(poundPrefixText, poundPrefixSelector(4), "for amount 3")
          textOnPageCheck(hintText, amount1hintTextSelector, "for amount 1")
          textOnPageCheck(taxPaid, taxPaidLabel)
          textOnPageCheck(user.specificExpectedResults.get.taxPaidParagraph, taxPaidParagraph)
          textOnPageCheck(hintText, amount2hintTextSelector, "for amount 2")
          inputFieldValueCheck(amount1InputName, amount1inputSelector, amountEmpty)
          inputFieldValueCheck(amount2InputName, amount2inputSelector, amountEmpty)
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(pensionLumpSumDetails(taxYearEOY), formSelector)
          multipleSummaryErrorCheck(List(
            (user.specificExpectedResults.get.beforeTaxErrorNoEntry, expectedAmount1ErrorHref),
            (user.specificExpectedResults.get.taxPaidErrorNoEntry, expectedAmount2ErrorHref)))
          errorAboveElementCheck(user.specificExpectedResults.get.beforeTaxErrorNoEntry, Some(amount1InputName))
          errorAboveElementCheck(user.specificExpectedResults.get.taxPaidErrorNoEntry, Some(amount2InputName))
          welshToggleCheck(user.isWelsh)
        }

        "return an error when 'before tax' and 'tax paid' are submitted with an invalid format input" which {

          lazy val invalidFormatForm: Map[String, String] = amountForm(amountInvalidFormat, amountInvalidFormat)
          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(aPensionsUserData, aUserRequest)
            urlPost(fullUrl(pensionLumpSumDetails(taxYearEOY)), body = invalidFormatForm, welsh = user.isWelsh,
              follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.checkThisWithProviderParagraph, mainParagraph(3))
          textOnPageCheck(beforeTax, beforeTaxLabel)
          textOnPageCheck(user.specificExpectedResults.get.beforeTaxParagraph, beforeTaxParagraph(5))
          textOnPageCheck(poundPrefixText, poundPrefixSelector(1), "for amount 1")
          textOnPageCheck(poundPrefixText, poundPrefixSelector(4), "for amount 3")
          textOnPageCheck(hintText, amount1hintTextSelector, "for amount 1")
          textOnPageCheck(taxPaid, taxPaidLabel)
          textOnPageCheck(user.specificExpectedResults.get.taxPaidParagraph, taxPaidParagraph)
          textOnPageCheck(hintText, amount2hintTextSelector, "for amount 2")
          inputFieldValueCheck(amount1InputName, amount1inputSelector, amountInvalidFormat)
          inputFieldValueCheck(amount2InputName, amount2inputSelector, amountInvalidFormat)
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(pensionLumpSumDetails(taxYearEOY), formSelector)
          multipleSummaryErrorCheck(List(
            (beforeTaxErrorIncorrectFormat, expectedAmount1ErrorHref),
            (taxPaidErrorIncorrectFormat, expectedAmount2ErrorHref)))
          errorAboveElementCheck(beforeTaxErrorIncorrectFormat, Some(amount1InputName))
          errorAboveElementCheck(taxPaidErrorIncorrectFormat, Some(amount2InputName))
          welshToggleCheck(user.isWelsh)
        }

        "return an error when form is submitted with input over maximum allowed value" which {

          lazy val overMaximumForm: Map[String, String] = amountForm(amountOverMaximum, amountOverMaximum)
          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(aPensionsUserData, aUserRequest)
            urlPost(fullUrl(pensionLumpSumDetails(taxYearEOY)), body = overMaximumForm, welsh = user.isWelsh,
              follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }
          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.checkThisWithProviderParagraph, mainParagraph(3))
          textOnPageCheck(beforeTax, beforeTaxLabel)
          textOnPageCheck(user.specificExpectedResults.get.beforeTaxParagraph, beforeTaxParagraph(5))
          textOnPageCheck(poundPrefixText, poundPrefixSelector(1), "for amount 1")
          textOnPageCheck(poundPrefixText, poundPrefixSelector(4), "for amount 3")
          textOnPageCheck(hintText, amount1hintTextSelector, "for amount 1")
          textOnPageCheck(taxPaid, taxPaidLabel)
          textOnPageCheck(user.specificExpectedResults.get.taxPaidParagraph, taxPaidParagraph)
          textOnPageCheck(hintText, amount2hintTextSelector, "for amount 2")
          inputFieldValueCheck(amount1InputName, amount1inputSelector, amountOverMaximum)
          inputFieldValueCheck(amount2InputName, amount2inputSelector, amountOverMaximum)
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(pensionLumpSumDetails(taxYearEOY), formSelector)
          multipleSummaryErrorCheck(List(
            (beforeTaxErrorOverMaximum, expectedAmount1ErrorHref),
            (taxPaidErrorOverMaximum, expectedAmount2ErrorHref)))
          errorAboveElementCheck(beforeTaxErrorOverMaximum, Some(amount1InputName))
          errorAboveElementCheck(taxPaidErrorOverMaximum, Some(amount2InputName))
          welshToggleCheck(user.isWelsh)
        }

      }

    }

    "redirect to the correct page when a valid amount is submitted when there is existing data" which {

      lazy val form: Map[String, String] = amountForm(newAmount.toString, newAmount2.toString)

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(pensionsUserDataWithLifetimeAllowance(aPensionLifetimeAllowanceViewModel), aUserRequest)

        urlPost(fullUrl(pensionLumpSumDetails(taxYearEOY)), body = form,
          follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has a SEE_OTHER(303) status" in {
        //TODO: Redirect to lifetime-other-status
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
      }

      "update state pension amount to Some (new values)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.pensionLifetimeAllowances.pensionAsLumpSum.map(_.amount) shouldBe Some(newAmount)
        cyaModel.pensions.pensionLifetimeAllowances.pensionAsLumpSum.map(_.taxPaid) shouldBe Some(newAmount2)
      }
    }


    "redirect to the correct page when a valid amount is submitted when there is No existing data" which {

      lazy val form: Map[String, String] = amountForm(newAmount.toString, newAmount2.toString)

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(
          pensionsUserDataWithLifetimeAllowance(aPensionLifetimeAllowancesEmptyViewModel.copy(
            pensionAsLumpSumQuestion = Some(true),
            pensionAsLumpSum = None
          )), aUserRequest)

        urlPost(fullUrl(pensionLumpSumDetails(taxYearEOY)), body = form,
          follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has a SEE_OTHER(303) status" in {
        //TODO: Redirect to lifetime-other-status
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
      }

      "update state pension amount to Some (new values)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.pensionLifetimeAllowances.pensionAsLumpSum.map(_.amount) shouldBe Some(newAmount)
        cyaModel.pensions.pensionLifetimeAllowances.pensionAsLumpSum.map(_.taxPaid) shouldBe Some(newAmount2)
      }
    }

  }

}

