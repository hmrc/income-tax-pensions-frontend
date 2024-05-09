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

package views.pensions.annualAllowances

import builders.PensionAnnualAllowanceViewModelBuilder.aPensionAnnualAllowanceViewModel
import common.TaxYear
import models.requests.UserSessionDataRequest
import org.jsoup.Jsoup
import controllers.pensions.annualAllowances.routes
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.pensions.annualAllowances.AnnualAllowancesCYAView

class AnnualAllowanceCYAViewSpec extends ViewUnitTest {

  object ChangeLinks {
    val changeReducedAnnualAllowance: String       = routes.ReducedAnnualAllowanceController.show(taxYear).url
    val changeTypeOfReducedAnnualAllowance: String = routes.ReducedAnnualAllowanceTypeController.show(taxYear).url
    val changeAboveAnnualAllowance: String         = routes.AboveReducedAnnualAllowanceController.show(taxYear).url
    val changeAmountAboveAnnualAllowance: String   = routes.AboveReducedAnnualAllowanceController.show(taxYear).url
    val changeAnnualAllowanceTax: String           = routes.PensionProviderPaidTaxController.show(taxYear).url
    val changeAnnualAllowanceSchemes: String       = routes.PstrSummaryController.show(taxYear).url
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val reducedAnnualAllowance: String
    val typeOfReducedAnnualAllowance: String
    val aboveAnnualAllowance: String
    val amountAboveAnnualAllowance: String
    val annualAllowanceTax: String
    val annualAllowanceSchemes: String
    val aboveAnnualAllowanceHidden: String
    val reducedAnnualAllowanceHidden: String
    val typeOfReducedAnnualAllowanceHidden: String
    val annualAllowanceTaxHidden: String
    val annualAllowanceSchemesHidden: String
    val yesText: String
    val noText: String
    val saveAndContinue: String
    val error: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String             = (taxYear: Int) => s"Annual allowances for 6 April ${taxYear - 1} to 5 April $taxYear"
    val reducedAnnualAllowance: String             = "Reduced annual allowance"
    val typeOfReducedAnnualAllowance: String       = "Type of reduced annual allowance"
    val aboveAnnualAllowance: String               = "Above annual allowance"
    val amountAboveAnnualAllowance: String         = "Amount above annual allowance"
    val annualAllowanceTax: String                 = "Annual allowance tax"
    val annualAllowanceSchemes: String             = "Schemes paying annual allowance tax"
    val aboveAnnualAllowanceHidden: String         = "Change above annual allowance"
    val reducedAnnualAllowanceHidden: String       = "Change reduced annual allowance"
    val typeOfReducedAnnualAllowanceHidden: String = "Change type of reduced annual allowance"
    val annualAllowanceTaxHidden: String           = "Change annual allowance tax"
    val annualAllowanceSchemesHidden: String       = "Change schemes paying annual allowance tax"
    val yesText                                    = "Yes"
    val noText                                     = "No"
    val saveAndContinue                            = "Save and continue"
    val error                                      = "Sorry, there is a problem with the service"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String             = (taxYear: Int) => s"Lwfans blynyddol ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val reducedAnnualAllowance: String             = "Lwfans blynyddol wedi’i ostwng"
    val typeOfReducedAnnualAllowance: String       = "Math o lwfans blynyddol wedi’i ostwng"
    val aboveAnnualAllowance: String               = "Uwch na’r lwfans blynyddol"
    val amountAboveAnnualAllowance: String         = "Swm uwch na’r lwfans blynyddol"
    val annualAllowanceTax: String                 = "Treth lwfans blynyddol"
    val annualAllowanceSchemes: String             = "Cynlluniau sy’n talu treth lwfans blynyddol"
    val aboveAnnualAllowanceHidden: String         = "Newidiwch uwch na’r lwfans blynyddol"
    val reducedAnnualAllowanceHidden: String       = "Newidiwch lwfans blynyddol wedi’i ostwng"
    val typeOfReducedAnnualAllowanceHidden: String = "Newidiwch fath o lwfans blynyddol wedi’i ostwng"
    val annualAllowanceTaxHidden: String           = "Newidiwch dreth lwfans blynyddol"
    val annualAllowanceSchemesHidden: String       = "Newidiwch gynlluniau sy’n talu treth lwfans blynyddol"
    val yesText                                    = "Iawn"
    val noText                                     = "Na"
    val saveAndContinue                            = "Cadw ac yn eich blaen"
    val error                                      = "Sorry, there is a problem with the service"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    lazy val expectedH1 = expectedTitle
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Check your annual allowance"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Check your client’s annual allowance"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Gwiriwch eich lwfansau blynyddol"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Gwiriwch lwfansau blynyddol eich cleient"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private lazy val underTest = inject[AnnualAllowancesCYAView]

  userScenarios.foreach { userScenario =>
    val booleanToYesOrNo: Option[Boolean] => String =
      bool => if (bool.getOrElse(false)) userScenario.commonExpectedResults.yesText else userScenario.commonExpectedResults.noText

    def checkCommonElements(userScenario: UserScenario[CommonExpectedResults, SpecificExpectedResults])(implicit document: Document): Unit = {
      titleCheck(userScenario.specificExpectedResults.get.expectedTitle, userScenario.isWelsh)
      h1Check(userScenario.specificExpectedResults.get.expectedH1)
      captionCheck(userScenario.commonExpectedResults.expectedCaption(taxYearEOY))
    }

    s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" should { // scalastyle:off magic.number

      "render the page with a full CYA model" which {
        import userScenario.commonExpectedResults._
        implicit val request: UserSessionDataRequest[AnyContent] = getUserSession(userScenario.isAgent)
        implicit val messages: Messages                          = getMessages(userScenario.isWelsh)

        val viewModel                   = aPensionAnnualAllowanceViewModel
        val htmlFormat                  = underTest(TaxYear(taxYearEOY), viewModel)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        checkCommonElements(userScenario)

        cyaRowCheck(
          reducedAnnualAllowance,
          booleanToYesOrNo(viewModel.reducedAnnualAllowanceQuestion),
          ChangeLinks.changeReducedAnnualAllowance,
          reducedAnnualAllowanceHidden,
          1
        )

        cyaRowCheck(
          typeOfReducedAnnualAllowance,
          viewModel.typeOfAllowance.getOrElse(Nil).mkString(", "),
          ChangeLinks.changeTypeOfReducedAnnualAllowance,
          typeOfReducedAnnualAllowanceHidden,
          2
        )

        cyaRowCheck(
          aboveAnnualAllowance,
          booleanToYesOrNo(viewModel.aboveAnnualAllowanceQuestion),
          ChangeLinks.changeAboveAnnualAllowance,
          aboveAnnualAllowanceHidden,
          3)

        cyaRowCheck(
          amountAboveAnnualAllowance,
          moneyContent(viewModel.aboveAnnualAllowance.getOrElse(0)),
          ChangeLinks.changeAboveAnnualAllowance,
          aboveAnnualAllowanceHidden,
          4
        )

        cyaRowCheck(
          annualAllowanceTax,
          moneyContent(viewModel.taxPaidByPensionProvider.getOrElse(0)),
          ChangeLinks.changeAnnualAllowanceTax,
          annualAllowanceTaxHidden,
          5)

        cyaRowCheck(
          annualAllowanceSchemes,
          s"${viewModel.pensionSchemeTaxReferences
              .getOrElse(Nil)
              .mkString(", ")}",
          ChangeLinks.changeAnnualAllowanceSchemes,
          annualAllowanceSchemesHidden,
          6
        )

        buttonCheck(saveAndContinue)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render the page with above annual allowance set to false" which {
        import userScenario.commonExpectedResults._
        implicit val request: UserSessionDataRequest[AnyContent] = getUserSession(userScenario.isAgent)
        implicit val messages: Messages                          = getMessages(userScenario.isWelsh)

        val viewModel                   = aPensionAnnualAllowanceViewModel.copy(aboveAnnualAllowanceQuestion = Some(false))
        val htmlFormat                  = underTest(TaxYear(taxYearEOY), viewModel)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        checkCommonElements(userScenario)

        cyaRowCheck(
          reducedAnnualAllowance,
          booleanToYesOrNo(viewModel.reducedAnnualAllowanceQuestion),
          ChangeLinks.changeReducedAnnualAllowance,
          reducedAnnualAllowanceHidden,
          1
        )

        cyaRowCheck(
          typeOfReducedAnnualAllowance,
          viewModel.typeOfAllowance.getOrElse(Nil).mkString(", "),
          ChangeLinks.changeTypeOfReducedAnnualAllowance,
          typeOfReducedAnnualAllowanceHidden,
          2
        )

        cyaRowCheck(
          aboveAnnualAllowance,
          booleanToYesOrNo(viewModel.aboveAnnualAllowanceQuestion),
          ChangeLinks.changeAboveAnnualAllowance,
          aboveAnnualAllowanceHidden,
          3)

        cyaNoMoreRowsAfterCheck(3)

        buttonCheck(saveAndContinue)
        welshToggleCheck(userScenario.isWelsh)
      }

      "render the page with reduced annual allowance set to false" which {
        import userScenario.commonExpectedResults._
        implicit val request: UserSessionDataRequest[AnyContent] = getUserSession(userScenario.isAgent)
        implicit val messages: Messages                          = getMessages(userScenario.isWelsh)

        val viewModel                   = aPensionAnnualAllowanceViewModel.copy(reducedAnnualAllowanceQuestion = Some(false))
        val htmlFormat                  = underTest(TaxYear(taxYearEOY), viewModel)
        implicit val document: Document = Jsoup.parse(htmlFormat.body)

        checkCommonElements(userScenario)

        cyaRowCheck(
          reducedAnnualAllowance,
          booleanToYesOrNo(viewModel.reducedAnnualAllowanceQuestion),
          ChangeLinks.changeReducedAnnualAllowance,
          reducedAnnualAllowanceHidden,
          1
        )

        cyaNoMoreRowsAfterCheck(1)

        buttonCheck(saveAndContinue)
        welshToggleCheck(userScenario.isWelsh)
      }
    }
  }
}
