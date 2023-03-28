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

package views.paymentsIntoOverSeasPensions

import builders.PaymentsIntoOverseasPensionsViewModelBuilder.aPaymentsIntoOverseasPensionsViewModel
import builders.PensionsCYAModelBuilder.aPensionsCYAEmptyModel
import builders.PensionsUserDataBuilder.aPensionsUserData
import builders.UserBuilder.{aUser, anAgentUser}
import forms.FormsProvider
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
import views.html.pensions.paymentsIntoOverseasPensions.{DoubleTaxationAgreementView, PensionReliefTypeView}

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
  }

  object CommonExpectedEN extends CommonExpectedResults {
    override val expectedCaption:  Int => String = (taxYear: Int) => s"Payments into overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    override val expectedTitle: String = "Double taxation agreement details"
    override val expectedCountyLabel: String = "Country"
    override val expectedArticleLabel: String = "Article (optional)"
    override val expectedArticleExample: String = "For example, ‘AB3211-1'"
    override val expectedTreatyLabel: String = "Treaty (optional)"
    override val expectedTreatyExample: String = "For example, 'Munich'"
    override val expectedReliefLabel: String = "Double taxation relief"
    override val expectedReliefExample: String = "For example, £193.54"
    override val expectedContinue: String = "Continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    override val expectedCaption:  Int => String = (taxYear: Int) => s"Payments into overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    override val expectedTitle: String = "Double taxation agreement details"
    override val expectedCountyLabel: String = "Country"
    override val expectedArticleLabel: String = "Article (optional)"
    override val expectedArticleExample: String = "For example, ‘AB3211-1'"
    override val expectedTreatyLabel: String = "Treaty (optional)"
    override val expectedTreatyExample: String = "For example, 'Munich'"
    override val expectedReliefLabel: String = "Double taxation relief"
    override val expectedReliefExample: String = "For example, £193.54"
    override val expectedContinue: String = "Continue"
  }

  object Selectors {
    val captionSelector = "#main-content > div > div > header > p"
    val titleSelector = "#main-content > div > div > header > h1"
    val buttonSelector = "#continue"
    val countryLabelSelector = "#main-content > div > div > form > div:nth-child(2) > div > label"
    val articleLabelSelector = "#main-content > div > div > form > div:nth-child(3) > label"
    val treatyLabelSelector = "#main-content > div > div > form > div:nth-child(4) > label"
    val reliefLabelSelector = "#main-content > div > div > form > div:nth-child(5) > label" //todo fix label format
    val articleExampleSelector = "#article-hint"
    val treatyExampleSelector = "#treaty-hint"
    val reliefExampleSelector = "#relief-hint"
    val countryValueSelector = "#countryId"
    val articleValueSelector = "#article"
    val treatyValueSelector = "#treaty"
    val reliefValueSelector = "#amount-2"
  }

  override protected val userScenarios: Seq[UserScenario[CommonExpectedResults, Unit]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY)
  )

  private lazy val underTest = inject[DoubleTaxationAgreementView]
  userScenarios.foreach { userScenario =>
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
      "render page with no prefilled data" which {
        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] =
          UserSessionDataRequest(aPensionsUserData.copy(
            pensions = aPensionsCYAEmptyModel.copy(paymentsIntoOverseasPensions = aPaymentsIntoOverseasPensionsViewModel.
              copy(reliefs = Seq.empty[Relief]))),
            if (userScenario.isAgent) anAgentUser else aUser,
            if (userScenario.isAgent) fakeAgentRequest else fakeIndividualRequest)

        def form: Form[DoubleTaxationAgreementFormModel] = DoubleTaxationAgreementForm.doubleTaxationAgreementForm("individual")

        implicit val document: Document = Jsoup.parse(underTest(form, taxYearEOY, Some(1)).body)

        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY), Selectors.captionSelector)
        titleCheck(userScenario.commonExpectedResults.expectedTitle, userScenario.isWelsh)
        textOnPageCheck(userScenario.commonExpectedResults.expectedCountyLabel, Selectors.countryLabelSelector)
        inputFieldValueCheck("countryId",Selectors.countryValueSelector,"")
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
      }
    }
}
