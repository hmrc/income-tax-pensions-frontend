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

package models.pension

import controllers.pensions
import enumeratum._
import play.api.mvc.Results.Redirect
import play.api.mvc.{PathBindable, Result}

sealed abstract class Journey(override val entryName: String) extends EnumEntry {
  override def toString: String = entryName
  def sectionCompletedRedirect(taxYear: Int): Result
}

object Journey extends Enum[Journey] with utils.PlayJsonEnum[Journey] {
  val values: IndexedSeq[Journey] = findValues

  implicit def pathBindable(implicit strBinder: PathBindable[String]): PathBindable[Journey] = new PathBindable[Journey] {

    override def bind(key: String, value: String): Either[String, Journey] =
      strBinder.bind(key, value).flatMap { stringValue =>
        Journey.withNameOption(stringValue) match {
          case Some(journeyName) => Right(journeyName)
          case None              => Left(s"Invalid journey name: $stringValue")
        }
      }

    override def unbind(key: String, journeyName: Journey): String =
      strBinder.unbind(key, journeyName.entryName)
  }

  case object PaymentsIntoPensions extends Journey("payments-into-pensions") {
    def sectionCompletedRedirect(taxYear: Int): Result = Redirect(pensions.routes.PensionsSummaryController.show(taxYear))
  }
}
