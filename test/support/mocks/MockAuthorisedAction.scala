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

package support.mocks

import common.{EnrolmentIdentifiers, EnrolmentKeys}
import config.{AppConfig, ErrorHandler}
import controllers.predicates.actions.AuthorisedAction
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.Helpers.stubMessagesControllerComponents
import services.AuthService
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.syntax.retrieved.authSyntaxForRetrieved
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait MockAuthorisedAction extends MockitoSugar with GuiceOneAppPerSuite {

  private val mockAppConfig: AppConfig = app.injector.instanceOf[AppConfig]
  private val testMockAuthConnector: AuthConnector = mock[AuthConnector]
  private val mockAuthService: AuthService = new AuthService(testMockAuthConnector)
  private val mockErrorHandler: ErrorHandler = mock[ErrorHandler]

  protected val mockAuthorisedAction: AuthorisedAction =
    new AuthorisedAction(mockAppConfig, mockErrorHandler)(mockAuthService, stubMessagesControllerComponents())
  protected val authorisedAction: AuthorisedAction =
    new AuthorisedAction(mockAppConfig, mockErrorHandler)(mockAuthService, stubMessagesControllerComponents())

  protected def mockAuthAsAgent(): Unit = {
    val enrolments: Enrolments = Enrolments(
      Set(
        Enrolment(EnrolmentKeys.Individual, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, "1234567890")), "Activated"),
        Enrolment(EnrolmentKeys.Agent, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.agentReference, "0987654321")), "Activated")
      )
    )

    val agentRetrievals: Some[AffinityGroup] = Some(AffinityGroup.Agent)

    when(testMockAuthConnector.authorise(any[Predicate], eqTo(Retrievals.affinityGroup))(any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(Future.successful(agentRetrievals))

    when(testMockAuthConnector.authorise(any[Predicate], eqTo(Retrievals.allEnrolments))(any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(Future.successful(enrolments))
  }

  protected def mockAuth(nino: Option[String]): Unit = mockAuthAsIndividual(nino)

  protected def mockAuthAsIndividual(nino: Option[String]): Unit = {
    val enrolments = Enrolments(
      Set(
        Enrolment(EnrolmentKeys.Individual, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, "1234567890")), "Activated"),
        Enrolment(EnrolmentKeys.Agent, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.agentReference, "0987654321")), "Activated")
      ) ++ nino.fold(Seq.empty[Enrolment])(unwrappedNino =>
        Seq(Enrolment(EnrolmentKeys.nino, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.nino, unwrappedNino)), "Activated"))
      )
    )

    when(testMockAuthConnector.authorise(any[Predicate], eqTo(Retrievals.affinityGroup))(any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(Future.successful(Some(AffinityGroup.Individual)))

    when(testMockAuthConnector.authorise(any[Predicate], eqTo(Retrievals.allEnrolments and Retrievals.confidenceLevel))(any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(Future.successful(enrolments and ConfidenceLevel.L250))
  }

  protected def mockAuthReturnException(exception: Exception): Unit = {
    when(testMockAuthConnector.authorise(any[Predicate], any[Retrieval[_]])(any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(Future.failed(exception))
  }

  protected def mockFailToAuthenticate(): Unit = {
    when(testMockAuthConnector.authorise(any[Predicate], any[Retrieval[_]])(any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(Future.failed(InsufficientConfidenceLevel()))
  }
}