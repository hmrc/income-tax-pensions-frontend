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

package models.mongo

import builders.IncomeFromOverseasPensionsViewModelBuilder.anIncomeFromOverseasPensionsWithFalseFtcrValueViewModel
import builders.IncomeFromPensionsViewModelBuilder.anIncomeFromPensionsViewModel
import builders.PaymentsIntoOverseasPensionsViewModelBuilder.aPaymentsIntoOverseasPensionsNoReliefsViewModel
import builders.PaymentsIntoPensionVewModelBuilder._
import builders.PensionAnnualAllowanceViewModelBuilder._
import builders.PensionsCYAModelBuilder
import builders.ShortServiceRefundsViewModelBuilder.aShortServiceRefundsNonUkEmptySchemeViewModel
import builders.TransfersIntoOverseasPensionsViewModelBuilder._
import builders.UnauthorisedPaymentsViewModelBuilder.anUnauthorisedPaymentsEmptySchemesViewModel
import cats.implicits.catsSyntaxOptionId
import models.pension.statebenefits.{IncomeFromPensionsViewModel, StateBenefitViewModel, UkPensionIncomeViewModel}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class PensionsCYAModelSpec extends AnyWordSpecLike with Matchers {

  val userSession: PensionsCYAModel =
    PensionsCYAModel(
      paymentsIntoPension = aPaymentsIntoPensionAnotherViewModel,
      pensionsAnnualAllowances = aPensionAnnualAllowanceAnotherViewModel,
      incomeFromPensions = anIncomeFromPensionsViewModel,
      unauthorisedPayments = anUnauthorisedPaymentsEmptySchemesViewModel,
      paymentsIntoOverseasPensions = aPaymentsIntoOverseasPensionsNoReliefsViewModel,
      incomeFromOverseasPensions = anIncomeFromOverseasPensionsWithFalseFtcrValueViewModel,
      transfersIntoOverseasPensions = aTransfersIntoOverseasPensionsAnotherViewModel,
      shortServiceRefunds = aShortServiceRefundsNonUkEmptySchemeViewModel
    )

  "merge" must {
    val sourceModel = PensionsCYAModelBuilder.aPensionsCYAModel

    "use original when merged with None" in {
      assert(sourceModel.merge(None) === sourceModel)
    }

    "use original when merged with empty" in {
      assert(sourceModel.merge(Some(PensionsCYAModelBuilder.emptyPensionsData)) === sourceModel)
    }

    "favors overridden values" in {
      assert(sourceModel.merge(Some(userSession)) === userSession)
    }

    "ensure state and uk pension overrides are independent of eachother" in {
      val sessionStatePension = StateBenefitViewModel(startDateQuestion = true.some)
      val priorStatePension   = StateBenefitViewModel(startDateQuestion = false.some)

      val priorUkPension = UkPensionIncomeViewModel(pensionId = "id".some)

      // I.e. some session state present for the state pensions journey, but no session present for the uk pensions journey.
      val sessionIncomeFromPensions = IncomeFromPensionsViewModel(
        statePension = sessionStatePension.some,
        statePensionLumpSum = None,
        uKPensionIncomesQuestion = None,
        uKPensionIncomes = Seq.empty
      )
      val priorIncomeFromPensions = IncomeFromPensionsViewModel(
        statePension = priorStatePension.some,
        statePensionLumpSum = None,
        uKPensionIncomesQuestion = true.some,
        uKPensionIncomes = Seq(priorUkPension)
      )

      val sessionModel = PensionsCYAModel.emptyModels.copy(incomeFromPensions = sessionIncomeFromPensions)
      val priorModel   = PensionsCYAModel.emptyModels.copy(incomeFromPensions = priorIncomeFromPensions)

      val result = priorModel.merge(sessionModel.some)

      withClue("State pension session was not retained")(result.incomeFromPensions.statePension shouldBe sessionStatePension.some)
      withClue("UK pension prior data was not merged in")(
        result.incomeFromPensions.uKPensionIncomes should contain theSameElementsAs Seq(priorUkPension))
    }
  }
}
