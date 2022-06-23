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

package controllers.pensions.incomeFromPensions

import builders.IncomeFromPensionsViewModelBuilder.{anIncomeFromPensionEmptyViewModel, anIncomeFromPensionsViewModel}
import builders.PensionsUserDataBuilder.{aPensionsUserData, pensionsUserDataWithIncomeFromPensions}
import builders.UkPensionIncomeViewModelBuilder.{anUkPensionIncomeViewModelOne, anUkPensionIncomeViewModelTwo}
import builders.UserBuilder.aUserRequest
import forms.PensionSchemeDetailsForm
import models.pension.statebenefits.UkPensionIncomeViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.IncomeFromPensionsPages.{pensionAmountUrl, pensionSchemeDetailsUrl, ukPensionIncomeCyaUrl, ukPensionSchemePayments, ukPensionSchemeSummaryListUrl}
import utils.PageUrls.{fullUrl, pensionSummaryUrl}
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

class PensionSchemeDetailsControllerISpec extends IntegrationTest with ViewHelpers with BeforeAndAfterEach with PensionsDatabaseHelper {

  private val providerNameInputName = "providerName"
  private val refInputName = "schemeReference"
  private val pIdInputName = "pensionId"
  private val validRef = "123/ABCab"
  private val validProviderName = "Valid Provider Name -&"
  private val validPensionId = "Valid Pension Id +-/"

  def pensionDetailsForm(providerName: String, ref: String, id: String): Map[String, String] = Map(
    PensionSchemeDetailsForm.providerName -> providerName,
    PensionSchemeDetailsForm.schemeReference -> ref,
    PensionSchemeDetailsForm.pensionId -> id)

  object Selectors {
    val captionSelector: String = "#main-content > div > div > header > p"
    val continueButtonSelector: String = "#continue"
    val formSelector: String = "#main-content > div > div > form"
    val refHintSelector = "#schemeReference-hint"
    val pIdHintSelector = "#pensionId-hint"
    val providerNameInputSelector = "#providerName"
    val refInputSelector = "#schemeReference"
    val pIdInputSelector = "#pensionId"
    val providerNameErrorHref = "#providerName"
    val refErrorHref = "#schemeReference"
    val pIdErrorHref = "#pensionId"

    def paragraphSelector(index: Int): String = s"#main-content > div > div > p:nth-of-type($index)"

    def labelSelector(index: Int): String = s"form > div:nth-of-type($index) > label"
  }

  trait CommonExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedErrorTitle: String
    val expectedCaption: Int => String
    val buttonText: String
    val refHintText: String
    val providerNameLabel: String
    val referenceLabel: String
    val pIdLabel: String
    val providerNameEmptyErrorText: String
    val refEmptyErrorText: String
    val pIdEmptyErrorText: String
    val providerNameInvalidFormatErrorText: String
    val refInvalidFormatErrorText: String
    val pIdInvalidFormatErrorText: String
    val providerNameOverCharLimitErrorText: String
    val pIdOverCharLimitText: String
  }

  trait SpecificExpectedResults {
    val expectedYouCanFindThisParagraph: String
    val expectedIfYouGetParagraph: String
    val pIdHintText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedTitle = "Pension scheme details"
    val expectedHeading = "Pension scheme details"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedCaption: Int => String = (taxYear: Int) => s"Income from pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val buttonText = "Continue"
    val providerNameLabel = "Pension provider name"
    val referenceLabel = "Pension scheme PAYE reference number"
    val pIdLabel = "Pension Identification (PID)"
    val refHintText = "For example 123/AB456"
    val providerNameEmptyErrorText = "Enter the pension provider name"
    val refEmptyErrorText = "Enter the pension scheme PAYE reference number"
    val pIdEmptyErrorText = "Enter the pension identification (PID)"
    val providerNameInvalidFormatErrorText: String = "The pension provider name must only include numbers 0 to 9, " +
      "letters a to z, hyphens, spaces, apostrophes, commas, full stops, round brackets, and the special characters &\\:"
    val refInvalidFormatErrorText = "Enter the pension scheme PAYE reference number in the correct format"
    val pIdInvalidFormatErrorText: String = "The pension identification (PID) must only include numbers " +
      "0 to 9, letters a to z, hyphens, spaces, apostrophes, commas, full stops, round brackets, and the special characters /=!\"%&*;<>+:\\?"
    val providerNameOverCharLimitErrorText = "The pension provider name must be 74 characters or fewer"
    val pIdOverCharLimitText = "The pension identification (PID) must be 38 characters or fewer"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedTitle = "Pension scheme details"
    val expectedHeading = "Pension scheme details"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedCaption: Int => String = (taxYear: Int) => s"Income from pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val buttonText = "Continue"
    val providerNameLabel = "Pension provider name"
    val referenceLabel = "Pension scheme PAYE reference number"
    val pIdLabel = "Pension Identification (PID)"
    val refHintText = "For example 123/AB456"
    val providerNameEmptyErrorText = "Enter the pension provider name"
    val refEmptyErrorText = "Enter the pension scheme PAYE reference number"
    val pIdEmptyErrorText = "Enter the pension identification (PID)"
    val providerNameInvalidFormatErrorText: String = "The pension provider name must only include numbers 0 to 9, " +
      "letters a to z, hyphens, spaces, apostrophes, commas, full stops, round brackets, and the special characters &\\:"
    val refInvalidFormatErrorText = "Enter the pension scheme PAYE reference number in the correct format"
    val pIdInvalidFormatErrorText: String = "The pension identification (PID) must only include numbers " +
      "0 to 9, letters a to z, hyphens, spaces, apostrophes, commas, full stops, round brackets, and the special characters /=!\"%&*;<>+:\\?"
    val providerNameOverCharLimitErrorText = "The pension provider name must be 74 characters or fewer"
    val pIdOverCharLimitText = "The pension identification (PID) must be 38 characters or fewer"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedYouCanFindThisParagraph: String = "You can find this information on your pension statement. " +
      "If you do not have a pension statement, you can ask your pension provider."
    val expectedIfYouGetParagraph = "If you get pension income from more than one UK pension scheme, you can add them later."
    val pIdHintText = "Check your pension statement or P60"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedYouCanFindThisParagraph: String = "You can find this information on your pension statement. " +
      "If you do not have a pension statement, you can ask your pension provider."
    val expectedIfYouGetParagraph = "If you get pension income from more than one UK pension scheme, you can add them later."
    val pIdHintText = "Check your pension statement or P60"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedYouCanFindThisParagraph: String = "Your client can find this information on their pension statement. " +
      "If they do not have a pension statement, they can ask their pension provider."
    val expectedIfYouGetParagraph = "If your client gets pension income from more than one UK pension scheme, you can add them later."
    val pIdHintText = "Check your client’s pension statement or P60"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedYouCanFindThisParagraph: String = "Your client can find this information on their pension statement. " +
      "If they do not have a pension statement, they can ask their pension provider."
    val expectedIfYouGetParagraph = "If your client gets pension income from more than one UK pension scheme, you can add them later."
    val pIdHintText = "Check your client’s pension statement or P60"
  }


  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))

  ".show" should {
    userScenarios.foreach { user =>
      import Selectors._
      import user.commonExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        "render Did you get a Pension Scheme Details page with no prefilling" which {
          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(aPensionsUserData, aUserRequest)
            urlGet(fullUrl(pensionSchemeDetailsUrl(taxYearEOY)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(expectedTitle)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedYouCanFindThisParagraph, paragraphSelector(1))
          textOnPageCheck(user.specificExpectedResults.get.expectedIfYouGetParagraph, paragraphSelector(2))
          textOnPageCheck(providerNameLabel, labelSelector(1))
          textOnPageCheck(referenceLabel, labelSelector(2))
          textOnPageCheck(pIdLabel, labelSelector(3))
          textOnPageCheck(refHintText, refHintSelector)
          textOnPageCheck(user.specificExpectedResults.get.pIdHintText, pIdHintSelector)
          inputFieldValueCheck(providerNameInputName, providerNameInputSelector, "")
          inputFieldValueCheck(refInputName, refInputSelector, "")
          inputFieldValueCheck(pIdInputName, pIdInputSelector, "")
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(pensionSchemeDetailsUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render Did you get a Pension Scheme Details page with prefilled data" which {
          lazy val pensionIncomeModel = UkPensionIncomeViewModel(
            pensionSchemeName = Some("Scheme Name"), pensionSchemeRef = Some("123/123AB"), pensionId = Some("Pension Id"))

          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(pensionsUserDataWithIncomeFromPensions(
              anIncomeFromPensionEmptyViewModel.copy(uKPensionIncomesQuestion = Some(true), uKPensionIncomes = Seq(pensionIncomeModel))), aUserRequest)
            urlGet(fullUrl(pensionSchemeDetailsUrl(taxYearEOY, 0)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(expectedTitle)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedYouCanFindThisParagraph, paragraphSelector(1))
          textOnPageCheck(user.specificExpectedResults.get.expectedIfYouGetParagraph, paragraphSelector(2))
          textOnPageCheck(providerNameLabel, labelSelector(1))
          textOnPageCheck(referenceLabel, labelSelector(2))
          textOnPageCheck(pIdLabel, labelSelector(3))
          textOnPageCheck(refHintText, refHintSelector)
          textOnPageCheck(user.specificExpectedResults.get.pIdHintText, pIdHintSelector)
          inputFieldValueCheck(providerNameInputName, providerNameInputSelector, pensionIncomeModel.pensionSchemeName.get)
          inputFieldValueCheck(refInputName, refInputSelector, pensionIncomeModel.pensionSchemeRef.get)
          inputFieldValueCheck(pIdInputName, pIdInputSelector, pensionIncomeModel.pensionId.get)
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(pensionSchemeDetailsUrl(taxYearEOY, 0), formSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "redirect to UK Pension Scheme Payments question page if uKPensionIncomesQuestion is Some(false)" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(pensionsUserDataWithIncomeFromPensions(
          anIncomeFromPensionsViewModel.copy(uKPensionIncomesQuestion = Some(false), uKPensionIncomes = Seq.empty)), aUserRequest)
        urlGet(fullUrl(pensionSchemeDetailsUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(ukPensionSchemePayments(taxYearEOY))
      }
    }

    "redirect to Uk Pension Income CYA page when there is no session data" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(pensionSchemeDetailsUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(ukPensionIncomeCyaUrl(taxYearEOY))
      }
    }

    "redirect to the Uk Pension Incomes Summary page if index is out of bounds" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val viewModel = anIncomeFromPensionEmptyViewModel.copy(uKPensionIncomesQuestion = Some(true), uKPensionIncomes = Seq(anUkPensionIncomeViewModelOne))
        insertCyaData(pensionsUserDataWithIncomeFromPensions(viewModel), aUserRequest)
        urlGet(fullUrl(pensionSchemeDetailsUrl(taxYearEOY, 2)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(ukPensionSchemeSummaryListUrl(taxYearEOY))
      }
    }
  }

  "submit" should {
    userScenarios.foreach { user =>
      import Selectors._
      import user.commonExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        "return correct errors when form is submitted with all fields empty" which {
          lazy val form: Map[String, String] = pensionDetailsForm("", "", "")

          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(aPensionsUserData, aUserRequest)
            urlPost(fullUrl(pensionSchemeDetailsUrl(taxYearEOY)), body = form, welsh = user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has an BAD REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(expectedErrorTitle)
          h1Check(expectedHeading)
          inputFieldValueCheck(providerNameInputName, providerNameInputSelector, "")
          inputFieldValueCheck(refInputName, refInputSelector, "")
          inputFieldValueCheck(pIdInputName, pIdInputSelector, "")
          multipleSummaryErrorCheck(List(
            (providerNameEmptyErrorText, providerNameErrorHref),
            (refEmptyErrorText, refErrorHref),
            (pIdEmptyErrorText, pIdErrorHref)))
          errorAboveElementCheck(providerNameEmptyErrorText, Some(providerNameInputName))
          errorAboveElementCheck(refEmptyErrorText, Some(refInputName))
          errorAboveElementCheck(pIdEmptyErrorText, Some(pIdInputName))
        }

        "return correct errors when form is submitted with invalid format for all fields" which {
          lazy val form: Map[String, String] = pensionDetailsForm("<>", "<>", "{}")

          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(aPensionsUserData, aUserRequest)
            urlPost(fullUrl(pensionSchemeDetailsUrl(taxYearEOY)), body = form, welsh = user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has an BAD REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(expectedErrorTitle)
          h1Check(expectedHeading)
          inputFieldValueCheck(providerNameInputName, providerNameInputSelector, "<>")
          inputFieldValueCheck(refInputName, refInputSelector, "<>")
          inputFieldValueCheck(pIdInputName, pIdInputSelector, "{}")
          multipleSummaryErrorCheck(List(
            (providerNameInvalidFormatErrorText, providerNameErrorHref),
            (refInvalidFormatErrorText, refErrorHref),
            (pIdInvalidFormatErrorText, pIdErrorHref)))
          errorAboveElementCheck(providerNameInvalidFormatErrorText, Some(providerNameInputName))
          errorAboveElementCheck(refInvalidFormatErrorText, Some(refInputName))
          errorAboveElementCheck(pIdInvalidFormatErrorText, Some(pIdInputName))
        }

        "return correct errors when Scheme Reference is valid, but Provider Name and Pension ID have inputs over the char limit" which {
          lazy val providerNameTooLong = "Provider name with 75 characters Provider name with 75 characters 12345678."
          lazy val pidTooLong = "PensionId with 39 chars. That's 2 much."
          lazy val form: Map[String, String] = pensionDetailsForm(providerNameTooLong, validRef, pidTooLong)

          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(aPensionsUserData, aUserRequest)
            urlPost(fullUrl(pensionSchemeDetailsUrl(taxYearEOY)), body = form, welsh = user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has an BAD REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(expectedErrorTitle)
          h1Check(expectedHeading)
          inputFieldValueCheck(providerNameInputName, providerNameInputSelector, providerNameTooLong)
          inputFieldValueCheck(refInputName, refInputSelector, validRef)
          inputFieldValueCheck(pIdInputName, pIdInputSelector, pidTooLong)
          multipleSummaryErrorCheck(List(
            (providerNameOverCharLimitErrorText, providerNameErrorHref),
            (pIdOverCharLimitText, pIdErrorHref)))
          errorAboveElementCheck(providerNameOverCharLimitErrorText, Some(providerNameInputName))
          errorAboveElementCheck(pIdOverCharLimitText, Some(pIdInputName))
        }
      }
    }
  }

  "redirect and update and existing Pension Scheme at given index when valid form is submitted" which {

    lazy val form: Map[String, String] = pensionDetailsForm(validProviderName, validRef, validPensionId)

    lazy val result: WSResponse = {
      dropPensionsDB()
      authoriseAgentOrIndividual(isAgent = false)
      val uKPensionIncomesModel = anUkPensionIncomeViewModelTwo.copy(
        pensionSchemeName = Some("Scheme Name"), pensionSchemeRef = Some("123/PREF"), pensionId = Some("Pension Id"))
      insertCyaData(pensionsUserDataWithIncomeFromPensions(
        anIncomeFromPensionsViewModel.copy(uKPensionIncomes = Seq(anUkPensionIncomeViewModelOne, uKPensionIncomesModel))), aUserRequest)
      urlPost(fullUrl(pensionSchemeDetailsUrl(taxYearEOY, 1)), body = form, follow = false,
        headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
    }

    "has an SEE_OTHER(303) status" in {
      result.status shouldBe SEE_OTHER
      result.header("location") shouldBe Some(pensionAmountUrl(taxYearEOY, 1))
    }

    "updates existing pension scheme with new values" in {
      lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
      cyaModel.pensions.incomeFromPensions.uKPensionIncomes.size shouldBe 2
      cyaModel.pensions.incomeFromPensions.uKPensionIncomes(1).pensionSchemeName shouldBe Some(validProviderName)
      cyaModel.pensions.incomeFromPensions.uKPensionIncomes(1).pensionSchemeRef shouldBe Some(validRef)
      cyaModel.pensions.incomeFromPensions.uKPensionIncomes(1).pensionId shouldBe Some(validPensionId)
      cyaModel.pensions.incomeFromPensions.uKPensionIncomes(1).employmentId shouldBe anUkPensionIncomeViewModelTwo.employmentId
    }
  }

  "redirect and update session data when valid form is submitted and there are no existing Pension Schemes" which {

    lazy val form: Map[String, String] = pensionDetailsForm(validProviderName, validRef, validPensionId)

    lazy val result: WSResponse = {
      dropPensionsDB()
      authoriseAgentOrIndividual(isAgent = false)
      insertCyaData(pensionsUserDataWithIncomeFromPensions(anIncomeFromPensionEmptyViewModel.copy(uKPensionIncomesQuestion = Some(true))), aUserRequest)
      urlPost(fullUrl(pensionSchemeDetailsUrl(taxYearEOY)), body = form, follow = false,
        headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
    }

    "has an SEE_OTHER(303) status" in {
      result.status shouldBe SEE_OTHER
      result.header("location") shouldBe Some(pensionAmountUrl(taxYearEOY, 0))
    }

    "updates existing pension scheme with new values" in {
      lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
      cyaModel.pensions.incomeFromPensions.uKPensionIncomes.size shouldBe 1
      cyaModel.pensions.incomeFromPensions.uKPensionIncomes.head.pensionSchemeName shouldBe Some(validProviderName)
      cyaModel.pensions.incomeFromPensions.uKPensionIncomes.head.pensionSchemeRef shouldBe Some(validRef)
      cyaModel.pensions.incomeFromPensions.uKPensionIncomes.head.pensionId shouldBe Some(validPensionId)
      cyaModel.pensions.incomeFromPensions.uKPensionIncomes.head.employmentId shouldBe None
    }
  }

  "redirect and add to an existing Uk Pension Schemes List when no index given and valid form is submitted" which {

    lazy val form: Map[String, String] = pensionDetailsForm(validProviderName, validRef, validPensionId)

    lazy val result: WSResponse = {
      dropPensionsDB()
      authoriseAgentOrIndividual(isAgent = false)
      insertCyaData(pensionsUserDataWithIncomeFromPensions(anIncomeFromPensionsViewModel), aUserRequest)
      urlPost(fullUrl(pensionSchemeDetailsUrl(taxYearEOY)), body = form, follow = false,
        headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
    }

    "has an SEE_OTHER(303) status" in {
      result.status shouldBe SEE_OTHER
      result.header("location") shouldBe Some(pensionAmountUrl(taxYearEOY, 2))
    }

    "updates existing pension scheme with new values without changing existing pension schemes" in {
      lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
      cyaModel.pensions.incomeFromPensions.uKPensionIncomes.size shouldBe 3
      cyaModel.pensions.incomeFromPensions.uKPensionIncomes(0).pensionSchemeName shouldBe anIncomeFromPensionsViewModel.uKPensionIncomes(0).pensionSchemeName
      cyaModel.pensions.incomeFromPensions.uKPensionIncomes(1).pensionSchemeName shouldBe anIncomeFromPensionsViewModel.uKPensionIncomes(1).pensionSchemeName
      cyaModel.pensions.incomeFromPensions.uKPensionIncomes(2).pensionSchemeName shouldBe Some(validProviderName)
      cyaModel.pensions.incomeFromPensions.uKPensionIncomes(2).pensionSchemeRef shouldBe Some(validRef)
      cyaModel.pensions.incomeFromPensions.uKPensionIncomes(2).pensionId shouldBe Some(validPensionId)
    }
  }

  "redirect to Uk Pension Income CYA Page if there is no session data" which {
    lazy val form: Map[String, String] = pensionDetailsForm(validProviderName, validRef, validPensionId)

    lazy val result: WSResponse = {
      dropPensionsDB()
      authoriseAgentOrIndividual(isAgent = false)
      urlPost(fullUrl(pensionSchemeDetailsUrl(taxYearEOY)), body = form, follow = false,
        headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
    }

    "has an SEE_OTHER(303) status" in {
      result.status shouldBe SEE_OTHER
      result.header("location") shouldBe Some(ukPensionIncomeCyaUrl(taxYearEOY))
    }
  }

  "redirect to the Uk Pension Incomes Summary page if index is out of bounds" which {
    lazy val form: Map[String, String] = pensionDetailsForm(validProviderName, validRef, validPensionId)

    lazy val result: WSResponse = {
      dropPensionsDB()
      authoriseAgentOrIndividual(isAgent = false)
      val viewModel = anIncomeFromPensionEmptyViewModel.copy(uKPensionIncomesQuestion = Some(true))
      insertCyaData(pensionsUserDataWithIncomeFromPensions(viewModel), aUserRequest)
      urlPost(fullUrl(pensionSchemeDetailsUrl(taxYearEOY, 0)), body = form, follow = false,
        headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
    }

    "has an SEE_OTHER(303) status" in {
      result.status shouldBe SEE_OTHER
      result.header("location") shouldBe Some(ukPensionSchemeSummaryListUrl(taxYearEOY))
    }
  }
}
