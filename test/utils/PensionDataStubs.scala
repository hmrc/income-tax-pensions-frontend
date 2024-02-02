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

import builders.EmploymentPensionsBuilder.anEmploymentPensions
import builders.PensionIncomeViewModelBuilder.aPensionIncome
import builders.StateBenefitsModelBuilder.aStateBenefitsModel
import models.pension.AllPensionsData
import models.pension.charges._
import models.pension.reliefs.{PensionReliefs, Reliefs}

object PensionDataStubs {

  val fullPensionsModel: AllPensionsData = AllPensionsData(
    pensionReliefs = Some(
      PensionReliefs(
        submittedOn = "2020-01-04T05:01:01Z",
        deletedOn = Some("2020-01-04T05:01:01Z"),
        pensionReliefs = Reliefs(
          regularPensionContributions = Some(100.01),
          oneOffPensionContributionsPaid = Some(100.01),
          retirementAnnuityPayments = Some(100.01),
          paymentToEmployersSchemeNoTaxRelief = Some(100.01),
          overseasPensionSchemeContributions = Some(100.01)
        )
      )),
    pensionCharges = Some(
      PensionCharges(
        submittedOn = "2020-07-27T17:00:19Z",
        pensionSavingsTaxCharges = Some(PensionSavingsTaxCharges(
          pensionSchemeTaxReference = Some(Seq("00123456RA", "00123456RB")),
          lumpSumBenefitTakenInExcessOfLifetimeAllowance = Some(LifetimeAllowance(
            amount = Some(800.02),
            taxPaid = Some(200.02)
          )),
          benefitInExcessOfLifetimeAllowance = Some(LifetimeAllowance(
            amount = Some(800.02),
            taxPaid = Some(200.02)
          ))
        )),
        pensionSchemeOverseasTransfers = Some(PensionSchemeOverseasTransfers(
          overseasSchemeProvider = Seq(OverseasSchemeProvider(
            providerName = "overseas providerName 1 qualifying scheme",
            providerAddress = "overseas address 1",
            providerCountryCode = "ESP",
            qualifyingRecognisedOverseasPensionScheme = Some(Seq("Q100000", "Q100002")),
            pensionSchemeTaxReference = None
          )),
          transferCharge = 22.77,
          transferChargeTaxPaid = 33.88
        )),
        pensionSchemeUnauthorisedPayments = Some(PensionSchemeUnauthorisedPayments(
          pensionSchemeTaxReference = Some(Seq("00123456RA", "00123456RB")),
          surcharge = Some(Charge(
            amount = 124.44,
            foreignTaxPaid = 123.33
          )),
          noSurcharge = Some(Charge(
            amount = 222.44,
            foreignTaxPaid = 223.33
          ))
        )),
        pensionContributions = Some(PensionContributions(
          pensionSchemeTaxReference = Seq("00123456RA", "00123456RB"),
          inExcessOfTheAnnualAllowance = 150.67,
          annualAllowanceTaxPaid = 178.65,
          isAnnualAllowanceReduced = Some(false),
          taperedAnnualAllowance = Some(false),
          moneyPurchasedAllowance = Some(false)
        )),
        overseasPensionContributions = Some(OverseasPensionContributions(
          overseasSchemeProvider = Seq(OverseasSchemeProvider(
            providerName = "overseas providerName 1 tax ref",
            providerAddress = "overseas address 1",
            providerCountryCode = "ESP",
            qualifyingRecognisedOverseasPensionScheme = None,
            pensionSchemeTaxReference = Some(Seq("00123456RA", "00123456RB"))
          )),
          shortServiceRefund = 1.11,
          shortServiceRefundTaxPaid = 2.22
        ))
      )),
    stateBenefits = Some(aStateBenefitsModel),
    employmentPensions = Some(anEmploymentPensions),
    pensionIncome = Some(aPensionIncome)
  )

}
