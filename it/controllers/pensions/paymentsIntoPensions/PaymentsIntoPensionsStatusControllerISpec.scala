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

package controllers.pensions.paymentsIntoPensions

import forms.YesNoForm
import org.scalatest.BeforeAndAfterEach
import play.api.{Application, Environment, Mode}
import play.api.http.HeaderNames
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.{route, writeableOf_AnyContentAsFormUrlEncoded}
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

import scala.concurrent.Future

class PaymentsIntoPensionsStatusControllerISpec extends IntegrationTest
  with ViewHelpers
  with BeforeAndAfterEach
  with PensionsDatabaseHelper {

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  val csrfContent: (String, String) = "Csrf-Token" -> "nocheck"

  protected val configWithTailoringDisabled: Map[String, String] = config ++ Map("feature-switch.tailoringEnabled" -> "false")

  lazy val appWithTailoringDisabled: Application = new GuiceApplicationBuilder()
    .in(Environment.simple(mode = Mode.Dev))
    .configure(configWithTailoringDisabled)
    .build()

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    dropPensionsDB()
  }

  private def url(taxYear: Int): String = {
    s"/update-and-submit-income-tax-return/pensions/$taxYear/payments-into-pensions/payments-into-pensions-status"
  }

  ".show" should {
    "redirect to income tax submission overview when tailoring is disabled" in {
      val request = FakeRequest("GET", url(taxYearEOY)).withHeaders(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
      lazy val result: Future[Result] = {
        authoriseIndividual(true)
        route(appWithTailoringDisabled, request, "{}").get
      }
      status(result) shouldBe SEE_OTHER
      await(result).header.headers("Location") shouldBe appConfig.incomeTaxSubmissionOverviewUrl(taxYearEOY)
    }

    "return OK when tailoring is enabled" in {
      val request = FakeRequest("GET", url(taxYearEOY)).withHeaders(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
      lazy val result: Future[Result] = {
        authoriseIndividual(true)
        route(app, request, "{}").get
      }

      status(result) shouldBe OK
    }
  }

  ".submit" should {
    "redirect to income tax submission overview when tailoring is disabled" in {
      val request = FakeRequest("POST", url(taxYearEOY)).withHeaders(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList), csrfContent)

      lazy val result: Future[Result] = {
        authoriseIndividual(true)
        route(appWithTailoringDisabled, request, "{}").get
      }

      status(result) shouldBe SEE_OTHER
      await(result).header.headers("Location") shouldBe appConfig.incomeTaxSubmissionOverviewUrl(taxYearEOY)
    }

    "redirect to pensions summary when tailoring is enabled and 'Yes' is selected" in {
      val request = FakeRequest("POST", url(taxYearEOY)).withHeaders(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList), csrfContent)
        .withFormUrlEncodedBody(YesNoForm.yesNo -> "true")

      lazy val result: Future[Result] = {
        authoriseIndividual(true)
        route(app, request).get
      }

      status(result) shouldBe SEE_OTHER
      await(result).header.headers("Location") shouldBe controllers.pensions.routes.PensionsSummaryController.show(taxYearEOY).url
    }

    "redirect to income tax submission overview when tailoring is enabled and 'No' is selected" in {
      val request = FakeRequest("POST", url(taxYearEOY)).withHeaders(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList), csrfContent)
        .withFormUrlEncodedBody(YesNoForm.yesNo -> "false")

      lazy val result: Future[Result] = {
        authoriseIndividual(true)
        route(app, request).get
      }

      status(result) shouldBe SEE_OTHER
      await(result).header.headers("Location") shouldBe appConfig.incomeTaxSubmissionOverviewUrl(taxYearEOY)
    }
  }
}
