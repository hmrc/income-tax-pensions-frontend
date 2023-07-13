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

import builders.PensionsUserDataBuilder.aPensionsUserData
import builders.UserBuilder.{aUser, anAgentUser}
import forms.{FormsProvider, YesNoForm}
import models.pension.charges.OverseasRefundPensionScheme
import models.pension.pages.shortServiceRefunds.TaxOnShortServiceRefundPage
import models.requests.UserSessionDataRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import play.twirl.api.HtmlFormat
import support.ViewUnitTest
import utils.FakeRequestProvider
import views.html.pensions.shortServiceRefunds.TaxPaidOnShortServiceRefundView

class TaxOnShortServiceRefundSpec extends ViewUnitTest with FakeRequestProvider {

  object Selectors {
    val captionSelector = "#main-content > div > div > header > p"
    val titleSelector = "#main-content > div > div > header > h1"
    val buttonSelector = "#continue"
  }


  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedTitle: String
    val continue: String
    val yes: String
    val no: String
  }

  object ExpectedCommonEN extends CommonExpectedResults {
    override val expectedCaption: Int => String = (taxYear: Int) => s"Short service refunds for 6 April ${taxYear - 1} to 5 April $taxYear"
    override val expectedTitle: String = "Did a UK pension scheme pay tax on the short service refunds?"
    override val continue: String = "Continue"
    override val yes: String = "Yes"
    override val no: String = "No"
  }

  object ExpectedCommonCY extends CommonExpectedResults {
    override val expectedCaption: Int => String = (taxYear: Int) => s"Ad-daliadau am wasanaeth byr ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    override val expectedTitle: String = "A wnaeth cynllun pensiwn y DU dalu treth ar yr ad-daliadau trethadwy am wasanaeth byr?"
    override val continue: String = "Yn eich blaen"
    override val yes: String = "Iawn"
    override val no: String = "Na"
  }

  trait SpecificExpectedResults {
    val expectedNoEntryErrorText : String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Short service refunds for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedTitle: String = "Did a UK pension scheme pay tax on the short service refunds?"
    val continue: String = "Continue"
    val expectedNoEntryErrorText : String = "Select yes if a UK pension scheme paid tax on the short service refund"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults{
    val expectedCaption: Int => String = (taxYear: Int) => s"Ad-daliadau am wasanaeth byr ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val expectedTitle: String = "A wnaeth cynllun pensiwn y DU dalu treth ar yr ad-daliadau trethadwy am wasanaeth byr?"
    val continue: String = "Continue"
    val expectedNoEntryErrorText : String = "Dewiswch ‘Iawn’ os gwnaeth cynllun pensiwn y DU dalu treth ar yr ad-daliad am wasanaeth byr"
  }

  object ExpectedAgentEN extends SpecificExpectedResults{
    val expectedCaption: Int => String = (taxYear: Int) => s"Short service refunds for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedTitle: String = "Did a UK pension scheme pay tax on the short service refunds?"
    val continue: String = "Continue"
    val expectedNoEntryErrorText : String = "Select yes if a UK pension scheme paid tax on the short service refund"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) =>  s"Ad-daliadau am wasanaeth byr ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val expectedTitle: String = "A wnaeth cynllun pensiwn y DU dalu treth ar yr ad-daliadau trethadwy am wasanaeth byr?"
    val continue: String = "Continue"
    val expectedNoEntryErrorText : String = "Dewiswch ‘Iawn’ os gwnaeth cynllun pensiwn y DU dalu treth ar yr ad-daliad am wasanaeth byr"
  }

  override protected val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, ExpectedCommonEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, ExpectedCommonEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, ExpectedCommonCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, ExpectedCommonCY, Some(ExpectedAgentCY))
  )

  private lazy val underTest = inject[TaxPaidOnShortServiceRefundView]
  userScenarios.foreach { userScenario =>
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render page with 'Yes' pre filled " which {
        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = UserSessionDataRequest(aPensionsUserData,
          if (userScenario.isAgent) anAgentUser else aUser,
          if (userScenario.isAgent) fakeAgentRequest else fakeIndividualRequest)
        def form: Form[Boolean] = new FormsProvider().shortServiceTaxOnShortServiceRefundForm
        val taxOnShortServiceRefundPage = TaxOnShortServiceRefundPage(taxYear - 1, Some(0), aPensionsUserData.pensions.shortServiceRefunds, form)
        implicit val htmlFormat: HtmlFormat.Appendable = underTest(taxOnShortServiceRefundPage)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY), Selectors.captionSelector)
        titleCheck(userScenario.commonExpectedResults.expectedTitle, userScenario.isWelsh)
        buttonCheck(userScenario.commonExpectedResults.continue)
        radioButtonCheck(userScenario.commonExpectedResults.yes, 1, checked = true)
        radioButtonCheck(userScenario.commonExpectedResults.no, 2, checked = false)
      }
      "render page with 'No' pre filled " which {
        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = UserSessionDataRequest(aPensionsUserData,
          if (userScenario.isAgent) anAgentUser else aUser,
          if (userScenario.isAgent) fakeAgentRequest else fakeIndividualRequest)
        def form: Form[Boolean] = new FormsProvider().shortServiceTaxOnShortServiceRefundForm

        val updatedShortServiceRefunds = aPensionsUserData.pensions.shortServiceRefunds.copy(
          refundPensionScheme = aPensionsUserData.pensions.shortServiceRefunds.refundPensionScheme.updated(0, OverseasRefundPensionScheme(
          ukRefundCharge = Some(false)
        )))

        val taxOnShortServiceRefundPage = TaxOnShortServiceRefundPage(taxYear - 1, Some(0), updatedShortServiceRefunds, form)
        implicit val htmlFormat: HtmlFormat.Appendable = underTest(taxOnShortServiceRefundPage)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY), Selectors.captionSelector)
        titleCheck(userScenario.commonExpectedResults.expectedTitle, userScenario.isWelsh)
        buttonCheck(userScenario.commonExpectedResults.continue)
        radioButtonCheck(userScenario.commonExpectedResults.yes, 1, checked = false)
        radioButtonCheck(userScenario.commonExpectedResults.no, 2, checked = true)
      }
      "render page with no value pre filled " which {
        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = UserSessionDataRequest(aPensionsUserData,
          if (userScenario.isAgent) anAgentUser else aUser,
          if (userScenario.isAgent) fakeAgentRequest else fakeIndividualRequest)
        def form: Form[Boolean] = new FormsProvider().shortServiceTaxOnShortServiceRefundForm

        val updatedShortServiceRefunds = aPensionsUserData.pensions.shortServiceRefunds.copy(
          refundPensionScheme = aPensionsUserData.pensions.shortServiceRefunds.refundPensionScheme.updated(0, OverseasRefundPensionScheme(
            ukRefundCharge = Some(false)
          )))

        val taxOnShortServiceRefundPage = TaxOnShortServiceRefundPage(taxYear - 1, Some(0),
          updatedShortServiceRefunds, form.bind(Map(YesNoForm.yesNo -> "")))
        implicit val htmlFormat: HtmlFormat.Appendable = underTest(taxOnShortServiceRefundPage)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        errorAboveElementCheck(userScenario.specificExpectedResults.get.expectedNoEntryErrorText, Some("value"))
        errorSummaryCheck(userScenario.specificExpectedResults.get.expectedNoEntryErrorText, "#value")
      }
    }
  }
}
