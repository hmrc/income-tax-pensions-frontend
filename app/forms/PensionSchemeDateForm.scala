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

package forms

import filters.InputFilters
import forms.validation.mappings.MappingUtil.trimmedText
import play.api.data.Forms.mapping
import play.api.data.{Form, FormError}

import java.time.LocalDate
import scala.util.Try

object PensionSchemeDateForm extends InputFilters {

  case class PensionSchemeDateModel(day: String, month: String, year: String) {
    def toLocalDate: LocalDate = LocalDate.of(year.toInt, month.toInt, day.toInt)
  }

  val tooLongAgoYear = 1900
  val tooLongAgoDate = LocalDate.of(tooLongAgoYear, 1, 1)

  val day: String = "pensionStartDate-day"
  val month: String = "pensionStartDate-month"
  val year: String = "pensionStartDate-year"

  def pensionSchemeDateForm: Form[PensionSchemeDateModel] = Form[PensionSchemeDateModel](
    mapping(
      day -> trimmedText,
      month -> trimmedText,
      year -> trimmedText
    )(PensionSchemeDateModel.apply)(PensionSchemeDateModel.unapply)
  )


  def areInputsEmpty(date: PensionSchemeDateModel): Seq[FormError] = {
    (date.day.isEmpty, date.month.isEmpty, date.year.isEmpty) match {
      case (true, true, true) => Seq(FormError("emptyAll", "incomeFromPensions.pensionStartDate.error.empty.all"))
      case (true, false, false) => Seq(FormError("emptyDay", "incomeFromPensions.pensionStartDate.error.empty.day"))
      case (true, true, false) => Seq(FormError("emptyDayMonth", "incomeFromPensions.pensionStartDate.error.empty.dayMonth"))
      case (true, false, true) => Seq(FormError("emptyDayYear", "incomeFromPensions.pensionStartDate.error.empty.dayYear"))
      case (false, true, false) => Seq(FormError("emptyMonth", "incomeFromPensions.pensionStartDate.error.empty.month"))
      case (false, true, true) => Seq(FormError("emptyMonthYear", "incomeFromPensions.pensionStartDate.error.empty.monthYear"))
      case (false, false, true) => Seq(FormError("emptyYear", "incomeFromPensions.pensionStartDate.error.empty.year"))
      case (_, _, _) => Seq()
    }
  }

  def dateValidation(date: LocalDate): Seq[FormError] = {
    (date.isAfter(LocalDate.now()), date.isBefore(tooLongAgoDate)) match {
      case (true, _) => Seq(FormError("invalidFormat", "incomeFromPensions.pensionStartDate.error.dateInFuture"))
      case (_, true) => Seq(FormError("invalidFormat", "incomeFromPensions.pensionStartDate.error.tooLongAgo"))
      case _ => Seq()
    }
  }

  def verifyDate(date: PensionSchemeDateModel): Seq[FormError] = {
    val emptyInputsErrors: Seq[FormError] = areInputsEmpty(date)

    if (emptyInputsErrors.isEmpty) {
      val newDate: Either[Throwable, LocalDate] = Try(LocalDate.of(date.year.toInt, date.month.toInt, date.day.toInt)).toEither
      newDate match {
        case Right(date) => dateValidation(date)
        case Left(_) => Seq(FormError("invalidFormat", "incomeFromPensions.pensionStartDate.error.invalidFormat"))
      }
    } else {
      emptyInputsErrors
    }
  }
}
