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

package models.pension.charges

import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.wordspec.AnyWordSpecLike

class PensionSchemeSpec extends AnyWordSpecLike with TableDrivenPropertyChecks with Matchers {
  val scheme = PensionScheme(
    alphaThreeCode = None,
    alphaTwoCode = Some("DE"),
    pensionPaymentAmount = Some(2000.00),
    pensionPaymentTaxPaid = Some(400.00),
    specialWithholdingTaxQuestion = Some(false),
    specialWithholdingTaxAmount = None,
    foreignTaxCreditReliefQuestion = Some(true),
    taxableAmount = Some(2000.00)
  )

  "isFinished" should {
    "return true" when {
      "all questions are populated" in {
        PensionScheme(
          alphaThreeCode = None,
          alphaTwoCode = Some("DE"),
          pensionPaymentAmount = Some(2000.00),
          pensionPaymentTaxPaid = Some(400.00),
          specialWithholdingTaxQuestion = Some(true),
          specialWithholdingTaxAmount = Some(400.00),
          foreignTaxCreditReliefQuestion = Some(true),
          taxableAmount = Some(2000.00)
        ).isFinished
      }
      "all required questions are answered" in {
        scheme.isFinished shouldBe true
        scheme.copy(taxableAmount = None).isFinished shouldBe false
      }
    }

    "return false" when {
      "not all necessary questions have been populated" in {
        PensionScheme(
          alphaThreeCode = None,
          alphaTwoCode = Some("DE"),
          pensionPaymentAmount = Some(2000.00),
          pensionPaymentTaxPaid = Some(400.00),
          specialWithholdingTaxQuestion = Some(true),
          specialWithholdingTaxAmount = None,
          foreignTaxCreditReliefQuestion = Some(false),
          taxableAmount = None
        ).isFinished shouldBe false
        PensionScheme(
          alphaThreeCode = None,
          alphaTwoCode = Some("DE"),
          pensionPaymentAmount = None,
          pensionPaymentTaxPaid = Some(400.00),
          specialWithholdingTaxQuestion = Some(true),
          specialWithholdingTaxAmount = Some(20),
          foreignTaxCreditReliefQuestion = Some(false),
          taxableAmount = None
        ).isFinished shouldBe false
      }
    }
  }

  "updateFTCR" should {
    val existingScheme = scheme.copy(foreignTaxCreditReliefQuestion = Some(true))

    "return old taxable amount when FTCR did not change" in {
      assert(scheme.updateFTCR(true) === existingScheme)
    }

    "return updated ftcr and taxable amount set to None for FTCR changing from true to false" in {
      assert(
        scheme.updateFTCR(false) ===
          existingScheme.copy(foreignTaxCreditReliefQuestion = Some(false), taxableAmount = None)
      )
    }

    "return updated ftcr and newly calculated taxable amount for FTCR changing from false to true" in {
      assert(
        scheme
          .copy(foreignTaxCreditReliefQuestion = Some(false))
          .updateFTCR(true) ===
          existingScheme.copy(foreignTaxCreditReliefQuestion = Some(true), taxableAmount = Some(BigDecimal("2000.00")))
      )
    }
  }

  "updatePensionPayment" should {
    "update pension payments and reflect this change for taxable amount for FTCR=true" in {
      val existingScheme = scheme.copy(pensionPaymentAmount = Some(2000.00), pensionPaymentTaxPaid = Some(400.00))
      assert(
        existingScheme.updatePensionPayment(Some(3000.00), Some(600.00)) ===
          existingScheme.copy(
            pensionPaymentAmount = Some(3000.00),
            pensionPaymentTaxPaid = Some(600.00),
            taxableAmount = Some(3000.00)
          )
      )
    }

    "update pension payments and reset taxable amount to None for FTCR=false" in {
      val existingScheme = scheme.copy(
        pensionPaymentAmount = Some(2000.00),
        pensionPaymentTaxPaid = Some(400.00),
        foreignTaxCreditReliefQuestion = Some(false)
      )
      assert(
        existingScheme.updatePensionPayment(Some(3000.00), Some(600.00)) ===
          existingScheme.copy(
            pensionPaymentAmount = Some(3000.00),
            pensionPaymentTaxPaid = Some(600.00),
            taxableAmount = None
          )
      )
    }
  }

  "calcTaxableAmount" should {
    val cases = Table(
      ("pensionPaymentAmount", "pensionPaymentTaxPaid", "foreignTaxCreditReliefQuestion", "expectedTaxableAmount"),
      (None, None, None, None),
      (Some(BigDecimal("500")), None, None, None),
      (None, Some(BigDecimal("500")), None, None),
      (None, None, Some(true), None),
      (Some(BigDecimal("500")), Some(BigDecimal("100")), Some(true), Some(BigDecimal("500"))),
      (Some(BigDecimal("500")), Some(BigDecimal("100")), Some(false), Some(BigDecimal("400")))
    )

    "return correct taxable amount" in forAll(cases) {
      case (pensionPaymentAmount, pensionPaymentTaxPaid, foreignTaxCreditReliefQuestion, expectedTaxableAmount) =>
        assert(PensionScheme.calcTaxableAmount(pensionPaymentAmount, pensionPaymentTaxPaid, foreignTaxCreditReliefQuestion) === expectedTaxableAmount)
    }
  }
}
