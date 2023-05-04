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
import utils.PageUrls.PensionAnnualAllowancePages.reducedAnnualAllowanceUrl
import utils.PageUrls.PensionLifetimeAllowance.{checkAnnualLifetimeAllowanceCYA, pensionAboveAnnualLifetimeAllowanceUrl}
import utils.PageUrls.fullUrl
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

class AboveAnnualLifetimeAllowanceControllerISpec extends IntegrationTest with BeforeAndAfterEach with ViewHelpers with PensionsDatabaseHelper {

  object Selectors {
    val captionSelector: String = "#main-content > div > div > header > p"
    val continueButtonSelector: String = "#continue"
    val formSelector: String = "#main-content > div > div > form"
    val yesSelector = "#value"
    val noSelector = "#value-no"
    val taxReliefLinkSelector = "#aboveAnnualLifetimeAllowance-link"
    val paragraphSelector = "#main-content > div > div > p"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    lazy val expectedHeading: String = expectedTitle
    val expectedParagraph: String
    val expectedErrorTitle: String
    val expectedError: String
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedButtonText: String
    val yesText: String
    val noText: String
    val infoLinkText: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Have you gone above your lifetime allowance?"
    val expectedParagraph = "Your pension providers would have told you if you went above your lifetime allowance."
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if you have gone above your lifetime allowance"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Have you gone above your lifetime allowance?"
    val expectedParagraph = "Byddai’ch darparwyr pensiwn wedi rhoi gwybod i chi pe baech yn mynd dros eich lwfans oes."
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedError = "Select yes if you have gone above your lifetime allowance"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Has your client gone above their lifetime allowance?"
    val expectedParagraph = "Your client’s pension providers would have told them if they went above their lifetime allowance."
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if your client has gone above their lifetime allowance"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Has your client gone above their lifetime allowance?"
    val expectedParagraph = "Byddai darparwyr pensiwn eich cleient wedi rhoi gwybod iddo os oedd wedi mynd dros ei lwfans oes."
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedError = "Select yes if your client has gone above their lifetime allowance"
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Annual and lifetime allowances for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedButtonText = "Continue"
    val yesText = "Yes"
    val noText = "No"
    val infoLinkText = "Find out more about lifetime allowance (opens in new tab)"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Lwfans blynyddol a lwfans oes ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val expectedButtonText = "Yn eich blaen"
    val yesText = "Iawn"
    val noText = "Na"
    val infoLinkText = "Find out more about lifetime allowance (opens in new tab)"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  val linkHref = "https://www.gov.uk/tax-on-your-private-pension/lifetime-allowance"

  ".show" should {
    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        import Selectors._
        import user.commonExpectedResults._

        def commonContent(implicit document: () => Document): Unit = {
          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph, paragraphSelector)
          textOnPageCheck(infoLinkText, taxReliefLinkSelector)
          linkCheck(infoLinkText, taxReliefLinkSelector, linkHref)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(pensionAboveAnnualLifetimeAllowanceUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the 'Have you gone above your lifetime allowance?' page with correct content and no pre-filling" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropPensionsDB()
            insertCyaData(anPensionsUserDataEmptyCya, aUserRequest)
            urlGet(fullUrl(pensionAboveAnnualLifetimeAllowanceUrl(taxYearEOY)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          commonContent
          radioButtonCheck(yesText, 1, checked = Some(false))
          radioButtonCheck(noText, 2, checked = Some(false))
        }

        "render the 'Have you gone above your lifetime allowance?' page with correct content and yes pre-filled" which {

          implicit lazy val result: WSResponse = {
            dropPensionsDB()
            val pensionsViewModel = aPensionLifetimeAllowanceViewModel.copy(
              aboveLifetimeAllowanceQuestion = Some(true)
            )
            insertCyaData(pensionsUserDataWithLifetimeAllowance(pensionsViewModel), aUserRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(fullUrl(pensionAboveAnnualLifetimeAllowanceUrl(taxYearEOY)), user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          commonContent
          radioButtonCheck(yesText, 1, checked = Some(true))
          radioButtonCheck(noText, 2, checked = Some(false))
        }

        "render the 'Have you gone above your lifetime allowance?' page with correct content and no pre-filled" which {

          implicit lazy val result: WSResponse = {
            dropPensionsDB()

            val pensionsViewModel = aPensionLifetimeAllowanceViewModel.copy(
              aboveLifetimeAllowanceQuestion = Some(false)
            )
            insertCyaData(pensionsUserDataWithLifetimeAllowance(pensionsViewModel), aUserRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(fullUrl(pensionAboveAnnualLifetimeAllowanceUrl(taxYearEOY)), user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          commonContent
          radioButtonCheck(yesText, 1, checked = Some(false))
          radioButtonCheck(noText, 2, checked = Some(true))
        }
      }
    }

    "redirect to CYA page if there is no session data" should {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(pensionAboveAnnualLifetimeAllowanceUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(checkAnnualLifetimeAllowanceCYA(taxYearEOY))
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
            urlPost(fullUrl(pensionAboveAnnualLifetimeAllowanceUrl(taxYearEOY)), body = form, follow = false, welsh = user.isWelsh,
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
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph, paragraphSelector)
          textOnPageCheck(infoLinkText, taxReliefLinkSelector)
          linkCheck(infoLinkText, taxReliefLinkSelector, linkHref)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          radioButtonCheck(yesText, 1, checked = Some(false))
          radioButtonCheck(noText, 2, checked = Some(false))
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(pensionAboveAnnualLifetimeAllowanceUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)

          errorSummaryCheck(user.specificExpectedResults.get.expectedError, Selectors.yesSelector)
          errorAboveElementCheck(user.specificExpectedResults.get.expectedError, Some("value"))
        }
      }
    }

    "redirect to Reduced Annual Allowance page and update question to 'Yes'" when {

      "there is currently no radio button value selected and the user selects 'Yes'" which {
        lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

        lazy val result: WSResponse = {
          dropPensionsDB()
          val pensionsViewModel = aPensionLifetimeAllowanceViewModel.copy(
            aboveLifetimeAllowanceQuestion = None
          )

          insertCyaData(pensionsUserDataWithLifetimeAllowance(pensionsViewModel), aUserRequest)

          authoriseAgentOrIndividual()
          urlPost(fullUrl(pensionAboveAnnualLifetimeAllowanceUrl(taxYearEOY)), body = form, follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        "has a SEE_OTHER(303) status and redirect Do you have a reduced annual allowance page" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(reducedAnnualAllowanceUrl(taxYearEOY))
        }

        "updates aboveLifetimeAllowanceQuestion to Some(true)" in {
          lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
          cyaModel.pensions.pensionLifetimeAllowances.aboveLifetimeAllowanceQuestion shouldBe Some(true)
          cyaModel.pensions.pensionLifetimeAllowances.pensionAsLumpSumQuestion shouldBe aPensionLifetimeAllowanceViewModel.pensionAsLumpSumQuestion
          cyaModel.pensions.pensionLifetimeAllowances.pensionAsLumpSum shouldBe aPensionLifetimeAllowanceViewModel.pensionAsLumpSum
          cyaModel.pensions.pensionLifetimeAllowances.pensionPaidAnotherWayQuestion shouldBe
            aPensionLifetimeAllowanceViewModel.pensionPaidAnotherWayQuestion
          cyaModel.pensions.pensionLifetimeAllowances.pensionPaidAnotherWay shouldBe aPensionLifetimeAllowanceViewModel.pensionPaidAnotherWay
        }
      }
    }

    "redirect to CYA page and update question to 'No'" when {
      "there is currently no radio button value selected and the user selects 'No'" which {
        lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)

        lazy val result: WSResponse = {
          dropPensionsDB()
          val pensionsViewModel = aPensionLifetimeAllowanceViewModel.copy(
            aboveLifetimeAllowanceQuestion = None
          )

          insertCyaData(pensionsUserDataWithLifetimeAllowance(pensionsViewModel), aUserRequest)

          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(pensionAboveAnnualLifetimeAllowanceUrl(taxYearEOY)), body = form, follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        "has a SEE_OTHER(303) status and redirect to the lifetime allowances CYA Page" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(checkAnnualLifetimeAllowanceCYA(taxYearEOY))
        }

        "updates aboveLifetimeAllowanceQuestion to Some(false) and clear the rest of the annual lifetime allowance data" in {
          findCyaData(taxYearEOY, aUserRequest).foreach { cyaModel =>
            cyaModel.pensions.pensionLifetimeAllowances.aboveLifetimeAllowanceQuestion shouldBe Some(false)
            cyaModel.pensions.pensionLifetimeAllowances.pensionAsLumpSumQuestion shouldBe None
            cyaModel.pensions.pensionLifetimeAllowances.pensionAsLumpSum shouldBe None
            cyaModel.pensions.pensionLifetimeAllowances.pensionPaidAnotherWayQuestion shouldBe None
            cyaModel.pensions.pensionLifetimeAllowances.pensionPaidAnotherWay shouldBe None
          }
        }
      }
    }
  }

  "redirect to CYA page if there is no session data" should {
    lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
    lazy val result: WSResponse = {
      dropPensionsDB()
      authoriseAgentOrIndividual(isAgent = false)
      urlPost(fullUrl(pensionAboveAnnualLifetimeAllowanceUrl(taxYearEOY)), body = form, follow = false,
        headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))

    }
    "has an SEE_OTHER status" in {
      result.status shouldBe SEE_OTHER
      result.header("location") shouldBe Some(checkAnnualLifetimeAllowanceCYA(taxYearEOY))
    }
  }
}