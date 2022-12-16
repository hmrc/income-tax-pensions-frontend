/*
 * Copyright 2022 HM Revenue & Customs
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

import controllers.pensions.incomeFromOverseasPensions.routes.PensionOverseasIncomeStatus
import controllers.pensions.overseas.incomeFromOverseasPension.routes.ForeignTaxCreditReliefController
import forms.Countries
import models.pension.charges.IncomeFromOverseasPensionsViewModel
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.CYABaseHelper

object IncomeFromOverseasPensionsCYAViewHelper extends CYABaseHelper {

  def summaryListRows(incomeFromOverseasPensionsViewModel: IncomeFromOverseasPensionsViewModel, taxYear: Int)(implicit messages: Messages): Seq[SummaryListRow] = {
    val summaryRowForPaymentsFromOverseasPensionsRow = summaryRowForPaymentsFromOverseasPensions(incomeFromOverseasPensionsViewModel, taxYear)
    val summaryRowForPensionSchemeCodesRows = summaryRowForPensionSchemeCodes(incomeFromOverseasPensionsViewModel, taxYear)
    (summaryRowForPaymentsFromOverseasPensionsRow +: summaryRowForPensionSchemeCodesRows).flatten
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
                                       (implicit messages: Messages) : Seq[Option[SummaryListRow]] = {
    val paymentsFromOverseasPensionsQuestionAnswer = incomeFromOverseasPensionsViewModel.paymentsFromOverseasPensionsQuestion.filter(_ == true)
    if (paymentsFromOverseasPensionsQuestionAnswer == Some(true) && incomeFromOverseasPensionsViewModel.overseasIncomePensionSchemes.length > 0) {
      incomeFromOverseasPensionsViewModel.overseasIncomePensionSchemes.zipWithIndex.map{
        case (scheme, index) =>
          val countryName = Countries.getCountryFromCode(incomeFromOverseasPensionsViewModel.overseasIncomePensionSchemes(index).countryCode).map(_.countryName).map(_.toUpperCase)
          Some(summaryListRowWithStrings(
            "incomeFromOverseasPensions.cya.overseasPensionSchemes",
            if (countryName == None) incomeFromOverseasPensionsViewModel.overseasIncomePensionSchemes(index).countryCode else countryName,
            //TODO - To change the redirect to the Oveseas Pension page
            ForeignTaxCreditReliefController.show(taxYear, Some(index)))(messages))
      }
    } else Seq(None)
  }

}
