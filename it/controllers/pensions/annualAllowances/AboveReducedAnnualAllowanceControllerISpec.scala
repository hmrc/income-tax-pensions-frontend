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

import builders.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import builders.PensionAnnualAllowanceViewModelBuilder.aPensionAnnualAllowanceViewModel
import builders.PensionsCYAModelBuilder._
import builders.PensionsUserDataBuilder
import builders.PensionsUserDataBuilder.aPensionsUserData
import builders.UserBuilder._
import forms.{RadioButtonAmountForm, YesNoForm}
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.pension.charges.PensionAnnualAllowancesViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.PensionAnnualAllowancePages._
import utils.PageUrls._
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

class AboveReducedAnnualAllowanceControllerISpec extends IntegrationTest with ViewHelpers with BeforeAndAfterEach with PensionsDatabaseHelper {

  private val externalHref = "https://www.tax.service.gov.uk/pension-annual-allowance-calculator"

  private def pensionsUsersData(pensionsCyaModel: PensionsCYAModel): PensionsUserData =
    PensionsUserDataBuilder.aPensionsUserData.copy(isPriorSubmission = false, pensions = pensionsCyaModel)

  private def pensionsUserDataWithAnnualAllowances(annualAllowances: PensionAnnualAllowancesViewModel,
                                                   isPriorSubmission: Boolean = true): PensionsUserData =
    aPensionsUserData.copy(isPriorSubmission = isPriorSubmission, pensions = aPensionsCYAModel.copy(pensionsAnnualAllowances = annualAllowances))

  object Selectors {
    val captionSelector: String        = "#main-content > div > div > header > p"
    val continueButtonSelector: String = "#continue"
    val formSelector: String           = "#main-content > div > div > form"
    val yesSelector                    = "#value"
    val noSelector                     = "#value-no"
    val amountHeadingSelector          = "#conditional-value > div > label"
    val amountValueSelector            = "#amount-2"
    val hintTextSelector: String       = "#amount-2-hint"
    lazy val expectedErrorHref: String = amountValueSelector
    val paragraphSelector: String      = s"#main-content > div > div > p"
    val expectedLinkSelector           = "#above-reduced-annual-allowance-link"
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val yesText: String
    val noText: String
    val buttonText: String
    val expectedUseACalculator: String
    val expectedLinkText: String
    val expectedAmountHeadingText: String
    val amountHintText: String
  }

  trait SpecificExpectedResults {
    val expectedReducedTitle: String
    lazy val expectedReducedHeading: String = expectedReducedTitle
    val expectedReducedErrorTitle: String
    val expectedNoEntryError: String
    val expectedNoAmountEntryError: String
    val expectedIncorrectFormatError: String
    val expectedOverMaxError: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Annual allowances for 6 April ${taxYear - 1} to 5 April $taxYear"
    val yesText                        = "Yes"
    val noText                         = "No"
    val buttonText                     = "Continue"
    val expectedLinkText               = "if you need to work this out (opens in new tab)"
    val expectedUseACalculator: String = s"Use a calculator $expectedLinkText."
    val expectedAmountHeadingText      = "Amount above the reduced annual allowance, in pounds"
    val amountHintText                 = "For example, £193.54"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Lwfans blynyddol ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val yesText                        = "Iawn"
    val noText                         = "Na"
    val buttonText                     = "Yn eich blaen"
    val expectedLinkText               = "os bydd angen i chi gyfrifo hyn (yn agor tab newydd)"
    val expectedUseACalculator: String = s"Defnyddiwch gyfrifiannell $expectedLinkText."
    val expectedAmountHeadingText      = "Swm uwchlaw’r lwfans blynyddol gostyngol, mewn punnoedd"
    val amountHintText                 = "Er enghraifft, £193.54"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedReducedTitle         = "Have you gone above your annual allowance?"
    val expectedReducedErrorTitle    = s"Error: $expectedReducedTitle"
    val expectedNoEntryError         = "Select yes if you have gone above your reduced annual allowance"
    val expectedNoAmountEntryError   = "Enter the amount above your reduced annual allowance"
    val expectedIncorrectFormatError = "Enter the amount above your reduced annual allowance in pounds and pence"
    val expectedOverMaxError         = "The amount above your reduced annual allowance must be less than £100,000,000,000"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedReducedTitle         = "A ydych wedi mynd yn uwch na’ch lwfans blynyddol wedi’i?"
    val expectedReducedErrorTitle    = s"Gwall: $expectedReducedTitle"
    val expectedNoEntryError         = "Dewiswch ‘Iawn’ os ydych wedi mynd yn uwch na’ch lwfans blynyddol wedi’i ostwng"
    val expectedNoAmountEntryError   = "Nodwch y swm sydd uwchlaw’ch lwfans blynyddol wedi’i ostwng"
    val expectedIncorrectFormatError = "Nodwch y swm sydd uwchlaw’ch lwfans blynyddol wedi’i ostwng yn y fformat cywir"
    val expectedOverMaxError         = "Mae’n rhaid i’r swm sydd uwchlaw’ch lwfans blynyddol wedi’i ostwng fod yn llai na £100,000,000,000"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedReducedTitle         = "Has your client gone above their annual allowance?"
    val expectedReducedErrorTitle    = s"Error: $expectedReducedTitle"
    val expectedNoEntryError         = "Select yes if your client has gone above their reduced annual allowance"
    val expectedNoAmountEntryError   = "Enter the amount above your client’s reduced annual allowance"
    val expectedIncorrectFormatError = "Enter the amount above your client’s reduced annual allowance in pounds and pence"
    val expectedOverMaxError         = "The amount above your client’s reduced annual allowance must be less than £100,000,000,000"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedReducedTitle         = "A yw’ch cleient wedi mynd yn uwch na’i lwfans blynyddol wedi’i?"
    val expectedReducedErrorTitle    = s"Gwall: $expectedReducedTitle"
    val expectedNoEntryError         = "Dewiswch ‘Iawn’ os yw’ch cleient wedi mynd yn uwch na’i lwfans blynyddol wedi’i ostwng"
    val expectedNoAmountEntryError   = "Nodwch y swm sydd uwchlaw lwfans blynyddol wedi’i ostwng eich cleient"
    val expectedIncorrectFormatError = "Nodwch y swm sydd uwchlaw lwfans blynyddol wedi’i ostwng eich cleient yn y fformat cywir"
    val expectedOverMaxError         = "Mae’n rhaid i’r swm sydd uwchlaw lwfans blynyddol wedi’i ostwng eich cleient fod yn llai na £100,000,000,000"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  ".show" should {
    userScenarios.foreach { user =>
      import Selectors._
      import user.commonExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" when {

        "render the 'Above annual allowance' question page with correct content and no pre-filling" which {

          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(aboveAnnualAllowanceQuestion = None, aboveAnnualAllowance = None)
            insertCyaData(pensionsUserDataWithAnnualAllowances(pensionsViewModel))
            urlGet(
              fullUrl(aboveReducedAnnualAllowanceUrl(taxYearEOY)),
              user.isWelsh,
              follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
            )
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedReducedTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedReducedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          radioButtonCheck(yesText, 1, checked = Some(false))
          radioButtonCheck(noText, 2, checked = Some(false))
          linkCheck(expectedLinkText, expectedLinkSelector, externalHref)
          buttonCheck(buttonText, continueButtonSelector)
          textOnPageCheck(expectedUseACalculator, paragraphSelector)
          formPostLinkCheck(aboveReducedAnnualAllowanceUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the 'Above annual allowance' question page with 'Yes' and amount pre-filled when CYA data exists" which {
          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            val pensionsViewModel =
              aPensionAnnualAllowanceViewModel.copy(aboveAnnualAllowanceQuestion = Some(true), aboveAnnualAllowance = Some(12.44))
            insertCyaData(pensionsUserDataWithAnnualAllowances(pensionsViewModel))
            urlGet(
              fullUrl(aboveReducedAnnualAllowanceUrl(taxYearEOY)),
              user.isWelsh,
              follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
            )
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedReducedTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedReducedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          radioButtonCheck(yesText, 1, checked = Some(true))
          radioButtonCheck(noText, 2, checked = Some(false))
          inputFieldValueCheck("amount-2", Selectors.amountValueSelector, "12.44")
          linkCheck(expectedLinkText, expectedLinkSelector, externalHref)
          buttonCheck(buttonText, continueButtonSelector)
          textOnPageCheck(expectedUseACalculator, paragraphSelector)
          textOnPageCheck(expectedAmountHeadingText, amountHeadingSelector)
          textOnPageCheck(amountHintText, hintTextSelector)
          formPostLinkCheck(aboveReducedAnnualAllowanceUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the 'Above annual allowance' question page with 'No' pre-filled and not a prior submission" which {
          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(aboveAnnualAllowanceQuestion = Some(false), aboveAnnualAllowance = None)
            insertCyaData(pensionsUserDataWithAnnualAllowances(pensionsViewModel))
            urlGet(
              fullUrl(aboveReducedAnnualAllowanceUrl(taxYearEOY)),
              user.isWelsh,
              follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
            )
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedReducedTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedReducedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          radioButtonCheck(yesText, 1, checked = Some(false))
          radioButtonCheck(noText, 2, checked = Some(true))
          linkCheck(expectedLinkText, expectedLinkSelector, externalHref)
          buttonCheck(buttonText, continueButtonSelector)
          textOnPageCheck(expectedUseACalculator, paragraphSelector)
          formPostLinkCheck(aboveReducedAnnualAllowanceUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "redirect to annual allowance CYA if there is no session data" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        urlGet(
          fullUrl(aboveReducedAnnualAllowanceUrl(taxYearEOY)),
          follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has a SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        // TODO redirect to annual CYA Page
        result.header("location").contains(pensionSummaryUrl(taxYearEOY)) shouldBe true
      }

    }

    "redirect to reduced annual allowance page" when {
      "previous questions have not been answered" which {
        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual()
          val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(reducedAnnualAllowanceQuestion = None)
          insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(pensionsAnnualAllowances = pensionsViewModel)))

          urlGet(
            fullUrl(aboveReducedAnnualAllowanceUrl(taxYearEOY)),
            follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        "has a SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(reducedAnnualAllowanceUrl(taxYearEOY)) shouldBe true
        }

      }
      "page is invalid in journey" which {
        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual()
          val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(reducedAnnualAllowanceQuestion = Some(false))
          insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(pensionsAnnualAllowances = pensionsViewModel)))

          urlGet(
            fullUrl(aboveReducedAnnualAllowanceUrl(taxYearEOY)),
            follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        "has a SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(reducedAnnualAllowanceUrl(taxYearEOY)) shouldBe true
        }
      }
    }
  }

  ".submit" should {
    userScenarios.foreach { user =>
      import Selectors._
      import user.commonExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "return BAD_REQUEST" when {

          "no value is submitted" when {

            lazy val form: Map[String, String] = Map(RadioButtonAmountForm.yesNo -> "")
            lazy val result: WSResponse = {
              dropPensionsDB()
              val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(aboveAnnualAllowanceQuestion = None, aboveAnnualAllowance = None)
              insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(pensionsAnnualAllowances = pensionsViewModel)))
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(
                fullUrl(aboveReducedAnnualAllowanceUrl(taxYearEOY)),
                body = form,
                welsh = user.isWelsh,
                follow = false,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
              )
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedReducedErrorTitle, user.isWelsh)
            h1Check(user.specificExpectedResults.get.expectedReducedHeading)
            captionCheck(expectedCaption(taxYearEOY), captionSelector)
            radioButtonCheck(yesText, 1, checked = Some(false))
            radioButtonCheck(noText, 2, checked = Some(false))
            buttonCheck(buttonText, continueButtonSelector)
            linkCheck(expectedLinkText, expectedLinkSelector, externalHref)
            textOnPageCheck(expectedUseACalculator, paragraphSelector)
            formPostLinkCheck(aboveReducedAnnualAllowanceUrl(taxYearEOY), formSelector)
            welshToggleCheck(user.isWelsh)
            errorSummaryCheck(user.specificExpectedResults.get.expectedNoEntryError, Selectors.yesSelector)
            errorAboveElementCheck(user.specificExpectedResults.get.expectedNoEntryError)
          }

          "user selects 'yes' but no amount" when {

            lazy val form: Map[String, String] = Map(RadioButtonAmountForm.yesNo -> RadioButtonAmountForm.yes)
            lazy val result: WSResponse = {
              dropPensionsDB()
              val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(aboveAnnualAllowance = None)
              insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(pensionsAnnualAllowances = pensionsViewModel)))
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(
                fullUrl(aboveReducedAnnualAllowanceUrl(taxYearEOY)),
                body = form,
                welsh = user.isWelsh,
                follow = false,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
              )
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedReducedErrorTitle, user.isWelsh)
            h1Check(user.specificExpectedResults.get.expectedReducedHeading)
            captionCheck(expectedCaption(taxYearEOY), captionSelector)
            radioButtonCheck(yesText, 1, checked = Some(true))
            radioButtonCheck(noText, 2, checked = Some(false))
            textOnPageCheck(expectedAmountHeadingText, amountHeadingSelector)
            textOnPageCheck(amountHintText, hintTextSelector)
            buttonCheck(buttonText, continueButtonSelector)
            linkCheck(expectedLinkText, expectedLinkSelector, externalHref)
            textOnPageCheck(expectedUseACalculator, paragraphSelector)
            formPostLinkCheck(aboveReducedAnnualAllowanceUrl(taxYearEOY), formSelector)
            welshToggleCheck(user.isWelsh)
            errorSummaryCheck(user.specificExpectedResults.get.expectedNoAmountEntryError, Selectors.amountValueSelector)
            errorAboveElementCheck(user.specificExpectedResults.get.expectedNoAmountEntryError)
          }

          "amount has an incorrect format" when {

            lazy val form: Map[String, String] =
              Map(RadioButtonAmountForm.yesNo -> RadioButtonAmountForm.yes, RadioButtonAmountForm.amount2 -> "wrongFormat")
            lazy val result: WSResponse = {
              dropPensionsDB()
              val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(aboveAnnualAllowance = None)
              insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(pensionsAnnualAllowances = pensionsViewModel)))
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(
                fullUrl(aboveReducedAnnualAllowanceUrl(taxYearEOY)),
                body = form,
                welsh = user.isWelsh,
                follow = false,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
              )
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedReducedErrorTitle, user.isWelsh)
            radioButtonCheck(yesText, 1, checked = Some(true))
            radioButtonCheck(noText, 2, checked = Some(false))
            inputFieldValueCheck("amount-2", amountValueSelector, "wrongFormat")
            errorSummaryCheck(user.specificExpectedResults.get.expectedIncorrectFormatError, Selectors.amountValueSelector)
            errorAboveElementCheck(user.specificExpectedResults.get.expectedIncorrectFormatError)
          }

          "amount is greater than £100,000,000,000" when {

            lazy val form: Map[String, String] =
              Map(RadioButtonAmountForm.yesNo -> RadioButtonAmountForm.yes, RadioButtonAmountForm.amount2 -> "100000000001")
            lazy val result: WSResponse = {
              dropPensionsDB()
              val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(aboveAnnualAllowance = None)
              insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(pensionsAnnualAllowances = pensionsViewModel)))
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(
                fullUrl(aboveReducedAnnualAllowanceUrl(taxYearEOY)),
                body = form,
                welsh = user.isWelsh,
                follow = false,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
              )
            }

            "has the correct status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedReducedErrorTitle, user.isWelsh)
            radioButtonCheck(yesText, 1, checked = Some(true))
            radioButtonCheck(noText, 2, checked = Some(false))
            inputFieldValueCheck("amount-2", amountValueSelector, "100000000001")
            errorSummaryCheck(user.specificExpectedResults.get.expectedOverMaxError, Selectors.amountValueSelector)
            errorAboveElementCheck(user.specificExpectedResults.get.expectedOverMaxError)
          }
        }
      }
    }

    "redirect to the Pension paid tax amount page when user selects 'yes' with an amount and is not a prior submission" which {
      lazy val form: Map[String, String] = Map(RadioButtonAmountForm.yesNo -> RadioButtonAmountForm.yes, RadioButtonAmountForm.amount2 -> "12.44")
      val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(
        aboveAnnualAllowanceQuestion = None,
        aboveAnnualAllowance = None,
        pensionProvidePaidAnnualAllowanceQuestion = None,
        taxPaidByPensionProvider = None,
        pensionSchemeTaxReferences = None
      )
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(pensionsAnnualAllowances = pensionsViewModel)))
        urlPost(
          fullUrl(aboveReducedAnnualAllowanceUrl(taxYearEOY)),
          body = form,
          follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
        )
      }

      "has a SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionProviderPaidTaxUrl(taxYearEOY))
      }

      "persists submission details" in {
        val expectedViewModel = pensionsViewModel.copy(aboveAnnualAllowanceQuestion = Some(true), aboveAnnualAllowance = Some(12.44))
        lazy val cyaModel     = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.pensionsAnnualAllowances shouldBe expectedViewModel
      }
    }

    "redirect to the CYA page when user selects 'yes' with a new amount and CYA data is now complete" which {
      lazy val form: Map[String, String] = Map(RadioButtonAmountForm.yesNo -> RadioButtonAmountForm.yes, RadioButtonAmountForm.amount2 -> "22.55")
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(pensionsAnnualAllowances = aPensionAnnualAllowanceViewModel)))
        urlPost(
          fullUrl(aboveReducedAnnualAllowanceUrl(taxYearEOY)),
          body = form,
          follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
        )
      }

      "has a SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(annualAllowancesCYAUrl(taxYearEOY))
      }

      "persists submission details" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.pensionsAnnualAllowances shouldBe aPensionAnnualAllowanceViewModel.copy(aboveAnnualAllowance = Some(22.55))
      }
    }

    "redirect to the AnnualAllowance CYA page when user selects 'no' and not a prior submission" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
      val pensionsViewModel = PensionAnnualAllowancesViewModel(
        reducedAnnualAllowanceQuestion = Some(true),
        moneyPurchaseAnnualAllowance = Some(true),
        taperedAnnualAllowance = Some(true))
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(pensionsAnnualAllowances = pensionsViewModel)))
        urlPost(
          fullUrl(aboveReducedAnnualAllowanceUrl(taxYearEOY)),
          body = form,
          follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
        )
      }

      "has a SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(annualAllowancesCYAUrl(taxYearEOY))
      }

      "persists submission details" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.pensionsAnnualAllowances shouldBe pensionsViewModel.copy(aboveAnnualAllowanceQuestion = Some(false))
      }
    }

    "redirect to the CYA page updating question to 'no' and clearing other prior data" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(pensionsAnnualAllowances = aPensionAnnualAllowanceViewModel)))
        urlPost(
          fullUrl(aboveReducedAnnualAllowanceUrl(taxYearEOY)),
          body = form,
          follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
        )
      }

      "has a SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(annualAllowancesCYAUrl(taxYearEOY))
      }

      "persists submission details" in {
        val expectedModel = PensionAnnualAllowancesViewModel(
          reducedAnnualAllowanceQuestion = Some(true),
          moneyPurchaseAnnualAllowance = Some(true),
          taperedAnnualAllowance = Some(true),
          aboveAnnualAllowanceQuestion = Some(false)
        )
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.pensionsAnnualAllowances shouldBe expectedModel
      }
    }

    "redirect to reduced annual allowance page" when {
      "previous questions have not been answered" which {
        lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual()
          val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(reducedAnnualAllowanceQuestion = None)
          insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(pensionsAnnualAllowances = pensionsViewModel)))

          urlPost(
            fullUrl(aboveReducedAnnualAllowanceUrl(taxYearEOY)),
            body = form,
            follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
          )
        }

        "has a SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(reducedAnnualAllowanceUrl(taxYearEOY)) shouldBe true
        }

      }
      "page is invalid in journey" which {
        lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual()
          val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(reducedAnnualAllowanceQuestion = Some(false))
          insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(pensionsAnnualAllowances = pensionsViewModel)))

          urlPost(
            fullUrl(aboveReducedAnnualAllowanceUrl(taxYearEOY)),
            body = form,
            follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
          )
        }

        "has a SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(reducedAnnualAllowanceUrl(taxYearEOY)) shouldBe true
        }
      }
    }
  }
}
