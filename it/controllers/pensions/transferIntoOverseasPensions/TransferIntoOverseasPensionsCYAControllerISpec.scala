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

package controllers.pensions.transferIntoOverseasPensions

import builders.AllPensionsDataBuilder.anAllPensionsData
import builders.CreateUpdatePensionChargesRequestBuilder.{priorPensionChargesRM, transferChargeSubmissionCRM}
import builders.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import builders.PensionsCYAModelBuilder.aPensionsCYAModel
import builders.PensionsUserDataBuilder
import builders.PensionsUserDataBuilder.{aPensionsUserData, pensionUserDataWithTransferIntoOverseasPension}
import builders.TransfersIntoOverseasPensionsViewModelBuilder.{aTransfersIntoOverseasPensionsViewModel, emptyTransfersIntoOverseasPensionsViewModel}
import builders.UserBuilder.aUser
import models.mongo.PensionsCYAModel
import models.pension.charges.TransferPensionScheme
import play.api.http.HeaderNames
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import utils.PageUrls.TransferIntoOverseasPensions._
import utils.PageUrls.{fullUrl, overseasPensionsSummaryUrl, overviewUrl, pensionSummaryUrl}
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

class TransferIntoOverseasPensionsCYAControllerISpec extends IntegrationTest with ViewHelpers with PensionsDatabaseHelper {

  private def pensionsUsersData(pensionsCyaModel: PensionsCYAModel, isPrior: Boolean = false) =
    PensionsUserDataBuilder.aPensionsUserData.copy(isPriorSubmission = isPrior, pensions = pensionsCyaModel)

  override val userScenarios: Seq[UserScenario[_, _]] = Nil

  ".show" should {

    "return 200 status when tax year is valid" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        insertCyaData(pensionsUsersData(aPensionsCYAModel))
        userDataStub(anIncomeTaxUserData.copy(pensions = Some(anAllPensionsData)), nino, taxYearEOY)

        urlGet(
          fullUrl(checkYourDetailsPensionUrl(taxYearEOY)),
          !aUser.isAgent,
          follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
        )
      }
      result.status shouldBe OK
    }

    "redirect to overview page when tax year is invalid" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        insertCyaData(pensionsUsersData(aPensionsCYAModel))
        userDataStub(anIncomeTaxUserData.copy(pensions = Some(anAllPensionsData)), nino, taxYearEOY)

        urlGet(
          fullUrl(checkYourDetailsPensionUrl(taxYear)),
          !aUser.isAgent,
          follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
        )
      }
      result.status shouldBe SEE_OTHER
      result.header("location") shouldBe Some(overviewUrl(taxYear))
    }
  }

  ".submit" should {
    "redirect to summary page when there is no CYA data" which {

      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        userDataStub(anIncomeTaxUserData.copy(pensions = Some(anAllPensionsData)), nino, taxYear)
        urlPost(
          fullUrl(checkYourDetailsPensionUrl(taxYear)),
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)),
          follow = false,
          body = "")
      }

      "have the status SEE OTHER" in {
        result.status shouldBe SEE_OTHER
      }

      "redirects to the summary page" in {
        result.header("location") shouldBe Some(pensionSummaryUrl(taxYear))
      }
    }

    "redirect to start of journey" when {
      "there is no prior data and CYA data is submitted" which {

        lazy implicit val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual(aUser.isAgent)
          pensionChargesSessionStub(Json.toJson(transferChargeSubmissionCRM).toString(), nino, taxYearEOY)
          userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
          insertCyaData(
            aPensionsUserData.copy(
              pensions = aPensionsCYAModel.copy(transfersIntoOverseasPensions = emptyTransfersIntoOverseasPensionsViewModel)
            ))
          urlPost(
            fullUrl(checkYourDetailsPensionUrl(taxYearEOY)),
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)),
            follow = false,
            body = Json.toJson(transferChargeSubmissionCRM).toString()
          )
        }

        "have the status SEE OTHER" in {
          result.status shouldBe SEE_OTHER
        }

        "redirects to the overview page" in {
          result.header("location") shouldBe Some(transferPensionSavingsUrl(taxYearEOY))
        }
      }
    }

    "redirect to Overseas Pension Summary page" when {

      "CYA data has been updated and differs from prior data" which {

        lazy implicit val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual(aUser.isAgent)
          pensionChargesSessionStub(Json.toJson(transferChargeSubmissionCRM).toString(), nino, taxYearEOY)
          userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
          insertCyaData(aPensionsUserData)
          urlPost(
            fullUrl(checkYourDetailsPensionUrl(taxYearEOY)),
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)),
            follow = false,
            body = Json.toJson(transferChargeSubmissionCRM).toString()
          )
        }

        "have the status SEE OTHER" in {
          result.status shouldBe SEE_OTHER
        }

        "redirects to the overview page" in {
          result.header("location") shouldBe Some(overseasPensionsSummaryUrl(taxYearEOY))
        }
      }

      "the user makes no changes and no submission to DES is made" which {

        lazy implicit val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual(aUser.isAgent)
          pensionChargesSessionStub(Json.toJson(priorPensionChargesRM).toString(), nino, taxYearEOY)
          userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
          insertCyaData(aPensionsUserData)
          urlPost(
            fullUrl(checkYourDetailsPensionUrl(taxYearEOY)),
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)),
            follow = false,
            body = Json.toJson(priorPensionChargesRM).toString()
          )
        }

        "have the status SEE OTHER" in {
          result.status shouldBe SEE_OTHER
        }

        "redirects to the overview page" in {
          result.header("location") shouldBe Some(overseasPensionsSummaryUrl(taxYearEOY))
        }
      }
    }

    "redirect to the first page in the journey if journey is incomplete" in {
      val incompleteViewModel = aTransfersIntoOverseasPensionsViewModel.copy(
        transferPensionScheme = Seq(
          TransferPensionScheme(
            ukTransferCharge = Some(true),
            name = Some("UK TPS"),
            pstr = None,
            qops = None,
            providerAddress = Some("Some address 1"),
            alphaTwoCountryCode = None,
            alphaThreeCountryCode = None
          ))
      )
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(aUser.isAgent)
        dropPensionsDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        insertCyaData(pensionUserDataWithTransferIntoOverseasPension(incompleteViewModel))

        urlPost(
          fullUrl(checkYourDetailsPensionUrl(taxYearEOY)),
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)),
          follow = false,
          body = "")
      }
      result.status shouldBe SEE_OTHER
      result.header("location") shouldBe Some(transferPensionSavingsUrl(taxYearEOY))
    }

  }
}
