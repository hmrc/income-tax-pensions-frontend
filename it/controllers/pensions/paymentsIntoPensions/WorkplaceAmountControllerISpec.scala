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

package controllers.pensions.paymentsIntoPensions

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
import utils.PageUrls.PaymentIntoPensions.{checkPaymentsIntoPensionCyaUrl, workplacePensionAmount, workplacePensionUrl}
import utils.PageUrls.fullUrl
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

// scalastyle:off magic.number
class WorkplaceAmountControllerISpec extends IntegrationTest with ViewHelpers with BeforeAndAfterEach with PensionsDatabaseHelper {

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

    def bulletListSelector(index: Int): String = s"#main-content > div > div > form > div > label > ul > li:nth-child($index)"

    def insetSpanText(index: Int): String = s"#main-content > div > div > form > div > label > div > span:nth-child($index)"

    def paragraphSelector(index: Int): String = s"#main-content > div > div > form > div > label > p:nth-child($index)"
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val emptyErrorText: String
    val invalidFormatErrorText: String
    val maxAmountErrorText: String
    val expectedParagraph: String
    val hintText: String
    val buttonText: String

  }

  trait SpecificExpectedResults {
    val expectedHeading: String
    val expectedTitle: String
    val expectedErrorTitle: String
    val expectedBullet1: String
    val expectedBullet2: String
    val expectedYouCanFindThisOut: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Payments into pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val hintText = "For example, £193.52"
    val emptyErrorText = "Enter the amount paid into workplace pensions"
    val invalidFormatErrorText = "Enter the amount paid into workplace pensions in the correct format"
    val maxAmountErrorText = "The amount paid into workplace pensions must be less than £100,000,000,000"
    val buttonText = "Continue"
    val expectedParagraph = "Only include payments:"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Payments into pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val hintText = "For example, £193.52"
    val emptyErrorText = "Enter the amount paid into workplace pensions"
    val invalidFormatErrorText = "Enter the amount paid into workplace pensions in the correct format"
    val maxAmountErrorText = "The amount paid into workplace pensions must be less than £100,000,000,000"
    val buttonText = "Continue"
    val expectedParagraph = "Only include payments:"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedHeading = "How much did you pay into your workplace pensions?"
    val expectedTitle = "How much did you pay into your workplace pensions?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedBullet1 = "made after your pay was taxed"
    val expectedBullet2 = "your pension provider will not claim tax relief for"
    val expectedYouCanFindThisOut = "You can find this out from your employer or your pension provider."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedHeading = "How much did you pay into your workplace pensions?"
    val expectedTitle = "How much did you pay into your workplace pensions?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedBullet1 = "made after your pay was taxed"
    val expectedBullet2 = "your pension provider will not claim tax relief for"
    val expectedYouCanFindThisOut = "You can find this out from your employer or your pension provider."

  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedHeading = "How much did your client pay into their workplace pensions?"
    val expectedTitle = "How much did your client pay into their workplace pensions?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedBullet1 = "made after your client’s pay was taxed"
    val expectedBullet2 = "your client’s pension provider will not claim tax relief for"
    val expectedYouCanFindThisOut = "Your client can find this out from their employer or pension provider."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedHeading = "How much did your client pay into their workplace pensions?"
    val expectedTitle = "How much did your client pay into their workplace pensions?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedBullet1 = "made after your client’s pay was taxed"
    val expectedBullet2 = "your client’s pension provider will not claim tax relief for"
    val expectedYouCanFindThisOut = "Your client can find this out from their employer or pension provider."
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
        "render how much did you pay into your workplace pensions amount page with no pre filling" which {
          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(
              workplacePensionPaymentsQuestion = Some(true), totalWorkplacePensionPayments = None)
            insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)), aUserRequest)
            urlGet(fullUrl(workplacePensionAmount(taxYearEOY)),
              user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(expectedParagraph, paragraphSelector(2))
          textOnPageCheck(user.specificExpectedResults.get.expectedBullet1, bulletListSelector(1))
          textOnPageCheck(user.specificExpectedResults.get.expectedBullet2, bulletListSelector(2))
          textOnPageCheck(user.specificExpectedResults.get.expectedYouCanFindThisOut, paragraphSelector(4))
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, inputSelector, "")
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(workplacePensionAmount(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render how much did you pay into your workplace pensions amount page when cya data" which {

          val existingAmount: String = "999.88"
          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(
              workplacePensionPaymentsQuestion = Some(true), totalWorkplacePensionPayments = Some(BigDecimal(existingAmount)))
            insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)), aUserRequest)
            urlGet(fullUrl(workplacePensionAmount(taxYearEOY)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }
          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(expectedParagraph, paragraphSelector(2))
          textOnPageCheck(user.specificExpectedResults.get.expectedBullet1, bulletListSelector(1))
          textOnPageCheck(user.specificExpectedResults.get.expectedBullet2, bulletListSelector(2))
          textOnPageCheck(user.specificExpectedResults.get.expectedYouCanFindThisOut, paragraphSelector(4))
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, inputSelector, existingAmount)
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(workplacePensionAmount(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }
    "redirect to the Workplace pension question page if the question has not been answered" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(
          workplacePensionPaymentsQuestion = None, totalWorkplacePensionPayments = None)
        insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)), aUserRequest)
        urlGet(fullUrl(workplacePensionAmount(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(workplacePensionUrl(taxYearEOY)) shouldBe true
      }

    }

    "redirect to the workplace question page if the workplaceQuestion has been answered as false" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(
          workplacePensionPaymentsQuestion = Some(false), totalWorkplacePensionPayments = None)
        insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)), aUserRequest)
        urlGet(fullUrl(workplacePensionAmount(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(workplacePensionUrl(taxYearEOY)) shouldBe true
      }

    }

    "redirect to the CYA page if there is no session data" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        // no cya insert
        urlGet(fullUrl(workplacePensionAmount(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an SEE_OTHER status" in {
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
              workplacePensionPaymentsQuestion = Some(true), totalWorkplacePensionPayments = None)
            insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)), aUserRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(fullUrl(workplacePensionAmount(taxYearEOY)), body = emptyForm, welsh = user.isWelsh,
              follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(expectedParagraph, paragraphSelector(2))
          textOnPageCheck(user.specificExpectedResults.get.expectedBullet1, bulletListSelector(1))
          textOnPageCheck(user.specificExpectedResults.get.expectedBullet2, bulletListSelector(2))
          textOnPageCheck(user.specificExpectedResults.get.expectedYouCanFindThisOut, paragraphSelector(4))
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, inputSelector, amountEmpty)
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(workplacePensionAmount(taxYearEOY), formSelector)
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
              workplacePensionPaymentsQuestion = Some(true), totalWorkplacePensionPayments = None)
            insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)), aUserRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(fullUrl(workplacePensionAmount(taxYearEOY)), body = invalidFormatForm, welsh = user.isWelsh,
              follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(expectedParagraph, paragraphSelector(2))
          textOnPageCheck(user.specificExpectedResults.get.expectedBullet1, bulletListSelector(1))
          textOnPageCheck(user.specificExpectedResults.get.expectedBullet2, bulletListSelector(2))
          textOnPageCheck(user.specificExpectedResults.get.expectedYouCanFindThisOut, paragraphSelector(4))
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, inputSelector, amountInvalidFormat)
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(workplacePensionAmount(taxYearEOY), formSelector)
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
              workplacePensionPaymentsQuestion = Some(true), totalWorkplacePensionPayments = None)
            insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)), aUserRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(fullUrl(workplacePensionAmount(taxYearEOY)), body = overMaximumForm, welsh = user.isWelsh,
              follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(expectedParagraph, paragraphSelector(2))
          textOnPageCheck(user.specificExpectedResults.get.expectedBullet1, bulletListSelector(1))
          textOnPageCheck(user.specificExpectedResults.get.expectedBullet2, bulletListSelector(2))
          textOnPageCheck(user.specificExpectedResults.get.expectedYouCanFindThisOut, paragraphSelector(4))
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, inputSelector, amountOverMaximum)
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(workplacePensionAmount(taxYearEOY), formSelector)
          errorSummaryCheck(maxAmountErrorText, expectedErrorHref)
          errorAboveElementCheck(maxAmountErrorText)
          welshToggleCheck(user.isWelsh)
        }
      }
    }
    "redirect to the correct page when a valid amount is submitted and update the session amount" which {

      val validAmount = "100.22"
      val validForm: Map[String, String] = Map(AmountForm.amount -> validAmount)

      lazy val result: WSResponse = {
        dropPensionsDB()
        val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(
          workplacePensionPaymentsQuestion = Some(true), totalWorkplacePensionPayments = None)
        insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)), aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(workplacePensionAmount(taxYearEOY)), body = validForm, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(checkPaymentsIntoPensionCyaUrl(taxYearEOY))
      }

      "updates workplace amount" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.paymentsIntoPension.totalWorkplacePensionPayments shouldBe Some(BigDecimal(validAmount))
      }
    }

  }
}
// scalastyle:on magic.number
