/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers.predicates

import builders.AuthorisationRequestBuilder.anAuthorisationRequest
import config.MockAppConfig
import play.api.mvc.Results.Redirect
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import support.UnitTest
import utils.TestTaxYearHelper

import scala.concurrent.ExecutionContext

class TailoringEnabledFilterActionSpec extends UnitTest with TestTaxYearHelper {

  private val appConfig = new MockAppConfig().config()
  private val appConfigWithTailoringDisabled = new MockAppConfig().config(enableTailoring = false)
  private val executionContext = ExecutionContext.global

  ".executionContext" should {
    "return the given execution context" in {
      val underTest = TailoringEnabledFilterAction(taxYear = taxYear, appConfig = appConfig)(executionContext)

      underTest.executionContext shouldBe executionContext
    }
  }

  ".filter" should {
    "return a redirect to Income Tax Submission Overview when tailoring is disabled" in {
      val underTest = TailoringEnabledFilterAction(taxYear = taxYear, appConfig = appConfigWithTailoringDisabled)(executionContext)

      await(underTest.filter(anAuthorisationRequest)) shouldBe Some(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
    }

    "return None when tailoring is enabled" in {
      val underTest = TailoringEnabledFilterAction(taxYear = taxYearEOY, appConfig = appConfig)(executionContext)

      await(underTest.filter(anAuthorisationRequest)) shouldBe None
    }
  }
}
