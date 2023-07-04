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
import controllers.pensions.paymentsIntoPensions.routes.{PaymentsIntoPensionsCYAController, ReliefAtSourcePensionsController, TotalPaymentsIntoRASController}
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.pension.reliefs.PaymentsIntoPensionsViewModel
import play.api.http.Status.SEE_OTHER
import play.api.mvc.Results.Redirect
import services.redirects.UnauthorisedPaymentsRedirects.cyaPageCall
import play.api.mvc.{Call, Result}
import builders.UnauthorisedPaymentsViewModelBuilder.{anUnauthorisedPaymentsEmptyViewModel, anUnauthorisedPaymentsViewModel}
import controllers.pensions.unauthorisedPayments.routes._
import play.api.libs.ws.WSResponse
import utils.UnitTest

import scala.concurrent.Future

class SimpleRedirectServiceSpec extends UnitTest {

  private val cyaData: PensionsCYAModel = PensionsCYAModel.emptyModels
  private val contextualRedirect: Call = TotalPaymentsIntoRASController.show(taxYear)
  private val cyaRedirect: Call = PaymentsIntoPensionsCYAController.show(taxYear)
  private val noneRedirect: PensionsCYAModel => Option[Result] = cyaData => None
  private val someRedirect: PensionsCYAModel => Option[Result] = cyaData => Some(Redirect(ReliefAtSourcePensionsController.show(taxYear)))
  private val continueToContextualRedirect: PensionsUserData => Future[Result] = aPensionsUserData => Future.successful(Redirect(contextualRedirect))
  private val pIPCyaPageCall = PaymentsIntoPensionsCYAController.show(taxYear)

  ".redirectBasedOnCurrentAnswers" should {

    "continue to attempted page when there is session data and 'shouldRedirect' is None" which {
      val result: Future[Result] = SimpleRedirectService.redirectBasedOnCurrentAnswers(taxYear, Some(aPensionsUserData), pIPCyaPageCall)(noneRedirect)(continueToContextualRedirect)
      val resultStatus = result.map(_.header.status)
      val resultHeader = result.map(_.header.headers)

      "result status is 303" in {
        val status = resultStatus.value.get.get

        status shouldBe SEE_OTHER
      }
      "location header is dependent on the 'continue' argument" in {
        val locationHeader = resultHeader.value.get.get.get("Location").get

        locationHeader shouldBe contextualRedirect.url
      }
    }

    "redirect to first page in journey when there is session data and 'shouldRedirect' is Some(firstPageRedirect)" which {
      val result = SimpleRedirectService.redirectBasedOnCurrentAnswers(taxYear, Some(aPensionsUserData), pIPCyaPageCall)(someRedirect)(continueToContextualRedirect)
      val resultStatus = result.map(_.header.status)
      val resultHeader = result.map(_.header.headers)

      "result status is 303" in {
        val status = resultStatus.value.get.get

        status shouldBe SEE_OTHER
      }
      "location header is first page of journey" in {
        val locationHeader = resultHeader.value.get.get.get("Location").get

        locationHeader shouldBe ReliefAtSourcePensionsController.show(taxYear).url
      }
    }

    "redirect to CYA page when there is no session data" which {
      val result = SimpleRedirectService.redirectBasedOnCurrentAnswers(taxYear, None, pIPCyaPageCall)(someRedirect)(continueToContextualRedirect)
      val resultStatus = result.map(_.header.status)
      val resultHeader = result.map(_.header.headers)

      "result status is 303" in {
        val status = resultStatus.value.get.get

        status shouldBe SEE_OTHER
      }
      "location header is CYA page" in {
        val locationHeader = resultHeader.value.get.get.get("Location").get

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

  ".checkForExistingSchemes" should {
    "return a Call to the first page in scheme loop when 'schemes' is empty" in {
      val emptySchemes: Seq[String] = Seq.empty
      val result = SimpleRedirectService.checkForExistingSchemes(
        nextPage = UnauthorisedPensionSchemeTaxReferenceController.show(taxYear, None),
        summaryPage = UkPensionSchemeDetailsController.show(taxYear),
        emptySchemes)

      result shouldBe UnauthorisedPensionSchemeTaxReferenceController.show(taxYear, None)
    }
    "return a Call to the scheme summary page when 'schemes' already exist" in {
      val existingSchemes: Seq[String] = Seq("12345", "54321", "55555")
      val result = SimpleRedirectService.checkForExistingSchemes(
        nextPage = UnauthorisedPensionSchemeTaxReferenceController.show(taxYear, None),
        summaryPage = UkPensionSchemeDetailsController.show(taxYear),
        existingSchemes)

      result shouldBe UkPensionSchemeDetailsController.show(taxYear)
    }
  }
}
