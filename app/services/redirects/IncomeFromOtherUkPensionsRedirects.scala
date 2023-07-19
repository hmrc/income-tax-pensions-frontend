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
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.pension.statebenefits.{IncomeFromPensionsViewModel, UkPensionIncomeViewModel}
import play.api.mvc.Results.Redirect
import play.api.mvc.{Call, Result}
import services.redirects.SimpleRedirectService.{checkForExistingSchemes, redirectBasedOnCurrentAnswers}

import scala.concurrent.Future

object IncomeFromOtherUkPensionsRedirects {

  def cyaPageCall(taxYear: Int): Call = UkPensionIncomeCYAController.show(taxYear)

  def indexCheckThenJourneyCheck(data: PensionsUserData, index: Option[Int],
                                 currentPage: IncomeFromOtherUkPensionsPages,
                                 taxYear: Int)(continue: PensionsUserData => Future[Result]): Future[Result] = {
    val pensionSchemes = data.pensions.incomeFromPensions.uKPensionIncomes
    val validatedIndex = index.filter(pensionSchemes.indices.contains)
    (pensionSchemes, validatedIndex) match {
      case (schemes, None) if schemes.nonEmpty =>
        val checkRedirect = journeyCheck(currentPage, _: PensionsCYAModel, taxYear)
        redirectBasedOnCurrentAnswers(taxYear, Some(data), cyaPageCall(taxYear))(checkRedirect) {
          _ =>
            Future.successful(Redirect(redirectForSchemeLoop(schemes, taxYear)))
        }

      case (_, someIndex) =>

        val checkRedirect = journeyCheck(currentPage, _, taxYear, someIndex)
        redirectBasedOnCurrentAnswers(taxYear, Some(data), cyaPageCall(taxYear))(checkRedirect) {
          data: PensionsUserData =>
            continue(data)
        }
    }
  }

  def redirectForSchemeLoop(schemes: Seq[UkPensionIncomeViewModel], taxYear: Int): Call = {
    val filteredSchemes = schemes.filter(_.isFinished)
    checkForExistingSchemes(
      nextPage = PensionSchemeDetailsController.show(taxYear, None),
      summaryPage = UkPensionIncomeSummaryController.show(taxYear),
      schemes = filteredSchemes
    )
  }

  def journeyCheck(currentPage: IncomeFromOtherUkPensionsPages, cya: PensionsCYAModel, taxYear: Int, index: Option[Int] = None): Option[Result] = {
    val incomeFromPensions = cya.incomeFromPensions
    if (isPageValidInJourney(currentPage.pageId, incomeFromPensions) && previousQuestionIsAnswered(currentPage.pageId, index, incomeFromPensions)) {
      None
    } else {
      Some(Redirect(UkPensionSchemePaymentsController.show(taxYear)))
    }
  }

  private def isPageValidInJourney(pageNumber: Int, viewModel: IncomeFromPensionsViewModel): Boolean =
    pageValidInJourneyMap.getOrElse(pageNumber, { _: IncomeFromPensionsViewModel => true })(viewModel)

  private val pageValidInJourneyMap: Map[Int, IncomeFromPensionsViewModel => Boolean] = {
    val valid: IncomeFromPensionsViewModel => Boolean = _ => true

    val firstQuestionTrue = { viewModel: IncomeFromPensionsViewModel =>
      viewModel.uKPensionIncomesQuestion.getOrElse(false)
    }

    Map(
      1 -> valid, 8 -> valid,
      // Page 2 to 7 requires first question to be answered true
      2 -> firstQuestionTrue, 3 -> firstQuestionTrue,
      4 -> firstQuestionTrue, 5 -> firstQuestionTrue,
      6 -> firstQuestionTrue, 7 -> firstQuestionTrue
    )
  }

  private def previousQuestionIsAnswered(pageId: Int, optIndex: Option[Int], viewModel: IncomeFromPensionsViewModel): Boolean = {
    val emptyScheme = viewModel.uKPensionIncomes.isEmpty
    val index = optIndex.getOrElse(if (emptyScheme) 0 else viewModel.uKPensionIncomes.size -1)

    val previousQuestionAnswered: Map[Int, IncomeFromPensionsViewModel => Boolean] = Map(
      1 -> { _: IncomeFromPensionsViewModel => true },
      2 -> { viewModel: IncomeFromPensionsViewModel =>
        viewModel.uKPensionIncomesQuestion.isDefined
      },
      3 -> { viewModel: IncomeFromPensionsViewModel =>
        if(emptyScheme) {
          false
        } else {
          viewModel.uKPensionIncomes(index).pensionSchemeName.isDefined &&
          viewModel.uKPensionIncomes(index).pensionSchemeRef.isDefined &&
          viewModel.uKPensionIncomes(index).pensionId.isDefined
        }
      },
      4 -> { viewModel: IncomeFromPensionsViewModel =>
        if (emptyScheme) {
          false
        } else {
          viewModel.uKPensionIncomes(index).amount.isDefined &&
          viewModel.uKPensionIncomes(index).taxPaid.isDefined
        }
      },
      5 -> { viewModel: IncomeFromPensionsViewModel =>
        if (emptyScheme) {
          false
        } else {
          viewModel.uKPensionIncomes(index).isFinished
        }
      },
      6 -> { viewModel: IncomeFromPensionsViewModel =>
        if (emptyScheme) {
          false
        } else {
          viewModel.uKPensionIncomes.forall(_.isFinished)
        }
      },
      7 -> { viewModel: IncomeFromPensionsViewModel =>
        if (emptyScheme) {
          false
        } else {
          viewModel.uKPensionIncomes(index).isFinished
        }
      },
      8 -> { viewModel: IncomeFromPensionsViewModel =>
        viewModel.uKPensionIncomesQuestion.isDefined
      }
    )
    previousQuestionAnswered(pageId)(viewModel)
  }
}
