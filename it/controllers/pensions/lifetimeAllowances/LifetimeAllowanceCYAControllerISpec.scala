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

package controllers.pensions.lifetimeAllowances

import builders.AllPensionsDataBuilder.anAllPensionsData
import builders.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import builders.PensionLifetimeAllowancesViewModelBuilder.{aPensionLifetimeAllowancesEmptySchemesViewModel, minimalPensionLifetimeAllowancesViewModel}
import builders.PensionsCYAModelBuilder.aPensionsCYAModel
import builders.PensionsUserDataBuilder
import builders.PensionsUserDataBuilder.{aPensionsUserData, pensionsUserDataWithLifetimeAllowance}
import builders.UserBuilder.aUser
import models.mongo.PensionsCYAModel
import play.api.http.HeaderNames
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.PensionLifetimeAllowance.{lifetimeAllowanceCYA, pensionAboveAnnualLifetimeAllowanceUrl}
import utils.PageUrls.{fullUrl, pensionSummaryUrl}
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

class LifetimeAllowanceCYAControllerISpec extends IntegrationTest with ViewHelpers
  with PensionsDatabaseHelper {

  private def pensionsUsersData(pensionsCyaModel: PensionsCYAModel) = {
    PensionsUserDataBuilder.aPensionsUserData.copy(isPriorSubmission = false, pensions = pensionsCyaModel)
  }

  override val userScenarios: Seq[UserScenario[_, _]] = Nil

  ".show" should {

    "show page when date is EOY" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        insertCyaData(pensionsUsersData(aPensionsCYAModel))
        userDataStub(anIncomeTaxUserData.copy(pensions = Some(anAllPensionsData)), nino, taxYearEOY)
        urlGet(fullUrl(lifetimeAllowanceCYA(taxYearEOY)), !aUser.isAgent, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }
      result.status shouldBe OK
    }

    "redirect to pension summary page when in year" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        insertCyaData(pensionsUsersData(aPensionsCYAModel))
        emptyUserDataStub()
        urlGet(fullUrl(lifetimeAllowanceCYA(taxYear)), !aUser.isAgent, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)))
      }

      result.status shouldBe SEE_OTHER
      result.headers("Location").head shouldBe pensionSummaryUrl(taxYear)
    }

    "redirect to first page in journey when submission model is incomplete" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        insertCyaData(pensionsUserDataWithLifetimeAllowance(aPensionLifetimeAllowancesEmptySchemesViewModel))
        userDataStub(anIncomeTaxUserData.copy(pensions = Some(anAllPensionsData)), nino, taxYearEOY)
        urlGet(fullUrl(lifetimeAllowanceCYA(taxYearEOY)), !aUser.isAgent, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      result.status shouldBe SEE_OTHER
      result.headers("Location").head shouldBe pensionAboveAnnualLifetimeAllowanceUrl(taxYearEOY)
    }
  }


  ".submit" should {
    "redirect to the pensions summary page when in year" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(pensionLifetimeAllowances = minimalPensionLifetimeAllowancesViewModel)))
        userDataStub(anIncomeTaxUserData.copy(pensions = Some(anAllPensionsData)), nino, taxYear)
        urlPost(
          fullUrl(lifetimeAllowanceCYA(taxYear)),
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)),
          follow = false,
          body = "")
      }
      result.status shouldBe SEE_OTHER
      result.headers("location").head shouldBe pensionSummaryUrl(taxYear)
    }

    "redirect to next page" when {

      "the cya data is persisted to pensions backend" in {

        lazy implicit val result: WSResponse = {
          dropPensionsDB()
          val userData = anIncomeTaxUserData.copy(pensions = Some(anAllPensionsData))
          authoriseAgentOrIndividual(aUser.isAgent)
          userDataStub(userData, nino, taxYearEOY)
          insertCyaData(aPensionsUserData)
          pensionChargesSessionStub("", nino, taxYearEOY)
          urlPost(
            fullUrl(lifetimeAllowanceCYA(taxYearEOY)),
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)),
            follow = false,
            body = "")
        }
        result.status shouldBe SEE_OTHER
        result.headers("location").head shouldBe pensionSummaryUrl(taxYearEOY)
      }

      "there is no CYA data available" which {
        val form = Map[String, String]()
        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual()
          pensionChargesSessionStub("", nino, taxYearEOY)
          urlPost(fullUrl(lifetimeAllowanceCYA(taxYearEOY)), form, follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }
        "have the status SEE OTHER" in {
          result.status shouldBe SEE_OTHER
        }
        "redirects to the overview page" in {
          result.headers("Location").head shouldBe pensionSummaryUrl(taxYearEOY)
        }
      }

    }

    "redirect to first page in journey when submission model is incomplete" in {
      val form = Map[String, String]()
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        insertCyaData(pensionsUserDataWithLifetimeAllowance(aPensionLifetimeAllowancesEmptySchemesViewModel))
        userDataStub(anIncomeTaxUserData.copy(pensions = Some(anAllPensionsData)), nino, taxYearEOY)
        urlPost(fullUrl(lifetimeAllowanceCYA(taxYearEOY)), form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      result.status shouldBe SEE_OTHER
      result.headers("Location").head shouldBe pensionAboveAnnualLifetimeAllowanceUrl(taxYearEOY)
    }

  }
}
