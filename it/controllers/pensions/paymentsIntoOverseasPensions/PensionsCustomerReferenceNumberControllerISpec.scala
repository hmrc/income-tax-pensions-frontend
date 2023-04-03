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

package controllers.pensions.paymentsIntoOverseasPensions

import builders.PaymentsIntoOverseasPensionsViewModelBuilder.aPaymentsIntoOverseasPensionsViewModel
import builders.PensionsUserDataBuilder.{aPensionsUserData, anPensionsUserDataEmptyCya, pensionUserDataWithOverseasPensions}
import builders.UserBuilder.aUserRequest
import forms.PensionCustomerReferenceNumberForm
import models.pension.charges.Relief
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.libs.ws.WSResponse
import utils.CommonUtils
import utils.PageUrls.{PaymentIntoOverseasPensions, pensionSummaryUrl}
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}


class PensionsCustomerReferenceNumberControllerISpec extends CommonUtils with BeforeAndAfterEach{
  object Selectors {
    val captionSelector: String = "#main-content > div > div > header > p"
    val continueButtonSelector: String = "#continue"
    val formSelector: String = "#main-content > div > div > form"
    val inputSelector = "#pensionsCustomerReferenceNumberId"
    val hintTextSelector = "#pensionsCustomerReferenceNumberId-hint"

    def labelSelector(index: Int): String = s"form > div:nth-of-type($index) > label"

    def paragraphSelector(index: Int): String = s"#main-content > div > div > p:nth-of-type($index)"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    lazy val expectedHeading = expectedTitle
    val expectedErrorTitle: String
    val expectedParagraph1: String
    val expectedIncorrectFormatError: String
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val hintText: String
    val expectedButtonText: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle: String = "What’s your customer reference number?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val expectedParagraph1: String = "Your pension provider should have given you a customer reference number."
    val expectedIncorrectFormatError: String = "Enter your customer reference number"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle: String = "What’s your customer reference number?"
    val expectedErrorTitle: String = s"Gwall: $expectedTitle"
    val expectedParagraph1: String = "Your pension provider should have given you a customer reference number."
    val expectedIncorrectFormatError: String = "Enter your customer reference number"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle: String = "What’s your client’s customer reference number?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val expectedParagraph1: String = "Your client’s pension provider should have given them a customer reference number."
    val expectedIncorrectFormatError: String = "Enter your client’s customer reference number"

  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle: String = "What’s your client’s customer reference number?"
    val expectedErrorTitle: String = s"Gwall: $expectedTitle"
    val expectedParagraph1: String = "Your client’s pension provider should have given them a customer reference number."
    val expectedIncorrectFormatError: String = "Enter your client’s customer reference number"
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear:Int) => s"Payments into overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val hintText: String = "For example, 'PENSIONINCOME245'"
    val expectedButtonText: String = "Continue"

  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Taliadau i bensiynau tramor ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val hintText: String = "For example, 'PENSIONINCOME245'"
    val expectedButtonText: String = "Yn eich blaen"
  }

  val inputName: String = "pensionsCustomerReferenceNumberId"
  implicit val pensionCustomerReferenceNumberUrl: Int => String = (taxYear: Int) => PaymentIntoOverseasPensions.pensionCustomerReferenceNumberUrl(taxYear)

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  ".show" should {
    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        import Selectors._
        import user.commonExpectedResults._

        "render the customer reference number page with correct content and no pre-filling" which {
          implicit lazy val result: WSResponse = showPage(user, anPensionsUserDataEmptyCya)


          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph1, paragraphSelector(1))
          textOnPageCheck(hintText, hintTextSelector)
          inputFieldValueCheck(inputName, inputSelector, "")
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(pensionCustomerReferenceNumberUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the page with correct content with pre-filling" which {
          val pensionsCustomerReferenceNumber = "PENSIONSINCOME245"

          val relief = Relief(customerReferenceNumberQuestion = Some(pensionsCustomerReferenceNumber))

          val pensionsViewModel = aPaymentsIntoOverseasPensionsViewModel.copy(
            reliefs = Seq(relief))

          val pensionUserData = pensionUserDataWithOverseasPensions(pensionsViewModel)
          implicit lazy val result: WSResponse = showPage(user, pensionUserData)

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph1, paragraphSelector(1))
          textOnPageCheck(hintText, hintTextSelector)
          inputFieldValueCheck(inputName, inputSelector, pensionsCustomerReferenceNumber)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(pensionCustomerReferenceNumberUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "Redirect to the pension summary page if there is no session data" should {
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

        s"return $BAD_REQUEST error when empty value is submitted" which {
          lazy val form: Map[String, String] = Map(PensionCustomerReferenceNumberForm.pensionsCustomerReferenceNumberId -> "")
          lazy val result: WSResponse = submitPage(user, aPensionsUserData, form)


          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)
          import Selectors._
          import user.commonExpectedResults._
          titleCheck(user.specificExpectedResults.get.expectedErrorTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph1, paragraphSelector(1))
          textOnPageCheck(hintText, hintTextSelector)
          inputFieldValueCheck(inputName, inputSelector, "")
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(pensionCustomerReferenceNumberUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
          errorSummaryCheck(user.specificExpectedResults.get.expectedIncorrectFormatError, inputSelector)
          errorAboveElementCheck(user.specificExpectedResults.get.expectedIncorrectFormatError)
        }
      }
    }

    "redirect and update question to contain customer reference number when no cya data exists" which {
      lazy val form: Map[String, String] = Map(PensionCustomerReferenceNumberForm.pensionsCustomerReferenceNumberId -> "PENSIONAINCOME245")

      val relief = Relief()

      val pensionsViewModel = aPaymentsIntoOverseasPensionsViewModel.copy(
        reliefs = Seq(relief))

      val pensionUserData = pensionUserDataWithOverseasPensions(pensionsViewModel)

      lazy val result: WSResponse = submitPage(pensionUserData, form)

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionCustomerReferenceNumberUrl(taxYearEOY))
      }

      "updates pension scheme customer reference number  reference to contain tax reference" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.paymentsIntoOverseasPensions.reliefs.head.customerReferenceNumberQuestion.get shouldBe "PENSIONAINCOME245"
      }
    }

    "redirect and update contain customer reference number when cya data exists" which {
      lazy val form: Map[String, String] = Map(PensionCustomerReferenceNumberForm.pensionsCustomerReferenceNumberId -> "PENSIONAINCOME245")
      val relief = Relief(customerReferenceNumberQuestion = Some("PENSIONAINCOME480"))

      val pensionsViewModel = aPaymentsIntoOverseasPensionsViewModel.copy(
        reliefs = Seq(relief))
      val pensionUserData = pensionUserDataWithOverseasPensions(pensionsViewModel)

      lazy val result: WSResponse = submitPage(pensionUserData, form)


      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionCustomerReferenceNumberUrl(taxYearEOY)) //todo redirect to correct page
      }

      "updates pension scheme customer reference number reference " in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.paymentsIntoOverseasPensions.reliefs.head.customerReferenceNumberQuestion.get shouldBe "PENSIONAINCOME245"
      }

    }

    "redirect to pension summary page if there is no session data" should {
      lazy val form: Map[String, String] = Map(PensionCustomerReferenceNumberForm.pensionsCustomerReferenceNumberId -> "123456")

      lazy val result: WSResponse = submitPageNoSessionData(form)

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER

        result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
      }
    }
  }


}
