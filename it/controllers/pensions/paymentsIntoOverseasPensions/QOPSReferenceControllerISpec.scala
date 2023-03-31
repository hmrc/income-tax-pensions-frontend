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
import builders.PensionsCYAModelBuilder.aPensionsCYAModel
import builders.PensionsUserDataBuilder.{aPensionsUserData, pensionUserDataWithOverseasPensions}
import builders.UserBuilder.aUserRequest
import forms.QOPSReferenceNumberForm
import models.pension.charges.Relief
import models.pension.charges.TaxReliefQuestion.{MigrantMemberRelief, TransitionalCorrespondingRelief}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.{CommonUtils, PensionsDatabaseHelper}
import utils.PageUrls.{PaymentIntoOverseasPensions, fullUrl, pensionSummaryUrl}

class QOPSReferenceControllerISpec extends CommonUtils with BeforeAndAfterEach with PensionsDatabaseHelper{
  object Selectors {
    val captionSelector: String = "#main-content > div > div > header > p"
    val continueButtonSelector: String = "#continue"
    val formSelector: String = "#main-content > div > div > form"
    val inputSelector = "#qopsReferenceId"
    val hintTextSelector = "#qopsReferenceId-hint"

    def labelSelector(index: Int): String = s"form > div:nth-of-type($index) > label"

    def paragraphSelector(index: Int): String = s"#main-content > div > div > p:nth-of-type($index)"
  }

  trait SpecificExpectedResults {
    val expectedParagraph1: String
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedTitle: String
    lazy val expectedHeading = expectedTitle
    val expectedErrorTitle: String
    val hintText: String
    val expectedButtonText: String
    val expectedIncorrectFormatError: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
     val expectedParagraph1: String = "You can find this on your pension statement."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedParagraph1: String = "You can find this on your pension statement."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedParagraph1: String = "You can find this on your client’s pension statement."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedParagraph1: String = "You can find this on your client’s pension statement."
  }

  object CommonExpectedEN extends CommonExpectedResults {
     val expectedCaption: Int => String = (taxYear:Int) => s"Payments into overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
     val expectedTitle: String = "Qualifying overseas pension scheme (QOPS) reference number (optional)"
     val expectedErrorTitle: String = s"Error: $expectedTitle"
     val hintText: String = "For example, QOPS123456"
     val expectedButtonText: String = "Continue"
     val expectedIncorrectFormatError: String = "Enter a six digit number"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Taliadau i bensiynau tramor ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val expectedTitle: String = "Qualifying overseas pension scheme (QOPS) reference number (optional)"
    val expectedErrorTitle: String = s"Gwall: $expectedTitle"
    val hintText: String = "For example, QOPS123456"
    val expectedButtonText: String = "Yn eich blaen"
    val expectedIncorrectFormatError: String = "Enter a six digit number"
  }

  val inputName: String = "qopsReferenceId"
//  implicit val qopsReferenceUrl: Int => String = (taxYear: Int) => PaymentIntoOverseasPensions.qopsReferenceUrl(taxYear)
implicit val qopsReferenceUrl = PaymentIntoOverseasPensions.qopsReferenceUrl(0)

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

        "render the 'QOPS' page with correct content and no pre-filling" which {
          val relief: Relief = Relief(
            reliefType = Some(TransitionalCorrespondingRelief),
            customerReferenceNumberQuestion = Some("PENSIONINCOME245"),
            employerPaymentsAmount = Some(1999.99),
            qualifyingOverseasPensionSchemeReferenceNumber = None)

          implicit lazy val result: WSResponse = showPage(user,
            aPensionsUserData.copy(pensions = aPensionsCYAModel.copy(
              paymentsIntoOverseasPensions = aPaymentsIntoOverseasPensionsViewModel.copy(
                reliefs = Seq(relief)))))

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(expectedTitle, user.isWelsh)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph1, paragraphSelector(1))
          textOnPageCheck(hintText, hintTextSelector)
          inputFieldValueCheck(inputName, inputSelector, "")
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(qopsReferenceUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the 'QOPS' page with correct content with pre-filling" which {
          val qopsRef = "123456"

          val relief = Relief(
            reliefType = Some(TransitionalCorrespondingRelief),
            customerReferenceNumberQuestion = Some("PENSIONINCOME245"),
            employerPaymentsAmount = Some(1999.99),
            qualifyingOverseasPensionSchemeReferenceNumber = Some(qopsRef),
            )

          val pensionsViewModel = aPaymentsIntoOverseasPensionsViewModel.copy(
            reliefs = Seq(relief))

          val pensionUserData = pensionUserDataWithOverseasPensions(pensionsViewModel)
          implicit lazy val result: WSResponse = showPage(user, pensionUserData)

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(expectedTitle, user.isWelsh)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph1, paragraphSelector(1))
          textOnPageCheck(hintText, hintTextSelector)
          inputFieldValueCheck(inputName, inputSelector, qopsRef)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(qopsReferenceUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the 'QOPS' page with correct content with pre-filling that contains reference with prefix" which {
          val qopsRef = "QOPS123456"
          val expectedQopsRef = "123456"

          val relief = Relief(
            reliefType = Some(TransitionalCorrespondingRelief),
            customerReferenceNumberQuestion = Some("PENSIONINCOME245"),
            employerPaymentsAmount = Some(1999.99),
            qualifyingOverseasPensionSchemeReferenceNumber = Some(qopsRef),
          )

          val pensionsViewModel = aPaymentsIntoOverseasPensionsViewModel.copy(
            reliefs = Seq(relief))
          val pensionUserData = pensionUserDataWithOverseasPensions(pensionsViewModel)
          implicit lazy val result: WSResponse = showPage(user, pensionUserData)

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(expectedTitle, user.isWelsh)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph1, paragraphSelector(1))
          textOnPageCheck(hintText, hintTextSelector)
          inputFieldValueCheck(inputName, inputSelector, expectedQopsRef)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(qopsReferenceUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the 'QOPS' page with correct content with pre-filling that contains reference with a different (incorrect) prefix" which {
          val qopsRef = "ABCD123456"
          val expectedQopsRef = "123456"

          val relief = Relief(
            reliefType = Some(TransitionalCorrespondingRelief),
            customerReferenceNumberQuestion = Some("PENSIONINCOME245"),
            employerPaymentsAmount = Some(1999.99),
            qualifyingOverseasPensionSchemeReferenceNumber = Some(qopsRef),
          )

          val pensionsViewModel = aPaymentsIntoOverseasPensionsViewModel.copy(
            reliefs = Seq(relief))
          val pensionUserData = pensionUserDataWithOverseasPensions(pensionsViewModel)
          implicit lazy val result: WSResponse = showPage(user, pensionUserData)

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(expectedTitle, user.isWelsh)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph1, paragraphSelector(1))
          textOnPageCheck(hintText, hintTextSelector)
          inputFieldValueCheck(inputName, inputSelector, expectedQopsRef)
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(qopsReferenceUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }


        "Redirect to the pension summary page if an out of bounds index is provided " should {
          val qopsRef = "123456"

          val relief = Relief(
            reliefType = Some(TransitionalCorrespondingRelief),
            customerReferenceNumberQuestion = Some("PENSIONINCOME245"),
            employerPaymentsAmount = Some(1999.99),
            qualifyingOverseasPensionSchemeReferenceNumber = Some(qopsRef),
          )
          implicit val qopsReferenceUrl = PaymentIntoOverseasPensions.qopsReferenceUrl(2)

          val pensionsViewModel = aPaymentsIntoOverseasPensionsViewModel.copy(
            reliefs = Seq(relief))

          val pensionUserData = pensionUserDataWithOverseasPensions(pensionsViewModel)
          implicit lazy val result: WSResponse = showPage(user, pensionUserData)

          "has an SEE_OTHER status" in {
            result.status shouldBe SEE_OTHER
            result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
          }
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

        s"return $BAD_REQUEST error when incorrect format is submitted" which {
          lazy val form: Map[String, String] = Map(QOPSReferenceNumberForm.qopsReferenceId -> "1234567")
          lazy val result: WSResponse = submitPage(user, aPensionsUserData, form)


          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)
          import Selectors._
          import user.commonExpectedResults._
          titleCheck(expectedErrorTitle, user.isWelsh)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph1, paragraphSelector(1))
          textOnPageCheck(hintText, hintTextSelector)
          inputFieldValueCheck(inputName, inputSelector, "1234567")
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(qopsReferenceUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
          errorSummaryCheck(user.commonExpectedResults.expectedIncorrectFormatError, inputSelector)
          errorAboveElementCheck(user.commonExpectedResults.expectedIncorrectFormatError)
        }
      }
    }

    "redirect and update question to contain QOPS reference when no prior data exists" which {
      lazy val form: Map[String, String] = Map(QOPSReferenceNumberForm.qopsReferenceId -> "123456")

      val relief = Relief(
        reliefType = Some(MigrantMemberRelief),
        customerReferenceNumberQuestion = Some("PENSIONINCOME245"),
        employerPaymentsAmount = Some(1999.99),
        qualifyingOverseasPensionSchemeReferenceNumber = None,
      )

      val pensionsViewModel = aPaymentsIntoOverseasPensionsViewModel.copy(
        reliefs = Seq(relief))

      val pensionUserData = pensionUserDataWithOverseasPensions(pensionsViewModel)

      lazy val result: WSResponse = submitPage(pensionUserData, form)

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(qopsReferenceUrl(taxYearEOY))
      }

      "updates pension scheme QOPS reference to contain tax reference" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.paymentsIntoOverseasPensions.reliefs.head.qualifyingOverseasPensionSchemeReferenceNumber shouldBe Some("123456")
      }
    }

    "redirect when user passes an out of bounds index" which {
      lazy val form: Map[String, String] = Map(QOPSReferenceNumberForm.qopsReferenceId -> "123456")

      val relief = Relief(
        reliefType = Some(MigrantMemberRelief),
        customerReferenceNumberQuestion = Some("PENSIONINCOME245"),
        employerPaymentsAmount = Some(1999.99),
        qualifyingOverseasPensionSchemeReferenceNumber = Some("111111"),
      )
      implicit val qopsReferenceUrl = PaymentIntoOverseasPensions.qopsReferenceUrl(2)

      val pensionsViewModel = aPaymentsIntoOverseasPensionsViewModel.copy(
        reliefs = Seq(relief))

      val pensionUserData = pensionUserDataWithOverseasPensions(pensionsViewModel)

      lazy val result: WSResponse = submitPage(pensionUserData, form)

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
      }
    }

    "redirect and update QOPS when cya data exists" which {
      lazy val form: Map[String, String] = Map(QOPSReferenceNumberForm.qopsReferenceId -> "654321")
      val qopsRef = "123456"

      val relief = Relief(
        reliefType = Some(MigrantMemberRelief),
        customerReferenceNumberQuestion = Some("PENSIONINCOME245"),
        employerPaymentsAmount = Some(1999.99),
        qualifyingOverseasPensionSchemeReferenceNumber = Some(qopsRef),
      )

      val pensionsViewModel = aPaymentsIntoOverseasPensionsViewModel.copy(reliefs = Seq(relief))

      val pensionUserData = pensionUserDataWithOverseasPensions(pensionsViewModel)

      lazy val result: WSResponse = submitPage(pensionUserData, form)


      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(qopsReferenceUrl(taxYearEOY)) //todo redirect to correct page
      }

      "updates pension scheme QOPS reference to contain tax reference" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.paymentsIntoOverseasPensions.reliefs.head.qualifyingOverseasPensionSchemeReferenceNumber.get shouldBe "654321"
      }

    }
    "redirect to pension summary page if there is no session data" should {
      lazy val form: Map[String, String] = Map(QOPSReferenceNumberForm.qopsReferenceId -> "123456")

      lazy val result: WSResponse = submitPageNoSessionData(form)

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER

        result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
      }
    }
  }
}
