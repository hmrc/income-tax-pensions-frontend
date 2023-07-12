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

package controllers.pensions.paymentsIntoPensions

import builders.PaymentsIntoPensionVewModelBuilder.aPaymentsIntoPensionViewModel
import builders.PensionsCYAModelBuilder._
import builders.PensionsUserDataBuilder
import builders.UserBuilder._
import forms.AmountForm
import models.mongo.{PensionsCYAModel, PensionsUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.PaymentIntoPensions._
import utils.PageUrls.fullUrl
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}
import views.pensions.paymentsIntoPensions.RetirementAnnuityAmountSpec.CommonExpectedEN._
import views.pensions.paymentsIntoPensions.RetirementAnnuityAmountSpec.ExpectedIndividualEN._
import views.pensions.paymentsIntoPensions.RetirementAnnuityAmountSpec.Selectors._
import views.pensions.paymentsIntoPensions.RetirementAnnuityAmountSpec._


class RetirementAnnuityAmountControllerISpec extends IntegrationTest with ViewHelpers with BeforeAndAfterEach with PensionsDatabaseHelper {

  private def pensionsUsersData(pensionsCyaModel: PensionsCYAModel): PensionsUserData = {
    PensionsUserDataBuilder.aPensionsUserData.copy(isPriorSubmission = false , pensions = pensionsCyaModel)
  }


  val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  ".show" should {
    "render How much did you pay into your retirement annuity contracts page with no value when no cya data" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(
          retirementAnnuityContractPaymentsQuestion = Some(true), totalRetirementAnnuityContractPayments = None)
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)))
        urlGet(fullUrl(retirementAnnuityAmountUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an OK status" in {
        result.status shouldBe OK
      }

      implicit def document: () => Document = () => Jsoup.parse(result.body)

      titleCheck(expectedTitle)
      h1Check(expectedHeading)
      captionCheck(expectedCaption(taxYearEOY), captionSelector)
      textOnPageCheck(onlyIncludePayment, paragraphSelector)
      textOnPageCheck(hintText, hintTextSelector)
      textOnPageCheck(poundPrefixText, poundPrefixSelector)
      inputFieldValueCheck(amountInputName, inputSelector, "")
      buttonCheck(buttonText, continueButtonSelector)
      formPostLinkCheck(retirementAnnuityAmountUrl(taxYearEOY), formSelector)
      welshToggleCheck(isWelsh = false)
    }

    "render How much did you pay into your retirement annuity contracts page prefilled when cya data" which {

      val existingAmount: String = "999.88"
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(
          retirementAnnuityContractPaymentsQuestion = Some(true), totalRetirementAnnuityContractPayments = Some(BigDecimal(existingAmount)))
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)))
        urlGet(fullUrl(retirementAnnuityAmountUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an OK status" in {
        result.status shouldBe OK
      }

      implicit def document: () => Document = () => Jsoup.parse(result.body)

      titleCheck(expectedTitle)
      h1Check(expectedHeading)
      captionCheck(expectedCaption(taxYearEOY), captionSelector)
      textOnPageCheck(onlyIncludePayment, paragraphSelector)
      textOnPageCheck(hintText, hintTextSelector)
      textOnPageCheck(poundPrefixText, poundPrefixSelector)
      inputFieldValueCheck(amountInputName, inputSelector, existingAmount)
      buttonCheck(buttonText, continueButtonSelector)
      formPostLinkCheck(retirementAnnuityAmountUrl(taxYearEOY), formSelector)
      welshToggleCheck(isWelsh = false)
    }

    "redirect to the ReliefAtSourcePensions question page if the previous question has not been answered" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(
          retirementAnnuityContractPaymentsQuestion = None, totalRetirementAnnuityContractPayments = None)
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)))
        urlGet(fullUrl(retirementAnnuityAmountUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(reliefAtSourcePensionsUrl(taxYearEOY)) shouldBe true
      }
    }

    "redirect to the ReliefAtSourcePensions question page if the retirementAnnuityContract question has been answered as false" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(
          retirementAnnuityContractPaymentsQuestion = Some(false), totalRetirementAnnuityContractPayments = None)
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)))
        urlGet(fullUrl(retirementAnnuityAmountUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(reliefAtSourcePensionsUrl(taxYearEOY)) shouldBe true
      }
    }

    "redirect to the CYA page if there is no session data" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        urlGet(fullUrl(retirementAnnuityAmountUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }
      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkPaymentsIntoPensionCyaUrl(taxYearEOY)) shouldBe true
      }
    }
  }

  ".submit" should {

    "return an error when form is submitted with no input entry" which {
      val amountEmpty = ""
      val emptyForm: Map[String, String] = Map(AmountForm.amount -> amountEmpty)
      lazy val result: WSResponse = {
        dropPensionsDB()
        val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(
          retirementAnnuityContractPaymentsQuestion = Some(true), totalRetirementAnnuityContractPayments = None)
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)))
        authoriseAgentOrIndividual()
        urlPost(fullUrl(retirementAnnuityAmountUrl(taxYearEOY)), body = emptyForm,
          follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }
      "has the correct status" in {
        result.status shouldBe BAD_REQUEST
      }

      implicit def document: () => Document = () => Jsoup.parse(result.body)

      titleCheck(expectedErrorTitle)
      h1Check(expectedHeading)
      captionCheck(expectedCaption(taxYearEOY), captionSelector)
      textOnPageCheck(onlyIncludePayment, paragraphSelector)
      textOnPageCheck(hintText, hintTextSelector)
      textOnPageCheck(poundPrefixText, poundPrefixSelector)
      inputFieldValueCheck(amountInputName, inputSelector, "")
      buttonCheck(buttonText, continueButtonSelector)
      formPostLinkCheck(retirementAnnuityAmountUrl(taxYearEOY), formSelector)
      errorSummaryCheck(emptyErrorText, expectedErrorHref)
      errorAboveElementCheck(emptyErrorText)
      welshToggleCheck(isWelsh = false)
    }

    "return an error when form is submitted with an invalid format input" which {

      val amountInvalidFormat = "invalid"
      val invalidFormatForm: Map[String, String] = Map(AmountForm.amount -> amountInvalidFormat)

      lazy val result: WSResponse = {
        dropPensionsDB()
        val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(
          retirementAnnuityContractPaymentsQuestion = Some(true), totalRetirementAnnuityContractPayments = None)
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)))
        authoriseAgentOrIndividual()
        urlPost(fullUrl(retirementAnnuityAmountUrl(taxYearEOY)), body = invalidFormatForm,
          follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has the correct status" in {
        result.status shouldBe BAD_REQUEST
      }

      implicit def document: () => Document = () => Jsoup.parse(result.body)
      titleCheck(expectedErrorTitle)
      h1Check(expectedHeading)
      captionCheck(expectedCaption(taxYearEOY), captionSelector)
      textOnPageCheck(onlyIncludePayment, paragraphSelector)
      textOnPageCheck(hintText, hintTextSelector)
      textOnPageCheck(poundPrefixText, poundPrefixSelector)
      inputFieldValueCheck(amountInputName, inputSelector, "invalid")
      buttonCheck(buttonText, continueButtonSelector)
      formPostLinkCheck(retirementAnnuityAmountUrl(taxYearEOY), formSelector)
      errorSummaryCheck(invalidFormatErrorText, expectedErrorHref)
      errorAboveElementCheck(invalidFormatErrorText)
      welshToggleCheck(isWelsh = false)

    }

    "return an error when form is submitted with input over maximum allowed value" which {

      val amountOverMaximum = "100,000,000,000"
      val overMaximumForm: Map[String, String] = Map(AmountForm.amount -> amountOverMaximum)

      lazy val result: WSResponse = {
        dropPensionsDB()
        val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(
          retirementAnnuityContractPaymentsQuestion = Some(true), totalRetirementAnnuityContractPayments = None)
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)))
        authoriseAgentOrIndividual()
        urlPost(fullUrl(retirementAnnuityAmountUrl(taxYearEOY)),
          body = overMaximumForm, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }
      "has the correct status" in {
        result.status shouldBe BAD_REQUEST
      }
      
      implicit def document: () => Document = () => Jsoup.parse(result.body)

      titleCheck(expectedErrorTitle)
      h1Check(expectedHeading)
      captionCheck(expectedCaption(taxYearEOY), captionSelector)
      textOnPageCheck(onlyIncludePayment, paragraphSelector)
      textOnPageCheck(hintText, hintTextSelector)
      textOnPageCheck(poundPrefixText, poundPrefixSelector)
      inputFieldValueCheck(amountInputName, inputSelector, amountOverMaximum)
      buttonCheck(buttonText, continueButtonSelector)
      formPostLinkCheck(retirementAnnuityAmountUrl(taxYearEOY), formSelector)
      errorSummaryCheck(maxAmountErrorText, expectedErrorHref)
      errorAboveElementCheck(maxAmountErrorText)
      welshToggleCheck(isWelsh = false)
    }

    "redirect to the workplace pension page when a valid amount is submitted which doesnt complete CYA model and update the session amount" which {

      val validAmount = "1888.88"
      val validForm: Map[String, String] = Map(AmountForm.amount -> validAmount)

      lazy val result: WSResponse = {
        dropPensionsDB()
        val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(
          totalRetirementAnnuityContractPayments = None, workplacePensionPaymentsQuestion = None)
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)))
        authoriseAgentOrIndividual()
        urlPost(fullUrl(retirementAnnuityAmountUrl(taxYearEOY)),
          body = validForm, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(workplacePensionUrl(taxYearEOY))
      }

      "updates retirement annuity contract payments question to Some(true)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.paymentsIntoPension.totalRetirementAnnuityContractPayments shouldBe Some(BigDecimal(validAmount))
      }
    }

    "redirect to the CYA page when a valid amount is submitted which completes CYA model and update the session amount" which {

      val validAmount = "1888.88"
      val validForm: Map[String, String] = Map(AmountForm.amount -> validAmount)

      lazy val result: WSResponse = {
        dropPensionsDB()
        val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(
          retirementAnnuityContractPaymentsQuestion = Some(true), totalRetirementAnnuityContractPayments = None)
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)))
        authoriseAgentOrIndividual()
        urlPost(fullUrl(retirementAnnuityAmountUrl(taxYearEOY)),
          body = validForm, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(checkPaymentsIntoPensionCyaUrl(taxYearEOY))
      }

      "updates retirement annuity contract payments question to Some(true)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.paymentsIntoPension.totalRetirementAnnuityContractPayments shouldBe Some(BigDecimal(validAmount))
      }
    }
  }
}
