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

package controllers.pensions.incomeFromOverseasPensions

import builders.PensionsUserDataBuilder.{aPensionsUserData, anPensionsUserDataEmptyCya}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.CommonUtils
import utils.PageUrls.{IncomeFromOverseasPensionsPages, pensionSummaryUrl}

import java.text.NumberFormat
import java.util.Locale

class IncomeFromOverseasTaxableAmountControllerISpec extends CommonUtils with BeforeAndAfterEach {
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

    def labelSelector(index: Int): String = s"form > div:nth-of-type($index) > label"
    def paragraphSelector(index: Int): String = s"#main-content > div > div > p:nth-of-type($index)"
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedTitle: String
    val expectedHeading: String
    val expectedParagraph: String
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
    val expectedTableCaption: String = "Your taxable amount calculation"
    val expectedTableHeader1: String = "Item"
    val expectedTableHeader2: String = "Amount"
    val expectedRowHeading1: String = "Foreign pension payments"
    val expectedRowHeading2: String = ""
    val expectedRowHeading3: String = "Taxable amount"
    val expectedButtonText: String = "Continue"
  }

  object CommonExpectedAgentEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Income from overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedTitle: String = "Your client’s taxable amount"
    val expectedHeading: String = "Your client’s taxable amount"
    val expectedParagraph: String = "Your client’s taxable amount is the amount they got in foreign pension payments."
    val expectedTableCaption: String = "Your client’s taxable amount calculation"
    val expectedTableHeader1: String = "Item"
    val expectedTableHeader2: String = "Amount"
    val expectedRowHeading1: String = "Foreign pension payments"
    val expectedRowHeading2: String = "Non-UK tax deducted"
    val expectedRowHeading3: String = "Taxable amount"
    val expectedButtonText: String = "Continue"
  }

  object CommonExpectedIndividualCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Income from overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedTitle: String = "Your taxable amount"
    val expectedHeading: String = "Your taxable amount"
    val expectedParagraph: String = "Your taxable amount is the amount you got in foreign pension payments."
    val expectedTableCaption: String = "Your taxable amount calculation"
    val expectedTableHeader1: String = "Item"
    val expectedTableHeader2: String = "Amount"
    val expectedRowHeading1: String = "Foreign pension payments"
    val expectedRowHeading2: String = "Non-UK tax deducted"
    val expectedRowHeading3: String = "Taxable amount"
    val expectedButtonText: String = "Continue"
  }

  object CommonExpectedAgentCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Income from overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedTitle: String = "Your client’s taxable amount"
    val expectedHeading: String = "Your client’s taxable amount"
    val expectedParagraph: String = "Your client’s taxable amount is the amount they got in foreign pension payments."
    val expectedTableCaption: String = "Your client’s taxable amount calculation"
    val expectedTableHeader1: String = "Item"
    val expectedTableHeader2: String = "Amount"
    val expectedRowHeading1: String = "Foreign pension payments"
    val expectedRowHeading3: String = "Taxable amount"
    val expectedRowHeading2: String = "Non-UK tax deducted"
    val expectedButtonText: String = "Continue"
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

          "has an SEE_OTHER status" in {
            result.status shouldBe SEE_OTHER
          }
        }

        "Redirect to the pension summary page if there is no session data" should {
          implicit val overseasIncomeCountryUrl: Int => String = IncomeFromOverseasPensionsPages.taxableAmountUrl(0)
          lazy val result: WSResponse = getResponseNoSessionData

          "has an SEE_OTHER status" in {
            result.status shouldBe SEE_OTHER
            result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
          }
        }

        "renders page when user data is available" which {
          implicit val overseasIncomeCountryUrl: Int => String = IncomeFromOverseasPensionsPages.taxableAmountUrl(0)
          implicit lazy val result: WSResponse = showPage(user, aPensionsUserData)

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(expectedTitle)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(expectedParagraph, paragraphSelector(1))

          textOnPageCheck(expectedTableCaption, tableCaptionSelector)
          textOnPageCheck(expectedTableHeader1, tableHeadSelector(1,1))
          textOnPageCheck(expectedTableHeader2, tableHeadSelector(1,2))
          textOnPageCheck(expectedRowHeading1, tableRowHeadSelector(1,1))
          textOnPageCheck(expectedRowHeading3, tableRowHeadSelector(2,1))

          formPostLinkCheck(IncomeFromOverseasPensionsPages.taxableAmountUrl(0)(taxYearEOY), formSelector)
          buttonCheck(expectedButtonText)
        }

        "renders with correct pension payment amount from user data" should {
          implicit val overseasIncomeCountryUrl: Int => String = IncomeFromOverseasPensionsPages.taxableAmountUrl(0)
          implicit lazy val result: WSResponse = showPage(user, aPensionsUserData)
          implicit def document: () => Document = () => Jsoup.parse(result.body)

          val pensionPaymentAmount = aPensionsUserData.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes(0).pensionPaymentAmount
          val formattedPensionPaymentAmount = formatNoZeros(pensionPaymentAmount.getOrElse(BigDecimal(0)))
          textOnPageCheck(formattedPensionPaymentAmount, tableSelector(1, 1))
        }

        "renders with correct calculated amount from user data" should {
          implicit val overseasIncomeCountryUrl: Int => String = IncomeFromOverseasPensionsPages.taxableAmountUrl(0)
          implicit lazy val result: WSResponse = showPage(user, aPensionsUserData)
          implicit def document: () => Document = () => Jsoup.parse(result.body)

          val taxableAmount = aPensionsUserData.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes(0).pensionPaymentAmount
          val formattedTaxableAmount = formatNoZeros(taxableAmount.getOrElse(BigDecimal(0)))
          textOnPageCheck(formattedTaxableAmount, tableSelector(2, 1))
        }
      }
    }
  }

  def formatNoZeros(amount: BigDecimal): String = NumberFormat.getCurrencyInstance(Locale.UK).format(amount).replaceAll("\\.00", "")
}
