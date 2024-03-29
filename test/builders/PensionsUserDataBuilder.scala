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

package builders

import builders.PensionsCYAModelBuilder.{emptyPensionsData, aPensionsCYAModel}
import models.mongo.PensionsUserData
import models.pension.charges._
import models.pension.reliefs.PaymentsIntoPensionsViewModel
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

  val anPensionsUserDataEmptyCya: PensionsUserData = aPensionsUserData.copy(pensions = emptyPensionsData)

  def pensionsUserDataWithPaymentsIntoPensions(paymentsIntoPension: PaymentsIntoPensionsViewModel,
                                               isPriorSubmission: Boolean = true): PensionsUserData =
    aPensionsUserData.copy(isPriorSubmission = isPriorSubmission, pensions = aPensionsCYAModel.copy(paymentsIntoPension = paymentsIntoPension))

  def pensionsUserDataWithAnnualAllowances(pensionAnnualAllowancesViewModel: PensionAnnualAllowancesViewModel,
                                           isPriorSubmission: Boolean = true): PensionsUserData =
    aPensionsUserData.copy(
      isPriorSubmission = isPriorSubmission,
      pensions = aPensionsCYAModel.copy(pensionsAnnualAllowances = pensionAnnualAllowancesViewModel))

  def pensionsUserDataWithIncomeFromPensions(incomeFromPensionsViewModel: IncomeFromPensionsViewModel,
                                             isPriorSubmission: Boolean = true,
                                             taxYear: Int = taxYearEOY): PensionsUserData =
    aPensionsUserData.copy(
      taxYear = taxYear,
      isPriorSubmission = isPriorSubmission,
      pensions = aPensionsCYAModel.copy(incomeFromPensions = incomeFromPensionsViewModel))

  def pensionsUserDataWithUnauthorisedPayments(unauthorisedPaymentViewModel: UnauthorisedPaymentsViewModel,
                                               isPriorSubmission: Boolean = true): PensionsUserData =
    aPensionsUserData.copy(
      isPriorSubmission = isPriorSubmission,
      pensions = aPensionsCYAModel.copy(unauthorisedPayments = unauthorisedPaymentViewModel))

  def pensionUserDataWithOverseasPensions(overseasPensionViewModel: PaymentsIntoOverseasPensionsViewModel,
                                          isPriorSubmission: Boolean = true): PensionsUserData =
    aPensionsUserData.copy(
      isPriorSubmission = isPriorSubmission,
      pensions = aPensionsCYAModel.copy(paymentsIntoOverseasPensions = overseasPensionViewModel))

  def pensionUserDataWithPaymentsIntoOverseasPensions(overseasPensionViewModel: PaymentsIntoOverseasPensionsViewModel,
                                                      isPriorSubmission: Boolean = true): PensionsUserData =
    anPensionsUserDataEmptyCya.copy(
      isPriorSubmission = isPriorSubmission,
      pensions = anPensionsUserDataEmptyCya.pensions.copy(paymentsIntoOverseasPensions = overseasPensionViewModel))

  def pensionUserDataWithIncomeOverseasPension(incomeOverseasPensions: IncomeFromOverseasPensionsViewModel,
                                               isPriorSubmission: Boolean = true): PensionsUserData =
    aPensionsUserData.copy(
      isPriorSubmission = isPriorSubmission,
      pensions = aPensionsCYAModel.copy(incomeFromOverseasPensions = incomeOverseasPensions))

  def pensionUserDataWithTransferIntoOverseasPension(transferViewModel: TransfersIntoOverseasPensionsViewModel,
                                                     isPriorSubmission: Boolean = true): PensionsUserData =
    aPensionsUserData.copy(
      isPriorSubmission = isPriorSubmission,
      pensions = aPensionsCYAModel.copy(transfersIntoOverseasPensions = transferViewModel))

  def pensionUserDataWithShortServiceViewModel(refundViewModel: ShortServiceRefundsViewModel, isPriorSubmission: Boolean = true): PensionsUserData =
    aPensionsUserData.copy(isPriorSubmission = isPriorSubmission, pensions = aPensionsCYAModel.copy(shortServiceRefunds = refundViewModel))
}
