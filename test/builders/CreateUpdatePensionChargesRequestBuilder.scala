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

import builders.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import builders.OverseasPensionContributionsBuilder.anOverseasPensionContributions
import builders.PensionSavingTaxChargesBuilder.aPensionSavingsTaxCharges
import builders.PensionSchemeOverseasTransfersBuilder.anPensionSchemeOverseasTransfers
import models.pension.charges.CreateUpdatePensionChargesRequestModel

object CreateUpdatePensionChargesRequestBuilder {

  val priorPensionChargesData = anIncomeTaxUserData.pensions.flatMap(_.pensionCharges)

  val priorPensionChargesRM: CreateUpdatePensionChargesRequestModel = CreateUpdatePensionChargesRequestModel(
    pensionSavingsTaxCharges = priorPensionChargesData.flatMap(_.pensionSavingsTaxCharges),
    pensionSchemeOverseasTransfers = priorPensionChargesData.flatMap(_.pensionSchemeOverseasTransfers),
    pensionSchemeUnauthorisedPayments = priorPensionChargesData.flatMap(_.pensionSchemeUnauthorisedPayments),
    pensionContributions = priorPensionChargesData.flatMap(_.pensionContributions),
    overseasPensionContributions = priorPensionChargesData.flatMap(_.overseasPensionContributions)
  )
  val transferChargeSubmissionCRM = priorPensionChargesRM.copy(
    pensionSchemeOverseasTransfers = Some(anPensionSchemeOverseasTransfers.copy(transferCharge = 500.20, transferChargeTaxPaid = 200.50))
  )

  val shortServiceRefundSubmissionCRM = priorPensionChargesRM.copy(
    overseasPensionContributions = Some(anOverseasPensionContributions.copy(shortServiceRefund = 1000.20, shortServiceRefundTaxPaid = 250.00))
  )

  val annualAllowanceSubmissionCRM = priorPensionChargesRM.copy(
    pensionSavingsTaxCharges =
      Some(aPensionSavingsTaxCharges.copy(lumpSumBenefitTakenInExcessOfLifetimeAllowance = None, benefitInExcessOfLifetimeAllowance = None))
  )

}
