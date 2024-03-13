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

package forms

import filters.InputFilters
import forms.validation.mappings.MappingUtil.trimmedText
import play.api.data.Forms.mapping
import play.api.data.{Form, FormError}
import play.api.i18n.Messages

import java.time.LocalDate
import scala.util.Try

// TODO: This code is needs refactoring. Validations should be on the forms themselves not applied post-request binding
//  (e.g. like in `StatePensionsStartDateController`)
object DateForm extends InputFilters {

  case class DateModel(day: String, month: String, year: String) {
    def toLocalDate: LocalDate = LocalDate.of(year.toInt, month.toInt, day.toInt)
  }

  private val tooLongAgoYear = 1900
  private val tooLongAgoDate = LocalDate.of(tooLongAgoYear, 1, 1)

  def day(id: String): String = s"$id-day"

  def month(id: String): String = s"$id-month"

  def year(id: String): String = s"$id-year"

  def dateForm(id: String): Form[DateModel] =
    Form[DateModel](
      mapping(
        day(id)   -> trimmedText,
        month(id) -> trimmedText,
        year(id)  -> trimmedText
      )(DateModel.apply)(DateModel.unapply)
    )

  private def areInputsEmpty(date: DateModel, messageStart: String)(implicit messages: Messages): Seq[FormError] =
    (date.day.isEmpty, date.month.isEmpty, date.year.isEmpty) match {
      case (true, true, true)   => Seq(FormError("emptyAll", Messages(s"$messageStart.error.empty.all")))
      case (true, false, false) => Seq(FormError("emptyDay", Messages(s"$messageStart.error.empty.day")))
      case (true, true, false)  => Seq(FormError("emptyDayMonth", Messages(s"$messageStart.error.empty.dayMonth")))
      case (true, false, true)  => Seq(FormError("emptyDayYear", Messages(s"$messageStart.error.empty.dayYear")))
      case (false, true, false) => Seq(FormError("emptyMonth", Messages(s"$messageStart.error.empty.month")))
      case (false, true, true)  => Seq(FormError("emptyMonthYear", Messages(s"$messageStart.error.empty.monthYear")))
      case (false, false, true) => Seq(FormError("emptyYear", Messages(s"$messageStart.error.empty.year")))
      case (_, _, _)            => Seq()
    }

  private def dateValidation(date: LocalDate, messageStart: String)(implicit messages: Messages): Seq[FormError] =
    (date.isAfter(LocalDate.now().minusDays(1)), date.isBefore(tooLongAgoDate)) match {
      case (true, _) => Seq(FormError("invalidFormat", Messages(s"$messageStart.error.dateInFuture")))
      case (_, true) => Seq(FormError("invalidFormat", Messages(s"$messageStart.error.tooLongAgo")))
      case _         => Seq()
    }

  def verifyDate(date: DateModel, messageStart: String)(implicit messages: Messages): Seq[FormError] = {
    val emptyInputsErrors: Seq[FormError] = areInputsEmpty(date, messageStart)

    if (emptyInputsErrors.isEmpty) {
      val newDate: Either[Throwable, LocalDate] = Try(LocalDate.of(date.year.toInt, date.month.toInt, date.day.toInt)).toEither
      newDate match {
        case Right(date) => dateValidation(date, messageStart)
        case Left(_)     => Seq(FormError("invalidFormat", Messages(s"$messageStart.error.invalidFormat")))
      }
    } else {
      emptyInputsErrors
    }
  }
}
