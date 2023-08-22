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

import builders.IncomeFromPensionsViewModelBuilder.{aUKIncomeFromPensionsViewModel, anIncomeFromPensionEmptyViewModel}
import builders.PensionsCYAModelBuilder.aPensionsCYAModel
import builders.PensionsUserDataBuilder
import builders.PensionsUserDataBuilder.pensionsUserDataWithIncomeFromPensions
import builders.UkPensionIncomeViewModelBuilder.anUkPensionIncomeViewModelOne
import builders.UserBuilder.aUser
import models.mongo.PensionsCYAModel
import play.api.http.HeaderNames
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.IncomeFromOverseasPensionsPages.overseasPensionsSchemeSummaryUrl
import utils.PageUrls.IncomeFromPensionsPages.{pensionSchemeSummaryUrl, ukPensionSchemePayments}
import utils.PageUrls.{fullUrl, overviewUrl, pensionSummaryUrl}
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

class PensionSchemeSummaryControllerISpec extends IntegrationTest with ViewHelpers with PensionsDatabaseHelper {

  override val userScenarios: Seq[UserScenario[_, _]] = Nil

  private def pensionsUsersData(pensionsCyaModel: PensionsCYAModel, isPrior: Boolean = false) =
    PensionsUserDataBuilder.aPensionsUserData.copy(isPriorSubmission = isPrior, pensions = pensionsCyaModel)

  val schemeIndex0 = 0

  ".show" should {
    "redirect to Overview Page when in year" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        insertCyaData(pensionsUsersData(aPensionsCYAModel))
        urlGet(fullUrl(pensionSchemeSummaryUrl(taxYear, Some(schemeIndex0))), !aUser.isAgent, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)))
      }

      result.status shouldBe SEE_OTHER
      result.headers("Location").head shouldBe overviewUrl(taxYear)
    }

       "show page when EOY" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        insertCyaData(pensionsUsersData(aPensionsCYAModel))
        urlGet(fullUrl(pensionSchemeSummaryUrl(taxYearEOY, Some(schemeIndex0))), !aUser.isAgent, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }
      result.status shouldBe OK
    }

    "redirect to the first page in journey" when {
      "page is invalid in journey" which {
        val invalidJourney = anIncomeFromPensionEmptyViewModel.copy(uKPensionIncomesQuestion = Some(false))
        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual()
          insertCyaData(pensionsUserDataWithIncomeFromPensions(invalidJourney))
          urlGet(fullUrl(pensionSchemeSummaryUrl(taxYearEOY, Some(0))), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(ukPensionSchemePayments(taxYearEOY))
        }
      }

      "previous questions are unanswered" which {
        val incompleteJourney = aUKIncomeFromPensionsViewModel.copy(
          uKPensionIncomes = Seq(anUkPensionIncomeViewModelOne.copy(amount = None)))
        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual()
          insertCyaData(pensionsUserDataWithIncomeFromPensions(incompleteJourney))
          urlGet(fullUrl(pensionSchemeSummaryUrl(taxYearEOY, Some(0))), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(ukPensionSchemePayments(taxYearEOY))
        }
      }

      "index is invalid" which {
        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual()
          val viewModel = anIncomeFromPensionEmptyViewModel.copy(uKPensionIncomesQuestion = Some(true), uKPensionIncomes = Seq(anUkPensionIncomeViewModelOne))
          insertCyaData(pensionsUserDataWithIncomeFromPensions(viewModel))
          urlGet(fullUrl(pensionSchemeSummaryUrl(taxYearEOY, Some(8))), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        "has an SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(ukPensionSchemePayments(taxYearEOY))
        }
      }

      "index is None" which {
        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual()
          val viewModel = anIncomeFromPensionEmptyViewModel.copy(uKPensionIncomesQuestion = Some(true), uKPensionIncomes = Seq(anUkPensionIncomeViewModelOne))
          insertCyaData(pensionsUserDataWithIncomeFromPensions(viewModel))
          urlGet(fullUrl(pensionSchemeSummaryUrl(taxYearEOY, None)), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        "has an SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(ukPensionSchemePayments(taxYearEOY))
        }
      }
    }

    "redirect to Pension Summary page when there is no session data" in {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        urlGet(fullUrl(pensionSchemeSummaryUrl(taxYearEOY, Some(schemeIndex0))), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      result.status shouldBe SEE_OTHER
      result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
    }
  }
}
