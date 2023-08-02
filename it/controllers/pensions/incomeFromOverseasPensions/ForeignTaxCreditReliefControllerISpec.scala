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

package controllers.pensions.incomeFromOverseasPensions

import builders.IncomeFromOverseasPensionsViewModelBuilder.anIncomeFromOverseasPensionsViewModel
import builders.PensionSchemeBuilder.{aPensionScheme1, aPensionScheme2}
import builders.PensionsCYAModelBuilder.aPensionsCYAModel
import controllers.ControllerSpec.PreferredLanguages.{English, Welsh}
import controllers.ControllerSpec.UserTypes.{Agent, Individual}
import controllers.ControllerSpec._
import controllers.YesNoControllerSpec
import models.pension.charges.{IncomeFromOverseasPensionsViewModel, PensionScheme}
import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.libs.ws.{WSClient, WSResponse}

class ForeignTaxCreditReliefControllerISpec extends YesNoControllerSpec("/overseas-pensions/income-from-overseas-pensions/pension-overseas-income-ftcr") {
  val selectorForSummaryText = "#main-content > div > div > details > summary"
  val selectorForSummaryPara1 = "#main-content > div > div > details > div > p:nth-child(1)"
  val selectorForSummaryPara2 = "#main-content > div > div > details > div > p:nth-child(2)"
  val selectorForSummaryPara3 = "#main-content > div > div > details > div > p:nth-child(3)"
  val selectorForSummaryPara4 = "#main-content > div > div > details > div > p:nth-child(4)"
  val selectorForSummaryPara4Bullet1 = "#main-content > div > div > details > div > #para4bullets > li:nth-child(1)"
  val selectorForSummaryPara4Bullet2 = "#main-content > div > div > details > div > #para4bullets > li:nth-child(2)"
  val selectorForSummaryPara5 = "#main-content > div > div > details > div > #para5"
  val selectorForSummaryPara5Bullet1 = "#main-content > div > div > details > div > #para5bullets > li:nth-child(1)"
  val selectorForSummaryPara5Bullet2 = "#main-content > div > div > details > div > #para5bullets > li:nth-child(2)"
  val selectorForSummaryPara5Bullet3 = "#main-content > div > div > details > div > #para5bullets > li:nth-child(3)"
  val selectorForSummaryPara6 = "#main-content > div > div > details > div > #para6"
  val selectorForSummaryPara7 = "#main-content > div > div > details > div > #para7"

  "show" should {
    "redirect to the summary page when the user has no stored session data at all" in {
      implicit val userConfig: UserConfig = userConfigWhenIrrelevant(None)
      implicit val response: WSResponse = getPageWithIndex()

      assertRedirectionAsExpected(PageRelativeURLs.pensionsSummaryPage)
    }

    "redirect to the the first page of the journey when current page is invalid and user has no previous schemes" in {
      val emptySchemesIFOPViewModel: IncomeFromOverseasPensionsViewModel = aPensionsCYAModel.incomeFromOverseasPensions.copy(overseasIncomePensionSchemes = Seq.empty)
      val cyaModel = aPensionsCYAModel.copy(incomeFromOverseasPensions = emptySchemesIFOPViewModel)
      val sessionData = pensionsUserData(cyaModel)
      implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
      implicit val response: WSResponse = getPageWithIndex()

      assertRedirectionAsExpected(PageRelativeURLs.incomeFromOverseasPensionsStatus)
    }

    "redirect to the the IFOP scheme summary page" when {
      "an incorrect index is provided" in {
        val sessionData = pensionsUserData(aPensionsCYAModel)
        implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
        implicit val response: WSResponse = getPageWithIndex(7)

        assertRedirectionAsExpected(PageRelativeURLs.incomeFromOverseasPensionsCountrySummary)
      }
      "the user provides a negative index number" in {
        val sessionData = pensionsUserData(aPensionsCYAModel)
        implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
        implicit val response: WSResponse = getPageWithIndex(-1)

        assertRedirectionAsExpected(PageRelativeURLs.incomeFromOverseasPensionsCountrySummary)
      }
    }

    "appear as expected" when {

      val expectedYesNoPageContentsIndividual = ExpectedYesNoPageContents(
        title = "Are you claiming Foreign Tax Credit Relief (FTCR)?",
        header = "Are you claiming Foreign Tax Credit Relief (FTCR)?",
        caption = s"Income from overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear",
        radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
        radioButtonForNo = uncheckedExpectedRadioButton("No"),
        buttonForContinue = ExpectedButton("Continue", ""),
        links = Set(
          ExpectedLink(
            "tax-foreign-income-taxed-twice-link",
            "Foreign Tax Credit Relief at GOV.uk (opens in new tab)",
            "https://www.gov.uk/tax-foreign-income/taxed-twice"),
        ),
        text = Set(
          ExpectedText(selectorForSummaryText, "Understanding Foreign Tax Credit Relief (FTCR)"),
          ExpectedText(selectorForSummaryPara1, "You can claim Foreign Tax Credit Relief if you’ve already paid foreign tax on income that’s normally taxed in the UK."),
          ExpectedText(selectorForSummaryPara2, "You may also be taxed on this income by the UK."),
          ExpectedText(selectorForSummaryPara3, "However, you may not have to pay twice if the country you live in has a ‘double-taxation agreement’ with the UK."),
          ExpectedText(selectorForSummaryPara4, "Depending on the agreement, you can apply for either:"),
          ExpectedText(selectorForSummaryPara4Bullet1, "partial or full relief before you’ve been taxed"),
          ExpectedText(selectorForSummaryPara4Bullet2, "a refund after you’ve been taxed"),
          ExpectedText(selectorForSummaryPara5, "Each double-taxation agreement sets out:"),
          ExpectedText(selectorForSummaryPara5Bullet1, "the country you pay tax in"),
          ExpectedText(selectorForSummaryPara5Bullet2, "the country you apply for relief in"),
          ExpectedText(selectorForSummaryPara5Bullet3, "how much tax relief you get"),
          ExpectedText(selectorForSummaryPara6, "If the tax rates in the two countries are different, you’ll pay the higher rate of tax."),
          ExpectedText(selectorForSummaryPara7, "You can read more about Foreign Tax Credit Relief at GOV.uk (opens in new tab).")
        ),
        formUrl = formUrl()
      )
      val expectedYesNoPageContentsAgent = expectedYesNoPageContentsIndividual
        .copy(title = "Is your client claiming Foreign Tax Credit Relief (FTCR)?",
          header = "Is your client claiming Foreign Tax Credit Relief (FTCR)?")

      "the user has relevant session data with neither option selected" when {
        val incomeViewModel = anIncomeFromOverseasPensionsViewModel.copy(overseasIncomePensionSchemes = Seq(
          anIncomeFromOverseasPensionsViewModel.overseasIncomePensionSchemes.head
            .copy(foreignTaxCreditReliefQuestion = None)))

        val updatedUserData = aPensionsCYAModel.copy(incomeFromOverseasPensions = incomeViewModel)

        val sessionData = pensionsUserData(updatedUserData)


        scenarioNameForIndividualAndEnglish in {
          implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
          implicit val response: WSResponse = getPageWithIndex()

          assertPageAsExpected(OK, expectedYesNoPageContentsIndividual)
        }

        scenarioNameForIndividualAndWelsh ignore {
          implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
          implicit val response: WSResponse = getPageWithIndex()

          assertPageAsExpected(OK, expectedYesNoPageContentsIndividual)
        }

        scenarioNameForAgentAndEnglish in {
          implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
          implicit val response: WSResponse = getPageWithIndex()

          assertPageAsExpected(OK, expectedYesNoPageContentsAgent)
        }

        scenarioNameForAgentAndWelsh ignore {
          implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
          implicit val response: WSResponse = getPageWithIndex()

          assertPageAsExpected(OK, expectedYesNoPageContentsAgent)
        }
      }
      "the user has relevant session data with 'yes' option selected" when {
        val incomeViewModel = anIncomeFromOverseasPensionsViewModel.copy(overseasIncomePensionSchemes = Seq(
          anIncomeFromOverseasPensionsViewModel.overseasIncomePensionSchemes.head
            .copy(foreignTaxCreditReliefQuestion = Some(true))))

        val updatedUserData = aPensionsCYAModel.copy(incomeFromOverseasPensions = incomeViewModel)

        val sessionData = pensionsUserData(updatedUserData)

        scenarioNameForIndividualAndEnglish in {
          implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
          implicit val response: WSResponse = getPageWithIndex()

          assertPageAsExpected(OK, expectedYesNoPageContentsIndividual
            .copy(radioButtonForYes = checkedExpectedRadioButton("Yes")))
        }

        scenarioNameForIndividualAndWelsh ignore {
          implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
          implicit val response: WSResponse = getPageWithIndex()

          assertPageAsExpected(OK, expectedYesNoPageContentsIndividual
            .copy(radioButtonForYes = checkedExpectedRadioButton("Yes")))
        }

        scenarioNameForAgentAndEnglish in {
          implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
          implicit val response: WSResponse = getPageWithIndex()

          assertPageAsExpected(OK, expectedYesNoPageContentsAgent
            .copy(radioButtonForYes = checkedExpectedRadioButton("Yes")))
        }

        scenarioNameForAgentAndWelsh ignore {
          implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
          implicit val response: WSResponse = getPageWithIndex()

          assertPageAsExpected(OK, expectedYesNoPageContentsAgent
            .copy(radioButtonForYes = checkedExpectedRadioButton("Yes")))
        }
      }

      "the user has relevant session data with 'no' option selected" when {
        val incomeViewModel = anIncomeFromOverseasPensionsViewModel.copy(overseasIncomePensionSchemes = Seq(
          anIncomeFromOverseasPensionsViewModel.overseasIncomePensionSchemes.head
            .copy(foreignTaxCreditReliefQuestion = Some(false))))

        val updatedUserData = aPensionsCYAModel.copy(incomeFromOverseasPensions = incomeViewModel)

        val sessionData = pensionsUserData(updatedUserData)

        scenarioNameForIndividualAndEnglish in {
          implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
          implicit val response: WSResponse = getPageWithIndex()

          assertPageAsExpected(OK, expectedYesNoPageContentsIndividual
            .copy(radioButtonForNo = checkedExpectedRadioButton("No")))
        }

        scenarioNameForIndividualAndWelsh ignore {
          implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
          implicit val response: WSResponse = getPageWithIndex()

          assertPageAsExpected(OK, expectedYesNoPageContentsIndividual
            .copy(radioButtonForNo = checkedExpectedRadioButton("No")))
        }

        scenarioNameForAgentAndEnglish in {
          implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
          implicit val response: WSResponse = getPageWithIndex()

          assertPageAsExpected(OK, expectedYesNoPageContentsAgent
            .copy(radioButtonForNo = checkedExpectedRadioButton("No")))
        }

        scenarioNameForAgentAndWelsh ignore {
          implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
          implicit val response: WSResponse = getPageWithIndex()

          assertPageAsExpected(OK, expectedYesNoPageContentsAgent
            .copy(radioButtonForNo = checkedExpectedRadioButton("No")))
        }
      }
    }
  }

  "submit" should {

    "redirect to the Overseas Summary page when there is no session data" in {
      implicit val userConfig: UserConfig = userConfigWhenIrrelevant(None)
      implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoPage(None))

      assertRedirectionAsExpected(PageRelativeURLs.overseasSummaryPage)
      getViewModel mustBe None
    }

    "succeed" when {
      "the user has relevant session data and" when {
        val sessionData = getSessionData

        "selected 'Yes', redirecting to the Taxable Amount page" in {
          val ifopViewModel: IncomeFromOverseasPensionsViewModel = anIncomeFromOverseasPensionsViewModel.copy(
            overseasIncomePensionSchemes = Seq(aPensionScheme1, aPensionScheme2.copy(
              foreignTaxCreditReliefQuestion = None, taxableAmount = None)))
          implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData.copy(pensions = sessionData.pensions.copy(
            incomeFromOverseasPensions = ifopViewModel))))
          implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoPage(Some(true)), 1)

          assertRedirectionAsExpected(PageRelativeURLs.incomeFromOverseasPensionstaxable + "?index=1")
          getViewModel mustBe Some(ifopViewModel.copy(overseasIncomePensionSchemes = Seq(
            aPensionScheme1, aPensionScheme2.copy(taxableAmount = None))))
        }

        "selected 'Yes', redirecting to the scheme summary page when scheme is now complete" in {
          implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
          implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoPage(Some(true)))

          val incomeViewModel = anIncomeFromOverseasPensionsViewModel.copy(overseasIncomePensionSchemes = Seq(
            anIncomeFromOverseasPensionsViewModel.overseasIncomePensionSchemes.head
              .copy(foreignTaxCreditReliefQuestion = Some(true))))

          assertRedirectionAsExpected(PageRelativeURLs.incomeFromOverseasPensionsScheme + "?index=0")
          getViewModel mustBe Some(incomeViewModel)
        }
      }

      "the user has relevant session data and selected 'No'" in {
        val sessionData = getSessionData
        implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
        implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoPage(Some(false)))

        val incomeViewModel = anIncomeFromOverseasPensionsViewModel.copy(overseasIncomePensionSchemes = Seq(
          anIncomeFromOverseasPensionsViewModel.overseasIncomePensionSchemes.head
            .copy(foreignTaxCreditReliefQuestion = Some(false))))

        assertRedirectionAsExpected(PageRelativeURLs.incomeFromOverseasPensionsScheme + "?index=0")
        getViewModel mustBe Some(incomeViewModel)
      }
    }

    "fail" when {
      "the user has selected neither 'Yes' nor 'No' and" when {
        val sessionData = getSessionData

        val expectedYesNoPageContentsIndividual = ExpectedYesNoPageContents(
          title = "Error: Are you claiming Foreign Tax Credit Relief (FTCR)?",
          header = "Are you claiming Foreign Tax Credit Relief (FTCR)?",
          caption = s"Income from overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear",
          buttonForContinue = ExpectedButton("Continue", ""),
          radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
          radioButtonForNo = uncheckedExpectedRadioButton("No"),
          errorSummarySectionOpt = Some(
            ErrorSummarySection(
              title = "There is a problem",
              body = "Enter yes or no if you are claiming Foreign Tax Credit Relief (FTCR)",
              link = "#value")
          ),
          errorAboveElementCheckSectionOpt = Some(
            ErrorAboveElementCheckSection(
              title = "Error: Enter yes or no if you are claiming Foreign Tax Credit Relief (FTCR)",
              idOpt = Some("value")
            )
          ),
          formUrl = formUrl()
        )

        val expectedYesNoPageContentsAgent = expectedYesNoPageContentsIndividual.copy(
          title = "Error: Is your client claiming Foreign Tax Credit Relief (FTCR)?",
          header = "Is your client claiming Foreign Tax Credit Relief (FTCR)?",
          errorSummarySectionOpt = Some(ErrorSummarySection(
            title = "There is a problem", body = "Enter yes or no if your client is claiming Foreign Tax Credit Relief (FTCR)", link = "#value")),
          errorAboveElementCheckSectionOpt = Some(ErrorAboveElementCheckSection(
            title = "Error: Enter yes or no if your client is claiming Foreign Tax Credit Relief (FTCR)", idOpt = Some("value"))),
        )

        def welshTitle(epc: ExpectedYesNoPageContents): Option[ErrorSummarySection] =
          epc.errorSummarySectionOpt.map(ess => ess.copy(title = "Mae problem wedi codi"))

        scenarioNameForIndividualAndEnglish in {

          implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
          implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoPage(None))

          assertPageAsExpected(BAD_REQUEST, expectedYesNoPageContentsIndividual)
        }

        scenarioNameForIndividualAndWelsh ignore {

          implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
          implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoPage(None))

          assertPageAsExpected(BAD_REQUEST,
            expectedYesNoPageContentsIndividual.copy(errorSummarySectionOpt = welshTitle(expectedYesNoPageContentsIndividual)))
        }

        scenarioNameForAgentAndEnglish in {

          implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
          implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoPage(None))

          response must haveStatus(BAD_REQUEST)
          assertPageAsExpected(BAD_REQUEST, expectedYesNoPageContentsAgent)
        }

        scenarioNameForAgentAndWelsh ignore {

          implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
          implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoPage(None))

          response must haveStatus(BAD_REQUEST)
          assertPageAsExpected(BAD_REQUEST, expectedYesNoPageContentsAgent.copy(errorSummarySectionOpt = welshTitle(expectedYesNoPageContentsAgent)))
        }
      }
    }
  }


  private def getSessionData = {
    pensionsUserData(aPensionsCYAModel.copy(incomeFromOverseasPensions = anIncomeFromOverseasPensionsViewModel.copy(
      paymentsFromOverseasPensionsQuestion = Some(true),
      overseasIncomePensionSchemes = Seq(
        PensionScheme(
          alphaTwoCode = Some("FR"),
          alphaThreeCode = None,
          pensionPaymentAmount = Some(1999.99),
          pensionPaymentTaxPaid = Some(1999.99),
          specialWithholdingTaxQuestion = Some(true),
          specialWithholdingTaxAmount = Some(1999.99),
          foreignTaxCreditReliefQuestion = None,
          taxableAmount = Some(1999.99)
        )
      )
    )))
  }

  private def getViewModel(implicit userConfig: UserConfig): Option[IncomeFromOverseasPensionsViewModel] =
    loadPensionUserData.map(_.pensions.incomeFromOverseasPensions)


  private def formUrl(index: Int = 0): Option[String] =
    Some(relativeUrlForThisPage + "?index=" + index)

  private def submitFormWithIndex(submittedFormData: SubmittedFormData, index: Int = 0)(implicit userConfig: UserConfig, wsClient: WSClient): WSResponse = {
    submitForm(submittedFormData, getMap(index))
  }

  private def getMap(index: Int): Map[String, String] = {
    Map("index" -> index.toString)
  }
}