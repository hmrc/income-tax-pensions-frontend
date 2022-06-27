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

import builders.AllPensionsDataBuilder.{anAllPensionDataEmpty, anAllPensionsData}
import builders.IncomeFromPensionsViewModelBuilder.{anIncomeFromPensionEmptyViewModel, anIncomeFromPensionsViewModel}
import builders.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import builders.PensionsCYAModelBuilder.{aPensionsCYAGeneratedFromPriorEmpty, aPensionsCYAModel}
import builders.PensionsUserDataBuilder.{aPensionsUserData, pensionsUserDataWithIncomeFromPensions}
import builders.UkPensionIncomeViewModelBuilder.anUkPensionIncomeViewModelOne
import builders.UserBuilder.aUserRequest
import models.IncomeTaxUserData
import models.pension.employmentPensions.{EmploymentPensionModel, EmploymentPensions}
import models.pension.statebenefits.IncomeFromPensionsViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.SEE_OTHER
import play.api.libs.ws.WSResponse
import utils.PageUrls.IncomeFromPensionsPages._
import utils.PageUrls.{fullUrl, pensionSummaryUrl}
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

class UkPensionIncomeCYAControllerISpec extends IntegrationTest with ViewHelpers with BeforeAndAfterEach with PensionsDatabaseHelper {

  private val schemeNames = anIncomeFromPensionsViewModel.uKPensionIncomes.collect { case scheme => scheme.pensionSchemeName.getOrElse("") }.mkString(" ")
  private val newIncomeFromPensions: IncomeFromPensionsViewModel = anIncomeFromPensionEmptyViewModel.copy(
    uKPensionIncomesQuestion = Some(true),
    uKPensionIncomes = Seq(anUkPensionIncomeViewModelOne.copy(
      pensionSchemeName = Some("New Pension Scheme"),
      pensionSchemeRef = Some("123/123"),
      pensionId = Some("123456"))))

  object Selectors {
    val continueButtonSelector: String = "#continue"
    val paragraphSelector = "#main-content > div > div > p"
    val ukPensionSchemesRowSelector = ".govuk-summary-list__row:nth-of-type(2)"
  }

  trait CommonExpectedResults {
    val expectedH1: String
    val expectedTitle: String

    def expectedCaption(taxYear: Int): String

    val buttonText: String
    val yesText: String
    val noText: String
    val ukPensionIncome: String
    val ukPensionSchemes: String
    val ukPensionSchemesHidden: String
  }

  trait SpecificExpectedResults {
    val expectedParagraph: String
    val ukPensionIncomesHidden: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedH1 = "Check income from pensions"
    val expectedTitle = "Check income from pensions"

    def expectedCaption(taxYear: Int): String = s"UK pension income for 6 April ${taxYear - 1} to 5 April $taxYear"

    val buttonText = "Continue"
    val yesText = "Yes"
    val noText = "No"
    val ukPensionIncome = "UK pension income"
    val ukPensionSchemes = "UK pension schemes"
    val ukPensionSchemesHidden = "Change UK pension scheme details"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedH1 = "Check income from pensions"
    val expectedTitle = "Check income from pensions"

    def expectedCaption(taxYear: Int): String = s"UK pension income for 6 April ${taxYear - 1} to 5 April $taxYear"

    val buttonText = "Continue"
    val yesText = "Yes"
    val noText = "No"
    val ukPensionIncome = "UK pension income"
    val ukPensionSchemes = "UK pension schemes"
    val ukPensionSchemesHidden = "Change UK pension scheme details"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedParagraph = "Your income from pensions is based on the information we already hold about you."
    val ukPensionIncomesHidden = "Change whether you got income from UK pensions"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedParagraph = "Your client’s income from pensions is based on the information we already hold about them."
    val ukPensionIncomesHidden = "Change whether your client got income from UK pensions"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedParagraph = "Your income from pensions is based on the information we already hold about you."
    val ukPensionIncomesHidden = "Change whether you got income from UK pensions"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedParagraph = "Your client’s income from pensions is based on the information we already hold about them."
    val ukPensionIncomesHidden = "Change whether your client got income from UK pensions"
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

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "render the Check UK pension income page" when {

          "CYA data is generated from prior data" which {
            lazy val result: WSResponse = {
              dropPensionsDB()
              authoriseAgentOrIndividual(user.isAgent)
              userDataStub(anIncomeTaxUserData, nino, taxYear)
              urlGet(fullUrl(ukPensionIncomeCyaUrl(taxYear)), welsh = user.isWelsh,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(expectedTitle)
            h1Check(expectedH1)
            captionCheck(user.commonExpectedResults.expectedCaption(taxYear))
            textOnPageCheck(user.specificExpectedResults.get.expectedParagraph, paragraphSelector)
            cyaRowCheck(ukPensionIncome, yesText, ukPensionSchemePayments(taxYear), user.specificExpectedResults.get.ukPensionIncomesHidden, 1)
            cyaRowCheck(ukPensionSchemes, schemeNames, ukPensionSchemeSummaryListUrl(taxYear), ukPensionSchemesHidden, 2)
            buttonCheck(buttonText)
            welshToggleCheck(user.isWelsh)
          }

          "there is CYA data with multiple pension schemes" which {
            lazy val result: WSResponse = {
              dropPensionsDB()
              authoriseAgentOrIndividual(user.isAgent)
              userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
              insertCyaData(pensionsUserDataWithIncomeFromPensions(anIncomeFromPensionsViewModel), aUserRequest)
              urlGet(fullUrl(ukPensionIncomeCyaUrl(taxYearEOY)), welsh = user.isWelsh,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(expectedTitle)
            h1Check(expectedH1)
            captionCheck(user.commonExpectedResults.expectedCaption(taxYearEOY))
            textOnPageCheck(user.specificExpectedResults.get.expectedParagraph, paragraphSelector)
            cyaRowCheck(ukPensionIncome, yesText, ukPensionSchemePayments(taxYearEOY), user.specificExpectedResults.get.ukPensionIncomesHidden, 1)
            cyaRowCheck(ukPensionSchemes, schemeNames, ukPensionSchemeSummaryListUrl(taxYearEOY), ukPensionSchemesHidden, 2)
            buttonCheck(buttonText)
            welshToggleCheck(user.isWelsh)

          }

          "there is CYA data with no pension schemes and uKPensionIncomesQuestion is Some(false)" which {
            lazy val result: WSResponse = {
              dropPensionsDB()
              authoriseAgentOrIndividual(user.isAgent)
              insertCyaData(pensionsUserDataWithIncomeFromPensions(anIncomeFromPensionEmptyViewModel.copy(
                uKPensionIncomesQuestion = Some(false), uKPensionIncomes = Seq.empty), taxYear = taxYear), aUserRequest)
              urlGet(fullUrl(ukPensionIncomeCyaUrl(taxYear)), welsh = user.isWelsh, follow = false,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(expectedTitle)
            h1Check(expectedH1)
            captionCheck(user.commonExpectedResults.expectedCaption(taxYear))
            textOnPageCheck(user.specificExpectedResults.get.expectedParagraph, paragraphSelector)
            cyaRowCheck(ukPensionIncome, noText, ukPensionSchemePayments(taxYear), user.specificExpectedResults.get.ukPensionIncomesHidden, 1)
            elementNotOnPageCheck(ukPensionSchemesRowSelector)
            buttonCheck(buttonText)
            welshToggleCheck(user.isWelsh)

          }
        }
      }
    }

    "redirect to the first page in the journey when there is no CYA or prior data" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(IncomeTaxUserData(None), nino, taxYearEOY)
        urlGet(fullUrl(ukPensionIncomeCyaUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "have the status SEE OTHER" in {
        result.status shouldBe SEE_OTHER
      }

      "redirects to the uk pension scheme payments question page" in {
        result.header("location") shouldBe Some(ukPensionSchemePayments(taxYearEOY))
      }
    }

  }

  ".submit" should {

    "redirect to overview page when there is no CYA data available" which {
      val form = Map[String, String]()

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(ukPensionIncomeCyaUrl(taxYear)), form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)))
      }

      "have the status SEE OTHER" in {
        result.status shouldBe SEE_OTHER
      }

      "redirects to the overview page" in {
        result.header("location") shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      }
    }

    "redirect to pensions summary" when {

      "there is no prior data and CYA data is submitted" which {
        val form = Map[String, String]()

        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual(isAgent = false)
          userDataStub(IncomeTaxUserData(None), nino, taxYear)
          insertCyaData(aPensionsUserData.copy(pensions = aPensionsCYAModel.copy(incomeFromPensions = newIncomeFromPensions), taxYear = taxYear), aUserRequest)
          urlPost(fullUrl(ukPensionIncomeCyaUrl(taxYear)), form, follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)))
        }

        "have the status SEE OTHER" in {
          result.status shouldBe SEE_OTHER
        }

        "redirects to the overview page" in {
          result.header("location") shouldBe Some(pensionSummaryUrl(taxYear))
        }
      }

      "CYA data has been updated and differs from prior data" which {
        val form = Map[String, String]()

        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual(isAgent = false)
          userDataStub(anIncomeTaxUserData.copy(pensions = Some(anAllPensionsData)), nino, taxYear)
          insertCyaData(aPensionsUserData.copy(pensions = aPensionsCYAModel.copy(incomeFromPensions = newIncomeFromPensions), taxYear = taxYear), aUserRequest)
          urlPost(fullUrl(ukPensionIncomeCyaUrl(taxYear)), form, follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)))
        }

        "have the status SEE OTHER" in {
          result.status shouldBe SEE_OTHER
        }

        "redirects to the overview page" in {
          result.header("location") shouldBe Some(pensionSummaryUrl(taxYear))
        }

      }

      "the user makes no changes and no submission to DES is made" which {
        val form = Map[String, String]()

        val priorData = anAllPensionDataEmpty.copy(employmentPensions = Some(EmploymentPensions(
          employmentData = Seq(EmploymentPensionModel(
            employmentId = anUkPensionIncomeViewModelOne.employmentId.get,
            pensionSchemeName = anUkPensionIncomeViewModelOne.pensionSchemeName.get,
            pensionId = anUkPensionIncomeViewModelOne.pensionId,
            startDate = anUkPensionIncomeViewModelOne.startDate,
            endDate = anUkPensionIncomeViewModelOne.endDate,
            pensionSchemeRef = anUkPensionIncomeViewModelOne.pensionSchemeRef,
            amount = anUkPensionIncomeViewModelOne.amount,
            taxPaid = anUkPensionIncomeViewModelOne.taxPaid,
            isCustomerEmploymentData = Some(true)
          ))
        )))

        val cyaData = aPensionsUserData.copy(taxYear = taxYear, pensions = aPensionsCYAGeneratedFromPriorEmpty.copy(
          incomeFromPensions = IncomeFromPensionsViewModel(
            uKPensionIncomesQuestion = Some(true),
            uKPensionIncomes = Seq(anUkPensionIncomeViewModelOne))))

        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual(isAgent = false)
          userDataStub(anIncomeTaxUserData.copy(pensions = Some(priorData)), nino, taxYear)
          insertCyaData(cyaData, aUserRequest)
          urlPost(fullUrl(ukPensionIncomeCyaUrl(taxYear)), form, follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)))
        }

        "have the status SEE OTHER" in {
          result.status shouldBe SEE_OTHER
        }

        "redirects to the overview page" in {
          result.header("location") shouldBe Some(pensionSummaryUrl(taxYear))
        }
      }
    }
  }
}
