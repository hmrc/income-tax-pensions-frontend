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
import builders.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import builders.PaymentsIntoPensionVewModelBuilder.aPaymentsIntoPensionViewModel
import builders.PensionContributionsBuilder.anPensionContributions
import builders.PensionLifetimeAllowanceViewModelBuilder.aPensionLifetimeAllowanceViewModel
import builders.PensionSavingTaxChargesBuilder.anPensionSavngTaxCharges
import builders.PensionsCYAModelBuilder.{aPensionsCYAModel, paymentsIntoPensionOnlyCYAModel}
import builders.PensionsUserDataBuilder.aPensionsUserData
import builders.ReliefsBuilder.anReliefs
import builders.UserBuilder.aUserRequest
import forms.Yes
import models.IncomeTaxUserData
import models.pension.charges.{PensionAnnualAllowancesViewModel, PensionLifetimeAllowancesViewModel}
import models.pension.reliefs.PaymentsIntoPensionViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.SEE_OTHER
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

  def cyaRowCheck(expectedText: String, expectedValue: String, changeLinkHref: String, changeLinkHiddenText: String, rowNumber: Int)
                 (implicit document: () => Document): Unit = {
    val keySelector = s"#main-content > div > div > dl > div:nth-child($rowNumber) > dt"
    val valueSelector = s"#main-content > div > div > dl > div:nth-child($rowNumber) > dd.govuk-summary-list__value"
    val changeLinkSelector = s"#main-content > div > div > dl > div:nth-child($rowNumber) > dd.govuk-summary-list__actions > a"
    val cyaHiddenChangeLink = s"#main-content > div > div > dl > div:nth-child($rowNumber) > dd.govuk-summary-list__actions > a > span.govuk-visually-hidden"

    s"row number $rowNumber is correct" which {

      s"has the correct row name of '$expectedText'" in {
        document().select(keySelector).text() shouldBe expectedText
      }

      s"has the correct row value of '$expectedValue'" in {
        document().select(valueSelector).text() shouldBe expectedValue
      }

      s"the change link should go to '$changeLinkHref''" in {
        document().select(changeLinkSelector).attr("href") shouldBe changeLinkHref
      }

      s"the change link should have hidden text '$changeLinkHiddenText''" in {
        document().select(cyaHiddenChangeLink).text() shouldBe changeLinkHiddenText
      }

    }
  }

  ".show" when {

    userScenarios.foreach { user =>

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "render the page with a full CYA model" when {

          "there is no CYA data and a CYA model is generated from prior data" which {
            lazy val result: WSResponse = {
              dropPensionsDB()
              userDataStub(anIncomeTaxUserData.copy(pensions = Some(anAllPensionsData)), nino, taxYear)
              authoriseAgentOrIndividual(user.isAgent)
              urlGet(url, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import user.commonExpectedResults._

            titleCheck(user.specificExpectedResults.get.expectedTitle)
            h1Check(user.specificExpectedResults.get.expectedH1)
            captionCheck(user.commonExpectedResults.expectedCaption(taxYear))

            //noinspection ScalaStyle
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

            welshToggleCheck(user.isWelsh)
          }

          "CYA data is finished" which {
            lazy val result: WSResponse = {
              dropPensionsDB()
              insertCyaData(aPensionsUserData.copy(taxYear = taxYear), aUserRequest)
              authoriseAgentOrIndividual(user.isAgent)
              urlGet(url, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import user.commonExpectedResults._

            titleCheck(user.specificExpectedResults.get.expectedTitle)
            h1Check(user.specificExpectedResults.get.expectedH1)
            captionCheck(user.commonExpectedResults.expectedCaption(taxYear))

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

            welshToggleCheck(user.isWelsh)
          }
        }

        "render the page with a minimal CYA view" which {

          lazy val result: WSResponse = {
            dropPensionsDB()
            insertCyaData(aPensionsUserData.copy(pensions = paymentsIntoPensionOnlyCYAModel(cyaDataMinimal), taxYear = taxYear), aUserRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(url, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import user.commonExpectedResults.{no => answerNo, _}

          //noinspection ScalaStyle
          cyaRowCheck(reliefAtSource, answerNo, ChangeLinks.reliefAtSource, reliefAtSourceHidden, 1)
          cyaRowCheck(pensionsTaxReliefNotClaimed, answerNo, ChangeLinks.pensionsTaxReliefNotClaimed, pensionsTaxReliefNotClaimedHidden, 2)

          buttonCheck(saveAndContinue)

          welshToggleCheck(user.isWelsh)
        }

        "redirect to the first unanswered question when the CYA data is incomplete" should {

          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(IncomeTaxUserData(None), nino, taxYear)
            insertCyaData(aPensionsUserData.copy(pensions = aPensionsCYAModel.copy(paymentsIntoPension = cyaDataIncomplete), taxYear = taxYear), aUserRequest)

            urlGet(url, welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          "the status is SEE OTHER" in {
            result.status shouldBe SEE_OTHER
          }

          "redirects to the pensions summary page" in {
            result.headers("Location").head shouldBe controllers.pensions.paymentsIntoPension.routes.ReliefAtSourcePensionsController.show(taxYear).url
          }
        }

        "redirect to the first page in the journey when there is no CYA or prior data" when {

          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(url, welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          "the status is SEE OTHER" in {
            result.status shouldBe SEE_OTHER
          }

          "redirects to the pensions summary page" in {
            result.headers("Location").head shouldBe controllers.pensions.paymentsIntoPension.routes.ReliefAtSourcePensionsController.show(taxYear).url
          }
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
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(url, form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
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
          urlPost(url, form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
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
            pensionLifetimeAllowances = aPensionLifetimeAllowanceViewModel), taxYear = taxYear), aUserRequest)
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(url, form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
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
