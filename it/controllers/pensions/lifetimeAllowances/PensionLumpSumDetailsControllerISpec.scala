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

package controllers.pensions.lifetimeAllowances

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
import utils.PageUrls.PensionLifetimeAllowance.pensionLumpSumDetails
import utils.PageUrls.{fullUrl, pensionSummaryUrl}
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

class PensionLumpSumDetailsControllerISpec extends IntegrationTest with ViewHelpers with BeforeAndAfterEach with PensionsDatabaseHelper {

  val newAmount = 25
  val newAmount2 = 30
  val poundPrefixText = "£"
  val amount1InputName = "amount-1"
  val amount2InputName = "amount-2"
  val amountInvalidFormat = "invalid"
  val amountEmpty = ""
  val amountOverMaximum = "100,000,000,000"


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


  def commonPageCheck(user : UserScenario[CommonExpectedResults, SpecificExpectedResults])(implicit document: () => Document): Unit = {
    titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
    h1Check(user.specificExpectedResults.get.expectedHeading)
    captionCheck(user.commonExpectedResults.expectedCaption(taxYearEOY), Selectors.captionSelector)
    textOnPageCheck(user.specificExpectedResults.get.checkThisWithProviderParagraph, Selectors.mainParagraph(2))
    textOnPageCheck(user.commonExpectedResults.beforeTax, Selectors.beforeTaxLabel)
    textOnPageCheck(user.specificExpectedResults.get.beforeTaxParagraph, Selectors.beforeTaxParagraph(4))
    textOnPageCheck(poundPrefixText, Selectors.poundPrefixSelector(2), "for amount 1")
    textOnPageCheck(poundPrefixText, Selectors.poundPrefixSelector(5), "for amount 2")
    textOnPageCheck(user.commonExpectedResults.hintText, Selectors.amount1hintTextSelector, "for amount 1")
    textOnPageCheck(user.commonExpectedResults.taxPaid, Selectors.taxPaidLabel)
    textOnPageCheck(user.specificExpectedResults.get.taxPaidParagraph, Selectors.taxPaidParagraph)
    textOnPageCheck(user.commonExpectedResults.hintText, Selectors.amount2hintTextSelector, "for amount 2")
    buttonCheck(user.commonExpectedResults.buttonText, Selectors.continueButtonSelector)
    formPostLinkCheck(pensionLumpSumDetails(taxYearEOY), Selectors.formSelector)
    welshToggleCheck(user.isWelsh)
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
    lazy val expectedHeading: String = expectedTitle
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
    val expectedCaption: Int => String = (taxYear: Int) => s"Lwfans blynyddol a lwfans oes ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val hintText = "Er enghraifft, £193.52"
    val buttonText = "Yn eich blaen"
    val beforeTax: String = "Cyfanswm cyn treth"
    val taxPaid: String = "Cyfanswm y dreth a dalwyd"
    val beforeTaxErrorIncorrectFormat: String = "Nodwch swm y cyfandaliad yn y fformat cywir"
    val beforeTaxErrorOverMaximum: String = "Mae’n rhaid i swm y lwfans oes fod yn llai na 100,000,000,000"
    val taxPaidErrorIncorrectFormat: String = "Nodwch swm y dreth lwfans oes yn y fformat cywir"
    val taxPaidErrorOverMaximum: String = "Mae’n rhaid i swm y dreth lwfans oes fod yn llai na 100,000,000,000"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Your pension lump sum"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val beforeTaxErrorNoEntry = "Enter the amount you took above your lifetime allowance as a lump sum"
    val taxPaidErrorNoEntry = "Enter the amount of lifetime allowance tax your pension provider paid or agreed to pay on the lump sum"
    val checkThisWithProviderParagraph = "Check with your pension providers if you’re unsure."
    val beforeTaxParagraph = "If you got a lump sum payment from more than one pension scheme, give the total."
    val taxPaidParagraph = "If more than one of your pension schemes paid lifetime allowance tax, give the total."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Eich cyfandaliad pensiwn"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val beforeTaxErrorNoEntry = "Nodwch y swm a gymeroch sy’n uwch na’ch lwfans oes fel cyfandaliad"
    val taxPaidErrorNoEntry = "Nodwch swm y dreth lwfans oes a dalodd eich darparwr pensiwn neu a gytunwyd i dalu ar y cyfandaliad"
    val checkThisWithProviderParagraph = "Gwiriwch â’ch darparwyr pensiwn os nad ydych yn siŵr."
    val beforeTaxParagraph = "Os cawsoch gyfandaliad pensiwn gan fwy nag un cynllun pensiwn, rhowch y cyfanswm."
    val taxPaidParagraph = "Os oedd mwy nag un o’ch cynlluniau pensiwn yn talu treth lwfans oes, rhowch y cyfanswm."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Your client’s pensions lump sum"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val beforeTaxErrorNoEntry = "Enter the amount your client took above their lifetime allowance as a lump sum"
    val taxPaidErrorNoEntry = "Enter the amount of lifetime allowance tax your client’s pension provider paid or agreed to pay on the lump sum"
    val checkThisWithProviderParagraph = "Your client can check with their pension provider if you’re unsure."
    val beforeTaxParagraph = "If your client got a lump sum payment from more than one pension scheme, give the total."
    val taxPaidParagraph = "If more than one of your client’s pension schemes paid lifetime allowance tax, give the total."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Cyfandaliad pensiwn eich cleient"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val beforeTaxErrorNoEntry = "Nodwch y swm a gymerodd eich cleient sy’n uwch na’u lwfans oes fel cyfandaliad"
    val taxPaidErrorNoEntry = "Nodwch swm y dreth lwfans oes a dalodd eich darparwr pensiwn neu a gytunwyd i dalu ar y cyfandaliad"
    val checkThisWithProviderParagraph = "Gall eich cleient wirio â’i ddarparwr pensiwn os nad ydych yn siŵr."
    val beforeTaxParagraph = "Os cafodd eich cleient gyfandaliad gan fwy nag un cynllun pensiwn, rhowch y cyfanswm."
    val taxPaidParagraph = "Os cafodd eich cleient gyfandaliad gan fwy nag un cynllun pensiwn, rhowch y cyfanswm."
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

          commonPageCheck(user)
          inputFieldValueCheck(amount1InputName, amount1inputSelector, "")
          inputFieldValueCheck(amount2InputName, amount2inputSelector, "")
        }

        "render render Your pension lump sum details page with prefilled value for before tax and tax paid" which {
          implicit lazy val result: WSResponse = {
            dropPensionsDB()
            val pensionsViewModel = aPensionLifetimeAllowanceViewModel.copy(
              pensionAsLumpSum = Some(LifetimeAllowance(Some(newAmount), Some(newAmount2)))
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
          commonPageCheck(user)
          inputFieldValueCheck(amount1InputName, amount1inputSelector, newAmount.toString)
          inputFieldValueCheck(amount2InputName, amount2inputSelector, newAmount2.toString)
        }

        "render render Your pension lump sum details page with prefilled value for before tax but not tax paid" which {
          implicit lazy val result: WSResponse = {
            dropPensionsDB()
            val pensionsViewModel = aPensionLifetimeAllowanceViewModel.copy(
              pensionAsLumpSum = Some(LifetimeAllowance(None, Some(newAmount2)))
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
          commonPageCheck(user)
          inputFieldValueCheck(amount1InputName, amount1inputSelector, "")
          inputFieldValueCheck(amount2InputName, amount2inputSelector, newAmount2.toString)
        }

        "render render Your pension lump sum details page with prefilled value for tax paid but not before tax" which {
          implicit lazy val result: WSResponse = {
            dropPensionsDB()
            val pensionsViewModel = aPensionLifetimeAllowanceViewModel.copy(
              pensionAsLumpSum = Some(LifetimeAllowance(Some(newAmount), None))
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
          commonPageCheck(user)
          inputFieldValueCheck(amount1InputName, amount1inputSelector, newAmount.toString)
          inputFieldValueCheck(amount2InputName, amount2inputSelector, "")
        }
      }
    }


    "redirect to the CYA page if there is no session data" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(pensionLumpSumDetails(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }
      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        //TODO: - Redirect to Annual Lifetime allowances cya page
        result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
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

          titleCheck(user.specificExpectedResults.get.expectedErrorTitle, user.isWelsh)
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

          titleCheck(user.specificExpectedResults.get.expectedErrorTitle, user.isWelsh)
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

          titleCheck(user.specificExpectedResults.get.expectedErrorTitle, user.isWelsh)
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
        cyaModel.pensions.pensionLifetimeAllowances.pensionAsLumpSum.flatMap(_.amount) shouldBe Some(newAmount)
        cyaModel.pensions.pensionLifetimeAllowances.pensionAsLumpSum.flatMap(_.taxPaid) shouldBe Some(newAmount2)
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
        cyaModel.pensions.pensionLifetimeAllowances.pensionAsLumpSum.flatMap(_.amount) shouldBe Some(newAmount)
        cyaModel.pensions.pensionLifetimeAllowances.pensionAsLumpSum.flatMap(_.taxPaid) shouldBe Some(newAmount2)
      }
    }

  }

}

