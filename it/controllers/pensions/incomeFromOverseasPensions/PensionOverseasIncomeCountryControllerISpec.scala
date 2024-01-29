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

package controllers.pensions.incomeFromOverseasPensions

import builders.IncomeFromOverseasPensionsViewModelBuilder.{
  anIncomeFromOverseasPensionsEmptyViewModel,
  anIncomeFromOverseasPensionsSingleSchemeViewModel,
  anIncomeFromOverseasPensionsViewModel
}
import builders.PensionSchemeBuilder.aPensionScheme1
import builders.PensionsUserDataBuilder.{aPensionsUserData, pensionUserDataWithIncomeOverseasPension}
import builders.UserBuilder.aUserRequest
import forms.CountryForm
import models.pension.charges.PensionScheme
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.CommonUtils
import utils.PageUrls.IncomeFromOverseasPensionsPages._
import utils.PageUrls.{IncomeFromOverseasPensionsPages, overseasPensionsSummaryUrl}

class PensionOverseasIncomeCountryControllerISpec extends CommonUtils with BeforeAndAfterEach {

  object Selectors {
    val captionSelector: String        = "#main-content > div > div > header > p"
    val continueButtonSelector: String = "#continue"
    val formSelector: String           = "#main-content > div > div > form"
    val inputSelector                  = "#countryId"

    def labelSelector(index: Int): String = s"form > div:nth-of-type($index) > label"

    def paragraphSelector(index: Int): String = s"#main-content > div > div > p:nth-of-type($index)"
  }

  trait SpecificExpectedResults {
    val expectedError: String
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedTitle: String
    lazy val expectedHeading = expectedTitle
    val expectedErrorTitle: String
    val expectedSubHeading: String
    val expectedParagraph: String
    val expectedButtonText: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedError: String = "Enter the name of the country the pension scheme is registered in"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedError: String = "Nodwch enw’r wlad lle y mae’r cynllun pensiwn wedi’i gofrestru"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedError: String = "Enter the name of the country that your client’s pension scheme is registered in"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedError: String = "Nodwch enw’r wlad lle mae cynllun pensiwn eich cleient wedi’i gofrestru"
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Income from overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedTitle: String          = "What country is the pension scheme registered in?"
    val expectedErrorTitle: String     = s"Error: $expectedTitle"
    val expectedButtonText: String     = "Continue"
    val expectedParagraph: String      = "You can add pension schemes from other countries later."
    val expectedSubHeading: String     = "Country"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Incwm o bensiynau tramor ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val expectedTitle: String          = "Ym mha wlad y mae’r cynllun pensiwn wedi’i gofrestru?"
    val expectedErrorTitle: String     = s"Gwall: $expectedTitle"
    val expectedButtonText: String     = "Yn eich blaen"
    val expectedParagraph: String      = "Gallwch ychwanegu cynlluniau pensiwn o wledydd eraill yn nes ymlaen."
    val expectedSubHeading: String     = "Gwlad"
  }

  val inputName: String = "countryId"

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

        "render the page with correct content with no prior country data" which {
          implicit val overseasIncomeCountryUrl: Int => String = IncomeFromOverseasPensionsPages.pensionOverseasIncomeCountryUrl
          val pensionUserData = pensionUserDataWithIncomeOverseasPension(
            anIncomeFromOverseasPensionsEmptyViewModel.copy(paymentsFromOverseasPensionsQuestion = Some(true)))
          implicit lazy val result: WSResponse = showPage(user, pensionUserData)

          "has an ok status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(expectedTitle, user.isWelsh)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(expectedParagraph, paragraphSelector(1))
          textOnPageCheck(expectedSubHeading, labelSelector(1))
          inputFieldValueCheck(inputName, inputSelector, "")
          formPostLinkCheck(pensionOverseasIncomeCountryUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the page with correct content (not pre-filled country) with prior data" which {
          val index                                            = 0
          implicit val overseasIncomeCountryUrl: Int => String = pensionOverseasIncomeCountryUrlIndex(index)
          val countryCode                                      = "GB"

          val pensionsViewModel = anIncomeFromOverseasPensionsViewModel.copy(
            overseasIncomePensionSchemes = Seq(
              PensionScheme(
                alphaTwoCode = Some(countryCode)
              ))
          )

          val pensionUserData                  = pensionUserDataWithIncomeOverseasPension(pensionsViewModel)
          implicit lazy val result: WSResponse = showPage(user, pensionUserData)

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(expectedTitle, user.isWelsh)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(expectedParagraph, paragraphSelector(1))
          textOnPageCheck(expectedSubHeading, labelSelector(1))
          inputFieldValueCheck(inputName, inputSelector, "", Some(""))
          formPostLinkCheck(pensionOverseasIncomeCountryUrlIndex2(taxYearEOY, index), formSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "redirect to the first page in journey" when {
      "page is invalid in journey" in {
        implicit val overseasIncomeCountryUrl: Int => String = IncomeFromOverseasPensionsPages.pensionOverseasIncomeCountryUrl
        val pensionUserData = pensionUserDataWithIncomeOverseasPension(
          anIncomeFromOverseasPensionsEmptyViewModel.copy(paymentsFromOverseasPensionsQuestion = Some(false)))

        implicit lazy val result: WSResponse = showPage(pensionUserData)

        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(incomeFromOverseasPensionsStatus(taxYearEOY))
      }
      "previous question is unanswered" in {
        implicit val overseasIncomeCountryUrl: Int => String = IncomeFromOverseasPensionsPages.pensionOverseasIncomeCountryUrl
        lazy val result: WSResponse                          = getResponseNoSessionData()

        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(incomeFromOverseasPensionsStatus(taxYearEOY))
      }
    }

  }

  ".submit" should {
    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        s"return $BAD_REQUEST error when no value is submitted" which {
          lazy val form: Map[String, String] = Map(CountryForm.countryId -> "")
          implicit val url: Int => String    = IncomeFromOverseasPensionsPages.pensionOverseasIncomeCountryUrl
          lazy val result: WSResponse        = submitPage(user, aPensionsUserData, form)

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)
          import Selectors._
          import user.commonExpectedResults._
          titleCheck(expectedErrorTitle, user.isWelsh)
          h1Check(expectedTitle)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(expectedParagraph, paragraphSelector(1))
          textOnPageCheck(expectedSubHeading, labelSelector(1))
          inputFieldValueCheck(inputName, inputSelector, "")
          formPostLinkCheck(pensionOverseasIncomeCountryUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
          errorSummaryCheck(user.specificExpectedResults.get.expectedError, inputSelector)
          errorAboveElementCheck(user.specificExpectedResults.get.expectedError)
        }
      }
    }

    "redirect to Payments Amounts page when creating a new scheme" when {
      "schemes list is empty" which {
        lazy val form: Map[String, String] = Map(CountryForm.countryId -> "GB")
        val pensionsViewModel = anIncomeFromOverseasPensionsEmptyViewModel.copy(
          paymentsFromOverseasPensionsQuestion = Some(true),
          overseasIncomePensionSchemes = Seq.empty
        )
        val pensionUserData             = pensionUserDataWithIncomeOverseasPension(pensionsViewModel)
        implicit val url: Int => String = IncomeFromOverseasPensionsPages.pensionOverseasIncomeCountryUrl

        lazy val result: WSResponse = submitPage(pensionUserData, form)

        "has a SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(incomeFromOverseasPensionsAmounts(taxYearEOY, 0))
        }

        "updates pension scheme tax reference to contain tax reference" in {
          lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
          cyaModel.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes.size shouldBe 1
          cyaModel.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes.head.alphaTwoCode.get shouldBe "GB"
        }
      }

      "adding to existing schemes list" which {
        lazy val form: Map[String, String] = Map(CountryForm.countryId -> "GB")
        val pensionUserData                = pensionUserDataWithIncomeOverseasPension(anIncomeFromOverseasPensionsSingleSchemeViewModel)

        implicit val url: Int => String = pensionOverseasIncomeCountryUrl
        lazy val result: WSResponse     = submitPage(pensionUserData, form)

        "has a SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(incomeFromOverseasPensionsAmounts(taxYearEOY, 1))
        }

        "updates pension scheme tax reference to contain both tax reference" in {
          lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
          cyaModel.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes shouldBe Seq(
            aPensionScheme1,
            PensionScheme(alphaTwoCode = Some("GB")))
        }
      }
    }

    "redirect to Scheme Summary page and update country code of existing scheme when scheme is now completed" which {
      lazy val form: Map[String, String] = Map(CountryForm.countryId -> "GB")
      val pensionsViewModel              = anIncomeFromOverseasPensionsSingleSchemeViewModel
      val pensionUserData                = pensionUserDataWithIncomeOverseasPension(pensionsViewModel)

      implicit val url: Int => String = pensionOverseasIncomeCountryUrlIndex(0)
      lazy val result: WSResponse     = submitPage(pensionUserData, form)

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(overseasPensionsSchemeSummaryUrl(taxYearEOY, 0))
      }

      "updates pension scheme tax reference to contain both tax reference" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes shouldBe Seq(aPensionScheme1.copy(alphaTwoCode = Some("GB")))
      }
    }

    "redirect to country summary page when there are existing schemes but the index is invalid" which {
      val index                          = 3
      implicit val url: Int => String    = pensionOverseasIncomeCountryUrlIndex(index)
      lazy val form: Map[String, String] = Map(CountryForm.countryId -> "GB")
      val pensionUserData                = pensionUserDataWithIncomeOverseasPension(anIncomeFromOverseasPensionsViewModel)
      lazy val result: WSResponse        = submitPage(pensionUserData, form)

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(countrySummaryListControllerUrl(taxYearEOY))
      }

      "updates pension scheme tax reference to contain both tax reference" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes.map(_.alphaTwoCode.get) should not contain "GB"
      }
    }

    "redirect to Overseas Pension Summary page there is no session data" should {
      implicit val url: Int => String    = pensionOverseasIncomeCountryUrl
      lazy val form: Map[String, String] = Map(CountryForm.countryId -> "GB")

      lazy val result: WSResponse = submitPageNoSessionData(form)

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(overseasPensionsSummaryUrl(taxYearEOY))
      }
    }
  }

}
