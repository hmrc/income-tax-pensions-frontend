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

package controllers.pensions

import builders.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import builders.PaymentsIntoPensionVewModelBuilder._
import builders.PensionsCYAModelBuilder._
import builders.PensionsUserDataBuilder
import builders.UserBuilder._
import forms.YesNoForm
import models.mongo.PensionsCYAModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.{fullUrl, retirementAnnuityUrl}
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

class RetirementAnnuityControllerISpec extends IntegrationTest with ViewHelpers with BeforeAndAfterEach with PensionsDatabaseHelper {

  private val taxYearEOY: Int = taxYear - 1

  private def pensionsUsersData(isPrior: Boolean = false, pensionsCyaModel: PensionsCYAModel) = {
    PensionsUserDataBuilder.aPensionsUserData.copy(
      isPriorSubmission = isPrior,
      pensions = pensionsCyaModel
    )
  }

  object Selectors {
    val captionSelector: String = "#main-content > div > div > form > div > fieldset > legend > header > p"
    val continueButtonSelector: String = "#continue"
    val formSelector: String = "#main-content > div > div > form"
    val yesSelector = "#value"
    val noSelector = "#value-no"
    val detailsSelector: String = s"#main-content > div > div > form > details > summary > span"

    def h3Selector(index: Int): String = s"#main-content > div > div > form > details > div > h3:nth-child($index)"

    def paragraphSelector(index: Int): String = s"#main-content > div > div > form > div > fieldset > legend > p:nth-child($index)"

    def bulletListSelector(index: Int): String = s"#main-content > div > div > form > div > fieldset > legend > ul > li:nth-child($index)"

    def detailsParagraphSelector(index: Int): String = s"#main-content > div > div > form > details > div > p:nth-child($index)"

    def detailsBulletList(index: Int): String = s"#main-content > div > div > form > details > div > ol > li:nth-child($index)"
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val yesText: String
    val noText: String
    val buttonText: String
    val expectedDetailsTitle: String
    val expectedDetails: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedParagraphText: String
    val expectedErrorTitle: String
    val expectedErrorMessage: String
    val expectedYouCanFindThisOut: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Payments into pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val yesText = "Yes"
    val noText = "No"
    val buttonText = "Continue"
    val expectedDetailsTitle = "What is a retirement annuity contract?"
    val expectedDetails: String =
      "Retirement annuity contracts are a type of pension scheme. " +
        "They were available before 1988 to the self-employed and to workers not offered a workplace pension."
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Payments into pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val yesText = "Yes"
    val noText = "No"
    val buttonText = "Continue"
    val expectedDetailsTitle = "What is a retirement annuity contract?"
    val expectedDetails: String =
      "Retirement annuity contracts are a type of pension scheme. " +
        "They were available before 1988 to the self-employed and to workers not offered a workplace pension."
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Did you pay into a retirement annuity contract?"
    val expectedHeading = "Did you pay into a retirement annuity contract?"
    val expectedParagraphText = "We only need to know about payments if your pension provider will not claim tax relief (opens in new tab)."
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorMessage = "Select yes if you paid into a retirement annuity contract"
    val expectedYouCanFindThisOut = "You can find this out from your pension provider."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Did you pay into a retirement annuity contract?"
    val expectedHeading = "Did you pay into a retirement annuity contract?"
    val expectedParagraphText = "We only need to know about payments if your pension provider will not claim tax relief (opens in new tab)."
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorMessage = "Select yes if you paid into a retirement annuity contract"
    val expectedYouCanFindThisOut = "You can find this out from your pension provider."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Did your client pay into a retirement annuity contract?"
    val expectedHeading = "Did your client pay into a retirement annuity contract?"
    val expectedParagraphText = "We only need to know about payments if your client’s pension provider will not claim tax relief (opens in new tab)."
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorMessage = "Select yes if your client paid into a retirement annuity contract"
    val expectedYouCanFindThisOut = "You can find this out from your client’s pension provider."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Did your client pay into a retirement annuity contract?"
    val expectedHeading = "Did your client pay into a retirement annuity contract?"
    val expectedParagraphText = "We only need to know about payments if your client’s pension provider will not claim tax relief (opens in new tab)."
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorMessage = "Select yes if your client paid into a retirement annuity contract"
    val expectedYouCanFindThisOut = "You can find this out from your client’s pension provider."
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  ".show" should {
    userScenarios.foreach { user =>

      import Selectors._
      import user.commonExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        "render the retirement annuity contract question page with no pre-filled radio buttons" which {
          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(retirementAnnuityContractPaymentsQuestion = None)
            insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)), aUserRequest)
            urlGet(fullUrl(retirementAnnuityUrl(taxYearEOY)), user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraphText, paragraphSelector(2))
          textOnPageCheck(user.specificExpectedResults.get.expectedYouCanFindThisOut, paragraphSelector(3))
          radioButtonCheck(yesText, 1, checked = Some(false))
          radioButtonCheck(noText, 2, checked = Some(false))
          buttonCheck(buttonText, continueButtonSelector)

          textOnPageCheck(expectedDetailsTitle, detailsSelector)
          textOnPageCheck(expectedDetails, detailsParagraphSelector(1))
          formPostLinkCheck(retirementAnnuityUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the retirement annuity contract question page with 'Yes' pre-filled when CYA data exists" which {
          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel), aUserRequest)
            urlGet(fullUrl(retirementAnnuityUrl(taxYearEOY)), user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraphText, paragraphSelector(2))
          textOnPageCheck(user.specificExpectedResults.get.expectedYouCanFindThisOut, paragraphSelector(3))
          radioButtonCheck(yesText, 1, checked = Some(true))
          radioButtonCheck(noText, 2, checked = Some(false))
          buttonCheck(buttonText, continueButtonSelector)

          textOnPageCheck(expectedDetailsTitle, detailsSelector)
          textOnPageCheck(expectedDetails, detailsParagraphSelector(1))
          formPostLinkCheck(retirementAnnuityUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the retirement annuity contract question page with 'No' pre-filled and not a prior submission" which {
          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            val paymentsIntoPensionsViewModel = aPaymentsIntoPensionViewModel.copy(retirementAnnuityContractPaymentsQuestion = Some(false))
            insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(paymentsIntoPensionsViewModel)), aUserRequest)
            urlGet(fullUrl(retirementAnnuityUrl(taxYearEOY)), user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraphText, paragraphSelector(2))
          textOnPageCheck(user.specificExpectedResults.get.expectedYouCanFindThisOut, paragraphSelector(3))
          radioButtonCheck(yesText, 1, checked = Some(false))
          radioButtonCheck(noText, 2, checked = Some(true))
          buttonCheck(buttonText, continueButtonSelector)

          textOnPageCheck(expectedDetailsTitle, detailsSelector)
          textOnPageCheck(expectedDetails, detailsParagraphSelector(1))
          formPostLinkCheck(retirementAnnuityUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }
  }

  ".submit" should {
    userScenarios.foreach { user =>

      import Selectors._
      import user.commonExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "return an error when form is submitted with no entry" which {
          lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> "")

          lazy val result: WSResponse = {
            dropPensionsDB()
            insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(
              aPaymentsIntoPensionViewModel.copy(retirementAnnuityContractPaymentsQuestion = None))), aUserRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(fullUrl(retirementAnnuityUrl(taxYearEOY)), body = form, welsh = user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraphText, paragraphSelector(2))
          textOnPageCheck(user.specificExpectedResults.get.expectedYouCanFindThisOut, paragraphSelector(3))
          radioButtonCheck(yesText, 1, checked = Some(false))
          radioButtonCheck(noText, 2, checked = Some(false))
          buttonCheck(buttonText, continueButtonSelector)

          textOnPageCheck(expectedDetailsTitle, detailsSelector)
          textOnPageCheck(expectedDetails, detailsParagraphSelector(1))
          formPostLinkCheck(retirementAnnuityUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
          errorSummaryCheck(user.specificExpectedResults.get.expectedErrorMessage, Selectors.yesSelector)
          errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorMessage, Some("value"))
        }
      }
    }

    "redirect to Pensions Summary page when user selects 'yes' and not a prior submission" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val paymentsIntoPensionsViewModel = aPaymentsIntoPensionViewModel.copy(
          retirementAnnuityContractPaymentsQuestion = Some(false), totalRetirementAnnuityContractPayments = None)
        insertCyaData(pensionsUsersData(isPrior = false, PensionsCYAModel(paymentsIntoPensionsViewModel)), aUserRequest)
        urlPost(fullUrl(retirementAnnuityUrl(taxYearEOY)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(controllers.pensions.routes.RetirementAnnuityAmountController.show(taxYearEOY).url)
      }

      "updates retirement annuity contract payments question to Some(true)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.paymentsIntoPension.retirementAnnuityContractPaymentsQuestion shouldBe Some(true)
        cyaModel.pensions.paymentsIntoPension.totalRetirementAnnuityContractPayments shouldBe None
      }
    }

    "redirect to Pensions Summary page when user selects 'no' and not a prior submission" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val paymentsIntoPensionsViewModel = aPaymentsIntoPensionViewModel.copy(
          retirementAnnuityContractPaymentsQuestion = Some(true), totalRetirementAnnuityContractPayments = Some(333.44))
        insertCyaData(pensionsUsersData(isPrior = false, PensionsCYAModel(paymentsIntoPensionsViewModel)), aUserRequest)
        urlPost(fullUrl(retirementAnnuityUrl(taxYearEOY)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(controllers.pensions.routes.PensionsSummaryController.show(taxYearEOY).url)
      }

      "updates retirement annuity contract payments question to Some(false) and deletes the totalRetirementAnnuityContractPayments amount" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.paymentsIntoPension.retirementAnnuityContractPaymentsQuestion shouldBe Some(false)
        cyaModel.pensions.paymentsIntoPension.totalRetirementAnnuityContractPayments shouldBe None
      }
    }

  }
}
