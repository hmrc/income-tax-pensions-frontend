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

package controllers.pensions.annualAllowances

import builders.PensionAnnualAllowanceViewModelBuilder.{aPensionAnnualAllowanceEmptyViewModel, aPensionAnnualAllowanceViewModel}
import builders.PensionsUserDataBuilder.pensionsUserDataWithAnnualAllowances
import builders.UserBuilder.aUserRequest
import forms.ReducedAnnualAllowanceTypeQuestionForm
import forms.ReducedAnnualAllowanceTypeQuestionForm.{moneyPurchaseCheckboxValue, taperedCheckboxValue}
import models.pension.charges.PensionAnnualAllowancesViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.PensionAnnualAllowancePages.{aboveReducedAnnualAllowanceUrl, annualAllowancesCYAUrl, reducedAnnualAllowanceTypeUrl, reducedAnnualAllowanceUrl}
import utils.PageUrls.{fullUrl, pensionSummaryUrl}
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

// scalastyle:off magic.number
class ReducedAnnualAllowanceTypeControllerISpec extends IntegrationTest with BeforeAndAfterEach with ViewHelpers with PensionsDatabaseHelper {

  private val externalHref = "https://www.gov.uk/guidance/pension-schemes-work-out-your-tapered-annual-allowance"

  object Selectors {
    val checkboxHintSelector = "#reducedAnnualAllowanceType-hint"
    val captionSelector: String = "#main-content > div > div > form > div > fieldset > legend > h1 > header > p"
    val continueButtonSelector: String = "#continue"
    val formSelector: String = "#main-content > div > div > form"
    val checkboxMoneyPurchaseSelector = "#reducedAnnualAllowanceType"
    val checkboxTaperedSelector = "#reducedAnnualAllowanceType-2"
    val expectedDetailsRevealTextSelector = "#main-content > div > div > form > details > summary > span"
    val expectedDetailsLinkSelector = "#tapered-info-link"

    def bulletSelector(index: Int): String = s"#main-content > div > div > form > details > div > ul > li:nth-child($index)"

    def detailsParagraphSelector(index: Int): String = s"#main-content > div > div > form > details > div > p:nth-child($index)"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    lazy val expectedHeading = expectedTitle
    val expectedError: String
    val expectedErrorTitle: String
    val expectedDetailsMoneyParagraphText: String
    val expectedDetailsTaperedParagraphText: String
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val checkboxHint: String
    val checkboxMoneyPurchaseText: String
    val checkboxTaperedText: String
    val expectedButtonText: String
    val expectedDetailsRevealText: String
    val expectedDetailsBullet1: String
    val expectedDetailsBullet2: String
    val expectedDetailsMoneyPurchaseText: String
    val expectedDetailsTaperedText: String
    val expectedDetailsExternalLinkText: String

  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "What type of reduced annual allowance do you have?"
    val expectedError = "Select the type of reduced annual allowance you have"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedDetailsMoneyParagraphText = "You’ll have this type of allowance if you flexibly access your pension. For example, this could include taking:"
    val expectedDetailsTaperedParagraphText: String = "You’ll have this type of annual allowance if both your ‘threshold income’ " +
      "and ‘adjusted income’ are over the limit (opens in new tab)."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Pa fath o lwfans blynyddol wedi’i ostwng sydd gennych?"
    val expectedError = "Dewiswch y math o lwfans blynyddol wedi’i ostwng sydd gennych"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedDetailsMoneyParagraphText = "Bydd gennych y math hwn o lwfans os ydych yn cyrchu eich pensiwn yn hyblyg. " +
      "Er enghraifft, gallai hyn gynnwys cymryd:"
    val expectedDetailsTaperedParagraphText: String = "Bydd gennych y math hwn o lwfans blynyddol os yw eich ‘incwm trothwy’ " +
      "yn ogystal â’ch ‘incwm wedi’i addasu’ yn dros y terfyn (yn agor tab newydd)."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "What type of reduced annual allowance does your client have?"
    val expectedError = "Select the type of reduced annual allowance your client has"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedDetailsMoneyParagraphText: String = "Your client will have this type of allowance if they flexibly access their pension. " +
      "For example, this could include taking:"
    val expectedDetailsTaperedParagraphText: String = "Your client will have this type of annual allowance if both their ‘threshold income’ " +
      "and ‘adjusted income’ are over the limit (opens in new tab)."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Pa fath o lwfans blynyddol wedi’i ostwng sydd gan eich cleient?"
    val expectedError = "Dewiswch y math o lwfans blynyddol wedi’i ostwng sydd gan eich cleient"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedDetailsMoneyParagraphText: String = "Bydd gan eich cleient y math hwn o lwfans os bydd yn cyrchu ei bensiwn yn hyblyg. " +
      "Er enghraifft, gallai hyn gynnwys cymryd:"
    val expectedDetailsTaperedParagraphText: String = "Bydd gan eich cleient y math hwn o lwfans blynyddol os yw ei ‘incwm trothwy’ " +
      "yn ogystal â’i ‘incwm wedi’i addasu’ yn dros y terfyn (yn agor tab newydd)."
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Annual allowances for 6 April ${taxYear - 1} to 5 April $taxYear"
    val checkboxHint = "Select all that apply."
    val checkboxMoneyPurchaseText = "Money purchase annual allowance"
    val checkboxTaperedText = "Tapered annual allowance"
    val expectedDetailsRevealText = "More information about the types of reduced annual allowance"
    val expectedDetailsMoneyPurchaseText = "Money purchase annual allowance"
    val expectedDetailsBullet1 = "income from a flexi-access drawdown fund"
    val expectedDetailsBullet2 = "cash directly from a pension pot (‘uncrystallised funds pension lump sums’)"
    val expectedDetailsTaperedText = "Tapered annual allowance"
    val expectedButtonText = "Continue"
    val expectedDetailsExternalLinkText = "over the limit (opens in new tab)"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Lwfansau blynyddol ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val checkboxHint = "Dewiswch bob un sy’n berthnasol."
    val checkboxMoneyPurchaseText = "Lwfans blynyddol pryniannau arian"
    val checkboxTaperedText = "Lwfans blynyddol wedi’i feinhau"
    val expectedDetailsRevealText = "Rhagor o wybodaeth am y mathau o lwfans blynyddol wedi’u gostwng"
    val expectedDetailsMoneyPurchaseText = "Lwfans blynyddol pryniannau arian"
    val expectedDetailsBullet1 = "incwm a gyrchir yn hyblyg o gronfa"
    val expectedDetailsBullet2 = "arian parod yn uniongyrchol o gronfa bensiwn (‘arian heb ei ddefnyddio ar ffurf cyfandaliad pensiwn’)"
    val expectedDetailsTaperedText = "Lwfans blynyddol wedi’i feinhau"
    val expectedDetailsExternalLinkText = "dros y terfyn (yn agor tab newydd)"
    val expectedButtonText = "Yn eich blaen"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  ".show" should {
    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        import Selectors._
        import user.commonExpectedResults._

        "render 'What type of reduced annual allowance do you have?' page with correct content and no pre-filling" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropPensionsDB()
            val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(
              reducedAnnualAllowanceQuestion = Some(true),
              moneyPurchaseAnnualAllowance = None,
              taperedAnnualAllowance = None
            )

            insertCyaData(pensionsUserDataWithAnnualAllowances(pensionsViewModel))
            urlGet(fullUrl(reducedAnnualAllowanceTypeUrl(taxYearEOY)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          hintTextCheck(checkboxHint, Selectors.checkboxHintSelector)
          checkBoxCheck(checkboxMoneyPurchaseText, 1, checked = Some(false))
          checkBoxCheck(checkboxTaperedText, 2, checked = Some(false))
          inputFieldValueCheck("reducedAnnualAllowanceType[]", Selectors.checkboxMoneyPurchaseSelector, moneyPurchaseCheckboxValue)
          inputFieldValueCheck("reducedAnnualAllowanceType[]", Selectors.checkboxTaperedSelector, taperedCheckboxValue)

          textOnPageCheck(expectedDetailsRevealText, Selectors.expectedDetailsRevealTextSelector)
          textOnPageCheck(expectedDetailsMoneyPurchaseText, Selectors.detailsParagraphSelector(1))
          textOnPageCheck(user.specificExpectedResults.get.expectedDetailsMoneyParagraphText, Selectors.detailsParagraphSelector(2))
          textOnPageCheck(expectedDetailsBullet1, Selectors.bulletSelector(1))
          textOnPageCheck(expectedDetailsBullet2, Selectors.bulletSelector(2))
          textOnPageCheck(expectedDetailsTaperedText, Selectors.detailsParagraphSelector(4))
          textOnPageCheck(user.specificExpectedResults.get.expectedDetailsTaperedParagraphText, Selectors.detailsParagraphSelector(5))
          linkCheck(expectedDetailsExternalLinkText,
            expectedDetailsLinkSelector, externalHref)

          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(reducedAnnualAllowanceTypeUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)

        }

        "render 'What type of reduced annual allowance do you have?' page with correct content and both checkboxes checked" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropPensionsDB()
            val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(
              reducedAnnualAllowanceQuestion = Some(true),
              moneyPurchaseAnnualAllowance = Some(true),
              taperedAnnualAllowance = Some(true)
            )

            insertCyaData(pensionsUserDataWithAnnualAllowances(pensionsViewModel))
            urlGet(fullUrl(reducedAnnualAllowanceTypeUrl(taxYearEOY)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          hintTextCheck(checkboxHint, Selectors.checkboxHintSelector)
          checkBoxCheck(checkboxMoneyPurchaseText, 1, checked = Some(true))
          checkBoxCheck(checkboxTaperedText, 2, checked = Some(true))
          inputFieldValueCheck("reducedAnnualAllowanceType[]", Selectors.checkboxMoneyPurchaseSelector, moneyPurchaseCheckboxValue)
          inputFieldValueCheck("reducedAnnualAllowanceType[]", Selectors.checkboxTaperedSelector, taperedCheckboxValue)

          textOnPageCheck(expectedDetailsRevealText, Selectors.expectedDetailsRevealTextSelector)
          textOnPageCheck(expectedDetailsMoneyPurchaseText, Selectors.detailsParagraphSelector(1))
          textOnPageCheck(user.specificExpectedResults.get.expectedDetailsMoneyParagraphText, Selectors.detailsParagraphSelector(2))
          textOnPageCheck(expectedDetailsBullet1, Selectors.bulletSelector(1))
          textOnPageCheck(expectedDetailsBullet2, Selectors.bulletSelector(2))
          textOnPageCheck(expectedDetailsTaperedText, Selectors.detailsParagraphSelector(4))
          textOnPageCheck(user.specificExpectedResults.get.expectedDetailsTaperedParagraphText, Selectors.detailsParagraphSelector(5))
          linkCheck(expectedDetailsExternalLinkText,
            expectedDetailsLinkSelector, externalHref)

          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(reducedAnnualAllowanceTypeUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render 'What type of reduced annual allowance do you have?' page with correct content and tapered checkbox only checked" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropPensionsDB()
            val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(
              reducedAnnualAllowanceQuestion = Some(true),
              moneyPurchaseAnnualAllowance = None,
              taperedAnnualAllowance = Some(true)
            )

            insertCyaData(pensionsUserDataWithAnnualAllowances(pensionsViewModel))
            urlGet(fullUrl(reducedAnnualAllowanceTypeUrl(taxYearEOY)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          hintTextCheck(checkboxHint, Selectors.checkboxHintSelector)
          checkBoxCheck(checkboxMoneyPurchaseText, 1, checked = None)
          checkBoxCheck(checkboxTaperedText, 2, checked = Some(true))
          inputFieldValueCheck("reducedAnnualAllowanceType[]", Selectors.checkboxMoneyPurchaseSelector, moneyPurchaseCheckboxValue)
          inputFieldValueCheck("reducedAnnualAllowanceType[]", Selectors.checkboxTaperedSelector, taperedCheckboxValue)

          textOnPageCheck(expectedDetailsRevealText, Selectors.expectedDetailsRevealTextSelector)
          textOnPageCheck(expectedDetailsMoneyPurchaseText, Selectors.detailsParagraphSelector(1))
          textOnPageCheck(user.specificExpectedResults.get.expectedDetailsMoneyParagraphText, Selectors.detailsParagraphSelector(2))
          textOnPageCheck(expectedDetailsBullet1, Selectors.bulletSelector(1))
          textOnPageCheck(expectedDetailsBullet2, Selectors.bulletSelector(2))
          textOnPageCheck(expectedDetailsTaperedText, Selectors.detailsParagraphSelector(4))
          textOnPageCheck(user.specificExpectedResults.get.expectedDetailsTaperedParagraphText, Selectors.detailsParagraphSelector(5))
          linkCheck(expectedDetailsExternalLinkText,
            expectedDetailsLinkSelector, externalHref)

          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(reducedAnnualAllowanceTypeUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render 'What type of reduced annual allowance do you have?' page with correct content and money purchase checkbox only checked" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropPensionsDB()
            val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(
              reducedAnnualAllowanceQuestion = Some(true),
              moneyPurchaseAnnualAllowance = Some(true),
              taperedAnnualAllowance = None
            )

            insertCyaData(pensionsUserDataWithAnnualAllowances(pensionsViewModel))
            urlGet(fullUrl(reducedAnnualAllowanceTypeUrl(taxYearEOY)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          hintTextCheck(checkboxHint, Selectors.checkboxHintSelector)
          checkBoxCheck(checkboxMoneyPurchaseText, 1, checked = Some(true))
          checkBoxCheck(checkboxTaperedText, 2, checked = None)
          inputFieldValueCheck("reducedAnnualAllowanceType[]", Selectors.checkboxMoneyPurchaseSelector, moneyPurchaseCheckboxValue)
          inputFieldValueCheck("reducedAnnualAllowanceType[]", Selectors.checkboxTaperedSelector, taperedCheckboxValue)

          textOnPageCheck(expectedDetailsRevealText, Selectors.expectedDetailsRevealTextSelector)
          textOnPageCheck(expectedDetailsMoneyPurchaseText, Selectors.detailsParagraphSelector(1))
          textOnPageCheck(user.specificExpectedResults.get.expectedDetailsMoneyParagraphText, Selectors.detailsParagraphSelector(2))
          textOnPageCheck(expectedDetailsBullet1, Selectors.bulletSelector(1))
          textOnPageCheck(expectedDetailsBullet2, Selectors.bulletSelector(2))
          textOnPageCheck(expectedDetailsTaperedText, Selectors.detailsParagraphSelector(4))
          textOnPageCheck(user.specificExpectedResults.get.expectedDetailsTaperedParagraphText, Selectors.detailsParagraphSelector(5))
          linkCheck(expectedDetailsExternalLinkText,
            expectedDetailsLinkSelector, externalHref)

          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(reducedAnnualAllowanceTypeUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "redirect to the reduced annual allowance question page if it has not been answered" which {

      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual()
        dropPensionsDB()
        val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(
          reducedAnnualAllowanceQuestion = None,
          moneyPurchaseAnnualAllowance = None,
          taperedAnnualAllowance = None
        )

        insertCyaData(pensionsUserDataWithAnnualAllowances(pensionsViewModel))
        urlGet(fullUrl(reducedAnnualAllowanceTypeUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(reducedAnnualAllowanceUrl(taxYearEOY)) shouldBe true
      }

    }

    "redirect to the reduced annual allowance question page if it has been answered as No" which {

      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual()
        dropPensionsDB()
        val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(
          reducedAnnualAllowanceQuestion = Some(false),
          moneyPurchaseAnnualAllowance = None,
          taperedAnnualAllowance = None
        )

        insertCyaData(pensionsUserDataWithAnnualAllowances(pensionsViewModel))
        urlGet(fullUrl(reducedAnnualAllowanceTypeUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(reducedAnnualAllowanceUrl(taxYearEOY)) shouldBe true
      }

    }

    "redirect to the CYA page if there is no session data" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        // no cya insert
        urlGet(fullUrl(reducedAnnualAllowanceTypeUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        //TODO - go to annual allowance cya page when available
        result.header("location").contains(pensionSummaryUrl(taxYearEOY)) shouldBe true
      }

    }

  }

  ".submit" should {

    val validFormAllChecked = Map(
      s"${ReducedAnnualAllowanceTypeQuestionForm.reducedAnnualAllowanceType}[]" -> Seq(moneyPurchaseCheckboxValue, taperedCheckboxValue))

    userScenarios.foreach { user =>

      import Selectors._
      import user.commonExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        val form = Map(s"${ReducedAnnualAllowanceTypeQuestionForm.reducedAnnualAllowanceType}[]" -> Seq.empty)

        s"return $BAD_REQUEST error when no value is submitted" which {

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropPensionsDB()
            val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(
              reducedAnnualAllowanceQuestion = Some(true),
              moneyPurchaseAnnualAllowance = Some(true),
              taperedAnnualAllowance = Some(true)
            )

            insertCyaData(pensionsUserDataWithAnnualAllowances(pensionsViewModel))
            urlPost(fullUrl(reducedAnnualAllowanceTypeUrl(taxYearEOY)), body = form, user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has an BAD_REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedErrorTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          hintTextCheck(checkboxHint, Selectors.checkboxHintSelector)
          checkBoxCheck(checkboxMoneyPurchaseText, 1, checked = None)
          checkBoxCheck(checkboxTaperedText, 2, checked = None)
          inputFieldValueCheck("reducedAnnualAllowanceType[]", Selectors.checkboxMoneyPurchaseSelector, moneyPurchaseCheckboxValue)
          inputFieldValueCheck("reducedAnnualAllowanceType[]", Selectors.checkboxTaperedSelector, taperedCheckboxValue)

          textOnPageCheck(expectedDetailsRevealText, Selectors.expectedDetailsRevealTextSelector)
          textOnPageCheck(expectedDetailsMoneyPurchaseText, Selectors.detailsParagraphSelector(1))
          textOnPageCheck(user.specificExpectedResults.get.expectedDetailsMoneyParagraphText, Selectors.detailsParagraphSelector(2))
          textOnPageCheck(expectedDetailsBullet1, Selectors.bulletSelector(1))
          textOnPageCheck(expectedDetailsBullet2, Selectors.bulletSelector(2))
          textOnPageCheck(expectedDetailsTaperedText, Selectors.detailsParagraphSelector(4))
          textOnPageCheck(user.specificExpectedResults.get.expectedDetailsTaperedParagraphText, Selectors.detailsParagraphSelector(5))
          linkCheck(expectedDetailsExternalLinkText,
            expectedDetailsLinkSelector, externalHref)
          errorSummaryCheck(user.specificExpectedResults.get.expectedError, checkboxMoneyPurchaseSelector)
          errorAboveElementCheck(user.specificExpectedResults.get.expectedError, Some("reducedAnnualAllowanceType"))

          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(reducedAnnualAllowanceTypeUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "redirect to Above Annual Allowance page and update CYA session" when {
      "user submits a valid form with both checkboxes checked" which {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual()
          dropPensionsDB()
          insertCyaData(pensionsUserDataWithAnnualAllowances(aPensionAnnualAllowanceEmptyViewModel.copy(
            reducedAnnualAllowanceQuestion = Some(true))))
          urlPost(fullUrl(reducedAnnualAllowanceTypeUrl(taxYearEOY)), body = validFormAllChecked, follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        "has a SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(aboveReducedAnnualAllowanceUrl(taxYearEOY))
        }

        "updates moneyPurchaseAnnualAllowance and taperedAnnualAllowance to Some(true)" in {
          lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
          cyaModel.pensions.pensionsAnnualAllowances shouldBe PensionAnnualAllowancesViewModel(
            reducedAnnualAllowanceQuestion = Some(true),
            moneyPurchaseAnnualAllowance = Some(true),
            taperedAnnualAllowance = Some(true))
        }
      }

      "user submits a valid form with one type checked" which {
        val form = Map(
          s"${ReducedAnnualAllowanceTypeQuestionForm.reducedAnnualAllowanceType}[]" -> Seq(taperedCheckboxValue))
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual()
          dropPensionsDB()
          insertCyaData(pensionsUserDataWithAnnualAllowances(aPensionAnnualAllowanceEmptyViewModel.copy(
            reducedAnnualAllowanceQuestion = Some(true))))
          urlPost(fullUrl(reducedAnnualAllowanceTypeUrl(taxYearEOY)), body = form, follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        "has a SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(aboveReducedAnnualAllowanceUrl(taxYearEOY))
        }

        "updates taperedAnnualAllowance to Some(true)" in {
          lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
          cyaModel.pensions.pensionsAnnualAllowances shouldBe PensionAnnualAllowancesViewModel(
            reducedAnnualAllowanceQuestion = Some(true),
            moneyPurchaseAnnualAllowance = Some(false),
            taperedAnnualAllowance = Some(true))
        }
      }
    }

    "redirect to CYA page and update CYA session when type is changed from existing data and CYA is now complete" which {
      val form = Map(
        s"${ReducedAnnualAllowanceTypeQuestionForm.reducedAnnualAllowanceType}[]" -> Seq(moneyPurchaseCheckboxValue))
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual()
        dropPensionsDB()
        insertCyaData(pensionsUserDataWithAnnualAllowances(aPensionAnnualAllowanceViewModel))
        urlPost(fullUrl(reducedAnnualAllowanceTypeUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(annualAllowancesCYAUrl(taxYearEOY))
      }

      "updates taperedAnnualAllowance to Some(false)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.pensionsAnnualAllowances shouldBe aPensionAnnualAllowanceViewModel.copy(taperedAnnualAllowance = Some(false))
      }
    }

    "redirect to the first page in journey" when {
      "previous questions are unanswered" which {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual()
          dropPensionsDB()
          insertCyaData(pensionsUserDataWithAnnualAllowances(aPensionAnnualAllowanceEmptyViewModel))
          urlPost(fullUrl(reducedAnnualAllowanceTypeUrl(taxYearEOY)), body = validFormAllChecked, follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }
        "has an SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(reducedAnnualAllowanceUrl(taxYearEOY))
        }
      }

      "page is invalid in journey" which {
        implicit lazy val result: WSResponse = {
          authoriseAgentOrIndividual()
          dropPensionsDB()
          insertCyaData(pensionsUserDataWithAnnualAllowances(aPensionAnnualAllowanceEmptyViewModel.copy(
            reducedAnnualAllowanceQuestion = Some(false))))
          urlPost(fullUrl(reducedAnnualAllowanceTypeUrl(taxYearEOY)), body = validFormAllChecked, follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }
        "has an SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(reducedAnnualAllowanceUrl(taxYearEOY))
        }
      }
    }

    "redirect to the Pension Summary page if there is no session data" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        urlPost(fullUrl(reducedAnnualAllowanceTypeUrl(taxYearEOY)), body = validFormAllChecked, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }
      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
      }
    }
  }
}
// scalastyle:on magic.number
