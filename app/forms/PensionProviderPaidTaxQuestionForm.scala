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

import play.api.data.Forms._
import play.api.data._
import play.api.data.format.Formatter

sealed trait PensionProviderPaidTaxAnswers

case object Yes extends PensionProviderPaidTaxAnswers
case object No extends PensionProviderPaidTaxAnswers
case object NoButHasAgreedToPay extends PensionProviderPaidTaxAnswers

object PensionProviderPaidTaxQuestionForm {

  val yesNo = "value"
  val yes = "Yes"
  val no = "No"
  val noHasAgreedToPay = "NoButHasAgreedToPay"

  def formatter(missingInputError: String): Formatter[PensionProviderPaidTaxAnswers] = new Formatter[PensionProviderPaidTaxAnswers] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], PensionProviderPaidTaxAnswers] = {
      data.get(key) match {
        case Some(`yes`) => Right(Yes)
        case Some(`no`) => Right(No)
        case Some(`noHasAgreedToPay`) => Right(NoButHasAgreedToPay)
        case _ => Left(Seq(FormError(key, missingInputError)))
      }
    }

    override def unbind(key: String, value: PensionProviderPaidTaxAnswers): Map[String, String] = {
      Map(
        key -> value.toString
      )
    }
  }

  def yesNoForm(missingInputError: String): Form[PensionProviderPaidTaxAnswers] = Form(
    single(
      yesNo -> of(formatter(missingInputError))
    )
  )

}
