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

package forms.overseas

import filters.InputFilters
import forms.CountryForm
import forms.CountryForm.countryId
import forms.validation.mappings.MappingUtil.{oText, optionCurrency}
import play.api.data.{Form, Mapping}
import play.api.data.Forms.{ignored, mapping}

object DoubleTaxationAgreementForm extends InputFilters {

  case class DoubleTaxationAgreementFormModel(countryId: Option[String] = None,
                                              article: Option[String] = None,
                                              treaty: Option[String] = None,
                                              reliefAmount: Option[BigDecimal])

  val article = "article"
  val treaty = "treaty"
  val reliefAmount = "amount-2"

  //country
  val countryNotEmptyMsg: String = "transferIntoOverseasPensions.doubleTaxation.country.error.noEntry"

  // relief amount
  val reliefNonEmpty: String = "transferIntoOverseasPensions.doubleTaxation.amount.error.noEntry"
  val wrongFormatKey: String = "transferIntoOverseasPensions.doubleTaxation.amount.error.incorrectFormat"
  val exceedsMaxAmountKey: String = "transferIntoOverseasPensions.doubleTaxation.amount.error.tooBig"

  val countryMapping: (String, Boolean) => (String, Mapping[Option[String]]) = (agentOrIndividual: String, isCountryUK: Boolean) => {
    if (isCountryUK)  countryId -> ignored(Option.empty[String]) else {
      val (str, constraint) = CountryForm.countryMapping(agentOrIndividual, countryNotEmptyMsg)
      str -> constraint.transform[Option[String]](str1 => Some(str1), optStr => optStr.getOrElse(""))
    }
  }

  def doubleTaxationAgreementForm(agentOrIndividual: String, isCountryUK: Boolean = false): Form[DoubleTaxationAgreementFormModel] =
    Form[DoubleTaxationAgreementFormModel](
      mapping(
        countryMapping(agentOrIndividual, isCountryUK),
        article -> oText,
        treaty -> oText,
        reliefAmount -> optionCurrency(
          requiredKey = reliefNonEmpty,
          wrongFormatKey = wrongFormatKey,
          maxAmountKey = exceedsMaxAmountKey
        )
      )(DoubleTaxationAgreementFormModel.apply)(DoubleTaxationAgreementFormModel.unapply)
    )
}
