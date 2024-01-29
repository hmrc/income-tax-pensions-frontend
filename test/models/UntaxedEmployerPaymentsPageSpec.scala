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

import builders.PaymentsIntoOverseasPensionsViewModelBuilder.aPaymentsIntoOverseasPensionsViewModel
import forms.FormsProvider
import models.pension.pages.UntaxedEmployerPayments
import utils.UnitTest

class UntaxedEmployerPaymentsPageSpec extends UnitTest {

  private val anyQuestionValue = 1000

  private val pageForm = new FormsProvider().untaxedEmployerPayments(false)

  "UntaxedEmployerPayments.apply" should {
    "return page with pre-filled form when scheme has value existing value" in {

      val index   = Some(0)
      val reliefs = aPaymentsIntoOverseasPensionsViewModel.reliefs(index.get).copy(employerPaymentsAmount = Some(anyQuestionValue))
      val cya     = aPaymentsIntoOverseasPensionsViewModel.copy(reliefs = Seq(reliefs))

      UntaxedEmployerPayments.apply(taxYear, index, cya, pageForm) shouldBe UntaxedEmployerPayments(
        taxYear = taxYear,
        pensionSchemeIndex = index,
        form = pageForm.fill(value = anyQuestionValue)
      )
    }

    "return page with pre-filled form with errors when form has errors" in {
      val index          = Some(0)
      val formWithErrors = pageForm.bind(Map("wrong-key" -> "wrong-value"))

      UntaxedEmployerPayments.apply(taxYear, index, aPaymentsIntoOverseasPensionsViewModel, formWithErrors) shouldBe UntaxedEmployerPayments(
        taxYear = taxYear,
        pensionSchemeIndex = index,
        form = formWithErrors
      )
    }
  }
}
