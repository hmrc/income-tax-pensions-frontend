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

package models.pension

import controllers.pensions
import enumeratum._
import play.api.mvc.Results.Redirect
import play.api.mvc.{PathBindable, Result}

sealed abstract class Journey(override val entryName: String) extends EnumEntry {
  override def toString: String = entryName
  def sectionCompletedRedirect(taxYear: Int): Result
}

object Journey extends Enum[Journey] with utils.PlayJsonEnum[Journey] {
  val values: IndexedSeq[Journey] = findValues

  private val pensionsSummaryRedirect = (taxYear: Int) => Redirect(pensions.routes.PensionsSummaryController.show(taxYear))
  private val overseasSummaryRedirect = (taxYear: Int) => Redirect(pensions.routes.OverseasPensionsSummaryController.show(taxYear))
  private val incomeFromPensionsSummaryRedirect = (taxYear: Int) =>
    Redirect(pensions.incomeFromPensions.routes.IncomeFromPensionsSummaryController.show(taxYear))

  case object AnnualAllowances extends Journey("annual-allowances") {
    def sectionCompletedRedirect(taxYear: Int): Result = pensionsSummaryRedirect(taxYear)
  }
  case object PaymentsIntoPensions extends Journey("payments-into-pensions") {
    def sectionCompletedRedirect(taxYear: Int): Result = pensionsSummaryRedirect(taxYear)
  }
  case object UnauthorisedPayments extends Journey("unauthorised-payments") {
    def sectionCompletedRedirect(taxYear: Int): Result = pensionsSummaryRedirect(taxYear)
  }

  case object OverseasPensionsSummary extends Journey("overseas-pensions-summary") {
    def sectionCompletedRedirect(taxYear: Int): Result = overseasSummaryRedirect(taxYear)
  }
  case object IncomeFromOverseasPensions extends Journey("income-from-overseas-pensions") {
    def sectionCompletedRedirect(taxYear: Int): Result = overseasSummaryRedirect(taxYear)
  }
  case object PaymentsIntoOverseasPensions extends Journey("payments-into-overseas-pensions") {
    def sectionCompletedRedirect(taxYear: Int): Result = overseasSummaryRedirect(taxYear)
  }
  case object TransferIntoOverseasPensions extends Journey("transfer-into-overseas-pensions") {
    def sectionCompletedRedirect(taxYear: Int): Result = overseasSummaryRedirect(taxYear)
  }
  case object ShortServiceRefunds extends Journey("short-service-refunds") {
    def sectionCompletedRedirect(taxYear: Int): Result = overseasSummaryRedirect(taxYear)
  }

  case object IncomeFromPensionsSummary extends Journey("income-from-pensions-summary") {
    def sectionCompletedRedirect(taxYear: Int): Result = pensionsSummaryRedirect(taxYear)
  }
  case object UkPensionIncome extends Journey("uk-pension-income") {
    def sectionCompletedRedirect(taxYear: Int): Result = incomeFromPensionsSummaryRedirect(taxYear)
  }
  case object StatePension extends Journey("state-pension") {
    def sectionCompletedRedirect(taxYear: Int): Result = incomeFromPensionsSummaryRedirect(taxYear)
  }

  implicit def pathBindable(implicit strBinder: PathBindable[String]): PathBindable[Journey] = new PathBindable[Journey] {

    override def bind(key: String, value: String): Either[String, Journey] =
      strBinder.bind(key, value).flatMap { stringValue =>
        Journey.withNameOption(stringValue) match {
          case Some(journeyName) => Right(journeyName)
          case None              => Left(s"Invalid journey name: $stringValue")
        }
      }

    override def unbind(key: String, journeyName: Journey): String =
      strBinder.unbind(key, journeyName.entryName)
  }
}
