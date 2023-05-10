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
import utils.PageUrls.PaymentIntoPensions.{checkPaymentsIntoPensionCyaUrl, reliefAtSourcePensionsUrl, workplacePensionAmount}
import utils.PageUrls.fullUrl
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}
import views.WorkplaceAmountSpec.CommonExpectedEN._
import views.WorkplaceAmountSpec.ExpectedIndividualEN._
import views.WorkplaceAmountSpec.Selectors._
import views.WorkplaceAmountSpec._

// scalastyle:off magic.number
class WorkplaceAmountControllerISpec extends IntegrationTest with ViewHelpers with BeforeAndAfterEach with PensionsDatabaseHelper {

  private def pensionsUsersData(pensionsCyaModel: PensionsCYAModel): PensionsUserData = {
    PensionsUserDataBuilder.aPensionsUserData.copy(
      isPriorSubmission = false,
      pensions = pensionsCyaModel
    )
  }

  val userScenarios: Seq[UserScenario[_, _]] = Seq.empty
  ".show" should {
    "render how much did you pay into your workplace pensions amount page with no pre filling" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(
          workplacePensionPaymentsQuestion = Some(true), totalWorkplacePensionPayments = None)
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)), aUserRequest)
        urlGet(
          fullUrl(workplacePensionAmount(taxYearEOY)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an OK status" in {
        result.status shouldBe OK
      }

      implicit def document: () => Document = () => Jsoup.parse(result.body)

      titleCheck(expectedTitle)
      h1Check(expectedHeading)
      captionCheck(expectedCaption(taxYearEOY), captionSelector)
      textOnPageCheck(expectedParagraph, paragraphSelector(1))
      textOnPageCheck(expectedBullet1, bulletListSelector(1))
      textOnPageCheck(expectedBullet2, bulletListSelector(2))
      textOnPageCheck(expectedYouCanFindThisOut, paragraphSelector(2))
      textOnPageCheck(hintText, hintTextSelector)
      textOnPageCheck(poundPrefixText, poundPrefixSelector)
      inputFieldValueCheck(amountInputName, inputSelector, "")
      buttonCheck(buttonText, continueButtonSelector)
      formPostLinkCheck(workplacePensionAmount(taxYearEOY), formSelector)
      welshToggleCheck(isWelsh = false)
      
    }

    "render how much did you pay into your workplace pensions amount page when cya data" which {

      val existingAmount: String = "999.88"
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(
          workplacePensionPaymentsQuestion = Some(true), totalWorkplacePensionPayments = Some(BigDecimal(existingAmount)))
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)), aUserRequest)
        urlGet(fullUrl(workplacePensionAmount(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }
      "has an OK status" in {
        result.status shouldBe OK
      }

      implicit def document: () => Document = () => Jsoup.parse(result.body)

      titleCheck(expectedTitle)
      h1Check(expectedHeading)
      captionCheck(expectedCaption(taxYearEOY), captionSelector)
      textOnPageCheck(expectedParagraph, paragraphSelector(1))
      textOnPageCheck(expectedBullet1, bulletListSelector(1))
      textOnPageCheck(expectedBullet2, bulletListSelector(2))
      textOnPageCheck(expectedYouCanFindThisOut, paragraphSelector(2))
      textOnPageCheck(hintText, hintTextSelector)
      textOnPageCheck(poundPrefixText, poundPrefixSelector)
      inputFieldValueCheck(amountInputName, inputSelector, existingAmount)
      buttonCheck(buttonText, continueButtonSelector)
      formPostLinkCheck(workplacePensionAmount(taxYearEOY), formSelector)
      welshToggleCheck(isWelsh = false)

    }

    "redirect to the ReliefAtSourcePensions question page if the previous question has not been answered" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(
          workplacePensionPaymentsQuestion = None, totalWorkplacePensionPayments = None)
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)), aUserRequest)
        urlGet(fullUrl(workplacePensionAmount(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(reliefAtSourcePensionsUrl(taxYearEOY)) shouldBe true
      }

    }

    "redirect to the ReliefAtSourcePensions question page if the workplaceQuestion has been answered as false" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(
          workplacePensionPaymentsQuestion = Some(false), totalWorkplacePensionPayments = None)
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)), aUserRequest)
        urlGet(fullUrl(workplacePensionAmount(taxYearEOY)), follow = false,
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
        authoriseAgentOrIndividual(isAgent = false)
        // no cya insert
        urlGet(fullUrl(workplacePensionAmount(taxYearEOY)), follow = false,
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
          workplacePensionPaymentsQuestion = Some(true), totalWorkplacePensionPayments = None)
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)), aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(workplacePensionAmount(taxYearEOY)), body = emptyForm,
          follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has the correct status" in {
        result.status shouldBe BAD_REQUEST
      }
      implicit def document: () => Document = () => Jsoup.parse(result.body)


      titleCheck(expectedErrorTitle)
      h1Check(expectedHeading)
      captionCheck(expectedCaption(taxYearEOY), captionSelector)
      textOnPageCheck(expectedParagraph, paragraphSelector(1))
      textOnPageCheck(expectedBullet1, bulletListSelector(1))
      textOnPageCheck(expectedBullet2, bulletListSelector(2))
      textOnPageCheck(expectedYouCanFindThisOut, paragraphSelector(2))
      textOnPageCheck(hintText, hintTextSelector)
      textOnPageCheck(poundPrefixText, poundPrefixSelector)
      inputFieldValueCheck(amountInputName, inputSelector, amountEmpty)
      buttonCheck(buttonText, continueButtonSelector)
      formPostLinkCheck(workplacePensionAmount(taxYearEOY), formSelector)
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
          workplacePensionPaymentsQuestion = Some(true), totalWorkplacePensionPayments = None)
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)), aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(workplacePensionAmount(taxYearEOY)), body = invalidFormatForm,
          follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has the correct status" in {
        result.status shouldBe BAD_REQUEST
      }

      implicit def document: () => Document = () => Jsoup.parse(result.body)


      titleCheck(expectedErrorTitle)
      h1Check(expectedHeading)
      captionCheck(expectedCaption(taxYearEOY), captionSelector)
      textOnPageCheck(expectedParagraph, paragraphSelector(1))
      textOnPageCheck(expectedBullet1, bulletListSelector(1))
      textOnPageCheck(expectedBullet2, bulletListSelector(2))
      textOnPageCheck(expectedYouCanFindThisOut, paragraphSelector(2))
      textOnPageCheck(hintText, hintTextSelector)
      textOnPageCheck(poundPrefixText, poundPrefixSelector)
      inputFieldValueCheck(amountInputName, inputSelector, amountInvalidFormat)
      buttonCheck(buttonText, continueButtonSelector)
      formPostLinkCheck(workplacePensionAmount(taxYearEOY), formSelector)
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
          workplacePensionPaymentsQuestion = Some(true), totalWorkplacePensionPayments = None)
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)), aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(workplacePensionAmount(taxYearEOY)), body = overMaximumForm,
          follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has the correct status" in {
        result.status shouldBe BAD_REQUEST
      }

      implicit def document: () => Document = () => Jsoup.parse(result.body)
      
      titleCheck(expectedErrorTitle)
      h1Check(expectedHeading)
      captionCheck(expectedCaption(taxYearEOY), captionSelector)
      textOnPageCheck(expectedParagraph, paragraphSelector(1))
      textOnPageCheck(expectedBullet1, bulletListSelector(1))
      textOnPageCheck(expectedBullet2, bulletListSelector(2))
      textOnPageCheck(expectedYouCanFindThisOut, paragraphSelector(2))
      textOnPageCheck(hintText, hintTextSelector)
      textOnPageCheck(poundPrefixText, poundPrefixSelector)
      inputFieldValueCheck(amountInputName, inputSelector, amountOverMaximum)
      buttonCheck(buttonText, continueButtonSelector)
      formPostLinkCheck(workplacePensionAmount(taxYearEOY), formSelector)
      errorSummaryCheck(maxAmountErrorText, expectedErrorHref)
      errorAboveElementCheck(maxAmountErrorText)
      welshToggleCheck(isWelsh = false)
    }

    "redirect to the CYA page when a valid amount is submitted and update the session amount completing the journey" which {

      val validAmount = "100.22"
      val validForm: Map[String, String] = Map(AmountForm.amount -> validAmount)

      lazy val result: WSResponse = {
        dropPensionsDB()
        val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(
          workplacePensionPaymentsQuestion = Some(true), totalWorkplacePensionPayments = None)
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)), aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(workplacePensionAmount(taxYearEOY)), body = validForm, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
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
