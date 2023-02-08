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

package views

import builders.PaymentsIntoPensionVewModelBuilder.aPaymentsIntoPensionViewModel
import config.{AppConfig, MockAppConfig}
import models.AuthorisationRequest
import models.pension.reliefs.PaymentsIntoPensionViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.pensions.paymentsIntoPensions.PaymentsIntoPensionsCYAView
import views.PaymentsIntoPensionsCYATestSupport._

import java.time.LocalDate

// scalastyle:off magic.number
object PaymentsIntoPensionsCYATestSupport {

  private val dateNow: LocalDate = LocalDate.now()
  private val taxYearCutoffDate: LocalDate = LocalDate.parse(s"${dateNow.getYear}-04-05")
  private val taxYear: Int = if (dateNow.isAfter(taxYearCutoffDate)) LocalDate.now().getYear + 1 else LocalDate.now().getYear

  val cyaDataMinimal: PaymentsIntoPensionViewModel = PaymentsIntoPensionViewModel(
    gateway = Some(true),
    rasPensionPaymentQuestion = Some(false),
    pensionTaxReliefNotClaimedQuestion = Some(false)
  )

  object ChangeLinks {
    val paymentsIntoUKPensions: String = controllers.pensions.paymentsIntoPensions.routes.PaymentsIntoPensionsStatusController.show(taxYear).url
    val reliefAtSource: String = controllers.pensions.paymentsIntoPensions.routes.ReliefAtSourcePensionsController.show(taxYear).url
    val reliefAtSourceAmount: String = controllers.pensions.paymentsIntoPensions.routes.ReliefAtSourcePaymentsAndTaxReliefAmountController.show(taxYear).url
    val oneOff: String = controllers.pensions.paymentsIntoPensions.routes.ReliefAtSourceOneOffPaymentsController.show(taxYear).url
    val oneOffAmount: String = controllers.pensions.paymentsIntoPensions.routes.OneOffRASPaymentsAmountController.show(taxYear).url
    val pensionsTaxReliefNotClaimed: String = controllers.pensions.paymentsIntoPensions.routes.PensionsTaxReliefNotClaimedController.show(taxYear).url
    val retirementAnnuity: String = controllers.pensions.paymentsIntoPensions.routes.RetirementAnnuityController.show(taxYear).url
    val retirementAnnuityAmount: String = controllers.pensions.paymentsIntoPensions.routes.RetirementAnnuityAmountController.show(taxYear).url
    val workplacePayments: String = controllers.pensions.paymentsIntoPensions.routes.WorkplacePensionController.show(taxYear).url
    val workplacePaymentsAmount: String = controllers.pensions.paymentsIntoPensions.routes.WorkplaceAmountController.show(taxYear).url
  }

  trait SpecificExpectedResults {
    val expectedH1: String
    val expectedTitle: String
  }

  trait CommonExpectedResults {
    def expectedCaption(taxYear: Int): String

    val yes: String
    val no: String

    val paymentsIntoUKPensions: String
    val reliefAtSource: String
    val reliefAtSourceAmount: String
    val oneOff: String
    val oneOffAmount: String
    val pensionsTaxReliefNotClaimed: String
    val retirementAnnuity: String
    val retirementAnnuityAmount: String
    val workplacePayments: String
    val workplacePaymentsAmount: String

    val saveAndContinue: String
    val error: String

    val paymentsIntoUKPensionsHidden: String
    val reliefAtSourceHidden: String
    val reliefAtSourceAmountHidden: String
    val oneOffHidden: String
    val oneOffAmountHidden: String
    val pensionsTaxReliefNotClaimedHidden: String
    val retirementAnnuityHidden: String
    val retirementAnnuityAmountHidden: String
    val workplacePaymentsHidden: String
    val workplacePaymentsAmountHidden: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    def expectedCaption(taxYear: Int): String = s"Payments into pensions for 6 April ${taxYear - 1} to 5 April $taxYear"

    val yes = "Yes"
    val no = "No"

    val paymentsIntoUKPensions = "Payments into UK pensions"
    val reliefAtSource = "Relief at source (RAS) pension payments"
    val reliefAtSourceAmount = "Total RAS payments plus tax relief"
    val oneOff = "One-off RAS payments"
    val oneOffAmount = "Total one-off RAS payments plus tax relief"
    val pensionsTaxReliefNotClaimed = "Pensions where tax relief is not claimed"
    val retirementAnnuity = "Retirement annuity contract payments"
    val retirementAnnuityAmount = "Total retirement annuity contract payments"
    val workplacePayments = "Workplace pension payments"
    val workplacePaymentsAmount = "Total workplace pension payments"

    val saveAndContinue = "Save and continue"
    val error = "Sorry, there is a problem with the service"

    val paymentsIntoUKPensionsHidden = "Change payments into UK pensions"
    val reliefAtSourceHidden = "Change whether relief at source pensions payments were made"
    val reliefAtSourceAmountHidden = "Change total relief at source pensions payments, plus tax relief"
    val oneOffHidden = "Change whether one-off relief at source pensions payments were made"
    val oneOffAmountHidden = "Change total one-off relief at source pensions payments, plus tax relief"
    val pensionsTaxReliefNotClaimedHidden = "Change whether payments were made into a pension where tax relief was not claimed"
    val retirementAnnuityHidden = "Change whether retirement annuity contract payments were made"
    val retirementAnnuityAmountHidden = "Change total retirement annuity contract payments"
    val workplacePaymentsHidden = "Change whether workplace pension payments were made"
    val workplacePaymentsAmountHidden = "Change total workplace pension payments"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    def expectedCaption(taxYear: Int): String = s"Payments into pensions for 6 April ${taxYear - 1} to 5 April $taxYear"

    val yes = "Yes"
    val no = "No"

    val paymentsIntoUKPensions = "Payments into UK pensions"
    val reliefAtSource = "Relief at source (RAS) pension payments"
    val reliefAtSourceAmount = "Total RAS payments plus tax relief"
    val oneOff = "One-off RAS payments"
    val oneOffAmount = "Total one-off RAS payments plus tax relief"
    val pensionsTaxReliefNotClaimed = "Pensions where tax relief is not claimed"
    val retirementAnnuity = "Retirement annuity contract payments"
    val retirementAnnuityAmount = "Total retirement annuity contract payments"
    val workplacePayments = "Workplace pension payments"
    val workplacePaymentsAmount = "Total workplace pension payments"

    val saveAndContinue = "Save and continue"
    val error = "Sorry, there is a problem with the service"

    val paymentsIntoUKPensionsHidden = "Change payments into UK pensions"
    val reliefAtSourceHidden = "Change whether relief at source pensions payments were made"
    val reliefAtSourceAmountHidden = "Change total relief at source pensions payments, plus tax relief"
    val oneOffHidden = "Change whether one-off relief at source pensions payments were made"
    val oneOffAmountHidden = "Change total one-off relief at source pensions payments, plus tax relief"
    val pensionsTaxReliefNotClaimedHidden = "Change whether payments were made into a pension where tax relief was not claimed"
    val retirementAnnuityHidden = "Change whether retirement annuity contract payments were made"
    val retirementAnnuityAmountHidden = "Change total retirement annuity contract payments"
    val workplacePaymentsHidden = "Change whether workplace pension payments were made"
    val workplacePaymentsAmountHidden = "Change total workplace pension payments"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedH1 = "Check your payments into pensions"
    val expectedTitle = "Check your payments into pensions"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedH1 = "Check your client’s payments into pensions"
    val expectedTitle = "Check your client’s payments into pensions"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedH1 = "Check your payments into pensions"
    val expectedTitle = "Check your payments into pensions"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedH1 = "Check your client’s payments into pensions"
    val expectedTitle = "Check your client’s payments into pensions"
  }
}

class PaymentsIntoPensionsCYATestSupport extends ViewUnitTest {
  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private lazy val underTest = inject[PaymentsIntoPensionsCYAView]

  userScenarios.foreach { userScenario =>

    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should {

      "render the page with a full CYA model" which {

        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYear, aPaymentsIntoPensionViewModel)

        import userScenario.commonExpectedResults._

        implicit val document: Document = Jsoup.parse(htmlFormat.body)


        titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
        h1Check(userScenario.specificExpectedResults.get.expectedH1)
        captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYear))

        //noinspection ScalaStyle
        cyaRowCheck(paymentsIntoUKPensions, yes, ChangeLinks.paymentsIntoUKPensions, paymentsIntoUKPensionsHidden, 1)
        cyaRowCheck(reliefAtSource, yes, ChangeLinks.reliefAtSource, reliefAtSourceHidden, 2)
        cyaRowCheck(reliefAtSourceAmount, s"${moneyContent(aPaymentsIntoPensionViewModel.totalRASPaymentsAndTaxRelief.get)}",
          ChangeLinks.reliefAtSourceAmount, reliefAtSourceAmountHidden, 3)
        cyaRowCheck(oneOff, yes, ChangeLinks.oneOff, oneOffHidden, 4)
        cyaRowCheck(oneOffAmount, s"${moneyContent(aPaymentsIntoPensionViewModel.totalOneOffRasPaymentPlusTaxRelief.get)}",
          ChangeLinks.oneOffAmount, oneOffAmountHidden, 5)
        cyaRowCheck(pensionsTaxReliefNotClaimed, yes, ChangeLinks.pensionsTaxReliefNotClaimed, pensionsTaxReliefNotClaimedHidden, 6)
        cyaRowCheck(retirementAnnuity, yes, ChangeLinks.retirementAnnuity, retirementAnnuityHidden, 7)
        cyaRowCheck(retirementAnnuityAmount, s"${moneyContent(aPaymentsIntoPensionViewModel.totalRetirementAnnuityContractPayments.get)}",
          ChangeLinks.retirementAnnuityAmount, retirementAnnuityAmountHidden, 8)
        cyaRowCheck(workplacePayments, yes, ChangeLinks.workplacePayments, workplacePaymentsHidden, 9)
        cyaRowCheck(workplacePaymentsAmount, s"${moneyContent(aPaymentsIntoPensionViewModel.totalWorkplacePensionPayments.get)}",
          ChangeLinks.workplacePaymentsAmount, workplacePaymentsAmountHidden, 10)

        buttonCheck(saveAndContinue)

        welshToggleCheck(userScenario.isWelsh)
      }
      "render the page with a minimal CYA view" which {

        implicit val authRequest: AuthorisationRequest[AnyContent] = getAuthRequest(userScenario.isAgent)
        implicit val messages: Messages = getMessages(userScenario.isWelsh)

        val htmlFormat = underTest(taxYear, cyaDataMinimal)

        import userScenario.commonExpectedResults._

        implicit val document: Document = Jsoup.parse(htmlFormat.body)


        import userScenario.commonExpectedResults.{no => answerNo}
        import userScenario.commonExpectedResults.{yes => anseryes}

        //noinspection ScalaStyle
        cyaRowCheck(paymentsIntoUKPensions, anseryes, ChangeLinks.paymentsIntoUKPensions, paymentsIntoUKPensionsHidden, 1)
        cyaRowCheck(reliefAtSource, answerNo, ChangeLinks.reliefAtSource, reliefAtSourceHidden, 2)
        cyaRowCheck(pensionsTaxReliefNotClaimed, answerNo, ChangeLinks.pensionsTaxReliefNotClaimed, pensionsTaxReliefNotClaimedHidden, 3)

        buttonCheck(saveAndContinue)

        welshToggleCheck(userScenario.isWelsh)
      }
    }
  }
}
// scalastyle:on magic.number
