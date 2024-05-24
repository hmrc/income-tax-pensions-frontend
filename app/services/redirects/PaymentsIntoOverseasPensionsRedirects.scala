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

import common.TaxYear
import controllers.pensions.paymentsIntoOverseasPensions.routes._
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.pension.charges.TaxReliefQuestion.{DoubleTaxationRelief, MigrantMemberRelief, TransitionalCorrespondingRelief}
import models.pension.charges.{PaymentsIntoOverseasPensionsViewModel, OverseasPensionScheme}
import play.api.mvc.Results.Redirect
import play.api.mvc.{Call, Result}
import services.redirects.PaymentsIntoOverseasPensionsPages.PensionsCustomerReferenceNumberPage
import services.redirects.SimpleRedirectService.{checkForExistingSchemes, redirectBasedOnCurrentAnswers}

import scala.concurrent.Future

object PaymentsIntoOverseasPensionsRedirects {

  def redirectForSchemeLoop(reliefs: Seq[OverseasPensionScheme], taxYear: Int): Call = {
    val filteredReliefs = reliefs.filter(relief => relief.isFinished)
    checkForExistingSchemes(
      nextPage = PensionsCustomerReferenceNumberController.show(taxYear, None),
      summaryPage = ReliefsSchemeSummaryController.show(taxYear),
      schemes = filteredReliefs
    )
  }

  def schemeIsFinishedCheck(schemes: Seq[OverseasPensionScheme], index: Int, taxYear: Int, continueRedirect: Call): Result =
    if (schemes(index).isFinished) {
      Redirect(ReliefsSchemeDetailsController.show(taxYear, Some(index)))
    } else {
      Redirect(continueRedirect)
    }

  def indexCheckThenJourneyCheck(data: PensionsUserData, optIndex: Option[Int], currentPage: PaymentsIntoOverseasPensionsPages, taxYear: Int)(
      continue: OverseasPensionScheme => Future[Result]): Future[Result] = {

    val reliefs                     = data.pensions.paymentsIntoOverseasPensions.schemes
    val validatedIndex: Option[Int] = validateIndex(optIndex, reliefs)
    (reliefs, validatedIndex) match {
      case (reliefs, None) if reliefs.nonEmpty =>
        val checkRedirect = journeyCheckIndex(currentPage, _, taxYear, reliefs, None)
        redirectBasedOnCurrentAnswers(taxYear, Some(data), cyaPageCall(taxYear))(checkRedirect) { _ =>
          Future.successful(Redirect(redirectForSchemeLoop(reliefs, taxYear)))
        }

      case (_, None) =>
        val checkRedirect = journeyCheck(currentPage, _: PensionsCYAModel, taxYear)
        redirectBasedOnCurrentAnswers(taxYear, Some(data), cyaPageCall(taxYear))(checkRedirect) { _ =>
          Future.successful(Redirect(redirectForSchemeLoop(reliefs, taxYear)))
        }

      case (_, Some(index)) =>
        val checkRedirect = journeyCheckIndex(currentPage, _, taxYear, reliefs, Some(index))
        redirectBasedOnCurrentAnswers(taxYear, Some(data), cyaPageCall(taxYear))(checkRedirect) { data: PensionsUserData =>
          continue(data.pensions.paymentsIntoOverseasPensions.schemes(index))
        }
    }
  }

  private def journeyCheckIndex(currentPage: PaymentsIntoOverseasPensionsPages,
                                cya: PensionsCYAModel,
                                taxYear: Int,
                                reliefs: Seq[OverseasPensionScheme],
                                index: Option[Int]): Option[Result] = {
    val paymentsIOP = cya.paymentsIntoOverseasPensions
    if (isPageValidInJourney(currentPage, paymentsIOP, index)) {
      None
    } else {
      if (currentPage.hasIndex) {
        Some(Redirect(redirectForSchemeLoop(reliefs, taxYear)))
      } else {
        Some(Redirect(PaymentIntoPensionSchemeController.show(taxYear)))
      }
    }
  }

  def journeyCheck(currentPage: PaymentsIntoOverseasPensionsPages, cya: PensionsCYAModel, taxYear: Int, index: Option[Int] = None): Option[Result] = {
    val paymentsIOP = cya.paymentsIntoOverseasPensions
    if (isPageValidInJourney(currentPage, paymentsIOP, index)) {
      None
    } else {
      Some(Redirect(PaymentIntoPensionSchemeController.show(taxYear)))
    }
  }

  def cyaPageCall(taxYear: Int): Call = PaymentsIntoOverseasPensionsCYAController.show(TaxYear(taxYear))

  private val indexPageValidInJourney: Map[Int, OverseasPensionScheme => Boolean] =
    Map(
      4 -> { relief: OverseasPensionScheme => relief.customerReference.isDefined },
      5 -> { relief: OverseasPensionScheme => relief.customerReference.isDefined },
      6 -> { relief: OverseasPensionScheme => relief.customerReference.isDefined },
      7 -> { relief: OverseasPensionScheme =>
        relief.customerReference.isDefined && relief.reliefType.contains(MigrantMemberRelief)
      }, // MMR QOPS Page
      8 -> { relief: OverseasPensionScheme => relief.customerReference.isDefined && relief.reliefType.contains(DoubleTaxationRelief) }, // DTR Page
      9 -> { relief: OverseasPensionScheme =>
        relief.customerReference.isDefined && relief.reliefType.contains(TransitionalCorrespondingRelief)
      }, // TCR SF74 Page
      10 -> { relief: OverseasPensionScheme => relief.isFinished },
      12 -> { relief: OverseasPensionScheme => relief.isFinished }
    )

  private val pageValidInJourneyMap: Map[Int, PaymentsIntoOverseasPensionsViewModel => Boolean] =
    Map(
      1 -> { _: PaymentsIntoOverseasPensionsViewModel => true },
      2 -> { piopVM: PaymentsIntoOverseasPensionsViewModel =>
        piopVM.paymentsIntoOverseasPensionsQuestions.exists(x => x && piopVM.paymentsIntoOverseasPensionsAmount.isDefined)
      },
      3  -> { piopVM: PaymentsIntoOverseasPensionsViewModel => piopVM.employerPaymentsQuestion.getOrElse(false) },
      4  -> { piopVM: PaymentsIntoOverseasPensionsViewModel => !piopVM.taxPaidOnEmployerPaymentsQuestion.getOrElse(true) },     // cust reference
      11 -> { piopVM: PaymentsIntoOverseasPensionsViewModel => piopVM.schemes.isEmpty || piopVM.schemes.forall(_.isFinished) }, // summary page
      13 -> { piopVM: PaymentsIntoOverseasPensionsViewModel => piopVM.isFinished }
    )

  private def isPageValidInJourney(currentPage: PaymentsIntoOverseasPensionsPages,
                                   pIPViewModel: PaymentsIntoOverseasPensionsViewModel,
                                   index: Option[Int]): Boolean =
    validateIndex(index, pIPViewModel.schemes) match {
      case Some(value) if currentPage.hasIndex =>
        indexPageValidInJourney.getOrElse(currentPage.journeyNo, { _: OverseasPensionScheme => false })(pIPViewModel.schemes(value))
      case None if !currentPage.hasIndex =>
        pageValidInJourneyMap.getOrElse(currentPage.journeyNo, { _: PaymentsIntoOverseasPensionsViewModel => false })(pIPViewModel)
      case None if currentPage == PensionsCustomerReferenceNumberPage =>
        pageValidInJourneyMap.getOrElse(currentPage.journeyNo, { _: PaymentsIntoOverseasPensionsViewModel => false })(pIPViewModel)
      case _ => false
    }

  private def validateIndex(index: Option[Int], pensionSchemesList: Seq[OverseasPensionScheme]): Option[Int] =
    index.filter(i => i >= 0 && i < pensionSchemesList.size)

}
