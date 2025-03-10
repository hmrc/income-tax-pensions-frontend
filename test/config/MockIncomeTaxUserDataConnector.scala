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

package config

import connectors.IncomeTaxUserDataConnector
import models.{APIErrorBodyModel, APIErrorModel, IncomeTaxUserData}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.INTERNAL_SERVER_ERROR
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait MockIncomeTaxUserDataConnector extends MockitoSugar {

  val apiError: APIErrorModel = APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError)

  val mockUserDataConnector: IncomeTaxUserDataConnector = mock[IncomeTaxUserDataConnector]

  def mockFind(
                nino: String,
                taxYear: Int,
                userData: IncomeTaxUserData
              ): Unit = {
    when(mockUserDataConnector.getUserData(
      eqTo(nino),
      eqTo(taxYear))(
      any[HeaderCarrier]
    )).thenReturn(Future.successful(Right(userData)))
  }

  def mockFindNoContent(
                         nino: String,
                         taxYear: Int
                       ): Unit = {
    when(mockUserDataConnector.getUserData(
      eqTo(nino),
      eqTo(taxYear))(
      any[HeaderCarrier]
    )).thenReturn(Future.successful(Right(IncomeTaxUserData())))
  }

  def mockFindFail(
                    nino: String,
                    taxYear: Int
                  ): Unit = {
    when(mockUserDataConnector.getUserData(
      eqTo(nino),
      eqTo(taxYear))(
      any[HeaderCarrier]
    )).thenReturn(Future.successful(Left(apiError)))
  }
}