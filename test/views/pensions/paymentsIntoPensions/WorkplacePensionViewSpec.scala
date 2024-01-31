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

import builders.PaymentsIntoPensionVewModelBuilder.aPaymentsIntoPensionViewModel
import builders.PensionsCYAModelBuilder.aPensionsCYAModel
import builders.PensionsUserDataBuilder.aPensionsUserData
import controllers.pensions.paymentsIntoPensions.PaymentsIntoPensionFormProvider
import forms.YesNoForm
import models.AuthorisationRequest
import models.mongo.PensionsUserData
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.pensions.paymentsIntoPensions.WorkplacePensionView
import views.pensions.paymentsIntoPensions.WorkplacePensionViewSpec._

// scalastyle:off magic.number
object WorkplacePensionViewSpec {

  val noWorkplaceCYAModel: PensionsUserData = aPensionsUserData.copy(
    pensions = aPensionsCYAModel.copy(
      paymentsIntoPension = aPaymentsIntoPensionViewModel.copy(
        workplacePensionPaymentsQuestion = None
      )))

  object Selectors {
    val captionSelector: String        = "#main-content > div > div > header > p"
    val continueButtonSelector: String = "#continue"
    val formSelector: String           = "#main-content > div > div > form"
    val yesSelector                    = "#value"
    val noSelector                     = "#value-no"
    val h2Selector: String             = s"#main-content > div > div > form > div > fieldset > legend > h2"
    val findOutMoreSelector: String    = s"#findOutMore-link"

    def paragraphSelector(index: Int): String = s"#main-content > div > div > p:nth-of-type($index)"
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val yesText: String
    val noText: String
    val buttonText: String
    val expectedFindOutMoreText: String
  }

  trait SpecificExpectedResults {
    val expectedHeading: String
    val expectedTitle: String
    val expectedInfoText: String
    val expectedTheseCases: String
    val expectedWhereToCheck: String
    val expectedErrorMessage: String
    val expectedErrorTitle: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Payments into pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val yesText                        = "Yes"
    val noText                         = "No"
    val buttonText                     = "Continue"
    val expectedFindOutMoreText        = "Find out more about tax relief (opens in new tab)"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Taliadau i bensiynau ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val yesText                        = "Iawn"
    val noText                         = "Na"
    val buttonText                     = "Yn eich blaen"
    val expectedFindOutMoreText        = "Dysgwch ragor am ryddhad treth (yn agor tab newydd)"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedHeading    = "Did you pay into a workplace pension and not receive tax relief?"
    val expectedTitle      = "Did you pay into a workplace pension and not receive tax relief?"
    val expectedInfoText   = "You would have made your payments after your pay was taxed."
    val expectedTheseCases = "These cases are unusual as most workplace pensions are set up to give you tax relief at the time of your payment."

    val expectedWhereToCheck = "Check with your employer or pension provider which arrangement you have."
    val expectedErrorMessage = "Select yes if you paid into a workplace pension and did not receive tax relief"
    val expectedErrorTitle   = s"Error: $expectedTitle"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedHeading  = "A wnaethoch dalu i mewn i bensiwn gweithle a heb dderbyn rhyddhad treth?"
    val expectedTitle    = "A wnaethoch dalu i mewn i bensiwn gweithle a heb dderbyn rhyddhad treth?"
    val expectedInfoText = "Byddech wedi gwneud eich taliadau ar ôl i’ch cyflog gael ei drethu."
    val expectedTheseCases =
      "Mae’r achosion hyn yn anarferol gan fod y rhan fwyaf o bensiynau gweithle wedi’u sefydlu i roi rhyddhad treth i chi ar adeg eich taliad."
    val expectedWhereToCheck = "Gwiriwch gyda’ch cyflogwr neu ddarparwr pensiwn pa drefniant sydd gennych."
    val expectedErrorMessage = "Dewiswch ‘Iawn’ os gwnaethoch dalu i mewn i bensiwn gweithle ac na chawsoch ryddhad treth"
    val expectedErrorTitle   = s"Gwall: $expectedTitle"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedHeading  = "Did your client pay into a workplace pension and not receive tax relief?"
    val expectedTitle    = "Did your client pay into a workplace pension and not receive tax relief?"
    val expectedInfoText = "Your client would have made their payments after their pay was taxed."
    val expectedTheseCases =
      "These cases are unusual as most workplace pensions are set up to give your client tax relief at the time of their payment."

    val expectedWhereToCheck = "Check with your client’s employer or pension provider which arrangement they have."
    val expectedErrorMessage = "Select yes if your client paid into a workplace pension and did not receive tax relief"
    val expectedErrorTitle   = s"Error: $expectedTitle"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedHeading  = "A wnaeth eich cleient dalu i mewn i bensiwn gweithle a heb dderbyn rhyddhad treth?"
    val expectedTitle    = "A wnaeth eich cleient dalu i mewn i bensiwn gweithle a heb dderbyn rhyddhad treth?"
    val expectedInfoText = "Byddai eich cleient wedi gwneud ei daliadau ar ôl i’w gyflog gael ei drethu."
    val expectedTheseCases =
      "Mae’r achosion hyn yn anarferol gan fod y rhan fwyaf o bensiynau gweithle wedi’u sefydlu i roi rhyddhad treth i’ch cleient ar adeg ei daliad."

    val expectedWhereToCheck = "Gwiriwch gyda chyflogwr neu ddarparwr pensiwn eich cleient pa drefniant sydd ganddo."
    val expectedErrorMessage = "Dewiswch ‘Iawn’ os talodd eich cleient i mewn i bensiwn gweithle ac na chafodd ryddhad treth"
    val expectedErrorTitle   = s"Gwall: $expectedTitle"
  }
}
class WorkplacePensionViewSpec extends ViewUnitTest {

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private def yesNoForm(isAgent: Boolean) = new PaymentsIntoPensionFormProvider().workplacePensionForm(isAgent)

  private lazy val underTest = inject[WorkplacePensionView]

  userScenarios.foreach { userScenario =>
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render the 'Workplace pension and not receive tax relief' question page with no pre-filled radio buttons when no CYA data for this item" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages                            = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(yesNoForm(userScenario.isAgent), taxYearEOY)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        import Selectors._
        import userScenario.commonExpectedResults._

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption(taxYearEOY), captionSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedInfoText, paragraphSelector(1))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedTheseCases, paragraphSelector(2))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedWhereToCheck, paragraphSelector(3))
        textOnPageCheck(expectedFindOutMoreText, findOutMoreSelector)
        radioButtonCheck(yesText, 1, checked = false)
        radioButtonCheck(noText, 2, checked = false)
        buttonCheck(buttonText, continueButtonSelector)
        formPostLinkCheck(workplacePensionUrl(taxYearEOY), formSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render the 'Workplace pension and not receive tax relief' question page with 'Yes' pre-filled when CYA data exists" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages                            = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(yesNoForm(userScenario.isAgent).fill(true), taxYearEOY)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        import Selectors._
        import userScenario.commonExpectedResults._

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption(taxYearEOY), captionSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedInfoText, paragraphSelector(1))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedTheseCases, paragraphSelector(2))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedWhereToCheck, paragraphSelector(3))
        textOnPageCheck(expectedFindOutMoreText, findOutMoreSelector)
        radioButtonCheck(yesText, 1, checked = true)
        radioButtonCheck(noText, 2, checked = false)
        buttonCheck(buttonText, continueButtonSelector)
        formPostLinkCheck(workplacePensionUrl(taxYearEOY), formSelector)
        welshToggleCheck(userScenario.isWelsh)

      }

      "render the 'Workplace pension and not receive tax relief' question page with 'No' pre-filled when CYA data exists" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages                            = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(yesNoForm(userScenario.isAgent).fill(false), taxYearEOY)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        import Selectors._
        import userScenario.commonExpectedResults._

        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption(taxYearEOY), captionSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedInfoText, paragraphSelector(1))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedTheseCases, paragraphSelector(2))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedWhereToCheck, paragraphSelector(3))
        textOnPageCheck(expectedFindOutMoreText, findOutMoreSelector)
        radioButtonCheck(yesText, 1, checked = false)
        radioButtonCheck(noText, 2, checked = true)
        buttonCheck(buttonText, continueButtonSelector)
        formPostLinkCheck(workplacePensionUrl(taxYearEOY), formSelector)
        welshToggleCheck(userScenario.isWelsh)

      }
      "return an error when form is submitted with no entry" which {
        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages                            = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(yesNoForm(userScenario.isAgent).bind(Map(YesNoForm.yesNo -> "")), taxYearEOY)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        import Selectors._
        import userScenario.commonExpectedResults._

        titleCheck(userScenario.specificExpectedResults.get.expectedErrorTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption(taxYearEOY), captionSelector)
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedInfoText, paragraphSelector(1))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedTheseCases, paragraphSelector(2))
        textOnPageCheck(userScenario.specificExpectedResults.get.expectedWhereToCheck, paragraphSelector(3))
        radioButtonCheck(yesText, 1, checked = false)
        radioButtonCheck(noText, 2, checked = false)
        buttonCheck(buttonText, continueButtonSelector)
        formPostLinkCheck(workplacePensionUrl(taxYearEOY), formSelector)
        welshToggleCheck(userScenario.isWelsh)
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedErrorMessage, Selectors.yesSelector)
        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedErrorMessage, Some("value"))

      }
    }
  }
}
// scalastyle:on magic.number
