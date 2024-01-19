/*
 * Copyright 2024 HM Revenue & Customs
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

package models.pension.pages


import forms.Countries.getCountryFromCodeWithDefault
import models.mongo.PensionsUserData
import play.api.i18n.Messages
import play.api.mvc.Call
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.ViewUtils.{bigDecimalCurrency, convertBoolToYesOrNo, summaryListRow}

case class OverseasPensionSchemeSummaryPage(taxYear: Int, summaryListDataRows: Seq[SummaryListRow], index: Option[Int])

object OverseasPensionSchemeSummaryPage {

  def apply(taxYear: Int, pensionsUserData: PensionsUserData, index: Option[Int])(implicit messages: Messages): OverseasPensionSchemeSummaryPage = {
    val overSeasPensions = pensionsUserData.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes(index.getOrElse(0))

    OverseasPensionSchemeSummaryPage(taxYear, Seq(   //scalastyle:off line.size.limit
      Some(summaryListRow(
        HtmlContent(Messages("incomeFromOverseasPensions.summary.country")),
        HtmlContent(getCountryFromCodeWithDefault(overSeasPensions.alphaTwoCode)),
        actions = Seq((Call("GET", controllers.pensions.incomeFromOverseasPensions.routes.PensionOverseasIncomeCountryController.show(taxYear, index).url),
          Messages("common.change"), Some(Messages("incomeFromOverseasPensions.summary.country.hidden")))))),
      Some(summaryListRow(
        HtmlContent(Messages("incomeFromOverseasPensions.summary.pension.payments")),
        HtmlContent(s"${Messages("incomeFromOverseasPensions.summary.amount", bigDecimalCurrency(overSeasPensions.pensionPaymentAmount.getOrElse(BigDecimal(0)).toString))} " +
          s"${overSeasPensions.pensionPaymentTaxPaid.map(taxPaid =>  s"<br> ${Messages("incomeFromOverseasPensions.summary.nonUk.amount",bigDecimalCurrency(taxPaid.toString))}").getOrElse("")}"),
        actions = Seq((Call("GET", controllers.pensions.incomeFromOverseasPensions.routes.PensionPaymentsController.show(taxYear, index).url),
          Messages("common.change"), Some(Messages("incomeFromOverseasPensions.summary.pension.payments.hidden")))))),
      Some(summaryListRow(
        HtmlContent(Messages("incomeFromOverseasPensions.summary.swt")),
        overSeasPensions.specialWithholdingTaxAmount.fold{HtmlContent(Messages("common.no"))}{swt => HtmlContent(bigDecimalCurrency(swt.toString()))},
        actions = Seq((Call("GET", controllers.pensions.incomeFromOverseasPensions.routes.SpecialWithholdingTaxController.show(taxYear, index).url),
          Messages("common.change"), Some(Messages("incomeFromOverseasPensions.summary.swt.hidden")))))),
      Some(summaryListRow(
        HtmlContent(Messages("incomeFromOverseasPensions.summary.ftc")),
        HtmlContent(convertBoolToYesOrNo(overSeasPensions.foreignTaxCreditReliefQuestion).getOrElse("No")),
        actions = Seq((Call("GET", controllers.pensions.incomeFromOverseasPensions.routes.ForeignTaxCreditReliefController.show(taxYear, index).url),
          Messages("common.change"), Some(Messages("incomeFromOverseasPensions.summary.ftc.hidden")))))),
      overSeasPensions.taxableAmount.map(
        amount => summaryListRow(
          HtmlContent(Messages("incomeFromOverseasPensions.summary.tax.amount")),
          HtmlContent(bigDecimalCurrency(amount.toString)),
          actions = Seq.empty))
    ).flatten, Some(index.getOrElse(0)))
  }
}
