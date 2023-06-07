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

package views.transferIntoOverseasPensions

import builders.TransfersIntoOverseasPensionsViewModelBuilder.{aTransfersIntoOverseasPensionsViewModel, emptyTransfersIntoOverseasPensionsViewModel}
import controllers.pensions.transferIntoOverseasPensions.routes
import models.AuthorisationRequest
import models.pension.charges.TransfersIntoOverseasPensionsViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.pensions.transferIntoOverseasPensions.TransferIntoOverseasPensionsCYAView

class TransferIntoOverseasPensionsCYAViewSpec extends ViewUnitTest {

  object Selectors {
    val continueButtonSelector: String = "#continue"
    val paragraphSelector = "#main-content > div > div > p"
    val ukPensionSchemesRowSelector = ".govuk-summary-list__row:nth-of-type(2)"
  }

  trait CommonExpectedResults {
    val expectedTitle: String
    lazy val expectedHeading = expectedTitle
    val expectedCaption: Int => String
    val buttonText: String
    val yesText: String
    val noText: String
    val noTaxPaidText: String
    val transfersIOP: String
    val transfersIOPChange: String
    val amountCharged: String
    val amountChargedChange: String
    val taxOnAmount: String
    val taxOnAmountChange: String
    val schemesPayingTax: String
    val schemesPayingTaxChange: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedTitle = "Check transfers into overseas pensions"
    val expectedCaption: Int => String = (taxYear: Int) => s"Transfers into overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val buttonText = "Save and continue"
    val yesText = "Yes"
    val noText = "No"
    val noTaxPaidText = "No tax paid"
    val transfersIOP = "Transfers into overseas pensions"
    val transfersIOPChange = "Change transfers into overseas pensions"
    val amountCharged = "Amount charged"
    val amountChargedChange = "Change amount charged"
    val taxOnAmount = "Tax on amount charged"
    val taxOnAmountChange = "Change tax on amount charged"
    val schemesPayingTax = "Schemes paying tax"
    val schemesPayingTaxChange = "Change schemes paying tax"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedTitle = "Gwirio trosglwyddiadau i bensiynau tramor"
    val expectedCaption: Int => String = (taxYear: Int) => s"Trosglwyddiadau i bensiynau tramor ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val buttonText = "Cadw ac yn eich blaen"
    val yesText = "Iawn"
    val noText = "Na"
    val noTaxPaidText = "No tax paid"
    val transfersIOP = "Trosglwyddiadau i bensiynau tramor"
    val transfersIOPChange = "Newid trosglwyddiadau i bensiynau tramor"
    val amountCharged = "Y swm a godir"
    val amountChargedChange = "Newid y swm a godir"
    val taxOnAmount = "Treth ar y swm a godir"
    val taxOnAmountChange = "Newid y dreth ar y swm a godir"
    val schemesPayingTax = "Cynlluniau sy’n talu treth"
    val schemesPayingTaxChange = "Newid y cynlluniau sy’n talu treth"
  }


  object ChangeLinks {
    val transferPensionSavingsUrl = routes.TransferPensionSavingsController.show(taxYearEOY).url
    val overseasTransferChargeUrl = routes.OverseasTransferChargeController.show(taxYearEOY).url
    val pensionSchemeTaxTransferUrl = routes.PensionSchemeTaxTransferController.show(taxYearEOY).url
    val transferChargeSummaryUrl = routes.TransferChargeSummaryController.show(taxYearEOY).url
  }

  private lazy val underTest = inject[TransferIntoOverseasPensionsCYAView]

  private def renderPage(userScenario: UserScenario[CommonExpectedResults, String], model: TransfersIntoOverseasPensionsViewModel): Document = {
    import userScenario.commonExpectedResults._
    implicit val authorisationRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
    implicit val messages: Messages = getMessages(userScenario.isWelsh)

    val htmlFormat = underTest(taxYearEOY, model)
    implicit val document: Document = Jsoup.parse(htmlFormat.body)

    titleCheck(expectedTitle, userScenario.isWelsh)
    h1Check(expectedHeading)
    captionCheck(expectedCaption(taxYearEOY))
    buttonCheck(buttonText)
    welshToggleCheck(userScenario.isWelsh)
    document
  }

  override val userScenarios: Seq[UserScenario[CommonExpectedResults, String]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, None),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, None)
  )

  userScenarios.foreach { userScenario =>
    import userScenario.commonExpectedResults._

    s"language is ${welshTest(userScenario.isWelsh)}" should {

      "render the CYA page with a full CYA model" which {
        implicit val document: Document = renderPage(userScenario, aTransfersIntoOverseasPensionsViewModel)

        cyaRowCheck(transfersIOP, yesText, ChangeLinks.transferPensionSavingsUrl, transfersIOPChange, 1)
        cyaRowCheck(amountCharged, "£1,999.99", ChangeLinks.overseasTransferChargeUrl, amountChargedChange, 2)
        cyaRowCheck(taxOnAmount, "£1,000", ChangeLinks.pensionSchemeTaxTransferUrl, taxOnAmountChange, 3)
        cyaRowCheck(schemesPayingTax, "Foreign Scheme Name", ChangeLinks.transferChargeSummaryUrl, schemesPayingTaxChange, 4)
      }

      "render the CYA page with no tax paid on amount charged" which {
        implicit val document: Document = renderPage(userScenario, aTransfersIntoOverseasPensionsViewModel.copy(
          pensionSchemeTransferCharge = Some(false), pensionSchemeTransferChargeAmount = None, transferPensionScheme = Nil
        ))

        cyaRowCheck(transfersIOP, yesText, ChangeLinks.transferPensionSavingsUrl, transfersIOPChange, 1)
        cyaRowCheck(amountCharged, "£1,999.99", ChangeLinks.overseasTransferChargeUrl, amountChargedChange, 2)
        cyaRowCheck(taxOnAmount, noTaxPaidText, ChangeLinks.pensionSchemeTaxTransferUrl, taxOnAmountChange, 3)
      }

      "render the CYA page with no transfers into overseas pensions" which {
        implicit val document: Document = renderPage(userScenario, emptyTransfersIntoOverseasPensionsViewModel.copy(
          transferPensionSavings = Some(false)))

        cyaRowCheck(transfersIOP, noText, ChangeLinks.transferPensionSavingsUrl, transfersIOPChange, 1)
      }
    }
  }
}
