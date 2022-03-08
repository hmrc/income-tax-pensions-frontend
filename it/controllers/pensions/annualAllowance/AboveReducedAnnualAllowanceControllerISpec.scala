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

package controllers.pensions.annualAllowance

import builders.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import builders.PensionAnnualAllowanceViewModelBuilder.aPensionAnnualAllowanceViewModel
import builders.PensionsCYAModelBuilder._
import builders.PensionsUserDataBuilder
import builders.PensionsUserDataBuilder.aPensionsUserData
import builders.UserBuilder._
import forms.YesNoForm
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.pension.charges.PensionAnnualAllowancesViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.PensionAnnualAllowancePages.{aboveReducedAnnualAllowanceAmountUrl, aboveReducedAnnualAllowanceUrl}
import utils.PageUrls._
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

class AboveReducedAnnualAllowanceControllerISpec extends IntegrationTest with ViewHelpers with BeforeAndAfterEach with PensionsDatabaseHelper {

  private val taxYearEOY: Int = taxYear - 1

  private def pensionsUsersData(isPrior: Boolean = false, pensionsCyaModel: PensionsCYAModel) = {
    PensionsUserDataBuilder.aPensionsUserData.copy(
      isPriorSubmission = isPrior, pensions = pensionsCyaModel)
  }

  private def pensionsUserDataWithAnnualAllowances(annualAllowances: PensionAnnualAllowancesViewModel,
                                           isPriorSubmission: Boolean = true): PensionsUserData = {
    aPensionsUserData.copy(isPriorSubmission = isPriorSubmission,
      pensions = aPensionsCYAModel.copy(pensionsAnnualAllowances = annualAllowances)
    )
  }

  object Selectors {
    val captionSelector: String = "#main-content > div > div > form > div > fieldset > legend > header > p"
    val continueButtonSelector: String = "#continue"
    val formSelector: String = "#main-content > div > div > form"
    val yesSelector = "#value"
    val noSelector = "#value-no"
    val detailsSelector: String = s"#main-content > div > div > form > div > fieldset > legend > p"

  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val yesText: String
    val noText: String
    val buttonText: String
    val expectedDetails: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedErrorTitle: String
    val expectedErrorMessage: String

  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Pension annual allowance for 6 April ${taxYear - 1} to 5 April $taxYear"
    val yesText = "Yes"
    val noText = "No"
    val buttonText = "Continue"
    val expectedDetails: String = "Use a calculator if you need to work this out (opens in new tab)."
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Pension annual allowance for 6 April ${taxYear - 1} to 5 April $taxYear"
    val yesText = "Yes"
    val noText = "No"
    val buttonText = "Continue"
    val expectedDetails: String = "Use a calculator if you need to work this out (opens in new tab)."
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Have you gone above your reduced annual allowance?"
    val expectedHeading = "Have you gone above your reduced annual allowance?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorMessage = "Select yes if you have gone above your reduced annual allowance"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Have you gone above your reduced annual allowance?"
    val expectedHeading = "Have you gone above your reduced annual allowance?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorMessage = "Select yes if you have gone above your reduced annual allowance"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Has your client gone above their reduced annual allowance?"
    val expectedHeading = "Has your client gone above their reduced annual allowance?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorMessage = "Select yes if your client has gone above their reduced annual allowance"

  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Has your client gone above their reduced annual allowance?"
    val expectedHeading = "Has your client gone above their reduced annual allowance?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorMessage = "Select yes if your client has gone above their reduced annual allowance"
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
        "render the above reduced annual allowance question page with no pre-filled radio buttons" which {
          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(reducedAnnualAllowanceQuestion = None)
            insertCyaData(pensionsUserDataWithAnnualAllowances(pensionsViewModel), aUserRequest)
            urlGet(fullUrl(aboveReducedAnnualAllowanceUrl(taxYearEOY)),
              user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          radioButtonCheck(yesText, 1, checked = Some(false))
          radioButtonCheck(noText, 2, checked = Some(false))
          buttonCheck(buttonText, continueButtonSelector)
          textOnPageCheck(expectedDetails, detailsSelector)
          formPostLinkCheck(aboveReducedAnnualAllowanceUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render above reduced annual allowance question page with 'Yes' pre-filled when CYA data exists" which {
          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(reducedAnnualAllowanceQuestion = Some(true))
            insertCyaData(pensionsUserDataWithAnnualAllowances(pensionsViewModel), aUserRequest)
            urlGet(fullUrl(aboveReducedAnnualAllowanceUrl(taxYearEOY)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          radioButtonCheck(yesText, 1, checked = Some(true))
          radioButtonCheck(noText, 2, checked = Some(false))
          buttonCheck(buttonText, continueButtonSelector)
          textOnPageCheck(expectedDetails, detailsSelector)
          formPostLinkCheck(aboveReducedAnnualAllowanceUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the above reduced annual allowance question page with 'No' pre-filled and not a prior submission" which {
          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(reducedAnnualAllowanceQuestion = Some(false))
            insertCyaData(pensionsUserDataWithAnnualAllowances(pensionsViewModel), aUserRequest)
            urlGet(fullUrl(aboveReducedAnnualAllowanceUrl(taxYearEOY)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          radioButtonCheck(yesText, 1, checked = Some(false))
          radioButtonCheck(noText, 2, checked = Some(true))
          buttonCheck(buttonText, continueButtonSelector)
          textOnPageCheck(expectedDetails, detailsSelector)
          formPostLinkCheck(aboveReducedAnnualAllowanceUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    //TODO redirect to annual CYA Page
    "redirect to Pension Summary page if there is no session data" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(aboveReducedAnnualAllowanceUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(pensionSummaryUrl(taxYearEOY)) shouldBe true
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
            val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(aboveAnnualAllowanceQuestion = None)
            insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(pensionsAnnualAllowances = pensionsViewModel)), aUserRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(fullUrl(aboveReducedAnnualAllowanceUrl(taxYearEOY)), body = form, welsh = user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          radioButtonCheck(yesText, 1, checked = Some(false))
          radioButtonCheck(noText, 2, checked = Some(false))
          buttonCheck(buttonText, continueButtonSelector)
          textOnPageCheck(expectedDetails, detailsSelector)
          formPostLinkCheck(aboveReducedAnnualAllowanceUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
          errorSummaryCheck(user.specificExpectedResults.get.expectedErrorMessage, Selectors.yesSelector)
          errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorMessage, Some("value"))
        }
      }
    }

    "redirect to Above your annual allowance payment page when user selects 'yes' and not a prior submission" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(
          aboveAnnualAllowanceQuestion = Some(true), aboveAnnualAllowance = None)
        insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(pensionsAnnualAllowances = pensionsViewModel)), aUserRequest)

        urlPost(fullUrl(aboveReducedAnnualAllowanceUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(aboveReducedAnnualAllowanceAmountUrl(taxYearEOY))

      }

      "updates above reduced annual allowance question to Some(true)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.pensionsAnnualAllowances.aboveAnnualAllowanceQuestion shouldBe Some(true)
        cyaModel.pensions.pensionsAnnualAllowances.aboveAnnualAllowance shouldBe None
      }
    }

    "redirect to Pensions Summary page when user selects 'no' and not a prior submission" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(
          aboveAnnualAllowanceQuestion = Some(true), aboveAnnualAllowance = Some(333.44))
        insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(pensionsAnnualAllowances = pensionsViewModel)), aUserRequest)
        urlPost(fullUrl(aboveReducedAnnualAllowanceUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
      }

      "updates above reduced annual allowance question to Some(false) and deletes the annual allowance amount" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.pensionsAnnualAllowances.aboveAnnualAllowanceQuestion shouldBe Some(false)
        cyaModel.pensions.pensionsAnnualAllowances.aboveAnnualAllowance shouldBe None
      }
    }

  }

}
