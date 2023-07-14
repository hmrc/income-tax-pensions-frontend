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
import services.redirects.SimpleRedirectService.checkForExistingSchemes

object AnnualAllowancesRedirects {

  def cyaPageCall(taxYear: Int): Call = AnnualAllowanceCYAController.show(taxYear)

  def redirectForSchemeLoop(schemes: Seq[String], taxYear: Int): Call = {
    checkForExistingSchemes(
      nextPage = PensionSchemeTaxReferenceController.show(taxYear, None),
      summaryPage = PstrSummaryController.show(taxYear),
      schemes = schemes
    )
  }

  def journeyCheck(currentPage: AnnualAllowancesPages, cya: PensionsCYAModel, taxYear: Int, index: Option[Int] = None): Option[Result] = {
    val annualAllowanceData = cya.pensionsAnnualAllowances
    if (isPageValidInJourney(currentPage.journeyNo, annualAllowanceData) && previousQuestionIsAnswered(currentPage.journeyNo, index, annualAllowanceData)) {
      None
    } else {
      Some(Redirect(ReducedAnnualAllowanceController.show(taxYear)))
    }
  }

  private def isPageValidInJourney(pageNumber: Int, annualAVM: PensionAnnualAllowancesViewModel): Boolean =
    pageValidInJourneyMap.getOrElse(pageNumber, { _: PensionAnnualAllowancesViewModel => true })(annualAVM)

  private val pageValidInJourneyMap: Map[Int, PensionAnnualAllowancesViewModel => Boolean] = {
    val isTrue = { _: PensionAnnualAllowancesViewModel => true }
    val firstQuestionTrue = { annualAVM: PensionAnnualAllowancesViewModel => annualAVM.reducedAnnualAllowanceQuestion.getOrElse(false) }
    val thirdQuestionTrue = { annualAVM: PensionAnnualAllowancesViewModel => annualAVM.aboveAnnualAllowanceQuestion.getOrElse(false) }

    Map(
      // 1 and 8 are always valid
      1 -> isTrue, 8 -> isTrue,
      // 2-7 need Q1 true
      2 -> firstQuestionTrue, 3 -> firstQuestionTrue,
      // 4-7 need Q3 true
      4 -> thirdQuestionTrue, 5 -> thirdQuestionTrue, 6 -> thirdQuestionTrue, 7 -> thirdQuestionTrue
    )
  }

  private def previousQuestionIsAnswered(pageNumber: Int, optIndex: Option[Int], annualAVM: PensionAnnualAllowancesViewModel): Boolean = {
    val schemesEmpty = !annualAVM.pensionSchemeTaxReferences.exists(_.nonEmpty)
    val index = optIndex.filter(i => i >= 0 && i < annualAVM.pensionSchemeTaxReferences.size)
    // 1. empty and None -> 1st time/new // 2. empty and Some() -> FALSE // 3. full and None -> FALSE/new // 4. full and Some() -> revisit
    val pstrPageCheck: Boolean = {
      (schemesEmpty, index) match {
        case (true, None) => true
        case (false, Some(_)) => true
        case (false, None) if optIndex.isEmpty => true
        case _ => false
      }
    }

    val prevQuestionIsAnsweredMap: Map[Int, PensionAnnualAllowancesViewModel => Boolean] = Map(
      1 -> { _: PensionAnnualAllowancesViewModel => true },

      2 -> { annualAVM: PensionAnnualAllowancesViewModel => annualAVM.reducedAnnualAllowanceQuestion.getOrElse(false) },
      3 -> { annualAVM: PensionAnnualAllowancesViewModel =>
        annualAVM.moneyPurchaseAnnualAllowance.getOrElse(false) || annualAVM.taperedAnnualAllowance.getOrElse(false)
      },
      4 -> { annualAVM: PensionAnnualAllowancesViewModel =>
        annualAVM.aboveAnnualAllowanceQuestion.exists(x => if (x) annualAVM.aboveAnnualAllowance.isDefined else true)
      },

      5 -> { annualAVM: PensionAnnualAllowancesViewModel =>
        annualAVM.pensionProvidePaidAnnualAllowanceQuestion.exists(x => if (x) annualAVM.taxPaidByPensionProvider.isDefined else true)
      },
      6 -> { _: PensionAnnualAllowancesViewModel => pstrPageCheck },
      7 -> { annualAVM: PensionAnnualAllowancesViewModel =>
        if (schemesEmpty || index.isEmpty) false else annualAVM.pensionSchemeTaxReferences.exists(x => x(index.get).nonEmpty)
      },

      8 -> { annualAVM: PensionAnnualAllowancesViewModel =>
        if (!isPageValidInJourney(2, annualAVM)) !annualAVM.reducedAnnualAllowanceQuestion.getOrElse(true)
        else if (!isPageValidInJourney(4, annualAVM)) !annualAVM.aboveAnnualAllowanceQuestion.getOrElse(true)
        else annualAVM.isFinished
      }
    )

    prevQuestionIsAnsweredMap(pageNumber)(annualAVM)
  }

}
