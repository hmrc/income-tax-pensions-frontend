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

package controllers.pensions.incomeFromPensions

import builders.IncomeFromPensionsViewModelBuilder.{aUKIncomeFromPensionsViewModel, anIncomeFromPensionEmptyViewModel, anIncomeFromPensionsViewModel}
import builders.PensionsUserDataBuilder.{aPensionsUserData, pensionsUserDataWithIncomeFromPensions}
import builders.UkPensionIncomeViewModelBuilder.{anUkPensionIncomeViewModelOne, anUkPensionIncomeViewModelTwo}
import builders.UserBuilder.aUserRequest
import forms.PensionSchemeDetailsForm
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.IncomeFromPensionsPages._
import utils.PageUrls.{fullUrl, pensionSummaryUrl}
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

class PensionSchemeDetailsControllerISpec extends IntegrationTest with ViewHelpers with BeforeAndAfterEach with PensionsDatabaseHelper {

  val providerNameInputName = "providerName"
  val refInputName = "schemeReference"
  val pIdInputName = "pensionId"
  val validRef = "123/ABCab"
  val validProviderName = "Valid Provider Name -&"
  val validPensionId = "Valid Pension Id +-/"

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
    lazy val expectedHeading: String = expectedTitle
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
    val expectedTitle = "Manylion y cynllun pensiwn"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedCaption: Int => String = (taxYear: Int) => s"Incwm o bensiynau ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val buttonText = "Yn eich blaen"
    val providerNameLabel = "Enw’r darparwr pensiwn"
    val referenceLabel = "Cyfeirnod TWE y cynllun pensiwn"
    val pIdLabel = "Rhif dynodydd pensiwn (PID)"
    val refHintText = "Er enghraifft, 123/AB456"
    val providerNameEmptyErrorText = "Nodwch enw’r darparwr pensiwn"
    val refEmptyErrorText = "Nodwch gyfeirnod TWE y cynllun pensiwn"
    val pIdEmptyErrorText = "Nodwch rif dynodydd y pensiwn (PID)"
    val providerNameInvalidFormatErrorText: String = "Mae’n rhaid i enw’r darparwr pensiwn gynnwys y rhifau 0 i 9, " +
      "llythrennau a-z, cysylltnodau, bylchau, collnodau, comas, atalnodau llawn, cromfachau crwn a’r cymeriadau arbennig &\\: yn unig"
    val refInvalidFormatErrorText = "Nodwch gyfeirnod TWE y cynllun pensiwn yn y fformat cywir"
    val pIdInvalidFormatErrorText: String = "Mae’n rhaid i rif dynodydd y pensiwn gynnwys y rhifau " +
      "0 i 9, llythrennau a-z, cysylltnodau, bylchau, collnodau, comas, atalnodau llawn, cromfachau crwn a’r cymeriadau arbennig /=!\"%&*;<>+:\\? yn unig"
    val providerNameOverCharLimitErrorText = "Mae’n rhaid i enw’r darparwr pensiwn fod yn 74 o gymeriadau neu lai"
    val pIdOverCharLimitText = "Mae’n rhaid i rif dynodydd y pensiwn (PID) fod yn 38 o gymeriadau neu lai"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedYouCanFindThisParagraph: String = "You can find this information on your pension statement. " +
      "If you do not have a pension statement, you can ask your pension provider."
    val expectedIfYouGetParagraph = "If you get pension income from more than one UK pension scheme, you can add them later."
    val pIdHintText = "Check your pension statement or P60"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedYouCanFindThisParagraph: String = "Gallwch ddod o hyd i’r wybodaeth hon ar eich datganiad pensiwn. " +
      "Os nad oes gennych ddatganiad pensiwn, gallwch ofyn i’ch darparwr pensiwn."
    val expectedIfYouGetParagraph = "Os cawsoch incwm o bensiwn o fwy nag un cynllun pensiwn y DU, gallwch eu hychwanegu nes ymlaen."
    val pIdHintText = "Gwirio’ch datganiad pensiwn neu P60"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedYouCanFindThisParagraph: String = "Your client can find this information on their pension statement. " +
      "If they do not have a pension statement, they can ask their pension provider."
    val expectedIfYouGetParagraph = "If your client gets pension income from more than one UK pension scheme, you can add them later."
    val pIdHintText = "Check your client’s pension statement or P60"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedYouCanFindThisParagraph: String = "Gall eich cleient ddod o hyd i’r wybodaeth hon ar ei ddatganiad pensiwn. " +
      "Os nad oes ganddo ddatganiad pensiwn, gall ofyn i’w ddarparwr pensiwn."
    val expectedIfYouGetParagraph = "Os cafodd eich cleient incwm o bensiwn o fwy nag un cynllun pensiwn y DU, gallwch eu hychwanegu nes ymlaen."
    val pIdHintText = "Gwirio datganiad pensiwn neu P60 eich cleient"
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
          lazy val pensionIncomeModel = anIncomeFromPensionEmptyViewModel.copy(uKPensionIncomesQuestion = Some(true))
          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(pensionsUserDataWithIncomeFromPensions(pensionIncomeModel))
            urlGet(fullUrl(pensionSchemeDetailsUrl(taxYearEOY, None)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(expectedTitle, user.isWelsh)
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
          formPostLinkCheck(pensionSchemeDetailsUrl(taxYearEOY, None), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render Did you get a Pension Scheme Details page with prefilled data" which {
          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(aPensionsUserData)
            urlGet(fullUrl(pensionSchemeDetailsUrl(taxYearEOY, Some(0))), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(expectedTitle, user.isWelsh)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedYouCanFindThisParagraph, paragraphSelector(1))
          textOnPageCheck(user.specificExpectedResults.get.expectedIfYouGetParagraph, paragraphSelector(2))
          textOnPageCheck(providerNameLabel, labelSelector(1))
          textOnPageCheck(referenceLabel, labelSelector(2))
          textOnPageCheck(pIdLabel, labelSelector(3))
          textOnPageCheck(refHintText, refHintSelector)
          textOnPageCheck(user.specificExpectedResults.get.pIdHintText, pIdHintSelector)
          inputFieldValueCheck(providerNameInputName, providerNameInputSelector, anUkPensionIncomeViewModelOne.pensionSchemeName.get)
          inputFieldValueCheck(refInputName, refInputSelector, anUkPensionIncomeViewModelOne.pensionSchemeRef.get)
          inputFieldValueCheck(pIdInputName, pIdInputSelector, anUkPensionIncomeViewModelOne.pensionId.get)
          buttonCheck(buttonText, continueButtonSelector)
          formPostLinkCheck(pensionSchemeDetailsUrl(taxYearEOY, Some(0)), formSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "redirect to the Pensions Summary page if there is no session data" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        urlGet(fullUrl(pensionSchemeDetailsUrl(taxYearEOY, None)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
      }
    }

    "redirect to the first page in journey" when {
      "page is invalid in journey" which {
        val invalidJourney = anIncomeFromPensionEmptyViewModel.copy(uKPensionIncomesQuestion = Some(false))
        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual()
          insertCyaData(pensionsUserDataWithIncomeFromPensions(invalidJourney))
          urlGet(fullUrl(pensionSchemeDetailsUrl(taxYearEOY, Some(0))), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(ukPensionSchemePayments(taxYearEOY))
        }
      }

      "previous questions are unanswered" which {
        val incompleteJourney = aUKIncomeFromPensionsViewModel.copy(
          uKPensionIncomesQuestion = None)
        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual()
          insertCyaData(pensionsUserDataWithIncomeFromPensions(incompleteJourney))
          urlGet(fullUrl(pensionSchemeDetailsUrl(taxYearEOY, Some(0))), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(ukPensionSchemePayments(taxYearEOY))
        }
      }

      "index is invalid" which {
        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual()
          val viewModel = anIncomeFromPensionEmptyViewModel.copy(uKPensionIncomesQuestion = Some(true), uKPensionIncomes = Seq(anUkPensionIncomeViewModelOne))
          insertCyaData(pensionsUserDataWithIncomeFromPensions(viewModel))
          urlGet(fullUrl(pensionSchemeDetailsUrl(taxYearEOY, Some(8))), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        "has an SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(ukPensionSchemePayments(taxYearEOY))
        }
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
            insertCyaData(aPensionsUserData)
            urlPost(fullUrl(pensionSchemeDetailsUrl(taxYearEOY, None)), body = form, welsh = user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has an BAD REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(expectedErrorTitle, user.isWelsh)
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
            insertCyaData(aPensionsUserData)
            urlPost(fullUrl(pensionSchemeDetailsUrl(taxYearEOY, None)), body = form, welsh = user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has an BAD REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(expectedErrorTitle, user.isWelsh)
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
            insertCyaData(aPensionsUserData)
            urlPost(fullUrl(pensionSchemeDetailsUrl(taxYearEOY, None)), body = form, welsh = user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has an BAD REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(expectedErrorTitle, user.isWelsh)
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

    "redirect to scheme summary page and update a completed Pension Scheme at given index when valid form is submitted" which {

      lazy val form: Map[String, String] = pensionDetailsForm(validProviderName, validRef, validPensionId)

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        val uKPensionIncomesModel = anUkPensionIncomeViewModelTwo.copy(
          pensionSchemeName = Some("Scheme Name"), pensionSchemeRef = Some("123/PREF"), pensionId = Some("Pension Id"))
        insertCyaData(pensionsUserDataWithIncomeFromPensions(
          anIncomeFromPensionsViewModel.copy(uKPensionIncomes = Seq(anUkPensionIncomeViewModelOne, uKPensionIncomesModel))))
        urlPost(fullUrl(pensionSchemeDetailsUrl(taxYearEOY, Some(1))), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionSchemeSummaryUrl(taxYearEOY, Some(1)))
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
        authoriseAgentOrIndividual()
        insertCyaData(pensionsUserDataWithIncomeFromPensions(anIncomeFromPensionEmptyViewModel.copy(uKPensionIncomesQuestion = Some(true))))
        urlPost(fullUrl(pensionSchemeDetailsUrl(taxYearEOY, None)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionAmountUrl(taxYearEOY, Some(0)))
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

    "redirect and add to an existing Uk Pension Schemes List when index is None and valid form is submitted" which {

      lazy val form: Map[String, String] = pensionDetailsForm(validProviderName, validRef, validPensionId)

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        insertCyaData(pensionsUserDataWithIncomeFromPensions(anIncomeFromPensionsViewModel))
        urlPost(fullUrl(pensionSchemeDetailsUrl(taxYearEOY, None)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionAmountUrl(taxYearEOY, Some(2)))
      }

      "updates existing pension scheme with new values without changing existing pension schemes" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.incomeFromPensions.uKPensionIncomes.size shouldBe 3
        cyaModel.pensions.incomeFromPensions.uKPensionIncomes.head.pensionSchemeName shouldBe
          anIncomeFromPensionsViewModel.uKPensionIncomes.head.pensionSchemeName
        cyaModel.pensions.incomeFromPensions.uKPensionIncomes(1).pensionSchemeName shouldBe
          anIncomeFromPensionsViewModel.uKPensionIncomes(1).pensionSchemeName
        cyaModel.pensions.incomeFromPensions.uKPensionIncomes(2).pensionSchemeName shouldBe Some(validProviderName)
        cyaModel.pensions.incomeFromPensions.uKPensionIncomes(2).pensionSchemeRef shouldBe Some(validRef)
        cyaModel.pensions.incomeFromPensions.uKPensionIncomes(2).pensionId shouldBe Some(validPensionId)
      }
    }

    "redirect to Pension Summary page when there is no session data" which {
      lazy val form: Map[String, String] = pensionDetailsForm(validProviderName, validRef, validPensionId)

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        urlPost(fullUrl(pensionSchemeDetailsUrl(taxYearEOY, None)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
      }
    }

    "redirect to the first page in journey" when {
      lazy val form: Map[String, String] = pensionDetailsForm(validProviderName, validRef, validPensionId)

      "page is invalid in journey" which {
        val invalidJourney = anIncomeFromPensionEmptyViewModel.copy(uKPensionIncomesQuestion = Some(false))
        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual()
          insertCyaData(pensionsUserDataWithIncomeFromPensions(invalidJourney))
          urlPost(fullUrl(pensionSchemeDetailsUrl(taxYearEOY, Some(0))), body = form,
            follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(ukPensionSchemePayments(taxYearEOY))
        }
      }

      "previous questions are unanswered" which {
        val incompleteJourney = aUKIncomeFromPensionsViewModel.copy(
          uKPensionIncomesQuestion = None)
        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual()
          insertCyaData(pensionsUserDataWithIncomeFromPensions(incompleteJourney))
          urlPost(fullUrl(pensionSchemeDetailsUrl(taxYearEOY, Some(0))), body = form,
            follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(ukPensionSchemePayments(taxYearEOY))
        }
      }

      "index is invalid" which {
        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual()
          insertCyaData(pensionsUserDataWithIncomeFromPensions(aUKIncomeFromPensionsViewModel))
          urlPost(fullUrl(pensionSchemeDetailsUrl(taxYearEOY, Some(4))), body = form,
            follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        "has an SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(ukPensionSchemePayments(taxYearEOY))
        }
      }
    }
  }
}
