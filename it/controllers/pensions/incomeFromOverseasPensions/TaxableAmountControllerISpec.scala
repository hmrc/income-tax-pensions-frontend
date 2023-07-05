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

package controllers.pensions.incomeFromOverseasPensions

import builders.IncomeFromOverseasPensionsViewModelBuilder.{anIncomeFromOverseasPensionsViewModel, anIncomeFromOverseasPensionsWithFalseFtcrValueViewModel}
import builders.PensionsUserDataBuilder.{aPensionsUserData, anPensionsUserDataEmptyCya, pensionUserDataWithIncomeOverseasPension}
import models.pension.charges.PensionScheme
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.IncomeFromOverseasPensionsPages.{countrySummaryListControllerUrl, incomeFromOverseasPensionsStatus, overseasPensionsSchemeSummaryUrl}
import utils.PageUrls.{IncomeFromOverseasPensionsPages, overseasPensionsSummaryUrl}
import utils.{CommonUtils, PensionsDatabaseHelper}

import java.text.NumberFormat
import java.util.Locale

class TaxableAmountControllerISpec extends
  CommonUtils with
  BeforeAndAfterEach with
  PensionsDatabaseHelper {

  object Selectors {
    val captionSelector: String = "#main-content > div > div > header > p"
    val formSelector: String = "#main-content > div > div > form"
    val continueButtonSelector: String = "#continue"

    val tableCaptionSelector: String = s"#main-content > div > div > table > caption"
    val tableHeadSelector: (Int, Int) => String = (row, column) =>
      s"#main-content > div > div > table > thead > tr:nth-child($row) > th:nth-of-type($column)"
    val tableSelector: (Int, Int) => String = (row, column) =>
      s"#main-content > div > div > table > tbody > tr:nth-child($row) > td:nth-of-type($column)"
    val tableRowHeadSelector: (Int, Int) => String = (row, column) =>
      s"#main-content > div > div > table > tbody > tr:nth-child($row) > th:nth-of-type($column)"

    val labelSelector: Int => String = index => s"form > div:nth-of-type($index) > label"
    val paragraphSelector: Int => String = index => s"#main-content > div > div > p:nth-of-type($index)"
    val paraItemSelector: Int => String = index => s"#main-content > div > div > ul > li:nth-of-type($index)"

  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedTitle: String
    val expectedHeading: String
    val expectedParagraph: String
    val expectedFtcrParagraph: String
    val expectedFtcrParaItem1: String
    val expectedFtcrParaItem2: String
    val expectedTableCaption: String
    val expectedTableHeader1: String
    val expectedTableHeader2: String
    val expectedRowHeading1: String
    val expectedRowHeading2: String
    val expectedRowHeading3: String
    val expectedButtonText: String
  }

  trait SpecificExpectedResults {
    val expectedError: String
  }

  object CommonExpectedIndividualEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Income from overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedTitle: String = "Your taxable amount"
    val expectedHeading: String = "Your taxable amount"
    val expectedParagraph: String = "Your taxable amount is the amount you got in foreign pension payments."
    val expectedFtcrParagraph: String = "Your taxable amount is:"
    val expectedFtcrParaItem1: String = "the amount you got in foreign pension payments"
    val expectedFtcrParaItem2: String = "minus any non-UK tax you paid"
    val expectedTableCaption: String = "Your taxable amount calculation"
    val expectedTableHeader1: String = "Item"
    val expectedTableHeader2: String = "Amount"
    val expectedRowHeading1: String = "Foreign pension payments"
    val expectedRowHeading2: String = "Non-UK tax deducted"
    val expectedRowHeading3: String = "Taxable amount"
    val expectedButtonText: String = "Continue"
  }

  object CommonExpectedAgentEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Income from overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedTitle: String = "Your client’s taxable amount"
    val expectedHeading: String = "Your client’s taxable amount"
    val expectedParagraph: String = "Your client’s taxable amount is the amount they got in foreign pension payments."
    val expectedFtcrParagraph: String = "Your client’s taxable amount is:"
    val expectedFtcrParaItem1: String = "the amount your client got in foreign pension payments"
    val expectedFtcrParaItem2: String = "minus any non-UK tax they paid"
    val expectedTableCaption: String = "Your client’s taxable amount calculation"
    val expectedTableHeader1: String = "Item"
    val expectedTableHeader2: String = "Amount"
    val expectedRowHeading1: String = "Foreign pension payments"
    val expectedRowHeading2: String = "Non-UK tax deducted"
    val expectedRowHeading3: String = "Taxable amount"
    val expectedButtonText: String = "Continue"
  }

  object CommonExpectedIndividualCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Incwm o bensiynau tramor ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val expectedTitle: String = "Eich cyfanswm trethadwy"
    val expectedHeading: String = "Eich cyfanswm trethadwy"
    val expectedParagraph: String = "Eich swm trethadwy yw’r swm a gawsoch mewn taliadau pensiwn tramor."
    val expectedFtcrParagraph: String = "Eich swm trethadwy yw:"
    val expectedFtcrParaItem1: String = "y swm a gawsoch mewn taliadau pensiwn tramor"
    val expectedFtcrParaItem2: String = "llai unrhyw dreth a dalwyd gennych y tu allan i’r DU"
    val expectedTableCaption: String = "Cyfrifiad eich swm trethadwy"
    val expectedTableHeader1: String = "Eitem"
    val expectedTableHeader2: String = "Swm"
    val expectedRowHeading1: String = "Taliadau pensiwn tramor"
    val expectedRowHeading3: String = "Swm trethadwy"
    val expectedRowHeading2: String = "Treth a dalwyd y tu allan i’r DU y didynnwyd"
    val expectedButtonText: String = "Yn eich blaen"
  }

  object CommonExpectedAgentCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Incwm o bensiynau tramor ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val expectedTitle: String = "Swm trethadwy eich cleient"
    val expectedHeading: String = "Swm trethadwy eich cleient"
    val expectedParagraph: String = "Swm trethadwy eich cleient yw’r swm a gafodd eich cleient mewn taliadau pensiwn tramor."
    val expectedFtcrParagraph: String = "Swm trethadwy eich cleient yw:"
    val expectedFtcrParaItem1: String = "y swm a gafodd eich cleient mewn taliadau pensiwn tramor"
    val expectedFtcrParaItem2: String = "llai unrhyw dreth a dalwyd gan eich cleient y tu allan i’r DU"
    val expectedTableCaption: String = "Cyfrifiad swm trethadwy eich cleient"
    val expectedTableHeader1: String = "Eitem"
    val expectedTableHeader2: String = "Swm"
    val expectedRowHeading1: String = "Taliadau pensiwn tramor"
    val expectedRowHeading3: String = "Swm trethadwy"
    val expectedRowHeading2: String = "Treth a dalwyd y tu allan i’r DU y didynnwyd"
    val expectedButtonText: String = "Yn eich blaen"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedIndividualEN),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedAgentEN),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedIndividualCY),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedAgentCY)
  )

  "show" when {
    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        import Selectors._
        import user.commonExpectedResults._

        "Redirects to the pension summary page when user data is empty" which {
          implicit val overseasIncomeCountryUrl: Int => String = IncomeFromOverseasPensionsPages.taxableAmountUrl(0)
          implicit lazy val result: WSResponse = showPage(user, anPensionsUserDataEmptyCya)

          "has SEE_OTHER status" in {
            result.status shouldBe SEE_OTHER
          }
        }

        "Redirect to the pension summary page if there is no session data" which {
          implicit val overseasIncomeCountryUrl: Int => String = IncomeFromOverseasPensionsPages.taxableAmountUrl(0)
          lazy val result: WSResponse = getResponseNoSessionData()

          "has SEE_OTHER status" in {
            result.status shouldBe SEE_OTHER
            result.header("location") shouldBe Some(overseasPensionsSummaryUrl(taxYearEOY))
          }
        }

        "Redirects to the first page in journey if pension payment amount is missing" which {
          implicit val taxableAmountUrl: Int => String = IncomeFromOverseasPensionsPages.taxableAmountUrl(0)
          val pensionUserData = pensionUserDataWithIncomeOverseasPension(anIncomeFromOverseasPensionsWithFalseFtcrValueViewModel
            .copy(overseasIncomePensionSchemes = Seq(PensionScheme(pensionPaymentAmount = None))))

          implicit lazy val result: WSResponse = showPage(user, pensionUserData)

          "has SEE_OTHER status" in {
            result.status shouldBe SEE_OTHER
            result.header("location") shouldBe Some(incomeFromOverseasPensionsStatus(taxYearEOY))
          }
        }

        "Redirects to the first page in journey if pension payment tax paid amount is missing" should {
          implicit val taxableAmountUrl: Int => String = IncomeFromOverseasPensionsPages.taxableAmountUrl(0)
          val pensionUserData = pensionUserDataWithIncomeOverseasPension(anIncomeFromOverseasPensionsWithFalseFtcrValueViewModel
            .copy(overseasIncomePensionSchemes = Seq(PensionScheme(pensionPaymentTaxPaid = None))))

          implicit lazy val result: WSResponse = showPage(user, pensionUserData)

          "has SEE_OTHER status" in {
            result.status shouldBe SEE_OTHER
            result.header("location") shouldBe Some(incomeFromOverseasPensionsStatus(taxYearEOY))
          }
        }

        "redirect to the country summary page when schemes exist but the index is invalid" which {
          val index = 3
          implicit val url: Int => String = IncomeFromOverseasPensionsPages.taxableAmountUrl(index)
          implicit lazy val result: WSResponse = showPage(user, aPensionsUserData)

          "has a SEE_OTHER(303) status" in {
            result.status shouldBe SEE_OTHER
            result.header("location") shouldBe Some(countrySummaryListControllerUrl(taxYearEOY))
          }
        }

        "renders page when user data is available" which {
          implicit val taxableAmountUrl: Int => String = IncomeFromOverseasPensionsPages.taxableAmountUrl(0)
          implicit lazy val result: WSResponse = showPage(user, aPensionsUserData)

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(expectedTitle, user.isWelsh)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(expectedParagraph, paragraphSelector(1))

          textOnPageCheck(expectedTableCaption, tableCaptionSelector)
          textOnPageCheck(expectedTableHeader1, tableHeadSelector(1, 1))
          textOnPageCheck(expectedTableHeader2, tableHeadSelector(1, 2))
          textOnPageCheck(expectedRowHeading1, tableRowHeadSelector(1, 1))
          textOnPageCheck(expectedRowHeading3, tableRowHeadSelector(2, 1))

          formPostLinkCheck(IncomeFromOverseasPensionsPages.taxableAmountUrl(0)(taxYearEOY), formSelector)
          buttonCheck(expectedButtonText)
          welshToggleCheck(user.isWelsh)
        }

        "renders correct pension payment amount from user data" should {
          implicit val taxableAmountUrl: Int => String = IncomeFromOverseasPensionsPages.taxableAmountUrl(0)
          implicit lazy val result: WSResponse = showPage(user, aPensionsUserData)

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          val pensionPaymentAmount = aPensionsUserData.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes.head.pensionPaymentAmount
          val formattedPensionPaymentAmount = formatNoZeros(pensionPaymentAmount.getOrElse(BigDecimal(0)))
          textOnPageCheck(formattedPensionPaymentAmount, tableSelector(1, 1))
        }

        "renders correct taxable amounts from user data" should {
          implicit val taxableAmountUrl: Int => String = IncomeFromOverseasPensionsPages.taxableAmountUrl(0)
          implicit lazy val result: WSResponse = showPage(user, aPensionsUserData)

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          val taxableAmount = aPensionsUserData.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes.head.pensionPaymentAmount
          val formattedTaxableAmount = formatNoZeros(taxableAmount.getOrElse(BigDecimal(0)))
          textOnPageCheck(formattedTaxableAmount, tableSelector(2, 1))
        }

        "renders correctly when FTCR value is false" should {
          implicit val taxableAmountUrl: Int => String = IncomeFromOverseasPensionsPages.taxableAmountUrl(0)
          val pensionUserData = pensionUserDataWithIncomeOverseasPension(anIncomeFromOverseasPensionsWithFalseFtcrValueViewModel)
          implicit lazy val result: WSResponse = showPage(user, pensionUserData)

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(expectedTitle, user.isWelsh)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(expectedFtcrParagraph, paragraphSelector(1))
          textOnPageCheck(expectedFtcrParaItem1, paraItemSelector(1))
          textOnPageCheck(expectedFtcrParaItem2, paraItemSelector(2))

          textOnPageCheck(expectedTableCaption, tableCaptionSelector)
          textOnPageCheck(expectedTableHeader1, tableHeadSelector(1, 1))
          textOnPageCheck(expectedTableHeader2, tableHeadSelector(1, 2))
          textOnPageCheck(expectedRowHeading1, tableRowHeadSelector(1, 1))
          textOnPageCheck(expectedRowHeading2, tableRowHeadSelector(2, 1))
          textOnPageCheck(expectedRowHeading3, tableRowHeadSelector(3, 1))

          formPostLinkCheck(IncomeFromOverseasPensionsPages.taxableAmountUrl(0)(taxYearEOY), formSelector)
          buttonCheck(expectedButtonText)
          welshToggleCheck(user.isWelsh)
        }

        "shows correctly calculated amounts from user data when FTCR is false" should {
          implicit val taxableAmountUrl: Int => String = IncomeFromOverseasPensionsPages.taxableAmountUrl(0)
          val pensionUserData = pensionUserDataWithIncomeOverseasPension(anIncomeFromOverseasPensionsWithFalseFtcrValueViewModel)
          implicit lazy val result: WSResponse = showPage(user, pensionUserData)

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          val pensionPaymentAmount = pensionUserData.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes.head.pensionPaymentAmount
          val formattedPensionPaymentAmount = formatNoZeros(pensionPaymentAmount.getOrElse(BigDecimal(0)))
          textOnPageCheck(formattedPensionPaymentAmount, tableSelector(1, 1))

          val pensionTaxPaid = pensionUserData.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes.head.pensionPaymentTaxPaid
          val formattedTaxPaid = formatNoZeros(-pensionTaxPaid.getOrElse(BigDecimal(0)))
          textOnPageCheck(formattedTaxPaid, tableSelector(2, 1))

          val tax = for {
            amountBeforeTax <- pensionPaymentAmount
            nonUkTaxPaid <- pensionTaxPaid
            taxableAmount = amountBeforeTax - nonUkTaxPaid
          } yield taxableAmount

          val formattedTaxableAmount = formatNoZeros(tax.getOrElse(BigDecimal(0)))
          textOnPageCheck(formattedTaxableAmount, tableSelector(3, 1))
        }
      }
    }
  }

  ".submit" should {
    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "save calculated taxable amount " which {
          implicit val taxableAmountUrl: Int => String = IncomeFromOverseasPensionsPages.taxableAmountUrl(0)
          val incomeViewModel = anIncomeFromOverseasPensionsViewModel.copy(overseasIncomePensionSchemes = Seq(
            anIncomeFromOverseasPensionsWithFalseFtcrValueViewModel.overseasIncomePensionSchemes.head
              .copy(taxableAmount = None)))
          val pensionUserData = pensionUserDataWithIncomeOverseasPension(incomeViewModel)

          implicit lazy val result: WSResponse = submitPage(user, pensionUserData, Map())

          "has a status of SEE_OTHER" in {
            result.status shouldBe SEE_OTHER
            result.header("location") shouldBe Some(overseasPensionsSchemeSummaryUrl(taxYearEOY, 0))
          }
        }
      }
    }
  }

  private def formatNoZeros(amount: BigDecimal): String = NumberFormat.getCurrencyInstance(Locale.UK).format(amount).replaceAll("\\.00", "")
}
