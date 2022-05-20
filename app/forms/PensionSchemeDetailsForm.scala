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

package forms

import forms.validation.StringConstraints.{nonEmpty, validateChar, validateSize}
import forms.validation.mappings.MappingUtil.trimmedText
import forms.validation.utils.ConstraintUtil.ConstraintUtil
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.validation.Constraint

object PensionSchemeDetailsForm {

  case class PensionSchemeDetailsModel(providerName: String, schemeReference: String, pensionId: String)

  val providerName = "providerName"
  val schemeReference = "schemeReference"
  val pensionId = "pensionId"

  val nameCharLimit = 74
  val nameRegex = "^[0-9a-zA-Z\\\\{À-˿’}\\- _&`():.'^,]{1,74}$"
  val schemeRefRegex = "^([0-9]{3})/([^ ].{0,9})$"
  val pIdCharLimit = 38
  val pId = "^[A-Za-z0-9.,\\-()/=!\"%&*; <>'+:?\\\\]{0,38}$"

  //provider name
  val nameNotEmpty: Constraint[String] = nonEmpty("incomeFromPensions.pensionSchemeDetails.providerName.error.noEntry")
  val nameNotTooLong: Constraint[String] = validateSize(nameCharLimit)("incomeFromPensions.pensionSchemeDetails.providerName.error.overCharLimit")
  val validateNameFormat: Constraint[String] = validateChar(nameRegex)("incomeFromPensions.pensionSchemeDetails.providerName.error.incorrectFormat")

  //scheme reference
  val refNotEmpty: Constraint[String] = nonEmpty("incomeFromPensions.pensionSchemeDetails.schemeRef.error.noEntry")
  val validateRefFormat: Constraint[String] = validateChar(schemeRefRegex)("incomeFromPensions.pensionSchemeDetails.schemeRef.error.incorrectFormat")

  //pid
  val pIdNotEmpty: Constraint[String] = nonEmpty("incomeFromPensions.pensionSchemeDetails.pid.error.noEntry")
  val pIdNotTooLong: Constraint[String] = validateSize(pIdCharLimit)("incomeFromPensions.pensionSchemeDetails.pid.error.overCharLimit")
  val validatePIdFormat: Constraint[String] = validateChar(pId)("incomeFromPensions.pensionSchemeDetails.pid.error.incorrectFormat")

  def pensionSchemeDetailsForm: Form[PensionSchemeDetailsModel] =
    Form[PensionSchemeDetailsModel](
      mapping(
        providerName -> trimmedText.verifying(
          nameNotEmpty andThen nameNotTooLong andThen validateNameFormat),
        schemeReference -> trimmedText.verifying(
          refNotEmpty andThen validateRefFormat),
        pensionId -> trimmedText.verifying(
          pIdNotEmpty andThen pIdNotTooLong andThen validatePIdFormat
        )
      )(PensionSchemeDetailsModel.apply)(PensionSchemeDetailsModel.unapply)
    )
}
