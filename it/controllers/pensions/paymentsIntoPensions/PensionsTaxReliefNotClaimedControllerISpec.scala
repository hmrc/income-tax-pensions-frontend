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

package controllers.pensions.paymentsIntoPensions

import builders.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import builders.PaymentsIntoPensionVewModelBuilder.aPaymentsIntoPensionViewModel
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
import utils.PageUrls.PaymentIntoPensions.{checkPaymentsIntoPensionCyaUrl, pensionTaxReliefNotClaimedUrl, retirementAnnuityUrl}
import utils.PageUrls._
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

class PensionsTaxReliefNotClaimedControllerISpec extends IntegrationTest with ViewHelpers with BeforeAndAfterEach with PensionsDatabaseHelper {

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
    val h2Selector: String = s"#main-content > div > div > form > div > fieldset > legend > h2"

    def paragraphSelector(index: Int): String = s"#main-content > div > div > form > div > fieldset > legend > p:nth-child($index)"

  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedHeading: String
    val expectedTitle: String
    val expectedErrorTitle: String
    val yesText: String
    val noText: String
    val buttonText: String
  }

  trait SpecificExpectedResults {
    val expectedQuestionsInfoText: String
    val expectedWhereToCheck: String
    val expectedSubHeading: String
    val expectedErrorMessage: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Payments into pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedHeading = "Pensions where tax relief is not claimed"
    val expectedTitle = "Pensions where tax relief is not claimed"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val yesText = "Yes"
    val noText = "No"
    val buttonText = "Continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Payments into pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedHeading = "Pensions where tax relief is not claimed"
    val expectedTitle = "Pensions where tax relief is not claimed"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val yesText = "Yes"
    val noText = "No"
    val buttonText = "Continue"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedQuestionsInfoText = "These questions are about pensions you pay into where tax relief is not claimed for you."
    val expectedWhereToCheck = "You can check your pension statements or contact your pension provider to find the information you need."
    val expectedSubHeading = "Did you pay into a pension where tax relief was not claimed for you?"
    val expectedErrorMessage = "Select yes if you paid into a pension where tax relief was not claimed for you"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedQuestionsInfoText = "These questions are about pensions you pay into where tax relief is not claimed for you."
    val expectedWhereToCheck = "You can check your pension statements or contact your pension provider to find the information you need."
    val expectedSubHeading = "Did you pay into a pension where tax relief was not claimed for you?"
    val expectedErrorMessage = "Select yes if you paid into a pension where tax relief was not claimed for you"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedQuestionsInfoText = "These questions are about pensions your client pays into where tax relief is not claimed for them."
    val expectedWhereToCheck = "You can check your client’s pension statements or contact your client’s pension provider to find the information you need."
    val expectedSubHeading = "Did your client pay into a pension where tax relief was not claimed for them?"
    val expectedErrorMessage = "Select yes if your client paid into a pension where tax relief was not claimed for them"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedQuestionsInfoText = "These questions are about pensions your client pays into where tax relief is not claimed for them."
    val expectedWhereToCheck = "You can check your client’s pension statements or contact your client’s pension provider to find the information you need."
    val expectedSubHeading = "Did your client pay into a pension where tax relief was not claimed for them?"
    val expectedErrorMessage = "Select yes if your client paid into a pension where tax relief was not claimed for them"
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
        "render the pensions where tax relief is not claimed question page with no pre-filled radio buttons if no CYA question data" which {
          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(pensionTaxReliefNotClaimedQuestion = None)
            insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)), aUserRequest)
            urlGet(fullUrl(pensionTaxReliefNotClaimedUrl(taxYearEOY)),
              user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(expectedTitle)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedQuestionsInfoText, paragraphSelector(2))
          textOnPageCheck(user.specificExpectedResults.get.expectedWhereToCheck, paragraphSelector(3))
          textOnPageCheck(user.specificExpectedResults.get.expectedSubHeading, h2Selector)
          radioButtonCheck(yesText, 1, checked = Some(false))
          radioButtonCheck(noText, 2, checked = Some(false))
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(pensionTaxReliefNotClaimedUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the pensions where tax relief is not claimed question page with 'Yes' pre-filled when CYA data exists" which {
          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel), aUserRequest)
            urlGet(fullUrl(pensionTaxReliefNotClaimedUrl(taxYearEOY)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(expectedTitle)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedQuestionsInfoText, paragraphSelector(2))
          textOnPageCheck(user.specificExpectedResults.get.expectedWhereToCheck, paragraphSelector(3))
          textOnPageCheck(user.specificExpectedResults.get.expectedSubHeading, h2Selector)
          radioButtonCheck(yesText, 1, checked = Some(true))
          radioButtonCheck(noText, 2, checked = Some(false))
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(pensionTaxReliefNotClaimedUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)

        }

        "render the pensions where tax relief is not claimed question page with 'No' pre-filled when CYA data exists" which {
          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            val paymentsIntoPensionsViewModel = aPaymentsIntoPensionViewModel.copy(pensionTaxReliefNotClaimedQuestion = Some(false))
            insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(paymentsIntoPensionsViewModel)), aUserRequest)
            urlGet(fullUrl(pensionTaxReliefNotClaimedUrl(taxYearEOY)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(expectedTitle)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedQuestionsInfoText, paragraphSelector(2))
          textOnPageCheck(user.specificExpectedResults.get.expectedWhereToCheck, paragraphSelector(3))
          textOnPageCheck(user.specificExpectedResults.get.expectedSubHeading, h2Selector)
          radioButtonCheck(yesText, 1, checked = Some(false))
          radioButtonCheck(noText, 2, checked = Some(true))
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(pensionTaxReliefNotClaimedUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)

        }

      }
    }

    "redirect to the CYA page if there is no session data" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        // no cya insert
        urlGet(fullUrl(pensionTaxReliefNotClaimedUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkPaymentsIntoPensionCyaUrl(taxYearEOY)) shouldBe true
      }

    }

  }

  ".submit" should {

    val validFormYes: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
    val validFormNo: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
    val invalidForm: Map[String, String] = Map(YesNoForm.yesNo -> "")

    userScenarios.foreach { user =>

      import Selectors._
      import user.commonExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "return an error when form is submitted with no entry" which {

          lazy val result: WSResponse = {
            dropPensionsDB()
            insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(
              aPaymentsIntoPensionViewModel.copy(pensionTaxReliefNotClaimedQuestion = None))), aUserRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(fullUrl(pensionTaxReliefNotClaimedUrl(taxYearEOY)), body = invalidForm, welsh = user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(expectedErrorTitle)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedQuestionsInfoText, paragraphSelector(2))
          textOnPageCheck(user.specificExpectedResults.get.expectedWhereToCheck, paragraphSelector(3))
          textOnPageCheck(user.specificExpectedResults.get.expectedSubHeading, h2Selector)
          radioButtonCheck(yesText, 1, checked = Some(false))
          radioButtonCheck(noText, 2, checked = Some(false))
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(pensionTaxReliefNotClaimedUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
          errorSummaryCheck(user.specificExpectedResults.get.expectedErrorMessage, Selectors.yesSelector)
          errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorMessage, Some("value"))

        }
      }

    }

    "redirect to Pensions Summary page when user submits a 'yes' answer and updates the session value to yes" which {

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val paymentsIntoPensionsViewModel = aPaymentsIntoPensionViewModel.copy(pensionTaxReliefNotClaimedQuestion = None)
        insertCyaData(pensionsUsersData(isPrior = false, paymentsIntoPensionOnlyCYAModel(paymentsIntoPensionsViewModel)), aUserRequest)
        urlPost(fullUrl(pensionTaxReliefNotClaimedUrl(taxYearEOY)), body = validFormYes, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))

      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(retirementAnnuityUrl(taxYearEOY))
      }

      "updates retirement annuity contract payments question to Some(true)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.paymentsIntoPension.pensionTaxReliefNotClaimedQuestion shouldBe Some(true)
      }
    }

    "redirect to Pensions Summary page when user submits a 'no' answer and updates the session value to no" which {

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val paymentsIntoPensionsViewModel = aPaymentsIntoPensionViewModel.copy(pensionTaxReliefNotClaimedQuestion = None)
        insertCyaData(pensionsUsersData(isPrior = false, paymentsIntoPensionOnlyCYAModel(paymentsIntoPensionsViewModel)), aUserRequest)
        urlPost(fullUrl(pensionTaxReliefNotClaimedUrl(taxYearEOY)), body = validFormNo, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))

      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(checkPaymentsIntoPensionCyaUrl(taxYearEOY))
      }

      "updates retirement annuity contract payments question to Some(false) and clears retirements and workplace answers" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.paymentsIntoPension.pensionTaxReliefNotClaimedQuestion shouldBe Some(false)
        cyaModel.pensions.paymentsIntoPension.retirementAnnuityContractPaymentsQuestion shouldBe None
        cyaModel.pensions.paymentsIntoPension.totalRetirementAnnuityContractPayments shouldBe None
        cyaModel.pensions.paymentsIntoPension.workplacePensionPaymentsQuestion shouldBe None
        cyaModel.pensions.paymentsIntoPension.totalWorkplacePensionPayments shouldBe None
      }
    }

  }
}
