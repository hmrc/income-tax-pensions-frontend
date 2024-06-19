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

import forms.behaviours.DateBehaviours
import forms.standard.StandardErrorKeys._
import play.api.data.{Form, FormError}

import java.time.LocalDate

class LocalDateFormProviderSpec extends DateBehaviours {

  private val fieldKey       = "constructionStartDate"
  private val earliestDate   = StandardEarlyDateError._1
  private val earlyDateError = StandardEarlyDateError._2
  private val latestDate     = StandardDateInFutureError._1
  private val lateDateError  = StandardDateInFutureError._2
  private val validDates     = datesBetween(earliestDate, latestDate)
  private val formProvider   = new LocalDateFormProvider()

  "LocalDate Form should" - {
    val form: Form[LocalDate] = formProvider("constructionStartDate", "", Some(StandardEarlyDateError), Some(StandardDateInFutureError))

    dateField(form, fieldKey, validDates)

    mandatoryDateField(form, fieldKey, MissingAllError)

    dateFieldWithMin(form, fieldKey, earliestDate, FormError(fieldKey, earlyDateError))

    dateFieldWithMax(form, fieldKey, latestDate, FormError(fieldKey, lateDateError))
  }
}
