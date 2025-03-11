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

package services

import models.audit.AuditModel
import org.mockito.ArgumentMatchers.{any, argThat}
import play.api.Configuration
import play.api.libs.json.Json
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import utils.UnitTestWithApp

import scala.concurrent.Future

class AuditServiceSpec extends UnitTestWithApp {

  private trait Test {

    val mockedAppName                      = "some-app-name"
    val mockAuditConnector: AuditConnector = mock[AuditConnector]
    val mockConfig: Configuration          = mock[Configuration]

    when(mockConfig.get(any[String])(any[play.api.ConfigLoader[String]]))
      .thenReturn(mockedAppName)

    lazy val target = new AuditService(mockAuditConnector, mockConfig)
  }

  "AuditService" when {
    "auditing an event" should {
      val auditType                     = "Type"
      val transactionName               = "Name"
      val eventDetails                  = "Details"
      val expected: Future[AuditResult] = Future.successful(Success)

      "return a successful audit result" in new Test {

        when(mockAuditConnector.sendExtendedEvent(any[ExtendedDataEvent])(any(), any()))
          .thenReturn(expected)

        private val event = AuditModel(auditType, transactionName, eventDetails)
        target.sendAudit(event) shouldBe expected
      }

      "generates an event with the correct auditSource" in new Test {
        when(mockAuditConnector.sendExtendedEvent(argThat((event: ExtendedDataEvent) => event.auditSource == mockedAppName))(any(), any()))
          .thenReturn(expected)

        private val event = AuditModel(auditType, transactionName, eventDetails)

        target.sendAudit(event)
      }

      "generates an event with the correct auditType" in new Test {
        when(mockAuditConnector.sendExtendedEvent(argThat((event: ExtendedDataEvent) => event.auditType == auditType))(any(), any()))
          .thenReturn(expected)

        private val event = AuditModel(auditType, transactionName, eventDetails)

        target.sendAudit(event)
      }

      "generates an event with the correct details" in new Test {
        when(mockAuditConnector.sendExtendedEvent(argThat((event: ExtendedDataEvent) => event.detail == Json.toJson(eventDetails)))(any(), any()))
          .thenReturn(expected)

        private val event = AuditModel(auditType, transactionName, eventDetails)

        target.sendAudit(event)
      }

      "generates an event with the correct transactionName" in new Test {
        when(mockAuditConnector.sendExtendedEvent(argThat((eventArg: ExtendedDataEvent) =>
          eventArg.tags.exists(tag => tag == "transactionName" -> transactionName)))(any(), any()))
          .thenReturn(expected)

        private val event = AuditModel(auditType, transactionName, eventDetails)

        target.sendAudit(event)
      }
    }
  }
}
