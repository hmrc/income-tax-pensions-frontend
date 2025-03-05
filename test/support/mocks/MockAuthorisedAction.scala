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

import common.{EnrolmentIdentifiers, EnrolmentKeys}
import config.{AppConfig, ErrorHandler}
import controllers.predicates.actions.AuthorisedAction
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.MockitoSugar
import org.mockito.stubbing.ScalaOngoingStubbing
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.Helpers.stubMessagesControllerComponents
import services.AuthService
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.syntax.retrieved.authSyntaxForRetrieved

import scala.concurrent.Future

trait MockAuthorisedAction extends MockitoSugar { this: GuiceOneAppPerSuite =>

  private val mockAppConfig                        = app.injector.instanceOf[AppConfig]
  private val testMockAuthConnector: AuthConnector = mock[AuthConnector]
  private val mockAuthService                      = new AuthService(testMockAuthConnector)
  private val mockErrorHandler: ErrorHandler       = mock[ErrorHandler]

  protected val mockAuthorisedAction: AuthorisedAction =
    new AuthorisedAction(mockAppConfig, mockErrorHandler)(mockAuthService, stubMessagesControllerComponents())

  protected val authorisedAction: AuthorisedAction =
    new AuthorisedAction(mockAppConfig, mockErrorHandler)(mockAuthService, stubMessagesControllerComponents())

  protected def mockAuthAsAgent(): ScalaOngoingStubbing[Future[Enrolments]] = {
    val enrolments: Enrolments = Enrolments(
      Set(
        Enrolment(EnrolmentKeys.Individual, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, "1234567890")), "Activated"),
        Enrolment(EnrolmentKeys.Agent, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.agentReference, "0987654321")), "Activated")
      ))

    val agentRetrievals: Some[AffinityGroup] = Some(AffinityGroup.Agent)

    when(testMockAuthConnector.authorise(any(), eqTo(Retrievals.affinityGroup))(any(), any()))
      .thenReturn(Future.successful(agentRetrievals))

//    (testMockAuthConnector
//      .authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
//      .expects(*, Retrievals.affinityGroup, *, *)
//      .returning(Future.successful(agentRetrievals))

    when(testMockAuthConnector.authorise(any(), eqTo(Retrievals.allEnrolments))(any(), any()))
      .thenReturn(Future.successful(enrolments))

//    (testMockAuthConnector
//      .authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
//      .expects(*, Retrievals.allEnrolments, *, *)
//      .returning(Future.successful(enrolments))
  }

  protected def mockAuthAsIndividual(nino: Option[String]) = {
    val enrolments = Enrolments(
      Set(
        Enrolment(EnrolmentKeys.Individual, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, "1234567890")), "Activated"),
        Enrolment(EnrolmentKeys.Agent, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.agentReference, "0987654321")), "Activated")
      ) ++ nino.fold(Seq.empty[Enrolment])(unwrappedNino =>
        Seq(Enrolment(EnrolmentKeys.nino, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.nino, unwrappedNino)), "Activated"))))

    when(testMockAuthConnector.authorise(any(), eqTo(Retrievals.affinityGroup))(any(), any()))
      .thenReturn(Future.successful(Some(AffinityGroup.Individual)))

//    (testMockAuthConnector
//      .authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
//      .expects(*, Retrievals.affinityGroup, *, *)
//      .returning(Future.successful(Some(AffinityGroup.Individual)))

    when(testMockAuthConnector.authorise(any(), eqTo(Retrievals.allEnrolments and Retrievals.confidenceLevel))(any(), any()))
      .thenReturn(Future.successful(enrolments and ConfidenceLevel.L250))

//    (testMockAuthConnector
//      .authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
//      .expects(*, Retrievals.allEnrolments and Retrievals.confidenceLevel, *, *)
//      .returning(Future.successful(enrolments and ConfidenceLevel.L250))
  }

  protected def mockAuthReturnException(exception: Exception): ScalaOngoingStubbing[Future[Nothing]] = {

    when(testMockAuthConnector.authorise(any(), any())(any(), any()))
      .thenReturn(Future.failed(exception))

    //    (testMockAuthConnector
    //      .authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
    //      .expects(*, *, *, *)
    //      .returning(Future.failed(exception))
  }

  protected def mockFailToAuthenticate(): ScalaOngoingStubbing[Future[Nothing]] = {

    when(testMockAuthConnector.authorise(any(), any())(any(), any()))
      .thenReturn(Future.failed(InsufficientConfidenceLevel()))

//    (testMockAuthConnector
//      .authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
//      .expects(*, *, *, *)
//      .returning(Future.failed(InsufficientConfidenceLevel()))
  }
}
