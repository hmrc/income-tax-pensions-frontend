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
import controllers.pensions.routes._
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.pension.reliefs.PaymentsIntoPensionsViewModel
import models.redirects.ConditionalRedirect
import play.api.Logging
import play.api.mvc.Results.Redirect
import play.api.mvc.{Call, Result}
import services.redirects.PaymentsIntoPensionPages
import services.redirects.PaymentsIntoPensionPages._

import scala.concurrent.Future

object RedirectService extends Logging {

  // 1. Current Redirects
  // 2. Next Page Redirects
  def redirectBasedOnRequest[T](optUserData: Option[PensionsUserData], taxYear: Int): Either[Result, PensionsUserData] =
    optUserData match {
      case Some(userData) => Right(userData)
      case None           => Left(Redirect(PensionsSummaryController.show(taxYear))) // No session data atm redirect to pensions summary page
    }

  object PaymentsIntoPensionsRedirects {
    def journeyCheck(currentPage: PaymentsIntoPensionPages, cya: PensionsCYAModel, taxYear: Int): Seq[ConditionalRedirect] = {

      val pIP = cya.paymentsIntoPension

      val startPageCheck = Seq(
        ConditionalRedirect( // TODO: This condition may not be needed
          pIP.rasPensionPaymentQuestion.isEmpty,
          ReliefAtSourcePensionsController.show(taxYear),
          Some(RasPage.journeyNo))
      )

      val taxReliefCheck = Seq(
        ConditionalRedirect(
          pIP.pensionTaxReliefNotClaimedQuestion.isEmpty,
          PensionsTaxReliefNotClaimedController.show(taxYear),
          Some(TaxReliefNotClaimedPage.journeyNo))
      )

      (startPageCheck ++
        rasSection(currentPage, pIP, taxYear) ++
        taxReliefCheck ++
        taxReliefNotClaimedSection(currentPage, pIP, taxYear)).filter(_.journeyNo.exists(_ < currentPage.journeyNo))
    }

    private def rasSection(currentPage: PaymentsIntoPensionPages, pIP: PaymentsIntoPensionsViewModel, taxYear: Int): Seq[ConditionalRedirect] =
      pIP.rasPensionPaymentQuestion match {
        case Some(true) =>
          Seq(
            ConditionalRedirect(
              pIP.totalRASPaymentsAndTaxRelief.isEmpty,
              ReliefAtSourcePaymentsAndTaxReliefAmountController.show(taxYear),
              Some(RasAmountPage.journeyNo)),
            ConditionalRedirect(
              pIP.oneOffRasPaymentPlusTaxReliefQuestion.isEmpty,
              ReliefAtSourceOneOffPaymentsController.show(taxYear),
              Some(OneOffRasPage.journeyNo)),
            ConditionalRedirect(
              pIP.oneOffRasPaymentPlusTaxReliefQuestion.contains(true) && pIP.totalOneOffRasPaymentPlusTaxRelief.isEmpty,
              OneOffRASPaymentsAmountController.show(taxYear),
              Some(OneOffRasAmountPage.journeyNo)
            ),
            ConditionalRedirect(
              !pIP.totalPaymentsIntoRASQuestion.contains(true),
              TotalPaymentsIntoRASController.show(taxYear),
              Some(TotalRasPage.journeyNo))
          )
        case Some(false) =>
          Seq(
            ConditionalRedirect(
              RasAmountPage.journeyNo <= currentPage.journeyNo && currentPage.journeyNo <= TotalRasPage.journeyNo,
              ReliefAtSourcePensionsController.show(taxYear),
              Some(currentPage.journeyNo - 1)
            )
          )
        case _ => Seq()
      }

    private def taxReliefNotClaimedSection(currentPage: PaymentsIntoPensionPages,
                                           pIP: PaymentsIntoPensionsViewModel,
                                           taxYear: Int): Seq[ConditionalRedirect] =
      pIP.pensionTaxReliefNotClaimedQuestion match {
        case Some(true) =>
          Seq(
            ConditionalRedirect(
              pIP.retirementAnnuityContractPaymentsQuestion.isEmpty,
              RetirementAnnuityController.show(taxYear),
              Some(RetirementAnnuityPage.journeyNo)),
            ConditionalRedirect(
              pIP.retirementAnnuityContractPaymentsQuestion.exists(x => x) && pIP.totalRetirementAnnuityContractPayments.isEmpty,
              RetirementAnnuityAmountController.show(taxYear),
              Some(RetirementAnnuityAmountPage.journeyNo)
            ),
            ConditionalRedirect(
              pIP.workplacePensionPaymentsQuestion.isEmpty,
              WorkplacePensionController.show(taxYear),
              Some(WorkplacePensionPage.journeyNo)),
            ConditionalRedirect(
              pIP.workplacePensionPaymentsQuestion.exists(x => x) && pIP.totalWorkplacePensionPayments.isEmpty,
              WorkplaceAmountController.show(taxYear),
              Some(WorkplacePensionAmountPage.journeyNo)
            )
          )
        case Some(false) =>
          Seq(
            ConditionalRedirect(
              RetirementAnnuityPage.journeyNo <= currentPage.journeyNo && currentPage.journeyNo <= WorkplacePensionAmountPage.journeyNo,
              PensionsTaxReliefNotClaimedController.show(taxYear),
              Some(currentPage.journeyNo - 1)
            )
          )
        case _ => Seq()
      }
  }

  def redirectBasedOnCurrentAnswers(taxYear: Int, data: Option[PensionsUserData])(conditions: PensionsCYAModel => Seq[ConditionalRedirect])(
      block: PensionsUserData => Future[Result]): Future[Result] = {
    val redirectOrData = calculateRedirect(taxYear, data, conditions)

    redirectOrData match {
      case Left(redirect) => Future.successful(redirect)
      case Right(cya)     => block(cya)
    }
  }

  private def calculateRedirect(taxYear: Int,
                                data: Option[PensionsUserData],
                                cyaConditions: PensionsCYAModel => Seq[ConditionalRedirect]): Either[Result, PensionsUserData] =
    data match {
      case Some(cya) =>
        val possibleRedirects = cyaConditions(cya.pensions)

        val optRedirect = possibleRedirects.collectFirst {
          case ConditionalRedirect(condition, result, _) if condition => Redirect(result)
        }

        optRedirect match {
          case Some(redirect) =>
            logger.info(
              s"[RedirectService][calculateRedirect]" +
                s" Some data is missing / in the wrong state for the requested page. Routing to ${redirect.header.headers.getOrElse("Location", "")}")
            Left(redirect)
          case None => Right(cya)
        }

      case None =>
        Left(Redirect(controllers.pensions.paymentsIntoPensions.routes.PaymentsIntoPensionsCYAController.show(taxYear)))
    }

  def isFinishedCheck(cya: PensionsCYAModel, taxYear: Int, redirect: Call): Result =
    if (cya.paymentsIntoPension.isFinished) {
      Redirect(controllers.pensions.paymentsIntoPensions.routes.PaymentsIntoPensionsCYAController.show(taxYear))
    } else {
      Redirect(redirect)
    }
}
