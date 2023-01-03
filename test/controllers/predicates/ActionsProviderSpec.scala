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

package controllers.predicates

import builders.AllPensionsDataBuilder.anAllPensionsData
import builders.PensionsUserDataBuilder.aPensionsUserData
import builders.UserBuilder.aUser
import common.SessionValues.{TAX_YEAR, VALID_TAX_YEARS}
import controllers.errors.routes.UnauthorisedUserErrorController
import models.{APIErrorBodyModel, APIErrorModel, IncomeTaxUserData}
import models.errors.HttpParserError
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.mvc.Results.{InternalServerError, Ok, Redirect}
import play.api.mvc.{AnyContent, AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers.status
import support.ControllerUnitTest
import support.mocks.{MockAuthorisedAction, MockErrorHandler, MockPensionSessionService}


class ActionsProviderSpec extends ControllerUnitTest
  with MockAuthorisedAction
  with MockPensionSessionService
  with MockErrorHandler {

  private val anyBlock = (_: Request[AnyContent]) => Ok("any-result")
  private val validTaxYears = validTaxYearList.mkString(",")
  
  val fakeIndividualRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
    .withHeaders(newHeaders = "X-Session-ID" -> aUser.sessionId)

  implicit val msgApi = cc.messagesApi
  
  private val actionsProvider = new ActionsProvider(
    mockAuthorisedAction,
    mockPensionSessionService,
    mockErrorHandler,
    appConfig
  )

  ".userPriorDataFor(taxYear)" should {
    "redirect to UnauthorisedUserErrorController when authentication fails" in {
      mockFailToAuthenticate()

      val underTest = actionsProvider.userPriorDataFor(taxYearEOY)(block = anyBlock)

      await(underTest(fakeIndividualRequest)) shouldBe Redirect(UnauthorisedUserErrorController.show)
    }

    "redirect to Income Tax Submission Overview when in year" in {
      mockAuthAsIndividual(Some(aUser.nino))

      val underTest = actionsProvider.userPriorDataFor(taxYear)(block = anyBlock)

      await(underTest(fakeIndividualRequest.withSession(TAX_YEAR -> taxYear.toString, VALID_TAX_YEARS -> validTaxYears))) shouldBe
        Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
    }

    "handle internal server error when getPriorData result in error" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockGetPriorData(taxYearEOY, aUser,
        Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("INTERNAL_SERVER_ERROR","The service is currently facing issues."))))
      mockHandleError(INTERNAL_SERVER_ERROR, InternalServerError)

      val underTest = actionsProvider.userPriorDataFor(taxYearEOY)(block = anyBlock)

      await(underTest(fakeIndividualRequest.withSession(TAX_YEAR -> taxYearEOY.toString, VALID_TAX_YEARS -> validTaxYears))) shouldBe InternalServerError
    }

    "return successful response when end of year" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockGetPriorData(taxYearEOY, aUser, Right(IncomeTaxUserData(pensions = Some(anAllPensionsData))))

      val underTest = actionsProvider.userPriorDataFor(taxYearEOY)(block = anyBlock)

      status(underTest(fakeIndividualRequest.withSession(TAX_YEAR -> taxYearEOY.toString, VALID_TAX_YEARS -> validTaxYears))) shouldBe OK
    }
  }

  ".userSessionDataFor(taxYear)" should {
    "redirect to UnauthorisedUserErrorController when authentication fails" in {
      mockFailToAuthenticate()

      val underTest = actionsProvider.userSessionDataFor(taxYearEOY)(block = anyBlock)
      await(underTest(fakeIndividualRequest)) shouldBe Redirect(UnauthorisedUserErrorController.show)
    }

    "redirect to Income Tax Submission Overview when in year" in {
      mockAuthAsIndividual(Some(aUser.nino))

      val underTest = actionsProvider.userSessionDataFor(taxYear)(block = anyBlock)

      await(underTest(fakeIndividualRequest.withSession(TAX_YEAR -> taxYear.toString, VALID_TAX_YEARS -> validTaxYears))) shouldBe
        Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
    }

    "handle internal server error when getUserSessionData result in error" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockGetPensionSessionData(taxYearEOY, Left(HttpParserError(INTERNAL_SERVER_ERROR)))
      mockHandleError(INTERNAL_SERVER_ERROR, InternalServerError)

      val underTest = actionsProvider.userSessionDataFor(taxYearEOY)(block = anyBlock)

      await(underTest(fakeIndividualRequest.withSession(TAX_YEAR -> taxYearEOY.toString, VALID_TAX_YEARS -> validTaxYears))) shouldBe InternalServerError
    }

    "return successful response when end of year" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockGetPensionSessionData(taxYearEOY, Right(Some(aPensionsUserData)))

      val underTest = actionsProvider.userSessionDataFor(taxYearEOY)(block = anyBlock)

      status(underTest(fakeIndividualRequest.withSession(TAX_YEAR -> taxYearEOY.toString, VALID_TAX_YEARS -> validTaxYears))) shouldBe OK
    }
  }

  ".createUserSessionDataFor(taxYear)" should {
    "redirect to UnauthorisedUserErrorController when authentication fails" in {
      mockFailToAuthenticate()

      val underTest = actionsProvider.endOfYear(taxYearEOY)(block = anyBlock)

      await(underTest(fakeIndividualRequest)) shouldBe Redirect(UnauthorisedUserErrorController.show)
    }

    "redirect to Income Tax Submission Overview when in year" in {
      mockAuthAsIndividual(Some(aUser.nino))

      val underTest = actionsProvider.endOfYear(taxYear)(block = anyBlock)

      await(underTest(fakeIndividualRequest.withSession(TAX_YEAR -> taxYear.toString, VALID_TAX_YEARS -> validTaxYears))) shouldBe
        Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
    }

    "return successful response when end of year" in {
      mockAuthAsIndividual(Some(aUser.nino))

      val underTest = actionsProvider.endOfYear(taxYearEOY)(block = anyBlock)

      status(underTest(fakeIndividualRequest.withSession(TAX_YEAR -> taxYearEOY.toString, VALID_TAX_YEARS -> validTaxYears))) shouldBe OK
    }
  }
}
