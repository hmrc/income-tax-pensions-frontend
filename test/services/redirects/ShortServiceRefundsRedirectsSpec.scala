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

import builders.OverseasRefundPensionSchemeBuilder.{anEmptyOverseasRefundPensionScheme, anOverseasRefundPensionSchemeWithoutUkRefundCharge}
import builders.PensionsUserDataBuilder.aPensionsUserData
import builders.ShortServiceRefundsViewModelBuilder._
import controllers.pensions.shortServiceRefunds.routes._
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.pension.charges.{OverseasRefundPensionScheme, ShortServiceRefundsViewModel}
import play.api.http.Status.SEE_OTHER
import play.api.mvc.Results.Redirect
import play.api.mvc.{Call, Result}
import services.redirects.ShortServiceRefundsPages._
import services.redirects.ShortServiceRefundsRedirects.{cyaPageCall, indexCheckThenJourneyCheck, journeyCheck, redirectForSchemeLoop}
import utils.UnitTest

import scala.concurrent.Future

class ShortServiceRefundsRedirectsSpec extends UnitTest {

  private val cyaData: PensionsCYAModel                                        = PensionsCYAModel.emptyModels
  private val journeyStartCall: Call                                           = TaxableRefundAmountController.show(taxYear)
  private val journeyStartRedirect: Option[Result]                             = Some(Redirect(journeyStartCall))
  private val schemeStartCall: Call                                            = TaxOnShortServiceRefundController.show(taxYear, None)
  private val schemeDetailsCall: Call                                          = ShortServicePensionsSchemeController.show(taxYear, None)
  private val schemeSummaryCall: Call                                          = RefundSummaryController.show(taxYear)
  private val checkYourAnswersCall: Call                                       = ShortServiceRefundsCYAController.show(taxYear)
  private val continueToContextualRedirect: PensionsUserData => Future[Result] = aPensionsUserData => Future.successful(Redirect(schemeDetailsCall))
  private val continueToSummaryRedirect: PensionsUserData => Future[Result]    = aPensionsUserData => Future.successful(Redirect(schemeSummaryCall))

  ".cyaPageCall" should {
    "return a redirect call to the cya page" in {
      cyaPageCall(taxYear) shouldBe checkYourAnswersCall
    }
  }

  ".indexCheckThenJourneyCheck" when {
    "index is valid" should {
      "return PensionsUserData if previous questions are answered and journey is valid" in {
        val result = indexCheckThenJourneyCheck(data = aPensionsUserData, optIndex = Some(0), currentPage = SchemeDetailsPage, taxYear = taxYear)(
          continueToContextualRedirect)
        val statusHeader   = await(result.map(_.header.status))
        val locationHeader = await(result.map(_.header.headers).map(_.get("Location")))

        statusHeader shouldBe SEE_OTHER
        locationHeader shouldBe Some(schemeDetailsCall.url)
      }
      "redirect to first page in journey" when {
        "previous questions are unanswered" in {
          val incompleteJourney = cyaData.copy(shortServiceRefunds = aShortServiceRefundsViewModel.copy(
            refundPensionScheme = Seq(anOverseasRefundPensionSchemeWithoutUkRefundCharge, anEmptyOverseasRefundPensionScheme)
          ))
          val result = indexCheckThenJourneyCheck(
            data = aPensionsUserData.copy(pensions = incompleteJourney),
            optIndex = Some(1),
            currentPage = SchemeDetailsPage,
            taxYear = taxYear)(continueToContextualRedirect)
          val statusHeader   = await(result.map(_.header.status))
          val locationHeader = await(result.map(_.header.headers).map(_.get("Location")))

          statusHeader shouldBe SEE_OTHER
          locationHeader shouldBe Some(journeyStartCall.url)
        }
        "journey is invalid" in {
          val invalidJourney = cyaData.copy(shortServiceRefunds = aShortServiceRefundsViewModel.copy(
            shortServiceRefund = Some(false)
          ))
          val result = indexCheckThenJourneyCheck(
            data = aPensionsUserData.copy(pensions = invalidJourney),
            optIndex = Some(0),
            currentPage = NonUkTaxRefundsAmountPage,
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
        val cyaModel = cyaData.copy(shortServiceRefunds = aShortServiceRefundsViewModel.copy(
          refundPensionScheme = Seq(anOverseasRefundPensionSchemeWithoutUkRefundCharge, anEmptyOverseasRefundPensionScheme)
        ))
        val result = indexCheckThenJourneyCheck(
          data = aPensionsUserData.copy(pensions = cyaModel),
          optIndex = Some(8),
          currentPage = SchemeDetailsPage,
          taxYear = taxYear)(continueToContextualRedirect)
        val statusHeader   = await(result.map(_.header.status))
        val locationHeader = await(result.map(_.header.headers).map(_.get("Location")))

        statusHeader shouldBe SEE_OTHER
        locationHeader shouldBe Some(journeyStartCall.url)
      }
      "redirect to the first page in journey when there are no schemes" in {
        val cyaModel = cyaData.copy(shortServiceRefunds = aShortServiceRefundsViewModel.copy(refundPensionScheme = Seq.empty))
        val result = indexCheckThenJourneyCheck(
          data = aPensionsUserData.copy(pensions = cyaModel),
          optIndex = Some(2),
          currentPage = SchemeDetailsPage,
          taxYear = taxYear)(continueToContextualRedirect)
        val statusHeader   = await(result.map(_.header.status))
        val locationHeader = await(result.map(_.header.headers).map(_.get("Location")))

        statusHeader shouldBe SEE_OTHER
        locationHeader shouldBe Some(journeyStartCall.url)
      }
      "redirect to the first page in scheme loop when trying to load the summary page but there are no schemes" in {
        val cyaModel = cyaData.copy(shortServiceRefunds = aShortServiceRefundsViewModel.copy(refundPensionScheme = Seq.empty))
        val result = indexCheckThenJourneyCheck(
          data = aPensionsUserData.copy(pensions = cyaModel),
          optIndex = Some(2),
          currentPage = RefundSchemesSummaryPage,
          taxYear = taxYear)(continueToSummaryRedirect)
        val statusHeader   = await(result.map(_.header.status))
        val locationHeader = await(result.map(_.header.headers).map(_.get("Location")))

        statusHeader shouldBe SEE_OTHER
        locationHeader shouldBe Some(schemeSummaryCall.url)
      }
      "redirect to the scheme summary page when schemes already exist" in {
        val result: Future[Result] =
          indexCheckThenJourneyCheck(data = aPensionsUserData, optIndex = Some(8), currentPage = SchemeDetailsPage, taxYear = taxYear)(
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
      val emptySchemes: Seq[OverseasRefundPensionScheme] = Seq.empty
      val result                                         = redirectForSchemeLoop(emptySchemes, taxYear)

      result shouldBe schemeStartCall
    }
    "return a Call to the scheme summary page when 'schemes' already exist" in {
      val existingSchemes: Seq[OverseasRefundPensionScheme] = Seq(
        OverseasRefundPensionScheme(
          ukRefundCharge = Some(false),
          name = Some("Overseas Refund Scheme Name"),
          pensionSchemeTaxReference = None,
          qualifyingRecognisedOverseasPensionScheme = Some("QOPS123456"),
          providerAddress = Some("Scheme Address"),
          alphaTwoCountryCode = Some("FR"),
          alphaThreeCountryCode = Some("FRA")
        ),
        OverseasRefundPensionScheme(
          ukRefundCharge = Some(true),
          name = Some("Overseas Refund Scheme Name"),
          pensionSchemeTaxReference = Some("12345678RA"),
          qualifyingRecognisedOverseasPensionScheme = None,
          providerAddress = Some("Scheme Address"),
          alphaTwoCountryCode = None,
          alphaThreeCountryCode = None
        )
      )
      val result = redirectForSchemeLoop(existingSchemes, taxYear)

      result shouldBe schemeSummaryCall
    }
  }

  ".journeyCheck" should {
    "return None if page is valid and all previous questions have been answered" when {
      "current page is empty and at end of journey so far" in {
        val ssrData1 = cyaData.copy(shortServiceRefunds = ShortServiceRefundsViewModel(
          shortServiceRefund = Some(true),
          shortServiceRefundCharge = Some(500)
        ))
        val result1 = journeyCheck(NonUkTaxRefundsAmountPage, ssrData1, taxYear)
        val ssrData2 = cyaData.copy(shortServiceRefunds = ShortServiceRefundsViewModel(
          shortServiceRefund = Some(true),
          shortServiceRefundCharge = Some(500),
          shortServiceRefundTaxPaid = Some(true),
          shortServiceRefundTaxPaidCharge = Some(200),
          refundPensionScheme = Seq(anEmptyOverseasRefundPensionScheme.copy(ukRefundCharge = Some(false)))
        ))
        val result2 = journeyCheck(SchemeDetailsPage, ssrData2, taxYear)

        result1 shouldBe None
        result2 shouldBe None
      }
      "current page is pre-filled and at end of journey so far" in {
        val ssrData1 = cyaData.copy(shortServiceRefunds = ShortServiceRefundsViewModel(
          shortServiceRefund = Some(true),
          shortServiceRefundCharge = Some(500),
          shortServiceRefundTaxPaid = Some(false)
        ))
        val result1 = journeyCheck(NonUkTaxRefundsAmountPage, ssrData1, taxYear)
        val ssrData2 = cyaData.copy(shortServiceRefunds = ShortServiceRefundsViewModel(
          shortServiceRefund = Some(true),
          shortServiceRefundCharge = Some(500),
          shortServiceRefundTaxPaid = Some(true),
          shortServiceRefundTaxPaidCharge = Some(200),
          refundPensionScheme = Seq(anOverseasRefundPensionSchemeWithoutUkRefundCharge)
        ))
        val result2 = journeyCheck(SchemeDetailsPage, ssrData2, taxYear, Some(0))

        result1 shouldBe None
        result2 shouldBe None
      }
      "current page is pre-filled and mid-journey" in {
        val ssrData1 = cyaData.copy(shortServiceRefunds = ShortServiceRefundsViewModel(
          shortServiceRefund = Some(true),
          shortServiceRefundCharge = Some(500),
          shortServiceRefundTaxPaid = Some(true),
          shortServiceRefundTaxPaidCharge = Some(200)
        ))
        val result1  = journeyCheck(TaxableRefundsAmountPage, ssrData1, taxYear)
        val ssrData2 = cyaData.copy(shortServiceRefunds = aShortServiceRefundsViewModel)
        val result2  = journeyCheck(SchemePaidTaxOnRefundsPage, ssrData2, taxYear, Some(1))

        result1 shouldBe None
        result2 shouldBe None
      }
      "previous page is unanswered but invalid and previous valid question has been answered" in {
        val ssrData = cyaData.copy(shortServiceRefunds = ShortServiceRefundsViewModel(
          shortServiceRefund = Some(false)
        ))
        val result = journeyCheck(CYAPage, ssrData, taxYear)

        result shouldBe None
      }
      "on the RemoveSchemePage and schemes exist" in {
        val ssrData = cyaData.copy(shortServiceRefunds = aShortServiceRefundsViewModel)
        val result  = journeyCheck(RemoveRefundSchemePage, ssrData, taxYear)

        result shouldBe None
      }
    }

    "return Some(redirect) with redirect to first page in journey" when {
      "previous question is unanswered" in {
        val ssrData1 = cyaData
        val result1  = journeyCheck(NonUkTaxRefundsAmountPage, ssrData1, taxYear)
        val ssrData2 = cyaData.copy(shortServiceRefunds = aShortServiceRefundsViewModel.copy(
          refundPensionScheme = Seq(anEmptyOverseasRefundPensionScheme)
        ))
        val result2 = journeyCheck(SchemeDetailsPage, ssrData2, taxYear)

        result1 shouldBe journeyStartRedirect
        result2 shouldBe journeyStartRedirect
      }
      "current page is invalid in journey" in {
        val ssrData = cyaData.copy(shortServiceRefunds = ShortServiceRefundsViewModel(
          shortServiceRefund = Some(false)
        ))
        val result = journeyCheck(NonUkTaxRefundsAmountPage, ssrData, taxYear)

        result shouldBe journeyStartRedirect
      }
    }
  }

}
