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
import builders.PensionsCYAModelBuilder.aPensionsCYAModel
import builders.PensionsUserDataBuilder.aPensionsUserData
import builders.UserBuilder.aUser
import models.pension.charges.{OverseasPensionScheme, TaxReliefQuestion}
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
    val titleSelector   = "#main-content > div > div > header > h1"
  }

  object ChangeLinks {
    val schemeNamesLink =
      controllers.pensions.paymentsIntoOverseasPensions.routes.PensionsCustomerReferenceNumberController.show(taxYearEOY, Some(0)).url
    val untaxedEmployerPensionsLink =
      controllers.pensions.paymentsIntoOverseasPensions.routes.UntaxedEmployerPaymentsController.show(taxYearEOY, Some(0)).url
    val reliefTypeLink        = controllers.pensions.paymentsIntoOverseasPensions.routes.PensionReliefTypeController.show(taxYearEOY, Some(0)).url
    val schemeDetailsSF74Link = controllers.pensions.paymentsIntoOverseasPensions.routes.SF74ReferenceController.show(taxYearEOY, Some(0)).url
    val schemeDetailsQOPSLink = controllers.pensions.paymentsIntoOverseasPensions.routes.QOPSReferenceController.show(taxYearEOY, Some(0)).url
    val schemeDetailsDblTaxLink =
      controllers.pensions.paymentsIntoOverseasPensions.routes.DoubleTaxationAgreementController.show(taxYearEOY, Some(0)).url
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
    val doubleTaxationRelief: String
    val migrantMemberRelief: String
    val transitionalCorrespondingRelief: String
    val noTaxRelief: String
    val details: String
    val no: String
    val button: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    override val expectedCaption: Int => String  = (taxYear: Int) => s"Payments into overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    override val expectedTitle: String           = "Pension scheme details"
    override val schemeName: String              = "Pension scheme name"
    override val untaxedEmployerPayments: String = "Untaxed employer payments"
    override val reliefType: String              = "Type of relief"
    override val schemeDetails: String           = "Scheme details"
    override val schemeNameHidden: String        = "Change pension scheme name"
    override val untaxedEmployerPaymentsHidden: String   = "Change untaxed employer payments"
    override val reliefTypeHidden: String                = "Change type of relief"
    override val schemeDetailsHidden: String             = "Change scheme details"
    override val doubleTaxationRelief: String            = "Double taxation relief"
    override val migrantMemberRelief: String             = "Migrant member relief"
    override val transitionalCorrespondingRelief: String = "Transitional corresponding relief"
    override val noTaxRelief                             = "No tax relief"
    override val details                                 = "Country code: Germany Article: AB3211-1 Treaty: Munich Relief: £123.45"
    override val no: String                              = "No"
    override val button: String                          = "Continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    override val expectedCaption: Int => String = (taxYear: Int) =>
      s"Taliadau i bensiynau tramor ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    override val expectedTitle: String                   = "Manylion y cynllun pensiwn"
    override val schemeName: String                      = "Enw’r cynllun pensiwn"
    override val untaxedEmployerPayments: String         = "Taliadau cyflogwr sydd heb eu trethu"
    override val reliefType: String                      = "Math o ryddhad"
    override val schemeDetails: String                   = "Manylion y cynllun"
    override val schemeNameHidden: String                = "Newid enw’r cynllun pensiwn"
    override val untaxedEmployerPaymentsHidden: String   = "Newid taliadau cyflogwr sydd heb eu trethu"
    override val reliefTypeHidden: String                = "Newid y math o ryddhad"
    override val schemeDetailsHidden: String             = "Newid manylion y cynllun"
    override val doubleTaxationRelief: String            = "Rhyddhad trethiant dwbl"
    override val migrantMemberRelief: String             = "Rhyddhad aelod mudol"
    override val transitionalCorrespondingRelief: String = "Rhyddhad cyfatebol trosiannol"
    override val noTaxRelief                             = "Dim rhyddhad treth"
    override val details                                 = "Cod y wlad: Germany Erthygl: AB3211-1 Cytuniad: Munich Rhyddhad: £123.45"
    override val no: String                              = "Na"
    override val button: String                          = "Yn eich blaen"
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
        val updatedRelief: OverseasPensionScheme =
          OverseasPensionScheme(
            Some("PENSIONINCOME245"),
            Some(193.54),
            Some(TaxReliefQuestion.TransitionalCorrespondingRelief),
            sf74Reference = Some("123456"))
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = UserSessionDataRequest(
          aPensionsUserData.copy(
            pensions =
              aPensionsCYAModel.copy(paymentsIntoOverseasPensions = aPaymentsIntoOverseasPensionsViewModel.copy(schemes = Seq(updatedRelief)))),
          aUser,
          fakeIndividualRequest
        )
        implicit val document: Document = Jsoup.parse(underTest(taxYearEOY, updatedRelief, Some(0)).body)

        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY), Selectors.captionSelector)
        titleCheck(userScenario.commonExpectedResults.expectedTitle, userScenario.isWelsh)
        cyaRowCheck(
          userScenario.commonExpectedResults.schemeName,
          "PENSIONINCOME245",
          ChangeLinks.schemeNamesLink,
          userScenario.commonExpectedResults.schemeNameHidden,
          1)
        cyaRowCheck(
          userScenario.commonExpectedResults.untaxedEmployerPayments,
          "£193.54",
          ChangeLinks.untaxedEmployerPensionsLink,
          userScenario.commonExpectedResults.untaxedEmployerPaymentsHidden,
          2
        )
        cyaRowCheck(
          userScenario.commonExpectedResults.reliefType,
          userScenario.commonExpectedResults.transitionalCorrespondingRelief,
          ChangeLinks.reliefTypeLink,
          userScenario.commonExpectedResults.reliefTypeHidden,
          3
        )
        cyaRowCheck(
          userScenario.commonExpectedResults.schemeDetails,
          "123456",
          ChangeLinks.schemeDetailsSF74Link,
          userScenario.commonExpectedResults.schemeDetailsHidden,
          4)
        buttonCheck(userScenario.commonExpectedResults.button)
      }

      "Render the page with prefilled data for Migrant member relief" which {
        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        val updatedRelief: OverseasPensionScheme = OverseasPensionScheme(
          Some("PENSIONINCOME245"),
          Some(193.54),
          Some(TaxReliefQuestion.MigrantMemberRelief),
          qopsReference = Some("123456")
        )
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = UserSessionDataRequest(
          aPensionsUserData.copy(
            pensions =
              aPensionsCYAModel.copy(paymentsIntoOverseasPensions = aPaymentsIntoOverseasPensionsViewModel.copy(schemes = Seq(updatedRelief)))),
          aUser,
          fakeIndividualRequest
        )
        implicit val document: Document = Jsoup.parse(underTest(taxYearEOY, updatedRelief, Some(0)).body)

        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY), Selectors.captionSelector)
        titleCheck(userScenario.commonExpectedResults.expectedTitle, userScenario.isWelsh)
        cyaRowCheck(
          userScenario.commonExpectedResults.schemeName,
          "PENSIONINCOME245",
          ChangeLinks.schemeNamesLink,
          userScenario.commonExpectedResults.schemeNameHidden,
          1)
        cyaRowCheck(
          userScenario.commonExpectedResults.untaxedEmployerPayments,
          "£193.54",
          ChangeLinks.untaxedEmployerPensionsLink,
          userScenario.commonExpectedResults.untaxedEmployerPaymentsHidden,
          2
        )
        cyaRowCheck(
          userScenario.commonExpectedResults.reliefType,
          userScenario.commonExpectedResults.migrantMemberRelief,
          ChangeLinks.reliefTypeLink,
          userScenario.commonExpectedResults.reliefTypeHidden,
          3
        )
        cyaRowCheck(
          userScenario.commonExpectedResults.schemeDetails,
          "123456",
          ChangeLinks.schemeDetailsQOPSLink,
          userScenario.commonExpectedResults.schemeDetailsHidden,
          4)
        buttonCheck(userScenario.commonExpectedResults.button)
      }

      "Render the page with prefilled data for Double taxation relief" which {
        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        val updatedRelief: OverseasPensionScheme = OverseasPensionScheme(
          Some("PENSIONINCOME245"),
          Some(193.54),
          Some(TaxReliefQuestion.DoubleTaxationRelief),
          alphaTwoCountryCode = Some("Germany"),
          doubleTaxationArticle = Some("AB3211-1"),
          doubleTaxationTreaty = Some("Munich"),
          doubleTaxationReliefAmount = Some(123.45)
        )
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = UserSessionDataRequest(
          aPensionsUserData.copy(
            pensions =
              aPensionsCYAModel.copy(paymentsIntoOverseasPensions = aPaymentsIntoOverseasPensionsViewModel.copy(schemes = Seq(updatedRelief)))),
          aUser,
          fakeIndividualRequest
        )
        implicit val document: Document = Jsoup.parse(underTest(taxYearEOY, updatedRelief, Some(0)).body)

        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY), Selectors.captionSelector)
        titleCheck(userScenario.commonExpectedResults.expectedTitle, userScenario.isWelsh)
        cyaRowCheck(
          userScenario.commonExpectedResults.schemeName,
          "PENSIONINCOME245",
          ChangeLinks.schemeNamesLink,
          userScenario.commonExpectedResults.schemeNameHidden,
          1)
        cyaRowCheck(
          userScenario.commonExpectedResults.untaxedEmployerPayments,
          "£193.54",
          ChangeLinks.untaxedEmployerPensionsLink,
          userScenario.commonExpectedResults.untaxedEmployerPaymentsHidden,
          2
        )
        cyaRowCheck(
          userScenario.commonExpectedResults.reliefType,
          userScenario.commonExpectedResults.doubleTaxationRelief,
          ChangeLinks.reliefTypeLink,
          userScenario.commonExpectedResults.reliefTypeHidden,
          3
        )
        cyaRowCheck(
          userScenario.commonExpectedResults.schemeDetails,
          userScenario.commonExpectedResults.details,
          ChangeLinks.schemeDetailsDblTaxLink,
          userScenario.commonExpectedResults.schemeDetailsHidden,
          4
        )
        buttonCheck(userScenario.commonExpectedResults.button)
      }

      "Render the page with prefilled data when all cases are no" which {
        implicit val messages: Messages          = getMessages(userScenario.isWelsh)
        val updatedRelief: OverseasPensionScheme = OverseasPensionScheme(None, None, None)
        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = UserSessionDataRequest(
          aPensionsUserData.copy(
            pensions =
              aPensionsCYAModel.copy(paymentsIntoOverseasPensions = aPaymentsIntoOverseasPensionsViewModel.copy(schemes = Seq(updatedRelief)))),
          aUser,
          fakeIndividualRequest
        )
        implicit val document: Document = Jsoup.parse(underTest(taxYearEOY, updatedRelief, Some(0)).body)

        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY), Selectors.captionSelector)
        titleCheck(userScenario.commonExpectedResults.expectedTitle, userScenario.isWelsh)
        cyaRowCheck(
          userScenario.commonExpectedResults.schemeName,
          userScenario.commonExpectedResults.no,
          ChangeLinks.schemeNamesLink,
          userScenario.commonExpectedResults.schemeNameHidden,
          1
        )
        cyaRowCheck(
          userScenario.commonExpectedResults.untaxedEmployerPayments,
          userScenario.commonExpectedResults.no,
          ChangeLinks.untaxedEmployerPensionsLink,
          userScenario.commonExpectedResults.untaxedEmployerPaymentsHidden,
          2
        )
        cyaRowCheck(
          userScenario.commonExpectedResults.reliefType,
          userScenario.commonExpectedResults.noTaxRelief,
          ChangeLinks.reliefTypeLink,
          userScenario.commonExpectedResults.reliefTypeHidden,
          3
        )
        buttonCheck(userScenario.commonExpectedResults.button)
      }

      "Render the page with prefilled data when none of the above is selected" which {
        implicit val messages: Messages = getMessages(userScenario.isWelsh)
        val updatedRelief: OverseasPensionScheme =
          OverseasPensionScheme(Some("PENSIONINCOME245"), Some(193.54), reliefType = Some(TaxReliefQuestion.NoTaxRelief))

        implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = UserSessionDataRequest(
          aPensionsUserData.copy(
            pensions =
              aPensionsCYAModel.copy(paymentsIntoOverseasPensions = aPaymentsIntoOverseasPensionsViewModel.copy(schemes = Seq(updatedRelief)))),
          aUser,
          fakeIndividualRequest
        )
        implicit val document: Document = Jsoup.parse(underTest(taxYearEOY, updatedRelief, Some(0)).body)

        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY), Selectors.captionSelector)
        titleCheck(userScenario.commonExpectedResults.expectedTitle, userScenario.isWelsh)
        cyaRowCheck(
          userScenario.commonExpectedResults.schemeName,
          "PENSIONINCOME245",
          ChangeLinks.schemeNamesLink,
          userScenario.commonExpectedResults.schemeNameHidden,
          1)
        cyaRowCheck(
          userScenario.commonExpectedResults.untaxedEmployerPayments,
          "£193.54",
          ChangeLinks.untaxedEmployerPensionsLink,
          userScenario.commonExpectedResults.untaxedEmployerPaymentsHidden,
          2
        )
        cyaRowCheck(
          userScenario.commonExpectedResults.reliefType,
          userScenario.commonExpectedResults.noTaxRelief,
          ChangeLinks.reliefTypeLink,
          userScenario.commonExpectedResults.reliefTypeHidden,
          3
        )
        buttonCheck(userScenario.commonExpectedResults.button)
      }
    }
  }
}
