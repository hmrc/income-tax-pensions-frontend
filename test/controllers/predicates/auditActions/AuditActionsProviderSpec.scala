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

package controllers.predicates.auditActions

import builders.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import builders.PensionsUserDataBuilder.aPensionsUserData
import builders.UserBuilder.aUser
import common.SessionValues.{TAX_YEAR, VALID_TAX_YEARS}
import controllers.errors.routes.UnauthorisedUserErrorController
import models.audit.PaymentsIntoPensionsAudit
import models.pension.AllPensionsData.generateCyaFromPrior
import models.{APIErrorBodyModel, APIErrorModel}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.i18n.MessagesApi
import play.api.mvc.Results.{InternalServerError, Ok, Redirect}
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers.status
import support.ControllerUnitTest
import support.mocks.MockAuditService.mockedAuditSuccessResult
import support.mocks.{MockAuditService, MockAuthorisedAction, MockErrorHandler, MockPensionSessionService}


class AuditActionsProviderSpec extends ControllerUnitTest
  with MockAuthorisedAction
  with MockPensionSessionService
  with MockAuditService
  with MockErrorHandler {

  private val anyBlock = (_: Request[AnyContent]) => Ok("any-result")
  private val validTaxYears = validTaxYearList.mkString(",")

  val fakeIndividualRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
    .withHeaders(newHeaders = "X-Session-ID" -> aUser.sessionId)

  implicit val msgApi: MessagesApi = cc.messagesApi

  private val auditProvider = new AuditActionsProvider(
    mockAuthorisedAction,
    mockPensionSessionService,
    mockErrorHandler,
    appConfig,
    mockAuditService
  )

  type ActionType = Int => ActionBuilder[WrappedRequest, AnyContent]

  for ((actionName: String, action) <- Seq(
    ("paymentsIntoPensionsViewAuditing", auditProvider.paymentsIntoPensionsViewAuditing: ActionType),
    ("paymentsIntoPensionsUpdateAuditing", auditProvider.paymentsIntoPensionsUpdateAuditing: ActionType)
  )) {

    s".$actionName(taxYear)" should {
      "redirect to UnauthorisedUserErrorController when authentication fails" in {
        mockFailToAuthenticate()

        val underTest = action(taxYearEOY)(block = anyBlock)
        await(underTest(fakeIndividualRequest)) shouldBe Redirect(UnauthorisedUserErrorController.show)
      }

      "handle internal server error when getUserSessionData result in error" in {
        mockAuthAsIndividual(Some(aUser.nino))
        mockGetPensionSessionData(taxYearEOY,
          Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("INTERNAL_SERVER_ERROR", "The service is currently facing issues."))))
        mockHandleError(INTERNAL_SERVER_ERROR, InternalServerError)

        val underTest = action(taxYearEOY)(block = anyBlock)
        await(underTest(fakeIndividualRequest.withSession(TAX_YEAR -> taxYearEOY.toString, VALID_TAX_YEARS -> validTaxYears))) shouldBe InternalServerError
      }
      if (actionName == "paymentsIntoPensionsUpdateAuditing") {
        "handle internal server error when getUserPriorAndSessionData result in error" in {
          mockAuthAsIndividual(Some(aUser.nino))
          mockGetPensionSessionData(taxYearEOY, Right(Some(aPensionsUserData)))
          mockGetPriorData(taxYearEOY, aUser,
            Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("INTERNAL_SERVER_ERROR", "The service is currently facing issues."))))
          mockHandleError(INTERNAL_SERVER_ERROR, InternalServerError)

          val underTest = action(taxYearEOY)(block = anyBlock)
          await(underTest(fakeIndividualRequest.withSession(TAX_YEAR -> taxYearEOY.toString, VALID_TAX_YEARS -> validTaxYears))) shouldBe InternalServerError
        }
      }
      
      val repeatSeq = if (actionName == "paymentsIntoPensionsUpdateAuditing") Seq("create", "amend") else Seq("View")

      for (auditType <- repeatSeq) {
        s"return successful $auditType response when end of year" in {
          mockAuthAsIndividual(Some(aUser.nino))
          mockGetPensionSessionData(taxYearEOY, Right(Some(aPensionsUserData)))

          val auditModel = if (actionName == "paymentsIntoPensionsUpdateAuditing") {
            val priorData = if (auditType == "amend") anIncomeTaxUserData else anIncomeTaxUserData.copy(pensions = None)
            mockGetPriorData(taxYearEOY, aUser, Right(priorData))
            val audModel = PaymentsIntoPensionsAudit(taxYearEOY, aUser, aPensionsUserData.pensions.paymentsIntoPension,
                                                        priorData.pensions.map(generateCyaFromPrior).map(_.paymentsIntoPension))
            if (audModel.priorPaymentsIntoPension.isEmpty) audModel.toAuditModelCreate else audModel.toAuditModelAmend
          } else {
            PaymentsIntoPensionsAudit(taxYearEOY, aUser, aPensionsUserData.pensions.paymentsIntoPension, None).toAuditModelView
          }
          mockAuditResult(auditModel, mockedAuditSuccessResult)
          
          val underTest = action(taxYearEOY)(block = anyBlock)
          status(underTest(fakeIndividualRequest.withSession(TAX_YEAR -> taxYearEOY.toString, VALID_TAX_YEARS -> validTaxYears))) shouldBe OK
        }
      }
    }
  }

}
