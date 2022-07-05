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

package views

import controllers.pensions.paymentsIntoPension.PaymentsIntoPensionFormProvider
import forms.AmountForm
import models.AuthorisationRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.AltViewUnitTest
import views.html.pensions.paymentsIntoPensions.OneOffRASPaymentsAmountView

// scalastyle:off magic.number

class OneOffRASPaymentsAmountViewSpec extends AltViewUnitTest {

  private lazy val viewUnderTest: OneOffRASPaymentsAmountView = inject[OneOffRASPaymentsAmountView]
  private val baseAmountForm: Form[BigDecimal] = new PaymentsIntoPensionFormProvider().oneOffRASPaymentsAmountForm
  private val rasAmount: BigDecimal = 189.01

  "the view" should {
    "render as expected" when {
      "there is no cya data" when {
        "requested by an individual" when {

          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(isAgent = false)

          "the preferred language is English" when {

            val isWelsh = false
            implicit val document: Document = render(isWelsh, baseAmountForm)

            verify(isWelsh, ExpectedContents(
              title = "Total one-off payments into relief at source (RAS) pensions, plus basic rate tax relief",
              header = "Total one-off payments into relief at source (RAS) pensions, plus basic rate tax relief",
              caption = s"Payments into pensions for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY",
              firstParagraph = "You told us the total amount you paid plus tax relief was £189.01. Tell us how much of this was a one-off payment. Include tax relief.",
              secondParagraph = "To work it out, divide your one-off payment amount by 80 and multiply the result by 100.",
              firstSpan = "Example calculation",
              secondSpan = "Emma made a one-off payment of £500. £500 divided by 80 and multiplied by 100 is £625. Her answer is £625.",
              hint = "For example, £193.52",
              continueButton = "Continue",
              currentAmount = "",
              error = None
            ))
          }

          "the preferred language is Welsh" when {

            val isWelsh = true
            implicit val document: Document = render(isWelsh, baseAmountForm)

            verify(isWelsh, ExpectedContents(
              title = "Total one-off payments into relief at source (RAS) pensions, plus basic rate tax relief",
              header = "Total one-off payments into relief at source (RAS) pensions, plus basic rate tax relief",
              caption = s"Payments into pensions for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY",
              firstParagraph = "You told us the total amount you paid plus tax relief was £189.01. Tell us how much of this was a one-off payment. Include tax relief.",
              secondParagraph = "To work it out, divide your one-off payment amount by 80 and multiply the result by 100.",
              firstSpan = "Example calculation",
              secondSpan = "Emma made a one-off payment of £500. £500 divided by 80 and multiplied by 100 is £625. Her answer is £625.",
              hint = "For example, £193.52",
              continueButton = "Continue",
              currentAmount = "",
              error = None
            ))

          }
        }
        "requested by an Agent" when {

          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(isAgent = true)

          "the preferred language is English" when {

            val isWelsh = false
            implicit val document: Document = render(isWelsh, baseAmountForm)

            verify(isWelsh, ExpectedContents(
              title = "Total one-off payments into relief at source (RAS) pensions, plus basic rate tax relief",
              header = "Total one-off payments into relief at source (RAS) pensions, plus basic rate tax relief",
              caption = s"Payments into pensions for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY",
              firstParagraph = "You told us the total amount your client paid plus tax relief was £189.01. Tell us how much of this was a one-off payment. Include tax relief.",
              secondParagraph = "To work it out, divide your one-off payment amount by 80 and multiply the result by 100.",
              firstSpan = "Example calculation",
              secondSpan = "Emma made a one-off payment of £500. £500 divided by 80 and multiplied by 100 is £625. Her answer is £625.",
              hint = "For example, £193.52",
              continueButton = "Continue",
              currentAmount = "",
              error = None
            ))

          }
          "the preferred language is Welsh" when {

            val isWelsh = true
            implicit val document: Document = render(isWelsh, baseAmountForm)

            verify(isWelsh, ExpectedContents(
              title = "Total one-off payments into relief at source (RAS) pensions, plus basic rate tax relief",
              header = "Total one-off payments into relief at source (RAS) pensions, plus basic rate tax relief",
              caption = s"Payments into pensions for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY",
              firstParagraph = "You told us the total amount your client paid plus tax relief was £189.01. Tell us how much of this was a one-off payment. Include tax relief.",
              secondParagraph = "To work it out, divide your one-off payment amount by 80 and multiply the result by 100.",
              firstSpan = "Example calculation",
              secondSpan = "Emma made a one-off payment of £500. £500 divided by 80 and multiplied by 100 is £625. Her answer is £625.",
              hint = "For example, £193.52",
              continueButton = "Continue",
              currentAmount = "",
              error = None
            ))

          }
        }
      }

      "there is cya data" when {


        "requested by an individual" when {

          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(isAgent = false)

          "the preferred language is English" when {

            val isWelsh = false
            implicit val document: Document = render(isWelsh, baseAmountForm.fill(999.88))

            verify(isWelsh, ExpectedContents(
              title = "Total one-off payments into relief at source (RAS) pensions, plus basic rate tax relief",
              header = "Total one-off payments into relief at source (RAS) pensions, plus basic rate tax relief",
              caption = s"Payments into pensions for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY",
              firstParagraph = "You told us the total amount you paid plus tax relief was £189.01. Tell us how much of this was a one-off payment. Include tax relief.",
              secondParagraph = "To work it out, divide your one-off payment amount by 80 and multiply the result by 100.",
              firstSpan = "Example calculation",
              secondSpan = "Emma made a one-off payment of £500. £500 divided by 80 and multiplied by 100 is £625. Her answer is £625.",
              hint = "For example, £193.52",
              continueButton = "Continue",
              currentAmount = "999.88",
              error = None
            ))
          }

          "the preferred language is Welsh" when {

            val isWelsh = true
            implicit val document: Document = render(isWelsh, baseAmountForm.fill(999.88))

            verify(isWelsh, ExpectedContents(
              title = "Total one-off payments into relief at source (RAS) pensions, plus basic rate tax relief",
              header = "Total one-off payments into relief at source (RAS) pensions, plus basic rate tax relief",
              caption = s"Payments into pensions for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY",
              firstParagraph = "You told us the total amount you paid plus tax relief was £189.01. Tell us how much of this was a one-off payment. Include tax relief.",
              secondParagraph = "To work it out, divide your one-off payment amount by 80 and multiply the result by 100.",
              firstSpan = "Example calculation",
              secondSpan = "Emma made a one-off payment of £500. £500 divided by 80 and multiplied by 100 is £625. Her answer is £625.",
              hint = "For example, £193.52",
              continueButton = "Continue",
              currentAmount = "999.88",
              error = None
            ))

          }
        }
        "requested by an Agent" when {

          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(isAgent = true)

          "the preferred language is English" when {

            val isWelsh = false
            implicit val document: Document = render(isWelsh, baseAmountForm.fill(999.88))

            verify(isWelsh, ExpectedContents(
              title = "Total one-off payments into relief at source (RAS) pensions, plus basic rate tax relief",
              header = "Total one-off payments into relief at source (RAS) pensions, plus basic rate tax relief",
              caption = s"Payments into pensions for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY",
              firstParagraph = "You told us the total amount your client paid plus tax relief was £189.01. Tell us how much of this was a one-off payment. Include tax relief.",
              secondParagraph = "To work it out, divide your one-off payment amount by 80 and multiply the result by 100.",
              firstSpan = "Example calculation",
              secondSpan = "Emma made a one-off payment of £500. £500 divided by 80 and multiplied by 100 is £625. Her answer is £625.",
              hint = "For example, £193.52",
              continueButton = "Continue",
              currentAmount = "999.88",
              error = None
            ))

          }
          "the preferred language is Welsh" when {

            val isWelsh = true
            implicit val document: Document = render(isWelsh, baseAmountForm.fill(999.88))

            verify(isWelsh, ExpectedContents(
              title = "Total one-off payments into relief at source (RAS) pensions, plus basic rate tax relief",
              header = "Total one-off payments into relief at source (RAS) pensions, plus basic rate tax relief",
              caption = s"Payments into pensions for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY",
              firstParagraph = "You told us the total amount your client paid plus tax relief was £189.01. Tell us how much of this was a one-off payment. Include tax relief.",
              secondParagraph = "To work it out, divide your one-off payment amount by 80 and multiply the result by 100.",
              firstSpan = "Example calculation",
              secondSpan = "Emma made a one-off payment of £500. £500 divided by 80 and multiplied by 100 is £625. Her answer is £625.",
              hint = "For example, £193.52",
              continueButton = "Continue",
              currentAmount = "999.88",
              error = None
            ))

          }
        }
      }

      "there is no input entry" when {

        "requested by an individual" when {

          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(isAgent = false)

          "the preferred language is English" when {

            val isWelsh = false
            implicit val document: Document = render(isWelsh, baseAmountForm.bind(Map(AmountForm.amount -> "")))

            verify(isWelsh, ExpectedContents(
              title = "Error: Total one-off payments into relief at source (RAS) pensions, plus basic rate tax relief",
              header = "Total one-off payments into relief at source (RAS) pensions, plus basic rate tax relief",
              caption = s"Payments into pensions for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY",
              firstParagraph = "You told us the total amount you paid plus tax relief was £189.01. Tell us how much of this was a one-off payment. Include tax relief.",
              secondParagraph = "To work it out, divide your one-off payment amount by 80 and multiply the result by 100.",
              firstSpan = "Example calculation",
              secondSpan = "Emma made a one-off payment of £500. £500 divided by 80 and multiplied by 100 is £625. Her answer is £625.",
              hint = "For example, £193.52",
              continueButton = "Continue",
              currentAmount = "",
              error = Some("Enter the total amount of one-off payments paid into RAS pensions, plus basic rate tax relief")
            ))
          }

          "the preferred language is Welsh" when {

            val isWelsh = true
            implicit val document: Document = render(isWelsh, baseAmountForm.bind(Map(AmountForm.amount -> "")))

            verify(isWelsh, ExpectedContents(
              title = "Error: Total one-off payments into relief at source (RAS) pensions, plus basic rate tax relief",
              header = "Total one-off payments into relief at source (RAS) pensions, plus basic rate tax relief",
              caption = s"Payments into pensions for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY",
              firstParagraph = "You told us the total amount you paid plus tax relief was £189.01. Tell us how much of this was a one-off payment. Include tax relief.",
              secondParagraph = "To work it out, divide your one-off payment amount by 80 and multiply the result by 100.",
              firstSpan = "Example calculation",
              secondSpan = "Emma made a one-off payment of £500. £500 divided by 80 and multiplied by 100 is £625. Her answer is £625.",
              hint = "For example, £193.52",
              continueButton = "Continue",
              currentAmount = "",
              error = Some("Enter the total amount of one-off payments paid into RAS pensions, plus basic rate tax relief")
            ))

          }
        }
        "requested by an Agent" when {

          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(isAgent = true)

          "the preferred language is English" when {

            val isWelsh = false
            implicit val document: Document = render(isWelsh, baseAmountForm.bind(Map(AmountForm.amount -> "")))

            verify(isWelsh, ExpectedContents(
              title = "Error: Total one-off payments into relief at source (RAS) pensions, plus basic rate tax relief",
              header = "Total one-off payments into relief at source (RAS) pensions, plus basic rate tax relief",
              caption = s"Payments into pensions for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY",
              firstParagraph = "You told us the total amount your client paid plus tax relief was £189.01. Tell us how much of this was a one-off payment. Include tax relief.",
              secondParagraph = "To work it out, divide your one-off payment amount by 80 and multiply the result by 100.",
              firstSpan = "Example calculation",
              secondSpan = "Emma made a one-off payment of £500. £500 divided by 80 and multiplied by 100 is £625. Her answer is £625.",
              hint = "For example, £193.52",
              continueButton = "Continue",
              currentAmount = "",
              error = Some("Enter the total amount of one-off payments paid into RAS pensions, plus basic rate tax relief")
            ))

          }
          "the preferred language is Welsh" when {

            val isWelsh = true
            implicit val document: Document = render(isWelsh, baseAmountForm.bind(Map(AmountForm.amount -> "")))

            verify(isWelsh, ExpectedContents(
              title = "Error: Total one-off payments into relief at source (RAS) pensions, plus basic rate tax relief",
              header = "Total one-off payments into relief at source (RAS) pensions, plus basic rate tax relief",
              caption = s"Payments into pensions for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY",
              firstParagraph = "You told us the total amount your client paid plus tax relief was £189.01. Tell us how much of this was a one-off payment. Include tax relief.",
              secondParagraph = "To work it out, divide your one-off payment amount by 80 and multiply the result by 100.",
              firstSpan = "Example calculation",
              secondSpan = "Emma made a one-off payment of £500. £500 divided by 80 and multiplied by 100 is £625. Her answer is £625.",
              hint = "For example, £193.52",
              continueButton = "Continue",
              currentAmount = "",
              error = Some("Enter the total amount of one-off payments paid into RAS pensions, plus basic rate tax relief")
            ))

          }
        }
      }
      "there an invalid format input" when {

        "requested by an individual" when {

          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(isAgent = false)

          "the preferred language is English" when {

            val isWelsh = false
            implicit val document: Document = render(isWelsh, baseAmountForm.bind(Map(AmountForm.amount -> "invalid")))

            verify(isWelsh, ExpectedContents(
              title = "Error: Total one-off payments into relief at source (RAS) pensions, plus basic rate tax relief",
              header = "Total one-off payments into relief at source (RAS) pensions, plus basic rate tax relief",
              caption = s"Payments into pensions for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY",
              firstParagraph = "You told us the total amount you paid plus tax relief was £189.01. Tell us how much of this was a one-off payment. Include tax relief.",
              secondParagraph = "To work it out, divide your one-off payment amount by 80 and multiply the result by 100.",
              firstSpan = "Example calculation",
              secondSpan = "Emma made a one-off payment of £500. £500 divided by 80 and multiplied by 100 is £625. Her answer is £625.",
              hint = "For example, £193.52",
              continueButton = "Continue",
              currentAmount = "invalid",
              error = Some("Enter the total amount of one-off payments paid into RAS pensions, plus basic rate tax relief, in the correct format")
            ))
          }

          "the preferred language is Welsh" when {

            val isWelsh = true
            implicit val document: Document = render(isWelsh, baseAmountForm.bind(Map(AmountForm.amount -> "invalid")))

            verify(isWelsh, ExpectedContents(
              title = "Error: Total one-off payments into relief at source (RAS) pensions, plus basic rate tax relief",
              header = "Total one-off payments into relief at source (RAS) pensions, plus basic rate tax relief",
              caption = s"Payments into pensions for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY",
              firstParagraph = "You told us the total amount you paid plus tax relief was £189.01. Tell us how much of this was a one-off payment. Include tax relief.",
              secondParagraph = "To work it out, divide your one-off payment amount by 80 and multiply the result by 100.",
              firstSpan = "Example calculation",
              secondSpan = "Emma made a one-off payment of £500. £500 divided by 80 and multiplied by 100 is £625. Her answer is £625.",
              hint = "For example, £193.52",
              continueButton = "Continue",
              currentAmount = "invalid",
              error = Some("Enter the total amount of one-off payments paid into RAS pensions, plus basic rate tax relief, in the correct format")
            ))

          }
        }
        "requested by an Agent" when {

          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(isAgent = true)

          "the preferred language is English" when {

            val isWelsh = false
            implicit val document: Document = render(isWelsh, baseAmountForm.bind(Map(AmountForm.amount -> "invalid")))

            verify(isWelsh, ExpectedContents(
              title = "Error: Total one-off payments into relief at source (RAS) pensions, plus basic rate tax relief",
              header = "Total one-off payments into relief at source (RAS) pensions, plus basic rate tax relief",
              caption = s"Payments into pensions for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY",
              firstParagraph = "You told us the total amount your client paid plus tax relief was £189.01. Tell us how much of this was a one-off payment. Include tax relief.",
              secondParagraph = "To work it out, divide your one-off payment amount by 80 and multiply the result by 100.",
              firstSpan = "Example calculation",
              secondSpan = "Emma made a one-off payment of £500. £500 divided by 80 and multiplied by 100 is £625. Her answer is £625.",
              hint = "For example, £193.52",
              continueButton = "Continue",
              currentAmount = "invalid",
              error = Some("Enter the total amount of one-off payments paid into RAS pensions, plus basic rate tax relief, in the correct format")
            ))

          }
          "the preferred language is Welsh" when {

            val isWelsh = true
            implicit val document: Document = render(isWelsh, baseAmountForm.bind(Map(AmountForm.amount -> "invalid")))

            verify(isWelsh, ExpectedContents(
              title = "Error: Total one-off payments into relief at source (RAS) pensions, plus basic rate tax relief",
              header = "Total one-off payments into relief at source (RAS) pensions, plus basic rate tax relief",
              caption = s"Payments into pensions for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY",
              firstParagraph = "You told us the total amount your client paid plus tax relief was £189.01. Tell us how much of this was a one-off payment. Include tax relief.",
              secondParagraph = "To work it out, divide your one-off payment amount by 80 and multiply the result by 100.",
              firstSpan = "Example calculation",
              secondSpan = "Emma made a one-off payment of £500. £500 divided by 80 and multiplied by 100 is £625. Her answer is £625.",
              hint = "For example, £193.52",
              continueButton = "Continue",
              currentAmount = "invalid",
              error = Some("Enter the total amount of one-off payments paid into RAS pensions, plus basic rate tax relief, in the correct format")
            ))

          }
        }
      }
      "there is an input over maximum allowed value" when {
        "requested by an individual" when {

          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(isAgent = false)

          "the preferred language is English" when {

            val isWelsh = false
            implicit val document: Document = render(isWelsh, baseAmountForm.bind(Map(AmountForm.amount -> "100,000,000,000")))

            verify(isWelsh, ExpectedContents(
              title = "Error: Total one-off payments into relief at source (RAS) pensions, plus basic rate tax relief",
              header = "Total one-off payments into relief at source (RAS) pensions, plus basic rate tax relief",
              caption = s"Payments into pensions for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY",
              firstParagraph = "You told us the total amount you paid plus tax relief was £189.01. Tell us how much of this was a one-off payment. Include tax relief.",
              secondParagraph = "To work it out, divide your one-off payment amount by 80 and multiply the result by 100.",
              firstSpan = "Example calculation",
              secondSpan = "Emma made a one-off payment of £500. £500 divided by 80 and multiplied by 100 is £625. Her answer is £625.",
              hint = "For example, £193.52",
              continueButton = "Continue",
              currentAmount = "100,000,000,000",
              error = Some("The total amount of one-off payments paid into RAS pensions, plus basic rate tax relief, must be less than £100,000,000,000")
            ))

          }

          "the preferred language is Welsh" when {

            val isWelsh = true
            implicit val document: Document = render(isWelsh, baseAmountForm.bind(Map(AmountForm.amount -> "100,000,000,000")))

            verify(isWelsh, ExpectedContents(
              title = "Error: Total one-off payments into relief at source (RAS) pensions, plus basic rate tax relief",
              header = "Total one-off payments into relief at source (RAS) pensions, plus basic rate tax relief",
              caption = s"Payments into pensions for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY",
              firstParagraph = "You told us the total amount you paid plus tax relief was £189.01. Tell us how much of this was a one-off payment. Include tax relief.",
              secondParagraph = "To work it out, divide your one-off payment amount by 80 and multiply the result by 100.",
              firstSpan = "Example calculation",
              secondSpan = "Emma made a one-off payment of £500. £500 divided by 80 and multiplied by 100 is £625. Her answer is £625.",
              hint = "For example, £193.52",
              continueButton = "Continue",
              currentAmount = "100,000,000,000",
              error = Some("The total amount of one-off payments paid into RAS pensions, plus basic rate tax relief, must be less than £100,000,000,000")
            ))

          }
        }
        "requested by an Agent" when {

          implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(isAgent = true)

          "the preferred language is English" when {

            val isWelsh = false
            implicit val document: Document = render(isWelsh, baseAmountForm.bind(Map(AmountForm.amount -> "100,000,000,000")))

            verify(isWelsh, ExpectedContents(
              title = "Error: Total one-off payments into relief at source (RAS) pensions, plus basic rate tax relief",
              header = "Total one-off payments into relief at source (RAS) pensions, plus basic rate tax relief",
              caption = s"Payments into pensions for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY",
              firstParagraph = "You told us the total amount your client paid plus tax relief was £189.01. Tell us how much of this was a one-off payment. Include tax relief.",
              secondParagraph = "To work it out, divide your one-off payment amount by 80 and multiply the result by 100.",
              firstSpan = "Example calculation",
              secondSpan = "Emma made a one-off payment of £500. £500 divided by 80 and multiplied by 100 is £625. Her answer is £625.",
              hint = "For example, £193.52",
              continueButton = "Continue",
              currentAmount = "100,000,000,000",
              error = Some("The total amount of one-off payments paid into RAS pensions, plus basic rate tax relief, must be less than £100,000,000,000")
            ))

          }
          "the preferred language is Welsh" when {

            val isWelsh = true
            implicit val document: Document = render(isWelsh, baseAmountForm.bind(Map(AmountForm.amount -> "100,000,000,000")))

            verify(isWelsh, ExpectedContents(
              title = "Error: Total one-off payments into relief at source (RAS) pensions, plus basic rate tax relief",
              header = "Total one-off payments into relief at source (RAS) pensions, plus basic rate tax relief",
              caption = s"Payments into pensions for 6 April ${taxYearEOY - 1} to 5 April $taxYearEOY",
              firstParagraph = "You told us the total amount your client paid plus tax relief was £189.01. Tell us how much of this was a one-off payment. Include tax relief.",
              secondParagraph = "To work it out, divide your one-off payment amount by 80 and multiply the result by 100.",
              firstSpan = "Example calculation",
              secondSpan = "Emma made a one-off payment of £500. £500 divided by 80 and multiplied by 100 is £625. Her answer is £625.",
              hint = "For example, £193.52",
              continueButton = "Continue",
              currentAmount = "100,000,000,000",
              error = Some("The total amount of one-off payments paid into RAS pensions, plus basic rate tax relief, must be less than £100,000,000,000")
            ))

          }
        }
      }

    }
  }

    case class ExpectedContents(title: String,
                                header: String,
                                caption: String,
                                firstParagraph: String,
                                secondParagraph: String,
                                firstSpan: String,
                                secondSpan: String,
                                hint: String,
                                continueButton: String,
                                currentAmount: String,
                                error: Option[String])

    private def verify(isWelsh: Boolean, expectedContents: ExpectedContents)(implicit document: Document): Unit = {

      val continueButtonSelector: String = "#continue"
      val formSelector: String = "#main-content > div > div > form"
      val hintTextSelector = "#amount-hint"
      val poundPrefixSelector = ".govuk-input__prefix"
      val inputSelector = "#amount"
      val expectedErrorHref = "#amount"

      def insetSpanText(index: Int): String = s"#main-content > div > div > div > span:nth-child($index)"

      def paragraphSelector(index: Int): String = s"#main-content > div > div > p:nth-of-type($index)"

      titleCheck(expectedContents.title, isWelsh)
      h1Check(expectedContents.header)
      captionCheck(expectedContents.caption)
      textOnPageCheck(expectedContents.firstParagraph, paragraphSelector(1))
      textOnPageCheck(expectedContents.secondParagraph, paragraphSelector(2))
      textOnPageCheck(expectedContents.firstSpan, insetSpanText(1))
      textOnPageCheck(expectedContents.secondSpan, insetSpanText(2))
      textOnPageCheck(expectedContents.hint, hintTextSelector)
      textOnPageCheck("£", poundPrefixSelector)
      inputFieldValueCheck("amount", inputSelector, expectedContents.currentAmount)
      buttonCheck(expectedContents.continueButton, continueButtonSelector)
      formPostLinkCheck(oneOffReliefAtSourcePaymentsAmountUrl(taxYearEOY), formSelector)
      expectedContents.error.foreach { error =>
        errorSummaryCheck(error, expectedErrorHref)
        errorAboveElementCheck(error)
      }
      welshToggleCheck(isWelsh)

    }

  private def render(isWelsh: Boolean, amountForm: Form[BigDecimal])(implicit authRequest: AuthorisationRequest[AnyContent]): Document = {
    implicit val messages: Messages = getMessages(isWelsh)
    val htmlFormat = viewUnderTest(amountForm, taxYearEOY, rasAmount)
    Jsoup.parse(htmlFormat.body)
  }
}

// scalastyle:on magic.number


