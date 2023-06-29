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

package services.redirects

import builders.UnauthorisedPaymentsViewModelBuilder.{anUnauthorisedPaymentsEmptyViewModel, anUnauthorisedPaymentsViewModel}
import controllers.pensions.unauthorisedPayments.routes.{UnauthorisedPaymentsCYAController, UnauthorisedPaymentsController}
import models.mongo.PensionsCYAModel
import play.api.mvc.Results.Redirect
import services.redirects.UnauthorisedPaymentsPages._
import services.redirects.UnauthorisedPaymentsRedirects.{cyaPageCall, journeyCheck}
import utils.UnitTest

class UnauthorisedPaymentsRedirectsSpec extends UnitTest {

  private val cyaData: PensionsCYAModel = PensionsCYAModel.emptyModels
  private val someRedirect = Some(Redirect(UnauthorisedPaymentsController.show(taxYear)))

  ".journeyCheck" should {
    "return None if page is valid and all previous questions have been answered" when {
      "current page is empty and at end of journey so far" in {
        val data = cyaData.copy(
          unauthorisedPayments = anUnauthorisedPaymentsViewModel.copy(
            noSurchargeTaxAmountQuestion = None,
            noSurchargeTaxAmount = None,
            ukPensionSchemesQuestion = None,
            pensionSchemeTaxReference = None
          )
        )
        val result = journeyCheck(NonUkTaxOnNotSurchargedAmountPage, data, taxYear)

        result shouldBe None
      }
      "current page is pre-filled and at end of journey so far" in {
        val data = cyaData.copy(
          unauthorisedPayments = anUnauthorisedPaymentsViewModel.copy(
            noSurchargeTaxAmountQuestion = Some(true),
            noSurchargeTaxAmount = Some(345),
            ukPensionSchemesQuestion = None,
            pensionSchemeTaxReference = None
          )
        )
        val result = journeyCheck(NonUkTaxOnNotSurchargedAmountPage, data, taxYear)

        result shouldBe None
      }
      "current page is pre-filled and mid-journey" in {
        val data = cyaData.copy(unauthorisedPayments = anUnauthorisedPaymentsViewModel)
        val result = journeyCheck(PSTRPage, data, taxYear)

        result shouldBe None
      }
      "previous page is unanswered but invalid and previous valid question has been answered" in {
        val data = cyaData.copy(
          unauthorisedPayments = anUnauthorisedPaymentsEmptyViewModel.copy(
            surchargeQuestion = Some(false),
            noSurchargeQuestion = Some(true),
            surchargeAmount = None,
            surchargeTaxAmountQuestion = None,
            surchargeTaxAmount = None,
            noSurchargeAmount = None,
          )
        )
        val result = journeyCheck(NotSurchargedAmountPage, data, taxYear)

        result shouldBe None
      }
    }

    "return Some(redirect) with redirect to the first page in journey page" when {
      "previous question is unanswered" in {
        val data = cyaData.copy(
          unauthorisedPayments = anUnauthorisedPaymentsEmptyViewModel.copy(
            surchargeQuestion = Some(true),
            surchargeAmount = None
          )
        )
        val result = journeyCheck(NonUkTaxOnSurchargedAmountPage, data, taxYear)

        result shouldBe someRedirect
      }
      "current page is invalid in journey" in {
        val data = cyaData.copy(
          unauthorisedPayments = anUnauthorisedPaymentsViewModel.copy(
            ukPensionSchemesQuestion = None,
            pensionSchemeTaxReference = None
          )
        )
        val result = journeyCheck(RemovePSTRPage, data, taxYear)

        result shouldBe someRedirect
      }
    }
  }

  ".cyaPageCall" should {
    "return a redirect call to the cya page" in {
      cyaPageCall(taxYear) shouldBe UnauthorisedPaymentsCYAController.show(taxYear)
    }
  }
}

