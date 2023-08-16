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

package controllers.pensions.lifetimeAllowances

import builders.LifetimeAllowanceBuilder.aLifetimeAllowance1
import builders.PensionLifetimeAllowancesViewModelBuilder.aPensionLifetimeAllowancesViewModel
import builders.PensionsUserDataBuilder.{aPensionsUserData, pensionsUserDataWithLifetimeAllowance}
import builders.UserBuilder.aUserRequest
import forms.YesNoForm
import models.pension.charges.{LifetimeAllowance, PensionLifetimeAllowancesViewModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.PensionLifetimeAllowance._
import utils.PageUrls.{fullUrl, pensionSummaryUrl}
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

class LifeTimeAllowanceAnotherWayControllerISpec extends IntegrationTest with BeforeAndAfterEach with ViewHelpers with PensionsDatabaseHelper {

  object Selectors {
    val captionSelector: String = "#main-content > div > div > header > p"
    val continueButtonSelector: String = "#continue"
    val formSelector: String = "#main-content > div > div > form"
    val yesSelector = "#value"
    val noSelector = "#value-no"

    def paragraphSelector(index: Int): String = s"#main-content > div > div > p:nth-child($index)"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    lazy val expectedHeading: String = expectedTitle
    val expectedParagraphText: String
    val expectedErrorTitle: String
    val expectedError: String
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedButtonText: String
    val yesText: String
    val noText: String
    val expectedLumpSumText: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Did you take the amount above your lifetime allowance another way?"
    val expectedParagraphText: String = "Tell us about any amount you’ve taken above your lifetime allowance in other " +
      "ways. This could be regular payments or a cash withdrawal"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if you took the amount above your lifetime allowance in another way"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "A wnaethoch gymryd y swm sy’n uwch na’ch lwfans oes ffordd arall?"
    val expectedParagraphText: String = "Rhowch wybod i ni am unrhyw swm rydych wedi’i gymryd sy’n uwch na’ch lwfans oes " +
      "mewn ffyrdd eraill. Gallai hyn fod yn daliadau rheolaidd neu’n tynnu’n ôl arian"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedError = "Dewiswch ‘Iawn’ os wnaethoch gymryd y swm sy’n uwch na’ch lwfans oes mewn ffordd arall"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Did your client take the amount above their lifetime allowance another way?"
    val expectedParagraphText: String = "Tell us about any amount your client has taken above their lifetime allowance in " +
      "other ways. This could be regular payments or a cash withdrawal"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if your client took the amount above their lifetime allowance in another way"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "A wnaeth eich cleient gymryd y swm sy’n uwch na’ch lwfans oes ffordd arall?"
    val expectedParagraphText: String = "Rhowch wybod i ni am unrhyw swm y mae’ch cleient wedi’i gymryd sy’n uwch na’u " +
      "lwfans oes mewn ffyrdd eraill. Gallai hyn fod yn daliadau rheolaidd neu’n tynnu’n ôl arian"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedError = "Dewiswch ‘Iawn’ os wnaeth eich cleient gymryd y swm sy’n uwch na’ch lwfans oes mewn ffordd arall"
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Lifetime allowances for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedButtonText = "Continue"
    val yesText = "Yes"
    val noText = "No"
    val expectedLumpSumText: String = "Do not tell us about lump sums."
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Lwfansau oes ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val expectedButtonText = "Yn eich blaen"
    val yesText = "Iawn"
    val noText = "Na"
    val expectedLumpSumText: String = "Peidiwch â rhoi gwybod i ni am gyfandaliadau."
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  ".show" should { //scalastyle:off magic.number
    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        import Selectors._
        import user.commonExpectedResults._

        "render the 'Did you take the amount above your lifetime allowance another way?' page with correct content and no pre-filling" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropPensionsDB()
            val pensionsViewModel = PensionLifetimeAllowancesViewModel(
              aboveLifetimeAllowanceQuestion = Some(true),
              pensionAsLumpSumQuestion = Some(true),
              pensionAsLumpSum = Some(aLifetimeAllowance1))
            insertCyaData(pensionsUserDataWithLifetimeAllowance(pensionsViewModel))
            urlGet(fullUrl(pensionLifeTimeAllowanceAnotherWayUrl(taxYearEOY)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(expectedLumpSumText, paragraphSelector(2))
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraphText, paragraphSelector(3))
          radioButtonCheck(yesText, 1, checked = Some(false))
          radioButtonCheck(noText, 2, checked = Some(false))
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(pensionLifeTimeAllowanceAnotherWayUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the 'Did you take the amount above your lifetime allowance another way?' page with correct content and yes pre-filled" which {

          implicit lazy val result: WSResponse = {
            dropPensionsDB()
            val pensionsViewModel = aPensionLifetimeAllowancesViewModel.copy(
              pensionPaidAnotherWayQuestion = Some(true)
            )
            insertCyaData(pensionsUserDataWithLifetimeAllowance(pensionsViewModel))
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(fullUrl(pensionLifeTimeAllowanceAnotherWayUrl(taxYearEOY)), user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(expectedLumpSumText, paragraphSelector(2))
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraphText, paragraphSelector(3))
          radioButtonCheck(yesText, 1, checked = Some(true))
          radioButtonCheck(noText, 2, checked = Some(false))
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(pensionLifeTimeAllowanceAnotherWayUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the 'Did you take the amount above your lifetime allowance another way?' page with correct content and no pre-filled" which {

          implicit lazy val result: WSResponse = {
            dropPensionsDB()

            val pensionsViewModel = aPensionLifetimeAllowancesViewModel.copy(
              pensionPaidAnotherWayQuestion = Some(false)
            )
            insertCyaData(pensionsUserDataWithLifetimeAllowance(pensionsViewModel))
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(fullUrl(pensionLifeTimeAllowanceAnotherWayUrl(taxYearEOY)), user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(expectedLumpSumText, paragraphSelector(2))
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraphText, paragraphSelector(3))
          radioButtonCheck(yesText, 1, checked = Some(false))
          radioButtonCheck(noText, 2, checked = Some(true))
          buttonCheck(expectedButtonText, continueButtonSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "redirect to Pensions Summary page if there is no session data" should {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        urlGet(fullUrl(pensionLifeTimeAllowanceAnotherWayUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
      }
    }
  }

  ".submit" should {
    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        s"return $BAD_REQUEST error when no value is submitted" which {
          lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> "")

          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(aPensionsUserData)
            urlPost(fullUrl(pensionLifeTimeAllowanceAnotherWayUrl(taxYearEOY)), body = form, follow = false, welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)
          import Selectors._
          import user.commonExpectedResults._
          titleCheck(user.specificExpectedResults.get.expectedErrorTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          textOnPageCheck(expectedLumpSumText, paragraphSelector(3))
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraphText, paragraphSelector(4))
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          radioButtonCheck(yesText, 1, checked = Some(false))
          radioButtonCheck(noText, 2, checked = Some(false))
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(pensionLifeTimeAllowanceAnotherWayUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)

          errorSummaryCheck(user.specificExpectedResults.get.expectedError, Selectors.yesSelector)
          errorAboveElementCheck(user.specificExpectedResults.get.expectedError, Some("value"))
        }
      }
    }

    "update question to 'true' and redirect to PaidAnotherWayAmount page when user selects 'Yes'" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
      val pensionsViewModel = aPensionLifetimeAllowancesViewModel.copy(
        pensionPaidAnotherWayQuestion = None, pensionPaidAnotherWay = None, pensionSchemeTaxReferences = None)

      lazy val result: WSResponse = {
        dropPensionsDB()
        insertCyaData(pensionsUserDataWithLifetimeAllowance(pensionsViewModel))
        authoriseAgentOrIndividual()
        urlPost(fullUrl(pensionLifeTimeAllowanceAnotherWayUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionTakenAnotherWayAmountUrl(taxYearEOY))
      }

      "updates pensionPaidAnotherWayQuestion to Some(true)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.pensionLifetimeAllowances shouldBe pensionsViewModel.copy(pensionPaidAnotherWayQuestion = Some(true))
      }
    }

    "update question to 'false', clear data and redirect to CYA page when user selects 'No'" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
      val pensionsViewModel = aPensionLifetimeAllowancesViewModel.copy(
        pensionPaidAnotherWayQuestion = Some(true),
        pensionPaidAnotherWay = Some(LifetimeAllowance(Some(999.99), Some(99.99)))
      )

      lazy val result: WSResponse = {
        dropPensionsDB()
        insertCyaData(pensionsUserDataWithLifetimeAllowance(pensionsViewModel))
        authoriseAgentOrIndividual()
        urlPost(fullUrl(pensionLifeTimeAllowanceAnotherWayUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has a SEE_OTHER(303) status and redirect to the PSTR summary Page" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(lifetimeAllowanceCYA(taxYearEOY))
      }

      "updates pensionPaidAnotherWayQuestion to Some(false) and clears further data" in {
        val expectedViewModel = aPensionLifetimeAllowancesViewModel.copy(
          pensionPaidAnotherWayQuestion = Some(false), pensionPaidAnotherWay = None, pensionSchemeTaxReferences = None)
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.pensionLifetimeAllowances shouldBe expectedViewModel
      }
    }

    "redirect to Pensions Summary page if there is no session data" should {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        urlPost(fullUrl(pensionLifeTimeAllowanceAnotherWayUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
      }
    }
  }

}
