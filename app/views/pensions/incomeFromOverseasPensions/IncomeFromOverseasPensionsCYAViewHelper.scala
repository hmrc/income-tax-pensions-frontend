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

package views.pensions.incomeFromOverseasPensions

import controllers.pensions.incomeFromOverseasPensions.routes.{CountrySummaryListController, PensionOverseasIncomeStatus}
import controllers.pensions.overseas.incomeFromOverseasPension.routes.ForeignTaxCreditReliefController
import forms.Countries
import models.pension.charges.IncomeFromOverseasPensionsViewModel
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.CYABaseHelper

object IncomeFromOverseasPensionsCYAViewHelper extends CYABaseHelper {

  def summaryListRows(incomeFromOverseasPensionsViewModel: IncomeFromOverseasPensionsViewModel, taxYear: Int)
                     (implicit messages: Messages): Seq[SummaryListRow] = {
    val summaryRowForPaymentsFromOverseasPensionsRow = summaryRowForPaymentsFromOverseasPensions(incomeFromOverseasPensionsViewModel, taxYear)
    val summaryRowForPensionSchemeCodesRows = summaryRowForPensionSchemeCodes(incomeFromOverseasPensionsViewModel, taxYear)
    (summaryRowForPaymentsFromOverseasPensionsRow +: Seq(summaryRowForPensionSchemeCodesRows)).flatten
  }


  private def summaryRowForPaymentsFromOverseasPensions(incomeFromOverseasPensionsViewModel: IncomeFromOverseasPensionsViewModel, taxYear: Int)
                                                       (implicit messages: Messages): Option[SummaryListRow] = {
    Some(
      summaryListRowWithBooleanValue(
        "incomeFromOverseasPensions.cya.paymentsFromOverseasPensions",
        incomeFromOverseasPensionsViewModel.paymentsFromOverseasPensionsQuestion,
        PensionOverseasIncomeStatus.show(taxYear))(messages)
    )
  }

  private def summaryRowForPensionSchemeCodes(incomeFromOverseasPensionsViewModel: IncomeFromOverseasPensionsViewModel, taxYear: Int)
                                             (implicit messages: Messages) : Option[SummaryListRow] = {

    if (
      incomeFromOverseasPensionsViewModel.paymentsFromOverseasPensionsQuestion.contains(true)
        && incomeFromOverseasPensionsViewModel.overseasIncomePensionSchemes.length > 0
    ) {
      val countryNames = for {
        pensionScheme <- incomeFromOverseasPensionsViewModel.overseasIncomePensionSchemes
        countryCode = pensionScheme.countryCode.getOrElse("N/A")
      } yield {
         Countries.getCountryFromCodeWithDefault(Some(countryCode))
            //TODO: Add 3 digit country code parsing as well.
      }

      Some(summaryListRowWithStrings(
        "incomeFromOverseasPensions.cya.overseasPensionSchemes",
        Some(countryNames.map(_.toUpperCase).mkString(", ")),
        CountrySummaryListController.show(taxYear))(messages)
      )
    } else { None }
  }

}
