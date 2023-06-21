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

import controllers.pensions.unauthorisedPayments.routes.UnauthorisedPaymentsController
import models.mongo.PensionsCYAModel
import models.pension.charges.UnauthorisedPaymentsViewModel
import play.api.mvc.{Call, Result}
import play.api.mvc.Results.Redirect

object UnauthorisedPaymentsRedirects { //scalastyle:off magic.number

  def cyaPageCall(taxYear: Int): Call = controllers.pensions.unauthorisedPayments.routes.UnauthorisedPaymentsCYAController.show(taxYear)

  def isFinishedCheck(cya: PensionsCYAModel, taxYear: Int, redirect: Call): Result = {
    if (cya.unauthorisedPayments.isFinished) {
      Redirect(cyaPageCall(taxYear))
    } else {
      Redirect(redirect)
    }
  }

  def journeyCheck(currentPage: UnauthorisedPaymentsPages, cya: PensionsCYAModel, taxYear: Int): Option[Result] = {
    val unauthorisedPayments = cya.unauthorisedPayments
    if (isPageValidInJourney(currentPage.journeyNo, unauthorisedPayments) && previousQuestionIsAnswered(currentPage.journeyNo, unauthorisedPayments)) {
      None
    } else {
      Some(Redirect(UnauthorisedPaymentsController.show(taxYear)))
    }
  }

  private def isPageValidInJourney(pageNumber: Int, unauthorisedPaymentsViewModel: UnauthorisedPaymentsViewModel): Boolean =
    pageValidInJourneyMap.getOrElse(pageNumber, { _: UnauthorisedPaymentsViewModel => true })(unauthorisedPaymentsViewModel)

  private val pageValidInJourneyMap: Map[Int, UnauthorisedPaymentsViewModel => Boolean] = {

    val surchargeQuestionFn = { unauthorisedPaymentsViewModel: UnauthorisedPaymentsViewModel => unauthorisedPaymentsViewModel.surchargeQuestion.getOrElse(false) }
    val noSurchargeQuestionFn = { unauthorisedPaymentsViewModel: UnauthorisedPaymentsViewModel => unauthorisedPaymentsViewModel.noSurchargeQuestion.getOrElse(false) }
    val surchargeOrNoSurchargeQuestionFn = { unauthorisedPaymentsViewModel: UnauthorisedPaymentsViewModel =>
      unauthorisedPaymentsViewModel.surchargeQuestion.getOrElse(false) || unauthorisedPaymentsViewModel.noSurchargeQuestion.getOrElse(false)
    }
    val ukPensionSchemesQuestionFn = { unauthorisedPaymentsViewModel: UnauthorisedPaymentsViewModel =>
      (unauthorisedPaymentsViewModel.surchargeQuestion.getOrElse(false) || unauthorisedPaymentsViewModel.noSurchargeQuestion.getOrElse(false)) && unauthorisedPaymentsViewModel.ukPensionSchemesQuestion.getOrElse(false)
    }

    Map(
      // 2-9 need Q1 true
      // 2,3 need Q1 true + surcharge
      2 -> surchargeQuestionFn, 3 -> surchargeQuestionFn,
      // 4,5 need Q1 true + no surcharge
      4 -> noSurchargeQuestionFn, 5 -> noSurchargeQuestionFn,
      // 6 needs Q1 or Q2 true
      6 -> surchargeOrNoSurchargeQuestionFn,
      // 7,8,9 need Q6 true
      7 -> ukPensionSchemesQuestionFn,
      8 -> ukPensionSchemesQuestionFn,
      9 -> ukPensionSchemesQuestionFn
    )
  }

  private def previousQuestionIsAnswered(pageNumber: Int, unauthorisedPaymentsViewModel: UnauthorisedPaymentsViewModel): Boolean =
    prevQuestionIsAnsweredMap(pageNumber)(unauthorisedPaymentsViewModel)

  private val prevQuestionIsAnsweredMap: Map[Int, UnauthorisedPaymentsViewModel => Boolean] = Map(
    1 -> { _: UnauthorisedPaymentsViewModel => true },

    2 -> { unauthorisedPaymentsViewModel: UnauthorisedPaymentsViewModel => unauthorisedPaymentsViewModel.surchargeQuestion.isDefined },
    3 -> { unauthorisedPaymentsViewModel: UnauthorisedPaymentsViewModel => unauthorisedPaymentsViewModel.surchargeAmount.isDefined },

    4 -> { unauthorisedPaymentsViewModel: UnauthorisedPaymentsViewModel =>
      if (isPageValidInJourney(3, unauthorisedPaymentsViewModel)) unauthorisedPaymentsViewModel.surchargeTaxAmountQuestion.isDefined
      else unauthorisedPaymentsViewModel.noSurchargeQuestion.isDefined
    },
    5 -> { unauthorisedPaymentsViewModel: UnauthorisedPaymentsViewModel => unauthorisedPaymentsViewModel.noSurchargeAmount.isDefined },

    6 -> { unauthorisedPaymentsViewModel: UnauthorisedPaymentsViewModel =>
      if (isPageValidInJourney(5, unauthorisedPaymentsViewModel)) unauthorisedPaymentsViewModel.noSurchargeTaxAmountQuestion.isDefined
      else unauthorisedPaymentsViewModel.surchargeTaxAmountQuestion.isDefined
    },

    7 -> { unauthorisedPaymentsViewModel: UnauthorisedPaymentsViewModel => unauthorisedPaymentsViewModel.ukPensionSchemesQuestion.isDefined },
    8 -> { unauthorisedPaymentsViewModel: UnauthorisedPaymentsViewModel => unauthorisedPaymentsViewModel.ukPensionSchemesQuestion.isDefined },
    9 -> { unauthorisedPaymentsViewModel: UnauthorisedPaymentsViewModel => unauthorisedPaymentsViewModel.pensionSchemeTaxReference.nonEmpty },

    10 -> { unauthorisedPaymentsViewModel: UnauthorisedPaymentsViewModel =>
      if (isPageValidInJourney(7, unauthorisedPaymentsViewModel)) {
        unauthorisedPaymentsViewModel.pensionSchemeTaxReference.nonEmpty
      } else if (isPageValidInJourney(6, unauthorisedPaymentsViewModel)) {
        !unauthorisedPaymentsViewModel.ukPensionSchemesQuestion.getOrElse(true)
      } else {
        !unauthorisedPaymentsViewModel.surchargeQuestion.getOrElse(true) && !unauthorisedPaymentsViewModel.noSurchargeQuestion.getOrElse(true)
      }
    }
  )
}
