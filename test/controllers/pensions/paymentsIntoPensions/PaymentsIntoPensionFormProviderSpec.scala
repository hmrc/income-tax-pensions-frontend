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

package controllers.pensions.paymentsIntoPensions

import forms.{AmountForm, YesNoForm}
import play.api.data.FormError
import utils.UnitTest

class PaymentsIntoPensionFormProviderSpec extends UnitTest {

  private val anyBoolean = true
  private val amount: String = 123.0.toString
  private val correctBooleanData = Map(YesNoForm.yesNo -> anyBoolean.toString)
  private val correctAmountData = Map(AmountForm.amount -> amount)
  private val wrongKeyData = Map("wrongKey" -> amount)
  private val wrongAmountFormat: Map[String, String] = Map(AmountForm.amount -> "123.45.6")
  private val emptyData: Map[String, String] = Map.empty
  private val overMaximumAmount: Map[String, String] = Map(AmountForm.amount -> "100,000,000,000")

  private val underTest = new PaymentsIntoPensionFormProvider()

  ".reliefAtSourcePensionsForm" should {
    "return a form that maps data when data is correct" in {
      underTest.reliefAtSourcePensionsForm(isAgent = anyBoolean).bind(correctBooleanData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.reliefAtSourcePensionsForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("pensions.reliefAtSource.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.reliefAtSourcePensionsForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("pensions.reliefAtSource.error.noEntry.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.reliefAtSourcePensionsForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("pensions.reliefAtSource.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.reliefAtSourcePensionsForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError("value", Seq("pensions.reliefAtSource.error.noEntry.individual"), Seq())
        )
      }
    }
  }

  ".reliefAtSourcePaymentsAndTaxReliefAmountForm" should {
    "return a form that maps data when data is correct" in {
      underTest.reliefAtSourcePaymentsAndTaxReliefAmountForm.bind(correctAmountData).errors shouldBe Seq.empty
    }

    "return a form that contains error" which {
      "key is wrong" in {
        underTest.reliefAtSourcePaymentsAndTaxReliefAmountForm.bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("pensions.reliefAtSourceTotalPaymentsAndTaxReliefAmount.error.noEntry"), Seq())
        )
      }

      "data is empty" in {
        underTest.reliefAtSourcePaymentsAndTaxReliefAmountForm.bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("pensions.reliefAtSourceTotalPaymentsAndTaxReliefAmount.error.noEntry"), Seq())
        )
      }

      "data is wrongFormat" in {
        underTest.reliefAtSourcePaymentsAndTaxReliefAmountForm.bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("pensions.reliefAtSourceTotalPaymentsAndTaxReliefAmount.error.invalidFormat"), Seq())
        )
      }

      "data is overMaximum" in {
        underTest.reliefAtSourcePaymentsAndTaxReliefAmountForm.bind(overMaximumAmount).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("pensions.reliefAtSourceTotalPaymentsAndTaxReliefAmount.error.overMaximum"), Seq())
        )
      }
    }
  }

  ".oneOffRASPaymentsAmountForm" should {
    "return a form that maps data when data is correct" in {
      underTest.oneOffRASPaymentsAmountForm.bind(correctAmountData).errors shouldBe Seq.empty
    }

    "return a form that contains error" which {
      "key is wrong" in {
        underTest.oneOffRASPaymentsAmountForm.bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("paymentsIntoPensions.oneOffRasAmount.error.noEntry"), Seq())
        )
      }

      "data is empty" in {
        underTest.oneOffRASPaymentsAmountForm.bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("paymentsIntoPensions.oneOffRasAmount.error.noEntry"), Seq())
        )
      }

      "data is wrongFormat" in {
        underTest.oneOffRASPaymentsAmountForm.bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("paymentsIntoPensions.oneOffRasAmount.error.invalidFormat"), Seq())
        )
      }

      "data is overMaximum" in {
        underTest.oneOffRASPaymentsAmountForm.bind(overMaximumAmount).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("paymentsIntoPensions.oneOffRasAmount.error.overMaximum"), Seq())
        )
      }
    }
  }

  ".totalPaymentsIntoRASForm" should {
    "return a form that maps data when data is correct" in {
      underTest.totalPaymentsIntoRASForm.bind(correctBooleanData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.totalPaymentsIntoRASForm.bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("paymentsIntoPensions.totalRASPayments.error"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.totalPaymentsIntoRASForm.bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("paymentsIntoPensions.totalRASPayments.error"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.totalPaymentsIntoRASForm.bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("paymentsIntoPensions.totalRASPayments.error"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.totalPaymentsIntoRASForm.bind(emptyData).errors shouldBe Seq(
          FormError("value", Seq("paymentsIntoPensions.totalRASPayments.error"), Seq())
        )
      }
    }
  }

  ".pensionsTaxReliefNotClaimedForm" should {
    "return a form that maps data when data is correct" in {
      underTest.pensionsTaxReliefNotClaimedForm(isAgent = anyBoolean).bind(correctBooleanData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.pensionsTaxReliefNotClaimedForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("pensions.pensionsTaxReliefNotClaimed.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.pensionsTaxReliefNotClaimedForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("pensions.pensionsTaxReliefNotClaimed.error.noEntry.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.pensionsTaxReliefNotClaimedForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("pensions.pensionsTaxReliefNotClaimed.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.pensionsTaxReliefNotClaimedForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError("value", Seq("pensions.pensionsTaxReliefNotClaimed.error.noEntry.individual"), Seq())
        )
      }
    }
  }

  ".retirementAnnuityForm" should {
    "return a form that maps data when data is correct" in {
      underTest.retirementAnnuityForm(isAgent = anyBoolean).bind(correctBooleanData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.retirementAnnuityForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("pensions.retirementAnnuityContract.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.retirementAnnuityForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("pensions.retirementAnnuityContract.error.noEntry.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.retirementAnnuityForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("pensions.retirementAnnuityContract.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.retirementAnnuityForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError("value", Seq("pensions.retirementAnnuityContract.error.noEntry.individual"), Seq())
        )
      }
    }
  }

  ".retirementAnnuityAmountForm" should {
    "return a form that maps data when data is correct" in {
      underTest.retirementAnnuityAmountForm.bind(correctAmountData).errors shouldBe Seq.empty
    }

    "return a form that contains error" which {
      "key is wrong" in {
        underTest.retirementAnnuityAmountForm.bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("pensions.retirementAnnuityAmount.error.noEntry"), Seq())
        )
      }

      "data is empty" in {
        underTest.retirementAnnuityAmountForm.bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("pensions.retirementAnnuityAmount.error.noEntry"), Seq())
        )
      }

      "data is wrongFormat" in {
        underTest.retirementAnnuityAmountForm.bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("pensions.retirementAnnuityAmount.error.incorrectFormat"), Seq())
        )
      }

      "data is overMaximum" in {
        underTest.retirementAnnuityAmountForm.bind(overMaximumAmount).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("pensions.retirementAnnuityAmount.error.overMaximum"), Seq())
        )
      }
    }
  }

  ".workplacePensionForm" should {
    "return a form that maps data when data is correct" in {
      underTest.workplacePensionForm(isAgent = anyBoolean).bind(correctBooleanData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.workplacePensionForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("pensions.workplacePension.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.workplacePensionForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("pensions.workplacePension.error.noEntry.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.workplacePensionForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("pensions.workplacePension.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.workplacePensionForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError("value", Seq("pensions.workplacePension.error.noEntry.individual"), Seq())
        )
      }
    }
  }

  ".workplacePensionAmountForm" should {
    "return a form that maps data when data is correct" in {
      underTest.workplacePensionAmountForm.bind(correctAmountData).errors shouldBe Seq.empty
    }

    "return a form that contains error" which {
      "key is wrong" in {
        underTest.workplacePensionAmountForm.bind(wrongKeyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("pensions.workplaceAmount.error.noEntry"), Seq())
        )
      }

      "data is empty" in {
        underTest.workplacePensionAmountForm.bind(emptyData).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("pensions.workplaceAmount.error.noEntry"), Seq())
        )
      }

      "data is wrongFormat" in {
        underTest.workplacePensionAmountForm.bind(wrongAmountFormat).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("pensions.workplaceAmount.error.incorrectFormat"), Seq())
        )
      }

      "data is overMaximum" in {
        underTest.workplacePensionAmountForm.bind(overMaximumAmount).errors shouldBe Seq(
          FormError(AmountForm.amount, Seq("pensions.workplaceAmount.error.maxAmount"), Seq())
        )
      }
    }
  }

  ".reliefAtSourceOneOffPaymentsForm" should {
    "return a form that maps data when data is correct" in {
      underTest.reliefAtSourceOneOffPaymentsForm(isAgent = anyBoolean).bind(correctBooleanData).errors shouldBe Seq.empty
    }

    "return a form that contains agent error" which {
      "when isAgent is true and key is wrong" in {
        underTest.reliefAtSourceOneOffPaymentsForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("pensions.reliefAtSourceOneOffPayments.error.noEntry.agent"), Seq())
        )
      }

      "when isAgent is true and data is empty" in {
        underTest.reliefAtSourceOneOffPaymentsForm(isAgent = true).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("pensions.reliefAtSourceOneOffPayments.error.noEntry.agent"), Seq())
        )
      }
    }

    "return a form that contains individual error" which {
      "when isAgent is false and key is wrong" in {
        underTest.reliefAtSourceOneOffPaymentsForm(isAgent = false).bind(wrongKeyData).errors shouldBe Seq(
          FormError("value", Seq("pensions.reliefAtSourceOneOffPayments.error.noEntry.individual"), Seq())
        )
      }

      "when isAgent is false and data is empty" in {
        underTest.reliefAtSourceOneOffPaymentsForm(isAgent = false).bind(emptyData).errors shouldBe Seq(
          FormError("value", Seq("pensions.reliefAtSourceOneOffPayments.error.noEntry.individual"), Seq())
        )
      }
    }
  }


}

