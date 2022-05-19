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

package controllers.pensions.incomeFromPensions

import builders.IncomeFromPensionsViewModelBuilder.anIncomeFromPensionsViewModel
import builders.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import builders.PensionsCYAModelBuilder.aPensionsCYAModel
import builders.PensionsUserDataBuilder
import builders.UserBuilder.aUserRequest
import forms.YesNoForm
import models.mongo.PensionsCYAModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.IncomeFromPensionsPages.{pensionSchemeDetailsUrl, ukPensionSchemePayments}
import utils.PageUrls.{fullUrl, overviewUrl, pensionSummaryUrl}
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

class UkPensionSchemePaymentsControllerISpec extends IntegrationTest with ViewHelpers with BeforeAndAfterEach with PensionsDatabaseHelper {

  private def pensionsUsersData(isPrior: Boolean = false, pensionsCyaModel: PensionsCYAModel) = {
    PensionsUserDataBuilder.aPensionsUserData.copy(
      isPriorSubmission = isPrior,
      pensions = pensionsCyaModel
    )
  }

  object Selectors {
    val captionSelector: String = "#main-content > div > div > header > p"
    val continueButtonSelector: String = "#continue"
    val formSelector: String = "#main-content > div > div > form"
    val yesSelector = "#value"
    val noSelector = "#value-no"
    val expectedDoesNotIncludeSelector: String = s"#main-content > div > div > p"
  }


  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val yesText: String
    val noText: String
    val buttonText: String
    val expectedDoesNotIncludeText: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedErrorTitle: String
    val expectedErrorMessage: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Dividends for 6 April ${taxYear - 1} to 5 April $taxYear"
    val yesText = "Yes"
    val noText = "No"
    val buttonText = "Continue"
    val expectedDoesNotIncludeText = "This does not include State Pension."

  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Dividends for 6 April ${taxYear - 1} to 5 April $taxYear"
    val yesText = "Yes"
    val noText = "No"
    val buttonText = "Continue"
    val expectedDoesNotIncludeText = "This does not include State Pension."

  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Do you get UK pension scheme payments?"
    val expectedHeading = "Do you get UK pension scheme payments?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorMessage = "Select Yes if you got payments from UK pension schemes"

  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Do you get UK pension scheme payments?"
    val expectedHeading = "Do you get UK pension scheme payments?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorMessage = "Select Yes if you got payments from UK pension schemes"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Does your client get UK pension scheme payments?"
    val expectedHeading = "Does your client get UK pension scheme payments?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorMessage = "Select Yes if your client got payments from UK pension schemes"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Does your client get UK pension scheme payments?"
    val expectedHeading = "Does your client get UK pension scheme payments?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorMessage = "Select Yes if your client got payments from UK pension schemes"
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
        "render uk pension schemes payments question page with no pre-filled radio buttons" which {
          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            val pensionsViewModel = anIncomeFromPensionsViewModel.copy(uKPensionIncomesQuestion = None)
            insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(incomeFromPensions = pensionsViewModel)), aUserRequest)
            urlGet(fullUrl(ukPensionSchemePayments(taxYearEOY)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
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
          textOnPageCheck(expectedDoesNotIncludeText, expectedDoesNotIncludeSelector)
          formPostLinkCheck(ukPensionSchemePayments(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }
        "render uk pension schemes payments question page with 'Yes' pre-filled when CYA data exists" which {
          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel), aUserRequest)
            urlGet(fullUrl(ukPensionSchemePayments(taxYearEOY)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
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
          textOnPageCheck(expectedDoesNotIncludeText, expectedDoesNotIncludeSelector)
          formPostLinkCheck(ukPensionSchemePayments(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render uk pension schemes payments question page with 'No' pre-filled and not a prior submission" which {
          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            val pensionsViewModel = anIncomeFromPensionsViewModel.copy(uKPensionIncomesQuestion = Some(false))
            insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(incomeFromPensions = pensionsViewModel)), aUserRequest)
            urlGet(fullUrl(ukPensionSchemePayments(taxYearEOY)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
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
          textOnPageCheck(expectedDoesNotIncludeText, expectedDoesNotIncludeSelector)
          formPostLinkCheck(ukPensionSchemePayments(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "redirect to the Pension Summary page if there is no session data" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(ukPensionSchemePayments(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(pensionSummaryUrl(taxYearEOY)) shouldBe true
      }

    }

    "Redirect user to the pension summary page when in year" which {

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val pensionsViewModel = anIncomeFromPensionsViewModel.copy(uKPensionIncomesQuestion = None)
        insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(incomeFromPensions = pensionsViewModel)), aUserRequest)
        urlGet(fullUrl(ukPensionSchemePayments(taxYear)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
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
            authoriseAgentOrIndividual(user.isAgent)

            val pensionsViewModel = anIncomeFromPensionsViewModel.copy(uKPensionIncomesQuestion = None)
            insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(incomeFromPensions = pensionsViewModel)), aUserRequest)

            urlPost(fullUrl(ukPensionSchemePayments(taxYearEOY)), body = form, welsh = user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
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
          textOnPageCheck(expectedDoesNotIncludeText, expectedDoesNotIncludeSelector)
          formPostLinkCheck(ukPensionSchemePayments(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
          errorSummaryCheck(user.specificExpectedResults.get.expectedErrorMessage, Selectors.yesSelector)
          errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorMessage, Some("value"))

        }
      }
    }


    "redirect to Pension Scheme details page when user selects 'yes' and not a prior submission" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val pensionsViewModel = anIncomeFromPensionsViewModel.copy(uKPensionIncomesQuestion = None)
        insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(incomeFromPensions = pensionsViewModel)), aUserRequest)
        urlPost(fullUrl(ukPensionSchemePayments(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionSchemeDetailsUrl(taxYearEOY))
      }

      "updates amount paid question to Some(true)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.incomeFromPensions.statePension.flatMap(_.amountPaidQuestion) shouldBe Some(true)
      }
    }
  }

  "redirect to CYA page if there is no session data" which {
    lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
    lazy val result: WSResponse = {
      dropPensionsDB()
      authoriseAgentOrIndividual(isAgent = false)
      urlPost(fullUrl(ukPensionSchemePayments(taxYearEOY)), body = form, follow = false,
        headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
    }

    "has an SEE_OTHER status" in {
      result.status shouldBe SEE_OTHER
      // TODO redirect to CYA Page
      result.header("location").contains(pensionSummaryUrl(taxYearEOY)) shouldBe true
    }

  }

  "redirect to Pension CYA page when user selects 'no'" which {
    lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
    lazy val result: WSResponse = {
      dropPensionsDB()
      authoriseAgentOrIndividual(isAgent = false)
      userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
      val pensionsViewModel = anIncomeFromPensionsViewModel.copy(uKPensionIncomesQuestion = Some(true))
      insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(incomeFromPensions = pensionsViewModel)), aUserRequest)

      urlPost(fullUrl(ukPensionSchemePayments(taxYearEOY)), body = form, follow = false,
        headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
    }

    "has a SEE_OTHER(303) status" in {
      result.status shouldBe SEE_OTHER
      //TODO redirect to Pension CYA page
      result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
    }

    "updates uKPensionIncomesQuestion to Some(false) and wipe the sets the uk pensions list to Seq.empty" in {
      lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
      cyaModel.pensions.incomeFromPensions.uKPensionIncomesQuestion shouldBe Some(false)
      cyaModel.pensions.incomeFromPensions.uKPensionIncomes shouldBe Seq.empty
    }

  }

  "Redirect user to the pension summary page when in year" which {
    lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
    lazy val result: WSResponse = {

      dropPensionsDB()
      authoriseAgentOrIndividual(isAgent = false)
      userDataStub(anIncomeTaxUserData, nino, taxYear)
      val pensionsViewModel = anIncomeFromPensionsViewModel.copy(uKPensionIncomesQuestion = Some(true))
      insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(incomeFromPensions = pensionsViewModel)), aUserRequest)
      urlPost(fullUrl(ukPensionSchemePayments(taxYear)), body = form, follow = false,
        headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)))
    }

    "has an SEE_OTHER(303) status" in {
      result.status shouldBe SEE_OTHER
      result.header("location").contains(overviewUrl(taxYear)) shouldBe true
    }

    "updates uKPensionIncomesQuestion to Some(true) and check pensions is not empty" in {
      lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
      cyaModel.pensions.incomeFromPensions.uKPensionIncomesQuestion shouldBe Some(true)
      Seq(cyaModel.pensions.incomeFromPensions.uKPensionIncomes).length.shouldBe(1)
    }
  }

}
