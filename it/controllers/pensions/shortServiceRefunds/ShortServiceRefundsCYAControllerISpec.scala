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

package controllers.pensions.shortServiceRefunds

import builders.AllPensionsDataBuilder.anAllPensionsData
import builders.CreateUpdatePensionChargesRequestBuilder.{priorPensionChargesRM, shortServiceRefundSubmissionCRM}
import builders.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import builders.PensionsCYAModelBuilder.aPensionsCYAModel
import builders.PensionsUserDataBuilder
import builders.PensionsUserDataBuilder.{aPensionsUserData, pensionUserDataWithShortServiceViewModel}
import builders.ShortServiceRefundsViewModelBuilder.aShortServiceRefundsViewModel
import builders.UserBuilder.aUser
import models.mongo.PensionsCYAModel
import models.pension.Journey.ShortServiceRefunds
import models.pension.charges.OverseasRefundPensionScheme
import play.api.http.HeaderNames
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import utils.PageUrls.IncomeFromOverseasPensionsPages.checkIncomeFromOverseasPensionsCyaUrl
import utils.PageUrls.ShortServiceRefunds.{shortServiceRefundsCYAUrl, shortServiceTaxableRefundUrl}
import utils.PageUrls.{fullUrl, sectionCompletedUrl}
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

class ShortServiceRefundsCYAControllerISpec extends IntegrationTest with ViewHelpers with PensionsDatabaseHelper {

  private def pensionsUsersData(pensionsCyaModel: PensionsCYAModel) =
    PensionsUserDataBuilder.aPensionsUserData.copy(isPriorSubmission = false, pensions = pensionsCyaModel)

  override val userScenarios: Seq[UserScenario[_, _]] = Nil

  ".show" should {
    "render the page when in year" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        insertCyaData(aPensionsUserData.copy(taxYear = taxYear))
        userDataStub(anIncomeTaxUserData, nino, taxYear)
        urlGet(
          fullUrl(shortServiceRefundsCYAUrl(taxYear)),
          follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)))
      }
      result.status shouldBe OK
    }
    "render page with a valid CYA model" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        insertCyaData(pensionsUsersData(aPensionsCYAModel))
        userDataStub(anIncomeTaxUserData.copy(pensions = Some(anAllPensionsData)), nino, taxYearEOY)
        urlGet(
          fullUrl(shortServiceRefundsCYAUrl(taxYearEOY)),
          !aUser.isAgent,
          follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
        )
      }
      result.status shouldBe OK
    }

    "return an OK status that continues to the first page of journey if journey is incomplete" in {
      val incompleteViewModel = aShortServiceRefundsViewModel.copy(
        refundPensionScheme = Seq(
          OverseasRefundPensionScheme(
            name = Some("Scheme Name with UK charge"),
            qualifyingRecognisedOverseasPensionScheme = None,
            providerAddress = Some("Scheme Address 1"),
            alphaTwoCountryCode = None,
            alphaThreeCountryCode = None
          ))
      )
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(aUser.isAgent)
        dropPensionsDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        insertCyaData(pensionUserDataWithShortServiceViewModel(incompleteViewModel))

        urlGet(
          fullUrl(checkIncomeFromOverseasPensionsCyaUrl(taxYearEOY)),
          aUser.isAgent,
          follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
        )
      }

      result.status shouldBe OK // proven this works correctly but can't test OK's location
    }
  }

  ".submit" should {
    "redirect to next page" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        insertCyaData(pensionsUsersData(aPensionsCYAModel))
        userDataStub(anIncomeTaxUserData.copy(pensions = Some(anAllPensionsData)), nino, taxYearEOY)
        pensionChargesSessionStub(Json.toJson(shortServiceRefundSubmissionCRM).toString(), nino, taxYearEOY)
        urlPost(
          fullUrl(shortServiceRefundsCYAUrl(taxYearEOY)),
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)),
          follow = false,
          body = "")
      }
      result.status shouldBe SEE_OTHER
      result.headers("location").head shouldBe sectionCompletedUrl(taxYearEOY, ShortServiceRefunds)
    }

    "CYA data is updated and differs from prior data" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        pensionChargesSessionStub(Json.toJson(shortServiceRefundSubmissionCRM).toString(), nino, taxYearEOY)
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        insertCyaData(aPensionsUserData)
        urlPost(
          fullUrl(shortServiceRefundsCYAUrl(taxYearEOY)),
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)),
          follow = false,
          body = "")
      }
      result.status shouldBe SEE_OTHER
      result.headers("location").head shouldBe sectionCompletedUrl(taxYearEOY, ShortServiceRefunds)
    }

    "the user makes no changes and no submission to DES is made" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        pensionChargesSessionStub(Json.toJson(priorPensionChargesRM).toString(), nino, taxYearEOY)
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        insertCyaData(aPensionsUserData)
        urlPost(
          fullUrl(shortServiceRefundsCYAUrl(taxYearEOY)),
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)),
          follow = false,
          body = "")
      }
      result.status shouldBe SEE_OTHER
      result.headers("location").head shouldBe sectionCompletedUrl(taxYearEOY, ShortServiceRefunds)
    }

    "redirect to the first page in the journey if journey is incomplete" in {
      val incompleteViewModel = aShortServiceRefundsViewModel.copy(
        refundPensionScheme = Seq(
          OverseasRefundPensionScheme(
            name = Some("Scheme Name with UK charge"),
            qualifyingRecognisedOverseasPensionScheme = None,
            providerAddress = Some("Scheme Address 1"),
            alphaTwoCountryCode = None,
            alphaThreeCountryCode = None
          ))
      )
      implicit lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        pensionChargesSessionStub(Json.toJson(priorPensionChargesRM).toString(), nino, taxYearEOY)
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        insertCyaData(pensionUserDataWithShortServiceViewModel(incompleteViewModel))

        urlPost(
          fullUrl(shortServiceRefundsCYAUrl(taxYearEOY)),
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)),
          follow = false,
          body = "")
      }

      result.status shouldBe SEE_OTHER
      result.header("location") shouldBe Some(shortServiceTaxableRefundUrl(taxYearEOY))
    }
  }
}
