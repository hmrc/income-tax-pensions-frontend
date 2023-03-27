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

import builders.PaymentsIntoOverseasPensionsViewModelBuilder.{aPaymentsIntoOverseasPensionsEmptyViewModel, aPaymentsIntoOverseasPensionsViewModel}
import builders.PensionsCYAModelBuilder.aPensionsCYAModel
import builders.PensionsUserDataBuilder.aPensionsUserData
import builders.UserBuilder.aUser
import controllers.pensions.paymentsIntoOverseasPensions.ReliefsSchemeDetailsHelper.displayedValueForOptionalAmount
import models.pension.charges.{Relief, TaxReliefQuestion}
import models.requests.UserSessionDataRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import utils.FakeRequestProvider
import views.html.pensions.paymentsIntoOverseasPensions.ReliefSchemeDetailsView

class ReliefsSchemeDetailsViewSpec extends ViewUnitTest with FakeRequestProvider {

  object Selectors {
    val captionSelector = "#main-content > div > div > header > p"
    val titleSelector = "#main-content > div > div > header > h1"
  }

  object ChangeLinks {
    val schemeNamesLink = controllers.pensions.paymentsIntoOverseasPensions.routes.PensionsCustomerReferenceNumberController.show(taxYearEOY).url
    val untaxedEmployerPensionsLink = controllers.pensions.paymentsIntoOverseasPensions.routes.UntaxedEmployerPaymentsController.show(taxYearEOY, Some(0)).url
    val reliefTypeLink = controllers.pensions.paymentsIntoOverseasPensions.routes.PensionReliefTypeController.show(taxYearEOY, Some(0)).url
    val schemeDetailsSF74Link = controllers.pensions.paymentsIntoOverseasPensions.routes.SF74ReferenceController.show(taxYearEOY).url
    val schemeDetailsQOPSLink = controllers.pensions.paymentsIntoOverseasPensions.routes.QOPSReferenceController.show(taxYearEOY).url
    val schemeDetailsMigrationLink = controllers.pensions.paymentsIntoOverseasPensions.routes.ReliefsSchemeDetailsController.show(taxYearEOY, Some(0)).url //todo: redirect to migration page when added
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedTitle: String
    val schemeName: String
    val untaxedEmployerPayments: String
    val reliefType: String
    val schemeDetails: String
    val schemeNameHidden: String
    val untaxedEmployerPaymentsHidden: String
    val reliefTypeHidden: String
    val schemeDetailsHidden: String
    val no: String
    val button: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    override val expectedCaption: Int => String = (taxYear: Int) => s"Payments into overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    override val expectedTitle: String = "Pension scheme details"
    override val schemeName: String = "Pension scheme name"
    override val untaxedEmployerPayments: String = "Untaxed employer payments"
    override val reliefType: String = "Type of relief"
    override val schemeDetails: String = "Scheme details"
    override val schemeNameHidden: String = "Change pension scheme name"
    override val untaxedEmployerPaymentsHidden: String = "Change untaxed employer payments"
    override val reliefTypeHidden: String = "Change type of relief"
    override val schemeDetailsHidden: String = "Change scheme details"
    override val no: String = "No"
    override val button: String = "Continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    override val expectedCaption: Int => String = (taxYear: Int) => s"Payments into overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    override val expectedTitle: String = "Pension scheme details"
    override val schemeName: String = "Pension scheme name"
    override val untaxedEmployerPayments: String = "Untaxed employer payments"
    override val reliefType: String = "Type of relief"
    override val schemeDetails: String = "Scheme details"
    override val schemeNameHidden: String = "Change pension scheme name"
    override val untaxedEmployerPaymentsHidden: String = "Change untaxed employer payments"
    override val reliefTypeHidden: String = "Change type of relief"
    override val schemeDetailsHidden: String = "Change scheme details"
    override val no: String = "No"
    override val button: String = "Continue"
  }

  override protected val userScenarios: Seq[UserScenario[CommonExpectedResults, Unit]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY)
  )

  private lazy val underTest = inject[ReliefSchemeDetailsView]
  userScenarios.foreach { userScenario =>
    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {
    "Render the page with prefilled data for Transitional corresponding relief" which {
      implicit val messages: Messages = getMessages(userScenario.isWelsh)
      val updatedRelief: Relief = Relief(
        Some("PENSIONINCOME245"), Some(193.54), Some(TaxReliefQuestion.TransitionalCorrespondingRelief), sf74Reference = Some("123456"))
      implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = UserSessionDataRequest(aPensionsUserData.copy(
        pensions = aPensionsCYAModel.copy(
          paymentsIntoOverseasPensions = aPaymentsIntoOverseasPensionsViewModel.copy(
            reliefs = Seq(updatedRelief)))), aUser, fakeIndividualRequest)
      implicit val document: Document = Jsoup.parse(underTest(taxYearEOY, updatedRelief, Some(0)).body)

      captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY), Selectors.captionSelector)
      titleCheck(userScenario.commonExpectedResults.expectedTitle, userScenario.isWelsh)
      cyaRowCheck(userScenario.commonExpectedResults.schemeName,
        "PENSIONINCOME245",
        ChangeLinks.schemeNamesLink,
        userScenario.commonExpectedResults.schemeNameHidden,
        1)
      cyaRowCheck(userScenario.commonExpectedResults.untaxedEmployerPayments,
        "£193.54",
        ChangeLinks.untaxedEmployerPensionsLink,
        userScenario.commonExpectedResults.untaxedEmployerPaymentsHidden,
        2)
      cyaRowCheck(userScenario.commonExpectedResults.reliefType,
        TaxReliefQuestion.TransitionalCorrespondingRelief,
        ChangeLinks.reliefTypeLink,
        userScenario.commonExpectedResults.reliefTypeHidden,
        3)
      cyaRowCheck(userScenario.commonExpectedResults.schemeDetails,
        "123456",
        ChangeLinks.schemeDetailsSF74Link,
        userScenario.commonExpectedResults.schemeDetailsHidden,
        4)
      buttonCheck(userScenario.commonExpectedResults.button)
    }

      "Render the page with prefilled data for Migrant member relief" which {
      implicit val messages: Messages = getMessages(userScenario.isWelsh)
        val updatedRelief: Relief = Relief(
          Some("PENSIONINCOME245"), Some(193.54), Some(TaxReliefQuestion.MigrantMemberRelief), qualifyingOverseasPensionSchemeReferenceNumber = Some("123456")
        )
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = UserSessionDataRequest(aPensionsUserData.copy(
          pensions = aPensionsCYAModel.copy(
            paymentsIntoOverseasPensions = aPaymentsIntoOverseasPensionsViewModel.copy(
              reliefs = Seq(updatedRelief)))), aUser, fakeIndividualRequest)
        implicit val document: Document = Jsoup.parse(underTest(taxYearEOY, updatedRelief, Some(0)).body)

        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY), Selectors.captionSelector)
        titleCheck(userScenario.commonExpectedResults.expectedTitle, userScenario.isWelsh)
        cyaRowCheck(userScenario.commonExpectedResults.schemeName,
          "PENSIONINCOME245",
          ChangeLinks.schemeNamesLink,
          userScenario.commonExpectedResults.schemeNameHidden,
          1)
        cyaRowCheck(userScenario.commonExpectedResults.untaxedEmployerPayments,
          "£193.54",
          ChangeLinks.untaxedEmployerPensionsLink,
          userScenario.commonExpectedResults.untaxedEmployerPaymentsHidden,
          2)
        cyaRowCheck(userScenario.commonExpectedResults.reliefType,
          TaxReliefQuestion.MigrantMemberRelief,
          ChangeLinks.reliefTypeLink,
          userScenario.commonExpectedResults.reliefTypeHidden,
          3)
        cyaRowCheck(userScenario.commonExpectedResults.schemeDetails,
          "123456",
          ChangeLinks.schemeDetailsQOPSLink,
          userScenario.commonExpectedResults.schemeDetailsHidden,
          4)
        buttonCheck(userScenario.commonExpectedResults.button)
      }

      "Render the page with prefilled data for Double taxation relief" which {
        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        val updatedRelief: Relief = Relief(
          Some("PENSIONINCOME245"),
          Some(193.54),
          Some(TaxReliefQuestion.DoubleTaxationRelief),
          doubleTaxationCountryCode = Some("Germany"),
          doubleTaxationCountryArticle = Some("AB3211-1"),
          doubleTaxationCountryTreaty = Some("Munich"),
          doubleTaxationReliefAmount = Some(123.45)
        )
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = UserSessionDataRequest(aPensionsUserData.copy(
          pensions = aPensionsCYAModel.copy(
            paymentsIntoOverseasPensions = aPaymentsIntoOverseasPensionsViewModel.copy(
              reliefs = Seq(updatedRelief)))), aUser, fakeIndividualRequest)
        implicit val document: Document = Jsoup.parse(underTest(taxYearEOY, updatedRelief, Some(0)).body)

        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY), Selectors.captionSelector)
        titleCheck(userScenario.commonExpectedResults.expectedTitle, userScenario.isWelsh)
        cyaRowCheck(userScenario.commonExpectedResults.schemeName,
          "PENSIONINCOME245",
          ChangeLinks.schemeNamesLink,
          userScenario.commonExpectedResults.schemeNameHidden,
          1)
        cyaRowCheck(userScenario.commonExpectedResults.untaxedEmployerPayments,
          "£193.54",
          ChangeLinks.untaxedEmployerPensionsLink,
          userScenario.commonExpectedResults.untaxedEmployerPaymentsHidden,
          2)
        cyaRowCheck(userScenario.commonExpectedResults.reliefType,
          TaxReliefQuestion.DoubleTaxationRelief,
          ChangeLinks.reliefTypeLink,
          userScenario.commonExpectedResults.reliefTypeHidden,
          3)
        cyaRowCheck(userScenario.commonExpectedResults.schemeDetails,
          s"Country code: Germany Article: AB3211-1 Treaty: Munich Relief: £123.45",
          ChangeLinks.schemeDetailsMigrationLink,
          userScenario.commonExpectedResults.schemeDetailsHidden,
          4)
        buttonCheck(userScenario.commonExpectedResults.button)
      }

      "Render the page with prefilled data when all cases are no" which {
        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        val updatedRelief: Relief = Relief(None, None, None)
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = UserSessionDataRequest(aPensionsUserData.copy(
          pensions = aPensionsCYAModel.copy(
            paymentsIntoOverseasPensions = aPaymentsIntoOverseasPensionsViewModel.copy(
              reliefs = Seq(updatedRelief)))), aUser, fakeIndividualRequest)
        implicit val document: Document = Jsoup.parse(underTest(taxYearEOY, updatedRelief, Some(0)).body)

        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY), Selectors.captionSelector)
        titleCheck(userScenario.commonExpectedResults.expectedTitle, userScenario.isWelsh)
        cyaRowCheck(userScenario.commonExpectedResults.schemeName,
          userScenario.commonExpectedResults.no,
          ChangeLinks.schemeNamesLink,
          userScenario.commonExpectedResults.schemeNameHidden,
          1)
        cyaRowCheck(userScenario.commonExpectedResults.untaxedEmployerPayments,
          userScenario.commonExpectedResults.no,
          ChangeLinks.untaxedEmployerPensionsLink,
          userScenario.commonExpectedResults.untaxedEmployerPaymentsHidden,
          2)
        cyaRowCheck(userScenario.commonExpectedResults.reliefType,
          "No tax relief",
          ChangeLinks.reliefTypeLink,
          userScenario.commonExpectedResults.reliefTypeHidden,
          3)
        buttonCheck(userScenario.commonExpectedResults.button)
      }

      "Render the page with prefilled data when none of the above is selected" which {
        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        val updatedRelief: Relief = Relief(Some("PENSIONINCOME245"),
          Some(193.54),
          reliefType = Some(TaxReliefQuestion.NoTaxRelief))

        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = UserSessionDataRequest(aPensionsUserData.copy(
          pensions = aPensionsCYAModel.copy(
            paymentsIntoOverseasPensions = aPaymentsIntoOverseasPensionsViewModel.copy(
              reliefs = Seq(updatedRelief)))), aUser, fakeIndividualRequest)
        implicit val document: Document = Jsoup.parse(underTest(taxYearEOY, updatedRelief, Some(0)).body)

        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY), Selectors.captionSelector)
        titleCheck(userScenario.commonExpectedResults.expectedTitle, userScenario.isWelsh)
        cyaRowCheck(userScenario.commonExpectedResults.schemeName,
          "PENSIONINCOME245",
          ChangeLinks.schemeNamesLink,
          userScenario.commonExpectedResults.schemeNameHidden,
          1)
        cyaRowCheck(userScenario.commonExpectedResults.untaxedEmployerPayments,
          "£193.54",
          ChangeLinks.untaxedEmployerPensionsLink,
          userScenario.commonExpectedResults.untaxedEmployerPaymentsHidden,
          2)
        cyaRowCheck(userScenario.commonExpectedResults.reliefType,
          "No tax relief",
          ChangeLinks.reliefTypeLink,
          userScenario.commonExpectedResults.reliefTypeHidden,
          3)
        buttonCheck(userScenario.commonExpectedResults.button)
      }

    }
    }
}
