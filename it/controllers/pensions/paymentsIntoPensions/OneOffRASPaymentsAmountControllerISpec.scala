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
import models.mongo.{PensionsCYAModel, PensionsUserData}
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.PaymentIntoPensions._
import utils.PageUrls.fullUrl
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

class OneOffRASPaymentsAmountControllerISpec extends IntegrationTest with ViewHelpers with BeforeAndAfterEach with PensionsDatabaseHelper {
  private def pensionsUsersData(pensionsCyaModel: PensionsCYAModel): PensionsUserData = {
    PensionsUserDataBuilder.aPensionsUserData.copy(
      isPriorSubmission = false,
      pensions = pensionsCyaModel
    )
  }

  val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  ".show" should {
    "render Total one off payments into relief at source (RAS) pensions page with no value when no cya data" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(
          oneOffRasPaymentPlusTaxReliefQuestion = Some(true), totalOneOffRasPaymentPlusTaxRelief = None)
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)), aUserRequest)
        urlGet(fullUrl(oneOffReliefAtSourcePaymentsAmountUrl(taxYearEOY)),
          follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an OK status" in {
        result.status shouldBe OK
      }
    }

    "render Total one off payments into relief at source (RAS) pensions page prefilled when cya data" which {

      val existingAmount: String = "999.88"
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(
          oneOffRasPaymentPlusTaxReliefQuestion = Some(true), totalOneOffRasPaymentPlusTaxRelief = Some(BigDecimal(existingAmount)))
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)), aUserRequest)
        urlGet(fullUrl(oneOffReliefAtSourcePaymentsAmountUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }
      "has an OK status" in {
        result.status shouldBe OK
      }
    }

    "redirect to the oneOffRasPensionPaymentQuestion page if the question has not been answered" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(
          oneOffRasPaymentPlusTaxReliefQuestion = None, totalOneOffRasPaymentPlusTaxRelief = None)
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)), aUserRequest)
        urlGet(fullUrl(oneOffReliefAtSourcePaymentsAmountUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has a SEE_OTHER status and redirects correctly" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(reliefAtSourceOneOffPaymentsUrl(taxYearEOY)) shouldBe true
      }
    }

    "redirect to the oneOffRasPaymentQuestion page if the oneOffRasPaymentQuestion question has been answered as false" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(
          oneOffRasPaymentPlusTaxReliefQuestion = Some(false), totalOneOffRasPaymentPlusTaxRelief = None)
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)), aUserRequest)
        urlGet(fullUrl(oneOffReliefAtSourcePaymentsAmountUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(reliefAtSourceOneOffPaymentsUrl(taxYearEOY)) shouldBe true
      }
    }

    "redirect to the ReliefAtSourcePaymentsAmount page if 'totalRASPaymentsAndTaxRelief' is missing" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(
          totalRASPaymentsAndTaxRelief = None)
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)), aUserRequest)
        urlGet(fullUrl(oneOffReliefAtSourcePaymentsAmountUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
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
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an SEE_OTHER status and redirects to the CYA page" in {
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
          oneOffRasPaymentPlusTaxReliefQuestion = Some(true), totalOneOffRasPaymentPlusTaxRelief = None)
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)), aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(oneOffReliefAtSourcePaymentsAmountUrl(taxYearEOY)), body = emptyForm,
          follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has the correct status" in {
        result.status shouldBe BAD_REQUEST
      }

    }

    "return an error when form is submitted with an invalid format input" which {

      val amountInvalidFormat = "invalid"
      val invalidFormatForm: Map[String, String] = Map(AmountForm.amount -> amountInvalidFormat)

      lazy val result: WSResponse = {
        dropPensionsDB()
        val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(
          oneOffRasPaymentPlusTaxReliefQuestion = Some(true), totalOneOffRasPaymentPlusTaxRelief = None)
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)), aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(oneOffReliefAtSourcePaymentsAmountUrl(taxYearEOY)), body = invalidFormatForm,
          follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has the correct status" in {
        result.status shouldBe BAD_REQUEST
      }
    }

    "return an error when form is submitted with input over maximum allowed value" which {

      val amountOverMaximum = "100,000,000,000"
      val overMaximumForm: Map[String, String] = Map(AmountForm.amount -> amountOverMaximum)

      lazy val result: WSResponse = {
        dropPensionsDB()
        val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(
          oneOffRasPaymentPlusTaxReliefQuestion = Some(true), totalOneOffRasPaymentPlusTaxRelief = None)
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)), aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(oneOffReliefAtSourcePaymentsAmountUrl(taxYearEOY)), body = overMaximumForm,
          follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has the correct status" in {
        result.status shouldBe BAD_REQUEST
      }
    }

    "redirect to the ReliefAtSourcePaymentsAmount page if 'totalRASPaymentsAndTaxRelief' is missing when form is submitted with an error" which {

      val amountOverMaximum = "100,000,000,000"
      val overMaximumForm: Map[String, String] = Map(AmountForm.amount -> amountOverMaximum)

      lazy val result: WSResponse = {
        dropPensionsDB()
        val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(
          totalRASPaymentsAndTaxRelief = None)
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)), aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(oneOffReliefAtSourcePaymentsAmountUrl(taxYearEOY)), body = overMaximumForm,
          follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an SEE_OTHER status and redirects to the RAS payments amount page" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(reliefAtSourcePaymentsAndTaxReliefAmountUrl(taxYearEOY)) shouldBe true
      }
    }

    "redirect to the total RAS payments page when a valid amount is submitted and update the session amount" which {

      val validAmount = "100.22"
      val validForm: Map[String, String] = Map(AmountForm.amount -> validAmount)

      lazy val result: WSResponse = {
        dropPensionsDB()
        val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(
          totalOneOffRasPaymentPlusTaxRelief = None, totalPaymentsIntoRASQuestion = Some(true))
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)), aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(oneOffReliefAtSourcePaymentsAmountUrl(taxYearEOY)), body = validForm,
          follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(totalPaymentsIntoRASUrl(taxYearEOY))
      }

      "updates totalOneOffRasPaymentPlusTaxRelief to Some(100.22)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.paymentsIntoPension.totalOneOffRasPaymentPlusTaxRelief shouldBe Some(BigDecimal(validAmount))
        cyaModel.pensions.paymentsIntoPension.totalPaymentsIntoRASQuestion shouldBe None
      }
    }
  }
}
