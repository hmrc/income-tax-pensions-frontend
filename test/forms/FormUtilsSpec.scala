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

import forms.AmountForm.amountForm
import forms.validation.StringConstraints.validateSize
import forms.validation.mappings.MappingUtil.trimmedText
import forms.validation.utils.ConstraintUtil.ConstraintUtil
import play.api.data.Form
import play.api.data.validation.Constraint
import play.api.data.validation.Constraints.nonEmpty
import utils.UnitTest

class FormUtilsSpec extends UnitTest with FormUtils {

  val charLimit: Int = 4
  val validValue = "name"
  val invalidValue = "nametoobig"

  def notEmpty(value: String): Constraint[String] =
    nonEmpty("Some error")

  val NotCharLimit: Constraint[String] = validateSize(charLimit)("It's too big")

  def theForm(): Form[BigDecimal] = {
    amountForm("nothing to see here", "this not good", "too big")
  }

  def aTestUtilForm(value: String): Form[String] = Form(
    value -> trimmedText.verifying(
      notEmpty(value) andThen NotCharLimit
    )
  )

  "The utils form" should {

    "fill the form without errors when constraints pass validation" when {

      "there is data" in {

        val actual: Form[String] = aTestUtilForm(validValue).fillAndValidate(validValue)
        actual.value shouldBe Some(validValue)
        actual.hasErrors shouldBe false
      }

      "fill the form with errors when constraints fail validation" in {

        val actual: Form[String] = aTestUtilForm(invalidValue).fillAndValidate(invalidValue)
        actual.value shouldBe Some(invalidValue)
        actual.hasErrors shouldBe true
      }
    }
  }

  "The utils form" should {

    "fill the form" when {

      "there is data" in {

        val actual = fillForm(theForm(), Some(44.44), Some(23.33))
        actual shouldBe theForm().fill(23.33)
      }
    }
  }

}
