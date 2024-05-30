/*
 * Copyright 2024 HM Revenue & Customs
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

package models.session

import builders.PensionsUserDataBuilder._
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.pension.AllPensionsData
import models.pension.charges._
import models.pension.reliefs.PaymentsIntoPensionsViewModel
import models.pension.statebenefits.{IncomeFromPensionsViewModel, StateBenefitViewModel, UkPensionIncomeViewModel}
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.wordspec.AnyWordSpecLike
import utils.PensionDataStubs._

import java.util.UUID

class PensionCYAMergedWithPriorDataSpec extends AnyWordSpecLike with TableDrivenPropertyChecks {

  // TODO Prior data is changing. Fix here https://jira.tools.tax.service.gov.uk/browse/SASS-8139
  "mergeSessionAndPriorData" ignore {
    val emptyModel            = PensionsCYAModel.emptyModels
    val sessionWithEmptyModel = aPensionsUserData.copy(pensions = emptyModel)
    val expectedFullSession   = AllPensionsData.generateSessionModelFromPrior(fullPensionsModel)
    val sessionFullModel      = sessionWithEmptyModel.copy(pensions = expectedFullSession)
    val sessionNoPaymentIntoPension =
      sessionWithEmptyModel.copy(pensions = expectedFullSession.copy(paymentsIntoPension = PaymentsIntoPensionsViewModel()))

    val cases = Table(
      ("sessionData", "priorData", "expectedResult"),
      (None, None, PensionCYAMergedWithPriorData(emptyModel, true)), // if there is no session at the beginning we want to initialize it, thus true
      (Some(sessionWithEmptyModel), None, PensionCYAMergedWithPriorData(emptyModel, false)),
      (None, Some(fullPensionsModel), PensionCYAMergedWithPriorData(expectedFullSession, true)),
      (Some(sessionWithEmptyModel), Some(fullPensionsModel), PensionCYAMergedWithPriorData(expectedFullSession, true)),
      (Some(sessionFullModel), Some(fullPensionsModel), PensionCYAMergedWithPriorData(expectedFullSession, false)),
      (Some(sessionNoPaymentIntoPension), Some(fullPensionsModel), PensionCYAMergedWithPriorData(expectedFullSession, true))
    )

    def assertMergeCases(sessionData: Option[PensionsUserData], priorData: Option[AllPensionsData], expectedResult: PensionCYAMergedWithPriorData) =
      assert(PensionCYAMergedWithPriorData.mergeSessionAndPriorData(sessionData, priorData) === expectedResult)

    "merge session with prior data" in forAll(cases)(assertMergeCases)

    // let's test if every journey with at least one field filled will stop prior data from being populated
    // (user is in the middle of the change, don't override their data)
    val pension1 = expectedFullSession.copy(paymentsIntoPension = PaymentsIntoPensionsViewModel(Some(true)))
    val pension2 = expectedFullSession.copy(pensionsAnnualAllowances = PensionAnnualAllowancesViewModel(Some(true)))
    val pension3 = expectedFullSession.copy(incomeFromPensions = IncomeFromPensionsViewModel(
      statePension = Some(StateBenefitViewModel(Some(UUID.randomUUID()))),
      uKPensionIncomesQuestion = Some(true),
      uKPensionIncomes = Some(List(UkPensionIncomeViewModel(employmentId = Some("id"))))
    ))
    val pension4 = expectedFullSession.copy(unauthorisedPayments = UnauthorisedPaymentsViewModel(Some(true)))
    val pension5 = expectedFullSession.copy(paymentsIntoOverseasPensions = PaymentsIntoOverseasPensionsViewModel(Some(true)))
    val pension6 = expectedFullSession.copy(incomeFromOverseasPensions = IncomeFromOverseasPensionsViewModel(Some(true)))
    val pension7 = expectedFullSession.copy(transfersIntoOverseasPensions = TransfersIntoOverseasPensionsViewModel(Some(true)))
    val pension8 = expectedFullSession.copy(shortServiceRefunds = ShortServiceRefundsViewModel(Some(true)))

    val session1 = sessionWithEmptyModel.copy(pensions = pension1)
    val session2 = sessionWithEmptyModel.copy(pensions = pension2)
    val session3 = sessionWithEmptyModel.copy(pensions = pension3)
    val session4 = sessionWithEmptyModel.copy(pensions = pension4)
    val session5 = sessionWithEmptyModel.copy(pensions = pension5)
    val session6 = sessionWithEmptyModel.copy(pensions = pension6)
    val session7 = sessionWithEmptyModel.copy(pensions = pension7)
    val session8 = sessionWithEmptyModel.copy(pensions = pension8)

    val dontOverrideCases = Table(
      ("sessionData", "priorData", "expectedResult"),
      (Some(session1), Some(fullPensionsModel), PensionCYAMergedWithPriorData(pension1, false)),
      (Some(session2), Some(fullPensionsModel), PensionCYAMergedWithPriorData(pension2, false)),
      (Some(session3), Some(fullPensionsModel), PensionCYAMergedWithPriorData(pension3, false)),
      (Some(session4), Some(fullPensionsModel), PensionCYAMergedWithPriorData(pension4, false)),
      (Some(session5), Some(fullPensionsModel), PensionCYAMergedWithPriorData(pension5, false)),
      (Some(session6), Some(fullPensionsModel), PensionCYAMergedWithPriorData(pension6, false)),
      (Some(session7), Some(fullPensionsModel), PensionCYAMergedWithPriorData(pension7, false)),
      (Some(session8), Some(fullPensionsModel), PensionCYAMergedWithPriorData(pension8, false))
    )

    "don't override data for user in the middle of a change (not submitted)" in forAll(dontOverrideCases)(assertMergeCases)
  }
}
