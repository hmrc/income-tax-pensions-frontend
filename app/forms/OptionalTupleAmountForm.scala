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

package forms

import forms.validation.mappings.MappingUtil._
import play.api.data.Form
import play.api.data.Forms.tuple


object OptionalTupleAmountForm {

  val amount = "amount-1"
  val amount2 = "amount-2"

  case class OptionalTupleAmountFormErrorMessage(
                                                   emptyFieldKey1: String,
                                                   wrongFormatKey1: String = "common.error.invalid_currency_format",
                                                   exceedsMaxAmountKey1: String = "common.error.amountMaxLimit",
                                                   emptyFieldArguments1: Seq[String] = Seq.empty[String],
                                                   emptyFieldKey2: String,
                                                   wrongFormatKey2: String = "common.error.invalid_currency_format",
                                                   exceedsMaxAmountKey2: String = "common.error.amountMaxLimit",
                                                   emptyFieldArguments2: Seq[String] = Seq.empty[String],
                                                   taxPaidLessThanAmountBeforeTaxErrorMessage : String = ""
                                                 )

  def amountForm(
                  optionalTupleAmountFormErrorMessage: OptionalTupleAmountFormErrorMessage
                ): Form[(Option[BigDecimal], Option[BigDecimal])] = {

    val amountMappingTuple = tuple(
      amount -> optionCurrency(
        requiredKey = optionalTupleAmountFormErrorMessage.emptyFieldKey1,
        wrongFormatKey = optionalTupleAmountFormErrorMessage.wrongFormatKey1,
        maxAmountKey = optionalTupleAmountFormErrorMessage.exceedsMaxAmountKey1,
        args = optionalTupleAmountFormErrorMessage.emptyFieldArguments1

      ),
      amount2 -> optionCurrency(
        requiredKey = optionalTupleAmountFormErrorMessage.emptyFieldKey2,
        wrongFormatKey = optionalTupleAmountFormErrorMessage.wrongFormatKey2,
        maxAmountKey = optionalTupleAmountFormErrorMessage.exceedsMaxAmountKey2,
        args = optionalTupleAmountFormErrorMessage.emptyFieldArguments2
      )
    )

    Form(if (optionalTupleAmountFormErrorMessage.taxPaidLessThanAmountBeforeTaxErrorMessage == "") amountMappingTuple else amountMappingTuple.verifying(
      optionalTupleAmountFormErrorMessage.taxPaidLessThanAmountBeforeTaxErrorMessage,
      amountsTuple => amountsTuple._2.getOrElse(BigDecimal(0)) <= amountsTuple._1.getOrElse(BigDecimal(0)))
    )
  }

}
