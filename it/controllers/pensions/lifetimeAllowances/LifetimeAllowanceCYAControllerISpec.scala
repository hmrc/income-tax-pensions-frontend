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
import builders.PensionsCYAModelBuilder.aPensionsCYAModel
import builders.PensionsUserDataBuilder
import builders.PensionLifetimeAllowanceViewModelBuilder.minimalPensionLifetimeAllowanceViewModel
import builders.UserBuilder.aUser
import models.mongo.PensionsCYAModel
import play.api.http.HeaderNames
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.PensionLifetimeAllowance.lifetimeAllowanceCYA
import utils.PageUrls.{fullUrl, overviewUrl, pensionSummaryUrl}
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

    "redirect to Overview Page when in year" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        insertCyaData(pensionsUsersData(aPensionsCYAModel))
        userDataStub(anIncomeTaxUserData.copy(pensions = Some(anAllPensionsData)), nino, taxYear)
        urlGet(fullUrl(lifetimeAllowanceCYA(taxYear)), !aUser.isAgent, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)))
      }

      result.status shouldBe SEE_OTHER
      result.headers("Location").head shouldBe overviewUrl(taxYear)
    }

    "redirect to pension summary Page when in year" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        insertCyaData(pensionsUsersData(aPensionsCYAModel))
        emptyUserDataStub()
        urlGet(fullUrl(lifetimeAllowanceCYA(taxYear)), !aUser.isAgent, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)))
      }

      result.status shouldBe SEE_OTHER
      result.headers("Location").head shouldBe overviewUrl(taxYear)
    }
  }


  ".submit" should {
    "redirect to overview when in year" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(pensionLifetimeAllowances = minimalPensionLifetimeAllowanceViewModel)))
        userDataStub(anIncomeTaxUserData.copy(pensions = Some(anAllPensionsData)), nino, taxYear)
        urlPost(
          fullUrl(lifetimeAllowanceCYA(taxYear)),
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)),
          follow = false,
          body = "")
      }
      result.status shouldBe SEE_OTHER
      result.headers("location").head shouldBe overviewUrl(taxYear)
    }

    "redirect to next page" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        insertCyaData(pensionsUsersData(aPensionsCYAModel))
        userDataStub(anIncomeTaxUserData.copy(pensions = Some(anAllPensionsData)), nino, taxYearEOY)
        urlPost(
          fullUrl(lifetimeAllowanceCYA(taxYearEOY)),
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)),
          follow = false,
          body = "")
      }
      result.status shouldBe SEE_OTHER
      result.headers("location").head shouldBe pensionSummaryUrl(taxYearEOY)
    }

  }
}