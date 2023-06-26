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

package controllers.pensions.lifetimeAllowances

import builders.AllPensionsDataBuilder.anAllPensionsData
import builders.IncomeFromPensionsViewModelBuilder.anIncomeFromPensionsViewModel
import builders.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import builders.PaymentsIntoOverseasPensionsViewModelBuilder.aPaymentsIntoOverseasPensionsViewModel
import builders.PaymentsIntoPensionVewModelBuilder.aPaymentsIntoPensionViewModel
import builders.PensionAnnualAllowanceViewModelBuilder.aPensionAnnualAllowanceViewModel
import builders.PensionChargesBuilder.anPensionCharges
import builders.PensionContributionsBuilder.anPensionContributions
import builders.PensionLifetimeAllowanceViewModelBuilder.aPensionLifetimeAllowanceViewModel
import builders.PensionSavingTaxChargesBuilder.anPensionSavngTaxCharges
import builders.PensionsCYAModelBuilder.aPensionsCYAModel
import builders.PensionsUserDataBuilder
import builders.PensionsUserDataBuilder.{aPensionsUserData, pensionsUserDataWithAnnualAndLifetimeAllowance}
import builders.UnauthorisedPaymentsViewModelBuilder.anUnauthorisedPaymentsViewModel
import controllers.pensions.lifetimeAllowances.routes._
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.pension.charges.PensionAnnualAllowancesViewModel
import models.pension.reliefs.PaymentsIntoPensionsViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.Logging
import play.api.http.HeaderNames
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.PensionLifetimeAllowance.checkAnnualLifetimeAllowanceCYA
import utils.PageUrls.fullUrl
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}


class AnnualLifetimeAllowanceCYAControllerISpec extends
  IntegrationTest with
  ViewHelpers with
  BeforeAndAfterEach with
  PensionsDatabaseHelper with Logging {

  val cyaDataIncomplete: PaymentsIntoPensionsViewModel = PaymentsIntoPensionsViewModel(
    rasPensionPaymentQuestion = Some(true)
  )

  // TODO: Duplicates UnauthorisedPaymentsCYAControllerISpec. Should we have a single Links object?
  object ChangeLinksUnauthorisedPayments {
    import controllers.pensions.annualAllowances.routes._
    import controllers.pensions.unauthorisedPayments.routes._
    
    val unauthorisedPayments: String = UnauthorisedPaymentsController.show(taxYear).url
    val amountSurcharged: String = SurchargeAmountController.show(taxYear).url
    val nonUKTaxOnAmountResultedInSurcharge: String = NonUKTaxOnAmountResultedInSurchargeController.show(taxYear).url
    val amountNotSurcharged: String = NoSurchargeAmountController.show(taxYear).url
    val nonUKTaxOnAmountNotResultedInSurcharge: String = NonUKTaxOnAmountNotResultedInSurchargeController.show(taxYear).url
    val ukPensionSchemes: String = WereAnyOfTheUnauthorisedPaymentsController.show(taxYear).url
    val pensionSchemeTaxReferences: String = UnauthorisedPensionSchemeTaxReferenceController.show(taxYear, None).url


    val aboveAnnualOrLifetimeAllowance: String = AboveAnnualLifetimeAllowanceController.show(taxYear).url
    val reducedAnnualAllowance: String = ReducedAnnualAllowanceController.show(taxYear).url
    val typeOfReducedAnnualAllowance: String = ReducedAnnualAllowanceTypeController.show(taxYear).url
    val aboveAnnualAllowance: String = AboveReducedAnnualAllowanceController.show(taxYear).url
    val annualAllowanceTax: String = PensionProviderPaidTaxController.show(taxYear).url
    val annualAllowanceSchemes: String = PstrSummaryController.show(taxYear).url
    val aboveLifetimeAllowance: String = AnnualLifetimeAllowanceCYAController.show(taxYear).url
    val lumpSum: String = PensionLumpSumController.show(taxYear).url
    val otherPayments: String = LifeTimeAllowanceAnotherWayController.show(taxYear).url
    val lifetimeAllowanceSchemes: String = AnnualLifetimeAllowanceCYAController.show(taxYear).url
    val lifetimeAllowancePstrSummary: String = LifetimePstrSummaryController.show(taxYear).url

  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    lazy val expectedH1 = expectedTitle
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    
    val yes: String
    val no: String
    
    val aboveAnnualOrLifetimeAllowance: String
    val reducedAnnualAllowance: String
    val typeOfReducedAnnualAllowance: String
    val aboveAnnualAllowance: String
    val annualAllowanceTax: String
    val annualAllowanceSchemes: String
    val aboveLifetimeAllowance: String
    val lumpSum: String
    val otherPayments: String
    val lifetimeAllowanceSchemes: String
    

    val aboveAnnualOrLifetimeAllowanceHidden: String
    val reducedAnnualAllowanceHidden: String
    val typeOfReducedAnnualAllowanceHidden: String
    val aboveAnnualAllowanceHidden: String
    val annualAllowanceTaxHidden: String
    val annualAllowanceSchemesHidden: String
    val aboveLifetimeAllowanceHidden: String
    val lumpSumHidden: String
    val otherPaymentsHidden: String
    val lifetimeAllowanceSchemesHidden: String
    

    val unauthorisedPayments: String
    val amountSurcharged: String
    val nonUkTaxAmountSurcharged: String
    val amountNotSurcharged: String
    val nonUkTaxAmountNotSurcharged: String
    val ukPensionSchemes: String
    val pensionSchemeTaxReferences: String

    val saveAndContinue: String
    val error: String

    val unauthorisedPaymentsHidden: String
    val amountSurchargedHidden: String
    val nonUkTaxAmountSurchargedHidden: String
    val amountNotSurchargedHidden: String
    val nonUkTaxAmountNotSurchargedHidden: String
    val ukPensionSchemesHidden: String
    val pensionSchemeTaxReferencesHidden: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Annual and lifetime allowances for 6 April ${taxYear - 1} to 5 April $taxYear"

    val yes = "Yes"
    val no = "No"

    val unauthorisedPayments = "Unauthorised payments"
    val amountSurcharged = "Amount surcharged"
    val nonUkTaxAmountSurcharged = "Non UK-tax on amount surcharged"
    val amountNotSurcharged = "Amount not surcharged"
    val nonUkTaxAmountNotSurcharged = "Non UK-tax on amount not surcharged"
    val ukPensionSchemes = "UK pension schemes"
    val pensionSchemeTaxReferences = "Pension Scheme Tax References"


    val saveAndContinue = "Save and continue"
    val error = "Sorry, there is a problem with the service"

    val unauthorisedPaymentsHidden = "Change unauthorised payments"
    val amountSurchargedHidden = "Change amount surcharged"
    val nonUkTaxAmountSurchargedHidden = "Change non UK-tax on amount surcharged"
    val amountNotSurchargedHidden = "Change amount not surcharged"
    val nonUkTaxAmountNotSurchargedHidden = "Change non UK-tax on amount not surcharged"
    val ukPensionSchemesHidden = "Change UK pension schemes"
    val pensionSchemeTaxReferencesHidden = "Change Pension Scheme Tax References"


    val aboveAnnualOrLifetimeAllowance: String = "Above annual or lifetime allowance"
    val reducedAnnualAllowance: String = "Reduced annual allowance"
    val typeOfReducedAnnualAllowance: String = "Type of reduced annual allowance"
    val aboveAnnualAllowance: String = "Above annual allowance"
    val annualAllowanceTax: String = "Annual allowance tax"
    val annualAllowanceSchemes: String = "Schemes paying annual allowance tax"
    val aboveLifetimeAllowance: String = "Above lifetime allowance"
    val lumpSum: String = "Lump sum"
    val otherPayments: String = "Other payments"
    val lifetimeAllowanceSchemes: String = "Schemes paying lifetime allowance tax"


    val aboveAnnualOrLifetimeAllowanceHidden: String = "Change above annual or lifetime allowance"
    val reducedAnnualAllowanceHidden: String = "Change reduced annual allowance"
    val typeOfReducedAnnualAllowanceHidden: String = "Change type of reduced annual allowance"
    val aboveAnnualAllowanceHidden: String = "Change above annual allowance"
    val annualAllowanceTaxHidden: String = "Change annual allowance tax"
    val annualAllowanceSchemesHidden: String = "Change schemes paying annual allowance tax"
    val aboveLifetimeAllowanceHidden: String = "Change above lifetime allowance"
    val lumpSumHidden: String = "Change lump sum"
    val otherPaymentsHidden: String = "Change other payments"
    val lifetimeAllowanceSchemesHidden: String = "Change schemes paying lifetime allowance tax"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Annual and lifetime allowances for 6 April ${taxYear - 1} to 5 April $taxYear"

    val yes = "Iawn"
    val no = "Na"

    val unauthorisedPayments = "Unauthorised payments"
    val amountSurcharged = "Amount surcharged"
    val nonUkTaxAmountSurcharged = "Non UK-tax on amount surcharged"
    val amountNotSurcharged = "Amount not surcharged"
    val nonUkTaxAmountNotSurcharged = "Non UK-tax on amount not surcharged"
    val ukPensionSchemes = "UK pension schemes"
    val pensionSchemeTaxReferences = "Pension Scheme Tax References"


    val saveAndContinue = "Cadw ac yn eich blaen"
    val error = "Sorry, there is a problem with the service"

    val unauthorisedPaymentsHidden = "Change unauthorised payments"
    val amountSurchargedHidden = "Change amount surcharged"
    val nonUkTaxAmountSurchargedHidden = "Change non UK-tax on amount surcharged"
    val amountNotSurchargedHidden = "Change amount not surcharged"
    val nonUkTaxAmountNotSurchargedHidden = "Change non UK-tax on amount not surcharged"
    val ukPensionSchemesHidden = "Change UK pension schemes"
    val pensionSchemeTaxReferencesHidden = "Change Pension Scheme Tax References"


    val aboveAnnualOrLifetimeAllowance: String = "Uwch na’r lwfans blynyddol neu lwfans oes"
    val reducedAnnualAllowance: String = "Lwfans blynyddol wedi’i ostwng"
    val typeOfReducedAnnualAllowance: String = "Math o lwfans blynyddol wedi’i ostwng"
    val aboveAnnualAllowance: String = "Uwch na’r lwfans blynyddol"
    val annualAllowanceTax: String = "Treth lwfans blynyddol"
    val annualAllowanceSchemes: String = "Cynlluniau sy’n talu treth lwfans blynyddol"
    val aboveLifetimeAllowance: String = "Uwch na’r lwfans oes pensiwn"
    val lumpSum: String = "Cyfandaliad"
    val otherPayments: String = "Taliadau eraill"
    val lifetimeAllowanceSchemes: String = "Cynlluniau sy’n talu treth lwfans oes"


    val aboveAnnualOrLifetimeAllowanceHidden: String = "Newidiwch uwch na lwfans blynyddol neu lwfans oes"
    val reducedAnnualAllowanceHidden: String = "Newidiwch lwfans blynyddol wedi’i ostwng"
    val typeOfReducedAnnualAllowanceHidden: String = "Newidiwch fath o lwfans blynyddol wedi’i ostwng"
    val aboveAnnualAllowanceHidden: String = "Newidiwch uwch na’r lwfans blynyddol"
    val annualAllowanceTaxHidden: String = "Newidiwch dreth lwfans blynyddol"
    val annualAllowanceSchemesHidden: String = "Newidiwch gynlluniau sy’n talu treth lwfans blynyddol"
    val aboveLifetimeAllowanceHidden: String = "Newidiwch uwch na’r lwfans oes pensiwn"
    val lumpSumHidden: String = "Newidiwch gyfandaliad"
    val otherPaymentsHidden: String = "Newidiwch daliadau eraill"
    val lifetimeAllowanceSchemesHidden: String = "Newidiwch gynlluniau sy’n talu treth lwfans oes"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Check your annual and lifetime allowances"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Check your client’s annual and lifetime allowances"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Gwiriwch eich lwfansau blynyddol ac oes"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Gwiriwch lwfansau blynyddol ac oes eich cleient"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  def pensionsUsersData(isPrior: Boolean = false, pensionsCyaModel: PensionsCYAModel): PensionsUserData = {
    PensionsUserDataBuilder.aPensionsUserData.copy(
      isPriorSubmission = isPrior, pensions = pensionsCyaModel)
  }

  ".show" should { //scalastyle:off magic.number
    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        import user.commonExpectedResults._
        
        val booleanToYesOrNo: Boolean => String = (bool: Boolean) =>
          if (bool) user.commonExpectedResults.yes else user.commonExpectedResults.no

        "there is no CYA data and a CYA model is generated" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropPensionsDB()
            insertCyaData(pensionsUserDataWithAnnualAndLifetimeAllowance(
              aPensionAnnualAllowanceViewModel, aPensionLifetimeAllowanceViewModel, isPriorSubmission = false))
            userDataStub(anIncomeTaxUserData.copy(pensions = Some(anAllPensionsData)), nino, taxYear)
            urlGet(fullUrl(checkAnnualLifetimeAllowanceCYA(taxYear)), welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          val pensionCharge = anAllPensionsData.pensionCharges.get
          val savingsTaxCharges = pensionCharge.pensionSavingsTaxCharges
          val pensionContributions = pensionCharge.pensionContributions

          val annualLifeTimeQuestion = savingsTaxCharges.map(_.benefitInExcessOfLifetimeAllowance).isDefined ||
            pensionCharge.pensionSavingsTaxCharges.map(_.lumpSumBenefitTakenInExcessOfLifetimeAllowance).isDefined

          val typeOfAnnualCharge: String =
            if(savingsTaxCharges.flatMap(_.moneyPurchasedAllowance).isDefined){
              "Money purchase"
            }else if(savingsTaxCharges.flatMap(_.taperedAnnualAllowance).isDefined){
              "Tapered"
            } else {
              ""
            }

          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          
          cyaRowCheck(aboveAnnualOrLifetimeAllowance, booleanToYesOrNo(annualLifeTimeQuestion),
            ChangeLinksUnauthorisedPayments.aboveAnnualOrLifetimeAllowance, aboveAnnualOrLifetimeAllowanceHidden, 1)
          
          cyaRowCheck(reducedAnnualAllowance, booleanToYesOrNo(savingsTaxCharges.map(_.isAnnualAllowanceReduced).get),
            ChangeLinksUnauthorisedPayments.reducedAnnualAllowance, reducedAnnualAllowanceHidden, 2)
          
          cyaRowCheck(typeOfReducedAnnualAllowance, typeOfAnnualCharge, ChangeLinksUnauthorisedPayments.typeOfReducedAnnualAllowance,
            typeOfReducedAnnualAllowanceHidden, 3)
          
          cyaRowCheck(aboveAnnualAllowance, moneyContent(pensionContributions.map(_.inExcessOfTheAnnualAllowance).get),
            ChangeLinksUnauthorisedPayments.aboveAnnualAllowance, aboveAnnualAllowanceHidden, 4)
          
          cyaRowCheck(annualAllowanceTax, moneyContent(pensionContributions.map(_.annualAllowanceTaxPaid).get),
            ChangeLinksUnauthorisedPayments.annualAllowanceTax, annualAllowanceTaxHidden, 5)
          
          cyaRowCheck(annualAllowanceSchemes, s"${pensionContributions.get.pensionSchemeTaxReference.mkString(", ")}",
            ChangeLinksUnauthorisedPayments.annualAllowanceSchemes, annualAllowanceSchemesHidden, 6)
          
          cyaRowCheck(aboveLifetimeAllowance, "", ChangeLinksUnauthorisedPayments.aboveLifetimeAllowance,
            aboveLifetimeAllowanceHidden, 7)
          
          cyaRowCheck(lumpSum, amountAndTaxPaidContent(savingsTaxCharges.get.lumpSumBenefitTakenInExcessOfLifetimeAllowance.flatMap(_.amount).get,
            savingsTaxCharges.get.lumpSumBenefitTakenInExcessOfLifetimeAllowance.flatMap(_.taxPaid).get),
            ChangeLinksUnauthorisedPayments.lumpSum, lumpSumHidden, 8)
          
          cyaRowCheck(otherPayments, amountAndTaxPaidContent(savingsTaxCharges.get.benefitInExcessOfLifetimeAllowance.flatMap(_.amount).get,
            savingsTaxCharges.get.benefitInExcessOfLifetimeAllowance.flatMap(_.taxPaid).get), ChangeLinksUnauthorisedPayments.otherPayments,
            otherPaymentsHidden, 9)
          
          cyaRowCheck(lifetimeAllowanceSchemes, s"${savingsTaxCharges.get.pensionSchemeTaxReference.get.mkString(", ")}",
            ChangeLinksUnauthorisedPayments.lifetimeAllowancePstrSummary, lifetimeAllowanceSchemesHidden, 10)
          
          buttonCheck(saveAndContinue)
          welshToggleCheck(user.isWelsh)
        }
        "there is no CYA data and a CYA model is generated with reduced annual allowance set to false" which {

          val updatedAllPensionsData = anAllPensionsData.copy(
            pensionCharges = Some(anPensionCharges.copy(
              pensionSavingsTaxCharges = Some(anPensionSavngTaxCharges.copy(
                isAnnualAllowanceReduced = false
              ))
            )))


          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropPensionsDB()
            insertCyaData(pensionsUserDataWithAnnualAndLifetimeAllowance(aPensionAnnualAllowanceViewModel, aPensionLifetimeAllowanceViewModel,
              isPriorSubmission = false))
            userDataStub(anIncomeTaxUserData.copy(pensions = Some(updatedAllPensionsData)), nino, taxYear)
            urlGet(fullUrl(checkAnnualLifetimeAllowanceCYA(taxYear)), welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          val pensionCharge = anAllPensionsData.pensionCharges.get
          val savingsTaxCharges = pensionCharge.pensionSavingsTaxCharges

          val annualLifeTimeQuestion = savingsTaxCharges.map(_.benefitInExcessOfLifetimeAllowance).isDefined ||
            pensionCharge.pensionSavingsTaxCharges.map(_.lumpSumBenefitTakenInExcessOfLifetimeAllowance).isDefined

          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          
          cyaRowCheck(aboveAnnualOrLifetimeAllowance, booleanToYesOrNo(annualLifeTimeQuestion),
            ChangeLinksUnauthorisedPayments.aboveAnnualOrLifetimeAllowance, aboveAnnualOrLifetimeAllowanceHidden, 1)
          
          cyaRowCheck(reducedAnnualAllowance, user.commonExpectedResults.no, ChangeLinksUnauthorisedPayments.reducedAnnualAllowance,
            reducedAnnualAllowanceHidden, 2)
          
          cyaRowCheck(aboveLifetimeAllowance, "", ChangeLinksUnauthorisedPayments.aboveLifetimeAllowance,
            aboveLifetimeAllowanceHidden, 3)
          
          cyaRowCheck(lumpSum, amountAndTaxPaidContent(savingsTaxCharges.get.lumpSumBenefitTakenInExcessOfLifetimeAllowance.flatMap(_.amount).get,
            savingsTaxCharges.get.lumpSumBenefitTakenInExcessOfLifetimeAllowance.flatMap(_.taxPaid).get),
            ChangeLinksUnauthorisedPayments.lumpSum, lumpSumHidden, 4)
          
          cyaRowCheck(otherPayments, amountAndTaxPaidContent(savingsTaxCharges.get.benefitInExcessOfLifetimeAllowance.flatMap(_.amount).get,
            savingsTaxCharges.get.benefitInExcessOfLifetimeAllowance.flatMap(_.taxPaid).get),
            ChangeLinksUnauthorisedPayments.otherPayments, otherPaymentsHidden, 5)
          
          buttonCheck(saveAndContinue)
          welshToggleCheck(user.isWelsh)
        }
      }

    }
  }

  ".submit" should {
    "redirect to the overview page" when {

      "there is no CYA data available" should {

        val form = Map[String, String]()

        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual()
          urlPost(fullUrl(checkAnnualLifetimeAllowanceCYA(taxYear)), form, follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)))
        }

        "have the status SEE OTHER" in {
          result.status shouldBe SEE_OTHER
        }

        "redirects to the overview page" in {
          result.headers("Location").head shouldBe appConfig.incomeTaxSubmissionOverviewUrl(taxYear)
        }
      }
    }

    "redirect to the summary page" when {

      "the CYA data differs from the prior data" should {

        val form = Map[String, String]()

        lazy val result: WSResponse = {
          dropPensionsDB()
          userDataStub(anIncomeTaxUserData.copy(pensions = Some(anAllPensionsData)), nino, taxYear)
          insertCyaData(aPensionsUserData.copy(pensions = aPensionsCYAModel.copy(paymentsIntoPension = cyaDataIncomplete),
            taxYear = taxYear))
          authoriseAgentOrIndividual()
          urlPost(fullUrl(checkAnnualLifetimeAllowanceCYA(taxYear)), form, follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)))
        }

        "the status is SEE OTHER" in {
          result.status shouldBe SEE_OTHER
        }

        "redirects to the summary page" in {
          result.headers("Location").head shouldBe AnnualLifetimeAllowanceCYAController.show(taxYear).url
        }
      }

      "the user makes no changes and no submission to backend API is made" should {

        val unchangedAllowances = PensionAnnualAllowancesViewModel(
          Some(anPensionSavngTaxCharges.isAnnualAllowanceReduced),
          anPensionSavngTaxCharges.moneyPurchasedAllowance, anPensionSavngTaxCharges.taperedAnnualAllowance,
          Some(true), Some(anPensionContributions.inExcessOfTheAnnualAllowance), Some(true),
          Some(anPensionContributions.annualAllowanceTaxPaid),
          Some(anPensionContributions.pensionSchemeTaxReference))

        val form = Map[String, String]()

        lazy val result: WSResponse = {
          dropPensionsDB()
          userDataStub(anIncomeTaxUserData.copy(pensions = Some(anAllPensionsData)), nino, taxYear)
          insertCyaData(aPensionsUserData.copy(pensions = aPensionsCYAModel.copy
          (paymentsIntoPension = aPaymentsIntoPensionViewModel,
            pensionsAnnualAllowances = unchangedAllowances,
            pensionLifetimeAllowances = aPensionLifetimeAllowanceViewModel,
            incomeFromPensions = anIncomeFromPensionsViewModel,
            unauthorisedPayments = anUnauthorisedPaymentsViewModel,
            paymentsIntoOverseasPensions = aPaymentsIntoOverseasPensionsViewModel
          ), taxYear = taxYear))
          authoriseAgentOrIndividual()
          urlPost(fullUrl(checkAnnualLifetimeAllowanceCYA(taxYear)), form, follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)))
        }

        "the status is SEE OTHER" in {
          result.status shouldBe SEE_OTHER
        }

        "redirects to the summary page" in {
          result.headers("Location").head shouldBe AnnualLifetimeAllowanceCYAController.show(taxYear).url
        }
      }
    }
  }
}

