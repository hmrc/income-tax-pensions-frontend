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

package controllers.pensions

import builders.AllPensionsDataBuilder.anAllPensionsData
import builders.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import builders.PensionChargesBuilder.anPensionCharges
import builders.StateBenefitsModelBuilder.anStateBenefitsModel
import models.pension.AllPensionsData
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.UNAUTHORIZED
import play.api.libs.ws.WSResponse
import utils.PageUrls._
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

class PensionsSummaryControllerISpec extends IntegrationTest with ViewHelpers with BeforeAndAfterEach with PensionsDatabaseHelper {

  // scalastyle:off magic.number

  object Selectors {
    val paymentsIntoPensionsLink = "#payments-into-pensions-link"
    val incomeFromPensionsLink = "#income-from-pensions-link"
    val pensionAnnualAllowanceLink = "#pension-annual-allowance-link"
    val pensionLifetimeAllowanceLink = "#pension-lifetime-allowance-link"
    val unauthorisedPaymentsFromPensionsLink = "#unauthorised-payments-from-pensions-link"
    val paymentsToOverseasPensionsLink = "#payments-to-overseas-pensions-link"
    val insetTextSelector = "#main-content > div > div > div.govuk-inset-text"
    val buttonSelector = "#returnToOverviewPageBtn"

    def summaryListStatusTagSelector(index: Int): String = {
      s"#pensions-Summary > dl > div:nth-child($index) > dd > strong"
    }
  }

  trait SpecificExpectedResults {
    val expectedH1: String
    val expectedTitle: String
  }

  trait CommonExpectedResults {
    def expectedCaption(taxYear: Int): String

    val buttonText: String
    val updated: String
    val cannotUpdate: String
    val toDo: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    def expectedCaption(taxYear: Int): String = s"Pensions for 6 April ${taxYear - 1} to 5 April $taxYear"

    val buttonText = "Return to overview"
    val updated = "Updated"
    val cannotUpdate = "Cannot update"
    val toDo = "To do"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    def expectedCaption(taxYear: Int): String = s"Pensions for 6 April ${taxYear - 1} to 5 April $taxYear"

    val buttonText = "Return to overview"
    val updated = "Updated"
    val cannotUpdate = "Cannot update"
    val toDo = "To do"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedH1 = "Pensions"
    val expectedTitle = "Pensions"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedH1 = "Pensions"
    val expectedTitle = "Pensions"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedH1: String = "Pensions"
    val expectedTitle: String = "Pensions"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedH1: String = "Pensions"
    val expectedTitle: String = "Pensions"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  ".show" when {
    import Selectors._
    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        "render the page where minimal prior data exists for all statuses to be all 'Updated'" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)

            // if customerAddedStateBenefits and pensionSchemeOverseasTransfers empty still 'updated' if
            // stateBenefits and overseasPensionContributions present
            val allUpdatedNoOverseasTransfersOrCustomerStateBenefits: AllPensionsData = anAllPensionsData.copy(
              stateBenefits = Some(anStateBenefitsModel.copy(customerAddedStateBenefits = None)),
              pensionCharges = Some(anPensionCharges.copy(pensionSchemeOverseasTransfers = None)))

            userDataStub(anIncomeTaxUserData.copy(pensions = Some(allUpdatedNoOverseasTransfersOrCustomerStateBenefits)), nino, taxYear)
            urlGet(fullUrl(pensionSummaryUrl(taxYear)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(user.commonExpectedResults.expectedCaption(taxYear))

          "has an payment into pensions section" which {
            linkCheck("Payments into pensions", paymentsIntoPensionsLink, checkPaymentsIntoPensionCyaUrl(taxYear))
            textOnPageCheck(user.commonExpectedResults.updated, summaryListStatusTagSelector(1))
          }

          "has an income from pensions section" which {
            //TODO: Change to use the href below when income From Pensions cya page available
            //linkCheck("Income from pensions", incomeFromPensionsLink, checkIncomeFromPensionCyaUrl(taxYear))
            linkCheck("Income from pensions", incomeFromPensionsLink, "#")
            textOnPageCheck(user.commonExpectedResults.updated, summaryListStatusTagSelector(2))
          }

          "has an pension annual allowance section" which {
            //TODO: Change to use the href below when pension annual allowance cya page available
            //linkCheck("Pension annual allowance", pensionAnnualAllowanceLink, checkPensionAnnualAllowanceCyaUrl(taxYear))
            linkCheck("Pension annual allowance", pensionAnnualAllowanceLink, "#")
            textOnPageCheck(user.commonExpectedResults.updated, summaryListStatusTagSelector(3))
          }

          "has an pension lifetime allowance section" which {
            //TODO: Change to use the href below when pension lifetime allowance cya page available
            //linkCheck("Pension lifetime allowance", pensionAnnualAllowanceLink, checkPensionLifetimeAllowanceCyaUrl(taxYear))
            linkCheck("Pension lifetime allowance", pensionLifetimeAllowanceLink, "#")
            textOnPageCheck(user.commonExpectedResults.updated, summaryListStatusTagSelector(4))
          }

          "has an unauthorised payments from pensions section" which {
            //TODO: Change to use the href below when pension lifetime allowance cya page available
            //linkCheck("Unauthorised payments from pensions", unauthorisedPaymentsFromPensionsLink, checkUnauthorisedPaymentsFromPensionsLinkCyaUrl(taxYear))
            linkCheck("Unauthorised payments from pensions", unauthorisedPaymentsFromPensionsLink, "#")
            textOnPageCheck(user.commonExpectedResults.updated, summaryListStatusTagSelector(5))
          }

          "has a payments into overseas pensions section" which {
            //TODO: Change to use the href below when pension lifetime allowance cya page available
            //linkCheck("Payments into overseas pensions", unauthorisedPaymentsFromPensionsLink, checkUnauthorisedPaymentsFromPensionsLinkCyaUrl(taxYear))
            linkCheck("Payments into overseas pensions", paymentsToOverseasPensionsLink, "#")
            textOnPageCheck(user.commonExpectedResults.updated, summaryListStatusTagSelector(6))
          }

          buttonCheck(user.commonExpectedResults.buttonText, buttonSelector)

          welshToggleCheck(user.isWelsh)
        }

        "render the page where alternate minimal prior data exists for all statuses to be all 'Updated'" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)

            // if stateBenefits and overseasPensionContributions empty still 'updated' if
            // customerAddedStateBenefits and pensionSchemeOverseasTransfers present
            val allUpdatedNoOverseasContributionsOrStateBenefits: AllPensionsData = anAllPensionsData.copy(
              stateBenefits = Some(anStateBenefitsModel.copy(stateBenefits = None)),
              pensionCharges = Some(anPensionCharges.copy(overseasPensionContributions = None)))

            userDataStub(anIncomeTaxUserData.copy(pensions = Some(allUpdatedNoOverseasContributionsOrStateBenefits)), nino, taxYear)
            urlGet(fullUrl(pensionSummaryUrl(taxYear)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))

          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(user.commonExpectedResults.expectedCaption(taxYear))

          "has an payment into pensions section" which {
            linkCheck("Payments into pensions", paymentsIntoPensionsLink, checkPaymentsIntoPensionCyaUrl(taxYear))
            textOnPageCheck(user.commonExpectedResults.updated, summaryListStatusTagSelector(1))
          }

          "has an income from pensions section" which {
            //TODO: Change to use the href below when income From Pensions cya page available
            //linkCheck("Income from pensions", incomeFromPensionsLink, checkIncomeFromPensionCyaUrl(taxYear))
            linkCheck("Income from pensions", incomeFromPensionsLink, "#")
            textOnPageCheck(user.commonExpectedResults.updated, summaryListStatusTagSelector(2))
          }

          "has an pension annual allowance section" which {
            //TODO: Change to use the href below when pension annual allowance cya page available
            //linkCheck("Pension annual allowance", pensionAnnualAllowanceLink, checkPensionAnnualAllowanceCyaUrl(taxYear))
            linkCheck("Pension annual allowance", pensionAnnualAllowanceLink, "#")
            textOnPageCheck(user.commonExpectedResults.updated, summaryListStatusTagSelector(3))
          }

          "has an pension lifetime allowance section" which {
            //TODO: Change to use the href below when pension lifetime allowance cya page available
            //linkCheck("Pension lifetime allowance", pensionAnnualAllowanceLink, checkPensionLifetimeAllowanceCyaUrl(taxYear))
            linkCheck("Pension lifetime allowance", pensionLifetimeAllowanceLink, "#")
            textOnPageCheck(user.commonExpectedResults.updated, summaryListStatusTagSelector(4))
          }

          "has an unauthorised payments from pensions section" which {
            //TODO: Change to use the href below when pension lifetime allowance cya page available
            //linkCheck("Unauthorised payments from pensions", unauthorisedPaymentsFromPensionsLink, checkUnauthorisedPaymentsFromPensionsLinkCyaUrl(taxYear))
            linkCheck("Unauthorised payments from pensions", unauthorisedPaymentsFromPensionsLink, "#")
            textOnPageCheck(user.commonExpectedResults.updated, summaryListStatusTagSelector(5))
          }

          "has a payments into overseas pensions section" which {
            //TODO: Change to use the href below when pension lifetime allowance cya page available
            //linkCheck("Payments into overseas pensions", unauthorisedPaymentsFromPensionsLink, checkUnauthorisedPaymentsFromPensionsLinkCyaUrl(taxYear))
            linkCheck("Payments into overseas pensions", paymentsToOverseasPensionsLink, "#")
            textOnPageCheck(user.commonExpectedResults.updated, summaryListStatusTagSelector(6))
          }

          buttonCheck(user.commonExpectedResults.buttonText, buttonSelector)

          welshToggleCheck(user.isWelsh)
        }

        "render the page where no prior data exists for all pension data so the statuses are all 'To do'" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            userDataStub(anIncomeTaxUserData.copy(pensions = None), nino, taxYear)
            urlGet(fullUrl(pensionSummaryUrl(taxYear)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(user.commonExpectedResults.expectedCaption(taxYear))

          "has an payment into pensions section" which {
            linkCheck("Payments into pensions", paymentsIntoPensionsLink, checkPaymentsIntoPensionCyaUrl(taxYear))
            textOnPageCheck(user.commonExpectedResults.toDo, summaryListStatusTagSelector(1))
          }

          "has an income from pensions section" which {
            //TODO: Change to use the href below when income From Pensions cya page available
            //linkCheck("Income from pensions", incomeFromPensionsLink, checkIncomeFromPensionCyaUrl(taxYear))
            linkCheck("Income from pensions", incomeFromPensionsLink, "#")
            textOnPageCheck(user.commonExpectedResults.toDo, summaryListStatusTagSelector(2))
          }

          "has an pension annual allowance section" which {
            //TODO: Change to use the href below when pension annual allowance cya page available
            //linkCheck("Pension annual allowance", pensionAnnualAllowanceLink, checkPensionAnnualAllowanceCyaUrl(taxYear))
            linkCheck("Pension annual allowance", pensionAnnualAllowanceLink, "#")
            textOnPageCheck(user.commonExpectedResults.toDo, summaryListStatusTagSelector(3))
          }

          "has an pension lifetime allowance section" which {
            //TODO: Change to use the href below when pension lifetime allowance cya page available
            //linkCheck("Pension lifetime allowance", pensionAnnualAllowanceLink, checkPensionLifetimeAllowanceCyaUrl(taxYear))
            linkCheck("Pension lifetime allowance", pensionLifetimeAllowanceLink, "#")
            textOnPageCheck(user.commonExpectedResults.toDo, summaryListStatusTagSelector(4))
          }

          "has an unauthorised payments from pensions section" which {
            //TODO: Change to use the href below when pension lifetime allowance cya page available
            //linkCheck("Unauthorised payments from pensions", unauthorisedPaymentsFromPensionsLink, checkUnauthorisedPaymentsFromPensionsLinkCyaUrl(taxYear))
            linkCheck("Unauthorised payments from pensions", unauthorisedPaymentsFromPensionsLink, "#")
            textOnPageCheck(user.commonExpectedResults.toDo, summaryListStatusTagSelector(5))
          }

          "has a payments into overseas pensions section" which {
            //TODO: Change to use the href below when pension lifetime allowance cya page available
            //linkCheck("Payments into overseas pensions", unauthorisedPaymentsFromPensionsLink, checkUnauthorisedPaymentsFromPensionsLinkCyaUrl(taxYear))
            linkCheck("Payments into overseas pensions", paymentsToOverseasPensionsLink, "#")
            textOnPageCheck(user.commonExpectedResults.toDo, summaryListStatusTagSelector(6))
          }

          buttonCheck(user.commonExpectedResults.buttonText, buttonSelector)

          welshToggleCheck(user.isWelsh)
        }

        "render the page with prior data but only a subset of the underlying pension charges are present leading to a mix of 'updated' and To do'" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)

            val halfPensionChargesData: AllPensionsData = anAllPensionsData.copy(pensionCharges =
              Some(anPensionCharges.copy(pensionSavingsTaxCharges = None, pensionContributions = None)))

            userDataStub(anIncomeTaxUserData.copy(pensions = Some(halfPensionChargesData)), nino, taxYear)

            urlGet(fullUrl(pensionSummaryUrl(taxYear)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(user.commonExpectedResults.expectedCaption(taxYear))

          "has an payment into pensions section" which {
            linkCheck("Payments into pensions", paymentsIntoPensionsLink, checkPaymentsIntoPensionCyaUrl(taxYear))
            textOnPageCheck(user.commonExpectedResults.updated, summaryListStatusTagSelector(1))
          }

          "has an income from pensions section" which {
            //TODO: Change to use the href below when income From Pensions cya page available
            //linkCheck("Income from pensions", incomeFromPensionsLink, checkIncomeFromPensionCyaUrl(taxYear))
            linkCheck("Income from pensions", incomeFromPensionsLink, "#")
            textOnPageCheck(user.commonExpectedResults.updated, summaryListStatusTagSelector(2))
          }

          "has an pension annual allowance section" which {
            //TODO: Change to use the href below when pension annual allowance cya page available
            //linkCheck("Pension annual allowance", pensionAnnualAllowanceLink, checkPensionAnnualAllowanceCyaUrl(taxYear))
            linkCheck("Pension annual allowance", pensionAnnualAllowanceLink, "#")
            textOnPageCheck(user.commonExpectedResults.toDo, summaryListStatusTagSelector(3))
          }

          "has an pension lifetime allowance section" which {
            //TODO: Change to use the href below when pension lifetime allowance cya page available
            //linkCheck("Pension lifetime allowance", pensionAnnualAllowanceLink, checkPensionLifetimeAllowanceCyaUrl(taxYear))
            linkCheck("Pension lifetime allowance", pensionLifetimeAllowanceLink, "#")
            textOnPageCheck(user.commonExpectedResults.toDo, summaryListStatusTagSelector(4))
          }

          "has an unauthorised payments from pensions section" which {
            //TODO: Change to use the href below when pension lifetime allowance cya page available
            //linkCheck("Unauthorised payments from pensions", unauthorisedPaymentsFromPensionsLink, checkUnauthorisedPaymentsFromPensionsLinkCyaUrl(taxYear))
            linkCheck("Unauthorised payments from pensions", unauthorisedPaymentsFromPensionsLink, "#")
            textOnPageCheck(user.commonExpectedResults.updated, summaryListStatusTagSelector(5))
          }

          "has a payments into overseas pensions section" which {
            //TODO: Change to use the href below when pension lifetime allowance cya page available
            //linkCheck("Payments into overseas pensions", unauthorisedPaymentsFromPensionsLink, checkUnauthorisedPaymentsFromPensionsLinkCyaUrl(taxYear))
            linkCheck("Payments into overseas pensions", paymentsToOverseasPensionsLink, "#")
            textOnPageCheck(user.commonExpectedResults.updated, summaryListStatusTagSelector(6))
          }

          buttonCheck(user.commonExpectedResults.buttonText, buttonSelector)

          welshToggleCheck(user.isWelsh)
        }

        "render the page with prior data but an opposite subset of the underlying pension charges are presenty leading to a mix of 'updated' and To do'" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)

            val otherHalfPensionChargesData: AllPensionsData = anAllPensionsData.copy(pensionCharges =
              Some(anPensionCharges.copy(pensionSchemeUnauthorisedPayments = None, pensionSchemeOverseasTransfers = None, overseasPensionContributions =None)))

            userDataStub(anIncomeTaxUserData.copy(pensions = Some(otherHalfPensionChargesData)), nino, taxYear)

            urlGet(fullUrl(pensionSummaryUrl(taxYear)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1)
          captionCheck(user.commonExpectedResults.expectedCaption(taxYear))

          "has an payment into pensions section" which {
            linkCheck("Payments into pensions", paymentsIntoPensionsLink, checkPaymentsIntoPensionCyaUrl(taxYear))
            textOnPageCheck(user.commonExpectedResults.updated, summaryListStatusTagSelector(1))
          }

          "has an income from pensions section" which {
            //TODO: Change to use the href below when income From Pensions cya page available
            //linkCheck("Income from pensions", incomeFromPensionsLink, checkIncomeFromPensionCyaUrl(taxYear))
            linkCheck("Income from pensions", incomeFromPensionsLink, "#")
            textOnPageCheck(user.commonExpectedResults.updated, summaryListStatusTagSelector(2))
          }

          "has an pension annual allowance section" which {
            //TODO: Change to use the href below when pension annual allowance cya page available
            //linkCheck("Pension annual allowance", pensionAnnualAllowanceLink, checkPensionAnnualAllowanceCyaUrl(taxYear))
            linkCheck("Pension annual allowance", pensionAnnualAllowanceLink, "#")
            textOnPageCheck(user.commonExpectedResults.updated, summaryListStatusTagSelector(3))
          }

          "has an pension lifetime allowance section" which {
            //TODO: Change to use the href below when pension lifetime allowance cya page available
            //linkCheck("Pension lifetime allowance", pensionAnnualAllowanceLink, checkPensionLifetimeAllowanceCyaUrl(taxYear))
            linkCheck("Pension lifetime allowance", pensionLifetimeAllowanceLink, "#")
            textOnPageCheck(user.commonExpectedResults.updated, summaryListStatusTagSelector(4))
          }

          "has an unauthorised payments from pensions section" which {
            //TODO: Change to use the href below when pension lifetime allowance cya page available
            //linkCheck("Unauthorised payments from pensions", unauthorisedPaymentsFromPensionsLink, checkUnauthorisedPaymentsFromPensionsLinkCyaUrl(taxYear))
            linkCheck("Unauthorised payments from pensions", unauthorisedPaymentsFromPensionsLink, "#")
            textOnPageCheck(user.commonExpectedResults.toDo, summaryListStatusTagSelector(5))
          }

          "has a payments into overseas pensions section" which {
            //TODO: Change to use the href below when pension lifetime allowance cya page available
            //linkCheck("Payments into overseas pensions", unauthorisedPaymentsFromPensionsLink, checkUnauthorisedPaymentsFromPensionsLinkCyaUrl(taxYear))
            linkCheck("Payments into overseas pensions", paymentsToOverseasPensionsLink, "#")
            textOnPageCheck(user.commonExpectedResults.toDo, summaryListStatusTagSelector(6))
          }

          buttonCheck(user.commonExpectedResults.buttonText, buttonSelector)

          welshToggleCheck(user.isWelsh)
        }

        "render Unauthorised user error page" which {
          lazy val result: WSResponse = {
            unauthorisedAgentOrIndividual(user.isAgent)
            urlGet(fullUrl(pensionSummaryUrl(taxYear)), welsh = user.isWelsh)
          }
          "has an UNAUTHORIZED(401) status" in {
            result.status shouldBe UNAUTHORIZED
          }
        }

      }
    }
  }

  // scalastyle:on magic.number
}
