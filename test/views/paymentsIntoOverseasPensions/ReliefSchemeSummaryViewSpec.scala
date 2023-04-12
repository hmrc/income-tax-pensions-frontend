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

package views.paymentsIntoOverseasPensions

import builders.PaymentsIntoOverseasPensionsViewModelBuilder.aPaymentsIntoOverseasPensionsViewModel
import builders.PensionsUserDataBuilder.aPensionsUserData
import builders.UserBuilder.aUser
import controllers.pensions.paymentsIntoOverseasPensions.routes._
import models.requests.UserSessionDataRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import utils.FakeRequestProvider
import views.html.pensions.paymentsIntoOverseasPensions.ReliefSchemeSummaryView

class ReliefSchemeSummaryViewSpec extends ViewUnitTest with FakeRequestProvider {


  object Selectors {
    val captionSelector: String = "#main-content > div > div > header > p"
    val addAnotherLinkSelector = "#add-another-relief-link"
    val addLinkSelector = "#add-relief-link"
    val continueButtonSelector: String = "#continue"
    val summaryListTableSelector = "#reliefSchemeSummaryList"

    def changeLinkSelector(index: Int): String = s"#reliefSchemeSummaryList > dl > div:nth-child($index) > dd.hmrc-add-to-a-list__change > a"

    def removeLinkSelector(index: Int): String = s"#reliefSchemeSummaryList > dl > div:nth-child($index) > dd.hmrc-add-to-a-list__remove > a"

    def pensionNameSelector(index: Int): String = s"#reliefSchemeSummaryList > dl > div:nth-child($index) > dt"

    val insetSpanText1: String = "#youNeedToAddOneOrMoreReliefScheme1"
    val insetSpanText2: String = "#youNeedToAddOneOrMoreReliefScheme2"
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedTitle: String
    val expectedHeading: String
    val change: String
    val remove: String
    val expectedButtonText: String
    val expectedAddAnotherText: String
    val expectedAddPensionSchemeText: String
    val addASchemeButton: String
    val returnToOverviewButton: String
    val text1: String
    val text2: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    override val expectedCaption: Int => String = (taxYear: Int) => s"Payments into overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedButtonText = "Continue"
    val expectedTitle = "Schemes with untaxed employer payments"
    val expectedHeading = "Schemes with untaxed employer payments"
    val change = "Change"
    val remove = "Remove"
    val expectedAddAnotherText = "Add another overseas pension scheme"
    val expectedAddPensionSchemeText = "Add an overseas pension scheme"
    val addASchemeButton = "Add a scheme"
    val returnToOverviewButton = "Return to overview"
    val text1 = "You need to add one or more pension scheme."
    val text2 = "If you don’t have a pensions scheme to add you can return to the overview page and come back later."
  }


  object CommonExpectedCY extends CommonExpectedResults {
    override val expectedCaption: Int => String = (taxYear: Int) => s"Taliadau i bensiynau tramor ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val expectedButtonText = "Yn eich blaen"
    val expectedTitle = "Schemes with untaxed employer payments"
    val expectedHeading = "Schemes with untaxed employer payments"
    val change = "Newid"
    val remove = "Tynnu"
    val expectedAddAnotherText = "Add another overseas pension scheme"
    val expectedAddPensionSchemeText = "Add an overseas pension scheme"
    val addASchemeButton = "Add a scheme"
    val returnToOverviewButton = "Yn ôl i’r trosolwg"
    val text1 = "You need to add one or more pension scheme."
    val text2 = "If you don’t have a pensions scheme to add you can return to the overview page and come back later."
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, Unit]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY)
  )

  private lazy val underTest = inject[ReliefSchemeSummaryView]
    userScenarios.foreach { userScenario =>
      s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
        "Render the refund scheme summary list page with multiple schemes" which {
            import Selectors._
            import userScenario.commonExpectedResults._

            implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = UserSessionDataRequest(aPensionsUserData, aUser, fakeIndividualRequest)
            implicit val messages: Messages = getMessages(userScenario.isWelsh)
            val schemes = aPaymentsIntoOverseasPensionsViewModel.reliefs
            val htmlFormat = underTest(taxYearEOY, schemes)
            implicit val document: Document = Jsoup.parse(htmlFormat.body)

            val cyaUrl = ReliefsSchemeSummaryController.show(taxYearEOY).url //todo update when CYA is added
            def changeUrl(index: Int) =  ReliefsSchemeDetailsController.show(taxYearEOY, Some(index)).url
            val addSchemeUrl = ReliefsSchemeSummaryController.show(taxYearEOY).url //todo update when PensionsCustomerReferenceNumberController is updated

            titleCheck(expectedTitle, userScenario.isWelsh)
            h1Check(expectedHeading)
            captionCheck(expectedCaption(taxYearEOY))
            textOnPageCheck(s"${schemes.head.customerReference.getOrElse("")}", pensionNameSelector(1))
            textOnPageCheck(s"${schemes.last.customerReference.getOrElse("")}", pensionNameSelector(2))

            linkCheck(s"$change $change ${schemes.head.customerReference.getOrElse("")}", changeLinkSelector(1), changeUrl(0))
            //linkCheck(s"$remove $remove ${schemes.head.customerReference.getOrElse("")}", removeLinkSelector(1), )

            linkCheck(s"$change $change ${schemes.last.customerReference.getOrElse("")}", changeLinkSelector(2), changeUrl(1))
            //linkCheck(s"$remove $remove ${schemes.last.customerReference.getOrElse("")}", removeLinkSelector(2), )
            linkCheck(expectedAddAnotherText, addAnotherLinkSelector, addSchemeUrl)

            buttonCheck(expectedButtonText, continueButtonSelector, Some(cyaUrl))
            welshToggleCheck(userScenario.isWelsh)
          }


        "Render the refund scheme summary list page with pre-filled content containing refund pension scheme but no customerReference" which {
            import Selectors._
            import userScenario.commonExpectedResults._
            implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = UserSessionDataRequest(aPensionsUserData, aUser, fakeIndividualRequest)
            implicit val messages: Messages = getMessages(userScenario.isWelsh)

            val schemes = aPaymentsIntoOverseasPensionsViewModel.reliefs.updated(1, aPaymentsIntoOverseasPensionsViewModel.reliefs.last.copy(customerReference = None))
            val htmlFormat = underTest(taxYearEOY, schemes)
            implicit val document: Document = Jsoup.parse(htmlFormat.body)

            val cyaUrl: String = ReliefsSchemeSummaryController.show(taxYearEOY).url //todo update when CYA is added
            def changeUrl(index: Int): String = ReliefsSchemeDetailsController.show(taxYearEOY, Some(index)).url
            val addSchemeUrl: String = ReliefsSchemeSummaryController.show(taxYearEOY).url //todo update when PensionsCustomerReferenceNumberController is updated


          titleCheck(expectedTitle, userScenario.isWelsh)
            h1Check(expectedHeading)
            captionCheck(expectedCaption(taxYearEOY))
            textOnPageCheck(s"${schemes.head.customerReference.getOrElse("")}", pensionNameSelector(1))
            elementNotOnPageCheck(pensionNameSelector(2))

            linkCheck(s"$change $change ${schemes.head.customerReference.getOrElse("")}", changeLinkSelector(1), changeUrl(0))
            //linkCheck(s"$remove $remove ${schemes.head.customerReference.getOrElse("")}", removeLinkSelector(1), removeLink(0))
            linkCheck(expectedAddAnotherText, addAnotherLinkSelector, addSchemeUrl)

            buttonCheck(expectedButtonText, continueButtonSelector, Some(cyaUrl))
            welshToggleCheck(userScenario.isWelsh)
          }

        "Render alternative refund scheme summary list page when no pensions schemes are provided" which {
            import Selectors._
            import userScenario.commonExpectedResults._

            implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = UserSessionDataRequest(aPensionsUserData, aUser, fakeIndividualRequest)
            implicit val messages: Messages = getMessages(userScenario.isWelsh)
            val htmlFormat = underTest(taxYearEOY, Seq.empty)
            implicit val document: Document = Jsoup.parse(htmlFormat.body)


            titleCheck(expectedTitle, userScenario.isWelsh)
            h1Check(expectedHeading)
            captionCheck(expectedCaption(taxYearEOY))
            elementNotOnPageCheck(summaryListTableSelector)
            //todo update redirect to to transfer journey CYA page when navigation is linked up
            buttonCheck(addASchemeButton, "#AddAScheme")
            textOnPageCheck(text1, insetSpanText1)
            buttonCheck(returnToOverviewButton, "#ReturnToOverview")
            textOnPageCheck(text2, insetSpanText2)
            welshToggleCheck(userScenario.isWelsh)
          }
        }
    }
}

