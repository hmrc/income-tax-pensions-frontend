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
import controllers.errors
import models.audit.UkPensionIncomeAudit.AuditUkPensionIncome
import models.audit._
import models.pension.AllPensionsData.{generateSessionModelFromPrior, generateUkPensionSessionFromPrior}
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

class AuditActionsProviderSpec
    extends ControllerUnitTest
    with MockAuthorisedAction
    with MockPensionSessionService
    with MockAuditService
    with MockErrorHandler {

  private val anyBlock      = (_: Request[AnyContent]) => Ok("any-result")
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
      ("paymentsIntoPensionsUpdateAuditing", auditProvider.paymentsIntoPensionsUpdateAuditing: ActionType),
      ("unauthorisedPaymentsViewAuditing", auditProvider.unauthorisedPaymentsViewAuditing: ActionType),
      ("unauthorisedPaymentsUpdateAuditing", auditProvider.unauthorisedPaymentsUpdateAuditing: ActionType),
      ("incomeFromOverseasPensionsViewAuditing", auditProvider.incomeFromOverseasPensionsViewAuditing: ActionType),
      ("incomeFromOverseasPensionsUpdateAuditing", auditProvider.incomeFromOverseasPensionsUpdateAuditing: ActionType),
      ("paymentsIntoOverseasPensionsViewAuditing", auditProvider.paymentsIntoOverseasPensionsViewAuditing: ActionType),
      ("paymentsIntoOverseasPensionsUpdateAuditing", auditProvider.paymentsIntoOverseasPensionsUpdateAuditing: ActionType),
      ("shortServiceRefundsViewAuditing", auditProvider.shortServiceRefundsViewAuditing: ActionType),
      ("shortServiceRefundsUpdateAuditing", auditProvider.shortServiceRefundsUpdateAuditing: ActionType),
      ("incomeFromStatePensionsViewAuditing", auditProvider.incomeFromStatePensionsViewAuditing: ActionType),
      ("incomeFromStatePensionsUpdateAuditing", auditProvider.incomeFromStatePensionsUpdateAuditing: ActionType),
      ("ukPensionIncomeViewAuditing", auditProvider.ukPensionIncomeViewAuditing: ActionType),
      ("ukPensionIncomeUpdateAuditing", auditProvider.ukPensionIncomeUpdateAuditing: ActionType),
      ("annualAllowancesViewAuditing", auditProvider.annualAllowancesViewAuditing: ActionType),
      ("annualAllowancesUpdateAuditing", auditProvider.annualAllowancesUpdateAuditing: ActionType),
      ("transfersIntoOverseasPensionsViewAuditing", auditProvider.transfersIntoOverseasPensionsViewAuditing: ActionType),
      ("transfersIntoOverseasPensionsUpdateAuditing", auditProvider.transfersIntoOverseasPensionsUpdateAuditing: ActionType)
    )) {

    s".$actionName(taxYear)" should {
      "redirect to UnauthorisedUserErrorController when authentication fails" in {
        mockFailToAuthenticate()

        val underTest = action(taxYearEOY)(block = anyBlock)
        await(underTest(fakeIndividualRequest)) shouldBe Redirect(errors.routes.UnauthorisedUserErrorController.show)
      }

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
      if (actionName.contains("UpdateAuditing")) {
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

      val repeatSeq = if (actionName.contains("UpdateAuditing")) Seq("create", "amend") else Seq("View")

      for (auditType <- repeatSeq) {
        s"return successful $auditType response when end of year" in {
          mockAuthAsIndividual(Some(aUser.nino))
          mockGetPensionSessionData(taxYearEOY, Right(Some(aPensionsUserData)))

          val auditModel = actionName match {
            case "paymentsIntoPensionsUpdateAuditing" =>
              val priorData = if (auditType == "amend") anIncomeTaxUserData else anIncomeTaxUserData.copy(pensions = None)
              mockGetPriorData(taxYearEOY, aUser, Right(priorData))
              val audModel = PaymentsIntoPensionsAudit(
                taxYearEOY,
                aUser,
                aPensionsUserData.pensions.paymentsIntoPension,
                priorData.pensions.map(generateSessionModelFromPrior).map(_.paymentsIntoPension))
              if (audModel.priorPaymentsIntoPension.isEmpty) audModel.toAuditModelCreate else audModel.toAuditModelAmend
            case "paymentsIntoPensionsViewAuditing" =>
              PaymentsIntoPensionsAudit(taxYearEOY, aUser, aPensionsUserData.pensions.paymentsIntoPension, None).toAuditModelView

            case "unauthorisedPaymentsUpdateAuditing" =>
              val priorData = if (auditType == "amend") anIncomeTaxUserData else anIncomeTaxUserData.copy(pensions = None)
              mockGetPriorData(taxYearEOY, aUser, Right(priorData))
              val audModel = UnauthorisedPaymentsAudit(
                taxYearEOY,
                aUser,
                aPensionsUserData.pensions.unauthorisedPayments,
                priorData.pensions.map(generateSessionModelFromPrior).map(_.unauthorisedPayments))
              if (audModel.priorUnauthorisedPayments.isEmpty) audModel.toAuditModelCreate else audModel.toAuditModelAmend
            case "unauthorisedPaymentsViewAuditing" =>
              UnauthorisedPaymentsAudit(taxYearEOY, aUser, aPensionsUserData.pensions.unauthorisedPayments, None).toAuditModelView

            case "incomeFromOverseasPensionsUpdateAuditing" =>
              val priorData = if (auditType == "amend") anIncomeTaxUserData else anIncomeTaxUserData.copy(pensions = None)
              mockGetPriorData(taxYearEOY, aUser, Right(priorData))
              val audModel = IncomeFromOverseasPensionsAudit(
                taxYearEOY,
                aUser,
                aPensionsUserData.pensions.incomeFromOverseasPensions,
                priorData.pensions.map(generateSessionModelFromPrior).map(_.incomeFromOverseasPensions))
              if (audModel.priorIncomeFromOverseasPensions.isEmpty) audModel.toAuditModelCreate else audModel.toAuditModelAmend
            case "incomeFromOverseasPensionsViewAuditing" =>
              IncomeFromOverseasPensionsAudit(taxYearEOY, aUser, aPensionsUserData.pensions.incomeFromOverseasPensions, None).toAuditModelView

            case "paymentsIntoOverseasPensionsUpdateAuditing" =>
              val priorData = if (auditType == "amend") anIncomeTaxUserData else anIncomeTaxUserData.copy(pensions = None)
              mockGetPriorData(taxYearEOY, aUser, Right(priorData))
              val audModel = PaymentsIntoOverseasPensionsAudit(
                taxYearEOY,
                aUser,
                aPensionsUserData.pensions.paymentsIntoOverseasPensions,
                priorData.pensions.map(generateSessionModelFromPrior).map(_.paymentsIntoOverseasPensions)
              )
              if (audModel.priorPaymentsIntoOverseasPensions.isEmpty) audModel.toAuditModelCreate else audModel.toAuditModelAmend
            case "paymentsIntoOverseasPensionsViewAuditing" =>
              PaymentsIntoOverseasPensionsAudit(taxYearEOY, aUser, aPensionsUserData.pensions.paymentsIntoOverseasPensions, None).toAuditModelView

            case "shortServiceRefundsUpdateAuditing" =>
              val priorData = if (auditType == "amend") anIncomeTaxUserData else anIncomeTaxUserData.copy(pensions = None)
              mockGetPriorData(taxYearEOY, aUser, Right(priorData))
              val audModel = ShortServiceRefundsAudit(
                taxYearEOY,
                aUser,
                aPensionsUserData.pensions.shortServiceRefunds,
                priorData.pensions.map(generateSessionModelFromPrior).map(_.shortServiceRefunds))
              if (audModel.priorShortServiceRefunds.isEmpty) audModel.toAuditModelCreate else audModel.toAuditModelAmend
            case "shortServiceRefundsViewAuditing" =>
              ShortServiceRefundsAudit(taxYearEOY, aUser, aPensionsUserData.pensions.shortServiceRefunds, None).toAuditModelView

            case "incomeFromStatePensionsUpdateAuditing" =>
              val priorData = if (auditType == "amend") anIncomeTaxUserData else anIncomeTaxUserData.copy(pensions = None)
              mockGetPriorData(taxYearEOY, aUser, Right(priorData))
              val audModel = IncomeFromStatePensionsAudit(
                taxYearEOY,
                aUser,
                aPensionsUserData.pensions.incomeFromPensions,
                priorData.pensions.map(generateSessionModelFromPrior).map(_.incomeFromPensions))
              if (audModel.priorIncomeFromStatePensions.isEmpty) audModel.toAuditModelCreate else audModel.toAuditModelAmend
            case "incomeFromStatePensionsViewAuditing" =>
              IncomeFromStatePensionsAudit(taxYearEOY, aUser, aPensionsUserData.pensions.incomeFromPensions, None).toAuditModelView

            case "ukPensionIncomeUpdateAuditing" =>
              val priorData = if (auditType == "amend") anIncomeTaxUserData else anIncomeTaxUserData.copy(pensions = None)
              mockGetPriorData(taxYearEOY, aUser, Right(priorData))
              val audModel = UkPensionIncomeAudit(
                taxYearEOY,
                aUser,
                AuditUkPensionIncome(
                  aPensionsUserData.pensions.incomeFromPensions.uKPensionIncomesQuestion,
                  aPensionsUserData.pensions.incomeFromPensions.uKPensionIncomes
                ),
                priorData.pensions
                  .map(pd => generateUkPensionSessionFromPrior(pd))
                  .map { case (ukPensionIncomeQ, ukPensionIncomes) => AuditUkPensionIncome(ukPensionIncomeQ, ukPensionIncomes) }
              )
              if (audModel.priorUkPensionIncome.isEmpty) audModel.toAuditModelCreate else audModel.toAuditModelAmend
            case "ukPensionIncomeViewAuditing" =>
              UkPensionIncomeAudit(
                taxYearEOY,
                aUser,
                AuditUkPensionIncome(
                  aPensionsUserData.pensions.incomeFromPensions.uKPensionIncomesQuestion,
                  aPensionsUserData.pensions.incomeFromPensions.uKPensionIncomes
                ),
                None
              ).toAuditModelView

            case "annualAllowancesUpdateAuditing" =>
              val priorData = if (auditType == "amend") anIncomeTaxUserData else anIncomeTaxUserData.copy(pensions = None)
              mockGetPriorData(taxYearEOY, aUser, Right(priorData))
              val audModel = AnnualAllowancesAudit(
                taxYearEOY,
                aUser,
                aPensionsUserData.pensions.pensionsAnnualAllowances,
                priorData.pensions.map(generateSessionModelFromPrior).map(_.pensionsAnnualAllowances))
              if (audModel.priorAnnualAllowances.isEmpty) audModel.toAuditModelCreate else audModel.toAuditModelAmend
            case "annualAllowancesViewAuditing" =>
              AnnualAllowancesAudit(taxYearEOY, aUser, aPensionsUserData.pensions.pensionsAnnualAllowances, None).toAuditModelView

            case "transfersIntoOverseasPensionsUpdateAuditing" =>
              val priorData = if (auditType == "amend") anIncomeTaxUserData else anIncomeTaxUserData.copy(pensions = None)
              mockGetPriorData(taxYearEOY, aUser, Right(priorData))
              val audModel = TransfersIntoOverseasPensionsAudit(
                taxYearEOY,
                aUser,
                aPensionsUserData.pensions.transfersIntoOverseasPensions,
                priorData.pensions.map(generateSessionModelFromPrior).map(_.transfersIntoOverseasPensions)
              )
              if (audModel.priorTransfersIntoOverseasPensions.isEmpty) audModel.toAuditModelCreate else audModel.toAuditModelAmend
            case "transfersIntoOverseasPensionsViewAuditing" =>
              TransfersIntoOverseasPensionsAudit(taxYearEOY, aUser, aPensionsUserData.pensions.transfersIntoOverseasPensions, None).toAuditModelView
          }

          mockAuditResult(auditModel, mockedAuditSuccessResult)

          val underTest = action(taxYearEOY)(block = anyBlock)
          status(underTest(fakeIndividualRequest.withSession(TAX_YEAR -> taxYearEOY.toString, VALID_TAX_YEARS -> validTaxYears))) shouldBe OK
        }
      }
    }
  }

}
