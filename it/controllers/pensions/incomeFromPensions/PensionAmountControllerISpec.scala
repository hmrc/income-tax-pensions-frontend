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

package controllers.pensions.incomeFromPensions

import builders.IncomeFromPensionsViewModelBuilder.{aUKIncomeFromPensionsViewModel, anIncomeFromPensionEmptyViewModel, anIncomeFromPensionsViewModel}
import builders.PensionsUserDataBuilder.{aPensionsUserData, pensionsUserDataWithIncomeFromPensions}
import builders.UkPensionIncomeViewModelBuilder.anUkPensionIncomeViewModelOne
import builders.UserBuilder.aUserRequest
import forms.OptionalTupleAmountForm
import models.pension.statebenefits.UkPensionIncomeViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.IncomeFromPensionsPages.{pensionAmountUrl, pensionSchemeSummaryUrl, pensionStartDateUrl, ukPensionSchemePayments}
import utils.PageUrls.{fullUrl, pensionSummaryUrl}
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

class PensionAmountControllerISpec extends IntegrationTest with ViewHelpers with BeforeAndAfterEach with PensionsDatabaseHelper {

  val newAmount = 25
  val newAmount2 = 30
  val poundPrefixText = "£"
  val amount1InputName = "amount-1"
  val amount2InputName = "amount-2"
  val index = 0
  val amountInvalidFormat = "invalid"
  val amountEmpty = ""
  val amountOverMaximum = "100,000,000,000"


  def pensionAmountForm(totalAmount: String, taxPaid: String): Map[String, String] = Map(
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
    val poundPrefixSelector1 = ".govuk-input__prefix"

    def labelIndex(index: Int): String = s"#main-content > div > div > form > div:nth-child($index) > label"

    def poundPrefixSelector(index: Int): String = s"#main-content > div > div > form > div:nth-child($index) > div.govuk-input__wrapper > div"
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val hintText: String
    val buttonText: String
    val totalTax: String
    val taxPaid: String
    val totalTaxErrorIncorrectFormat: String
    val totalTaxErrorOverMaximum: String
    val taxPaidErrorOverMaximum: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedErrorTitle: String
    val totalTaxErrorNoEntry: String
    val taxPaidErrorNoEntry: String
    val taxPaidErrorIncorrectFormat: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Income from pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val hintText = "For example, £193.52"
    val buttonText = "Continue"
    val totalTax: String = "Total amount this tax year"
    val taxPaid: String = "Tax paid"
    val totalTaxErrorIncorrectFormat: String = "Enter the amount of pension paid in pounds"
    val totalTaxErrorOverMaximum: String = "The amount of pension paid must be less than £100,000,000,000"
    val taxPaidErrorOverMaximum: String = "The amount of tax paid must be less than £100,000,000,000"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Incwm o bensiynau ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val hintText = "Er enghraifft, £193.52"
    val buttonText = "Yn eich blaen"
    val totalTax: String = "Cyfanswm ar gyfer y flwyddyn dreth hon"
    val taxPaid: String = "Treth a dalwyd"
    val totalTaxErrorIncorrectFormat: String = "Enter the amount of pension paid in pounds"
    val totalTaxErrorOverMaximum: String = "Mae’n rhaid i swm y pensiwn a dalwyd fod yn llai na £100,000,000,000"
    val taxPaidErrorOverMaximum: String = "Mae’n rhaid i swm y dreth a dalwyd fod yn llai na £100,000,000,000"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "How much pension did you get paid?"
    val expectedHeading = "How much pension did you get paid?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val totalTaxErrorNoEntry: String = "Enter the total amount of your pension income"
    val taxPaidErrorNoEntry: String = "Enter the amount of tax you paid"
    val taxPaidErrorIncorrectFormat: String = "Enter the total amount of tax paid in pounds"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Faint o bensiwn a gawsoch chi?"
    val expectedHeading = "Faint o bensiwn a gawsoch chi?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val totalTaxErrorNoEntry: String = "Enter the total amount of your pension income"
    val taxPaidErrorNoEntry: String = "Enter the amount of tax you paid"
    val taxPaidErrorIncorrectFormat: String = "Enter the total amount of tax paid in pounds"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "How much pension did your client get paid?"
    val expectedHeading = "How much pension did your client get paid?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val totalTaxErrorNoEntry: String = "Enter the total amount of your client’s pension income"
    val taxPaidErrorNoEntry: String = "Enter the amount of tax paid by your client"
    val taxPaidErrorIncorrectFormat: String = "Enter the total amount of tax paid by your client in pounds"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Faint o bensiwn a gafodd eich cleient?"
    val expectedHeading = "Faint o bensiwn a gafodd eich cleient?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val totalTaxErrorNoEntry: String = "Enter the total amount of your client’s pension income"
    val taxPaidErrorNoEntry: String = "Enter the amount of tax paid by your client"
    val taxPaidErrorIncorrectFormat: String = "Enter the total amount of tax paid by your client in pounds"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))


  ".show" should {
    userScenarios.foreach { user =>
      import Selectors._
      import user.commonExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "render page with no prefilled values" which {
          lazy val pensionIncomeModel = aUKIncomeFromPensionsViewModel.copy(uKPensionIncomes = Seq(anUkPensionIncomeViewModelOne.copy(
            amount = None, taxPaid = None, startDate = None)))
          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(pensionsUserDataWithIncomeFromPensions(pensionIncomeModel))
            urlGet(fullUrl(pensionAmountUrl(taxYearEOY, Some(index))), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }
          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(totalTax, labelIndex(2))
          textOnPageCheck(poundPrefixText, poundPrefixSelector(2), "for amount 1")
          textOnPageCheck(poundPrefixText, poundPrefixSelector(3), "for amount 2")
          textOnPageCheck(hintText, amount1hintTextSelector, "for amount 1")
          textOnPageCheck(taxPaid, labelIndex(3))
          textOnPageCheck(hintText, amount2hintTextSelector, "for amount 2")
          inputFieldValueCheck(amount1InputName, amount1inputSelector, "")
          inputFieldValueCheck(amount2InputName, amount2inputSelector, "")
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(pensionAmountUrl(taxYearEOY, Some(index)), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render page with prefilled values" which {
          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(pensionsUserDataWithIncomeFromPensions(aUKIncomeFromPensionsViewModel))
            urlGet(fullUrl(pensionAmountUrl(taxYearEOY, Some(index))), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }
          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(totalTax, labelIndex(2))
          textOnPageCheck(poundPrefixText, poundPrefixSelector(2), "for amount 1")
          textOnPageCheck(hintText, amount1hintTextSelector, "for amount 1")
          textOnPageCheck(taxPaid, labelIndex(3))
          textOnPageCheck(hintText, amount2hintTextSelector, "for amount 2")
          textOnPageCheck(poundPrefixText, poundPrefixSelector(3), "for amount 2")
          inputFieldValueCheck(amount1InputName, amount1inputSelector, "211.33")
          inputFieldValueCheck(amount2InputName, amount2inputSelector, "14.77")
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(pensionAmountUrl(taxYearEOY, Some(index)), formSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "redirect to the Pensions Summary page if there is no session data" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        urlGet(fullUrl(pensionAmountUrl(taxYearEOY, Some(index))), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }
      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(pensionSummaryUrl(taxYearEOY)) shouldBe true
      }
    }

    "redirect to the first page in journey" when {
      "page is invalid in journey" which {
        val invalidJourney = anIncomeFromPensionEmptyViewModel.copy(uKPensionIncomesQuestion = Some(false))
        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual()
          insertCyaData(pensionsUserDataWithIncomeFromPensions(invalidJourney))
          urlGet(fullUrl(pensionAmountUrl(taxYearEOY, Some(0))), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(ukPensionSchemePayments(taxYearEOY))
        }
      }

      "previous questions are unanswered" which {
        val incompleteJourney = aUKIncomeFromPensionsViewModel.copy(
          uKPensionIncomes = Seq(anUkPensionIncomeViewModelOne.copy(pensionSchemeName = None)))
        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual()
          insertCyaData(pensionsUserDataWithIncomeFromPensions(incompleteJourney))
          urlGet(fullUrl(pensionAmountUrl(taxYearEOY, Some(0))), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(ukPensionSchemePayments(taxYearEOY))
        }
      }

      "index is None" which {
        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual()
          insertCyaData(aPensionsUserData)
          urlGet(fullUrl(pensionAmountUrl(taxYearEOY, None)), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(ukPensionSchemePayments(taxYearEOY))
        }
      }

      "index is invalid" which {
        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual()
          val viewModel = anIncomeFromPensionEmptyViewModel.copy(uKPensionIncomesQuestion = Some(true), uKPensionIncomes = Seq(anUkPensionIncomeViewModelOne))
          insertCyaData(pensionsUserDataWithIncomeFromPensions(viewModel))
          urlGet(fullUrl(pensionAmountUrl(taxYearEOY, Some(8))), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        "has an SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(ukPensionSchemePayments(taxYearEOY))
        }
      }
    }
  }


  ".submit" should {

    userScenarios.foreach { user =>

      import Selectors._
      import user.commonExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "return an error when tax paid is submitted with no input entry" which {

          lazy val emptyForm: Map[String, String] = pensionAmountForm(amountEmpty, amountEmpty)
          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(aPensionsUserData)
            urlPost(fullUrl(pensionAmountUrl(taxYearEOY, Some(index))), body = emptyForm, welsh = user.isWelsh,
              follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }
          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedErrorTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(totalTax, labelIndex(1))
          textOnPageCheck(taxPaid, labelIndex(2))
          textOnPageCheck(poundPrefixText, poundPrefixSelector(1), "for amount 1")
          textOnPageCheck(poundPrefixText, poundPrefixSelector(2), "for amount 2")
          textOnPageCheck(hintText, amount1hintTextSelector, "for amount 1")
          textOnPageCheck(hintText, amount2hintTextSelector, "for amount 2")
          inputFieldValueCheck(amount1InputName, amount1inputSelector, amountEmpty)
          inputFieldValueCheck(amount2InputName, amount2inputSelector, amountEmpty)
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(pensionAmountUrl(taxYearEOY, Some(index)), formSelector)
          multipleSummaryErrorCheck(List(
            (user.specificExpectedResults.get.totalTaxErrorNoEntry, expectedAmount1ErrorHref),
            (user.specificExpectedResults.get.taxPaidErrorNoEntry, expectedAmount2ErrorHref)))
          errorAboveElementCheck(user.specificExpectedResults.get.totalTaxErrorNoEntry, Some(amount1InputName))
          errorAboveElementCheck(user.specificExpectedResults.get.taxPaidErrorNoEntry, Some(amount2InputName))
          welshToggleCheck(user.isWelsh)
        }

        "return an error when total tax is submitted with an invalid format input" which {

          lazy val invalidFormatForm: Map[String, String] = pensionAmountForm(amountInvalidFormat, amountInvalidFormat)
          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(aPensionsUserData)
            urlPost(fullUrl(pensionAmountUrl(taxYearEOY, Some(index))), body = invalidFormatForm, welsh = user.isWelsh,
              follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedErrorTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(totalTax, labelIndex(1))
          textOnPageCheck(taxPaid, labelIndex(2))
          textOnPageCheck(poundPrefixText, poundPrefixSelector(1), "for amount 1")
          textOnPageCheck(poundPrefixText, poundPrefixSelector(2), "for amount 2")
          textOnPageCheck(hintText, amount1hintTextSelector, "for amount 1")
          textOnPageCheck(hintText, amount2hintTextSelector, "for amount 2")
          inputFieldValueCheck(amount1InputName, amount1inputSelector, amountInvalidFormat)
          inputFieldValueCheck(amount2InputName, amount2inputSelector, amountInvalidFormat)
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(pensionAmountUrl(taxYearEOY, Some(index)), formSelector)
          multipleSummaryErrorCheck(List(
            (totalTaxErrorIncorrectFormat, expectedAmount1ErrorHref),
            (user.specificExpectedResults.get.taxPaidErrorIncorrectFormat, expectedAmount2ErrorHref)))
          errorAboveElementCheck(totalTaxErrorIncorrectFormat, Some(amount1InputName))
          errorAboveElementCheck(user.specificExpectedResults.get.taxPaidErrorIncorrectFormat, Some(amount2InputName))
          welshToggleCheck(user.isWelsh)
        }

        "return an error when form is submitted with input over maximum allowed value" which {

          lazy val overMaximumForm: Map[String, String] = pensionAmountForm(amountOverMaximum, amountOverMaximum)
          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(aPensionsUserData)
            urlPost(fullUrl(pensionAmountUrl(taxYearEOY, Some(index))), body = overMaximumForm, welsh = user.isWelsh,
              follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }
          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedErrorTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(totalTax, labelIndex(1))
          textOnPageCheck(taxPaid, labelIndex(2))
          textOnPageCheck(poundPrefixText, poundPrefixSelector(1), "for amount 1")
          textOnPageCheck(poundPrefixText, poundPrefixSelector(2), "for amount 2")
          textOnPageCheck(hintText, amount1hintTextSelector, "for amount 1")
          textOnPageCheck(hintText, amount2hintTextSelector, "for amount 2")
          inputFieldValueCheck(amount1InputName, amount1inputSelector, amountOverMaximum)
          inputFieldValueCheck(amount2InputName, amount2inputSelector, amountOverMaximum)
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(pensionAmountUrl(taxYearEOY, Some(index)), formSelector)
          multipleSummaryErrorCheck(List(
            (totalTaxErrorOverMaximum, expectedAmount1ErrorHref),
            (taxPaidErrorOverMaximum, expectedAmount2ErrorHref)))
          errorAboveElementCheck(totalTaxErrorOverMaximum, Some(amount1InputName))
          errorAboveElementCheck(taxPaidErrorOverMaximum, Some(amount2InputName))
          welshToggleCheck(user.isWelsh)
        }

      }

    }

    "redirect to the PensionSchemeStartDate page when a valid amount is submitted" which {
      lazy val form: Map[String, String] = pensionAmountForm(newAmount.toString, newAmount2.toString)
      val scheme = UkPensionIncomeViewModel(
        pensionSchemeName = Some("pension name 2"),
        pensionId = Some("Some hmrc ref 1"),
        pensionSchemeRef = Some("777/77777")
      )

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        insertCyaData(pensionsUserDataWithIncomeFromPensions(anIncomeFromPensionsViewModel.copy(uKPensionIncomes = Seq(scheme))))
        urlPost(fullUrl(pensionAmountUrl(taxYearEOY, Some(index))), body = form,
          follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionStartDateUrl(taxYearEOY, Some(index)))
      }

      "update state pension amount to Some(new values)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.incomeFromPensions.uKPensionIncomes(index) shouldBe scheme.copy(
          amount = Some(newAmount), taxPaid = Some(newAmount2)
        )
      }
    }

    "redirect to the Scheme Summary page when a valid amount is submitted, completing the scheme" which {
      lazy val form: Map[String, String] = pensionAmountForm(newAmount.toString, newAmount2.toString)
      val viewModel = anIncomeFromPensionsViewModel.copy(uKPensionIncomes = Seq(anUkPensionIncomeViewModelOne))

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        insertCyaData(pensionsUserDataWithIncomeFromPensions(viewModel))
        urlPost(fullUrl(pensionAmountUrl(taxYearEOY, Some(index))), body = form,
          follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionSchemeSummaryUrl(taxYearEOY, Some(index)))
      }

      "update state pension amount to Some(new values)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.incomeFromPensions.uKPensionIncomes(index) shouldBe anUkPensionIncomeViewModelOne.copy(
          amount = Some(newAmount), taxPaid = Some(newAmount2)
        )
      }
    }

    "redirect to the first page in journey" when {
      "page is invalid in journey" which {
        lazy val form: Map[String, String] = pensionAmountForm(newAmount.toString, newAmount2.toString)
        val invalidJourney = anIncomeFromPensionEmptyViewModel.copy(uKPensionIncomesQuestion = Some(false))
        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual()
          insertCyaData(pensionsUserDataWithIncomeFromPensions(invalidJourney))
          urlPost(fullUrl(pensionAmountUrl(taxYearEOY, Some(0))), body = form,
            follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(ukPensionSchemePayments(taxYearEOY))
        }
      }

      "previous questions are unanswered" which {
        lazy val form: Map[String, String] = pensionAmountForm(newAmount.toString, newAmount2.toString)
        val incompleteJourney = aUKIncomeFromPensionsViewModel.copy(
          uKPensionIncomes = Seq(anUkPensionIncomeViewModelOne.copy(pensionSchemeName = None)))
        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual()
          insertCyaData(pensionsUserDataWithIncomeFromPensions(incompleteJourney))
          urlPost(fullUrl(pensionAmountUrl(taxYearEOY, Some(0))), body = form,
            follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(ukPensionSchemePayments(taxYearEOY))
        }
      }

      "index is out of bound" which {
        lazy val form: Map[String, String] = pensionAmountForm(newAmount.toString, newAmount2.toString)
        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual()
          insertCyaData(pensionsUserDataWithIncomeFromPensions(aUKIncomeFromPensionsViewModel))

          urlPost(fullUrl(pensionAmountUrl(taxYearEOY, Some(2))), body = form,
            follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        "has an SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(ukPensionSchemePayments(taxYearEOY))
        }
      }
    }

  }

}

