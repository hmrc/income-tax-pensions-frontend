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

//scalastyle:off number.of.methods
object PageUrls extends IntegrationTest {

  def fullUrl(endOfUrl: String): String = s"http://localhost:$port" + endOfUrl

  override lazy val appUrl = "/update-and-submit-income-tax-return/pensions"

  //  *****************       Overview page      *****************************************

  def overviewUrl(taxYear: Int): String = s"http://localhost:11111/update-and-submit-income-tax-return/$taxYear/view"

  def startUrl(taxYear: Int): String = s"http://localhost:11111/update-and-submit-income-tax-return/$taxYear/start"

  val tryAnotherExpectedHref = "http://localhost:11111/report-quarterly/income-and-expenses/view/agents/client-utr"

  //  *****************       External pages      *****************************************

  //  *****************       Summary pages      *****************************************

  def pensionSummaryUrl(taxYear: Int): String = s"$appUrl/$taxYear/pensions-summary"

  //  *****************       payment into pensions pages      ******************************

  def checkPaymentsIntoPensionCyaUrl(taxYear: Int): String = s"$appUrl/$taxYear/payments-into-pensions"

  def pensionTaxReliefNotClaimedUrl(taxYear: Int): String = s"$appUrl/$taxYear/payments-into-pensions/no-tax-relief"

  def retirementAnnuityUrl(taxYear: Int): String = s"$appUrl/$taxYear/payments-into-pensions/no-tax-relief/retirement-annuity"

  def reliefAtSourcePensionsUrl(taxYear: Int): String = s"$appUrl/$taxYear/payments-into-pensions/relief-at-source"

  def reliefAtSourcePaymentsAndTaxReliefAmountUrl(taxYear: Int): String = s"$appUrl/$taxYear/payments-into-pensions/relief-at-source-amount"

  //scalastyle:on number.of.methods

}
