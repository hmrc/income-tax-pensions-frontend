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
import cats.implicits.catsSyntaxEitherId
import models.mongo.{DataNotFound, PensionsUserData, ServiceError}
import models.{APIErrorBodyModel, APIErrorModel, IncomeTaxUserData}
import play.api.http.Status.BAD_REQUEST
import utils.EitherTUtils.CasterOps
import utils.UnitTest

import scala.concurrent.Future

trait BaseServiceSpec extends UnitTest {

  type PriorAndSession = (IncomeTaxUserData, PensionsUserData)

  val apiError: APIErrorModel =
    APIErrorModel(BAD_REQUEST, APIErrorBodyModel("FAILED", "failed"))

  val notFoundResponse: EitherT[Future, ServiceError, PriorAndSession] =
    DataNotFound.asLeft[PriorAndSession].toEitherT.leftAs[ServiceError]

  val apiErrorResponse: EitherT[Future, ServiceError, PriorAndSession] =
    apiError.asLeft[PriorAndSession].toEitherT.leftAs[ServiceError]

}
