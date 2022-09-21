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

import builders.UserBuilder.aUserRequest
import forms.YesNoForm
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.pension.reliefs.PaymentsIntoPensionViewModel
import org.scalatest.BeforeAndAfterEach
import play.api.{Application, Environment, Mode}
import play.api.http.HeaderNames
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.{route, writeableOf_AnyContentAsFormUrlEncoded}
import utils.PageUrls.PaymentIntoPensions.{checkPaymentsIntoPensionCyaUrl, reliefAtSourcePensionsUrl}
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

import scala.concurrent.Future

class PaymentsIntoPensionsStatusControllerISpec extends IntegrationTest
  with ViewHelpers
  with BeforeAndAfterEach
  with PensionsDatabaseHelper {

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  val csrfContent: (String, String) = "Csrf-Token" -> "nocheck"

  lazy val testApp: Application = new GuiceApplicationBuilder()
    .in(Environment.simple(mode = Mode.Dev))
    .configure(config)
    .build()

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    dropPensionsDB()
  }

  private def url(taxYear: Int): String = {
    s"/update-and-submit-income-tax-return/pensions/$taxYear/payments-into-pensions/payments-into-pensions-status"
  }

  ".show" should {

    "return OK when page is accessed " in {
      val request = FakeRequest("GET", url(taxYearEOY)).withHeaders(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
      lazy val result: Future[Result] = {
        authoriseIndividual(true)
        route(app, request, "{}").get
      }

      status(result) shouldBe OK
    }
  }

  ".submit" when {

    "redirect to the next step in the journey when 'Yes' is selected" which {
      val request = FakeRequest("POST", url(taxYearEOY)).withHeaders(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList), csrfContent)
        .withFormUrlEncodedBody(YesNoForm.yesNo -> "true")

      lazy val result: Future[Result] = {
        authoriseIndividual(true)
        dropPensionsDB()
        emptyUserDataStub(nino, taxYearEOY)
        insertCyaData(
          PensionsUserData("", nino, mtditid, taxYear = taxYearEOY, isPriorSubmission = false,
            pensions = PensionsCYAModel.emptyModels.copy(paymentsIntoPension = PaymentsIntoPensionViewModel(rasPensionPaymentQuestion = Some(true)))),
          aUserRequest
        )
        route(app, request).get
      }

      "has a status of SEE_OTHER(303)" in {
        status(result) shouldBe SEE_OTHER
      }

      "has the correct redirect location" in {
        await(result).header.headers("Location") shouldBe reliefAtSourcePensionsUrl(taxYearEOY)
      }
    }

    "redirect to income tax submission overview when 'No' is selected with incomplete cya data & no prior data" should {
      val request = FakeRequest("POST", url(taxYearEOY)).withHeaders(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList), csrfContent)
        .withFormUrlEncodedBody(YesNoForm.yesNo -> "false")

      lazy val result = {
        authoriseIndividual(true)
        dropPensionsDB()
        emptyUserDataStub(nino, taxYearEOY)
        insertCyaData(
          PensionsUserData("", nino, mtditid, taxYear = taxYearEOY, isPriorSubmission = false,
          pensions = PensionsCYAModel.emptyModels.copy(paymentsIntoPension = PaymentsIntoPensionViewModel(rasPensionPaymentQuestion = Some(true)))),
          aUserRequest
        )
        route(app, request, Map("value" -> Seq("false"))).get
      }

      "has a status of SEE_OTHER(303)" in {
        status(result) shouldBe SEE_OTHER
      }

      "has the correct redirect location" in {
        await(result).header.headers("Location") shouldBe checkPaymentsIntoPensionCyaUrl(taxYearEOY)
      }
    }

    "redirect to pensions summary when 'No' is selected with incomplete cya data & no prior data" which {

      val request = FakeRequest("POST", url(taxYearEOY)).withHeaders(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList), csrfContent)
        .withFormUrlEncodedBody(YesNoForm.yesNo -> "false")

        lazy val result = {
          authoriseIndividual(true)
          dropPensionsDB()
          emptyUserDataStub(nino, taxYearEOY)
          insertCyaData(
            PensionsUserData("", nino, mtditid, taxYear = taxYearEOY, isPriorSubmission = false,
              pensions = PensionsCYAModel.emptyModels),
            aUserRequest
          )
          route(app, request, Map("value" -> Seq("false"))).get
        }

        "has a status of SEE_OTHER(303)" in {
          status(result) shouldBe SEE_OTHER
        }

        "has the correct redirect location" in {
          await(result).header.headers("Location") shouldBe checkPaymentsIntoPensionCyaUrl(taxYearEOY)
        }
      }

    }
}
