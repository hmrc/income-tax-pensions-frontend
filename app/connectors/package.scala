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
import models.APIErrorModel

import scala.concurrent.Future

package object connectors {
  type DownstreamErrorOr[A] = Either[APIErrorModel, A]

  type DownstreamOutcome[A]  = Future[Either[APIErrorModel, A]]
  type DownstreamOutcomeT[A] = EitherT[Future, APIErrorModel, A]

  type ServiceError       = APIErrorModel
  type ContentResponse[A] = Either[ServiceError, A]
  type NoContentResponse  = ContentResponse[Unit]

  def isSuccess(status: Int): Boolean = status >= 200 && status <= 299

  def isNoContent(status: Int): Boolean = status == 204

}
