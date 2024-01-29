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

import forms.validation.utils.ConstraintUtil.constraint
import play.api.data.Form
import play.api.data.Forms.{mapping, seq, text}
import play.api.data.validation.{Constraint, Invalid, Valid}

object ReducedAnnualAllowanceTypeQuestionForm {

  val moneyPurchaseCheckboxValue = "moneyPurchaseType"
  val taperedCheckboxValue       = "taperedCheckboxType"

  case class ReducedAnnualAllowanceTypeQuestionModel(reducedAllowanceTypes: Seq[String]) {
    val containsMoneyPurchase: Boolean = reducedAllowanceTypes.contains(moneyPurchaseCheckboxValue)
    val containsTapered: Boolean       = reducedAllowanceTypes.contains(taperedCheckboxValue)
  }

  val reducedAnnualAllowanceType: String = "reducedAnnualAllowanceType"

  val allEmpty: String => Constraint[ReducedAnnualAllowanceTypeQuestionModel] = msgKey =>
    constraint[ReducedAnnualAllowanceTypeQuestionModel] { reducedAnnualAllowanceType =>
      if (!reducedAnnualAllowanceType.containsMoneyPurchase & !reducedAnnualAllowanceType.containsTapered) Invalid(msgKey) else Valid
    }

  def reducedAnnualAllowanceTypeForm(isAgent: Boolean): Form[ReducedAnnualAllowanceTypeQuestionModel] = Form[ReducedAnnualAllowanceTypeQuestionModel](
    mapping(
      reducedAnnualAllowanceType -> seq(text)
    )(ReducedAnnualAllowanceTypeQuestionModel.apply)(ReducedAnnualAllowanceTypeQuestionModel.unapply).verifying(
      allEmpty(s"pensions.reducedAnnualAllowanceType.checkbox.error.${if (isAgent) "agent" else "individual"}")
    )
  )
}
