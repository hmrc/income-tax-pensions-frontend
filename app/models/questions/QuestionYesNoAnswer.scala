/*
 * Copyright 2024 HM Revenue & Customs
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

package models.questions

import QuestionYesNoAnswer._

sealed trait QuestionYesNoAnswer {
  def isYes =
    this match {
      case Yes(_)          => true
      case No | NotDefined => false
    }

  def toBooleanOpt: Option[Boolean] =
    this match {
      case Yes(v)     => Some(true)
      case No         => Some(false)
      case NotDefined => None
    }

  def toBigDecimalOpt: Option[BigDecimal] =
    this match {
      case Yes(v)     => Some(v)
      case No         => None
      case NotDefined => None
    }

}

object QuestionYesNoAnswer {
  final case class Yes private (value: BigDecimal) extends QuestionYesNoAnswer
  case object No                                   extends QuestionYesNoAnswer
  case object NotDefined                           extends QuestionYesNoAnswer

  def apply(value: Option[BigDecimal]): QuestionYesNoAnswer =
    value
      .map { v =>
        if (v == 0.0) No
        else Yes(v)
      }
      .getOrElse(NotDefined)
}
