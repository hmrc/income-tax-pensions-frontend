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
import builders.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import builders.PensionsCYAModelBuilder._
import builders.PensionsUserDataBuilder
import builders.UserBuilder._
import forms.RadioButtonAmountForm
import models.mongo.{PensionsCYAModel, PensionsUserData}
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.IncomeFromPensionsPages.{statePension, statePensionStartDateUrl}
import utils.PageUrls._
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

class StatePensionControllerISpec extends IntegrationTest with ViewHelpers with BeforeAndAfterEach with PensionsDatabaseHelper {

  private def pensionsUsersData(pensionsCyaModel: PensionsCYAModel): PensionsUserData = {
    PensionsUserDataBuilder.aPensionsUserData.copy(
      isPriorSubmission = false,
      pensions = pensionsCyaModel
    )
  }

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  ".show" should {
    "return Ok response with Regular State Pension Question view page if there is no data" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        insertCyaData(pensionsUsersData(aPensionsCYAModel), aUserRequest)
        urlGet(fullUrl(statePension(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
        )
      }
      result.status shouldBe OK
    }

    "return Ok with view page when CYA data exists" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        insertCyaData(pensionsUsersData(aPensionsCYAModel), aUserRequest)
        userDataStub(anIncomeTaxUserData.copy(pensions = Some(anAllPensionsData)), nino, taxYearEOY)
        urlGet(fullUrl(statePension(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
        )
      }
      result.status shouldBe OK
    }
  }

  ".submit" should {
    "redirect to StatePensionView when user selects 'Yes' with amount" in {
      //todo redirect to 'When did you start getting State Pension payments" page once created
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        val form: Map[String, String] = Map(RadioButtonAmountForm.yesNo -> "true", RadioButtonAmountForm.amount2 -> "42.24")
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy()), aUserRequest)
        urlPost(
          fullUrl(statePension(taxYearEOY)),
          body = form,
          follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }
      result.status shouldBe SEE_OTHER
      result.header("location").contains(statePensionStartDateUrl(taxYearEOY)) shouldBe true
    }

    "redirect to StatePensionView when user selects 'No'" in {
      //todo redirect to Check your State Pension page
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        val form: Map[String, String] = Map(RadioButtonAmountForm.yesNo -> "false")
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy()), aUserRequest)
        urlPost(
          fullUrl(statePension(taxYearEOY)),
          body = form,
          follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }
      result.status shouldBe SEE_OTHER
      result.header("location").contains(statePension(taxYearEOY)) shouldBe true
    }

    "return a Bad Request when form is submitted with errors" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        val form: Map[String, String] = Map(RadioButtonAmountForm.yesNo -> "")
        insertCyaData(pensionsUsersData(aPensionsCYAModel), aUserRequest)
        urlPost(
          fullUrl(statePension(taxYearEOY)),
          body = form,
          follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }
      result.status shouldBe BAD_REQUEST
    }

    "redirect if not within Tax Year" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        val form: Map[String, String] = Map(RadioButtonAmountForm.yesNo -> "false")
        insertCyaData(pensionsUsersData(aPensionsCYAModel), aUserRequest)
        urlPost(
          fullUrl(statePension(taxYear)),
          body = form,
          follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)))
      }
      result.status shouldBe SEE_OTHER
      result.header("location").contains(overviewUrl(taxYear)) shouldBe true
    }
  }
}

