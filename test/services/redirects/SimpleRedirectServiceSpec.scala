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
import models.pension.reliefs.PaymentsIntoPensionViewModel
import play.api.http.Status.SEE_OTHER
import play.api.mvc.Results.Redirect
import play.api.mvc.{Call, Result}
import utils.UnitTest

import scala.concurrent.Future

class SimpleRedirectServiceSpec extends UnitTest {

  val cyaData: PensionsCYAModel = PensionsCYAModel.emptyModels
  val contextualRedirect: Call = TotalPaymentsIntoRASController.show(taxYear)
  val cyaRedirect: Call = PaymentsIntoPensionsCYAController.show(taxYear)
  val noneRedirect: PensionsCYAModel => Option[Result] = cyaData => None
  val someRedirect: PensionsCYAModel => Option[Result] = cyaData => Some(Redirect(ReliefAtSourcePensionsController.show(taxYear)))
  val continueRedirect: PensionsUserData => Future[Result] = aPensionsUserData => Future.successful(Redirect(contextualRedirect))
  val cyaPageCall = controllers.pensions.paymentsIntoPensions.routes.PaymentsIntoPensionsCYAController.show(taxYear)

  ".redirectBasedOnCurrentAnswers" should {

    "continue to attempted page when there is session data and 'shouldRedirect' is None" which {
      val result: Future[Result] = SimpleRedirectService.redirectBasedOnCurrentAnswers(taxYear, Some(aPensionsUserData), cyaPageCall)(noneRedirect)(continueRedirect)
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
      val result = SimpleRedirectService.redirectBasedOnCurrentAnswers(taxYear, Some(aPensionsUserData), cyaPageCall)(someRedirect)(continueRedirect)
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
      val result = SimpleRedirectService.redirectBasedOnCurrentAnswers(taxYear, None, cyaPageCall)(someRedirect)(continueRedirect)
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

}
