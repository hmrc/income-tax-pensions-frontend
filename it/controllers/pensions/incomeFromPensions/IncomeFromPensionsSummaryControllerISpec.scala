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

package controllers.pensions.incomeFromPensions

import builders.IncomeFromPensionsViewModelBuilder.{aUKIncomeFromPensionsViewModel, anIncomeFromPensionsViewModel}
import builders.IncomeTaxUserDataBuilder.{anIncomeTaxUserData, anIncomeTaxUserDataEmpty}
import builders.PensionsUserDataBuilder.pensionsUserDataWithIncomeFromPensions
import builders.StateBenefitViewModelBuilder.anStateBenefitViewModelOne
import models.pension.Journey
import models.pension.Journey.{StatePension, UkPensionIncome}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.Status.OK
import play.api.libs.ws.WSResponse
import utils.CommonUtils
import utils.PageUrls.IncomeFromPensionsPages.pensionIncomeSummaryUrl

class IncomeFromPensionsSummaryControllerISpec extends CommonUtils with BeforeAndAfterEach {

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val notStartedText: String
    val inProgress: String
    val expectedTitle: String
    val statePensionLinkText: String
    val otherUKPensionsLinktext: String
    val expectedHeading: String
    val buttonText: String
  }

  trait SpecificExpectedResults {
    val expectedParagraph: String
  }

  def statePensionsLink(taxYear: Int = taxYear): String   = s"/update-and-submit-income-tax-return/pensions/$taxYear/pension-income/state-pension"
  def otherUkPensionsLink(taxYear: Int = taxYear): String = s"/update-and-submit-income-tax-return/pensions/$taxYear/pension-income/state-pension"

  object Selectors {
    val captionSelector: String        = "#main-content > div > div > header > p"
    val continueButtonSelector: String = "#continue"
    val formSelector: String           = "#main-content > div > div > form"
    val hintTextSelector               = "#amount-hint"
    val poundPrefixSelector            = ".govuk-input__prefix"
    val inputSelector                  = "#amount"
    val expectedErrorHref              = "#amount"

    def summaryListLinkSelector(index: Int): String            = s"#main-content > div > div > div > ul > li:nth-child($index) > span > a"
    def summaryListStatusTagSelector(journey: Journey): String = s"#journey-${journey.toString}-status"

    def paragraphSelector(index: Int): String = s"#main-content > div > div > p:nth-of-type($index)"
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedTitle                  = "Income from pensions"
    val notStartedText                 = "Not Started"
    val inProgress                     = "In Progress"
    val statePensionLinkText           = "State pension"
    val otherUKPensionsLinktext        = "Other UK pensions"
    val expectedHeading                = "Income from pensions"
    val buttonText                     = "Return to overview"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Pensiynau ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val expectedTitle                  = "Incwm o bensiynau"
    val notStartedText                 = "Heb ddechrau"
    val inProgress                     = "Ar y gweill"
    val statePensionLinkText           = "State pension"
    val otherUKPensionsLinktext        = "Other UK pensions"
    val expectedHeading                = expectedTitle
    val buttonText                     = "Yn ôl i’r trosolwg"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedParagraph = "You only need to fill in the sections that apply to you."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedParagraph = "Dim ond yr adrannau sy’n berthnasol i chi y mae angen i chi eu llenwi."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedParagraph = "You only need to fill in the sections that apply to your client."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedParagraph = "Dim ond yr adrannau sy’n berthnasol i’ch cleient y mae angen i chi eu llenwi."
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] =
    Seq(
      UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
    )

  private implicit val url: Int => String = pensionIncomeSummaryUrl

  ".show" should {
    userScenarios.foreach { user =>
      import Selectors._
      import user.commonExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        "render page when both values are in progress " which {

          val viewModel = anIncomeFromPensionsViewModel
          lazy val result: WSResponse =
            showPage(user, pensionsUserDataWithIncomeFromPensions(viewModel, isPriorSubmission = false), anIncomeTaxUserData)
          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(expectedTitle, user.isWelsh)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph, paragraphSelector(1))

          buttonCheck(buttonText, continueButtonSelector)
          welshToggleCheck(user.isWelsh)

          "has a State Pension section" which {
            textOnPageCheck(inProgress, summaryListStatusTagSelector(StatePension))
          }

          "has a Other UK pensions section" which {
            textOnPageCheck(inProgress, summaryListStatusTagSelector(UkPensionIncome))
          }
        }

        "render page when StatePension is not started " which {
          val viewModel = aUKIncomeFromPensionsViewModel

          lazy val result: WSResponse =
            showPage(user, pensionsUserDataWithIncomeFromPensions(viewModel, isPriorSubmission = false), anIncomeTaxUserDataEmpty)

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(expectedTitle, user.isWelsh)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph, paragraphSelector(1))

          buttonCheck(buttonText, continueButtonSelector)
          welshToggleCheck(user.isWelsh)

          "has a State Pension section" which {
            textOnPageCheck(notStartedText, summaryListStatusTagSelector(StatePension))
          }

          "has a Other UK pensions section" which {
            textOnPageCheck(inProgress, summaryListStatusTagSelector(UkPensionIncome))
          }
        }

        "render page when Other UK Pensions are not started " which {

          val viewModel = anIncomeFromPensionsViewModel.copy(uKPensionIncomesQuestion = None)
          insertCyaData(pensionsUserDataWithIncomeFromPensions(viewModel, isPriorSubmission = false))
          lazy val result: WSResponse =
            showPage(user, pensionsUserDataWithIncomeFromPensions(viewModel, isPriorSubmission = false), anIncomeTaxUserDataEmpty)

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(expectedTitle, user.isWelsh)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph, paragraphSelector(1))

          buttonCheck(buttonText, continueButtonSelector)
          welshToggleCheck(user.isWelsh)

          "has a State Pension section" which {
            textOnPageCheck(inProgress, summaryListStatusTagSelector(StatePension))
          }

          "has a Other UK pensions section" which {
            textOnPageCheck(notStartedText, summaryListStatusTagSelector(UkPensionIncome))
          }
        }

        "render page when other UK pension are started and the user selected no" which {

          val anUpdatedStatePensionModel = anStateBenefitViewModelOne.copy(amountPaidQuestion = Some(false))
          val viewModel = anIncomeFromPensionsViewModel.copy(statePension = Some(anUpdatedStatePensionModel), uKPensionIncomesQuestion = Some(false))
          lazy val result: WSResponse =
            showPage(user, pensionsUserDataWithIncomeFromPensions(viewModel, isPriorSubmission = false), anIncomeTaxUserData)

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(expectedTitle, user.isWelsh)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph, paragraphSelector(1))

          buttonCheck(buttonText, continueButtonSelector)
          welshToggleCheck(user.isWelsh)

          "has a State Pension section" which {
            textOnPageCheck(inProgress, summaryListStatusTagSelector(StatePension))
          }

          "has a Other UK pensions section" which {
            textOnPageCheck(inProgress, summaryListStatusTagSelector(UkPensionIncome))
          }
        }

        "render page when there is no session and prior data" which {
          lazy val result: WSResponse = showPageNoData(user)

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(expectedTitle, user.isWelsh)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph, paragraphSelector(1))

          buttonCheck(buttonText, continueButtonSelector)
          welshToggleCheck(user.isWelsh)

          "has a State Pension section" which {
            textOnPageCheck(notStartedText, summaryListStatusTagSelector(StatePension))
          }

          "has a Other UK pensions section" which {
            textOnPageCheck(notStartedText, summaryListStatusTagSelector(UkPensionIncome))
          }
        }
      }
    }
  }
}
