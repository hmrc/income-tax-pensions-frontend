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

package controllers.pensions.paymentsIntoPensions

import builders.AllPensionsDataBuilder.anAllPensionsData
import builders.IncomeFromOverseasPensionsViewModelBuilder.anIncomeFromOverseasPensionsViewModel
import builders.IncomeFromPensionsViewModelBuilder.anIncomeFromPensionsViewModel
import builders.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import builders.PaymentsIntoOverseasPensionsViewModelBuilder.aPaymentsIntoOverseasPensionsViewModel
import builders.PaymentsIntoPensionVewModelBuilder.aPaymentsIntoPensionViewModel
import builders.PensionContributionsBuilder.anPensionContributions
import builders.PensionLifetimeAllowanceViewModelBuilder.aPensionLifetimeAllowanceViewModel
import builders.PensionSavingTaxChargesBuilder.anPensionSavngTaxCharges
import builders.PensionsCYAModelBuilder.{aPensionsCYAModel, paymentsIntoPensionOnlyCYAModel}
import builders.PensionsUserDataBuilder.aPensionsUserData
import builders.ReliefsBuilder.anReliefs
import builders.UnauthorisedPaymentsViewModelBuilder.anUnauthorisedPaymentsViewModel
import builders.UserBuilder.aUserRequest
import models.IncomeTaxUserData
import models.pension.charges.PensionAnnualAllowancesViewModel
import models.pension.reliefs.PaymentsIntoPensionViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.PaymentIntoPensions.checkPaymentsIntoPensionCyaUrl
import utils.PageUrls.fullUrl
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}
import views.PaymentsIntoPensionsCYASpec.CommonExpectedEN.{no => answerNo, _}
import views.PaymentsIntoPensionsCYASpec.ExpectedIndividualEN._
import views.PaymentsIntoPensionsCYASpec._


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

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq.empty

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

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(expectedTitle)
        h1Check(expectedH1)
        captionCheck(expectedCaption(taxYear))

        cyaRowCheck(reliefAtSource, yes, ChangeLinks.reliefAtSource, reliefAtSourceHidden, 1)
        cyaRowCheck(reliefAtSourceAmount, s"${moneyContent(anReliefs.regularPensionContributions.get)}",
          ChangeLinks.reliefAtSourceAmount, reliefAtSourceAmountHidden, 2)
        cyaRowCheck(oneOff, yes, ChangeLinks.oneOff, oneOffHidden, 3)
        cyaRowCheck(oneOffAmount, s"${moneyContent(anReliefs.oneOffPensionContributionsPaid.get)}",
          ChangeLinks.oneOffAmount, oneOffAmountHidden, 4)
        cyaRowCheck(pensionsTaxReliefNotClaimed, yes, ChangeLinks.pensionsTaxReliefNotClaimed, pensionsTaxReliefNotClaimedHidden, 5)
        cyaRowCheck(retirementAnnuity, yes, ChangeLinks.retirementAnnuity, retirementAnnuityHidden, 6)
        cyaRowCheck(retirementAnnuityAmount, s"${moneyContent(anReliefs.retirementAnnuityPayments.get)}",
          ChangeLinks.retirementAnnuityAmount, retirementAnnuityAmountHidden, 7)
        cyaRowCheck(workplacePayments, yes, ChangeLinks.workplacePayments, workplacePaymentsHidden, 8)
        cyaRowCheck(workplacePaymentsAmount, s"${moneyContent(anReliefs.paymentToEmployersSchemeNoTaxRelief.get)}",
          ChangeLinks.workplacePaymentsAmount, workplacePaymentsAmountHidden, 9)

        buttonCheck(saveAndContinue)
        welshToggleCheck(isWelsh = false)
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

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(expectedTitle)
        h1Check(expectedH1)
        captionCheck(expectedCaption(taxYear))

        //noinspection ScalaStyle
        cyaRowCheck(reliefAtSource, yes, ChangeLinks.reliefAtSource, reliefAtSourceHidden, 1)
        cyaRowCheck(reliefAtSourceAmount, s"${moneyContent(aPaymentsIntoPensionViewModel.totalRASPaymentsAndTaxRelief.get)}",
          ChangeLinks.reliefAtSourceAmount, reliefAtSourceAmountHidden, 2)
        cyaRowCheck(oneOff, yes, ChangeLinks.oneOff, oneOffHidden, 3)
        cyaRowCheck(oneOffAmount, s"${moneyContent(aPaymentsIntoPensionViewModel.totalOneOffRasPaymentPlusTaxRelief.get)}",
          ChangeLinks.oneOffAmount, oneOffAmountHidden, 4)
        cyaRowCheck(pensionsTaxReliefNotClaimed, yes, ChangeLinks.pensionsTaxReliefNotClaimed, pensionsTaxReliefNotClaimedHidden, 5)
        cyaRowCheck(retirementAnnuity, yes, ChangeLinks.retirementAnnuity, retirementAnnuityHidden, 6)
        cyaRowCheck(retirementAnnuityAmount, s"${moneyContent(aPaymentsIntoPensionViewModel.totalRetirementAnnuityContractPayments.get)}",
          ChangeLinks.retirementAnnuityAmount, retirementAnnuityAmountHidden, 7)
        cyaRowCheck(workplacePayments, yes, ChangeLinks.workplacePayments, workplacePaymentsHidden, 8)
        cyaRowCheck(workplacePaymentsAmount, s"${moneyContent(aPaymentsIntoPensionViewModel.totalWorkplacePensionPayments.get)}",
          ChangeLinks.workplacePaymentsAmount, workplacePaymentsAmountHidden, 9)

        buttonCheck(saveAndContinue)

        welshToggleCheck(isWelsh = false)
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

      implicit def document: () => Document = () => Jsoup.parse(result.body)


      //noinspection ScalaStyle
      cyaRowCheck(reliefAtSource, answerNo, ChangeLinks.reliefAtSource, reliefAtSourceHidden, 1)
      cyaRowCheck(pensionsTaxReliefNotClaimed, answerNo, ChangeLinks.pensionsTaxReliefNotClaimed, pensionsTaxReliefNotClaimedHidden, 2)

      buttonCheck(saveAndContinue)

      welshToggleCheck(isWelsh = false)
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
          controllers.pensions.paymentsIntoPensions.routes.ReliefAtSourcePaymentsAndTaxReliefAmountController.show(taxYear).url
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
        result.headers("Location").head shouldBe controllers.pensions.paymentsIntoPensions.routes.ReliefAtSourcePensionsController.show(taxYear).url
      }
    }
  }

  ".submit" should {

    "redirect to the overview page" when {

      "there is no CYA data available" should {

        val form = Map[String, String]()
        val userData = anIncomeTaxUserData.copy(pensions = Some(anAllPensionsData))

        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual(isAgent = false)
          userDataStub(userData, nino, taxYear)
          pensionReliefsSessionStub("", nino, taxYear)
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

      "the CYA data differs from the prior data and submission changes are persisted" should {

        val form = Map[String, String]()

        lazy val result: WSResponse = {
          dropPensionsDB()
          userDataStub(anIncomeTaxUserData.copy(pensions = Some(anAllPensionsData)), nino, taxYear)
          pensionReliefsSessionStub("", nino, taxYear)
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
          Some(true), Some(anPensionContributions.inExcessOfTheAnnualAllowance), Some(true),
          Some(anPensionContributions.annualAllowanceTaxPaid),
          Some(anPensionContributions.pensionSchemeTaxReference))

        val form = Map[String, String]()

        lazy val result: WSResponse = {
          dropPensionsDB()
          userDataStub(anIncomeTaxUserData.copy(pensions = Some(anAllPensionsData)), nino, taxYear)
          pensionReliefsSessionStub("", nino, taxYear)
          insertCyaData(aPensionsUserData.copy(pensions = aPensionsCYAModel.copy
          (paymentsIntoPension = unchangedModel,
            pensionsAnnualAllowances = unchangedAllowances,
            pensionLifetimeAllowances = aPensionLifetimeAllowanceViewModel,
            incomeFromPensions = anIncomeFromPensionsViewModel,
            unauthorisedPayments = anUnauthorisedPaymentsViewModel,
            paymentsIntoOverseasPensions = aPaymentsIntoOverseasPensionsViewModel,
            incomeFromOverseasPensions = anIncomeFromOverseasPensionsViewModel
          ), taxYear = taxYear), aUserRequest)
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
  }
}
// scalastyle:on magic.number
