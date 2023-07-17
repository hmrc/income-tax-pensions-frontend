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

package controllers.pensions.transferIntoOverseasPensions

import builders.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import builders.PensionsUserDataBuilder.pensionUserDataWithTransferIntoOverseasPension
import builders.TransfersIntoOverseasPensionsViewModelBuilder.{aTransfersIntoOverseasPensionsViewModel, emptyTransfersIntoOverseasPensionsViewModel}
import builders.UserBuilder.aUser
import models.pension.charges.TransferPensionScheme
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.TransferIntoOverseasPensions._
import utils.PageUrls.{fullUrl, pensionSummaryUrl}
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

// scalastyle:off magic.number
class TransferChargeSummaryControllerISpec extends IntegrationTest with BeforeAndAfterEach with ViewHelpers with PensionsDatabaseHelper {
  
  object Selectors {
    val captionSelector: String = "#main-content > div > div > header > p"
    val addAnotherLinkSelector = "#add-another-pension-link"
    val addLinkSelector = "#add-pension-income-link"
    val continueButtonSelector: String = "#continue"
    val summaryListTableSelector = "#pensionTransferSummaryList"
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
    val expectedCaption: Int => String = (taxYear: Int) => s"Transfers into overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedButtonText = "Continue"
    val expectedTitle = "Pension schemes paying transfer charges - summary"
    val expectedHeading = "Pension schemes paying transfer charges - summary"
    val change = "Change"
    val remove = "Remove"
    val expectedAddAnotherText = "Add another pension scheme"
    val expectedAddPensionSchemeText = "Add a pension scheme"
    val addASchemeButton = "Add a scheme"
    val returnToOverviewButton = "Return to overview"
    val text1 = "You need to add one or more pension scheme."
    val text2 = "If you don’t have a pensions scheme to add you can return to the overview page and come back later."
  }
  
  
  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Trosglwyddiadau i bensiynau tramor ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val expectedButtonText = "Yn eich blaen"
    val expectedTitle = "Cynlluniau pensiwn sy’n talu’r taliadau trosglwyddiadau – crynodeb"
    val expectedHeading = "Cynlluniau pensiwn sy’n talu’r taliadau trosglwyddiadau – crynodeb"
    val change = "Newid"
    val remove = "Tynnu"
    val expectedAddAnotherText = "Ychwanegu cynllun pensiwn arall"
    val expectedAddPensionSchemeText = "Add a pension scheme"
    val addASchemeButton = "Add a scheme"
    val returnToOverviewButton = "Yn ôl i’r trosolwg"
    val text1 = "You need to add one or more pension scheme."
    val text2 = "If you don’t have a pensions scheme to add you can return to the overview page and come back later."
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, Nothing]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY)
  )

  ".show" should {
    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        import Selectors._
        import user.commonExpectedResults._
        
        "render the 'overseas transfer charge' summary list page with pre-filled content" which {
          val pensionScheme = TransferPensionScheme(ukTransferCharge = Some(true), name = Some("Pension Scheme 1"))
          val pensionScheme2 = TransferPensionScheme(ukTransferCharge = Some(true), name = Some("Pension Scheme 2"))
          val newPensionSchemes = Seq(pensionScheme, pensionScheme2)
          val transferViewModel = aTransfersIntoOverseasPensionsViewModel.copy(transferPensionScheme = newPensionSchemes)

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropPensionsDB()
            val viewModel = transferViewModel
            insertCyaData(pensionUserDataWithTransferIntoOverseasPension(viewModel))
            urlGet(fullUrl(transferChargeSummaryUrl(taxYearEOY)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(expectedTitle, user.isWelsh)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(s"${pensionScheme.name.get}", pensionNameSelector(1))
          textOnPageCheck(s"${pensionScheme2.name.get}", pensionNameSelector(2))

          linkCheck(s"$change $change ${pensionScheme.name.get}", changeLinkSelector(1), overseasTransferChargePaidUrl(taxYearEOY, 0))
          linkCheck(s"$remove $remove ${pensionScheme.name.get}", removeLinkSelector(1), removeTransferChargeScheme(taxYearEOY, 0))
          linkCheck(s"$change $change ${pensionScheme2.name.get}", changeLinkSelector(2), overseasTransferChargePaidUrl(taxYearEOY, 1))
          linkCheck(s"$remove $remove ${pensionScheme2.name.get}", removeLinkSelector(2), removeTransferChargeScheme(taxYearEOY, 1))
          linkCheck(expectedAddAnotherText, addAnotherLinkSelector, overseasTransferChargePaidUrl(taxYearEOY))

          buttonCheck(expectedButtonText, continueButtonSelector, Some(checkYourDetailsPensionUrl(taxYearEOY)))
          welshToggleCheck(user.isWelsh)
        }

        "render the 'overseas transfer charge' summary list page with pre-filled content with transfer pension scheme but no name" which {
          val pensionScheme = TransferPensionScheme(ukTransferCharge = Some(true), name = Some("Pension Scheme 1"))
          val pensionScheme2 = TransferPensionScheme(ukTransferCharge = Some(true), name = None)
          val newPensionSchemes = Seq(pensionScheme, pensionScheme2)
          val transferViewModel = aTransfersIntoOverseasPensionsViewModel.copy(transferPensionScheme = newPensionSchemes)

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropPensionsDB()
            val viewModel = transferViewModel
            insertCyaData(pensionUserDataWithTransferIntoOverseasPension(viewModel))
            urlGet(fullUrl(transferChargeSummaryUrl(taxYearEOY)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(expectedTitle, user.isWelsh)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(s"${pensionScheme.name.get}", pensionNameSelector(1))
          elementNotOnPageCheck(pensionNameSelector(2))

          linkCheck(s"$change $change ${pensionScheme.name.get}", changeLinkSelector(1), overseasTransferChargePaidUrl(taxYearEOY, 0))
          linkCheck(s"$remove $remove ${pensionScheme.name.get}", removeLinkSelector(1), removeTransferChargeScheme(taxYearEOY, 0))
          linkCheck(expectedAddAnotherText, addAnotherLinkSelector, overseasTransferChargePaidUrl(taxYearEOY))

          buttonCheck(expectedButtonText, continueButtonSelector, Some(checkYourDetailsPensionUrl(taxYearEOY)))
          welshToggleCheck(user.isWelsh)
        }

        "render the 'overseas transfer charge' summary list page with only an add link when there are no overseas income from pensions" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropPensionsDB()
            val emptyViewModel = emptyTransfersIntoOverseasPensionsViewModel
            insertCyaData(pensionUserDataWithTransferIntoOverseasPension(emptyViewModel))

            urlGet(fullUrl(transferChargeSummaryUrl(taxYearEOY)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(expectedTitle, user.isWelsh)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          elementNotOnPageCheck(summaryListTableSelector)
          //todo update redirect to to transfer journey CYA page when navigation is linked up
          buttonCheck(addASchemeButton, "#AddAScheme", Some(overseasTransferChargePaidUrlNoIndex(taxYearEOY)))
          textOnPageCheck(text1, insetSpanText1)
          buttonCheck(returnToOverviewButton, "#ReturnToOverview")
          textOnPageCheck(text2, insetSpanText2)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "redirect to the first page in the journey" should {
      val incompleteViewModel = aTransfersIntoOverseasPensionsViewModel.copy(
        transferPensionScheme = Seq.empty
      )
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(aUser.isAgent)
        dropPensionsDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        insertCyaData(pensionUserDataWithTransferIntoOverseasPension(incompleteViewModel))

        urlPost(fullUrl(checkYourDetailsPensionUrl(taxYearEOY)),
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)),
          follow = false,
          body = "")
      }

      "if journey is incomplete" in {
      result.status shouldBe SEE_OTHER
      result.header("location") shouldBe Some(transferPensionSavingsUrl(taxYearEOY))
    }
    }

    "redirect to the pensions summary page if there is no session data" should {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        urlGet(fullUrl(transferChargeSummaryUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "have a SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
      }
    }
  }
}
// scalastyle:on magic.number
