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

package views.paymentsIntoOverseasPensions

import builders.PaymentsIntoOverseasPensionsViewModelBuilder.aPaymentsIntoOverseasPensionsViewModel
import builders.PensionsCYAModelBuilder.aPensionsCYAModel
import builders.PensionsUserDataBuilder.aPensionsUserData
import builders.UserBuilder.aUser
import models.pension.charges.{Relief, TaxReliefQuestion}
import models.requests.UserSessionDataRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import utils.FakeRequestProvider
import views.html.pensions.paymentsIntoOverseasPensions.RemoveReliefSchemeView


class RemoveReliefSchemeViewSpec extends ViewUnitTest with FakeRequestProvider {

    object Selectors {
      val captionSelector = "#main-content > div > div > header > p"
      val titleSelector = "#main-content > div > div > header > h1"
      val paraSelector = s"#main-content > div > div > form > p"
    }

    trait CommonExpectedResults {
      val expectedCaption: Int => String
      val expectedTitle: String
      val expectedSubCaption: String
      val schemeName: String
      val untaxedEmployerPayments: String
      val reliefType: String
      val schemeDetails: String
      val migrantMemberRelief: String
      val noTaxRelief: String
      val no: String
      val button: String
    }

    object CommonExpectedEN extends CommonExpectedResults {
      override val expectedCaption: Int => String = (taxYear: Int) => s"Payments into overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
      override val expectedTitle: String = "Are you sure you want to remove this overseas pension scheme?"
      override val expectedSubCaption: String = "This will remove:"
      override val schemeName: String = "Pension scheme name"
      override val untaxedEmployerPayments: String = "Untaxed employer payments"
      override val reliefType: String = "Type of relief"
      override val schemeDetails: String = "Scheme details"
      override val migrantMemberRelief: String = "Migrant member relief"
      override val noTaxRelief = "No tax relief"
      override val no: String = "No"
      override val button: String = "Remove"
    }

    object CommonExpectedCY extends CommonExpectedResults {
      override val expectedCaption: Int => String = (taxYear: Int) => s"Taliadau i bensiynau tramor ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
      override val expectedTitle: String = "Are you sure you want to remove this overseas pension scheme?"
      override val expectedSubCaption: String = "This will remove:"
      override val schemeName: String = "Enw’r cynllun pensiwn"
      override val untaxedEmployerPayments: String = "Taliadau cyflogwr sydd heb eu trethu"
      override val reliefType: String = "Math o ryddhad"
      override val schemeDetails: String = "Manylion y cynllun"
      override val migrantMemberRelief: String = "Rhyddhad aelod mudol"
      override val noTaxRelief = "Dim rhyddhad treth"
      override val no: String = "Na"
      override val button: String = "Tynnu"
    }

    override protected val userScenarios: Seq[UserScenario[CommonExpectedResults, Unit]] = Seq(
      UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY)
    )


    private lazy val underTest = inject[RemoveReliefSchemeView]
    userScenarios.foreach { userScenario =>
      s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
        "Render the page with prefilled data for Migrant Scheme" which {
          implicit val messages: Messages = getMessages(userScenario.isWelsh)

          val reliefToRemove: Relief = Relief(customerReference = Some("PENSIONINCOME245"),
            employerPaymentsAmount = Some(193.54),
            reliefType = Some(TaxReliefQuestion.MigrantMemberRelief),
            qopsReference = Some("123456"),
            sf74Reference = Some("123456"))

          implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = UserSessionDataRequest(aPensionsUserData.copy(
            pensions = aPensionsCYAModel.copy(
              paymentsIntoOverseasPensions = aPaymentsIntoOverseasPensionsViewModel.copy(
                reliefs = Seq(reliefToRemove)))), aUser, fakeIndividualRequest)

          implicit val document: Document = Jsoup.parse(underTest(taxYearEOY, Seq(reliefToRemove), Some(0)).body)

          captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY), Selectors.captionSelector)
          titleCheck(userScenario.commonExpectedResults.expectedTitle, userScenario.isWelsh)
          captionCheck(userScenario.commonExpectedResults.expectedSubCaption, Selectors.paraSelector)
          removeCyaRowCheck(userScenario.commonExpectedResults.schemeName,
            "PENSIONINCOME245", 1)
          removeCyaRowCheck(userScenario.commonExpectedResults.untaxedEmployerPayments,
            "£193.54",
            2)
          removeCyaRowCheck(userScenario.commonExpectedResults.reliefType,
            "Migrant member relief",
            rowNumber = 3)
          buttonCheck(userScenario.commonExpectedResults.button)
        }
      }
    }
  }


