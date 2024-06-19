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

package forms.mappings

import play.api.data.FormError
import play.api.data.format.Formatter

import java.time.LocalDate
import scala.util.{Failure, Success, Try}

private[mappings] class LocalDateFormatter(missingAllError: String,
                                           missingDay: String,
                                           missingMonth: String,
                                           missingYear: String,
                                           missingDayMonth: String,
                                           missingMonthYear: String,
                                           missingDayYear: String,
                                           invalidFormatError: String,
                                           earliestDateAndError: Option[(LocalDate, String)],
                                           latestDateAndError: Option[(LocalDate, String)],
                                           args: Seq[String] = Seq.empty)
    extends Formatter[LocalDate]
    with Formatters {

  private val fieldKeys: List[String] = List("day", "month", "year")

  override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], LocalDate] = {

    val fields = fieldKeys.map(field => field -> data.get(s"$key-$field").filter(_.nonEmpty)).toMap

    fields.count(_._2.isDefined) match {
      case 0 => Left(List(FormError(key, missingAllError, args)))
      case 1 => Left(getMultipleEmptyFieldsError(key, data))
      case _ =>
        formatDate(key, data).left.map {
          _.map(_.copy(key = key, args = args))
        }
    }
  }

  private def int(fieldSpecificError: String): Formatter[Int] = intFormatter(
    requiredKey = fieldSpecificError,
    wholeNumberKey = invalidFormatError,
    nonNumericKey = invalidFormatError,
    args
  )

  private def getMultipleEmptyFieldsError(key: String, data: Map[String, String]): Seq[FormError] = {
    val day: Either[Seq[FormError], Int] = int(missingDay).bind(s"$key-day", data)
    val month                            = int(missingMonth).bind(s"$key-month", data)
    val year                             = int(missingYear).bind(s"$key-year", data)
    (day.isRight, month.isRight, year.isRight) match {
      case (false, false, true) => Seq(FormError(key, missingDayMonth))
      case (false, true, false) => Seq(FormError(key, missingDayYear))
      case (true, false, false) => Seq(FormError(key, missingMonthYear))
      case (_, _, _)            => Seq()
    }
  }

  private def formatDate(key: String, data: Map[String, String]): Either[Seq[FormError], LocalDate] =
    for {
      day            <- int(missingDay).bind(s"$key-day", data)
      month          <- int(missingMonth).bind(s"$key-month", data)
      year           <- int(missingYear).bind(s"$key-year", data)
      localDate      <- toDate(key, day, month, year)
      validDate      <- dateIsNotTooEarly(localDate)
      finalValidDate <- dateIsNotTooLate(validDate)
    } yield finalValidDate

  private def toDate(key: String, day: Int, month: Int, year: Int): Either[Seq[FormError], LocalDate] =
    Try(LocalDate.of(year, month, day)) match {
      case Success(date) =>
        Right(date)
      case Failure(_) =>
        Left(Seq(FormError(key, invalidFormatError, args)))
    }

  private def dateIsNotTooEarly(date: LocalDate): Either[Seq[FormError], LocalDate] =
    earliestDateAndError match {
      case Some(earliestDate) =>
        if (date.isAfter(earliestDate._1)) Right(date) else Left(Seq(FormError("", earliestDate._2, args)))
      case _ => Right(date)
    }

  private def dateIsNotTooLate(date: LocalDate): Either[Seq[FormError], LocalDate] =
    latestDateAndError match {
      case Some(latestDate) =>
        if (date.isBefore(latestDate._1)) Right(date) else Left(Seq(FormError("", latestDate._2, args)))
      case _ => Right(date)
    }

  override def unbind(key: String, value: LocalDate): Map[String, String] =
    Map(
      s"$key-day"   -> value.getDayOfMonth.toString,
      s"$key-month" -> value.getMonthValue.toString,
      s"$key-year"  -> value.getYear.toString
    )
}
