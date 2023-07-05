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

import builders.IncomeFromOverseasPensionsViewModelBuilder.{anIncomeFromOverseasPensionsEmptyViewModel, anIncomeFromOverseasPensionsViewModel}
import builders.PensionsCYAModelBuilder.aPensionsCYAModel
import builders.PensionsUserDataBuilder.aPensionsUserData
import controllers.pensions.incomeFromOverseasPensions.routes._
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.pension.charges.{IncomeFromOverseasPensionsViewModel, PensionScheme}
import play.api.http.Status.SEE_OTHER
import play.api.mvc.Results.Redirect
import play.api.mvc.{Call, Result}
import services.redirects.IncomeFromOverseasPensionsPages._
import services.redirects.IncomeFromOverseasPensionsRedirects.{cyaPageCall, indexCheckThenJourneyCheck, journeyCheck, redirectForSchemeLoop}
import utils.UnitTest

import scala.concurrent.Future

class IncomeFromOverseasPensionsRedirectsSpec extends UnitTest {

  private val cyaData: PensionsCYAModel = PensionsCYAModel.emptyModels
  private val statusPageRedirect = Some(Redirect(PensionOverseasIncomeStatus.show(taxYear)))
  private val taxableAmountPageCall: Call = TaxableAmountController.show(taxYear, None)
  private val schemeStartCall: Call = PensionOverseasIncomeCountryController.show(taxYear, None)
  private val schemeSummaryCall: Call = CountrySummaryListController.show(taxYear)
  private val continueToContextualRedirect: PensionsUserData => Future[Result] = aPensionsUserData => Future.successful(Redirect(taxableAmountPageCall))

  ".indexCheckThenJourneyCheck" when {
    "index is valid" should {
      "return PensionsUserData if previous questions are answered and journey is valid" in {
        val result = indexCheckThenJourneyCheck(
          data = aPensionsUserData,
          optIndex = Some(0),
          currentPage = ForeignTaxCreditReliefPage,
          taxYear = taxYear)(continueToContextualRedirect)
        val statusHeader = await(result.map(_.header.status))
        val locationHeader = await(result.map(_.header.headers).map(_.get("Location")))

        statusHeader shouldBe SEE_OTHER
        locationHeader shouldBe Some(taxableAmountPageCall.url)
      }
      "redirect to first page in journey" when {
        "previous questions are unanswered" in {
          val incompleteJourney = cyaData.copy(incomeFromOverseasPensions = IncomeFromOverseasPensionsViewModel(
            paymentsFromOverseasPensionsQuestion = Some(true),
            overseasIncomePensionSchemes = Seq(
              PensionScheme(
                alphaThreeCode = Some("FRA"),
                alphaTwoCode = Some("FR"),
                pensionPaymentAmount = Some(1999.99),
                pensionPaymentTaxPaid = Some(1999.99),
                specialWithholdingTaxQuestion = None,
                specialWithholdingTaxAmount = None,
                foreignTaxCreditReliefQuestion = None,
                taxableAmount = None
              )
            )
          ))
          val result = indexCheckThenJourneyCheck(
            data = aPensionsUserData.copy(pensions = incompleteJourney),
            optIndex = Some(0),
            currentPage = ForeignTaxCreditReliefPage,
            taxYear = taxYear)(continueToContextualRedirect)
          val statusHeader = await(result.map(_.header.status))
          val locationHeader = await(result.map(_.header.headers).map(_.get("Location")))

          statusHeader shouldBe SEE_OTHER
          locationHeader shouldBe Some(PensionOverseasIncomeStatus.show(taxYear).url)
        }
        "journey is invalid" in {
          val invalidJourney = cyaData.copy(incomeFromOverseasPensions = IncomeFromOverseasPensionsViewModel(
            paymentsFromOverseasPensionsQuestion = Some(false),
            overseasIncomePensionSchemes = Seq.empty))
          val result = indexCheckThenJourneyCheck(
            data = aPensionsUserData.copy(pensions = invalidJourney),
            optIndex = Some(0),
            currentPage = ForeignTaxCreditReliefPage,
            taxYear = taxYear)(continueToContextualRedirect)
          val statusHeader = await(result.map(_.header.status))
          val locationHeader = await(result.map(_.header.headers).map(_.get("Location")))

          statusHeader shouldBe SEE_OTHER
          locationHeader shouldBe Some(PensionOverseasIncomeStatus.show(taxYear).url)
        }
      }
    }
    "index is invalid" should {
      "redirect to the first page in journey when previous questions are unanswered" in {
        val cyaModel = aPensionsCYAModel.copy (incomeFromOverseasPensions = anIncomeFromOverseasPensionsEmptyViewModel)
        val result = indexCheckThenJourneyCheck(
          data = aPensionsUserData.copy(pensions = cyaModel),
          optIndex = Some(8),
          currentPage = YourTaxableAmountPage,
          taxYear = taxYear)(continueToContextualRedirect)
        val statusHeader = await(result.map(_.header.status))
        val locationHeader = await(result.map(_.header.headers).map(_.get("Location")))

        statusHeader shouldBe SEE_OTHER
        locationHeader shouldBe Some(PensionOverseasIncomeStatus.show(taxYear).url)
      }
      "redirect to the first page in journey when there are no schemes" in {
        val emptySchemesIFOPViewModel: IncomeFromOverseasPensionsViewModel = aPensionsCYAModel.incomeFromOverseasPensions.copy(overseasIncomePensionSchemes = Seq.empty)
        val cyaModel = aPensionsCYAModel.copy(incomeFromOverseasPensions = emptySchemesIFOPViewModel)
        val result = indexCheckThenJourneyCheck(
          data = aPensionsUserData.copy(pensions = cyaModel),
          optIndex = Some(8),
          currentPage = YourTaxableAmountPage,
          taxYear = taxYear)(continueToContextualRedirect)
        val statusHeader = await(result.map(_.header.status))
        val locationHeader = await(result.map(_.header.headers).map(_.get("Location")))

        statusHeader shouldBe SEE_OTHER
        locationHeader shouldBe Some(PensionOverseasIncomeStatus.show(taxYear).url)
      }
      "redirect to the scheme summary page when schemes already exist" in {
        val result: Future[Result] = indexCheckThenJourneyCheck(
          data = aPensionsUserData,
          optIndex = Some(8),
          currentPage = YourTaxableAmountPage,
          taxYear = taxYear)(continueToContextualRedirect)
        val statusHeader = await(result.map(_.header.status))
        val locationHeader = await(result.map(_.header.headers).map(_.get("Location")))

        statusHeader shouldBe SEE_OTHER
        locationHeader shouldBe Some(schemeSummaryCall.url)
      }
    }
  }

  ".redirectForSchemeLoop" should {
    "return a Call to the first page in scheme loop when 'schemes' is empty" in {
      val emptySchemes: Seq[PensionScheme] = Seq.empty
      val result = redirectForSchemeLoop(emptySchemes, taxYear)

      result shouldBe schemeStartCall
    }
    "return a Call to the scheme summary page when 'schemes' already exist" in {
      val existingSchemes: Seq[PensionScheme] = Seq(
        PensionScheme(
          alphaThreeCode = Some("FRA"),
          alphaTwoCode = Some("FR"),
          pensionPaymentAmount = Some(1999.99),
          pensionPaymentTaxPaid = Some(1999.99),
          specialWithholdingTaxQuestion = Some(true),
          specialWithholdingTaxAmount = Some(1999.99),
          foreignTaxCreditReliefQuestion = Some(true),
          taxableAmount = Some(1999.99)
        ),
        PensionScheme(
          alphaThreeCode = Some("DEU"),
          alphaTwoCode = Some("DE"),
          pensionPaymentAmount = Some(2000.00),
          pensionPaymentTaxPaid = Some(400.00),
          specialWithholdingTaxQuestion = Some(true),
          specialWithholdingTaxAmount = Some(400.00),
          foreignTaxCreditReliefQuestion = Some(true),
          taxableAmount = Some(2000.00)
        )
      )
      val result = redirectForSchemeLoop(existingSchemes, taxYear)

      result shouldBe schemeSummaryCall
    }
  }

  ".journeyCheck" should {
    "return None if page is valid and all previous questions have been answered" when {
      "current page is empty and at end of journey so far" in {
        val iFOPData = cyaData.copy(incomeFromOverseasPensions = IncomeFromOverseasPensionsViewModel(
          paymentsFromOverseasPensionsQuestion = Some(true),
          overseasIncomePensionSchemes = Seq(
            PensionScheme(
              alphaThreeCode = Some("FRA"),
              alphaTwoCode = Some("FR"),
              pensionPaymentAmount = Some(1999.99),
              pensionPaymentTaxPaid = Some(1999.99),
              specialWithholdingTaxQuestion = Some(true),
              specialWithholdingTaxAmount = Some(1999.99),
              foreignTaxCreditReliefQuestion = None,
              taxableAmount = None
            )
        )
        ))
        val result = journeyCheck(ForeignTaxCreditReliefPage, iFOPData, taxYear)

        result shouldBe None
      }
      "current page is pre-filled and at end of journey so far" in {
        val iFOPData = cyaData.copy(incomeFromOverseasPensions = IncomeFromOverseasPensionsViewModel(
          paymentsFromOverseasPensionsQuestion = Some(true),
          overseasIncomePensionSchemes = Seq(
            PensionScheme(
              alphaThreeCode = Some("FRA"),
              alphaTwoCode = Some("FR"),
              pensionPaymentAmount = Some(1999.99),
              pensionPaymentTaxPaid = Some(1999.99),
              specialWithholdingTaxQuestion = None,
              specialWithholdingTaxAmount = None,
              foreignTaxCreditReliefQuestion = None,
              taxableAmount = None
            )
          )
        ))
        val result = journeyCheck(PensionsPaymentsAmountPage, iFOPData, taxYear)

        result shouldBe None
      }
      "current page is pre-filled and mid-journey" in {
        val iFOPData = cyaData.copy(incomeFromOverseasPensions = IncomeFromOverseasPensionsViewModel(
          paymentsFromOverseasPensionsQuestion = Some(true),
          overseasIncomePensionSchemes = Seq(
            PensionScheme(
              alphaThreeCode = Some("FRA"),
              alphaTwoCode = Some("FR"),
              pensionPaymentAmount = Some(1999.99),
              pensionPaymentTaxPaid = Some(1999.99),
              specialWithholdingTaxQuestion = Some(true),
              specialWithholdingTaxAmount = Some(1999.99),
              foreignTaxCreditReliefQuestion = None,
              taxableAmount = None
            )
          )
        ))
        val result = journeyCheck(WhatCountryIsSchemeRegisteredInPage, iFOPData, taxYear)

        result shouldBe None
      }
      "previous page is unanswered but invalid and previous valid question has been answered" in {
        val iFOPData = cyaData.copy(incomeFromOverseasPensions = anIncomeFromOverseasPensionsEmptyViewModel.copy(
          paymentsFromOverseasPensionsQuestion = Some(false)
        ))
        val result = journeyCheck(CYAPage, iFOPData, taxYear)

        result shouldBe None
      }
      "on the RemoveSchemePage and schemes exist" in {
        val iFOPData = cyaData.copy(incomeFromOverseasPensions = anIncomeFromOverseasPensionsViewModel)
        val result = journeyCheck(RemoveSchemePage, iFOPData, taxYear)

        result shouldBe None
      }
    }

    "return Some(redirect) with redirect to first page in journey" when {
      "previous question is unanswered" in {
        val iFOPData = cyaData.copy(incomeFromOverseasPensions = IncomeFromOverseasPensionsViewModel(
          paymentsFromOverseasPensionsQuestion = Some(true),
          overseasIncomePensionSchemes = Seq(
            PensionScheme(
              alphaThreeCode = Some("FRA"),
              alphaTwoCode = Some("FR"),
              pensionPaymentAmount = Some(1999.99),
              pensionPaymentTaxPaid = Some(1999.99),
              specialWithholdingTaxQuestion = None,
              specialWithholdingTaxAmount = None,
              foreignTaxCreditReliefQuestion = None,
              taxableAmount = None
            )
          )
        ))
        val result = journeyCheck(PensionSchemeSummaryPage, iFOPData, taxYear)

        result shouldBe statusPageRedirect
      }
      "current page is invalid in journey" in {
        val iFOPData = cyaData.copy(incomeFromOverseasPensions = anIncomeFromOverseasPensionsEmptyViewModel.copy(
          paymentsFromOverseasPensionsQuestion = Some(false)
        ))
        val result = journeyCheck(ForeignTaxCreditReliefPage, iFOPData, taxYear)

        result shouldBe statusPageRedirect
      }
    }
  }

  ".cyaPageCall" should {
    "return a redirect call to the cya page" in {
      cyaPageCall(taxYear) shouldBe IncomeFromOverseasPensionsCYAController.show(taxYear)
    }
  }
  
}
