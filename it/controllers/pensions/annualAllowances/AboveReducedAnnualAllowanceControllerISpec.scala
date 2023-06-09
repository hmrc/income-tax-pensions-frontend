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
import forms.YesNoForm
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.pension.charges.PensionAnnualAllowancesViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.PensionAnnualAllowancePages.{amountAboveAnnualAllowanceUrl, aboveAnnualAllowanceUrl, reducedAnnualAllowanceUrl}
import utils.PageUrls._
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

class AboveReducedAnnualAllowanceControllerISpec extends IntegrationTest with ViewHelpers with BeforeAndAfterEach with PensionsDatabaseHelper {

  private val externalHref = "https://www.tax.service.gov.uk/pension-annual-allowance-calculator"

  private def pensionsUsersData(pensionsCyaModel: PensionsCYAModel): PensionsUserData = {
    PensionsUserDataBuilder.aPensionsUserData.copy(
      isPriorSubmission = false, pensions = pensionsCyaModel)
  }

  private def pensionsUserDataWithAnnualAllowances(annualAllowances: PensionAnnualAllowancesViewModel,
                                                   isPriorSubmission: Boolean = true): PensionsUserData = {
    aPensionsUserData.copy(isPriorSubmission = isPriorSubmission,
      pensions = aPensionsCYAModel.copy(pensionsAnnualAllowances = annualAllowances)
    )
  }

  object Selectors {
    val captionSelector: String = "#main-content > div > div > header > p"
    val continueButtonSelector: String = "#continue"
    val formSelector: String = "#main-content > div > div > form"
    val yesSelector = "#value"
    val noSelector = "#value-no"
    val paragraphSelector: String = s"#main-content > div > div > p"
    val expectedLinkSelector = "#above-reduced-annual-allowance-link"

  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val yesText: String
    val noText: String
    val buttonText: String
    val expectedUseACalculator: String
    val expectedLinkText: String
  }

  trait SpecificExpectedResults {
    val expectedReducedTitle: String
    lazy val expectedReducedHeading: String = expectedReducedTitle
    val expectedReducedErrorTitle: String
    val expectedReducedErrorMessage: String
    val expectedNonReducedTitle: String
    lazy val expectedNonReducedHeading: String = expectedNonReducedTitle
    val expectedNonReducedErrorTitle: String
    val expectedNonReducedErrorMessage: String

  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Pension annual allowance for 6 April ${taxYear - 1} to 5 April $taxYear"
    val yesText = "Yes"
    val noText = "No"
    val buttonText = "Continue"
    val expectedLinkText = "if you need to work this out (opens in new tab)"
    val expectedUseACalculator: String = s"Use a calculator $expectedLinkText."
    
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Lwfans blynyddol pensiwn ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val yesText = "Iawn"
    val noText = "Na"
    val buttonText = "Yn eich blaen"
    val expectedLinkText = "os bydd angen i chi gyfrifo hyn (yn agor tab newydd)"
    val expectedUseACalculator: String = s"Defnyddiwch gyfrifiannell $expectedLinkText."
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedReducedTitle = "Have you gone above your reduced annual allowance?"
    val expectedReducedErrorTitle = s"Error: $expectedReducedTitle"
    val expectedReducedErrorMessage = "Select yes if you have gone above your reduced annual allowance"

    val expectedNonReducedTitle = "Have you gone above the annual allowance?"
    val expectedNonReducedErrorTitle = s"Error: $expectedNonReducedTitle"
    val expectedNonReducedErrorMessage = "Select yes if you have gone above the annual allowance"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedReducedTitle = "A ydych wedi mynd yn uwch na’ch lwfans blynyddol wedi’i ostwng?"
    val expectedReducedErrorTitle = s"Gwall: $expectedReducedTitle"
    val expectedReducedErrorMessage = "Dewiswch ‘Iawn’ os ydych wedi mynd yn uwch na’ch lwfans blynyddol wedi’i ostwng"

    val expectedNonReducedTitle = "A ydych wedi mynd yn uwch na’r lwfans blynyddol?"
    val expectedNonReducedErrorTitle = s"Gwall: $expectedNonReducedTitle"
    val expectedNonReducedErrorMessage = "Dewiswch ‘Iawn’ os ydych wedi mynd yn uwch na’r lwfans blynyddol"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedReducedTitle = "Has your client gone above their reduced annual allowance?"
    val expectedReducedErrorTitle = s"Error: $expectedReducedTitle"
    val expectedReducedErrorMessage = "Select yes if your client has gone above their reduced annual allowance"

    val expectedNonReducedTitle = "Has your client gone above the annual allowance?"
    val expectedNonReducedErrorTitle = s"Error: $expectedNonReducedTitle"
    val expectedNonReducedErrorMessage = "Select yes if your client has gone above the annual allowance"

  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedReducedTitle = "A yw’ch cleient wedi mynd yn uwch na’i lwfans blynyddol wedi’i ostwng?"
    val expectedReducedErrorTitle = s"Gwall: $expectedReducedTitle"
    val expectedReducedErrorMessage = "Dewiswch ‘Iawn’ os yw’ch cleient wedi mynd yn uwch na’i lwfans blynyddol wedi’i ostwng"

    val expectedNonReducedTitle = "A yw’ch cleient wedi mynd yn uwch na’r lwfans blynyddol?"
    val expectedNonReducedErrorTitle = s"Gwall: $expectedNonReducedTitle"
    val expectedNonReducedErrorMessage = "Dewiswch ‘Iawn’ os yw’ch cleient wedi mynd yn uwch na’i lwfans blynyddol"
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

        "reducedAnnualAllowanceQuestion is true" should {

          "render the above reduced annual allowance question page with no pre-filled radio buttons" which {

            lazy val result: WSResponse = {
              dropPensionsDB()
              authoriseAgentOrIndividual(user.isAgent)
              val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(aboveAnnualAllowanceQuestion = None, reducedAnnualAllowanceQuestion = Some(true))
              insertCyaData(pensionsUserDataWithAnnualAllowances(pensionsViewModel))
              urlGet(fullUrl(aboveAnnualAllowanceUrl(taxYearEOY)),
                user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
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
            formPostLinkCheck(aboveAnnualAllowanceUrl(taxYearEOY), formSelector)
            welshToggleCheck(user.isWelsh)
          }

          "render above reduced annual allowance question page with 'Yes' pre-filled when CYA data exists" which {
            lazy val result: WSResponse = {
              dropPensionsDB()
              authoriseAgentOrIndividual(user.isAgent)
              val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(aboveAnnualAllowanceQuestion = Some(true),
                reducedAnnualAllowanceQuestion = Some(true))
              insertCyaData(pensionsUserDataWithAnnualAllowances(pensionsViewModel))
              urlGet(fullUrl(aboveAnnualAllowanceUrl(taxYearEOY)), user.isWelsh, follow = false,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
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
            linkCheck(expectedLinkText, expectedLinkSelector, externalHref)
            buttonCheck(buttonText, continueButtonSelector)
            textOnPageCheck(expectedUseACalculator, paragraphSelector)
            formPostLinkCheck(aboveAnnualAllowanceUrl(taxYearEOY), formSelector)
            welshToggleCheck(user.isWelsh)
          }

          "render the above reduced annual allowance question page with 'No' pre-filled and not a prior submission" which {
            lazy val result: WSResponse = {
              dropPensionsDB()
              authoriseAgentOrIndividual(user.isAgent)
              val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(aboveAnnualAllowanceQuestion = Some(false),
                reducedAnnualAllowanceQuestion = Some(true))
              insertCyaData(pensionsUserDataWithAnnualAllowances(pensionsViewModel))
              urlGet(fullUrl(aboveAnnualAllowanceUrl(taxYearEOY)), user.isWelsh, follow = false,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
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
            formPostLinkCheck(aboveAnnualAllowanceUrl(taxYearEOY), formSelector)
            welshToggleCheck(user.isWelsh)
          }
        }

        "reducedAnnualAllowanceQuestion is false" should {
          "render the above reduced annual allowance question page with no pre-filled radio buttons" which {

            lazy val result: WSResponse = {
              dropPensionsDB()
              authoriseAgentOrIndividual(user.isAgent)
              val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(aboveAnnualAllowanceQuestion = None, reducedAnnualAllowanceQuestion = Some(false))
              insertCyaData(pensionsUserDataWithAnnualAllowances(pensionsViewModel))
              urlGet(fullUrl(aboveAnnualAllowanceUrl(taxYearEOY)),
                user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            "has an OK status" in {
              result.status shouldBe OK
            }

            titleCheck(user.specificExpectedResults.get.expectedNonReducedTitle, user.isWelsh)
            h1Check(user.specificExpectedResults.get.expectedNonReducedHeading)
            captionCheck(expectedCaption(taxYearEOY), captionSelector)
            radioButtonCheck(yesText, 1, checked = Some(false))
            radioButtonCheck(noText, 2, checked = Some(false))
            linkCheck(expectedLinkText, expectedLinkSelector, externalHref)
            buttonCheck(buttonText, continueButtonSelector)
            textOnPageCheck(expectedUseACalculator, paragraphSelector)
            formPostLinkCheck(aboveAnnualAllowanceUrl(taxYearEOY), formSelector)
            welshToggleCheck(user.isWelsh)
          }

          "render above reduced annual allowance question page with 'Yes' pre-filled when CYA data exists" which {
            lazy val result: WSResponse = {
              dropPensionsDB()
              authoriseAgentOrIndividual(user.isAgent)
              val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(aboveAnnualAllowanceQuestion = Some(true),
                reducedAnnualAllowanceQuestion = Some(false))
              insertCyaData(pensionsUserDataWithAnnualAllowances(pensionsViewModel))
              urlGet(fullUrl(aboveAnnualAllowanceUrl(taxYearEOY)), user.isWelsh, follow = false,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            "has an OK status" in {
              result.status shouldBe OK
            }

            titleCheck(user.specificExpectedResults.get.expectedNonReducedTitle, user.isWelsh)
            h1Check(user.specificExpectedResults.get.expectedNonReducedHeading)
            captionCheck(expectedCaption(taxYearEOY), captionSelector)
            radioButtonCheck(yesText, 1, checked = Some(true))
            radioButtonCheck(noText, 2, checked = Some(false))
            linkCheck(expectedLinkText, expectedLinkSelector, externalHref)
            buttonCheck(buttonText, continueButtonSelector)
            textOnPageCheck(expectedUseACalculator, paragraphSelector)
            formPostLinkCheck(aboveAnnualAllowanceUrl(taxYearEOY), formSelector)
            welshToggleCheck(user.isWelsh)
          }

          "render the above reduced annual allowance question page with 'No' pre-filled and not a prior submission" which {
            lazy val result: WSResponse = {
              dropPensionsDB()
              authoriseAgentOrIndividual(user.isAgent)
              val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(aboveAnnualAllowanceQuestion = Some(false),
                reducedAnnualAllowanceQuestion = Some(false))
              insertCyaData(pensionsUserDataWithAnnualAllowances(pensionsViewModel))
              urlGet(fullUrl(aboveAnnualAllowanceUrl(taxYearEOY)), user.isWelsh, follow = false,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            "has an OK status" in {
              result.status shouldBe OK
            }

            titleCheck(user.specificExpectedResults.get.expectedNonReducedTitle, user.isWelsh)
            h1Check(user.specificExpectedResults.get.expectedNonReducedHeading)
            captionCheck(expectedCaption(taxYearEOY), captionSelector)
            radioButtonCheck(yesText, 1, checked = Some(false))
            radioButtonCheck(noText, 2, checked = Some(true))
            linkCheck(expectedLinkText, expectedLinkSelector, externalHref)
            buttonCheck(buttonText, continueButtonSelector)
            textOnPageCheck(expectedUseACalculator, paragraphSelector)
            formPostLinkCheck(aboveAnnualAllowanceUrl(taxYearEOY), formSelector)
            welshToggleCheck(user.isWelsh)
          }

        }
      }
    }


    "redirect to annual allowance CYA if there is no session data" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        urlGet(fullUrl(aboveAnnualAllowanceUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        //TODO redirect to annual CYA Page
        result.header("location").contains(pensionSummaryUrl(taxYearEOY)) shouldBe true
      }

    }
    "redirect to reduced annual allowance page if question has not been answered" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(
          aboveAnnualAllowanceQuestion = Some(true), reducedAnnualAllowanceQuestion = None)
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(pensionsAnnualAllowances = pensionsViewModel)))

        urlGet(fullUrl(aboveAnnualAllowanceUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        //TODO redirect to annual CYA Page
        result.header("location").contains(reducedAnnualAllowanceUrl(taxYearEOY)) shouldBe true
      }

    }
  }

  ".submit" should {
    userScenarios.foreach { user =>

      import Selectors._
      import user.commonExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "return an error when form is submitted with no entry and reducedAnnualAllowanceQuestion is true" when {

          lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> "")
          lazy val result: WSResponse = {
            dropPensionsDB()
            val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(aboveAnnualAllowanceQuestion = None, reducedAnnualAllowanceQuestion = Some(true))
            insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(pensionsAnnualAllowances = pensionsViewModel)))
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(fullUrl(aboveAnnualAllowanceUrl(taxYearEOY)), body = form, welsh = user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
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
          formPostLinkCheck(aboveAnnualAllowanceUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
          errorSummaryCheck(user.specificExpectedResults.get.expectedReducedErrorMessage, Selectors.yesSelector)
          errorAboveElementCheck(user.specificExpectedResults.get.expectedReducedErrorMessage, Some("value"))
        }

        "return an error when form is submitted with no entry and reducedAnnualAllowanceQuestion is false" when {

          lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> "")
          lazy val result: WSResponse = {
            dropPensionsDB()
            val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(aboveAnnualAllowanceQuestion = None, reducedAnnualAllowanceQuestion = Some(false))
            insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(pensionsAnnualAllowances = pensionsViewModel)))
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(fullUrl(aboveAnnualAllowanceUrl(taxYearEOY)), body = form, welsh = user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedNonReducedErrorTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedNonReducedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          radioButtonCheck(yesText, 1, checked = Some(false))
          radioButtonCheck(noText, 2, checked = Some(false))
          buttonCheck(buttonText, continueButtonSelector)
          linkCheck(expectedLinkText, expectedLinkSelector, externalHref)
          textOnPageCheck(expectedUseACalculator, paragraphSelector)
          formPostLinkCheck(aboveAnnualAllowanceUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
          errorSummaryCheck(user.specificExpectedResults.get.expectedNonReducedErrorMessage, Selectors.yesSelector)
          errorAboveElementCheck(user.specificExpectedResults.get.expectedNonReducedErrorMessage, Some("value"))
        }
      }
    }

    "redirect to reduced annual allowance page if question has not been answered" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(
          aboveAnnualAllowanceQuestion = Some(true), reducedAnnualAllowanceQuestion = None)
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(pensionsAnnualAllowances = pensionsViewModel)))

        urlGet(fullUrl(aboveAnnualAllowanceUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(reducedAnnualAllowanceUrl(taxYearEOY)) shouldBe true
      }
    }

    "redirect to the above annual allowance amount page when user selects 'yes' and not a prior submission" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(
          aboveAnnualAllowanceQuestion = Some(true), aboveAnnualAllowance = None)
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(pensionsAnnualAllowances = pensionsViewModel)))

        urlPost(fullUrl(aboveAnnualAllowanceUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(amountAboveAnnualAllowanceUrl(taxYearEOY))

      }

      "updates above reduced annual allowance question to Some(true)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.pensionsAnnualAllowances.aboveAnnualAllowanceQuestion shouldBe Some(true)
        cyaModel.pensions.pensionsAnnualAllowances.aboveAnnualAllowance shouldBe None
      }
    }

    "redirect to Pensions Summary page when user selects 'no' and not a prior submission" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(
          aboveAnnualAllowanceQuestion = Some(true), aboveAnnualAllowance = Some(333.44))
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(pensionsAnnualAllowances = pensionsViewModel)))
        urlPost(fullUrl(aboveAnnualAllowanceUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
      }

      "updates above reduced annual allowance question to Some(false) and deletes the annual allowance amount" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.pensionsAnnualAllowances.aboveAnnualAllowanceQuestion shouldBe Some(false)
        cyaModel.pensions.pensionsAnnualAllowances.aboveAnnualAllowance shouldBe None
      }
    }

  }

}
