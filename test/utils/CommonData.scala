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

package utils

import common.TaxYear
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import java.time.LocalDate
import scala.concurrent.ExecutionContext

object CommonData {
  private val dateNow           = LocalDate.now()
  private val taxYearCutoffDate = LocalDate.parse(s"${dateNow.getYear}-04-05")

  private val taxYear: Int = if (dateNow.isAfter(taxYearCutoffDate)) LocalDate.now().getYear + 1 else LocalDate.now().getYear

  val nino: String      = "AA123456A"
  val mtditid: String   = "1234567890"
  val sessionId: String = "eb3158c2-0aff-4ce8-8d1b-f2208ace52fe"

  val currTaxYear: TaxYear = TaxYear(taxYear)

  implicit val headerCarrierWithSession: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(sessionId)))
  implicit val ec: ExecutionContext                    = ExecutionContext.Implicits.global

}
