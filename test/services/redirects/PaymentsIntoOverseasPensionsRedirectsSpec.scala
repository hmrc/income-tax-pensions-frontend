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

import builders.PaymentsIntoOverseasPensionsViewModelBuilder.aPaymentsIntoOverseasPensionsViewModel
import builders.PensionsUserDataBuilder.aPensionsUserData
import builders.ReliefBuilder.{aDoubleTaxationRelief, aMigrantMemberRelief, aNoTaxRelief, aTransitionalCorrespondingRelief}
import controllers.pensions.paymentsIntoOverseasPensions.routes._
import models.mongo.PensionsCYAModel
import models.pension.charges.TaxReliefQuestion.TransitionalCorrespondingRelief
import models.pension.charges.{PaymentsIntoOverseasPensionsViewModel, Relief}
import play.api.http.Status.SEE_OTHER
import play.api.mvc.Results.Redirect
import play.api.mvc.{Call, Result}
import services.redirects.PaymentsIntoOverseasPensionsPages._
import services.redirects.PaymentsIntoOverseasPensionsRedirects.{cyaPageCall, indexCheckThenJourneyCheck, journeyCheck, redirectForSchemeLoop}
import utils.UnitTest

import scala.concurrent.Future

class PaymentsIntoOverseasPensionsRedirectsSpec extends UnitTest {  //scalastyle:off magic.number

  private val cyaData: PensionsCYAModel = PensionsCYAModel.emptyModels
  private val journeyStartCall: Call = PaymentIntoPensionSchemeController.show(taxYear)
  private val journeyStartRedirect: Option[Result] = Some(Redirect(journeyStartCall))
  private val reliefStartCall: Call = PensionsCustomerReferenceNumberController.show(taxYear, None)
  private val reliefDetailsCall: Call = ReliefsSchemeDetailsController.show(taxYear, Some(0))
  private val reliefSummaryCall: Call = ReliefsSchemeSummaryController.show(taxYear)
  private val checkYourAnswersCall: Call = PaymentsIntoOverseasPensionsCYAController.show(taxYear)
  private val continueToContextualRedirect: Relief => Future[Result] = _ => Future.successful(Redirect(reliefDetailsCall))

  ".cyaPageCall" should {
    "return a redirect call to the cya page" in {
      cyaPageCall(taxYear) shouldBe checkYourAnswersCall
    }
  }

  ".redirectForSchemeLoop" should {
    "clear incomplete reliefs and return a Call to the first page in scheme loop when 'schemes' is empty" in {
      val emptySchemes: Seq[Relief] = Seq.empty
      val incompleteSchemes: Seq[Relief] = Seq(
        aTransitionalCorrespondingRelief.copy(sf74Reference = None),
        aMigrantMemberRelief.copy(employerPaymentsAmount = None),
        aDoubleTaxationRelief.copy(alphaTwoCountryCode = None),
        aNoTaxRelief.copy(reliefType = None))
      val result1 = redirectForSchemeLoop(emptySchemes, taxYear)
      val result2 = redirectForSchemeLoop(incompleteSchemes, taxYear)

      result1 shouldBe reliefStartCall
      result2 shouldBe reliefStartCall
    }
    "clear incomplete reliefs and return a Call to the scheme summary page when 'schemes' already exist" in {
      val existingCompleteSchemes: Seq[Relief] = Seq(aTransitionalCorrespondingRelief, aMigrantMemberRelief, aDoubleTaxationRelief, aNoTaxRelief)
      val existingMixedSchemes: Seq[Relief] = Seq(
        aTransitionalCorrespondingRelief,
        aMigrantMemberRelief,
        aDoubleTaxationRelief.copy(alphaTwoCountryCode = None),
        aNoTaxRelief.copy(reliefType = None))
      val result1 = redirectForSchemeLoop(existingCompleteSchemes, taxYear)
      val result2 = redirectForSchemeLoop(existingMixedSchemes, taxYear)

      result1 shouldBe reliefSummaryCall
      result2 shouldBe reliefSummaryCall
    }
  }

  ".indexCheckThenJourneyCheck" when {
    "index is valid" should {
      "return PensionsUserData if previous questions are answered and journey is valid" in {
        val result = indexCheckThenJourneyCheck(
          data = aPensionsUserData,
          optIndex = Some(0),
          currentPage = ReliefsSchemeDetailsPage,
          taxYear = taxYear)(continueToContextualRedirect)
        val statusHeader = await(result.map(_.header.status))
        val locationHeader = await(result.map(_.header.headers).map(_.get("Location")))

        statusHeader shouldBe SEE_OTHER
        locationHeader shouldBe Some(reliefDetailsCall.url)
      }
      "redirect to the relief summary page" when {
        "previous relief questions are unanswered" in {
          val incompleteJourney = cyaData.copy(paymentsIntoOverseasPensions = aPaymentsIntoOverseasPensionsViewModel.copy(
            reliefs = Seq(aTransitionalCorrespondingRelief)
          ))
          val result = indexCheckThenJourneyCheck(
            data = aPensionsUserData.copy(pensions = incompleteJourney),
            optIndex = Some(1),
            currentPage = ReliefsSchemeDetailsPage,
            taxYear = taxYear)(continueToContextualRedirect)
          val statusHeader = await(result.map(_.header.status))
          val locationHeader = await(result.map(_.header.headers).map(_.get("Location")))

          statusHeader shouldBe SEE_OTHER
          locationHeader shouldBe Some(reliefSummaryCall.url)
        }
        "page is invalid to current relief journey" in {
          val invalidJourney = cyaData.copy(paymentsIntoOverseasPensions = aPaymentsIntoOverseasPensionsViewModel.copy(
            reliefs = Seq(aTransitionalCorrespondingRelief)
          ))
          val result = indexCheckThenJourneyCheck(
            data = aPensionsUserData.copy(pensions = invalidJourney),
            optIndex = Some(0),
            currentPage = DoubleTaxationAgreementPage,
            taxYear = taxYear)(continueToContextualRedirect)
          val statusHeader = await(result.map(_.header.status))
          val locationHeader = await(result.map(_.header.headers).map(_.get("Location")))

          statusHeader shouldBe SEE_OTHER
          locationHeader shouldBe Some(reliefSummaryCall.url)
        }
      }
    }
    "index is invalid" should {
      "redirect to the first page in journey" when {
        "there are no schemes" in {
          val cyaModel = cyaData.copy(paymentsIntoOverseasPensions = aPaymentsIntoOverseasPensionsViewModel.copy(reliefs = Seq.empty))
          val result = indexCheckThenJourneyCheck(
            data = aPensionsUserData.copy(pensions = cyaModel),
            optIndex = Some(2),
            currentPage = ReliefsSchemeDetailsPage,
            taxYear = taxYear)(continueToContextualRedirect)
          val statusHeader = await(result.map(_.header.status))
          val locationHeader = await(result.map(_.header.headers).map(_.get("Location")))

          statusHeader shouldBe SEE_OTHER
          locationHeader shouldBe Some(journeyStartCall.url)
        }
        "previous questions are unanswered" in {
          val cyaModel = cyaData.copy(paymentsIntoOverseasPensions = aPaymentsIntoOverseasPensionsViewModel.copy(
            taxPaidOnEmployerPaymentsQuestion = None, reliefs = Seq.empty
          ))
          val result = indexCheckThenJourneyCheck(
            data = aPensionsUserData.copy(pensions = cyaModel),
            optIndex = Some(8),
            currentPage = PensionsCustomerReferenceNumberPage,
            taxYear = taxYear)(continueToContextualRedirect)
          val statusHeader = await(result.map(_.header.status))
          val locationHeader = await(result.map(_.header.headers).map(_.get("Location")))

          statusHeader shouldBe SEE_OTHER
          locationHeader shouldBe Some(journeyStartCall.url)
        }
      }
      "redirect to the scheme summary page when schemes already exist" in {
        val result: Future[Result] = indexCheckThenJourneyCheck(
          data = aPensionsUserData,
          optIndex = Some(8),
          currentPage = ReliefsSchemeDetailsPage,
          taxYear = taxYear)(continueToContextualRedirect)
        val statusHeader = await(result.map(_.header.status))
        val locationHeader = await(result.map(_.header.headers).map(_.get("Location")))

        statusHeader shouldBe SEE_OTHER
        locationHeader shouldBe Some(reliefSummaryCall.url)
      }
    }
  }

  ".journeyCheck" should {
    "return None if page is valid and all previous questions have been answered" when {
      "current page is empty and at end of journey so far" in {
        val piopData1 = cyaData.copy(paymentsIntoOverseasPensions = PaymentsIntoOverseasPensionsViewModel(
          paymentsIntoOverseasPensionsQuestions = Some(true),
          paymentsIntoOverseasPensionsAmount = Some(500),
          employerPaymentsQuestion = Some(true)
        ))
        val result1 = journeyCheck(TaxEmployerPaymentsPage, piopData1, taxYear)
        val piopData2 = cyaData.copy(paymentsIntoOverseasPensions = aPaymentsIntoOverseasPensionsViewModel.copy(
          reliefs = Seq(Relief(
            reliefType = Some(TransitionalCorrespondingRelief),
            customerReference = Some("tcrPENSIONINCOME2000"),
            employerPaymentsAmount = Some(1999.99)
          ))))
        val result2 = journeyCheck(SF74ReferencePage, piopData2, taxYear, Some(0))

        result1 shouldBe None
        result2 shouldBe None
      }
      "current page is pre-filled and at end of journey so far" in {
        val piopData1 = cyaData.copy(paymentsIntoOverseasPensions = aPaymentsIntoOverseasPensionsViewModel.copy(
          reliefs = Seq(Relief(customerReference = Some("tcrPENSIONINCOME2000")))))
        val result1 = journeyCheck(PensionsCustomerReferenceNumberPage, piopData1, taxYear, Some(0))
        val piopData2 = cyaData.copy(paymentsIntoOverseasPensions = aPaymentsIntoOverseasPensionsViewModel.copy(
          reliefs = Seq(aMigrantMemberRelief, aTransitionalCorrespondingRelief)))
        val result2 = journeyCheck(SF74ReferencePage, piopData2, taxYear, Some(1))
        val piopData3 = cyaData.copy(paymentsIntoOverseasPensions = aPaymentsIntoOverseasPensionsViewModel)
        val result3 = journeyCheck(PaymentsIntoOverseasPensionsCYAPage, piopData3, taxYear)

        result1 shouldBe None
        result2 shouldBe None
        result3 shouldBe None
      }
      "current page is pre-filled and mid-journey" in {
        val piopData1 = cyaData.copy(paymentsIntoOverseasPensions = aPaymentsIntoOverseasPensionsViewModel)
        val result1 = journeyCheck(QOPSReferencePage, piopData1, taxYear, Some(1))
        val piopData2 = cyaData.copy(paymentsIntoOverseasPensions = aPaymentsIntoOverseasPensionsViewModel)
        val result2 = journeyCheck(ReliefsSchemeDetailsPage, piopData2, taxYear, Some(3))

        result1 shouldBe None
        result2 shouldBe None
      }
      "previous page is unanswered but invalid and previous valid question has been answered" in {
        val piopData = cyaData.copy(paymentsIntoOverseasPensions = PaymentsIntoOverseasPensionsViewModel(
          paymentsIntoOverseasPensionsQuestions = Some(true),
          paymentsIntoOverseasPensionsAmount = Some(1999.99),
          employerPaymentsQuestion = Some(false)
        ))
        val result = journeyCheck(PaymentsIntoOverseasPensionsCYAPage, piopData, taxYear)

        result shouldBe None
      }
      "on the RemoveSchemePage and schemes exist" in {
        val piopData = cyaData.copy(paymentsIntoOverseasPensions = aPaymentsIntoOverseasPensionsViewModel)
        val result = journeyCheck(RemoveReliefsSchemePage, piopData, taxYear, Some(3))

        result shouldBe None
      }
    }

    "return Some(redirect) with redirect to first page in journey" when {
      "previous question is unanswered" in {
        val piopData1 = cyaData
        val result1 = journeyCheck(EmployerPayOverseasPensionPage, piopData1, taxYear)
        val piopData2 = cyaData.copy(paymentsIntoOverseasPensions = aPaymentsIntoOverseasPensionsViewModel.copy(
          reliefs = Seq(aTransitionalCorrespondingRelief, aMigrantMemberRelief, aDoubleTaxationRelief, aNoTaxRelief.copy(
            reliefType = None
          ))
        ))
        val result2 = journeyCheck(ReliefsSchemeDetailsPage, piopData2, taxYear, Some(3))

        result1 shouldBe journeyStartRedirect
        result2 shouldBe journeyStartRedirect
      }
      "current page is invalid in journey" in {
        val piopData = cyaData.copy(paymentsIntoOverseasPensions = aPaymentsIntoOverseasPensionsViewModel.copy(
          reliefs = Seq(aTransitionalCorrespondingRelief)))
        val result = journeyCheck(DoubleTaxationAgreementPage, piopData, taxYear, Some(0))

        result shouldBe journeyStartRedirect
      }
    }
  }

}
