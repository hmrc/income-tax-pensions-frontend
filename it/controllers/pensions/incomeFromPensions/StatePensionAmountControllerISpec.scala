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
import builders.StateBenefitViewModelBuilder.anStateBenefitViewModelOne
import builders.UserBuilder.aUserRequest
import forms.AmountForm
import models.mongo.PensionsCYAModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.IncomeFromPensionsPages.{statePension, statePensionAmountUrl}
import utils.PageUrls.{fullUrl, overviewUrl, pensionSummaryUrl}
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

class StatePensionAmountControllerISpec extends IntegrationTest with ViewHelpers with BeforeAndAfterEach with PensionsDatabaseHelper {

  private val newAmount = 25
  private val poundPrefixText = "£"
  private val amountInputName = "amount"

  private def pensionsUsersData(isPrior: Boolean = false, pensionsCyaModel: PensionsCYAModel) = {
    PensionsUserDataBuilder.aPensionsUserData.copy(isPriorSubmission = isPrior, pensions = pensionsCyaModel)
  }

  object Selectors {
    val captionSelector: String = "#main-content > div > div > header > p"
    val continueButtonSelector: String = "#continue"
    val formSelector: String = "#main-content > div > div > form"
    val hintTextSelector = "#amount-hint"
    val poundPrefixSelector = ".govuk-input__prefix"
    val inputSelector = "#amount"
    val expectedErrorHref = "#amount"
    val expectedYouToldUsSelector = "#main-content > div > div > p"
    val whereToFindThisInformationSelector: String = "#main-content > div > div > form > details > summary > span"
    val paragraphFindThisInfoSelector = "#main-content > div > div > form > details > div > p"
    def paragraphSelector(index: Int): String = s"#main-content > div > div > form > details > div > ul > li:nth-child($index)"

  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val hintText: String
    val buttonText: String
    val expectedWhereToFindThisInformation: String
    val expectedYouCanFindThisInformationIn: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedErrorTitle: String
    val emptyErrorText: String
    val invalidFormatErrorText: String
    val maxAmountErrorText: String
    val expectedDetailsExample1: String
    val expectedDetailsExample2: String
    val expectedYouToldUsText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Income from pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val hintText = "For example, £193.52"
    val buttonText = "Continue"
    val expectedWhereToFindThisInformation = "Where to find this information"
    val expectedYouCanFindThisInformationIn = "You can find this information in:"

  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Income from pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val hintText = "For example, £193.52"
    val buttonText = "Continue"
    val expectedWhereToFindThisInformation = "Where to find this information"
    val expectedYouCanFindThisInformationIn = "You can find this information in:"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "How much was your State Pension?"
    val expectedHeading = "How much was your State Pension?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val emptyErrorText = "Enter your State Pension amount"
    val invalidFormatErrorText = "Enter your State Pension amount in the correct format"
    val maxAmountErrorText = "Your State Pension amount must be less than £100,000,000,000"
    val expectedDetailsExample1 = "your P60"
    val expectedDetailsExample2 = "the ‘About general increases in benefits’ letter the Pension Service sent you"
    val expectedYouToldUsText = "You told us you did not get State Pension of £8,400 this year. Tell us how much you got."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "How much was your State Pension?"
    val expectedHeading = "How much was your State Pension?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val emptyErrorText = "Enter your State Pension amount"
    val invalidFormatErrorText = "Enter your State Pension amount in the correct format"
    val maxAmountErrorText = "Your State Pension amount must be less than £100,000,000,000"
    val expectedDetailsExample1 = "your P60"
    val expectedDetailsExample2 = "the ‘About general increases in benefits’ letter the Pension Service sent you"
    val expectedYouToldUsText = "You told us you did not get State Pension of £8,400 this year. Tell us how much you got."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "How much was your client’s State Pension?"
    val expectedHeading = "How much was your client’s State Pension?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val emptyErrorText = "Enter your client’s State Pension amount"
    val invalidFormatErrorText = "Enter your client’s State Pension amount in the correct format"
    val maxAmountErrorText = "Your client’s State Pension amount must be less than £100,000,000,000"
    val expectedDetailsExample1 = "your client’s P60"
    val expectedDetailsExample2 = "the ‘About general increases in benefits’ letter the Pension Service sent your client"
    val expectedYouToldUsText = "You told us your client did not get State Pension of £8,400 this year. Tell us how much they got."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "How much was your client’s State Pension?"
    val expectedHeading = "How much was your client’s State Pension?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val emptyErrorText = "Enter your client’s State Pension amount"
    val invalidFormatErrorText = "Enter your client’s State Pension amount in the correct format"
    val maxAmountErrorText = "Your client’s State Pension amount must be less than £100,000,000,000"
    val expectedDetailsExample1 = "your client’s P60"
    val expectedDetailsExample2 = "the ‘About general increases in benefits’ letter the Pension Service sent your client"
    val expectedYouToldUsText = "You told us your client did not get State Pension of £8,400 this year. Tell us how much they got."
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))

  ".show" should {
    userScenarios.foreach { user =>
      import Selectors._
      import user.commonExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "render Did you get a state pension amount page with no value when no cya data" which {
          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            val pensionsViewModel = anIncomeFromPensionsViewModel.copy(
              statePension= Some(anStateBenefitViewModelOne.copy(amountPaidQuestion = Some(true), amount = None)))
            insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(incomeFromPensions = pensionsViewModel)), aUserRequest)
            urlGet(fullUrl(statePensionAmountUrl(taxYearEOY)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }
          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          textOnPageCheck(expectedWhereToFindThisInformation, whereToFindThisInformationSelector)
          textOnPageCheck(expectedYouCanFindThisInformationIn, paragraphFindThisInfoSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedDetailsExample1, paragraphSelector(1))
          textOnPageCheck(user.specificExpectedResults.get.expectedDetailsExample2, paragraphSelector(2))
          inputFieldValueCheck(amountInputName, inputSelector, "")
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(statePensionAmountUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render Did you get a state pension amount page page prefilled when cya data" which {

          val existingAmount: String = "999.88"
          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            val pensionsViewModel = anIncomeFromPensionsViewModel.copy(
              statePension = Some(anStateBenefitViewModelOne.copy(amountPaidQuestion = Some(true), amount = Some(BigDecimal(existingAmount)))))
            insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(incomeFromPensions = pensionsViewModel)), aUserRequest)
            urlGet(fullUrl(statePensionAmountUrl(taxYearEOY)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }
          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(expectedWhereToFindThisInformation, whereToFindThisInformationSelector)
          textOnPageCheck(expectedYouCanFindThisInformationIn, paragraphFindThisInfoSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedDetailsExample1, paragraphSelector(1))
          textOnPageCheck(user.specificExpectedResults.get.expectedDetailsExample2, paragraphSelector(2))
          inputFieldValueCheck(amountInputName, inputSelector, existingAmount)
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(statePensionAmountUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "redirect to overview page if the user is in year" which {

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYear)
        val pensionsViewModel = anIncomeFromPensionsViewModel.copy(
          statePension = Some(anStateBenefitViewModelOne.copy(amountPaidQuestion = Some(true), amount = None)))
        insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(incomeFromPensions = pensionsViewModel)), aUserRequest)
        urlGet(fullUrl(statePensionAmountUrl(taxYear)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }

    "redirect to the did you get state pension Question page if the question has not been answered" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val pensionsViewModel = anIncomeFromPensionsViewModel.copy(
          statePension= Some(anStateBenefitViewModelOne.copy(amountPaidQuestion = None, amount = None)))
        insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(incomeFromPensions = pensionsViewModel)), aUserRequest)
        urlGet(fullUrl(statePensionAmountUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      implicit def document: () => Document = () => Jsoup.parse(result.body)

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(statePension(taxYearEOY)) shouldBe true
      }
    }

    "redirect to did you get state pension  Question page if question has been answered as false" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val pensionsViewModel = anIncomeFromPensionsViewModel.copy(
          statePension = Some(anStateBenefitViewModelOne.copy(amountPaidQuestion = Some(false), amount = None)))
        insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(incomeFromPensions = pensionsViewModel)), aUserRequest)
        urlGet(fullUrl(statePensionAmountUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      implicit def document: () => Document = () => Jsoup.parse(result.body)

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(statePension(taxYearEOY)) shouldBe true
      }
    }

    "redirect to the CYA page if there is no session data" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(statePensionAmountUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }
      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        //todo - redirect to income from pensions cya page
        result.header("location").contains(pensionSummaryUrl(taxYearEOY)) shouldBe true
      }
    }
  }

  ".submit" should {
    userScenarios.foreach { user =>
      import Selectors._
      import user.commonExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        "return an error when form is submitted with no input entry" which {
          val amountEmpty = ""
          val emptyForm: Map[String, String] = Map(AmountForm.amount -> amountEmpty)
          lazy val result: WSResponse = {
            dropPensionsDB()
             val pensionsViewModel = anIncomeFromPensionsViewModel.copy(
               statePension = Some(anStateBenefitViewModelOne.copy(amountPaidQuestion= Some(true), amount = None)))
            insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(incomeFromPensions = pensionsViewModel)), aUserRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(fullUrl(statePensionAmountUrl(taxYearEOY)), body = emptyForm, welsh = user.isWelsh,
              follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }
          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedYouToldUsText, expectedYouToldUsSelector)
          textOnPageCheck(expectedWhereToFindThisInformation, whereToFindThisInformationSelector)
          textOnPageCheck(expectedYouCanFindThisInformationIn, paragraphFindThisInfoSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedDetailsExample1, paragraphSelector(1))
          textOnPageCheck(user.specificExpectedResults.get.expectedDetailsExample2, paragraphSelector(2))
          inputFieldValueCheck(amountInputName, inputSelector, amountEmpty)
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(statePensionAmountUrl(taxYearEOY), formSelector)
          errorSummaryCheck(user.specificExpectedResults.get.emptyErrorText, expectedErrorHref)
          errorAboveElementCheck(user.specificExpectedResults.get.emptyErrorText)
          welshToggleCheck(user.isWelsh)
        }

        "return an error when form is submitted with an invalid format input" which {

          val amountInvalidFormat = "invalid"
          val invalidFormatForm: Map[String, String] = Map(AmountForm.amount -> amountInvalidFormat)

          lazy val result: WSResponse = {
            dropPensionsDB()
            val pensionsViewModel = anIncomeFromPensionsViewModel.copy(
              statePension = Some(anStateBenefitViewModelOne.copy(amountPaidQuestion= Some(true), amount = None)))
            insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(incomeFromPensions = pensionsViewModel)), aUserRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(fullUrl(statePensionAmountUrl(taxYearEOY)), body = invalidFormatForm, welsh = user.isWelsh,
              follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedYouToldUsText, expectedYouToldUsSelector)
          textOnPageCheck(expectedWhereToFindThisInformation, whereToFindThisInformationSelector)
          textOnPageCheck(expectedYouCanFindThisInformationIn, paragraphFindThisInfoSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedDetailsExample1, paragraphSelector(1))
          textOnPageCheck(user.specificExpectedResults.get.expectedDetailsExample2, paragraphSelector(2))
          inputFieldValueCheck(amountInputName, inputSelector, amountInvalidFormat)
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(statePensionAmountUrl(taxYearEOY), formSelector)
          errorSummaryCheck(user.specificExpectedResults.get.invalidFormatErrorText, expectedErrorHref)
          errorAboveElementCheck(user.specificExpectedResults.get.invalidFormatErrorText)
          welshToggleCheck(user.isWelsh)

        }

        "return an error when form is submitted with input over maximum allowed value" which {

          val amountOverMaximum = "100,000,000,000"
          val overMaximumForm: Map[String, String] = Map(AmountForm.amount -> amountOverMaximum)

          lazy val result: WSResponse = {
            dropPensionsDB()
            val pensionsViewModel = anIncomeFromPensionsViewModel.copy(
              statePension = Some(anStateBenefitViewModelOne.copy(amountPaidQuestion= Some(true), amount = None)))
            insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(incomeFromPensions = pensionsViewModel)), aUserRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(fullUrl(statePensionAmountUrl(taxYearEOY)),
              body = overMaximumForm, welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }
          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedYouToldUsText, expectedYouToldUsSelector)
          textOnPageCheck(expectedWhereToFindThisInformation, whereToFindThisInformationSelector)
          textOnPageCheck(expectedYouCanFindThisInformationIn, paragraphFindThisInfoSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedDetailsExample1, paragraphSelector(1))
          textOnPageCheck(user.specificExpectedResults.get.expectedDetailsExample2, paragraphSelector(2))
          inputFieldValueCheck(amountInputName, inputSelector, amountOverMaximum)
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(statePensionAmountUrl(taxYearEOY), formSelector)
          errorSummaryCheck(user.specificExpectedResults.get.maxAmountErrorText, expectedErrorHref)
          errorAboveElementCheck(user.specificExpectedResults.get.maxAmountErrorText)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "redirect to overview page if the user is in year" which {
      lazy val form: Map[String, String] = Map(AmountForm.amount -> s"$newAmount")
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYear)
        val pensionsViewModel = anIncomeFromPensionsViewModel.copy(
          statePension = Some(anStateBenefitViewModelOne.copy(amountPaidQuestion = Some(true), amount = None)))
        insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(incomeFromPensions = pensionsViewModel)), aUserRequest)
        urlPost(fullUrl(statePensionAmountUrl(taxYear)),
          body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(overviewUrl(taxYear)) shouldBe true
      }
    }

    "redirect to the did you get state pension Question page if the question has not been answered" which {

      lazy val form: Map[String, String] = Map(AmountForm.amount -> s"$newAmount")
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val pensionsViewModel = anIncomeFromPensionsViewModel.copy(
          statePension = Some(anStateBenefitViewModelOne.copy(amountPaidQuestion = None, amount = None)))
        insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(incomeFromPensions = pensionsViewModel)), aUserRequest)
        urlPost(fullUrl(statePensionAmountUrl(taxYearEOY)),
          body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      implicit def document: () => Document = () => Jsoup.parse(result.body)

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(statePension(taxYearEOY)) shouldBe true
      }

      "update state pension amount to None" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.incomeFromPensions.statePension.flatMap(_.amount) shouldBe None
      }
    }

    "redirect to the correct page when a valid amount is submitted and update the session amount" which {

      lazy val form: Map[String, String] = Map(AmountForm.amount -> s"$newAmount")
      lazy val result: WSResponse = {
        dropPensionsDB()
        val pensionsViewModel = anIncomeFromPensionsViewModel.copy(
          statePension = Some(anStateBenefitViewModelOne.copy(amountPaidQuestion= Some(true), amount = None)))
            insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(incomeFromPensions = pensionsViewModel)), aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(statePensionAmountUrl(taxYearEOY)),
          body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        //todo - redirect to cya page
        result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
      }

      "update state pension amount to Some(newAmount)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.incomeFromPensions.statePension.flatMap(_.amount) shouldBe Some(BigDecimal(newAmount))
      }
    }
  }

}
