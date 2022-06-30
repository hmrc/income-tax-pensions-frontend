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

package forms

import forms.validation.mappings.MappingUtil.currency
import play.api.data.Forms.{of, tuple}
import play.api.data.format.Formatter
import play.api.data.{Form, FormError}

object RadioButtonAmountForm {

  val yesNo = "value"
  val yes = "true"
  val no = "false"

  def formatter(missingInputError: String): Formatter[Boolean] = new Formatter[Boolean] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Boolean] = {
      data.get(key) match {
        case Some(`yes`) => Right(true)
        case Some(`no`) => Right(false)
        case _ => Left(Seq(FormError(key, missingInputError)))
      }
    }

    override def unbind(key: String, value: Boolean): Map[String, String] = {
      Map(
        key -> value.toString
      )
    }
  }


  val amount2 = "amount-2"

  def radioButtonAndAmountForm(missingInputError: String,
                               emptyFieldKey: String,
                               wrongFormatKey: String = "common.error.invalid_currency_format",
                               exceedsMaxAmountKey: String = "common.error.amountMaxLimit",
                               emptyFieldArguments: Seq[String] = Seq.empty[String]
                ): Form[(Boolean, BigDecimal)] =
    Form(
      tuple(
        yesNo -> of(formatter(missingInputError)),

        amount2 -> currency(
          requiredKey = emptyFieldKey,
          wrongFormatKey = wrongFormatKey,
          maxAmountKey = exceedsMaxAmountKey,
          args = emptyFieldArguments
        )
      )
    )

}
