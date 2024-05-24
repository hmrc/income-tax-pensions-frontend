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

package models.pension.pages

import models.pension.charges.PaymentsIntoOverseasPensionsViewModel
import play.api.data.Form

case class UntaxedEmployerPayments(taxYear: Int, pensionSchemeIndex: Option[Int], form: Form[BigDecimal])

object UntaxedEmployerPayments {

  def apply(taxYear: Int,
            pensionSchemeIndex: Option[Int],
            dataModel: PaymentsIntoOverseasPensionsViewModel,
            form: Form[BigDecimal]): UntaxedEmployerPayments = {

    val optQuestionValue: Option[BigDecimal] = pensionSchemeIndex match {
      case Some(value) => dataModel.schemes(value).employerPaymentsAmount
      case None        => None
    }

    UntaxedEmployerPayments(
      taxYear = taxYear,
      pensionSchemeIndex = pensionSchemeIndex,
      form = optQuestionValue.fold(form)(questionValue => if (form.hasErrors) form else form.fill(questionValue))
    )
  }
}
