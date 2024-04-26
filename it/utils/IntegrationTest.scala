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

import builders.PensionsUserDataBuilder.aPensionsUserData
import builders.StateBenefitsModelBuilder.aStateBenefitsModel
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import common.SessionValues
import config.AppConfig
import controllers.predicates.actions.AuthorisedAction
import helpers.{PlaySessionCookieBaker, WireMockHelper, WiremockStubHelpers}
import models.IncomeTaxUserData
import models.mongo.PensionsUserData
import models.pension.JourneyNameAndStatus
import org.apache.pekko.actor.ActorSystem
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.http.Status.NO_CONTENT
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc.{AnyContentAsEmpty, MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.OK
import play.api.{Application, Environment, Mode}
import services.AuthService
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.syntax.retrieved.authSyntaxForRetrieved
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, SessionKeys}
import views.html.templates.AgentAuthErrorPageView

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Awaitable, ExecutionContext, Future}

trait IntegrationTest
    extends AnyWordSpec
    with Matchers
    with GuiceOneServerPerSuite
    with WireMockHelper
    with WiremockStubHelpers
    with BeforeAndAfterAll
    with TaxYearHelper {
  val nino          = "AA123456A"
  val mtditid       = "1234567890"
  val sessionId     = "sessionId-eb3158c2-0aff-4ce8-8d1b-f2208ace52fe"
  val affinityGroup = "affinityGroup"

  val taxYearEndOfYearMinusOne: Int = taxYearEOY - 1

  val validTaxYearList: Seq[Int]       = Seq(taxYearEndOfYearMinusOne, taxYearEOY, taxYear)
  val validTaxYearListSingle: Seq[Int] = Seq(taxYear)

  val defaultUser: PensionsUserData = aPensionsUserData
  val xSessionId: (String, String)  = "X-Session-ID" -> defaultUser.sessionId

  implicit val ec: ExecutionContext         = ExecutionContext.Implicits.global
  implicit val headerCarrier: HeaderCarrier = HeaderCarrier().withExtraHeaders("mtditid" -> mtditid)

  implicit val actorSystem: ActorSystem = ActorSystem()

  implicit def wsClient: WSClient = app.injector.instanceOf[WSClient]

  val httpClient: HttpClient = app.injector.instanceOf[HttpClient]

  val appUrl = s"http://localhost:$port/update-and-submit-income-tax-return/pensions"

  def await[T](awaitable: Awaitable[T]): T = Await.result(awaitable, Duration.Inf)

  def config: Map[String, String] = Map(
    "defaultTaxYear"                                           -> taxYear.toString,
    "auditing.enabled"                                         -> "false",
    "play.filters.csrf.header.bypassHeaders.Csrf-Token"        -> "nocheck",
    "microservice.services.income-tax-submission-frontend.url" -> s"http://$wiremockHost:$wiremockPort",
    "microservice.services.auth.host"                          -> wiremockHost,
    "microservice.services.auth.port"                          -> wiremockPort.toString,
    "microservice.services.income-tax-pensions.url"            -> s"http://$wiremockHost:$wiremockPort",
    "microservice.services.income-tax-submission.url"          -> s"http://$wiremockHost:$wiremockPort",
    "microservice.services.income-tax-state-benefits.url"      -> s"http://$wiremockHost:$wiremockPort",
    "microservice.services.view-and-change.url"                -> s"http://$wiremockHost:$wiremockPort",
    "microservice.services.income-tax-employment.url"          -> s"http://$wiremockHost:$wiremockPort",
    "microservice.services.sign-in.url"                        -> s"/auth-login-stub/gg-sign-in",
    "taxYearErrorFeatureSwitch"                                -> "false",
    "useEncryption"                                            -> "true"
  )

  def configWithInvalidEncryptionKey: Map[String, String] = Map(
    "defaultTaxYear"                                           -> taxYear.toString,
    "auditing.enabled"                                         -> "false",
    "play.filters.csrf.header.bypassHeaders.Csrf-Token"        -> "nocheck",
    "microservice.services.income-tax-submission-frontend.url" -> s"http://$wiremockHost:$wiremockPort",
    "microservice.services.auth.host"                          -> wiremockHost,
    "microservice.services.auth.port"                          -> wiremockPort.toString,
    "microservice.services.income-tax-employment.url"          -> s"http://$wiremockHost:$wiremockPort",
    "microservice.services.income-tax-pensions.url"            -> s"http://$wiremockHost:$wiremockPort",
    "microservice.services.income-tax-expenses.url"            -> s"http://$wiremockHost:$wiremockPort",
    "microservice.services.income-tax-submission.url"          -> s"http://$wiremockHost:$wiremockPort",
    "microservice.services.income-tax-state-benefits.url"      -> s"http://$wiremockHost:$wiremockPort",
    "microservice.services.view-and-change.url"                -> s"http://$wiremockHost:$wiremockPort",
    "microservice.services.sign-in.url"                        -> s"/auth-login-stub/gg-sign-in",
    "taxYearErrorFeatureSwitch"                                -> "false",
    "useEncryption"                                            -> "true",
    "mongodb.encryption.key"                                   -> "key"
  )

  def externalConfig: Map[String, String] = Map(
    "defaultTaxYear"                                      -> taxYear.toString,
    "auditing.enabled"                                    -> "false",
    "play.filters.csrf.header.bypassHeaders.Csrf-Token"   -> "nocheck",
    "microservice.services.income-tax-submission.url"     -> s"http://127.0.0.1:$wiremockPort",
    "microservice.services.income-tax-state-benefits.url" -> s"http://127.0.0.1:$wiremockPort",
    "metrics.enabled"                                     -> "false"
  )

  lazy val agentAuthErrorPage: AgentAuthErrorPageView = app.injector.instanceOf[AgentAuthErrorPageView]

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .in(Environment.simple(mode = Mode.Dev))
    .configure(config)
    .build()

  lazy val appWithFakeExternalCall: Application = new GuiceApplicationBuilder()
    .in(Environment.simple(mode = Mode.Dev))
    .configure(externalConfig)
    .build()

  lazy val appWithInvalidEncryptionKey: Application = GuiceApplicationBuilder()
    .configure(configWithInvalidEncryptionKey)
    .build()

  implicit lazy val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  override def beforeAll(): Unit = {
    super.beforeAll()
    startWiremock()
  }

  override def afterAll(): Unit = {
    stopWiremock()
    super.afterAll()
  }

  def status(awaitable: Future[Result]): Int = await(awaitable).header.status

  def bodyOf(awaitable: Future[Result]): String = {
    val awaited = await(awaitable)
    await(awaited.body.consumeData.map(_.utf8String))
  }

  lazy val mcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]

  val defaultAcceptedConfidenceLevels: Seq[ConfidenceLevel] = Seq(
    ConfidenceLevel.L250,
    ConfidenceLevel.L500
  )

  def authService(stubbedRetrieval: Future[_], acceptedConfidenceLevel: Seq[ConfidenceLevel]): AuthService = new AuthService(
    new MockAuthConnector(stubbedRetrieval, acceptedConfidenceLevel)
  )

  def authAction(
      stubbedRetrieval: Future[_],
      acceptedConfidenceLevel: Seq[ConfidenceLevel] = Seq.empty[ConfidenceLevel]
  ): AuthorisedAction = new AuthorisedAction(
    appConfig
  )(
    authService(
      stubbedRetrieval,
      if (acceptedConfidenceLevel.nonEmpty) {
        acceptedConfidenceLevel
      } else {
        defaultAcceptedConfidenceLevels
      }),
    mcc
  )

  def successfulRetrieval: Future[Enrolments ~ Some[AffinityGroup] ~ ConfidenceLevel] = Future.successful(
    Enrolments(
      Set(
        Enrolment("HMRC-MTD-IT", Seq(EnrolmentIdentifier("MTDITID", "1234567890")), "Activated", None),
        Enrolment("HMRC-NI", Seq(EnrolmentIdentifier("NINO", "AA123456A")), "Activated", None)
      )) and Some(AffinityGroup.Individual) and ConfidenceLevel.L250
  )

  def insufficientConfidenceRetrieval: Future[Enrolments ~ Some[AffinityGroup] ~ ConfidenceLevel] = Future.successful(
    Enrolments(
      Set(
        Enrolment("HMRC-MTD-IT", Seq(EnrolmentIdentifier("MTDITID", "1234567890")), "Activated", None),
        Enrolment("HMRC-NI", Seq(EnrolmentIdentifier("NINO", "AA123456A")), "Activated", None)
      )) and Some(AffinityGroup.Individual) and ConfidenceLevel.L50
  )

  def incorrectCredsRetrieval: Future[Enrolments ~ Some[AffinityGroup] ~ ConfidenceLevel] = Future.successful(
    Enrolments(
      Set(
        Enrolment("HMRC-MTD-IT", Seq(EnrolmentIdentifier("UTR", "1234567890")), "Activated", None),
        Enrolment("HMRC-NI", Seq(EnrolmentIdentifier("NINO", "AA123456A")), "Activated", None)
      )) and Some(AffinityGroup.Individual) and ConfidenceLevel.L250
  )

  def playSessionCookies(taxYear: Int, validTaxYears: Seq[Int], extraData: Map[String, String] = Map.empty): String =
    PlaySessionCookieBaker.bakeSessionCookie(
      Map(
        SessionKeys.authToken         -> "mock-bearer-token",
        SessionValues.TAX_YEAR        -> taxYear.toString,
        SessionValues.VALID_TAX_YEARS -> validTaxYears.mkString(","),
        SessionKeys.sessionId         -> sessionId,
        SessionValues.CLIENT_NINO     -> nino,
        SessionValues.CLIENT_MTDITID  -> mtditid
      ) ++ extraData)

  def userDataStub(userData: IncomeTaxUserData, nino: String, taxYear: Int): StubMapping =
    stubGetWithHeadersCheck(
      url = s"/income-tax-submission-service/income-tax/nino/$nino/sources/session\\?taxYear=$taxYear",
      status = OK,
      body = Json.toJson(userData).toString(),
      sessionHeader = "X-Session-ID" -> defaultUser.sessionId,
      mtdidHeader = "mtditid"        -> defaultUser.mtdItId
    )

  def pensionChargesSessionStub(jsonBody: String, nino: String, taxYear: Int): StubMapping =
    stubPutWithHeadersCheck(
      url = s"/income-tax-pensions/pension-charges/session-data/nino/$nino/taxYear/$taxYear",
      status = NO_CONTENT,
      body = jsonBody,
      sessionHeader = "X-Session-ID" -> defaultUser.sessionId,
      mtdidHeader = "mtditid"        -> defaultUser.mtdItId
    )

  def pensionIncomeSessionStub(jsonBody: String, nino: String, taxYear: Int): StubMapping =
    stubPutWithHeadersCheck(
      url = s"/income-tax-pensions/pension-income/session-data/nino/$nino/taxYear/$taxYear",
      status = NO_CONTENT,
      body = jsonBody,
      sessionHeader = "X-Session-ID" -> defaultUser.sessionId,
      mtdidHeader = "mtditid"        -> defaultUser.mtdItId
    )

  def pensionReliefsSessionStub(jsonBody: String, nino: String, taxYear: Int): StubMapping =
    stubPutWithHeadersCheck(
      url = s"/income-tax-pensions/pension-reliefs/nino/$nino/taxYear/$taxYear",
      status = NO_CONTENT,
      body = jsonBody,
      sessionHeader = "X-Session-ID" -> defaultUser.sessionId,
      mtdidHeader = "mtditid"        -> defaultUser.mtdItId
    )

  def employmentPensionStub(jsonBody: String, nino: String, stat: Int, response: String): StubMapping =
    stubPostWithHeadersCheck(
      url = s"/income-tax-employment/income-tax/nino/$nino/sources\\?taxYear=$taxYear",
      status = stat,
      requestBody = jsonBody,
      responseBody = response,
      sessionHeader = "X-Session-ID" -> defaultUser.sessionId,
      mtdidHeader = "mtditid"        -> defaultUser.mtdItId
    )

  def stateBenefitsSubmissionStub(jsonBody: String, nino: String): StubMapping =
    stubPutWithHeadersCheck(
      url = s"/income-tax-state-benefits/claim-data/nino/$nino",
      status = NO_CONTENT,
      body = jsonBody,
      sessionHeader = "X-Session-ID" -> defaultUser.sessionId,
      mtdidHeader = "mtditid"        -> defaultUser.mtdItId
    )

  def getAllJourneyStatusesStub(taxYear: Int, jsonBody: String = Json.toJson(List[JourneyNameAndStatus]()).toString()): StubMapping =
    stubGetWithHeadersCheck(
      s"/income-tax-pensions/journey-statuses/taxYear/$taxYear",
      OK,
      jsonBody,
      "X-Session-ID" -> sessionId,
      "mtditid"      -> mtditid)

  def userData: IncomeTaxUserData = IncomeTaxUserData(
    Some(PensionDataStubs.fullPensionsModel),
    Some(aStateBenefitsModel)
  )

  def emptyUserDataStub(nino: String = nino, taxYear: Int = taxYear): StubMapping =
    userDataStub(IncomeTaxUserData(None), nino, taxYear)

  val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
}
