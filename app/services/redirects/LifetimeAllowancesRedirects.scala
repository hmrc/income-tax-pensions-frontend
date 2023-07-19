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

import controllers.pensions.lifetimeAllowances.routes._
import models.mongo.PensionsCYAModel
import models.pension.charges.PensionLifetimeAllowancesViewModel
import play.api.mvc.Results.Redirect
import play.api.mvc.{Call, Result}
import services.redirects.LifetimeAllowancesPages.{PSTRPage, RemovePSTRPage}
import services.redirects.SimpleRedirectService.checkForExistingSchemes

object LifetimeAllowancesRedirects {

  def cyaPageCall(taxYear: Int): Call = LifetimeAllowanceCYAController.show(taxYear)

  def redirectForSchemeLoop(schemes: Seq[String], taxYear: Int): Call = {
    val filteredSchemes: Seq[String] = schemes.filter(_.trim.nonEmpty)
    checkForExistingSchemes(
      nextPage = PensionSchemeTaxReferenceLifetimeController.show(taxYear, None),
      summaryPage = LifetimePstrSummaryController.show(taxYear),
      schemes = filteredSchemes
    )
  }

  def journeyCheck(currentPage: LifetimeAllowancesPages, cya: PensionsCYAModel, taxYear: Int, optIndex: Option[Int] = None): Option[Result] = {
    val lifetimeAllowancesData = cya.pensionLifetimeAllowances

    if (!previousQuestionIsAnswered(currentPage.journeyNo, lifetimeAllowancesData) || !isPageValidInJourney(currentPage.journeyNo, lifetimeAllowancesData))
      Some(Redirect(AboveAnnualLifetimeAllowanceController.show(taxYear)))
    else if (currentPage.equals(RemovePSTRPage))
      removePstrPageCheck(cya, taxYear, optIndex)
    else if (currentPage.equals(PSTRPage))
      pstrPageCheck(cya, taxYear, optIndex)
    else
      None
  }

  private def removePstrPageCheck(cya: PensionsCYAModel, taxYear: Int, optIndex: Option[Int] = None): Option[Result] = {
    val lifetimeAVM: PensionLifetimeAllowancesViewModel = cya.pensionLifetimeAllowances
    val optSchemes: Seq[String] = lifetimeAVM.pensionSchemeTaxReferences.getOrElse(Seq.empty)
    val index: Option[Int] = optIndex.filter(i => i >= 0 && i < optSchemes.size)
    val schemeIsValid: Boolean = if (index.isEmpty) false else optSchemes(index.getOrElse(0)).nonEmpty

    if (schemeIsValid) None
    else Some(Redirect(LifetimePstrSummaryController.show(taxYear)))
  }

  private def pstrPageCheck(cya: PensionsCYAModel, taxYear: Int, optIndex: Option[Int] = None): Option[Result] = {
    val lifetimeAllowanceVM: PensionLifetimeAllowancesViewModel = cya.pensionLifetimeAllowances
    val optSchemes: Seq[String] = lifetimeAllowanceVM.pensionSchemeTaxReferences.getOrElse(Seq.empty)
    val schemesEmpty: Boolean = !optSchemes.exists(_.nonEmpty)
    val index: Option[Int] = optIndex.filter(i => i >= 0 && i < optSchemes.size)

    (schemesEmpty, index) match {
      // schemes but bad-index -> redirect
      case (false, None) if optIndex.nonEmpty => Some(Redirect(redirectForSchemeLoop(optSchemes, taxYear)))
      // no schemes but index -> error, redirect
      case (true, Some(_)) => Some(Redirect(redirectForSchemeLoop(optSchemes, taxYear)))
      case (_, _) => None
    }
  }

  private def isPageValidInJourney(pageNumber: Int, lifetimeAVM: PensionLifetimeAllowancesViewModel): Boolean =
    pageValidInJourneyMap.getOrElse(pageNumber, { _: PensionLifetimeAllowancesViewModel => true })(lifetimeAVM)

  private val pageValidInJourneyMap: Map[Int, PensionLifetimeAllowancesViewModel => Boolean] = {
    val isTrue = { _: PensionLifetimeAllowancesViewModel => true }
    val firstQuestionTrue = { lifetimeAVM: PensionLifetimeAllowancesViewModel => lifetimeAVM.aboveLifetimeAllowanceQuestion.getOrElse(false) }
    val secondQuestionTrue = { lifetimeAVM: PensionLifetimeAllowancesViewModel =>
      lifetimeAVM.aboveLifetimeAllowanceQuestion.getOrElse(false) && lifetimeAVM.pensionAsLumpSumQuestion.getOrElse(false)
    }
    val fourthQuestionTrue = { lifetimeAVM: PensionLifetimeAllowancesViewModel =>
      lifetimeAVM.aboveLifetimeAllowanceQuestion.getOrElse(false) && lifetimeAVM.pensionPaidAnotherWayQuestion.getOrElse(false)
    }

    Map(
      // 1 and 9 are always valid
      1 -> isTrue, 9 -> isTrue,
      // 2-8 need Q1 true
      2 -> firstQuestionTrue, 4 -> firstQuestionTrue,
      // 3 need Q1+2 true
      3 -> secondQuestionTrue,
      // 5-8 need Q1+4 true
      5 -> fourthQuestionTrue, 6 -> fourthQuestionTrue, 7 -> fourthQuestionTrue, 8 -> fourthQuestionTrue
    )
  }

  private def previousQuestionIsAnswered(pageNumber: Int, lifetimeAVM: PensionLifetimeAllowancesViewModel): Boolean = {

    val prevQuestionIsAnsweredMap: Map[Int, PensionLifetimeAllowancesViewModel => Boolean] = Map(
      1 -> { _: PensionLifetimeAllowancesViewModel => true },

      2 -> { lifetimeAVM: PensionLifetimeAllowancesViewModel => lifetimeAVM.aboveLifetimeAllowanceQuestion.isDefined },

      3 -> { lifetimeAVM: PensionLifetimeAllowancesViewModel => lifetimeAVM.pensionAsLumpSumQuestion.isDefined },

      4 -> { lifetimeAVM: PensionLifetimeAllowancesViewModel =>
        if (isPageValidInJourney(3, lifetimeAVM)) lifetimeAVM.pensionAsLumpSum.exists(_.isFinished)
        else lifetimeAVM.pensionAsLumpSumQuestion.isDefined
      },

      5 -> { lifetimeAllowanceVM: PensionLifetimeAllowancesViewModel => lifetimeAllowanceVM.pensionPaidAnotherWayQuestion.isDefined },

      6 -> { lifetimeAVM: PensionLifetimeAllowancesViewModel => lifetimeAVM.pensionPaidAnotherWay.exists(_.isFinished) },

      7 -> { _: PensionLifetimeAllowancesViewModel => true },

      8 -> { lifetimeAVM: PensionLifetimeAllowancesViewModel => lifetimeAVM.pensionPaidAnotherWay.exists(_.isFinished) },

      9 -> { lifetimeAVM: PensionLifetimeAllowancesViewModel =>
        if (!isPageValidInJourney(2, lifetimeAVM)) !lifetimeAVM.aboveLifetimeAllowanceQuestion.getOrElse(true)
        else if (!isPageValidInJourney(5, lifetimeAVM)) !lifetimeAVM.pensionPaidAnotherWayQuestion.getOrElse(true)
        else lifetimeAVM.isFinished
      }
    )

    prevQuestionIsAnsweredMap(pageNumber)(lifetimeAVM)
  }

}
