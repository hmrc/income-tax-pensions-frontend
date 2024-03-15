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

package utils

import builders.IncomeFromOverseasPensionsViewModelBuilder.anIncomeFromOverseasPensionsViewModel
import builders.IncomeFromPensionsViewModelBuilder.anIncomeFromPensionsViewModel
import builders.PaymentsIntoOverseasPensionsViewModelBuilder.aPaymentsIntoOverseasPensionsViewModel
import builders.ShortServiceRefundsViewModelBuilder.aShortServiceRefundsViewModel
import builders.TransfersIntoOverseasPensionsViewModelBuilder.aTransfersIntoOverseasPensionsViewModel
import builders.UnauthorisedPaymentsViewModelBuilder.anUnauthorisedPaymentsViewModel
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.pension.charges._
import models.pension.reliefs.PaymentsIntoPensionsViewModel

object PensionUserDataStub extends IntegrationTest {

  val paymentsIntoPensionViewModel: PaymentsIntoPensionsViewModel = PaymentsIntoPensionsViewModel(
    Some(true),
    Some(222.3),
    Some(true),
    Some(22.44),
    Some(true),
    Some(true),
    Some(true),
    Some(44.00),
    Some(true),
    Some(55.55))

  val pensionsAnnualAllowancesViewModel: PensionAnnualAllowancesViewModel = PensionAnnualAllowancesViewModel(
    reducedAnnualAllowanceQuestion = Some(true),
    moneyPurchaseAnnualAllowance = Some(true),
    taperedAnnualAllowance = Some(true),
    aboveAnnualAllowanceQuestion = Some(true),
    aboveAnnualAllowance = Some(12.44),
    pensionProvidePaidAnnualAllowanceQuestion = Some(true),
    taxPaidByPensionProvider = Some(14.55),
    pensionSchemeTaxReferences = Some(Seq("1234567CRC", "12345678RB", "1234567DRD"))
  )

  // scalastyle:off magic.number
  def pensionUserData(
      sessionId: String = "sessionid",
      mtdItId: String = "1234567890",
      nino: String = "nino",
      taxyear: Int = taxYear,
      isPriorSubmission: Boolean = true,
      cya: PensionsCYAModel = PensionsCYAModel(
        paymentsIntoPensionViewModel,
        pensionsAnnualAllowancesViewModel,
        anIncomeFromPensionsViewModel,
        anUnauthorisedPaymentsViewModel,
        aPaymentsIntoOverseasPensionsViewModel,
        anIncomeFromOverseasPensionsViewModel,
        aTransfersIntoOverseasPensionsViewModel,
        aShortServiceRefundsViewModel
      )
  ): PensionsUserData =
    PensionsUserData(
      sessionId = sessionId,
      mtdItId = mtdItId,
      nino = nino,
      taxYear = taxyear,
      isPriorSubmission = isPriorSubmission,
      pensions = cya,
      lastUpdated = testClock.now()
    )
  // scalastyle:on magic.number

}
