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

package controllers.pensions

import builders.PaymentsIntoPensionVewModelBuilder.aPaymentsIntoPensionViewModel
import builders.PensionsCYAModelBuilder._
import builders.PensionsUserDataBuilder
import builders.UserBuilder._
import forms.AmountForm
import models.mongo.PensionsCYAModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls._
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

class OneOffRASPaymentsAmountControllerISpec extends IntegrationTest with ViewHelpers with BeforeAndAfterEach with PensionsDatabaseHelper {

  private val taxYearEOY: Int = taxYear - 1
  private val poundPrefixText = "£"
  private val amountInputName = "amount"

  private def pensionsUsersData(isPrior: Boolean = false, pensionsCyaModel: PensionsCYAModel) = {
    PensionsUserDataBuilder.aPensionsUserData.copy(
      isPriorSubmission = isPrior,
      pensions = pensionsCyaModel
    )
  }

  object Selectors {
    val captionSelector: String = "#main-content > div > div > form > div > label > header > p"
    val continueButtonSelector: String = "#continue"
    val formSelector: String = "#main-content > div > div > form"
    val hintTextSelector = "#amount-hint"
    val poundPrefixSelector = ".govuk-input__prefix"
    val inputSelector = "#amount"
    val expectedErrorHref = "#amount"
    def insetSpanText(index: Int): String = s"#main-content > div > div > form > div > label > div > span:nth-child($index)"
    def paragraphSelector(index: Int): String = s"#main-content > div > div > form > div > label > p:nth-child($index)"
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedHeading: String
    val expectedTitle: String
    val expectedErrorTitle: String
    val expectedHowToWorkOut: String
    val expectedCalculationHeading: String
    val expectedExampleCalculation: String
    val emptyErrorText: String
    val invalidFormatErrorText: String
    val maxAmountErrorText: String
    val hintText: String
    val buttonText: String
  }

  trait SpecificExpectedResults {
    val expectedYouToldUs: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Payments into pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedHeading = "Total one-off payments into relief at source (RAS) pensions, plus basic rate tax relief"
    val expectedTitle = "Total one-off payments into relief at source (RAS) pensions, plus basic rate tax relief"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedHowToWorkOut = "To work it out, divide your one-off payment amount by 80 and multiply the result by 100."
    val expectedCalculationHeading = "Example calculation"
    val expectedExampleCalculation = "Emma made a one-off payment of £500. £500 divided by 80 and multiplied by 100 is £625. Her answer is £625."
    val hintText = "For example, £193.52"
    val emptyErrorText = "Enter the total amount of one-off payments paid into RAS pensions, plus basic rate tax relief"
    val invalidFormatErrorText = "Enter the total amount of one-off payments paid into RAS pensions, plus basic rate tax relief, in the correct format"
    val maxAmountErrorText = "The total amount of one-off payments paid into RAS pensions, plus basic rate tax relief, must be less than £100,000,000,000"
    val buttonText = "Continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Payments into pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedHeading = "Total one-off payments into relief at source (RAS) pensions, plus basic rate tax relief"
    val expectedTitle = "Total one-off payments into relief at source (RAS) pensions, plus basic rate tax relief"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedHowToWorkOut = "To work it out, divide your one-off payment amount by 80 and multiply the result by 100."
    val expectedCalculationHeading = "Example calculation"
    val expectedExampleCalculation = "Emma made a one-off payment of £500. £500 divided by 80 and multiplied by 100 is £625. Her answer is £625."
    val hintText = "For example, £193.52"
    val emptyErrorText = "Enter the total amount of one-off payments paid into RAS pensions, plus basic rate tax relief"
    val invalidFormatErrorText = "Enter the total amount of one-off payments paid into RAS pensions, plus basic rate tax relief, in the correct format"
    val maxAmountErrorText = "The total amount of one-off payments paid into RAS pensions, plus basic rate tax relief, must be less than £100,000,000,000"
    val buttonText = "Continue"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedYouToldUs =
      "You told us the total amount you paid plus tax relief was £189.01. Tell us how much of this was a one-off payment. Include tax relief."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedYouToldUs =
      "You told us the total amount you paid plus tax relief was £189.01. Tell us how much of this was a one-off payment. Include tax relief."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedYouToldUs =
      "You told us the total amount your client paid plus tax relief was £189.01. Tell us how much of this was a one-off payment. Include tax relief."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedYouToldUs =
      "You told us the total amount your client paid plus tax relief was £189.01. Tell us how much of this was a one-off payment. Include tax relief."
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
        "render Total one off payments into relief at source (RAS) pensions page with no value when no cya data" which {
          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(
              oneOffRasPaymentPlusTaxReliefQuestion = Some(true), totalOneOffRasPaymentPlusTaxRelief = None)
            insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)), aUserRequest)
            urlGet(fullUrl(oneOffReliefAtSourcePaymentsAmountUrl(taxYearEOY)),
              user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(expectedTitle)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedYouToldUs, paragraphSelector(2))
          textOnPageCheck(expectedHowToWorkOut, paragraphSelector(3))
          textOnPageCheck(expectedCalculationHeading, insetSpanText(1))
          textOnPageCheck(expectedExampleCalculation, insetSpanText(2))
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, inputSelector, "")
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(oneOffReliefAtSourcePaymentsAmountUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render Total one off payments into relief at source (RAS) pensions page prefilled when cya data" which {

          val existingAmount: String = "999.88"
          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(
              oneOffRasPaymentPlusTaxReliefQuestion = Some(true), totalOneOffRasPaymentPlusTaxRelief = Some(BigDecimal(existingAmount)))
            insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)), aUserRequest)
            urlGet(fullUrl(oneOffReliefAtSourcePaymentsAmountUrl(taxYearEOY)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(expectedTitle)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedYouToldUs, paragraphSelector(2))
          textOnPageCheck(expectedHowToWorkOut, paragraphSelector(3))
          textOnPageCheck(expectedCalculationHeading, insetSpanText(1))
          textOnPageCheck(expectedExampleCalculation, insetSpanText(2))
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, inputSelector, existingAmount)
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(oneOffReliefAtSourcePaymentsAmountUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)

        }
      }
    }

    "redirect to the oneOffRasPensionPaymentQuestion page if the question has not been answered" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(
          oneOffRasPaymentPlusTaxReliefQuestion = None, totalOneOffRasPaymentPlusTaxRelief = None)
        insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)), aUserRequest)
        urlGet(fullUrl(oneOffReliefAtSourcePaymentsAmountUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has a SEE_OTHER status and redirects correctly" in {
        result.status shouldBe SEE_OTHER
        //TODO - update once page is available
        result.header("location").contains(checkPaymentsIntoPensionCyaUrl(taxYearEOY)) shouldBe true
      }
    }

    "redirect to the oneOffRasPaymentQuestion page if the oneOffRasPaymentQuestion question has been answered as false" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(
          oneOffRasPaymentPlusTaxReliefQuestion = Some(false), totalOneOffRasPaymentPlusTaxRelief = None)
        insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)), aUserRequest)
        urlGet(fullUrl(oneOffReliefAtSourcePaymentsAmountUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        //TODO - update once page is available
        result.header("location").contains(checkPaymentsIntoPensionCyaUrl(taxYearEOY)) shouldBe true
      }
    }

    "redirect to the ReliefAtSourcePaymentsAmount page if 'totalRASPaymentsAndTaxRelief' is missing" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(
          totalRASPaymentsAndTaxRelief = None)
        insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)), aUserRequest)
        urlGet(fullUrl(oneOffReliefAtSourcePaymentsAmountUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an SEE_OTHER status and redirects to the RAS payments amount page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(reliefAtSourcePaymentsAndTaxReliefAmountUrl(taxYearEOY)) shouldBe true
      }
    }

    "redirect to the CYA page if there is no session data" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(oneOffReliefAtSourcePaymentsAmountUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an SEE_OTHER status and redirects to the CYA page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkPaymentsIntoPensionCyaUrl(taxYearEOY)) shouldBe true
      }

    }
  }

  ".submit" should {
    userScenarios.foreach { user =>

      import Selectors._
      import user.commonExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "return an error when form is submitted with no input entry" which {

          val amountEmpty = ""
          val emptyForm: Map[String, String] = Map(AmountForm.amount -> amountEmpty)

          lazy val result: WSResponse = {
            dropPensionsDB()
            val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(
              oneOffRasPaymentPlusTaxReliefQuestion = Some(true), totalOneOffRasPaymentPlusTaxRelief = None)
            insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)), aUserRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(fullUrl(oneOffReliefAtSourcePaymentsAmountUrl(taxYearEOY)), body = emptyForm, welsh = user.isWelsh,
              follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(expectedErrorTitle)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedYouToldUs, paragraphSelector(2))
          textOnPageCheck(expectedHowToWorkOut, paragraphSelector(3))
          textOnPageCheck(expectedCalculationHeading, insetSpanText(1))
          textOnPageCheck(expectedExampleCalculation, insetSpanText(2))
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, inputSelector, amountEmpty)
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(oneOffReliefAtSourcePaymentsAmountUrl(taxYearEOY), formSelector)
          errorSummaryCheck(emptyErrorText, expectedErrorHref)
          errorAboveElementCheck(emptyErrorText)
          welshToggleCheck(user.isWelsh)
        }

        "return an error when form is submitted with an invalid format input" which {

          val amountInvalidFormat = "invalid"
          val invalidFormatForm: Map[String, String] = Map(AmountForm.amount -> amountInvalidFormat)

          lazy val result: WSResponse = {
            dropPensionsDB()
            val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(
              oneOffRasPaymentPlusTaxReliefQuestion = Some(true), totalOneOffRasPaymentPlusTaxRelief = None)
            insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)), aUserRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(fullUrl(oneOffReliefAtSourcePaymentsAmountUrl(taxYearEOY)), body = invalidFormatForm, welsh = user.isWelsh,
              follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(expectedErrorTitle)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedYouToldUs, paragraphSelector(2))
          textOnPageCheck(expectedHowToWorkOut, paragraphSelector(3))
          textOnPageCheck(expectedCalculationHeading, insetSpanText(1))
          textOnPageCheck(expectedExampleCalculation, insetSpanText(2))
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, inputSelector, amountInvalidFormat)
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(oneOffReliefAtSourcePaymentsAmountUrl(taxYearEOY), formSelector)
          errorSummaryCheck(invalidFormatErrorText, expectedErrorHref)
          errorAboveElementCheck(invalidFormatErrorText)
          welshToggleCheck(user.isWelsh)
        }

        "return an error when form is submitted with input over maximum allowed value" which {

          val amountOverMaximum = "100,000,000,000"
          val overMaximumForm: Map[String, String] = Map(AmountForm.amount -> amountOverMaximum)

          lazy val result: WSResponse = {
            dropPensionsDB()
            val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(
              oneOffRasPaymentPlusTaxReliefQuestion = Some(true), totalOneOffRasPaymentPlusTaxRelief = None)
            insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)), aUserRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(fullUrl(oneOffReliefAtSourcePaymentsAmountUrl(taxYearEOY)), body = overMaximumForm, welsh = user.isWelsh,
              follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(expectedErrorTitle)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedYouToldUs, paragraphSelector(2))
          textOnPageCheck(expectedHowToWorkOut, paragraphSelector(3))
          textOnPageCheck(expectedCalculationHeading, insetSpanText(1))
          textOnPageCheck(expectedExampleCalculation, insetSpanText(2))
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, inputSelector, amountOverMaximum)
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(oneOffReliefAtSourcePaymentsAmountUrl(taxYearEOY), formSelector)
          errorSummaryCheck(maxAmountErrorText, expectedErrorHref)
          errorAboveElementCheck(maxAmountErrorText)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "redirect to the ReliefAtSourcePaymentsAmount page if 'totalRASPaymentsAndTaxRelief' is missing when form is submitted with an error" which {

      val amountOverMaximum = "100,000,000,000"
      val overMaximumForm: Map[String, String] = Map(AmountForm.amount -> amountOverMaximum)

      lazy val result: WSResponse = {
        dropPensionsDB()
        val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(
          totalRASPaymentsAndTaxRelief = None)
        insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)), aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(oneOffReliefAtSourcePaymentsAmountUrl(taxYearEOY)), body = overMaximumForm,
          follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an SEE_OTHER status and redirects to the RAS payments amount page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(reliefAtSourcePaymentsAndTaxReliefAmountUrl(taxYearEOY)) shouldBe true
      }
    }

    "redirect to the correct page when a valid amount is submitted and update the session amount" which {

      val validAmount = "100.22"
      val validForm: Map[String, String] = Map(AmountForm.amount -> validAmount)

      lazy val result: WSResponse = {
        dropPensionsDB()
        val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(
          oneOffRasPaymentPlusTaxReliefQuestion = Some(true), totalOneOffRasPaymentPlusTaxRelief = None)
        insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)), aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(oneOffReliefAtSourcePaymentsAmountUrl(taxYearEOY)), body = validForm,
          follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        // TODO - redirect to total payments into RAS pensions page when built
        result.header("location") shouldBe Some(controllers.pensions.routes.PensionsTaxReliefNotClaimedController.show(taxYearEOY).url)
      }

      "updates totalOneOffRasPaymentPlusTaxRelief to Some(100.22)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.paymentsIntoPension.totalOneOffRasPaymentPlusTaxRelief shouldBe Some(BigDecimal(validAmount))
      }
    }
  }
}
