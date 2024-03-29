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
import builders.PensionsCYAModelBuilder.aPensionsCYAModel
import builders.PensionsUserDataBuilder.{aPensionsUserData, pensionsUserDataWithIncomeFromPensions}
import builders.StateBenefitViewModelBuilder.{aStatePensionViewModel, anStateBenefitViewModelOne}
import builders.UserBuilder.aUser
import models.pension.statebenefits.IncomeFromPensionsViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.IncomeFromPensionsPages.{statePension, statePensionCyaUrl, statePensionLumpSumUrl, statePensionStartDateUrl}
import utils.PageUrls.fullUrl
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

import java.time.LocalDate

class StatePensionStartDateControllerISpec extends IntegrationTest with ViewHelpers with BeforeAndAfterEach with PensionsDatabaseHelper {

  def startDateForm(day: String, month: String, year: String): Map[String, String] = Map(
    dayInputName   -> day,
    monthInputName -> month,
    yearInputName  -> year
  )

  override val userScenarios: Seq[UserScenario[_, _]] = Nil

  private val dayInputName   = "stateBenefitStartDate-day"
  private val monthInputName = "stateBenefitStartDate-month"
  private val yearInputName  = "stateBenefitStartDate-year"
  private val validDay       = "27"
  private val validMonth     = "10"
  private val validYear      = "2021"

  object Selectors {
    val dayInputSelector   = "#day"
    val monthInputSelector = "#month"
    val yearInputSelector  = "#year"
    val dayInputHref       = "#day"
    val monthInputHref     = "#month"
    val yearInputHref      = "#year"

    def labelSelector(index: Int): String = s"#stateBenefitStartDate > div:nth-child($index) > div > label"

  }

  val dateInFutureErrorText   = "The pension start date must be in the past"
  val realDateErrorText       = "The day, month and year must be valid"
  val tooLongAgoErrorText     = "The pension start date must be after 1 January 1900"
  val emptyAllErrorText       = "Enter the pension start date"
  val emptyDayErrorText       = "The pension start date must include a day"
  val emptyDayMonthErrorText  = "The pension start date must include a day and month"
  val emptyDayYearErrorText   = "The pension start date must include a day and year"
  val emptyMonthErrorText     = "The pension start date must include a month"
  val emptyMonthYearErrorText = "The pension start date must include a month and year"
  val emptyYearErrorText      = "The pension start date must include a year"
  val expectedErrorTitle      = "Error: When did you start getting State Pension payments?"

  ".show" should {
    "show page when EOY" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        insertCyaData(aPensionsUserData)
        urlGet(
          fullUrl(statePensionStartDateUrl(taxYearEOY)),
          !aUser.isAgent,
          follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
        )
      }
      result.status shouldBe OK
    }

    "redirect to the first page in journey if the previous question has not been answered" in {
      val data = aPensionsUserData.copy(
        pensions = aPensionsCYAModel.copy(
          incomeFromPensions = anIncomeFromPensionsViewModel.copy(
            statePension = Some(anStateBenefitViewModelOne.copy(amountPaidQuestion = None, amount = None))
          )))

      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        insertCyaData(data)
        urlGet(
          fullUrl(statePensionStartDateUrl(taxYearEOY)),
          !aUser.isAgent,
          follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
        )
      }
      result.status shouldBe SEE_OTHER
      result.header("location").contains(statePension(taxYearEOY))
    }
  }

  ".submit" should {
    // TODO: Uncomment me during SASS-7742
    "persist amount and redirect to StatePensionLumpSum page" ignore {
      val formData = startDateForm(validDay, validMonth, validYear)
      val viewModel = IncomeFromPensionsViewModel(statePension =
        Some(aStatePensionViewModel.copy(startDate = Some(LocalDate.parse("2019-11-14")), startDateQuestion = Some(true))))

      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        insertCyaData(pensionsUserDataWithIncomeFromPensions(viewModel))
        urlPost(
          fullUrl(statePensionStartDateUrl(taxYearEOY)),
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)),
          follow = false,
          body = formData
        )
      }
      result.status shouldBe SEE_OTHER
      result.headers("location").head shouldBe statePensionLumpSumUrl(taxYearEOY)
    }

    "persist amount and redirect to CYA page when model is now completed" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        val formData = startDateForm(validDay, validMonth, validYear)

        insertCyaData(aPensionsUserData)
        urlPost(
          fullUrl(statePensionStartDateUrl(taxYearEOY)),
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)),
          follow = false,
          body = formData
        )
      }
      result.status shouldBe SEE_OTHER
      result.headers("location").head shouldBe statePensionCyaUrl(taxYearEOY)
    }

    "return BAD_REQUEST" when {

      "the day field is empty" which {
        lazy val form = startDateForm("", validMonth, validYear)

        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual()
          insertCyaData(aPensionsUserData)
          urlPost(
            fullUrl(statePensionStartDateUrl(taxYearEOY)),
            body = form,
            follow = false,
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

      "the day and month fields are empty" which {
        lazy val form = startDateForm("", "", validYear)

        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual()
          insertCyaData(aPensionsUserData)
          urlPost(
            fullUrl(statePensionStartDateUrl(taxYearEOY)),
            body = form,
            follow = false,
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
          authoriseAgentOrIndividual()
          insertCyaData(aPensionsUserData)
          urlPost(
            fullUrl(statePensionStartDateUrl(taxYearEOY)),
            body = form,
            follow = false,
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

      "the month field is empty" which {
        lazy val form = startDateForm(validDay, "", validYear)

        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual()
          insertCyaData(aPensionsUserData)
          urlPost(
            fullUrl(statePensionStartDateUrl(taxYearEOY)),
            body = form,
            follow = false,
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

      "the month and year fields are empty" which {
        lazy val form = startDateForm(validDay, "", "")

        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual()
          insertCyaData(aPensionsUserData)
          urlPost(
            fullUrl(statePensionStartDateUrl(taxYearEOY)),
            body = form,
            follow = false,
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

      "the year field is empty" which {
        lazy val form = startDateForm(validDay, validMonth, "")

        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual()
          insertCyaData(aPensionsUserData)
          urlPost(
            fullUrl(statePensionStartDateUrl(taxYearEOY)),
            body = form,
            follow = false,
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

      "the date submitted is in the future" which {
        val futureYear: Int = LocalDate.now().getYear + 1
        lazy val form       = startDateForm(validDay, validMonth, s"$futureYear")

        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual()
          insertCyaData(aPensionsUserData)
          urlPost(
            fullUrl(statePensionStartDateUrl(taxYearEOY)),
            body = form,
            follow = false,
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
          authoriseAgentOrIndividual()
          insertCyaData(aPensionsUserData)
          urlPost(
            fullUrl(statePensionStartDateUrl(taxYearEOY)),
            body = form,
            follow = false,
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
          authoriseAgentOrIndividual()
          insertCyaData(aPensionsUserData)
          urlPost(
            fullUrl(statePensionStartDateUrl(taxYearEOY)),
            body = form,
            follow = false,
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
}
