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

package utils

import models.pension.AllPensionsData

object StatusHelper {

  def paymentsIntoPensionsIsUpdated(prior: Option[AllPensionsData]): Boolean = {

    // TODO: Do we care if it's deleted so might do as below instead?
    //  val reliefs = prior.flatMap(_.pensionReliefs)
    // reliefs.isDefined && reliefs.get.deletedOn.isEmpty

    prior.flatMap(_.pensionReliefs).isDefined
  }

  def incomeFromPensionsIsUpdated(prior: Option[AllPensionsData]): Boolean = {
    //TODO: confirm any one of these is sufficient to be 'updated'
    prior.flatMap(_.stateBenefits.flatMap(_.customerAddedStateBenefits)).isDefined ||
      prior.flatMap(_.stateBenefits.flatMap(_.stateBenefits)).isDefined
  }

  def pensionFromAnnualAllowanceIsUpdated(prior: Option[AllPensionsData]): Boolean = {
    prior.flatMap(_.pensionCharges.flatMap(_.pensionSavingsTaxCharges)).isDefined
  }

  def pensionLifetimeAllowanceIsUpdated(prior: Option[AllPensionsData]): Boolean = {
    prior.flatMap(_.pensionCharges.flatMap(_.pensionContributions)).isDefined
  }

  def unauthorisedPaymentsFromPensionsIsUpdated(prior: Option[AllPensionsData]): Boolean = {
    prior.flatMap(_.pensionCharges.flatMap(_.pensionSchemeUnauthorisedPayments)).isDefined
  }

  def paymentsIntoOverseasPensionsIsUpdated(prior: Option[AllPensionsData]): Boolean = {
    prior.flatMap(_.pensionCharges.flatMap(_.pensionSchemeOverseasTransfers)).isDefined
  }

}
