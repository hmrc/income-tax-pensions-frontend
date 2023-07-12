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

package controllers.helperSpecs

import controllers.pensions.paymentsIntoOverseasPensions.ReliefsSchemeDetailsHelper
import models.pension.charges.{Relief, TaxReliefQuestion}
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.i18n.Lang
import play.i18n.{Langs, Messages}
import play.test.Helpers.stubMessagesApi
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ActionItem, SummaryListRow, Value}

class ReliefsSchemeDetailsHelperSpec extends AnyWordSpec with Matchers {

  implicit val messages: Messages = stubbedMessages()

  val taxYear = 2022
  val index = Some(1)

  val reliefTransactional: Relief = Relief(
    Some("PENSIONINCOME245"), Some(193.54), Some(TaxReliefQuestion.TransitionalCorrespondingRelief), sf74Reference = Some("123456"))

  val reliefMigrant: Relief = Relief(
    Some("PENSIONINCOME245"), Some(193.54), Some(TaxReliefQuestion.MigrantMemberRelief), qopsReference = Some("123456")
  )

  val reliefDouble: Relief = Relief(
    Some("PENSIONINCOME245"),
    Some(193.54),
    Some(TaxReliefQuestion.DoubleTaxationRelief),
    alphaTwoCountryCode = Some("Germany"),
    doubleTaxationArticle = Some("AB3211-1"),
    doubleTaxationTreaty = Some("Munich"),
    doubleTaxationReliefAmount = Some(123.45)
  )

  val reliefNone: Relief = Relief(None,
    None,
    reliefType = Some(TaxReliefQuestion.NoTaxRelief))

  "Getting the summary rows" should {
    "return the expected" when {
      "we have provided all answers for transitional corresponding relief" in {
        val summaryListRows = ReliefsSchemeDetailsHelper.summaryListRows(reliefTransactional, taxYear, index)

        summaryListRows.length shouldBe 4
        assertRowForSchemeName(summaryListRows.head, "PENSIONINCOME245")
        assertRowForUntaxedEmployerPayments(summaryListRows(1), "£193.54")
        assertRowForReliefType(summaryListRows(2), "Transitional corresponding relief")
        assertRowForSf74SchemeDetails(summaryListRows(3), "123456")
      }
    }
    "we have provided all answers for migration relief" in {
      val summaryListRows = ReliefsSchemeDetailsHelper.summaryListRows(reliefMigrant, taxYear, index)

      summaryListRows.length shouldBe 4
      assertRowForSchemeName(summaryListRows.head, "PENSIONINCOME245")
      assertRowForUntaxedEmployerPayments(summaryListRows(1), "£193.54")
      assertRowForReliefType(summaryListRows(2), "Migrant member relief")
      assertRowForQOPSSchemeDetails(summaryListRows(3), "123456")
    }
    "we have provided all answers for double taxation relief" in {
      val summaryListRows = ReliefsSchemeDetailsHelper.summaryListRows(reliefDouble, taxYear, index)

      summaryListRows.length shouldBe 4
      assertRowForSchemeName(summaryListRows.head, "PENSIONINCOME245")
      assertRowForUntaxedEmployerPayments(summaryListRows(1), "£193.54")
      assertRowForReliefType(summaryListRows(2), "Double taxation relief")
      assertRowForMigrationSchemeDetails(summaryListRows(3), "Country code: Germany<br>Article:  AB3211-1<br>Treaty: Munich<br>Relief: £123.45")
    }

    "we have provided none of the above" in {
      val summaryListRows = ReliefsSchemeDetailsHelper.summaryListRows(reliefNone, taxYear, index)

      summaryListRows.length shouldBe 3
      assertRowForSchemeName(summaryListRows.head, "No")
      assertRowForUntaxedEmployerPayments(summaryListRows(1), "No")
      assertRowForReliefType(summaryListRows(2), "No tax relief")
    }
  }

  private def assertRowForSchemeName(summaryListRow: SummaryListRow, expectedValue: String): Unit = {
    val addOn = if (index.isDefined) s"?index=${index.get}" else ""
    assertSummaryListRow(summaryListRow, ExpectedSummaryRowContents(
      "Pension scheme name",
      expectedValue,
      "Change",
      s"/2022/overseas-pensions/payments-into-overseas-pensions/pensions-customer-reference-number$addOn",
      messages("overseasPension.reliefDetails.pensionSchemeName.hidden")))
  }

  private def assertRowForUntaxedEmployerPayments(summaryListRow: SummaryListRow, expectedValue: String): Unit = {
    assertSummaryListRow(summaryListRow, ExpectedSummaryRowContents(
      "Untaxed employer payments",
      expectedValue,
      "Change",
      "/2022/overseas-pensions/payments-into-overseas-pensions/untaxed-employer-payments?index=1",
      messages("overseasPension.reliefDetails.amount.hidden")))
  }

  private def assertRowForReliefType(summaryListRow: SummaryListRow, expectedValue: String): Unit = {
    assertSummaryListRow(summaryListRow, ExpectedSummaryRowContents(
      "Type of relief",
      expectedValue,
      "Change",
      "/2022/overseas-pensions/payments-into-overseas-pensions/pensions-overseas-emp-relief-status?reliefIndex=1",
      messages("overseasPension.reliefDetails.typeOfRelief.hidden")))
  }

  private def assertRowForQOPSSchemeDetails(summaryListRow: SummaryListRow, expectedValue: String): Unit = {
    assertSummaryListRow(summaryListRow, ExpectedSummaryRowContents(
      "Scheme details",
      expectedValue,
      "Change",
      "/2022/overseas-pensions/payments-into-overseas-pensions/qualifying-overseas-pension-scheme-reference-number?index=1",
      messages("overseasPension.reliefDetails.schemeDetail.hidden")))
  }

  private def assertRowForSf74SchemeDetails(summaryListRow: SummaryListRow, expectedValue: String): Unit = {
    assertSummaryListRow(summaryListRow, ExpectedSummaryRowContents(
      "Scheme details",
      expectedValue,
      "Change",
      "/2022/overseas-pensions/payments-into-overseas-pensions/pensions-overseas-sf74?reliefIndex=1",
      messages("overseasPension.reliefDetails.schemeDetail.hidden")))
  }

  private def assertRowForMigrationSchemeDetails(summaryListRow: SummaryListRow, expectedValue: String): Unit = {
    assertSummaryListRow(summaryListRow, ExpectedSummaryRowContents(
      "Scheme details",
      expectedValue,
      "Change",
      "/2022/overseas-pensions/payments-into-overseas-pensions/double-taxation-agreement-details?index=1",
      messages("overseasPension.reliefDetails.schemeDetail.hidden")))
  }

  private def assertSummaryListRow(summaryListRow: SummaryListRow, expectedSummaryRowContents: ExpectedSummaryRowContents): Unit = {
    assertLabel(summaryListRow, expectedSummaryRowContents.label)
    assertValue(summaryListRow, expectedSummaryRowContents.value)
    assertAction(summaryListRow, expectedSummaryRowContents.linkLabel, expectedSummaryRowContents.linkPathEnding, expectedSummaryRowContents.hiddenText)
  }

  private def assertAction(summaryListRow: SummaryListRow, expectedLabel: String, expectedPath: String, expectedHiddenText: String): Unit = {

    summaryListRow.actions shouldBe defined
    val actionsForFirstSummaryRow = summaryListRow.actions.get
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
      summaryListRow.key.content shouldBe HtmlContent(expectedLabel)
    }
  }

  private def assertValue(summaryListRow: SummaryListRow, expectedValue: String): Assertion = {
    withClue(s"We had expected the value to be '$expectedValue':") {
      summaryListRow.value shouldBe Value(HtmlContent(expectedValue), "govuk-!-width-one-third")
    }
  }

  private def stubbedMessages() = {
    import scala.jdk.CollectionConverters._

    val messagesApi = stubMessagesApi(
      Map(
        Lang.defaultLang.code ->
          Map(
            "common.no" -> "No",
            "common.change" -> "Change",
            "overseasPension.reliefDetails.pensionSchemeName" -> "Pension scheme name",
            "overseasPension.reliefDetails.amount" -> "Untaxed employer payments",
            "overseasPension.reliefDetails.typeOfRelief" -> "Type of relief",
            "overseasPension.reliefDetails.schemeDetail" -> "Scheme details",
            "overseasPension.reliefDetails.countryCode" -> "Country code:",
            "overseasPension.reliefDetails.article" -> "Article:",
            "overseasPension.reliefDetails.treaty" -> "Treaty:",
            "overseasPension.reliefDetails.relief" -> "Relief:",
            "overseasPension.reliefDetails.pensionSchemeName.hidden" -> "Change pension scheme name",
            "overseasPension.reliefDetails.amount.hidden" -> "Change untaxed employer payments",
            "overseasPension.reliefDetails.typeOfRelief.hidden" -> "Change type of relief",
            "overseasPension.reliefDetails.schemeDetail.hidden" -> "Change scheme details",
            "overseasPension.pensionReliefType.TCR" -> "Transitional corresponding relief",
            "overseasPension.pensionReliefType.MMR" -> "Migrant member relief",
            "overseasPension.pensionReliefType.DTR" -> "Double taxation relief",
            "overseasPension.reliefDetails.noTaxRelief" -> "No tax relief"
          ).asJava
      ).asJava,
      new Langs(new play.api.i18n.DefaultLangs()))
    messagesApi.preferred(new Langs(new play.api.i18n.DefaultLangs()).availables())
  }

  private case class ExpectedSummaryRowContents(label: String, value: String, linkLabel: String, linkPathEnding: String, hiddenText: String)
}


