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

package controllers.pensions.paymentsIntoPensions

import controllers.ControllerSpecBase
import play.api.test.Helpers._
import testdata.PaymentsIntoPensionsViewModelTestData._
import utils.CommonData.currTaxYear
import views.html.pensions.paymentsIntoPensions.PaymentsIntoPensionsCYAView

class PaymentsIntoPensionsCYAControllerSpec extends ControllerSpecBase {
  val view = app.injector.instanceOf[PaymentsIntoPensionsCYAView]
  val controller = new PaymentsIntoPensionsCYAController(
    auditProvider,
    pensionsService,
    excludeJourneyService,
    view,
    errorHandler,
    mcc
  )(appConfig)

  "show" should {
    "return OK and the correct view for a GET" in {
      val result = controller.show(currTaxYear)(fakeRequest)

      assert(status(result) === OK)
      assert(contentAsString(result) === view(currTaxYear.endYear, answers).toString())
    }
  }

  "submit" should {
    "upsert answers and return a redirect to the next page" in {
      val result = controller.submit(currTaxYear)(fakeRequest)

      status(result) mustBe SEE_OTHER
      assert(pensionsService.paymentsIntoPensionsList === List(answers))
    }
  }
}
