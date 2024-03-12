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

import controllers.pensions.incomeFromPensions.routes._
import models.mongo.PensionsCYAModel
import models.pension.statebenefits.IncomeFromPensionsViewModel
import play.api.mvc.Results.Redirect
import play.api.mvc.{Call, Result}

object StatePensionRedirects {

  def firstPageRedirect(taxYear: Int): Result             = Redirect(StatePensionController.show(taxYear))
  def claimLumpSumRedirect(taxYear: Int): Result          = Redirect(StatePensionLumpSumController.show(taxYear))
  def taxPaidOnLumpSumRedirect(taxYear: Int): Result      = Redirect(TaxPaidOnStatePensionLumpSumController.show(taxYear))
  def statePensionStartDateRedirect(taxYear: Int): Result = Redirect(StatePensionStartDateController.show(taxYear))
  def lumpSumStartDateRedirect(taxYear: Int): Result      = Redirect(StatePensionLumpSumStartDateController.show(taxYear))
  def cyaPageRedirect(taxYear: Int): Result               = Redirect(cyaPageCall(taxYear))
  def taskListRedirect(taxYear: Int): Result              = Redirect(IncomeFromPensionsSummaryController.show(taxYear))

  def cyaPageCall(taxYear: Int): Call = StatePensionCYAController.show(taxYear)

  def journeyCheck(currentPage: StatePensionPages, cya: PensionsCYAModel, taxYear: Int): Option[Result] = {
    val incomeFromPensions = cya.incomeFromPensions
    if (isPageValidInJourney(currentPage.journeyNo, incomeFromPensions) && previousQuestionIsAnswered(currentPage.journeyNo, incomeFromPensions)) {
      None
    } else {
      Some(Redirect(StatePensionController.show(taxYear)))
    }
  }

  private def isPageValidInJourney(pageNumber: Int, incomeFromPensionsViewModel: IncomeFromPensionsViewModel): Boolean =
    pageValidInJourneyMap.getOrElse(pageNumber, { _: IncomeFromPensionsViewModel => true })(incomeFromPensionsViewModel)

  private val pageValidInJourneyMap: Map[Int, IncomeFromPensionsViewModel => Boolean] = {

    val isTrue = { _: IncomeFromPensionsViewModel => true }
    val statePensionQuestionFn = { incomeFromPensionsViewModel: IncomeFromPensionsViewModel =>
      incomeFromPensionsViewModel.statePension.exists(_.amountPaidQuestion.exists(x => x))
    }
    val lumpSumQuestionFn = { incomeFromPensionsViewModel: IncomeFromPensionsViewModel =>
      incomeFromPensionsViewModel.statePensionLumpSum.exists(_.amountPaidQuestion.exists(x => x))
    }

    Map(
      1 -> isTrue,
      3 -> isTrue,
      6 -> isTrue,
      7 -> isTrue,
      // 2 need Q1: statePension = true
      2 -> statePensionQuestionFn,
      // 4 and 5 need Q1: statePensionLumpSome = true
      4 -> lumpSumQuestionFn,
      5 -> lumpSumQuestionFn
    )
  }

  private def previousQuestionIsAnswered(pageNumber: Int, incomeFromPensionsViewModel: IncomeFromPensionsViewModel): Boolean =
    prevQuestionIsAnsweredMap(pageNumber)(incomeFromPensionsViewModel)

  private val prevQuestionIsAnsweredMap: Map[Int, IncomeFromPensionsViewModel => Boolean] = Map(
    1 -> { _: IncomeFromPensionsViewModel => true },
    2 -> { incomeFromPensionsViewModel: IncomeFromPensionsViewModel => incomeFromPensionsViewModel.statePension.exists(_.amount.isDefined) },
    3 -> { incomeFromPensionsViewModel: IncomeFromPensionsViewModel =>
      if (isPageValidInJourney(2, incomeFromPensionsViewModel)) {
        incomeFromPensionsViewModel.statePension.exists(_.startDateQuestion.isDefined)
      } else {
        incomeFromPensionsViewModel.statePension.exists(_.amountPaidQuestion.isDefined)
      }
    },
    4 -> { incomeFromPensionsViewModel: IncomeFromPensionsViewModel => incomeFromPensionsViewModel.statePensionLumpSum.exists(_.amount.isDefined) },
    5 -> { incomeFromPensionsViewModel: IncomeFromPensionsViewModel =>
      incomeFromPensionsViewModel.statePensionLumpSum.exists(spls => spls.taxPaidQuestion.exists(x => if (x) spls.taxPaid.isDefined else true))
    },
    6 -> { incomeFromPensionsViewModel: IncomeFromPensionsViewModel =>
      if (isPageValidInJourney(5, incomeFromPensionsViewModel)) {
        incomeFromPensionsViewModel.statePensionLumpSum.exists(_.startDateQuestion.isDefined)
      } else {
        incomeFromPensionsViewModel.statePensionLumpSum.isDefined
      }
    },
    7 -> { incomeFromPensionsViewModel: IncomeFromPensionsViewModel => incomeFromPensionsViewModel.isStatePensionFinished }
  )

}
