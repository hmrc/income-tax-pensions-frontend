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

import builders.PensionLifetimeAllowanceViewModelBuilder.aPensionLifetimeAllowanceViewModel
import builders.PensionsUserDataBuilder.{aPensionsUserData, anPensionsUserDataEmptyCya, pensionsUserDataWithLifetimeAllowance}
import builders.UserBuilder.aUserRequest
import forms.YesNoForm
import models.pension.charges.LifetimeAllowance
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.PensionLifetimeAllowance.{pensionLifeTimeAllowanceAnotherWayUrl, pensionTakenAnotherWayAmountUrl}
import utils.PageUrls.{fullUrl, pensionSummaryUrl}
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

class LifeTimeAllowanceAnotherWayControllerISpec extends IntegrationTest with BeforeAndAfterEach with ViewHelpers with PensionsDatabaseHelper {

  object Selectors {
    val captionSelector: String = "#main-content > div > div > header > p"
    val continueButtonSelector: String = "#continue"
    val formSelector: String = "#main-content > div > div > form"
    val yesSelector = "#value"
    val noSelector = "#value-no"
    def paragraphSelector(index : Int) : String = s"#main-content > div > div > p:nth-child($index)"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    lazy val expectedHeading = expectedTitle
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
    val expectedParagraphText = "Tell us about any amount you’ve taken above your lifetime allowance in other " +
      "ways. This could be regular payments or a cash withdrawal"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if you took the amount above your lifetime allowance in another way"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "A wnaethoch gymryd y swm sy’n uwch na’ch lwfans oes ffordd arall?"
    val expectedParagraphText = "Rhowch wybod i ni am unrhyw swm rydych wedi’i gymryd sy’n uwch na’ch lwfans oes " +
      "mewn ffyrdd eraill. Gallai hyn fod yn daliadau rheolaidd neu’n tynnu’n ôl arian"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedError = "Dewiswch ‘Iawn’ os wnaethoch gymryd y swm sy’n uwch na’ch lwfans oes mewn ffordd arall"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Did your client take the amount above their lifetime allowance another way?"
    val expectedParagraphText = "Tell us about any amount your client has taken above their lifetime allowance in " +
      "other ways. This could be regular payments or a cash withdrawal"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if your client took the amount above their lifetime allowance in another way"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "A wnaeth eich cleient gymryd y swm sy’n uwch na’ch lwfans oes ffordd arall?"
    val expectedParagraphText = "Rhowch wybod i ni am unrhyw swm y mae’ch cleient wedi’i gymryd sy’n uwch na’u " +
      "lwfans oes mewn ffyrdd eraill. Gallai hyn fod yn daliadau rheolaidd neu’n tynnu’n ôl arian"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedError = "Dewiswch ‘Iawn’ os wnaeth eich cleient gymryd y swm sy’n uwch na’ch lwfans oes mewn ffordd arall"
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Annual and lifetime allowances for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedButtonText = "Continue"
    val yesText = "Yes"
    val noText = "No"
    val expectedLumpSumText: String = "Do not tell us about lump sums."
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Lwfans blynyddol a lwfans oes ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
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
            insertCyaData(anPensionsUserDataEmptyCya, aUserRequest)
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
            val pensionsViewModel = aPensionLifetimeAllowanceViewModel.copy(
              pensionPaidAnotherWayQuestion = Some(true)
            )
            insertCyaData(pensionsUserDataWithLifetimeAllowance(pensionsViewModel), aUserRequest)
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

            val pensionsViewModel = aPensionLifetimeAllowanceViewModel.copy(
              pensionPaidAnotherWayQuestion = Some(false)
            )
            insertCyaData(pensionsUserDataWithLifetimeAllowance(pensionsViewModel), aUserRequest)
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
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(pensionLifeTimeAllowanceAnotherWayUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        //TODO - redirect to CYA page once implemented
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
            insertCyaData(aPensionsUserData, aUserRequest)
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

    "redirect and update question to 'Yes' when there is currently no radio button value selected and the user selects 'Yes'" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

      lazy val result: WSResponse = {
        dropPensionsDB()
        val pensionsViewModel = aPensionLifetimeAllowanceViewModel.copy(
          pensionPaidAnotherWayQuestion = None
        )

        insertCyaData(pensionsUserDataWithLifetimeAllowance(pensionsViewModel), aUserRequest)

        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(pensionLifeTimeAllowanceAnotherWayUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has a SEE_OTHER(303) status and redirect Do you have a reduced annual allowance page" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionTakenAnotherWayAmountUrl(taxYearEOY))
      }

      "updates pensionPaidAnotherWayQuestion to Some(true)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.pensionLifetimeAllowances.pensionPaidAnotherWayQuestion shouldBe Some(true)
        cyaModel.pensions.pensionLifetimeAllowances.pensionAsLumpSumQuestion shouldBe aPensionLifetimeAllowanceViewModel.pensionAsLumpSumQuestion
        cyaModel.pensions.pensionLifetimeAllowances.pensionAsLumpSum shouldBe aPensionLifetimeAllowanceViewModel.pensionAsLumpSum
        cyaModel.pensions.pensionLifetimeAllowances.pensionPaidAnotherWay shouldBe aPensionLifetimeAllowanceViewModel.pensionPaidAnotherWay
        cyaModel.pensions.pensionLifetimeAllowances.aboveLifetimeAllowanceQuestion shouldBe
          aPensionLifetimeAllowanceViewModel.aboveLifetimeAllowanceQuestion
      }
    }

    "redirect and update question to 'No' when there is currently radio button value is currently set to Some(true) and the user selects 'No'" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)

      lazy val result: WSResponse = {
        dropPensionsDB()
        val pensionsViewModel = aPensionLifetimeAllowanceViewModel.copy(
          pensionPaidAnotherWayQuestion = Some(true),
          pensionPaidAnotherWay = LifetimeAllowance(Some(999.99), Some(99.99))
        )

        insertCyaData(pensionsUserDataWithLifetimeAllowance(pensionsViewModel), aUserRequest)

        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(pensionLifeTimeAllowanceAnotherWayUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }


      "has a SEE_OTHER(303) status and redirect to the lifetime allowances CYA Page" in {
        result.status shouldBe SEE_OTHER
        //TODO redirect page to the Annual and lifetime allowances CYA Page
        result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
      }

      "updates aboveLifetimeAllowanceQuestion to Some(false) and clear the rest of the annual lifetime allowance data" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.pensionLifetimeAllowances.pensionPaidAnotherWay shouldBe LifetimeAllowance(None,None)
        cyaModel.pensions.pensionLifetimeAllowances.pensionPaidAnotherWayQuestion shouldBe Some(false)
        cyaModel.pensions.pensionLifetimeAllowances.pensionAsLumpSumQuestion shouldBe aPensionLifetimeAllowanceViewModel.pensionAsLumpSumQuestion
        cyaModel.pensions.pensionLifetimeAllowances.pensionAsLumpSum shouldBe aPensionLifetimeAllowanceViewModel.pensionAsLumpSum
        cyaModel.pensions.pensionLifetimeAllowances.aboveLifetimeAllowanceQuestion shouldBe aPensionLifetimeAllowanceViewModel.aboveLifetimeAllowanceQuestion
      }
    }

    "redirect to Pensions Summary page if there is no session data" should {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(pensionLifeTimeAllowanceAnotherWayUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))

      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        //TODO - redirect to "Check your client's annual and lifetime allowances" Page
        result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
      }
    }
  }
}
