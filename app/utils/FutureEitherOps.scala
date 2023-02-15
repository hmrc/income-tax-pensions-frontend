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

package utils

import models.APIErrorModel
import models.mongo.ServiceError
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

case class FutureEitherOps[E <: ServiceError, R](value: Future[Either[E, R]])(implicit ec: ExecutionContext, hc: HeaderCarrier){

  def map[B](mappingFunction: R => B): FutureEitherOps[E, B] = {
    FutureEitherOps(value.map {
      case Right(mappedValue) => Right(mappingFunction(mappedValue))
      case Left(error) => Left(error)
    })
  }

  def flatMap[O](mappingFunction: R => FutureEitherOps[E, O]): FutureEitherOps[E, O] = FutureEitherOps(value.flatMap{
    case Right(currentRight) => mappingFunction(currentRight).value.map {
      case Right(nextRight) => Right(nextRight)
      case left@Left(_) => left
    }
    case Left(currentLeft) => Future.successful(Left(currentLeft))
  })
}
