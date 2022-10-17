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

package controllers.pensions.lifetimeAllowance

import models.pension.charges.{LifetimeAllowance, PensionAnnualAllowancesViewModel, PensionLifetimeAllowancesViewModel, UnauthorisedPaymentsViewModel}
import org.scalatest.Assertion
import org.scalatest.matchers.should._
import org.scalatest.wordspec.AnyWordSpec
import play.api.i18n.Lang
import play.i18n.{Langs, Messages}
import play.test.Helpers.stubMessagesApi
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ActionItem, Key, SummaryListRow, Value}

class AnnualAndLifetimeAllowanceCYAViewHelperTest extends AnyWordSpec with Matchers {

  val taxYear = 2022

  implicit val messages: Messages = stubbedMessages()
  "Getting the summary rows" should {
    "return the expected" when {
      "you have selected yes for 'reduced annual allowance question'" in {
        val annualModel = PensionAnnualAllowancesViewModel(
          reducedAnnualAllowanceQuestion = Some(true),
          moneyPurchaseAnnualAllowance = None,
          taperedAnnualAllowance = None,
          aboveAnnualAllowanceQuestion = None,
          aboveAnnualAllowance = None,
          pensionProvidePaidAnnualAllowanceQuestion = None,
          taxPaidByPensionProvider = None,
          pensionSchemeTaxReferences = None
        )
        val lifetimeModel = PensionLifetimeAllowancesViewModel(
          aboveLifetimeAllowanceQuestion = Some(true)
        )

        val summaryListRows = AnnualAndLifetimeAllowanceCYAViewHelper.summaryListRows(annualModel, lifetimeModel, taxYear)

        summaryListRows.length shouldBe 7
        assertRowForAboveAnnualOrLifeTimeAllowance(summaryListRows.head, "Yes")
        assertRowReducedAnnualAllowance(summaryListRows(1), "Yes")
        assertRowTypeOfReducedAnnualAllowance(summaryListRows(2), "")
        assertRowAboveAnnualAllowance(summaryListRows(3), "")
        assertRowAboveLifetimeAllowance(summaryListRows(4), "")
      }
      "you have selected no for 'reduced annual allowance question'" in {
        val annualModel = PensionAnnualAllowancesViewModel(
          reducedAnnualAllowanceQuestion = Some(false),
          moneyPurchaseAnnualAllowance = None,
          taperedAnnualAllowance = None,
          aboveAnnualAllowanceQuestion = None,
          aboveAnnualAllowance = None,
          pensionProvidePaidAnnualAllowanceQuestion = None,
          taxPaidByPensionProvider = None,
          pensionSchemeTaxReferences = None
        )
        val lifetimeModel = PensionLifetimeAllowancesViewModel(
          aboveLifetimeAllowanceQuestion = Some(true)
        )

        val summaryListRows = AnnualAndLifetimeAllowanceCYAViewHelper.summaryListRows(annualModel, lifetimeModel, taxYear)

        summaryListRows.length shouldBe 5
        assertRowForAboveAnnualOrLifeTimeAllowance(summaryListRows.head, "Yes")
        assertRowReducedAnnualAllowance(summaryListRows(1), "No")
        assertRowAboveLifetimeAllowance(summaryListRows(2), "")
      }
      "you have selected yes and an amount for 'above annual allowance question" in {
        val annualModel = PensionAnnualAllowancesViewModel(
          reducedAnnualAllowanceQuestion = Some(true),
          moneyPurchaseAnnualAllowance = None,
          taperedAnnualAllowance = None,
          aboveAnnualAllowanceQuestion = Some(true),
          aboveAnnualAllowance = Some(BigDecimal("1000.00")),
          pensionProvidePaidAnnualAllowanceQuestion = None,
          taxPaidByPensionProvider = None,
          pensionSchemeTaxReferences = None
        )
        val lifetimeModel = PensionLifetimeAllowancesViewModel(
          aboveLifetimeAllowanceQuestion = Some(true)
        )

        val summaryListRows = AnnualAndLifetimeAllowanceCYAViewHelper.summaryListRows(annualModel, lifetimeModel, taxYear)

        summaryListRows.length shouldBe 8
        assertRowForAboveAnnualOrLifeTimeAllowance(summaryListRows.head, "Yes")
        assertRowReducedAnnualAllowance(summaryListRows(1), "Yes")
        assertRowTypeOfReducedAnnualAllowance(summaryListRows(2), "")
        assertRowAboveAnnualAllowance(summaryListRows(3), "£1000.00")
        assertRowAnnualAllowanceTax(summaryListRows(4), "")
        assertRowAboveLifetimeAllowance(summaryListRows(5), "")
      }
      "you have selected no for 'above annual allowance question" in {
        val annualModel = PensionAnnualAllowancesViewModel(
          reducedAnnualAllowanceQuestion = Some(true),
          moneyPurchaseAnnualAllowance = None,
          taperedAnnualAllowance = None,
          aboveAnnualAllowanceQuestion = Some(false),
          aboveAnnualAllowance = None,
          pensionProvidePaidAnnualAllowanceQuestion = None,
          taxPaidByPensionProvider = None,
          pensionSchemeTaxReferences = None
        )
        val lifetimeModel = PensionLifetimeAllowancesViewModel(
          aboveLifetimeAllowanceQuestion = Some(true)
        )

        val summaryListRows = AnnualAndLifetimeAllowanceCYAViewHelper.summaryListRows(annualModel, lifetimeModel, taxYear)

        summaryListRows.length shouldBe 7
        assertRowForAboveAnnualOrLifeTimeAllowance(summaryListRows.head, "Yes")
        assertRowReducedAnnualAllowance(summaryListRows(1), "Yes")
        assertRowTypeOfReducedAnnualAllowance(summaryListRows(2), "")
        assertRowAboveAnnualAllowance(summaryListRows(3), "No")
        assertRowAboveLifetimeAllowance(summaryListRows(4), "")
      }
      "you have selected yes and an amount 'annual allowance tax" in {
        val annualModel = PensionAnnualAllowancesViewModel(
          reducedAnnualAllowanceQuestion = Some(true),
          moneyPurchaseAnnualAllowance = None,
          taperedAnnualAllowance = None,
          aboveAnnualAllowanceQuestion = Some(true),
          aboveAnnualAllowance = Some(BigDecimal("1000.00")),
          pensionProvidePaidAnnualAllowanceQuestion = Some(true),
          taxPaidByPensionProvider = Some(BigDecimal("500.00")),
          pensionSchemeTaxReferences = None
        )
        val lifetimeModel = PensionLifetimeAllowancesViewModel(
          aboveLifetimeAllowanceQuestion = Some(true)
        )

        val summaryListRows = AnnualAndLifetimeAllowanceCYAViewHelper.summaryListRows(annualModel, lifetimeModel, taxYear)

        summaryListRows.length shouldBe 9
        assertRowForAboveAnnualOrLifeTimeAllowance(summaryListRows.head, "Yes")
        assertRowReducedAnnualAllowance(summaryListRows(1), "Yes")
        assertRowTypeOfReducedAnnualAllowance(summaryListRows(2), "")
        assertRowAboveAnnualAllowance(summaryListRows(3), "£1000.00")
        assertRowAnnualAllowanceTax(summaryListRows(4), "£500.00")
        assertRowAnnualAllowanceSchemes(summaryListRows(5), "")
        assertRowAboveLifetimeAllowance(summaryListRows(6), "")
      }
      "you have selected no for 'annual allowance tax" in {
        val annualModel = PensionAnnualAllowancesViewModel(
          reducedAnnualAllowanceQuestion = Some(true),
          moneyPurchaseAnnualAllowance = None,
          taperedAnnualAllowance = None,
          aboveAnnualAllowanceQuestion = Some(true),
          aboveAnnualAllowance = Some(BigDecimal("1000.00")),
          pensionProvidePaidAnnualAllowanceQuestion = Some(false),
          taxPaidByPensionProvider = None,
          pensionSchemeTaxReferences = None
        )
        val lifetimeModel = PensionLifetimeAllowancesViewModel(
          aboveLifetimeAllowanceQuestion = Some(true)
        )

        val summaryListRows = AnnualAndLifetimeAllowanceCYAViewHelper.summaryListRows(annualModel, lifetimeModel, taxYear)

        summaryListRows.length shouldBe 8
        assertRowForAboveAnnualOrLifeTimeAllowance(summaryListRows.head, "Yes")
        assertRowReducedAnnualAllowance(summaryListRows(1), "Yes")
        assertRowTypeOfReducedAnnualAllowance(summaryListRows(2), "")
        assertRowAboveAnnualAllowance(summaryListRows(3), "£1000.00")
        assertRowAnnualAllowanceTax(summaryListRows(4), "No")
        assertRowAboveLifetimeAllowance(summaryListRows(5), "")
      }
      "we haven't provided any answers" in {
        val annualModel = PensionAnnualAllowancesViewModel()
        val lifetimeModel = PensionLifetimeAllowancesViewModel()

        val summaryListRows = AnnualAndLifetimeAllowanceCYAViewHelper.summaryListRows(annualModel, lifetimeModel, taxYear)

        summaryListRows.length shouldBe 1
        assertRowForAboveAnnualOrLifeTimeAllowance(summaryListRows.head, "")
      }
      "we have only completed the 'annual allowance' section" in {
        val annualModel = PensionAnnualAllowancesViewModel(
          reducedAnnualAllowanceQuestion = Some(true),
          moneyPurchaseAnnualAllowance = Some(true),
          taperedAnnualAllowance = Some(false),
          aboveAnnualAllowanceQuestion = Some(true),
          aboveAnnualAllowance = Option(BigDecimal("1000.00")),
          pensionProvidePaidAnnualAllowanceQuestion = Some(true),
          taxPaidByPensionProvider = Option(BigDecimal("120.00")),
          pensionSchemeTaxReferences = Some(Seq("12345678RX", "12345678RY"))
        )
        val lifetimeModel = PensionLifetimeAllowancesViewModel(
          aboveLifetimeAllowanceQuestion = Some(true)
        )

        val summaryListRows = AnnualAndLifetimeAllowanceCYAViewHelper.summaryListRows(annualModel, lifetimeModel, taxYear)

        summaryListRows.length shouldBe 9
        assertRowForAboveAnnualOrLifeTimeAllowance(summaryListRows.head, "Yes")
        assertRowReducedAnnualAllowance(summaryListRows(1), "Yes")
        assertRowTypeOfReducedAnnualAllowance(summaryListRows(2), "Money purchase")
        assertRowAboveAnnualAllowance(summaryListRows(3), "£1000.00")
        assertRowAnnualAllowanceTax(summaryListRows(4), "£120.00")
        assertRowAnnualAllowanceSchemes(summaryListRows(5), "12345678RX, 12345678RY")
        assertRowAboveLifetimeAllowance(summaryListRows(6), "")
      }
      "our data for the 'annual allowance' section has got into an unrealistic state" in {
        val annualModel = PensionAnnualAllowancesViewModel(
          reducedAnnualAllowanceQuestion = Some(true),
          moneyPurchaseAnnualAllowance = Some(true),
          taperedAnnualAllowance = Some(false),
          aboveAnnualAllowanceQuestion = Some(true),
          aboveAnnualAllowance = Option(BigDecimal("1000.00")),
          pensionProvidePaidAnnualAllowanceQuestion = Some(true),
          taxPaidByPensionProvider = Option(BigDecimal("120.00")),
          pensionSchemeTaxReferences = Some(Seq("12345678RX", "12345678RY"))
        )
        val lifetimeModel = PensionLifetimeAllowancesViewModel(
          aboveLifetimeAllowanceQuestion = Some(false)
        )

        val summaryListRows = AnnualAndLifetimeAllowanceCYAViewHelper.summaryListRows(annualModel, lifetimeModel, taxYear)

        summaryListRows.length shouldBe 1
        assertRowForAboveAnnualOrLifeTimeAllowance(summaryListRows.head, "No")
      }
      // TODO add lifetime allowance tests when implementing //SASS-3549
    }
  }

  private def assertRowForAboveAnnualOrLifeTimeAllowance(summaryListRow: SummaryListRow, expectedValue: String): Unit = {
    assertSummaryListRow(summaryListRow, ExpectedSummaryRowContents(
      "Above annual or lifetime allowance",
      expectedValue,
      "Change",
      "/2022/annual-lifetime-allowances/above-annual-allowance",
      "lifetimeAllowance.cya.aboveAnnualOrLifetimeAllowance.hidden"))
  }

  private def assertRowReducedAnnualAllowance(summaryListRow: SummaryListRow, expectedValue: String): Unit = {
    assertSummaryListRow(summaryListRow, ExpectedSummaryRowContents(
      "Reduced annual allowance",
      expectedValue,
      "Change",
      "/2022/annual-allowance/reduced-annual-allowance",
      "lifetimeAllowance.cya.reducedAnnualAllowance.hidden"))
  }

  private def assertRowTypeOfReducedAnnualAllowance(summaryListRow: SummaryListRow, expectedValue: String): Unit = {
    assertSummaryListRow(summaryListRow, ExpectedSummaryRowContents(
      "Type of reduced annual allowance",
      expectedValue,
      "Change",
      "/2022/annual-allowance/reduced-annual-allowance-type",
      "lifetimeAllowance.cya.typeOfReducedAnnualAllowance.hidden"))
  }

  private def assertRowAboveAnnualAllowance(summaryListRow: SummaryListRow, expectedValue: String): Unit = {
    assertSummaryListRow(summaryListRow, ExpectedSummaryRowContents(
      "Above annual allowance",
      expectedValue,
      "Change",
      "/2022/annual-allowance/above-annual-allowance",
      "lifetimeAllowance.cya.aboveAnnualAllowance.hidden"))
  }

  private def assertRowAnnualAllowanceTax(summaryListRow: SummaryListRow, expectedValue: String): Unit = {
    assertSummaryListRow(summaryListRow, ExpectedSummaryRowContents(
      "Annual allowance tax",
      expectedValue,
      "Change",
      "/2022/annual-lifetime-allowances/pension-provider-paid-tax",
      "lifetimeAllowance.cya.annualAllowanceTax.hidden"))
  }

  private def assertRowAnnualAllowanceSchemes(summaryListRow: SummaryListRow, expectedValue: String): Unit = {
    assertSummaryListRow(summaryListRow, ExpectedSummaryRowContents(
      "Schemes paying annual allowance tax",
      expectedValue,
      "Change",
      "/2022/annual-allowance/pension-scheme-tax-reference-summary",
      "lifetimeAllowance.cya.annualPensionSchemeTaxReferences.hidden"))
  }

  private def assertRowAboveLifetimeAllowance(summaryListRow: SummaryListRow, expectedValue: String): Unit = {
    assertSummaryListRow(summaryListRow, ExpectedSummaryRowContents(
      "Above lifetime allowance",
      expectedValue,
      "Change",
      "/2022/annual-lifetime-allowances/check-annual-and-lifetime-allowances",
      "lifetimeAllowance.cya.aboveLifetimeAllowance.hidden"))
  }


  private def assertRowLumpSum(summaryListRow: SummaryListRow, expectedValue: String): Unit = {
    assertSummaryListRow(summaryListRow, ExpectedSummaryRowContents(
      "Lump sum",
      expectedValue,
      "Change",
      "/2022/annual-lifetime-allowances/lifetime-allowance-lump-sum",
      "lifetimeAllowance.cya.lumpSum.hidden"))
  }

  private def assertRowOtherPayments(summaryListRow: SummaryListRow, expectedValue: String): Unit = {
    assertSummaryListRow(summaryListRow, ExpectedSummaryRowContents(
      "Other payments",
      expectedValue,
      "Change",
      "/2022/annual-lifetime-allowances/lifetime-allowance-another-way",
      "lifetimeAllowance.cya.otherPayments.hidden"))
  }

  private def assertRowLifetimeAllowanceSchemes(summaryListRow: SummaryListRow, expectedValue: String): Unit = {
    assertSummaryListRow(summaryListRow, ExpectedSummaryRowContents(
      "Schemes paying lifetime allowance tax",
      expectedValue,
      "Change",
      "/2022/annual-lifetime-allowances/check-annual-and-lifetime-allowances",
      "lifetimeAllowance.cya.lifetimePensionSchemeTaxReferences.hidden"))
  }

  private def assertSummaryListRow(summaryListRow: SummaryListRow, expectedSummaryRowContents: ExpectedSummaryRowContents): Unit = {
    assertLabel(summaryListRow, expectedSummaryRowContents.label)
    assertValue(summaryListRow, expectedSummaryRowContents.value)
    assertAction(summaryListRow, expectedSummaryRowContents.linkLabel, expectedSummaryRowContents.linkPathEnding, expectedSummaryRowContents.hiddenText)
  }

  private def assertAction(summaryListRow: SummaryListRow, expectedLabel: String, expectedPath: String, expectedHiddenText: String): Unit = {

    summaryListRow.actions shouldBe defined
    val actionsForFirstSummaryRow = summaryListRow.actions.get
    actionsForFirstSummaryRow.items.length shouldBe 1
    val firstAction: ActionItem = actionsForFirstSummaryRow.items.head
    firstAction.content shouldBe HtmlContent("<span aria-hidden=\"true\">" + expectedLabel + "</span>")
    withClue(s"We had expected the link path to end with '$expectedPath':") {
      firstAction.href should endWith(expectedPath)
    }
    firstAction.visuallyHiddenText shouldBe defined
    firstAction.visuallyHiddenText.get shouldBe expectedHiddenText
  }

  private def assertLabel(summaryListRow: SummaryListRow, expectedLabel: String) = {
    withClue(s"We had expected the label to be '$expectedLabel':") {
      summaryListRow.key shouldBe Key(HtmlContent(expectedLabel), "govuk-!-width-one-third")
    }
  }

  private def assertValue(summaryListRow: SummaryListRow, expectedValue: String): Assertion = {
    withClue(s"We had expected the value to be '$expectedValue':") {
      summaryListRow.value shouldBe Value(HtmlContent(expectedValue), "govuk-!-width-one-third")
    }
  }

  private def stubbedMessages() = {
    import collection.JavaConverters._

    val messagesApi = stubMessagesApi(
      Map(
        Lang.defaultLang.code ->
          Map(
            "lifetimeAllowance.cya.heading" -> "Check your annual and lifetime allowances",
            "common.yes" -> "Yes",
            "common.no" -> "No",
            "common.change" -> "Change",
            "lifetimeAllowance.cya.aboveAnnualOrLifetimeAllowance" -> "Above annual or lifetime allowance",
            "lifetimeAllowance.cya.reducedAnnualAllowance" -> "Reduced annual allowance",
            "lifetimeAllowance.cya.typeOfReducedAnnualAllowance" -> "Type of reduced annual allowance",
            "lifetimeAllowance.cya.aboveAnnualAllowance" -> "Above annual allowance",
            "lifetimeAllowance.cya.annualAllowanceTax" -> "Annual allowance tax",
            "lifetimeAllowance.cya.annualPensionSchemeTaxReferences" -> "Schemes paying annual allowance tax",
            "lifetimeAllowance.cya.aboveLifetimeAllowance" -> "Above lifetime allowance",
            "lifetimeAllowance.cya.lumpSum" -> "Lump sum",
            "lifetimeAllowance.cya.otherPayments" -> "Other payments",
            "lifetimeAllowance.cya.lifetimePensionSchemeTaxReferences" -> "Schemes paying lifetime allowance tax",
          )
            .asJava
      ).asJava,
      new Langs(new play.api.i18n.DefaultLangs()))
    messagesApi.preferred(new Langs(new play.api.i18n.DefaultLangs()).availables())
  }

  private case class ExpectedSummaryRowContents(label: String, value: String, linkLabel: String, linkPathEnding: String, hiddenText: String)

}
