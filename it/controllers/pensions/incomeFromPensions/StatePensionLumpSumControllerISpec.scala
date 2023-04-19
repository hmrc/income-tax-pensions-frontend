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

import builders.IncomeFromPensionsViewModelBuilder.anIncomeFromPensionsViewModel
import builders.PensionsUserDataBuilder.{aPensionsUserData, anPensionsUserDataEmptyCya, pensionsUserDataWithIncomeFromPensions}
import builders.StateBenefitViewModelBuilder.anStateBenefitViewModelTwo
import builders.UserBuilder.aUserRequest
import forms.YesNoForm
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.{fullUrl, pensionSummaryUrl, overviewUrl}
import utils.PageUrls.IncomeFromPensionsPages.{statePensionLumpSumUrl, statePensionLumpSumAmountUrl, taxOnLumpSumUrl}
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

class StatePensionLumpSumControllerISpec extends IntegrationTest with BeforeAndAfterEach with ViewHelpers with PensionsDatabaseHelper {

  object Selectors {
    val captionSelector: String = "#main-content > div > div > header > p"
    val continueButtonSelector: String = "#continue"
    val formSelector: String = "#main-content > div > div > form"
    val yesSelector = "#value"
    val noSelector = "#value-no"
    val whereToFindSelector = "#main-content > div > div > form > details > summary > span"
    val detailsSelector = "#main-content > div > div > form > details > summary > span"
    def paragraphSelector(index: Int, withError: Boolean = false): String = s"#main-content > div > div > p:nth-child(${index + (if(withError) 2 else 1)})"
    def bulletSelector(index: Int): String = s"#main-content > div > div > form > details > div > ul > li:nth-child($index)"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    lazy val expectedHeading = expectedTitle
    val expectedErrorTitle: String
    val expectedError: String
    val expectedP1: String
    val expectedBullet1: String
    val expectedBullet2: String
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedWhereToFind: String
    val expectedP2: String
    val expectedButtonText: String
    val yesText: String
    val noText: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Did you get a State Pension lump sum?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if you got a State Pension lump sum"
    val expectedP1 = "You might have got a one-off lump sum payment if you delayed claiming your State Pension for 12 months in a row."
    val expectedBullet1 = "your P60"
    val expectedBullet2 = "the ’About general increases in benefits’ letter the Pension Service sent you"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Did you get a State Pension lump sum?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedError = "Select yes if you got a State Pension lump sum"
    val expectedP1 = "You might have got a one-off lump sum payment if you delayed claiming your State Pension for 12 months in a row."
    val expectedBullet1 = "eich P60"
    val expectedBullet2 = "y llythyr ’Ynglŷn â’r cynnydd cyffredinol mewn budd-daliadau’ a anfonwyd atoch gan y Gwasanaeth Pensiwn"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Did your client get a State Pension lump sum?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedError = "Select yes if your client got a State Pension lump sum"
    val expectedP1 = "Your client might have got a one-off lump sum payment if they delayed claiming their State Pension for 12 months in a row."
    val expectedBullet1 = "your client’s P60"
    val expectedBullet2 = "the ’About general increases in benefits’ letter the Pension Service sent your client"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "A gafodd eich cleient gyfandaliad Pensiwn y Wladwriaeth?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedError = "Dewiswch ‘Iawn’ os cafodd eich cleient gyfandaliad Pensiwn y Wladwriaeth"
    val expectedP1 = "Mae’n bosibl y byddai’ch cleient wedi cael cyfandaliad unigol os oedden nhw wedi oedi cyn hawlio ei Bensiwn y Wladwriaeth am 12 mis yn olynol."
    val expectedBullet1 = "P60 eich cleient"
    val expectedBullet2 = "y llythyr ’Ynglŷn â’r cynnydd cyffredinol mewn budd-daliadau’ a anfonwyd at eich cleient gan y Gwasanaeth Pensiwn"
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Income from pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedWhereToFind = "Where to find this information"
    val expectedP2 = "This only applies to people who reach State Pension age before 6 April 2016."
    val expectedButtonText = "Continue"
    val yesText = "Yes"
    val noText = "No"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Incwm o bensiynau ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val expectedWhereToFind = "Ble i ddod o hyd i’r wybodaeth hon"
    val expectedP2 = "Mae hyn ond yn berthnasol i bobl sy’n cyrraedd oedran Pensiwn y Wladwriaeth cyn 6 Ebrill 2016."
    val expectedButtonText = "Yn eich blaen"
    val yesText = "Iawn"
    val noText = "Na"
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

        "render the 'Did you get a lump sum' page with correct content and no pre-filling" which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropPensionsDB()
            insertCyaData(anPensionsUserDataEmptyCya, aUserRequest)
            urlGet(fullUrl(statePensionLumpSumUrl(taxYearEOY)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedP1, paragraphSelector(1))
          textOnPageCheck(expectedP2, paragraphSelector(2))
          textOnPageCheck(expectedWhereToFind, whereToFindSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedBullet1, bulletSelector(1))
          textOnPageCheck(user.specificExpectedResults.get.expectedBullet2, bulletSelector(2))
          radioButtonCheck(yesText, 1, checked = Some(false))
          radioButtonCheck(noText, 2, checked = Some(false))
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(statePensionLumpSumUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the 'Did you get a lump sum' page with correct content and yes pre-filled" which {

          implicit lazy val result: WSResponse = {
            dropPensionsDB()
            val pensionsViewModel = anIncomeFromPensionsViewModel
            insertCyaData(pensionsUserDataWithIncomeFromPensions(pensionsViewModel), aUserRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(fullUrl(statePensionLumpSumUrl(taxYearEOY)), user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedP1, paragraphSelector(1))
          textOnPageCheck(expectedP2, paragraphSelector(2))
          textOnPageCheck(expectedWhereToFind, whereToFindSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedBullet1, bulletSelector(1))
          textOnPageCheck(user.specificExpectedResults.get.expectedBullet2, bulletSelector(2))
          radioButtonCheck(yesText, 1, checked = Some(true))
          radioButtonCheck(noText, 2, checked = Some(false))
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(statePensionLumpSumUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the 'Did you get a lump sum' page with correct content and no pre-filled" which {

          implicit lazy val result: WSResponse = {
            dropPensionsDB()
            val pensionsViewModel =
              anIncomeFromPensionsViewModel.copy(
                statePensionLumpSum = Some(anStateBenefitViewModelTwo.copy(
                  amountPaidQuestion = Some(false)
                ))
              )
            insertCyaData(pensionsUserDataWithIncomeFromPensions(pensionsViewModel), aUserRequest)
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(fullUrl(statePensionLumpSumUrl(taxYearEOY)), user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedP1, paragraphSelector(1))
          textOnPageCheck(expectedP2, paragraphSelector(2))
          textOnPageCheck(expectedWhereToFind, whereToFindSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedBullet1, bulletSelector(1))
          textOnPageCheck(user.specificExpectedResults.get.expectedBullet2, bulletSelector(2))
          radioButtonCheck(yesText, 1, checked = Some(false))
          radioButtonCheck(noText, 2, checked = Some(true))
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(statePensionLumpSumUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "redirect to Pensions Summary page if there is no session data" should {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(statePensionLumpSumUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }


      //TODO - redirect to CYA page once implemented
      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
      }
    }

    "redirect to Pensions overview page if it is not end of year" should {
      lazy val result: WSResponse = {
        dropPensionsDB()

        val pensionsViewModel = anIncomeFromPensionsViewModel.copy(statePensionLumpSum =
          Some(anStateBenefitViewModelTwo.copy(amountPaidQuestion = None)))
        insertCyaData(pensionsUserDataWithIncomeFromPensions(pensionsViewModel), aUserRequest)

        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(statePensionLumpSumUrl(taxYear)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location")shouldBe  Some(overviewUrl(taxYear))
      }
    }
  }

  ".submit" should {
    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        s"return $BAD_REQUEST error when no value is submitted" which {
          lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> "")

          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(aPensionsUserData, aUserRequest)
            urlPost(fullUrl(statePensionLumpSumUrl(taxYearEOY)), body = form, follow = false, welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)
          import Selectors._
          import user.commonExpectedResults._
          titleCheck(user.specificExpectedResults.get.expectedErrorTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedP1, paragraphSelector(1, withError = true))
          textOnPageCheck(expectedP2, paragraphSelector(2, withError = true))
          textOnPageCheck(expectedWhereToFind, whereToFindSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedBullet1, bulletSelector(1))
          textOnPageCheck(user.specificExpectedResults.get.expectedBullet2, bulletSelector(2))
          radioButtonCheck(yesText, 1, checked = Some(false))
          radioButtonCheck(noText, 2, checked = Some(false))
          buttonCheck(expectedButtonText, continueButtonSelector)
          formPostLinkCheck(statePensionLumpSumUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)

          errorSummaryCheck(user.specificExpectedResults.get.expectedError, Selectors.yesSelector)
          errorAboveElementCheck(user.specificExpectedResults.get.expectedError, Some("value"))
        }
      }
    }

    "redirect to the overview page if it is not end of year" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

      lazy val result: WSResponse = {
        dropPensionsDB()

        val pensionsViewModel = anIncomeFromPensionsViewModel.copy(statePensionLumpSum =
          Some(anStateBenefitViewModelTwo.copy(amountPaidQuestion = None)))
        insertCyaData(pensionsUserDataWithIncomeFromPensions(pensionsViewModel), aUserRequest)

        authoriseAgentOrIndividual(isAgent = false)

        urlPost(fullUrl(statePensionLumpSumUrl(taxYear)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location")shouldBe  Some(overviewUrl(taxYear))
      }

    }

    "redirect and update question to 'Yes' when user selects yes when there is no cya data" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(statePensionLumpSumUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has a SEE_OTHER(303) status and redirect to the lump sum amount page" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(statePensionLumpSumAmountUrl(taxYearEOY))
      }

      "updates amountPaidQuestion to Some(true)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.incomeFromPensions.statePensionLumpSum.get.amountPaidQuestion shouldBe Some(true)
      }
    }

    "redirect and update question to 'Yes' when user selects yes and cya data exists" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val pensionsViewModel =
          anIncomeFromPensionsViewModel.copy(
            statePensionLumpSum = Some(anStateBenefitViewModelTwo.copy(
              amountPaidQuestion = Some(false)
            ))
          )
        insertCyaData(pensionsUserDataWithIncomeFromPensions(pensionsViewModel), aUserRequest)
        urlPost(fullUrl(statePensionLumpSumUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has a SEE_OTHER(303) status and redirect to the lump sum amount page" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(statePensionLumpSumAmountUrl(taxYearEOY))
      }

      "updates amountPaidQuestion to Some(true)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.incomeFromPensions.statePensionLumpSum.get.amountPaidQuestion shouldBe Some(true)
      }
    }

    "redirect and update question to 'No' when user selects no and cya data exists" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val pensionsViewModel = anIncomeFromPensionsViewModel
        insertCyaData(pensionsUserDataWithIncomeFromPensions(pensionsViewModel), aUserRequest)
        urlPost(fullUrl(statePensionLumpSumUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }
      "has a SEE_OTHER(303) status and redirect to the tax paid on lump sum page" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(taxOnLumpSumUrl(taxYearEOY))
      }

      "updates amountPaidQuestion to Some(false) and wipes amount value" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.incomeFromPensions.statePensionLumpSum.get.amountPaidQuestion shouldBe Some(false)
        cyaModel.pensions.incomeFromPensions.statePensionLumpSum.get.amount shouldBe None
      }
    }
  }
}
