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

package controllers.pensions

import builders.AllPensionsDataBuilder.anAllPensionsData
import builders.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import builders.PensionChargesBuilder.anPensionCharges
import builders.PensionsUserDataBuilder.anPensionsUserDataEmptyCya
import builders.StateBenefitsModelBuilder.aStateBenefitsModel
import builders.UserBuilder
import models.User
import models.pension.Journey
import models.pension.Journey.{AnnualAllowances, IncomeFromPensionsSummary, OverseasPensionsSummary, PaymentsIntoPensions, UnauthorisedPayments}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.UNAUTHORIZED
import play.api.libs.ws.WSResponse
import play.api.test.Injecting
import services.PensionSessionService
import utils.CommonUtils
import utils.PageUrls.IncomeFromPensionsPages.pensionIncomeSummaryUrl
import utils.PageUrls.OverseasPensions.overseasPensionsUrl
import utils.PageUrls.PaymentIntoPensions.{checkPaymentsIntoPensionCyaUrl, reliefAtSourcePensionsUrl}
import utils.PageUrls.PensionAnnualAllowancePages.{annualAllowancesCYAUrl, reducedAnnualAllowanceUrl}
import utils.PageUrls.UnauthorisedPaymentsPages.{checkUnauthorisedPaymentsCyaUrl, unauthorisedPaymentsUrl}
import utils.PageUrls._

class PensionsSummaryControllerISpec extends CommonUtils with BeforeAndAfterEach with Injecting {

  object Selectors {
    val insetTextSelector = "#main-content > div > div > div.govuk-inset-text"
    val buttonSelector    = "#returnToOverviewPageBtn"

    def summaryListLinkSelector(index: Int): String            = s"#main-content > div > div > div > ul > li:nth-child($index) > span > a"
    def summaryListStatusTagSelector(journey: Journey): String = s"#journey-${journey.toString}-status"
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedTitle: String
    lazy val expectedH1 = expectedTitle
    val paymentsLinkText: String
    val incomeLinkText: String
    val unauthLinkText: String
    val overseasPensionsLinkText: String
    val annualAllowance: String
    val buttonText: String
    val inProgress: String
    val completed: String
    val cannotUpdate: String
    val notStarted: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedTitle                  = "Pensions"
    val paymentsLinkText               = "Payments into pensions"
    val incomeLinkText                 = "Income from pensions"
    val unauthLinkText                 = "Unauthorised payments from pensions"
    val overseasPensionsLinkText       = "Overseas pensions"
    val annualAllowance                = "Pension annual allowance"
    val buttonText                     = "Return to overview"
    val inProgress                     = "In Progress"
    val completed                      = "Completed"
    val cannotUpdate                   = "Cannot update"
    val notStarted                     = "Not Started"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Pensiynau ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val expectedTitle: String          = "Pensiynau"
    val paymentsLinkText               = "Taliadau i bensiynau"
    val incomeLinkText                 = "Incwm o bensiynau"
    val unauthLinkText                 = "Taliadau heb awdurdod o bensiynau"
    val overseasPensionsLinkText       = "Pensiynau tramor"
    val annualAllowance                = "Lwfans blynyddol pensiwn"
    val buttonText                     = "Yn ôl i’r trosolwg"
    val inProgress                     = "Ar y gweill"
    val completed                      = "Cwblhawyd"
    val cannotUpdate                   = "Cannot update"
    val notStarted                     = "Heb ddechrau"
    val paymentsToOverseasPensionsText = "Taliadau i bensiynau tramor"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, Nothing]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY)
  )

  lazy val pensionSessionService: PensionSessionService = inject[PensionSessionService]

  implicit val penSumUrl: Int => String = pensionSummaryUrl

  ".show" when {

    userScenarios.foreach { userScenario =>
      s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
        import Selectors._
        import userScenario.commonExpectedResults._

        "render the page where minimal prior data exists for all statuses to be all 'In Progress'" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(userScenario.isAgent)

            // if customerAddedStateBenefits and pensionSchemeOverseasTransfers empty still 'In Progress' if
            // stateBenefits and overseasPensionContributions present
            val userData = anIncomeTaxUserData.copy(pensions = Some(anAllPensionsData.copy(
              stateBenefits = Some(aStateBenefitsModel.copy(customerAddedStateBenefitsData = None)),
              pensionCharges = Some(anPensionCharges.copy(pensionSchemeOverseasTransfers = None))
            )))
            showPage(userScenario, anPensionsUserDataEmptyCya, userData)
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          checkCommonBehaviour(userScenario)

          "has an payment into pensions section" which {
            linkCheck(paymentsLinkText, summaryListLinkSelector(1), checkPaymentsIntoPensionCyaUrl(taxYearEOY))
            textOnPageCheck(inProgress, summaryListStatusTagSelector(PaymentsIntoPensions))
          }

          "has an income from pensions section" which {
            linkCheck(incomeLinkText, summaryListLinkSelector(2), pensionIncomeSummaryUrl(taxYearEOY))
            textOnPageCheck(inProgress, summaryListStatusTagSelector(IncomeFromPensionsSummary))
          }

          "has an pension annual allowance section" which {
            linkCheck(annualAllowance, summaryListLinkSelector(3), annualAllowancesCYAUrl(taxYearEOY))
            textOnPageCheck(inProgress, summaryListStatusTagSelector(AnnualAllowances))
          }

          "has an unauthorised payments from pensions section" which {
            linkCheck(unauthLinkText, summaryListLinkSelector(4), checkUnauthorisedPaymentsCyaUrl(taxYearEOY))
            textOnPageCheck(inProgress, summaryListStatusTagSelector(UnauthorisedPayments))
          }

          "has an overseas pensions section" which {
            linkCheck(overseasPensionsLinkText, summaryListLinkSelector(5), overseasPensionsUrl(taxYearEOY))
            textOnPageCheck(inProgress, summaryListStatusTagSelector(OverseasPensionsSummary))
          }

          buttonCheck(buttonText, buttonSelector)
          welshToggleCheck(userScenario.isWelsh)
        }

        "render the page where alternate minimal prior data exists for all statuses to be all 'In Progress'" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(userScenario.isAgent)

            // if stateBenefits and overseasPensionContributions empty still 'In Progress' if
            // customerAddedStateBenefits and pensionSchemeOverseasTransfers present
            val userData = anIncomeTaxUserData.copy(pensions = Some(
              anAllPensionsData.copy(
                stateBenefits = Some(aStateBenefitsModel.copy(stateBenefitsData = None)),
                pensionCharges = Some(anPensionCharges.copy(overseasPensionContributions = None)))))
            showPage(userScenario, anPensionsUserDataEmptyCya, userData)
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)
          checkCommonBehaviour(userScenario)

          "has an payment into pensions section" which {
            linkCheck(paymentsLinkText, summaryListLinkSelector(1), checkPaymentsIntoPensionCyaUrl(taxYearEOY))
            textOnPageCheck(inProgress, summaryListStatusTagSelector(PaymentsIntoPensions))
          }

          "has an income from pensions section" which {
            // TODO: Change to use the href below when income From Pensions cya page available
            // linkCheck(incomeLinkText, summaryListLinkSelector(2), checkIncomeFromPensionCyaUrl(taxYearEOY))
            linkCheck(incomeLinkText, summaryListLinkSelector(2), pensionIncomeSummaryUrl(taxYearEOY))
            textOnPageCheck(inProgress, summaryListStatusTagSelector(IncomeFromPensionsSummary))
          }

          "has an pension annual allowance section" which {
            linkCheck(annualAllowance, summaryListLinkSelector(3), annualAllowancesCYAUrl(taxYearEOY))
            textOnPageCheck(inProgress, summaryListStatusTagSelector(AnnualAllowances))
          }

          "has an unauthorised payments from pensions section" which {
            linkCheck(unauthLinkText, summaryListLinkSelector(4), checkUnauthorisedPaymentsCyaUrl(taxYearEOY))
            textOnPageCheck(inProgress, summaryListStatusTagSelector(UnauthorisedPayments))
          }

          "has an overseas pensions section" which {
            linkCheck(overseasPensionsLinkText, summaryListLinkSelector(5), overseasPensionsUrl(taxYearEOY))
            textOnPageCheck(inProgress, summaryListStatusTagSelector(OverseasPensionsSummary))
          }

          buttonCheck(buttonText, buttonSelector)
          welshToggleCheck(userScenario.isWelsh)
        }

        "render the page where no prior data exists for all pension data so the statuses are all 'Not Started'" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(userScenario.isAgent)
            emptyUserDataStub(nino, taxYearEOY)
            dropPensionsDB()
            urlGet(
              fullUrl(pensionSummaryUrl(taxYearEOY)),
              welsh = userScenario.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)
          checkCommonBehaviour(userScenario)

          "has an payment into pensions section" which {
            linkCheck(paymentsLinkText, summaryListLinkSelector(1), reliefAtSourcePensionsUrl(taxYearEOY))
            textOnPageCheck(notStarted, summaryListStatusTagSelector(PaymentsIntoPensions))
          }

          "has an income from pensions section" which {
            linkCheck(incomeLinkText, summaryListLinkSelector(2), pensionIncomeSummaryUrl(taxYearEOY))
            textOnPageCheck(notStarted, summaryListStatusTagSelector(IncomeFromPensionsSummary))
          }

          "has an pension annual allowance section" which {
            linkCheck(annualAllowance, summaryListLinkSelector(3), reducedAnnualAllowanceUrl(taxYearEOY))
            textOnPageCheck(notStarted, summaryListStatusTagSelector(AnnualAllowances))
          }

          "has an unauthorised payments from pensions section" which {
            linkCheck(unauthLinkText, summaryListLinkSelector(4), unauthorisedPaymentsUrl(taxYearEOY))
            textOnPageCheck(notStarted, summaryListStatusTagSelector(UnauthorisedPayments))
          }

          "has an overseas pensions section" which {
            linkCheck(overseasPensionsLinkText, summaryListLinkSelector(5), overseasPensionsUrl(taxYearEOY))
            textOnPageCheck(notStarted, summaryListStatusTagSelector(OverseasPensionsSummary))
          }
          buttonCheck(buttonText, buttonSelector)

          welshToggleCheck(userScenario.isWelsh)
        }

        "render the page with prior data but only a subset of the underlying pension charges are present leading to a mix of 'In Progress' and 'Not Started'" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(userScenario.isAgent)

            val userData = anIncomeTaxUserData.copy(pensions = Some(
              anAllPensionsData.copy(pensionCharges = Some(anPensionCharges.copy(pensionSavingsTaxCharges = None, pensionContributions = None)))))
            showPage(userScenario, anPensionsUserDataEmptyCya, userData)
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)
          checkCommonBehaviour(userScenario)

          "has an payment into pensions section" which {
            linkCheck(paymentsLinkText, summaryListLinkSelector(1), checkPaymentsIntoPensionCyaUrl(taxYearEOY))
            textOnPageCheck(inProgress, summaryListStatusTagSelector(PaymentsIntoPensions))
          }

          "has an income from pensions section" which {
            linkCheck(incomeLinkText, summaryListLinkSelector(2), pensionIncomeSummaryUrl(taxYearEOY))
            textOnPageCheck(inProgress, summaryListStatusTagSelector(IncomeFromPensionsSummary))
          }

          "has an pension annual allowance section" which {
            linkCheck(annualAllowance, summaryListLinkSelector(3), reducedAnnualAllowanceUrl(taxYearEOY))
            textOnPageCheck(notStarted, summaryListStatusTagSelector(AnnualAllowances))
          }

          "has an unauthorised payments from pensions section" which {
            linkCheck(unauthLinkText, summaryListLinkSelector(4), checkUnauthorisedPaymentsCyaUrl(taxYearEOY))
            textOnPageCheck(inProgress, summaryListStatusTagSelector(UnauthorisedPayments))
          }

          "has an overseas pensions section" which {
            linkCheck(overseasPensionsLinkText, summaryListLinkSelector(5), overseasPensionsUrl(taxYearEOY))
            textOnPageCheck(inProgress, summaryListStatusTagSelector(OverseasPensionsSummary))
          }

          buttonCheck(buttonText, buttonSelector)

          welshToggleCheck(userScenario.isWelsh)
        }

        "render the page with prior data but an opposite subset of the underlying pension charges are leading to a mix of 'In Progress' and 'Not Started'" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(userScenario.isAgent)

            val userData = anIncomeTaxUserData.copy(pensions = Some(
              anAllPensionsData.copy(
                pensionCharges = Some(anPensionCharges
                  .copy(pensionSchemeUnauthorisedPayments = None, pensionSchemeOverseasTransfers = None, overseasPensionContributions = None))
              )))
            showPage(userScenario, anPensionsUserDataEmptyCya, userData)
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)
          checkCommonBehaviour(userScenario)

          "has an payment into pensions section" which {
            linkCheck(paymentsLinkText, summaryListLinkSelector(1), checkPaymentsIntoPensionCyaUrl(taxYearEOY))
            textOnPageCheck(inProgress, summaryListStatusTagSelector(PaymentsIntoPensions))
          }

          "has an income from pensions section" which {
            linkCheck(incomeLinkText, summaryListLinkSelector(2), pensionIncomeSummaryUrl(taxYearEOY))
            textOnPageCheck(inProgress, summaryListStatusTagSelector(IncomeFromPensionsSummary))
          }

          "has an pension annual allowance section" which {
            linkCheck(annualAllowance, summaryListLinkSelector(3), annualAllowancesCYAUrl(taxYearEOY))
            textOnPageCheck(inProgress, summaryListStatusTagSelector(AnnualAllowances))
          }

          "has an unauthorised payments from pensions section" which {
            linkCheck(unauthLinkText, summaryListLinkSelector(4), unauthorisedPaymentsUrl(taxYearEOY))
            textOnPageCheck(notStarted, summaryListStatusTagSelector(UnauthorisedPayments))
          }

          "has an overseas pensions section" which {
            linkCheck(overseasPensionsLinkText, summaryListLinkSelector(5), overseasPensionsUrl(taxYearEOY))
            textOnPageCheck(inProgress, summaryListStatusTagSelector(OverseasPensionsSummary))
          }

          buttonCheck(buttonText, buttonSelector)

          welshToggleCheck(userScenario.isWelsh)
        }

        "render Unauthorised user error page" which {
          lazy val result: WSResponse = {
            unauthorisedAgentOrIndividual(userScenario.isAgent)
            urlGet(
              fullUrl(pensionSummaryUrl(taxYearEOY)),
              welsh = userScenario.isWelsh,
              headers = Seq(Predef.ArrowAssoc(HeaderNames.COOKIE) -> playSessionCookies(taxYearEOY, validTaxYearList))
            )
          }
          "has an UNAUTHORIZED(401) status" in {
            result.status shouldBe UNAUTHORIZED
          }
        }
      }
    }
  }

  private def checkCommonBehaviour(userScenario: UserScenario[CommonExpectedResults, Nothing])(implicit document: () => Document): Unit = {
    import userScenario.commonExpectedResults._
    titleCheck(expectedTitle, userScenario.isWelsh)
    h1Check(expectedH1)
    captionCheck(expectedCaption(taxYearEOY))
    checkPensionsUserDataIsInitialised(userScenario.isAgent)
  }

  private def checkPensionsUserDataIsInitialised(isAgent: Boolean): Unit =
    s"have pensions user data initialised" in {
      val user: User = if (isAgent) UserBuilder.anAgentUser else UserBuilder.aUser
      await(pensionSessionService.loadSessionData(taxYearEOY, user)) match {
        case Right(optPensionsUserData) =>
          optPensionsUserData.nonEmpty shouldBe true
        case Left(_) => fail("An unexpected failure to retrieve  user session data")
      }
    }
}
