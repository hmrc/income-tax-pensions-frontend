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

import builders.IncomeFromPensionsViewModelBuilder.anIncomeFromPensionsViewModel
import builders.PensionsUserDataBuilder.{aPensionsUserData, pensionsUserDataWithIncomeFromPensions}
import builders.StateBenefitViewModelBuilder.anStateBenefitViewModelOne
import builders.UserBuilder.aUserRequest
import forms.YesNoForm
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.IncomeFromPensionsPages.{taxOnLumpSumAmountUrl, taxOnLumpSumUrl}
import utils.PageUrls.{fullUrl, overviewUrl, pensionSummaryUrl}
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

// scalastyle:off magic.number
class TaxPaidOnStatePensionLumpSumControllerISpec extends IntegrationTest with BeforeAndAfterEach with ViewHelpers with PensionsDatabaseHelper {

  object Selectors {
    val captionSelector: String = "#main-content > div > div > header > p"
    val continueButtonSelector: String = "#continue"
    val formSelector: String = "#main-content > div > div > form"
    val yesSelector = "#value"
    val noSelector = "#value-no"
    val findOutLinkSelector = "#annual-allowance-link"
    val overLimitLinkSelector = "#over-limit-link"
    val detailsSelector = "#main-content > div > div > form > details > summary > span"
    val paragraphSelector: String = "#main-content > div > div > form > details > div > p"
    def detailsBulletSelector(index: Int): String = s"#main-content > div > div > form > details > div > ul > li:nth-child($index)"

  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedErrorTitle: String
    val expectedError: String
    val expectedDetailsBullet1: String
    val expectedDetailsBullet2: String

  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedDetailsTitle: String
    val expectedDetailsWhereToFind: String
    val expectedButtonText: String
    val yesText: String
    val noText: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Did you pay tax on the State Pension lump sum?"
    val expectedHeading = "Did you pay tax on the State Pension lump sum?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if you paid tax on the State Pension lump sum"
    val expectedDetailsBullet1 = "your P60"
    val expectedDetailsBullet2 = "the ’About general increases in benefits’ letter the Pension Service sent you"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Did you pay tax on the State Pension lump sum?"
    val expectedHeading = "Did you pay tax on the State Pension lump sum?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if you paid tax on the State Pension lump sum"
    val expectedDetailsBullet1 = "eich P60"
    val expectedDetailsBullet2 = "y llythyr ’Ynglŷn â’r cynnydd cyffredinol mewn budd-daliadau’ a anfonwyd atoch gan y Gwasanaeth Pensiwn"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Did your client pay tax on the State Pension lump sum?"
    val expectedHeading = "Did your client pay tax on the State Pension lump sum?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if your client paid tax on the State Pension lump sum"
    val expectedDetailsBullet1 = "your client’s P60"
    val expectedDetailsBullet2 = "the ’About general increases in benefits’ letter the Pension Service sent your client"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Did your client pay tax on the State Pension lump sum?"
    val expectedHeading = "Did your client pay tax on the State Pension lump sum?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if your client paid tax on the State Pension lump sum"
    val expectedDetailsBullet1 = "P60 eich cleient"
    val expectedDetailsBullet2 = "y llythyr ’Ynglŷn â’r cynnydd cyffredinol mewn budd-daliadau’ a anfonwyd at eich cleient gan y Gwasanaeth Pensiwn"
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Income from pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedDetailsTitle = "Where to find this information"
    val expectedDetailsWhereToFind = "You can find this information in:"
    val expectedButtonText = "Continue"
    val yesText = "Yes"
    val noText = "No"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Income from pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedDetailsTitle = "Ble i ddod o hyd i’r wybodaeth hon"
    val expectedDetailsWhereToFind = "Gallwch ddod o hyd i’r wybodaeth hon yn:"
    val expectedButtonText = "Continue"
    val yesText = "Yes"
    val noText = "No"
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

        "render the 'Did you pay tax on State Pension lump sum' page when there is no CYA session data for the question" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropPensionsDB()
            val pensionsViewModel = anIncomeFromPensionsViewModel.copy(statePensionLumpSum =
              Some(anStateBenefitViewModelOne.copy(taxPaidQuestion = None)))
            insertCyaData(pensionsUserDataWithIncomeFromPensions(pensionsViewModel), aUserRequest)
            urlGet(fullUrl(taxOnLumpSumUrl(taxYearEOY)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          formPostLinkCheck(taxOnLumpSumUrl(taxYearEOY), formSelector)
          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          radioButtonCheck(yesText, 1, checked = Some(false))
          radioButtonCheck(noText, 2, checked = Some(false))
          textOnPageCheck(expectedDetailsTitle, detailsSelector)
          textOnPageCheck(expectedDetailsWhereToFind, paragraphSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedDetailsBullet1, detailsBulletSelector(1))
          textOnPageCheck(user.specificExpectedResults.get.expectedDetailsBullet2, detailsBulletSelector(2))
          buttonCheck(expectedButtonText, continueButtonSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the 'Did you pay tax on State Pension lump sum' page with correct content and yes pre-filled" which {

          implicit lazy val result: WSResponse = {
            dropPensionsDB()
            val pensionsViewModel = anIncomeFromPensionsViewModel.copy(statePensionLumpSum =
              Some(anStateBenefitViewModelOne.copy(taxPaidQuestion = Some(true))))
            insertCyaData(pensionsUserDataWithIncomeFromPensions(pensionsViewModel), aUserRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(fullUrl(taxOnLumpSumUrl(taxYearEOY)), user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          formPostLinkCheck(taxOnLumpSumUrl(taxYearEOY), formSelector)
          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          radioButtonCheck(yesText, 1, checked = Some(true))
          radioButtonCheck(noText, 2, checked = Some(false))
          textOnPageCheck(expectedDetailsTitle, detailsSelector)
          textOnPageCheck(expectedDetailsWhereToFind, paragraphSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedDetailsBullet1, detailsBulletSelector(1))
          textOnPageCheck(user.specificExpectedResults.get.expectedDetailsBullet2, detailsBulletSelector(2))
          buttonCheck(expectedButtonText, continueButtonSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the 'Did you pay tax on State Pension lump sum' page with correct content and no pre-filled" which {

          implicit lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            val pensionsViewModel = anIncomeFromPensionsViewModel.copy(statePensionLumpSum =
              Some(anStateBenefitViewModelOne.copy(taxPaidQuestion = Some(false))))
            insertCyaData(pensionsUserDataWithIncomeFromPensions(pensionsViewModel), aUserRequest)
            urlGet(fullUrl(taxOnLumpSumUrl(taxYearEOY)), user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          formPostLinkCheck(taxOnLumpSumUrl(taxYearEOY), formSelector)
          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          radioButtonCheck(yesText, 1, checked = Some(false))
          radioButtonCheck(noText, 2, checked = Some(true))
          textOnPageCheck(expectedDetailsTitle, detailsSelector)
          textOnPageCheck(expectedDetailsWhereToFind, paragraphSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedDetailsBullet1, detailsBulletSelector(1))
          textOnPageCheck(user.specificExpectedResults.get.expectedDetailsBullet2, detailsBulletSelector(2))
          buttonCheck(expectedButtonText, continueButtonSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }


    "redirect to Pensions overview page if it is not end of year" should {
      lazy val result: WSResponse = {
        dropPensionsDB()

        val pensionsViewModel = anIncomeFromPensionsViewModel.copy(statePensionLumpSum =
          Some(anStateBenefitViewModelOne.copy(taxPaidQuestion = None)))
        insertCyaData(pensionsUserDataWithIncomeFromPensions(pensionsViewModel), aUserRequest)

        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(taxOnLumpSumUrl(taxYear)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location")shouldBe  Some(overviewUrl(taxYear))
      }
    }


    "redirect to Pensions Summary page if there is no session data" should {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(taxOnLumpSumUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        //TODO: navigate to the income from pensions CYA page
        result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
      }
    }
  }

  ".submit" should {
    import Selectors._
    userScenarios.foreach { user =>

      import user.commonExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        s"return $BAD_REQUEST error when no value is submitted" which {
          lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> "")

          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(aPensionsUserData, aUserRequest)
            urlPost(fullUrl(taxOnLumpSumUrl(taxYearEOY)), body = form, follow = false, welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)


          formPostLinkCheck(taxOnLumpSumUrl(taxYearEOY), formSelector)
          titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          radioButtonCheck(yesText, 1, checked = Some(false))
          radioButtonCheck(noText, 2, checked = Some(false))
          errorSummaryCheck(user.specificExpectedResults.get.expectedError, Selectors.yesSelector)
          errorAboveElementCheck(user.specificExpectedResults.get.expectedError, Some("value"))
          textOnPageCheck(expectedDetailsTitle, detailsSelector)
          textOnPageCheck(expectedDetailsWhereToFind, paragraphSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedDetailsBullet1, detailsBulletSelector(1))
          textOnPageCheck(user.specificExpectedResults.get.expectedDetailsBullet2, detailsBulletSelector(2))
          buttonCheck(expectedButtonText, continueButtonSelector)
          welshToggleCheck(user.isWelsh)

        }
      }
    }

    "redirect to the overview page if it is not end of year" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

      lazy val result: WSResponse = {
        dropPensionsDB()

        val pensionsViewModel = anIncomeFromPensionsViewModel.copy(statePensionLumpSum =
          Some(anStateBenefitViewModelOne.copy(taxPaidQuestion = None)))
        insertCyaData(pensionsUserDataWithIncomeFromPensions(pensionsViewModel), aUserRequest)

        authoriseAgentOrIndividual(isAgent = false)

        urlPost(fullUrl(taxOnLumpSumUrl(taxYear)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location")shouldBe  Some(overviewUrl(taxYear))
      }

    }

    "redirect and update question to 'Yes' when user selects yes" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)

        val pensionsViewModel = anIncomeFromPensionsViewModel.copy(statePensionLumpSum =
          Some(anStateBenefitViewModelOne.copy(taxPaidQuestion = None)))
        insertCyaData(pensionsUserDataWithIncomeFromPensions(pensionsViewModel), aUserRequest)

        urlPost(fullUrl(taxOnLumpSumUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(taxOnLumpSumAmountUrl(taxYearEOY))
      }

      "updates taxPaidQuestion to Some(true)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.incomeFromPensions.statePensionLumpSum.get.taxPaidQuestion shouldBe Some(true)
      }
    }

    "redirect and update question to No and delete the tax paid amount when user selects no" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)

        val pensionsViewModel = anIncomeFromPensionsViewModel.copy(statePensionLumpSum =
          Some(anStateBenefitViewModelOne.copy(taxPaidQuestion = Some(true), taxPaid = Some(44.55))))
        insertCyaData(pensionsUserDataWithIncomeFromPensions(pensionsViewModel), aUserRequest)

        urlPost(fullUrl(taxOnLumpSumUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }
      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        //TODO: navigate to the correct next page
        result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
      }

      "updates taxPaidQuestion to Some(false) and wipe the taxPaid value to None" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.incomeFromPensions.statePensionLumpSum.get.taxPaidQuestion shouldBe Some(false)
        cyaModel.pensions.incomeFromPensions.statePensionLumpSum.get.taxPaid shouldBe None
      }
    }
  }
}
// scalastyle:on magic.number
