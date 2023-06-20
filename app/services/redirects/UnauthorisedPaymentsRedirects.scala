
package services.redirects

import controllers.pensions.unauthorisedPayments.routes.UnauthorisedPaymentsController
import models.mongo.PensionsCYAModel
import models.pension.charges.UnauthorisedPaymentsViewModel
import play.api.mvc.Result
import play.api.mvc.Results.Redirect

object UnauthorisedPaymentsRedirects { //scalastyle:off magic.number

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
    val ukPensionSchemesQuestionFn = { unauthorisedPaymentsViewModel: UnauthorisedPaymentsViewModel =>
      (unauthorisedPaymentsViewModel.surchargeQuestion.getOrElse(false) || unauthorisedPaymentsViewModel.noSurchargeQuestion.getOrElse(false)) && unauthorisedPaymentsViewModel.ukPensionSchemesQuestion.getOrElse(false)
    }

    Map(
      // ^ 2-8 need Q1 true ^
      // ^ 2,3 need Q1 true + surcharge ^
      2 -> surchargeQuestionFn, 3 -> surchargeQuestionFn,
      // ^ 4,5 need Q1 true + no surcharge ^
      4 -> noSurchargeQuestionFn, 5 -> noSurchargeQuestionFn,
      // ^ 7,8 need Q6 true ^
      7 -> ukPensionSchemesQuestionFn,
      8 -> ukPensionSchemesQuestionFn
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

    6 -> { unauthorisedPaymentsViewModel: UnauthorisedPaymentsViewModel => unauthorisedPaymentsViewModel.noSurchargeTaxAmountQuestion.isDefined },

    7 -> { unauthorisedPaymentsViewModel: UnauthorisedPaymentsViewModel => unauthorisedPaymentsViewModel.ukPensionSchemesQuestion.isDefined },
    8 -> { unauthorisedPaymentsViewModel: UnauthorisedPaymentsViewModel => unauthorisedPaymentsViewModel.pensionSchemeTaxReference.nonEmpty },

    9 -> { unauthorisedPaymentsViewModel: UnauthorisedPaymentsViewModel =>
      if (isPageValidInJourney(8, unauthorisedPaymentsViewModel)) unauthorisedPaymentsViewModel.pensionSchemeTaxReference.nonEmpty
      else if (isPageValidInJourney(6, unauthorisedPaymentsViewModel)) unauthorisedPaymentsViewModel.ukPensionSchemesQuestion.isDefined
      else {
        !unauthorisedPaymentsViewModel.surchargeQuestion.getOrElse(false) && !unauthorisedPaymentsViewModel.noSurchargeQuestion.getOrElse(false)
      }
    }
  )
}
