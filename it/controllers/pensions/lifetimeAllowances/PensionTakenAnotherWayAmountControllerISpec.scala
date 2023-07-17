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

import builders.PensionLifetimeAllowancesViewModelBuilder.{aPensionLifetimeAllowancesViewModel, aPensionLifetimeAllowancesEmptyViewModel}
import builders.PensionsUserDataBuilder.{aPensionsUserData, pensionsUserDataWithLifetimeAllowance}
import builders.UserBuilder.aUserRequest
import forms.OptionalTupleAmountForm
import models.pension.charges.LifetimeAllowance
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.PensionLifetimeAllowance._
import utils.PageUrls.fullUrl
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

class PensionTakenAnotherWayAmountControllerISpec extends IntegrationTest with BeforeAndAfterEach with ViewHelpers with PensionsDatabaseHelper {
  val newAmount = 25
  val newAmount2 = 30
  val poundPrefixText = "£"
  val amount1InputName = "amount-1"
  val amount2InputName = "amount-2"
  val amountInvalidFormat = "invalid"
  val amountEmpty = ""
  val amountOverMaximum = "£100,000,000,000"


  def amountForm(totalAmount: String, taxPaid: String): Map[String, String] = Map(
    OptionalTupleAmountForm.amount -> totalAmount,
    OptionalTupleAmountForm.amount2 -> taxPaid
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
    val beforeTaxLabel = "#main-content > div > div > p.govuk-label.govuk-label--m"
    val taxPaidLabel = "#main-content > div > div > form > p.govuk-label.govuk-label--m"
    val taxPaidParagraphSelector = "#main-content > div > div > form > p.govuk-body"

    def mainParagraph(index: Int): String = s"#main-content > div > div > p:nth-child($index)"

    def beforeTaxParagraph(index: Int): String = s"#main-content > div > div > p:nth-child($index)"

    def poundPrefixSelector(index: Int): String = s"#main-content > div > div > form > div:nth-child($index) > div.govuk-input__wrapper > div"

  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val hintText: String
    val buttonText: String
    val beforeTax: String
    val taxPaid: String
    val taxPaidParagraph: String
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
    val amountAboveAllowanceParagraph: String
    val checkThisWithProviderParagraph: String
    val beforeTaxParagraph: String
    val beforeTaxErrorIncorrectFormat: String

  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Lifetime allowances for 6 April ${taxYear - 1} to 5 April $taxYear"
    val hintText = "For example, £193.52"
    val buttonText = "Continue"
    val beforeTax = "Total amount before tax"
    val taxPaid = "Total tax paid"
    val beforeTaxErrorOverMaximum = "The amount of lifetime allowance must be less than £100,000,000,000"
    val taxPaidErrorIncorrectFormat = "Enter the amount of lifetime allowance tax in the correct format"
    val taxPaidErrorOverMaximum = "The amount of lifetime allowance tax must be less than £100,000,000,000"
    val taxPaidParagraph = "If more than one pension scheme paid the lifetime allowance tax, give the total."
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Lifetime allowances for 6 April ${taxYear - 1} to 5 April $taxYear"
    val hintText = "Er enghraifft, £193.52"
    val buttonText = "Yn eich blaen"
    val beforeTax = "Cyfanswm cyn treth"
    val taxPaid = "Cyfanswm y dreth a dalwyd"
    val beforeTaxErrorOverMaximum = "Mae’n rhaid i swm y lwfans oes fod yn llai na £100,000,000,000"
    val taxPaidErrorIncorrectFormat = "Nodwch swm y dreth lwfans oes yn y fformat cywir"
    val taxPaidErrorOverMaximum = "Mae’n rhaid i swm y dreth lwfans oes fod yn llai na £100,000,000,000"
    val taxPaidParagraph = "Os oedd mwy nag un cynllun pensiwn yn talu’r dreth lwfans oes, rhowch y cyfanswm."

  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Your pension taken in another way"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val beforeTaxErrorNoEntry = "Enter the amount you took above your lifetime allowance in another way"
    val taxPaidErrorNoEntry = "Enter the amount of lifetime allowance tax your pension provider paid or agreed to pay on the amount taken in another way"
    val checkThisWithProviderParagraph = "Check with your pension providers if you’re unsure."
    val amountAboveAllowanceParagraph =
      "Tell us the amount above your lifetime allowance you’ve taken in other ways. This could be regular payments or a cash withdrawal."
    val beforeTaxParagraph = "If you got payments from more than one pension scheme, give the total."
    val beforeTaxErrorIncorrectFormat = "Enter the amount you took above your lifetime allowance in the correct format"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Eich pensiwn wedi’i gymryd mewn ffordd arall"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val beforeTaxErrorNoEntry = "Nodwch y swm a gymeroch sy’n uwch na’ch lwfans oes mewn ffordd arall"
    val taxPaidErrorNoEntry = "Nodwch swm y dreth lwfans oes a dalodd eich darparwr pensiwn neu a gytunwyd i dalu ar y swm a gymerir mewn ffordd arall"
    val checkThisWithProviderParagraph = "Gwiriwch â’ch darparwyr pensiwn os nad ydych yn siŵr."
    val amountAboveAllowanceParagraph =
      "Rhowch wybod i ni’r swm sy’n uwch na’ch lwfans oes rydych wedi’i gymryd mewn ffyrdd eraill. Gallai hyn fod yn daliadau rheolaidd neu’n tynnu’n ôl arian."
    val beforeTaxParagraph = "Os oeddech yn cael taliadau o fwy nag un cynllun pensiwn, rhowch y cyfanswm."
    val beforeTaxErrorIncorrectFormat = "Nodwch y swm a gymeroch sy’n uwch na’ch lwfans oes yn y fformat cywir"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Your client’s pension taken in another way"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val beforeTaxErrorNoEntry = "Enter the amount your client took above their lifetime allowance in another way"
    val taxPaidErrorNoEntry =
      "Enter the amount of lifetime allowance tax your client’s pension provider paid or agreed to pay on the amount taken in another way"
    val amountAboveAllowanceParagraph =
      "Tell us the amount above your client’s lifetime allowance they’ve taken in other ways. This could be regular payments or a cash withdrawal."
    val checkThisWithProviderParagraph = "Your client can check with their pension provider if you’re unsure."
    val beforeTaxParagraph = "If your client got payments from more than one pension scheme, give the total."
    val beforeTaxErrorIncorrectFormat = "Enter the amount your client took above their lifetime allowance in the correct format"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Pensiwn eich cleient wedi’i gymryd mewn ffordd arall"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val beforeTaxErrorNoEntry = "Nodwch y swm a gymerodd eich cleient sy’n uwch na’u lwfans oes mewn ffordd arall"
    val taxPaidErrorNoEntry =
      "Rhowch swm y dreth lwfans oes a dalodd darparwr pensiwn eich cleient neu a gytunwyd i dalu ar y swm a gymerwyd mewn ffordd arall"
    val amountAboveAllowanceParagraph =
      "Rhowch wybod i ni’r swm sy’n uwch na lwfans oes eich cleient maen nhw wedi’i gymryd mewn ffyrdd eraill. " +
        "Gallai hyn fod yn daliadau rheolaidd neu’n tynnu’n ôl arian."
    val checkThisWithProviderParagraph = "Gall eich cleient wirio â’i ddarparwr pensiwn os nad ydych yn siŵr."
    val beforeTaxParagraph = "Os oedd eich cleient yn cael taliadau o fwy nag un cynllun pensiwn, rhowch y cyfanswm."
    val beforeTaxErrorIncorrectFormat = "Nodwch y swm a gymerodd eich cleient sy’n uwch na’u lwfans oes yn y fformat cywir"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  ".show" should { //scalastyle:off magic.number
    userScenarios.foreach { user =>
      import Selectors._
      import user.commonExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "render pension taken another way page with no prefilled value for tax paid and before tax" which {

          implicit lazy val result: WSResponse = {
            dropPensionsDB()
            val pensionsViewModel = aPensionLifetimeAllowancesViewModel.copy(
              pensionPaidAnotherWay = None
            )
            insertCyaData(pensionsUserDataWithLifetimeAllowance(pensionsViewModel))
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(
              fullUrl(pensionTakenAnotherWayAmountUrl(taxYearEOY)),
              user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.amountAboveAllowanceParagraph, mainParagraph(2))
          textOnPageCheck(user.specificExpectedResults.get.checkThisWithProviderParagraph, mainParagraph(3))
          textOnPageCheck(beforeTax, beforeTaxLabel)
          textOnPageCheck(user.specificExpectedResults.get.beforeTaxParagraph, beforeTaxParagraph(5))
          textOnPageCheck(poundPrefixText, poundPrefixSelector(2), "for amount 1")
          textOnPageCheck(poundPrefixText, poundPrefixSelector(5), "for amount 2")
          textOnPageCheck(hintText, amount1hintTextSelector, "for amount 1")
          textOnPageCheck(taxPaid, taxPaidLabel)
          textOnPageCheck(taxPaidParagraph, taxPaidParagraphSelector)
          textOnPageCheck(hintText, amount2hintTextSelector, "for amount 2")
          inputFieldValueCheck(amount1InputName, amount1inputSelector, "")
          inputFieldValueCheck(amount2InputName, amount2inputSelector, "")
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(pensionTakenAnotherWayAmountUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render pension taken another way page with prefilled value for before tax and tax paid" which {
          implicit lazy val result: WSResponse = {
            dropPensionsDB()
            val pensionsViewModel = aPensionLifetimeAllowancesViewModel.copy(
              pensionPaidAnotherWay = Some(LifetimeAllowance(Some(newAmount), Some(newAmount2)))
            )
            insertCyaData(pensionsUserDataWithLifetimeAllowance(pensionsViewModel))
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(
              fullUrl(pensionTakenAnotherWayAmountUrl(taxYearEOY)),
              user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }
          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.amountAboveAllowanceParagraph, mainParagraph(2))
          textOnPageCheck(user.specificExpectedResults.get.checkThisWithProviderParagraph, mainParagraph(3))
          textOnPageCheck(beforeTax, beforeTaxLabel)
          textOnPageCheck(user.specificExpectedResults.get.beforeTaxParagraph, beforeTaxParagraph(5))
          textOnPageCheck(poundPrefixText, poundPrefixSelector(2), "for amount 1")
          textOnPageCheck(poundPrefixText, poundPrefixSelector(5), "for amount 2")
          textOnPageCheck(hintText, amount1hintTextSelector, "for amount 1")
          textOnPageCheck(taxPaid, taxPaidLabel)
          textOnPageCheck(taxPaidParagraph, taxPaidParagraphSelector)
          textOnPageCheck(hintText, amount2hintTextSelector, "for amount 2")
          inputFieldValueCheck(amount1InputName, amount1inputSelector, newAmount.toString)
          inputFieldValueCheck(amount2InputName, amount2inputSelector, newAmount2.toString)
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(pensionTakenAnotherWayAmountUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render pension taken another way page with prefilled value for before tax and None for tax paid" which {
          implicit lazy val result: WSResponse = {
            dropPensionsDB()
            val pensionsViewModel = aPensionLifetimeAllowancesViewModel.copy(
              pensionPaidAnotherWay = Some(LifetimeAllowance(Some(newAmount), None))
            )
            insertCyaData(pensionsUserDataWithLifetimeAllowance(pensionsViewModel))
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(
              fullUrl(pensionTakenAnotherWayAmountUrl(taxYearEOY)),
              user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }
          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.amountAboveAllowanceParagraph, mainParagraph(2))
          textOnPageCheck(user.specificExpectedResults.get.checkThisWithProviderParagraph, mainParagraph(3))
          textOnPageCheck(beforeTax, beforeTaxLabel)
          textOnPageCheck(user.specificExpectedResults.get.beforeTaxParagraph, beforeTaxParagraph(5))
          textOnPageCheck(poundPrefixText, poundPrefixSelector(2), "for amount 1")
          textOnPageCheck(poundPrefixText, poundPrefixSelector(5), "for amount 2")
          textOnPageCheck(hintText, amount1hintTextSelector, "for amount 1")
          textOnPageCheck(taxPaid, taxPaidLabel)
          textOnPageCheck(taxPaidParagraph, taxPaidParagraphSelector)
          textOnPageCheck(hintText, amount2hintTextSelector, "for amount 2")
          inputFieldValueCheck(amount1InputName, amount1inputSelector, newAmount.toString)
          inputFieldValueCheck(amount2InputName, amount2inputSelector, "")
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(pensionTakenAnotherWayAmountUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render pension taken another way page with None value for before tax and value for tax paid" which {
          implicit lazy val result: WSResponse = {
            dropPensionsDB()
            val pensionsViewModel = aPensionLifetimeAllowancesViewModel.copy(
              pensionPaidAnotherWay = Some(LifetimeAllowance(None, Some(newAmount)))
            )
            insertCyaData(pensionsUserDataWithLifetimeAllowance(pensionsViewModel))
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(
              fullUrl(pensionTakenAnotherWayAmountUrl(taxYearEOY)),
              user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }
          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.amountAboveAllowanceParagraph, mainParagraph(2))
          textOnPageCheck(user.specificExpectedResults.get.checkThisWithProviderParagraph, mainParagraph(3))
          textOnPageCheck(beforeTax, beforeTaxLabel)
          textOnPageCheck(user.specificExpectedResults.get.beforeTaxParagraph, beforeTaxParagraph(5))
          textOnPageCheck(poundPrefixText, poundPrefixSelector(2), "for amount 1")
          textOnPageCheck(poundPrefixText, poundPrefixSelector(5), "for amount 2")
          textOnPageCheck(hintText, amount1hintTextSelector, "for amount 1")
          textOnPageCheck(taxPaid, taxPaidLabel)
          textOnPageCheck(taxPaidParagraph, taxPaidParagraphSelector)
          textOnPageCheck(hintText, amount2hintTextSelector, "for amount 2")
          inputFieldValueCheck(amount1InputName, amount1inputSelector, "")
          inputFieldValueCheck(amount2InputName, amount2inputSelector, newAmount.toString)
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(pensionTakenAnotherWayAmountUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "redirect to the CYA page if there is no session data" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        urlGet(fullUrl(pensionTakenAnotherWayAmountUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }
      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(lifetimeAllowanceCYA(taxYearEOY))
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
            insertCyaData(aPensionsUserData)
            urlPost(fullUrl(pensionTakenAnotherWayAmountUrl(taxYearEOY)), body = emptyForm, welsh = user.isWelsh,
              follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }
          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedErrorTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.amountAboveAllowanceParagraph, mainParagraph(3))
          textOnPageCheck(user.specificExpectedResults.get.checkThisWithProviderParagraph, mainParagraph(4))
          textOnPageCheck(beforeTax, beforeTaxLabel)
          textOnPageCheck(user.specificExpectedResults.get.beforeTaxParagraph, beforeTaxParagraph(6))
          textOnPageCheck(poundPrefixText, poundPrefixSelector(1), "for amount 1")
          textOnPageCheck(poundPrefixText, poundPrefixSelector(4), "for amount 3")
          textOnPageCheck(hintText, amount1hintTextSelector, "for amount 1")
          textOnPageCheck(taxPaid, taxPaidLabel)
          textOnPageCheck(taxPaidParagraph, taxPaidParagraphSelector)
          textOnPageCheck(hintText, amount2hintTextSelector, "for amount 2")
          inputFieldValueCheck(amount1InputName, amount1inputSelector, amountEmpty)
          inputFieldValueCheck(amount2InputName, amount2inputSelector, amountEmpty)
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(pensionTakenAnotherWayAmountUrl(taxYearEOY), formSelector)
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
            insertCyaData(aPensionsUserData)
            urlPost(fullUrl(pensionTakenAnotherWayAmountUrl(taxYearEOY)), body = invalidFormatForm, welsh = user.isWelsh,
              follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedErrorTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.amountAboveAllowanceParagraph, mainParagraph(3))
          textOnPageCheck(user.specificExpectedResults.get.checkThisWithProviderParagraph, mainParagraph(4))
          textOnPageCheck(beforeTax, beforeTaxLabel)
          textOnPageCheck(user.specificExpectedResults.get.beforeTaxParagraph, beforeTaxParagraph(6))
          textOnPageCheck(poundPrefixText, poundPrefixSelector(1), "for amount 1")
          textOnPageCheck(poundPrefixText, poundPrefixSelector(4), "for amount 3")
          textOnPageCheck(hintText, amount1hintTextSelector, "for amount 1")
          textOnPageCheck(taxPaid, taxPaidLabel)
          textOnPageCheck(taxPaidParagraph, taxPaidParagraphSelector)
          textOnPageCheck(hintText, amount2hintTextSelector, "for amount 2")
          inputFieldValueCheck(amount1InputName, amount1inputSelector, amountInvalidFormat)
          inputFieldValueCheck(amount2InputName, amount2inputSelector, amountInvalidFormat)
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(pensionTakenAnotherWayAmountUrl(taxYearEOY), formSelector)
          multipleSummaryErrorCheck(List(
            (user.specificExpectedResults.get.beforeTaxErrorIncorrectFormat, expectedAmount1ErrorHref),
            (taxPaidErrorIncorrectFormat, expectedAmount2ErrorHref)))
          errorAboveElementCheck(user.specificExpectedResults.get.beforeTaxErrorIncorrectFormat, Some(amount1InputName))
          errorAboveElementCheck(taxPaidErrorIncorrectFormat, Some(amount2InputName))
          welshToggleCheck(user.isWelsh)
        }

        "return an error when form is submitted with input over maximum allowed value" which {

          lazy val overMaximumForm: Map[String, String] = amountForm(amountOverMaximum, amountOverMaximum)
          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(aPensionsUserData)
            urlPost(fullUrl(pensionTakenAnotherWayAmountUrl(taxYearEOY)), body = overMaximumForm, welsh = user.isWelsh,
              follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }
          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedErrorTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.amountAboveAllowanceParagraph, mainParagraph(3))
          textOnPageCheck(user.specificExpectedResults.get.checkThisWithProviderParagraph, mainParagraph(4))
          textOnPageCheck(beforeTax, beforeTaxLabel)
          textOnPageCheck(user.specificExpectedResults.get.beforeTaxParagraph, beforeTaxParagraph(6))
          textOnPageCheck(poundPrefixText, poundPrefixSelector(1), "for amount 1")
          textOnPageCheck(poundPrefixText, poundPrefixSelector(4), "for amount 3")
          textOnPageCheck(hintText, amount1hintTextSelector, "for amount 1")
          textOnPageCheck(taxPaid, taxPaidLabel)
          textOnPageCheck(taxPaidParagraph, taxPaidParagraphSelector)
          textOnPageCheck(hintText, amount2hintTextSelector, "for amount 2")
          inputFieldValueCheck(amount1InputName, amount1inputSelector, amountOverMaximum)
          inputFieldValueCheck(amount2InputName, amount2inputSelector, amountOverMaximum)
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(pensionTakenAnotherWayAmountUrl(taxYearEOY), formSelector)
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
        authoriseAgentOrIndividual()
        insertCyaData(pensionsUserDataWithLifetimeAllowance(aPensionLifetimeAllowancesViewModel))

        urlPost(fullUrl(pensionTakenAnotherWayAmountUrl(taxYearEOY)), body = form,
          follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(lifetimeAllowancePstrSummaryUrl(taxYearEOY))
      }

      "update state pension amount to Some (new values)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.pensionLifetimeAllowances.pensionPaidAnotherWay.flatMap(_.amount) shouldBe Some(newAmount)
        cyaModel.pensions.pensionLifetimeAllowances.pensionPaidAnotherWay.flatMap(_.taxPaid) shouldBe Some(newAmount2)
      }
    }

    "redirect to the correct page when a valid amount is submitted when there is No existing data" which {

      lazy val form: Map[String, String] = amountForm(newAmount.toString, newAmount2.toString)
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        insertCyaData(
          pensionsUserDataWithLifetimeAllowance(aPensionLifetimeAllowancesEmptyViewModel.copy(
            pensionPaidAnotherWayQuestion = Some(true),
            pensionPaidAnotherWay = None
          )))

        urlPost(fullUrl(pensionTakenAnotherWayAmountUrl(taxYearEOY)), body = form,
          follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionTaxReferenceNumberLifetimeAllowanceUrl(taxYearEOY))
      }

      "update state pension amount to Some (new values)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.pensionLifetimeAllowances.pensionPaidAnotherWay.flatMap(_.amount) shouldBe Some(newAmount)
        cyaModel.pensions.pensionLifetimeAllowances.pensionPaidAnotherWay.flatMap(_.taxPaid) shouldBe Some(newAmount2)
      }
    }

  }

}
