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

import models.mongo.PensionsCYAModel
import play.api.mvc.{Call, Result}
import play.api.mvc.Results.Redirect
import controllers.pensions.incomeFromPensions.routes.{StatePensionController, UkPensionIncomeCYAController}
import models.pension.statebenefits.{IncomeFromPensionsViewModel, StateBenefitViewModel}

object IncomeFromPensionsRedirects { //scalastyle:off magic.number

  def cyaPageCall(taxYear: Int): Call = UkPensionIncomeCYAController.show(taxYear)

  def journeyCheck(currentPage: IncomeFromPensionsPages, cya: PensionsCYAModel, taxYear: Int): Option[Result] = {
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

    val amountPaidQuestionFn = {statePensionViewModel: StateBenefitViewModel =>
      statePensionViewModel.amountPaidQuestion.getOrElse(false)
    }


    Map(
      // 2-7 need Q1 true
      // 2 need Q1 true + Q1 amount
//      2 -> amountPaidQuestionFn,
      // 3 need Q1 true + Q2 date OR Q1 false
      // 4 and 5 need Q1 true + Q3 amount
    )
  }

  private def previousQuestionIsAnswered(pageNumber: Int, incomeFromPensionsViewModel: IncomeFromPensionsViewModel): Boolean =
    prevQuestionIsAnsweredMap(pageNumber)(incomeFromPensionsViewModel)

  private val prevQuestionIsAnsweredMap: Map[Int, IncomeFromPensionsViewModel => Boolean] = Map(
    1 -> {_: IncomeFromPensionsViewModel => true},

    2 -> {incomeFromPensionsViewModel: IncomeFromPensionsViewModel => incomeFromPensionsViewModel.statePension.head.amountPaidQuestion.isDefined},

    3 -> {incomeFromPensionsViewModel: IncomeFromPensionsViewModel =>
          if (isPageValidInJourney(3, incomeFromPensionsViewModel)) {
            incomeFromPensionsViewModel.statePension.head.startDateQuestion.isDefined}
          else {
            incomeFromPensionsViewModel.statePension.head.amountPaidQuestion.isDefined
          }
    },

    4 -> {incomeFromPensionsViewModel: IncomeFromPensionsViewModel => incomeFromPensionsViewModel.statePensionLumpSum.head.amountPaidQuestion.isDefined},

    5 -> {incomeFromPensionsViewModel: IncomeFromPensionsViewModel => incomeFromPensionsViewModel.statePensionLumpSum.head.startDateQuestion.isDefined},

    6 -> {incomeFromPensionsViewModel: IncomeFromPensionsViewModel =>
      if (isPageValidInJourney(6, incomeFromPensionsViewModel)) {
        incomeFromPensionsViewModel.statePensionLumpSum.head.startDateQuestion.isDefined}
      else {
        incomeFromPensionsViewModel.statePensionLumpSum.head.amountPaidQuestion.isDefined
      }
    },
    7 -> {incomeFromPensionsViewModel: IncomeFromPensionsViewModel => incomeFromPensionsViewModel.statePension.head.addToCalculation.isDefined}
  )

}