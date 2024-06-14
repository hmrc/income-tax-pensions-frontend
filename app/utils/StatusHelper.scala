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

}
