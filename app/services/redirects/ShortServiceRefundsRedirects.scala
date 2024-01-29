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

import controllers.pensions.shortServiceRefunds.routes._
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.pension.charges.{OverseasRefundPensionScheme, ShortServiceRefundsViewModel}
import play.api.mvc.Results.Redirect
import play.api.mvc.{Call, Result}
import services.redirects.SimpleRedirectService.{checkForExistingSchemes, redirectBasedOnCurrentAnswers}

import scala.concurrent.Future

object ShortServiceRefundsRedirects {

  def indexCheckThenJourneyCheck(data: PensionsUserData, optIndex: Option[Int], currentPage: ShortServiceRefundsPages, taxYear: Int)(
      continue: PensionsUserData => Future[Result]): Future[Result] = {

    val schemes: Seq[OverseasRefundPensionScheme] = data.pensions.shortServiceRefunds.refundPensionScheme
    val validatedIndex: Option[Int]               = validateIndex(optIndex, schemes)
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

  def cyaPageCall(taxYear: Int): Call = ShortServiceRefundsCYAController.show(taxYear)

  def redirectForSchemeLoop(refundSchemes: Seq[OverseasRefundPensionScheme], taxYear: Int): Call = {
    val filteredSchemes = refundSchemes.filter(scheme => scheme.isFinished)
    checkForExistingSchemes(
      nextPage = TaxOnShortServiceRefundController.show(taxYear, None),
      summaryPage = RefundSummaryController.show(taxYear),
      schemes = filteredSchemes
    )
  }

  def journeyCheck(currentPage: ShortServiceRefundsPages, cya: PensionsCYAModel, taxYear: Int, index: Option[Int] = None): Option[Result] = {
    val ssrData = cya.shortServiceRefunds
    if (isPageValidInJourney(currentPage.journeyNo, ssrData) && previousQuestionIsAnswered(currentPage.journeyNo, index, ssrData)) {
      None
    } else {
      Some(Redirect(TaxableRefundAmountController.show(taxYear)))
    }
  }

  private def isPageValidInJourney(pageNumber: Int, shortServiceRefundsViewModel: ShortServiceRefundsViewModel): Boolean =
    pageValidInJourneyMap.getOrElse(pageNumber, { _: ShortServiceRefundsViewModel => true })(shortServiceRefundsViewModel)

  private val pageValidInJourneyMap: Map[Int, ShortServiceRefundsViewModel => Boolean] = {
    val isTrue            = { _: ShortServiceRefundsViewModel => true }
    val firstQuestionTrue = { ssr: ShortServiceRefundsViewModel => ssr.shortServiceRefund.getOrElse(false) }

    Map(
      // 1 and 7 are always valid
      1 -> isTrue,
      7 -> isTrue,
      // 2-6 need Q1 true
      2 -> firstQuestionTrue,
      3 -> firstQuestionTrue,
      4 -> firstQuestionTrue,
      5 -> firstQuestionTrue,
      6 -> firstQuestionTrue
    )
  }

  private def previousQuestionIsAnswered(pageNumber: Int,
                                         optIndex: Option[Int],
                                         shortServiceRefundsViewModel: ShortServiceRefundsViewModel): Boolean = {
    val schemesEmpty = shortServiceRefundsViewModel.refundPensionScheme.isEmpty
    val index        = optIndex.getOrElse(if (schemesEmpty) 0 else shortServiceRefundsViewModel.refundPensionScheme.size - 1)

    val prevQuestionIsAnsweredMap: Map[Int, ShortServiceRefundsViewModel => Boolean] = Map(
      1 -> { _: ShortServiceRefundsViewModel => true },
      2 -> { shortServiceRefundsViewModel: ShortServiceRefundsViewModel =>
        shortServiceRefundsViewModel.shortServiceRefund.getOrElse(false) && shortServiceRefundsViewModel.shortServiceRefundCharge.isDefined
      },
      3 -> { shortServiceRefundsViewModel: ShortServiceRefundsViewModel =>
        shortServiceRefundsViewModel.shortServiceRefundTaxPaid.exists(value =>
          !value || (value && shortServiceRefundsViewModel.shortServiceRefundTaxPaidCharge.nonEmpty))
      },
      4 -> { shortServiceRefundsViewModel: ShortServiceRefundsViewModel =>
        if (schemesEmpty) false else shortServiceRefundsViewModel.refundPensionScheme(index).ukRefundCharge.isDefined
      },
      5 -> { shortServiceRefundsViewModel: ShortServiceRefundsViewModel =>
        if (schemesEmpty) true else shortServiceRefundsViewModel.refundPensionScheme.forall(_.isFinished)
      },
      6 -> { shortServiceRefundsViewModel: ShortServiceRefundsViewModel =>
        if (schemesEmpty) false else shortServiceRefundsViewModel.refundPensionScheme(index).isFinished
      },
      7 -> { shortServiceRefundsViewModel: ShortServiceRefundsViewModel =>
        if (isPageValidInJourney(2, shortServiceRefundsViewModel)) shortServiceRefundsViewModel.isFinished
        else !shortServiceRefundsViewModel.shortServiceRefund.getOrElse(true)
      }
    )

    prevQuestionIsAnsweredMap(pageNumber)(shortServiceRefundsViewModel)
  }

  private def validateIndex(index: Option[Int], schemeList: Seq[OverseasRefundPensionScheme]): Option[Int] =
    index.filter(i => i >= 0 && i < schemeList.size)

}
