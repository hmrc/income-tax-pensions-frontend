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

package services

import cats.data.EitherT
import config.ErrorHandler
import models.mongo.PensionsUserData
import play.api.mvc.{Request, Result}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ShortServiceRefundsService @Inject() (service: PensionSessionService, errorHandler: ErrorHandler) {

  def upsertSession(session: PensionsUserData)(implicit ec: ExecutionContext, request: Request[_]): EitherT[Future, Result, Unit] =
    EitherT(service.createOrUpdateSession(session))
      .leftMap(_ => errorHandler.internalServerError())

}
