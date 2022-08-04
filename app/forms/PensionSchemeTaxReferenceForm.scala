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

import filters.InputFilters
import forms.validation.StringConstraints.{validateChar, validateSize}
import forms.validation.mappings.MappingUtil.trimmedText
import forms.validation.utils.ConstraintUtil.ConstraintUtil
import play.api.data.Form
import play.api.data.validation.Constraint
import play.api.data.validation.Constraints.nonEmpty

object PensionSchemeTaxReferenceForm extends InputFilters {

  val taxReferenceId: String = "taxReferenceId"
  val regex: String = "^\\d{8}[R]{1}[a-zA-Z]{1}$"

  def notEmpty(message : String): Constraint[String] = nonEmpty(message)

  def validateFormat(message : String): Constraint[String] = validateChar(regex)(message)

  def pensionSchemeTaxReferenceForm(noEntryMsg : String, incorrectFormatMsg : String): Form[String] = Form(
    taxReferenceId -> trimmedText.transform[String](filter, identity).verifying(
      notEmpty(noEntryMsg) andThen validateFormat(incorrectFormatMsg)
    )
  )
}
