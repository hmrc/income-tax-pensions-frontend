/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers.pensions.paymentsIntoOverseasPensions

import builders.PaymentsIntoOverseasPensionsViewModelBuilder.aPaymentsIntoOverseasPensionsViewModel
import builders.PensionsUserDataBuilder.{aPensionsUserData, anPensionsUserDataEmptyCya, pensionUserDataWithOverseasPensions}
import builders.UserBuilder.aUserRequest
import forms.YesNoForm
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.overseasPensionPages.employerPayOverseasPensionUrl
import utils.PageUrls.{fullUrl, pensionSummaryUrl}
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

class EmployerPayOverseasPensionControllerISpec extends IntegrationTest with ViewHelpers with BeforeAndAfterEach with PensionsDatabaseHelper {

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedErrorTitle: String
    val expectedError: String
    val expectedFindOut: String
    val expectedBullet1: String
    val expectedBullet2: String
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedButtonText: String
    val yesText: String
    val noText: String
  }

  object Selectors {
    val captionSelector: String = "#main-content > div > div > header > p"
    val continueButtonSelector: String = "#continue"
    val formSelector: String = "#main-content > div > div > form"
    val yesSelector = "#value"
    val noSelector = "#value-no"
    val findOutLinkSelector = "#annual-allowance-link"
    val overLimitLinkSelector = "#over-limit-link"
    val detailsSelector = "#main-content > div > div > form > details > summary > span"

    def paragraphSelector(index: Int): String = s"#main-content > div > div > p:nth-of-type($index)"

    def bulletSelector(index: Int): String = s"#main-content > div > div > ul > li:nth-child($index)"

    def detailsBulletSelector(index: Int): String = s"#main-content > div > div > form > details > div > ul > li:nth-child($index)"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle: String = "Did your employers pay into your overseas pension schemes?"
    val expectedHeading: String = "Did your employers pay into your overseas pension schemes?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val expectedError: String = "Select yes if your employer paid into your overseas pension schemes"
    val expectedFindOut: String = "To find out you can:"
    val expectedBullet1: String = "check your pension statement"
    val expectedBullet2: String = "ask your employer"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle: String = "Did your employers pay into your overseas pension schemes?"
    val expectedHeading: String = "Did your employers pay into your overseas pension schemes?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val expectedError: String = "Select yes if your employer paid into your overseas pension schemes"
    val expectedFindOut: String = "To find out you can:"
    val expectedBullet1: String = "check your pension statement"
    val expectedBullet2: String = "ask your employer"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle: String = "Did your client’s employers pay into the overseas pension schemes?"
    val expectedHeading: String = "Did your client’s employers pay into the overseas pension schemes?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val expectedError: String = "Select yes if your client’s employer paid into their overseas pension schemes"
    val expectedFindOut: String = "To find out you can ask your client to:"
    val expectedBullet1: String = "check their pension statement"
    val expectedBullet2: String = "ask their employer"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle: String = "Did your client’s employers pay into the overseas pension schemes?"
    val expectedHeading: String = "Did your client’s employers pay into the overseas pension schemes?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val expectedError: String = "Select yes if your client’s employer paid into their overseas pension schemes"
    val expectedFindOut: String = "To find out you can ask your client to:"
    val expectedBullet1: String = "check their pension statement"
    val expectedBullet2: String = "ask their employer"
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Payments into overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedButtonText = "Continue"
    val yesText = "Yes"
    val noText = "No"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Payments into overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedButtonText = "Continue"
    val yesText = "Yes"
    val noText = "No"
  }

  ".show" should {
    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        import Selectors._
        import user.commonExpectedResults._

        "render the 'Employer Pay Overseas Pension' page with correct content and no pre-filling" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropPensionsDB()
            insertCyaData(anPensionsUserDataEmptyCya, aUserRequest)
            urlGet(fullUrl(employerPayOverseasPensionUrl(taxYearEOY)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedFindOut, paragraphSelector(1))
          textOnPageCheck(user.specificExpectedResults.get.expectedBullet1, bulletSelector(1))
          textOnPageCheck(user.specificExpectedResults.get.expectedBullet2, bulletSelector(2))
          radioButtonCheck(yesText, 1, checked = Some(false))
          radioButtonCheck(noText, 2, checked = Some(false))
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(employerPayOverseasPensionUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the 'Employer Pay Overseas Pension' page with correct content and yes pre-filled" which {

          implicit lazy val result: WSResponse = {
            dropPensionsDB()
            val overseasPensionViewModel = aPaymentsIntoOverseasPensionsViewModel.copy(employerPaymentsQuestion = Some(true))
            insertCyaData(pensionUserDataWithOverseasPensions(overseasPensionViewModel), aUserRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(fullUrl(employerPayOverseasPensionUrl(taxYearEOY)), user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedFindOut, paragraphSelector(1))
          textOnPageCheck(user.specificExpectedResults.get.expectedBullet1, bulletSelector(1))
          textOnPageCheck(user.specificExpectedResults.get.expectedBullet2, bulletSelector(2))
          radioButtonCheck(yesText, 1, checked = Some(true))
          radioButtonCheck(noText, 2, checked = Some(false))
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(employerPayOverseasPensionUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the 'Employer Pay Overseas Pension' page with correct content and no pre-filled" which {

          implicit lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            val overseasPensionViewModel = aPaymentsIntoOverseasPensionsViewModel.copy(employerPaymentsQuestion = Some(false))
            insertCyaData(pensionUserDataWithOverseasPensions(overseasPensionViewModel), aUserRequest)
            urlGet(fullUrl(employerPayOverseasPensionUrl(taxYearEOY)), user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedFindOut, paragraphSelector(1))
          textOnPageCheck(user.specificExpectedResults.get.expectedBullet1, bulletSelector(1))
          textOnPageCheck(user.specificExpectedResults.get.expectedBullet2, bulletSelector(2))
          radioButtonCheck(yesText, 1, checked = Some(false))
          radioButtonCheck(noText, 2, checked = Some(true))
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(employerPayOverseasPensionUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "redirect to Pensions Summary page if there is no session data" should {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(employerPayOverseasPensionUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY)) //todo
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
            urlPost(fullUrl(employerPayOverseasPensionUrl(taxYearEOY)), body = form, follow = false, welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)
          import Selectors._
          import user.commonExpectedResults._

          titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedFindOut, paragraphSelector(1))
          textOnPageCheck(user.specificExpectedResults.get.expectedBullet1, bulletSelector(1))
          textOnPageCheck(user.specificExpectedResults.get.expectedBullet2, bulletSelector(2))
          radioButtonCheck(yesText, 1, checked = Some(false))
          radioButtonCheck(noText, 2, checked = Some(false))
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(employerPayOverseasPensionUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
          errorSummaryCheck(user.specificExpectedResults.get.expectedError, Selectors.yesSelector)
          errorAboveElementCheck(user.specificExpectedResults.get.expectedError, Some("value"))
        }
      }
    }

    "redirect and update question to 'Yes' when user selects yes when there is no cya data" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val pensionsViewModel = aPaymentsIntoOverseasPensionsViewModel.copy(employerPaymentsQuestion = None)
        insertCyaData(pensionUserDataWithOverseasPensions(pensionsViewModel), aUserRequest)
        urlPost(fullUrl(employerPayOverseasPensionUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(employerPayOverseasPensionUrl(taxYearEOY))
      }

      "updates employerPaymentsQuestion to Some(true)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.paymentsIntoOverseasPensions.employerPaymentsQuestion shouldBe Some(true)
      }
    }

    "redirect and update question to 'Yes' when user selects yes and cya data exists" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val pensionsViewModel = aPaymentsIntoOverseasPensionsViewModel.copy(employerPaymentsQuestion = None)
        insertCyaData(pensionUserDataWithOverseasPensions(pensionsViewModel), aUserRequest)
        urlPost(fullUrl(employerPayOverseasPensionUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(employerPayOverseasPensionUrl(taxYearEOY))
      }

      "updates employerPaymentsQuestion to Some(true)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.paymentsIntoOverseasPensions.employerPaymentsQuestion shouldBe Some(true)
      }
    }

    "redirect and update question to 'No' when user selects no and cya data exists" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val pensionsViewModel = aPaymentsIntoOverseasPensionsViewModel.copy(employerPaymentsQuestion = Some(true))
        insertCyaData(pensionUserDataWithOverseasPensions(pensionsViewModel), aUserRequest)
        urlPost(fullUrl(employerPayOverseasPensionUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }
      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(employerPayOverseasPensionUrl(taxYearEOY))
      }

      "updates employerPaymentsQuestion to Some(false)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.paymentsIntoOverseasPensions.employerPaymentsQuestion shouldBe Some(false)
      }
    }
  }
}
