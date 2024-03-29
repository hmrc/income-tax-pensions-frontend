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

import filters.InputFilters
import forms.validation.mappings.MappingUtil.trimmedText
import forms.validation.utils.ConstraintUtil.{ConstraintUtil, constraint}
import play.api.data.validation.Constraints.nonEmpty
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.data.{Form, Mapping}

object CountryForm extends InputFilters {

  val countryId: String = "countryId"

  def notEmpty(message: String): Constraint[String] = nonEmpty(message)

  private def validateCountry(): String => Constraint[String] = msgKey =>
    constraint[String](x => if (Countries.allCountries.map(_.alphaTwoCode).contains(x)) Valid else Invalid(msgKey))

  def countryMapping(agentOrIndividual: String, noEntryMsg: String): (String, Mapping[String]) =
    countryId -> trimmedText
      .transform[String](filter, identity)
      .verifying(
        notEmpty(noEntryMsg) andThen validateCountry()(noEntryMsg)
      )
  def countryMapping(agentOrIndividual: String): (String, Mapping[String]) = {
    val noEntryMsg: String = s"incomeFromOverseasPensions.pensionOverseasIncomeCountry.error.noEntry.$agentOrIndividual"
    countryMapping(agentOrIndividual, noEntryMsg)
  }

  def countryForm(agentOrIndividual: String): Form[String] =
    Form(countryMapping(agentOrIndividual))

}
