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

package utils

import cats.Functor
import cats.data.EitherT
import models.mongo.ServiceError
import play.api.mvc.Result

import scala.concurrent.{ExecutionContext, Future}

object EitherTUtils {

  implicit class CasterOps[F[_]: Functor, A <: ServiceError, B](value: EitherT[F, A, B]) {
    def leftAs[T <: ServiceError]: EitherT[F, ServiceError, B] =
      value.leftMap(a => a: ServiceError)
  }

  implicit class ResultMergersOps[A](value: EitherT[Future, Result, A]) {
    def onSuccess(result: Result)(implicit ec: ExecutionContext): Future[Result] =
      value.map(_ => result).merge
  }

}
