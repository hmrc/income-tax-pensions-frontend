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

import controllers.pensions.annualAllowances.routes._
import models.mongo.PensionsCYAModel
import models.pension.charges.PensionAnnualAllowancesViewModel
import play.api.mvc.Results.Redirect
import play.api.mvc.{Call, Result}
import services.redirects.AnnualAllowancesPages.{PSTRPage, RemovePSTRPage}
import services.redirects.SimpleRedirectService.checkForExistingSchemes

object AnnualAllowancesRedirects {

  def cyaPageCall(taxYear: Int): Call = AnnualAllowanceCYAController.show(taxYear)

  def redirectForSchemeLoop(schemes: Seq[String], taxYear: Int): Call = {
    val filteredSchemes: Seq[String] = schemes.filter(_.trim.nonEmpty)
    checkForExistingSchemes(
      nextPage = PensionSchemeTaxReferenceController.show(taxYear, None),
      summaryPage = PstrSummaryController.show(taxYear),
      schemes = filteredSchemes
    )
  }

  def journeyCheck(currentPage: AnnualAllowancesPages, cya: PensionsCYAModel, taxYear: Int, optIndex: Option[Int] = None): Option[Result] = {
    val annualAllowanceData = cya.pensionsAnnualAllowances

    if (!previousQuestionIsAnswered(currentPage.journeyNo, annualAllowanceData) || !isPageValidInJourney(
        currentPage.journeyNo,
        annualAllowanceData)) {
      Some(Redirect(ReducedAnnualAllowanceController.show(taxYear)))
    } else if (currentPage.equals(RemovePSTRPage)) {
      removePstrPageCheck(cya, taxYear, optIndex)
    } else if (currentPage.equals(PSTRPage)) {
      pstrPageCheck(cya, taxYear, optIndex)
    } else {
      None
    }
  }

  private def removePstrPageCheck(cya: PensionsCYAModel, taxYear: Int, optIndex: Option[Int]): Option[Result] = {
    val annualAVM: PensionAnnualAllowancesViewModel = cya.pensionsAnnualAllowances
    val optSchemes: Seq[String]                     = annualAVM.pensionSchemeTaxReferences.getOrElse(Seq.empty)
    val index: Option[Int]                          = optIndex.filter(i => i >= 0 && i < optSchemes.size)
    val schemeIsValid: Boolean                      = if (index.isEmpty) false else optSchemes(index.getOrElse(0)).nonEmpty

    if (schemeIsValid) None else Some(Redirect(PstrSummaryController.show(taxYear)))
  }

  private def pstrPageCheck(cya: PensionsCYAModel, taxYear: Int, optIndex: Option[Int]): Option[Result] = {
    val annualAllowanceVM: PensionAnnualAllowancesViewModel = cya.pensionsAnnualAllowances
    val optSchemes: Seq[String]                             = annualAllowanceVM.pensionSchemeTaxReferences.getOrElse(Seq.empty)
    val schemesEmpty: Boolean                               = !optSchemes.exists(_.nonEmpty)
    val index: Option[Int]                                  = optIndex.filter(i => i >= 0 && i < optSchemes.size)

    (schemesEmpty, index) match {
      // schemes but bad-index -> redirect
      case (false, None) if optIndex.nonEmpty => Some(Redirect(redirectForSchemeLoop(optSchemes, taxYear)))
      // no schemes but index -> error, redirect
      case (true, Some(_)) => Some(Redirect(redirectForSchemeLoop(optSchemes, taxYear)))
      case (_, _)          => None
    }
  }

  private def isPageValidInJourney(pageNumber: Int, annualAVM: PensionAnnualAllowancesViewModel): Boolean =
    pageValidInJourneyMap.getOrElse(pageNumber, { _: PensionAnnualAllowancesViewModel => true })(annualAVM)

  private val pageValidInJourneyMap: Map[Int, PensionAnnualAllowancesViewModel => Boolean] = {
    val isTrue            = { _: PensionAnnualAllowancesViewModel => true }
    val firstQuestionTrue = { annualAVM: PensionAnnualAllowancesViewModel => annualAVM.reducedAnnualAllowanceQuestion.getOrElse(false) }
    val thirdQuestionTrue = { annualAVM: PensionAnnualAllowancesViewModel =>
      annualAVM.reducedAnnualAllowanceQuestion.getOrElse(false) && annualAVM.aboveAnnualAllowanceQuestion.getOrElse(false)
    }
    val fourthQuestionTrue = { annualAVM: PensionAnnualAllowancesViewModel =>
      annualAVM.reducedAnnualAllowanceQuestion.getOrElse(false) &&
      annualAVM.aboveAnnualAllowanceQuestion.getOrElse(false) &&
      annualAVM.pensionProvidePaidAnnualAllowanceQuestion.getOrElse(false)
    }

    Map(
      // 1 and 8 are always valid
      1 -> isTrue,
      8 -> isTrue,
      // 2-7 need Q1 true
      2 -> firstQuestionTrue,
      3 -> firstQuestionTrue,
      // 4-7 need Q3 true
      4 -> thirdQuestionTrue,
      // 5-7 need Q4 true
      5 -> fourthQuestionTrue,
      6 -> fourthQuestionTrue,
      7 -> fourthQuestionTrue
    )
  }

  private def previousQuestionIsAnswered(pageNumber: Int, annualAVM: PensionAnnualAllowancesViewModel): Boolean = {

    val prevQuestionIsAnsweredMap: Map[Int, PensionAnnualAllowancesViewModel => Boolean] = Map(
      1 -> { _: PensionAnnualAllowancesViewModel => true },
      2 -> { annualAVM: PensionAnnualAllowancesViewModel => annualAVM.reducedAnnualAllowanceQuestion.getOrElse(false) },
      3 -> { annualAVM: PensionAnnualAllowancesViewModel =>
        annualAVM.moneyPurchaseAnnualAllowance.getOrElse(false) || annualAVM.taperedAnnualAllowance.getOrElse(false)
      },
      4 -> { annualAVM: PensionAnnualAllowancesViewModel =>
        annualAVM.aboveAnnualAllowanceQuestion.exists(x => x && annualAVM.aboveAnnualAllowance.isDefined)
      },
      5 -> { annualAVM: PensionAnnualAllowancesViewModel =>
        annualAVM.pensionProvidePaidAnnualAllowanceQuestion.exists(x => x && annualAVM.taxPaidByPensionProvider.isDefined)
      },
      6 -> { _: PensionAnnualAllowancesViewModel => true }, // if valid then PSTRs can exist or be empty
      7 -> { annualAVM: PensionAnnualAllowancesViewModel =>
        annualAVM.pensionProvidePaidAnnualAllowanceQuestion.exists(x => x && annualAVM.taxPaidByPensionProvider.isDefined)
      },
      8 -> { annualAVM: PensionAnnualAllowancesViewModel =>
        if (!isPageValidInJourney(2, annualAVM)) {
          !annualAVM.reducedAnnualAllowanceQuestion.getOrElse(true)
        } else if (!isPageValidInJourney(4, annualAVM)) {
          !annualAVM.aboveAnnualAllowanceQuestion.getOrElse(true)
        } else if (!isPageValidInJourney(5, annualAVM)) {
          !annualAVM.pensionProvidePaidAnnualAllowanceQuestion.getOrElse(true)
        } else {
          annualAVM.isFinished
        }
      }
    )

    prevQuestionIsAnsweredMap(pageNumber)(annualAVM)
  }

}
