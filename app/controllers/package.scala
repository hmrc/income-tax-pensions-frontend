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

import models.pension.Journey
import play.api.mvc.Result
import play.api.mvc.Results.Redirect

package object controllers {

  def validatedIndex(index: Option[Int], collectionSize: Int): Option[Int] =
    index.filter(i => i >= 0 && i < collectionSize)

  def validatedSchemes[T](index: Option[Int], listItem: Seq[T]): Either[Unit, Option[T]] =
    index match {
      case Some(value) if listItem.indices contains value => Right(Some(listItem(value)))
      case None                                           => Right(None)
      case _                                              => Left(())
    }

  def redirectToSectionCompletedPage(taxYear: Int, journey: Journey): Result = Redirect(
    controllers.pensions.routes.SectionCompletedStateController.show(taxYear, journey.toString)
  )
}
