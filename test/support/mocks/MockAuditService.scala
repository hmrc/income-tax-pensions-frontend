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

package support.mocks

import models.audit.AuditModel
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.MockitoSugar
import org.mockito.stubbing.ScalaOngoingStubbing
import services.AuditService
import uk.gov.hmrc.play.audit.http.connector.AuditResult

import scala.concurrent.Future

trait MockAuditService extends MockitoSugar {

  val mockAuditService: AuditService = mock[AuditService]

  def mockAuditResult[T](event: AuditModel[T],
                         auditResult: Future[AuditResult]
                        ): ScalaOngoingStubbing[Future[AuditResult]] = {
    when(mockAuditService.sendAudit(eqTo(event))(any(), any(), any()))
      .thenReturn(auditResult)

  }
}

object MockAuditService {
  val mockedAuditSuccessResult: Future[AuditResult.Success.type] = Future.successful(AuditResult.Success)
  val mockedAuditFailureResult: Future[AuditResult.Failure] = Future.successful(AuditResult.Failure("Some audit send failure"))
}
