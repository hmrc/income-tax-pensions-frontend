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

package controllers.predicates.actions

import common.SessionValues
import config.AppConfig
import models.{AuthorisationRequest, User}
import play.api.http.Status.SEE_OTHER
import play.api.i18n.MessagesApi
import uk.gov.hmrc.auth.core.AffinityGroup
import utils.{TestTaxYearHelper, UnitTest}

class TaxYearActionSpec extends UnitTest with TestTaxYearHelper {

  implicit val mockedConfig: AppConfig = app.injector.instanceOf[AppConfig]
  implicit lazy val cc: MessagesApi    = mockControllerComponents.messagesApi

  def taxYearAction(taxYear: Int, reset: Boolean = true): TaxYearAction = new TaxYearAction(taxYear, reset)

  "TaxYearAction.refine" should {

    "return a Right(request)" when {

      "the tax year is within the list of valid tax years, and matches that in session if the feature switch is on" in {
        lazy val userRequest = AuthorisationRequest(
          User("1234567890", None, "AA123456A", sessionId, AffinityGroup.Individual.toString),
          fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString, SessionValues.VALID_TAX_YEARS -> validTaxYearList.mkString(","))
        )

        lazy val result =
          await(taxYearAction(taxYear).refine(userRequest))

        result.isRight shouldBe true
      }

      "the tax year is within the list of valid tax years, and matches that in session if the feature switch is off" in {
        lazy val userRequest = AuthorisationRequest(
          User("1234567890", None, "AA123456A", sessionId, AffinityGroup.Individual.toString),
          fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYearEOY.toString, SessionValues.VALID_TAX_YEARS -> validTaxYearList.mkString(","))
        )

        lazy val result =
          await(taxYearAction(taxYearEOY).refine(userRequest))

        result.isRight shouldBe true
      }

      "the tax year is different to the session value if the reset variable input is false" in {
        lazy val userRequest = AuthorisationRequest(
          User("1234567890", None, "AA123456A", sessionId, AffinityGroup.Individual.toString),
          fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString, SessionValues.VALID_TAX_YEARS -> validTaxYearList.mkString(","))
        )

        lazy val result =
          await(taxYearAction(taxYearEOY, reset = false).refine(userRequest))

        result.isRight shouldBe true
      }

    }

    "return a Left(result)" when {

      "the VALID_TAX_YEARS session value is not present" which {
        lazy val userRequest = AuthorisationRequest(
          User("1234567890", None, "AA123456A", sessionId, AffinityGroup.Individual.toString),
          fakeRequest.withSession(
            SessionValues.TAX_YEAR -> s"$taxYear"
          )
        )

        lazy val result = taxYearAction(taxYear).refine(userRequest)

        "has a status of SEE_OTHER (303)" in {
          status(result.map(_.left.toOption.get)) shouldBe SEE_OTHER
        }

        "has the start page redirect url" in {
          redirectUrl(result.map(_.left.toOption.get)) shouldBe "http://localhost:9302/update-and-submit-income-tax-return/2026/start"
        }

      }

      "the tax year is outside of validTaxYearList while the feature switch is on" which {
        lazy val userRequest = AuthorisationRequest(
          User("1234567890", None, "AA123456A", sessionId, AffinityGroup.Individual.toString),
          fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString, SessionValues.VALID_TAX_YEARS -> validTaxYearList.mkString(","))
        )

        lazy val result =
          taxYearAction(invalidTaxYear).refine(userRequest)

        "has a status of SEE_OTHER (303)" in {
          status(result.map(_.left.toOption.get)) shouldBe SEE_OTHER
        }

        "has the TaxYearError redirect url" in {
          redirectUrl(result.map(_.left.toOption.get)) shouldBe controllers.errors.routes.TaxYearErrorController.show.url
        }
      }

      "the tax year is within the validTaxYearList but the missing tax year reset is true" which {
        lazy val userRequest = AuthorisationRequest(
          User("1234567890", None, "AA123456A", sessionId, AffinityGroup.Individual.toString),
          fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString, SessionValues.VALID_TAX_YEARS -> validTaxYearList.mkString(","))
        )

        lazy val result =
          taxYearAction(taxYearEOY).refine(userRequest)

        "has a status of SEE_OTHER (303)" in {
          status(result.map(_.left.toOption.get)) shouldBe SEE_OTHER
        }

        "has the Overview page redirect url" in {
          redirectUrl(result.map(_.left.toOption.get)) shouldBe s"http://localhost:9302/update-and-submit-income-tax-return/$taxYearEOY/view"
        }

        "has the updated TAX_YEAR session value" in {
          await(result.map(_.left.toOption.get)).session.get(SessionValues.TAX_YEAR).get shouldBe taxYearEOY.toString
        }
      }
    }
  }
}
