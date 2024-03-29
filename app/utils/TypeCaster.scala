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

package utils

import java.time.{Instant, LocalDate, Month}
import java.util.UUID

object TypeCaster {

  trait Converter[T] { self =>
    def convert(v: String): T
  }

  object Converter {
    implicit val stringLoader: Converter[String]         = (v: String) => v
    implicit val booleanLoader: Converter[Boolean]       = (v: String) => v.toBoolean
    implicit val bigDecimalLoader: Converter[BigDecimal] = (v: String) => BigDecimal(v)
    implicit val monthLoader: Converter[Month]           = (v: String) => Month.valueOf(v)
    implicit val uuidLoader: Converter[UUID]             = (v: String) => UUID.fromString(v)
    implicit val instantLoader: Converter[Instant]       = (v: String) => Instant.parse(v)
    implicit val localDateLoader: Converter[LocalDate]   = (v: String) => LocalDate.parse(v)
  }
}
