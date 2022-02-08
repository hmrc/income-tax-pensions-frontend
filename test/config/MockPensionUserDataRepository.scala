/*
 * Copyright 2022 HM Revenue & Customs
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


package config

import models.mongo.{DatabaseError, PensionsUserData}
import org.scalamock.handlers.{CallHandler2, CallHandler3}
import org.scalamock.scalatest.MockFactory
import repositories.PensionsUserDataRepository
import models.User


import scala.concurrent.Future

trait MockPensionUserDataRepository extends MockFactory {

  val mockPensionUserDataRepository: PensionsUserDataRepository = mock[PensionsUserDataRepository]

  def mockFind(taxYear: Int,
               repositoryResponse: Either[DatabaseError, Option[PensionsUserData]]
              ): CallHandler2[Int, User[_], Future[Either[DatabaseError, Option[PensionsUserData]]]] = {
    (mockPensionUserDataRepository.find(_: Int)(_: User[_]))
      .expects(taxYear, *)
      .returns(Future.successful(repositoryResponse))
      .anyNumberOfTimes()
  }

  def mockCreateOrUpdate(PensionUserData: PensionsUserData,
                         response: Either[DatabaseError, Unit]): CallHandler2[PensionsUserData, User[_], Future[Either[DatabaseError, Unit]]] = {
    (mockPensionUserDataRepository.createOrUpdate(_: PensionsUserData)(_: User[_]))
      .expects(PensionUserData, *)
      .returns(Future.successful(response))
      .anyNumberOfTimes()
  }

  def mockClear(taxYear: Int, response: Boolean): Unit = {
    (mockPensionUserDataRepository.clear(_: Int)(_: User[_]))
      .expects(taxYear, *)
      .returns(Future.successful(response))
      .anyNumberOfTimes()
  }


}
