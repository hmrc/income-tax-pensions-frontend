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

package models.pension

import cats.implicits.{catsSyntaxOptionId, none}
import forms.Countries
import models.mongo.PensionsCYAModel
import models.pension.charges.UnauthorisedPaymentsViewModel.AmountType.{Amount, TaxAmount}
import models.pension.charges.UnauthorisedPaymentsViewModel.PaymentResult.{NoSurcharge, Surcharge}
import models.pension.charges.UnauthorisedPaymentsViewModel.{AmountType, PaymentResult}
import models.pension.charges._
import models.pension.employmentPensions.EmploymentPensions
import models.pension.income.PensionIncome
import models.pension.reliefs.{PaymentsIntoPensionsViewModel, PensionReliefs}
import models.pension.statebenefits._
import play.api.libs.json.{Json, OFormat}
import utils.Constants.zero

case class AllPensionsData(pensionReliefs: Option[PensionReliefs],
                           pensionCharges: Option[PensionCharges],
                           stateBenefits: Option[AllStateBenefitsData],    /* Model taken from income-tax-state-benefits */
                           employmentPensions: Option[EmploymentPensions], /* Model taken from income-tax-employment */
                           pensionIncome: Option[PensionIncome]) {

  def getPaymentsIntoPensionsCyaFromPrior: PaymentsIntoPensionsViewModel =
    pensionReliefs
      .map(pr => PaymentsIntoPensionsViewModel.fromSubmittedReliefs(pr.pensionReliefs))
      .getOrElse(PaymentsIntoPensionsViewModel.empty.copy(totalPaymentsIntoRASQuestion = Some(true)))
}

object AllPensionsData {
  type PriorPensionsData = AllPensionsData
  implicit val formats: OFormat[AllPensionsData] = Json.format[AllPensionsData]

  val Zero: BigDecimal = 0.0

  def isNotZero(value: Option[BigDecimal]): Boolean = value.exists(_ != Zero)

  def generateSessionModelFromPrior(prior: AllPensionsData): PensionsCYAModel =
    PensionsCYAModel(
      paymentsIntoPension = PaymentsIntoPensionsViewModel.empty, // We only load prior answers when entering the payments into pension data
      pensionsAnnualAllowances = PensionAnnualAllowancesViewModel.empty,
      incomeFromPensions = generateIncomeFromPensionsModelFromPrior(prior),
      unauthorisedPayments = generateUnauthorisedPaymentsCyaModelFromPrior(prior),
      paymentsIntoOverseasPensions = PaymentsIntoOverseasPensionsViewModel.empty,
      incomeFromOverseasPensions = IncomeFromOverseasPensionsViewModel.empty,
      transfersIntoOverseasPensions = TransfersIntoOverseasPensionsViewModel.empty,
      shortServiceRefunds = generateShortServiceRefundCyaFromPrior(prior)
    )

  private def generateIncomeFromPensionsModelFromPrior(prior: AllPensionsData): IncomeFromPensionsViewModel = {
    val (statePen, statePenLumpSum)   = populateStatePensionSessionFromPrior(prior)
    val (uKPenIncomesQ, uKPenIncomes) = generateUkPensionSessionFromPrior(prior)

    IncomeFromPensionsViewModel(
      statePension = statePen,
      statePensionLumpSum = statePenLumpSum,
      uKPensionIncomesQuestion = uKPenIncomesQ,
      uKPensionIncomes = if (uKPenIncomes.isEmpty) None else Some(uKPenIncomes)
    )
  }

  private def populateStatePensionSessionFromPrior(prior: AllPensionsData): (Option[StateBenefitViewModel], Option[StateBenefitViewModel]) = {
    def getStatePensionModel(statePension: Option[StateBenefit]): Option[StateBenefitViewModel] =
      statePension match {
        case Some(benefit) =>
          Some(
            StateBenefitViewModel(
              benefitId = Some(benefit.benefitId),
              startDateQuestion = Some(true),
              startDate = Some(benefit.startDate),
              amountPaidQuestion = Some(benefit.amount.isDefined),
              amount = benefit.amount,
              taxPaidQuestion = Some(benefit.taxPaid.isDefined),
              taxPaid = benefit.taxPaid
            ))
        case _ => None
      }

    (
      getStatePensionModel(prior.stateBenefits.flatMap(_.stateBenefitsData.flatMap(_.statePension))),
      getStatePensionModel(prior.stateBenefits.flatMap(_.stateBenefitsData.flatMap(_.statePensionLumpSum)))
    )
  }

  def generateUkPensionSessionFromPrior(prior: AllPensionsData): (Option[Boolean], List[UkPensionIncomeViewModel]) = {
    def getUkPensionIncome(prior: AllPensionsData): List[UkPensionIncomeViewModel] =
      prior.employmentPensions match {
        case Some(ep) =>
          ep.employmentData.map(data =>
            UkPensionIncomeViewModel(
              employmentId = Some(data.employmentId),
              pensionId = data.pensionId,
              startDate = data.startDate,
              endDate = data.endDate,
              pensionSchemeName = Some(data.pensionSchemeName),
              pensionSchemeRef = data.pensionSchemeRef,
              amount = data.amount,
              taxPaid = data.taxPaid,
              isCustomerEmploymentData = data.isCustomerEmploymentData
            ))
        case _ => Nil
      }

    def getUkPensionQuestion(prior: AllPensionsData) =
      if (getUkPensionIncome(prior).nonEmpty) Some(true) else None

    (getUkPensionQuestion(prior), getUkPensionIncome(prior))
  }

  private def generateUnauthorisedPaymentsCyaModelFromPrior(prior: AllPensionsData): UnauthorisedPaymentsViewModel = {
    val journeyPrior = prior.pensionCharges.flatMap(_.pensionSchemeUnauthorisedPayments)

    def determineQuestionValue(paymentResult: PaymentResult)(valueIfBlankSubmission: Option[Boolean]): Option[Boolean] = {
      val maybeCharge = paymentResult match {
        case Surcharge   => journeyPrior.flatMap(_.surcharge)
        case NoSurcharge => journeyPrior.flatMap(_.noSurcharge)
      }
      maybeCharge.fold(ifEmpty = none[Boolean]) { c =>
        if (c.amount == 0 && c.foreignTaxPaid == 0) valueIfBlankSubmission else true.some
      }
    }

    def determineAmount(questionValue: Option[Boolean], paymentType: PaymentResult, amountType: AmountType): Option[BigDecimal] =
      questionValue.fold(ifEmpty = none[BigDecimal]) { bool =>
        val amountFromPrior =
          (paymentType, amountType) match {
            case (Surcharge, Amount)      => journeyPrior.flatMap(_.surcharge.map(_.amount))
            case (Surcharge, TaxAmount)   => journeyPrior.flatMap(_.surcharge.map(_.foreignTaxPaid))
            case (NoSurcharge, Amount)    => journeyPrior.flatMap(_.noSurcharge.map(_.amount))
            case (NoSurcharge, TaxAmount) => journeyPrior.flatMap(_.noSurcharge.map(_.foreignTaxPaid))
          }
        if (bool) amountFromPrior else none[BigDecimal]
      }

    val hasSurcharge          = determineQuestionValue(Surcharge)(valueIfBlankSubmission = false.some)
    val hasSurchargeTaxAmount = determineQuestionValue(Surcharge)(valueIfBlankSubmission = none[Boolean])

    val hasNoSurcharge          = determineQuestionValue(NoSurcharge)(valueIfBlankSubmission = false.some)
    val hasNoSurchargeTaxAmount = determineQuestionValue(NoSurcharge)(valueIfBlankSubmission = none[Boolean])

    UnauthorisedPaymentsViewModel(
      surchargeQuestion = hasSurcharge,
      noSurchargeQuestion = hasNoSurcharge,
      surchargeAmount = determineAmount(hasSurcharge, Surcharge, Amount),
      surchargeTaxAmountQuestion = hasSurchargeTaxAmount,
      surchargeTaxAmount = determineAmount(hasSurcharge, Surcharge, TaxAmount),
      noSurchargeAmount = determineAmount(hasNoSurcharge, NoSurcharge, Amount),
      noSurchargeTaxAmountQuestion = hasNoSurchargeTaxAmount,
      noSurchargeTaxAmount = determineAmount(hasNoSurcharge, NoSurcharge, TaxAmount),
      ukPensionSchemesQuestion =
        if (journeyPrior.flatMap(_.surcharge).exists(_.isBlankSubmission) && journeyPrior.flatMap(_.noSurcharge).exists(_.isBlankSubmission))
          none[Boolean]
        else journeyPrior.map(_.pensionSchemeTaxReference).map(_.nonEmpty),
      pensionSchemeTaxReference = journeyPrior.flatMap(_.pensionSchemeTaxReference)
    )
  }

  private def generateShortServiceRefundCyaFromPrior(prior: AllPensionsData): ShortServiceRefundsViewModel = {
    val priorTaxPaidAmount = prior.pensionCharges.flatMap(_.overseasPensionContributions.map(_.shortServiceRefundTaxPaid))
    ShortServiceRefundsViewModel(
      shortServiceRefund = prior.pensionCharges.map(_.overseasPensionContributions.map(_.shortServiceRefund).isDefined),
      shortServiceRefundCharge = prior.pensionCharges.flatMap(_.overseasPensionContributions.map(_.shortServiceRefund)),
      shortServiceRefundTaxPaid =
        if (priorTaxPaidAmount.contains(zero)) false.some
        else if (priorTaxPaidAmount.isEmpty) none[Boolean]
        else true.some,
      shortServiceRefundTaxPaidCharge =
        if (priorTaxPaidAmount.contains(zero)) none[BigDecimal]
        else priorTaxPaidAmount,
      refundPensionScheme = prior.pensionCharges
        .flatMap(_.overseasPensionContributions.map(_.overseasSchemeProvider.map(osp =>
          OverseasRefundPensionScheme(
            name = Some(osp.providerName),
            qualifyingRecognisedOverseasPensionScheme = osp.qualifyingRecognisedOverseasPensionScheme.map(_.head).map(_.replace("Q", "")),
            providerAddress = Some(osp.providerAddress),
            alphaTwoCountryCode = Countries.get2AlphaCodeFrom3AlphaCode(Some(osp.providerCountryCode)),
            alphaThreeCountryCode = Some(osp.providerCountryCode)
          ))))
        .getOrElse(Nil)
    )
  }
}
