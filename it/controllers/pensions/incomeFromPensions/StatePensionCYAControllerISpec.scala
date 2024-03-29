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

import builders.IncomeFromPensionsViewModelBuilder.aStatePensionIncomeFromPensionsViewModel
import builders.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import builders.PensionsCYAModelBuilder.{aPensionsCYAGeneratedFromPriorEmpty, aPensionsCYAModel}
import builders.PensionsUserDataBuilder
import builders.PensionsUserDataBuilder.aPensionsUserData
import builders.StateBenefitViewModelBuilder.anEmptyStateBenefitViewModel
import builders.StateBenefitsUserDataBuilder.aCreateStatePensionBenefitsUD
import builders.UkPensionIncomeViewModelBuilder.anUkPensionIncomeViewModelOne
import builders.UserBuilder.aUser
import cats.implicits.catsSyntaxOptionId
import models.mongo.PensionsCYAModel
import models.pension.statebenefits.{ClaimCYAModel, IncomeFromPensionsViewModel, StateBenefitViewModel}
import play.api.http.HeaderNames
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.IncomeFromPensionsPages.{statePension, statePensionCyaUrl}
import utils.PageUrls.fullUrl
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

class StatePensionCYAControllerISpec extends IntegrationTest with ViewHelpers with PensionsDatabaseHelper {

  override val userScenarios: Seq[UserScenario[_, _]] = Nil
  private val statePensionCYAModel: ClaimCYAModel     = aCreateStatePensionBenefitsUD.claim.get
  private val stateBenefitData = StateBenefitViewModel(
    benefitId = statePensionCYAModel.benefitId,
    startDateQuestion = Some(true),
    startDate = Some(statePensionCYAModel.startDate),
    endDateQuestion = statePensionCYAModel.endDateQuestion,
    endDate = statePensionCYAModel.endDate,
    submittedOnQuestion = Some(true),
    submittedOn = statePensionCYAModel.submittedOn,
    dateIgnoredQuestion = None,
    dateIgnored = None,
    amountPaidQuestion = Some(true),
    amount = statePensionCYAModel.amount,
    taxPaidQuestion = statePensionCYAModel.taxPaidQuestion,
    taxPaid = statePensionCYAModel.taxPaid
  )
  private val priorCYAData = aPensionsUserData.copy(
    taxYear = taxYear,
    pensions = aPensionsCYAGeneratedFromPriorEmpty.copy(
      incomeFromPensions = IncomeFromPensionsViewModel(
        statePension = Some(stateBenefitData),
        statePensionLumpSum = StateBenefitViewModel.empty.copy(amountPaidQuestion = false.some).some,
        uKPensionIncomesQuestion = Some(true),
        uKPensionIncomes = Seq(anUkPensionIncomeViewModelOne)
      ))
  )

  private def pensionsUsersData(pensionsCyaModel: PensionsCYAModel, isPrior: Boolean = false) =
    PensionsUserDataBuilder.aPensionsUserData.copy(isPriorSubmission = isPrior, pensions = pensionsCyaModel)

  ".show" should {

    "render Page when in year" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        insertCyaData(aPensionsUserData.copy(taxYear = taxYear))
        userDataStub(anIncomeTaxUserData, nino, taxYear)
        urlGet(
          fullUrl(statePensionCyaUrl(taxYear)),
          follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)))
      }
      result.status shouldBe OK
    }

    "show page when EOY" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        insertCyaData(pensionsUsersData(aPensionsCYAModel))
        urlGet(
          fullUrl(statePensionCyaUrl(taxYearEOY)),
          !aUser.isAgent,
          follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }
      result.status shouldBe OK
    }

    "redirect to the first page in journey if journey is incomplete" in {
      val data = aPensionsUserData.copy(pensions = aPensionsCYAModel.copy(
        incomeFromPensions = aStatePensionIncomeFromPensionsViewModel
          .copy(statePension = Some(anEmptyStateBenefitViewModel), statePensionLumpSum = Some(anEmptyStateBenefitViewModel))))

      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        insertCyaData(data)
        urlGet(
          fullUrl(statePensionCyaUrl(taxYearEOY)),
          !aUser.isAgent,
          follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }
      result.status shouldBe SEE_OTHER
      result.header("location").contains(statePension(taxYearEOY))
    }
  }
}
