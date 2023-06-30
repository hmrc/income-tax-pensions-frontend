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


import controllers.pensions.incomeFromOverseasPensions.routes.{CountrySummaryListController, IncomeFromOverseasPensionsCYAController, PensionOverseasIncomeCountryController, PensionOverseasIncomeStatus}
import models.mongo.PensionsCYAModel
import models.pension.charges.{IncomeFromOverseasPensionsViewModel, PensionScheme}
import play.api.mvc.Results.Redirect
import play.api.mvc.{Call, Result}
import services.redirects.SimpleRedirectService.checkForExistingSchemes

object IncomeFromOverseasPensionsRedirects {

  def redirectForSchemeLoop(schemes: Seq[PensionScheme], taxYear: Int): Call = {
    checkForExistingSchemes(
      nextPage = PensionOverseasIncomeCountryController.show(taxYear, None),
      summaryPage = CountrySummaryListController.show(taxYear),
      schemes = schemes
    )
  }

  def cyaPageCall(taxYear: Int): Call = IncomeFromOverseasPensionsCYAController.show(taxYear)

  def journeyCheck(currentPage: IncomeFromOverseasPensionsPages, cya: PensionsCYAModel, taxYear: Int): Option[Result] = {
    val incomeFOP = cya.incomeFromOverseasPensions
    if (isPageValidInJourney(currentPage.journeyNo, incomeFOP) && previousQuestionIsAnswered(currentPage.journeyNo, incomeFOP)) {
      None
    } else {
      Some(Redirect(PensionOverseasIncomeStatus.show(taxYear)))
    }
  }

  private def isPageValidInJourney(pageNumber: Int, incomeFromOverseasPensionsViewModel: IncomeFromOverseasPensionsViewModel): Boolean =
    pageValidInJourneyMap.getOrElse(pageNumber, { _: IncomeFromOverseasPensionsViewModel => true })(incomeFromOverseasPensionsViewModel)

  private val pageValidInJourneyMap: Map[Int, IncomeFromOverseasPensionsViewModel => Boolean] = {

    Map(
      1 | 10 -> true,
      // 2-9 need Q1 true
      2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 -> { incomeFromOverseasPensionsViewModel: IncomeFromOverseasPensionsViewModel =>
        incomeFromOverseasPensionsViewModel.paymentsFromOverseasPensionsQuestion.getOrElse(false)
      }
    )
  }

  private def previousQuestionIsAnswered(pageNumber: Int, incomeFromOverseasPensionsViewModel: IncomeFromOverseasPensionsViewModel): Boolean =
    prevQuestionIsAnsweredMap(pageNumber)(incomeFromOverseasPensionsViewModel)

  private val prevQuestionIsAnsweredMap: Map[Int, IncomeFromOverseasPensionsViewModel => Boolean] = Map(
    1 -> { _: IncomeFromOverseasPensionsViewModel => true },

    2 -> { incomeFromOverseasPensionsViewModel: IncomeFromOverseasPensionsViewModel => incomeFromOverseasPensionsViewModel.paymentsFromOverseasPensionsQuestion.isDefined },

    3 -> { incomeFromOverseasPensionsViewModel: IncomeFromOverseasPensionsViewModel =>
      incomeFromOverseasPensionsViewModel.overseasIncomePensionSchemes.head.alphaThreeCode.isDefined
    },
    4 -> { incomeFromOverseasPensionsViewModel: IncomeFromOverseasPensionsViewModel =>
      incomeFromOverseasPensionsViewModel.overseasIncomePensionSchemes.head.pensionPaymentAmount.isDefined &&
        incomeFromOverseasPensionsViewModel.overseasIncomePensionSchemes.head.pensionPaymentTaxPaid.isDefined
    },
    5 -> { incomeFromOverseasPensionsViewModel: IncomeFromOverseasPensionsViewModel =>
      incomeFromOverseasPensionsViewModel.overseasIncomePensionSchemes.head.specialWithholdingTaxQuestion.exists(x =>
        if (x) incomeFromOverseasPensionsViewModel.overseasIncomePensionSchemes.head.specialWithholdingTaxAmount.isDefined else true)
    },

    6 | 7 | 8 -> { incomeFromOverseasPensionsViewModel: IncomeFromOverseasPensionsViewModel =>
      incomeFromOverseasPensionsViewModel.overseasIncomePensionSchemes.head.foreignTaxCreditReliefQuestion.isDefined
    },

    9 -> { incomeFromOverseasPensionsViewModel: IncomeFromOverseasPensionsViewModel => incomeFromOverseasPensionsViewModel.overseasIncomePensionSchemes.nonEmpty },

    10 -> { incomeFromOverseasPensionsViewModel: IncomeFromOverseasPensionsViewModel =>
      if (isPageValidInJourney(2, incomeFromOverseasPensionsViewModel)) {
        incomeFromOverseasPensionsViewModel.overseasIncomePensionSchemes.nonEmpty
      } else {
        !incomeFromOverseasPensionsViewModel.paymentsFromOverseasPensionsQuestion.getOrElse(true)
      }
    }
  )

}
