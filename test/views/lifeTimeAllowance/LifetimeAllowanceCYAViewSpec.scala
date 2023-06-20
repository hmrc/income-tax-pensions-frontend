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

package views.lifeTimeAllowance

import builders.PensionLifetimeAllowanceViewModelBuilder.{aPensionLifetimeAllowanceViewModel, minimalPensionLifetimeAllowanceViewModel}
import builders.PensionsUserDataBuilder.authorisationRequest
import models.requests.UserSessionDataRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.pensions.lifetimeAllowances.LifetimeAllowanceCYAView

class LifetimeAllowanceCYAViewSpec extends ViewUnitTest { //scalastyle:off magic.number

  object ChangeLinks {
    val changeAboveLifetimeAllowance = controllers.pensions.lifetimeAllowances.routes.AnnualLifetimeAllowanceCYAController.show(taxYear).url
    val changeLumpSum = controllers.pensions.lifetimeAllowances.routes.PensionLumpSumController.show(taxYear).url
    val changeOtherPayments = controllers.pensions.lifetimeAllowances.routes.LifeTimeAllowanceAnotherWayController.show(taxYear).url
    val changeSchemes = controllers.pensions.lifetimeAllowances.routes.AnnualLifetimeAllowanceCYAController.show(taxYear).url
  }

  trait CommonExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedCaption: Int => String
    val aboveLifetimeAllowance: String
    val lumpSumAmount: String
    val otherPaymentsAmount: String
    val schemes: String
    val hiddenAboveLifetimeAllowance: String
    val hiddenLumpSumAmount: String
    val hiddenOtherPayments: String
    val hiddenSchemes: String
    val buttonText: String
    val noText: String
    val yesText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedTitle: String = "Check your lifetime allowances"
    val expectedHeading: String = "Check your lifetime allowances"
    val expectedCaption: Int => String = (taxYear: Int) => s"Lifetime allowances for 6 April ${taxYear - 1} to 5 April $taxYear"
    val aboveLifetimeAllowance: String = "Above lifetime allowance"
    val lumpSumAmount: String = "Lump sum"
    val otherPaymentsAmount: String = "Other payments"
    val schemes: String = "Schemes paying lifetime allowance tax"
    val hiddenAboveLifetimeAllowance: String = "Change above lifetime allowance"
    val hiddenLumpSumAmount: String = "Change lump sum"
    val hiddenOtherPayments: String = "Change other payments"
    val hiddenSchemes: String = "Change schemes paying lifetime allowance tax"
    val buttonText: String = "Save and continue"
    val noText: String = "No"
    val yesText: String = "Yes"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedTitle: String = "Check your lifetime allowances"
    val expectedHeading: String = "Check your lifetime allowances"
    val expectedCaption: Int => String = (taxYear: Int) => s"Lifetime allowances for 6 April ${taxYear - 1} to 5 April $taxYear"
    val aboveLifetimeAllowance: String = "Uwch na’r lwfans oes pensiwn"
    val lumpSumAmount: String = "Cyfandaliad"
    val otherPaymentsAmount: String = "Taliadau eraill"
    val schemes: String = "Cynlluniau sy’n talu treth lwfans oes"
    val hiddenAboveLifetimeAllowance: String = "Change above lifetime allowance"
    val hiddenLumpSumAmount: String = "Change lump sum"
    val hiddenOtherPayments: String = "Change other payments"
    val hiddenSchemes: String = "Change schemes paying lifetime allowance tax"
    val buttonText: String = "Cadw ac yn eich blaen"
    val noText: String = "Na"
    val yesText: String = "Iawn"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, String]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, None),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, None),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, None),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, None)
  )

  private lazy val underTest = inject[LifetimeAllowanceCYAView]

  userScenarios.foreach { userScenario =>

    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {

      "render the page with a full CYA model" which {

        implicit val request: UserSessionDataRequest[AnyContent] = getUserSession(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, aPensionLifetimeAllowanceViewModel)
        import userScenario.commonExpectedResults._
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(expectedTitle, userScenario.isWelsh)
        h1Check(expectedHeading)
        captionCheck(expectedCaption(taxYearEOY))

        cyaRowCheck(aboveLifetimeAllowance, yesText, ChangeLinks.changeAboveLifetimeAllowance, hiddenAboveLifetimeAllowance, 1)
        cyaRowCheck(lumpSumAmount, "Amount: £134.22 Tax paid: £23.55", ChangeLinks.changeAboveLifetimeAllowance, hiddenLumpSumAmount, 2)
        cyaRowCheck(otherPaymentsAmount, "Amount: £1,667.22 Tax paid: £11.33", ChangeLinks.changeLumpSum, hiddenOtherPayments, 3)
        cyaRowCheck(schemes, "1234567CRC, 12345678RB, 1234567DRD", ChangeLinks.changeSchemes, hiddenOtherPayments, 4)
        buttonCheck(buttonText)
      }

      "render the page with a minimal CYA model" which {

        implicit val request: UserSessionDataRequest[AnyContent] = getUserSession(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, minimalPensionLifetimeAllowanceViewModel)
        import userScenario.commonExpectedResults._
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(expectedTitle, userScenario.isWelsh)
        h1Check(expectedHeading)
        captionCheck(expectedCaption(taxYearEOY))

        cyaRowCheck(aboveLifetimeAllowance, noText, ChangeLinks.changeAboveLifetimeAllowance, hiddenAboveLifetimeAllowance, 1)
        buttonCheck(buttonText)
      }

    }
  }

}
