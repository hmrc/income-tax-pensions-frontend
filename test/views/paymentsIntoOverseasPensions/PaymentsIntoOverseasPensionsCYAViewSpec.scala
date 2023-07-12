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

import builders.PaymentsIntoOverseasPensionsViewModelBuilder._
import controllers.pensions.paymentsIntoOverseasPensions.routes
import models.pension.charges.PaymentsIntoOverseasPensionsViewModel
import models.requests.UserSessionDataRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.pensions.paymentsIntoOverseasPensions.PaymentsIntoOverseasPensionsCYAView

class PaymentsIntoOverseasPensionsCYAViewSpec extends ViewUnitTest { //scalastyle:off magic.number

  object ChangeLinks {
    val changePiop = routes.PaymentIntoPensionSchemeController.show(taxYearEOY).url
    val changeTotalPayments = routes.PaymentIntoPensionSchemeController.show(taxYearEOY).url
    val changeEmployerPayments = routes.EmployerPayOverseasPensionController.show(taxYearEOY).url
    val changeEmployerTax = routes.TaxEmployerPaymentsController.show(taxYearEOY).url
    val changeScheme = routes.ReliefsSchemeSummaryController.show(taxYearEOY).url
  }

  trait CommonExpectedResults {
    val expectedTitle: String
    lazy val expectedHeading = expectedTitle
    val expectedCaption: Int => String
    val piopYesNo: String
    val totalPayments: String
    val employerPaymentsYesNo: String
    val employerTaxYesNo: String
    val schemes: String
    val hiddenPiopYesNo: String
    val hiddenTotalPayments: String
    val hiddenEmployerPaymentsYesNo: String
    val hiddenEmployerTaxYesNo: String
    val hiddenSchemes: String
    val buttonText: String
    val noText: String
    val yesText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedTitle: String = "Check payments into overseas pensions"
    val expectedCaption: Int => String = (taxYear: Int) => s"Payments into overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val piopYesNo: String = "Payments into overseas pensions"
    val totalPayments: String = "Total payments"
    val employerPaymentsYesNo: String = "Employer payments"
    val employerTaxYesNo: String = "Tax paid on employer payments"
    val schemes: String = "Overseas pension schemes"
    val hiddenPiopYesNo: String = "Change payments into overseas pensions"
    val hiddenTotalPayments: String = "Change total payments"
    val hiddenEmployerPaymentsYesNo: String = "Change employer payments"
    val hiddenEmployerTaxYesNo: String = "Change tax paid on employer payments"
    val hiddenSchemes: String = "Change overseas pension schemes"
    val buttonText: String = "Save and continue"
    val noText: String = "No"
    val yesText: String = "Yes"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedTitle: String = "Gwirio taliadau i bensiynau tramor"
    val expectedCaption: Int => String = (taxYear: Int) => s"Taliadau i bensiynau tramor ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val piopYesNo: String = "Taliadau i bensiynau tramor"
    val totalPayments: String = "Cyfanswm y taliadau"
    val employerPaymentsYesNo: String = "Taliadau cyflogwr"
    val employerTaxYesNo: String = "Treth sydd wedi’i thalu ar daliadau cyflogwr"
    val schemes: String = "Cynllun pensiwn tramor"
    val hiddenPiopYesNo: String = "Newid taliadau i bensiynau tramor"
    val hiddenTotalPayments: String = "Change total payments"
    val hiddenEmployerPaymentsYesNo: String = "Change employer payments"
    val hiddenEmployerTaxYesNo: String = "Change tax paid on employer payments"
    val hiddenSchemes: String = "Change overseas pension schemes"
    val buttonText: String = "Cadw ac yn eich blaen"
    val noText: String = "Na"
    val yesText: String = "Iawn"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, String]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, None),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, None),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, None),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, None)
  )

  lazy val underTest = inject[PaymentsIntoOverseasPensionsCYAView]

  private def renderPage(userScenario: UserScenario[CommonExpectedResults, String], model: PaymentsIntoOverseasPensionsViewModel) = {
    implicit val request: UserSessionDataRequest[AnyContent] = getUserSession(userScenario.isAgent)
    implicit val messages: Messages = getMessages(userScenario.isWelsh)

    val htmlFormat = underTest(taxYearEOY, model)
    import userScenario.commonExpectedResults._
    implicit val document: Document = Jsoup.parse(htmlFormat.body)

    titleCheck(expectedTitle, userScenario.isWelsh)
    h1Check(expectedHeading)
    captionCheck(expectedCaption(taxYearEOY))
    document
  }


  userScenarios.foreach { userScenario =>
    import userScenario.commonExpectedResults._

    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {

      "render the page with a full CYA model" when {
        implicit val document: Document = renderPage(userScenario, aPaymentsIntoOverseasPensionsViewModel)

        cyaRowCheck(piopYesNo, yesText, ChangeLinks.changePiop, hiddenPiopYesNo, 1)
        cyaRowCheck(totalPayments, "£1,999.99", ChangeLinks.changeTotalPayments, hiddenTotalPayments, 2)
        cyaRowCheck(employerPaymentsYesNo, yesText, ChangeLinks.changeEmployerPayments, hiddenEmployerPaymentsYesNo, 3)
        cyaRowCheck(employerTaxYesNo, noText, ChangeLinks.changeEmployerTax, hiddenEmployerTaxYesNo, 4)
        cyaRowCheck(schemes, "tcrPENSIONINCOME2000, mmrPENSIONINCOME356, dtrPENSIONINCOME550, noPENSIONINCOME100", ChangeLinks.changeScheme, hiddenSchemes, 5)
        cyaNoMoreRowsAfterCheck(5)
        buttonCheck(buttonText)
      }

      "render the page with 'Yes' to all gateway questions and no schemes" when {
        val piopModel = aPaymentsIntoOverseasPensionsViewModel.copy(
          taxPaidOnEmployerPaymentsQuestion = Some(true), reliefs = Seq.empty)
        implicit val document: Document = renderPage(userScenario, piopModel)

        cyaRowCheck(piopYesNo, yesText, ChangeLinks.changePiop, hiddenPiopYesNo, 1)
        cyaRowCheck(totalPayments, "£1,999.99", ChangeLinks.changeTotalPayments, hiddenPiopYesNo, 2)
        cyaRowCheck(employerPaymentsYesNo, yesText, ChangeLinks.changeEmployerPayments, hiddenEmployerPaymentsYesNo, 3)
        cyaRowCheck(employerTaxYesNo, yesText, ChangeLinks.changeEmployerTax, hiddenEmployerTaxYesNo, 4)
        cyaNoMoreRowsAfterCheck(4)
        buttonCheck(buttonText)
      }

      "render the page with 'No' Payments into Overseas pensions" when {
        val minPiopModel = aPaymentsIntoOverseasPensionsEmptyViewModel.copy(
          paymentsIntoOverseasPensionsQuestions = Some(false))
        import userScenario.commonExpectedResults._
        implicit val document: Document = renderPage(userScenario, minPiopModel)

        cyaRowCheck(piopYesNo, noText, ChangeLinks.changePiop, hiddenPiopYesNo, 1)
        cyaNoMoreRowsAfterCheck(1)
        buttonCheck(buttonText)
      }
    }
  }
}
