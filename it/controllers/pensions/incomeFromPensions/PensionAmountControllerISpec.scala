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

import builders.IncomeFromPensionsViewModelBuilder.{anIncomeFromPensionEmptyViewModel, anIncomeFromPensionsViewModel}
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
import utils.PageUrls.IncomeFromPensionsPages.{pensionAmountUrl, pensionStartDateUrl, ukPensionIncomeCyaUrl, ukPensionSchemeSummaryListUrl}
import utils.PageUrls.fullUrl
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
    val totalTaxErrorNoEntry: String
    val totalTaxErrorIncorrectFormat: String
    val totalTaxErrorOverMaximum: String
    val taxPaidErrorNoEntry: String
    val taxPaidErrorIncorrectFormat: String
    val taxPaidErrorOverMaximum: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedErrorTitle: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Income from pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val hintText = "For example, £193.52"
    val buttonText = "Continue"
    val totalTax: String = "Total amount this tax year"
    val taxPaid: String = "Tax paid"
    val totalTaxErrorNoEntry: String = "Enter the amount of pension paid"
    val totalTaxErrorIncorrectFormat: String = "Enter the amount of pension paid in the correct format"
    val totalTaxErrorOverMaximum: String = "The amount of pension paid must be less than £100,000,000,000"
    val taxPaidErrorNoEntry: String = "Enter the amount of tax paid"
    val taxPaidErrorIncorrectFormat: String = "Enter the amount of tax paid in the correct format"
    val taxPaidErrorOverMaximum: String = "The amount of tax paid must be less than £100,000,000,000"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Incwm o bensiynau ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val hintText = "Er enghraifft, £193.52"
    val buttonText = "Yn eich blaen"
    val totalTax: String = "Cyfanswm ar gyfer y flwyddyn dreth hon"
    val taxPaid: String = "Treth a dalwyd"
    val totalTaxErrorNoEntry: String = "Nodwch swm y pensiwn a dalwyd"
    val totalTaxErrorIncorrectFormat: String = "Nodwch swm y pensiwn a dalwyd, yn y fformat cywir"
    val totalTaxErrorOverMaximum: String = "Mae’n rhaid i swm y pensiwn a dalwyd fod yn llai na £100,000,000,000"
    val taxPaidErrorNoEntry: String = "Nodwch swm y dreth a dalwyd"
    val taxPaidErrorIncorrectFormat: String = "Nodwch swm y dreth a dalwyd yn y fformat cywir"
    val taxPaidErrorOverMaximum: String = "Mae’n rhaid i swm y dreth a dalwyd fod yn llai na £100,000,000,000"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "How much pension did you get paid?"
    val expectedHeading = "How much pension did you get paid?"
    val expectedErrorTitle = s"Error: $expectedTitle"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Faint o bensiwn a gawsoch chi?"
    val expectedHeading = "Faint o bensiwn a gawsoch chi?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "How much pension did your client get paid?"
    val expectedHeading = "How much pension did your client get paid?"
    val expectedErrorTitle = s"Error: $expectedTitle"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Faint o bensiwn a gafodd eich cleient?"
    val expectedHeading = "Faint o bensiwn a gafodd eich cleient?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
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

        "render How much pension did you get paid with no prefilled value for tax paid and total tax" which {
          lazy val pensionIncomeModel = UkPensionIncomeViewModel(
            amount = None, taxPaid = None)

          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(pensionsUserDataWithIncomeFromPensions(
              anIncomeFromPensionsViewModel.copy(uKPensionIncomesQuestion = Some(true), uKPensionIncomes = Seq(pensionIncomeModel))))
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

        "render How much pension did you get paid with prefilled value for total paid, and taxPaid is None" which {
          lazy val pensionIncomeModel = UkPensionIncomeViewModel(
            amount = Some(newAmount), taxPaid = None)
          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(pensionsUserDataWithIncomeFromPensions(
              anIncomeFromPensionsViewModel.copy(uKPensionIncomesQuestion = Some(true), uKPensionIncomes = Seq(pensionIncomeModel))))
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
          inputFieldValueCheck(amount1InputName, amount1inputSelector, newAmount.toString)
          inputFieldValueCheck(amount2InputName, amount2inputSelector, "")
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(pensionAmountUrl(taxYearEOY, Some(index)), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render How much pension did you get paid with total tax when total tax is none" which {
          lazy val pensionIncomeModel = UkPensionIncomeViewModel(
            amount = None, taxPaid = Some(newAmount))

          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(pensionsUserDataWithIncomeFromPensions(
              anIncomeFromPensionsViewModel.copy(uKPensionIncomesQuestion = Some(true), uKPensionIncomes = Seq(pensionIncomeModel))))
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
          inputFieldValueCheck(amount1InputName, amount1inputSelector, "")
          inputFieldValueCheck(amount2InputName, amount2inputSelector, newAmount.toString)
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(pensionAmountUrl(taxYearEOY, Some(index)), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render How much pension did you get paid with prefilled value for tax paid and total tax" which {
          lazy val pensionIncomeModel = UkPensionIncomeViewModel(
            amount = Some(newAmount), taxPaid = Some(newAmount2))

          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(pensionsUserDataWithIncomeFromPensions(
              anIncomeFromPensionsViewModel.copy(uKPensionIncomesQuestion = Some(true), uKPensionIncomes = Seq(pensionIncomeModel))))
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
          inputFieldValueCheck(amount1InputName, amount1inputSelector, newAmount.toString)
          inputFieldValueCheck(amount2InputName, amount2inputSelector, newAmount2.toString)
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(pensionAmountUrl(taxYearEOY, Some(index)), formSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "redirect to uk pension scheme summary list page if index is out of bound" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        val viewModel = anIncomeFromPensionEmptyViewModel.copy(uKPensionIncomesQuestion = Some(true), uKPensionIncomes = Seq(anUkPensionIncomeViewModelOne))
        insertCyaData(pensionsUserDataWithIncomeFromPensions(viewModel))
        urlGet(fullUrl(pensionAmountUrl(taxYearEOY, Some(2))), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(ukPensionSchemeSummaryListUrl(taxYearEOY)) shouldBe true
      }
    }

    "redirect to the CYA page if there is no session data" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        urlGet(fullUrl(pensionAmountUrl(taxYearEOY, Some(index))), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }
      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(ukPensionIncomeCyaUrl(taxYearEOY)) shouldBe true
      }
    }

    "redirect to Uk Pension Incomes Summary page if no index is given" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        insertCyaData(aPensionsUserData)
        urlGet(fullUrl(pensionAmountUrl(taxYearEOY, None)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(ukPensionSchemeSummaryListUrl(taxYearEOY))
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
            (totalTaxErrorNoEntry, expectedAmount1ErrorHref),
            (taxPaidErrorNoEntry, expectedAmount2ErrorHref)))
          errorAboveElementCheck(totalTaxErrorNoEntry, Some(amount1InputName))
          errorAboveElementCheck(taxPaidErrorNoEntry, Some(amount2InputName))
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
            (taxPaidErrorIncorrectFormat, expectedAmount2ErrorHref)))
          errorAboveElementCheck(totalTaxErrorIncorrectFormat, Some(amount1InputName))
          errorAboveElementCheck(taxPaidErrorIncorrectFormat, Some(amount2InputName))
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

    "redirect to the correct page when a valid amount is submitted when there is existing data" which {

      lazy val form: Map[String, String] = pensionAmountForm(newAmount.toString, newAmount2.toString)

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        val viewModel = anIncomeFromPensionsViewModel.copy(uKPensionIncomes = Seq(anUkPensionIncomeViewModelOne))
        insertCyaData(pensionsUserDataWithIncomeFromPensions(viewModel))
        urlPost(fullUrl(pensionAmountUrl(taxYearEOY, Some(index))), body = form,
          follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionStartDateUrl(taxYearEOY, Some(index)))
      }

      "update state pension amount to Some (new values)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.incomeFromPensions.uKPensionIncomes(index).amount shouldBe Some(newAmount)
        cyaModel.pensions.incomeFromPensions.uKPensionIncomes(index).taxPaid shouldBe Some(newAmount2)
      }
    }


    "redirect to the correct page when a valid amount is submitted when there is No existing data" which {

      lazy val form: Map[String, String] = pensionAmountForm(newAmount.toString, newAmount2.toString)

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        insertCyaData(pensionsUserDataWithIncomeFromPensions(anIncomeFromPensionEmptyViewModel.copy
        (uKPensionIncomesQuestion = Some(true), uKPensionIncomes = Seq(UkPensionIncomeViewModel(amount =
          None, taxPaid = None)))))

        urlPost(fullUrl(pensionAmountUrl(taxYearEOY, Some(index))), body = form,
          follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionStartDateUrl(taxYearEOY, Some(index)))
      }

      "update state pension amount to Some (new values)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.incomeFromPensions.uKPensionIncomes(index).amount shouldBe Some(newAmount)
        cyaModel.pensions.incomeFromPensions.uKPensionIncomes(index).taxPaid shouldBe Some(newAmount2)
      }
    }

    "redirect to pension scheme details page when there is No existing data and index is out of bound" which {
      lazy val form: Map[String, String] = pensionAmountForm(newAmount.toString, newAmount2.toString)
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        insertCyaData(pensionsUserDataWithIncomeFromPensions(anIncomeFromPensionEmptyViewModel.copy
        (uKPensionIncomesQuestion = Some(true), uKPensionIncomes = Seq(UkPensionIncomeViewModel(amount =
          None, taxPaid = None)))))

        urlPost(fullUrl(pensionAmountUrl(taxYearEOY, Some(2))), body = form,
          follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(ukPensionSchemeSummaryListUrl(taxYearEOY)) shouldBe true
      }
    }

  }

}

