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

package controllers.pensions.incomeFromPensions

import builders.IncomeFromPensionsViewModelBuilder.{aStatePensionIncomeFromPensionsViewModel, anIncomeFromPensionsViewModel}
import builders.PensionsCYAModelBuilder.aPensionsCYAModel
import builders.PensionsUserDataBuilder.{aPensionsUserData, pensionsUserDataWithIncomeFromPensions}
import builders.StateBenefitViewModelBuilder.{aMinimalStatePensionViewModel, aStatePensionNoAddToCalculationViewModel, anStateBenefitViewModelOne}
import builders.UserBuilder.{aUser, aUserRequest}
import forms.RadioButtonAmountForm
import models.pension.statebenefits.StateBenefitViewModel
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.IncomeFromPensionsPages._
import utils.PageUrls.{fullUrl, overviewUrl}
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

class StatePensionLumpSumControllerISpec extends IntegrationTest with BeforeAndAfterEach with ViewHelpers with PensionsDatabaseHelper {

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  ".show" should {
    "Return OK response 'Did you get a State Pension lump sum?' page with correct content and no pre-filling" in {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(aUser.isAgent)
        dropPensionsDB()
        val pensionsViewModel = anIncomeFromPensionsViewModel.copy(statePensionLumpSum =
          Some(anStateBenefitViewModelOne.copy(amountPaidQuestion = None)))
        insertCyaData(pensionsUserDataWithIncomeFromPensions(pensionsViewModel))
        urlGet(fullUrl(statePensionLumpSumUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      result.status shouldBe OK
    }

    "Return OK response 'Did you get a State Pension lump sum?' page with correct content and yes pre-filled" in {
      implicit lazy val result: WSResponse = {
        dropPensionsDB()
        val pensionsViewModel = anIncomeFromPensionsViewModel.copy(statePensionLumpSum =
          Some(anStateBenefitViewModelOne.copy(amountPaidQuestion = Some(true), amount = Some(BigDecimal("100")))))
        insertCyaData(pensionsUserDataWithIncomeFromPensions(pensionsViewModel))
        authoriseAgentOrIndividual(aUser.isAgent)
        urlGet(fullUrl(statePensionLumpSumUrl(taxYearEOY)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }
      result.status shouldBe OK
    }

    "redirect to the first page in journey if the previous question has not been answered" in {
      val data = aPensionsUserData.copy(
        pensions = aPensionsCYAModel.copy(
          incomeFromPensions = anIncomeFromPensionsViewModel.copy(
            statePension = Some(anStateBenefitViewModelOne.copy(
              amountPaidQuestion = None,
              amount = None,
              startDateQuestion = None
            ))
          )))

      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        insertCyaData(data)
        urlGet(fullUrl(statePensionLumpSumUrl(taxYearEOY)), !aUser.isAgent, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }
      result.status shouldBe SEE_OTHER
      result.header("location").contains(statePension(taxYearEOY))
    }
  }

  ".submit" should {
    "return BAD_REQUEST when no value is submitted" in {
      lazy val form: Map[String, String] = Map(RadioButtonAmountForm.yesNo -> "")

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        val pensionsViewModel = anIncomeFromPensionsViewModel.copy(statePensionLumpSum =
          Some(anStateBenefitViewModelOne.copy(amountPaidQuestion = None)))
        insertCyaData(pensionsUserDataWithIncomeFromPensions(pensionsViewModel))
        urlPost(fullUrl(statePensionLumpSumUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      result.status shouldBe BAD_REQUEST
    }

    "return a BAD_REQUEST when user selects yes but no amount" in {
      lazy val form: Map[String, String] = Map(RadioButtonAmountForm.yesNo -> RadioButtonAmountForm.yes)

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()

        val pensionsViewModel = anIncomeFromPensionsViewModel.copy(statePensionLumpSum =
          Some(anStateBenefitViewModelOne.copy(amountPaidQuestion = None)))
        insertCyaData(pensionsUserDataWithIncomeFromPensions(pensionsViewModel))

        urlPost(fullUrl(statePensionLumpSumUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      result.status shouldBe BAD_REQUEST
    }

    "redirect to the overview page if it is not end of year" in {
      lazy val form: Map[String, String] = Map(RadioButtonAmountForm.yesNo -> RadioButtonAmountForm.yes)

      lazy val result: WSResponse = {
        dropPensionsDB()

        val pensionsViewModel = anIncomeFromPensionsViewModel.copy(statePensionLumpSum =
          Some(anStateBenefitViewModelOne.copy(amountPaidQuestion = None)))
        insertCyaData(pensionsUserDataWithIncomeFromPensions(pensionsViewModel))

        authoriseAgentOrIndividual()

        urlPost(fullUrl(statePensionLumpSumUrl(taxYear)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)))
      }

      result.status shouldBe SEE_OTHER
      result.header("location") shouldBe Some(overviewUrl(taxYear))
    }

    "redirect to the 'Tax paid on state pension lump sum' page when submitting answer as 'Yes' with correct taxPaid amount" in {
      lazy val form: Map[String, String] = Map(RadioButtonAmountForm.yesNo -> "true", RadioButtonAmountForm.amount2 -> "42.24")

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()

        val pensionsViewModel = aStatePensionIncomeFromPensionsViewModel.copy(statePensionLumpSum = None)
        insertCyaData(pensionsUserDataWithIncomeFromPensions(pensionsViewModel))

        urlPost(
          fullUrl(statePensionLumpSumUrl(taxYearEOY)),
          body = form,
          follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
        )
      }

      result.status shouldBe SEE_OTHER
      result.header("location") shouldBe Some(taxOnLumpSumUrl(taxYearEOY))

      lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
      cyaModel.pensions.incomeFromPensions.statePensionLumpSum shouldBe Some(StateBenefitViewModel(
        amountPaidQuestion = Some(true), amount = Some(BigDecimal("42.24"))))
    }

    "redirect to the 'Add to calculation' page when submitting answer as 'No' with no further lump sum data" in {
      lazy val form: Map[String, String] = Map(RadioButtonAmountForm.yesNo -> RadioButtonAmountForm.no)

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        insertCyaData(pensionsUserDataWithIncomeFromPensions(aStatePensionIncomeFromPensionsViewModel.copy(
          statePension = Some(aStatePensionNoAddToCalculationViewModel),
          statePensionLumpSum = None
        )))
        urlPost(fullUrl(statePensionLumpSumUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      result.status shouldBe SEE_OTHER
      result.header("location") shouldBe Some(addToCalculationUrl(taxYearEOY))

      lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
      cyaModel.pensions.incomeFromPensions.statePensionLumpSum shouldBe Some(aMinimalStatePensionViewModel)
    }

    "redirect to the CYA page when updating answer to 'No', clearing prior data and model is now complete" in {
      lazy val form: Map[String, String] = Map(RadioButtonAmountForm.yesNo -> RadioButtonAmountForm.no)

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        insertCyaData(pensionsUserDataWithIncomeFromPensions(aStatePensionIncomeFromPensionsViewModel))
        urlPost(fullUrl(statePensionLumpSumUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      result.status shouldBe SEE_OTHER
      result.header("location") shouldBe Some(statePensionCyaUrl(taxYearEOY))

      lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
      cyaModel.pensions.incomeFromPensions shouldBe aStatePensionIncomeFromPensionsViewModel.copy(
        statePensionLumpSum = Some(aMinimalStatePensionViewModel))
    }
  }
}
