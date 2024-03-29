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
import controllers.{SubmittedFormDataForOptionTupleAmountPage, TwoAmountsControllerISpec}
import models.pension.charges.{IncomeFromOverseasPensionsViewModel, PensionScheme}
import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.libs.ws.{WSClient, WSResponse}

class PensionPaymentsControllerISpec
    extends TwoAmountsControllerISpec("/overseas-pensions/income-from-overseas-pensions/pension-overseas-income-amounts") {

  "show" should {
    "redirect to the summary page the user has no stored session data" in {
      implicit val userConfig: UserConfig = userConfigWhenIrrelevant(None)
      implicit val response: WSResponse   = getPageWithIndex()

      assertRedirectionAsExpected(PageRelativeURLs.overseasSummaryPage)
    }

    "redirect to the the first page of the journey when the user has no relevant scheme data" in {
      val emptySchemesIFOPViewModel: IncomeFromOverseasPensionsViewModel =
        aPensionsCYAModel.incomeFromOverseasPensions.copy(overseasIncomePensionSchemes = Seq.empty)
      val cyaModel                        = aPensionsCYAModel.copy(incomeFromOverseasPensions = emptySchemesIFOPViewModel)
      val sessionData                     = pensionsUserData(cyaModel)
      implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
      implicit val response: WSResponse   = getPageWithIndex()

      assertRedirectionAsExpected(PageRelativeURLs.incomeFromOverseasPensionsStatus)
    }

    "redirect to the the IFOP scheme summary page" when {
      "an incorrect index provided" in {
        val sessionData                     = pensionsUserData(aPensionsCYAModel)
        implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
        implicit val response: WSResponse   = getPageWithIndex(7)

        assertRedirectionAsExpected(PageRelativeURLs.incomeFromOverseasPensionsCountrySummary)
      }
      "the user provides a negative index number" in {
        val sessionData                     = pensionsUserData(aPensionsCYAModel)
        implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
        implicit val response: WSResponse   = getPageWithIndex(-1)

        assertRedirectionAsExpected(PageRelativeURLs.incomeFromOverseasPensionsCountrySummary)
      }
    }

    "appear as expected" when {

      val expectedPageContents = ExpectedOptionTupleAmountPageContents(
        title = "Pension payments",
        header = "Pension payments",
        caption = s"Income from overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear",
        buttonForContinue = ExpectedButton("Continue", ""),
        amountSection1 = ExpectedAmountSection("Amount before tax", "", Some("For example, £193.54")),
        amountSection2 = ExpectedAmountSection("Non-UK tax paid", "", Some("For example, £193.54")),
        errorSummarySectionOpt = None,
        errorAboveElementCheckSectionOpt = None,
        formUrl = formUrl()
      )

      "the user has relevant session data with empty payments in pension scheme" when {
        val incomeViewModel = anIncomeFromOverseasPensionsViewModel.copy(overseasIncomePensionSchemes = Seq(
          anIncomeFromOverseasPensionsViewModel.overseasIncomePensionSchemes.head
            .copy(pensionPaymentAmount = None, pensionPaymentTaxPaid = None)))

        val updatedUserData = aPensionsCYAModel.copy(incomeFromOverseasPensions = incomeViewModel)

        val sessionData = pensionsUserData(updatedUserData)

        scenarioNameForIndividualAndEnglish in {
          implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
          implicit val response: WSResponse   = getPageWithIndex()

          assertPageAsExpected(OK, expectedPageContents)
        }

        scenarioNameForIndividualAndWelsh ignore {
          implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
          implicit val response: WSResponse   = getPageWithIndex()

          assertPageAsExpected(OK, expectedPageContents)
        }

        scenarioNameForAgentAndEnglish in {
          implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
          implicit val response: WSResponse   = getPageWithIndex()

          assertPageAsExpected(OK, expectedPageContents)
        }

        scenarioNameForAgentAndWelsh ignore {
          implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
          implicit val response: WSResponse   = getPageWithIndex()

          assertPageAsExpected(OK, expectedPageContents)
        }
      }
      "the user has relevant session data with both payments in pension scheme" when {
        val incomeViewModel = anIncomeFromOverseasPensionsViewModel.copy(overseasIncomePensionSchemes = Seq(
          anIncomeFromOverseasPensionsViewModel.overseasIncomePensionSchemes.head
            .copy(pensionPaymentAmount = Some(BigDecimal(1992.36)), pensionPaymentTaxPaid = Some(BigDecimal(1121.31)))))

        val updatedUserData = aPensionsCYAModel.copy(incomeFromOverseasPensions = incomeViewModel)

        val sessionData = pensionsUserData(updatedUserData)

        val expPageContents = expectedPageContents.copy(
          amountSection1 = ExpectedAmountSection("Amount before tax", "1,992.36", Some("For example, £193.54")),
          amountSection2 = ExpectedAmountSection("Non-UK tax paid", "1,121.31", Some("For example, £193.54"))
        )

        scenarioNameForIndividualAndEnglish in {
          implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
          implicit val response: WSResponse   = getPageWithIndex()

          assertPageAsExpected(OK, expPageContents)
        }

        scenarioNameForIndividualAndWelsh ignore {
          implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
          implicit val response: WSResponse   = getPageWithIndex()

          assertPageAsExpected(OK, expPageContents)
        }

        scenarioNameForAgentAndEnglish in {
          implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
          implicit val response: WSResponse   = getPageWithIndex()

          assertPageAsExpected(OK, expPageContents)
        }

        scenarioNameForAgentAndWelsh ignore {
          implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
          implicit val response: WSResponse   = getPageWithIndex()

          assertPageAsExpected(OK, expPageContents)
        }
      }
      "the user has relevant session data with only Amount before tax in pension scheme" when {
        val incomeViewModel = anIncomeFromOverseasPensionsViewModel.copy(overseasIncomePensionSchemes = Seq(
          anIncomeFromOverseasPensionsViewModel.overseasIncomePensionSchemes.head
            .copy(pensionPaymentAmount = Some(BigDecimal(1992.36)), pensionPaymentTaxPaid = None)))

        val updatedUserData = aPensionsCYAModel.copy(incomeFromOverseasPensions = incomeViewModel)

        val sessionData = pensionsUserData(updatedUserData)

        val expPageContents = expectedPageContents.copy(
          amountSection1 = ExpectedAmountSection("Amount before tax", "1,992.36", Some("For example, £193.54")),
          amountSection2 = ExpectedAmountSection("Non-UK tax paid", "", Some("For example, £193.54"))
        )

        scenarioNameForIndividualAndEnglish in {
          implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
          implicit val response: WSResponse   = getPageWithIndex()

          assertPageAsExpected(OK, expPageContents)
        }

        scenarioNameForIndividualAndWelsh ignore {
          implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
          implicit val response: WSResponse   = getPageWithIndex()

          assertPageAsExpected(OK, expPageContents)
        }

        scenarioNameForAgentAndEnglish in {
          implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
          implicit val response: WSResponse   = getPageWithIndex()

          assertPageAsExpected(OK, expPageContents)
        }

        scenarioNameForAgentAndWelsh ignore {
          implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
          implicit val response: WSResponse   = getPageWithIndex()

          assertPageAsExpected(OK, expPageContents)
        }
      }
      "the user has relevant session data with only Non-Uk tax paid in pension scheme" when {
        val incomeViewModel = anIncomeFromOverseasPensionsViewModel.copy(overseasIncomePensionSchemes = Seq(
          anIncomeFromOverseasPensionsViewModel.overseasIncomePensionSchemes.head
            .copy(pensionPaymentAmount = None, pensionPaymentTaxPaid = Some(BigDecimal(1312.21)))))

        val updatedUserData = aPensionsCYAModel.copy(incomeFromOverseasPensions = incomeViewModel)

        val sessionData = pensionsUserData(updatedUserData)

        val expPageContents = expectedPageContents.copy(
          amountSection1 = ExpectedAmountSection("Amount before tax", "", Some("For example, £193.54")),
          amountSection2 = ExpectedAmountSection("Non-UK tax paid", "1,312.21", Some("For example, £193.54"))
        )

        scenarioNameForIndividualAndEnglish in {
          implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
          implicit val response: WSResponse   = getPageWithIndex()

          assertPageAsExpected(OK, expPageContents)
        }

        scenarioNameForIndividualAndWelsh ignore {
          implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
          implicit val response: WSResponse   = getPageWithIndex()

          assertPageAsExpected(OK, expPageContents)
        }

        scenarioNameForAgentAndEnglish in {
          implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
          implicit val response: WSResponse   = getPageWithIndex()

          assertPageAsExpected(OK, expPageContents)
        }

        scenarioNameForAgentAndWelsh ignore {
          implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
          implicit val response: WSResponse   = getPageWithIndex()

          assertPageAsExpected(OK, expPageContents)
        }
      }
    }
  }

  "submit" should {

    "redirect to the Overseas Summary page when there is no session data" in {
      implicit val userConfig: UserConfig = userConfigWhenIrrelevant(None)
      implicit val response: WSResponse   = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(None, None))

      assertRedirectionAsExpected(PageRelativeURLs.overseasSummaryPage)
      getViewModel mustBe None
    }

    "succeed" when {
      "the user submits both amount values which are persisted" when {
        "the scheme is not yet complete for first pension and so redirects to the Special Withholding Tax page" in {
          val sessionData                     = pensionsUserData(aPensionsCYAModel)
          implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
          implicit val response: WSResponse   = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(Some("1234.56"), Some("78.90")), 1)

          val expectedViewModel = anIncomeFromOverseasPensionsViewModel.copy(overseasIncomePensionSchemes = Seq(
            aPensionScheme1,
            aPensionScheme2.copy(pensionPaymentAmount = Some(1234.56), pensionPaymentTaxPaid = Some(78.90), taxableAmount = None)))

          assertRedirectionAsExpected(PageRelativeURLs.incomeFromOverseasPensionsSwt + "?index=1")
          getViewModel mustBe Some(expectedViewModel)
        }

        "the scheme is not yet complete and so redirects to the Special Withholding Tax page" in {
          val viewModel = anIncomeFromOverseasPensionsViewModel.copy(
            overseasIncomePensionSchemes = Seq(aPensionScheme1, aPensionScheme2, PensionScheme(alphaTwoCode = Some("GB"))))
          val sessionData                     = pensionsUserData(aPensionsCYAModel.copy(incomeFromOverseasPensions = viewModel))
          implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
          implicit val response: WSResponse   = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(Some("1234.56"), Some("78.90")), 2)

          val expectedViewModel = viewModel.copy(
            overseasIncomePensionSchemes = Seq(
              aPensionScheme1,
              aPensionScheme2,
              PensionScheme(alphaTwoCode = Some("GB"), pensionPaymentAmount = Some(1234.56), pensionPaymentTaxPaid = Some(78.90)))
          )

          assertRedirectionAsExpected(PageRelativeURLs.incomeFromOverseasPensionsSwt + "?index=2")
          getViewModel mustBe Some(expectedViewModel)
        }
      }
    }

    "fail" when {

      val expectedPageContents = ExpectedOptionTupleAmountPageContents(
        title = "Error: Pension payments",
        header = "Pension payments",
        caption = s"Income from overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear",
        buttonForContinue = ExpectedButton("Continue", ""),
        amountSection1 = ExpectedAmountSection("Amount before tax", "1,233.41", Some("For example, £193.54")),
        amountSection2 = ExpectedAmountSection("Non-UK tax paid", "", Some("For example, £193.54")),
        errorSummarySectionOpt = Some(
          ErrorSummarySection(title = "There is a problem", body = "Enter the amount of non-UK tax paid", link = "#amount-2")
        ),
        errorAboveElementCheckSectionOpt = Some(
          ErrorAboveElementCheckSection(
            title = "Error: Enter the amount of non-UK tax paid",
            idOpt = Some("amount-2")
          )
        ),
        formUrl = formUrl()
      )

      def welshTitle(epc: ExpectedOptionTupleAmountPageContents): Option[ErrorSummarySection] =
        epc.errorSummarySectionOpt.map(ess => ess.copy(title = "Mae problem wedi codi"))

      "the user has relevant session data and" when {
        val sessionData       = pensionsUserData(aPensionsCYAModel)
        val expectedViewModel = sessionData.pensions.incomeFromOverseasPensions

        "amount before tax with no non uk tax paid" when {
          scenarioNameForIndividualAndEnglish in {
            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            implicit val response: WSResponse   = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(Some("1233.41"), None))

            assertPageAsExpected(BAD_REQUEST, expectedPageContents)
            getViewModel mustBe Some(expectedViewModel)
          }
          scenarioNameForIndividualAndWelsh ignore {
            implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
            implicit val response: WSResponse   = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(Some("1233.41"), None))

            assertPageAsExpected(BAD_REQUEST, expectedPageContents.copy(errorSummarySectionOpt = welshTitle(expectedPageContents)))
            getViewModel mustBe Some(expectedViewModel)
          }
          scenarioNameForAgentAndEnglish in {
            implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
            implicit val response: WSResponse   = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(Some("1233.41"), None))

            assertPageAsExpected(BAD_REQUEST, expectedPageContents)
            getViewModel mustBe Some(expectedViewModel)
          }
          scenarioNameForAgentAndWelsh ignore {
            implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
            implicit val response: WSResponse   = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(Some("1233.41"), None))

            assertPageAsExpected(BAD_REQUEST, expectedPageContents.copy(errorSummarySectionOpt = welshTitle(expectedPageContents)))
            getViewModel mustBe Some(expectedViewModel)
          }
        }
        "non uk tax paid with no amount before tax" when {

          val expPageContents = expectedPageContents.copy(
            amountSection1 = ExpectedAmountSection("Amount before tax", "", Some("For example, £193.54")),
            amountSection2 = ExpectedAmountSection("Non-UK tax paid", "113.91", Some("For example, £193.54")),
            errorSummarySectionOpt = Some(ErrorSummarySection(title = "There is a problem", body = "Enter an amount before tax", link = "#amount-1")),
            errorAboveElementCheckSectionOpt =
              Some(ErrorAboveElementCheckSection(title = "Error: Enter an amount before tax", idOpt = Some("amount-1")))
          )

          scenarioNameForIndividualAndEnglish in {
            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            implicit val response: WSResponse   = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(None, Some("113.91")))

            assertPageAsExpected(BAD_REQUEST, expPageContents)
            getViewModel mustBe Some(expectedViewModel)
          }
          scenarioNameForIndividualAndWelsh ignore {
            implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
            implicit val response: WSResponse   = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(None, Some("113.91")))

            assertPageAsExpected(BAD_REQUEST, expPageContents.copy(errorSummarySectionOpt = welshTitle(expPageContents)))
            getViewModel mustBe Some(expectedViewModel)
          }
          scenarioNameForAgentAndEnglish in {
            implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
            implicit val response: WSResponse   = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(None, Some("113.91")))

            assertPageAsExpected(BAD_REQUEST, expPageContents)
            getViewModel mustBe Some(expectedViewModel)
          }
          scenarioNameForAgentAndWelsh ignore {
            implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
            implicit val response: WSResponse   = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(None, Some("113.91")))

            assertPageAsExpected(BAD_REQUEST, expPageContents.copy(errorSummarySectionOpt = welshTitle(expPageContents)))
            getViewModel mustBe Some(expectedViewModel)
          }
        }
        "no amount before tax with no non uk tax paid" when {

          val expPageContents = expectedPageContents.copy(
            amountSection1 = ExpectedAmountSection("Amount before tax", "", Some("For example, £193.54")),
            amountSection2 = ExpectedAmountSection("Non-UK tax paid", "", Some("For example, £193.54")),
            errorSummarySectionOpt = Some(
              ErrorSummarySection(
                title = "There is a problem",
                Seq(
                  ErrorSummaryMessage("Enter an amount before tax", "#amount-1"),
                  ErrorSummaryMessage("Enter the amount of non-UK tax paid", "#amount-2")
                )
              )),
            errorAboveElementCheckSectionOpt =
              Some(ErrorAboveElementCheckSection(title = "Error: Enter the amount of non-UK tax paid", idOpt = Some("amount-2")))
          )

          scenarioNameForIndividualAndEnglish in {
            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            implicit val response: WSResponse   = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(None, None))

            assertPageAsExpected(BAD_REQUEST, expPageContents)
            getViewModel mustBe Some(expectedViewModel)
          }
          scenarioNameForIndividualAndWelsh ignore {
            implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
            implicit val response: WSResponse   = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(None, None))

            assertPageAsExpected(BAD_REQUEST, expPageContents.copy(errorSummarySectionOpt = welshTitle(expPageContents)))
            getViewModel mustBe Some(expectedViewModel)
          }
          scenarioNameForAgentAndEnglish in {
            implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
            implicit val response: WSResponse   = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(None, None))

            assertPageAsExpected(BAD_REQUEST, expPageContents)
            getViewModel mustBe Some(expectedViewModel)
          }
          scenarioNameForAgentAndWelsh ignore {
            implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
            implicit val response: WSResponse   = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(None, None))

            assertPageAsExpected(BAD_REQUEST, expPageContents.copy(errorSummarySectionOpt = welshTitle(expPageContents)))
            getViewModel mustBe Some(expectedViewModel)
          }
        }
        "incorrect amount before tax with non uk tax paid" when {

          val expPageContentsIndividual = expectedPageContents.copy(
            amountSection1 = ExpectedAmountSection("Amount before tax", "$342.23", Some("For example, £193.54")),
            amountSection2 = ExpectedAmountSection("Non-UK tax paid", "1,233.41", Some("For example, £193.54")),
            errorSummarySectionOpt =
              Some(ErrorSummarySection(title = "There is a problem", body = "Enter an amount before tax in pounds", link = "#amount-1")),
            errorAboveElementCheckSectionOpt =
              Some(ErrorAboveElementCheckSection(title = "Error: Enter an amount before tax in pounds", idOpt = Some("amount-1")))
          )
          val expPageContentsAgent = expPageContentsIndividual.copy(
            errorSummarySectionOpt =
              Some(ErrorSummarySection(title = "There is a problem", body = "Enter the amount before tax in pounds", link = "#amount-1")),
            errorAboveElementCheckSectionOpt =
              Some(ErrorAboveElementCheckSection(title = "Error: Enter the amount before tax in pounds", idOpt = Some("amount-1")))
          )

          scenarioNameForIndividualAndEnglish in {
            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            implicit val response: WSResponse   = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(Some("$342.23"), Some("1233.41")))

            assertPageAsExpected(BAD_REQUEST, expPageContentsIndividual)
            getViewModel mustBe Some(expectedViewModel)
          }
          scenarioNameForIndividualAndWelsh ignore {
            implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
            implicit val response: WSResponse   = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(Some("$342.23"), Some("1233.41")))

            assertPageAsExpected(BAD_REQUEST, expPageContentsIndividual.copy(errorSummarySectionOpt = welshTitle(expPageContentsIndividual)))
            getViewModel mustBe Some(expectedViewModel)
          }
          scenarioNameForAgentAndEnglish in {
            implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
            implicit val response: WSResponse   = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(Some("$342.23"), Some("1233.41")))

            assertPageAsExpected(BAD_REQUEST, expPageContentsAgent)
            getViewModel mustBe Some(expectedViewModel)
          }
          scenarioNameForAgentAndWelsh ignore {
            implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
            implicit val response: WSResponse   = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(Some("$342.23"), Some("1233.41")))

            assertPageAsExpected(BAD_REQUEST, expPageContentsAgent.copy(errorSummarySectionOpt = welshTitle(expPageContentsAgent)))
            getViewModel mustBe Some(expectedViewModel)
          }
        }
        "incorrect non uk tax paid with amount before tax" when {

          val expPageContents = expectedPageContents.copy(
            amountSection1 = ExpectedAmountSection("Amount before tax", "1,231.26", Some("For example, £193.54")),
            amountSection2 = ExpectedAmountSection("Non-UK tax paid", "asd", Some("For example, £193.54")),
            errorSummarySectionOpt =
              Some(ErrorSummarySection(title = "There is a problem", body = "Enter the amount of non-UK tax paid in pounds", link = "#amount-2")),
            errorAboveElementCheckSectionOpt =
              Some(ErrorAboveElementCheckSection(title = "Error: Enter the amount of non-UK tax paid in pounds", idOpt = Some("amount-2")))
          )

          scenarioNameForIndividualAndEnglish in {
            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            implicit val response: WSResponse   = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(Some("1231.26"), Some("asd")))

            assertPageAsExpected(BAD_REQUEST, expPageContents)
            getViewModel mustBe Some(expectedViewModel)
          }
          scenarioNameForIndividualAndWelsh ignore {
            implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
            implicit val response: WSResponse   = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(Some("1231.26"), Some("asd")))

            assertPageAsExpected(BAD_REQUEST, expPageContents.copy(errorSummarySectionOpt = welshTitle(expPageContents)))
            getViewModel mustBe Some(expectedViewModel)
          }
          scenarioNameForAgentAndEnglish in {
            implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
            implicit val response: WSResponse   = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(Some("1231.26"), Some("asd")))

            assertPageAsExpected(BAD_REQUEST, expPageContents)
            getViewModel mustBe Some(expectedViewModel)
          }
          scenarioNameForAgentAndWelsh ignore {
            implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
            implicit val response: WSResponse   = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(Some("1231.26"), Some("asd")))

            assertPageAsExpected(BAD_REQUEST, expPageContents.copy(errorSummarySectionOpt = welshTitle(expPageContents)))
            getViewModel mustBe Some(expectedViewModel)
          }
        }
        "negative amount before tax with non uk tax paid" when {

          val expPageContentsIndividual = expectedPageContents.copy(
            amountSection1 = ExpectedAmountSection("Amount before tax", "-1", Some("For example, £193.54")),
            amountSection2 = ExpectedAmountSection("Non-UK tax paid", "1,233.41", Some("For example, £193.54")),
            errorSummarySectionOpt =
              Some(ErrorSummarySection(title = "There is a problem", body = "Enter an amount before tax in pounds", link = "#amount-1")),
            errorAboveElementCheckSectionOpt =
              Some(ErrorAboveElementCheckSection(title = "Error: Enter an amount before tax in pounds", idOpt = Some("amount-1")))
          )
          val expPageContentsAgent = expPageContentsIndividual.copy(
            errorSummarySectionOpt =
              Some(ErrorSummarySection(title = "There is a problem", body = "Enter the amount before tax in pounds", link = "#amount-1")),
            errorAboveElementCheckSectionOpt =
              Some(ErrorAboveElementCheckSection(title = "Error: Enter the amount before tax in pounds", idOpt = Some("amount-1")))
          )

          scenarioNameForIndividualAndEnglish in {
            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            implicit val response: WSResponse   = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(Some("-1"), Some("1233.41")))

            assertPageAsExpected(BAD_REQUEST, expPageContentsIndividual)
            getViewModel mustBe Some(expectedViewModel)
          }
          scenarioNameForIndividualAndWelsh ignore {
            implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
            implicit val response: WSResponse   = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(Some("-1"), Some("1233.41")))

            assertPageAsExpected(BAD_REQUEST, expPageContentsIndividual.copy(errorSummarySectionOpt = welshTitle(expPageContentsIndividual)))
            getViewModel mustBe Some(expectedViewModel)
          }
          scenarioNameForAgentAndEnglish in {
            implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
            implicit val response: WSResponse   = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(Some("-1"), Some("1233.41")))

            assertPageAsExpected(BAD_REQUEST, expPageContentsAgent)
            getViewModel mustBe Some(expectedViewModel)
          }
          scenarioNameForAgentAndWelsh ignore {
            implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
            implicit val response: WSResponse   = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(Some("-1"), Some("1233.41")))

            assertPageAsExpected(BAD_REQUEST, expPageContentsAgent.copy(errorSummarySectionOpt = welshTitle(expPageContentsAgent)))
            getViewModel mustBe Some(expectedViewModel)
          }
        }
        "negative non uk tax paid with amount before tax" when {

          val expPageContents = expectedPageContents.copy(
            amountSection1 = ExpectedAmountSection("Amount before tax", "1,231.26", Some("For example, £193.54")),
            amountSection2 = ExpectedAmountSection("Non-UK tax paid", "-1", Some("For example, £193.54")),
            errorSummarySectionOpt =
              Some(ErrorSummarySection(title = "There is a problem", body = "Enter the amount of non-UK tax paid in pounds", link = "#amount-2")),
            errorAboveElementCheckSectionOpt =
              Some(ErrorAboveElementCheckSection(title = "Error: Enter the amount of non-UK tax paid in pounds", idOpt = Some("amount-2")))
          )

          scenarioNameForIndividualAndEnglish in {
            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            implicit val response: WSResponse   = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(Some("1231.26"), Some("-1")))

            assertPageAsExpected(BAD_REQUEST, expPageContents)
            getViewModel mustBe Some(expectedViewModel)
          }
          scenarioNameForIndividualAndWelsh ignore {
            implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
            implicit val response: WSResponse   = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(Some("1231.26"), Some("-1")))

            assertPageAsExpected(BAD_REQUEST, expPageContents.copy(errorSummarySectionOpt = welshTitle(expPageContents)))
            getViewModel mustBe Some(expectedViewModel)
          }
          scenarioNameForAgentAndEnglish in {
            implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
            implicit val response: WSResponse   = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(Some("1231.26"), Some("-1")))

            assertPageAsExpected(BAD_REQUEST, expPageContents)
            getViewModel mustBe Some(expectedViewModel)
          }
          scenarioNameForAgentAndWelsh ignore {
            implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
            implicit val response: WSResponse   = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(Some("1231.26"), Some("-1")))

            assertPageAsExpected(BAD_REQUEST, expPageContents.copy(errorSummarySectionOpt = welshTitle(expPageContents)))
            getViewModel mustBe Some(expectedViewModel)
          }
        }
        "too big amount before tax with non uk tax paid" when {

          val expPageContents = expectedPageContents.copy(
            amountSection1 = ExpectedAmountSection("Amount before tax", "1000000000001", Some("For example, £193.54")),
            amountSection2 = ExpectedAmountSection("Non-UK tax paid", "1,233.41", Some("For example, £193.54")),
            errorSummarySectionOpt = Some(
              ErrorSummarySection(
                title = "There is a problem",
                body = "The amount before tax must be less than £100,000,000,000",
                link = "#amount-1")),
            errorAboveElementCheckSectionOpt =
              Some(ErrorAboveElementCheckSection(title = "Error: The amount before tax must be less than £100,000,000,000", idOpt = Some("amount-1")))
          )

          scenarioNameForIndividualAndEnglish in {
            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(Some("1000000000001"), Some("1233.41")))

            assertPageAsExpected(BAD_REQUEST, expPageContents)
            getViewModel mustBe Some(expectedViewModel)
          }
          scenarioNameForIndividualAndWelsh ignore {
            implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
            implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(Some("1000000000001"), Some("1233.41")))

            assertPageAsExpected(BAD_REQUEST, expPageContents.copy(errorSummarySectionOpt = welshTitle(expPageContents)))
            getViewModel mustBe Some(expectedViewModel)
          }
          scenarioNameForAgentAndEnglish in {
            implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
            implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(Some("1000000000001"), Some("1233.41")))

            assertPageAsExpected(BAD_REQUEST, expPageContents)
            getViewModel mustBe Some(expectedViewModel)
          }
          scenarioNameForAgentAndWelsh ignore {
            implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
            implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(Some("1000000000001"), Some("1233.41")))

            assertPageAsExpected(BAD_REQUEST, expPageContents.copy(errorSummarySectionOpt = welshTitle(expPageContents)))
            getViewModel mustBe Some(expectedViewModel)
          }
        }
        "too big non uk tax paid with amount before tax" when {

          val expPageContents = expectedPageContents.copy(
            amountSection1 = ExpectedAmountSection("Amount before tax", "1,233.41", Some("For example, £193.54")),
            amountSection2 = ExpectedAmountSection("Non-UK tax paid", "1000000000001", Some("For example, £193.54")),
            errorSummarySectionOpt = Some(
              ErrorSummarySection(
                title = "There is a problem",
                body = "The amount of non-UK tax paid must be less than £100,000,000,000",
                link = "#amount-2")),
            errorAboveElementCheckSectionOpt = Some(
              ErrorAboveElementCheckSection(
                title = "Error: The amount of non-UK tax paid must be less than £100,000,000,000",
                idOpt = Some("amount-2")))
          )

          scenarioNameForIndividualAndEnglish in {
            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(Some("1233.41"), Some("1000000000001")))

            assertPageAsExpected(BAD_REQUEST, expPageContents)
            getViewModel mustBe Some(expectedViewModel)
          }
          scenarioNameForIndividualAndWelsh ignore {
            implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
            implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(Some("1233.41"), Some("1000000000001")))

            assertPageAsExpected(BAD_REQUEST, expPageContents.copy(errorSummarySectionOpt = welshTitle(expPageContents)))
            getViewModel mustBe Some(expectedViewModel)
          }
          scenarioNameForAgentAndEnglish in {
            implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
            implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(Some("1233.41"), Some("1000000000001")))

            assertPageAsExpected(BAD_REQUEST, expPageContents)
            getViewModel mustBe Some(expectedViewModel)
          }
          scenarioNameForAgentAndWelsh ignore {
            implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
            implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(Some("1233.41"), Some("1000000000001")))

            assertPageAsExpected(BAD_REQUEST, expPageContents.copy(errorSummarySectionOpt = welshTitle(expPageContents)))
            getViewModel mustBe Some(expectedViewModel)
          }
        }

        "amount before tax is greater than non uk tax" when {

          scenarioNameForIndividualAndEnglish in {
            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            implicit val response: WSResponse   = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(Some("1233.41"), Some("10000")))

            response.status mustBe BAD_REQUEST
            getViewModel mustBe Some(expectedViewModel)
          }
          scenarioNameForIndividualAndWelsh ignore {
            implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
            implicit val response: WSResponse   = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(Some("1233.41"), Some("10000")))

            response.status mustBe BAD_REQUEST
            getViewModel mustBe Some(expectedViewModel)
          }
          scenarioNameForAgentAndEnglish in {
            implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
            implicit val response: WSResponse   = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(Some("1233.41"), Some("10000")))

            response.status mustBe BAD_REQUEST
            getViewModel mustBe Some(expectedViewModel)
          }
          scenarioNameForAgentAndWelsh ignore {
            implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
            implicit val response: WSResponse   = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(Some("1233.41"), Some("10000")))

            response.status mustBe BAD_REQUEST
            getViewModel mustBe Some(expectedViewModel)
          }
        }
      }
    }
  }

  private def getViewModel(implicit userConfig: UserConfig): Option[IncomeFromOverseasPensionsViewModel] =
    loadPensionUserData.map(_.pensions.incomeFromOverseasPensions)

  private def formUrl(index: Int = 0): Option[String] =
    Some(relativeUrlForThisPage + "?index=" + index)

  private def submitFormWithIndex(submittedFormData: SubmittedFormData, index: Int = 0)(implicit
      userConfig: UserConfig,
      wsClient: WSClient): WSResponse =
    submitForm(submittedFormData, getMap(index))

  private def getMap(index: Int): Map[String, String] =
    Map("index" -> index.toString)
}
