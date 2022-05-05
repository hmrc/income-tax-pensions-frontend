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

package controllers.pensions.incomeFromPensions

import builders.IncomeFromPensionsViewModelBuilder.anIncomeFromPensionsViewModel
import builders.PensionsUserDataBuilder.pensionsUserDataWithIncomeFromPensions
import builders.StateBenefitViewModelBuilder.anStateBenefitViewModelTwo
import builders.UserBuilder.aUserRequest
import forms.AmountForm
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.IncomeFromPensionsPages.{taxOnLumpSumAmountUrl, taxOnLumpSumUrl}
import utils.PageUrls.{fullUrl, overviewUrl, pensionSummaryUrl}
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

class TaxPaidOnLumpSumAmountControllerISpec extends IntegrationTest with ViewHelpers with BeforeAndAfterEach with PensionsDatabaseHelper {

  private val poundPrefixText = "£"
  private val amountInputName = "amount"
  private val newAmount = 123.99
  lazy val validForm: Map[String, String] = Map(AmountForm.amount -> s"$newAmount")

  object Selectors {
    val captionSelector: String = "#main-content > div > div > header > p"
    val continueButtonSelector: String = "#continue"
    val formSelector: String = "#main-content > div > div > form"
    val hintTextSelector = "#amount-hint"
    val poundPrefixSelector = ".govuk-input__prefix"
    val inputSelector = "#amount"
    val expectedErrorHref = "#amount"
    val paragraphSelector = "#main-content > div > div > p"
    val detailsSelector: String = "#main-content > div > div > form > details > summary > span"
    val detailsYouCanFindThisSelector = "#main-content > div > div > form > details > div > p"

    def bulletSelector(index: Int): String = s"#main-content > div > div > form > details > div > ul > li:nth-child($index)"
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val hintText: String
    val buttonText: String
    val expectedWhereToFindThisInformation: String
    val expectedYouCanFindThisInformationIn: String
    val emptyErrorText: String
    val invalidFormatErrorText: String
    val maxAmountErrorText: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedErrorTitle: String
    val expectedParagraphText: String
    val expectedDetailsExample1: String
    val expectedDetailsExample2: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Income from pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val hintText = "For example, £193.52"
    val buttonText = "Continue"
    val expectedWhereToFindThisInformation = "Where to find this information"
    val expectedYouCanFindThisInformationIn = "You can find this information in:"
    val emptyErrorText = "Enter the amount of tax paid on the State Pension lump sum"
    val invalidFormatErrorText = "Enter the amount of tax paid on the State Pension lump sum in the correct format"
    val maxAmountErrorText = "The amount of tax paid on the State Pension lump sum must be less than £100,000,000,000"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Income from pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val hintText = "For example, £193.52"
    val buttonText = "Continue"
    val expectedWhereToFindThisInformation = "Where to find this information"
    val expectedYouCanFindThisInformationIn = "You can find this information in:"
    val emptyErrorText = "Enter the amount of tax paid on the State Pension lump sum"
    val invalidFormatErrorText = "Enter the amount of tax paid on the State Pension lump sum in the correct format"
    val maxAmountErrorText = "The amount of tax paid on the State Pension lump sum must be less than £100,000,000,000"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "How much tax did you pay on the State Pension lump sum?"
    val expectedHeading = "How much tax did you pay on the State Pension lump sum?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedDetailsExample1 = "your P60"
    val expectedDetailsExample2 = "the ‘About general increases in benefits’ letter the Pension Service sent you"
    val expectedParagraphText = s"You told us you did not pay £$newAmount tax on your State Pension lump sum this year. Tell us how much you paid."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "How much tax did you pay on the State Pension lump sum?"
    val expectedHeading = "How much tax did you pay on the State Pension lump sum?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedDetailsExample1 = "your P60"
    val expectedDetailsExample2 = "the ‘About general increases in benefits’ letter the Pension Service sent you"
    val expectedParagraphText = s"You told us you did not pay £$newAmount tax on your State Pension lump sum this year. Tell us how much you paid."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "How much tax did your client pay on the State Pension lump sum?"
    val expectedHeading = "How much tax did your client pay on the State Pension lump sum?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedDetailsExample1 = "your client’s P60"
    val expectedDetailsExample2 = "the ‘About general increases in benefits’ letter the Pension Service sent your client"
    val expectedParagraphText: String = s"You told us your client did not pay £$newAmount tax on their State Pension lump sum this year. " +
      s"Tell us how much your client paid."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "How much tax did your client pay on the State Pension lump sum?"
    val expectedHeading = "How much tax did your client pay on the State Pension lump sum?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedDetailsExample1 = "your client’s P60"
    val expectedDetailsExample2 = "the ‘About general increases in benefits’ letter the Pension Service sent your client"
    val expectedParagraphText: String = s"You told us your client did not pay £$newAmount tax on their State Pension lump sum this year. " +
      s"Tell us how much your client paid."
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
        "render the 'Tax Paid on Lump Sum Amount' page with no prefilling when no cya data" which {
          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            val viewModel = anIncomeFromPensionsViewModel.copy(statePensionLumpSum = Some(anStateBenefitViewModelTwo.copy(taxPaid = None)))
            insertCyaData(pensionsUserDataWithIncomeFromPensions(viewModel), aUserRequest)
            urlGet(fullUrl(taxOnLumpSumAmountUrl(taxYearEOY)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }
          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          textOnPageCheck(hintText, hintTextSelector)
          elementNotOnPageCheck(paragraphSelector)
          textOnPageCheck(expectedWhereToFindThisInformation, detailsSelector)
          textOnPageCheck(expectedYouCanFindThisInformationIn, detailsYouCanFindThisSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedDetailsExample1, bulletSelector(1))
          textOnPageCheck(user.specificExpectedResults.get.expectedDetailsExample2, bulletSelector(2))
          inputFieldValueCheck(amountInputName, inputSelector, "")
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(taxOnLumpSumAmountUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the 'Tax Paid on Lump Sum Amount' page with prefilled amount and replay content" which {
          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            val viewModel = anIncomeFromPensionsViewModel.copy(statePensionLumpSum = Some(anStateBenefitViewModelTwo.copy(taxPaid = Some(newAmount))))
            insertCyaData(pensionsUserDataWithIncomeFromPensions(viewModel), aUserRequest)
            urlGet(fullUrl(taxOnLumpSumAmountUrl(taxYearEOY)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }
          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraphText, paragraphSelector)
          textOnPageCheck(expectedWhereToFindThisInformation, detailsSelector)
          textOnPageCheck(expectedYouCanFindThisInformationIn, detailsYouCanFindThisSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedDetailsExample1, bulletSelector(1))
          textOnPageCheck(user.specificExpectedResults.get.expectedDetailsExample2, bulletSelector(2))
          inputFieldValueCheck(amountInputName, inputSelector, s"$newAmount")
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(taxOnLumpSumAmountUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "redirect to the overview page when it is in year" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val viewModel = anIncomeFromPensionsViewModel.copy(statePensionLumpSum = Some(anStateBenefitViewModelTwo.copy(taxPaid = None)))
        insertCyaData(pensionsUserDataWithIncomeFromPensions(viewModel), aUserRequest)
        urlGet(fullUrl(taxOnLumpSumAmountUrl(taxYear)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has an SEE_OTHER(303 status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(overviewUrl(taxYear))
      }
    }

    "redirect to the 'Tax Paid on Lump Sum' Question page if it has been answered false" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val viewModel = anIncomeFromPensionsViewModel.copy(statePensionLumpSum = Some(anStateBenefitViewModelTwo.copy(
          taxPaidQuestion = Some(false), taxPaid = None)))
        insertCyaData(pensionsUserDataWithIncomeFromPensions(viewModel), aUserRequest)
        urlGet(fullUrl(taxOnLumpSumAmountUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an SEE_OTHER(303 status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(taxOnLumpSumUrl(taxYearEOY))
      }
    }

    "redirect to the Income from Pensions CYA page if there is no session data" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(taxOnLumpSumAmountUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an SEE_OTHER(303 status" in {
        result.status shouldBe SEE_OTHER
        //TODO redirect to Income From Pension CYA Page
        result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
      }

    }
  }

  ".submit" should {
    userScenarios.foreach { user =>
      import Selectors._
      import user.commonExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        "return an error when form is submitted with no input entry" which {
          val emptyAmount = ""

          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(isAgent = user.isAgent)
            val viewModel = anIncomeFromPensionsViewModel.copy(statePensionLumpSum = Some(anStateBenefitViewModelTwo.copy(taxPaid = None)))
            insertCyaData(pensionsUserDataWithIncomeFromPensions(viewModel), aUserRequest)
            urlPost(fullUrl(taxOnLumpSumAmountUrl(taxYearEOY)), welsh = user.isWelsh, body = Map(AmountForm.amount -> emptyAmount),
              follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          "has a BAD_REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          textOnPageCheck(hintText, hintTextSelector)
          elementNotOnPageCheck(paragraphSelector)
          textOnPageCheck(expectedWhereToFindThisInformation, detailsSelector)
          textOnPageCheck(expectedYouCanFindThisInformationIn, detailsYouCanFindThisSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedDetailsExample1, bulletSelector(1))
          textOnPageCheck(user.specificExpectedResults.get.expectedDetailsExample2, bulletSelector(2))
          inputFieldValueCheck(amountInputName, inputSelector, s"$emptyAmount")
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(taxOnLumpSumAmountUrl(taxYearEOY), formSelector)
          errorSummaryCheck(emptyErrorText, expectedErrorHref)
          errorAboveElementCheck(emptyErrorText)
          welshToggleCheck(user.isWelsh)

        }

        "return an error when form is submitted with an invalid format input" which {
          val invalidAmount = "invalid"

          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(isAgent = user.isAgent)
            val viewModel = anIncomeFromPensionsViewModel.copy(statePensionLumpSum = Some(anStateBenefitViewModelTwo.copy(taxPaid = None)))
            insertCyaData(pensionsUserDataWithIncomeFromPensions(viewModel), aUserRequest)
            urlPost(fullUrl(taxOnLumpSumAmountUrl(taxYearEOY)), welsh = user.isWelsh, body = Map(AmountForm.amount -> invalidAmount),
              follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          "has a BAD_REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          textOnPageCheck(hintText, hintTextSelector)
          elementNotOnPageCheck(paragraphSelector)
          textOnPageCheck(expectedWhereToFindThisInformation, detailsSelector)
          textOnPageCheck(expectedYouCanFindThisInformationIn, detailsYouCanFindThisSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedDetailsExample1, bulletSelector(1))
          textOnPageCheck(user.specificExpectedResults.get.expectedDetailsExample2, bulletSelector(2))
          inputFieldValueCheck(amountInputName, inputSelector, s"$invalidAmount")
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(taxOnLumpSumAmountUrl(taxYearEOY), formSelector)
          errorSummaryCheck(invalidFormatErrorText, expectedErrorHref)
          errorAboveElementCheck(invalidFormatErrorText)
          welshToggleCheck(user.isWelsh)
        }

        "return an error when form is submitted with input over maximum allowed value" which {
          val overMaxAmount = "100,000,000,000"

          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(isAgent = user.isAgent)
            val viewModel = anIncomeFromPensionsViewModel.copy(statePensionLumpSum = Some(anStateBenefitViewModelTwo.copy(taxPaid = None)))
            insertCyaData(pensionsUserDataWithIncomeFromPensions(viewModel), aUserRequest)
            urlPost(fullUrl(taxOnLumpSumAmountUrl(taxYearEOY)), welsh = user.isWelsh, body = Map(AmountForm.amount -> overMaxAmount),
              follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          "has a BAD_REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          textOnPageCheck(hintText, hintTextSelector)
          elementNotOnPageCheck(paragraphSelector)
          textOnPageCheck(expectedWhereToFindThisInformation, detailsSelector)
          textOnPageCheck(expectedYouCanFindThisInformationIn, detailsYouCanFindThisSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedDetailsExample1, bulletSelector(1))
          textOnPageCheck(user.specificExpectedResults.get.expectedDetailsExample2, bulletSelector(2))
          inputFieldValueCheck(amountInputName, inputSelector, s"$overMaxAmount")
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(taxOnLumpSumAmountUrl(taxYearEOY), formSelector)
          errorSummaryCheck(maxAmountErrorText, expectedErrorHref)
          errorAboveElementCheck(maxAmountErrorText)
          welshToggleCheck(user.isWelsh)

        }
      }
    }

    "redirect to the overview page when it is in year" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val viewModel = anIncomeFromPensionsViewModel.copy(statePensionLumpSum = Some(anStateBenefitViewModelTwo.copy(
          taxPaidQuestion = Some(true), taxPaid = None)))
        insertCyaData(pensionsUserDataWithIncomeFromPensions(viewModel), aUserRequest)
        urlPost(fullUrl(taxOnLumpSumAmountUrl(taxYear)), body = validForm, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(overviewUrl(taxYear))

        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.incomeFromPensions.statePensionLumpSum.flatMap(_.taxPaid) shouldBe None
      }
    }

    "redirect to the 'Tax Paid on Lump Sum' Question page if it has been answered false" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val viewModel = anIncomeFromPensionsViewModel.copy(statePensionLumpSum = Some(anStateBenefitViewModelTwo.copy(
          taxPaidQuestion = Some(false), taxPaid = None)))
        insertCyaData(pensionsUserDataWithIncomeFromPensions(viewModel), aUserRequest)
        urlPost(fullUrl(taxOnLumpSumAmountUrl(taxYearEOY)), body = validForm, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        //TODO redirect to CYA page or next page
        result.header("location") shouldBe Some(taxOnLumpSumUrl(taxYearEOY))

        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.incomeFromPensions.statePensionLumpSum.flatMap(_.taxPaid) shouldBe None
      }
    }

    "redirect to the Annual Allowance CYA page when a valid amount is submitted and update session data" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val viewModel = anIncomeFromPensionsViewModel.copy(statePensionLumpSum = Some(anStateBenefitViewModelTwo.copy(
          taxPaidQuestion = Some(true), taxPaid = Some(100.10))))
        insertCyaData(pensionsUserDataWithIncomeFromPensions(viewModel), aUserRequest)
        urlPost(fullUrl(taxOnLumpSumAmountUrl(taxYearEOY)), body = validForm, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }
      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        //TODO redirect to CYA page or next page
        result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
      }
      "update state lump sum amount to Some(validAmount)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.incomeFromPensions.statePensionLumpSum.flatMap(_.taxPaid) shouldBe Some(newAmount)
      }
    }
  }
}
