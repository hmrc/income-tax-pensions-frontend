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

import connectors.{DownstreamOutcome, IncomeTaxUserDataConnector}
import models.IncomeTaxUserData
import org.mockito.scalatest.MockitoSugar
import org.mockito.stubbing.ScalaOngoingStubbing
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait MockSubmissionsConnector extends MockitoSugar {

  val mockSubmissionsConnector: IncomeTaxUserDataConnector = mock[IncomeTaxUserDataConnector]

  object MockSubmissionsConnector {

    def getUserData(nino: String,
                    taxYear: Int
                   ): ScalaOngoingStubbing[DownstreamOutcome[IncomeTaxUserData]] = {

      when(mockSubmissionsConnector.getUserData(eqTo(nino), eqTo(taxYear))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(IncomeTaxUserData())))

      //      (mockSubmissionsConnector
      //        .getUserData(_: String, _: Int)(_: HeaderCarrier))
      //        .expects(nino, taxYear, *)
    }

    def refreshPensionsResponse(nino: String,
                                mtditid: String,
                                taxYear: Int
                               ): ScalaOngoingStubbing[DownstreamOutcome[Unit]] = {


      when(mockSubmissionsConnector.refreshPensionsResponse(eqTo(nino), eqTo(mtditid), eqTo(taxYear))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(())))

      //      (mockSubmissionsConnector
      //        .refreshPensionsResponse(_: String, _: String, _: Int)(_: HeaderCarrier))
      //        .expects(nino, mtditid, taxYear, *)
    }

  }

}
