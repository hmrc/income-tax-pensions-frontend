/*
 * Copyright 2022 HM Revenue & Customs
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

package builders

import builders.PensionsCYAModelBuilder.{aPensionsCYAEmptyModel, aPensionsCYAModel}
import models.mongo.PensionsUserData
import models.pension.charges.{PaymentsIntoOverseasPensionsViewModel, PensionAnnualAllowancesViewModel, PensionLifetimeAllowancesViewModel, UnauthorisedPaymentsViewModel}
import models.pension.reliefs.PaymentsIntoPensionViewModel
import models.pension.statebenefits.IncomeFromPensionsViewModel
import utils.UnitTest

object PensionsUserDataBuilder extends UnitTest {

  val aPensionsUserData: PensionsUserData = PensionsUserData(
    sessionId = "sessionId-eb3158c2-0aff-4ce8-8d1b-f2208ace52fe",
    mtdItId = "1234567890",
    nino = "AA123456A",
    taxYear = taxYearEOY,
    isPriorSubmission = true,
    pensions = aPensionsCYAModel
  )

  val anPensionsUserDataEmptyCya: PensionsUserData = aPensionsUserData.copy(pensions = aPensionsCYAEmptyModel)

  def pensionsUserDataWithPaymentsIntoPensions(paymentsIntoPension: PaymentsIntoPensionViewModel,
                                               isPriorSubmission: Boolean = true): PensionsUserData = {
    aPensionsUserData.copy(isPriorSubmission = isPriorSubmission,
      pensions = aPensionsCYAModel.copy(paymentsIntoPension = paymentsIntoPension)
    )
  }

  def pensionsUserDataWithAnnualAllowances(pensionAnnualAllowancesViewModel: PensionAnnualAllowancesViewModel,
                                           isPriorSubmission: Boolean = true): PensionsUserData = {
    aPensionsUserData.copy(isPriorSubmission = isPriorSubmission,
      pensions = aPensionsCYAModel.copy(pensionsAnnualAllowances = pensionAnnualAllowancesViewModel)
    )
  }

  def pensionsUserDataWithIncomeFromPensions(incomeFromPensionsViewModel: IncomeFromPensionsViewModel,
                                             isPriorSubmission: Boolean = true, taxYear: Int = taxYearEOY): PensionsUserData = {
    aPensionsUserData.copy(taxYear = taxYear, isPriorSubmission = isPriorSubmission,
      pensions = aPensionsCYAModel.copy(incomeFromPensions = incomeFromPensionsViewModel)
    )
  }

  def pensionsUserDataWithLifetimeAllowance(pensionLifetimeAllowancesViewModel: PensionLifetimeAllowancesViewModel,
                                            isPriorSubmission: Boolean = true): PensionsUserData = {
    aPensionsUserData.copy(isPriorSubmission = isPriorSubmission,
      pensions = aPensionsCYAModel.copy(pensionLifetimeAllowances = pensionLifetimeAllowancesViewModel)
    )
  }

  def pensionsUserDataWithUnauthorisedPayments(unauthorisedPaymentViewModel: UnauthorisedPaymentsViewModel,
                                               isPriorSubmission: Boolean = true): PensionsUserData = {
    aPensionsUserData.copy(isPriorSubmission = isPriorSubmission,
      pensions = aPensionsCYAModel.copy(unauthorisedPayments = unauthorisedPaymentViewModel)
    )
  }

  def pensionUserDataWithOverseasPensions(overseasPensionViewModel: PaymentsIntoOverseasPensionsViewModel,
                                          isPriorSubmission: Boolean = true): PensionsUserData = {
    aPensionsUserData.copy(isPriorSubmission = isPriorSubmission,
      pensions = aPensionsCYAModel.copy(paymentsIntoOverseasPensions = overseasPensionViewModel))
  }
}
