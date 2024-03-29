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

import builders.AllPensionsDataBuilder.{anAllPensionDataEmpty, anAllPensionsData}
import builders.IncomeFromOverseasPensionsViewModelBuilder.anIncomeFromOverseasPensionsViewModel
import builders.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import builders.PaymentsIntoOverseasPensionsViewModelBuilder.aPaymentsIntoOverseasPensionsViewModel
import builders.PensionsCYAModelBuilder.aPensionsCYAModel
import builders.PensionsUserDataBuilder.{aPensionsUserData, anPensionsUserDataEmptyCya}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.Status.UNAUTHORIZED
import play.api.libs.ws.WSResponse
import utils.CommonUtils
import utils.PageUrls.IncomeFromOverseasPensionsPages.{checkIncomeFromOverseasPensionsCyaUrl, incomeFromOverseasPensionsStatus}
import utils.PageUrls.PaymentIntoOverseasPensions.{paymentsIntoOverseasPensionsCyaUrl, paymentsIntoPensionSchemeUrl}
import utils.PageUrls._
import utils.PageUrls.ShortServiceRefunds.{shortServiceRefundsCYAUrl, shortServiceTaxableRefundUrl}
import utils.PageUrls.TransferIntoOverseasPensions.{checkYourDetailsPensionUrl, transferPensionSavingsUrl}

class OverseasPensionsSummaryControllerISpec extends CommonUtils with BeforeAndAfterEach { // scalastyle:off magic.number

  implicit val overseasPensionSummaryUrl: Int => String = overseasPensionsSummaryUrl

  object Selectors {
    val paymentsIntoOverseasPensionsLink = "#payments-into-overseas-pensions-link"
    val incomeFromOverseasPensionsLink   = "#income-from-overseas-pensions-link"
    val overseasTransferChargesLink      = "#overseas-transfer-charges-link"
    val shortServiceRefundsLink          = "#short-service-refunds-link"
    val insetTextSelector                = "#main-content > div > div > div.govuk-inset-text"
    val buttonSelector                   = "#returnToOverviewPageBtn"

    def summaryListStatusTagSelector(index: Int): String =
      s"#overseas-pensions-summary > dl > div:nth-child($index) > dd > strong"
    def paragraphSelector(index: Int): String = s"#main-content > div > div > p:nth-of-type($index)"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    lazy val expectedH1 = expectedTitle
    val expectedSectionsToFill: String
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val buttonText: String
    val updated: String
    val notStarted: String
    val paymentsToOverseasPensionsText: String
    val incomeFromOverseasPensionsText: String
    val overseasTransferChargesText: String
    val shortServiceRefundsText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val buttonText                     = "Return to overview"
    val updated                        = "Updated"
    val notStarted                     = "Not Started"
    val paymentsToOverseasPensionsText = "Payments into overseas pensions"
    val incomeFromOverseasPensionsText = "Income from overseas pensions"
    val overseasTransferChargesText    = "Overseas transfer charges"
    val shortServiceRefundsText        = "Short service refunds"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Pensiynau tramor ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val buttonText                     = "Yn ôl i’r trosolwg"
    val updated                        = "Wedi diweddaru"
    val notStarted                     = "Heb ddechrau"
    val paymentsToOverseasPensionsText = "Taliadau i bensiynau tramor"
    val incomeFromOverseasPensionsText = "Incwm o bensiynau tramor"
    val overseasTransferChargesText    = "Ffioedd ar drosglwyddiadau tramor"
    val shortServiceRefundsText        = "Ad-daliadau am wasanaeth byr"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle          = "Overseas pensions"
    val expectedSectionsToFill = "You only need to fill in the sections that apply to you."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle          = "Overseas pensions"
    val expectedSectionsToFill = "You only need to fill in the sections that apply to your client."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle: String  = "Pensiynau tramor"
    val expectedSectionsToFill = "Dim ond yr adrannau sy’n berthnasol i chi y mae angen i chi eu llenwi."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle: String  = "Pensiynau tramor"
    val expectedSectionsToFill = "Dim ond yr adrannau sy’n berthnasol i’ch cleient y mae angen i chi eu llenwi."
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  ".show" when {
    import Selectors._
    userScenarios.foreach { userScenario =>
      val common   = userScenario.commonExpectedResults
      val specific = userScenario.specificExpectedResults.get
      s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
        "render the page where data does not exist and everything is 'Not Updated'" which {
          implicit lazy val result: WSResponse =
            showPage(userScenario, anPensionsUserDataEmptyCya, anIncomeTaxUserData.copy(pensions = Some(anAllPensionDataEmpty)))

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(specific.expectedTitle, userScenario.isWelsh)
          h1Check(specific.expectedH1)
          captionCheck(common.expectedCaption(taxYearEOY))
          textOnPageCheck(specific.expectedSectionsToFill, paragraphSelector(1))

          "has a payment into overseas pensions section" which {
            linkCheck(common.paymentsToOverseasPensionsText, paymentsIntoOverseasPensionsLink, paymentsIntoPensionSchemeUrl(taxYearEOY))
            textOnPageCheck(userScenario.commonExpectedResults.notStarted, summaryListStatusTagSelector(1))
          }

          "has an income from overseas pensions section" which {
            linkCheck(common.incomeFromOverseasPensionsText, incomeFromOverseasPensionsLink, incomeFromOverseasPensionsStatus(taxYearEOY))
            textOnPageCheck(userScenario.commonExpectedResults.notStarted, summaryListStatusTagSelector(2))
          }

          "has an overseas transfer charges section" which {
            linkCheck(common.overseasTransferChargesText, overseasTransferChargesLink, transferPensionSavingsUrl(taxYearEOY))
            textOnPageCheck(userScenario.commonExpectedResults.notStarted, summaryListStatusTagSelector(3))
          }

          "has a short service refunds section" which {
            linkCheck(common.shortServiceRefundsText, shortServiceRefundsLink, shortServiceTaxableRefundUrl(taxYearEOY))
            textOnPageCheck(userScenario.commonExpectedResults.notStarted, summaryListStatusTagSelector(4))
          }

          buttonCheck(userScenario.commonExpectedResults.buttonText, buttonSelector)

          welshToggleCheck(userScenario.isWelsh)
        }

        "render the page where data exists for payments into pensions and income for overseas pensions to be 'Updated'" which {
          val userData = aPensionsUserData.copy(
            isPriorSubmission = true,
            pensions = aPensionsCYAModel.copy(
              paymentsIntoOverseasPensions = aPaymentsIntoOverseasPensionsViewModel,
              incomeFromOverseasPensions = anIncomeFromOverseasPensionsViewModel)
          )
          val incomeTaxUserData = anIncomeTaxUserData.copy(pensions = Some(anAllPensionsData))

          implicit lazy val result: WSResponse = showPage(userScenario, userData, incomeTaxUserData)

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(specific.expectedTitle, userScenario.isWelsh)
          h1Check(specific.expectedH1)
          captionCheck(common.expectedCaption(taxYearEOY))
          textOnPageCheck(specific.expectedSectionsToFill, paragraphSelector(1))

          "has a payment into overseas pensions section" which {
            linkCheck(common.paymentsToOverseasPensionsText, paymentsIntoOverseasPensionsLink, paymentsIntoOverseasPensionsCyaUrl(taxYearEOY))
            textOnPageCheck(userScenario.commonExpectedResults.updated, summaryListStatusTagSelector(1))
          }

          "has an income from overseas pensions section" which {
            linkCheck(common.incomeFromOverseasPensionsText, incomeFromOverseasPensionsLink, checkIncomeFromOverseasPensionsCyaUrl(taxYearEOY))
            textOnPageCheck(userScenario.commonExpectedResults.updated, summaryListStatusTagSelector(2))
          }

          "has an overseas transfer charges section" which {
            linkCheck(common.overseasTransferChargesText, overseasTransferChargesLink, checkYourDetailsPensionUrl(taxYearEOY))
            textOnPageCheck(userScenario.commonExpectedResults.updated, summaryListStatusTagSelector(3))
          }

          "has a short service refunds section" which {
            linkCheck(common.shortServiceRefundsText, shortServiceRefundsLink, shortServiceRefundsCYAUrl(taxYearEOY))
            textOnPageCheck(userScenario.commonExpectedResults.updated, summaryListStatusTagSelector(4))
          }

          buttonCheck(userScenario.commonExpectedResults.buttonText, buttonSelector)

          welshToggleCheck(userScenario.isWelsh)
        }

        "render Unauthorised user error page" which {
          lazy val result: WSResponse = showUnauthorisedPage(userScenario)

          "has an UNAUTHORIZED(401) status" in {
            result.status shouldBe UNAUTHORIZED
          }
        }

      }
    }
  }

  // scalastyle:on magic.number
}
