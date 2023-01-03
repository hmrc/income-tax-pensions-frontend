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

import common.SessionValues

import java.time.{LocalDate, LocalDateTime, ZoneId}
import play.api.mvc.Request

trait TaxYearHelper extends SessionHelper {

  private val dateNow: LocalDate = LocalDate.now()
  private val taxYearCutoffDate: LocalDate = LocalDate.parse(s"${dateNow.getYear}-04-05")
  private val londonZoneId = ZoneId.of("Europe/London")
  private val taxYearStartDay = 6
  private val taxYearStartMonth = 4
  private val taxYearStartHour = 0
  private val taxYearStartMinute = 0


  val taxYear: Int = if (dateNow.isAfter(taxYearCutoffDate)) LocalDate.now().getYear + 1 else LocalDate.now().getYear

  def inYear(taxYear: Int, now: LocalDateTime = LocalDateTime.now): Boolean = {
    val endOfYearCutOffDate = LocalDateTime.of(taxYear, taxYearStartMonth, taxYearStartDay, taxYearStartHour, taxYearStartMinute)
    now.atZone(londonZoneId).isBefore(endOfYearCutOffDate.atZone(londonZoneId))
  }
  
  val taxYearEOY: Int = taxYear - 1

  def retrieveTaxYearList(implicit request: Request[_]): Seq[Int] = {
    getFromSession(SessionValues.VALID_TAX_YEARS)(request).getOrElse("").split(',').toSeq.map(_.toInt)
  }

  def firstClientTaxYear(implicit request: Request[_]): Int = retrieveTaxYearList.head
  def latestClientTaxYear(implicit request: Request[_]): Int = retrieveTaxYearList.last

  def singleValidTaxYear(implicit request: Request[_]): Boolean = firstClientTaxYear == latestClientTaxYear
}
