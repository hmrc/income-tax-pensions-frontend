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

package forms.behaviours

import cats.implicits.catsSyntaxOptionId
import forms.standard.StandardErrorKeys._
import org.scalacheck.Gen
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.data.{Form, FormError}
import utils.generators.Generators

import java.time.LocalDate
import java.time.format.DateTimeFormatter

trait DateBehaviours extends AnyFreeSpec with ScalaCheckPropertyChecks with Generators with Matchers with OptionValues {

  def dateField(form: Form[_], key: String, validData: Gen[LocalDate]): Unit = {
    "bind valid data" in {

      forAll(validData -> "valid date") { date =>
        val data = Map(
          s"$key-day"   -> date.getDayOfMonth.toString,
          s"$key-month" -> date.getMonthValue.toString,
          s"$key-year"  -> date.getYear.toString
        )

        val result = form.bind(data)

        result.value mustEqual date.some
        result.errors mustBe Seq.empty
      }
    }

    "not bind an invalid field" in {
      val validDate = LocalDate.now
      val data1 = Map(
        s"$key-day"   -> "invalid",
        s"$key-month" -> validDate.getMonthValue.toString,
        s"$key-year"  -> validDate.getYear.toString
      )
      val data2 = Map(
        s"$key-day"   -> validDate.getDayOfMonth.toString,
        s"$key-month" -> "invalid",
        s"$key-year"  -> validDate.getYear.toString
      )
      val data3 = Map(
        s"$key-day"   -> validDate.getDayOfMonth.toString,
        s"$key-month" -> validDate.getMonthValue.toString,
        s"$key-year"  -> "invalid"
      )
      val invalidDayResult   = form.bind(data1)
      val invalidMonthResult = form.bind(data2)
      val invalidYearResult  = form.bind(data3)

      invalidDayResult.errors mustBe List(FormError(key, InvalidFormatError))
      invalidMonthResult.errors mustBe List(FormError(key, InvalidFormatError))
      invalidYearResult.errors mustBe List(FormError(key, InvalidFormatError))
    }
  }

  def dateFieldWithMax(form: Form[_], key: String, max: LocalDate, formError: FormError): Unit =
    s"fail to bind a date greater than ${max.format(DateTimeFormatter.ISO_LOCAL_DATE)}" in {

      val generator = datesBetween(max.plusDays(1), max.plusYears(10))

      forAll(generator -> "invalid dates") { date =>
        val data = Map(
          s"$key-day"   -> date.getDayOfMonth.toString,
          s"$key-month" -> date.getMonthValue.toString,
          s"$key-year"  -> date.getYear.toString
        )

        val result = form.bind(data)

        result.errors must contain only formError
      }
    }

  def dateFieldWithMin(form: Form[_], key: String, min: LocalDate, formError: FormError): Unit =
    s"fail to bind a date earlier than ${min.format(DateTimeFormatter.ISO_LOCAL_DATE)}" in {

      val generator = datesBetween(min.minusYears(10), min.minusDays(1))

      forAll(generator -> "invalid dates") { date =>
        val data = Map(
          s"$key-day"   -> date.getDayOfMonth.toString,
          s"$key-month" -> date.getMonthValue.toString,
          s"$key-year"  -> date.getYear.toString
        )

        val result = form.bind(data)

        result.errors must contain only formError
      }
    }

  def mandatoryDateField(form: Form[_], key: String, requiredAllKey: String, errorArgs: Seq[String] = Seq.empty): Unit = {
    "fail to bind an empty date" in {

      val result = form.bind(Map.empty[String, String])

      result.errors must contain only FormError(key, requiredAllKey, errorArgs)
    }

    "return errors when a field is empty" in {
      val validDate = LocalDate.now
      val data1 = Map(
        s"$key-day"   -> "",
        s"$key-month" -> validDate.getMonthValue.toString,
        s"$key-year"  -> validDate.getYear.toString
      )
      val data2 = Map(
        s"$key-day"   -> validDate.getDayOfMonth.toString,
        s"$key-month" -> "",
        s"$key-year"  -> validDate.getYear.toString
      )
      val data3 = Map(
        s"$key-day"   -> validDate.getDayOfMonth.toString,
        s"$key-month" -> validDate.getMonthValue.toString,
        s"$key-year"  -> ""
      )
      val missingDayResult   = form.bind(data1)
      val missingMonthResult = form.bind(data2)
      val missingYearResult  = form.bind(data3)

      missingDayResult.errors mustBe List(FormError(key, MissingDayError))
      missingMonthResult.errors mustBe List(FormError(key, MissingMonthError))
      missingYearResult.errors mustBe List(FormError(key, MissingYearError))
    }

    "return errors when two fields are empty" in {
      val validDate = LocalDate.now
      val data1 = Map(
        s"$key-day"   -> "",
        s"$key-month" -> "",
        s"$key-year"  -> validDate.getYear.toString
      )
      val data2 = Map(
        s"$key-day"   -> validDate.getDayOfMonth.toString,
        s"$key-month" -> "",
        s"$key-year"  -> ""
      )
      val data3 = Map(
        s"$key-day"   -> "",
        s"$key-month" -> validDate.getMonthValue.toString,
        s"$key-year"  -> ""
      )
      val missingDayMonthResult  = form.bind(data1)
      val missingMonthYearResult = form.bind(data2)
      val missingDayYearResult   = form.bind(data3)

      missingDayMonthResult.errors mustBe List(FormError(key, MissingDayMonthError))
      missingMonthYearResult.errors mustBe List(FormError(key, MissingMonthYearError))
      missingDayYearResult.errors mustBe List(FormError(key, MissingDayYearError))
    }
  }
}
