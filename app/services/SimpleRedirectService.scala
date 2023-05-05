
package services

import controllers.pensions.paymentsIntoPensions.routes._
import models.mongo.{PensionsCYAModel, PensionsUserData}
import play.api.Logging
import play.api.mvc.{Call, Result}
import play.api.mvc.Results.Redirect
import utils.PaymentsIntoPensionPages

import scala.concurrent.Future

object SimpleRedirectService extends Logging {

  def redirectBasedOnCurrentAnswers(taxYear: Int, data: Option[PensionsUserData])
                                   (shouldRedirect: PensionsCYAModel => Either[Result, Unit])
                                   (block: PensionsUserData => Future[Result]): Future[Result] = {

    val redirectOrData = data match {
      case Some(cya) =>
        shouldRedirect(cya.pensions) match {
          case Right(_) => Right(cya)
          case Left(redirect) =>
            logger.info(s"[RedirectService][calculateRedirect]" +
              s" Some data is missing / in the wrong state for the requested page. Routing to ${
                redirect.header.headers.getOrElse("Location", "")
              }")
            Left(redirect)
        }
      case None =>
        Left(Redirect(controllers.pensions.paymentsIntoPensions.routes.PaymentsIntoPensionsCYAController.show(taxYear)))
    }
    redirectOrData match {
      case Left(redirect) => Future.successful(redirect)
      case Right(cya) => block(cya)
    }
  }


  def isFinishedCheck(cya: PensionsCYAModel, taxYear: Int, redirect: Call): Result = {
    if (cya.paymentsIntoPension.isFinished) {
      Redirect(controllers.pensions.paymentsIntoPensions.routes.PaymentsIntoPensionsCYAController.show(taxYear))
    } else {
      Redirect(redirect)
    }
  }

  object PaymentsIntoPensionsRedirects {
    def journeyCheck(currentPage: PaymentsIntoPensionPages, cya: PensionsCYAModel, taxYear: Int): Either[Result, Unit] = {

      val pIP = cya.paymentsIntoPension

      def isPageValidInJourney(pageNumber: Int): Boolean = {
        // 1 skips to 6
        // 3 skips to 5
        // 6 skips to 11
        // 7 skips to 9
        // 9 skips to 11
        System.out.println(pageNumber)
        pageNumber match {
          case 2 => pIP.rasPensionPaymentQuestion.getOrElse(false)
          case 3 => pIP.rasPensionPaymentQuestion.getOrElse(false)
          case 5 => pIP.rasPensionPaymentQuestion.getOrElse(false)
          // ^ checks Q1 ^
          case 4 =>
            pIP.oneOffRasPaymentPlusTaxReliefQuestion.getOrElse(false) &&
              pIP.rasPensionPaymentQuestion.getOrElse(false)
          // ^ checks Q1 Q3
          case 7 => pIP.pensionTaxReliefNotClaimedQuestion.getOrElse(false)
          case 9 => pIP.pensionTaxReliefNotClaimedQuestion.getOrElse(false)
          // ^ checks Q6 ^
          case 8 =>
            pIP.pensionTaxReliefNotClaimedQuestion.getOrElse(false) &&
              pIP.retirementAnnuityContractPaymentsQuestion.getOrElse(false)
          // ^ checks Q6 Q7 ^
          case 10 => pIP.workplacePensionPaymentsQuestion.getOrElse(false)
          // ^ checks Q10 ^
          case _ => true
        }
      }

      def previousQuestionIsUnanswered(pageNumber: Int): Boolean = {
        // check if previous Q answered ONLY IF prev Q shouldn't be skipped
        // todo is this properly checking the prev Q is answered or just if we need to check the prev page?
        pageNumber match {
          case 1 => false
          case 2 => pIP.rasPensionPaymentQuestion.isEmpty
          case 3 => pIP.totalRASPaymentsAndTaxRelief.isEmpty
          case 4 => pIP.oneOffRasPaymentPlusTaxReliefQuestion.isEmpty
          case 5 =>
            if (isPageValidInJourney(4)) pIP.totalOneOffRasPaymentPlusTaxRelief.isEmpty
            else false
          case 6 =>
            if (isPageValidInJourney(5)) pIP.totalPaymentsIntoRASQuestion.isEmpty
            else false
          case 7 => pIP.pensionTaxReliefNotClaimedQuestion.isEmpty
          case 8 => pIP.retirementAnnuityContractPaymentsQuestion.isEmpty
          case 9 =>
            if (isPageValidInJourney(8)) pIP.totalRetirementAnnuityContractPayments.isEmpty
            else false
          case 10 => pIP.workplacePensionPaymentsQuestion.isEmpty
          case 11 =>
            if (isPageValidInJourney(10)) pIP.totalWorkplacePensionPayments.isEmpty
            else false
        }
      }

      if (!isPageValidInJourney(currentPage.journeyNo) || previousQuestionIsUnanswered(currentPage.journeyNo))
        Left(Redirect(ReliefAtSourcePensionsController.show(taxYear)))
      else Right()
    }
  }


}
