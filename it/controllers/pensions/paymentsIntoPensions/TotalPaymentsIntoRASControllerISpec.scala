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

import builders.IncomeFromPensionsViewModelBuilder.anIncomeFromPensionEmptyViewModel
import builders.PensionAnnualAllowanceViewModelBuilder.aPensionAnnualAllowanceEmptyViewModel
import builders.PensionLifetimeAllowanceViewModelBuilder.aPensionLifetimeAllowancesEmptyViewModel
import builders.PensionsCYAModelBuilder.aPensionsCYAEmptyModel
import builders.PensionsUserDataBuilder
import builders.PensionsUserDataBuilder.aPensionsUserData
import builders.UnauthorisedPaymentsViewModelBuilder.anUnauthorisedPaymentsViewModel
import builders.UserBuilder.aUserRequest
import forms.YesNoForm
import models.mongo.PensionsCYAModel
import models.pension.reliefs.PaymentsIntoPensionViewModel
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.PaymentIntoPensions._
import utils.PageUrls.fullUrl
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

class TotalPaymentsIntoRASControllerISpec extends IntegrationTest with BeforeAndAfterEach with ViewHelpers with PensionsDatabaseHelper {

  private def pensionsUsersData(paymentsIntoPensionViewModel: PaymentsIntoPensionViewModel) = {
    PensionsUserDataBuilder.aPensionsUserData.copy(
      pensions = PensionsCYAModel(paymentsIntoPensionViewModel, aPensionAnnualAllowanceEmptyViewModel,
        aPensionLifetimeAllowancesEmptyViewModel, anIncomeFromPensionEmptyViewModel, anUnauthorisedPaymentsViewModel)
    )
  }

  private val requiredViewModel = PaymentsIntoPensionViewModel(
    rasPensionPaymentQuestion = Some(true),
    totalRASPaymentsAndTaxRelief = Some(BigDecimal(7400)),
    oneOffRasPaymentPlusTaxReliefQuestion = Some(true),
    totalOneOffRasPaymentPlusTaxRelief = Some(BigDecimal(1400))
  )

  val userScenarios: Seq[UserScenario[_,_]] =Seq.empty

  ".show" should {
    "render 'Total payments into RAS pensions' page with correct content and no pre-filling" which {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropPensionsDB()
        insertCyaData(pensionsUsersData(requiredViewModel), aUserRequest)
        urlGet(fullUrl(totalPaymentsIntoRASUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an OK status" in {
        result.status shouldBe OK
      }
    }

    "render 'Total payments into RAS pensions' page without a one-off amount" should {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropPensionsDB()
        insertCyaData(pensionsUsersData(requiredViewModel.copy(
          oneOffRasPaymentPlusTaxReliefQuestion = Some(false),
          totalOneOffRasPaymentPlusTaxRelief = None)
        ), aUserRequest)
        urlGet(fullUrl(totalPaymentsIntoRASUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an OK status" in {
        result.status shouldBe OK
      }
    }

    "render the page with the radio button pre-filled" should {

      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropPensionsDB()
        insertCyaData(pensionsUsersData(requiredViewModel.copy(totalPaymentsIntoRASQuestion = Some(true))), aUserRequest)
        urlGet(fullUrl(totalPaymentsIntoRASUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an OK status" in {
        result.status shouldBe OK
      }
    }

    "redirect to the check your answers CYA page when there is no CYA data" when {

      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropPensionsDB()
        urlGet(fullUrl(totalPaymentsIntoRASUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has a SEE_OTHER status and redirects correctly" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkPaymentsIntoPensionCyaUrl(taxYearEOY)) shouldBe true
      }
    }

    "redirect to the 'RAS Payments And Tax Relief Amount' page if does not have a RAS amount in CYA" when {

      val userDataModel = aPensionsUserData.copy(
        pensions = aPensionsCYAEmptyModel.copy(
          paymentsIntoPension = PaymentsIntoPensionViewModel(rasPensionPaymentQuestion = Some(true))))

      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropPensionsDB()
        insertCyaData(userDataModel, aUserRequest)
        urlGet(fullUrl(totalPaymentsIntoRASUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has a SEE_OTHER status and redirects correctly" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(reliefAtSourcePaymentsAndTaxReliefAmountUrl(taxYearEOY)) shouldBe true
      }
    }
  }

  ".submit" should {
    "return an error when form is submitted with no entry" which {
      lazy val invalidForm: Map[String, String] = Map(YesNoForm.yesNo -> "")

      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropPensionsDB()
        insertCyaData(pensionsUsersData(requiredViewModel), aUserRequest)
        urlPost(fullUrl(totalPaymentsIntoRASUrl(taxYearEOY)), body = invalidForm, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has the correct status" in {
        result.status shouldBe BAD_REQUEST
      }
    }

    "redirect to the next page and update the session value when the user submits 'yes'" should {
      lazy val yesForm: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropPensionsDB()
        insertCyaData(pensionsUsersData(requiredViewModel), aUserRequest)
        urlPost(fullUrl(totalPaymentsIntoRASUrl(taxYearEOY)), body = yesForm, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionTaxReliefNotClaimedUrl(taxYearEOY))
      }

      "updates totalPaymentsIntoRASQuestion to Some(true)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.paymentsIntoPension.totalPaymentsIntoRASQuestion shouldBe Some(true)

      }
    }

    "redirect to the 'RAS payments and tax relief amount' page" when {

      "the user submits 'no'" should {
        lazy val noForm: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)

        lazy val result: WSResponse = {
          authoriseAgentOrIndividual(isAgent = false)
          dropPensionsDB()
          insertCyaData(pensionsUsersData(requiredViewModel), aUserRequest)
          urlPost(fullUrl(totalPaymentsIntoRASUrl(taxYearEOY)), body = noForm, follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        "has a SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(reliefAtSourcePaymentsAndTaxReliefAmountUrl(taxYearEOY))
        }

        "updates totalPaymentsIntoRASQuestion to Some(false)" in {
          lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
          cyaModel.pensions.paymentsIntoPension.totalPaymentsIntoRASQuestion shouldBe Some(false)

        }
      }

      "the user does not have a value for 'totalRASPaymentsAndTaxRelief' in session and does not select an answer" should {
        lazy val invalidForm: Map[String, String] = Map(YesNoForm.yesNo -> "")

        val userDataModel = aPensionsUserData.copy(
          pensions = aPensionsCYAEmptyModel.copy(
            paymentsIntoPension = PaymentsIntoPensionViewModel(rasPensionPaymentQuestion = Some(true))))

        lazy val result: WSResponse = {
          authoriseAgentOrIndividual(isAgent = false)
          dropPensionsDB()
          insertCyaData(userDataModel, aUserRequest)
          urlPost(fullUrl(totalPaymentsIntoRASUrl(taxYearEOY)), body = invalidForm, follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        "has a SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(reliefAtSourcePaymentsAndTaxReliefAmountUrl(taxYearEOY))
        }

        "keep totalPaymentsIntoRASQuestion as None" in {
          lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
          cyaModel.pensions.paymentsIntoPension.totalPaymentsIntoRASQuestion shouldBe None

        }
      }
    }

    "redirect to the CYA page when the user does not have data in session" when {
      lazy val noForm: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)

      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropPensionsDB()
        urlPost(fullUrl(totalPaymentsIntoRASUrl(taxYearEOY)), body = noForm, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(checkPaymentsIntoPensionCyaUrl(taxYearEOY))
      }
    }
  }
}
