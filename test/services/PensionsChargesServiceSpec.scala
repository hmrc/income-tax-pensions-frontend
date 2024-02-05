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

import builders.AllPensionsDataBuilder.anAllPensionsData
import builders.PensionsCYAModelBuilder.emptyPensionsData
import builders.PensionsUserDataBuilder.aPensionsUserData
import builders.UserBuilder.aUser
import config.{MockIncomeTaxUserDataConnector, MockPensionUserDataRepository, MockPensionsConnector}
import models.mongo.{DataNotFound, DataNotUpdated, MongoError}
import models.pension.charges.CreateUpdatePensionChargesRequestModel
import models.{APIErrorBodyModel, APIErrorModel, IncomeTaxUserData}
import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status.BAD_REQUEST
import utils.UnitTest

class PensionsChargesServiceSpec
    extends UnitTest
    with MockPensionUserDataRepository
    with MockPensionsConnector
    with MockIncomeTaxUserDataConnector
    with ScalaFutures {

  val pensionChargesService = new PensionChargesService(mockPensionUserDataRepository, mockPensionConnectorHelper, mockUserDataConnector)

  val userWithEmptyCya = aPensionsUserData.copy(pensions = emptyPensionsData)

  val unauthorisedSessionUserData =
    aPensionsUserData.copy(pensions = emptyPensionsData.copy(unauthorisedPayments = aPensionsUserData.pensions.unauthorisedPayments))
  val transferIOPSessionUserData =
    aPensionsUserData.copy(pensions =
      emptyPensionsData.copy(transfersIntoOverseasPensions = aPensionsUserData.pensions.transfersIntoOverseasPensions))

  val shortServiceRefundsSessionUserData =
    aPensionsUserData.copy(pensions = emptyPensionsData.copy(shortServiceRefunds = aPensionsUserData.pensions.shortServiceRefunds))

  val annualAllowanceSessionUserData =
    aPensionsUserData.copy(pensions = emptyPensionsData.copy(pensionsAnnualAllowances = aPensionsUserData.pensions.pensionsAnnualAllowances))

  val priorPensionChargesData = IncomeTaxUserData(Some(anAllPensionsData)).pensions.flatMap(_.pensionCharges)
  val unauthorisedPaymentsRequestModel = CreateUpdatePensionChargesRequestModel(
    pensionSavingsTaxCharges = priorPensionChargesData.flatMap(_.pensionSavingsTaxCharges),
    pensionSchemeOverseasTransfers = priorPensionChargesData.flatMap(_.pensionSchemeOverseasTransfers),
    pensionSchemeUnauthorisedPayments = Some(unauthorisedSessionUserData.pensions.unauthorisedPayments.toUnauth),
    pensionContributions = priorPensionChargesData.flatMap(_.pensionContributions),
    overseasPensionContributions = priorPensionChargesData.flatMap(_.overseasPensionContributions)
  )
  val transferIOPRequestModel = CreateUpdatePensionChargesRequestModel(
    pensionSavingsTaxCharges = priorPensionChargesData.flatMap(_.pensionSavingsTaxCharges),
    pensionSchemeOverseasTransfers = Some(transferIOPSessionUserData.pensions.transfersIntoOverseasPensions.toTransfersIOP),
    pensionSchemeUnauthorisedPayments = priorPensionChargesData.flatMap(_.pensionSchemeUnauthorisedPayments),
    pensionContributions = priorPensionChargesData.flatMap(_.pensionContributions),
    overseasPensionContributions = priorPensionChargesData.flatMap(_.overseasPensionContributions)
  )

  val shortServiceRefundsRequestModel = CreateUpdatePensionChargesRequestModel(
    pensionSavingsTaxCharges = priorPensionChargesData.flatMap(_.pensionSavingsTaxCharges),
    pensionSchemeOverseasTransfers = priorPensionChargesData.flatMap(_.pensionSchemeOverseasTransfers),
    pensionSchemeUnauthorisedPayments = priorPensionChargesData.flatMap(_.pensionSchemeUnauthorisedPayments),
    pensionContributions = priorPensionChargesData.flatMap(_.pensionContributions),
    overseasPensionContributions = Some(shortServiceRefundsSessionUserData.pensions.shortServiceRefunds.toOverseasPensionContributions)
  )

  val annualAllowanceRequestModel = {
    val annualAllowanceChargesModel = annualAllowanceSessionUserData.pensions.pensionsAnnualAllowances
      .toAnnualAllowanceChargesModel(Some(anAllPensionsData))
    CreateUpdatePensionChargesRequestModel(
      pensionSavingsTaxCharges = annualAllowanceChargesModel.pensionSavingsTaxCharges,
      pensionSchemeOverseasTransfers = priorPensionChargesData.flatMap(_.pensionSchemeOverseasTransfers),
      pensionSchemeUnauthorisedPayments = priorPensionChargesData.flatMap(_.pensionSchemeUnauthorisedPayments),
      pensionContributions = annualAllowanceChargesModel.pensionContributions,
      overseasPensionContributions = priorPensionChargesData.flatMap(_.overseasPensionContributions)
    )
  }

  ".saveUnauthorisedViewModel" should {

    "return Right(Unit) when model is saved successfully and unauthorised cya is cleared from DB" in {

      mockFind(taxYear, aUser, Right(Option(unauthorisedSessionUserData)))
      mockFind(aUser.nino, taxYear, IncomeTaxUserData(Some(anAllPensionsData)))

      mockSavePensionChargesSessionData(nino, taxYear, unauthorisedPaymentsRequestModel, Right(()))
      mockCreateOrUpdate(userWithEmptyCya, Right(()))

      val result = await(pensionChargesService.saveUnauthorisedViewModel(aUser, taxYear))
      result shouldBe Right(())
    }

    "return Left(DataNotFound) when user can not be found in DB" in {
      mockFind(taxYear, aUser, Left(DataNotFound))
      mockFind(aUser.nino, taxYear, IncomeTaxUserData(None))

      val result = await(pensionChargesService.saveUnauthorisedViewModel(aUser, taxYear))
      result shouldBe Left(DataNotFound)
    }

    "return Left(APIErrorModel) when pension connector could not be connected" in {
      mockFind(taxYear, aUser, Right(Option(unauthorisedSessionUserData)))
      mockFind(aUser.nino, taxYear, IncomeTaxUserData(Some(anAllPensionsData)))

      mockSavePensionChargesSessionData(
        nino,
        taxYear,
        unauthorisedPaymentsRequestModel,
        Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel("FAILED", "failed"))))
      mockCreateOrUpdate(userWithEmptyCya, Left(MongoError("Failed to connect to database")))

      val result = await(pensionChargesService.saveUnauthorisedViewModel(aUser, taxYear))
      result shouldBe Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel("FAILED", "failed")))
    }

    "return Left(DataNotUpdated) when data could not be updated" in {
      mockFind(taxYear, aUser, Right(Option(unauthorisedSessionUserData)))
      mockFind(aUser.nino, taxYear, IncomeTaxUserData(Some(anAllPensionsData)))

      mockSavePensionChargesSessionData(nino, taxYear, unauthorisedPaymentsRequestModel, Right(()))
      mockCreateOrUpdate(userWithEmptyCya, Left(DataNotUpdated))

      val result = await(pensionChargesService.saveUnauthorisedViewModel(aUser, taxYear))
      result shouldBe Left(DataNotUpdated)
    }

  }

  ".saveTransfersIntoOverseasPensionsViewModel" should {

    "return Right(Unit) when model is saved successfully and transfersIOP cya is cleared from DB" in {
      mockFind(taxYear, aUser, Right(Option(transferIOPSessionUserData)))
      mockFind(aUser.nino, taxYear, IncomeTaxUserData(Some(anAllPensionsData)))

      mockSavePensionChargesSessionData(nino, taxYear, transferIOPRequestModel, Right(()))
      mockCreateOrUpdate(userWithEmptyCya, Right(()))

      val result = await(pensionChargesService.saveTransfersIntoOverseasPensionsViewModel(aUser, taxYear))
      result shouldBe Right(())
    }

    "return Left(DataNotFound) when user can not be found in DB" in {
      mockFind(taxYear, aUser, Left(DataNotFound))
      mockFind(aUser.nino, taxYear, IncomeTaxUserData(None))

      val result = await(pensionChargesService.saveTransfersIntoOverseasPensionsViewModel(aUser, taxYear))
      result shouldBe Left(DataNotFound)
    }

    "return Left(APIErrorModel) when pension connector could not be connected" in {
      mockFind(taxYear, aUser, Right(Option(transferIOPSessionUserData)))
      mockFind(aUser.nino, taxYear, IncomeTaxUserData(Some(anAllPensionsData)))

      mockSavePensionChargesSessionData(
        nino,
        taxYear,
        transferIOPRequestModel,
        Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel("FAILED", "failed"))))
      mockCreateOrUpdate(userWithEmptyCya, Left(MongoError("Failed to connect to database")))

      val result = await(pensionChargesService.saveTransfersIntoOverseasPensionsViewModel(aUser, taxYear))
      result shouldBe Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel("FAILED", "failed")))
    }

    "return Left(DataNotUpdated) when data could not be updated" in {
      mockFind(taxYear, aUser, Right(Option(transferIOPSessionUserData)))
      mockFind(aUser.nino, taxYear, IncomeTaxUserData(Some(anAllPensionsData)))

      mockSavePensionChargesSessionData(nino, taxYear, transferIOPRequestModel, Right(()))
      mockCreateOrUpdate(userWithEmptyCya, Left(DataNotUpdated))

      val result = await(pensionChargesService.saveTransfersIntoOverseasPensionsViewModel(aUser, taxYear))
      result shouldBe Left(DataNotUpdated)
    }
  }

  ".saveShortServiceRefundsViewModel" should {

    "return Right(Unit) when model is saved successfully and shortServiceRefunds cya is cleared from DB" in {
      mockFind(taxYear, aUser, Right(Option(shortServiceRefundsSessionUserData)))
      mockFind(aUser.nino, taxYear, IncomeTaxUserData(Some(anAllPensionsData)))

      mockSavePensionChargesSessionData(nino, taxYear, shortServiceRefundsRequestModel, Right(()))
      mockCreateOrUpdate(userWithEmptyCya, Right(()))

      val result = await(pensionChargesService.saveShortServiceRefundsViewModel(aUser, taxYear))
      result shouldBe Right(())
    }

    "return Left(DataNotFound) when user can not be found in DB" in {
      mockFind(taxYear, aUser, Left(DataNotFound))
      mockFind(aUser.nino, taxYear, IncomeTaxUserData(None))

      val result = await(pensionChargesService.saveShortServiceRefundsViewModel(aUser, taxYear))
      result shouldBe Left(DataNotFound)
    }

    "return Left(APIErrorModel) when pension connector could not be connected" in {
      mockFind(taxYear, aUser, Right(Option(shortServiceRefundsSessionUserData)))
      mockFind(aUser.nino, taxYear, IncomeTaxUserData(Some(anAllPensionsData)))

      mockSavePensionChargesSessionData(
        nino,
        taxYear,
        shortServiceRefundsRequestModel,
        Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel("FAILED", "failed"))))
      mockCreateOrUpdate(userWithEmptyCya, Left(MongoError("Failed to connect to database")))

      val result = await(pensionChargesService.saveShortServiceRefundsViewModel(aUser, taxYear))
      result shouldBe Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel("FAILED", "failed")))
    }

    "return Left(DataNotUpdated) when data could not be updated" in {
      mockFind(taxYear, aUser, Right(Option(shortServiceRefundsSessionUserData)))
      mockFind(aUser.nino, taxYear, IncomeTaxUserData(Some(anAllPensionsData)))

      mockSavePensionChargesSessionData(nino, taxYear, shortServiceRefundsRequestModel, Right(()))
      mockCreateOrUpdate(userWithEmptyCya, Left(DataNotUpdated))

      val result = await(pensionChargesService.saveShortServiceRefundsViewModel(aUser, taxYear))
      result shouldBe Left(DataNotUpdated)
    }
  }
  ".saveAnnualAllowanceViewModel" should {

    "return Right(Unit) when model is saved successfully and shortServiceRefunds cya is cleared from DB" in {
      mockFind(taxYear, aUser, Right(Option(annualAllowanceSessionUserData)))
      mockFind(aUser.nino, taxYear, IncomeTaxUserData(Some(anAllPensionsData)))

      mockSavePensionChargesSessionData(nino, taxYear, annualAllowanceRequestModel, Right(()))
      mockCreateOrUpdate(userWithEmptyCya, Right(()))

      val result = await(pensionChargesService.saveAnnualAllowanceViewModel(aUser, taxYear))
      result shouldBe Right(())
    }

    "return Left(DataNotFound) when user can not be found in DB" in {
      mockFind(taxYear, aUser, Left(DataNotFound))
      mockFind(aUser.nino, taxYear, IncomeTaxUserData(None))

      val result = await(pensionChargesService.saveAnnualAllowanceViewModel(aUser, taxYear))
      result shouldBe Left(DataNotFound)
    }

    "return Left(APIErrorModel) when pension connector could not be connected" in {
      mockFind(taxYear, aUser, Right(Option(annualAllowanceSessionUserData)))
      mockFind(aUser.nino, taxYear, IncomeTaxUserData(Some(anAllPensionsData)))

      mockSavePensionChargesSessionData(
        nino,
        taxYear,
        annualAllowanceRequestModel,
        Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel("FAILED", "failed"))))
      mockCreateOrUpdate(userWithEmptyCya, Left(MongoError("Failed to connect to database")))

      val result = await(pensionChargesService.saveAnnualAllowanceViewModel(aUser, taxYear))
      result shouldBe Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel("FAILED", "failed")))
    }

    "return Left(DataNotUpdated) when data could not be updated" in {
      mockFind(taxYear, aUser, Right(Option(annualAllowanceSessionUserData)))
      mockFind(aUser.nino, taxYear, IncomeTaxUserData(Some(anAllPensionsData)))

      mockSavePensionChargesSessionData(nino, taxYear, annualAllowanceRequestModel, Right(()))
      mockCreateOrUpdate(userWithEmptyCya, Left(DataNotUpdated))

      val result = await(pensionChargesService.saveAnnualAllowanceViewModel(aUser, taxYear))
      result shouldBe Left(DataNotUpdated)
    }
  }
}
