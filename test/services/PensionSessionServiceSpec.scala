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

package services

import builders.AllPensionsDataBuilder.anAllPensionDataEmpty
import builders.EmploymentPensionsBuilder.anEmploymentPensions
import builders.PensionIncomeViewModelBuilder.aPensionIncome
import builders.PensionsCYAModelBuilder._
import config._
import connectors.IncomeSourceConnector
import models.mongo._
import models.pension.AllPensionsData
import models.pension.AllPensionsData.generateCyaFromPrior
import models.pension.charges._
import models.pension.reliefs.{PensionReliefs, Reliefs}
import models.pension.statebenefits._
import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status.{INTERNAL_SERVER_ERROR, SEE_OTHER}
import play.api.i18n.MessagesApi
import play.api.mvc.Result
import play.api.mvc.Results.{Ok, Redirect}
import utils.UnitTest
import views.html.templates.{InternalServerErrorTemplate, NotFoundTemplate, ServiceUnavailableTemplate}

import java.time.{Instant, LocalDate}
import java.util.UUID
import scala.concurrent.Future

class PensionSessionServiceSpec extends UnitTest
  with MockPensionUserDataRepository
  with MockIncomeTaxUserDataConnector
  with ScalaFutures {

  val serviceUnavailableTemplate: ServiceUnavailableTemplate = app.injector.instanceOf[ServiceUnavailableTemplate]
  val notFoundTemplate: NotFoundTemplate = app.injector.instanceOf[NotFoundTemplate]
  val internalServerErrorTemplate: InternalServerErrorTemplate = app.injector.instanceOf[InternalServerErrorTemplate]
  val mockMessagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  val mockFrontendAppConfig: AppConfig = app.injector.instanceOf[AppConfig]

  val errorHandler = new ErrorHandler(internalServerErrorTemplate, serviceUnavailableTemplate, mockMessagesApi, notFoundTemplate)(mockFrontendAppConfig)

  val messages: MessagesApi = app.injector.instanceOf[MessagesApi]

  val mockIncomeSourceConnector = app.injector.instanceOf[IncomeSourceConnector]

  val service: PensionSessionService =
    new PensionSessionService(mockPensionUserDataRepository, mockUserDataConnector, mockIncomeSourceConnector, mockAppConfig,
      errorHandler, mockExecutionContext)

  private val user = authorisationRequest.user

//  val fullPensionsModel: AllPensionsData = AllPensionsData(
//    pensionReliefs = Some(PensionReliefs(
//      submittedOn = "2020-01-04T05:01:01Z",
//      deletedOn = Some("2020-01-04T05:01:01Z"),
//      pensionReliefs = Reliefs(
//        regularPensionContributions = Some(100.01),
//        oneOffPensionContributionsPaid = Some(100.01),
//        retirementAnnuityPayments = Some(100.01),
//        paymentToEmployersSchemeNoTaxRelief = Some(100.01),
//        overseasPensionSchemeContributions = Some(100.01)))
//    ),
//    pensionCharges = Some(PensionCharges(
//      submittedOn = "2020-07-27T17:00:19Z",
//      pensionSavingsTaxCharges = Some(PensionSavingsTaxCharges(
//        pensionSchemeTaxReference = Seq("00123456RA", "00123456RB"),
//        lumpSumBenefitTakenInExcessOfLifetimeAllowance = Some(LifetimeAllowance(
//          amount = Some(800.02),
//          taxPaid = Some(200.02)
//        )),
//        benefitInExcessOfLifetimeAllowance = Some(LifetimeAllowance(
//          amount = Some(800.02),
//          taxPaid = Some(200.02)
//        )),
//        isAnnualAllowanceReduced = false,
//        taperedAnnualAllowance = Some(false),
//        moneyPurchasedAllowance = Some(false)
//      )),
//      pensionSchemeOverseasTransfers = Some(PensionSchemeOverseasTransfers(
//        overseasSchemeProvider = Seq(OverseasSchemeProvider(
//          providerName = "overseas providerName 1 qualifying scheme",
//          providerAddress = "overseas address 1",
//          providerCountryCode = "ESP",
//          qualifyingRecognisedOverseasPensionScheme = Some(Seq("Q100000", "Q100002")),
//          pensionSchemeTaxReference = None
//        )),
//        transferCharge = 22.77,
//        transferChargeTaxPaid = 33.88
//      )),
//      pensionSchemeUnauthorisedPayments = Some(PensionSchemeUnauthorisedPayments(
//        pensionSchemeTaxReference = Seq("00123456RA", "00123456RB"),
//        surcharge = Some(Charge(
//          amount = 124.44,
//          foreignTaxPaid = 123.33
//        )),
//        noSurcharge = Some(Charge(
//          amount = 222.44,
//          foreignTaxPaid = 223.33
//        ))
//      )),
//      pensionContributions = Some(PensionContributions(
//        pensionSchemeTaxReference = Seq("00123456RA", "00123456RB"),
//        inExcessOfTheAnnualAllowance = 150.67,
//        annualAllowanceTaxPaid = 178.65)),
//      overseasPensionContributions = Some(OverseasPensionContributions(
//        overseasSchemeProvider = Seq(OverseasSchemeProvider(
//          providerName = "overseas providerName 1 tax ref",
//          providerAddress = "overseas address 1",
//          providerCountryCode = "ESP",
//          qualifyingRecognisedOverseasPensionScheme = None,
//          pensionSchemeTaxReference = Some(Seq("00123456RA", "00123456RB"))
//        )),
//        shortServiceRefund = 1.11,
//        shortServiceRefundTaxPaid = 2.22
//      )))
//    ),
//    stateBenefits = Some(AllStateBenefitsData(
//      Some(StateBenefitsData(
//        incapacityBenefit = Some(List(StateBenefit(
//          benefitId = UUID.fromString("a1e8057e-fbbc-47a8-a8b4-78d9f015c934"),
//          startDate = LocalDate.parse("2019-11-13"),
//          dateIgnored = Some(Instant.parse("2019-04-11T16:22:00Z")),
//          submittedOn = Some(Instant.parse("2020-09-11T17:23:00Z")),
//          endDate = Some(LocalDate.parse("2020-08-23")),
//          amount = Some(1212.34),
//          taxPaid = Some(22323.23)
//        ))),
//        statePension = Some(StateBenefit(
//          benefitId = UUID.fromString("a1e8057e-fbbc-47a8-a8b4-78d9f015c935"),
//          startDate = LocalDate.parse("2018-06-03"),
//          dateIgnored = Some(Instant.parse("2018-09-09T19:23:00Z")),
//          submittedOn = Some(Instant.parse("2020-08-07T12:23:00Z")),
//          endDate = Some(LocalDate.parse("2020-09-13")),
//          amount = Some(42323.23),
//          taxPaid = Some(2323.44)
//        )),
//        statePensionLumpSum = Some(StateBenefit(
//          benefitId = UUID.fromString("a1e8057e-fbbc-47a8-a8b4-78d9f015c936"),
//          startDate = LocalDate.parse("2019-04-23"),
//          dateIgnored = Some(Instant.parse("2019-07-08T05:23:00Z")),
//          submittedOn = Some(Instant.parse("2020-03-13T19:23:00Z")),
//          endDate = Some(LocalDate.parse("2020-08-13")),
//          amount = Some(45454.23),
//          taxPaid = Some(45432.56)
//        )),
//        employmentSupportAllowance = Some(List(StateBenefit(
//          benefitId = UUID.fromString("a1e8057e-fbbc-47a8-a8b4-78d9f015c937"),
//          startDate = LocalDate.parse("2019-09-23"),
//          dateIgnored = Some(Instant.parse("2019-09-28T10:23:00Z")),
//          submittedOn = Some(Instant.parse("2020-11-13T19:23:00Z")),
//          endDate = Some(LocalDate.parse("2020-08-23")),
//          amount = Some(44545.43),
//          taxPaid = Some(35343.23)
//        ))),
//        jobSeekersAllowance = Some(List(StateBenefit(
//          benefitId = UUID.fromString("a1e8057e-fbbc-47a8-a8b4-78d9f015c938"),
//          startDate = LocalDate.parse("2019-09-19"),
//          dateIgnored = Some(Instant.parse("2019-08-18T13:23:00Z")),
//          submittedOn = Some(Instant.parse("2020-07-10T18:23:00Z")),
//          endDate = Some(LocalDate.parse("2020-09-23")),
//          amount = Some(33223.12),
//          taxPaid = Some(44224.56)
//        ))),
//        bereavementAllowance = Some(StateBenefit(
//          benefitId = UUID.fromString("a1e8057e-fbbc-47a8-a8b4-78d9f015c939"),
//          startDate = LocalDate.parse("2019-05-22"),
//          dateIgnored = Some(Instant.parse("2020-08-10T12:23:00Z")),
//          submittedOn = Some(Instant.parse("2020-09-19T19:23:00Z")),
//          endDate = Some(LocalDate.parse("2020-09-26")),
//          amount = Some(56534.23),
//          taxPaid = Some(34343.57)
//        )),
//        otherStateBenefits = Some(StateBenefit(
//          benefitId = UUID.fromString("a1e8057e-fbbc-47a8-a8b4-78d9f015c940"),
//          startDate = LocalDate.parse("2018-09-03"),
//          dateIgnored = Some(Instant.parse("2020-01-11T15:23:00Z")),
//          submittedOn = Some(Instant.parse("2020-09-13T15:23:00Z")),
//          endDate = Some(LocalDate.parse("2020-06-03")),
//          amount = Some(56532.45),
//          taxPaid = Some(5656.89)
//        ))
//      )),
//      Some(StateBenefits(
//        incapacityBenefit = Some(List(StateBenefit(
//          benefitId = UUID.fromString("a1e8057e-fbbc-47a8-a8b4-78d9f015c941"),
//          startDate = LocalDate.parse("2018-07-17"),
//          submittedOn = Some(Instant.parse("2020-11-17T19:23:00Z")),
//          endDate = Some(LocalDate.parse("2020-09-23")),
//          amount = Some(45646.78),
//          taxPaid = Some(4544.34),
//          dateIgnored = None
//        ))),
//        statePension = Some(StateBenefit(
//          benefitId = UUID.fromString("a1e8057e-fbbc-47a8-a8b4-78d9f015c943"),
//          startDate = LocalDate.parse("2018-04-03"),
//          submittedOn = Some(Instant.parse("2020-06-11T10:23:00Z")),
//          endDate = Some(LocalDate.parse("2020-09-13")),
//          amount = Some(45642.45),
//          taxPaid = Some(6764.34),
//          dateIgnored = None
//        )),
//        statePensionLumpSum = Some(StateBenefit(
//          benefitId = UUID.fromString("a1e8057e-fbbc-47a8-a8b4-78d9f015c956"),
//          startDate = LocalDate.parse("2019-09-23"),
//          submittedOn = Some(Instant.parse("2020-06-13T05:29:00Z")),
//          endDate = Some(LocalDate.parse("2020-09-26")),
//          amount = Some(34322.34),
//          taxPaid = Some(4564.45),
//          dateIgnored = None
//        )),
//        employmentSupportAllowance = Some(List(StateBenefit(
//          benefitId = UUID.fromString("a1e8057e-fbbc-47a8-a8b4-78d9f015c988"),
//          startDate = LocalDate.parse("2019-09-11"),
//          submittedOn = Some(Instant.parse("2020-02-10T11:20:00Z")),
//          endDate = Some(LocalDate.parse("2020-06-13")),
//          amount = Some(45424.23),
//          taxPaid = Some(23232.34),
//          dateIgnored = None
//        ))),
//        jobSeekersAllowance = Some(List(StateBenefit(
//          benefitId = UUID.fromString("a1e8057e-fbbc-47a8-a8b4-78d9f015c990"),
//          startDate = LocalDate.parse("2019-07-10"),
//          submittedOn = Some(Instant.parse("2020-05-13T14:23:00Z")),
//          endDate = Some(LocalDate.parse("2020-05-11")),
//          amount = Some(34343.78),
//          taxPaid = Some(3433.56),
//          dateIgnored = None
//        ))),
//        bereavementAllowance = Some(StateBenefit(
//          benefitId = UUID.fromString("a1e8057e-fbbc-47a8-a8b4-78d9f015c997"),
//          startDate = LocalDate.parse("2018-08-12"),
//          submittedOn = Some(Instant.parse("2020-02-13T11:23:00Z")),
//          endDate = Some(LocalDate.parse("2020-07-13")),
//          amount = Some(45423.45),
//          taxPaid = Some(4543.64),
//          dateIgnored = None
//        )),
//        otherStateBenefits = Some(StateBenefit(
//          benefitId = UUID.fromString("a1e8057e-fbbc-47a8-a8b4-78d9f015c957"),
//          startDate = LocalDate.parse("2018-01-13"),
//          submittedOn = Some(Instant.parse("2020-09-12T12:23:00Z")),
//          endDate = Some(LocalDate.parse("2020-08-13")),
//          amount = Some(63333.33),
//          taxPaid = Some(4644.45),
//          dateIgnored = None
//        )),
//      )))
//    ),
//    employmentPensions = Some(
//      anEmploymentPensions
//    ),
//    pensionIncome = Some(
//      aPensionIncome
//    )
//  )

  //TODO add view models
  val pensionCYA: PensionsCYAModel = aPensionsCYAEmptyModel

  val pensionDataFull: PensionsUserData = PensionsUserData(
    sessionId, "1234567890", nino, taxYear, isPriorSubmission = true,
    pensionCYA, testClock.now()
  )

  "getAndHandle" should {
    "redirect if no data and redirect is set to true" in {
      mockFind(taxYear, user, Right(None))
      mockFindNoContent(nino, taxYear)
      val response = service.getAndHandle(taxYear, redirectWhenNoPrior = true, user = user)((_, _) => Future(Ok))

      status(response) shouldBe SEE_OTHER
    }

    "return an error if the call failed" in {
      mockFind(taxYear, user, Right(None))
      mockFindFail(nino, taxYear)
      val response = service.getAndHandle(taxYear, user)((_, _) => Future(Ok))

      status(response) shouldBe INTERNAL_SERVER_ERROR
    }

    "return an internal server error if the CYA find failed" in {
      mockFind(taxYear, user, Left(DataNotFound))
      mockFindNoContent(nino, taxYear)
      val response = service.getAndHandle(taxYear, user)((_, _) => Future(Ok))

      status(response) shouldBe INTERNAL_SERVER_ERROR
    }
  }

  ".createOrUpdateSessionData" should {
    "return SEE_OTHER(303) status when createOrUpdate succeeds" in {
      mockCreateOrUpdate(pensionDataFull, Right(()))
      val response = service.createOrUpdateSessionData(user,
        pensionCYA, taxYear, isPriorSubmission = true,
      )(Redirect("400"))(Redirect("303"))

      status(response) shouldBe SEE_OTHER
      redirectUrl(response) shouldBe "303"
    }

    "return BAD_REQUEST(400) status when createOrUpdate fails" in {
      mockCreateOrUpdate(pensionDataFull, Left(DataNotUpdated))
      val response: Future[Result] = service.createOrUpdateSessionData(user,
        pensionCYA, taxYear, isPriorSubmission = true
      )(Redirect("400"))(Redirect("303"))

      status(response) shouldBe SEE_OTHER
      redirectUrl(response) shouldBe "400"
    }
  }

  "generateCyaFromPrior" should {
    "generate a PensionsCYAModel from prior AllPensionsData" in {
      mockCreateOrUpdate(pensionDataFull, Right(()))
      val response = generateCyaFromPrior(anAllPensionDataEmpty)
      response shouldBe aPensionsCYAGeneratedFromPriorEmpty
    }
  }


  ".createOrUpdateSessionData" should {
    "return Right(unit) when createOrUpdate succeeds" in {
      mockCreateOrUpdate(pensionDataFull, Right(()))
      val response = await(service.createOrUpdateSessionData(pensionDataFull))
      response shouldBe Right(())
    }

    "return Left DB Error(400) when createOrUpdate fails" in {
      mockCreateOrUpdate(pensionDataFull, Left(DataNotUpdated))
      val Left(response) = await(service.createOrUpdateSessionData(pensionDataFull))
      response shouldBe a[DatabaseError]
    }
  }
}
