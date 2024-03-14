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

import builders.IncomeFromPensionsViewModelBuilder.{
  aStatePensionIncomeFromPensionsNoAddToCalculationViewModel,
  aStatePensionIncomeFromPensionsViewModel,
  anIncomeFromPensionsViewModel
}
import builders.PensionsCYAModelBuilder.aPensionsCYAModel
import builders.PensionsUserDataBuilder.{aPensionsUserData, pensionsUserDataWithIncomeFromPensions}
import builders.StateBenefitViewModelBuilder.{
  aStatePensionLumpSumNoAddToCalculationViewModel,
  aStatePensionLumpSumViewModel,
  anStateBenefitViewModelOne,
  anStateBenefitViewModelTwo
}
import builders.UserBuilder.{aUser, aUserRequest}
import cats.implicits.{catsSyntaxOptionId, none}
import forms.{RadioButtonAmountForm, YesNoForm}
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.IncomeFromPensionsPages.{statePension, statePensionCyaUrl, statePensionLumpSumStartDateUrl, taxOnLumpSumUrl}
import utils.PageUrls.fullUrl
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

import java.time.LocalDate

// scalastyle:off magic.number
// TODO: We need to ideally rewrite these integration tests, as they are too tightly coupled.
class TaxPaidOnStatePensionLumpSumControllerISpec extends IntegrationTest with BeforeAndAfterEach with ViewHelpers with PensionsDatabaseHelper {

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty
  ".show" should {
    "Return OK response 'Did you pay tax on the State Pension lump sum?'page when there is no session data" in {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(aUser.isAgent)
        dropPensionsDB()
        val pensionsViewModel =
          anIncomeFromPensionsViewModel.copy(statePensionLumpSum = Some(anStateBenefitViewModelOne.copy(taxPaidQuestion = None)))
        insertCyaData(pensionsUserDataWithIncomeFromPensions(pensionsViewModel))
        urlGet(
          fullUrl(taxOnLumpSumUrl(taxYearEOY)),
          follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      result.status shouldBe OK
    }

    "Return OK response 'Did you pay tax on the State Pension lump sum?' page with correct content and yes pre-filled" in {

      implicit lazy val result: WSResponse = {
        dropPensionsDB()
        val pensionsViewModel = anIncomeFromPensionsViewModel.copy(statePensionLumpSum =
          Some(anStateBenefitViewModelOne.copy(taxPaidQuestion = Some(true), taxPaid = Some(BigDecimal("100")))))
        insertCyaData(pensionsUserDataWithIncomeFromPensions(pensionsViewModel))
        authoriseAgentOrIndividual(aUser.isAgent)
        urlGet(fullUrl(taxOnLumpSumUrl(taxYearEOY)), headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }
      result.status shouldBe OK
    }

    "redirect to the first page in journey if the previous question has not been answered" in {
      val data = aPensionsUserData.copy(
        pensions = aPensionsCYAModel.copy(
          incomeFromPensions = anIncomeFromPensionsViewModel.copy(
            statePensionLumpSum = Some(anStateBenefitViewModelTwo.copy(
              amountPaidQuestion = None
            ))
          )))

      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        insertCyaData(data)
        urlGet(
          fullUrl(taxOnLumpSumUrl(taxYearEOY)),
          !aUser.isAgent,
          follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }
      result.status shouldBe SEE_OTHER
      result.header("location").contains(statePension(taxYearEOY))
    }
  }

  ".submit" should {

    "return a BAD_REQUEST" when {
      "no value is submitted" in {
        lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> "")

        lazy val result: WSResponse = {
          dropPensionsDB()
          val pensionsViewModel =
            anIncomeFromPensionsViewModel.copy(statePensionLumpSum = Some(anStateBenefitViewModelOne.copy(taxPaidQuestion = None)))

          authoriseAgentOrIndividual(aUser.isAgent)
          insertCyaData(pensionsUserDataWithIncomeFromPensions(pensionsViewModel))
          urlPost(
            fullUrl(taxOnLumpSumUrl(taxYearEOY)),
            body = form,
            follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        result.status shouldBe BAD_REQUEST
      }

      "user selects yes but no amount" in {
        lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual()

          val pensionsViewModel =
            anIncomeFromPensionsViewModel.copy(statePensionLumpSum = Some(anStateBenefitViewModelOne.copy(taxPaidQuestion = None)))
          insertCyaData(pensionsUserDataWithIncomeFromPensions(pensionsViewModel))

          urlPost(
            fullUrl(taxOnLumpSumUrl(taxYearEOY)),
            body = form,
            follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        result.status shouldBe BAD_REQUEST
      }
    }

    "redirect to the 'State Pension Lump Sum start date' page and update question to 'Yes' with correct taxPaid amount" in {
      val amount                         = BigDecimal(42.24)
      lazy val form: Map[String, String] = Map(RadioButtonAmountForm.yesNo -> "true", RadioButtonAmountForm.amount2 -> s"$amount")

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        val pensionsViewModel =
          anIncomeFromPensionsViewModel.copy(statePensionLumpSum = Some(
            anStateBenefitViewModelOne
              .copy(startDateQuestion = none[Boolean], startDate = none[LocalDate], taxPaidQuestion = true.some, taxPaid = amount.some)))
        insertCyaData(pensionsUserDataWithIncomeFromPensions(pensionsViewModel))

        urlPost(
          fullUrl(taxOnLumpSumUrl(taxYearEOY)),
          body = form,
          follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
        )
      }

      result.status shouldBe SEE_OTHER
      result.header("location") shouldBe Some(statePensionLumpSumStartDateUrl(taxYearEOY))

      lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
      cyaModel.pensions.incomeFromPensions.statePensionLumpSum.get.taxPaidQuestion shouldBe Some(true)
      cyaModel.pensions.incomeFromPensions.statePensionLumpSum.get.taxPaid shouldBe Some(BigDecimal("42.24"))
    }

    "redirect to the CYA page and update question to 'Yes' with correct taxPaid amount when model is now completed" in {
      lazy val form: Map[String, String] = Map(RadioButtonAmountForm.yesNo -> "true", RadioButtonAmountForm.amount2 -> "42.24")

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        val pensionsViewModel = aStatePensionIncomeFromPensionsViewModel.copy(statePensionLumpSum =
          Some(aStatePensionLumpSumViewModel.copy(taxPaidQuestion = Some(false), taxPaid = None)))
        insertCyaData(pensionsUserDataWithIncomeFromPensions(pensionsViewModel))

        urlPost(
          fullUrl(taxOnLumpSumUrl(taxYearEOY)),
          body = form,
          follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
        )
      }

      result.status shouldBe SEE_OTHER
      result.header("location") shouldBe Some(statePensionCyaUrl(taxYearEOY))

      lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
      cyaModel.pensions.incomeFromPensions.statePensionLumpSum.get.taxPaidQuestion shouldBe Some(true)
      cyaModel.pensions.incomeFromPensions.statePensionLumpSum.get.taxPaid shouldBe Some(BigDecimal("42.24"))
    }

    "redirect to the 'State Pension Lump Sum start date' page and update question to No and delete the tax paid amount when user selects no" in {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        val lumpSum = aStatePensionLumpSumNoAddToCalculationViewModel.copy(startDateQuestion = none[Boolean], startDate = none[LocalDate])
        insertCyaData(
          pensionsUserDataWithIncomeFromPensions(aStatePensionIncomeFromPensionsNoAddToCalculationViewModel.copy(statePensionLumpSum = lumpSum.some)))

        urlPost(
          fullUrl(taxOnLumpSumUrl(taxYearEOY)),
          body = form,
          follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      result.status shouldBe SEE_OTHER
      result.header("location") shouldBe Some(statePensionLumpSumStartDateUrl(taxYearEOY))

      lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
      cyaModel.pensions.incomeFromPensions.statePensionLumpSum.get.taxPaidQuestion shouldBe Some(false)
      cyaModel.pensions.incomeFromPensions.statePensionLumpSum.get.taxPaid shouldBe None
    }
  }
}
// scalastyle:on magic.number
