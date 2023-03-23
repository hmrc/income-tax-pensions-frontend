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

package controllers.pensions.paymentsIntoOverseasPensions

import builders.PensionsCYAModelBuilder.aPensionsCYAModel
import controllers.ControllerSpec
import controllers.ControllerSpec.UserConfig
import play.api.libs.ws.WSResponse


class UntaxedEmployerPaymentsControllerISpec extends ControllerSpec("/overseas-pensions/payments-into-overseas-pensions/untaxed-employer-payments") {

  "This page" when {
    "shown" should {
      "not show the form page but redirect to the summary page" when {
        "the user has no stored session data at all" in {
          implicit val userConfig: UserConfig = userConfigWhenIrrelevant(None)
          implicit val response: WSResponse = getPageWithIndex()
          assertRedirectionAsExpected(PageRelativeURLs.overseasPensionsSummary)
        }
      }

      "redirect to transfer charge pension scheme summary page" when {
        "the user accesses page with index out of bounds" in {
          val sessionData = pensionsUserData(aPensionsCYAModel)
          implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
          implicit val response: WSResponse = getPageWithIndex(10)

          assertRedirectionAsExpected(PageRelativeURLs.overseasPensionsSummary)
        }
      }
      "submitted" should {
        "the user has relevant session data and" when {
          "updated untaxed employer amount successfully" in {

            val sessionData = pensionsUserData(aPensionsCYAModel)
            implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))

            implicit val response: WSResponse = submitForm(Map("amount" -> "1001"), Map("index" -> "0"))

            assertRedirectionAsExpected(PageRelativeURLs.untaxedEmployerPayments ++ "?index=0")
            val expectedRelief = sessionData.pensions.paymentsIntoOverseasPensions.reliefs.head.copy(employerPaymentsAmount = Some(1001))
            val expectedViewModel = sessionData.pensions.paymentsIntoOverseasPensions.copy(reliefs = sessionData.pensions.paymentsIntoOverseasPensions.reliefs.updated(0, expectedRelief))

            getPaymentsIntoOverseasPensionsViewModel.get.reliefs mustBe expectedViewModel.reliefs
          }
        }
      }
    }
  }

}