/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.errors

import controllers.ControllerSpecBase
import play.api.test.Helpers._
import views.html.SupportingAgentAuthErrorView

class SupportingAgentAuthErrorControllerSpec extends ControllerSpecBase {
  val view       = app.injector.instanceOf[SupportingAgentAuthErrorView]
  val controller = new SupportingAgentAuthErrorController(mcc, appConfig, view)

  "show" should {
    "return Unauthorized and render the correct view for a GET" in {
      val result = controller.show(fakeRequest)

      assert(status(result) === UNAUTHORIZED)
      assert(contentAsString(result) === view().toString())
    }
  }
}
