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

object UnAuthorisedPaymentsForm {

  val yesSurchargeValue= "yesSurchargeValue"
  val yesNotSurchargeValue = "yesNotSurchargeValue"
  val noValue = "noValue"

  case class UnAuthorisedPaymentsModel(unauthorisedPayments: Seq[String]) {
    val containsYesSurcharge: Boolean = unauthorisedPayments.contains(yesSurchargeValue)
    val containsYesNotSurcharge: Boolean = unauthorisedPayments.contains(yesNotSurchargeValue)
    val containsNoVal: Boolean = unauthorisedPayments.contains(noValue)
  }

  val unauthorisedPaymentsType: String = "unauthorisedPayments"

  val allEmpty: String => Constraint[UnAuthorisedPaymentsModel] = msgKey => constraint[UnAuthorisedPaymentsModel](
    unauthorisedPayments => {
      if (!unauthorisedPayments.containsYesSurcharge & !unauthorisedPayments.containsYesNotSurcharge & !unauthorisedPayments.containsNoVal){
        Invalid(msgKey)
      }
      else {
        Valid
      }
    }
  )

  def unAuthorisedPaymentsTypeForm(): Form[UnAuthorisedPaymentsModel] = Form[UnAuthorisedPaymentsModel](
    mapping(
      unauthorisedPaymentsType -> seq(text)
    )(UnAuthorisedPaymentsModel.apply)(UnAuthorisedPaymentsModel.unapply).verifying(
      allEmpty("common.unauthorisedPayments.error.checkbox.or.radioButton.noEntry")
    )
  )
}
