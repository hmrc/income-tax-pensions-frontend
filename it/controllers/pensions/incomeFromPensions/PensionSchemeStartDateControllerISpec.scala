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
import builders.UserBuilder.{aUser, aUserRequest}
import models.pension.statebenefits.IncomeFromPensionsViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.IncomeFromPensionsPages.{pensionStartDateUrl, ukPensionSchemeSummaryListUrl}
import utils.PageUrls.{fullUrl, overseasPensionsSummaryUrl, pensionSummaryUrl}
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
    dayInputName -> day,
    monthInputName -> month,
    yearInputName -> year
  )

  override val userScenarios: Seq[UserScenario[_, _]] = Nil

  object Selectors {
    val dayInputSelector = "#day"
    val monthInputSelector = "#month"
    val yearInputSelector = "#year"
    val dayInputHref = "#day"
    val monthInputHref = "#month"
    val yearInputHref = "#year"

    def labelSelector(index: Int): String = s"#pensionStartDate > div:nth-child($index) > div > label"
  }

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
  val expectedErrorTitle = "Error: When did you start getting payments from this scheme?"


  ".show" should {  //scalastyle:off magic.number

    "return Ok response with PensionSchemeStartDate page" when {
      "no previous scheme data" in {
        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual(aUser.isAgent)
          val viewModel = anIncomeFromPensionsViewModel.copy(
            uKPensionIncomes = Seq(anUkPensionIncomeViewModelOne.copy(startDate = None)))
          insertCyaData(pensionsUserDataWithIncomeFromPensions(viewModel), aUserRequest)
          urlGet(fullUrl(pensionStartDateUrl(taxYearEOY, Some(0))), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        result.status shouldBe OK
        result.body.contains("/pension-income/pension-start-date?index=0")
      }

      "there's previous scheme data" in {
        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual(aUser.isAgent)
          val viewModel: IncomeFromPensionsViewModel = anIncomeFromPensionsViewModel.copy(
            uKPensionIncomes = Seq(anUkPensionIncomeViewModelOne.copy(startDate = Some(s"$validYear-$validMonth-$validDay"))))
          insertCyaData(pensionsUserDataWithIncomeFromPensions(viewModel), aUserRequest)
          urlGet(fullUrl(pensionStartDateUrl(taxYearEOY, Some(0))), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        result.status shouldBe OK
        result.body.contains("/pension-income/pension-start-date?index=0")
      }
    }

    "redirect to Uk Pension Incomes Summary page" when {
      "index is out of bounds" in {
        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual(isAgent = false)
          val viewModel: IncomeFromPensionsViewModel = anIncomeFromPensionsViewModel.copy(
            uKPensionIncomes = Seq(anUkPensionIncomeViewModelOne, anUkPensionIncomeViewModelTwo))
          insertCyaData(pensionsUserDataWithIncomeFromPensions(viewModel), aUserRequest)
          urlGet(fullUrl(pensionStartDateUrl(taxYearEOY, Some(2))), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(ukPensionSchemeSummaryListUrl(taxYearEOY))
      }
      
      "no index is given" in {
        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual(isAgent = false)
          insertCyaData(aPensionsUserData, aUserRequest)
          urlGet(fullUrl(pensionStartDateUrl(taxYearEOY, None)), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(ukPensionSchemeSummaryListUrl(taxYearEOY))
      }
    }

    "redirect to Overseas Pension Summary page when there is no session data" in {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(pensionStartDateUrl(taxYearEOY, Some(0))), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      result.status shouldBe SEE_OTHER
      result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
    }
  }

  "submit" should {

    "redirect to UkPensionIncomesSummary page and update scheme data when valid form is submitted" in {
          // todo redirect to Check Scheme Details page when created
          lazy val form = startDateForm(validDay, validMonth, validYear)

          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(isAgent = false)
            val viewModel: IncomeFromPensionsViewModel = anIncomeFromPensionsViewModel.copy(
              uKPensionIncomes = Seq(anUkPensionIncomeViewModelOne, anUkPensionIncomeViewModelTwo))
            insertCyaData(pensionsUserDataWithIncomeFromPensions(viewModel), aUserRequest)
            urlPost(fullUrl(pensionStartDateUrl(taxYearEOY, Some(1))), body = form, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

            result.status shouldBe SEE_OTHER
            result.header("location") shouldBe Some(ukPensionSchemeSummaryListUrl(taxYearEOY))

            lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
            cyaModel.pensions.incomeFromPensions.uKPensionIncomes(1).startDate shouldBe Some(s"$validYear-$validMonth-$validDay")
        }

    "redirect to the Uk Pension Incomes Summary page if index is out of bounds" in {
          lazy val form = startDateForm(validDay, validMonth, validYear)
          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(isAgent = false)
            val viewModel: IncomeFromPensionsViewModel = anIncomeFromPensionsViewModel.copy(
              uKPensionIncomes = Seq(anUkPensionIncomeViewModelOne, anUkPensionIncomeViewModelTwo))
            insertCyaData(pensionsUserDataWithIncomeFromPensions(viewModel), aUserRequest)
            urlPost(fullUrl(pensionStartDateUrl(taxYearEOY, Some(-2))), body = form, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

            result.status shouldBe SEE_OTHER
            result.header("location") shouldBe Some(ukPensionSchemeSummaryListUrl(taxYearEOY))
        }

    "all fields are empty" which {
      lazy val form = startDateForm("", "", "")

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(aPensionsUserData, aUserRequest)
        urlPost(fullUrl(pensionStartDateUrl(taxYearEOY, Some(0))), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has the correct status" in {
        result.status shouldBe BAD_REQUEST
      }

      implicit def document: () => Document = () => Jsoup.parse(result.body)

      inputFieldValueCheck(dayInputName, Selectors.dayInputSelector, "")
      inputFieldValueCheck(monthInputName, Selectors.monthInputSelector, "")
      inputFieldValueCheck(yearInputName, Selectors.yearInputSelector, "")
      errorSummaryCheck(emptyAllErrorText, Selectors.dayInputHref)
      errorAboveElementCheck(emptyAllErrorText)
    }

    "the day field is empty" which {
      lazy val form = startDateForm("", validMonth, validYear)

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(aPensionsUserData, aUserRequest)
        urlPost(fullUrl(pensionStartDateUrl(taxYearEOY, Some(0))), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has the correct status" in {
        result.status shouldBe BAD_REQUEST
      }

      implicit def document: () => Document = () => Jsoup.parse(result.body)

      inputFieldValueCheck(dayInputName, Selectors.dayInputSelector, "")
      inputFieldValueCheck(monthInputName, Selectors.monthInputSelector, validMonth)
      inputFieldValueCheck(yearInputName, Selectors.yearInputSelector, validYear)
      errorSummaryCheck(emptyDayErrorText, Selectors.dayInputHref)
      errorAboveElementCheck(emptyDayErrorText)
    }

    "the month field is empty" which {
      lazy val form = startDateForm(validDay, "", validYear)

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(aPensionsUserData, aUserRequest)
        urlPost(fullUrl(pensionStartDateUrl(taxYearEOY, Some(0))), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }
      "has the correct status" in {
        result.status shouldBe BAD_REQUEST
      }

      implicit def document: () => Document = () => Jsoup.parse(result.body)

      titleCheck(expectedErrorTitle)
      inputFieldValueCheck(dayInputName, Selectors.dayInputSelector, validDay)
      inputFieldValueCheck(monthInputName, Selectors.monthInputSelector, "")
      inputFieldValueCheck(yearInputName, Selectors.yearInputSelector, validYear)
      errorSummaryCheck(emptyMonthErrorText, Selectors.monthInputHref)
      errorAboveElementCheck(emptyMonthErrorText)
    }

    "the year field is empty" which {
      lazy val form = startDateForm(validDay, validMonth, "")

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(aPensionsUserData, aUserRequest)
        urlPost(fullUrl(pensionStartDateUrl(taxYearEOY, Some(0))), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has the correct status" in {
        result.status shouldBe BAD_REQUEST
      }

      implicit def document: () => Document = () => Jsoup.parse(result.body)

      titleCheck(expectedErrorTitle)

      inputFieldValueCheck(dayInputName, Selectors.dayInputSelector, validDay)
      inputFieldValueCheck(monthInputName, Selectors.monthInputSelector, validMonth)
      inputFieldValueCheck(yearInputName, Selectors.yearInputSelector, "")
      errorSummaryCheck(emptyYearErrorText, Selectors.yearInputHref)
      errorAboveElementCheck(emptyYearErrorText)
    }

    "the day and month fields are empty" which {
      lazy val form = startDateForm("", "", validYear)

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(aPensionsUserData, aUserRequest)
        urlPost(fullUrl(pensionStartDateUrl(taxYearEOY, Some(0))), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has the correct status" in {
        result.status shouldBe BAD_REQUEST
      }

      implicit def document: () => Document = () => Jsoup.parse(result.body)

      titleCheck(expectedErrorTitle)
      inputFieldValueCheck(dayInputName, Selectors.dayInputSelector, "")
      inputFieldValueCheck(monthInputName, Selectors.monthInputSelector, "")
      inputFieldValueCheck(yearInputName, Selectors.yearInputSelector, validYear)
      errorSummaryCheck(emptyDayMonthErrorText, Selectors.dayInputHref)
      errorAboveElementCheck(emptyDayMonthErrorText)
    }

    "the day and year fields are empty" which {
      lazy val form = startDateForm("", validMonth, "")

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(aPensionsUserData, aUserRequest)
        urlPost(fullUrl(pensionStartDateUrl(taxYearEOY, Some(0))), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has the correct status" in {
        result.status shouldBe BAD_REQUEST
      }

      implicit def document: () => Document = () => Jsoup.parse(result.body)

      titleCheck(expectedErrorTitle)
      inputFieldValueCheck(dayInputName, Selectors.dayInputSelector, "")
      inputFieldValueCheck(monthInputName, Selectors.monthInputSelector, validMonth)
      inputFieldValueCheck(yearInputName, Selectors.yearInputSelector, "")
      errorSummaryCheck(emptyDayYearErrorText, Selectors.dayInputHref)
      errorAboveElementCheck(emptyDayYearErrorText)
    }

    "the month and year fields are empty" which {
      lazy val form = startDateForm(validDay, "", "")

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(aPensionsUserData, aUserRequest)
        urlPost(fullUrl(pensionStartDateUrl(taxYearEOY, Some(0))), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has the correct status" in {
        result.status shouldBe BAD_REQUEST
      }

      implicit def document: () => Document = () => Jsoup.parse(result.body)

      titleCheck(expectedErrorTitle)
      inputFieldValueCheck(dayInputName, Selectors.dayInputSelector, validDay)
      inputFieldValueCheck(monthInputName, Selectors.monthInputSelector, "")
      inputFieldValueCheck(yearInputName, Selectors.yearInputSelector, "")
      errorSummaryCheck(emptyMonthYearErrorText, Selectors.monthInputHref)
      errorAboveElementCheck(emptyMonthYearErrorText)
    }

    "the date submitted is in the future" which {
      val futureYear: Int = LocalDate.now().getYear + 1
      lazy val form = startDateForm(validDay, validMonth, s"$futureYear")

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(aPensionsUserData, aUserRequest)
        urlPost(fullUrl(pensionStartDateUrl(taxYearEOY, Some(0))), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has the correct status" in {
        result.status shouldBe BAD_REQUEST
      }

      implicit def document: () => Document = () => Jsoup.parse(result.body)

      titleCheck(expectedErrorTitle)
      inputFieldValueCheck(dayInputName, Selectors.dayInputSelector, validDay)
      inputFieldValueCheck(monthInputName, Selectors.monthInputSelector, validMonth)
      inputFieldValueCheck(yearInputName, Selectors.yearInputSelector, s"$futureYear")
      errorSummaryCheck(dateInFutureErrorText, Selectors.dayInputHref)
      errorAboveElementCheck(dateInFutureErrorText)
    }

    "the date submitted is invalid and not a real date" which {
      lazy val form = startDateForm(validDay, "13", validYear)

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(aPensionsUserData, aUserRequest)
        urlPost(fullUrl(pensionStartDateUrl(taxYearEOY, Some(0))), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has the correct status" in {
        result.status shouldBe BAD_REQUEST
      }

      implicit def document: () => Document = () => Jsoup.parse(result.body)

      titleCheck(expectedErrorTitle)
      inputFieldValueCheck(dayInputName, Selectors.dayInputSelector, validDay)
      inputFieldValueCheck(monthInputName, Selectors.monthInputSelector, "13")
      inputFieldValueCheck(yearInputName, Selectors.yearInputSelector, validYear)
      errorSummaryCheck(realDateErrorText, Selectors.dayInputHref)
      errorAboveElementCheck(realDateErrorText)
    }

    "the date submitted is too long ago before 1 Jan 1900" which {
      lazy val form = startDateForm("01", "01", "1898")

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(aPensionsUserData, aUserRequest)
        urlPost(fullUrl(pensionStartDateUrl(taxYearEOY, Some(0))), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has the correct status" in {
        result.status shouldBe BAD_REQUEST
      }

      implicit def document: () => Document = () => Jsoup.parse(result.body)

      titleCheck(expectedErrorTitle)
      inputFieldValueCheck(dayInputName, Selectors.dayInputSelector, "01")
      inputFieldValueCheck(monthInputName, Selectors.monthInputSelector, "01")
      inputFieldValueCheck(yearInputName, Selectors.yearInputSelector, "1898")
      errorSummaryCheck(tooLongAgoErrorText, Selectors.dayInputHref)
      errorAboveElementCheck(tooLongAgoErrorText)
    }

  }
}
