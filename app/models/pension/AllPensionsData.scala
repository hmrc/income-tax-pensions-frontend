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

import forms.Countries
import models.mongo.PensionsCYAModel
import models.pension.charges._
import models.pension.employmentPensions.EmploymentPensions
import models.pension.income.{OverseasPensionContribution, PensionIncome}
import models.pension.reliefs.{PaymentsIntoPensionsViewModel, PensionReliefs}
import models.pension.statebenefits._
import play.api.libs.json.{Json, OFormat}

case class AllPensionsData(pensionReliefs: Option[PensionReliefs],
                           pensionCharges: Option[PensionCharges],
                           stateBenefits: Option[AllStateBenefitsData],
                           employmentPensions: Option[EmploymentPensions],
                           pensionIncome: Option[PensionIncome]
                          )

object AllPensionsData {
  implicit val formats: OFormat[AllPensionsData] = Json.format[AllPensionsData]

  def generateCyaFromPrior(prior: AllPensionsData): PensionsCYAModel = { //scalastyle:off method.length
    PensionsCYAModel(
      paymentsIntoPension = generatePaymentsIntoPensionsCyaFromPrior(prior),
      pensionsAnnualAllowances = generateAnnualAllowanceCyaFromPrior(prior),
      pensionLifetimeAllowances = generateLifetimeAllowanceCyaFromPrior(prior),
      incomeFromPensions = generateIncomeFromPensionsModelFromPrior(prior),
      unauthorisedPayments = generateUnauthorisedPaymentsCysFromPrior(prior),
      paymentsIntoOverseasPensions = generatePaymentsIntoOverseasPensionsFromPrior(prior),
      incomeFromOverseasPensions = generateIncomeFromOverseasPensionsCyaFromPrior(prior),
      transfersIntoOverseasPensions = generateTransfersIntoOverseasPensionsCyaFromPrior(prior),
      shortServiceRefunds = generateShortServiceRefundCyaFromPrior(prior)
    )
  }
  
  def generatePaymentsIntoPensionsCyaFromPrior(prior: AllPensionsData): PaymentsIntoPensionsViewModel =
    PaymentsIntoPensionsViewModel(
      rasPensionPaymentQuestion = prior.pensionReliefs.map(_.pensionReliefs.regularPensionContributions.isDefined),
      totalRASPaymentsAndTaxRelief = prior.pensionReliefs.flatMap(_.pensionReliefs.regularPensionContributions),
      oneOffRasPaymentPlusTaxReliefQuestion = prior.pensionReliefs.map(_.pensionReliefs.oneOffPensionContributionsPaid.isDefined),
      totalOneOffRasPaymentPlusTaxRelief = prior.pensionReliefs.flatMap(_.pensionReliefs.oneOffPensionContributionsPaid),
      Some(true),
      pensionTaxReliefNotClaimedQuestion = prior.pensionReliefs.map(a =>
        a.pensionReliefs.retirementAnnuityPayments.isDefined || a.pensionReliefs.paymentToEmployersSchemeNoTaxRelief.isDefined
      ),
      retirementAnnuityContractPaymentsQuestion = prior.pensionReliefs.map(_.pensionReliefs.retirementAnnuityPayments.isDefined),
      totalRetirementAnnuityContractPayments = prior.pensionReliefs.flatMap(_.pensionReliefs.retirementAnnuityPayments),
      workplacePensionPaymentsQuestion = prior.pensionReliefs.map(_.pensionReliefs.paymentToEmployersSchemeNoTaxRelief.isDefined),
      totalWorkplacePensionPayments = prior.pensionReliefs.flatMap(_.pensionReliefs.paymentToEmployersSchemeNoTaxRelief)
    )

  def generateAnnualAllowanceCyaFromPrior(prior: AllPensionsData): PensionAnnualAllowancesViewModel =
    PensionAnnualAllowancesViewModel(
      reducedAnnualAllowanceQuestion = prior.pensionCharges.flatMap(_.pensionSavingsTaxCharges).flatMap(_.isAnnualAllowanceReduced),
      moneyPurchaseAnnualAllowance = prior.pensionCharges.flatMap(_.pensionSavingsTaxCharges).flatMap(_.moneyPurchasedAllowance),
      taperedAnnualAllowance = prior.pensionCharges.flatMap(_.pensionSavingsTaxCharges).flatMap(_.taperedAnnualAllowance),
      aboveAnnualAllowanceQuestion = prior.pensionCharges.flatMap(_.pensionContributions).map(_.annualAllowanceTaxPaid > 0),
      aboveAnnualAllowance = prior.pensionCharges.flatMap(_.pensionContributions).map(_.inExcessOfTheAnnualAllowance),
      pensionProvidePaidAnnualAllowanceQuestion = prior.pensionCharges.flatMap(_.pensionContributions).map(_.annualAllowanceTaxPaid > 0),
      taxPaidByPensionProvider = prior.pensionCharges.flatMap(_.pensionContributions).map(_.annualAllowanceTaxPaid),
      pensionSchemeTaxReferences = prior.pensionCharges.flatMap(_.pensionContributions).map(_.pensionSchemeTaxReference)
    )

  def generateLifetimeAllowanceCyaFromPrior(prior: AllPensionsData): PensionLifetimeAllowancesViewModel = {
    def getAboveLifetimeAllowanceQuestion(prior: AllPensionsData): Option[Boolean] = {
      if (prior.pensionCharges.flatMap(_.pensionSavingsTaxCharges).map(
        _.benefitInExcessOfLifetimeAllowance).isDefined || prior.pensionCharges.flatMap(
        _.pensionSavingsTaxCharges).map(_.lumpSumBenefitTakenInExcessOfLifetimeAllowance).isDefined) {
        Some(true)
      } else {
        None
      }
    }
    
    PensionLifetimeAllowancesViewModel(
      aboveLifetimeAllowanceQuestion = getAboveLifetimeAllowanceQuestion(prior),
      pensionAsLumpSumQuestion = prior.pensionCharges.flatMap(_.pensionSavingsTaxCharges)
        .flatMap(_.lumpSumBenefitTakenInExcessOfLifetimeAllowance).flatMap(_.amount).map(_ > 0),
      pensionAsLumpSum = prior.pensionCharges.flatMap(_.pensionSavingsTaxCharges)
        .flatMap(_.lumpSumBenefitTakenInExcessOfLifetimeAllowance),
      pensionPaidAnotherWayQuestion = prior.pensionCharges.flatMap(_.pensionSavingsTaxCharges)
        .flatMap(_.benefitInExcessOfLifetimeAllowance).flatMap(_.amount).map(_ > 0),
      pensionPaidAnotherWay = prior.pensionCharges.flatMap(_.pensionSavingsTaxCharges).flatMap(_.benefitInExcessOfLifetimeAllowance),
      pensionSchemeTaxReferences = prior.pensionCharges.flatMap(_.pensionSavingsTaxCharges.flatMap(_.pensionSchemeTaxReference))
    )
  }

  def generateIncomeFromPensionsModelFromPrior(prior: AllPensionsData): IncomeFromPensionsViewModel = {
    val (statePen, statePenLumpSum) = generateStatePensionCyaFromPrior(prior)
    val (uKPenIncomesQ, uKPenIncomes) = generateUkPensionCyaFromPrior(prior)

    IncomeFromPensionsViewModel(
      //TODO: validate as necessary on building CYA page
      statePension = statePen,
      statePensionLumpSum = statePenLumpSum,

      //TODO: set the question below based on the list from backend
      uKPensionIncomesQuestion = uKPenIncomesQ,
      uKPensionIncomes = uKPenIncomes
    )
  }

  def generateStatePensionCyaFromPrior(prior: AllPensionsData): (Option[StateBenefitViewModel], Option[StateBenefitViewModel]) = {
    def getStatePensionModel(statePension: Option[StateBenefit]): Option[StateBenefitViewModel] = {
      statePension match {
        case Some(benefit) => Some(StateBenefitViewModel(
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
          taxPaid = benefit.taxPaid)
        )
        case _ => None
      }
    }
    (
      getStatePensionModel(prior.stateBenefits.flatMap(_.stateBenefitsData.flatMap(_.statePension))),
      getStatePensionModel(prior.stateBenefits.flatMap(_.stateBenefitsData.flatMap(_.statePensionLumpSum)))
    )
  }

  def generateUkPensionCyaFromPrior(prior: AllPensionsData): (Option[Boolean], Seq[UkPensionIncomeViewModel]) = {
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

  def generateUnauthorisedPaymentsCysFromPrior(prior: AllPensionsData): UnauthorisedPaymentsViewModel =
    UnauthorisedPaymentsViewModel(
      surchargeQuestion = prior.pensionCharges.flatMap(_.pensionSchemeUnauthorisedPayments.flatMap(_.surcharge).map(_.amount > 0)),
      noSurchargeQuestion = prior.pensionCharges.flatMap(_.pensionSchemeUnauthorisedPayments.flatMap(_.noSurcharge).map(_ => true)),
      surchargeAmount = prior.pensionCharges.flatMap(_.pensionSchemeUnauthorisedPayments.flatMap(_.surcharge.map(_.amount))),
      surchargeTaxAmountQuestion = prior.pensionCharges.flatMap(_.pensionSchemeUnauthorisedPayments.flatMap(_.surcharge).map(_.foreignTaxPaid > 0)),
      surchargeTaxAmount = prior.pensionCharges.flatMap(_.pensionSchemeUnauthorisedPayments.flatMap(_.surcharge.map(_.foreignTaxPaid))),
      noSurchargeAmount = prior.pensionCharges.flatMap(_.pensionSchemeUnauthorisedPayments.flatMap(_.noSurcharge.map(_.amount))),
      noSurchargeTaxAmountQuestion = prior.pensionCharges.flatMap(_.pensionSchemeUnauthorisedPayments.flatMap(_.noSurcharge).map(_ => true)),
      noSurchargeTaxAmount = prior.pensionCharges.flatMap(_.pensionSchemeUnauthorisedPayments.flatMap(_.noSurcharge.map(_.foreignTaxPaid))),
      ukPensionSchemesQuestion = prior.pensionCharges.flatMap(_.pensionSchemeUnauthorisedPayments).map(_.pensionSchemeTaxReference).map(_.nonEmpty),
      pensionSchemeTaxReference = prior.pensionCharges.flatMap(_.pensionSchemeUnauthorisedPayments.flatMap(_.pensionSchemeTaxReference))
    )

  def generatePaymentsIntoOverseasPensionsFromPrior(prior: AllPensionsData): PaymentsIntoOverseasPensionsViewModel = {    
    def getTaxReliefQuestion(overseasPensionContribution: OverseasPensionContribution): String = {
      if (overseasPensionContribution.sf74Reference.isDefined) {
        TaxReliefQuestion.TransitionalCorrespondingRelief
      } else if (overseasPensionContribution.dblTaxationCountry.isDefined) {
        TaxReliefQuestion.DoubleTaxationRelief
      } else if (overseasPensionContribution.migrantMemReliefQopsRefNo.isDefined) {
        TaxReliefQuestion.MigrantMemberRelief
      } else {
        TaxReliefQuestion.NoTaxRelief
      }
    }
    PaymentsIntoOverseasPensionsViewModel(
      paymentsIntoOverseasPensionsQuestions = prior.pensionReliefs.map(_.pensionReliefs.overseasPensionSchemeContributions.isDefined),
      paymentsIntoOverseasPensionsAmount = prior.pensionReliefs.flatMap(_.pensionReliefs.overseasPensionSchemeContributions),
      employerPaymentsQuestion = prior.pensionIncome.flatMap(_.overseasPensionContribution).flatMap(_.headOption.map(_.customerReference.isDefined)),
      taxPaidOnEmployerPaymentsQuestion = prior.pensionIncome.flatMap(_.overseasPensionContribution).flatMap(_.headOption.map(_.customerReference.isEmpty)),
      reliefs = prior.pensionIncome.flatMap(_.overseasPensionContribution.map(_.map(oPC =>
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
        )
      ))).getOrElse(Nil)
    )
  }

  def generateIncomeFromOverseasPensionsCyaFromPrior(prior: AllPensionsData): IncomeFromOverseasPensionsViewModel =
    IncomeFromOverseasPensionsViewModel(
      paymentsFromOverseasPensionsQuestion = prior.pensionIncome.flatMap(_.foreignPension.map(_.nonEmpty)),
      overseasIncomePensionSchemes = prior.pensionIncome.flatMap(_.foreignPension.map(_.map(fP =>
        PensionScheme(
          alphaThreeCode = Some(fP.countryCode),
          alphaTwoCode = Countries.get2AlphaCodeFrom3AlphaCode(Some(fP.countryCode)),
          pensionPaymentAmount = fP.amountBeforeTax,
          pensionPaymentTaxPaid = fP.taxTakenOff,
          specialWithholdingTaxQuestion = Some(fP.specialWithholdingTax.isDefined),
          specialWithholdingTaxAmount = fP.specialWithholdingTax,
          foreignTaxCreditReliefQuestion = fP.foreignTaxCreditRelief,
          taxableAmount = Some(fP.taxableAmount)
        )
      ))).getOrElse(Nil)
    )

  def generateTransfersIntoOverseasPensionsCyaFromPrior(prior: AllPensionsData): TransfersIntoOverseasPensionsViewModel =
    TransfersIntoOverseasPensionsViewModel(
      transferPensionSavings = prior.pensionCharges.map(_.pensionSchemeOverseasTransfers.map(_.transferCharge).isDefined),
      overseasTransferCharge = prior.pensionCharges.map(_.pensionSchemeOverseasTransfers.map(_.transferCharge).isDefined),
      overseasTransferChargeAmount = prior.pensionCharges.flatMap(_.pensionSchemeOverseasTransfers.map(_.transferCharge)),
      pensionSchemeTransferCharge = prior.pensionCharges.map(_.pensionSchemeOverseasTransfers.map(_.transferChargeTaxPaid).isDefined),
      pensionSchemeTransferChargeAmount = prior.pensionCharges.flatMap(_.pensionSchemeOverseasTransfers.map(_.transferChargeTaxPaid)),
      transferPensionScheme = prior.pensionCharges.flatMap(_.pensionSchemeOverseasTransfers.map( _.overseasSchemeProvider.map(osp =>
        TransferPensionScheme(
          ukTransferCharge = Some(osp.providerCountryCode == "GBR"),
          name = Some(osp.providerName),
          pstr = osp.pensionSchemeTaxReference.map(_.head).map(_.replace("Q", "")),
          qops = osp.qualifyingRecognisedOverseasPensionScheme.map(_.head),
          providerAddress = Some(osp.providerAddress),
          alphaTwoCountryCode = Countries.get2AlphaCodeFrom3AlphaCode(Some(osp.providerCountryCode)),
          alphaThreeCountryCode = Some(osp.providerCountryCode)
        )
      ))).getOrElse(Nil)
    )
  
  def generateShortServiceRefundCyaFromPrior(prior: AllPensionsData): ShortServiceRefundsViewModel =
    ShortServiceRefundsViewModel(
      shortServiceRefund = prior.pensionCharges.map(_.overseasPensionContributions.map(_.shortServiceRefund).isDefined),
      shortServiceRefundCharge = prior.pensionCharges.flatMap(_.overseasPensionContributions.map(_.shortServiceRefund)),
      shortServiceRefundTaxPaid = prior.pensionCharges.map(_.overseasPensionContributions.map(_.shortServiceRefundTaxPaid).isDefined),
      shortServiceRefundTaxPaidCharge = prior.pensionCharges.flatMap(_.overseasPensionContributions.map(_.shortServiceRefundTaxPaid)),
      refundPensionScheme = prior.pensionCharges.flatMap(_.overseasPensionContributions.map(_.overseasSchemeProvider.map(osp =>
        OverseasRefundPensionScheme(
          ukRefundCharge = Some(osp.providerCountryCode == "GBR"),
          name = Some(osp.providerName),
          pensionSchemeTaxReference = osp.pensionSchemeTaxReference.map(_.head),
          qualifyingRecognisedOverseasPensionScheme = osp.qualifyingRecognisedOverseasPensionScheme.map(_.head).map(_.replace("Q", "")),
          providerAddress = Some(osp.providerAddress),
          alphaTwoCountryCode = Countries.get2AlphaCodeFrom3AlphaCode(Some(osp.providerCountryCode)),
          alphaThreeCountryCode = Some(osp.providerCountryCode)
        )
      ))).getOrElse(Nil)
    )
}
