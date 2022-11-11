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

package models.pension

import models.mongo.PensionsCYAModel
import models.pension.charges.{IncomeFromOverseasPensionsViewModel, LifetimeAllowance, PaymentsIntoOverseasPensionsViewModel, PensionAnnualAllowancesViewModel, PensionCharges, PensionLifetimeAllowancesViewModel, PensionScheme, TaxReliefQuestion, UnauthorisedPaymentsViewModel}
import models.pension.employmentPensions.EmploymentPensions
import models.pension.reliefs.{PaymentsIntoPensionViewModel, PensionReliefs}
import models.pension.statebenefits.{IncomeFromPensionsViewModel, StateBenefit, StateBenefitViewModel, StateBenefitsModel, UkPensionIncomeViewModel}
import models.pension.income.{ForeignPension, OverseasPensionContribution, PensionIncome}
import play.api.libs.json.{Json, OFormat}

case class AllPensionsData(pensionReliefs: Option[PensionReliefs],
                           pensionCharges: Option[PensionCharges],
                           stateBenefits: Option[StateBenefitsModel],
                           employmentPensions: Option[EmploymentPensions],
                           pensionIncome: Option[PensionIncome]
                          )

object AllPensionsData {
  implicit val formats: OFormat[AllPensionsData] = Json.format[AllPensionsData]

  def generateCyaFromPrior(prior: AllPensionsData): PensionsCYAModel = {

    val statePension: Option[StateBenefit] = prior.stateBenefits.flatMap(_.stateBenefits.flatMap(_.statePension))
    val statePensionLumpSum: Option[StateBenefit] = prior.stateBenefits.flatMap(_.stateBenefits.flatMap(_.statePensionLumpSum))

    PensionsCYAModel(
      PaymentsIntoPensionViewModel(
        Some(true),
        rasPensionPaymentQuestion = prior.pensionReliefs.map(a => a.pensionReliefs.regularPensionContributions.isDefined),
        prior.pensionReliefs.flatMap(a => a.pensionReliefs.regularPensionContributions),
        prior.pensionReliefs.map(a => a.pensionReliefs.oneOffPensionContributionsPaid.isDefined),
        prior.pensionReliefs.flatMap(a => a.pensionReliefs.oneOffPensionContributionsPaid),
        Some(true),
        pensionTaxReliefNotClaimedQuestion = prior.pensionReliefs.map(a =>
          a.pensionReliefs.retirementAnnuityPayments.isDefined || a.pensionReliefs.paymentToEmployersSchemeNoTaxRelief.isDefined
        ),
        prior.pensionReliefs.map(a => a.pensionReliefs.retirementAnnuityPayments.isDefined),
        prior.pensionReliefs.flatMap(a => a.pensionReliefs.retirementAnnuityPayments),
        prior.pensionReliefs.map(a => a.pensionReliefs.paymentToEmployersSchemeNoTaxRelief.isDefined),
        prior.pensionReliefs.flatMap(a => a.pensionReliefs.paymentToEmployersSchemeNoTaxRelief)
      ),

      PensionAnnualAllowancesViewModel(
        reducedAnnualAllowanceQuestion = prior.pensionCharges.flatMap(a => a.pensionSavingsTaxCharges).map(_.isAnnualAllowanceReduced),
        moneyPurchaseAnnualAllowance = prior.pensionCharges.flatMap(a => a.pensionSavingsTaxCharges).flatMap(_.moneyPurchasedAllowance),
        taperedAnnualAllowance = prior.pensionCharges.flatMap(a => a.pensionSavingsTaxCharges).flatMap(_.taperedAnnualAllowance),
        aboveAnnualAllowanceQuestion = prior.pensionCharges.map(a => a.pensionContributions.isDefined),
        aboveAnnualAllowance = prior.pensionCharges.flatMap(a => a.pensionContributions).map(_.inExcessOfTheAnnualAllowance),
        pensionProvidePaidAnnualAllowanceQuestion = prior.pensionCharges.flatMap(a => a.pensionContributions.map(x => x.annualAllowanceTaxPaid)) match {
          case Some(taxVal) if taxVal > 0 => Some(true)
          case _ => Some(false)
        },
        taxPaidByPensionProvider = prior.pensionCharges.flatMap(a => a.pensionContributions).map(_.annualAllowanceTaxPaid),
        pensionSchemeTaxReferences = prior.pensionCharges.flatMap(a => a.pensionContributions).map(_.pensionSchemeTaxReference)
      ),

      pensionLifetimeAllowances = PensionLifetimeAllowancesViewModel(
        aboveLifetimeAllowanceQuestion = getAboveLifetimeAllowanceQuestion(prior),
        pensionAsLumpSumQuestion = prior.pensionCharges.flatMap(_.pensionSavingsTaxCharges).map(_.lumpSumBenefitTakenInExcessOfLifetimeAllowance.isDefined),
        pensionAsLumpSum = prior.pensionCharges.flatMap(_.pensionSavingsTaxCharges).flatMap(_.lumpSumBenefitTakenInExcessOfLifetimeAllowance),
        pensionPaidAnotherWayQuestion = prior.pensionCharges.flatMap(_.pensionSavingsTaxCharges).map(_.benefitInExcessOfLifetimeAllowance.isDefined),
        pensionPaidAnotherWay = prior.pensionCharges.flatMap(_.pensionSavingsTaxCharges).flatMap(_.benefitInExcessOfLifetimeAllowance).getOrElse(LifetimeAllowance()),
        pensionSchemeTaxReferences = prior.pensionCharges.flatMap(_.pensionSavingsTaxCharges.map(_.pensionSchemeTaxReference))
      ),

      //TODO: validate as necessary on building CYA page
      incomeFromPensions = IncomeFromPensionsViewModel(
        statePension = getStatePensionModel(statePension),
        statePensionLumpSum = getStatePensionModel(statePensionLumpSum),
        //TODO: set the question below based on the list from backend
        uKPensionIncomesQuestion = Some(getUkPensionIncome(prior).nonEmpty),
        uKPensionIncomes = getUkPensionIncome(prior)
      ),

      unauthorisedPayments = UnauthorisedPaymentsViewModel(
        surchargeQuestion = prior.pensionCharges.map(_.pensionSchemeUnauthorisedPayments.flatMap(_.surcharge).isDefined),
        noSurchargeQuestion = prior.pensionCharges.map(_.pensionSchemeUnauthorisedPayments.flatMap(_.noSurcharge).isDefined),
        surchargeAmount = prior.pensionCharges.flatMap(_.pensionSchemeUnauthorisedPayments.flatMap(_.surcharge.map(_.amount))),
        surchargeTaxAmountQuestion = prior.pensionCharges.map(_.pensionSchemeUnauthorisedPayments.flatMap(_.surcharge.map(_.foreignTaxPaid)).isDefined),
        surchargeTaxAmount = prior.pensionCharges.flatMap(_.pensionSchemeUnauthorisedPayments.flatMap(_.surcharge.map(_.foreignTaxPaid))),
        noSurchargeAmount = prior.pensionCharges.flatMap(_.pensionSchemeUnauthorisedPayments.flatMap(_.noSurcharge.map(_.amount))),
        noSurchargeTaxAmountQuestion = prior.pensionCharges.map(_.pensionSchemeUnauthorisedPayments.map(_.noSurcharge.map(_.foreignTaxPaid)).isDefined),
        noSurchargeTaxAmount = prior.pensionCharges.flatMap(_.pensionSchemeUnauthorisedPayments.flatMap(_.noSurcharge.map(_.foreignTaxPaid))),
        ukPensionSchemesQuestion = prior.pensionCharges.map(_.pensionSchemeUnauthorisedPayments.map(_.pensionSchemeTaxReference).isDefined),
        pensionSchemeTaxReference = prior.pensionCharges.flatMap(_.pensionSchemeUnauthorisedPayments.map(_.pensionSchemeTaxReference))
      ),

    paymentsIntoOverseasPensions = PaymentsIntoOverseasPensionsViewModel(
      paymentsIntoOverseasPensionsQuestions = prior.pensionReliefs.map(_.pensionReliefs.overseasPensionSchemeContributions.isDefined),
      paymentsIntoOverseasPensionsAmount = prior.pensionReliefs.flatMap(_.pensionReliefs.overseasPensionSchemeContributions),
      employerPaymentsQuestion = prior.pensionIncome.map(_.overseasPensionContribution.headOption.flatMap(_.customerReference).isDefined),
      taxPaidOnEmployerPaymentsQuestion = prior.pensionIncome.map(_.overseasPensionContribution.headOption.flatMap(_.customerReference).isEmpty),
      customerReferenceNumberQuestion = prior.pensionIncome.flatMap(_.overseasPensionContribution.headOption.flatMap(_.customerReference)),
      employerPaymentsAmount = prior.pensionIncome.flatMap(_.overseasPensionContribution.headOption.map(_.exemptEmployersPensionContribs)),
      taxReliefQuestion = prior.pensionIncome.flatMap(_.overseasPensionContribution.headOption.map(getTaxReliefQuestion)),
      qualifyingOverseasPensionSchemeReferenceNumber = prior.pensionIncome.flatMap(_.overseasPensionContribution.headOption.flatMap(_.migrantMemReliefQopsRefNo)),
      doubleTaxationCountryCode = prior.pensionIncome.flatMap(_.overseasPensionContribution.headOption.flatMap(_.dblTaxationCountry)),
      doubleTaxationCountryArticle = prior.pensionIncome.flatMap(_.overseasPensionContribution.headOption.flatMap(_.dblTaxationArticle)),
      doubleTaxationCountryTreaty = prior.pensionIncome.flatMap(_.overseasPensionContribution.headOption.flatMap(_.dblTaxationTreaty)),
      doubleTaxationReliefAmount = prior.pensionIncome.flatMap(_.overseasPensionContribution.headOption.flatMap(_.dblTaxationRelief)),
      sf74Reference = prior.pensionIncome.flatMap(_.overseasPensionContribution.headOption.flatMap(_.sf74Reference))
    ),
      incomeFromOverseasPensionsViewModel = IncomeFromOverseasPensionsViewModel(
        paymentsFromOverseasPensionsQuestion = prior.pensionIncome.map(_.foreignPension.nonEmpty),
        pensionSchemes = prior.pensionIncome.map(x => fromForeignPensionToPensionScheme(x.foreignPension))
      )
    )
  }

  private def fromForeignPensionToPensionScheme(foreignPension: Seq[ForeignPension]) = {
    foreignPension.map( fP =>
      PensionScheme(
        countryCode = Some(fP.countryCode),
        pensionPaymentAmount = fP.amountBeforeTax,
        pensionPaymentTaxPaid = fP.taxTakenOff,
        specialWithholdingTaxQuestion = Some(fP.specialWithholdingTax.isDefined),
        specialWithholdingTaxAmount = fP.specialWithholdingTax,
        foreignTaxCreditReliefQuestion = fP.foreignTaxCreditRelief,
        taxableAmount = Some(fP.taxableAmount)
      )
    )
  }

  private def getTaxReliefQuestion(overseasPensionContribution: OverseasPensionContribution): String = {
    if (overseasPensionContribution.sf74Reference.isDefined) {
      TaxReliefQuestion.TransitionalCorrespondingRelief
    } else if (overseasPensionContribution.dblTaxationCountry.isDefined) {
      TaxReliefQuestion.DoubleTaxationRelief
    } else if (overseasPensionContribution.migrantMemReliefQopsRefNo.isDefined) {
      TaxReliefQuestion.MigrantMemberRelief
    } else TaxReliefQuestion.NoTaxRelief
  }


  private def getStatePensionModel(statePension: Option[StateBenefit]): Option[StateBenefitViewModel] = {
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

  private def getAboveLifetimeAllowanceQuestion(prior: AllPensionsData): Option[Boolean] = {
    if (prior.pensionCharges.flatMap(_.pensionSavingsTaxCharges).map(
      _.benefitInExcessOfLifetimeAllowance).isDefined || prior.pensionCharges.flatMap(
      a => a.pensionSavingsTaxCharges).map(_.lumpSumBenefitTakenInExcessOfLifetimeAllowance).isDefined) {
      Some(true)
    } else {
      None
    }
  }

  private def getUkPensionIncome(prior: AllPensionsData): Seq[UkPensionIncomeViewModel] = {
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
          )
        )
      case _ => Seq()
    }
  }

}
