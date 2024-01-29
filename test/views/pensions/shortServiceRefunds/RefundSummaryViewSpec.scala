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

import models.pension.charges.OverseasRefundPensionScheme
import models.requests.UserSessionDataRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.pensions.shortServiceRefunds.RefundSummaryView
import controllers.pensions.shortServiceRefunds.routes._
class RefundSummaryViewSpec extends ViewUnitTest {

  object Selectors {
    val captionSelector: String        = "#main-content > div > div > header > p"
    val addAnotherLinkSelector         = "#add-another-pension-link"
    val addLinkSelector                = "#add-pension-income-link"
    val continueButtonSelector: String = "#continue"
    val summaryListTableSelector       = "#pensionTransferSummaryList"

    def changeLinkSelector(index: Int): String = s"#pensionTransferSummaryList > dl > div:nth-child($index) > dd.hmrc-add-to-a-list__change > a"

    def removeLinkSelector(index: Int): String = s"#pensionTransferSummaryList > dl > div:nth-child($index) > dd.hmrc-add-to-a-list__remove > a"

    def pensionNameSelector(index: Int): String = s"#pensionTransferSummaryList > dl > div:nth-child($index) > dt"

    val insetSpanText1: String = "#youNeedToAddOneOrMorePensionScheme1"
    val insetSpanText2: String = "#youNeedToAddOneOrMorePensionScheme2"
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
    val expectedCaption: Int => String = (taxYear: Int) => s"Short service refunds for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedButtonText             = "Continue"
    val expectedTitle                  = "Short service refund summary"
    val expectedHeading                = "Short service refund summary"
    val change                         = "Change"
    val remove                         = "Remove"
    val expectedAddAnotherText         = "Add another pension scheme"
    val expectedAddPensionSchemeText   = "Add a pension scheme"
    val addASchemeButton               = "Add a scheme"
    val returnToOverviewButton         = "Return to overview"
    val text1                          = "You need to add one or more pension scheme."
    val text2                          = "If you don’t have a pensions scheme to add you can return to the overview page and come back later."
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Ad-daliadau am wasanaeth byr ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val expectedButtonText             = "Yn eich blaen"
    val expectedTitle                  = "Crynodeb o’r ad-daliad am wasanaeth byr"
    val expectedHeading                = "Crynodeb o’r ad-daliad am wasanaeth byr"
    val change                         = "Newid"
    val remove                         = "Tynnu"
    val expectedAddAnotherText         = "Ychwanegu cynllun pensiwn arall"
    val expectedAddPensionSchemeText   = "Add a pension scheme"
    val addASchemeButton               = "Ychwanegu cynllun"
    val returnToOverviewButton         = "Yn ôl i’r trosolwg"
    val text1                          = "Bydd angen i chi ychwanegu un cynllun pensiwn neu fwy."
    val text2                          = "Os nad oes gennych gynllun pensiwn i’w ychwanegu, gallwch ddychwelyd i’r trosolwg a dod nôl yn nes ymlaen."
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, Nothing]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY)
  )

  private lazy val underTest = inject[RefundSummaryView]
  "on show" should {
    userScenarios.foreach { userScenario =>
      import Selectors._
      import userScenario.commonExpectedResults._

      implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = getUserSession(userScenario.isAgent)
      implicit val messages: Messages                                         = getMessages(userScenario.isWelsh)
      s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" which {
        "render the refund scheme summary list page with multiple schemes" which {

          val schemes                     = getSchemes
          val htmlFormat                  = underTest(taxYearEOY, schemes)
          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          val cyaUrl = ShortServiceRefundsCYAController.show(taxYearEOY).url
          val taxOnShortServiceRefundUrl = (refundPensionSchemeIndex: Option[Int]) =>
            TaxOnShortServiceRefundController.show(taxYearEOY, refundPensionSchemeIndex = refundPensionSchemeIndex).url

          titleCheck(expectedTitle, userScenario.isWelsh)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(s"${schemes.head.name.get}", pensionNameSelector(1))
          textOnPageCheck(s"${schemes.last.name.get}", pensionNameSelector(2))

          // links need to be updated
          linkCheck(s"$change $change ${schemes.head.name.get}", changeLinkSelector(1), taxOnShortServiceRefundUrl(Some(0)))
          linkCheck(s"$remove $remove ${schemes.head.name.get}", removeLinkSelector(1), removeLink(0))

          linkCheck(s"$change $change ${schemes.last.name.get}", changeLinkSelector(2), taxOnShortServiceRefundUrl(Some(1)))
          linkCheck(s"$remove $remove ${schemes.last.name.get}", removeLinkSelector(2), removeLink(1))
          linkCheck(expectedAddAnotherText, addAnotherLinkSelector, taxOnShortServiceRefundUrl(None))

          buttonCheck(expectedButtonText, continueButtonSelector, Some(cyaUrl))
          welshToggleCheck(userScenario.isWelsh)
        }

        "render the refund scheme summary list page with pre-filled content containing refund pension scheme but no name" which {
          val schemes                     = getSchemes.updated(1, getSchemes.last.copy(name = None))
          val htmlFormat                  = underTest(taxYearEOY, schemes)
          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          val taxOnShortServiceRefundUrl = (refundPensionSchemeIndex: Option[Int]) =>
            TaxOnShortServiceRefundController.show(taxYearEOY, refundPensionSchemeIndex = refundPensionSchemeIndex).url
          val cyaUrl = ShortServiceRefundsCYAController.show(taxYearEOY).url

          titleCheck(expectedTitle, userScenario.isWelsh)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(s"${schemes.head.name.get}", pensionNameSelector(1))
          elementNotOnPageCheck(pensionNameSelector(2))

          linkCheck(s"$change $change ${schemes.head.name.get}", changeLinkSelector(1), taxOnShortServiceRefundUrl(Some(0)))
          linkCheck(s"$remove $remove ${schemes.head.name.get}", removeLinkSelector(1), removeLink(0))
          linkCheck(expectedAddAnotherText, addAnotherLinkSelector, taxOnShortServiceRefundUrl(None))

          buttonCheck(expectedButtonText, continueButtonSelector, Some(cyaUrl))
          welshToggleCheck(userScenario.isWelsh)
        }

        "render alternative refund scheme summary list page when no pensions schemes are provided" which {

          val htmlFormat                  = underTest(taxYearEOY, Seq.empty)
          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(expectedTitle, userScenario.isWelsh)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          elementNotOnPageCheck(summaryListTableSelector)
          // todo update redirect to to transfer journey CYA page when navigation is linked up
          buttonCheck(addASchemeButton, "#AddAScheme")
          textOnPageCheck(text1, insetSpanText1)
          buttonCheck(returnToOverviewButton, "#ReturnToOverview")
          textOnPageCheck(text2, insetSpanText2)
          welshToggleCheck(userScenario.isWelsh)
        }
      }
    }
  }

  private def getSchemes: Seq[OverseasRefundPensionScheme] = {
    val scheme1 = OverseasRefundPensionScheme(
      ukRefundCharge = Some(true),
      name = Some("Overseas Refund Scheme Name"),
      pensionSchemeTaxReference = None,
      qualifyingRecognisedOverseasPensionScheme = Some("QOPS123456"),
      providerAddress = Some("Scheme Address"),
      alphaTwoCountryCode = Some("FR"),
      alphaThreeCountryCode = Some("FRA")
    )
    val scheme2 = OverseasRefundPensionScheme(
      ukRefundCharge = Some(true),
      name = Some("Pension Scheme 2"),
      pensionSchemeTaxReference = None,
      qualifyingRecognisedOverseasPensionScheme = Some("QOPS123456"),
      providerAddress = Some("Scheme Address"),
      alphaTwoCountryCode = Some("FR"),
      alphaThreeCountryCode = Some("FRA")
    )

    Seq(scheme1, scheme2)
  }
  private def removeLink(index: Int) = RemoveRefundSchemeController.show(taxYearEOY, Some(index)).url
}
