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

import cats.data.EitherT
import cats.implicits.{catsSyntaxOptionId, none}
import models.mongo.DatabaseError
import play.api.mvc.Result

import scala.concurrent.{ExecutionContext, Future}

package object controllers {

  def validatedIndex(index: Option[Int], collectionSize: Int): Option[Int] =
    index.filter(i => i >= 0 && i < collectionSize)

  def validateIndex_old[A](index: Int, collection: Seq[A]): Option[Int] =
    if (index >= 0 && index < collection.size) index.some
    else none[Int]

  def validatedSchemes[T](index: Option[Int], listItem: Seq[T]): Either[Unit, Option[T]] =
    index match {
      case Some(value) if listItem.indices contains value => Right(Some(listItem(value)))
      case None                                           => Right(None)
      case _                                              => Left(())
    }

  def upsertSessionHandler(result: Future[Either[DatabaseError, Unit]])(ifSuccessful: Result, ifFailed: Result)(implicit
      ec: ExecutionContext): Future[Result] =
    EitherT(result).bimap(_ => ifFailed, _ => ifSuccessful).merge
}
