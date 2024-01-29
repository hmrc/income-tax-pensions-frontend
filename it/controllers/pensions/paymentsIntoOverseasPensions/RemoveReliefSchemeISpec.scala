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

import builders.PaymentsIntoOverseasPensionsViewModelBuilder.aPaymentsIntoOverseasPensionsViewModel
import builders.PensionsCYAModelBuilder.aPensionsCYAModel
import builders.ReliefBuilder.{aDoubleTaxationRelief, aMigrantMemberRelief, aNoTaxRelief}
import controllers.ControllerSpec
import controllers.ControllerSpec.UserConfig
import play.api.http.Status.OK
import play.api.libs.ws.WSResponse

class RemoveReliefSchemeISpec extends ControllerSpec("/overseas-pensions/payments-into-overseas-pensions/remove-schemes") {

  "This page" when {

    "shown" should {
      "show page when using a valid index" in {
        val sessionData                     = pensionsUserData(aPensionsCYAModel)
        implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
        implicit val response: WSResponse   = getPageWithIndex(1)

        response.status equals OK
      }

      "redirect to the summary page" when {
        "the user has no stored session data at all" in {
          implicit val userConfig: UserConfig = userConfigWhenIrrelevant(None)
          implicit val response: WSResponse   = getPageWithIndex()
          assertRedirectionAsExpected(PageRelativeURLs.pensionsSummaryPage)
        }
      }

      "redirect to relief scheme summary page" when {
        "the user accesses page with index out of bounds" in {
          val sessionData                     = pensionsUserData(aPensionsCYAModel)
          implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
          implicit val response: WSResponse   = getPageWithIndex(8)
          assertRedirectionAsExpected(PageRelativeURLs.reliefsSchemeSummary)
        }
      }
    }

    "submitted" should {
      "redirect to the scheme summary page" when {
        "a valid index is used and the scheme is successfully removed from the session data" in {
          val sessionData                     = pensionsUserData(aPensionsCYAModel)
          implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
          implicit val response: WSResponse   = submitForm(Map("" -> ""), Map("index" -> "0"))
          val expectedViewModel =
            sessionData.pensions.paymentsIntoOverseasPensions.copy(reliefs = Seq(aMigrantMemberRelief, aDoubleTaxationRelief, aNoTaxRelief))

          assertRedirectionAsExpected(PageRelativeURLs.reliefsSchemeSummary)
          getPaymentsIntoOverseasPensionsViewModel mustBe Some(expectedViewModel)
        }

        "an invalid index is used and there are existing schemes" in {
          val sessionData                     = pensionsUserData(aPensionsCYAModel)
          implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
          implicit val response: WSResponse   = submitForm(Map("" -> ""), Map("index" -> "4"))

          assertRedirectionAsExpected(PageRelativeURLs.reliefsSchemeSummary)
          getPaymentsIntoOverseasPensionsViewModel mustBe Some(aPaymentsIntoOverseasPensionsViewModel)
        }
        "an invalid index is used and there are no existing schemes" in {
          val sessionData =
            pensionsUserData(aPensionsCYAModel.copy(paymentsIntoOverseasPensions = aPaymentsIntoOverseasPensionsViewModel.copy(reliefs = Seq.empty)))
          implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
          implicit val response: WSResponse   = submitForm(Map("" -> ""), Map("index" -> "4"))

          assertRedirectionAsExpected(PageRelativeURLs.reliefsSchemeSummary)
          getPaymentsIntoOverseasPensionsViewModel mustBe Some(aPaymentsIntoOverseasPensionsViewModel.copy(reliefs = Seq.empty))
        }
      }

      "redirect to the Pensions Summary page when the user has no stored session data at all" in {
        implicit val userConfig: UserConfig = userConfigWhenIrrelevant(None)
        implicit val response: WSResponse   = submitForm(Map("" -> ""), Map("index" -> "0"))

        assertRedirectionAsExpected(PageRelativeURLs.pensionsSummaryPage)
        getTransferPensionsViewModel mustBe None
      }
    }
  }
}
