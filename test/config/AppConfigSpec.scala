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

package config

import uk.gov.hmrc.play.bootstrap.binders.SafeRedirectUrl
import utils.UnitTest

class AppConfigSpec extends UnitTest {
  private val appConfig = app.injector.instanceOf[AppConfig]

  "AppConfig" should {

    "return correct feedbackUrl when the user is an individual" in {
      val expectedServiceIdentifier = "update-and-submit-income-tax-return"

      implicit val isAgent: Boolean = false

      val expectedBetaFeedbackUrl =
        s"http://localhost:9250/contact/beta-feedback?service=update-and-submit-income-tax-return&backUrl=http%3A%2F%2Flocalhost%3A9321%2F"

      val expectedFeedbackSurveyUrl = s"http://localhost:9514/feedback/$expectedServiceIdentifier"
      val expectedContactUrl        = s"http://localhost:9250/contact/contact-hmrc?service=$expectedServiceIdentifier"
      val expectedSignOutUrl        = s"http://localhost:9553/bas-gateway/sign-out-without-state"
      val expectedSignInUrl =
        "http://localhost:9949/auth-login-stub/gg-sign-in?continue=http%3A%2F%2Flocalhost%3A9152&origin=income-tax-pensions-frontend"
      val expectedSignInContinueUrl = "http%3A%2F%2Flocalhost%3A9152"

      appConfig.betaFeedbackUrl(fakeRequest, isAgent) shouldBe expectedBetaFeedbackUrl
      appConfig.feedbackSurveyUrl shouldBe expectedFeedbackSurveyUrl
      appConfig.contactUrl shouldBe expectedContactUrl
      appConfig.signOutUrl shouldBe expectedSignOutUrl

      appConfig.signInUrl shouldBe expectedSignInUrl
      appConfig.signInContinueUrl shouldBe expectedSignInContinueUrl

      appConfig.incomeTaxSubmissionBaseUrl shouldBe "http://localhost:9302/update-and-submit-income-tax-return"
      appConfig.statePensionBEBaseUrl shouldBe "http://localhost:9377"
      appConfig.incomeTaxSubmissionBEBaseUrl shouldBe "http://localhost:9304/income-tax-submission-service"
      appConfig.incomeTaxSubmissionIvRedirect shouldBe "http://localhost:9302/update-and-submit-income-tax-return/iv-uplift"
    }

    "return the correct feedback url when the user is an agent" in {
      val expectedServiceIdentifierAgent = "update-and-submit-income-tax-return-agent"

      implicit val isAgent: Boolean = true

      val expectedBetaFeedbackUrl =
        "http://localhost:9250/contact/beta-feedback?service=update-and-submit-income-tax-return-agent&backUrl=http%3A%2F%2Flocalhost%3A9321%2F"

      val expectedFeedbackSurveyUrl = s"http://localhost:9514/feedback/$expectedServiceIdentifierAgent"
      val expectedContactUrl        = s"http://localhost:9250/contact/contact-hmrc?service=$expectedServiceIdentifierAgent"
      val expectedSignOutUrl        = s"http://localhost:9553/bas-gateway/sign-out-without-state"

      appConfig.betaFeedbackUrl(fakeRequest, isAgent) shouldBe expectedBetaFeedbackUrl
      appConfig.feedbackSurveyUrl shouldBe expectedFeedbackSurveyUrl
      appConfig.contactUrl shouldBe expectedContactUrl
      appConfig.signOutUrl shouldBe expectedSignOutUrl
    }
  }
}
