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

package views.pensions.lifeTimeAllowance

import builders.PensionLifetimeAllowancesViewModelBuilder.{aPensionLifetimeAllowancesViewModel, minimalPensionLifetimeAllowancesViewModel}
import models.requests.UserSessionDataRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.pensions.lifetimeAllowances.LifetimeAllowanceCYAView

class LifetimeAllowanceCYAViewSpec extends ViewUnitTest { //scalastyle:off magic.number

  object ChangeLinks {
    val changeAboveLifetimeAllowance: String = controllers.pensions.lifetimeAllowances.routes.AboveAnnualLifetimeAllowanceController.show(taxYear).url
    val changeLumpSum: String = controllers.pensions.lifetimeAllowances.routes.PensionLumpSumController.show(taxYear).url
    val changeOtherPayments: String = controllers.pensions.lifetimeAllowances.routes.LifeTimeAllowanceAnotherWayController.show(taxYear).url
    val changeSchemes: String = controllers.pensions.annualAllowances.routes.PstrSummaryController.show(taxYear).url
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val aboveLifetimeAllowanceText: String
    val lumpSumText: String
    val lumpSumAmount: String
    val otherPaymentsText: String
    val otherPaymentsAmount: String
    val schemes: String
    val schemesList: String
    val hiddenAboveLifetimeAllowance: String
    val hiddenLumpSumAmount: String
    val hiddenOtherPayments: String
    val hiddenSchemes: String
    val buttonText: String
    val noText: String
    val yesText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Lifetime allowances for 6 April ${taxYear - 1} to 5 April $taxYear"
    val aboveLifetimeAllowanceText: String = "Above lifetime allowance"
    val lumpSumText: String = "Lump sum"
    val lumpSumAmount: String = "Amount: £50 Tax paid: £20"
    val otherPaymentsText: String = "Other payments"
    val otherPaymentsAmount: String = "Amount: £22.22 Tax paid: £11.11"
    val schemes: String = "Schemes paying lifetime allowance tax"
    val schemesList: String = "1234567CRC, 12345678RB, 1234567DRD"
    val hiddenAboveLifetimeAllowance: String = "Change above lifetime allowance"
    val hiddenLumpSumAmount: String = "Change lump sum"
    val hiddenOtherPayments: String = "Change other payments"
    val hiddenSchemes: String = "Change schemes paying lifetime allowance tax"
    val buttonText: String = "Save and continue"
    val noText: String = "No"
    val yesText: String = "Yes"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Lwfansau oes ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val aboveLifetimeAllowanceText: String = "Uwch na’r lwfans oes pensiwn"
    val lumpSumText: String = "Cyfandaliad"
    val lumpSumAmount: String = "Amount: £50 Tax paid: £20"
    val otherPaymentsText: String = "Taliadau eraill"
    val otherPaymentsAmount: String = "Amount: £22.22 Tax paid: £11.11"
    val schemes: String = "Cynlluniau sy’n talu treth lwfans oes"
    val schemesList: String = "1234567CRC, 12345678RB, 1234567DRD"
    val hiddenAboveLifetimeAllowance: String = "Change above lifetime allowance"
    val hiddenLumpSumAmount: String = "Change lump sum"
    val hiddenOtherPayments: String = "Change other payments"
    val hiddenSchemes: String = "Change schemes paying lifetime allowance tax"
    val buttonText: String = "Cadw ac yn eich blaen"
    val noText: String = "Na"
    val yesText: String = "Iawn"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    lazy val expectedHeading: String = expectedTitle
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Check your lifetime allowance"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Check your client’s lifetime allowance"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Check your lifetime allowance"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Check your client’s lifetime allowance"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private lazy val underTest = inject[LifetimeAllowanceCYAView]

  userScenarios.foreach { userScenario =>

    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      import userScenario.specificExpectedResults

      "render the page with a full CYA model" which {

        implicit val request: UserSessionDataRequest[AnyContent] = getUserSession(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, aPensionLifetimeAllowancesViewModel)
        import userScenario.commonExpectedResults._


        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption(taxYearEOY))

        cyaRowCheck(aboveLifetimeAllowanceText, yesText, ChangeLinks.changeAboveLifetimeAllowance, hiddenAboveLifetimeAllowance, 1)
        cyaRowCheck(lumpSumText, lumpSumAmount, ChangeLinks.changeAboveLifetimeAllowance, hiddenLumpSumAmount, 2)
        cyaRowCheck(otherPaymentsText, otherPaymentsAmount, ChangeLinks.changeLumpSum, hiddenOtherPayments, 3)
        cyaRowCheck(schemes, schemesList, ChangeLinks.changeSchemes, hiddenOtherPayments, 4)
        buttonCheck(buttonText)
      }

      "render the page with a minimal CYA model" which {

        implicit val request: UserSessionDataRequest[AnyContent] = getUserSession(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, minimalPensionLifetimeAllowancesViewModel)
        import userScenario.commonExpectedResults._
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(specificExpectedResults.get.expectedHeading)
        captionCheck(expectedCaption(taxYearEOY))

        cyaRowCheck(aboveLifetimeAllowanceText, noText, ChangeLinks.changeAboveLifetimeAllowance, hiddenAboveLifetimeAllowance, 1)
        buttonCheck(buttonText)
      }

    }
  }

}
  
