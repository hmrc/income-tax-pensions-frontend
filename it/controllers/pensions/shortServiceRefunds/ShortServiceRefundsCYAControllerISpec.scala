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
import builders.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import builders.OverseasPensionContributionsBuilder.anOverseasPensionContributions
import builders.PensionsCYAModelBuilder.aPensionsCYAModel
import builders.PensionsUserDataBuilder
import builders.PensionsUserDataBuilder.aPensionsUserData
import builders.ShortServiceRefundsViewModelBuilder.emptyShortServiceRefundsViewModel
import builders.UserBuilder.aUser
import models.mongo.PensionsCYAModel
import models.pension.charges.{CreateUpdatePensionChargesRequestModel, PensionCharges}
import play.api.http.HeaderNames
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import utils.PageUrls.ShortServiceRefunds.shortServiceRefundsCYAUrl
import utils.PageUrls.{fullUrl, overseasPensionsSummaryUrl, overviewUrl}
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

class ShortServiceRefundsCYAControllerISpec extends IntegrationTest with ViewHelpers
  with PensionsDatabaseHelper {

  private def pensionsUsersData(pensionsCyaModel: PensionsCYAModel) = {
    PensionsUserDataBuilder.aPensionsUserData.copy(isPriorSubmission = false, pensions = pensionsCyaModel)
  }


  val priorPensionChargesData: Option[PensionCharges] = anIncomeTaxUserData.pensions.flatMap(_.pensionCharges)

  val priorPensionCharges: CreateUpdatePensionChargesRequestModel = CreateUpdatePensionChargesRequestModel(
    pensionSavingsTaxCharges = priorPensionChargesData.flatMap(_.pensionSavingsTaxCharges),
    pensionSchemeOverseasTransfers = priorPensionChargesData.flatMap(_.pensionSchemeOverseasTransfers),
    pensionSchemeUnauthorisedPayments = priorPensionChargesData.flatMap(_.pensionSchemeUnauthorisedPayments),
    pensionContributions = priorPensionChargesData.flatMap(_.pensionContributions),
    overseasPensionContributions = priorPensionChargesData.flatMap(_.overseasPensionContributions)
  )
  val submissionChargesModel: CreateUpdatePensionChargesRequestModel = priorPensionCharges.copy(
    overseasPensionContributions = Some(anOverseasPensionContributions.copy(shortServiceRefund = 1000.20, shortServiceRefundTaxPaid = 250.00))
  )

  override val userScenarios: Seq[UserScenario[_, _]] = Nil

  ".show" should {
    "redirect to Overview Page when in year" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        insertCyaData(pensionsUsersData(aPensionsCYAModel))
        userDataStub(anIncomeTaxUserData.copy(pensions = Some(anAllPensionsData)), nino, taxYear)
        urlGet(fullUrl(shortServiceRefundsCYAUrl(taxYear)), !aUser.isAgent, follow = false,
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
        userDataStub(anIncomeTaxUserData.copy(pensions = Some(anAllPensionsData)), nino, taxYearEOY)
        urlGet(fullUrl(shortServiceRefundsCYAUrl(taxYearEOY)), !aUser.isAgent, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }
      result.status shouldBe OK
    }
  }

  ".submit" should {
    "redirect to overview when in year" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(shortServiceRefunds = emptyShortServiceRefundsViewModel)))
        userDataStub(anIncomeTaxUserData.copy(pensions = Some(anAllPensionsData)), nino, taxYear)
        urlPost(
          fullUrl(shortServiceRefundsCYAUrl(taxYear)),
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
          fullUrl(shortServiceRefundsCYAUrl(taxYearEOY)),
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)),
          follow = false,
          body = "")
      }
      result.status shouldBe SEE_OTHER
      result.headers("location").head shouldBe overseasPensionsSummaryUrl(taxYearEOY)
    }

    "CYA data is updated and differs from prior data" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        pensionChargesSessionStub(Json.toJson(submissionChargesModel).toString(), nino, taxYearEOY)
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        insertCyaData(aPensionsUserData)
        urlPost(
          fullUrl(shortServiceRefundsCYAUrl(taxYearEOY)),
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)),
          follow = false,
          body = "")
      }
      result.status shouldBe SEE_OTHER
      result.headers("location").head shouldBe overseasPensionsSummaryUrl(taxYearEOY)
    }

    "the user makes no changes and no submission to DES is made" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        pensionChargesSessionStub(Json.toJson(priorPensionCharges).toString(), nino, taxYearEOY)
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        insertCyaData(aPensionsUserData)
        urlPost(
          fullUrl(shortServiceRefundsCYAUrl(taxYearEOY)),
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)),
          follow = false,
          body = "")
      }
      result.status shouldBe SEE_OTHER
      result.headers("location").head shouldBe overseasPensionsSummaryUrl(taxYearEOY)
    }
  }
}
