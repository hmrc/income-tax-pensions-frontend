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
    cya.flatMap(_.paymentsIntoPension.rasPensionPaymentQuestion).isDefined

  def incomeFromPensionsIsUpdated(cya: Option[PensionsCYAModel]): Boolean =
    statePensionIsUpdated(cya) || ukPensionsSchemeIsUpdated(cya)

  def pensionFromAnnualAllowanceIsUpdated(cya: Option[PensionsCYAModel]): Boolean =
    cya.flatMap(_.pensionsAnnualAllowances.aboveAnnualAllowanceQuestion).isDefined

  def pensionLifetimeAllowanceIsUpdated(cya: Option[PensionsCYAModel]): Boolean =
    cya.flatMap(_.pensionLifetimeAllowances.aboveLifetimeAllowanceQuestion).isDefined

  def unauthorisedPaymentsFromPensionsIsUpdated(cya: Option[PensionsCYAModel]): Boolean = {
    cya.flatMap(_.unauthorisedPayments.unauthorisedPaymentQuestion).isDefined
  }

  def overseasPensionsIsUpdated(cya: Option[PensionsCYAModel]): Boolean = {
    cya.flatMap(_.paymentsIntoOverseasPensions.paymentsIntoOverseasPensionsQuestions).isDefined ||
    cya.flatMap(_.incomeFromOverseasPensions.paymentsFromOverseasPensionsQuestion).isDefined ||
    cya.flatMap(_.transfersIntoOverseasPensions.transferPensionSavings).isDefined ||
    cya.flatMap(_.shortServiceRefunds.shortServiceRefund).isDefined
  }

  def paymentsIntoOverseasPensionsIsUpdated(cya: Option[PensionsCYAModel]): Boolean =
    cya.flatMap(_.paymentsIntoOverseasPensions.paymentsIntoOverseasPensionsQuestions).isDefined
  
  def incomeFromOverseasPensionsIsUpdated(cya: Option[PensionsCYAModel]): Boolean =
    cya.flatMap(_.incomeFromOverseasPensions.paymentsFromOverseasPensionsQuestion).isDefined
  
  def overseasPensionsTransferChargesIsUpdated(cya: Option[PensionsCYAModel]): Boolean =
    cya.flatMap(_.transfersIntoOverseasPensions.transferPensionSavings).isDefined
  
  def shortServiceRefundsIsUpdated(cya: Option[PensionsCYAModel]): Boolean =
    cya.flatMap(_.shortServiceRefunds.shortServiceRefund).isDefined

  def statePensionIsUpdated(cya: Option[PensionsCYAModel]): Boolean =
    cya.exists(_.incomeFromPensions.statePension.exists(_.amountPaidQuestion.isDefined))

  def ukPensionsSchemeIsUpdated(cya: Option[PensionsCYAModel]): Boolean =
    cya.exists(_.incomeFromPensions.uKPensionIncomesQuestion.isDefined)
  
  /* ------- hasPriorData  statuses------------------
    We will primarily use these to determine if when going to a journey you
    first either navigate to the CYA page or the 1st page of the journey
   */
  
  def paymentIntoPensionHasPriorData(prior: Option[AllPensionsData]): Boolean =
    prior.exists(_.pensionReliefs.exists(_.pensionReliefs.regularPensionContributions.isDefined))

  def statePensionsHasPriorData(prior: Option[AllPensionsData]): Boolean =
    prior.exists(_.stateBenefits.exists(_.stateBenefitsData.exists(_.statePension.nonEmpty)))
    
  def ukPensionsSchemeHasPriorData(prior: Option[AllPensionsData]): Boolean =
    prior.exists(_.employmentPensions.exists(_.employmentData.nonEmpty))
  
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
