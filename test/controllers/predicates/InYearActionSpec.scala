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

package controllers.predicates

import java.time.LocalDateTime

import config.AppConfig
import play.api.http.HeaderNames
import play.api.mvc.{Result, Results}
import play.api.test.Helpers._
import utils.UnitTest

import scala.concurrent.Future

class InYearActionSpec extends UnitTest {
  val year2021: Int = 2021
  val year2022: Int = 2022
  val month4: Int = 4
  val day5: Int = 5
  val day6: Int = 6
  val hour23: Int = 23
  val minute59: Int = 59


  implicit lazy val mockedConfig: AppConfig = mock[AppConfig]

  def newDate(year: Int, month: Int, day: Int): LocalDateTime = LocalDateTime.of(year, month, day, 0, 0)

  "InYearAction.inYear" when {

    "return true when taxYear is 2022 and current date is 5th April 2022 and time is just before midnight" in {
      val currentDate: LocalDateTime = LocalDateTime.of(year2022, month4, day5, hour23, minute59)
      inYearAction.inYear(year2022, currentDate) shouldBe true
    }

    "return false when taxYear is 2022 and current date is 6th April 2022 at midnight" in {
      val currentDate: LocalDateTime = newDate(year2022, month4, day6)
      inYearAction.inYear(year2022, currentDate) shouldBe false
    }

    "return false when taxYear is 2022 and current date is 6th April 2022 at one minute past midnight" in {
      val currentDate: LocalDateTime = LocalDateTime.of(year2022, month4, day6, 0, 1)
      inYearAction.inYear(year2022, currentDate) shouldBe false
    }

    "return false when taxYear is 2022 and current date is 6th April 2022 at one hour past midnight" in {
      val currentDate: LocalDateTime = LocalDateTime.of(year2022, month4, day6, 1, 1)
      inYearAction.inYear(year2022, currentDate) shouldBe false
    }
  }

  "InYearAction.notInYear" when {

    def block: Future[Result] = { Future.successful(Results.Ok("Success Response")) }

    "current date is before April 6th 2021 which means the tax year is 2020-21" should {
      val currentDate = LocalDateTime.of(2021, 4, 5, 23, 59, 59, 999)

      "return redirect to incomeTaxSubmissionOverview page when the requested tax year is 2022" in {
        val futureTaxYear = 2022
        val result = inYearAction.notInYear(futureTaxYear, currentDate)(block)

        status(result) shouldBe SEE_OTHER
        header(HeaderNames.LOCATION, result) shouldBe Some(mockAppConfig.incomeTaxSubmissionOverviewUrl(futureTaxYear))
      }

      "return redirect to incomeTaxSubmissionOverview page when the requested tax year is in year (2021)" in {
        val result = inYearAction.notInYear(2021, currentDate)(block)

        status(result) shouldBe SEE_OTHER
        header(HeaderNames.LOCATION, result) shouldBe Some(mockAppConfig.incomeTaxSubmissionOverviewUrl(year2022))
      }

      Seq(2018, 2019, 2020).foreach{ previousTaxYear =>
        val yearMinusOne = currentDate.getYear-1
        s"return Ok when the requested tax year ($previousTaxYear) precedes the current tax year ($yearMinusOne-${currentDate.getYear})" in {
          val result = inYearAction.notInYear(previousTaxYear, currentDate)(block)

          status(result) shouldBe OK
        }
      }
    }

    "current date is on or after April 6th 2021 which means the tax year is 2021-22" should {
      val currentDate = LocalDateTime.of(2021, 4, 6, 0, 0)

      "return redirect to incomeTaxSubmissionOverview page when the requested tax year is 2023" in {
        val futureTaxYear = 2023
        val result = inYearAction.notInYear(futureTaxYear, currentDate)(block)

        status(result) shouldBe SEE_OTHER
        header(HeaderNames.LOCATION, result) shouldBe Some(mockAppConfig.incomeTaxSubmissionOverviewUrl(futureTaxYear))
      }

      "return redirect to incomeTaxSubmissionOverview page when the requested tax year is in year (2022)" in {
        val result = inYearAction.notInYear(year2022, currentDate)(block)

        status(result) shouldBe SEE_OTHER
        header(HeaderNames.LOCATION, result) shouldBe Some(mockAppConfig.incomeTaxSubmissionOverviewUrl(year2022))
      }

      Seq(2019, 2020, 2021).foreach{ previousTaxYear =>
        val yearMinusOne = currentDate.getYear-1
        s"return Ok when the requested tax year ($previousTaxYear) precedes the current tax year ($yearMinusOne-${currentDate.getYear})" in {
          val result = inYearAction.notInYear(previousTaxYear, currentDate)(block)

          status(result) shouldBe OK
        }
      }
    }

  }
}
