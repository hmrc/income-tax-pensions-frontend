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

package views.pensions.transferIntoOverseasPensions

import forms.TransferPensionSavingsForm.yesNoForm
import forms.YesNoForm
import models.requests.UserSessionDataRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.pensions.transferIntoOverseasPensions.TransferPensionSavingsView

class TransferPensionSavingsViewSpec extends ViewUnitTest {

  object Selectors {
    val captionSelector: String        = "#main-content > div > div > header > p"
    val yesSelector                    = "#value"
    val noSelector                     = "#value-no"
    val continueButtonSelector: String = "#continue"
  }

  trait ExpectedContents {
    val expectedCaption: Int => String
    val expectedTitle: String
    val expectedErrorTitle: String
    val errorMessage: String
    val yesText: String
    val noText: String
    val buttonText: String
  }

  object ExpectedContentsIndividualEN extends ExpectedContents {
    val expectedCaption: Int => String = (taxYear: Int) => s"Transfers into overseas pensions for 6 April ${taxYear - 1} to 5 April ${taxYear}"
    val expectedTitle                  = "Did you transfer pension savings into an overseas pension scheme?"
    val expectedErrorTitle             = s"Error: $expectedTitle"
    val errorMessage                   = "Select yes if you transferred savings into an overseas pension scheme"
    val yesText                        = "Yes"
    val noText                         = "No"
    val buttonText                     = "Continue"
  }

  object ExpectedContentsAgentEN extends ExpectedContents {
    val expectedCaption: Int => String = (taxYear: Int) => s"Transfers into overseas pensions for 6 April ${taxYear - 1} to 5 April ${taxYear}"
    val expectedTitle                  = "Did your client transfer pension savings into an overseas pension scheme?"
    val expectedErrorTitle             = s"Error: $expectedTitle"
    val errorMessage                   = "Select yes if your client transferred savings into an overseas pension scheme"
    val yesText                        = "Yes"
    val noText                         = "No"
    val buttonText                     = "Continue"
  }

  object ExpectedContentsIndividualCY extends ExpectedContents {
    val expectedCaption: Int => String = (taxYear: Int) =>
      s"Trosglwyddiadau i bensiynau tramor ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill ${taxYear}"
    val expectedTitle      = "A wnaethoch drosglwyddo cynilion pensiwn i gynllun pensiwn tramor?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val errorMessage       = "Dewiswch ‘Iawn’ os gwnaethoch drosglwyddo cynilion i gynllun pensiwn tramor"
    val yesText            = "Iawn"
    val noText             = "Na"
    val buttonText         = "Yn eich blaen"
  }

  object ExpectedContentsAgentCY extends ExpectedContents {
    val expectedCaption: Int => String = (taxYear: Int) =>
      s"Trosglwyddiadau i bensiynau tramor ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill ${taxYear}"
    val expectedTitle      = "A wnaeth eich cleient drosglwyddo cynilion pensiwn i gynllun pensiwn tramor?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val errorMessage       = "Dewiswch ‘Iawn’ os gwnaeth eich cleient drosglwyddo cynilion i gynllun pensiwn tramor"
    val yesText            = "Iawn"
    val noText             = "Na"
    val buttonText         = "Yn eich blaen"
  }

  val userScenarios: Seq[UserScenario[ExpectedContents, Unit]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, ExpectedContentsIndividualEN),
    UserScenario(isWelsh = false, isAgent = true, ExpectedContentsAgentEN),
    UserScenario(isWelsh = true, isAgent = false, ExpectedContentsIndividualCY),
    UserScenario(isWelsh = true, isAgent = true, ExpectedContentsAgentCY)
  )

  private lazy val underTest = inject[TransferPensionSavingsView]

  userScenarios.foreach { userScenario =>
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "show the transfers into overseas page" which {
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = getUserSession(userScenario.isAgent)
        implicit val messages: Messages                                         = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(yesNoForm(userSessionDataRequest.user), taxYearEOY)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        import Selectors._
        import userScenario.commonExpectedResults._

        titleCheck(userScenario.commonExpectedResults.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.commonExpectedResults.expectedTitle)
        captionCheck(expectedCaption(taxYearEOY), captionSelector)
        radioButtonCheck(yesText, 1, checked = false)
        radioButtonCheck(noText, 2, checked = false)
        buttonCheck(buttonText, continueButtonSelector)
        welshToggleCheck(userScenario.isWelsh)
      }
      "show the transfers into overseas page when yes is selected" which {
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = getUserSession(userScenario.isAgent)
        implicit val messages: Messages                                         = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(yesNoForm(userSessionDataRequest.user).fill(true), taxYearEOY)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        import Selectors._
        import userScenario.commonExpectedResults._

        titleCheck(userScenario.commonExpectedResults.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.commonExpectedResults.expectedTitle)
        captionCheck(expectedCaption(taxYearEOY), captionSelector)
        radioButtonCheck(yesText, 1, checked = true)
        radioButtonCheck(noText, 2, checked = false)
        buttonCheck(buttonText, continueButtonSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "show the transfers into overseas page when no is selected" which {
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = getUserSession(userScenario.isAgent)
        implicit val messages: Messages                                         = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(yesNoForm(userSessionDataRequest.user).fill(false), taxYearEOY)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        import Selectors._
        import userScenario.commonExpectedResults._

        titleCheck(userScenario.commonExpectedResults.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.commonExpectedResults.expectedTitle)
        captionCheck(expectedCaption(taxYearEOY), captionSelector)
        radioButtonCheck(yesText, 1, checked = false)
        radioButtonCheck(noText, 2, checked = true)
        buttonCheck(buttonText, continueButtonSelector)
        welshToggleCheck(userScenario.isWelsh)
      }

      "return an error when form is submitted with no entry" which {
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = getUserSession(userScenario.isAgent)
        implicit val messages: Messages                                         = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(yesNoForm(userSessionDataRequest.user).bind(Map(YesNoForm.yesNo -> "")), taxYearEOY)

        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        import Selectors._
        import userScenario.commonExpectedResults._

        titleCheck(userScenario.commonExpectedResults.expectedErrorTitle, userScenario.isWelsh)
        h1Check(userScenario.commonExpectedResults.expectedTitle)
        captionCheck(expectedCaption(taxYearEOY), captionSelector)
        errorSummaryCheck(errorMessage, Selectors.yesSelector)
        errorAboveElementCheck(errorMessage, Some("value"))
        buttonCheck(buttonText, continueButtonSelector)
        welshToggleCheck(userScenario.isWelsh)
      }
    }
  }
}
