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

package controllers.pensions.incomeFromOverseasPensions

import builders.IncomeFromOverseasPensionsViewModelBuilder.{anIncomeFromOverseasPensionsEmptyViewModel, anIncomeFromOverseasPensionsViewModel}
import builders.PaymentsIntoOverseasPensionsViewModelBuilder.aPaymentsIntoOverseasPensionsViewModel
import builders.PensionsUserDataBuilder.{aPensionsUserData, anPensionsUserDataEmptyCya, pensionUserDataWithIncomeOverseasPension, pensionUserDataWithOverseasPensions, pensionsUserDataWithUnauthorisedPayments}
import builders.UnauthorisedPaymentsViewModelBuilder.anUnauthorisedPaymentsViewModel
import forms.CountryForm
import models.pension.charges.PensionScheme
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.CommonUtils
import utils.PageUrls.{IncomeFromOverseasPensionsPages, fullUrl, pensionSummaryUrl}
import utils.PageUrls.IncomeFromOverseasPensionsPages.{pensionOverseasIncomeCountryUrl, pensionOverseasIncomeCountryUrlIndex, pensionOverseasIncomeCountryUrlIndex2}
import utils.PageUrls.PensionLifetimeAllowance.pensionTaxReferenceNumberLifetimeAllowanceUrl
import builders.UserBuilder.{aUser, aUserRequest}
import play.api.http.HeaderNames
import utils.PageUrls.UnAuthorisedPayments.surchargeAmountUrl


class PensionOverseasIncomeCountryControllerSpec extends CommonUtils with BeforeAndAfterEach {

  object Selectors {
    val captionSelector: String = "#main-content > div > div > header > p"
    val continueButtonSelector: String = "#continue"
    val formSelector: String = "#main-content > div > div > form"
    val inputSelector = "#countryId"

    def labelSelector(index: Int): String = s"form > div:nth-of-type($index) > label"

    def paragraphSelector(index: Int): String = s"#main-content > div > div > p:nth-of-type($index)"
  }


  trait SpecificExpectedResults {
    val expectedError: String
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedTitle: String
    val expectedHeading: String
    val expectedErrorTitle: String
    val expectedSubHeading: String
    val expectedParagraph: String
    val expectedButtonText: String
  }


  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedError: String = "Enter the name of the country the pension scheme is registered in"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedError: String = "Enter the name of the country the pension scheme is registered in"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedError: String = "Enter the name of the country that your client’s pension scheme is registered in"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedError: String = "Enter the name of the country that your client’s pension scheme is registered in"
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Income from overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedTitle: String = "What country is the pension scheme registered in?"
    val expectedHeading: String = "What country is the pension scheme registered in?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val expectedButtonText: String = "Continue"
    val expectedParagraph: String = "You can add pension schemes from other countries later."
    val expectedSubHeading: String = "Country"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Income from overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedTitle: String = "What country is the pension scheme registered in?"
    val expectedHeading: String = "What country is the pension scheme registered in?"
    val expectedErrorTitle: String = s"Error: $expectedTitle"
    val expectedButtonText: String = "Continue"
    val expectedParagraph: String = "You can add pension schemes from other countries later."
    val expectedSubHeading: String = "Country"
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


        "render the page with correct content and no prefilling" which {
          implicit val overseasIncomeCountryUrl: Int => String = IncomeFromOverseasPensionsPages.pensionOverseasIncomeCountryUrl
          implicit lazy val result: WSResponse = showPage(user, anPensionsUserDataEmptyCya)


          "has an ok status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(expectedTitle)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(expectedParagraph, paragraphSelector(1))
          textOnPageCheck(expectedSubHeading, labelSelector(1))
          inputFieldValueCheck(inputName, inputSelector, "")
          formPostLinkCheck(pensionOverseasIncomeCountryUrl(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the page with correct content without prefilling" which {
          implicit val overseasIncomeCountryUrl: Int => String = pensionOverseasIncomeCountryUrlIndex(0)
          val countryCode = "GB"

          val pensionsViewModel = anIncomeFromOverseasPensionsViewModel.copy(
            overseasIncomePensionSchemes = Seq(PensionScheme(
              countryCode = Some(countryCode)
            ))
          )

          val pensionUserData = pensionUserDataWithIncomeOverseasPension(pensionsViewModel)
          implicit lazy val result: WSResponse = showPage(user, pensionUserData)

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(expectedTitle)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY))
          textOnPageCheck(expectedParagraph, paragraphSelector(1))
          textOnPageCheck(expectedSubHeading, labelSelector(1))
          inputFieldValueCheck(inputName, inputSelector, "", Some(""))
          formPostLinkCheck(pensionOverseasIncomeCountryUrlIndex(0)(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "Redirect to the pension summary page if there is no session data" should {
      implicit val overseasIncomeCountryUrl: Int => String = IncomeFromOverseasPensionsPages.pensionOverseasIncomeCountryUrl
      lazy val result: WSResponse = getResponseNoSessionData

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
      }
    }

  }

  ".submit" should {
    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        s"return $BAD_REQUEST error when no value is submitted" which {
          lazy val form: Map[String, String] = Map(CountryForm.countryId -> "")
          implicit val url: Int => String = IncomeFromOverseasPensionsPages.pensionOverseasIncomeCountryUrl
          lazy val result: WSResponse = submitPage(user, aPensionsUserData, form)


          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)
          import Selectors._
          import user.commonExpectedResults._
          titleCheck(expectedErrorTitle)
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

    "redirect and update question to contain country code when pension schemes list is empty" which {
      lazy val form: Map[String, String] = Map(CountryForm.countryId -> "GB")
      val pensionsViewModel = anIncomeFromOverseasPensionsEmptyViewModel.copy(
        overseasIncomePensionSchemes = Seq.empty
      )
      val pensionUserData = pensionUserDataWithIncomeOverseasPension(pensionsViewModel)
      implicit val url: Int => String = IncomeFromOverseasPensionsPages.pensionOverseasIncomeCountryUrl

      lazy val result: WSResponse = submitPage(pensionUserData, form)

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionOverseasIncomeCountryUrl(taxYearEOY))
      }

      "updates pension scheme tax reference to contain tax reference" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes.size shouldBe 1
        cyaModel.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes.head.countryCode.get shouldBe "GB"
      }
    }

    "redirect and update country code when cya data exists" which {
      val index = 0
      implicit val url: Int => String = pensionOverseasIncomeCountryUrlIndex(index)
      lazy val form: Map[String, String] = Map(CountryForm.countryId -> "GB")
      val pensionsViewModel = anIncomeFromOverseasPensionsEmptyViewModel.copy(
        overseasIncomePensionSchemes = Seq(PensionScheme(countryCode = Some("GB")))
      )
      val pensionUserData = pensionUserDataWithIncomeOverseasPension(pensionsViewModel)
      lazy val result: WSResponse = submitPage(pensionUserData, form)


      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionOverseasIncomeCountryUrlIndex(0)(taxYearEOY)) //todo redirect to pstr summary page when implemented
      }

      "updates pension scheme tax reference to contain both tax reference" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes.size shouldBe 1
        cyaModel.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes.head.countryCode.get shouldBe "GB"
      }
    }

    "redirect and update pension schemes list to contain new country code when there is an existing pension schemes list" which {

      lazy val form: Map[String, String] = Map(CountryForm.countryId -> "GB")
      val pensionsViewModel = anIncomeFromOverseasPensionsEmptyViewModel.copy(
        overseasIncomePensionSchemes =  Seq(
          PensionScheme(countryCode = Some("IE")),
          PensionScheme(countryCode = Some("US")),
        )
      )
      val pensionUserData = pensionUserDataWithIncomeOverseasPension(pensionsViewModel)


      implicit val url: Int => String = pensionOverseasIncomeCountryUrl
      lazy val result: WSResponse = submitPage(pensionUserData, form)

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionOverseasIncomeCountryUrl(taxYearEOY))
      }

      "updates pension scheme tax reference to contain both tax reference" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes.size shouldBe 3
        cyaModel.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes.last.countryCode.get shouldBe "GB"
      }
    }

    "redirect to pension summary page when country code index does not exist" which {
      val index = 3
      implicit val url: Int => String = pensionOverseasIncomeCountryUrlIndex(index)
      lazy val form: Map[String, String] = Map(CountryForm.countryId -> "GB")
      val pensionsViewModel = anIncomeFromOverseasPensionsEmptyViewModel.copy(
        overseasIncomePensionSchemes = Seq(
          PensionScheme(countryCode = Some("IE")),
          PensionScheme(countryCode = Some("US")),
        )
      )
      val pensionUserData = pensionUserDataWithIncomeOverseasPension(pensionsViewModel)
      lazy val result: WSResponse = submitPage(pensionUserData, form)

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
      }

      "updates pension scheme tax reference to contain both tax reference" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes.map(_.countryCode.get) should not contain ("GB")
      }
    }

    "redirect to  CYA page if there is no session data" should {
      implicit val url: Int => String = pensionOverseasIncomeCountryUrl
      lazy val form: Map[String, String] = Map(CountryForm.countryId -> "GB")

      lazy val result: WSResponse = submitPageNoSessionData(form)

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
      }
    }
  }

}
