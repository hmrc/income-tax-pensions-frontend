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

package views.pensions.paymentsIntoOverseasPensions

import builders.PaymentsIntoOverseasPensionsViewModelBuilder.aPaymentsIntoOverseasPensionsViewModel
import builders.PensionsCYAModelBuilder.aPensionsCYAEmptyModel
import builders.PensionsUserDataBuilder.aPensionsUserData
import builders.UserBuilder.{aUser, anAgentUser}
import forms.overseas.DoubleTaxationAgreementForm
import forms.overseas.DoubleTaxationAgreementForm.DoubleTaxationAgreementFormModel
import models.pension.charges.Relief
import models.requests.UserSessionDataRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import utils.FakeRequestProvider
import views.html.pensions.paymentsIntoOverseasPensions.DoubleTaxationAgreementView

class DoubleTaxationAgreementViewSpec extends ViewUnitTest with FakeRequestProvider {

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedTitle: String
    val expectedCountyLabel: String
    val expectedArticleLabel: String
    val expectedArticleExample: String
    val expectedTreatyLabel: String
    val expectedTreatyExample: String
    val expectedReliefLabel: String
    val expectedReliefExample: String
    val expectedContinue: String
    val errorTitle: String
    val noCountryErrorText: String
    val noReliefErrorText: String
    val reliefWrongFormatErrorText: String
    val reliefTooBigErrorText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption:  Int => String = (taxYear: Int) => s"Payments into overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedTitle: String = "Double taxation agreement details"
    val expectedCountyLabel: String = "Country"
    val expectedArticleLabel: String = "Article (optional)"
    val expectedArticleExample: String = "For example, ‘AB3211-1’"
    val expectedTreatyLabel: String = "Treaty (optional)"
    val expectedTreatyExample: String = "For example, ’Munich’"
    val expectedReliefLabel: String = "Double taxation relief"
    val expectedReliefExample: String = "For example, £193.54"
    val expectedContinue: String = "Continue"
    val errorTitle: String = s"Error: $expectedTitle"
    val noCountryErrorText: String = "Enter the tax treaty country"
    val noReliefErrorText: String = "Enter the amount of double taxation relief"
    val reliefWrongFormatErrorText: String = "Enter the amount of double taxation relief in pounds"
    val reliefTooBigErrorText: String = "The amount of double taxation relief must be less than £100,000,000,000"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption:  Int => String = (taxYear: Int) => s"Taliadau i bensiynau tramor ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val expectedTitle: String = "Manylion y cytundeb trethiant dwbl"
    val expectedCountyLabel: String = "Gwlad"
    val expectedArticleLabel: String = "Erthygl (dewisol)"
    val expectedArticleExample: String = "Er enghraifft, ‘AB3211-1’"
    val expectedTreatyLabel: String = "Cytuniad (dewisol)"
    val expectedTreatyExample: String = "Er enghraifft, ‘Munich’"
    val expectedReliefLabel: String = "Rhyddhad trethiant dwbl"
    val expectedReliefExample: String = "Er enghraifft, £193.54"
    val expectedContinue: String = "Yn eich blaen"
    val errorTitle: String = s"Gwall: $expectedTitle"
    val noCountryErrorText: String = "Nodwch wlad y cytuniad treth"
    val noReliefErrorText: String = "Nodwch swm y rhyddhad trethiant dwbl"
    val reliefWrongFormatErrorText: String = "Nodwch swm y rhyddhad trethiant dwbl yn y fformat cywir"
    val reliefTooBigErrorText: String = "Mae’n rhaid i swm y rhyddhad trethiant dwbl fod yn llai na £100,000,000,000"
  }

  object Selectors {
    val captionSelector = "#main-content > div > div > header > p"
    val titleSelector = "#main-content > div > div > header > h1"
    val buttonSelector = "#continue"
    val countryLabelSelector = "#main-content > div > div > form > div:nth-child(1) > div > label"
    val articleLabelSelector = "#main-content > div > div > form > div:nth-child(2) > label"
    val treatyLabelSelector = "#main-content > div > div > form > div:nth-child(3) > label"
    val reliefLabelSelector = "#main-content > div > div > form > div:nth-child(4) > label" //todo fix label format
    val articleExampleSelector = "#article-hint"
    val treatyExampleSelector = "#treaty-hint"
    val reliefExampleSelector = "#amount-2-hint"
    val countryValueSelector = "#countryId"
    val articleValueSelector = "#article"
    val treatyValueSelector = "#treaty"
    val reliefValueSelector = "#amount-2"
  }

  protected val userScenarios: Seq[UserScenario[CommonExpectedResults, Unit]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY)
  )

  private lazy val underTest = inject[DoubleTaxationAgreementView]
  userScenarios.foreach { userScenario =>

    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render page with no prefilled data" which {
        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        val relief = Relief(Some(""))
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] =
          UserSessionDataRequest(aPensionsUserData.copy(
            pensions = aPensionsCYAEmptyModel.copy(paymentsIntoOverseasPensions = aPaymentsIntoOverseasPensionsViewModel.
              copy(reliefs = Seq(relief)))),
            if (userScenario.isAgent) anAgentUser else aUser,
            if (userScenario.isAgent) fakeAgentRequest else fakeIndividualRequest)

        def form: Form[DoubleTaxationAgreementFormModel] =
          DoubleTaxationAgreementForm.doubleTaxationAgreementForm("individual")

        implicit val document: Document = Jsoup.parse(underTest(form, taxYearEOY, Some(0)).body)

        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY), Selectors.captionSelector)
        titleCheck(userScenario.commonExpectedResults.expectedTitle, userScenario.isWelsh)
        textOnPageCheck(userScenario.commonExpectedResults.expectedCountyLabel, Selectors.countryLabelSelector)
        selectFieldValueCheck("countryId", Selectors.countryValueSelector, "")

        textOnPageCheck(userScenario.commonExpectedResults.expectedArticleLabel, Selectors.articleLabelSelector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedArticleExample, Selectors.articleExampleSelector)
        inputFieldValueCheck("article", Selectors.articleValueSelector, "")

        textOnPageCheck(userScenario.commonExpectedResults.expectedTreatyLabel, Selectors.treatyLabelSelector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedTreatyExample, Selectors.treatyExampleSelector)
        inputFieldValueCheck("treaty", Selectors.treatyValueSelector, "")

        textOnPageCheck(userScenario.commonExpectedResults.expectedReliefLabel, Selectors.reliefLabelSelector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedReliefExample, Selectors.reliefExampleSelector)
        inputFieldValueCheck("amount-2", Selectors.reliefValueSelector, "")
        buttonCheck(userScenario.commonExpectedResults.expectedContinue)
      }

      "render page with prefilled data" which {
        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        val relief = Relief(
          Some(""),
          alphaTwoCountryCode = Some("AD"),
          doubleTaxationReliefAmount = Some(99.99),
          doubleTaxationArticle = Some("exampleArticle"),
          doubleTaxationTreaty = Some("exampleTreaty")
        )
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] =
          UserSessionDataRequest(aPensionsUserData.copy(
            pensions = aPensionsCYAEmptyModel.copy(paymentsIntoOverseasPensions = aPaymentsIntoOverseasPensionsViewModel
              .copy(reliefs = Seq(relief)))),
            if (userScenario.isAgent) anAgentUser else aUser,
            if (userScenario.isAgent) fakeAgentRequest else fakeIndividualRequest)

        def form: Form[DoubleTaxationAgreementFormModel] =
          DoubleTaxationAgreementForm.doubleTaxationAgreementForm("individual").fill(
            DoubleTaxationAgreementFormModel(
              countryId = Some("AD"),
              article = Some("exampleArticle"),
              treaty = Some("exampleTreaty"),
              reliefAmount = Some(99.99)
            )
          )

        implicit val document: Document = Jsoup.parse(underTest(form, taxYearEOY, Some(0)).body)

        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY), Selectors.captionSelector)
        titleCheck(userScenario.commonExpectedResults.expectedTitle, userScenario.isWelsh)
        textOnPageCheck(userScenario.commonExpectedResults.expectedCountyLabel, Selectors.countryLabelSelector)
        selectFieldValueCheck("countryId", Selectors.countryValueSelector, "AD")

        textOnPageCheck(userScenario.commonExpectedResults.expectedArticleLabel, Selectors.articleLabelSelector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedArticleExample, Selectors.articleExampleSelector)
        inputFieldValueCheck("article", Selectors.articleValueSelector, "exampleArticle")

        textOnPageCheck(userScenario.commonExpectedResults.expectedTreatyLabel, Selectors.treatyLabelSelector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedTreatyExample, Selectors.treatyExampleSelector)
        inputFieldValueCheck("treaty", Selectors.treatyValueSelector, "exampleTreaty")

        textOnPageCheck(userScenario.commonExpectedResults.expectedReliefLabel, Selectors.reliefLabelSelector)
        textOnPageCheck(userScenario.commonExpectedResults.expectedReliefExample, Selectors.reliefExampleSelector)
        inputFieldValueCheck("amount-2", Selectors.reliefValueSelector, "99.99")

        buttonCheck(userScenario.commonExpectedResults.expectedContinue)
      }

      "render page with error text when no country is selected" which {
        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        val relief = Relief(Some(""))
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] =
          UserSessionDataRequest(aPensionsUserData.copy(
            pensions = aPensionsCYAEmptyModel.copy(paymentsIntoOverseasPensions = aPaymentsIntoOverseasPensionsViewModel
              .copy(reliefs = Seq(relief)))),
            if (userScenario.isAgent) anAgentUser else aUser,
            if (userScenario.isAgent) fakeAgentRequest else fakeIndividualRequest)

        val form: Form[DoubleTaxationAgreementFormModel] =
          DoubleTaxationAgreementForm.doubleTaxationAgreementForm("individual")
            .bind(Map("countryId" -> "noCountry", "article" -> "exampleArticle", "treaty" -> "exampleTreaty", "amount-2" -> "99.99"))


        implicit val document: Document = Jsoup.parse(underTest(form, taxYearEOY, Some(0)).body)

        titleCheck(userScenario.commonExpectedResults.errorTitle, userScenario.isWelsh)
        errorAboveElementCheck(userScenario.commonExpectedResults.noCountryErrorText, Some("countryId"))
        errorSummaryCheck(userScenario.commonExpectedResults.noCountryErrorText, "#countryId")
      }

      "render page with no results found error when country input doesn't match any countries" which {
        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        val relief = Relief(Some(""))
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] =
          UserSessionDataRequest(aPensionsUserData.copy(
            pensions = aPensionsCYAEmptyModel.copy(paymentsIntoOverseasPensions = aPaymentsIntoOverseasPensionsViewModel
              .copy(reliefs = Seq(relief)))),
            if (userScenario.isAgent) anAgentUser else aUser,
            if (userScenario.isAgent) fakeAgentRequest else fakeIndividualRequest)

        val form: Form[DoubleTaxationAgreementFormModel] =
          DoubleTaxationAgreementForm.doubleTaxationAgreementForm("individual")
            .bind(Map("article" -> "exampleArticle", "treaty" -> "exampleTreaty", "amount-2" -> "99.99"))


        implicit val document: Document = Jsoup.parse(underTest(form, taxYearEOY, Some(0)).body)

        titleCheck(userScenario.commonExpectedResults.errorTitle, userScenario.isWelsh)
        errorAboveElementCheck(userScenario.commonExpectedResults.noCountryErrorText, Some("countryId"))
        errorSummaryCheck(userScenario.commonExpectedResults.noCountryErrorText, "#countryId")
      }

      "render page with error text when no double taxation relief is inputted" which {
        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        val relief = Relief(Some(""))
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] =
          UserSessionDataRequest(aPensionsUserData.copy(
            pensions = aPensionsCYAEmptyModel.copy(paymentsIntoOverseasPensions = aPaymentsIntoOverseasPensionsViewModel
              .copy(reliefs = Seq(relief)))),
            if (userScenario.isAgent) anAgentUser else aUser,
            if (userScenario.isAgent) fakeAgentRequest else fakeIndividualRequest)

        val form: Form[DoubleTaxationAgreementFormModel] =
          DoubleTaxationAgreementForm.doubleTaxationAgreementForm("individual")
            .bind(Map("countryId" -> "AD", "article" -> "exampleArticle", "treaty" -> "exampleTreaty"))

        implicit val document: Document = Jsoup.parse(underTest(form, taxYearEOY, Some(0)).body)

        titleCheck(userScenario.commonExpectedResults.errorTitle, userScenario.isWelsh)
        errorAboveElementCheck(userScenario.commonExpectedResults.noReliefErrorText, Some("amount-2"))
        errorSummaryCheck(userScenario.commonExpectedResults.noReliefErrorText, "#amount-2")
      }

      "render page with incorrect format error when taxation relief input is in the wrong format" which {
        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        val relief = Relief(Some(""))
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] =
          UserSessionDataRequest(aPensionsUserData.copy(
            pensions = aPensionsCYAEmptyModel.copy(paymentsIntoOverseasPensions = aPaymentsIntoOverseasPensionsViewModel
              .copy(reliefs = Seq(relief)))),
            if (userScenario.isAgent) anAgentUser else aUser,
            if (userScenario.isAgent) fakeAgentRequest else fakeIndividualRequest)

        val form: Form[DoubleTaxationAgreementFormModel] =
          DoubleTaxationAgreementForm.doubleTaxationAgreementForm("individual")
            .bind(Map("countryId" -> "AD",
              "article" -> "exampleArticle",
              "treaty" -> "exampleTreaty",
              "amount-2" -> "wrongFormat"))

        implicit val document: Document = Jsoup.parse(underTest(form, taxYearEOY, Some(0)).body)

        titleCheck(userScenario.commonExpectedResults.errorTitle, userScenario.isWelsh)
        errorAboveElementCheck(userScenario.commonExpectedResults.reliefWrongFormatErrorText, Some("amount-2"))
        errorSummaryCheck(userScenario.commonExpectedResults.reliefWrongFormatErrorText, "#amount-2")
      }

      "render page with value too big error when taxation relief input is greater than £100,000,000,000" which {
        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        val relief = Relief(Some(""))
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] =
          UserSessionDataRequest(aPensionsUserData.copy(
            pensions = aPensionsCYAEmptyModel.copy(paymentsIntoOverseasPensions = aPaymentsIntoOverseasPensionsViewModel
              .copy(reliefs = Seq(relief)))),
            if (userScenario.isAgent) anAgentUser else aUser,
            if (userScenario.isAgent) fakeAgentRequest else fakeIndividualRequest)

        val form: Form[DoubleTaxationAgreementFormModel] =
          DoubleTaxationAgreementForm.doubleTaxationAgreementForm("individual")
            .bind(Map("countryId" -> "AD",
              "article" -> "exampleArticle",
              "treaty" -> "exampleTreaty",
              "amount-2" -> "100,000,000,099.99"))

        implicit val document: Document = Jsoup.parse(underTest(form, taxYearEOY, Some(0)).body)

        titleCheck(userScenario.commonExpectedResults.errorTitle, userScenario.isWelsh)
        errorAboveElementCheck(userScenario.commonExpectedResults.reliefTooBigErrorText, Some("amount-2"))
        errorSummaryCheck(userScenario.commonExpectedResults.reliefTooBigErrorText, "#amount-2")
      }
    }
  }
}