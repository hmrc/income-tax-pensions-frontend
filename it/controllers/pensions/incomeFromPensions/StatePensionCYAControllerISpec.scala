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

import builders.AllPensionsDataBuilder.anAllPensionsData
import builders.IncomeFromPensionsViewModelBuilder.{aStatePensionIncomeFromPensionsViewModel, anIncomeFromPensionEmptyViewModel}
import builders.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import builders.PensionsCYAModelBuilder.{aPensionsCYAGeneratedFromPriorEmpty, aPensionsCYAModel}
import builders.PensionsUserDataBuilder
import builders.PensionsUserDataBuilder.aPensionsUserData
import builders.StateBenefitViewModelBuilder.anStateBenefitViewModelOne
import builders.StateBenefitsUserDataBuilder.aCreateStatePensionBenefitsUD
import builders.UkPensionIncomeViewModelBuilder.anUkPensionIncomeViewModelOne
import builders.UserBuilder.aUser
import models.mongo.PensionsCYAModel
import models.pension.statebenefits.{ClaimCYAModel, IncomeFromPensionsViewModel, StateBenefitViewModel}
import play.api.http.HeaderNames
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import utils.PageUrls.IncomeFromPensionsPages.{pensionIncomeSummaryUrl, statePension, statePensionCyaUrl}
import utils.PageUrls.{fullUrl, overviewUrl, pensionSummaryUrl}
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

class StatePensionCYAControllerISpec extends IntegrationTest with ViewHelpers with PensionsDatabaseHelper {

  override val userScenarios: Seq[UserScenario[_, _]] = Nil

  private def pensionsUsersData(pensionsCyaModel: PensionsCYAModel, isPrior: Boolean = false) =
    PensionsUserDataBuilder.aPensionsUserData.copy(isPriorSubmission = isPrior, pensions = pensionsCyaModel)

  private val newIncomeFromPensions: IncomeFromPensionsViewModel = anIncomeFromPensionEmptyViewModel.copy(
    uKPensionIncomesQuestion = Some(true),
    uKPensionIncomes = Seq(anUkPensionIncomeViewModelOne.copy(
      pensionSchemeName = Some("New Pension Scheme"),
      pensionSchemeRef = Some("123/123"),
      pensionId = Some("123456"))))

  private val statePensionCYAModel: ClaimCYAModel = aCreateStatePensionBenefitsUD.claim.get

  private val stateBenefitData = StateBenefitViewModel(
    benefitId = statePensionCYAModel.benefitId,
    startDateQuestion = Some(true),
    startDate = Some(statePensionCYAModel.startDate),
    endDateQuestion = statePensionCYAModel.endDateQuestion,
    endDate = statePensionCYAModel.endDate,
    submittedOnQuestion = Some(true),
    submittedOn = statePensionCYAModel.submittedOn,
    dateIgnoredQuestion = None,
    dateIgnored = None,
    amountPaidQuestion = Some(true),
    amount = statePensionCYAModel.amount,
    taxPaidQuestion = statePensionCYAModel.taxPaidQuestion,
    taxPaid = statePensionCYAModel.taxPaid)

  private val priorCYAData = aPensionsUserData.copy(taxYear = taxYear, pensions = aPensionsCYAGeneratedFromPriorEmpty.copy(
    incomeFromPensions = IncomeFromPensionsViewModel(
      statePension = Some(stateBenefitData),
      uKPensionIncomesQuestion = Some(true),
      uKPensionIncomes = Seq(anUkPensionIncomeViewModelOne))))


  ".show" should {

    "render Page when in year" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        insertCyaData(aPensionsUserData.copy(taxYear = taxYear))
        userDataStub(anIncomeTaxUserData, nino, taxYear)
        urlGet(fullUrl(statePensionCyaUrl(taxYear)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)))
      }
      result.status shouldBe OK
    }

    "show page when EOY" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        insertCyaData(pensionsUsersData(aPensionsCYAModel))
        urlGet(fullUrl(statePensionCyaUrl(taxYearEOY)), !aUser.isAgent, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }
      result.status shouldBe OK
    }

    "redirect to the first page in journey if journey is incomplete" in {
      val data = aPensionsUserData.copy(pensions = aPensionsCYAModel.copy(
          incomeFromPensions = aStatePensionIncomeFromPensionsViewModel.copy(
            statePension = Some(anStateBenefitViewModelOne.copy(startDateQuestion = None)))))

      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        insertCyaData(data)
        urlGet(fullUrl(statePensionCyaUrl(taxYearEOY)), !aUser.isAgent, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }
      result.status shouldBe SEE_OTHER
      result.header("location").contains(statePension(taxYearEOY))
    }
  }

  ".submit" should {
    "redirect to overview page" when {
      "there is no CYA data available" in {
        val form = Map[String, String]()

        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual()
          urlPost(fullUrl(statePensionCyaUrl(taxYear)), form, follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)))
        }
        result.status shouldBe SEE_OTHER
        result.headers("location").head shouldBe pensionSummaryUrl(taxYear)
      }
    }

    "redirect to pensions summary" when {

      "there is no prior data and CYA data is submitted" which {
        val form = Map[String, String]()

        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual()
          stateBenefitsSubmissionStub(Json.toJson(stateBenefitData).toString(), nino)
          userDataStub(anIncomeTaxUserData.copy(pensions = Some(anAllPensionsData)), nino, taxYear)
          insertCyaData(aPensionsUserData.copy(
            pensions = aPensionsCYAModel.copy(incomeFromPensions = newIncomeFromPensions), taxYear = taxYear
          ))
          urlPost(fullUrl(statePensionCyaUrl(taxYear)), form, follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)))
        }

        "have the status SEE OTHER" in {
          result.status shouldBe SEE_OTHER
        }

        "redirects to the summary page" in {
          result.header("location") shouldBe Some(pensionIncomeSummaryUrl(taxYear))
        }
      }

      "CYA data has been updated and differs from prior data" which {
        val form = Map[String, String]()
        val submissionData = stateBenefitData.copy(amount = Some(500.20), taxPaid = Some(20.05))

        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual()
          stateBenefitsSubmissionStub(Json.toJson(submissionData).toString(), nino)
          userDataStub(anIncomeTaxUserData.copy(pensions = Some(anAllPensionsData)), nino, taxYear)
          insertCyaData(priorCYAData)
          urlPost(fullUrl(statePensionCyaUrl(taxYear)), form, follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)))
        }

        "have the status SEE OTHER" in {
          result.status shouldBe SEE_OTHER
        }

        "redirects to the summary page" in {
          result.header("location") shouldBe Some(pensionIncomeSummaryUrl(taxYear))
        }
      }

      "the user makes no changes and no submission to DES is made" which {
        val form = Map[String, String]()

        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual()
          stateBenefitsSubmissionStub(Json.toJson(stateBenefitData).toString(), nino)
          userDataStub(anIncomeTaxUserData.copy(pensions = Some(anAllPensionsData)), nino, taxYear)
          insertCyaData(priorCYAData)
          urlPost(fullUrl(statePensionCyaUrl(taxYear)), form, follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)))
        }

        "have the status SEE OTHER" in {
          result.status shouldBe SEE_OTHER
        }

        "redirects to the summary page" in {
          result.header("location") shouldBe Some(pensionIncomeSummaryUrl(taxYear))
        }
      }

      "submitting in year" in {
        lazy implicit val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual(aUser.isAgent)
          insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(incomeFromPensions = anIncomeFromPensionEmptyViewModel)))
          userDataStub(anIncomeTaxUserData.copy(pensions = Some(anAllPensionsData)), nino, taxYear)
          urlPost(
            fullUrl(statePensionCyaUrl(taxYear)),
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)),
            follow = false,
            body = "")
        }
        result.status shouldBe SEE_OTHER
        result.headers("location").head shouldBe controllers.pensions.routes.PensionsSummaryController.show(taxYear).url
      }

    }
  }
}
