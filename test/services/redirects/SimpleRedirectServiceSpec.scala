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

import builders.PensionsUserDataBuilder.aPensionsUserData
import builders.UnauthorisedPaymentsViewModelBuilder.anUnauthorisedPaymentsViewModel
import controllers.pensions.paymentsIntoPensions.routes.{PaymentsIntoPensionsCYAController, ReliefAtSourcePensionsController, TotalPaymentsIntoRASController}
import controllers.pensions.unauthorisedPayments.routes._
import models.mongo.{PensionsCYAModel, PensionsUserData}
import play.api.http.Status.SEE_OTHER
import play.api.mvc.Results.Redirect
import play.api.mvc.{Call, Result}
import services.redirects.UnauthorisedPaymentsRedirects.cyaPageCall
import utils.UnitTest

import scala.concurrent.Future

class SimpleRedirectServiceSpec extends UnitTest {

  val cyaData: PensionsCYAModel = PensionsCYAModel.emptyModels
  val contextualRedirect: Call = TotalPaymentsIntoRASController.show(taxYear)
  val cyaRedirect: Call = PaymentsIntoPensionsCYAController.show(taxYear)
  val noneRedirect: PensionsCYAModel => Option[Result] = _ => None
  val someRedirect: PensionsCYAModel => Option[Result] = _ => Some(Redirect(ReliefAtSourcePensionsController.show(taxYear)))
  val continueRedirect: PensionsUserData => Future[Result] = _ => Future.successful(Redirect(contextualRedirect))
  val cyaPageCallLocal = PaymentsIntoPensionsCYAController.show(taxYear)

  ".redirectBasedOnCurrentAnswers" should {

    "continue to attempted page when there is session data and 'shouldRedirect' is None" which {
      val result: Future[Result] = SimpleRedirectService.redirectBasedOnCurrentAnswers(taxYear, Some(aPensionsUserData),
        cyaPageCallLocal)(noneRedirect)(continueRedirect)
      val resultStatus = result.map(_.header.status)
      val resultHeader = result.map(_.header.headers)

      "result status is 303" in {
        val status = resultStatus.value.get.get

        status shouldBe SEE_OTHER
      }
      "location header is dependent on the 'continue' argument" in {
        val locationHeader = resultHeader.value.get.get("Location")

        locationHeader shouldBe contextualRedirect.url
      }
    }

    "redirect to first page in journey when there is session data and 'shouldRedirect' is Some(firstPageRedirect)" which {
      val result = SimpleRedirectService.redirectBasedOnCurrentAnswers(taxYear, Some(aPensionsUserData), cyaPageCallLocal)(someRedirect)(continueRedirect)
      val resultStatus = result.map(_.header.status)
      val resultHeader = result.map(_.header.headers)

      "result status is 303" in {
        val status = resultStatus.value.get.get

        status shouldBe SEE_OTHER
      }
      "location header is first page of journey" in {
        val locationHeader = resultHeader.value.get.get("Location")

        locationHeader shouldBe ReliefAtSourcePensionsController.show(taxYear).url
      }
    }

    "redirect to CYA page when there is no session data" which {
      val result = SimpleRedirectService.redirectBasedOnCurrentAnswers(taxYear, None, cyaPageCallLocal)(someRedirect)(continueRedirect)
      val resultStatus = result.map(_.header.status)
      val resultHeader = result.map(_.header.headers)

      "result status is 303" in {
        val status = resultStatus.value.get.get

        status shouldBe SEE_OTHER
      }
      "location header is CYA page" in {
        val locationHeader = resultHeader.value.get.get("Location")

        locationHeader shouldBe cyaRedirect.url
      }
    }

  }

  ".isFinishedCheck" should {
    "redirect to the CYA page" when {
      "all journey questions are answered" in {
        val result: Result = SimpleRedirectService.isFinishedCheck(
          anUnauthorisedPaymentsViewModel,
          taxYear,
          UnauthorisedPensionSchemeTaxReferenceController.show(taxYear, Some(1)),
          cyaPageCall
        )

        result.header.status shouldBe SEE_OTHER
        result.header.headers("location").contains(UnauthorisedPaymentsCYAController)
      }
      "all necessary journey questions are answered" in {
        val result = SimpleRedirectService.isFinishedCheck(
          anUnauthorisedPaymentsViewModel.copy(
            noSurchargeQuestion = Some(false),
            noSurchargeAmount = None,
            noSurchargeTaxAmountQuestion = None,
            noSurchargeTaxAmount = None
          ),
          taxYear,
          UnauthorisedPensionSchemeTaxReferenceController.show(taxYear, Some(1)),
          cyaPageCall
        )

        result.header.status shouldBe SEE_OTHER
        result.header.headers("location").contains(UnauthorisedPaymentsCYAController)
      }
    }

    "continue to the next page when not all journey questions have been answered" in {
      val result = SimpleRedirectService.isFinishedCheck(
        anUnauthorisedPaymentsViewModel.copy(
          noSurchargeQuestion = Some(true),
          noSurchargeAmount = None,
          noSurchargeTaxAmountQuestion = None,
          noSurchargeTaxAmount = None
        ),
        taxYear,
        UnauthorisedPensionSchemeTaxReferenceController.show(taxYear, Some(1)),
        cyaPageCall
      )

      result.header.status shouldBe SEE_OTHER
      result.header.headers("location").contains(UnauthorisedPensionSchemeTaxReferenceController)
    }
  }
}
