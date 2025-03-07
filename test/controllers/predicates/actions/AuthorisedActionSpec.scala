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

package controllers.predicates.actions

import common.{EnrolmentIdentifiers, EnrolmentKeys}
import models.AuthorisationRequest
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.stubbing.ScalaOngoingStubbing
import play.api.http.Status._
import play.api.mvc.Results._
import play.api.mvc.{AnyContent, Result}
import support.mocks.MockErrorHandler
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{allEnrolments, confidenceLevel}
import uk.gov.hmrc.auth.core.syntax.retrieved.authSyntaxForRetrieved
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import utils.UnitTest

import scala.concurrent.Future

class AuthorisedActionSpec extends UnitTest {

  val viewAndChangeUrl: String = "/report-quarterly/income-and-expenses/view/agents/client-utr"

  val mtdItId: String = "1234567890"
  val arn: String     = "0987654321"

  trait AgentTest extends MockErrorHandler {
    val baseUrl = "/update-and-submit-income-tax-return/pensions"

    val validHeaderCarrier: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionId")))

    val testBlock: AuthorisationRequest[AnyContent] => Future[Result] = user => Future.successful(Ok(s"${user.user.mtditid} ${user.user.arn.get}"))

    val viewAndChangeUrl: String = "/report-quarterly/income-and-expenses/view/agents/client-utr"
    val signInUrl: String        = s"$baseUrl/signIn"

    def primaryAgentPredicate(mtdId: String): Predicate =
      Enrolment("HMRC-MTD-IT")
        .withIdentifier("MTDITID", mtdId)
        .withDelegatedAuthRule("mtd-it-auth")

    def secondaryAgentPredicate(mtdId: String): Predicate =
      Enrolment("HMRC-MTD-IT-SUPP")
        .withIdentifier("MTDITID", mtdId)
        .withDelegatedAuthRule("mtd-it-auth-supp")

    def mockMultipleAgentsSwitch(bool: Boolean): ScalaOngoingStubbing[Boolean] =
      when(mockAppConfig.emaSupportingAgentsEnabled)
        .thenReturn(bool)

    val primaryAgentEnrolment: Enrolments = Enrolments(
      Set(
        Enrolment(EnrolmentKeys.Individual, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, mtdItId)), "Activated"),
        Enrolment(EnrolmentKeys.Agent, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.agentReference, arn)), "Activated")
      ))

    val supportingAgentEnrolment: Enrolments = Enrolments(
      Set(
        Enrolment(EnrolmentKeys.SupportingAgent, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, mtdItId)), "Activated"),
        Enrolment(EnrolmentKeys.Agent, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.agentReference, arn)), "Activated")
      ))

    def mockAuthReturnException(exception: Exception, predicate: Predicate): ScalaOngoingStubbing[Future[Nothing]] =
      when(mockAuthConnector.authorise(eqTo(predicate), any())(any(), any()))
        .thenReturn(Future.failed(exception))

    def mockAuthReturn(enrolments: Enrolments, predicate: Predicate): ScalaOngoingStubbing[Future[Any]] =
      when(mockAuthConnector.authorise(eqTo(predicate), any[Retrieval[Any]])(any(), any()))
        .thenReturn(Future.successful(enrolments))

    def mockSignInUrl(): ScalaOngoingStubbing[String] =
      when(mockAppConfig.signInUrl)
        .thenReturn(signInUrl)

    def mockViewAndChangeUrl(): ScalaOngoingStubbing[String] =
      when(mockAppConfig.viewAndChangeEnterUtrUrl)
        .thenReturn(viewAndChangeUrl)

//    def testAuth: AuthorisedAction = {
//      mockViewAndChangeUrl()
//      mockSignInUrl()

//      new AuthorisedAction(
//        appConfig = mockAppConfig,
//        mockErrorHandler
//      )(
//        mockAuthService,
//        mcc = stubMessagesControllerComponents()
//      )
//    }
  }

  ".enrolmentGetIdentifierValue" should {

    "return the value for the given identifier" in {
      val returnValue      = "anIdentifierValue"
      val returnValueAgent = "anAgentIdentifierValue"

      val enrolments = Enrolments(
        Set(
          Enrolment(EnrolmentKeys.Individual, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, returnValue)), "Activated"),
          Enrolment(EnrolmentKeys.Agent, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.agentReference, returnValueAgent)), "Activated")
        ))

      authorisedAction.enrolmentGetIdentifierValue(EnrolmentKeys.Individual, EnrolmentIdentifiers.individualId, enrolments) shouldBe Some(returnValue)
      authorisedAction.enrolmentGetIdentifierValue(EnrolmentKeys.Agent, EnrolmentIdentifiers.agentReference, enrolments) shouldBe Some(returnValueAgent)
    }

    "return a None" when {
      val key           = "someKey"
      val identifierKey = "anIdentifier"
      val returnValue   = "anIdentifierValue"

      val enrolments = Enrolments(Set(Enrolment(key, Seq(EnrolmentIdentifier(identifierKey, returnValue)), "someState")))

      "the given identifier cannot be found" in {
        authorisedAction.enrolmentGetIdentifierValue(key, "someOtherIdentifier", enrolments) shouldBe None
      }

      "the given key cannot be found" in {
        authorisedAction.enrolmentGetIdentifierValue("someOtherKey", identifierKey, enrolments) shouldBe None
      }

    }

  }

  ".individualAuthentication" should {

    "perform the block action" when {

      "the correct enrolment exist" which {
        val block: AuthorisationRequest[AnyContent] => Future[Result] = request => Future.successful(Ok(request.user.mtditid))
        val mtditid                                                   = "AAAAAA"
        val enrolments = Enrolments(
          Set(
            Enrolment(EnrolmentKeys.Individual, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, mtditid)), "Activated"),
            Enrolment(EnrolmentKeys.nino, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.nino, nino)), "Activated")
          ))

        lazy val result: Future[Result] = {

          when(mockAuthConnector.authorise(any(), eqTo(allEnrolments and confidenceLevel))(any(), any()))
            .thenReturn(Future.successful(enrolments and ConfidenceLevel.L250))

          authorisedAction.individualAuthentication[AnyContent](block, AffinityGroup.Individual)(fakeRequest, headerCarrierWithSession)
        }

        "returns an OK status" in {
          status(result) shouldBe OK
        }

        "returns a body of the mtditid" in {
          bodyOf(result) shouldBe mtditid
        }
      }

    }

    "return a redirect" when {

      "the session id does not exist in the headers" which {
        val block: AuthorisationRequest[AnyContent] => Future[Result] = request => Future.successful(Ok(request.user.mtditid))
        val mtditid                                                   = "AAAAAA"
        val enrolments = Enrolments(
          Set(
            Enrolment(EnrolmentKeys.Individual, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, mtditid)), "Activated"),
            Enrolment(EnrolmentKeys.nino, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.nino, nino)), "Activated")
          ))

        lazy val result: Future[Result] = {
          when(mockAuthConnector.authorise(any(), eqTo(allEnrolments and confidenceLevel))(any(), any()))
            .thenReturn(Future.successful(enrolments and ConfidenceLevel.L250))

          authorisedAction.individualAuthentication[AnyContent](block, AffinityGroup.Individual)(fakeRequest, emptyHeaderCarrier)
        }

        "returns an SEE_OTHER status" in {
          status(result) shouldBe SEE_OTHER
        }
      }

      "the nino enrolment is missing" which {
        val block: AuthorisationRequest[AnyContent] => Future[Result] = request => Future.successful(Ok(request.user.mtditid))
        val enrolments                                                = Enrolments(Set())

        lazy val result: Future[Result] = {
          when(mockAuthConnector.authorise(any(), eqTo(allEnrolments and confidenceLevel))(any(), any()))
            .thenReturn(Future.successful(enrolments and ConfidenceLevel.L250))

          authorisedAction.individualAuthentication[AnyContent](block, AffinityGroup.Individual)(fakeRequest, headerCarrierWithSession)
        }

        "returns a forbidden" in {
          status(result) shouldBe SEE_OTHER
        }
      }

      "the individual enrolment is missing but there is a nino" which {
        val block: AuthorisationRequest[AnyContent] => Future[Result] = request => Future.successful(Ok(request.user.mtditid))
        val nino                                                      = "AA123456A"
        val enrolments = Enrolments(Set(Enrolment("HMRC-NI", Seq(EnrolmentIdentifier(EnrolmentIdentifiers.nino, nino)), "Activated")))

        lazy val result: Future[Result] = {
          when(mockAuthConnector.authorise(any(), eqTo(allEnrolments and confidenceLevel))(any(), any()))
            .thenReturn(Future.successful(enrolments and ConfidenceLevel.L250))

          authorisedAction.individualAuthentication[AnyContent](block, AffinityGroup.Individual)(fakeRequest, headerCarrierWithSession)
        }

        "returns an Unauthorised" in {
          status(result) shouldBe SEE_OTHER
        }
        "returns an redirect to the correct page" in {
          redirectUrl(result) shouldBe "/update-and-submit-income-tax-return/pensions/error/you-need-to-sign-up"
        }
      }

    }

    "return the user to IV Uplift" when {

      "the confidence level is below minimum" which {
        val block: AuthorisationRequest[AnyContent] => Future[Result] = request => Future.successful(Ok(request.user.mtditid))
        val mtditid                                                   = "1234567890"
        val enrolments = Enrolments(
          Set(
            Enrolment(EnrolmentKeys.Individual, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, mtditid)), "Activated"),
            Enrolment(EnrolmentKeys.nino, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.nino, "AA123456A")), "Activated")
          ))

        lazy val result: Future[Result] = {
          when(mockAuthConnector.authorise(any(), eqTo(allEnrolments and confidenceLevel))(any(), any()))
            .thenReturn(Future.successful(enrolments and ConfidenceLevel.L50))

          authorisedAction.individualAuthentication[AnyContent](block, AffinityGroup.Individual)(fakeRequest, headerCarrierWithSession)
        }

        "has a status of 303" in {
          status(result) shouldBe SEE_OTHER
        }

        "redirects to the iv url" in {
          await(result).header.headers("Location") shouldBe "http://localhost:9302/update-and-submit-income-tax-return/iv-uplift"
        }
      }
    }
  }

  ".agentAuthentication" should {

    "MTD ID and/or NINO are not found in the session" when {
      "return a redirect to View and Change service" which {
        lazy val block: AuthorisationRequest[AnyContent] => Future[Result] = { request =>
          val arnPart = request.user.arn.getOrElse("")
          Future.successful(Ok(s"${request.user.mtditid} $arnPart"))
        }

        lazy val result: Future[Result] =
          authorisedAction.agentAuthentication[AnyContent](block)(fakeRequest, headerCarrierWithSession)

        "has a status of 303" in {
          status(result) shouldBe SEE_OTHER
        }

        "redirects to the iv url" in {
          redirectUrl(result) shouldBe "http://localhost:9081" + this.viewAndChangeUrl
        }
      }
    }

    "NINO and MTD IT ID are present in the session" when {
      "results in a NoActiveSession error to be returned from Auth" which {
        "return a redirect to the login page" in new AgentTest {
          object AuthException extends NoActiveSession("Some reason")

          mockAuthReturnException(AuthException, primaryAgentPredicate(mtdItId))

          override def mockSignInUrl(): ScalaOngoingStubbing[String] = super.mockSignInUrl()
          mockAuthAsAgent()

          lazy val result: Future[Result] =
            authorisedAction.agentAuthentication[AnyContent](testBlock)(fakeRequestWithMtditidAndNino, headerCarrierWithSession)

          status(result) shouldBe SEE_OTHER

          redirectUrl(result) shouldBe s"$baseUrl/signIn"
        }
      }
    }

    "[EMA disabled] results in an AuthorisationException error being returned from Auth" when {
      "return a redirect to the agent error page" in new AgentTest {
        mockMultipleAgentsSwitch(false)

        object AuthException extends AuthorisationException("Some reason")

        mockAuthReturnException(AuthException, primaryAgentPredicate(mtdItId))

        // mockAuthAsAgent()

        lazy val result: Future[Result] =
          authorisedAction.agentAuthentication(testBlock)(fakeRequestWithMtditidAndNino, emptyHeaderCarrier)

        //          val result: Future[Result] = testAuth.agentAuthentication(testBlock)(
        //            request = FakeRequest().withSession(fakeRequestWithMtditidAndNino.session.data.toSeq: _*),
        //            hc = emptyHeaderCarrier
        //          )

        status(result) shouldBe SEE_OTHER
        redirectUrl(result) shouldBe s"$baseUrl/error/you-need-client-authorisation"
      }
    }

    "[EMA enabled] results in an AuthorisationException error being returned from Auth" when {
      "return a redirect to the agent error page when secondary agent auth call also fails" in new AgentTest {
        mockMultipleAgentsSwitch(true)

        object AuthException extends AuthorisationException("Some reason")

        mockAuthReturnException(AuthException, primaryAgentPredicate(mtdItId))
        mockAuthReturnException(AuthException, secondaryAgentPredicate(mtdItId))

        lazy val result: Future[Result] =
          authorisedAction.agentAuthentication[AnyContent](testBlock)(fakeRequestWithMtditidAndNino, emptyHeaderCarrier)

        status(result) shouldBe SEE_OTHER
        redirectUrl(result) shouldBe s"$baseUrl/error/you-need-client-authorisation"
      }
    }

    "handle appropriately when a supporting agent is properly authorised" in new AgentTest {
      mockMultipleAgentsSwitch(true)

      object AuthException extends AuthorisationException("Some reason")

      mockAuthReturnException(AuthException, primaryAgentPredicate(mtdItId))
      mockAuthReturn(supportingAgentEnrolment, secondaryAgentPredicate(mtdItId))

      mockAuthAsAgent()

      lazy val result: Future[Result] =
        authorisedAction.agentAuthentication[AnyContent](testBlock)(fakeRequestWithMtditidAndNino, validHeaderCarrier)

      status(result) shouldBe OK
      bodyOf(result) shouldBe s"$mtdItId $arn"
    }

    "results in successful authorisation for a primary agent" when {
      "return a redirect to You Need Agent Services page when an ARN cannot be found" in new AgentTest {
        val primaryAgentEnrolmentNoArn: Enrolments = Enrolments(
          Set(
            Enrolment(EnrolmentKeys.Individual, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, mtdItId)), "Activated"),
            Enrolment(EnrolmentKeys.Agent, Seq.empty, "Activated")
          ))

        mockAuthReturn(primaryAgentEnrolmentNoArn, primaryAgentPredicate(mtdItId))

        lazy val result: Future[Result] =
          authorisedAction.agentAuthentication[AnyContent](testBlock)(fakeRequestWithMtditidAndNino, validHeaderCarrier)

        status(result) shouldBe SEE_OTHER
        redirectUrl(result) shouldBe s"$baseUrl/error/you-need-agent-services-account"
      }

      "return a redirect to Sign In page when a session ID cannot be found" in new AgentTest {
        mockAuthReturn(primaryAgentEnrolment, primaryAgentPredicate(mtdItId))

        //        lazy val result: Future[Result] = testAuth.agentAuthentication(testBlock)(
        //          request = FakeRequest().withSession(fakeRequestWithMtditidAndNino.session.data.toSeq: _*),
        //          hc = emptyHeaderCarrier
        //        )
        mockAuthAsAgent()

        lazy val result: Future[Result] =
          authorisedAction.agentAuthentication[AnyContent](testBlock)(fakeRequestWithMtditidAndNino, emptyHeaderCarrier)

        status(result) shouldBe SEE_OTHER

        redirectUrl(result) shouldBe s"$baseUrl/signIn"
      }

      "invoke block when the user is properly authenticated" in new AgentTest {
        mockAuthReturn(primaryAgentEnrolment, primaryAgentPredicate(mtdItId))

        lazy val result: Future[Result] =
          authorisedAction.agentAuthentication[AnyContent](testBlock)(fakeRequestWithMtditidAndNino, validHeaderCarrier)

        status(result) shouldBe OK
        bodyOf(result) shouldBe s"$mtdItId $arn"
      }
    }

  "results in an unexpected error to be returned during primary agent auth call" when {
    "render ISE page" in new AgentTest {
      object AuthException extends Exception("Some reason")

      mockFutureInternalServerError("an error")
      mockAuthReturnException(AuthException, primaryAgentPredicate(mtdItId))
      mockAuthAsAgent()

//      val result: Future[Result] = testAuth.agentAuthentication(testBlock)(
//        request = FakeRequest().withSession(fakeRequestWithMtditidAndNino.session.data.toSeq: _*),
//        hc = emptyHeaderCarrier
//      )
      lazy val result: Future[Result] =
        authorisedAction.agentAuthentication[AnyContent](testBlock)(fakeRequestWithMtditidAndNino, validHeaderCarrier)

      status(result) shouldBe INTERNAL_SERVER_ERROR
      //bodyOf(result) shouldBe "an error"
    }
  }

    "[EMA enabled] results in an unexpected error to be returned during secondary agent auth call" when {
      "render ISE page" in new AgentTest {
        mockMultipleAgentsSwitch(true)

        object AuthException  extends AuthorisationException("Some reason")
        object OtherException extends Exception("Some reason")

        mockAuthReturnException(AuthException, primaryAgentPredicate(mtdItId))
        mockAuthReturnException(OtherException, secondaryAgentPredicate(mtdItId))
        mockFutureInternalServerError("an error")
        mockAuthAsAgent()

        //        val result: Future[Result] = testAuth.agentAuthentication(testBlock)(
//          request = FakeRequest().withSession(fakeRequestWithMtditidAndNino.session.data.toSeq: _*),
//          hc = emptyHeaderCarrier
//        )

        lazy val result: Future[Result] =
          authorisedAction.agentAuthentication[AnyContent](testBlock)(fakeRequestWithMtditidAndNino, emptyHeaderCarrier)

        status(result) shouldBe INTERNAL_SERVER_ERROR
        //bodyOf(result) shouldBe "an error"
      }
    }
  }

  ".invokeBlock" should {

    lazy val block: AuthorisationRequest[AnyContent] => Future[Result] =
      request => Future.successful(Ok(s"mtditid: ${request.user.mtditid}${request.user.arn.fold("")(arn => " arn: " + arn)}"))

    "perform the block action" when {

      "the user is successfully verified as an agent" which {

        lazy val result = {
          mockAuthAsAgent()
          authorisedAction.invokeBlock(fakeRequestWithMtditidAndNino, block)
        }

        "should return an OK(200) status" in {
          status(result) shouldBe OK
          bodyOf(result) shouldBe "mtditid: 1234567890 arn: 0987654321"
        }
      }

      "the user is successfully verified as an individual" in {

        lazy val result = {
          mockAuth(Some("AA123456A"))
          authorisedAction.invokeBlock(fakeRequest, block)
        }

        status(result) shouldBe OK

        bodyOf(result) shouldBe "mtditid: 1234567890"
      }
    }

    "return a redirect" when {

      "the authorisation service returns an AuthorisationException exception" in {
        object AuthException extends AuthorisationException("Some reason")

        lazy val result = {
          mockAuthReturnException(AuthException)
          authorisedAction.invokeBlock(fakeRequest, block)
        }
        status(result) shouldBe SEE_OTHER
      }

      "there is no MTDITID value in session for an agent" in {
        lazy val result = {

          when(mockAuthConnector.authorise(any(), eqTo(Retrievals.affinityGroup))(any(), any()))
            .thenReturn(Future.successful(Some(AffinityGroup.Agent)))

          authorisedAction.invokeBlock(fakeRequestWithNino, block)
        }
        status(result) shouldBe SEE_OTHER
        redirectUrl(result) should endWith(viewAndChangeUrl)
      }
    }

    "redirect to the sign in page" when {
      "the authorisation service returns a NoActiveSession exception" in {
        object NoActiveSession extends NoActiveSession("Some reason")

        lazy val result = {
          mockAuthReturnException(NoActiveSession)
          authorisedAction.invokeBlock(fakeRequest, block)
        }

        status(result) shouldBe SEE_OTHER
      }
    }

    "return ISE" when {

      "the authorisation service returns an Exception that is not an Auth related Exception" in {
        mockInternalServerError
        mockAuthReturnException(new Exception("Some reason"))

        lazy val result: Future[Result] = authorisedAction.async(block)(fakeRequest)

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }

  }

}
