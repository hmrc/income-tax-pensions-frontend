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

import java.time.LocalDate

object StandardErrorKeys {

  val MissingAllError       = "common.error.localDate.empty.all"
  val MissingDayError       = "common.error.localDate.empty.day"
  val MissingMonthError     = "common.error.localDate.empty.month"
  val MissingYearError      = "common.error.localDate.empty.year"
  val MissingDayMonthError  = "common.error.localDate.empty.dayMonth"
  val MissingDayYearError   = "common.error.localDate.empty.dayYear"
  val MissingMonthYearError = "common.error.localDate.empty.monthYear"

  val InvalidFormatError = "common.error.localDate.invalidFormat"
  val DateInFutureError  = "common.error.localDate.dateInFuture"
  val TooLongAgoError    = "common.error.localDate.tooLongAgo"

  val EarliestDate = LocalDate.of(1900, 1, 1)
  val PresentDate  = LocalDate.now()

  val StandardEarlyDateError    = (EarliestDate, TooLongAgoError)
  val StandardDateInFutureError = (PresentDate, DateInFutureError)

  def prefixError(prefix: String)(error: String) = if (prefix.isBlank) error else error.replace("common", prefix)

}
