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

package views.pensions.shortServiceRefunds

import builders.ShortServiceRefundsViewModelBuilder.{aShortServiceRefundsViewModel, minimalShortServiceRefundsViewModel}
import controllers.pensions.shortServiceRefunds.routes._
import models.requests.UserSessionDataRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.pensions.shortServiceRefunds.ShortServiceRefundsCYAView

class ShortServiceRefundsCYAViewSpec extends ViewUnitTest { // scalastyle:off magic.number

  object ChangeLinks {
    val changeRefund      = TaxableRefundAmountController.show(taxYearEOY).url
    val changeNonUkRefund = NonUkTaxRefundsController.show(taxYearEOY).url
    val changeScheme      = RefundSummaryController.show(taxYearEOY).url
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
    val noText: String
    val yesText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedTitle: String          = "Check short service refunds"
    val expectedHeading: String        = "Check short service refunds"
    val expectedCaption: Int => String = (taxYear: Int) => s"Short service refunds for 6 April ${taxYear - 1} to 5 April $taxYear"
    val refundBoolean: String          = "Short service refunds"
    val refundAmount: String           = "Refund amount"
    val nonUKBoolean: String           = "Paid non-UK tax"
    val nonUKAmount: String            = "Amount of non-UK tax"
    val schemes: String                = "Pension schemes paying tax"
    val hiddenRefundBoolean: String    = "Change short service refunds"
    val hiddenRefundAmount: String     = "Change refund amount"
    val hiddenNonUKBoolean: String     = "Change paid non-UK tax"
    val hiddenNonUKAmount: String      = "Change amount of non-UK tax"
    val hiddenSchemes: String          = "Change pension schemes paying tax"
    val buttonText: String             = "Save and continue"
    val noText: String                 = "No"
    val yesText: String                = "Yes"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedTitle: String          = "Gwirio’r ad-daliadau am wasanaeth byr"
    val expectedHeading: String        = "Gwirio’r ad-daliadau am wasanaeth byr"
    val expectedCaption: Int => String = (taxYear: Int) => s"Ad-daliadau am wasanaeth byr ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val refundBoolean: String          = "Ad-daliadau am wasanaeth byr"
    val refundAmount: String           = "Swm yr ad-daliad"
    val nonUKBoolean: String           = "Treth y tu allan i’r DU sydd wedi’i dalu"
    val nonUKAmount: String            = "Swm yr incwm y tu allan i’r DU"
    val schemes: String                = "Cynlluniau pensiwn yn talu treth"
    val hiddenRefundBoolean: String    = "Newid ad-daliadau am wasanaeth byr"
    val hiddenRefundAmount: String     = "Newid swm yr ad-daliad"
    val hiddenNonUKBoolean: String     = "Newid treth y tu allan i’r DU sydd wedi’i dalu"
    val hiddenNonUKAmount: String      = "Newid swm y dreth y tu allan i’r DU"
    val hiddenSchemes: String          = "Newid cynlluniau pensiwn yn talu treth"
    val buttonText: String             = "Cadw ac yn eich blaen"
    val noText: String                 = "Na"
    val yesText: String                = "Iawn"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, String]] = Seq(
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
        implicit val messages: Messages                          = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(TaxYearFormatEOY, aShortServiceRefundsViewModel)
        import userScenario.commonExpectedResults._
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(expectedTitle, userScenario.isWelsh)
        h1Check(expectedHeading)
        captionCheck(expectedCaption(taxYearEOY))

        cyaRowCheck(refundBoolean, yesText, ChangeLinks.changeRefund, hiddenRefundBoolean, 1)
        cyaRowCheck(refundAmount, "£1,999.99", ChangeLinks.changeRefund, hiddenRefundAmount, 2)
        cyaRowCheck(nonUKBoolean, yesText, ChangeLinks.changeNonUkRefund, hiddenNonUKBoolean, 3)
        cyaRowCheck(nonUKAmount, "£1,000", ChangeLinks.changeNonUkRefund, hiddenNonUKAmount, 4)
        cyaRowCheck(schemes, "Scheme Name without UK charge", ChangeLinks.changeScheme, hiddenSchemes, 5)
        buttonCheck(buttonText)
      }

      "render the page with a minimal CYA model" which {

        implicit val request: UserSessionDataRequest[AnyContent] = getUserSession(userScenario.isAgent)
        implicit val messages: Messages                          = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(TaxYearFormatEOY, minimalShortServiceRefundsViewModel)
        import userScenario.commonExpectedResults._
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        titleCheck(expectedTitle, userScenario.isWelsh)
        h1Check(expectedHeading)
        captionCheck(expectedCaption(taxYearEOY))

        cyaRowCheck(refundBoolean, noText, ChangeLinks.changeRefund, hiddenRefundBoolean, 1)
        buttonCheck(buttonText)
      }

    }
  }
}
