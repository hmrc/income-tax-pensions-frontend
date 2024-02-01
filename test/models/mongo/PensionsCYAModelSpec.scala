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
import builders.IncomeFromPensionsViewModelBuilder.aStatePensionIncomeFromPensionsViewModel
import builders.PaymentsIntoOverseasPensionsViewModelBuilder.aPaymentsIntoOverseasPensionsNoReliefsViewModel
import builders.PaymentsIntoPensionVewModelBuilder._
import builders.PensionAnnualAllowanceViewModelBuilder._
import builders.PensionsCYAModelBuilder
import builders.ShortServiceRefundsViewModelBuilder.aShortServiceRefundsNonUkEmptySchemeViewModel
import builders.TransfersIntoOverseasPensionsViewModelBuilder._
import builders.UnauthorisedPaymentsViewModelBuilder.anUnauthorisedPaymentsEmptySchemesViewModel
import org.scalatest.wordspec.AnyWordSpecLike

class PensionsCYAModelSpec extends AnyWordSpecLike {

  "merge" should {
    val sourceModel = PensionsCYAModelBuilder.aPensionsCYAModel

    "use original when merged with None" in {
      assert(sourceModel.merge(None) === sourceModel)
    }

    "use original when merged with empty" in {
      assert(sourceModel.merge(Some(PensionsCYAModelBuilder.aPensionsCYAEmptyModel)) === sourceModel)
    }

    "favors overridden values" in {
      val userSession = PensionsCYAModel(
        paymentsIntoPension = aPaymentsIntoPensionAnotherViewModel,
        pensionsAnnualAllowances = aPensionAnnualAllowanceAnotherViewModel,
        incomeFromPensions = aStatePensionIncomeFromPensionsViewModel,
        unauthorisedPayments = anUnauthorisedPaymentsEmptySchemesViewModel,
        paymentsIntoOverseasPensions = aPaymentsIntoOverseasPensionsNoReliefsViewModel,
        incomeFromOverseasPensions = anIncomeFromOverseasPensionsWithFalseFtcrValueViewModel,
        transfersIntoOverseasPensions = aTransfersIntoOverseasPensionsAnotherViewModel,
        shortServiceRefunds = aShortServiceRefundsNonUkEmptySchemeViewModel
      )

      assert(sourceModel.merge(Some(userSession)) === userSession)
    }
  }
}
