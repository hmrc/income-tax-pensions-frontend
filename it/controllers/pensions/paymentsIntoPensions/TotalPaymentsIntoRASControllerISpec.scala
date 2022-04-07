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

import builders.IncomeFromPensionsViewModelBuilder.anIncomeFromPensionEmptyViewModel
import builders.PensionAnnualAllowanceViewModelBuilder.aPensionAnnualAllowanceEmptyViewModel
import builders.PensionLifetimeAllowanceViewModelBuilder.aPensionLifetimeAllowancesEmptyViewModel
import builders.PensionsCYAModelBuilder.aPensionsCYAEmptyModel
import builders.PensionsUserDataBuilder
import builders.PensionsUserDataBuilder.aPensionsUserData
import builders.UserBuilder.aUserRequest
import forms.YesNoForm
import models.mongo.PensionsCYAModel
import models.pension.reliefs.PaymentsIntoPensionViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.PaymentIntoPensions._
import utils.PageUrls.fullUrl
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

class TotalPaymentsIntoRASControllerISpec extends IntegrationTest with BeforeAndAfterEach with ViewHelpers with PensionsDatabaseHelper {

  private val oneOffAmount: String = "1,400"
  private val rasTotal: String = "8,800"

  private val calculatedRAS: String = "7,040"
  private val calculatedRelief: String = "1,760"

  private def pensionsUsersData(paymentsIntoPensionViewModel: PaymentsIntoPensionViewModel) = {
    PensionsUserDataBuilder.aPensionsUserData.copy(
      pensions = PensionsCYAModel(paymentsIntoPensionViewModel, aPensionAnnualAllowanceEmptyViewModel,
        aPensionLifetimeAllowancesEmptyViewModel, anIncomeFromPensionEmptyViewModel)
    )
  }

  private val requiredViewModel = PaymentsIntoPensionViewModel(
    rasPensionPaymentQuestion = Some(true),
    totalRASPaymentsAndTaxRelief = Some(BigDecimal(7400)),
    oneOffRasPaymentPlusTaxReliefQuestion = Some(true),
    totalOneOffRasPaymentPlusTaxRelief = Some(BigDecimal(1400))
  )

  object Selectors {
    val captionSelector: String = "#main-content > div > div > header > p"
    val continueButtonSelector: String = "#continue"
    val formSelector: String = "#main-content > div > div > form"
    val yesSelector = "#value"
    val noSelector = "#value-no"
    val pSelector = "#main-content > div > div > p"
    val isCorrectSelector = "#main-content > div > div > form > div > fieldset > legend"
    val tableSelector: (Int, Int) => String = (row, column) =>
      s"#main-content > div > div > table > tbody > tr:nth-child($row) > td:nth-of-type($column)"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedParagraph: String
    val expectedErrorTitle: String
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val totalPayments: String
    val oneOff: String
    val claimed: String
    val total: String
    val isCorrect: String
    val expectedError: String
    val expectedButtonText: String
    val yesText: String
    val noText: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle: String = "Your total payments into relief at source (RAS) pensions"
    val expectedParagraph: String = s"The total amount you paid, plus basic rate tax relief, is £$rasTotal. " +
      "You can find this figure on the pension certificate or receipt from your administrator."
    val expectedErrorTitle: String = s"Error: $expectedTitle"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle: String = "Your total payments into relief at source (RAS) pensions"
    val expectedParagraph: String = s"The total amount you paid, plus basic rate tax relief, is £$rasTotal. " +
      "You can find this figure on the pension certificate or receipt from your administrator."
    val expectedErrorTitle: String = s"Error: $expectedTitle"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle: String = "Your client’s total payments into relief at source (RAS) pensions"
    val expectedParagraph: String = s"The total amount your client paid, plus basic rate tax relief, is £$rasTotal. " +
      "You can find this figure on the pension certificate or receipt from your client’s administrator."
    val expectedErrorTitle: String = s"Error: $expectedTitle"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle: String = "Your client’s total payments into relief at source (RAS) pensions"
    val expectedParagraph: String = s"The total amount your client paid, plus basic rate tax relief, is £$rasTotal. " +
      "You can find this figure on the pension certificate or receipt from your client’s administrator."
    val expectedErrorTitle: String = s"Error: $expectedTitle"
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Payments into pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val totalPayments: String = "Total pension payments"
    val oneOff: String = s"(Including £$oneOffAmount one-off payments)"
    val claimed: String = "Tax relief claimed by scheme"
    val total: String = "Total"
    val isCorrect: String = "Is this correct?"
    val expectedError: String = "Select yes if the figures are correct"
    val expectedButtonText: String = "Continue"
    val yesText = "Yes"
    val noText = "No"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Payments into pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val totalPayments: String = "Total pension payments"
    val oneOff: String = s"(Including £$oneOffAmount one-off payments)"
    val claimed: String = "Tax relief claimed by scheme"
    val total: String = "Total"
    val isCorrect: String = "Is this correct?"
    val expectedError: String = "Select yes if the figures are correct"
    val expectedButtonText: String = "Continue"
    val yesText = "Yes"
    val noText = "No"
  }

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

        "render 'Total payments into RAS pensions' page with correct content and no pre-filling" which {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropPensionsDB()
            insertCyaData(pensionsUsersData(requiredViewModel), aUserRequest)
            urlGet(fullUrl(totalPaymentsIntoRASUrl(taxYearEOY)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedTitle)
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph, pSelector)
          textOnPageCheck(isCorrect, isCorrectSelector)
          radioButtonCheck(yesText, 1, checked = Some(false))
          radioButtonCheck(noText, 2, checked = Some(false))
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(totalPaymentsIntoRASUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)

          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(s"$totalPayments $oneOff", tableSelector(1, 1))
          textOnPageCheck(s"£$calculatedRAS", tableSelector(1, 2))
          textOnPageCheck(claimed, tableSelector(2, 1))
          textOnPageCheck(s"£$calculatedRelief", tableSelector(2, 2))
          textOnPageCheck(total, tableSelector(3, 1))
          textOnPageCheck(s"£$rasTotal", tableSelector(3, 2))
        }
      }
    }

    "render 'Total payments into RAS pensions' page without a one-off amount" should {
      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropPensionsDB()
        insertCyaData(pensionsUsersData(requiredViewModel.copy(
          oneOffRasPaymentPlusTaxReliefQuestion = Some(false),
          totalOneOffRasPaymentPlusTaxRelief = None)
        ), aUserRequest)
        urlGet(fullUrl(totalPaymentsIntoRASUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has an OK status" in {
        result.status shouldBe OK
      }

      implicit def document: () => Document = () => Jsoup.parse(result.body)

      textOnPageCheck(s"${CommonExpectedEN.totalPayments}", Selectors.tableSelector(1, 1))
    }

    "render the page with the radio button pre-filled" should {

      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropPensionsDB()
        insertCyaData(pensionsUsersData(requiredViewModel.copy(totalPaymentsIntoRASQuestion = Some(true))), aUserRequest)
        urlGet(fullUrl(totalPaymentsIntoRASUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }
      implicit def document: () => Document = () => Jsoup.parse(result.body)

      "have the yes button pre-filled" when {
        radioButtonCheck(CommonExpectedEN.yesText, 1, checked = Some(true))
        radioButtonCheck(CommonExpectedEN.noText, 2, checked = Some(false))
      }
    }

    "redirect to the check your answers CYA page when there is no CYA data" when {

      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropPensionsDB()
        urlGet(fullUrl(totalPaymentsIntoRASUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has a SEE_OTHER status and redirects correctly" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkPaymentsIntoPensionCyaUrl(taxYearEOY)) shouldBe true
      }
    }

    "redirect to the 'RAS Payments And Tax Relief Amount' page if does not have a RAS amount in CYA" when {

      val userDataModel = aPensionsUserData.copy(
        pensions = aPensionsCYAEmptyModel.copy(
          paymentsIntoPension = PaymentsIntoPensionViewModel(rasPensionPaymentQuestion = Some(true))))

      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropPensionsDB()
        insertCyaData(userDataModel, aUserRequest)
        urlGet(fullUrl(totalPaymentsIntoRASUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has a SEE_OTHER status and redirects correctly" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(reliefAtSourcePaymentsAndTaxReliefAmountUrl(taxYearEOY)) shouldBe true
      }
    }
  }

  ".submit" should {

    userScenarios.foreach { user =>

      import Selectors._
      import user.commonExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "return an error when form is submitted with no entry" which {
          lazy val invalidForm: Map[String, String] = Map(YesNoForm.yesNo -> "")

          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropPensionsDB()
            insertCyaData(pensionsUsersData(requiredViewModel), aUserRequest)
            urlPost(fullUrl(totalPaymentsIntoRASUrl(taxYearEOY)), body = invalidForm, user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedTitle)
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph, pSelector)
          textOnPageCheck(isCorrect, isCorrectSelector)
          radioButtonCheck(yesText, 1, checked = Some(false))
          radioButtonCheck(noText, 2, checked = Some(false))
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(totalPaymentsIntoRASUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)

          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(s"$totalPayments $oneOff", tableSelector(1, 1))
          textOnPageCheck(s"£$calculatedRAS", tableSelector(1, 2))
          textOnPageCheck(claimed, tableSelector(2, 1))
          textOnPageCheck(s"£$calculatedRelief", tableSelector(2, 2))
          textOnPageCheck(total, tableSelector(3, 1))
          textOnPageCheck(s"£$rasTotal", tableSelector(3, 2))

          errorSummaryCheck(expectedError, Selectors.yesSelector)
          errorAboveElementCheck(expectedError, Some("value"))
        }
      }
    }

    "redirect to the next page and update the session value when the user submits 'yes'" should {

      lazy val yesForm: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropPensionsDB()
        insertCyaData(pensionsUsersData(requiredViewModel), aUserRequest)
        urlPost(fullUrl(totalPaymentsIntoRASUrl(taxYearEOY)), body = yesForm, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionTaxReliefNotClaimedUrl(taxYearEOY))
      }

      "updates totalPaymentsIntoRASQuestion to Some(true)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.paymentsIntoPension.totalPaymentsIntoRASQuestion shouldBe Some(true)

      }
    }

    "redirect to the 'RAS payments and tax relief amount' page" when {

      "the user submits 'no'" should {
        lazy val noForm: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)

        lazy val result: WSResponse = {
          authoriseAgentOrIndividual(isAgent = false)
          dropPensionsDB()
          insertCyaData(pensionsUsersData(requiredViewModel), aUserRequest)
          urlPost(fullUrl(totalPaymentsIntoRASUrl(taxYearEOY)), body = noForm, follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has a SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(reliefAtSourcePaymentsAndTaxReliefAmountUrl(taxYearEOY))
        }

        "updates totalPaymentsIntoRASQuestion to Some(false)" in {
          lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
          cyaModel.pensions.paymentsIntoPension.totalPaymentsIntoRASQuestion shouldBe Some(false)

        }
      }

      "the user does not have a value for 'totalRASPaymentsAndTaxRelief' in session and does not select an answer" should {
        lazy val invalidForm: Map[String, String] = Map(YesNoForm.yesNo -> "")

        val userDataModel = aPensionsUserData.copy(
          pensions = aPensionsCYAEmptyModel.copy(
            paymentsIntoPension = PaymentsIntoPensionViewModel(rasPensionPaymentQuestion = Some(true))))

        lazy val result: WSResponse = {
          authoriseAgentOrIndividual(isAgent = false)
          dropPensionsDB()
          insertCyaData(userDataModel, aUserRequest)
          urlPost(fullUrl(totalPaymentsIntoRASUrl(taxYearEOY)), body = invalidForm, follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has a SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(reliefAtSourcePaymentsAndTaxReliefAmountUrl(taxYearEOY))
        }

        "keep totalPaymentsIntoRASQuestion as None" in {
          lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
          cyaModel.pensions.paymentsIntoPension.totalPaymentsIntoRASQuestion shouldBe None

        }
      }
    }

    "redirect to the CYA page when the user does not have data in session" when {
      lazy val noForm: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)

      lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropPensionsDB()
        urlPost(fullUrl(totalPaymentsIntoRASUrl(taxYearEOY)), body = noForm, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(checkPaymentsIntoPensionCyaUrl(taxYearEOY))
      }
    }
  }
}
