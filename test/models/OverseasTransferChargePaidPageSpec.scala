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

package models

import builders.TransfersIntoOverseasPensionsViewModelBuilder.aTransfersIntoOverseasPensionsViewModel
import forms.FormsProvider
import models.pension.pages.OverseasTransferChargePaidPage
import utils.UnitTest

class OverseasTransferChargePaidPageSpec extends UnitTest {

  private val anyQuestionValue = true

  private val pageForm = new FormsProvider().overseasTransferChargePaidForm

  "OverseasTransferChargePaid.apply" should {
    "return page with pre-filled form when scheme has value existing value" in {

      val index   = Some(0)
      val schemes = aTransfersIntoOverseasPensionsViewModel.transferPensionScheme(index.get).copy(ukTransferCharge = Some(anyQuestionValue))
      val cya     = aTransfersIntoOverseasPensionsViewModel.copy(transferPensionScheme = Seq(schemes))

      OverseasTransferChargePaidPage.apply(taxYear, index, cya, pageForm) shouldBe OverseasTransferChargePaidPage(
        taxYear = taxYear,
        pensionSchemeIndex = index,
        form = pageForm.fill(value = anyQuestionValue)
      )
    }

    "return page without pre-filled form when no scheme exists" in {
      val index = None
      val cya   = aTransfersIntoOverseasPensionsViewModel

      OverseasTransferChargePaidPage.apply(taxYear, index, cya, pageForm) shouldBe OverseasTransferChargePaidPage(
        taxYear = taxYear,
        pensionSchemeIndex = index,
        form = pageForm
      )
    }

    "return page with pre-filled form with errors when form has errors" in {
      val index          = Some(0)
      val formWithErrors = pageForm.bind(Map("wrong-key" -> "wrong-value"))

      OverseasTransferChargePaidPage.apply(
        taxYear,
        index,
        aTransfersIntoOverseasPensionsViewModel,
        formWithErrors) shouldBe OverseasTransferChargePaidPage(
        taxYear = taxYear,
        pensionSchemeIndex = index,
        form = formWithErrors
      )
    }
  }
}
