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
import controllers.pensions.incomeFromPensions.routes._
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.pension.statebenefits.{IncomeFromPensionsViewModel, UkPensionIncomeViewModel}
import play.api.mvc.Results.Redirect
import play.api.mvc.{Call, Result}
import services.redirects.IncomeFromOtherUkPensionsPages._
import services.redirects.SimpleRedirectService.{checkForExistingSchemes, redirectBasedOnCurrentAnswers}

import scala.concurrent.Future

object IncomeFromOtherUkPensionsRedirects {

  def cyaPageCall(taxYear: Int): Call = UkPensionIncomeCYAController.show(TaxYear(taxYear))

  def schemeIsFinishedCheck(schemes: Seq[UkPensionIncomeViewModel], index: Int, taxYear: Int, continueRedirect: Call): Result =
    if (schemes(index).isFinished) {
      Redirect(PensionSchemeSummaryController.show(taxYear, Some(index)))
    } else {
      Redirect(continueRedirect)
    }

  def indexCheckThenJourneyCheck(data: PensionsUserData, optIndex: Option[Int], currentPage: IncomeFromOtherUkPensionsPages, taxYear: Int)(
      continue: PensionsUserData => Future[Result]): Future[Result] = {

    val checkRedirect = journeyCheck(currentPage, _, taxYear, optIndex)
    redirectBasedOnCurrentAnswers(taxYear, Some(data), cyaPageCall(taxYear))(checkRedirect) { data: PensionsUserData =>
      val pensionSchemes: Seq[UkPensionIncomeViewModel] = data.pensions.incomeFromPensions.getUKPensionIncomes
      val validatedIndex: Option[Int]                   = optIndex.filter(pensionSchemes.indices.contains)

      (pensionSchemes.nonEmpty, validatedIndex) match {
        case (true, Some(_))                                                   => continue(data) // happy path
        case (_, _) if currentPage.equals(DoYouGetUkPensionSchemePaymentsPage) => continue(data)
        case (_, _) if currentPage.equals(CheckUkPensionIncomeCYAPage)         => continue(data)
        case (_, None) if currentPage.equals(PensionSchemeDetailsPage)         => continue(data) // new scheme
        case (_, None) if currentPage.equals(RemovePensionIncomePage) =>
          Future.successful(Redirect(UkPensionIncomeSummaryController.show(taxYear))) // bad remove request
        case (_, _) => Future.successful(Redirect(redirectForSchemeLoop(pensionSchemes, taxYear))) // scheme start or summary
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

  def journeyCheck(currentPage: IncomeFromOtherUkPensionsPages, cya: PensionsCYAModel, taxYear: Int, optIndex: Option[Int] = None): Option[Result] = {
    val incomeFromPensions = cya.incomeFromPensions
    if (isPageValidInJourney(currentPage.pageId, incomeFromPensions) && previousQuestionIsAnswered(
        currentPage.pageId,
        optIndex,
        incomeFromPensions)) {
      None
    } else {
      Some(Redirect(UkPensionSchemePaymentsController.show(taxYear)))
    }
  }

  private def isPageValidInJourney(pageNumber: Int, viewModel: IncomeFromPensionsViewModel): Boolean =
    pageValidInJourneyMap.getOrElse(pageNumber, { _: IncomeFromPensionsViewModel => true })(viewModel)

  private val pageValidInJourneyMap: Map[Int, IncomeFromPensionsViewModel => Boolean] = {
    val valid: IncomeFromPensionsViewModel => Boolean = _ => true
    val firstQuestionTrue = { viewModel: IncomeFromPensionsViewModel => viewModel.uKPensionIncomesQuestion.getOrElse(false) }

    Map(
      1 -> valid,
      8 -> valid,
      // Page 2 to 7 requires first question to be answered true
      2 -> firstQuestionTrue,
      3 -> firstQuestionTrue,
      4 -> firstQuestionTrue,
      5 -> firstQuestionTrue,
      6 -> firstQuestionTrue,
      7 -> firstQuestionTrue
    )
  }

  private def previousQuestionIsAnswered(pageId: Int, optIndex: Option[Int], viewModel: IncomeFromPensionsViewModel): Boolean = {
    val validIndex = optIndex.filter(viewModel.getUKPensionIncomes.indices.contains)

    val previousQuestionAnswered: Map[Int, IncomeFromPensionsViewModel => Boolean] = Map(
      1 -> { _: IncomeFromPensionsViewModel => true },
      2 -> { viewModel: IncomeFromPensionsViewModel =>
        if (optIndex.nonEmpty && validIndex.isEmpty) false else viewModel.uKPensionIncomesQuestion.isDefined
      },
      3 -> { viewModel: IncomeFromPensionsViewModel =>
        if (validIndex.isEmpty) {
          false
        } else {
          viewModel.getUKPensionIncomes(validIndex.get).pensionSchemeName.isDefined &&
          viewModel.getUKPensionIncomes(validIndex.get).pensionSchemeRef.isDefined &&
          viewModel.getUKPensionIncomes(validIndex.get).pensionId.isDefined
        }
      },
      4 -> { viewModel: IncomeFromPensionsViewModel =>
        if (validIndex.isEmpty) {
          false
        } else {
          viewModel.getUKPensionIncomes(validIndex.get).amount.isDefined && viewModel.getUKPensionIncomes(validIndex.get).taxPaid.isDefined
        }
      },
      5 -> { viewModel: IncomeFromPensionsViewModel =>
        if (validIndex.isEmpty) {
          false
        } else {
          viewModel.getUKPensionIncomes(validIndex.get).isFinished
        }
      },
      6 -> { _: IncomeFromPensionsViewModel => true }, // if valid then schemes can exist or be empty
      7 -> { viewModel: IncomeFromPensionsViewModel =>
        if (validIndex.isEmpty) {
          true // to summary page
        } else {
          viewModel.getUKPensionIncomes(validIndex.get).isFinished // to journey start
        }
      },
      8 -> { viewModel: IncomeFromPensionsViewModel =>
        if (isPageValidInJourney(2, viewModel)) {
          viewModel.isUkPensionFinished
        } else {
          !viewModel.uKPensionIncomesQuestion.getOrElse(true)
        }
      }
    )

    previousQuestionAnswered(pageId)(viewModel)
  }
}
