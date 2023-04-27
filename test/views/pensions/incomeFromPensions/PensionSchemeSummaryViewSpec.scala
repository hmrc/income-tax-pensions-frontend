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

package views.pensions.incomeFromPensions

import builders.UkPensionIncomeViewModelBuilder._
import controllers.pensions.incomeFromPensions.routes
import models.pension.statebenefits.UkPensionIncomeViewModel
import models.requests.UserSessionDataRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.pensions.incomeFromPensions.PensionSchemeSummaryView

class PensionSchemeSummaryViewSpec extends ViewUnitTest { //scalastyle:off magic.number

  object ChangeLinks {
    val changeDetails = (index: Int) => routes.PensionSchemeDetailsController.show(taxYearEOY, Some(index)).url
    val changeAmount = (index: Int) => routes.PensionAmountController.show(taxYearEOY, Some(index)).url
    val changeStartDate = (index: Int) => routes.PensionSchemeStartDateController.show(taxYearEOY, Some(index)).url
  }

  trait CommonExpectedResults {
    val expectedTitle: String
    lazy val expectedHeading = expectedTitle
    val expectedCaption: Int => String
    val schemeDetails: String
    val hiddenSchemeDetails: String
    val schemeName: String
    val schemePAYE: String
    val schemePID: String
    lazy val schemeDetailsValue = s"$schemeName\n$schemePAYE\n$schemePID"
    val schemeIncome: String
    val hiddenSchemeIncome: String
    val paymentsAmount: String
    val paymentsTax: String
    lazy val schemeIncomeValue = s"$paymentsAmount\n$paymentsTax"
    val schemeStartDate: String
    val schemeStartDateValue: String = "01 March 2020"
    val buttonText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedTitle: String = "Check pensions scheme details"
    val expectedCaption: Int => String = (taxYear: Int) => s"Income from pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val schemeDetails: String = "Scheme details"
    val hiddenSchemeDetails = "Change scheme details"
    val schemeName: String = "Simple-Scheme"
    val schemePAYE: String = "PAYE: 123/AB456"
    val schemePID: String = "PID:  11223344"
    val schemeIncome = "Pensions income"
    val hiddenSchemeIncome = "Change pensions income"
    val paymentsAmount: String = "Pay: £2,000"
    val paymentsTax: String = "Tax: £200"
    val schemeStartDate: String = "Pension start date"
    val buttonText: String = "Continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedTitle: String = "Check payments into overseas pensions"
    val expectedCaption: Int => String = (taxYear: Int) => s"Taliadau i bensiynau tramor ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val schemeDetails: String = "Scheme details"
    val hiddenSchemeDetails = "Change scheme details"
    val schemeName: String = "Simple-Scheme"
    val schemePAYE: String = "PAYE: 123/AB456"
    val schemePID: String = "PID:  11223344"
    val schemeIncome = "Pensions income"
    val hiddenSchemeIncome = "Change pensions income"
    val paymentsAmount: String = "Pay: £2,000"
    val paymentsTax: String = "Tax: £200"
    val schemeStartDate: String = "Pension start date"
    val buttonText: String = "Continue"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, String]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, None),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, None),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, None),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, None)
  )

  lazy val underTest = inject[PensionSchemeSummaryView]

  private def renderPage(userScenario: UserScenario[CommonExpectedResults, String], model: Seq[UkPensionIncomeViewModel], index: Int) = {
    implicit val request: UserSessionDataRequest[AnyContent] = getUserSession(userScenario.isAgent)
    implicit val messages: Messages = getMessages(userScenario.isWelsh)

    val htmlFormat = underTest(taxYearEOY, model(index), Some(index))
    import userScenario.commonExpectedResults._
    implicit val document: Document = Jsoup.parse(htmlFormat.body)

    titleCheck(expectedTitle, userScenario.isWelsh)
    h1Check(expectedHeading)
    captionCheck(expectedCaption(taxYearEOY))
    document
  }


  userScenarios.foreach { userScenario =>
    import userScenario.commonExpectedResults._

    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {

      val schemeIndex0 = 0

      "render the page with a UK Pension Income Model" when {
        implicit val document: Document = renderPage(userScenario, anUkPensionIncomeViewModelSeq, schemeIndex0)

        cyaRowCheck(schemeDetails, schemeDetailsValue, ChangeLinks.changeDetails(schemeIndex0),hiddenSchemeDetails,1)
        cyaRowCheck(schemeIncome, schemeIncomeValue, ChangeLinks.changeAmount(schemeIndex0),hiddenSchemeDetails,2)
        cyaRowCheck(schemeStartDate, schemeStartDateValue, ChangeLinks.changeStartDate(schemeIndex0),hiddenSchemeDetails,3)
        buttonCheck(buttonText)
      }
    }
  }
}
