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
import controllers.pensions.overseasTransferCharges.FormsProvider
import models.pension.pages.OverseasTransferChargePaidPage
import utils.UnitTest


class OverseasTransferChargePaidPageSpec extends UnitTest {

  private val anyQuestionValue = true

  private val pageForm = new FormsProvider().overseasTransferChargePaidForm

  "OverseasTransferChargePaid.apply" should {
    "return page with pre-filled form when transfersIntoOverseas has value" in {

      val cya = aTransfersIntoOverseasPensionsViewModel.copy(transfersIntoOverseas = Some(anyQuestionValue))

      OverseasTransferChargePaidPage.apply(taxYear, cya, pageForm) shouldBe OverseasTransferChargePaidPage(
        taxYear = taxYear,
        form = pageForm.fill(value = anyQuestionValue)
      )
    }

    "return page without pre-filled form when endDateQuestion is None" in {
      val cya = aTransfersIntoOverseasPensionsViewModel.copy(transfersIntoOverseas = None)

      OverseasTransferChargePaidPage.apply(taxYear, cya, pageForm) shouldBe OverseasTransferChargePaidPage(
        taxYear = taxYear,
        form = pageForm,
      )
    }

    "return page with pre-filled form with errors when form has errors" in {
      val formWithErrors = pageForm.bind(Map("wrong-key" -> "wrong-value"))

      OverseasTransferChargePaidPage.apply(taxYear, aTransfersIntoOverseasPensionsViewModel, formWithErrors) shouldBe OverseasTransferChargePaidPage(
        taxYear = taxYear,
        form = formWithErrors
      )
    }
  }
}
