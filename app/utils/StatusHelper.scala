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

package utils

import models.mongo.PensionsCYAModel
import models.pension.AllPensionsData

object StatusHelper {

  def paymentsIntoPensionsIsUpdated(cya: Option[PensionsCYAModel]): Boolean =
    cya.exists(!_.paymentsIntoPension.isEmpty)

  def incomeFromPensionsIsUpdated(cya: Option[PensionsCYAModel]): Boolean =
    statePensionIsUpdated(cya) || ukPensionsSchemeIsUpdated(cya)

  def annualAllowanceIsUpdated(cya: Option[PensionsCYAModel]): Boolean =
    cya.exists(!_.pensionsAnnualAllowances.isEmpty)

  def unauthorisedPaymentsFromPensionsIsUpdated(cya: Option[PensionsCYAModel]): Boolean =
    cya.exists(!_.unauthorisedPayments.isEmpty)

  def overseasPensionsIsUpdated(cya: Option[PensionsCYAModel]): Boolean =
    paymentsIntoOverseasPensionsIsUpdated(cya) || incomeFromOverseasPensionsIsUpdated(cya) ||
      overseasPensionsTransferChargesIsUpdated(cya) || shortServiceRefundsIsUpdated(cya)
  def paymentsIntoOverseasPensionsIsUpdated(cya: Option[PensionsCYAModel]): Boolean =
    cya.exists(!_.paymentsIntoOverseasPensions.isEmpty)

  def incomeFromOverseasPensionsIsUpdated(cya: Option[PensionsCYAModel]): Boolean =
    cya.exists(!_.incomeFromOverseasPensions.isEmpty)

  def overseasPensionsTransferChargesIsUpdated(cya: Option[PensionsCYAModel]): Boolean =
    cya.exists(!_.transfersIntoOverseasPensions.isEmpty)

  def shortServiceRefundsIsUpdated(cya: Option[PensionsCYAModel]): Boolean =
    cya.exists(!_.shortServiceRefunds.isEmpty)

  def statePensionIsUpdated(cya: Option[PensionsCYAModel]): Boolean =
    cya.exists(_.incomeFromPensions.statePension.exists(!_.isEmpty)) ||
      cya.exists(_.incomeFromPensions.statePensionLumpSum.exists(!_.isEmpty))

  def ukPensionsSchemeIsUpdated(cya: Option[PensionsCYAModel]): Boolean =
    cya.exists(_.incomeFromPensions.uKPensionIncomesQuestion.isDefined)

  /* ------- hasPriorData  statuses------------------
    We will primarily use these to determine if when going to a journey you
    first either navigate to the CYA page or the 1st page of the journey
   */

  def paymentIntoPensionHasPriorData(prior: Option[AllPensionsData]): Boolean =
    prior.map(_.getPaymentsIntoPensionsCyaFromPrior).exists(_.isFinished)

  def statePensionsHasPriorData(prior: Option[AllPensionsData]): Boolean =
    prior.exists(_.stateBenefits.exists(_.stateBenefitsData.exists(data => data.statePension.nonEmpty || data.statePensionLumpSum.nonEmpty)))

  def ukPensionsSchemeHasPriorData(prior: Option[AllPensionsData]): Boolean =
    prior.exists(_.employmentPensions.exists(_.employmentData.nonEmpty))

  def annualAllowanceHasPriorData(prior: Option[AllPensionsData]): Boolean =
    prior.exists(_.pensionCharges.exists(pstc => pstc.pensionSavingsTaxCharges.nonEmpty && pstc.pensionContributions.nonEmpty))

  def unauthorisedPaymentsHasPriorData(prior: Option[AllPensionsData]): Boolean =
    prior.exists(_.pensionCharges.exists(_.pensionSchemeUnauthorisedPayments.nonEmpty))

  def paymentsIntoOverseasPensionsHasPriorData(prior: Option[AllPensionsData]): Boolean =
    prior.exists(_.pensionReliefs.exists(_.pensionReliefs.overseasPensionSchemeContributions.isDefined))

  def incomeFromOverseasPensionsHasPriorData(prior: Option[AllPensionsData]): Boolean =
    prior.exists(_.pensionIncome.exists(_.foreignPension.nonEmpty))

  def transferIntoOverseasPensionHasPriorData(prior: Option[AllPensionsData]): Boolean =
    prior.exists(_.pensionCharges.exists(_.pensionSchemeOverseasTransfers.nonEmpty))

  def shortServiceRefundsHasPriorData(prior: Option[AllPensionsData]): Boolean =
    prior.exists(_.pensionCharges.exists(_.overseasPensionContributions.nonEmpty))
}
