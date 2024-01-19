/*
 * Copyright 2024 HM Revenue & Customs
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

import controllers.pensions.paymentsIntoPensions.routes.{PaymentsIntoPensionsCYAController, ReliefAtSourcePensionsController}
import models.mongo.PensionsCYAModel
import models.pension.reliefs.PaymentsIntoPensionsViewModel
import play.api.mvc.Results.Redirect
import play.api.mvc.{Call, Result}


object PaymentsIntoPensionsRedirects { //scalastyle:off magic.number

  def cyaPageCall(taxYear: Int): Call = PaymentsIntoPensionsCYAController.show(taxYear)

  def journeyCheck(currentPage: PaymentsIntoPensionPages, cya: PensionsCYAModel, taxYear: Int): Option[Result] = {
    val pIP = cya.paymentsIntoPension
    if (isPageValidInJourney(currentPage.journeyNo, pIP) && previousQuestionIsAnswered(currentPage.journeyNo, pIP)) {
      None
    } else {
      Some(Redirect(ReliefAtSourcePensionsController.show(taxYear)))
    }
  }

  private val pageValidInJourneyMap: Map[Int, PaymentsIntoPensionsViewModel => Boolean] = {

    val rasPaymentQuestionFn = { pIPViewModel: PaymentsIntoPensionsViewModel => pIPViewModel.rasPensionPaymentQuestion.getOrElse(false) }
    val taxReliefNotClaimedQuestionFn = { pIPViewModel: PaymentsIntoPensionsViewModel => pIPViewModel.pensionTaxReliefNotClaimedQuestion.getOrElse(false) }

    Map(
      // ^ 2,3,5 need Q1 ^
      2 -> rasPaymentQuestionFn, 3 -> rasPaymentQuestionFn, 5 -> rasPaymentQuestionFn,
      4 -> { pIPViewModel: PaymentsIntoPensionsViewModel =>
        pIPViewModel.oneOffRasPaymentPlusTaxReliefQuestion.getOrElse(false) && // ^ 4 needs Q1+3
          pIPViewModel.rasPensionPaymentQuestion.getOrElse(false)
      },
      // ^ 7,9 need Q6 ^
      7 -> taxReliefNotClaimedQuestionFn, 9 -> taxReliefNotClaimedQuestionFn,
      8 -> { pIPViewModel: PaymentsIntoPensionsViewModel =>
        pIPViewModel.pensionTaxReliefNotClaimedQuestion.getOrElse(false) &&
          pIPViewModel.retirementAnnuityContractPaymentsQuestion.getOrElse(false)
      },
      10 -> { pIPViewModel: PaymentsIntoPensionsViewModel => pIPViewModel.workplacePensionPaymentsQuestion.getOrElse(false) }
    )
  }

  private val prevQuestionIsAnsweredMap: Map[Int, PaymentsIntoPensionsViewModel => Boolean] = Map(
    1 -> { _: PaymentsIntoPensionsViewModel => true },
    2 -> { pIPViewModel: PaymentsIntoPensionsViewModel => pIPViewModel.rasPensionPaymentQuestion.nonEmpty },
    3 -> { pIPViewModel: PaymentsIntoPensionsViewModel => pIPViewModel.totalRASPaymentsAndTaxRelief.nonEmpty },
    4 -> { pIPViewModel: PaymentsIntoPensionsViewModel => pIPViewModel.oneOffRasPaymentPlusTaxReliefQuestion.nonEmpty },

    5 -> { pIPViewModel: PaymentsIntoPensionsViewModel =>
      if (isPageValidInJourney(4, pIPViewModel)) {pIPViewModel.totalOneOffRasPaymentPlusTaxRelief.nonEmpty}
      else {pIPViewModel.oneOffRasPaymentPlusTaxReliefQuestion.nonEmpty}
    },

    6 -> { pIPViewModel: PaymentsIntoPensionsViewModel =>
      if (isPageValidInJourney(5, pIPViewModel)) {pIPViewModel.totalPaymentsIntoRASQuestion.nonEmpty}
      else {pIPViewModel.rasPensionPaymentQuestion.isDefined}
    },

    7 -> { pIPViewModel: PaymentsIntoPensionsViewModel => pIPViewModel.pensionTaxReliefNotClaimedQuestion.isDefined },
    8 -> { pIPViewModel: PaymentsIntoPensionsViewModel => pIPViewModel.retirementAnnuityContractPaymentsQuestion.isDefined },

    9 -> { pIPViewModel: PaymentsIntoPensionsViewModel =>
      if (isPageValidInJourney(8, pIPViewModel)) {pIPViewModel.totalRetirementAnnuityContractPayments.isDefined}
      else {pIPViewModel.retirementAnnuityContractPaymentsQuestion.isDefined}
    },

    10 -> { pIPViewModel: PaymentsIntoPensionsViewModel => pIPViewModel.workplacePensionPaymentsQuestion.isDefined },

    11 -> { pIPViewModel: PaymentsIntoPensionsViewModel =>
      if (isPageValidInJourney(10, pIPViewModel)) {pIPViewModel.totalWorkplacePensionPayments.isDefined}
      else if (isPageValidInJourney(7, pIPViewModel)) {pIPViewModel.workplacePensionPaymentsQuestion.isDefined}
      else {pIPViewModel.pensionTaxReliefNotClaimedQuestion.isDefined}
    }
  )

  private def isPageValidInJourney(pageNumber: Int, pIPViewModel: PaymentsIntoPensionsViewModel): Boolean =
    pageValidInJourneyMap.getOrElse(pageNumber, { _: PaymentsIntoPensionsViewModel => true })(pIPViewModel)

  private def previousQuestionIsAnswered(pageNumber: Int, pIPViewModel: PaymentsIntoPensionsViewModel): Boolean =
    prevQuestionIsAnsweredMap(pageNumber)(pIPViewModel)
}
