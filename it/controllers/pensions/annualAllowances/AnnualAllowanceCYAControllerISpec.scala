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

package controllers.pensions.annualAllowances

import builders.AllPensionsDataBuilder.anAllPensionsData
import builders.CreateUpdatePensionChargesRequestBuilder.{annualAllowanceSubmissionCRM, priorPensionChargesRM}
import builders.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import builders.PensionAnnualAllowanceViewModelBuilder.aPensionAnnualAllowanceViewModel
import builders.PensionsCYAModelBuilder.aPensionsCYAModel
import builders.PensionsUserDataBuilder
import builders.PensionsUserDataBuilder.pensionsUserDataWithAnnualAllowances
import builders.UserBuilder.aUser
import models.IncomeTaxUserData
import models.mongo.PensionsCYAModel
import play.api.http.HeaderNames
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import utils.PageUrls.PensionAnnualAllowancePages.{annualAllowancesCYAUrl, reducedAnnualAllowanceUrl}
import utils.PageUrls.{fullUrl, pensionSummaryUrl}
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

class AnnualAllowanceCYAControllerISpec extends IntegrationTest with ViewHelpers with PensionsDatabaseHelper {
  private def pensionsUsersData(pensionsCyaModel: PensionsCYAModel) =
    PensionsUserDataBuilder.aPensionsUserData.copy(isPriorSubmission = false, pensions = pensionsCyaModel)

  override val userScenarios: Seq[UserScenario[_, _]] = Nil

  ".show" should {

    "show page when EOY" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        insertCyaData(pensionsUsersData(aPensionsCYAModel))
        userDataStub(anIncomeTaxUserData.copy(pensions = Some(anAllPensionsData)), nino, taxYearEOY)
        urlGet(
          fullUrl(annualAllowancesCYAUrl(taxYearEOY)),
          !aUser.isAgent,
          follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
        )
      }
      result.status shouldBe OK
    }

    "redirect to reduced annual allowance page previous questions have not been answered" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(taxPaidByPensionProvider = None)
        insertCyaData(pensionsUserDataWithAnnualAllowances(pensionsViewModel))

        urlGet(
          fullUrl(annualAllowancesCYAUrl(taxYearEOY)),
          follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has a SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(reducedAnnualAllowanceUrl(taxYearEOY))
      }
    }

    "redirect to the Pensions Summary page" when {
      "there is no CYA regardless of prior data" in {
        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual()
          userDataStub(IncomeTaxUserData(None), nino, taxYearEOY)
          urlGet(
            fullUrl(annualAllowancesCYAUrl(taxYearEOY)),
            follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        result.status shouldBe SEE_OTHER
        result.header("location").head shouldBe pensionSummaryUrl(taxYearEOY)
      }

      "in year" in {
        lazy implicit val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual(aUser.isAgent)
          insertCyaData(pensionsUsersData(aPensionsCYAModel))
          userDataStub(anIncomeTaxUserData.copy(pensions = Some(anAllPensionsData)), nino, taxYear)
          urlGet(
            fullUrl(annualAllowancesCYAUrl(taxYear)),
            !aUser.isAgent,
            follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)))
        }

        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe pensionSummaryUrl(taxYear)
      }
    }
  }

  ".submit" should {

    "redirect to next page" when {
      "submitting updated CYA data that differs from prior data" in {
        lazy implicit val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual(aUser.isAgent)
          insertCyaData(pensionsUsersData(aPensionsCYAModel))
          userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
          pensionChargesSessionStub(Json.toJson(annualAllowanceSubmissionCRM).toString(), nino, taxYearEOY)
          urlPost(
            fullUrl(annualAllowancesCYAUrl(taxYearEOY)),
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)),
            follow = false,
            body = ""
          )
        }
        result.status shouldBe SEE_OTHER
        result.headers("location").head shouldBe pensionSummaryUrl(taxYearEOY)
      }

      "the user makes no changes and no API submission is made" in {
        lazy implicit val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual(aUser.isAgent)
          insertCyaData(pensionsUsersData(aPensionsCYAModel))
          userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
          pensionChargesSessionStub(Json.toJson(priorPensionChargesRM).toString(), nino, taxYearEOY)
          urlPost(
            fullUrl(annualAllowancesCYAUrl(taxYearEOY)),
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)),
            follow = false,
            body = "")
        }
        result.status shouldBe SEE_OTHER
        result.headers("location").head shouldBe pensionSummaryUrl(taxYearEOY)
      }
    }

    "redirect to Pensions Summary page when in year" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        insertCyaData(pensionsUsersData(aPensionsCYAModel))
        userDataStub(anIncomeTaxUserData, nino, taxYear)
        urlPost(
          fullUrl(annualAllowancesCYAUrl(taxYear)),
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)),
          follow = false,
          body = ""
        )
      }
      result.status shouldBe SEE_OTHER
      result.headers("location").head shouldBe pensionSummaryUrl(taxYear)
    }

    "redirect to reduced annual allowance page when previous questions have not been answered" which {
      lazy val form: Map[String, String] = Map("" -> "12345678RA")
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(pensionProvidePaidAnnualAllowanceQuestion = None)
        insertCyaData(pensionsUserDataWithAnnualAllowances(pensionsViewModel))

        urlPost(
          fullUrl(annualAllowancesCYAUrl(taxYearEOY)),
          body = form,
          follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has a SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(reducedAnnualAllowanceUrl(taxYearEOY))
      }

    }
  }
}
