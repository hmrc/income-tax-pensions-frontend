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
import builders.PensionsUserDataBuilder.{aPensionsUserData, pensionsUserDataWithPaymentsIntoPensions}
import builders.UserBuilder._
import forms.AmountForm
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.pension.reliefs.PaymentsIntoPensionsViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.PaymentIntoPensions._
import utils.PageUrls._
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}
import views.pensions.paymentsIntoPensions.ReliefAtSourcePaymentsAndTaxReliefAmountSpec.CommonExpectedEN._
import views.pensions.paymentsIntoPensions.ReliefAtSourcePaymentsAndTaxReliefAmountSpec.ExpectedIndividualEN._
import views.pensions.paymentsIntoPensions.ReliefAtSourcePaymentsAndTaxReliefAmountSpec.Selectors._
import views.pensions.paymentsIntoPensions.ReliefAtSourcePaymentsAndTaxReliefAmountSpec._

class ReliefAtSourcePaymentsAndTaxReliefAmountControllerISpec
    extends IntegrationTest
    with ViewHelpers
    with BeforeAndAfterEach
    with PensionsDatabaseHelper {

  private def pensionsUsersData(pensionsCyaModel: PensionsCYAModel): PensionsUserData =
    PensionsUserDataBuilder.aPensionsUserData.copy(
      isPriorSubmission = false,
      pensions = pensionsCyaModel
    )

  val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  ".show" should {
    "render Total payments into relief at source (RAS) pensions, plus basic rate tax relief page with no value when no cya data" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(rasPensionPaymentQuestion = Some(true), totalRASPaymentsAndTaxRelief = None)
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)))
        urlGet(
          fullUrl(reliefAtSourcePaymentsAndTaxReliefAmountUrl(taxYearEOY)),
          follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
        )
      }

      "has an OK status" in {
        result.status shouldBe OK
      }

      implicit def document: () => Document = () => Jsoup.parse(result.body)

      titleCheck(expectedTitle)
      h1Check(expectedHeading)
      captionCheck(expectedCaption(taxYearEOY), captionSelector)
      textOnPageCheck(expectedWhereToFind, paragraphSelector(1))
      textOnPageCheck(expectedHowToWorkOut, paragraphSelector(2))
      textOnPageCheck(expectedCalculationHeading, insetSpanText(1))
      textOnPageCheck(expectedExampleCalculation, insetSpanText(2))
      textOnPageCheck(hintText, hintTextSelector)
      textOnPageCheck(poundPrefixText, poundPrefixSelector)
      inputFieldValueCheck(amountInputName, inputSelector, "")
      buttonCheck(buttonText, continueButtonSelector)
      formPostLinkCheck(reliefAtSourcePaymentsAndTaxReliefAmountUrl(taxYearEOY), formSelector)
      welshToggleCheck(isWelsh = false)
    }

    "render Total payments into relief at source (RAS) pensions, plus basic rate tax relief page prefilled when cya data" which {

      val existingAmount: String = "999.88"
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        val pensionsViewModel =
          aPaymentsIntoPensionViewModel.copy(rasPensionPaymentQuestion = Some(true), totalRASPaymentsAndTaxRelief = Some(BigDecimal(existingAmount)))
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)))
        urlGet(
          fullUrl(reliefAtSourcePaymentsAndTaxReliefAmountUrl(taxYearEOY)),
          follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
        )
      }

      "has an OK status" in {
        result.status shouldBe OK
      }

      implicit def document: () => Document = () => Jsoup.parse(result.body)

      titleCheck(expectedTitle)
      h1Check(expectedHeading)
      captionCheck(expectedCaption(taxYearEOY), captionSelector)
      textOnPageCheck(expectedWhereToFind, paragraphSelector(1))
      textOnPageCheck(expectedHowToWorkOut, paragraphSelector(2))
      textOnPageCheck(expectedCalculationHeading, insetSpanText(1))
      textOnPageCheck(expectedExampleCalculation, insetSpanText(2))
      textOnPageCheck(hintText, hintTextSelector)
      textOnPageCheck(poundPrefixText, poundPrefixSelector)
      inputFieldValueCheck(amountInputName, inputSelector, "999.88")
      buttonCheck(buttonText, continueButtonSelector)
      formPostLinkCheck(reliefAtSourcePaymentsAndTaxReliefAmountUrl(taxYearEOY), formSelector)
      welshToggleCheck(isWelsh = false)
    }

    "redirect to the rasPensionPaymentQuestion page if the question has not been answered" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(rasPensionPaymentQuestion = None, totalRASPaymentsAndTaxRelief = None)
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)))
        urlGet(
          fullUrl(reliefAtSourcePaymentsAndTaxReliefAmountUrl(taxYearEOY)),
          follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
        )
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(reliefAtSourcePensionsUrl(taxYearEOY)) shouldBe true
      }

    }

    "redirect to the rasPensionPaymentQuestion page if the rasPensionPaymentQuestion question has been answered as false" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(rasPensionPaymentQuestion = Some(false), totalRASPaymentsAndTaxRelief = None)
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)))
        urlGet(
          fullUrl(reliefAtSourcePaymentsAndTaxReliefAmountUrl(taxYearEOY)),
          follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
        )
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
        // no cya insert
        urlGet(
          fullUrl(reliefAtSourcePaymentsAndTaxReliefAmountUrl(taxYearEOY)),
          follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
        )
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkPaymentsIntoPensionCyaUrl(taxYearEOY)) shouldBe true
      }

    }

  }

  ".submit" should {
    "return an error" when {
      "form is submitted with no input entry" which {

        val emptyForm: Map[String, String] = Map(AmountForm.amount -> "")
        lazy val result: WSResponse = {
          dropPensionsDB()
          val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(rasPensionPaymentQuestion = Some(true), totalRASPaymentsAndTaxRelief = None)
          insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)))
          authoriseAgentOrIndividual()
          urlPost(
            fullUrl(reliefAtSourcePaymentsAndTaxReliefAmountUrl(taxYearEOY)),
            body = emptyForm,
            follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
          )
        }

        "has the correct status" in {
          result.status shouldBe BAD_REQUEST
        }

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(expectedErrorTitle)
        h1Check(expectedHeading)
        captionCheck(expectedCaption(taxYearEOY), captionSelector)
        textOnPageCheck(expectedWhereToFind, paragraphSelector(1))
        textOnPageCheck(expectedHowToWorkOut, paragraphSelector(2))
        textOnPageCheck(expectedCalculationHeading, insetSpanText(1))
        textOnPageCheck(expectedExampleCalculation, insetSpanText(2))
        textOnPageCheck(hintText, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, "")
        buttonCheck(buttonText, continueButtonSelector)
        formPostLinkCheck(reliefAtSourcePaymentsAndTaxReliefAmountUrl(taxYearEOY), formSelector)
        errorSummaryCheck(emptyErrorText, expectedErrorHref)
        errorAboveElementCheck(emptyErrorText)
        welshToggleCheck(isWelsh = false)

      }

      "form is submitted with an invalid format input" which {

        val invalidFormatForm: Map[String, String] = Map(AmountForm.amount -> "invalid")

        lazy val result: WSResponse = {
          dropPensionsDB()
          val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(rasPensionPaymentQuestion = Some(true), totalRASPaymentsAndTaxRelief = None)
          insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)))
          authoriseAgentOrIndividual()
          urlPost(
            fullUrl(reliefAtSourcePaymentsAndTaxReliefAmountUrl(taxYearEOY)),
            body = invalidFormatForm,
            follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
          )
        }

        "has the correct status" in {
          result.status shouldBe BAD_REQUEST
        }

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(expectedErrorTitle)
        h1Check(expectedHeading)
        captionCheck(expectedCaption(taxYearEOY), captionSelector)
        textOnPageCheck(expectedWhereToFind, paragraphSelector(1))
        textOnPageCheck(expectedHowToWorkOut, paragraphSelector(2))
        textOnPageCheck(expectedCalculationHeading, insetSpanText(1))
        textOnPageCheck(expectedExampleCalculation, insetSpanText(2))
        textOnPageCheck(hintText, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, "invalid")
        buttonCheck(buttonText, continueButtonSelector)
        formPostLinkCheck(reliefAtSourcePaymentsAndTaxReliefAmountUrl(taxYearEOY), formSelector)
        errorSummaryCheck(invalidFormatErrorText, expectedErrorHref)
        errorAboveElementCheck(invalidFormatErrorText)
        welshToggleCheck(isWelsh = false)
      }

      "form is submitted with input over maximum allowed value" which {

        val overMaximumForm: Map[String, String] = Map(AmountForm.amount -> "100,000,000,000")
        lazy val result: WSResponse = {
          dropPensionsDB()
          val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(rasPensionPaymentQuestion = Some(true), totalRASPaymentsAndTaxRelief = None)
          insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)))
          authoriseAgentOrIndividual()
          urlPost(
            fullUrl(reliefAtSourcePaymentsAndTaxReliefAmountUrl(taxYearEOY)),
            body = overMaximumForm,
            follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
          )
        }

        "has the correct status" in {
          result.status shouldBe BAD_REQUEST
        }

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(expectedErrorTitle)
        h1Check(expectedHeading)
        captionCheck(expectedCaption(taxYearEOY), captionSelector)
        textOnPageCheck(expectedWhereToFind, paragraphSelector(1))
        textOnPageCheck(expectedHowToWorkOut, paragraphSelector(2))
        textOnPageCheck(expectedCalculationHeading, insetSpanText(1))
        textOnPageCheck(expectedExampleCalculation, insetSpanText(2))
        textOnPageCheck(hintText, hintTextSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldValueCheck(amountInputName, inputSelector, "100,000,000,000")
        buttonCheck(buttonText, continueButtonSelector)
        formPostLinkCheck(reliefAtSourcePaymentsAndTaxReliefAmountUrl(taxYearEOY), formSelector)
        errorSummaryCheck(maxAmountErrorText, expectedErrorHref)
        errorAboveElementCheck(maxAmountErrorText)
        welshToggleCheck(isWelsh = false)
      }
    }

    "redirect to the 'RAS one-off' page when a valid amount is submitted and update the session amount" which {
      val validForm: Map[String, String] = Map(AmountForm.amount -> "100.22")
      val pensionsViewModel              = PaymentsIntoPensionsViewModel(rasPensionPaymentQuestion = Some(true))

      lazy val result: WSResponse = {
        dropPensionsDB()
        insertCyaData(pensionsUserDataWithPaymentsIntoPensions(pensionsViewModel))
        authoriseAgentOrIndividual()
        urlPost(
          fullUrl(reliefAtSourcePaymentsAndTaxReliefAmountUrl(taxYearEOY)),
          body = validForm,
          follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
        )
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(reliefAtSourceOneOffPaymentsUrl(taxYearEOY))
      }

      "updates the totalRASPaymentsAndTaxRelief amount" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.paymentsIntoPension shouldBe pensionsViewModel.copy(
          totalRASPaymentsAndTaxRelief = Some(100.22)
        )
      }
    }

    "redirect to the CYA page when a valid amount is submitted and completes the CYA data" which {
      val validForm: Map[String, String] = Map(AmountForm.amount -> "100.22")

      lazy val result: WSResponse = {
        dropPensionsDB()
        insertCyaData(aPensionsUserData)
        authoriseAgentOrIndividual()
        urlPost(
          fullUrl(reliefAtSourcePaymentsAndTaxReliefAmountUrl(taxYearEOY)),
          body = validForm,
          follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
        )
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(checkPaymentsIntoPensionCyaUrl(taxYearEOY))
      }

      "updates the totalRASPaymentsAndTaxRelief amount" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.paymentsIntoPension shouldBe aPaymentsIntoPensionViewModel.copy(totalRASPaymentsAndTaxRelief = Some(100.22))
      }
    }
  }
}
