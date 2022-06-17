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

  override val appUrl = "/update-and-submit-income-tax-return/pensions"

  //  *****************       Overview page      *****************************************

  def overviewUrl(taxYear: Int): String = s"http://localhost:11111/update-and-submit-income-tax-return/$taxYear/view"

  def startUrl(taxYear: Int): String = s"http://localhost:11111/update-and-submit-income-tax-return/$taxYear/start"

  val tryAnotherExpectedHref = "http://localhost:11111/report-quarterly/income-and-expenses/view/agents/client-utr"

  //  *****************       External pages      *****************************************

  //  *****************       Summary pages      *****************************************

  def pensionSummaryUrl(taxYear: Int): String = s"$appUrl/$taxYear/pensions-summary"

  //  *****************       payment into pensions pages      ******************************

  object PaymentIntoPensions {


    def checkPaymentsIntoPensionCyaUrl(taxYear: Int): String = s"$appUrl/$taxYear/payments-into-pensions/check-payments-into-pensions"

    def pensionTaxReliefNotClaimedUrl(taxYear: Int): String = s"$appUrl/$taxYear/payments-into-pensions/no-tax-relief"

    def retirementAnnuityUrl(taxYear: Int): String = s"$appUrl/$taxYear/payments-into-pensions/no-tax-relief/retirement-annuity"

    def reliefAtSourcePensionsUrl(taxYear: Int): String = s"$appUrl/$taxYear/payments-into-pensions/relief-at-source"

    def reliefAtSourcePaymentsAndTaxReliefAmountUrl(taxYear: Int): String = s"$appUrl/$taxYear/payments-into-pensions/relief-at-source-amount"

    def reliefAtSourceOneOffPaymentsUrl(taxYear: Int): String = s"$appUrl/$taxYear/payments-into-pensions/one-off-payments"

    def workplacePensionUrl(taxYear: Int): String = s"$appUrl/$taxYear/payments-into-pensions/no-tax-relief/workplace"

    def workplacePensionAmount(taxYear: Int): String = s"$appUrl/$taxYear/payments-into-pensions/no-tax-relief/workplace-amount"

    def retirementAnnuityAmountUrl(taxYear: Int): String = s"$appUrl/$taxYear/payments-into-pensions/no-tax-relief/retirement-annuity-amount"

    def oneOffReliefAtSourcePaymentsAmountUrl(taxYear: Int): String = s"$appUrl/$taxYear/payments-into-pensions/one-off-payments-amount"

    def totalPaymentsIntoRASUrl(taxYear: Int): String = s"$appUrl/$taxYear/payments-into-pensions/total-relief-at-source-check"
  }

  //  *****************     Pension annual allowance pages      ******************************

  object PensionAnnualAllowancePages {
    def reducedAnnualAllowanceUrl(taxYear: Int): String = s"$appUrl/$taxYear/annual-allowance/reduced-annual-allowance"

    def pensionProviderPaidTaxUrl(taxYear: Int): String = s"$appUrl/$taxYear/annual-allowance/pension-provider-paid-tax"

    def amountAboveAnnualAllowanceUrl(taxYear: Int): String = s"$appUrl/$taxYear/annual-allowance/amount-above-annual-allowance"

    def reducedAnnualAllowanceTypeUrl(taxYear: Int): String = s"$appUrl/$taxYear/annual-allowance/reduced-annual-allowance-type"

    def aboveAnnualAllowanceUrl(taxYear: Int): String = s"$appUrl/$taxYear/annual-allowance/above-annual-allowance"

    def pensionProviderPaidTaxAmountUrl(taxYear: Int): String = s"$appUrl/$taxYear/annual-allowance/pension-provider-paid-tax-amount"

    def pensionSchemeTaxReferenceUrl(taxYear: Int): String = s"$appUrl/$taxYear/annual-allowance/pension-scheme-tax-reference"

    def pensionSchemeTaxReferenceUrl(taxYear: Int, pensionSchemeTaxReference: Int): String =
      s"$appUrl/$taxYear/annual-allowance/pension-scheme-tax-reference?pensionSchemeTaxReferenceIndex=$pensionSchemeTaxReference"

    def pstrSummaryUrl(taxYear: Int): String = s"$appUrl/$taxYear/annual-allowance/pension-scheme-tax-reference-summary"

  }

  //  *****************     Income from pensions pages      ******************************

  object IncomeFromPensionsPages {
    def taxOnLumpSumUrl(taxYear: Int): String = s"$appUrl/$taxYear/pension-income/state-pension-lump-sum-tax"

    def taxOnLumpSumAmountUrl(taxYear: Int): String = s"$appUrl/$taxYear/pension-income/state-pension-lump-sum-tax-amount"

    def statePensionLumpSumAmountUrl(taxYear: Int): String = s"$appUrl/$taxYear/pension-income/state-pension-lump-sum-amount"

    def statePensionAmountUrl(taxYear: Int): String = s"$appUrl/$taxYear/pension-income/state-pension-amount"

    def statePensionLumpSumUrl(taxYear: Int): String = s"$appUrl/$taxYear/pension-income/state-pension-lump-sum"

    def statePension(taxYear: Int): String = s"$appUrl/$taxYear/pension-income/state-pension"

    def ukPensionSchemePayments(taxYear: Int): String = s"$appUrl/$taxYear/pension-income/uk-pension-schemes"

    def ukPensionSchemeSummaryListUrl(taxYear: Int): String = s"$appUrl/$taxYear/pension-income/uk-pension-income"

    def pensionSchemeDetailsUrl(taxYear: Int): String = s"$appUrl/$taxYear/pension-income/pension-income-details"

    def pensionSchemeDetailsUrl(taxYear: Int, pensionSchemeIndex: Int): String =
      s"$appUrl/$taxYear/pension-income/pension-income-details?pensionSchemeIndex=$pensionSchemeIndex"

    def pensionAmountUrl(taxYear: Int, index: Int): String = s"$appUrl/$taxYear/pension-income/pension-amount?pensionSchemeIndex=$index"

    def pensionAmountUrl(taxYear: Int): String = s"$appUrl/$taxYear/pension-income/pension-amount"

    def pensionStartDateUrl(taxYear: Int, pensionSchemeIndex: Int): String =
      s"$appUrl/$taxYear/pension-income/pension-start-date?pensionSchemeIndex=$pensionSchemeIndex"

    def pensionStartDateUrl(taxYear: Int): String =
      s"$appUrl/$taxYear/pension-income/pension-start-date"

    def removePensionSchemeUrl(taxYear: Int, pensionSchemeIndex: Option[Int] = None): String = {
      val addOn = if(pensionSchemeIndex.isDefined){s"?pensionSchemeIndex=${pensionSchemeIndex.get}"} else {""}
      s"$appUrl/$taxYear/pension-income/remove-pension-scheme" + addOn
    }
  }

  object PensionLifetimeAllowance {

    def pensionLumpSumUrl(taxYear: Int): String = s"$appUrl/$taxYear/annual-lifetime-allowances/lifetime-allowance-lump-sum"

    def pensionAboveAnnualLifetimeAllowanceUrl(taxYear: Int): String = s"$appUrl/$taxYear/annual-lifetime-allowances/above-annual-allowance"

    def pensionLumpSumDetails(taxYear: Int): String = s"$appUrl/$taxYear/annual-lifetime-allowances/lump-sum-details"

  }

}

//scalastyle:on number.of.methods
