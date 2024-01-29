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

package views.pensions.paymentsIntoPensions

import controllers.pensions.paymentsIntoPensions.PaymentsIntoPensionFormProvider
import forms.YesNoForm
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.pensions.paymentsIntoPensions.PensionsTaxReliefNotClaimedView

object PensionsTaxReliefNotClaimedSpec {
  object Selectors {
    val captionSelector: String        = "#main-content > div > div > header > p"
    val continueButtonSelector: String = "#continue"
    val formSelector: String           = "#main-content > div > div > form"
    val yesSelector                    = "#value"
    val noSelector                     = "#value-no"
    val h2Selector: String             = s"#main-content > div > div > form > div > fieldset > legend"

    def paragraphSelector(index: Int): String = s"#main-content > div > div > p:nth-of-type($index)"

  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedHeading: String
    val expectedTitle: String
    val expectedErrorTitle: String
    val yesText: String
    val noText: String
    val buttonText: String
  }

  trait SpecificExpectedResults {
    val expectedQuestionsInfoText: String
    val expectedWhereToCheck: String
    val expectedSubHeading: String
    val expectedErrorMessage: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Payments into pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedHeading                = "Pensions where tax relief is not claimed"
    val expectedTitle                  = "Pensions where tax relief is not claimed"
    val expectedErrorTitle             = s"Error: $expectedTitle"
    val yesText                        = "Yes"
    val noText                         = "No"
    val buttonText                     = "Continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Taliadau i bensiynau ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val expectedHeading                = "Pensiynau lle na chaiff rhyddhad treth ei hawlio"
    val expectedTitle                  = "Pensiynau lle na chaiff rhyddhad treth ei hawlio"
    val expectedErrorTitle             = s"Gwall: $expectedTitle"
    val yesText                        = "Iawn"
    val noText                         = "Na"
    val buttonText                     = "Yn eich blaen"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedQuestionsInfoText = "These questions are about pensions you pay into where tax relief is not claimed for you."
    val expectedWhereToCheck      = "You can check your pension statements or contact your pension provider to find the information you need."
    val expectedSubHeading        = "Did you pay into a pension where tax relief was not claimed for you?"
    val expectedErrorMessage      = "Select yes if you paid into a pension where tax relief was not claimed for you"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedQuestionsInfoText =
      "Mae’r cwestiynau hyn yn ymwneud â phensiynau y byddwch yn talu i mewn iddynt, lle na chaiff rhyddhad treth ei hawlio ar eich cyfer."
    val expectedWhereToCheck =
      "Gallwch wirio eich datganiadau pensiwn neu gysylltu â darparwr eich pensiwn i ddod o hyd i’r wybodaeth sydd ei hangen arnoch."
    val expectedSubHeading   = "A wnaethoch dalu i mewn i bensiwn lle na hawliwyd rhyddhad treth ar eich cyfer?"
    val expectedErrorMessage = "Dewiswch ‘Iawn’ os gwnaethoch dalu i mewn i bensiwn lle na hawliwyd rhyddhad treth ar eich cyfer"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedQuestionsInfoText = "These questions are about pensions your client pays into where tax relief is not claimed for them."
    val expectedWhereToCheck =
      "You can check your client’s pension statements or contact your client’s pension provider to find the information you need."
    val expectedSubHeading   = "Did your client pay into a pension where tax relief was not claimed for them?"
    val expectedErrorMessage = "Select yes if your client paid into a pension where tax relief was not claimed for them"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedQuestionsInfoText =
      "Mae’r cwestiynau hyn yn ymwneud â phensiynau y bydd eich cleient yn talu i mewn iddynt, lle na chaiff rhyddhad treth ei hawlio ar ei gyfer."
    val expectedWhereToCheck =
      "Gallwch wirio datganiadau pensiwn eich cleient neu gysylltu â darparwr pensiwn eich cleient i ddod o hyd i’r wybodaeth sydd ei hangen arnoch."
    val expectedSubHeading   = "A wnaeth eich cleient dalu i mewn i bensiwn lle na hawliwyd rhyddhad treth ar ei gyfer?"
    val expectedErrorMessage = "Dewiswch ‘Iawn’ os gwnaeth eich cleient dalu i mewn i bensiwn lle na hawliwyd rhyddhad treth ar ei gyfer"
  }
}

class PensionsTaxReliefNotClaimedSpec extends ViewUnitTest {

  import PensionsTaxReliefNotClaimedSpec._

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private def yesNoForm(isAgent: Boolean) = new PaymentsIntoPensionFormProvider().pensionsTaxReliefNotClaimedForm(isAgent)

  private lazy val underTest = inject[PensionsTaxReliefNotClaimedView]

  ".show" should {
    userScenarios.foreach { userScenario =>
      s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {

        "render the pensions where tax relief is not claimed question page with no pre-filled radio buttons if no CYA question data" which {

          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages                            = getMessages(userScenario.isWelsh)

          val htmlFormat = underTest(yesNoForm(userScenario.isAgent), taxYearEOY)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          import Selectors._
          import userScenario.commonExpectedResults._

          titleCheck(expectedTitle, userScenario.isWelsh)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(userScenario.specificExpectedResults.get.expectedQuestionsInfoText, paragraphSelector(1))
          textOnPageCheck(userScenario.specificExpectedResults.get.expectedWhereToCheck, paragraphSelector(2))
          textOnPageCheck(userScenario.specificExpectedResults.get.expectedSubHeading, h2Selector)
          radioButtonCheck(yesText, 1, checked = false)
          radioButtonCheck(noText, 2, checked = false)
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(pensionTaxReliefNotClaimedUrl(taxYearEOY), formSelector)
          welshToggleCheck(userScenario.isWelsh)
        }

        "render the pensions where tax relief is not claimed question page with 'Yes' pre-filled when CYA data exists" which {

          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages                            = getMessages(userScenario.isWelsh)

          val htmlFormat = underTest(yesNoForm(userScenario.isAgent).fill(true), taxYearEOY)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          import Selectors._
          import userScenario.commonExpectedResults._

          titleCheck(expectedTitle, userScenario.isWelsh)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(userScenario.specificExpectedResults.get.expectedQuestionsInfoText, paragraphSelector(1))
          textOnPageCheck(userScenario.specificExpectedResults.get.expectedWhereToCheck, paragraphSelector(2))
          textOnPageCheck(userScenario.specificExpectedResults.get.expectedSubHeading, h2Selector)
          radioButtonCheck(yesText, 1, checked = true)
          radioButtonCheck(noText, 2, checked = false)
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(pensionTaxReliefNotClaimedUrl(taxYearEOY), formSelector)
          welshToggleCheck(userScenario.isWelsh)

        }

        "render the pensions where tax relief is not claimed question page with 'No' pre-filled when CYA data exists" which {

          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages                            = getMessages(userScenario.isWelsh)

          val htmlFormat = underTest(yesNoForm(userScenario.isAgent).fill(false), taxYearEOY)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          import Selectors._
          import userScenario.commonExpectedResults._

          titleCheck(expectedTitle, userScenario.isWelsh)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(userScenario.specificExpectedResults.get.expectedQuestionsInfoText, paragraphSelector(1))
          textOnPageCheck(userScenario.specificExpectedResults.get.expectedWhereToCheck, paragraphSelector(2))
          textOnPageCheck(userScenario.specificExpectedResults.get.expectedSubHeading, h2Selector)
          radioButtonCheck(yesText, 1, checked = false)
          radioButtonCheck(noText, 2, checked = true)
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(pensionTaxReliefNotClaimedUrl(taxYearEOY), formSelector)
          welshToggleCheck(userScenario.isWelsh)
        }

        "return an error when form is submitted with no entry" which {
          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
          implicit val messages: Messages                            = getMessages(userScenario.isWelsh)
          val htmlFormat = underTest(yesNoForm(userScenario.isAgent).bind(Map(YesNoForm.yesNo -> "")), taxYearEOY)

          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          import Selectors._
          import userScenario.commonExpectedResults._

          titleCheck(expectedErrorTitle, userScenario.isWelsh)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(userScenario.specificExpectedResults.get.expectedQuestionsInfoText, paragraphSelector(1))
          textOnPageCheck(userScenario.specificExpectedResults.get.expectedWhereToCheck, paragraphSelector(2))
          textOnPageCheck(userScenario.specificExpectedResults.get.expectedSubHeading, h2Selector)
          radioButtonCheck(yesText, 1, checked = false)
          radioButtonCheck(noText, 2, checked = false)
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(pensionTaxReliefNotClaimedUrl(taxYearEOY), formSelector)
          welshToggleCheck(userScenario.isWelsh)
          errorSummaryCheck(userScenario.specificExpectedResults.get.expectedErrorMessage, Selectors.yesSelector)
          errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedErrorMessage, Some("value"))

        }
      }
    }
  }
}
