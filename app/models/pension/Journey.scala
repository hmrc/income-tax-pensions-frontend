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

import models.redirects.AppLocations.{HOME, INCOME_FROM_PENSIONS_HOME, OVERSEAS_HOME}
import play.api.libs.json._
import play.api.mvc.Results.Redirect
import play.api.mvc.{PathBindable, Result}

sealed abstract class Journey(name: String) {
  override def toString: String                      = name
  def sectionCompletedRedirect(taxYear: Int): Result = ???
}

object Journey {
  val values: Seq[Journey] = Seq(
    PensionsSummary,
    AnnualAllowances,
    PaymentsIntoPensions,
    UnauthorisedPayments,
    OverseasPensionsSummary,
    IncomeFromOverseasPensions,
    PaymentsIntoOverseasPensions,
    TransferIntoOverseasPensions,
    ShortServiceRefunds,
    IncomeFromPensionsSummary,
    UkPensionIncome,
    StatePension
  )

  private def withName(journey: String): Either[String, Journey] = {
    val namesToValuesMap = values.map(v => v.toString -> v).toMap
    namesToValuesMap.get(journey) match {
      case Some(journeyName) => Right(journeyName)
      case None              => Left(s"Invalid journey name: $journey")
    }
  }

  implicit val format: Format[Journey] = new Format[Journey] {
    override def writes(journey: Journey): JsValue = JsString(journey.toString)

    override def reads(json: JsValue): JsResult[Journey] = json match {
      case JsString(name) =>
        withName(name) match {
          case Right(journey) => JsSuccess(journey)
          case Left(error)    => JsError(error)
        }
      case _ => JsError("String value expected")
    }
  }

  implicit def pathBindable(implicit strBinder: PathBindable[String]): PathBindable[Journey] = new PathBindable[Journey] {

    override def bind(key: String, value: String): Either[String, Journey] =
      strBinder.bind(key, value).flatMap(withName)

    override def unbind(key: String, journeyName: Journey): String =
      strBinder.unbind(key, journeyName.toString)
  }

  case object PensionsSummary extends Journey("pensions-summary")

  case object AnnualAllowances extends Journey("annual-allowances") {
    override def sectionCompletedRedirect(taxYear: Int): Result = Redirect(HOME(taxYear))
  }
  case object PaymentsIntoPensions extends Journey("payments-into-pensions") {
    override def sectionCompletedRedirect(taxYear: Int): Result = Redirect(HOME(taxYear))
  }
  case object UnauthorisedPayments extends Journey("unauthorised-payments") {
    override def sectionCompletedRedirect(taxYear: Int): Result = Redirect(HOME(taxYear))
  }

  case object OverseasPensionsSummary extends Journey("overseas-pensions-summary")
  case object IncomeFromOverseasPensions extends Journey("income-from-overseas-pensions") {
    override def sectionCompletedRedirect(taxYear: Int): Result = Redirect(OVERSEAS_HOME(taxYear))
  }
  case object PaymentsIntoOverseasPensions extends Journey("payments-into-overseas-pensions") {
    override def sectionCompletedRedirect(taxYear: Int): Result = Redirect(OVERSEAS_HOME(taxYear))
  }
  case object TransferIntoOverseasPensions extends Journey("transfer-into-overseas-pensions") {
    override def sectionCompletedRedirect(taxYear: Int): Result = Redirect(OVERSEAS_HOME(taxYear))
  }
  case object ShortServiceRefunds extends Journey("short-service-refunds") {
    override def sectionCompletedRedirect(taxYear: Int): Result = Redirect(OVERSEAS_HOME(taxYear))
  }

  case object IncomeFromPensionsSummary extends Journey("income-from-pensions-summary")
  case object UkPensionIncome extends Journey("uk-pension-income") {
    override def sectionCompletedRedirect(taxYear: Int): Result = Redirect(INCOME_FROM_PENSIONS_HOME(taxYear))
  }
  case object StatePension extends Journey("state-pension") {
    override def sectionCompletedRedirect(taxYear: Int): Result = Redirect(INCOME_FROM_PENSIONS_HOME(taxYear))
  }

}
