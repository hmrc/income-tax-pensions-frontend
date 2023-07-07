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
import builders.IncomeFromPensionsViewModelBuilder.anIncomeFromPensionsViewModel
import builders.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import builders.PensionsCYAModelBuilder.aPensionsCYAModel
import builders.PensionsUserDataBuilder
import builders.PensionsUserDataBuilder.aPensionsUserData
import builders.StateBenefitViewModelBuilder.anStateBenefitViewModelTwo
import builders.UserBuilder.aUser
import forms.RadioButtonForm
import models.mongo.{PensionsCYAModel, PensionsUserData}
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.IncomeFromPensionsPages.{addToCalculationUrl, statePension, statePensionCyaUrl}
import utils.PageUrls.{fullUrl, overviewUrl}
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

class StatePensionAddToCalculationControllerISpec extends IntegrationTest
  with ViewHelpers with BeforeAndAfterEach with PensionsDatabaseHelper {
  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  private def pensionsUsersData(pensionsCyaModel: PensionsCYAModel): PensionsUserData = {
    PensionsUserDataBuilder.aPensionsUserData.copy(
      isPriorSubmission = false,
      pensions = pensionsCyaModel
    )
  }

  ".show" should {
    "return Ok response with Add to calculation Question view page if there is no data" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        insertCyaData(pensionsUsersData(aPensionsCYAModel))
        urlGet(fullUrl(addToCalculationUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
        )
      }
      result.status shouldBe OK
    }

    "return Ok response with Add to calculation Question view page when CYA data exists" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        insertCyaData(pensionsUsersData(aPensionsCYAModel))
        userDataStub(anIncomeTaxUserData.copy(pensions = Some(anAllPensionsData)), nino, taxYearEOY)
        urlGet(fullUrl(addToCalculationUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
        )
      }
      result.status shouldBe OK
    }

    "redirect to the first page in journey if the previous question has not been answered" in {
      val data = aPensionsUserData.copy(
        pensions = aPensionsCYAModel.copy(
          incomeFromPensions = anIncomeFromPensionsViewModel.copy(
            statePensionLumpSum = Some(anStateBenefitViewModelTwo.copy(
              startDateQuestion = None
            ))
          )))

      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        insertCyaData(data)
        urlGet(fullUrl(addToCalculationUrl(taxYearEOY)), !aUser.isAgent, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }
      result.status shouldBe SEE_OTHER
      result.header("location").contains(statePension(taxYearEOY))
    }
  }

  ".submit" should {
    "redirect to the state pension page when the user selects yes" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        insertCyaData(pensionsUsersData(aPensionsCYAModel))
        val form: Map[String, String] = Map(RadioButtonForm.value -> "true")
        urlPost(
          fullUrl(addToCalculationUrl(taxYearEOY)),
          follow = false,
          body = form,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
        )
      }
      result.status shouldBe SEE_OTHER
      result.header(HeaderNames.LOCATION) shouldBe Some(statePensionCyaUrl(taxYearEOY))
    }

    "redirect to the state pension page when the user selects no" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        insertCyaData(pensionsUsersData(aPensionsCYAModel))
        val form: Map[String, String] = Map(RadioButtonForm.value -> "false")
        urlPost(
          fullUrl(addToCalculationUrl(taxYearEOY)),
          follow = false,
          body = form,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
        )
      }
      result.status shouldBe SEE_OTHER
      result.header(HeaderNames.LOCATION) shouldBe Some(statePensionCyaUrl(taxYearEOY))
    }

    "return a bad request when the user submits without selecting an option" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        val form: Map[String, String] = Map(RadioButtonForm.value -> "")
        insertCyaData(pensionsUsersData(aPensionsCYAModel))
        userDataStub(anIncomeTaxUserData.copy(pensions = Some(anAllPensionsData)), nino, taxYearEOY)
        urlPost(
          fullUrl(addToCalculationUrl(taxYearEOY)),
          follow = false,
          body = form,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }
      result.status shouldBe BAD_REQUEST
    }

    "redirect if not within Tax Year" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        val form: Map[String, String] = Map(RadioButtonForm.value -> "false")
        insertCyaData(pensionsUsersData(aPensionsCYAModel))
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
