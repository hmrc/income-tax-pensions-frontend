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

package controllers.pensions.annualAllowances

import controllers.ControllerSpecBase
import play.api.test.Helpers._
import testdata.AnnualAllowancesViewModelTestData._
import utils.CommonData.{currTaxYear, ec}
import views.html.pensions.annualAllowances.AnnualAllowancesCYAView

class AnnualAllowancesCYAControllerSpec extends ControllerSpecBase {
  val view       = app.injector.instanceOf[AnnualAllowancesCYAView]
  val controller = new AnnualAllowanceCYAController(auditProvider, view, pensionsService, errorHandler, mcc)(appConfig, ec)

  "show" should {
    "return OK and the correct view for a GET" in {
      val result = controller.show(currTaxYear)(fakeRequest)

      assert(status(result) === OK)
      assert(contentAsString(result) === view(currTaxYear, answers).toString())
    }
  }

  "submit" should {
    "upsert answers and return a redirect to the next page" in {
      val result = controller.submit(currTaxYear)(fakeRequest)

      status(result) mustBe SEE_OTHER
      assert(pensionsService.annualAllowancesList === List(answers))
    }
  }
}
