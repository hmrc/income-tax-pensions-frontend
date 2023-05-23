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

import builders.AllPensionsDataBuilder.{anAllPensionDataEmpty, anAllPensionsData}
import builders.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import builders.PensionsCYAModelBuilder.{aPensionsCYAEmptyModel, aPensionsCYAModel}
import builders.PensionsUserDataBuilder
import builders.UserBuilder.{aUser, aUserRequest}
import models.mongo.PensionsCYAModel
import models.pension.AllPensionsData.generateCyaFromPrior
import play.api.http.HeaderNames
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.IncomeFromPensionsPages.{pensionIncomeSummaryUrl, statePensionCyaUrl}
import utils.PageUrls.{fullUrl, overviewUrl}
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

class StatePensionCYAControllerISpec extends IntegrationTest with ViewHelpers with PensionsDatabaseHelper {

  override val userScenarios: Seq[UserScenario[_, _]] = Nil
  
  private def pensionsUsersData(pensionsCyaModel: PensionsCYAModel, isPrior: Boolean = false) =
    PensionsUserDataBuilder.aPensionsUserData.copy(isPriorSubmission = isPrior, pensions = pensionsCyaModel)
  
  val schemeIndex0 = 0

  ".show" should {
    
    "redirect to Overview Page when in year" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        insertCyaData(pensionsUsersData(aPensionsCYAModel), aUserRequest)
        urlGet(fullUrl(statePensionCyaUrl(taxYear)), !aUser.isAgent, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)))
      }

      result.status shouldBe SEE_OTHER
      result.headers("Location").head shouldBe overviewUrl(taxYear)
    }

    "show page when EOY" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        insertCyaData(pensionsUsersData(aPensionsCYAModel), aUserRequest)
        urlGet(fullUrl(statePensionCyaUrl(taxYearEOY)), !aUser.isAgent, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }
      result.status shouldBe OK
    }

    ".submit" should {
      "redirect to overview when in year" in {
        lazy implicit val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual(aUser.isAgent)
          insertCyaData(pensionsUsersData(aPensionsCYAModel), aUserRequest)
          userDataStub(anIncomeTaxUserData.copy(pensions = Some(anAllPensionsData)), nino, taxYear)
          urlPost(
            fullUrl(statePensionCyaUrl(taxYear)),
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)),
            follow = false,
            body = "")
        }
        result.status shouldBe SEE_OTHER
        result.headers("location").head shouldBe overviewUrl(taxYear)
      }

      for ( (state, cya) <- Seq( ("changed", aPensionsCYAModel), ("not changed", generateCyaFromPrior(anAllPensionDataEmpty)) )) {
        s"redirect to pension Income Summary page when data has $state" in {
          lazy implicit val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(aUser.isAgent)
            insertCyaData(pensionsUsersData(cya), aUserRequest)
            userDataStub(anIncomeTaxUserData.copy(pensions = Some(anAllPensionDataEmpty)), nino, taxYearEOY)
            urlPost(
              fullUrl(statePensionCyaUrl(taxYearEOY)),
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)),
              follow = false,
              body = "")
          }
          result.status shouldBe SEE_OTHER
          result.headers("location").head shouldBe pensionIncomeSummaryUrl(taxYearEOY)
        }
      }
      //TODO: ADD tests for unhappy path  conditions following submission to API via connector
    }
  }
}
