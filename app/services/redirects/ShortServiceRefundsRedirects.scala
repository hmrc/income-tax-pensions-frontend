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
import play.api.mvc.Results.Redirect
import play.api.mvc.{Call, Result}

object ShortServiceRefundsRedirects {

  def firstPageRedirect(taxYear: Int): Result                             = Redirect(TaxableRefundAmountController.show(taxYear))
  def taskListRedirect(taxYear: Int): Result                              = Redirect(OverseasPensionsSummaryController.show(taxYear))
  def refundSummaryRedirect(taxYear: Int): Result                         = Redirect(refundSummaryCall(taxYear))
  def cyaPageRedirect(taxYear: Int): Result                               = Redirect(cyaPageCall(taxYear))
  def refundSchemeRedirect(taxYear: Int, maybeIndex: Option[Int]): Result = Redirect(ShortServicePensionsSchemeController.show(taxYear, maybeIndex))
  def nonUkTaxRefundsRedirect(taxYear: Int): Result                       = Redirect(NonUkTaxRefundsController.show(taxYear))

  def refundSummaryCall(taxYear: Int): Call = RefundSummaryController.show(taxYear)
  def cyaPageCall(taxYear: Int): Call       = ShortServiceRefundsCYAController.show(taxYear)
}
