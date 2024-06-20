/*
 * Copyright 2024 HM Revenue & Customs
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

package forms.standard

import cats.implicits.catsSyntaxOptionId
import forms.standard.StandardErrorKeys._
import org.scalatest.OptionValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.data.{Form, FormError}
import utils.generators.Generators

import java.time.LocalDate
import java.time.format.DateTimeFormatter

class LocalDateFormProviderSpec extends AnyWordSpecLike with ScalaCheckPropertyChecks with Generators with Matchers with OptionValues {

  private val fieldKey       = "pageStartDate"
  private val earliestDate   = StandardEarlyDateError._1
  private val earlyDateError = StandardEarlyDateError._2
  private val latestDate     = StandardDateInFutureError._1
  private val lateDateError  = StandardDateInFutureError._2
  private val validDates     = datesBetween(earliestDate, latestDate)
  private val formProvider   = new LocalDateFormProvider()

  "LocalDateFormProvider" should {
    val form: Form[LocalDate] = formProvider("pageStartDate", "", Some(StandardEarlyDateError), Some(StandardDateInFutureError))

    "bind valid data" in {

      forAll(validDates -> "valid date") { date =>
        val data = Map(
          s"$fieldKey-day"   -> date.getDayOfMonth.toString,
          s"$fieldKey-month" -> date.getMonthValue.toString,
          s"$fieldKey-year"  -> date.getYear.toString
        )

        val result = form.bind(data)

        result.value mustEqual date.some
        result.errors mustBe Seq.empty
      }
    }

    "not bind an invalid field" in {
      val validDate = LocalDate.now
      val data1 = Map(
        s"$fieldKey-day"   -> "invalid",
        s"$fieldKey-month" -> validDate.getMonthValue.toString,
        s"$fieldKey-year"  -> validDate.getYear.toString
      )
      val data2 = Map(
        s"$fieldKey-day"   -> validDate.getDayOfMonth.toString,
        s"$fieldKey-month" -> "invalid",
        s"$fieldKey-year"  -> validDate.getYear.toString
      )
      val data3 = Map(
        s"$fieldKey-day"   -> validDate.getDayOfMonth.toString,
        s"$fieldKey-month" -> validDate.getMonthValue.toString,
        s"$fieldKey-year"  -> "invalid"
      )
      val invalidDayResult   = form.bind(data1)
      val invalidMonthResult = form.bind(data2)
      val invalidYearResult  = form.bind(data3)

      invalidDayResult.errors mustBe List(FormError(fieldKey, InvalidFormatError))
      invalidMonthResult.errors mustBe List(FormError(fieldKey, InvalidFormatError))
      invalidYearResult.errors mustBe List(FormError(fieldKey, InvalidFormatError))
    }

    "fail to bind an empty date" in {

      val result = form.bind(Map.empty[String, String])

      result.errors must contain only FormError(fieldKey, MissingAllError)
    }

    "return errors when a field is empty" in {
      val validDate = LocalDate.now
      val data1 = Map(
        s"$fieldKey-day"   -> "",
        s"$fieldKey-month" -> validDate.getMonthValue.toString,
        s"$fieldKey-year"  -> validDate.getYear.toString
      )
      val data2 = Map(
        s"$fieldKey-day"   -> validDate.getDayOfMonth.toString,
        s"$fieldKey-month" -> "",
        s"$fieldKey-year"  -> validDate.getYear.toString
      )
      val data3 = Map(
        s"$fieldKey-day"   -> validDate.getDayOfMonth.toString,
        s"$fieldKey-month" -> validDate.getMonthValue.toString,
        s"$fieldKey-year"  -> ""
      )
      val missingDayResult   = form.bind(data1)
      val missingMonthResult = form.bind(data2)
      val missingYearResult  = form.bind(data3)

      missingDayResult.errors mustBe List(FormError(fieldKey, MissingDayError))
      missingMonthResult.errors mustBe List(FormError(fieldKey, MissingMonthError))
      missingYearResult.errors mustBe List(FormError(fieldKey, MissingYearError))
    }

    "return errors when two fields are empty" in {
      val validDate = LocalDate.now
      val data1 = Map(
        s"$fieldKey-day"   -> "",
        s"$fieldKey-month" -> "",
        s"$fieldKey-year"  -> validDate.getYear.toString
      )
      val data2 = Map(
        s"$fieldKey-day"   -> validDate.getDayOfMonth.toString,
        s"$fieldKey-month" -> "",
        s"$fieldKey-year"  -> ""
      )
      val data3 = Map(
        s"$fieldKey-day"   -> "",
        s"$fieldKey-month" -> validDate.getMonthValue.toString,
        s"$fieldKey-year"  -> ""
      )
      val missingDayMonthResult  = form.bind(data1)
      val missingMonthYearResult = form.bind(data2)
      val missingDayYearResult   = form.bind(data3)

      missingDayMonthResult.errors mustBe List(FormError(fieldKey, MissingDayMonthError))
      missingMonthYearResult.errors mustBe List(FormError(fieldKey, MissingMonthYearError))
      missingDayYearResult.errors mustBe List(FormError(fieldKey, MissingDayYearError))
    }

    s"fail to bind a date greater than ${latestDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}" in {

      val generator = datesBetween(latestDate.plusDays(1), latestDate.plusYears(10))

      forAll(generator -> "invalid dates") { date =>
        val data = Map(
          s"$fieldKey-day"   -> date.getDayOfMonth.toString,
          s"$fieldKey-month" -> date.getMonthValue.toString,
          s"$fieldKey-year"  -> date.getYear.toString
        )

        val result = form.bind(data)

        result.errors mustBe List(FormError(fieldKey, lateDateError))
      }
    }

    s"fail to bind a date earlier than ${earliestDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}" in {

      val generator = datesBetween(earliestDate.minusYears(10), earliestDate.minusDays(1))

      forAll(generator -> "invalid dates") { date =>
        val data = Map(
          s"$fieldKey-day"   -> date.getDayOfMonth.toString,
          s"$fieldKey-month" -> date.getMonthValue.toString,
          s"$fieldKey-year"  -> date.getYear.toString
        )

        val result = form.bind(data)

        result.errors mustBe List(FormError(fieldKey, earlyDateError))
      }
    }
  }
}
