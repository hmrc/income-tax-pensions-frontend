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
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.CommonUtils
import utils.PageUrls.OverseasPensionPages.taxEmployerPaymentsUrl
import utils.PageUrls.pensionSummaryUrl

class TaxEmployerPaymentsControllerISpec extends CommonUtils with BeforeAndAfterEach {

  implicit val url: Int => String = taxEmployerPaymentsUrl

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

  //overseasPension.taxEmployerPayments.error.noEntry.agent = Select yes if your client paid tax on the amount their employer paid
  
  
  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle: String = "Did you pay tax on the amount your employer paid?"
    val expectedHeading: String = "Did you pay tax on the amount your employer paid?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val expectedError: String = "Select yes if you paid tax on the amount your employer paid"
    val expectedFindOut: String = "To find out you can:"
    val expectedBullet1: String = "check your pension statement"
    val expectedBullet2: String = "ask your pension provider"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle: String = "Did you pay tax on the amount your employer paid?"
    val expectedHeading: String = "Did you pay tax on the amount your employer paid?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val expectedError: String = "Select yes if you paid tax on the amount your employer paid"
    val expectedFindOut: String = "To find out you can:"
    val expectedBullet1: String = "check your pension statement"
    val expectedBullet2: String = "ask your pension provider"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle: String = "Did your client pay tax on the amount their employer paid?"
    val expectedHeading: String = "Did your client pay tax on the amount their employer paid?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val expectedError: String = "Select yes if your client paid tax on the amount their employer paid"
    val expectedFindOut: String = "To find out you can ask your client to:"
    val expectedBullet1: String = "check their pension statement"
    val expectedBullet2: String = "ask their pension providers"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle: String = "Did your client pay tax on the amount their employer paid?"
    val expectedHeading: String = "Did your client pay tax on the amount their employer paid?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val expectedError: String = "Select yes if your client paid tax on the amount their employer paid"
    val expectedFindOut: String = "To find out you can ask your client to:"
    val expectedBullet1: String = "check their pension statement"
    val expectedBullet2: String = "ask their pension providers"
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

        "render the 'Tax Employer Payments' page with correct content and no pre-filling" which {

          implicit lazy val result: WSResponse = showPage(user, anPensionsUserDataEmptyCya)

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
          formPostLinkCheck(taxEmployerPaymentsUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the 'Tax Employer Payments' page with correct content and 'yes' pre-filled" which {
          val overseasPensionViewModel = aPaymentsIntoOverseasPensionsViewModel.copy(taxPaidOnEmployerPaymentsQuestion = Some(true))
          val pensionsUserData = pensionUserDataWithOverseasPensions(overseasPensionViewModel)

          implicit lazy val result: WSResponse = showPage(user, pensionsUserData)

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
          formPostLinkCheck(taxEmployerPaymentsUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the 'Tax Employer Payments' page with correct content and 'no' pre-filled" which {
          val overseasPensionViewModel = aPaymentsIntoOverseasPensionsViewModel.copy(taxPaidOnEmployerPaymentsQuestion = Some(false))
          val pensionsUserData = pensionUserDataWithOverseasPensions(overseasPensionViewModel)
          implicit lazy val result: WSResponse = showPage(user, pensionsUserData)

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
          formPostLinkCheck(taxEmployerPaymentsUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "redirect to Pensions Summary page if there is no session data" should {
      lazy val result: WSResponse = getResponseNoSessionData
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

          lazy val result: WSResponse = submitPage(user, aPensionsUserData, form)


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
          formPostLinkCheck(taxEmployerPaymentsUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
          errorSummaryCheck(user.specificExpectedResults.get.expectedError, Selectors.yesSelector)
          errorAboveElementCheck(user.specificExpectedResults.get.expectedError, Some("value"))
        }
      }
    }

    "redirect and update question to 'Yes' when user selects yes and there was no previous selection" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
      val pensionsViewModel = aPaymentsIntoOverseasPensionsViewModel.copy(taxPaidOnEmployerPaymentsQuestion = None)
      val pensionUserData = pensionUserDataWithOverseasPensions(pensionsViewModel)
      lazy val result: WSResponse = submitPage(pensionUserData, form)

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(taxEmployerPaymentsUrl(taxYearEOY)) // Todo redirect to SASS-2587
      }

      "updates taxPaidOnEmployerPaymentsQuestion to Some(true)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.paymentsIntoOverseasPensions.employerPaymentsQuestion shouldBe Some(true)
      }
    }

    "redirect and update question to 'Yes' when user selects yes and previously selected no" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
      val pensionsViewModel = aPaymentsIntoOverseasPensionsViewModel.copy(taxPaidOnEmployerPaymentsQuestion = Some(false))
      val pensionUserData = pensionUserDataWithOverseasPensions(pensionsViewModel)

      lazy val result: WSResponse = submitPage(pensionUserData, form)

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(taxEmployerPaymentsUrl(taxYearEOY)) //Todo redirect to SASS-2587
      }

      "updates taxPaidOnEmployerPaymentsQuestion to Some(true)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.paymentsIntoOverseasPensions.employerPaymentsQuestion shouldBe Some(true)
      }
    }

    "redirect and update question to 'No' when user selects no and previously selected yes" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
      val pensionsViewModel = aPaymentsIntoOverseasPensionsViewModel.copy(taxPaidOnEmployerPaymentsQuestion = Some(true))
      val pensionsUserData = pensionUserDataWithOverseasPensions(pensionsViewModel)

      lazy val result: WSResponse = submitPage(pensionsUserData, form)

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(taxEmployerPaymentsUrl(taxYearEOY)) //TODO - redirect to SASS-2588
      }

      "updates taxPaidOnEmployerPaymentsQuestion to Some(false)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.paymentsIntoOverseasPensions.taxPaidOnEmployerPaymentsQuestion shouldBe Some(false)
      }
    }
  }
}
