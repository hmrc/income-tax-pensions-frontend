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

//scalastyle:off number.of.methods
object PageUrls extends IntegrationTest {

  override val appUrl = "/update-and-submit-income-tax-return/pensions"
  val tryAnotherExpectedHref = "http://localhost:11111/report-quarterly/income-and-expenses/view/agents/client-utr"

  //  *****************       Overview page      *****************************************

  def fullUrl(endOfUrl: String): String = s"http://localhost:$port" + endOfUrl

  def overviewUrl(taxYear: Int): String = s"http://localhost:11111/update-and-submit-income-tax-return/$taxYear/view"

  def startUrl(taxYear: Int): String = s"http://localhost:11111/update-and-submit-income-tax-return/$taxYear/start"

  //  *****************       External pages      *****************************************

  //  *****************       Summary pages      *****************************************

  def pensionSummaryUrl(taxYear: Int): String = s"$appUrl/$taxYear/pensions-summary"

  def overseasPensionsSummaryUrl(taxYear: Int): String = s"$appUrl/$taxYear/overseas-pensions"

  //  *****************       payment into pensions pages      ******************************

  object PaymentIntoPensions {

    def checkPaymentsIntoPensionStatusUrl(taxYear: Int): String = s"$appUrl/$taxYear/payments-into-pensions/payments-into-pensions-status"

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

    def pensionProviderPaidTaxUrl(taxYear: Int): String = s"$appUrl/$taxYear/annual-lifetime-allowances/pension-provider-paid-tax"

    def amountAboveAnnualAllowanceUrl(taxYear: Int): String = s"$appUrl/$taxYear/annual-allowance/gone-above-lifetime-allowance"

    def reducedAnnualAllowanceTypeUrl(taxYear: Int): String = s"$appUrl/$taxYear/annual-allowance/reduced-annual-allowance-type"

    def aboveAnnualAllowanceUrl(taxYear: Int): String = s"$appUrl/$taxYear/annual-allowance/above-annual-allowance"

    def pensionSchemeTaxReferenceUrl(taxYear: Int): String = s"$appUrl/$taxYear/annual-allowance/pension-scheme-tax-reference"

    def pensionSchemeTaxReferenceUrl(taxYear: Int, pensionSchemeTaxReference: Int): String =
      s"$appUrl/$taxYear/annual-allowance/pension-scheme-tax-reference?pensionSchemeTaxReferenceIndex=$pensionSchemeTaxReference"

    def pstrSummaryUrl(taxYear: Int): String = s"$appUrl/$taxYear/annual-allowance/pension-scheme-tax-reference-summary"

    def transferPensionSchemeTaxUrl(taxYear: Int): String = s"$appUrl/${taxYear.toString}/overseas-pensions/overseas-transfer-charges/overseas-transfer-charge-tax"

    def shortServiceTaxableRefundUrl(taxYear: Int): String = s"$appUrl/${taxYear.toString}/overseas-pensions/short-service-refunds/taxable-short-service-refunds"

    def nonUkTaxRefundsUrl(taxYear: Int): String = s"$appUrl/${taxYear.toString}/overseas-pensions/short-service-refunds/short-service-refund-non-uk-tax"
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
      val addOn = if (pensionSchemeIndex.isDefined) {
        s"?pensionSchemeIndex=${pensionSchemeIndex.get}"
      } else {
        ""
      }
      s"$appUrl/$taxYear/pension-income/remove-pension-scheme" + addOn
    }

    def ukPensionIncomeCyaUrl(taxYear: Int): String = s"$appUrl/$taxYear/pension-income/uk-pension-income/check-pension-income"

    def ukPensionincomeSummaryUrl(taxYear: Int): String = s"$appUrl/$taxYear/pension-income/pensions-income-summary"
  }

  object PensionLifetimeAllowance {

    def pensionLumpSumUrl(taxYear: Int): String = s"$appUrl/$taxYear/annual-lifetime-allowances/pension-lump-sum"

    def pensionAboveAnnualLifetimeAllowanceUrl(taxYear: Int): String = s"$appUrl/$taxYear/annual-lifetime-allowances/above-annual-allowance"

    def pensionLumpSumDetails(taxYear: Int): String = s"$appUrl/$taxYear/annual-lifetime-allowances/lump-sum-details"

    def pensionLifeTimeAllowanceAnotherWayUrl(taxYear: Int): String = s"$appUrl/$taxYear/annual-lifetime-allowances/pension-another-way"

    def pensionTakenAnotherWayAmountUrl(taxYear: Int): String = s"$appUrl/$taxYear/annual-lifetime-allowances/pension-another-way-details"

    def pensionTaxReferenceNumberLifetimeAllowanceUrl(taxYear: Int): String = s"$appUrl/$taxYear/lifetime-allowance/pension-scheme-tax-reference"

    def pensionTaxReferenceNumberLifetimeAllowanceUrlIndex: Int => Int => String = (pensionSchemeTaxReference: Int) =>
      (taxYear: Int) => s"$appUrl/$taxYear/lifetime-allowance/pension-scheme-tax-reference?pensionSchemeTaxReferenceIndex=$pensionSchemeTaxReference"

    def checkAnnualLifetimeAllowanceCYA(taxYear: Int): String = s"$appUrl/$taxYear/annual-lifetime-allowances/check-lifetime-allowances"

  }

  object UnAuthorisedPayments {
    def surchargeAmountUrl(taxYear: Int): String = s"$appUrl/$taxYear/unauthorised-payments-from-pensions/amount-surcharged"

    def noSurchargeAmountUrl(taxYear: Int): String = s"$appUrl/$taxYear/unauthorised-payments-from-pensions/amount-not-surcharged"
  }

  object unauthorisedPaymentsPages {
    def didYouPayNonUkTaxUrl(taxYear: Int): String = s"$appUrl/$taxYear/unauthorised-payments-from-pensions/tax-on-amount-surcharged"

    def nonUKTaxOnAmountSurcharge(taxYear: Int): String = s"$appUrl/$taxYear/unauthorised-payments-from-pensions/tax-on-amount-not-surcharged"

    def whereAnyOfTheUnauthorisedPaymentsUrl(taxYear: Int): String = s"$appUrl/$taxYear/unauthorised-payments-from-pensions/uk-pension-scheme"

    def unauthorisedPaymentsUrl(taxYear: Int): String = s"$appUrl/$taxYear/unauthorised-payments-from-pensions/unauthorised-payments"

    def pensionSchemeTaxReferenceUrl(taxYear: Int): String = s"$appUrl/$taxYear/unauthorised-payments-from-pensions/pension-scheme-tax-reference"

    def pensionSchemeTaxReferenceUrlWithIndex(taxYear: Int, index: Int): String =
      s"$appUrl/$taxYear/unauthorised-payments-from-pensions/pension-scheme-tax-reference?pensionSchemeIndex=$index"

    def ukPensionSchemeDetailsUrl(taxYear: Int): String = s"$appUrl/$taxYear/unauthorised-payments-from-pensions/uk-pension-scheme-details"

    def checkUnauthorisedPaymentsCyaUrl(taxYear: Int): String = s"$appUrl/$taxYear/unauthorised-payments-from-pensions/check-unauthorised-payments"

    def removePensionSchemeReferenceUrl(taxYear: Int, pensionSchemeIndex: Option[Int] = None): String = {
      val addOn = if (pensionSchemeIndex.isDefined) {
        s"?pensionSchemeIndex=${pensionSchemeIndex.get}"
      } else {
        ""
      }
      s"$appUrl/$taxYear/unauthorised-payments-from-pensions/remove-pension-scheme-tax-reference" + addOn
    }

  }

  object OverseasPensionPages {
    def paymentsIntoPensionSchemeUrl(taxYear: Int): String = s"$appUrl/$taxYear/overseas-pensions/payments-into-overseas-pensions/payments-into-schemes"

    def employerPayOverseasPensionUrl(taxYear: Int): String =
      s"$appUrl/$taxYear/overseas-pensions/payments-into-overseas-pensions/employer-payments-into-schemes"

    def taxEmployerPaymentsUrl(taxYear: Int): String = s"$appUrl/$taxYear/overseas-pensions/payments-into-overseas-pensions/tax-employer-payments"

    def qopsReferenceUrl(taxYear: Int): String =
      s"$appUrl/$taxYear/overseas-pensions/payments-into-overseas-pensions/qualifying-overseas-pension-scheme-reference-number"

    def pensionCustomerReferenceNumberUrl(taxYear: Int): String =
      s"$appUrl/$taxYear/overseas-pensions/payments-into-overseas-pensions/pensions-customer-reference-number"
  }

  //  *****************     Income from overseas pensions pages      ******************************
  object IncomeFromOverseasPensionsPages {
    def incomeFromOverseasPensionsStatus(taxYear: Int): String =
      s"$appUrl/$taxYear/overseas-pensions/income-from-overseas-pensions/pension-overseas-income-status"

    def pensionOverseasIncomeCountryUrl(taxYear: Int): String =
      s"$appUrl/$taxYear/overseas-pensions/income-from-overseas-pensions/pension-overseas-income-country"

    def pensionOverseasIncomeCountryUrlIndex: Int => Int => String = (index: Int) =>
      (taxYear: Int) => s"$appUrl/$taxYear/overseas-pensions/income-from-overseas-pensions/pension-overseas-income-country?index=$index"

    def pensionOverseasIncomeCountryUrlIndex2(taxYear: Int, index: Int): String =
      s"$appUrl/$taxYear/overseas-pensions/income-from-overseas-pensions/pension-overseas-income-country?index=$index"

    def incomeFromOverseasPensionsAmounts(taxYear: Int, index: Int): String =
      s"$appUrl/$taxYear/overseas-pensions/income-from-overseas-pensions/pension-overseas-income-amounts?index=$index"

    def checkIncomeFromOverseasPensionsCyaUrl(taxYear: Int): String =
      s"$appUrl/$taxYear/overseas-pensions/income-from-overseas-pensions/check-overseas-pension-income-cya"

    def foreignTaxCreditReliefControllerUrl(taxYear: Int, index: Int): String =
      s"$appUrl/$taxYear/overseas-pensions/income-from-overseas-pensions/pension-overseas-income-ftcr?index=$index"

    def taxableAmountUrl: Int => Int => String =
      (index: Int) => (taxYear: Int) => s"$appUrl/$taxYear/overseas-pensions/income-from-overseas-pensions/taxable-amount?index=$index"

    def countrySummaryListControllerUrl(taxYear: Int): String =
      s"$appUrl/$taxYear/overseas-pensions/income-from-overseas-pensions/pension-overseas-income-country-summary"

    def overseasPensionsSchemeSummaryUrl(taxYear: Int, index: Int): String =
      s"$appUrl/$taxYear/overseas-pensions/income-from-overseas-pensions/pension-scheme-summary?index=$index"

  }

  object TransferIntoOverseasPensions {

    def overseasTransferChargePaidUrl(taxYear: Int): String =
      s"$appUrl/$taxYear/overseas-pensions/overseas-transfer-charges/overseas-transfer-charge-paid"

    def overseasTransferChargePaidUrl(taxYear: Int, index: Int): String =
      s"$appUrl/$taxYear/overseas-pensions/overseas-transfer-charges/overseas-transfer-charge-paid?pensionSchemeIndex=$index"

    def transferChargeSummaryUrl(taxYear: Int): String =
      s"$appUrl/$taxYear/overseas-pensions/overseas-transfer-charges/transfer-charges-summary"

    def removeTransferChargeScheme(taxYear: Int, index: Int):String =
      s"$appUrl/$taxYear/overseas-pensions/payments-into-overseas-pensions/remove-overseas-pension-scheme?index=$index"
  }

  object TransferIntoOverseasPensionsPages {
    def pensionSchemespayingTrransferCharges(taxYear: Int): String =
      s"$appUrl/$taxYear/overseas-pensions/overseas-transfer-charges/transfer-charges-summary"

    def transferPensionSavingsUrl(taxYear: Int): String =
      s"$appUrl/$taxYear/overseas-pensions/overseas-transfer-charges/transfer-pension-savings"
  }

  object ShortServiceRefunds {
    def taxOnShortServiceRefund(taxYear: Int): String =
      s"$appUrl/$taxYear/overseas-pensions/short-service-refunds/short-service-refunds-uk-tax"
  }

}
//scalastyle:on number.of.methods
