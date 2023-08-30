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
import forms.validation.StringConstraints.{nonEmpty, validateChar, validateSize}
import forms.validation.mappings.MappingUtil.trimmedText
import forms.validation.utils.ConstraintUtil.ConstraintUtil
import play.api.data.Forms.{ignored, mapping}
import play.api.data.validation.Constraint
import play.api.data.{Form, Mapping}

object PensionSchemeForm extends InputFilters {

  case class TcSsrPensionsSchemeFormModel(providerName: String, schemeReference: String, providerAddress: String, countryId: Option[String] = None)

  val providerName = "providerName"
  val schemeReference = "schemeReference"
  val providerAddress = "providerAddress"

  val nameCharLimit = 105
  private val addressCharLimit = 250

  val nameRegex = "^[0-9a-zA-Z\\\\{À-˿’}\\- _&`():.'^,]{1,105}$"
  private val schemeUKRefRegex = "^([0-9]{8})R[A-Z]$"
  private val schemeNonUKRefRegex = "^([0-9]{6})$"

  //provider name
  val nameNotEmpty: Constraint[String] = nonEmpty("common.overseasPensions.providerName.error.noEntry")
  val nameNotTooLong: Constraint[String] = validateSize(nameCharLimit)("common.overseasPensions.providerName.error.overCharLimit")
  val validateNameFormat: Constraint[String] = validateChar(nameRegex)("common.overseasPensions.providerName.error.incorrectFormat")

  //scheme reference
  val refNotEmpty: Boolean => Constraint[String] = (isUKScheme: Boolean) =>
    if (isUKScheme) nonEmpty("common.overseasPensions.schemeTaxRef.error.noEntry") else nonEmpty("common.overseasPensions.qopsRef.error.noEntry")
  private val validateUKRefFormat: Constraint[String] = validateChar(schemeUKRefRegex)("common.pensionSchemeTaxReference.error.incorrectFormat")
  private val validateNonUKRefFormat: Constraint[String] = validateChar(schemeNonUKRefRegex)("common.overseasPensions.qops.error.incorrectFormat")
  private val validateSchemeTaxRef: Boolean => Constraint[String] = (isUKScheme: Boolean) =>
    if (isUKScheme) validateUKRefFormat else validateNonUKRefFormat

  //provider address
  private val addressNotEmpty: Constraint[String] = nonEmpty("common.overseasPensions.providerAddress.error.noEntry")
  private val addressNotTooLong: Constraint[String] = validateSize(addressCharLimit)("common.overseasPensions.providerAddress.error.overCharLimit")

  //country
  val countryNotEmptyMsg: String = "common.overseasPensions.country.error.noEntry"

  val countryMapping: (String, Boolean) => (String, Mapping[Option[String]]) = (agentOrIndividual: String, isCountryUK: Boolean) => {
    if (isCountryUK) countryId -> ignored(Option.empty[String]) else {
      val (str, constraint) = CountryForm.countryMapping(agentOrIndividual, countryNotEmptyMsg)
      str -> constraint.transform[Option[String]](str1 => Some(str1), optStr => optStr.getOrElse(""))
    }
  }

  def tcSsrPensionSchemeForm(agentOrIndividual: String, isCountryUK: Boolean): Form[TcSsrPensionsSchemeFormModel] =
    Form[TcSsrPensionsSchemeFormModel](
      mapping(
        providerName -> trimmedText.verifying(
          nameNotEmpty andThen nameNotTooLong andThen validateNameFormat),
        schemeReference -> trimmedText.verifying(
          refNotEmpty(isCountryUK) andThen validateSchemeTaxRef(isCountryUK)),
        providerAddress -> trimmedText.verifying(addressNotEmpty andThen addressNotTooLong),
        countryMapping(agentOrIndividual, isCountryUK)
      )(TcSsrPensionsSchemeFormModel.apply)(TcSsrPensionsSchemeFormModel.unapply)
    )
}
