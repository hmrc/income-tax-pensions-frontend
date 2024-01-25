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

package models.logging

import scala.util.matching.Regex

/**
 * It will programatically remove PII information from logs. Keep it up-to-date depending what PII we may log.
 */
object PIIMaskingConverter {
  private val ninoPattern: Regex = """(?<=nino/)\w+""".r

  private val allPatterns = List(ninoPattern)

  def mask(raw: String): String = {
    allPatterns.foldLeft(raw) { (maskedString, pattern) =>
      pattern.replaceAllIn(maskedString, _ => "REDACTED_FROM_LOGS")
    }
  }
}
