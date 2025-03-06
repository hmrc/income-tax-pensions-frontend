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

package services


import builders.PensionsUserDataBuilder.{aPensionsUserData, user}
import builders.ShortServiceRefundsViewModelBuilder.aShortServiceRefundsViewModel
import cats.data.EitherT
import common.Nino
import config.MockPensionsConnector
import models.APIErrorModel
import models.mongo.PensionsUserData
import models.pension.Journey
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.wordspec.AnyWordSpecLike
import support.mocks.MockPensionSessionService
import uk.gov.hmrc.http.HeaderCarrier
import utils.CommonData._

import scala.concurrent.{ExecutionContext, Future}

class PensionsServiceImplSpec extends AnyWordSpecLike with MockPensionsConnector with MockPensionSessionService {

  val service: PensionsServiceImpl = new PensionsServiceImpl(mockPensionsConnector, mockPensionSessionService)
  val session: PensionsUserData = aPensionsUserData
  val nino: Nino = Nino("nino")
  override implicit val hc: HeaderCarrier = HeaderCarrier()

  "upsertPaymentsIntoPensions" should {
    "remove answers after submission" in {
      mockSavePaymentsIntoPensions(nino, currTaxYear)
      mockClearSessionOnSuccess(Journey.PaymentsIntoPensions, session)
      val result = service.upsertPaymentsIntoPensions(user, currTaxYear, session)(hc, ec).value.futureValue

      assert(result.isRight === true)
    }
  }

  "upsertUkIncomePension" should {
    "remove answers after submission" in {
      mockSaveUkPensionIncome(nino, currTaxYear)
      mockClearSessionOnSuccess(Journey.UkPensionIncome, session)
      val result = service.upsertUkPensionIncome(user, currTaxYear, session)(hc, ec).value.futureValue

      assert(result.isRight === true)
    }
  }

  "upsertAnnualAllowances" should {
    "remove answers after submission" in {
      mockSaveAnnualAllowances(nino, currTaxYear)
      mockClearSessionOnSuccess(Journey.AnnualAllowances, session)
      val result = service.upsertAnnualAllowances(user, currTaxYear, session)(hc, ec).value.futureValue

      assert(result.isRight === true)
    }
  }

  "upsertTransferIntoOverseasPensions" should {
    "remove answers after submission" in {
      val session = aPensionsUserData
      mockSaveTransfersIntoOverseasPensions(nino, currTaxYear)
      mockClearSessionOnSuccess(Journey.TransferIntoOverseasPensions, session)
      val result = service.upsertTransferIntoOverseasPensions(user, currTaxYear, session)(hc, ec).value.futureValue

      assert(result.isRight === true)
    }
  }

  "upsertIncomeFromOverseasPensions" should {
    "remove answers after submission" in {
      mockSaveIncomeFromOverseasPensions(nino, currTaxYear)
      mockClearSessionOnSuccess(Journey.IncomeFromOverseasPensions, session)
      val result = service.upsertIncomeFromOverseasPensions(user, currTaxYear, session)(hc, ec).value.futureValue

      assert(result.isRight === true)
    }
  }


  //  def mockShortServiceRefunds(nino: Nino,
  //                              taxYear: TaxYear,
  //                              shortServiceRefundsViewModel: ShortServiceRefundsViewModel,
  //                              result: Either[APIErrorModel, Unit] = Right(())
  //                             ): ScalaOngoingStubbing[ApiResultT[Unit]] = {
  //
  //    when(mockPensionsConnector.saveShortServiceRefunds(eqTo(nino), eqTo(taxYear), eqTo(shortServiceRefundsViewModel))
  //    (any[HeaderCarrier], any[ExecutionContext]))
  //      .thenReturn(EitherT.fromEither[Future](result))
  //
  ////    when(mockPensionsConnector.saveShortServiceRefunds(eqTo(nino), eqTo(taxYear), eqTo(shortServiceRefundsViewModel))
  ////    (any[HeaderCarrier], any[ExecutionContext]))
  ////      .thenReturn(EitherT.rightT[Future, APIErrorModel](()))
  ////  (mockPensionsConnector
  //    //      .saveShortServiceRefunds(_: Nino, _: TaxYear, _: ShortServiceRefundsViewModel)(_: HeaderCarrier, _: ExecutionContext))
  //    //      .expects(*, *, *, *, *)
  //    //      .returns(EitherT.rightT[Future, APIErrorModel](()))
  //    //      .anyNumberOfTimes()
  //  }

  //val mockPensionSessionService = mock[PensionSessionService]
  //val service = new PensionsServiceImpl(mockPensionsConnector, mockPensionSessionService)

  "upsertShortServiceRefunds" should {
    "remove answers after submission" in {
          when(mockPensionsConnector.saveShortServiceRefunds(eqTo(nino), eqTo(currTaxYear), eqTo(aShortServiceRefundsViewModel))
          (any[HeaderCarrier], any[ExecutionContext]))
            .thenReturn(EitherT.rightT[Future, APIErrorModel](()))

      mockClearSessionOnSuccess(Journey.ShortServiceRefunds, session)

      val result = service.upsertShortServiceRefunds(eqTo(user),eqTo(currTaxYear), eqTo(session))(any[HeaderCarrier], any[ExecutionContext]).value.futureValue

      assert(result.isRight)

      //assert(result.isRight === true)
    }
  }
}
