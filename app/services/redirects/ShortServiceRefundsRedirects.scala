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

import controllers.pensions.routes.OverseasPensionsSummaryController
import controllers.pensions.shortServiceRefunds.routes._
import models.pension.charges.ShortServiceRefundsViewModel
import play.api.mvc.Results.Redirect
import play.api.mvc.{Call, Result}
import services.redirects.ShortServiceRefundsPages.RemoveRefundSchemePage
import validation.pensions.shortServiceRefunds.IndexValidator

import scala.concurrent.Future

object ShortServiceRefundsRedirects {

  def firstPageRedirect(taxYear: Int): Result     = Redirect(TaxableRefundAmountController.show(taxYear))
  def taskListRedirect(taxYear: Int): Result      = Redirect(OverseasPensionsSummaryController.show(taxYear))
  def refundSummaryRedirect(taxYear: Int): Result = Redirect(refundSummaryCall(taxYear))

  def refundSummaryCall(taxYear: Int): Call = RefundSummaryController.show(taxYear)
  def cyaPageCall(taxYear: Int): Call       = ShortServiceRefundsCYAController.show(taxYear)

  def validateIndex[A <: ShortServiceRefundsPages: IndexValidator](index: Option[Int], answers: ShortServiceRefundsViewModel, taxYear: Int)(
      blockIfValid: Int => Future[Result]): Future[Result] = {
    val validator = implicitly[IndexValidator[A]]
    validator.validate(index, answers.refundPensionScheme, taxYear) match {
      case Right(index)   => blockIfValid(index)
      case Left(redirect) => Future.successful(Redirect(redirect.location))
    }
  }

  def validateFlow(page: ShortServiceRefundsPages, answers: ShortServiceRefundsViewModel, taxYear: Int, index: Option[Int] = None)(
      blockIfValid: => Future[Result]): Future[Result] =
    if (page.isValidInCurrentState(answers, index)) blockIfValid
    else {
      val redirectLocation = page match {
        case RemoveRefundSchemePage() => refundSummaryRedirect(taxYear)
        case _                        => firstPageRedirect(taxYear)
      }
      Future.successful(redirectLocation)
    }
}
