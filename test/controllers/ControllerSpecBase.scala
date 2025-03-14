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

package controllers

import builders.UserBuilder
import config.{AppConfig, ErrorHandler}
import controllers.predicates.auditActions.AuditActionsProvider
import models.User
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.requests.{UserPriorAndSessionDataRequest, UserSessionDataRequest}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.Messages
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application, Environment, Mode}
import services.{ExcludeJourneyService, PensionSessionService}
import stubs.services.PensionsServiceStub
import testdata.allData
import utils.CommonData.{currTaxYear, nino}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

trait ControllerSpecBase extends PlaySpec with AnyWordSpecLike with MockitoSugar {

  implicit val appConfig: AppConfig = mock[AppConfig]

  lazy val app: Application = new GuiceApplicationBuilder()
    .in(Environment.simple(mode = Mode.Dev))
    .build()

  val auditProvider: AuditActionsProvider          = mock[AuditActionsProvider]
  val pensionsService: PensionsServiceStub         = PensionsServiceStub()
  val pensionSessionService: PensionSessionService = mock[PensionSessionService]
  val excludeJourneyService: ExcludeJourneyService = mock[ExcludeJourneyService]
  val errorHandler: ErrorHandler                   = mock[ErrorHandler]
  val mcc: MessagesControllerComponents            = stubMessagesControllerComponents()
  val user: User                                   = UserBuilder.aUser

  val fakeRequest: FakeRequest[AnyContentAsEmpty.type]                     = FakeRequest("GET", "/")
  val mockActionBuilder: ActionBuilder[UserSessionDataRequest, AnyContent] = mock[ActionBuilder[UserSessionDataRequest, AnyContent]]

  implicit val messages: Messages = mcc.messagesApi.preferred(FakeRequest())
  implicit val userSessionDataRequest: UserSessionDataRequest[_] =
    UserSessionDataRequest(PensionsUserData.empty(user, currTaxYear), user, fakeRequest)

  when(auditProvider.paymentsIntoPensionsUpdateAuditing(any[Int])).thenReturn(mkAction(allData))
  when(auditProvider.paymentsIntoPensionsViewAuditing(any[Int])).thenReturn(mkUserSessionDataRequest(allData))
  when(auditProvider.annualAllowancesUpdateAuditing(any[Int])).thenReturn(mkAction(allData))
  when(auditProvider.annualAllowancesViewAuditing(any[Int])).thenReturn(mkUserSessionDataRequest(allData))
  when(auditProvider.transfersIntoOverseasPensionsUpdateAuditing(any[Int])).thenReturn(mkAction(allData))
  when(auditProvider.transfersIntoOverseasPensionsViewAuditing(any[Int])).thenReturn(mkUserSessionDataRequest(allData))
  when(auditProvider.paymentsIntoOverseasPensionsUpdateAuditing(any[Int])).thenReturn(mkAction(allData))
  when(auditProvider.paymentsIntoOverseasPensionsViewAuditing(any[Int])).thenReturn(mkUserSessionDataRequest(allData))
  when(auditProvider.incomeFromOverseasPensionsUpdateAuditing(any[Int])).thenReturn(mkAction(allData))
  when(auditProvider.incomeFromOverseasPensionsViewAuditing(any[Int])).thenReturn(mkUserSessionDataRequest(allData))
  when(auditProvider.shortServiceRefundsUpdateAuditing(any[Int])).thenReturn(mkAction(allData))
  when(auditProvider.shortServiceRefundsViewAuditing(any[Int])).thenReturn(mkUserSessionDataRequest(allData))

  def mkAction(existingData: PensionsCYAModel) =
    new ActionBuilder[UserPriorAndSessionDataRequest, AnyContent] {
      def invokeBlock[A](request: Request[A], block: UserPriorAndSessionDataRequest[A] => Future[Result]): Future[Result] =
        block(
          UserPriorAndSessionDataRequest(
            PensionsUserData("sessionId", "mtditid", nino, currTaxYear.endYear, false, existingData),
            None,
            user,
            request
          )
        )

      def parser: BodyParser[AnyContent]               = mcc.parsers.default
      protected def executionContext: ExecutionContext = global
    }

  def mkUserSessionDataRequest(existingData: PensionsCYAModel) =
    new ActionBuilder[UserSessionDataRequest, AnyContent] {
      def invokeBlock[A](request: Request[A], block: UserSessionDataRequest[A] => Future[Result]): Future[Result] =
        block(
          UserSessionDataRequest(
            PensionsUserData("sessionId", "mtditid", nino, currTaxYear.endYear, false, existingData),
            user,
            request
          )
        )

      def parser: BodyParser[AnyContent]               = mcc.parsers.default
      protected def executionContext: ExecutionContext = global
    }
}
