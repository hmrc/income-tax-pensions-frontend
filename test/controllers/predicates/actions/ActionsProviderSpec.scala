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

package controllers.predicates.actions

import builders.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import builders.PensionsUserDataBuilder.aPensionsUserData
import builders.UserBuilder.aUser
import common.SessionValues.{TAX_YEAR, VALID_TAX_YEARS}
import controllers.errors.routes.UnauthorisedUserErrorController
import models.{APIErrorBodyModel, APIErrorModel}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.i18n.MessagesApi
import play.api.mvc.Results.{InternalServerError, Ok, Redirect}
import play.api.mvc.{ActionBuilder, AnyContent, AnyContentAsEmpty, WrappedRequest}
import play.api.test.FakeRequest
import play.api.test.Helpers.status
import support.ControllerUnitTest
import support.mocks.{MockAuthorisedAction, MockErrorHandler, MockPensionSessionService}

class ActionsProviderSpec extends ControllerUnitTest with MockAuthorisedAction with MockPensionSessionService with MockErrorHandler {

  private val anyBlock      = (_: WrappedRequest[AnyContent]) => Ok("any-result")
  private val validTaxYears = validTaxYearList.mkString(",")

  val fakeIndividualRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
    .withHeaders(newHeaders = "X-Session-ID" -> aUser.sessionId)

  implicit val msgApi: MessagesApi = cc.messagesApi

  private val actionsProvider = new ActionsProvider(
    mockAuthorisedAction,
    mockPensionSessionService,
    mockErrorHandler,
    appConfig
  )

  type ActionType = Int => ActionBuilder[WrappedRequest, AnyContent]

  for ((actionName: String, action) <- Seq(
      ("endOfYear", actionsProvider.endOfYear: ActionType),
      ("userSessionDataFo", actionsProvider.userSessionDataFor: ActionType),
      ("userPriorAndSessionDataFor", actionsProvider.userPriorAndSessionDataFor: ActionType)
    ))
    s".$actionName(taxYear)" should {

      "redirect to UnauthorisedUserErrorController when authentication fails" in {
        mockFailToAuthenticate()

        val underTest = action(taxYearEOY)(block = anyBlock)
        await(underTest(fakeIndividualRequest)) shouldBe Redirect(UnauthorisedUserErrorController.show)
      }

      if (actionName != "endOfYear") {
        "handle internal server error when getUserSessionData result in error" in {
          mockAuthAsIndividual(Some(aUser.nino))
          mockGetPensionSessionData(
            taxYearEOY,
            Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("INTERNAL_SERVER_ERROR", "The service is currently facing issues."))))
          mockHandleError(INTERNAL_SERVER_ERROR, InternalServerError)

          val underTest = action(taxYearEOY)(block = anyBlock)
          await(
            underTest(
              fakeIndividualRequest.withSession(TAX_YEAR -> taxYearEOY.toString, VALID_TAX_YEARS -> validTaxYears))) shouldBe InternalServerError
        }
      }

      if (actionName == "userPriorAndSessionDataFor") {
        "handle internal server error when getUserPriorAndSessionData result in error" in {
          mockAuthAsIndividual(Some(aUser.nino))
          mockGetPensionSessionData(taxYearEOY, Right(Some(aPensionsUserData)))
          mockGetPriorData(
            taxYearEOY,
            aUser,
            Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("INTERNAL_SERVER_ERROR", "The service is currently facing issues."))))
          mockHandleError(INTERNAL_SERVER_ERROR, InternalServerError)

          val underTest = action(taxYearEOY)(block = anyBlock)
          await(
            underTest(
              fakeIndividualRequest.withSession(TAX_YEAR -> taxYearEOY.toString, VALID_TAX_YEARS -> validTaxYears))) shouldBe InternalServerError
        }
      }

      "return successful response when end of year" in {
        mockAuthAsIndividual(Some(aUser.nino))
        if (actionName != "endOfYear") mockGetPensionSessionData(taxYearEOY, Right(Some(aPensionsUserData)))
        if (actionName == "userPriorAndSessionDataFor") mockGetPriorData(taxYearEOY, aUser, Right(anIncomeTaxUserData))

        val underTest = action(taxYearEOY)(block = anyBlock)
        status(underTest(fakeIndividualRequest.withSession(TAX_YEAR -> taxYearEOY.toString, VALID_TAX_YEARS -> validTaxYears))) shouldBe OK
      }
    }

}
