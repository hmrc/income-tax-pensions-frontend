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

import play.api.data.Forms.of
import play.api.data.format.Formatter
import play.api.data.{Form, FormError}

object RadioButtonForm {

  val value = "value"

  def formatter(missingInputError: String, radioButtonOptions: Seq[String]): Formatter[String] = new Formatter[String] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], String] = {
      data.get(key) match {
        case Some(res) => if (radioButtonOptions.contains(res)) Right(res) else Left(Seq(FormError(key, missingInputError)))
        case _ => Left(Seq(FormError(key, missingInputError)))
      }
    }

    override def unbind(key: String, value: String): Map[String, String] = Map(key -> value)
  }

  def radioButtonForm(missingInputError: String, radioButtonOptions: Seq[String]): Form[String] = {
    Form(value -> of(formatter(missingInputError, radioButtonOptions)))
  }

  }
