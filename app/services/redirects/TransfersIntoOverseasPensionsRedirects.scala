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
import controllers.pensions.transferIntoOverseasPensions.routes.{
  OverseasTransferChargePaidController,
  TransferChargeSummaryController,
  TransferIntoOverseasPensionsCYAController,
  TransferPensionSavingsController
}
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.pension.charges.{TransferPensionScheme, TransfersIntoOverseasPensionsViewModel}
import play.api.mvc.Results.Redirect
import play.api.mvc.{Call, Result}
import services.redirects.SimpleRedirectService.{checkForExistingSchemes, redirectBasedOnCurrentAnswers}

import scala.concurrent.Future

object TransfersIntoOverseasPensionsRedirects {

  def indexCheckThenJourneyCheck(data: PensionsUserData, optIndex: Option[Int], currentPage: TransfersIntoOverseasPensionsPages, taxYear: Int)(
      continue: PensionsUserData => Future[Result]): Future[Result] = {

    val schemes: Seq[TransferPensionScheme] = data.pensions.transfersIntoOverseasPensions.transferPensionScheme
    val validatedIndex: Option[Int]         = validateIndex(optIndex, schemes)
    (schemes, validatedIndex) match {
      case (schemes, None) if schemes.nonEmpty =>
        val checkRedirect = journeyCheck(currentPage, _: PensionsCYAModel, taxYear)
        redirectBasedOnCurrentAnswers(taxYear, Some(data), cyaPageCall(taxYear))(checkRedirect) { _ =>
          Future.successful(Redirect(redirectForSchemeLoop(schemes, taxYear)))
        }

      case (_, optIndex) =>
        val checkRedirect = journeyCheck(currentPage, _, taxYear, optIndex)
        redirectBasedOnCurrentAnswers(taxYear, Some(data), cyaPageCall(taxYear))(checkRedirect) { data: PensionsUserData =>
          continue(data)
        }
    }
  }

  def cyaPageCall(taxYear: Int): Call = TransferIntoOverseasPensionsCYAController.show(TaxYear(taxYear))

  def redirectForSchemeLoop(schemes: Seq[TransferPensionScheme], taxYear: Int): Call = {
    val filteredSchemes = schemes.filter(scheme => scheme.isFinished)
    checkForExistingSchemes(
      nextPage = OverseasTransferChargePaidController.show(taxYear, None),
      summaryPage = TransferChargeSummaryController.show(taxYear),
      schemes = filteredSchemes
    )
  }

  def journeyCheck(currentPage: TransfersIntoOverseasPensionsPages,
                   cya: PensionsCYAModel,
                   taxYear: Int,
                   index: Option[Int] = None): Option[Result] = {
    val tIOPData = cya.transfersIntoOverseasPensions
    if (isPageValidInJourney(currentPage.journeyNo, tIOPData) && previousQuestionIsAnswered(currentPage.journeyNo, index, tIOPData)) {
      None
    } else {
      Some(Redirect(TransferPensionSavingsController.show(taxYear)))
    }
  }

  private def isPageValidInJourney(pageNumber: Int, transferOverseasViewModel: TransfersIntoOverseasPensionsViewModel): Boolean =
    pageValidInJourneyMap.getOrElse(pageNumber, { _: TransfersIntoOverseasPensionsViewModel => true })(transferOverseasViewModel)

  private val pageValidInJourneyMap: Map[Int, TransfersIntoOverseasPensionsViewModel => Boolean] = {
    val isTrue            = { v: TransfersIntoOverseasPensionsViewModel => true }
    val firstQuestionTrue = { tIOP: TransfersIntoOverseasPensionsViewModel => tIOP.transferPensionSavings.getOrElse(false) }
    val firstAndSecondTrue = { tIOP: TransfersIntoOverseasPensionsViewModel =>
      tIOP.overseasTransferCharge.getOrElse(false)
    }
    val firstSecondAndThirdTrue = { tIOP: TransfersIntoOverseasPensionsViewModel => tIOP.pensionSchemeTransferCharge.getOrElse(false) }

    Map(
      // 1 and 8 are always valid
      1 -> isTrue,
      8 -> isTrue,
      // 2 need Q1 true
      2 -> firstQuestionTrue,
      // 3 needs Q1 & Q2 true
      3 -> firstAndSecondTrue,
      // 4-7 need Q1, Q2 and Q3 true
      4 -> firstSecondAndThirdTrue,
      5 -> firstSecondAndThirdTrue,
      6 -> firstSecondAndThirdTrue,
      7 -> firstSecondAndThirdTrue
    )
  }

  private def previousQuestionIsAnswered(pageNumber: Int,
                                         optIndex: Option[Int],
                                         transferOverseasViewModel: TransfersIntoOverseasPensionsViewModel): Boolean = {
    val schemesEmpty = transferOverseasViewModel.transferPensionScheme.isEmpty
    val index        = optIndex.getOrElse(if (schemesEmpty) 0 else transferOverseasViewModel.transferPensionScheme.size - 1)

    def cyaPageCheck(tIOPVM: TransfersIntoOverseasPensionsViewModel) =
      if (!isPageValidInJourney(2, tIOPVM)) { !tIOPVM.transferPensionSavings.getOrElse(true) }
      else if (!isPageValidInJourney(3, tIOPVM)) {
        !tIOPVM.overseasTransferCharge.getOrElse(true)
      } else if (!isPageValidInJourney(4, tIOPVM)) {
        !tIOPVM.pensionSchemeTransferCharge.getOrElse(true)
      } else { tIOPVM.isFinished }

    val prevQuestionIsAnsweredMap: Map[Int, TransfersIntoOverseasPensionsViewModel => Boolean] = Map(
      1 -> { _: TransfersIntoOverseasPensionsViewModel => true },
      2 -> { tIOPVM: TransfersIntoOverseasPensionsViewModel => tIOPVM.transferPensionSavings.getOrElse(false) },
      3 -> { tIOPVM: TransfersIntoOverseasPensionsViewModel =>
        tIOPVM.overseasTransferCharge.getOrElse(false) && tIOPVM.overseasTransferChargeAmount.nonEmpty
      },
      4 -> { tIOPVM: TransfersIntoOverseasPensionsViewModel =>
        if (schemesEmpty && tIOPVM.pensionSchemeTransferChargeAmount.nonEmpty) true else tIOPVM.transferPensionScheme.forall(p => p.isFinished)
      },
      5 -> { tIOPVM: TransfersIntoOverseasPensionsViewModel =>
        tIOPVM.pensionSchemeTransferCharge.exists(value => !value || (value && tIOPVM.pensionSchemeTransferCharge.nonEmpty))
      },
      6 -> { tIOPVM: TransfersIntoOverseasPensionsViewModel =>
        if (schemesEmpty) false else tIOPVM.transferPensionScheme(index).ukTransferCharge.isDefined
      },
      7 -> { tIOPVM: TransfersIntoOverseasPensionsViewModel =>
        if (schemesEmpty) false else tIOPVM.transferPensionScheme(index).isFinished
      },
      8 -> { tIOPVM: TransfersIntoOverseasPensionsViewModel =>
        cyaPageCheck(tIOPVM)
      }
    )

    prevQuestionIsAnsweredMap(pageNumber)(transferOverseasViewModel)
  }

  private def validateIndex(index: Option[Int], schemeList: Seq[TransferPensionScheme]): Option[Int] =
    index.filter(i => i >= 0 && i < schemeList.size)

}
