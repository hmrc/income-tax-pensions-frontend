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

package forms

import forms.validation.StringConstraints.{nonEmpty, validateChar, validateSize}
import forms.validation.mappings.MappingUtil.trimmedText
import forms.validation.utils.ConstraintUtil.ConstraintUtil
import models.User
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.validation.Constraint

object PensionSchemeDetailsForm {

  case class PensionSchemeDetailsModel(providerName: String, schemeReference: String, pensionId: String)

  val providerName    = "providerName"
  val schemeReference = "schemeReference"
  val pensionId       = "pensionId"

  val nameCharLimit          = 74
  val nameRegex              = "^[0-9a-zA-Z\\\\{À-˿’}\\- _&`():.'^,]{1,74}$"
  private val schemeRefRegex = "^([0-9]{3})/([^ ].{0,9})$"
  private val pIdCharLimit   = 38
  private val pId            = "^[A-Za-z0-9.,\\-()/=!\"%&*; <>'+:?\\\\]{0,38}$"

  // provider name
  def nameNotEmpty(agentIndividual: String): Constraint[String] = nonEmpty(
    s"incomeFromPensions.pensionSchemeDetails.providerName.error.noEntry.$agentIndividual")

  def nameNotTooLong(agentIndividual: String): Constraint[String] =
    validateSize(nameCharLimit)(s"incomeFromPensions.pensionSchemeDetails.providerName.error.overCharLimit.$agentIndividual")

  val validateNameFormat: Constraint[String] = validateChar(nameRegex)("incomeFromPensions.pensionSchemeDetails.providerName.error.incorrectFormat")

  // scheme reference
  val refNotEmpty: Constraint[String] = nonEmpty("incomeFromPensions.pensionSchemeDetails.schemeRef.error.noEntry")
  private val validateRefFormat: Constraint[String] =
    validateChar(schemeRefRegex)("incomeFromPensions.pensionSchemeDetails.schemeRef.error.incorrectFormat")

  // pid
  private def pIdNotEmpty(agentIndividual: String): Constraint[String] = nonEmpty(
    s"incomeFromPensions.pensionSchemeDetails.pid.error.noEntry.$agentIndividual")

  private val pIdNotTooLong: Constraint[String]     = validateSize(pIdCharLimit)("incomeFromPensions.pensionSchemeDetails.pid.error.overCharLimit")
  private val validatePIdFormat: Constraint[String] = validateChar(pId)("incomeFromPensions.pensionSchemeDetails.pid.error.incorrectFormat")

  def pensionSchemeDetailsForm(user: User): Form[PensionSchemeDetailsModel] = {
    val agentIndividual = if (user.isAgent) "agent" else "individual"
    Form[PensionSchemeDetailsModel](
      mapping(
        providerName    -> trimmedText.verifying(nameNotEmpty(agentIndividual) andThen nameNotTooLong(agentIndividual) andThen validateNameFormat),
        schemeReference -> trimmedText.verifying(refNotEmpty andThen validateRefFormat),
        pensionId -> trimmedText.verifying(
          pIdNotEmpty(agentIndividual) andThen pIdNotTooLong andThen validatePIdFormat
        )
      )(PensionSchemeDetailsModel.apply)(PensionSchemeDetailsModel.unapply)
    )
  }
}
