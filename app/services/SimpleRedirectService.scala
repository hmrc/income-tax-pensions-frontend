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

package services

import controllers.pensions.paymentsIntoPensions.routes._
import models.mongo.{PensionsCYAModel, PensionsUserData}
import play.api.Logging
import play.api.mvc.Results.Redirect
import play.api.mvc.{Call, Result}
import utils.PaymentsIntoPensionPages

import scala.concurrent.Future

object SimpleRedirectService extends Logging {

  def redirectBasedOnCurrentAnswers(taxYear: Int, data: Option[PensionsUserData])
                                   (shouldRedirect: PensionsCYAModel => Either[Result, Unit])
                                   (block: PensionsUserData => Future[Result]): Future[Result] = {

    val redirectOrData = data match {
      case Some(cya) =>
        shouldRedirect(cya.pensions) match {
          case Right(_) =>
            Right(cya)
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
        pageNumber match {
          case 2 => pIP.rasPensionPaymentQuestion.getOrElse(false)
          case 3 => pIP.rasPensionPaymentQuestion.getOrElse(false)
          case 5 => pIP.rasPensionPaymentQuestion.getOrElse(false)
          // ^ 2,3,5 need Q1 ^
          case 4 =>
            pIP.oneOffRasPaymentPlusTaxReliefQuestion.getOrElse(false) &&
              pIP.rasPensionPaymentQuestion.getOrElse(false)
          // ^ 4 needs Q1+3
          case 7 => pIP.pensionTaxReliefNotClaimedQuestion.getOrElse(false)
          case 9 => pIP.pensionTaxReliefNotClaimedQuestion.getOrElse(false)
          // ^ 7,9 need Q6 ^
          case 8 =>
            pIP.pensionTaxReliefNotClaimedQuestion.getOrElse(false) &&
              pIP.retirementAnnuityContractPaymentsQuestion.getOrElse(false)
          // ^ 8 needs Q6+7 ^
          case 10 => pIP.workplacePensionPaymentsQuestion.getOrElse(false)
          // ^ 10 needs Q9 ^
          case _ => true
        }
      }

      def previousQuestionIsAnswered(pageNumber: Int): Boolean = {
        // check if previous Q answered if not skipped, else check relevant Q
        pageNumber match {
          case 1 => true
          case 2 => pIP.rasPensionPaymentQuestion.isDefined
          case 3 => pIP.totalRASPaymentsAndTaxRelief.isDefined
          case 4 => pIP.oneOffRasPaymentPlusTaxReliefQuestion.isDefined
          case 5 =>
            if (isPageValidInJourney(4)) pIP.totalOneOffRasPaymentPlusTaxRelief.isDefined
            else pIP.oneOffRasPaymentPlusTaxReliefQuestion.isDefined
          case 6 =>
            if (isPageValidInJourney(5)) pIP.totalPaymentsIntoRASQuestion.isDefined
            else pIP.rasPensionPaymentQuestion.isDefined
          case 7 => pIP.pensionTaxReliefNotClaimedQuestion.isDefined
          case 8 => pIP.retirementAnnuityContractPaymentsQuestion.isDefined
          case 9 =>
            if (isPageValidInJourney(8)) pIP.totalRetirementAnnuityContractPayments.isDefined
            else pIP.retirementAnnuityContractPaymentsQuestion.isDefined
          case 10 => pIP.workplacePensionPaymentsQuestion.isDefined
          case 11 =>
            if (isPageValidInJourney(10)) pIP.totalWorkplacePensionPayments.isDefined
            else if (isPageValidInJourney(7)) pIP.workplacePensionPaymentsQuestion.isDefined
            else pIP.pensionTaxReliefNotClaimedQuestion.isDefined
        }
      }

      if (isPageValidInJourney(currentPage.journeyNo) && previousQuestionIsAnswered(currentPage.journeyNo)) {
        Right()
      } else {
        Left(Redirect(ReliefAtSourcePensionsController.show(taxYear)))
      }
    }
  }

}
