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

package config

import models.User
import models.mongo.{DatabaseError, PensionsUserData}
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.MockitoSugar
import org.mockito.stubbing.ScalaOngoingStubbing
import repositories.PensionsUserDataRepository
import repositories.PensionsUserDataRepository.QueryResult
import utils.UnitTest

import scala.concurrent.Future

trait MockPensionUserDataRepository extends UnitTest with MockitoSugar {

  val mockPensionUserDataRepository: PensionsUserDataRepository = mock[PensionsUserDataRepository]

  def mockFind(taxYear: Int,
               user: User,
               repositoryResponse: Either[DatabaseError, Option[PensionsUserData]]
              ):ScalaOngoingStubbing[QueryResult[Option[PensionsUserData]]] = {
    when(mockPensionUserDataRepository.find(eqTo(taxYear), eqTo(user)))
      .thenReturn(Future.successful(repositoryResponse))

  }

  def mockCreateOrUpdate(pensionUserData: PensionsUserData,
                         response: Either[DatabaseError, Unit]
                        ): ScalaOngoingStubbing[QueryResult[Unit]] = {
    when(mockPensionUserDataRepository.createOrUpdate(eqTo(pensionUserData)))
      .thenReturn(Future.successful(response))

  }

  def mockCreateOrUpdate(response: Either[DatabaseError, Unit]
                        ): ScalaOngoingStubbing[QueryResult[Unit]] = {
    when(mockPensionUserDataRepository.createOrUpdate(any[PensionsUserData]))
      .thenReturn(Future.successful(response))

  }

  def mockClear(taxYear: Int,
                user: User,
                response: Boolean
               ): ScalaOngoingStubbing[Future[Boolean]] = {
    when(mockPensionUserDataRepository.clear(eqTo(taxYear), eqTo(user)))
      .thenReturn(Future.successful(response))

  }

}
