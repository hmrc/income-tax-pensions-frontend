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

import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.pension.AllPensionsData
import models.pension.charges.PensionSavingsTaxCharges

object StatusHelper {

  def paymentsIntoPensionsIsUpdated(prior: Option[AllPensionsData]): Boolean = {

    // TODO: Do we care if it's deleted so might do as below instead?
    // val reliefs = prior.flatMap(_.pensionReliefs)
    // reliefs.isDefined && reliefs.get.deletedOn.isEmpty

    prior.flatMap(_.pensionReliefs).isDefined
  }

  def incomeFromPensionsIsUpdated(prior: Option[AllPensionsData]): Boolean = {
    //TODO: confirm any one of these is sufficient to be 'updated'
    prior.flatMap(_.stateBenefits.flatMap(_.customerAddedStateBenefits)).isDefined ||
      prior.flatMap(_.stateBenefits.flatMap(_.stateBenefits)).isDefined
  }

  def pensionFromAnnualAllowanceIsUpdated(prior: Option[AllPensionsData]): Boolean = {
    prior.flatMap(_.pensionCharges.flatMap(_.pensionSavingsTaxCharges)).isDefined ||
      prior.flatMap(_.pensionCharges.flatMap(_.pensionContributions)).isDefined
  }

  def pensionLifetimeAllowanceIsUpdated(prior: Option[AllPensionsData]): Boolean = {
    val taxCharges: Option[PensionSavingsTaxCharges] = prior.flatMap(_.pensionCharges.flatMap(_.pensionSavingsTaxCharges))

    taxCharges.map(_.lumpSumBenefitTakenInExcessOfLifetimeAllowance).isDefined ||
      taxCharges.map(_.benefitInExcessOfLifetimeAllowance).isDefined
  }

  def unauthorisedPaymentsFromPensionsIsUpdated(prior: Option[AllPensionsData]): Boolean = {
    prior.flatMap(_.pensionCharges.flatMap(_.pensionSchemeUnauthorisedPayments)).isDefined
  }

  def overseasPensionsIsUpdated(prior: Option[AllPensionsData]): Boolean = {
    prior.flatMap(_.pensionCharges.flatMap(_.pensionSchemeOverseasTransfers)).isDefined ||
    prior.flatMap(_.pensionCharges.flatMap(_.overseasPensionContributions)).isDefined
  }

  def paymentsIntoOverseasPensionsIsUpdated(cya: Option[PensionsCYAModel]): Boolean = {
    cya.flatMap(_.paymentsIntoOverseasPensions.paymentsIntoOverseasPensionsQuestions).isDefined
  }
  
  def incomeFromOverseasPensionsIsUpdated(cya: Option[PensionsCYAModel]): Boolean =
    cya.flatMap((_.incomeFromOverseasPensions.paymentsFromOverseasPensionsQuestion)).isDefined
  
  def overseasPensionsTransferChargesIsUpdated(cya: Option[PensionsCYAModel]): Boolean =
    cya.flatMap((_.transfersIntoOverseasPensions.transferPensionSavings)).isDefined
  
  def shortServiceRefundsIsUpdated(cya: Option[PensionsCYAModel]): Boolean =
    cya.flatMap((_.shortServiceRefunds.shortServiceRefund)).isDefined

  def statePensionIsUpdated(pensionsUserData: Option[PensionsUserData]): Boolean = {
    pensionsUserData.map(_.pensions).map(_.incomeFromPensions).flatMap(_.statePension).flatMap(_.amountPaidQuestion).isDefined
  }

  def ukPensionsSchemeIsUpdated(pensionsUserData: Option[PensionsUserData]): Boolean = {
    pensionsUserData.map(_.pensions).map(_.incomeFromPensions).flatMap(_.uKPensionIncomesQuestion).isDefined
  }
  
  /* ------- hasPriorData  statuses------------------*/
  def paymentIntoPensionHasPriorData(prior: Option[AllPensionsData]): Boolean =
    prior.exists(_.pensionReliefs.nonEmpty)
    
  def unauthorisedPaymentsHasPriorData(prior: Option[AllPensionsData]): Boolean =
    prior.exists(_.pensionCharges.exists(_.pensionSchemeUnauthorisedPayments.nonEmpty))
  
  def incomeFromOverseasPensionsHasPriorData(prior: Option[AllPensionsData]): Boolean =
    prior.exists(_.pensionIncome.exists(_.foreignPension.nonEmpty))
  
  def transferIntoOverseasPensionHasPriorData(prior: Option[AllPensionsData]): Boolean =
    prior.exists(_.pensionCharges.exists(_.pensionSchemeOverseasTransfers.nonEmpty))
    
  def shortServiceRefundsHasPriorData(prior: Option[AllPensionsData]): Boolean =
    prior.exists(_.pensionCharges.exists(_.overseasPensionContributions.nonEmpty))
}
