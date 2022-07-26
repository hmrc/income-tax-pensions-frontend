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

import builders.PensionsCYAModelBuilder.aPensionsCYAModel
import builders.PensionsUserDataBuilder
import builders.PensionsUserDataBuilder.anPensionsUserDataEmptyCya
import builders.UnauthorisedPaymentsViewModelBuilder.anUnauthorisedPaymentsViewModel
import builders.UserBuilder.aUserRequest
import forms.RadioButtonAmountForm
import models.mongo.PensionsCYAModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.unauthorisedPaymentsPages.didYouPayNonUkTaxUrl
import utils.PageUrls.{fullUrl, pensionSummaryUrl}
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

class DidYouPayNonUkTaxControllerISpec extends IntegrationTest with ViewHelpers with BeforeAndAfterEach with PensionsDatabaseHelper {

  private val poundPrefixText = "£"
  private val amountInputName = "amount-2"
  private val amountInvalidFormat = "invalid"
  private val zeroAmount = "0"
  private val amountOverMaximum = "100,000,000,000"
  private val existingAmount: String = "200"


  private def pensionsUsersData(isPrior: Boolean = false, pensionsCyaModel: PensionsCYAModel) = {
    PensionsUserDataBuilder.aPensionsUserData.copy(
      isPriorSubmission = isPrior, pensions = pensionsCyaModel)
  }


  object Selectors {
    val yesSelector = "#value"
    val captionSelector: String = "#main-content > div > div > header > p"
    val continueButtonSelector: String = "#continue"
    val formSelector: String = "#main-content > div > div > form"
    val amountInputSelector = "#amount-2"
    val expectedAmountErrorHref = "#amount-2"
    val poundPrefixSelector = ".govuk-input__prefix"
    val amountText = "#conditional-value > div > label"
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val yesText: String
    val noText: String
    val buttonText: String
    val totalNonUkTax: String
    val totalNonUkTaxErrorNoEntry: String
    val totalNonUkTaxErrorIncorrectFormat: String
    val totalNonUkTaxErrorOverMaximum: String
    val expectedErrorMessage: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedErrorTitle: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Unauthorised payments from pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val yesText = "Yes"
    val noText = "No"
    val buttonText = "Continue"
    val totalNonUkTax: String = "Total non-UK tax in pounds"
    val totalNonUkTaxErrorNoEntry: String = "Enter the amount of non-UK tax paid"
    val totalNonUkTaxErrorIncorrectFormat: String = "Enter the amount of non-UK tax in the correct format"
    val totalNonUkTaxErrorOverMaximum: String = "The amount of non-UK tax paid must be less than £100,000,000,000"
    val expectedErrorMessage = "Select yes if you paid non-UK tax on the amount surcharged"

  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Unauthorised payments from pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val yesText = "Yes"
    val noText = "No"
    val buttonText = "Continue"
    val totalNonUkTax: String = "Total non-UK tax in pounds"
    val totalNonUkTaxErrorNoEntry: String = "Enter the amount of non-UK tax paid"
    val totalNonUkTaxErrorIncorrectFormat: String = "Enter the amount of non-UK tax in the correct format"
    val totalNonUkTaxErrorOverMaximum: String = "The amount of non-UK tax paid must be less than £100,000,000,000"
    val expectedErrorMessage = "Select yes if you paid non-UK tax on the amount surcharged"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Did you pay non-UK tax on the amount that resulted in a surcharge?"
    val expectedHeading = "Did you pay non-UK tax on the amount that resulted in a surcharge?"
    val expectedErrorTitle = s"Error: $expectedTitle"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Did you pay non-UK tax on the amount that resulted in a surcharge?"
    val expectedHeading = "Did you pay non-UK tax on the amount that resulted in a surcharge?"
    val expectedErrorTitle = s"Error: $expectedTitle"

  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Did your client pay non-UK tax on the amount that resulted in a surcharge?"
    val expectedHeading = "Did your client pay non-UK tax on the amount that resulted in a surcharge?"
    val expectedErrorTitle = s"Error: $expectedTitle"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Did your client pay non-UK tax on the amount that resulted in a surcharge?"
    val expectedHeading = "Did your client pay non-UK tax on the amount that resulted in a surcharge?"
    val expectedErrorTitle = s"Error: $expectedTitle"
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

        "render did you pay non-uk tax on the amount that resulted in a surcharge with no pre-filling" which {
          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(anPensionsUserDataEmptyCya, aUserRequest)
            urlGet(fullUrl(didYouPayNonUkTaxUrl(taxYearEOY)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }
          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          radioButtonCheck(yesText, 1, checked = Some(false))
          radioButtonCheck(noText, 2, checked = Some(false))
          buttonCheck(buttonText, continueButtonSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          textOnPageCheck(totalNonUkTax, amountText)
          inputFieldValueCheck(amountInputName, amountInputSelector, "")
          formPostLinkCheck(didYouPayNonUkTaxUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render surcharge page with yes pre-filled and amount field set" which {

          lazy val result: WSResponse = {
            dropPensionsDB()
            val unauthorisedPaymentsViewModel = anUnauthorisedPaymentsViewModel.copy(surchargeAmount = Some(BigDecimal(existingAmount)),
              surchargeTaxAmountQuestion = Some(true), surchargeTaxAmount = Some(BigDecimal(existingAmount)))
            insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(
              unauthorisedPayments = unauthorisedPaymentsViewModel)), aUserRequest)

            authoriseAgentOrIndividual(user.isAgent)
            urlGet(fullUrl(didYouPayNonUkTaxUrl(taxYearEOY)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          radioButtonCheck(yesText, 1, checked = Some(true))
          radioButtonCheck(noText, 2, checked = Some(false))
          buttonCheck(buttonText, continueButtonSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          textOnPageCheck(totalNonUkTax, amountText)
          inputFieldValueCheck(amountInputName, amountInputSelector, existingAmount)
          formPostLinkCheck(didYouPayNonUkTaxUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render surcharge page with NO pre-filled and amount field set to 0" which {

          lazy val result: WSResponse = {
            dropPensionsDB()
            val unauthorisedPaymentsViewModel = anUnauthorisedPaymentsViewModel.copy(
              surchargeTaxAmount = Some(BigDecimal("0")))
            insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(
              unauthorisedPayments = unauthorisedPaymentsViewModel)), aUserRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(fullUrl(didYouPayNonUkTaxUrl(taxYearEOY)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          radioButtonCheck(yesText, 1, checked = Some(false))
          radioButtonCheck(noText, 2, checked = Some(true))
          buttonCheck(buttonText, continueButtonSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          textOnPageCheck(totalNonUkTax, amountText)
          inputFieldValueCheck(amountInputName, amountInputSelector, "0")
          formPostLinkCheck(didYouPayNonUkTaxUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }

      }

    }

    "redirect to Pensions Summary page if there is no session data" should {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(didYouPayNonUkTaxUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      //TODO - redirect to unauthorised payments CYA page once implemented
      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
      }
    }

    "redirect to Unauthorised payments question page when surchargeAmount has no amount defined" which {
      lazy val form: Map[String, String] = Map(
        RadioButtonAmountForm.yesNo -> RadioButtonAmountForm.no, RadioButtonAmountForm.amount2 -> existingAmount)
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val unauthorisedPaymentsViewModel = anUnauthorisedPaymentsViewModel.copy(surchargeAmount = None,
          surchargeTaxAmountQuestion = None, surchargeTaxAmount = None)
        insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(
          unauthorisedPayments = unauthorisedPaymentsViewModel)), aUserRequest)
        urlPost(fullUrl(didYouPayNonUkTaxUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))

      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        //todo redirect to Unauthorised payments question page
        result.header("location") shouldBe Some(controllers.pensions.routes.PensionsSummaryController.show(taxYearEOY).url)
      }
    }


  }

  ".submit" should {

    userScenarios.foreach { user =>
      import Selectors._
      import user.commonExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        s"return $BAD_REQUEST error when no radio button value is submitted" which {

          lazy val emptyForm: Map[String, String] = Map(RadioButtonAmountForm.yesNo -> "", RadioButtonAmountForm.amount2 -> "")
          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            val unauthorisedPaymentsViewModel = anUnauthorisedPaymentsViewModel.copy(surchargeAmount = Some(BigDecimal(existingAmount)),
              surchargeTaxAmountQuestion = None, surchargeTaxAmount = None)
            insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(
              unauthorisedPayments = unauthorisedPaymentsViewModel)), aUserRequest)

            urlPost(fullUrl(didYouPayNonUkTaxUrl(taxYearEOY)), body = emptyForm, follow = false, welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          radioButtonCheck(yesText, 1, checked = Some(false))
          radioButtonCheck(noText, 2, checked = Some(false))
          buttonCheck(buttonText, continueButtonSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          textOnPageCheck(totalNonUkTax, amountText)
          inputFieldValueCheck(amountInputName, amountInputSelector, "")
          formPostLinkCheck(didYouPayNonUkTaxUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
          errorAboveElementCheck(expectedErrorMessage, Some("value"))
          errorSummaryCheck(expectedErrorMessage, Selectors.yesSelector)
        }

        s"return $BAD_REQUEST error when empty amount value is submitted" which {

          lazy val form: Map[String, String] = Map(RadioButtonAmountForm.yesNo -> RadioButtonAmountForm.yes, RadioButtonAmountForm.amount2 -> "")
          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            val unauthorisedPaymentsViewModel = anUnauthorisedPaymentsViewModel.copy(surchargeAmount = Some(BigDecimal(existingAmount)),
              surchargeTaxAmountQuestion = None, surchargeTaxAmount = None)

            insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(
              unauthorisedPayments = unauthorisedPaymentsViewModel)), aUserRequest)

            urlPost(fullUrl(didYouPayNonUkTaxUrl(taxYearEOY)), body = form, follow = false, welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          radioButtonCheck(yesText, 1, checked = Some(true))
          radioButtonCheck(noText, 2, checked = Some(false))
          buttonCheck(buttonText, continueButtonSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          textOnPageCheck(totalNonUkTax, amountText)
          inputFieldValueCheck(amountInputName, amountInputSelector, "")
          formPostLinkCheck(didYouPayNonUkTaxUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
          errorSummaryCheck(totalNonUkTaxErrorNoEntry, expectedAmountErrorHref)
          errorAboveElementCheck(totalNonUkTaxErrorNoEntry)
        }

        s"return $BAD_REQUEST error when invalid amount value is submitted" which {

          lazy val form: Map[String, String] = Map(
            RadioButtonAmountForm.yesNo -> RadioButtonAmountForm.yes, RadioButtonAmountForm.amount2 -> amountInvalidFormat)
          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            val unauthorisedPaymentsViewModel = anUnauthorisedPaymentsViewModel.copy(surchargeAmount = Some(BigDecimal(existingAmount)),
              surchargeTaxAmountQuestion = None, surchargeTaxAmount = None)
            insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(
              unauthorisedPayments = unauthorisedPaymentsViewModel)), aUserRequest)
            urlPost(fullUrl(didYouPayNonUkTaxUrl(taxYearEOY)), body = form, follow = false, welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          radioButtonCheck(yesText, 1, checked = Some(true))
          radioButtonCheck(noText, 2, checked = Some(false))
          buttonCheck(buttonText, continueButtonSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          textOnPageCheck(totalNonUkTax, amountText)
          inputFieldValueCheck(amountInputName, amountInputSelector, amountInvalidFormat)
          formPostLinkCheck(didYouPayNonUkTaxUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
          errorSummaryCheck(totalNonUkTaxErrorIncorrectFormat, expectedAmountErrorHref)
          errorAboveElementCheck(totalNonUkTaxErrorIncorrectFormat)
        }

        s"return $BAD_REQUEST when form is submitted with input over maximum allowed value" which {

          lazy val form: Map[String, String] = Map(
            RadioButtonAmountForm.yesNo -> RadioButtonAmountForm.yes, RadioButtonAmountForm.amount2 -> amountOverMaximum)
          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            val unauthorisedPaymentsViewModel = anUnauthorisedPaymentsViewModel.copy(surchargeAmount = Some(BigDecimal(existingAmount)),
              surchargeTaxAmountQuestion = None, surchargeTaxAmount = None)
            insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(
              unauthorisedPayments = unauthorisedPaymentsViewModel)), aUserRequest)
            urlPost(fullUrl(didYouPayNonUkTaxUrl(taxYearEOY)), body = form, follow = false, welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          radioButtonCheck(yesText, 1, checked = Some(true))
          radioButtonCheck(noText, 2, checked = Some(false))
          buttonCheck(buttonText, continueButtonSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          textOnPageCheck(totalNonUkTax, amountText)
          inputFieldValueCheck(amountInputName, amountInputSelector, amountOverMaximum)
          formPostLinkCheck(didYouPayNonUkTaxUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
          errorSummaryCheck(totalNonUkTaxErrorOverMaximum, expectedAmountErrorHref)
          errorAboveElementCheck(totalNonUkTaxErrorOverMaximum)
        }

      }
    }

    "redirect to Unauthorised payments question page when user selects 'yes' with a valid amount" which {

      lazy val form: Map[String, String] = Map(
        RadioButtonAmountForm.yesNo -> RadioButtonAmountForm.yes, RadioButtonAmountForm.amount2 -> existingAmount)
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val unauthorisedPaymentsViewModel = anUnauthorisedPaymentsViewModel.copy(surchargeAmount = Some(BigDecimal(existingAmount)),
          surchargeTaxAmountQuestion = None, surchargeTaxAmount = None)
        insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(
          unauthorisedPayments = unauthorisedPaymentsViewModel)), aUserRequest)
        urlPost(fullUrl(didYouPayNonUkTaxUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))

      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        //todo redirect to Unauthorised payments question page
        result.header("location") shouldBe Some(controllers.pensions.unauthorisedPayments.routes.WhereAnyOfTheUnauthorisedPaymentsController.show(taxYearEOY).url)
      }

      "updates did you non uk tax on the amount that resulted in a surcharge page to  Some(true) with valid amount" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.unauthorisedPayments.surchargeTaxAmountQuestion shouldBe Some(true)
        cyaModel.pensions.unauthorisedPayments.surchargeTaxAmount shouldBe Some(BigDecimal(existingAmount))
      }
    }

    "redirect to Unauthorised payments question page when user selects 'No' and update amount to 0" which {

      lazy val form: Map[String, String] = Map(
        RadioButtonAmountForm.yesNo -> RadioButtonAmountForm.no, RadioButtonAmountForm.amount2 -> zeroAmount)
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val unauthorisedPaymentsViewModel = anUnauthorisedPaymentsViewModel.copy(surchargeAmount = Some(BigDecimal(existingAmount)),
          surchargeTaxAmountQuestion = None, surchargeTaxAmount = None)
        insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(
          unauthorisedPayments = unauthorisedPaymentsViewModel)), aUserRequest)
        urlPost(fullUrl(didYouPayNonUkTaxUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))

      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        //todo redirect to Unauthorised payments question page
        result.header("location") shouldBe Some(controllers.pensions.unauthorisedPayments.routes.WhereAnyOfTheUnauthorisedPaymentsController.show(taxYearEOY).url)
      }

      "updates did you non uk tax on the amount that resulted in a surcharge page to  Some(false) with amount set to 0" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.unauthorisedPayments.surchargeTaxAmountQuestion shouldBe Some(false)
        cyaModel.pensions.unauthorisedPayments.surchargeTaxAmount shouldBe Some(BigDecimal(zeroAmount))
      }
    }

  }

}
