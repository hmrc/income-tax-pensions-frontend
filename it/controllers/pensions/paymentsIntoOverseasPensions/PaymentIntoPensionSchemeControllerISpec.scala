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

package controllers.pensions.paymentsIntoOverseasPensions

import builders.PaymentsIntoOverseasPensionsViewModelBuilder.aPaymentsIntoOverseasPensionsViewModel
import builders.PensionsCYAModelBuilder.aPensionsCYAModel
import builders.PensionsUserDataBuilder
import builders.PensionsUserDataBuilder.anPensionsUserDataEmptyCya
import builders.UserBuilder.aUserRequest
import forms.RadioButtonAmountForm
import models.mongo.PensionsCYAModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.overseasPensionPages.paymentsIntoPensionSchemeUrl
import utils.PageUrls.{fullUrl, pensionSummaryUrl}
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

class PaymentIntoPensionSchemeControllerISpec extends IntegrationTest with ViewHelpers with BeforeAndAfterEach with PensionsDatabaseHelper {

  private val poundPrefixText = "£"
  private val amountInputName = "amount-2"
  private val amountInvalidFormat = "invalid"
  private val amountOverMaximum = "100,000,000,000"
  private val existingAmount: String = "200"


  private def pensionsUsersData(isPrior: Boolean = false, pensionsCyaModel: PensionsCYAModel) = {
    PensionsUserDataBuilder.aPensionsUserData.copy(
      isPriorSubmission = isPrior, pensions = pensionsCyaModel)
  }

  private val externalHref = "https://www.gov.uk/guidance/overseas-pensions-tax-relief-on-your-contributions"

  object Selectors {
    val captionSelector: String = "#main-content > div > div > header > p"
    def paragraphOneSelector(index: Int): String = s"#main-content > div > div > p:nth-child($index)"
    def paragraphTwoTextSelector (index: Int): String = s"#main-content > div > div > p:nth-child($index)"
    val expectedLinkSelector = "#eligibleForTaxRelief-link"
    val bulletTwoTextSelector: String = "#main-content > div > div > ul > li:nth-child(2)"
    val questionTextSelector: String = "#main-content > div > div > form > div > fieldset > legend"
    val yesSelector = "#value"
    val continueButtonSelector: String = "#continue"
    val formSelector: String = "#main-content > div > div > form"
    val amountText = "#conditional-value > div > label"
    val amountHintSelector = "#amount-2-hint"
    val amountInputSelector = "#amount-2"
    val expectedAmountErrorHref = "#amount-2"
    val poundPrefixSelector = ".govuk-input__prefix"
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedTitle: String
    val expectedHeading: String
    val expectedErrorTitle: String
    val paragraphTwoText: String
    val bulletPointOneLinkText: String
    val bulletPointTwoText: String
    val yesText: String
    val noText: String
    val buttonText: String
    val totalAmountPounds: String
    val HintAmount: String

  }

  trait SpecificExpectedResults {
    val paragraphOneText: String
    val pensionSchemeQuestionText: String
    val HintText: String
    val expectedRadioErrorMessage: String
    val overseasPensionSchemeAmountNoEntry: String
    val overseasPensionSchemeErrorAmountIncorrectFormat: String
    val overseasPensionSchemeErrorAmountOverMaximum: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Payments into overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val paragraphTwoText = "They must also be:"
    val bulletPointOneLinkText = "eligible for tax relief (opens in new tab)"
    val bulletPointTwoText = "paid into after tax (net income)"
    val yesText = "Yes"
    val noText = "No"
    val buttonText = "Continue"
    val totalAmountPounds: String = "Total amount, in pounds"
    val HintAmount: String = "For example, £193.52"
    val expectedTitle = "Payments into overseas pension schemes"
    val expectedHeading = "Payments into overseas pension schemes"
    val expectedErrorTitle = s"Error: $expectedTitle"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Payments into overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val paragraphTwoText = "They must also be:"
    val bulletPointOneLinkText = "eligible for tax relief (opens in new tab)"
    val bulletPointTwoText = "paid into after tax (net income)"
    val yesText = "Yes"
    val noText = "No"
    val buttonText = "Continue"
    val totalAmountPounds: String = "Total amount, in pounds"
    val HintAmount: String = "For example, £193.52"
    val expectedTitle = "Payments into overseas pension schemes"
    val expectedHeading = "Payments into overseas pension schemes"
    val expectedErrorTitle = s"Error: $expectedTitle"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val paragraphOneText: String = "Your overseas pension schemes must not be registered in the UK."
    val pensionSchemeQuestionText: String = "Did you pay into overseas pension schemes?"
    val HintText: String = "For example, £193.52 Do not include payments your employer made"
    val expectedRadioErrorMessage = "Select yes if you paid into an overseas pension scheme"
    val overseasPensionSchemeAmountNoEntry: String = "Enter the amount you paid into overseas pension schemes"
    val overseasPensionSchemeErrorAmountIncorrectFormat: String = "Enter the amount you paid into overseas pension schemes in the correct format"
    val overseasPensionSchemeErrorAmountOverMaximum: String = "The amount you paid into overseas pension schemes must be less than £100,000,000,000"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val paragraphOneText: String = "Your overseas pension schemes must not be registered in the UK."
    val pensionSchemeQuestionText: String = "Did you pay into overseas pension schemes?"
    val HintText: String = "For example, £193.52 Do not include payments your employer made"
    val expectedRadioErrorMessage = "Select yes if you paid into an overseas pension scheme"
    val overseasPensionSchemeAmountNoEntry: String = "Enter the amount you paid into overseas pension schemes"
    val overseasPensionSchemeErrorAmountIncorrectFormat: String = "Enter the amount you paid into overseas pension schemes in the correct format"
    val overseasPensionSchemeErrorAmountOverMaximum: String = "The amount you paid into overseas pension schemes must be less than £100,000,000,000"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val paragraphOneText: String = "Your client’s overseas pension schemes must not be registered in the UK."
    val pensionSchemeQuestionText: String = "Did your client pay into overseas pension schemes?"
    val HintText: String = "For example, £193.52 Do not include payments your client’s employer made"
    val expectedRadioErrorMessage = "Select yes if your client paid into an overseas pension scheme"
    val overseasPensionSchemeAmountNoEntry: String = "Enter the amount your client paid into overseas pension schemes"
    val overseasPensionSchemeErrorAmountIncorrectFormat: String = "Enter the amount your client paid into overseas pension schemes in the correct format"
    val overseasPensionSchemeErrorAmountOverMaximum: String = "The amount your client paid into overseas pension schemes must be less than £100,000,000,000"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val paragraphOneText: String = "Your client’s overseas pension schemes must not be registered in the UK."
    val pensionSchemeQuestionText: String = "Did your client pay into overseas pension schemes?"
    val HintText: String = "For example, £193.52 Do not include payments your client’s employer made"
    val expectedRadioErrorMessage = "Select yes if your client paid into an overseas pension scheme"
    val overseasPensionSchemeAmountNoEntry: String = "Enter the amount your client paid into overseas pension schemes"
    val overseasPensionSchemeErrorAmountIncorrectFormat: String = "Enter the amount your client paid into overseas pension schemes in the correct format"
    val overseasPensionSchemeErrorAmountOverMaximum: String = "The amount your client paid into overseas pension schemes must be less than £100,000,000,000"
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

        "render payments into overseas pension schemes with no pre-filling" which {
          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(anPensionsUserDataEmptyCya, aUserRequest)
            urlGet(fullUrl(paymentsIntoPensionSchemeUrl(taxYearEOY)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }
          titleCheck(expectedTitle)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          radioButtonCheck(yesText, 1, checked = Some(false))
          radioButtonCheck(noText, 2, checked = Some(false))
          buttonCheck(buttonText, continueButtonSelector)
          textOnPageCheck(user.specificExpectedResults.get.paragraphOneText, paragraphOneSelector(2))
          textOnPageCheck(paragraphTwoText, paragraphTwoTextSelector(3))
          linkCheck(bulletPointOneLinkText, expectedLinkSelector, externalHref)
          textOnPageCheck(bulletPointTwoText, bulletTwoTextSelector)
          textOnPageCheck(totalAmountPounds, amountText)
          textOnPageCheck(user.specificExpectedResults.get.HintText, amountHintSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, amountInputSelector, "")
          formPostLinkCheck(paymentsIntoPensionSchemeUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render payments into overseas pension schemes with yes pre-filled and amount field set" which {

          lazy val result: WSResponse = {
            dropPensionsDB()
            val viewModel = aPaymentsIntoOverseasPensionsViewModel.copy(paymentsIntoOverseasPensionsAmount = Some(BigDecimal(existingAmount)),
              paymentsIntoOverseasPensionsQuestions = Some(true))
            insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(
              paymentsIntoOverseasPensions = viewModel)), aUserRequest)

            authoriseAgentOrIndividual(user.isAgent)
            urlGet(fullUrl(paymentsIntoPensionSchemeUrl(taxYearEOY)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(expectedTitle)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          radioButtonCheck(yesText, 1, checked = Some(true))
          radioButtonCheck(noText, 2, checked = Some(false))
          buttonCheck(buttonText, continueButtonSelector)
          textOnPageCheck(user.specificExpectedResults.get.paragraphOneText, paragraphOneSelector(2))
          textOnPageCheck(paragraphTwoText, paragraphTwoTextSelector(3))
          linkCheck(bulletPointOneLinkText, expectedLinkSelector, externalHref)
          textOnPageCheck(bulletPointTwoText, bulletTwoTextSelector)
          textOnPageCheck(totalAmountPounds, amountText)
          textOnPageCheck(user.specificExpectedResults.get.HintText, amountHintSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, amountInputSelector, existingAmount)
          formPostLinkCheck(paymentsIntoPensionSchemeUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }

      }

    }

    "redirect to Pensions Summary page if there is no session data" should {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(paymentsIntoPensionSchemeUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }


      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        //TODO - redirect to overseas  CYA page once implemented
        result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
      }
    }

  }

  ".submit" should {

    userScenarios.foreach { user =>
      import Selectors._
      import user.commonExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        s"return $BAD_REQUEST error when no radio button value is selected" which {

          lazy val emptyForm: Map[String, String] = Map(RadioButtonAmountForm.yesNo -> "", RadioButtonAmountForm.amount2 -> "")
          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            val viewModel = aPaymentsIntoOverseasPensionsViewModel.copy(paymentsIntoOverseasPensionsAmount = None,
              paymentsIntoOverseasPensionsQuestions = None)
            insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(
              paymentsIntoOverseasPensions = viewModel)), aUserRequest)
            urlPost(fullUrl(paymentsIntoPensionSchemeUrl(taxYearEOY)), body = emptyForm, follow = false, welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)
          titleCheck(expectedErrorTitle)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          radioButtonCheck(yesText, 1, checked = Some(false))
          radioButtonCheck(noText, 2, checked = Some(false))
          buttonCheck(buttonText, continueButtonSelector)
          textOnPageCheck(user.specificExpectedResults.get.paragraphOneText, paragraphOneSelector(3))
          textOnPageCheck(paragraphTwoText, paragraphTwoTextSelector(4))
          linkCheck(bulletPointOneLinkText, expectedLinkSelector, externalHref)
          textOnPageCheck(bulletPointTwoText, bulletTwoTextSelector)
          textOnPageCheck(totalAmountPounds, amountText)
          textOnPageCheck(user.specificExpectedResults.get.HintText, amountHintSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, amountInputSelector, "")
          formPostLinkCheck(paymentsIntoPensionSchemeUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
          errorAboveElementCheck(user.specificExpectedResults.get.expectedRadioErrorMessage, Some("value"))
          errorSummaryCheck(user.specificExpectedResults.get.expectedRadioErrorMessage, Selectors.yesSelector)
        }

        s"return $BAD_REQUEST error when empty amount value is submitted" which {

          lazy val form: Map[String, String] = Map(RadioButtonAmountForm.yesNo -> RadioButtonAmountForm.yes, RadioButtonAmountForm.amount2 -> "")
          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            authoriseAgentOrIndividual(user.isAgent)
            val viewModel = aPaymentsIntoOverseasPensionsViewModel.copy(paymentsIntoOverseasPensionsAmount = None,
              paymentsIntoOverseasPensionsQuestions = None)
            insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(
              paymentsIntoOverseasPensions = viewModel)), aUserRequest)
            urlPost(fullUrl(paymentsIntoPensionSchemeUrl(taxYearEOY)), body = form, follow = false, welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)
          titleCheck(expectedErrorTitle)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          radioButtonCheck(yesText, 1, checked = Some(true))
          radioButtonCheck(noText, 2, checked = Some(false))
          buttonCheck(buttonText, continueButtonSelector)
          textOnPageCheck(user.specificExpectedResults.get.paragraphOneText, paragraphOneSelector(3))
          textOnPageCheck(paragraphTwoText, paragraphTwoTextSelector(4))
          linkCheck(bulletPointOneLinkText, expectedLinkSelector, externalHref)
          textOnPageCheck(bulletPointTwoText, bulletTwoTextSelector)
          textOnPageCheck(totalAmountPounds, amountText)
          textOnPageCheck(user.specificExpectedResults.get.HintText, amountHintSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, amountInputSelector, "")
          formPostLinkCheck(paymentsIntoPensionSchemeUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
          errorAboveElementCheck(user.specificExpectedResults.get.overseasPensionSchemeAmountNoEntry)
          errorSummaryCheck(user.specificExpectedResults.get.overseasPensionSchemeAmountNoEntry, expectedAmountErrorHref)
        }

        s"return $BAD_REQUEST error when invalid amount value is submitted" which {

          lazy val form: Map[String, String] = Map(
            RadioButtonAmountForm.yesNo -> RadioButtonAmountForm.yes, RadioButtonAmountForm.amount2 -> amountInvalidFormat)
          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            val viewModel = aPaymentsIntoOverseasPensionsViewModel.copy(paymentsIntoOverseasPensionsAmount = None,
              paymentsIntoOverseasPensionsQuestions = None)
            insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(
              paymentsIntoOverseasPensions = viewModel)), aUserRequest)
            urlPost(fullUrl(paymentsIntoPensionSchemeUrl(taxYearEOY)), body = form, follow = false, welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)
          titleCheck(expectedErrorTitle)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          radioButtonCheck(yesText, 1, checked = Some(true))
          radioButtonCheck(noText, 2, checked = Some(false))
          buttonCheck(buttonText, continueButtonSelector)
          textOnPageCheck(user.specificExpectedResults.get.paragraphOneText, paragraphOneSelector(3))
          textOnPageCheck(paragraphTwoText, paragraphTwoTextSelector(4))
          linkCheck(bulletPointOneLinkText, expectedLinkSelector, externalHref)
          textOnPageCheck(bulletPointTwoText, bulletTwoTextSelector)
          textOnPageCheck(totalAmountPounds, amountText)
          textOnPageCheck(user.specificExpectedResults.get.HintText, amountHintSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, amountInputSelector, amountInvalidFormat)
          formPostLinkCheck(paymentsIntoPensionSchemeUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
          errorAboveElementCheck(user.specificExpectedResults.get.overseasPensionSchemeErrorAmountIncorrectFormat)
          errorSummaryCheck(user.specificExpectedResults.get.overseasPensionSchemeErrorAmountIncorrectFormat, expectedAmountErrorHref)
        }

        s"return $BAD_REQUEST when form is submitted with input over maximum allowed value" which {

          lazy val form: Map[String, String] = Map(
            RadioButtonAmountForm.yesNo -> RadioButtonAmountForm.yes, RadioButtonAmountForm.amount2 -> amountOverMaximum)
          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            val viewModel = aPaymentsIntoOverseasPensionsViewModel.copy(paymentsIntoOverseasPensionsAmount = None,
              paymentsIntoOverseasPensionsQuestions = None)
            insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(
              paymentsIntoOverseasPensions = viewModel)), aUserRequest)
            urlPost(fullUrl(paymentsIntoPensionSchemeUrl(taxYearEOY)), body = form, follow = false, welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(expectedErrorTitle)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          radioButtonCheck(yesText, 1, checked = Some(true))
          radioButtonCheck(noText, 2, checked = Some(false))
          buttonCheck(buttonText, continueButtonSelector)
          textOnPageCheck(user.specificExpectedResults.get.paragraphOneText, paragraphOneSelector(3))
          textOnPageCheck(paragraphTwoText, paragraphTwoTextSelector(4))
          linkCheck(bulletPointOneLinkText, expectedLinkSelector, externalHref)
          textOnPageCheck(bulletPointTwoText, bulletTwoTextSelector)
          textOnPageCheck(totalAmountPounds, amountText)
          textOnPageCheck(user.specificExpectedResults.get.HintText, amountHintSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, amountInputSelector, amountOverMaximum)
          formPostLinkCheck(paymentsIntoPensionSchemeUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
          errorAboveElementCheck(user.specificExpectedResults.get.overseasPensionSchemeErrorAmountOverMaximum)
          errorSummaryCheck(user.specificExpectedResults.get.overseasPensionSchemeErrorAmountOverMaximum, expectedAmountErrorHref)
        }

      }
    }

    "redirect to did your employer pay question page when user selects 'yes' with a valid amount" which {

      lazy val form: Map[String, String] = Map(
        RadioButtonAmountForm.yesNo -> RadioButtonAmountForm.yes, RadioButtonAmountForm.amount2 -> existingAmount)
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val viewModel = aPaymentsIntoOverseasPensionsViewModel.copy(paymentsIntoOverseasPensionsAmount = None,
          paymentsIntoOverseasPensionsQuestions = None)
        insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(
          paymentsIntoOverseasPensions = viewModel)), aUserRequest)
        urlPost(fullUrl(paymentsIntoPensionSchemeUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))

      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        //todo redirect to did your employer pay into your overseas pension scheme
        result.header("location") shouldBe Some(paymentsIntoPensionSchemeUrl(taxYearEOY))
      }

      "updates payment into overseas pension page to  Some(true) with valid amount" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.paymentsIntoOverseasPensions.paymentsIntoOverseasPensionsQuestions shouldBe Some(true)
        cyaModel.pensions.paymentsIntoOverseasPensions.paymentsIntoOverseasPensionsAmount shouldBe Some(BigDecimal(existingAmount))
      }
    }

    "redirect to did your employer pay question page when user selects 'No' " which {

      lazy val form: Map[String, String] = Map(
        RadioButtonAmountForm.yesNo -> RadioButtonAmountForm.no, RadioButtonAmountForm.amount2 -> "")
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val viewModel = aPaymentsIntoOverseasPensionsViewModel.copy(paymentsIntoOverseasPensionsAmount = None,
          paymentsIntoOverseasPensionsQuestions = None)
        insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(
          paymentsIntoOverseasPensions = viewModel)), aUserRequest)
        urlPost(fullUrl(paymentsIntoPensionSchemeUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))

      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        //todo redirect to did your employer pay into your overseas pension scheme
        result.header("location") shouldBe Some(paymentsIntoPensionSchemeUrl(taxYearEOY))
      }

      "update payment into pension to  Some(false) with amount set to None" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.paymentsIntoOverseasPensions.paymentsIntoOverseasPensionsQuestions shouldBe Some(false)
        cyaModel.pensions.paymentsIntoOverseasPensions.paymentsIntoOverseasPensionsAmount shouldBe None
      }
    }

    "redirect to did your employer pay question page when user selects 'No' when there is existing amount" which {

      lazy val form: Map[String, String] = Map(
        RadioButtonAmountForm.yesNo -> RadioButtonAmountForm.no, RadioButtonAmountForm.amount2 -> "")
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val viewModel = aPaymentsIntoOverseasPensionsViewModel.copy(paymentsIntoOverseasPensionsAmount = Some(BigDecimal(existingAmount)),
          paymentsIntoOverseasPensionsQuestions = Some(true))
        insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(
          paymentsIntoOverseasPensions = viewModel)), aUserRequest)
        urlPost(fullUrl(paymentsIntoPensionSchemeUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))

      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        //todo redirect to did your employer pay into your overseas pension scheme
        result.header("location") shouldBe Some(paymentsIntoPensionSchemeUrl(taxYearEOY))
      }

      "update payment into pension to  Some(false) with amount set to None" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.paymentsIntoOverseasPensions.paymentsIntoOverseasPensionsQuestions shouldBe Some(false)
        cyaModel.pensions.paymentsIntoOverseasPensions.paymentsIntoOverseasPensionsAmount shouldBe None
      }
    }


  }

}
