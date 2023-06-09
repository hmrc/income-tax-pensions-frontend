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

package controllers.pensions.unauthorisedPayments

import builders.AllPensionsDataBuilder.anAllPensionsData
import builders.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import builders.PensionsUserDataBuilder.pensionsUserDataWithUnauthorisedPayments
import builders.UnauthorisedPaymentsViewModelBuilder.{anUnauthorisedPaymentsEmptyViewModel, anUnauthorisedPaymentsViewModel}
import builders.UserBuilder.aUserRequest
import forms.UnAuthorisedPaymentsForm
import forms.UnAuthorisedPaymentsForm.{noValue, yesNotSurchargeValue, yesSurchargeValue}
import models.pension.charges.UnauthorisedPaymentsViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.UnAuthorisedPayments.{noSurchargeAmountUrl, surchargeAmountUrl}
import utils.PageUrls.fullUrl
import utils.PageUrls.unauthorisedPaymentsPages.{checkUnauthorisedPaymentsCyaUrl, unauthorisedPaymentsUrl}
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

class UnauthorisedPaymentsControllerISpec extends IntegrationTest with BeforeAndAfterEach with ViewHelpers with PensionsDatabaseHelper {

  private val externalHref = "https://www.gov.uk/guidance/pension-schemes-and-unauthorised-payments"

  object Selectors {
    val checkboxHintSelector = "#unauthorisedPayments-hint"
    val captionSelector: String = "#main-content > div > div > header > p"
    val paragraphTextSelector: String = "#paymentsOutside"
    val paragraphText1Selector: String = "#moreThanOneUnauthorisedPayment"
    val continueButtonSelector: String = "#continue"
    val formSelector: String = "#main-content > div > div > form"
    val yesSurchargeSelector = "#unauthorisedPayments"
    val yesNotSurchargeSelector = "#unauthorisedPayments-2"
    val noSelector = "#unauthorisedPayments-4"
    val expectedDetailsLinkSelector = "#unauthorised-find-out-more-link"
    val subHeadingSelector = "#didYouGetAnUnauthorisedPayment"
    val errorSelector = "#unauthorisedPayments-error"

    def labelIndex(index: Int): String = s"#main-content > div > div > form > div:nth-child($index) > label"

    def bulletSelector(index: Int): String = s"#main-content > div > div > form > details > div > ul > li:nth-child($index)"

    def detailsParagraphSelector(index: Int): String = s"#main-content > div > div > form > details > div > p:nth-child($index)"
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedButtonText: String
    val expectedTitle: String
    lazy val expectedHeading = expectedTitle
    val expectedError: String
    val expectedErrorTitle: String
    val expectedSubHeading: String
    val checkboxHint: String
    val expectedParagraphText: String
    val expectedParagraphText1: String
    val expectedYesSurchargeCheckboxText: String
    val expectedYesNotSurchargeCheckboxText: String
    val expectedNoSurchargeCheckboxText: String
    val noEntryErrorMessage: String
    val expectedDetailsExternalLinkText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Unauthorised payments from pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedButtonText = "Continue"
    val expectedTitle = "Unauthorised payments"
    val expectedError = "Select yes if you got an unauthorised payment from a pension scheme"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedSubHeading = "Did you get an unauthorised payment from a pension scheme?"
    val expectedParagraphText = "Unauthorised payments are made outside the tax rules"
    val expectedParagraphText1: String = "If you got more than one unauthorised payment, you might " +
      "have paid a surcharge (an extra fee) on some of them. It all " +
      "depends on if you’ve taken 25% or more of your pension pots."
    val expectedYesSurchargeCheckboxText = "Yes, unauthorised payments that resulted in a surcharge"
    val expectedYesNotSurchargeCheckboxText = "Yes, unauthorised payments that did not result in a surcharge"
    val expectedNoSurchargeCheckboxText = "No"
    val noEntryErrorMessage = "Select yes if you got an unauthorised payment from a pension scheme"
    val checkboxHint = "Select all that apply."
    val expectedDetailsExternalLinkText = "Find out more about unauthorised payments (opens in new tab)"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Taliadau heb awdurdod o bensiynau ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val expectedButtonText = "Yn eich blaen"
    val expectedTitle = "Taliadau heb awdurdod"
    val expectedError = "Dewiswch ‘Iawn’ os cawsoch daliad heb awdurdod o gynllun pensiwn"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedSubHeading = "A gawsoch daliad heb awdurdod o gynllun pensiwn?"
    val expectedParagraphText = "Gwneir taliadau heb awdurdod y tu allan i’r rheolau treth"
    val expectedParagraphText1: String = "Os cawsoch fwy nag un taliad heb awdurdod, mae’n bosibl " +
      "eich bod wedi talu gordal (ffi ychwanegol) ar rai ohonynt. Mae’n " +
      "dibynnu p’un a ydych wedi cymryd 25% neu fwy o’ch cronfeydd pensiwn."
    val expectedYesSurchargeCheckboxText = "Iawn, taliadau heb awdurdod a wnaeth arwain at ordal"
    val expectedYesNotSurchargeCheckboxText = "Iawn, taliadau heb awdurdod na wnaethant arwain at ordal"
    val expectedNoSurchargeCheckboxText = "Na"
    val noEntryErrorMessage = "Dewiswch ‘Iawn’ os cawsoch daliad heb awdurdod o gynllun pensiwn"
    val checkboxHint = "Dewiswch bob un sy’n berthnasol."
    val expectedDetailsExternalLinkText = "Dysgwch ragor am daliadau heb awdurdod (yn agor tab newydd)"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, _]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY)
  )

  ".show" should {
    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        import Selectors._
        import user.commonExpectedResults._

        "render the 'unauthorised payments page' with no pre filling" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropPensionsDB()

            val pensionsViewModel = anUnauthorisedPaymentsEmptyViewModel.copy()

            insertCyaData(pensionsUserDataWithUnauthorisedPayments(pensionsViewModel, isPriorSubmission = false))
            urlGet(fullUrl(unauthorisedPaymentsUrl(taxYearEOY)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(expectedTitle, user.isWelsh)
          h1Check(user.commonExpectedResults.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          hintTextCheck(checkboxHint, Selectors.checkboxHintSelector)
          checkBoxCheck(expectedYesSurchargeCheckboxText, 1, checked = Some(false))
          checkBoxCheck(expectedYesNotSurchargeCheckboxText, 2, checked = Some(false))
          checkBoxCheck(expectedNoSurchargeCheckboxText, 3, checked = Some(false))
          inputFieldValueCheck("unauthorisedPayments[]", Selectors.yesSurchargeSelector, yesSurchargeValue)
          inputFieldValueCheck("unauthorisedPayments[]", Selectors.yesNotSurchargeSelector, yesNotSurchargeValue)
          inputFieldValueCheck("unauthorisedPayments[]", Selectors.noSelector, noValue)
          textOnPageCheck(expectedParagraphText, Selectors.paragraphTextSelector)
          textOnPageCheck(expectedParagraphText1, Selectors.paragraphText1Selector)
          linkCheck(expectedDetailsExternalLinkText, expectedDetailsLinkSelector, externalHref)
          textOnPageCheck(expectedSubHeading, Selectors.subHeadingSelector)
          buttonCheck(expectedButtonText, continueButtonSelector)
          welshToggleCheck(user.isWelsh)
          formPostLinkCheck(unauthorisedPaymentsUrl(taxYearEOY), formSelector)
        }
      }
    }

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        import Selectors._
        import user.commonExpectedResults._

        "render the 'unauthorised payments page' with no surchargeQuestion true" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropPensionsDB()

            val pensionsViewModel: UnauthorisedPaymentsViewModel = UnauthorisedPaymentsViewModel().copy(surchargeQuestion = Some(true))

            insertCyaData(pensionsUserDataWithUnauthorisedPayments(pensionsViewModel, isPriorSubmission = false))
            urlGet(fullUrl(unauthorisedPaymentsUrl(taxYearEOY)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(expectedTitle, user.isWelsh)
          h1Check(user.commonExpectedResults.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          hintTextCheck(checkboxHint, Selectors.checkboxHintSelector)
          checkBoxCheck(expectedYesSurchargeCheckboxText, 1, checked = Some(true))
          checkBoxCheck(expectedYesNotSurchargeCheckboxText, 2, checked = Some(false))
          checkBoxCheck(expectedNoSurchargeCheckboxText, 3, checked = Some(false))
          inputFieldValueCheck("unauthorisedPayments[]", Selectors.yesSurchargeSelector, yesSurchargeValue)
          inputFieldValueCheck("unauthorisedPayments[]", Selectors.yesNotSurchargeSelector, yesNotSurchargeValue)
          inputFieldValueCheck("unauthorisedPayments[]", Selectors.noSelector, noValue)
          textOnPageCheck(expectedParagraphText, Selectors.paragraphTextSelector)
          textOnPageCheck(expectedParagraphText1, Selectors.paragraphText1Selector)
          linkCheck(expectedDetailsExternalLinkText, expectedDetailsLinkSelector, externalHref)
          textOnPageCheck(expectedSubHeading, Selectors.subHeadingSelector)
          buttonCheck(expectedButtonText, continueButtonSelector)
          welshToggleCheck(user.isWelsh)
          formPostLinkCheck(unauthorisedPaymentsUrl(taxYearEOY), formSelector)
        }
      }
    }

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        import Selectors._
        import user.commonExpectedResults._

        "render the 'unauthorised payments page' with noSurchargeQuestion true" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropPensionsDB()

            val pensionsViewModel: UnauthorisedPaymentsViewModel = UnauthorisedPaymentsViewModel().copy(noSurchargeQuestion = Some(true))

            insertCyaData(pensionsUserDataWithUnauthorisedPayments(pensionsViewModel, isPriorSubmission = false))
            urlGet(fullUrl(unauthorisedPaymentsUrl(taxYearEOY)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(expectedTitle, user.isWelsh)
          h1Check(user.commonExpectedResults.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          hintTextCheck(checkboxHint, Selectors.checkboxHintSelector)
          checkBoxCheck(expectedYesSurchargeCheckboxText, 1, checked = Some(false))
          checkBoxCheck(expectedYesNotSurchargeCheckboxText, 2, checked = Some(true))
          checkBoxCheck(expectedNoSurchargeCheckboxText, 3, checked = Some(false))
          inputFieldValueCheck("unauthorisedPayments[]", Selectors.yesSurchargeSelector, yesSurchargeValue)
          inputFieldValueCheck("unauthorisedPayments[]", Selectors.yesNotSurchargeSelector, yesNotSurchargeValue)
          inputFieldValueCheck("unauthorisedPayments[]", Selectors.noSelector, noValue)
          textOnPageCheck(expectedParagraphText, Selectors.paragraphTextSelector)
          textOnPageCheck(expectedParagraphText1, Selectors.paragraphText1Selector)
          linkCheck(expectedDetailsExternalLinkText, expectedDetailsLinkSelector, externalHref)
          textOnPageCheck(expectedSubHeading, Selectors.subHeadingSelector)
          buttonCheck(expectedButtonText, continueButtonSelector)
          welshToggleCheck(user.isWelsh)
          formPostLinkCheck(unauthorisedPaymentsUrl(taxYearEOY), formSelector)
        }
      }
    }

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        import Selectors._
        import user.commonExpectedResults._

        "render the 'unauthorised payments page' with surchargeQuestion and noSurchargeQuestion true" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropPensionsDB()

            val pensionsViewModel: UnauthorisedPaymentsViewModel = UnauthorisedPaymentsViewModel().copy(surchargeQuestion = Some(true),
              noSurchargeQuestion = Some(true))

            insertCyaData(pensionsUserDataWithUnauthorisedPayments(pensionsViewModel, isPriorSubmission = false))
            urlGet(fullUrl(unauthorisedPaymentsUrl(taxYearEOY)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }
          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(expectedTitle, user.isWelsh)
          h1Check(user.commonExpectedResults.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          hintTextCheck(checkboxHint, Selectors.checkboxHintSelector)
          checkBoxCheck(expectedYesSurchargeCheckboxText, 1, checked = Some(true))
          checkBoxCheck(expectedYesNotSurchargeCheckboxText, 2, checked = Some(true))
          checkBoxCheck(expectedNoSurchargeCheckboxText, 3, checked = Some(false))
          inputFieldValueCheck("unauthorisedPayments[]", Selectors.yesSurchargeSelector, yesSurchargeValue)
          inputFieldValueCheck("unauthorisedPayments[]", Selectors.yesNotSurchargeSelector, yesNotSurchargeValue)
          inputFieldValueCheck("unauthorisedPayments[]", Selectors.noSelector, noValue)
          textOnPageCheck(expectedParagraphText, Selectors.paragraphTextSelector)
          textOnPageCheck(expectedParagraphText1, Selectors.paragraphText1Selector)
          linkCheck(expectedDetailsExternalLinkText, expectedDetailsLinkSelector, externalHref)
          textOnPageCheck(expectedSubHeading, Selectors.subHeadingSelector)
          buttonCheck(expectedButtonText, continueButtonSelector)
          welshToggleCheck(user.isWelsh)
          formPostLinkCheck(unauthorisedPaymentsUrl(taxYearEOY), formSelector)
        }
      }
    }

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        import Selectors._
        import user.commonExpectedResults._

        "render the 'unauthorised payments page' with noValue true" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropPensionsDB()

            val pensionsViewModel: UnauthorisedPaymentsViewModel = UnauthorisedPaymentsViewModel().copy(
              surchargeQuestion = Some(false), noSurchargeQuestion= Some(false)
            )

            insertCyaData(pensionsUserDataWithUnauthorisedPayments(pensionsViewModel, isPriorSubmission = false))
            urlGet(fullUrl(unauthorisedPaymentsUrl(taxYearEOY)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(expectedTitle, user.isWelsh)
          h1Check(user.commonExpectedResults.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          hintTextCheck(checkboxHint, Selectors.checkboxHintSelector)
          checkBoxCheck(expectedYesSurchargeCheckboxText, 1, checked = Some(false))
          checkBoxCheck(expectedYesNotSurchargeCheckboxText, 2, checked = Some(false))
          checkBoxCheck(expectedNoSurchargeCheckboxText, 3, checked = Some(true))
          inputFieldValueCheck("unauthorisedPayments[]", Selectors.yesSurchargeSelector, yesSurchargeValue)
          inputFieldValueCheck("unauthorisedPayments[]", Selectors.yesNotSurchargeSelector, yesNotSurchargeValue)
          inputFieldValueCheck("unauthorisedPayments[]", Selectors.noSelector, noValue)
          textOnPageCheck(expectedParagraphText, Selectors.paragraphTextSelector)
          textOnPageCheck(expectedParagraphText1, Selectors.paragraphText1Selector)
          linkCheck(expectedDetailsExternalLinkText, expectedDetailsLinkSelector, externalHref)
          textOnPageCheck(expectedSubHeading, Selectors.subHeadingSelector)
          buttonCheck(expectedButtonText, continueButtonSelector)
          welshToggleCheck(user.isWelsh)
          formPostLinkCheck(unauthorisedPaymentsUrl(taxYearEOY), formSelector)
        }
      }
    }

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        import Selectors._
        import user.commonExpectedResults._

        "render the 'unauthorised payments page' with noValue and surchargeQuestion and noSurchargeQuestion true" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropPensionsDB()

            val pensionsViewModel: UnauthorisedPaymentsViewModel = UnauthorisedPaymentsViewModel().copy(
              surchargeQuestion = Some(true), noSurchargeQuestion = Some(true)
            )

            insertCyaData(pensionsUserDataWithUnauthorisedPayments(pensionsViewModel, isPriorSubmission = false))
            urlGet(fullUrl(unauthorisedPaymentsUrl(taxYearEOY)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(expectedTitle, user.isWelsh)
          h1Check(user.commonExpectedResults.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          hintTextCheck(checkboxHint, Selectors.checkboxHintSelector)
          checkBoxCheck(expectedYesSurchargeCheckboxText, 1, checked = Some(true))
          checkBoxCheck(expectedYesNotSurchargeCheckboxText, 2, checked = Some(true))
          checkBoxCheck(expectedNoSurchargeCheckboxText, 3, checked = Some(false))
          inputFieldValueCheck("unauthorisedPayments[]", Selectors.yesSurchargeSelector, yesSurchargeValue)
          inputFieldValueCheck("unauthorisedPayments[]", Selectors.yesNotSurchargeSelector, yesNotSurchargeValue)
          inputFieldValueCheck("unauthorisedPayments[]", Selectors.noSelector, noValue)
          textOnPageCheck(expectedParagraphText, Selectors.paragraphTextSelector)
          textOnPageCheck(expectedParagraphText1, Selectors.paragraphText1Selector)
          linkCheck(expectedDetailsExternalLinkText, expectedDetailsLinkSelector, externalHref)
          textOnPageCheck(expectedSubHeading, Selectors.subHeadingSelector)
          buttonCheck(expectedButtonText, continueButtonSelector)
          welshToggleCheck(user.isWelsh)
          formPostLinkCheck(unauthorisedPaymentsUrl(taxYearEOY), formSelector)
        }
      }
    }
  }

  ".submit" should {
    userScenarios.foreach { user =>

      import Selectors._
      import user.commonExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        val form = Map(s"${UnAuthorisedPaymentsForm.unauthorisedPaymentsType}[]" -> Seq.empty)

        s"return $BAD_REQUEST error when no value is submitted" which {

          implicit lazy val result: WSResponse = {

            authoriseAgentOrIndividual(user.isAgent)
            dropPensionsDB()
            insertCyaData(pensionsUserDataWithUnauthorisedPayments(anUnauthorisedPaymentsViewModel, isPriorSubmission = false))
            userDataStub(anIncomeTaxUserData.copy(pensions = Some(anAllPensionsData)), nino, taxYearEOY)

            val pensionsViewModel = anUnauthorisedPaymentsViewModel.copy()

            insertCyaData(pensionsUserDataWithUnauthorisedPayments(pensionsViewModel, isPriorSubmission = false))
            urlPost(fullUrl(unauthorisedPaymentsUrl(taxYearEOY)), body = form, user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has an BAD_REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(expectedErrorTitle, user.isWelsh)
          h1Check(user.commonExpectedResults.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          hintTextCheck(checkboxHint, Selectors.checkboxHintSelector)
          checkBoxCheck(expectedYesSurchargeCheckboxText, 1, checked = Some(false))
          checkBoxCheck(expectedYesNotSurchargeCheckboxText, 2, checked = Some(false))
          checkBoxCheck(expectedNoSurchargeCheckboxText, 3, checked = Some(false))
          inputFieldValueCheck("unauthorisedPayments[]", Selectors.yesSurchargeSelector, yesSurchargeValue)
          inputFieldValueCheck("unauthorisedPayments[]", Selectors.yesNotSurchargeSelector, yesNotSurchargeValue)
          inputFieldValueCheck("unauthorisedPayments[]", Selectors.noSelector, noValue)
          textOnPageCheck(expectedParagraphText, Selectors.paragraphTextSelector)
          textOnPageCheck(expectedParagraphText1, Selectors.paragraphText1Selector)
          linkCheck(expectedDetailsExternalLinkText, expectedDetailsLinkSelector, externalHref)
          textOnPageCheck(expectedSubHeading, Selectors.subHeadingSelector)
          buttonCheck(expectedButtonText, continueButtonSelector)
          welshToggleCheck(user.isWelsh)
          formPostLinkCheck(unauthorisedPaymentsUrl(taxYearEOY), formSelector)

          errorSummaryCheck(user.commonExpectedResults.expectedError, "#")
          errorAboveElementCheck(user.commonExpectedResults.expectedError, Some("unauthorisedPayments"))
        }
      }

      s"redirects to surchargeAmountUrl when ${agentTest(user.isAgent)} submits a valid form with " +
        s"yesSurcharge checkboxes checked  in Language${welshTest(user.isWelsh)}" which {

        val form = {
          Map(s"${UnAuthorisedPaymentsForm.unauthorisedPaymentsType}[]" -> Seq(yesSurchargeValue))
        }

        implicit lazy val result: WSResponse = {

          authoriseAgentOrIndividual(user.isAgent)
          dropPensionsDB()

          val pensionsViewModel = anUnauthorisedPaymentsViewModel.copy()

          insertCyaData(pensionsUserDataWithUnauthorisedPayments(pensionsViewModel, isPriorSubmission = false))
          urlPost(fullUrl(unauthorisedPaymentsUrl(taxYearEOY)), body = form, user.isWelsh, follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        "has a SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(surchargeAmountUrl(taxYearEOY)) shouldBe true
        }
      }

      //TODO - redirect to Amount that did not result in a surcharge page once implemented
      s"redirects to surchargeAmountUrl when ${agentTest(user.isAgent)} submits a valid form with " +
        s"yesNotSurchargeValue checkboxes checked in Language ${welshTest(user.isWelsh)}" which {
        val form = {
          Map(s"${UnAuthorisedPaymentsForm.unauthorisedPaymentsType}[]" -> Seq("", yesNotSurchargeValue))
        }

        implicit lazy val result: WSResponse = {

          authoriseAgentOrIndividual(user.isAgent)
          dropPensionsDB()

          val pensionsViewModel = anUnauthorisedPaymentsViewModel.copy()

          insertCyaData(pensionsUserDataWithUnauthorisedPayments(pensionsViewModel, isPriorSubmission = false))
          urlPost(fullUrl(unauthorisedPaymentsUrl(taxYearEOY)), body = form, user.isWelsh, follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        "has a SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(noSurchargeAmountUrl(taxYearEOY)) shouldBe true
        }
      }

      s"redirects to CYA page when ${agentTest(user.isAgent)} submits a valid form with " +
        s"noValue checkboxes checked in Language ${welshTest(user.isWelsh)}" which {
          val form = {
            Map(s"${UnAuthorisedPaymentsForm.unauthorisedPaymentsType}[]" -> Seq("", "", noValue))
          }

          implicit lazy val result: WSResponse = {

            authoriseAgentOrIndividual(user.isAgent)
            dropPensionsDB()

            val pensionsViewModel = anUnauthorisedPaymentsViewModel.copy()

            insertCyaData(pensionsUserDataWithUnauthorisedPayments(pensionsViewModel, isPriorSubmission = false))
            urlPost(fullUrl(unauthorisedPaymentsUrl(taxYearEOY)), body = form, user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has a SEE_OTHER(303) status" in {
            result.status shouldBe SEE_OTHER
            result.header("location").contains(checkUnauthorisedPaymentsCyaUrl(taxYearEOY)) shouldBe true
          }
      }

      s"redirects to surchargeAmountUrl when ${agentTest(user.isAgent)} submits a valid form with both yesSurcharge " +
        s"and yesNotSurchargeValue checkboxes checked in Language ${welshTest(user.isWelsh)}" which {
        val form = {
          Map(s"${UnAuthorisedPaymentsForm.unauthorisedPaymentsType}[]" -> Seq(yesSurchargeValue, yesNotSurchargeValue))
        }

        implicit lazy val result: WSResponse = {

          authoriseAgentOrIndividual(user.isAgent)
          dropPensionsDB()

          val pensionsViewModel = anUnauthorisedPaymentsViewModel.copy()

          insertCyaData(pensionsUserDataWithUnauthorisedPayments(pensionsViewModel, isPriorSubmission = false))
          urlPost(fullUrl(unauthorisedPaymentsUrl(taxYearEOY)), body = form, user.isWelsh, follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        "has a SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(surchargeAmountUrl(taxYearEOY)) shouldBe true
        }
      }

      s"redirects to surchargeAmountUrl when ${agentTest(user.isAgent)} submits a valid form with yesSurcharge " +
        s"and yesNotSurchargeValue and noValue checkboxes checked in Language ${welshTest(user.isWelsh)}" which {
        val form = {
          Map(s"${UnAuthorisedPaymentsForm.unauthorisedPaymentsType}[]" -> Seq(yesSurchargeValue, yesNotSurchargeValue, noValue))
        }

        implicit lazy val result: WSResponse = {

          authoriseAgentOrIndividual(user.isAgent)
          dropPensionsDB()

          val pensionsViewModel = anUnauthorisedPaymentsViewModel.copy()

          insertCyaData(pensionsUserDataWithUnauthorisedPayments(pensionsViewModel, isPriorSubmission = false))
          urlPost(fullUrl(unauthorisedPaymentsUrl(taxYearEOY)), body = form, user.isWelsh, follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        "has a SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(surchargeAmountUrl(taxYearEOY)) shouldBe true
        }
      }
    }

    "clear the surcharge content if the surcharge question is not marked" in {
      val form = {
        Map(s"${UnAuthorisedPaymentsForm.unauthorisedPaymentsType}[]" -> Seq(yesNotSurchargeValue))
      }

      implicit lazy val result: WSResponse = {

        authoriseAgentOrIndividual()
        dropPensionsDB()

        val pensionsViewModel = anUnauthorisedPaymentsViewModel.copy()

        insertCyaData(pensionsUserDataWithUnauthorisedPayments(pensionsViewModel, isPriorSubmission = false))
        urlPost(fullUrl(unauthorisedPaymentsUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      result.status shouldBe SEE_OTHER

      val unauthorisedPayments = findCyaData(taxYearEOY, aUserRequest).get.pensions.unauthorisedPayments
      unauthorisedPayments.surchargeAmount shouldBe None
      unauthorisedPayments.surchargeTaxAmountQuestion shouldBe None
      unauthorisedPayments.surchargeTaxAmount shouldBe None
    }

    "clear the 'no surcharge' content if the 'no surcharge' question is not marked" in {
      val form = {
        Map(s"${UnAuthorisedPaymentsForm.unauthorisedPaymentsType}[]" -> Seq(yesSurchargeValue))
      }

      implicit lazy val result: WSResponse = {

        authoriseAgentOrIndividual()
        dropPensionsDB()

        val pensionsViewModel = anUnauthorisedPaymentsViewModel.copy()

        insertCyaData(pensionsUserDataWithUnauthorisedPayments(pensionsViewModel, isPriorSubmission = false))
        urlPost(fullUrl(unauthorisedPaymentsUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      result.status shouldBe SEE_OTHER

      val unauthorisedPayments = findCyaData(taxYearEOY, aUserRequest).get.pensions.unauthorisedPayments
      unauthorisedPayments.noSurchargeAmount shouldBe None
      unauthorisedPayments.noSurchargeTaxAmountQuestion shouldBe None
      unauthorisedPayments.noSurchargeTaxAmount shouldBe None
    }

    "clear both the 'surcharge' and the 'no surcharge' content if the 'no' option is marked" in {
      val form = {
        Map(s"${UnAuthorisedPaymentsForm.unauthorisedPaymentsType}[]" -> Seq(noValue))
      }

      implicit lazy val result: WSResponse = {

        authoriseAgentOrIndividual()
        dropPensionsDB()

        val pensionsViewModel = anUnauthorisedPaymentsViewModel.copy()

        insertCyaData(pensionsUserDataWithUnauthorisedPayments(pensionsViewModel, isPriorSubmission = false))
        urlPost(fullUrl(unauthorisedPaymentsUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      result.status shouldBe SEE_OTHER

      val unauthorisedPayments = findCyaData(taxYearEOY, aUserRequest).get.pensions.unauthorisedPayments
      unauthorisedPayments.surchargeAmount shouldBe None
      unauthorisedPayments.surchargeTaxAmountQuestion shouldBe None
      unauthorisedPayments.surchargeTaxAmount shouldBe None
      unauthorisedPayments.noSurchargeAmount shouldBe None
      unauthorisedPayments.noSurchargeTaxAmountQuestion shouldBe None
      unauthorisedPayments.noSurchargeTaxAmount shouldBe None
    }
  }
}
