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

package views.shortServiceRefunds

import builders.ShortServiceRefundsViewModelBuilder.{aShortServiceRefundsViewModel, minimalShortServiceRefundsViewModel}
import models.requests.UserSessionDataRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.pensions.shortServiceRefunds.ShortServiceRefundsCYAView

class ShortServiceCYAViewSpec extends ViewUnitTest {

  object ChangeLinks {
    val changeRefund = controllers.pensions.shortServiceRefunds.routes.TaxableRefundAmountController.show(taxYearEOY).url
    val changeNonUkRefund = controllers.pensions.shortServiceRefunds.routes.NonUkTaxRefundsController.show(taxYearEOY).url
    val changeScheme = controllers.pensions.shortServiceRefunds.routes.RefundSummaryController.show(taxYearEOY).url
  }

  trait CommonExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedCaption: Int => String
    val refundBoolean: String
    val refundAmount: String
    val nonUKBoolean: String
    val nonUKAmount: String
    val schemes: String
    val hiddenRefundBoolean: String
    val hiddenRefundAmount: String
    val hiddenNonUKBoolean: String
    val hiddenNonUKAmount: String
    val hiddenSchemes: String
    val buttonText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    override val expectedTitle: String = "Check short service refunds"
    override val expectedHeading: String = "Check short service refunds"
    override val expectedCaption: Int => String = (taxYear: Int) => s"Short service refunds for 6 April ${taxYear - 1} to 5 April $taxYear"
    override val refundBoolean: String = "Short service refunds"
    override val refundAmount: String = "Refund amount"
    override val nonUKBoolean: String = "Paid non-UK tax"
    override val nonUKAmount: String = "Amount of non-UK tax"
    override val schemes: String = "Pension schemes paying tax"
    override val hiddenRefundBoolean: String = "Change short service refunds"
    override val hiddenRefundAmount: String = "Change refund amount"
    override val hiddenNonUKBoolean: String = "Change paid non-UK tax"
    override val hiddenNonUKAmount: String = "Change amount of non-UK tax"
    override val hiddenSchemes: String = "Change pension schemes paying tax"
    override val buttonText: String = "Save and continue"

  }

  object CommonExpectedCY extends CommonExpectedResults {
    override val expectedTitle: String = "Check short service refunds"
    override val expectedHeading: String = "Check short service refunds"
    override val expectedCaption: Int => String = (taxYear: Int) => s"Short service refunds for 6 April ${taxYear - 1} to 5 April $taxYear"
    override val refundBoolean: String = "Short service refunds"
    override val refundAmount: String = "Refund amount"
    override val nonUKBoolean: String = "Paid non-UK tax"
    override val nonUKAmount: String = "Amount of non-UK tax"
    override val schemes: String = "Pension schemes paying tax"
    override val hiddenRefundBoolean: String = "Change short service refunds"
    override val hiddenRefundAmount: String = "Change refund amount"
    override val hiddenNonUKBoolean: String = "Change paid non-UK tax"
    override val hiddenNonUKAmount: String = "Change amount of non-UK tax"
    override val hiddenSchemes: String = "Change pension schemes paying tax"
    override val buttonText: String = "Save and continue"
  }

  override val userScenarios: Seq[UserScenario[CommonExpectedResults, String]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, None),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, None),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, None),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, None)
  )

  private lazy val underTest = inject[ShortServiceRefundsCYAView]

  userScenarios.foreach { userScenario =>

    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {

      "render the page with a full CYA model" which {

        implicit val request: UserSessionDataRequest[AnyContent] = getUserSession(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, aShortServiceRefundsViewModel)
        import userScenario.commonExpectedResults._
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(expectedTitle, userScenario.isWelsh)
        h1Check(expectedHeading)
        captionCheck(expectedCaption(taxYearEOY))

        //noinspection ScalaStyle
        cyaRowCheck(refundBoolean, "Yes", ChangeLinks.changeRefund, hiddenRefundBoolean, 1)
        cyaRowCheck(refundAmount, "£1,999.99", ChangeLinks.changeRefund, hiddenRefundAmount, 2)
        cyaRowCheck(nonUKBoolean, "Yes", ChangeLinks.changeNonUkRefund, hiddenNonUKBoolean, 3)
        cyaRowCheck(nonUKAmount, "£1,000", ChangeLinks.changeNonUkRefund, hiddenNonUKAmount, 4)
        cyaRowCheck(schemes, "Overseas Refund Scheme Name", ChangeLinks.changeScheme, hiddenSchemes, 5)
        buttonCheck(buttonText)
      }

      "render the page with a minimal CYA model" which {

        implicit val request: UserSessionDataRequest[AnyContent] = getUserSession(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYearEOY, minimalShortServiceRefundsViewModel)
        import userScenario.commonExpectedResults._
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(expectedTitle, userScenario.isWelsh)
        h1Check(expectedHeading)
        captionCheck(expectedCaption(taxYearEOY))

        //noinspection ScalaStyle
        cyaRowCheck(refundBoolean, "No", ChangeLinks.changeRefund, hiddenRefundBoolean, 1)
        cyaRowCheck(nonUKBoolean, "No tax paid", ChangeLinks.changeNonUkRefund, hiddenNonUKBoolean, 2)
        buttonCheck(buttonText)
      }

    }
  }
}
