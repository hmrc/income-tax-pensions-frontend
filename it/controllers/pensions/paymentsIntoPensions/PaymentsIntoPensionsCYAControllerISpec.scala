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

package controllers.pensions.paymentsIntoPensions

import builders.AllPensionsDataBuilder.anAllPensionsData
import builders.IncomeFromPensionsViewModelBuilder.anIncomeFromPensionsViewModel
import builders.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import builders.PensionContributionsBuilder.anPensionContributions
import builders.PensionLifetimeAllowanceViewModelBuilder.aPensionLifetimeAllowanceViewModel
import builders.PensionSavingTaxChargesBuilder.anPensionSavngTaxCharges
import builders.PensionsCYAModelBuilder.{aPensionsCYAModel, paymentsIntoPensionOnlyCYAModel}
import builders.PensionsUserDataBuilder.aPensionsUserData
import builders.ReliefsBuilder.anReliefs
import builders.UnauthorisedPaymentsViewModelBuilder.anUnauthorisedPaymentsViewModel
import builders.UserBuilder.aUserRequest
import forms.Yes
import models.IncomeTaxUserData
import models.pension.charges.PensionAnnualAllowancesViewModel
import models.pension.reliefs.PaymentsIntoPensionViewModel
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.PaymentIntoPensions.checkPaymentsIntoPensionCyaUrl
import utils.PageUrls.fullUrl
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

// scalastyle:off magic.number
class PaymentsIntoPensionsCYAControllerISpec extends IntegrationTest with ViewHelpers with BeforeAndAfterEach with PensionsDatabaseHelper {

  val url: String = fullUrl(checkPaymentsIntoPensionCyaUrl(taxYear))

  val cyaDataIncomplete: PaymentsIntoPensionViewModel = PaymentsIntoPensionViewModel(
    rasPensionPaymentQuestion = Some(true)
  )

  val cyaDataMinimal: PaymentsIntoPensionViewModel = PaymentsIntoPensionViewModel(
    rasPensionPaymentQuestion = Some(false),
    pensionTaxReliefNotClaimedQuestion = Some(false)
  )

  object ChangeLinks {
    val reliefAtSource: String = controllers.pensions.paymentsIntoPension.routes.ReliefAtSourcePensionsController.show(taxYear).url
    val reliefAtSourceAmount: String = controllers.pensions.paymentsIntoPension.routes.ReliefAtSourcePaymentsAndTaxReliefAmountController.show(taxYear).url
    val oneOff: String = controllers.pensions.paymentsIntoPension.routes.ReliefAtSourceOneOffPaymentsController.show(taxYear).url
    val oneOffAmount: String = controllers.pensions.paymentsIntoPension.routes.OneOffRASPaymentsAmountController.show(taxYear).url
    val pensionsTaxReliefNotClaimed: String = controllers.pensions.paymentsIntoPension.routes.PensionsTaxReliefNotClaimedController.show(taxYear).url
    val retirementAnnuity: String = controllers.pensions.paymentsIntoPension.routes.RetirementAnnuityController.show(taxYear).url
    val retirementAnnuityAmount: String = controllers.pensions.paymentsIntoPension.routes.RetirementAnnuityAmountController.show(taxYear).url
    val workplacePayments: String = controllers.pensions.paymentsIntoPension.routes.WorkplacePensionController.show(taxYear).url
    val workplacePaymentsAmount: String = controllers.pensions.paymentsIntoPension.routes.WorkplaceAmountController.show(taxYear).url
  }

  trait SpecificExpectedResults {
    val expectedH1: String
    val expectedTitle: String
  }

  trait CommonExpectedResults {
    def expectedCaption(taxYear: Int): String

    val yes: String
    val no: String

    val reliefAtSource: String
    val reliefAtSourceAmount: String
    val oneOff: String
    val oneOffAmount: String
    val pensionsTaxReliefNotClaimed: String
    val retirementAnnuity: String
    val retirementAnnuityAmount: String
    val workplacePayments: String
    val workplacePaymentsAmount: String

    val saveAndContinue: String
    val error: String

    val reliefAtSourceHidden: String
    val reliefAtSourceAmountHidden: String
    val oneOffHidden: String
    val oneOffAmountHidden: String
    val pensionsTaxReliefNotClaimedHidden: String
    val retirementAnnuityHidden: String
    val retirementAnnuityAmountHidden: String
    val workplacePaymentsHidden: String
    val workplacePaymentsAmountHidden: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    def expectedCaption(taxYear: Int): String = s"Payments into pensions for 6 April ${taxYear - 1} to 5 April $taxYear"

    val yes = "Yes"
    val no = "No"

    val reliefAtSource = "Relief at source (RAS) pension payments"
    val reliefAtSourceAmount = "Total RAS payments plus tax relief"
    val oneOff = "One-off RAS payments"
    val oneOffAmount = "Total one-off RAS payments plus tax relief"
    val pensionsTaxReliefNotClaimed = "Pensions where tax relief is not claimed"
    val retirementAnnuity = "Retirement annuity contract payments"
    val retirementAnnuityAmount = "Total retirement annuity contract payments"
    val workplacePayments = "Workplace pension payments"
    val workplacePaymentsAmount = "Total workplace pension payments"

    val saveAndContinue = "Save and continue"
    val error = "Sorry, there is a problem with the service"

    val reliefAtSourceHidden = "Change whether relief at source pensions payments were made"
    val reliefAtSourceAmountHidden = "Change total relief at source pensions payments, plus tax relief"
    val oneOffHidden = "Change whether one-off relief at source pensions payments were made"
    val oneOffAmountHidden = "Change total one-off relief at source pensions payments, plus tax relief"
    val pensionsTaxReliefNotClaimedHidden = "Change whether payments were made into a pension where tax relief was not claimed"
    val retirementAnnuityHidden = "Change whether retirement annuity contract payments were made"
    val retirementAnnuityAmountHidden = "Change total retirement annuity contract payments"
    val workplacePaymentsHidden = "Change whether workplace pension payments were made"
    val workplacePaymentsAmountHidden = "Change total workplace pension payments"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    def expectedCaption(taxYear: Int): String = s"Payments into pensions for 6 April ${taxYear - 1} to 5 April $taxYear"

    val yes = "Yes"
    val no = "No"

    val reliefAtSource = "Relief at source (RAS) pension payments"
    val reliefAtSourceAmount = "Total RAS payments plus tax relief"
    val oneOff = "One-off RAS payments"
    val oneOffAmount = "Total one-off RAS payments plus tax relief"
    val pensionsTaxReliefNotClaimed = "Pensions where tax relief is not claimed"
    val retirementAnnuity = "Retirement annuity contract payments"
    val retirementAnnuityAmount = "Total retirement annuity contract payments"
    val workplacePayments = "Workplace pension payments"
    val workplacePaymentsAmount = "Total workplace pension payments"

    val saveAndContinue = "Save and continue"
    val error = "Sorry, there is a problem with the service"

    val reliefAtSourceHidden = "Change whether relief at source pensions payments were made"
    val reliefAtSourceAmountHidden = "Change total relief at source pensions payments, plus tax relief"
    val oneOffHidden = "Change whether one-off relief at source pensions payments were made"
    val oneOffAmountHidden = "Change total one-off relief at source pensions payments, plus tax relief"
    val pensionsTaxReliefNotClaimedHidden = "Change whether payments were made into a pension where tax relief was not claimed"
    val retirementAnnuityHidden = "Change whether retirement annuity contract payments were made"
    val retirementAnnuityAmountHidden = "Change total retirement annuity contract payments"
    val workplacePaymentsHidden = "Change whether workplace pension payments were made"
    val workplacePaymentsAmountHidden = "Change total workplace pension payments"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedH1 = "Check your payments into pensions"
    val expectedTitle = "Check your payments into pensions"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedH1 = "Check your client’s payments into pensions"
    val expectedTitle = "Check your client’s payments into pensions"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedH1 = "Check your payments into pensions"
    val expectedTitle = "Check your payments into pensions"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedH1 = "Check your client’s payments into pensions"
    val expectedTitle = "Check your client’s payments into pensions"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )


  ".show" when {
    "render the page with a full CYA model" when {

      "there is no CYA data and a CYA model is generated from prior data" which {
        lazy val result: WSResponse = {
          dropPensionsDB()
          userDataStub(anIncomeTaxUserData.copy(pensions = Some(anAllPensionsData)), nino, taxYear)
          authoriseAgentOrIndividual(isAgent = false)
          urlGet(url, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)))
        }

        "has an OK status" in {
          result.status shouldBe OK
        }

      }

      "CYA data is finished" which {
        lazy val result: WSResponse = {
          dropPensionsDB()
          insertCyaData(aPensionsUserData.copy(taxYear = taxYear), aUserRequest)
          authoriseAgentOrIndividual(isAgent = false)
          urlGet(url, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)))
        }

        "has an OK status" in {
          result.status shouldBe OK
        }
      }
    }
    "render the page with a minimal CYA view" which {

      lazy val result: WSResponse = {
        dropPensionsDB()
        insertCyaData(aPensionsUserData.copy(pensions = paymentsIntoPensionOnlyCYAModel(cyaDataMinimal), taxYear = taxYear), aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(url, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)))
      }

      "has an OK status" in {
        result.status shouldBe OK
      }
    }

    "redirect to the first unanswered question when the CYA data is incomplete" should {

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(IncomeTaxUserData(None), nino, taxYear)
        insertCyaData(aPensionsUserData.copy(pensions = aPensionsCYAModel.copy(paymentsIntoPension = cyaDataIncomplete), taxYear = taxYear), aUserRequest)

        urlGet(url, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)))
      }

      "the status is SEE OTHER" in {
        result.status shouldBe SEE_OTHER
      }

      "redirects to the RAS Pensions page" in {
        result.headers("Location").head shouldBe
          controllers.pensions.paymentsIntoPension.routes.ReliefAtSourcePaymentsAndTaxReliefAmountController.show(taxYear).url
      }
    }

    "redirect to the first page in the journey when there is no CYA or prior data" when {

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(url, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)))
      }

      "the status is SEE OTHER" in {
        result.status shouldBe SEE_OTHER
      }

      "redirects to the RAS Pensions page" in {
        result.headers("Location").head shouldBe controllers.pensions.paymentsIntoPension.routes.ReliefAtSourcePensionsController.show(taxYear).url
      }
    }
  }
  ".submit" should {

    "redirect to the overview page" when {

      "there is no CYA data available" should {

        val form = Map[String, String]()

        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(url, form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)))
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
          insertCyaData(aPensionsUserData.copy(pensions = aPensionsCYAModel.copy(paymentsIntoPension = cyaDataIncomplete), taxYear = taxYear), aUserRequest)
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(url, form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)))
        }

        "the status is SEE OTHER" in {
          result.status shouldBe SEE_OTHER
        }

        "redirects to the summary page" in {
          result.headers("Location").head shouldBe controllers.pensions.routes.PensionsSummaryController.show(taxYear).url
        }
      }

      "the user makes no changes and no submission to DES is made" should {

        val unchangedModel =
          PaymentsIntoPensionViewModel(
            Some(true), anReliefs.regularPensionContributions,
            Some(true), anReliefs.oneOffPensionContributionsPaid, Some(true), Some(true), Some(true),
            anReliefs.retirementAnnuityPayments, Some(true), anReliefs.paymentToEmployersSchemeNoTaxRelief)

        val unchangedAllowances = PensionAnnualAllowancesViewModel(
          Some(anPensionSavngTaxCharges.isAnnualAllowanceReduced),
          anPensionSavngTaxCharges.moneyPurchasedAllowance, anPensionSavngTaxCharges.taperedAnnualAllowance,
          Some(true), Some(anPensionContributions.inExcessOfTheAnnualAllowance), Some(Yes.toString),
          Some(anPensionContributions.annualAllowanceTaxPaid),
          Some(anPensionContributions.pensionSchemeTaxReference))

        val form = Map[String, String]()

        lazy val result: WSResponse = {
          dropPensionsDB()
          userDataStub(anIncomeTaxUserData.copy(pensions = Some(anAllPensionsData)), nino, taxYear)
          insertCyaData(aPensionsUserData.copy(pensions = aPensionsCYAModel.copy
          (paymentsIntoPension = unchangedModel, pensionsAnnualAllowances = unchangedAllowances,
            pensionLifetimeAllowances = aPensionLifetimeAllowanceViewModel,
            incomeFromPensions = anIncomeFromPensionsViewModel,
            unauthorisedPayments = anUnauthorisedPaymentsViewModel), taxYear = taxYear), aUserRequest)
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(url, form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)))
        }

        "the status is SEE OTHER" in {
          result.status shouldBe SEE_OTHER
        }

        "redirects to the summary page" in {
          result.headers("Location").head shouldBe controllers.pensions.routes.PensionsSummaryController.show(taxYear).url
        }
      }
    }

    "redirect to an error page" when {

      "an error is returned from DES" in {

      }
    }
  }
}
// scalastyle:on magic.number
