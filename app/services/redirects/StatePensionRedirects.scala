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

import controllers.pensions.incomeFromPensions.routes._
import play.api.mvc.Results.Redirect
import play.api.mvc.{Call, Result}

object StatePensionRedirects {

  def firstPageRedirect(taxYear: Int): Result             = Redirect(StatePensionController.show(taxYear))
  def claimLumpSumRedirect(taxYear: Int): Result          = Redirect(StatePensionLumpSumController.show(taxYear))
  def taxPaidOnLumpSumRedirect(taxYear: Int): Result      = Redirect(TaxPaidOnStatePensionLumpSumController.show(taxYear))
  def statePensionStartDateRedirect(taxYear: Int): Result = Redirect(StatePensionStartDateController.show(taxYear))
  def lumpSumStartDateRedirect(taxYear: Int): Result      = Redirect(StatePensionLumpSumStartDateController.show(taxYear))
  def cyaPageRedirect(taxYear: Int): Result               = Redirect(cyaPageCall(taxYear))
  def taskListRedirect(taxYear: Int): Result              = Redirect(IncomeFromPensionsSummaryController.show(taxYear))

  def cyaPageCall(taxYear: Int): Call = StatePensionCYAController.show(taxYear)
}
