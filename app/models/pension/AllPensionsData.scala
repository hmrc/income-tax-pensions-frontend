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
import models.pension.income.{OverseasPensionContribution, PensionIncome}
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
  implicit val formats: OFormat[AllPensionsData] = Json.format[AllPensionsData]

  val Zero: BigDecimal = 0.0

  def isNotZero(value: Option[BigDecimal]) =
    value.exists(_ != Zero)

  def generateSessionModelFromPrior(prior: AllPensionsData): PensionsCYAModel =
    PensionsCYAModel(
      paymentsIntoPension = prior.getPaymentsIntoPensionsCyaFromPrior,
      pensionsAnnualAllowances = generateAnnualAllowanceSessionFromPrior(prior),
      incomeFromPensions = generateIncomeFromPensionsModelFromPrior(prior),
      unauthorisedPayments = generateUnauthorisedPaymentsCyaModelFromPrior(prior),
      paymentsIntoOverseasPensions = generatePaymentsIntoOverseasPensionsFromPrior(prior),
      incomeFromOverseasPensions = generateIncomeFromOverseasPensionsCyaFromPrior(prior),
      transfersIntoOverseasPensions = generateTransfersIntoOverseasPensionsCyaFromPrior(prior),
      shortServiceRefunds = generateShortServiceRefundCyaFromPrior(prior)
    )

  def generateAnnualAllowanceSessionFromPrior(prior: AllPensionsData): PensionAnnualAllowancesViewModel =
    PensionAnnualAllowancesViewModel(
      reducedAnnualAllowanceQuestion = prior.pensionCharges.flatMap(_.pensionContributions).flatMap(_.isAnnualAllowanceReduced),
      moneyPurchaseAnnualAllowance = prior.pensionCharges.flatMap(_.pensionContributions).flatMap(_.moneyPurchasedAllowance),
      taperedAnnualAllowance = prior.pensionCharges.flatMap(_.pensionContributions).flatMap(_.taperedAnnualAllowance),
      aboveAnnualAllowanceQuestion = prior.pensionCharges.flatMap(_.pensionContributions).map(_.annualAllowanceTaxPaid > 0),
      aboveAnnualAllowance = prior.pensionCharges.flatMap(_.pensionContributions).map(_.inExcessOfTheAnnualAllowance),
      pensionProvidePaidAnnualAllowanceQuestion = prior.pensionCharges.flatMap(_.pensionContributions).map(_.annualAllowanceTaxPaid > 0),
      taxPaidByPensionProvider = prior.pensionCharges.flatMap(_.pensionContributions).map(_.annualAllowanceTaxPaid),
      pensionSchemeTaxReferences = prior.pensionCharges.flatMap(_.pensionContributions).map(_.pensionSchemeTaxReference)
    )

  private def generateIncomeFromPensionsModelFromPrior(prior: AllPensionsData): IncomeFromPensionsViewModel = {
    val (statePen, statePenLumpSum)   = populateStatePensionSessionFromPrior(prior)
    val (uKPenIncomesQ, uKPenIncomes) = generateUkPensionSessionFromPrior(prior)

    IncomeFromPensionsViewModel(
      statePension = statePen,
      statePensionLumpSum = statePenLumpSum,

      // TODO: set the question below based on the list from backend
      uKPensionIncomesQuestion = uKPenIncomesQ,
      uKPensionIncomes = uKPenIncomes
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
              endDateQuestion = Some(benefit.endDate.isDefined),
              endDate = benefit.endDate,
              submittedOnQuestion = Some(benefit.submittedOn.isDefined),
              submittedOn = benefit.submittedOn,
              dateIgnoredQuestion = Some(benefit.dateIgnored.isDefined),
              dateIgnored = benefit.dateIgnored,
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

  def generateUkPensionSessionFromPrior(prior: AllPensionsData): (Option[Boolean], Seq[UkPensionIncomeViewModel]) = {
    def getUkPensionIncome(prior: AllPensionsData): Seq[UkPensionIncomeViewModel] =
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
        case _ => Seq()
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

  private def generatePaymentsIntoOverseasPensionsFromPrior(prior: AllPensionsData): PaymentsIntoOverseasPensionsViewModel = {
    def getTaxReliefQuestion(overseasPensionContribution: OverseasPensionContribution): String =
      if (overseasPensionContribution.sf74Reference.isDefined) {
        TaxReliefQuestion.TransitionalCorrespondingRelief
      } else if (overseasPensionContribution.dblTaxationCountry.isDefined) {
        TaxReliefQuestion.DoubleTaxationRelief
      } else if (overseasPensionContribution.migrantMemReliefQopsRefNo.isDefined) {
        TaxReliefQuestion.MigrantMemberRelief
      } else {
        TaxReliefQuestion.NoTaxRelief
      }

    PaymentsIntoOverseasPensionsViewModel(
      paymentsIntoOverseasPensionsQuestions = prior.pensionReliefs.flatMap(_.pensionReliefs.overseasPensionSchemeContributions.map(_ != zero)),
      paymentsIntoOverseasPensionsAmount = prior.pensionReliefs.flatMap(_.pensionReliefs.overseasPensionSchemeContributions),
      // Cannot see a way to gather the right data to make the below Some(true) (i.e. Yes on the UI) if they answer Yes.
      // Atm, always Some(false) (i.e. `No` on the UI) unless they claim for reliefs (say `No` to the follow up question).
      employerPaymentsQuestion = prior.pensionIncome.flatMap(_.overseasPensionContribution).flatMap(_.headOption.map(!_.isBlankSubmission)),
      taxPaidOnEmployerPaymentsQuestion = prior.pensionIncome.flatMap(_.overseasPensionContribution).map(opcs => opcs.forall(_.isBlankSubmission)),
      reliefs = prior.pensionIncome
        .flatMap(_.overseasPensionContribution.map(_.map(oPC =>
          Relief(
            customerReference = oPC.customerReference,
            employerPaymentsAmount = Some(oPC.exemptEmployersPensionContribs),
            reliefType = Some(getTaxReliefQuestion(oPC)),
            qopsReference = oPC.migrantMemReliefQopsRefNo,
            alphaTwoCountryCode = Countries.get2AlphaCodeFrom3AlphaCode(oPC.dblTaxationCountry),
            alphaThreeCountryCode = oPC.dblTaxationCountry,
            doubleTaxationArticle = oPC.dblTaxationArticle,
            doubleTaxationTreaty = oPC.dblTaxationTreaty,
            doubleTaxationReliefAmount = oPC.dblTaxationRelief,
            sf74Reference = oPC.sf74Reference
          ))))
        .getOrElse(Nil)
    )
  }

  def generateIncomeFromOverseasPensionsCyaFromPrior(prior: AllPensionsData): IncomeFromOverseasPensionsViewModel =
    IncomeFromOverseasPensionsViewModel(
      paymentsFromOverseasPensionsQuestion = prior.pensionIncome.flatMap(_.foreignPension.map(_.nonEmpty)),
      overseasIncomePensionSchemes = prior.pensionIncome
        .flatMap(_.foreignPension.map(_.map(fP =>
          PensionScheme(
            alphaThreeCode = Some(fP.countryCode),
            alphaTwoCode = Countries.get2AlphaCodeFrom3AlphaCode(Some(fP.countryCode)),
            pensionPaymentAmount = fP.amountBeforeTax,
            pensionPaymentTaxPaid = fP.taxTakenOff,
            specialWithholdingTaxQuestion = Some(fP.specialWithholdingTax.isDefined),
            specialWithholdingTaxAmount = fP.specialWithholdingTax,
            foreignTaxCreditReliefQuestion = fP.foreignTaxCreditRelief,
            taxableAmount = Some(fP.taxableAmount)
          ))))
        .getOrElse(Nil)
    )

  private def generateTransfersIntoOverseasPensionsCyaFromPrior(prior: AllPensionsData): TransfersIntoOverseasPensionsViewModel =
    TransfersIntoOverseasPensionsViewModel(
      transferPensionSavings = prior.pensionCharges.map(_.pensionSchemeOverseasTransfers.map(_.transferCharge).isDefined),
      overseasTransferCharge = prior.pensionCharges.map(_.pensionSchemeOverseasTransfers.map(_.transferCharge).isDefined),
      overseasTransferChargeAmount = prior.pensionCharges.flatMap(_.pensionSchemeOverseasTransfers.map(_.transferCharge)),
      pensionSchemeTransferCharge = prior.pensionCharges.map(_.pensionSchemeOverseasTransfers.map(_.transferChargeTaxPaid).isDefined),
      pensionSchemeTransferChargeAmount = prior.pensionCharges.flatMap(_.pensionSchemeOverseasTransfers.map(_.transferChargeTaxPaid)),
      transferPensionScheme = prior.pensionCharges
        .flatMap(_.pensionSchemeOverseasTransfers.map(_.overseasSchemeProvider.map(osp =>
          TransferPensionScheme(
            ukTransferCharge = Some(osp.providerCountryCode == "GBR"),
            name = Some(osp.providerName),
            pstr = osp.pensionSchemeTaxReference.map(_.head).map(_.replace("Q", "")),
            qops = osp.qualifyingRecognisedOverseasPensionScheme.map(_.head),
            providerAddress = Some(osp.providerAddress),
            alphaTwoCountryCode = Countries.get2AlphaCodeFrom3AlphaCode(Some(osp.providerCountryCode)),
            alphaThreeCountryCode = Some(osp.providerCountryCode)
          ))))
        .getOrElse(Nil)
    )

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
