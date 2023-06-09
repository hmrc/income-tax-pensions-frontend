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

  
  //  *****************     Income from pensions pages      ******************************

  object IncomeFromPensionsPages {
    def pensionIncomeSummaryUrl(taxYear: Int): String = s"$appUrl/$taxYear/pension-income/pensions-income-summary"
    
    def taxOnLumpSumUrl(taxYear: Int): String = s"$appUrl/$taxYear/pension-income/state-pension-lump-sum-tax"

    def statePensionLumpSumStartDateUrl(taxYear: Int): String = s"$appUrl/$taxYear/pension-income/state-pension-lump-sum-date"

    def statePensionLumpSumUrl(taxYear: Int): String = s"$appUrl/$taxYear/pension-income/state-pension-lump-sum"

    def statePension(taxYear: Int): String = s"$appUrl/$taxYear/pension-income/state-pension"

    def statePensionStartDateUrl(taxYear: Int): String = s"$appUrl/${taxYear.toString}/pension-income/state-pension-start-date"

    def ukPensionSchemePayments(taxYear: Int): String = s"$appUrl/$taxYear/pension-income/uk-pension-schemes"

    def ukPensionSchemeSummaryListUrl(taxYear: Int): String = s"$appUrl/$taxYear/pension-income/uk-pension-income"

    def pensionSchemeDetailsUrl(taxYear: Int, pensionSchemeIndex: Option[Int]): String = {
      val baseUrl = s"$appUrl/$taxYear/pension-income/pension-income-details"
      pensionSchemeIndex.fold(baseUrl)(idx =>s"$baseUrl?pensionSchemeIndex=$idx")
    }

    def pensionAmountUrl(taxYear: Int, pensionSchemeIndex: Option[Int]): String = {
      val baseUrl = s"$appUrl/$taxYear/pension-income/pension-amount"
      pensionSchemeIndex.fold(baseUrl)(idx =>s"$baseUrl?pensionSchemeIndex=$idx")
    }

    def pensionStartDateUrl(taxYear: Int, pensionSchemeIndex:Option[Int]): String = {
      val baseUrl = s"$appUrl/$taxYear/pension-income/pension-start-date"
      pensionSchemeIndex.fold(baseUrl)(idx =>s"$baseUrl?pensionSchemeIndex=$idx")
    }

    def removePensionSchemeUrl(taxYear: Int, pensionSchemeIndex: Option[Int] = None): String = {
      val baseUrl = s"$appUrl/$taxYear/pension-income/remove-pension-scheme"
      pensionSchemeIndex.fold(baseUrl)(idx =>s"$baseUrl?pensionSchemeIndex=$idx")
    }
    def pensionSchemeSummaryUrl(taxYear: Int, pensionSchemeIndex: Option[Int]): String = {
      val baseUrl = s"$appUrl/$taxYear/pension-income/pension-scheme-summary"
      pensionSchemeIndex.fold(baseUrl)(idx =>s"$baseUrl?pensionSchemeIndex=$idx")
    }

    def addToCalculationUrl(taxYear: Int): String = s"$appUrl/$taxYear/pension-income/state-pension-add-to-calculation"
    def ukPensionIncomeCyaUrl(taxYear: Int): String = s"$appUrl/$taxYear/pension-income/check-uk-pension-income"
    
    def statePensionCyaUrl(taxYear: Int): String = s"$appUrl/$taxYear/pension-income/check-state-pension"
  }

  //  *****************     Pension annual allowance pages      ******************************

  object PensionAnnualAllowancePages {
    def reducedAnnualAllowanceUrl(taxYear: Int): String = s"$appUrl/$taxYear/annual-allowance/reduced-annual-allowance"

    def pensionProviderPaidTaxUrl(taxYear: Int): String = s"$appUrl/$taxYear/annual-lifetime-allowances/pension-provider-paid-tax"

    def reducedAnnualAllowanceTypeUrl(taxYear: Int): String = s"$appUrl/$taxYear/annual-allowance/reduced-annual-allowance-type"

    def aboveAnnualAllowanceUrl(taxYear: Int): String = s"$appUrl/$taxYear/annual-lifetime-allowances/above-annual-allowance"

    def pensionSchemeTaxReferenceUrl(taxYear: Int): String = s"$appUrl/$taxYear/annual-allowance/pension-scheme-tax-reference"

    def pensionSchemeTaxReferenceUrl(taxYear: Int, pensionSchemeTaxReference: Int): String =
      s"$appUrl/$taxYear/annual-allowance/pension-scheme-tax-reference?pensionSchemeTaxReferenceIndex=$pensionSchemeTaxReference"

    def pstrSummaryUrl(taxYear: Int): String = s"$appUrl/$taxYear/annual-allowance/pension-scheme-tax-reference-summary"

    def transferPensionSchemeTaxUrl(taxYear: Int): String =
      s"$appUrl/${taxYear.toString}/overseas-pensions/overseas-transfer-charges/overseas-transfer-charge-tax"
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
      val baseUrl = s"$appUrl/$taxYear/unauthorised-payments-from-pensions/remove-pension-scheme-tax-reference"
      pensionSchemeIndex.fold(baseUrl)(idx =>s"$baseUrl?pensionSchemeIndex=$idx")
    }
  }

  object PaymentIntoOverseasPensions {

    def paymentsIntoPensionSchemeUrl(taxYear: Int): String = s"$appUrl/$taxYear/overseas-pensions/payments-into-overseas-pensions/payments-into-schemes"

    def employerPayOverseasPensionUrl(taxYear: Int): String =
      s"$appUrl/$taxYear/overseas-pensions/payments-into-overseas-pensions/employer-payments-into-schemes"

    def taxEmployerPaymentsUrl(taxYear: Int): String = s"$appUrl/$taxYear/overseas-pensions/payments-into-overseas-pensions/tax-employer-payments"

    def qopsReferenceUrl(taxYear : Int): String =
      s"$appUrl/$taxYear/overseas-pensions/payments-into-overseas-pensions/qualifying-overseas-pension-scheme-reference-number?index=0"

    def qopsReferenceUrlWithIndex(taxYear: Int, index: Int): String =
      s"$appUrl/$taxYear/overseas-pensions/payments-into-overseas-pensions/qualifying-overseas-pension-scheme-reference-number?index=$index"

    def sf74ReferenceUrl(taxYear: Int, index: Int): String =
      s"$appUrl/$taxYear/overseas-pensions/payments-into-overseas-pensions/pensions-overseas-sf74?reliefIndex=$index"

    def pensionCustomerReferenceNumberUrl(taxYear:Int, index:Option[Int]): String = {
      val baseUrl = s"$appUrl/$taxYear/overseas-pensions/payments-into-overseas-pensions/pensions-customer-reference-number"
      index.fold(baseUrl)(idx => s"$baseUrl?index=$idx")
    }
    
   def untaxedEmployerPaymentsUrl(taxYear: Int, index: Int): String =
      s"$appUrl/$taxYear/overseas-pensions/payments-into-overseas-pensions/untaxed-employer-payments?index=$index"

    def pensionReliefTypeUrl(taxYear: Int, index: Int): String =
      s"$appUrl/$taxYear/overseas-pensions/payments-into-overseas-pensions/pensions-overseas-emp-relief-status?reliefIndex=$index"

    def pensionReliefSchemeDetailsUrl(taxYear: Int, index: Int): String =
      s"$appUrl/$taxYear/overseas-pensions/payments-into-overseas-pensions/pensions-overseas-details-summary?reliefIndex=$index"

    val doubleTaxationAgreementUrl: Int => Int => String =
      (index: Int) => (taxYear: Int) => s"$appUrl/$taxYear/overseas-pensions/payments-into-overseas-pensions/double-taxation-agreement-details?index=$index"

    def pensionReliefSchemeSummaryUrl(taxYear: Int) : String =
      s"$appUrl/$taxYear/overseas-pensions/payments-into-overseas-pensions/untaxed-schemes-summary"

    val paymentsIntoOverseasPensionsCyaUrl: Int => String = (taxYear: Int) =>
      s"$appUrl/$taxYear/overseas-pensions/payments-into-overseas-pensions/check-overseas-pension-details"

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

    def overseasTransferChargePaidUrlNoIndex(taxYear: Int): String =
      s"$appUrl/$taxYear/overseas-pensions/overseas-transfer-charges/overseas-transfer-charge-paid"

    def overseasTransferChargePaidUrl(taxYear: Int, index: Int): String =
      s"$appUrl/$taxYear/overseas-pensions/overseas-transfer-charges/overseas-transfer-charge-paid?pensionSchemeIndex=$index"

    def removeTransferChargeScheme(taxYear: Int, index: Int): String =
      s"$appUrl/$taxYear/overseas-pensions/payments-into-overseas-pensions/remove-overseas-pension-scheme?index=$index"

    def transferPensionSavingsUrl(taxYear: Int): String =
      s"$appUrl/$taxYear/overseas-pensions/overseas-transfer-charges/transfer-pension-savings"

    def overseasTransferChargeUrl(taxYear: Int): String =
      s"$appUrl/$taxYear/overseas-pensions/overseas-transfer-charges/transfer-charge"

    def pensionSchemeTaxTransferUrl(taxYear: Int): String =
      s"$appUrl/$taxYear/overseas-pensions/overseas-transfer-charges/overseas-transfer-charge-tax"

    def transferChargeSummaryUrl(taxYear: Int): String =
      s"$appUrl/$taxYear/overseas-pensions/overseas-transfer-charges/transfer-charges-summary"

    def checkYourDetailsPensionUrl(taxYear: Int): String =
      s"$appUrl/$taxYear/overseas-pensions/overseas-transfer-charges/transfer-charges/check-transfer-charges-details"
  }

  object ShortServiceRefunds {
    def taxOnShortServiceRefund(taxYear: Int): String = s"$appUrl/$taxYear/overseas-pensions/short-service-refunds/short-service-refunds-uk-tax"

    def refundSummaryUrl(taxYear: Int): String = s"$appUrl/$taxYear/overseas-pensions/short-service-refunds/short-service-refund-summary"

    def shortServiceTaxableRefundUrl(taxYear: Int): String =
      s"$appUrl/${taxYear.toString}/overseas-pensions/short-service-refunds/taxable-short-service-refunds"

    def nonUkTaxRefundsUrl(taxYear: Int): String = s"$appUrl/${taxYear.toString}/overseas-pensions/short-service-refunds/short-service-refund-non-uk-tax"

    def shortServiceRefundsCYAUrl(taxYear: Int): String =
      s"$appUrl/${taxYear.toString}/overseas-pensions/short-service-refunds/check-short-service-refund-details"

  }

}
//scalastyle:on number.of.methods
