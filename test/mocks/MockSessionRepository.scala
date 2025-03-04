/*
 * Copyright 2025 HM Revenue & Customs
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

package mocks

import models.User
import models.mongo.{DatabaseError, PensionsUserData}
import org.scalamock.handlers.{CallHandler1, CallHandler2}
import org.scalamock.scalatest.MockFactory
import org.scalatest.TestSuite
import repositories.PensionsUserDataRepository
import utils.UnitTest

import scala.concurrent.Future

trait MockSessionRepository extends MockFactory with TestSuite {

  val mockSessionRepository: PensionsUserDataRepository = mock[PensionsUserDataRepository]

  object MockSessionRepository {

    def find(taxYear: Int, user: User): CallHandler2[Int, User, Future[Either[DatabaseError, Option[PensionsUserData]]]] =
      (mockSessionRepository
        .find(_: Int, _: User))
        .expects(taxYear, user)

    def createOrUpdate(pensionsUserData: PensionsUserData): CallHandler1[PensionsUserData, Future[Either[DatabaseError, Unit]]] =
      (mockSessionRepository
        .createOrUpdate(_: PensionsUserData))
        .expects(pensionsUserData)
  }

}
