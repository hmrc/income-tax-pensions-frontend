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

import builders.UkPensionIncomeViewModelBuilder.{anUkPensionIncomeViewModelOne, anUkPensionIncomeViewModelSeq, anUkPensionIncomeViewModelTwo}
import controllers.pensions.incomeFromPensions.routes.{PensionSchemeDetailsController, UkPensionIncomeSummaryController}
import models.pension.statebenefits.UkPensionIncomeViewModel
import play.api.mvc.Call
import services.redirects.IncomeFromOtherUkPensionsRedirects.redirectForSchemeLoop
import utils.UnitTest

class IncomeFromOtherUkPensionsRedirectsSpec extends UnitTest {

  private val schemeStartCall: Call = PensionSchemeDetailsController.show(taxYear, None)
  private val schemeSummaryCall: Call = UkPensionIncomeSummaryController.show(taxYear)
  private val checkYourAnswersCall: Call = UkPensionIncomeCYAController.show(taxYear)

    ".cyaPageCall" should {
    "return a redirect call to the cya page" in {
      cyaPageCall(taxYear) shouldBe checkYourAnswersCall
    }
  }

  ".redirectForSchemeLoop" should {
    "filter incomplete schemes and return a Call to the first page in scheme loop when 'schemes' is empty" in {
      val emptySchemes: Seq[UkPensionIncomeViewModel] = Seq.empty
      val incompleteSchemes: Seq[UkPensionIncomeViewModel] = Seq(
        UkPensionIncomeViewModel(amount = Some(211.33), taxPaid = Some(14.77)),
        UkPensionIncomeViewModel(startDate = Some("2019-07-23"), pensionSchemeName = Some("pension name 1"))
      )
      val result1 = redirectForSchemeLoop(emptySchemes, taxYear)
      val result2 = redirectForSchemeLoop(incompleteSchemes, taxYear)

      result1 shouldBe schemeStartCall
      result2 shouldBe schemeStartCall
    }
    "filter incomplete schemes and return a Call to the scheme summary page when 'schemes' already exist" in {
      val existingSchemes: Seq[UkPensionIncomeViewModel] = Seq(anUkPensionIncomeViewModelOne, anUkPensionIncomeViewModelTwo,
        UkPensionIncomeViewModel(amount = Some(211.33), taxPaid = Some(14.77)),
        UkPensionIncomeViewModel(startDate = Some("2019-07-23"), pensionSchemeName = Some("pension name 1"))
      )
      val result = redirectForSchemeLoop(existingSchemes, taxYear)

      result shouldBe schemeSummaryCall
    }
  }

}
