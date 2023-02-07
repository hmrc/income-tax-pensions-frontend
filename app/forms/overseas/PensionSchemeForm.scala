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
import play.api.data.{Form, Mapping}
import play.api.data.Forms.{ignored, mapping}
import play.api.data.validation.Constraint

object PensionSchemeForm extends InputFilters {
  
  case class TransferPensionsSchemeFormModel(providerName: String, schemeReference: String, providerAddress: String, countryId: Option[String] = None)

  val providerName = "providerName"
  val schemeReference = "schemeReference"
  val providerAddress = "providerAddress"

  val nameCharLimit = 105
  val addressCharLimit = 250
  
  val nameRegex = "^[0-9a-zA-Z\\\\{À-˿’}\\- _&`():.'^,]{1,105}$"
  val schemeUKRefRegex = "^([0-9]{8})R[A-Z]$"
  val schemeNonUKRefRegex = "^([0-9]{6})$"

  //provider name
  val nameNotEmpty: Constraint[String] = nonEmpty("common.overseasPensions.providerName.error.noEntry")
  val nameNotTooLong: Constraint[String] = validateSize(nameCharLimit)("common.overseasPensions.providerName.error.overCharLimit")
  val validateNameFormat: Constraint[String] = validateChar(nameRegex)("common.overseasPensions.providerName.error.incorrectFormat")

  //scheme reference
  val refNotEmpty: Constraint[String] = nonEmpty("common.overseasPensions.schemeTaxRef.error.noEntry")
  val validateUKRefFormat: Constraint[String] = validateChar(schemeUKRefRegex)("common.overseasPensions.pstr.error.incorrectFormat")
  val validateNonUKRefFormat: Constraint[String] = validateChar(schemeNonUKRefRegex)("common.overseasPensions.qops.error.incorrectFormat")
  val validateSchemeTaxRef: Boolean => Constraint[String] = (isUKScheme: Boolean) =>if (isUKScheme) validateUKRefFormat else validateNonUKRefFormat
  
  //provider address
  val addressNotEmpty: Constraint[String] = nonEmpty("common.overseasPensions.providerAddress.error.noEntry")
  val addressNotTooLong: Constraint[String] = validateSize(addressCharLimit)("common.overseasPensions.providerAddress.error.overCharLimit")
  

  //country
  val countryNotEmptyMsg: String = "common.overseasPensions.country.error.noEntry"
  
  def countryMapping(agentOrIndividual: String, isCountryUK: Boolean): (String, Mapping[Option[String]]) = {
    if (isCountryUK)  countryId -> ignored(Option.empty[String]) else {
      val (str, constraint) = CountryForm.countryMapping(agentOrIndividual, countryNotEmptyMsg)
      str -> constraint.transform[Option[String]](str1 => Some(str1), optStr => optStr.getOrElse(""))
    }
  }
  
  def transferPensionSchemeForm(agentOrIndividual: String, isCountryUK: Boolean): Form[TransferPensionsSchemeFormModel] =
    Form[TransferPensionsSchemeFormModel](
      mapping(
        providerName -> trimmedText.verifying(
          nameNotEmpty andThen nameNotTooLong andThen validateNameFormat),
        schemeReference -> trimmedText.verifying(
          refNotEmpty andThen validateSchemeTaxRef(isCountryUK)),
        providerAddress -> trimmedText.verifying(addressNotEmpty andThen addressNotTooLong),
        countryMapping(agentOrIndividual, isCountryUK)
      )(TransferPensionsSchemeFormModel.apply)(TransferPensionsSchemeFormModel.unapply)
    )
}
