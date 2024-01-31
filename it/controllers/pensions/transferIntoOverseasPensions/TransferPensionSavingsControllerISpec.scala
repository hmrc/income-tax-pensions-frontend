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

package controllers.pensions.transferIntoOverseasPensions

import builders.PensionsCYAModelBuilder.aPensionsCYAModel
import builders.PensionsUserDataBuilder.taxYearEOY
import controllers.ControllerSpec.UserConfig
import controllers.YesNoControllerSpec
import models.pension.charges.TransfersIntoOverseasPensionsViewModel
import play.api.http.Status.BAD_REQUEST
import play.api.libs.ws.WSResponse
import utils.PageUrls.TransferIntoOverseasPensions.checkYourDetailsPensionUrl

class TransferPensionSavingsControllerISpec extends YesNoControllerSpec("/overseas-pensions/overseas-transfer-charges/transfer-pension-savings") {

  "This page" when {
    ".show" should {
      "redirect to the summary page" when {
        "the user has no stored session data at all" in {

          implicit val userConfig: UserConfig = userConfigWhenIrrelevant(None)
          implicit val response: WSResponse   = getPage

          assertRedirectionAsExpected(PageRelativeURLs.pensionsSummaryPage)
        }
      }
    }
    ".submit" should {
      "redirect to the expected page" when {
        "the user has no stored session data at all" in {

          implicit val userConfig: UserConfig = userConfigWhenIrrelevant(None)
          implicit val response: WSResponse   = submitForm(SubmittedFormDataForYesNoPage(Some(false)))

          assertRedirectionAsExpected(PageRelativeURLs.pensionsSummaryPage)
          getTransferPensionsViewModel mustBe None
        }
      }
      "succeed" when {
        "the user has relevant session data and" when {
          val sessionData = pensionsUserData(aPensionsCYAModel)

          "the user has selected 'Yes'" in {
            val redirectPage = relativeUrl("/overseas-pensions/overseas-transfer-charges/transfer-charge")

            val expectedViewModel = sessionData.pensions.transfersIntoOverseasPensions.copy(
              transferPensionSavings = Some(true)
            )

            implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
            implicit val response: WSResponse   = submitForm(SubmittedFormDataForYesNoPage(Some(true)))

            assertRedirectionAsExpected(redirectPage)
            getTransferPensionsViewModel mustBe Some(expectedViewModel)
          }

          "the user has selected 'No'" in {
            implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
            implicit val response: WSResponse   = submitForm(SubmittedFormDataForYesNoPage(Some(false)))

            assertRedirectionAsExpected(checkYourDetailsPensionUrl(taxYearEOY))
            getTransferPensionsViewModel mustBe Some(TransfersIntoOverseasPensionsViewModel(transferPensionSavings = Some(false)))
          }

          "the user has not selected any option" in {

            implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
            implicit val response: WSResponse   = submitForm(SubmittedFormDataForYesNoPage(None))

            response must haveStatus(BAD_REQUEST)
          }
        }
      }
    }
  }
}
