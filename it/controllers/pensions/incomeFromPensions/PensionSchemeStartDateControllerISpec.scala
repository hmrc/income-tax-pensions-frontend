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

package controllers.pensions.incomeFromPensions

import builders.IncomeFromPensionsViewModelBuilder.anIncomeFromPensionsViewModel
import builders.PensionsUserDataBuilder.{aPensionsUserData, pensionsUserDataWithIncomeFromPensions}
import builders.UkPensionIncomeViewModelBuilder.{anUkPensionIncomeViewModelOne, anUkPensionIncomeViewModelTwo}
import builders.UserBuilder.aUserRequest
import forms.PensionSchemeDateForm
import models.pension.statebenefits.IncomeFromPensionsViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.IncomeFromPensionsPages.{pensionStartDateUrl, ukPensionIncomeCyaUrl, ukPensionSchemeSummaryListUrl}
import utils.PageUrls.{fullUrl, pensionSummaryUrl}
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

import java.time.LocalDate

class PensionSchemeStartDateControllerISpec extends IntegrationTest with ViewHelpers with BeforeAndAfterEach with PensionsDatabaseHelper {

  private val dayInputName = "pensionStartDate-day"
  private val monthInputName = "pensionStartDate-month"
  private val yearInputName = "pensionStartDate-year"
  private val validDay = "27"
  private val validMonth = "10"
  private val validYear = "2021"

  def startDateForm(day: String, month: String, year: String): Map[String, String] = Map(
    PensionSchemeDateForm.day -> day,
    PensionSchemeDateForm.month -> month,
    PensionSchemeDateForm.year -> year
  )

  object Selectors {
    val captionSelector: String = "#main-content > div > div > header > p"
    val continueButtonSelector: String = "#continue"
    val formSelector: String = "#main-content > div > div > form"
    val hintSelector = "#pensionStartDate-hint"
    val dayInputSelector = "#day"
    val monthInputSelector = "#month"
    val yearInputSelector = "#year"
    val dayInputHref = "#day"
    val monthInputHref = "#month"
    val yearInputHref = "#year"

    def labelSelector(index: Int): String = s"#pensionStartDate > div:nth-child($index) > div > label"
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val buttonText: String
    val expectedHintText: String
    val expectedDayLabel: String
    val expectedMonthLabel: String
    val expectedYearLabel: String
    val dateInFutureErrorText: String
    val realDateErrorText: String
    val tooLongAgoErrorText: String
    val emptyAllErrorText: String
    val emptyDayErrorText: String
    val emptyDayMonthErrorText: String
    val emptyDayYearErrorText: String
    val emptyMonthErrorText: String
    val emptyMonthYearErrorText: String
    val emptyYearErrorText: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedErrorTitle: String

  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Income from pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val buttonText = "Continue"
    val expectedHintText = "For example, 12 11 2007"
    val expectedDayLabel = "Day"
    val expectedMonthLabel = "Month"
    val expectedYearLabel = "Year"
    val dateInFutureErrorText = "The pension start date must be in the past"
    val realDateErrorText = "The pension start date must be a real date"
    val tooLongAgoErrorText = "The pension start date must be after 1 January 1900"
    val emptyAllErrorText = "Enter the pension start date"
    val emptyDayErrorText = "The pension start date must include a day"
    val emptyDayMonthErrorText = "The pension start date must include a day and month"
    val emptyDayYearErrorText = "The pension start date must include a day and year"
    val emptyMonthErrorText = "The pension start date must include a month"
    val emptyMonthYearErrorText = "The pension start date must include a month and year"
    val emptyYearErrorText = "The pension start date must include a year"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Income from pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val buttonText = "Continue"
    val expectedHintText = "For example, 12 11 2007"
    val expectedDayLabel = "Diwrnod"
    val expectedMonthLabel = "Mis"
    val expectedYearLabel = "Blwyddyn"
    val dateInFutureErrorText = "The pension start date must be in the past"
    val realDateErrorText = "The pension start date must be a real date"
    val tooLongAgoErrorText = "The pension start date must be after 1 January 1900"
    val emptyAllErrorText = "Enter the pension start date"
    val emptyDayErrorText = "The pension start date must include a day"
    val emptyDayMonthErrorText = "The pension start date must include a day and month"
    val emptyDayYearErrorText = "The pension start date must include a day and year"
    val emptyMonthErrorText = "The pension start date must include a month"
    val emptyMonthYearErrorText = "The pension start date must include a month and year"
    val emptyYearErrorText = "The pension start date must include a year"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "When did you start getting payments from this scheme?"
    val expectedHeading = "When did you start getting payments from this scheme?"
    val expectedErrorTitle = s"Error: $expectedTitle"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "When did you start getting payments from this scheme?"
    val expectedHeading = "When did you start getting payments from this scheme?"
    val expectedErrorTitle = s"Error: $expectedTitle"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "When did your client start getting payments from this scheme?"
    val expectedHeading = "When did your client start getting payments from this scheme?"
    val expectedErrorTitle = s"Error: $expectedTitle"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "When did your client start getting payments from this scheme?"
    val expectedHeading = "When did your client start getting payments from this scheme?"
    val expectedErrorTitle = s"Error: $expectedTitle"
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
        "render Pension Start Date page with no prefilling" which {
          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            val viewModel: IncomeFromPensionsViewModel = anIncomeFromPensionsViewModel.copy(
              uKPensionIncomes = Seq(anUkPensionIncomeViewModelOne.copy(startDate = None)))
            insertCyaData(pensionsUserDataWithIncomeFromPensions(viewModel), aUserRequest)
            urlGet(fullUrl(pensionStartDateUrl(taxYearEOY, 0)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          inputFieldValueCheck(dayInputName, dayInputSelector, "")
          inputFieldValueCheck(monthInputName, monthInputSelector, "")
          inputFieldValueCheck(yearInputName, yearInputSelector, "")
          textOnPageCheck(expectedHintText, hintSelector)
          textOnPageCheck(expectedDayLabel, labelSelector(1))
          textOnPageCheck(expectedMonthLabel, labelSelector(2))
          textOnPageCheck(expectedYearLabel, labelSelector(3))
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(pensionStartDateUrl(taxYearEOY, 0), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render Pension Start Date page with prefilled date" which {
          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            val viewModel: IncomeFromPensionsViewModel = anIncomeFromPensionsViewModel.copy(
              uKPensionIncomes = Seq(anUkPensionIncomeViewModelOne.copy(startDate = Some(s"$validYear-$validMonth-$validDay"))))
            insertCyaData(pensionsUserDataWithIncomeFromPensions(viewModel), aUserRequest)
            urlGet(fullUrl(pensionStartDateUrl(taxYearEOY, 0)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          inputFieldValueCheck(dayInputName, dayInputSelector, validDay)
          inputFieldValueCheck(monthInputName, monthInputSelector, validMonth)
          inputFieldValueCheck(yearInputName, yearInputSelector, validYear)
          textOnPageCheck(expectedHintText, hintSelector)
          textOnPageCheck(expectedDayLabel, labelSelector(1))
          textOnPageCheck(expectedMonthLabel, labelSelector(2))
          textOnPageCheck(expectedYearLabel, labelSelector(3))
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(pensionStartDateUrl(taxYearEOY, 0), formSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "redirect to Uk Pension Incomes Summary page if index is out of bounds" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val viewModel: IncomeFromPensionsViewModel = anIncomeFromPensionsViewModel.copy(
          uKPensionIncomes = Seq(anUkPensionIncomeViewModelOne, anUkPensionIncomeViewModelTwo))
        insertCyaData(pensionsUserDataWithIncomeFromPensions(viewModel), aUserRequest)
        urlGet(fullUrl(pensionStartDateUrl(taxYearEOY, 2)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(ukPensionSchemeSummaryListUrl(taxYearEOY))
      }
    }

    "redirect to Uk Pension Income CYA page when there is no session data" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(pensionStartDateUrl(taxYearEOY, 0)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(ukPensionIncomeCyaUrl(taxYearEOY))
      }
    }

    "redirect to Uk Pension Incomes Summary page if no index is given" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(aPensionsUserData, aUserRequest)
        urlGet(fullUrl(pensionStartDateUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(ukPensionSchemeSummaryListUrl(taxYearEOY))
      }
    }
  }

  "submit" should {
    userScenarios.foreach { user =>
      import Selectors._
      import user.commonExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        s"return a BAD_REQUEST($BAD_REQUEST) status" when {

          "all fields are empty" which {
            lazy val form: Map[String, String] = startDateForm("", "", "")

            lazy val result: WSResponse = {
              dropPensionsDB()
              authoriseAgentOrIndividual(user.isAgent)
              insertCyaData(aPensionsUserData, aUserRequest)
              urlPost(fullUrl(pensionStartDateUrl(taxYearEOY, 0)), body = form, welsh = user.isWelsh, follow = false,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            inputFieldValueCheck(dayInputName, dayInputSelector, "")
            inputFieldValueCheck(monthInputName, monthInputSelector, "")
            inputFieldValueCheck(yearInputName, yearInputSelector, "")
            errorSummaryCheck(emptyAllErrorText, dayInputHref)
            errorAboveElementCheck(emptyAllErrorText)
            textOnPageCheck(expectedDayLabel, labelSelector(1))
            textOnPageCheck(expectedMonthLabel, labelSelector(2))
            textOnPageCheck(expectedYearLabel, labelSelector(3))
            textOnPageCheck(expectedHintText, hintSelector)
          }

          "the day field is empty" which {
            lazy val form: Map[String, String] = startDateForm("", validMonth, validYear)

            lazy val result: WSResponse = {
              dropPensionsDB()
              authoriseAgentOrIndividual(user.isAgent)
              insertCyaData(aPensionsUserData, aUserRequest)
              urlPost(fullUrl(pensionStartDateUrl(taxYearEOY, 0)), body = form, welsh = user.isWelsh, follow = false,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            inputFieldValueCheck(dayInputName, dayInputSelector, "")
            inputFieldValueCheck(monthInputName, monthInputSelector, validMonth)
            inputFieldValueCheck(yearInputName, yearInputSelector, validYear)
            errorSummaryCheck(emptyDayErrorText, dayInputHref)
            errorAboveElementCheck(emptyDayErrorText)
            textOnPageCheck(expectedDayLabel, labelSelector(1))
            textOnPageCheck(expectedMonthLabel, labelSelector(2))
            textOnPageCheck(expectedYearLabel, labelSelector(3))
            textOnPageCheck(expectedHintText, hintSelector)
          }

          "the day and month fields are empty" which {
            lazy val form: Map[String, String] = startDateForm("", "", validYear)

            lazy val result: WSResponse = {
              dropPensionsDB()
              authoriseAgentOrIndividual(user.isAgent)
              insertCyaData(aPensionsUserData, aUserRequest)
              urlPost(fullUrl(pensionStartDateUrl(taxYearEOY, 0)), body = form, welsh = user.isWelsh, follow = false,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            inputFieldValueCheck(dayInputName, dayInputSelector, "")
            inputFieldValueCheck(monthInputName, monthInputSelector, "")
            inputFieldValueCheck(yearInputName, yearInputSelector, validYear)
            errorSummaryCheck(emptyDayMonthErrorText, dayInputHref)
            errorAboveElementCheck(emptyDayMonthErrorText)
            textOnPageCheck(expectedDayLabel, labelSelector(1))
            textOnPageCheck(expectedMonthLabel, labelSelector(2))
            textOnPageCheck(expectedYearLabel, labelSelector(3))
            textOnPageCheck(expectedHintText, hintSelector)
          }

          "the day and year fields are empty" which {
            lazy val form: Map[String, String] = startDateForm("", validMonth, "")

            lazy val result: WSResponse = {
              dropPensionsDB()
              authoriseAgentOrIndividual(user.isAgent)
              insertCyaData(aPensionsUserData, aUserRequest)
              urlPost(fullUrl(pensionStartDateUrl(taxYearEOY, 0)), body = form, welsh = user.isWelsh, follow = false,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            inputFieldValueCheck(dayInputName, dayInputSelector, "")
            inputFieldValueCheck(monthInputName, monthInputSelector, validMonth)
            inputFieldValueCheck(yearInputName, yearInputSelector, "")
            errorSummaryCheck(emptyDayYearErrorText, dayInputHref)
            errorAboveElementCheck(emptyDayYearErrorText)
            textOnPageCheck(expectedDayLabel, labelSelector(1))
            textOnPageCheck(expectedMonthLabel, labelSelector(2))
            textOnPageCheck(expectedYearLabel, labelSelector(3))
            textOnPageCheck(expectedHintText, hintSelector)
          }

          "the month field is empty" which {
            lazy val form: Map[String, String] = startDateForm(validDay, "", validYear)

            lazy val result: WSResponse = {
              dropPensionsDB()
              authoriseAgentOrIndividual(user.isAgent)
              insertCyaData(aPensionsUserData, aUserRequest)
              urlPost(fullUrl(pensionStartDateUrl(taxYearEOY, 0)), body = form, welsh = user.isWelsh, follow = false,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            inputFieldValueCheck(dayInputName, dayInputSelector, validDay)
            inputFieldValueCheck(monthInputName, monthInputSelector, "")
            inputFieldValueCheck(yearInputName, yearInputSelector, validYear)
            errorSummaryCheck(emptyMonthErrorText, monthInputHref)
            errorAboveElementCheck(emptyMonthErrorText)
            textOnPageCheck(expectedDayLabel, labelSelector(1))
            textOnPageCheck(expectedMonthLabel, labelSelector(2))
            textOnPageCheck(expectedYearLabel, labelSelector(3))
            textOnPageCheck(expectedHintText, hintSelector)
          }

          "the month and year fields are empty" which {
            lazy val form: Map[String, String] = startDateForm(validDay, "", "")

            lazy val result: WSResponse = {
              dropPensionsDB()
              authoriseAgentOrIndividual(user.isAgent)
              insertCyaData(aPensionsUserData, aUserRequest)
              urlPost(fullUrl(pensionStartDateUrl(taxYearEOY, 0)), body = form, welsh = user.isWelsh, follow = false,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            inputFieldValueCheck(dayInputName, dayInputSelector, validDay)
            inputFieldValueCheck(monthInputName, monthInputSelector, "")
            inputFieldValueCheck(yearInputName, yearInputSelector, "")
            errorSummaryCheck(emptyMonthYearErrorText, monthInputHref)
            errorAboveElementCheck(emptyMonthYearErrorText)
            textOnPageCheck(expectedDayLabel, labelSelector(1))
            textOnPageCheck(expectedMonthLabel, labelSelector(2))
            textOnPageCheck(expectedYearLabel, labelSelector(3))
            textOnPageCheck(expectedHintText, hintSelector)
          }

          "the year field is empty" which {
            lazy val form: Map[String, String] = startDateForm(validDay, validMonth, "")

            lazy val result: WSResponse = {
              dropPensionsDB()
              authoriseAgentOrIndividual(user.isAgent)
              insertCyaData(aPensionsUserData, aUserRequest)
              urlPost(fullUrl(pensionStartDateUrl(taxYearEOY, 0)), body = form, welsh = user.isWelsh, follow = false,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            inputFieldValueCheck(dayInputName, dayInputSelector, validDay)
            inputFieldValueCheck(monthInputName, monthInputSelector, validMonth)
            inputFieldValueCheck(yearInputName, yearInputSelector, "")
            errorSummaryCheck(emptyYearErrorText, yearInputHref)
            errorAboveElementCheck(emptyYearErrorText)
            textOnPageCheck(expectedDayLabel, labelSelector(1))
            textOnPageCheck(expectedMonthLabel, labelSelector(2))
            textOnPageCheck(expectedYearLabel, labelSelector(3))
            textOnPageCheck(expectedHintText, hintSelector)
          }

          "the date submitted is in the future" which {
            val futureYear: Int = LocalDate.now().getYear + 1
            lazy val form: Map[String, String] = startDateForm(validDay, validMonth, s"$futureYear")

            lazy val result: WSResponse = {
              dropPensionsDB()
              authoriseAgentOrIndividual(user.isAgent)
              insertCyaData(aPensionsUserData, aUserRequest)
              urlPost(fullUrl(pensionStartDateUrl(taxYearEOY, 0)), body = form, welsh = user.isWelsh, follow = false,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            inputFieldValueCheck(dayInputName, dayInputSelector, validDay)
            inputFieldValueCheck(monthInputName, monthInputSelector, validMonth)
            inputFieldValueCheck(yearInputName, yearInputSelector, s"$futureYear")
            errorSummaryCheck(dateInFutureErrorText, dayInputHref)
            errorAboveElementCheck(dateInFutureErrorText)
            textOnPageCheck(expectedDayLabel, labelSelector(1))
            textOnPageCheck(expectedMonthLabel, labelSelector(2))
            textOnPageCheck(expectedYearLabel, labelSelector(3))
            textOnPageCheck(expectedHintText, hintSelector)
          }

          "the date submitted is invalid and not a real date" which {
            lazy val form: Map[String, String] = startDateForm(validDay, "13", validYear)

            lazy val result: WSResponse = {
              dropPensionsDB()
              authoriseAgentOrIndividual(user.isAgent)
              insertCyaData(aPensionsUserData, aUserRequest)
              urlPost(fullUrl(pensionStartDateUrl(taxYearEOY, 0)), body = form, welsh = user.isWelsh, follow = false,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            inputFieldValueCheck(dayInputName, dayInputSelector, validDay)
            inputFieldValueCheck(monthInputName, monthInputSelector, "13")
            inputFieldValueCheck(yearInputName, yearInputSelector, validYear)
            errorSummaryCheck(realDateErrorText, dayInputHref)
            errorAboveElementCheck(realDateErrorText)
            textOnPageCheck(expectedDayLabel, labelSelector(1))
            textOnPageCheck(expectedMonthLabel, labelSelector(2))
            textOnPageCheck(expectedYearLabel, labelSelector(3))
            textOnPageCheck(expectedHintText, hintSelector)
          }

          "the date submitted is too long ago before 1 Jan 1900" which {
            lazy val form: Map[String, String] = startDateForm("01", "01", "1898")

            lazy val result: WSResponse = {
              dropPensionsDB()
              authoriseAgentOrIndividual(user.isAgent)
              insertCyaData(aPensionsUserData, aUserRequest)
              urlPost(fullUrl(pensionStartDateUrl(taxYearEOY, 0)), body = form, welsh = user.isWelsh, follow = false,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            inputFieldValueCheck(dayInputName, dayInputSelector, "01")
            inputFieldValueCheck(monthInputName, monthInputSelector, "01")
            inputFieldValueCheck(yearInputName, yearInputSelector, "1898")
            errorSummaryCheck(tooLongAgoErrorText, dayInputHref)
            errorAboveElementCheck(tooLongAgoErrorText)
            textOnPageCheck(expectedDayLabel, labelSelector(1))
            textOnPageCheck(expectedMonthLabel, labelSelector(2))
            textOnPageCheck(expectedYearLabel, labelSelector(3))
            textOnPageCheck(expectedHintText, hintSelector)
          }

        }
      }
    }
    "redirect to update session data when valid form is submitted" which {
      lazy val form = startDateForm(validDay, validMonth, validYear)

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val viewModel: IncomeFromPensionsViewModel = anIncomeFromPensionsViewModel.copy(
          uKPensionIncomes = Seq(anUkPensionIncomeViewModelOne, anUkPensionIncomeViewModelTwo))
        insertCyaData(pensionsUserDataWithIncomeFromPensions(viewModel), aUserRequest)
        urlPost(fullUrl(pensionStartDateUrl(taxYearEOY, 1)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(ukPensionSchemeSummaryListUrl(taxYearEOY))
      }

      "updates correct scheme with new values" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.incomeFromPensions.uKPensionIncomes(1).startDate shouldBe Some(s"$validYear-$validMonth-$validDay")
      }
    }

    "redirect to the Uk Pension Incomes Summary page if index is out of bounds" which {
      lazy val form = startDateForm(validDay, validMonth, validYear)
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val viewModel: IncomeFromPensionsViewModel = anIncomeFromPensionsViewModel.copy(
          uKPensionIncomes = Seq(anUkPensionIncomeViewModelOne, anUkPensionIncomeViewModelTwo))
        insertCyaData(pensionsUserDataWithIncomeFromPensions(viewModel), aUserRequest)
        urlPost(fullUrl(pensionStartDateUrl(taxYearEOY, 0)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(ukPensionSchemeSummaryListUrl(taxYearEOY))
      }

    }

    "redirect to Uk Pension Income CYA page when there is no session data" which {
      lazy val form = startDateForm(validDay, validMonth, validYear)

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(pensionStartDateUrl(taxYearEOY, 0)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(ukPensionIncomeCyaUrl(taxYearEOY))
      }
    }
  }
}
