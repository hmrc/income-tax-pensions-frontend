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

package forms.validation.mappings

import play.api.data.FieldMapping
import play.api.data.Forms.of

import java.time.LocalDate

object Mappings {

  def localDate(missingAllError: String,
                missingDay: String,
                missingMonth: String,
                missingYear: String,
                missingDayMonth: String,
                missingMonthYear: String,
                missingDayYear: String,
                invalidFormatError: String,
                earliestDateAndError: Option[(LocalDate, String)] = None,
                latestDateAndError: Option[(LocalDate, String)] = None,
                args: Seq[String] = Seq.empty): FieldMapping[LocalDate] =
    of(
      new LocalDateFormatter(
        missingAllError,
        missingDay,
        missingMonth,
        missingYear,
        missingDayMonth,
        missingMonthYear,
        missingDayYear,
        invalidFormatError,
        earliestDateAndError,
        latestDateAndError,
        args
      ))
}
