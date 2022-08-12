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

package controllers.pensions.unauthorisedPayments

import builders.PensionsUserDataBuilder.pensionsUserDataWithUnauthorisedPayments
import builders.UnauthorisedPaymentsViewModelBuilder.anUnauthorisedPaymentsViewModel
import builders.UserBuilder.aUserRequest
import forms.AmountForm
import models.pension.charges.UnauthorisedPaymentsViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.UnAuthorisedPayments.noSurchargeAmountUrl
import utils.PageUrls.{fullUrl, pensionSummaryUrl}
import utils.PageUrls.unauthorisedPaymentsPages.{nonUKTaxOnAmountSurcharge, whereAnyOfTheUnauthorisedPaymentsUrl}
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

class NoSurchargeAmountControllerISpec extends IntegrationTest with ViewHelpers with BeforeAndAfterEach with PensionsDatabaseHelper {


  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] =
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, None),
      UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, None),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, None),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, None))

  private val poundPrefixText = "£"
  private val amountInputName = "amount"

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedTitle: String
    val expectedHeading: String
    val expectedErrorTitle: String
    val noEntryErrorMessage: String
    val invalidFormatErrorText: String
    val maxAmountErrorText: String
    val hintText: String
    val buttonText: String
  }

  trait SpecificExpectedResults {
    val expectedParagraph: String
  }

  object Selectors {
    val captionSelector: String = "#main-content > div > div > header > p"
    val continueButtonSelector: String = "#continue"
    val formSelector: String = "#main-content > div > div > form"
    val hintTextSelector = "#amount-hint"
    val poundPrefixSelector = ".govuk-input__prefix"
    val inputSelector = "#amount"
    val expectedErrorHref = "#amount"

    def paragraphSelector(index: Int): String = s"#main-content > div > div > p:nth-of-type($index)"
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Unauthorised payments from pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedTitle = "Amount that did not result in a surcharge"
    val expectedHeading = "Amount that did not result in a surcharge"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val hintText = "For example, £193.52"
    val noEntryErrorMessage = "Enter the total amount of unauthorised payment that did not result in a surcharge"
    val invalidFormatErrorText = "Enter the total amount in the correct format"
    val maxAmountErrorText = "The total amount must be less than £100,000,000,000"
    val buttonText = "Continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Taliadau heb awdurdod o bensiynau ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val expectedTitle = "Amount that did not result in a surcharge"
    val expectedHeading = "Amount that did not result in a surcharge"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val hintText = "For example, £193.52"
    val noEntryErrorMessage = "Enter the total amount of unauthorised payment that did not result in a surcharge"
    val invalidFormatErrorText = "Enter the total amount in the correct format"
    val maxAmountErrorText = "The total amount must be less than £100,000,000,000"
    val buttonText = "Continue"
  }


  ".show" should {
    userScenarios.foreach { user =>
      import Selectors._
      import user.commonExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        "render unauthorised payments that resulted in no surcharges page with no pre filling" which {
          val viewModel = anUnauthorisedPaymentsViewModel.copy(noSurchargeAmount = None)
          lazy val result: WSResponse = showPage(user, viewModel)

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          s"has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(expectedTitle)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, inputSelector, "")
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(noSurchargeAmountUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render into your amount not surcharge page when cya data" which {
          val existingAmount: BigDecimal = 999.88
          val viewModel = anUnauthorisedPaymentsViewModel.copy(noSurchargeAmount = Some(existingAmount))
          lazy val result: WSResponse = showPage(user, viewModel)

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.commonExpectedResults.expectedTitle)
          h1Check(user.commonExpectedResults.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, inputSelector, s"$existingAmount")
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(noSurchargeAmountUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "redirect to the pension summary page if the amount not surcharge question has not been answered" which {
      val viewModel = anUnauthorisedPaymentsViewModel.copy(noSurchargeAmount = None, noSurchargeQuestion = None)
      lazy val result: WSResponse = showPage(viewModel)

      "has a SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER //TODO - redirect to the CYA page
      }
    }

    "redirect to the CYA page if there is no session data" which {
      lazy val result: WSResponse = getResponseNoSessionData

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER //TODO - redirect to CYA page once implemented
      }
    }
    "redirect to the non uk tax not surcharge page if user tries to access link directly" which {
      val validAmount = "100.22"
      val viewModel = anUnauthorisedPaymentsViewModel.copy(noSurchargeQuestion = Some(false))
      val validForm: Map[String, String] = Map(AmountForm.amount -> validAmount)

      implicit lazy val result: WSResponse = submitPage(viewModel, validForm)

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
      }

      "redirects successfully to Unauthorised Payements page" in {
        result.header("location").contains(whereAnyOfTheUnauthorisedPaymentsUrl(taxYearEOY)) shouldBe true
      }
    }

    "redirect to the CYA page if user tries to access link directly" which {
      val viewModel = anUnauthorisedPaymentsViewModel.copy(noSurchargeQuestion = None)
      implicit lazy val result: WSResponse = showPage(viewModel)

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
      }

      "redirects successfully to cya page" in {
        result.header("location").contains(pensionSummaryUrl(taxYearEOY)) shouldBe true //Todo change this to check your answers page once implemented
      }
    }
  }

  ".submit" should {
    userScenarios.foreach { user =>
      import Selectors._
      import user.commonExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        "return an error when form is submitted with no input entry for amount not surcharge page" which {
          val amountEmpty = ""
          val emptyForm: Map[String, String] = Map(AmountForm.amount -> amountEmpty)
          val viewModel = anUnauthorisedPaymentsViewModel.copy(noSurchargeAmount = None)


          lazy val result: WSResponse = submitPage(user, viewModel, emptyForm)

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(expectedErrorTitle)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, inputSelector, amountEmpty)
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(noSurchargeAmountUrl(taxYearEOY), formSelector)
          errorSummaryCheck(noEntryErrorMessage, expectedErrorHref)
          errorAboveElementCheck(noEntryErrorMessage)
          welshToggleCheck(user.isWelsh)
        }

        "return an error when form is submitted with an invalid format input" which {
          val amountInvalidFormat = "invalid"
          val invalidFormatForm: Map[String, String] = Map(AmountForm.amount -> amountInvalidFormat)
          val viewModel = anUnauthorisedPaymentsViewModel.copy(noSurchargeAmount = None)

          lazy val result: WSResponse = submitPage(user, viewModel, invalidFormatForm)

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(expectedErrorTitle)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, inputSelector, amountInvalidFormat)
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(noSurchargeAmountUrl(taxYearEOY), formSelector)
          errorSummaryCheck(invalidFormatErrorText, expectedErrorHref)
          errorAboveElementCheck(invalidFormatErrorText)
          welshToggleCheck(user.isWelsh)
        }

        "return an error when form is submitted with input over maximum allowed value" which {
          val amountOverMaximum = "100,000,000,000"
          val overMaximumForm: Map[String, String] = Map(AmountForm.amount -> amountOverMaximum)
          val viewModel = anUnauthorisedPaymentsViewModel.copy(noSurchargeAmount = None)

          lazy val result: WSResponse = submitPage(user, viewModel, overMaximumForm)

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(expectedErrorTitle)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(hintText, hintTextSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldValueCheck(amountInputName, inputSelector, amountOverMaximum)
          formPostLinkCheck(noSurchargeAmountUrl(taxYearEOY), formSelector)
          buttonCheck(buttonText, continueButtonSelector)
          errorSummaryCheck(maxAmountErrorText, expectedErrorHref)
          errorAboveElementCheck(maxAmountErrorText)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "redirect to the no surcharge tax amount page when a valid amount is submitted and update the session amount completing the journey" which {
      val validAmount = "100.22"
      val validForm: Map[String, String] = Map(AmountForm.amount -> validAmount)
      val viewModel = anUnauthorisedPaymentsViewModel.copy(noSurchargeAmount = None)

      lazy val result: WSResponse = submitPage(viewModel, validForm)

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(nonUKTaxOnAmountSurcharge(taxYearEOY)) shouldBe true
      }

      "updates surcharge amount" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.unauthorisedPayments.noSurchargeAmount shouldBe Some(BigDecimal(validAmount))
      }
    }

    "redirect to the non uk tax not surcharge page if not surcharge question is false" which {
      val validAmount = "100.22"
      val validForm: Map[String, String] = Map(AmountForm.amount -> validAmount)
      val viewModel = anUnauthorisedPaymentsViewModel.copy(noSurchargeQuestion = Some(false))

      implicit lazy val result: WSResponse = submitPage(viewModel, validForm)

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
      }

      "redirects successfully to cya page" in {
        result.header("location").contains(whereAnyOfTheUnauthorisedPaymentsUrl(taxYearEOY)) shouldBe true
      }
    }

    "redirect to the CYA page if user tries to submits request directly without providing information prior" which {
      val validAmount = "100.22"
      val validForm: Map[String, String] = Map(AmountForm.amount -> validAmount)
      val viewModel = anUnauthorisedPaymentsViewModel.copy(noSurchargeQuestion = None)

      implicit lazy val result: WSResponse = submitPage(viewModel, validForm)

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
      }

      "redirects successfully to cya page" in {
        result.header("location").contains(noSurchargeAmountUrl(taxYearEOY)) shouldBe true //Todo change this to check your answers page once implemented
      }
    }
  }

  private def showPage(user: UserScenario[CommonExpectedResults, SpecificExpectedResults],
                          anUnauthorisedPaymentsViewModel: UnauthorisedPaymentsViewModel) = {
    lazy val result: WSResponse = {
      dropPensionsDB()
      authoriseAgentOrIndividual(user.isAgent)
      val viewModel = anUnauthorisedPaymentsViewModel
      insertCyaData(pensionsUserDataWithUnauthorisedPayments(viewModel, isPriorSubmission = false), aUserRequest)
      urlGet(fullUrl(noSurchargeAmountUrl(taxYearEOY)), user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
    }
    result
  }

  private def showPage(anUnauthorisedPaymentsViewModel: UnauthorisedPaymentsViewModel) = {
    lazy val result: WSResponse = {
      dropPensionsDB()
      authoriseAgentOrIndividual(isAgent = false)
      val viewModel = anUnauthorisedPaymentsViewModel
      insertCyaData(pensionsUserDataWithUnauthorisedPayments(viewModel, isPriorSubmission = false), aUserRequest)
      urlGet(fullUrl(noSurchargeAmountUrl(taxYearEOY)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
    }
    result
  }

  private def submitPage(user: UserScenario[CommonExpectedResults, SpecificExpectedResults],
                           anUnauthorisedPaymentsViewModel: UnauthorisedPaymentsViewModel,
                           form: Map[String, String]) = {
    val result: WSResponse = {
      dropPensionsDB()
      val pensionsViewModel = anUnauthorisedPaymentsViewModel.copy(noSurchargeAmount = None)
      insertCyaData(pensionsUserDataWithUnauthorisedPayments(pensionsViewModel, isPriorSubmission = false), aUserRequest)
      authoriseAgentOrIndividual(user.isAgent)
      urlPost(fullUrl(noSurchargeAmountUrl(taxYearEOY)), body = form, welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
    }

    result
  }

  private def submitPage(anUnauthorisedPaymentsViewModel: UnauthorisedPaymentsViewModel,
                           form: Map[String, String]) = {
    val result: WSResponse = {
      dropPensionsDB()
      insertCyaData(pensionsUserDataWithUnauthorisedPayments(anUnauthorisedPaymentsViewModel), aUserRequest)
      authoriseAgentOrIndividual(isAgent = false)
      urlPost(fullUrl(noSurchargeAmountUrl(taxYearEOY)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
    }

    result
  }

  private def getResponseNoSessionData = {
    val result: WSResponse = {
      dropPensionsDB()
      authoriseAgentOrIndividual(isAgent = false)
      urlGet(fullUrl(noSurchargeAmountUrl(taxYearEOY)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
    }
    result
  }
}
