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

package controllers.pensions.paymentsIntoOverseasPensions

import builders.AllPensionsDataBuilder.anAllPensionsData
import builders.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import builders.PaymentsIntoOverseasPensionsViewModelBuilder.aPaymentsIntoOverseasPensionsNoReliefsViewModel
import builders.PensionsCYAModelBuilder.aPensionsCYAModel
import builders.PensionsUserDataBuilder
import builders.PensionsUserDataBuilder.aPensionsUserData
import builders.UserBuilder.aUser
import models.mongo.PensionsCYAModel
import play.api.http.HeaderNames
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.PaymentIntoOverseasPensions.{paymentsIntoOverseasPensionsCyaUrl, paymentsIntoPensionSchemeUrl}
import utils.PageUrls.{fullUrl, overseasPensionsSummaryUrl, pensionSummaryUrl}
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

class PaymentsIntoOverseasPensionsCYAControllerISpec extends IntegrationTest with ViewHelpers with PensionsDatabaseHelper {

  private def pensionsUsersData(pensionsCyaModel: PensionsCYAModel, isPrior: Boolean = false) =
    PensionsUserDataBuilder.aPensionsUserData.copy(isPriorSubmission = isPrior, pensions = pensionsCyaModel)

  override val userScenarios: Seq[UserScenario[_, _]] = Nil

  ".show" should {
    "show page when EOY" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        insertCyaData(pensionsUsersData(aPensionsCYAModel))
        userDataStub(anIncomeTaxUserData.copy(pensions = Some(anAllPensionsData)), nino, taxYearEOY)

        urlGet(
          fullUrl(paymentsIntoOverseasPensionsCyaUrl(taxYearEOY)),
          !aUser.isAgent,
          follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
        )
      }
      result.status shouldBe OK
    }

    "redirect to the Pensions Summary page" when {
      "in year" in {
        lazy implicit val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual(aUser.isAgent)
          insertCyaData(pensionsUsersData(aPensionsCYAModel))
          userDataStub(anIncomeTaxUserData.copy(pensions = Some(anAllPensionsData)), nino, taxYear)

          urlGet(
            fullUrl(paymentsIntoOverseasPensionsCyaUrl(taxYear)),
            !aUser.isAgent,
            follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList))
          )
        }

        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe pensionSummaryUrl(taxYear)
      }

      "there is no CYA data" in {
        lazy implicit val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual(aUser.isAgent)
          userDataStub(anIncomeTaxUserData.copy(pensions = Some(anAllPensionsData)), nino, taxYearEOY)

          urlGet(
            fullUrl(paymentsIntoOverseasPensionsCyaUrl(taxYearEOY)),
            !aUser.isAgent,
            follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
          )
        }

        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe pensionSummaryUrl(taxYearEOY)
      }
    }

    "redirect to the first page in journey when submission model is incomplete" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        insertCyaData(
          pensionsUsersData(aPensionsCYAModel.copy(
            paymentsIntoOverseasPensions = aPaymentsIntoOverseasPensionsNoReliefsViewModel.copy(employerPaymentsQuestion = None))))
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)

        urlGet(
          fullUrl(paymentsIntoOverseasPensionsCyaUrl(taxYearEOY)),
          !aUser.isAgent,
          follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
        )
      }

      result.status shouldBe SEE_OTHER
      result.headers("location").head shouldBe paymentsIntoPensionSchemeUrl(taxYearEOY)
    }
  }

  ".submit" should {
    "redirect to next page" when {
      "submitting updated CYA data that differs from prior data" in {
        lazy implicit val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual(aUser.isAgent)
          insertCyaData(
            pensionsUsersData(aPensionsCYAModel.copy(
              paymentsIntoOverseasPensions = aPaymentsIntoOverseasPensionsNoReliefsViewModel.copy(taxPaidOnEmployerPaymentsQuestion = Some(true)))))

          userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
          pensionReliefsSessionStub("", nino, taxYearEOY)
          pensionIncomeSessionStub("", nino, taxYearEOY)

          urlPost(
            fullUrl(paymentsIntoOverseasPensionsCyaUrl(taxYearEOY)),
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)),
            follow = false,
            body = ""
          )
        }

        result.status shouldBe SEE_OTHER
        result.headers("location").head shouldBe overseasPensionsSummaryUrl(taxYearEOY)
      }

      "the user makes no changes and no API submission is made" in {
        lazy implicit val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual(aUser.isAgent)
          insertCyaData(aPensionsUserData)

          userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
          pensionReliefsSessionStub("", nino, taxYearEOY)
          pensionIncomeSessionStub("", nino, taxYearEOY)

          urlPost(
            fullUrl(paymentsIntoOverseasPensionsCyaUrl(taxYearEOY)),
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)),
            follow = false,
            body = ""
          )
        }

        result.status shouldBe SEE_OTHER
        result.headers("location").head shouldBe overseasPensionsSummaryUrl(taxYearEOY)
      }
    }

    "redirect to the Pensions Summary page when in year" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        insertCyaData(aPensionsUserData)
        userDataStub(anIncomeTaxUserData, nino, taxYear)
        urlPost(
          fullUrl(paymentsIntoOverseasPensionsCyaUrl(taxYear)),
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)),
          follow = false,
          body = ""
        )
      }

      result.status shouldBe SEE_OTHER
      result.headers("location").head shouldBe pensionSummaryUrl(taxYear)
    }

    "redirect to the first page in journey when submission model is incomplete" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        insertCyaData(
          pensionsUsersData(aPensionsCYAModel.copy(
            paymentsIntoOverseasPensions = aPaymentsIntoOverseasPensionsNoReliefsViewModel.copy(paymentsIntoOverseasPensionsQuestions = None))))
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)

        urlPost(
          fullUrl(paymentsIntoOverseasPensionsCyaUrl(taxYearEOY)),
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)),
          follow = false,
          body = ""
        )
      }

      result.status shouldBe SEE_OTHER
      result.headers("location").head shouldBe paymentsIntoPensionSchemeUrl(taxYearEOY)
    }
  }
}
