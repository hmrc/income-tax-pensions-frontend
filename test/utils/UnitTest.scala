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

package utils

import builders.PensionsCYAModelBuilder.emptyPensionsData
import cats.data.EitherT
import com.codahale.metrics.SharedMetricRegistries
import common.{EnrolmentIdentifiers, EnrolmentKeys, SessionValues}
import config.{AppConfig, ErrorHandler}
import controllers.predicates.actions.AuthorisedAction
import models.mongo.PensionsUserData
import models.{AuthorisationRequest, User}
import org.apache.pekko.actor.ActorSystem
import org.scalamock.handlers.CallHandler4
import org.scalamock.scalatest.MockFactory
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.mvc._
import play.api.test.{FakeRequest, Helpers}
import services.AuthService
import support.mocks.MockErrorHandler
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.syntax.retrieved.authSyntaxForRetrieved
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import views.html.templates.AgentAuthErrorPageView

import java.time.{Clock, ZoneOffset, ZonedDateTime}
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Awaitable, ExecutionContext, Future}

trait UnitTest
    extends AnyWordSpec
    with Matchers
    with MockFactory
    with BeforeAndAfterEach
    with GuiceOneAppPerSuite
    with TestTaxYearHelper
    with MockErrorHandler {

  class TestWithAuth(isAgent: Boolean = false, nino: Option[String] = Some("AA123456A")) {
    if (isAgent) mockAuthAsAgent() else mockAuth(nino)
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    SharedMetricRegistries.clear()
  }

  implicit val actorSystem: ActorSystem = ActorSystem()

  def await[T](awaitable: Awaitable[T]): T = Await.result(awaitable, Duration.Inf)

  val sessionId: String = "eb3158c2-0aff-4ce8-8d1b-f2208ace52fe"

  val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withHeaders("X-Session-ID" -> sessionId)
  val fakeRequestWithMtditidAndNino: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
    .withSession(
      SessionValues.CLIENT_MTDITID  -> "1234567890",
      SessionValues.CLIENT_NINO     -> "AA123456A",
      SessionValues.TAX_YEAR        -> s"$taxYear",
      SessionValues.VALID_TAX_YEARS -> validTaxYearList.mkString(",")
    )
    .withHeaders("X-Session-ID" -> sessionId)
  val fakeRequestWithNino: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(
    SessionValues.CLIENT_NINO     -> "AA123456A",
    SessionValues.VALID_TAX_YEARS -> validTaxYearList.mkString(",")
  )
  implicit val headerCarrierWithSession: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(sessionId)))
  val emptyHeaderCarrier: HeaderCarrier                = HeaderCarrier()

  implicit val mockAppConfig: AppConfig                       = app.injector.instanceOf[AppConfig]
  implicit val mockControllerComponents: ControllerComponents = Helpers.stubControllerComponents()
  implicit val mockExecutionContext: ExecutionContext         = ExecutionContext.Implicits.global
  implicit val mockAuthConnector: AuthConnector               = mock[AuthConnector]
  implicit val mockAuthService: AuthService                   = new AuthService(mockAuthConnector)
  val agentAuthErrorPageView: AgentAuthErrorPageView          = app.injector.instanceOf[AgentAuthErrorPageView]

  implicit lazy val mockMessagesControllerComponents: MessagesControllerComponents = Helpers.stubMessagesControllerComponents()
  implicit lazy val authorisationRequest: AuthorisationRequest[AnyContent] =
    new AuthorisationRequest[AnyContent](User("1234567890", None, "AA123456A", sessionId, AffinityGroup.Individual.toString), fakeRequest)

  val authorisedAction = new AuthorisedAction(mockAppConfig, mockErrorHandler)(mockAuthService, stubMessagesControllerComponents())
  def status(awaitable: Future[Result]): Int = await(awaitable).header.status

  def bodyOf(awaitable: Future[Result]): String = {
    val awaited = await(awaitable)
    await(awaited.body.consumeData.map(_.utf8String))
  }

  def redirectUrl(awaitable: Future[Result]): String =
    await(awaitable).header.headers.getOrElse("Location", "/")

  def getSession(awaitable: Future[Result]): Session =
    await(awaitable).session

  // noinspection ScalaStyle
  def mockAuth(nino: Option[String]): CallHandler4[Predicate, Retrieval[_], HeaderCarrier, ExecutionContext, Future[Any]] = {
    val enrolments = Enrolments(
      Set(
        Enrolment(EnrolmentKeys.Individual, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, "1234567890")), "Activated"),
        Enrolment(EnrolmentKeys.Agent, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.agentReference, "0987654321")), "Activated")
      ) ++ nino.fold(Seq.empty[Enrolment])(unwrappedNino =>
        Seq(Enrolment(EnrolmentKeys.nino, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.nino, unwrappedNino)), "Activated"))))

    (mockAuthConnector
      .authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, Retrievals.affinityGroup, *, *)
      .returning(Future.successful(Some(AffinityGroup.Individual)))

    (mockAuthConnector
      .authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, Retrievals.allEnrolments and Retrievals.confidenceLevel, *, *)
      .returning(Future.successful(enrolments and ConfidenceLevel.L250))
  }

  // noinspection ScalaStyle
  def mockAuthAsAgent(): CallHandler4[Predicate, Retrieval[_], HeaderCarrier, ExecutionContext, Future[Any]] = {
    val enrolments: Enrolments = Enrolments(
      Set(
        Enrolment(EnrolmentKeys.Individual, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, "1234567890")), "Activated"),
        Enrolment(EnrolmentKeys.Agent, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.agentReference, "0987654321")), "Activated")
      ))

    val agentRetrievals: Some[AffinityGroup] = Some(AffinityGroup.Agent)

    (mockAuthConnector
      .authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, Retrievals.affinityGroup, *, *)
      .returning(Future.successful(agentRetrievals))

    (mockAuthConnector
      .authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, Retrievals.allEnrolments, *, *)
      .returning(Future.successful(enrolments))
  }

  // noinspection ScalaStyle
  def mockAuthReturnException(exception: Exception): CallHandler4[Predicate, Retrieval[_], HeaderCarrier, ExecutionContext, Future[Any]] =
    (mockAuthConnector
      .authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, *, *, *)
      .returning(Future.failed(exception))

  val nino       = "AA123456A"
  val mtditid    = "1234567890"
  val user: User = authorisationRequest.user

  def emptySessionData(updateTime: Option[ZonedDateTime] = None): PensionsUserData =
    PensionsUserData(
      sessionId,
      "1234567890",
      nino,
      taxYear,
      isPriorSubmission = true,
      emptyPensionsData,
      updateTime.getOrElse(Clock.systemUTC().instant().atZone(ZoneOffset.UTC)))

  implicit class ToFutureOps[A](value: A) {
    def asFuture: Future[A] = Future.successful(value)
  }
  implicit class ToEitherTOps[A, B](value: Either[A, B]) {
    def toEitherT: EitherT[Future, A, B] = EitherT.fromEither[Future](value)
  }
}
