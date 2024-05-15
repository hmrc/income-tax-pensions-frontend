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

package services.redirects

import models.mongo.{PensionsCYAModel, PensionsUserData}
import play.api.mvc.{Call, Result}
import play.api.mvc.Results.Redirect
import utils.UnitTest
import builders.PensionsUserDataBuilder.aPensionsUserData
import builders.TransferPensionSchemeBuilder.{aNonUkTransferPensionScheme, anEmptyTransferPensionScheme}
import builders.TransfersIntoOverseasPensionsViewModelBuilder.aTransfersIntoOverseasPensionsViewModel
import controllers.pensions.transferIntoOverseasPensions.routes._
import models.pension.charges.TransferPensionScheme
import play.api.http.Status.SEE_OTHER
import services.redirects.TransfersIntoOverseasPensionsPages.{
  DidYouTransferIntoAnOverseasPensionSchemePage,
  OverseasTransferChargeAmountPage,
  PensionSchemeDetailsPage,
  RemoveSchemePage,
  TaxOnPensionSchemesAmountPage,
  TransferIntoOverseasPensionsCYA
}
import services.redirects.TransfersIntoOverseasPensionsRedirects.{cyaPageCall, indexCheckThenJourneyCheck, journeyCheck, redirectForSchemeLoop}

import scala.concurrent.Future

class TransfersIntoOverseasPensionsRedirectsSpec extends UnitTest {

  private val cyaData: PensionsCYAModel                                        = PensionsCYAModel.emptyModels
  private val journeyStartCall: Call                                           = TransferPensionSavingsController.show(taxYear)
  private val journeyStartRedirect: Option[Result]                             = Some(Redirect(journeyStartCall))
  private val schemeStartCall: Call                                            = OverseasTransferChargePaidController.show(taxYear, None)
  private val schemeDetailsCall: Call                                          = TransferPensionsSchemeController.show(taxYear, None)
  private val schemeSummaryCall: Call                                          = TransferChargeSummaryController.show(taxYear)
  private val checkYourAnswersCall: Call                                       = TransferIntoOverseasPensionsCYAController.show(currentTaxYear)
  private val continueToContextualRedirect: PensionsUserData => Future[Result] = aPensionsUserData => Future.successful(Redirect(schemeDetailsCall))

  ".cyaPageCall" should {
    "return a redirect call to the cya page" in {
      cyaPageCall(taxYear) shouldBe checkYourAnswersCall
    }
  }

  ".indexCheckThenJourneyCheck" when {
    "index is valid" should {
      "return PensionsUserData if previous questions are answered and journey is valid" in {
        val result =
          indexCheckThenJourneyCheck(data = aPensionsUserData, optIndex = Some(0), currentPage = PensionSchemeDetailsPage, taxYear = taxYear)(
            continueToContextualRedirect)
        val statusHeader   = await(result.map(_.header.status))
        val locationHeader = await(result.map(_.header.headers).map(_.get("Location")))

        statusHeader shouldBe SEE_OTHER
        locationHeader shouldBe Some(schemeDetailsCall.url)
      }

      "redirect to first page in journey" when {
        "previous questions are unanswered" in {
          val incompleteJourney = cyaData.copy(transfersIntoOverseasPensions = aTransfersIntoOverseasPensionsViewModel.copy(
            transferPensionScheme = Seq(aNonUkTransferPensionScheme, anEmptyTransferPensionScheme)
          ))
          val result = indexCheckThenJourneyCheck(
            data = aPensionsUserData.copy(pensions = incompleteJourney),
            optIndex = Some(1),
            currentPage = PensionSchemeDetailsPage,
            taxYear = taxYear)(continueToContextualRedirect)
          val statusHeader   = await(result.map(_.header.status))
          val locationHeader = await(result.map(_.header.headers).map(_.get("Location")))

          statusHeader shouldBe SEE_OTHER
          locationHeader shouldBe Some(journeyStartCall.url)
        }
        "journey is invalid" in {

          val invalidJourney = cyaData.copy(transfersIntoOverseasPensions = aTransfersIntoOverseasPensionsViewModel.copy(
            transferPensionSavings = Some(false)
          ))
          val result = indexCheckThenJourneyCheck(
            data = aPensionsUserData.copy(pensions = invalidJourney),
            optIndex = Some(0),
            currentPage = OverseasTransferChargeAmountPage,
            taxYear = taxYear)(continueToContextualRedirect)

          val statusHeader   = await(result.map(_.header.status))
          val locationHeader = await(result.map(_.header.headers).map(_.get("Location")))

          statusHeader shouldBe SEE_OTHER
          locationHeader shouldBe Some(journeyStartCall.url)
        }
      }
    }

    "index is invalid" should {
      "redirect to the first page in journey when previous questions are unanswered" in {
        val cyaModel = cyaData.copy(transfersIntoOverseasPensions =
          aTransfersIntoOverseasPensionsViewModel.copy(transferPensionScheme = Seq(aNonUkTransferPensionScheme, anEmptyTransferPensionScheme)))

        val result = indexCheckThenJourneyCheck(
          data = aPensionsUserData.copy(pensions = cyaModel),
          optIndex = Some(11),
          currentPage = PensionSchemeDetailsPage,
          taxYear = taxYear)(continueToContextualRedirect)

        val statusHeader   = await(result.map(_.header.status))
        val locationHeader = await(result.map(_.header.headers).map(_.get("Location")))

        statusHeader shouldBe SEE_OTHER
        locationHeader shouldBe Some(journeyStartCall.url)
      }
      "redirect to the first page in journey when there are no schemes" in {

        val CYAModel = cyaData.copy(transfersIntoOverseasPensions = aTransfersIntoOverseasPensionsViewModel.copy(transferPensionScheme = Seq.empty))

        val result = indexCheckThenJourneyCheck(
          data = aPensionsUserData.copy(pensions = CYAModel),
          optIndex = Some(2),
          currentPage = PensionSchemeDetailsPage,
          taxYear = taxYear)(continueToContextualRedirect)
        val statusHeader   = await(result.map(_.header.status))
        val locationHeader = await(result.map(_.header.headers).map(_.get("Location")))

        statusHeader shouldBe SEE_OTHER
        locationHeader shouldBe Some(journeyStartCall.url)
      }

      "redirect to the scheme summary page when schemes already exist" in {
        val result: Future[Result] =
          indexCheckThenJourneyCheck(data = aPensionsUserData, optIndex = Some(11), currentPage = PensionSchemeDetailsPage, taxYear = taxYear)(
            continueToContextualRedirect)
        val statusHeader   = await(result.map(_.header.status))
        val locationHeader = await(result.map(_.header.headers).map(_.get("Location")))

        statusHeader shouldBe SEE_OTHER
        locationHeader shouldBe Some(schemeSummaryCall.url)
      }
    }
  }

  ".redirectForSchemeLoop" should {
    "return a Call to the first page in scheme loop when 'schemes' is empty" in {
      val emptySchemes: Seq[TransferPensionScheme] = Seq.empty
      val result                                   = redirectForSchemeLoop(emptySchemes, taxYear)

      result shouldBe schemeStartCall
    }

    "return a Call to the scheme summary page when 'schemes' already exist" in {
      val existingSchemes: Seq[TransferPensionScheme] = Seq(
        TransferPensionScheme(
          ukTransferCharge = Some(false),
          name = Some("UK TPS"),
          pstr = Some("12345678RA"),
          qops = None,
          providerAddress = Some("Some address 1"),
          alphaTwoCountryCode = None,
          alphaThreeCountryCode = None
        )
      )
      val result = redirectForSchemeLoop(existingSchemes, taxYear)
      result shouldBe schemeStartCall

    }
  }

  ".journeyCheck" should {
    "return None if page is valid and all previous questions have been answered" when {
      "current page is empty and at end of journey so far" in {

        val data1 = cyaData.copy(transfersIntoOverseasPensions = aTransfersIntoOverseasPensionsViewModel.copy(
          transferPensionSavings = Some(true),
          overseasTransferCharge = Some(true),
          overseasTransferChargeAmount = Some(500)
        ))
        val result1 = journeyCheck(OverseasTransferChargeAmountPage, data1, taxYear)

        val data2 = cyaData.copy(transfersIntoOverseasPensions = aTransfersIntoOverseasPensionsViewModel.copy(
          transferPensionSavings = Some(true),
          overseasTransferCharge = Some(true),
          overseasTransferChargeAmount = Some(500),
          pensionSchemeTransferCharge = Some(true),
          pensionSchemeTransferChargeAmount = Some(1000.00),
          transferPensionScheme = Seq(anEmptyTransferPensionScheme.copy(ukTransferCharge = Some(false)))
        ))

        val result2 = journeyCheck(PensionSchemeDetailsPage, data2, taxYear, Some(0))

        result1 shouldBe None
        result2 shouldBe None
      }

      "current page is pre-filled and at end of journey so far" in {
        val data1 = cyaData.copy(transfersIntoOverseasPensions = aTransfersIntoOverseasPensionsViewModel.copy(
          transferPensionSavings = Some(true),
          overseasTransferCharge = Some(false)
        ))
        val result1 = journeyCheck(OverseasTransferChargeAmountPage, data1, taxYear)

        val data2 = cyaData.copy(transfersIntoOverseasPensions = aTransfersIntoOverseasPensionsViewModel.copy(
          transferPensionSavings = Some(true),
          overseasTransferCharge = Some(true),
          overseasTransferChargeAmount = Some(500),
          pensionSchemeTransferCharge = Some(true),
          pensionSchemeTransferChargeAmount = Some(1000.00),
          transferPensionScheme = Seq(aNonUkTransferPensionScheme)
        ))
        val result2 = journeyCheck(PensionSchemeDetailsPage, data2, taxYear)
        result1 shouldBe None
        result2 shouldBe None
      }

      "current page is pre-filled and mid-journey" in {
        val data1 = cyaData.copy(transfersIntoOverseasPensions = aTransfersIntoOverseasPensionsViewModel.copy(
          transferPensionSavings = Some(true),
          overseasTransferCharge = Some(true),
          overseasTransferChargeAmount = Some(500),
          pensionSchemeTransferCharge = Some(true),
          pensionSchemeTransferChargeAmount = Some(1000.00)
        ))
        val result1 = journeyCheck(DidYouTransferIntoAnOverseasPensionSchemePage, data1, taxYear)

        val data2   = cyaData.copy(transfersIntoOverseasPensions = aTransfersIntoOverseasPensionsViewModel)
        val result2 = journeyCheck(TaxOnPensionSchemesAmountPage, data2, taxYear, Some(1))

        result1 shouldBe None
        result2 shouldBe None
      }

      "previous page is unanswered but invalid and previous valid question has been answered" in {
        val data = cyaData.copy(transfersIntoOverseasPensions = aTransfersIntoOverseasPensionsViewModel.copy(
          transferPensionSavings = Some(false)
        ))

        val result = journeyCheck(TransferIntoOverseasPensionsCYA, data, taxYear)
        result shouldBe None
      }

      "on the RemoveSchemePage and schemes exist" in {
        val data   = cyaData.copy(transfersIntoOverseasPensions = aTransfersIntoOverseasPensionsViewModel)
        val result = journeyCheck(RemoveSchemePage, data, taxYear)

        result shouldBe None
      }
    }

    "return Some(redirect) with redirect to first page in journey" when {
      "previous question is unanswered" in {
        val data1   = cyaData
        val result1 = journeyCheck(OverseasTransferChargeAmountPage, data1, taxYear)

        val data2 = cyaData.copy(transfersIntoOverseasPensions = aTransfersIntoOverseasPensionsViewModel.copy(
          transferPensionScheme = Seq(anEmptyTransferPensionScheme)
        ))
        val result2 = journeyCheck(PensionSchemeDetailsPage, data2, taxYear)

        result1 shouldBe journeyStartRedirect
        result2 shouldBe journeyStartRedirect
      }

      "current page is invalid in journey" in {
        val data = cyaData.copy(transfersIntoOverseasPensions = aTransfersIntoOverseasPensionsViewModel.copy(
          transferPensionSavings = Some(false)
        ))
        val result = journeyCheck(OverseasTransferChargeAmountPage, data, taxYear)

        result shouldBe journeyStartRedirect
      }
    }
  }

}
