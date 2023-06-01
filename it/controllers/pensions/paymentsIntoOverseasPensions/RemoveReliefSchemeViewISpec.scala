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
import models.pension.charges.Relief
import play.api.libs.ws.WSResponse

class RemoveReliefSchemeViewISpec extends ControllerSpec("/overseas-pensions/payments-into-overseas-pensions/remove-schemes") {

  "This page" when {

    "shown" should {
      "redirect to the summary page" when {
        "the user has no stored session data at all" in {
          implicit val userConfig: UserConfig = userConfigWhenIrrelevant(None)
          implicit val response: WSResponse = getPageWithIndex()
          assertRedirectionAsExpected(PageRelativeURLs.pensionsSummaryPage)
        }
      }

      "redirect to relief scheme summary page" when {
        "the user accesses page with index out of bounds" in {
          val sessionData = pensionsUserData(aPensionsCYAModel)
          implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
          implicit val response: WSResponse = getPageWithIndex(2)
          assertRedirectionAsExpected(PageRelativeURLs.ReliefsSchemeDetails)
        }
      }
    }

    "submitted" should {
      "the user has relevant session data and" when {
        "removes a pension scheme successfully" in {

          val sessionData = pensionsUserData(aPensionsCYAModel)
          implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))

          implicit val response: WSResponse = submitForm(Map("" -> ""), Map("index" -> "0"))

          assertRedirectionAsExpected(PageRelativeURLs.ReliefsSchemeSummary)
          val expectedViewModel = sessionData.pensions.paymentsIntoOverseasPensions.copy(
            reliefs = Seq(Relief(
              Some("PENSIONINCOME356"),
              Some(100.0),
              Some("Migrant member relief"),
              None, None, None, None, None,
              Some("123456"),
              None)
            )
          )
          getPaymentsIntoOverseasPensionsViewModel mustBe Some(expectedViewModel)

        }
      }
    }
  }
}
