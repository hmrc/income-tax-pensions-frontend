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


import controllers.pensions.incomeFromOverseasPensions.routes._
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.pension.charges.{IncomeFromOverseasPensionsViewModel, PensionScheme}
import play.api.mvc.Results.Redirect
import play.api.mvc.{Call, Result}
import services.redirects.SimpleRedirectService.{checkForExistingSchemes, redirectBasedOnCurrentAnswers}

import scala.concurrent.Future

object IncomeFromOverseasPensionsRedirects {

  def cyaPageCall(taxYear: Int): Call = IncomeFromOverseasPensionsCYAController.show(taxYear)

  def indexCheckThenJourneyCheck(data: PensionsUserData, optIndex: Option[Int],
                                 currentPage: IncomeFromOverseasPensionsPages,
                                 taxYear: Int)(continue: PensionsUserData => Future[Result]): Future[Result] = {

    val pensionSchemes = data.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes
    validateIndex(optIndex, pensionSchemes) match {
      case Some(index) =>

        val checkRedirect = journeyCheck(currentPage, _, taxYear, Some(index))
        redirectBasedOnCurrentAnswers(taxYear, Some(data), cyaPageCall(taxYear))(checkRedirect) {
          data: PensionsUserData =>
            continue(data)
        }

      case None =>

        val checkRedirect = journeyCheck(currentPage, _: PensionsCYAModel, taxYear)
        redirectBasedOnCurrentAnswers(taxYear, Some(data), cyaPageCall(taxYear))(checkRedirect) {
          _ =>
            Future.successful(Redirect(redirectForSchemeLoop(pensionSchemes, taxYear)))
        }
    }
  }

  def redirectForSchemeLoop(schemes: Seq[PensionScheme], taxYear: Int): Call = {
    checkForExistingSchemes(
      nextPage = PensionOverseasIncomeCountryController.show(taxYear, None),
      summaryPage = CountrySummaryListController.show(taxYear),
      schemes = schemes
    )
  }

  def journeyCheck(currentPage: IncomeFromOverseasPensionsPages, cya: PensionsCYAModel, taxYear: Int, index: Option[Int] = None): Option[Result] = {
    val incomeFOP = cya.incomeFromOverseasPensions
    if (isPageValidInJourney(currentPage.journeyNo, incomeFOP) && previousQuestionIsAnswered(currentPage.journeyNo, index, incomeFOP)) {
      None
    } else {
      Some(Redirect(PensionOverseasIncomeStatus.show(taxYear)))
    }
  }

  private def isPageValidInJourney(pageNumber: Int, incomeFromOverseasPensionsViewModel: IncomeFromOverseasPensionsViewModel): Boolean =
    pageValidInJourneyMap.getOrElse(pageNumber, { _: IncomeFromOverseasPensionsViewModel => true })(incomeFromOverseasPensionsViewModel)

  private val pageValidInJourneyMap: Map[Int, IncomeFromOverseasPensionsViewModel => Boolean] = {
    val isTrue = { _: IncomeFromOverseasPensionsViewModel => true }
    val firstQuestionTrue = { incomeFromOverseasPensionsViewModel: IncomeFromOverseasPensionsViewModel =>
      incomeFromOverseasPensionsViewModel.paymentsFromOverseasPensionsQuestion.getOrElse(false)
    }

    Map(
      1 -> isTrue, 10 -> isTrue,
      // 2-9 need Q1 true
      2 -> firstQuestionTrue, 3 -> firstQuestionTrue, 4 -> firstQuestionTrue, 5 -> firstQuestionTrue,
      6 -> firstQuestionTrue, 7 -> firstQuestionTrue, 8 -> firstQuestionTrue, 9 -> firstQuestionTrue
    )
  }

  private def previousQuestionIsAnswered(pageNumber: Int, optIndex: Option[Int], incomeFromOverseasPensionsViewModel: IncomeFromOverseasPensionsViewModel): Boolean = {
    val index = optIndex.getOrElse(0)

    val prevQuestionIsAnsweredMap: Map[Int, IncomeFromOverseasPensionsViewModel => Boolean] = Map(
      1 -> { _: IncomeFromOverseasPensionsViewModel => true },

      2 -> { incomeFromOverseasPensionsViewModel: IncomeFromOverseasPensionsViewModel =>
        incomeFromOverseasPensionsViewModel.paymentsFromOverseasPensionsQuestion.isDefined
      },

      3 -> { incomeFromOverseasPensionsViewModel: IncomeFromOverseasPensionsViewModel =>
        incomeFromOverseasPensionsViewModel.overseasIncomePensionSchemes(index).alphaTwoCode.isDefined
      },
      4 -> { incomeFromOverseasPensionsViewModel: IncomeFromOverseasPensionsViewModel =>
        incomeFromOverseasPensionsViewModel.overseasIncomePensionSchemes(index).pensionPaymentAmount.isDefined &&
          incomeFromOverseasPensionsViewModel.overseasIncomePensionSchemes(index).pensionPaymentTaxPaid.isDefined
      },
      5 -> { incomeFromOverseasPensionsViewModel: IncomeFromOverseasPensionsViewModel =>
        incomeFromOverseasPensionsViewModel.overseasIncomePensionSchemes(index).specialWithholdingTaxQuestion.exists(x =>
          if (x) incomeFromOverseasPensionsViewModel.overseasIncomePensionSchemes(index).specialWithholdingTaxAmount.isDefined else true)
      },

      6 -> { incomeFromOverseasPensionsViewModel: IncomeFromOverseasPensionsViewModel =>
        incomeFromOverseasPensionsViewModel.overseasIncomePensionSchemes(index).foreignTaxCreditReliefQuestion.isDefined
      },
      7 -> { incomeFromOverseasPensionsViewModel: IncomeFromOverseasPensionsViewModel =>
        incomeFromOverseasPensionsViewModel.overseasIncomePensionSchemes(index).foreignTaxCreditReliefQuestion.isDefined
      },
      8 -> { incomeFromOverseasPensionsViewModel: IncomeFromOverseasPensionsViewModel =>
        incomeFromOverseasPensionsViewModel.overseasIncomePensionSchemes(index).foreignTaxCreditReliefQuestion.isDefined
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

    if (incomeFromOverseasPensionsViewModel.overseasIncomePensionSchemes.isEmpty)
      incomeFromOverseasPensionsViewModel.paymentsFromOverseasPensionsQuestion.isDefined
    else
      prevQuestionIsAnsweredMap(pageNumber)(incomeFromOverseasPensionsViewModel)
  }

  private def validateIndex(index: Option[Int], pensionSchemesList: Seq[PensionScheme]): Option[Int] = {
    index.filter(i => i >= 0 && i < pensionSchemesList.size)
  }

}
