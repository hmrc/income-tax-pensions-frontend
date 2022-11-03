/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers.pensions.overseas.incomeFromOverseasPension


import builders.IncomeFromOverseasPensionsViewModelBuilder.anIncomeFromOverseasPensionsViewModel
import builders.PensionsCYAModelBuilder.{aPensionsCYAEmptyModel, aPensionsCYAModel}
import controllers.ControllerSpec.PreferredLanguages.{English, Welsh}
import controllers.ControllerSpec.UserTypes.{Agent, Individual}
import controllers.ControllerSpec._
import controllers.{SubmittedFormDataForOptionTupleAmountPage, TwoAmountsControllerISpec}
import models.pension.charges.IncomeFromOverseasPensionsViewModel
import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.libs.ws.{WSClient, WSResponse}

class PensionPaymentsControllerISpec extends TwoAmountsControllerISpec("/overseas-pensions/income-from-overseas-pensions/pension-overseas-income-amounts") {

  "This page" when {
    "show" should {
      "redirect to the summary page" when {
        "the user has no stored session data at all" in {
          implicit val userConfig: UserConfig = userConfigWhenIrrelevant(None)
          implicit val response: WSResponse = getPageWithIndex()

          assertRedirectionAsExpected(PageRelativeURLs.summaryPage)
        }
        "the user has no relevant session data and" in {
          val sessionData = pensionsUserData(aPensionsCYAEmptyModel)

          implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
          implicit val response: WSResponse = getPageWithIndex()
          assertRedirectionAsExpected(PageRelativeURLs.summaryPage)
        }
        "incorrect index provided" in {
          val sessionData = pensionsUserData(aPensionsCYAModel)

          implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
          implicit val response: WSResponse = getPageWithIndex(7)
          assertRedirectionAsExpected(PageRelativeURLs.summaryPage)
        }
        "the user provides a negative index number" in {
          val sessionData = pensionsUserData(aPensionsCYAModel)
          implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
          implicit val response: WSResponse = getPageWithIndex(-1)

          assertRedirectionAsExpected(PageRelativeURLs.summaryPage)
        }
      }

      "appear as expected" should {
        "the user has relevant session data with empty payments in pension scheme" when {
          val incomeViewModel = anIncomeFromOverseasPensionsViewModel.copy(overseasIncomePensionSchemes = Seq(
            anIncomeFromOverseasPensionsViewModel.overseasIncomePensionSchemes.head
              .copy(pensionPaymentAmount = None, pensionPaymentTaxPaid = None)))

          val updatedUserData = aPensionsCYAModel.copy(incomeFromOverseasPensions = incomeViewModel)

          val sessionData = pensionsUserData(updatedUserData)

          scenarioNameForIndividualAndEnglish in {
            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            implicit val response: WSResponse = getPageWithIndex()

            assertPageAsExpected(
              OK,
              ExpectedOptionTupleAmountPageContents(
                title = "Pension payments",
                header = "Pension payments",
                caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection1 = ExpectedAmountSection("Amount before tax", "For example, £193.54", ""),
                amountSection2 = ExpectedAmountSection("Non-UK tax paid", "For example, £193.54", ""),
                errorSummarySectionOpt = None,
                errorAboveElementCheckSectionOpt = None,
                formUrl = formUrl()
              ))
          }

          scenarioNameForIndividualAndWelsh in {
            implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
            implicit val response: WSResponse = getPageWithIndex()
            assertPageAsExpected(
              OK,
              ExpectedOptionTupleAmountPageContents(
                title = "Pension payments",
                header = "Pension payments",
                caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection1 = ExpectedAmountSection("Amount before tax", "For example, £193.54", ""),
                amountSection2 = ExpectedAmountSection("Non-UK tax paid", "For example, £193.54", ""),
                errorSummarySectionOpt = None,
                errorAboveElementCheckSectionOpt = None,
                formUrl = formUrl()
              ))
          }

          scenarioNameForAgentAndEnglish in {
            implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
            implicit val response: WSResponse = getPageWithIndex()

            assertPageAsExpected(
              OK,
              ExpectedOptionTupleAmountPageContents(
                title = "Pension payments",
                header = "Pension payments",
                caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection1 = ExpectedAmountSection("Amount before tax", "For example, £193.54", ""),
                amountSection2 = ExpectedAmountSection("Non-UK tax paid", "For example, £193.54", ""),
                errorSummarySectionOpt = None,
                errorAboveElementCheckSectionOpt = None,
                formUrl = formUrl()
              ))
          }

          scenarioNameForAgentAndWelsh in {
            implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
            implicit val response: WSResponse = getPageWithIndex()
            assertPageAsExpected(
              OK,
              ExpectedOptionTupleAmountPageContents(
                title = "Pension payments",
                header = "Pension payments",
                caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection1 = ExpectedAmountSection("Amount before tax", "For example, £193.54", ""),
                amountSection2 = ExpectedAmountSection("Non-UK tax paid", "For example, £193.54", ""),
                errorSummarySectionOpt = None,
                errorAboveElementCheckSectionOpt = None,
                formUrl = formUrl()
              ))
          }
        }
        "the user has relevant session data with both payments in pension scheme" when {
          val incomeViewModel = anIncomeFromOverseasPensionsViewModel.copy(overseasIncomePensionSchemes = Seq(
            anIncomeFromOverseasPensionsViewModel.overseasIncomePensionSchemes.head
              .copy(pensionPaymentAmount = Some(BigDecimal(1992.36)), pensionPaymentTaxPaid = Some(BigDecimal(1121.31)))))

          val updatedUserData = aPensionsCYAModel.copy(incomeFromOverseasPensions = incomeViewModel)

          val sessionData = pensionsUserData(updatedUserData)

          scenarioNameForIndividualAndEnglish in {
            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            implicit val response: WSResponse = getPageWithIndex()

            assertPageAsExpected(
              OK,
              ExpectedOptionTupleAmountPageContents(
                title = "Pension payments",
                header = "Pension payments",
                caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection1 = ExpectedAmountSection("Amount before tax", "For example, £193.54", "1,992.36"),
                amountSection2 = ExpectedAmountSection("Non-UK tax paid", "For example, £193.54", "1,121.31"),
                errorSummarySectionOpt = None,
                errorAboveElementCheckSectionOpt = None,
                formUrl = formUrl()
              ))
          }

          scenarioNameForIndividualAndWelsh in {
            implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
            implicit val response: WSResponse = getPageWithIndex()
            assertPageAsExpected(
              OK,
              ExpectedOptionTupleAmountPageContents(
                title = "Pension payments",
                header = "Pension payments",
                caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection1 = ExpectedAmountSection("Amount before tax", "For example, £193.54", "1,992.36"),
                amountSection2 = ExpectedAmountSection("Non-UK tax paid", "For example, £193.54", "1,121.31"),
                errorSummarySectionOpt = None,
                errorAboveElementCheckSectionOpt = None,
                formUrl = formUrl()
              ))
          }

          scenarioNameForAgentAndEnglish in {
            implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
            implicit val response: WSResponse = getPageWithIndex()

            assertPageAsExpected(
              OK,
              ExpectedOptionTupleAmountPageContents(
                title = "Pension payments",
                header = "Pension payments",
                caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection1 = ExpectedAmountSection("Amount before tax", "For example, £193.54", "1,992.36"),
                amountSection2 = ExpectedAmountSection("Non-UK tax paid", "For example, £193.54", "1,121.31"),
                errorSummarySectionOpt = None,
                errorAboveElementCheckSectionOpt = None,
                formUrl = formUrl()
              ))
          }

          scenarioNameForAgentAndWelsh in {
            implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
            implicit val response: WSResponse = getPageWithIndex()
            assertPageAsExpected(
              OK,
              ExpectedOptionTupleAmountPageContents(
                title = "Pension payments",
                header = "Pension payments",
                caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection1 = ExpectedAmountSection("Amount before tax", "For example, £193.54", "1,992.36"),
                amountSection2 = ExpectedAmountSection("Non-UK tax paid", "For example, £193.54", "1,121.31"),
                errorSummarySectionOpt = None,
                errorAboveElementCheckSectionOpt = None,
                formUrl = formUrl()
              ))
          }
        }
        "the user has relevant session data with only Amount before tax in pension scheme" when {
          val incomeViewModel = anIncomeFromOverseasPensionsViewModel.copy(overseasIncomePensionSchemes = Seq(
            anIncomeFromOverseasPensionsViewModel.overseasIncomePensionSchemes.head
              .copy(pensionPaymentAmount = Some(BigDecimal(1992.36)), pensionPaymentTaxPaid = None)))

          val updatedUserData = aPensionsCYAModel.copy(incomeFromOverseasPensions = incomeViewModel)

          val sessionData = pensionsUserData(updatedUserData)

          scenarioNameForIndividualAndEnglish in {
            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            implicit val response: WSResponse = getPageWithIndex()

            assertPageAsExpected(
              OK,
              ExpectedOptionTupleAmountPageContents(
                title = "Pension payments",
                header = "Pension payments",
                caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection1 = ExpectedAmountSection("Amount before tax", "For example, £193.54", "1,992.36"),
                amountSection2 = ExpectedAmountSection("Non-UK tax paid", "For example, £193.54", ""),
                errorSummarySectionOpt = None,
                errorAboveElementCheckSectionOpt = None,
                formUrl = formUrl()
              ))
          }

          scenarioNameForIndividualAndWelsh in {
            implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
            implicit val response: WSResponse = getPageWithIndex()
            assertPageAsExpected(
              OK,
              ExpectedOptionTupleAmountPageContents(
                title = "Pension payments",
                header = "Pension payments",
                caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection1 = ExpectedAmountSection("Amount before tax", "For example, £193.54", "1,992.36"),
                amountSection2 = ExpectedAmountSection("Non-UK tax paid", "For example, £193.54", ""),
                errorSummarySectionOpt = None,
                errorAboveElementCheckSectionOpt = None,
                formUrl = formUrl()
              ))
          }

          scenarioNameForAgentAndEnglish in {
            implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
            implicit val response: WSResponse = getPageWithIndex()

            assertPageAsExpected(
              OK,
              ExpectedOptionTupleAmountPageContents(
                title = "Pension payments",
                header = "Pension payments",
                caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection1 = ExpectedAmountSection("Amount before tax", "For example, £193.54", "1,992.36"),
                amountSection2 = ExpectedAmountSection("Non-UK tax paid", "For example, £193.54", ""),
                errorSummarySectionOpt = None,
                errorAboveElementCheckSectionOpt = None,
                formUrl = formUrl()
              ))
          }

          scenarioNameForAgentAndWelsh in {
            implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
            implicit val response: WSResponse = getPageWithIndex()
            assertPageAsExpected(
              OK,
              ExpectedOptionTupleAmountPageContents(
                title = "Pension payments",
                header = "Pension payments",
                caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection1 = ExpectedAmountSection("Amount before tax", "For example, £193.54", "1,992.36"),
                amountSection2 = ExpectedAmountSection("Non-UK tax paid", "For example, £193.54", ""),
                errorSummarySectionOpt = None,
                errorAboveElementCheckSectionOpt = None,
                formUrl = formUrl()
              ))
          }
        }
        "the user has relevant session data with only Non-Uk tax paid in pension scheme" when {
          val incomeViewModel = anIncomeFromOverseasPensionsViewModel.copy(overseasIncomePensionSchemes = Seq(
            anIncomeFromOverseasPensionsViewModel.overseasIncomePensionSchemes.head
              .copy(pensionPaymentAmount = None, pensionPaymentTaxPaid = Some(BigDecimal(1312.21)))))

          val updatedUserData = aPensionsCYAModel.copy(incomeFromOverseasPensions = incomeViewModel)

          val sessionData = pensionsUserData(updatedUserData)

          scenarioNameForIndividualAndEnglish in {
            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            implicit val response: WSResponse = getPageWithIndex()

            assertPageAsExpected(
              OK,
              ExpectedOptionTupleAmountPageContents(
                title = "Pension payments",
                header = "Pension payments",
                caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection1 = ExpectedAmountSection("Amount before tax", "For example, £193.54", ""),
                amountSection2 = ExpectedAmountSection("Non-UK tax paid", "For example, £193.54", "1,312.21"),
                errorSummarySectionOpt = None,
                errorAboveElementCheckSectionOpt = None,
                formUrl = formUrl()
              ))
          }

          scenarioNameForIndividualAndWelsh in {
            implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
            implicit val response: WSResponse = getPageWithIndex()
            assertPageAsExpected(
              OK,
              ExpectedOptionTupleAmountPageContents(
                title = "Pension payments",
                header = "Pension payments",
                caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection1 = ExpectedAmountSection("Amount before tax", "For example, £193.54", ""),
                amountSection2 = ExpectedAmountSection("Non-UK tax paid", "For example, £193.54", "1,312.21"),
                errorSummarySectionOpt = None,
                errorAboveElementCheckSectionOpt = None,
                formUrl = formUrl()
              ))
          }

          scenarioNameForAgentAndEnglish in {
            implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
            implicit val response: WSResponse = getPageWithIndex()

            assertPageAsExpected(
              OK,
              ExpectedOptionTupleAmountPageContents(
                title = "Pension payments",
                header = "Pension payments",
                caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection1 = ExpectedAmountSection("Amount before tax", "For example, £193.54", ""),
                amountSection2 = ExpectedAmountSection("Non-UK tax paid", "For example, £193.54", "1,312.21"),
                errorSummarySectionOpt = None,
                errorAboveElementCheckSectionOpt = None,
                formUrl = formUrl()
              ))
          }

          scenarioNameForAgentAndWelsh in {
            implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
            implicit val response: WSResponse = getPageWithIndex()
            assertPageAsExpected(
              OK,
              ExpectedOptionTupleAmountPageContents(
                title = "Pension payments",
                header = "Pension payments",
                caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection1 = ExpectedAmountSection("Amount before tax", "For example, £193.54", ""),
                amountSection2 = ExpectedAmountSection("Non-UK tax paid", "For example, £193.54", "1,312.21"),
                errorSummarySectionOpt = None,
                errorAboveElementCheckSectionOpt = None,
                formUrl = formUrl()
              ))
          }
        }
      }
    }
    "submit" should {
      "redirect to the expected page" when {
        "the user has no stored session data at all" in {
          implicit val userConfig: UserConfig = userConfigWhenIrrelevant(None)
          implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(None, None))
          assertRedirectionAsExpected(PageRelativeURLs.summaryPage)
          getViewModel mustBe None
        }
      }
      "succeed" when {
        "the user has relevant session data and" when {
          val sessionData = pensionsUserData(aPensionsCYAModel)
          "with payments" in {
            implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
            implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(Some("1234.56"), Some("78.90")))

            val incomeViewModel = anIncomeFromOverseasPensionsViewModel.copy(overseasIncomePensionSchemes = Seq(
              anIncomeFromOverseasPensionsViewModel.overseasIncomePensionSchemes.head
                .copy(pensionPaymentAmount = Some(BigDecimal(1234.56)), pensionPaymentTaxPaid = Some(BigDecimal(78.90)))))

            assertRedirectionAsExpected(relativeUrlForThisPage + "?index=0")
            getViewModel mustBe Some(incomeViewModel)
          }
        }
      }
      "fail" when {
        "the user has relevant session data and" when {
          val sessionData = pensionsUserData(aPensionsCYAModel)
          val expectedViewModel = sessionData.pensions.incomeFromOverseasPensions
          "amount before tax with no non uk tax paid" when {
            scenarioNameForIndividualAndEnglish in {
              implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
              implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(Some("1233.41"), None))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedOptionTupleAmountPageContents(
                  title = "Error: Pension payments",
                  header = "Pension payments",
                  caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection1 = ExpectedAmountSection("Amount before tax", "For example, £193.54", "1,233.41"),
                  amountSection2 = ExpectedAmountSection("Non-UK tax paid", "For example, £193.54", ""),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "Enter the amount of non-UK tax paid",
                      link = "#amount-2"),

                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter the amount of non-UK tax paid",
                      idOpt = Some("amount-2")
                    )
                  ),
                  formUrl = formUrl()
                )
              )

              getViewModel mustBe Some(expectedViewModel)
            }
            scenarioNameForIndividualAndWelsh in {
              implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(Some("1233.41"), None))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedOptionTupleAmountPageContents(
                  title = "Error: Pension payments",
                  header = "Pension payments",
                  caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection1 = ExpectedAmountSection("Amount before tax", "For example, £193.54", "1,233.41"),
                  amountSection2 = ExpectedAmountSection("Non-UK tax paid", "For example, £193.54", ""),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "Enter the amount of non-UK tax paid",
                      link = "#amount-2"),

                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter the amount of non-UK tax paid",
                      idOpt = Some("amount-2")
                    )
                  ),
                  formUrl = formUrl()
                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
            scenarioNameForAgentAndEnglish in {
              implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
              implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(Some("1233.41"), None))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedOptionTupleAmountPageContents(
                  title = "Error: Pension payments",
                  header = "Pension payments",
                  caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection1 = ExpectedAmountSection("Amount before tax", "For example, £193.54", "1,233.41"),
                  amountSection2 = ExpectedAmountSection("Non-UK tax paid", "For example, £193.54", ""),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "Enter the amount of non-UK tax paid",
                      link = "#amount-2"),

                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter the amount of non-UK tax paid",
                      idOpt = Some("amount-2")
                    )
                  ),
                  formUrl = formUrl()
                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
            scenarioNameForAgentAndWelsh in {
              implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(Some("1233.41"), None))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedOptionTupleAmountPageContents(
                  title = "Error: Pension payments",
                  header = "Pension payments",
                  caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection1 = ExpectedAmountSection("Amount before tax", "For example, £193.54", "1,233.41"),
                  amountSection2 = ExpectedAmountSection("Non-UK tax paid", "For example, £193.54", ""),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "Enter the amount of non-UK tax paid",
                      link = "#amount-2"),

                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter the amount of non-UK tax paid",
                      idOpt = Some("amount-2")
                    )
                  ),
                  formUrl = formUrl()
                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
          }
          "non uk tax paid with no amount before tax" when {
            scenarioNameForIndividualAndEnglish in {
              implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
              implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(None, Some("113.91")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedOptionTupleAmountPageContents(
                  title = "Error: Pension payments",
                  header = "Pension payments",
                  caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection1 = ExpectedAmountSection("Amount before tax", "For example, £193.54", ""),
                  amountSection2 = ExpectedAmountSection("Non-UK tax paid", "For example, £193.54", "113.91"),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "Enter an amount before tax",
                      link = "#amount-1"),

                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter an amount before tax",
                      idOpt = Some("amount-1")
                    )
                  ),
                  formUrl = formUrl()
                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
            scenarioNameForIndividualAndWelsh in {
              implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(None, Some("113.91")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedOptionTupleAmountPageContents(
                  title = "Error: Pension payments",
                  header = "Pension payments",
                  caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection1 = ExpectedAmountSection("Amount before tax", "For example, £193.54", ""),
                  amountSection2 = ExpectedAmountSection("Non-UK tax paid", "For example, £193.54", "113.91"),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "Enter an amount before tax",
                      link = "#amount-1"),

                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter an amount before tax",
                      idOpt = Some("amount-1")
                    )
                  ),
                  formUrl = formUrl()
                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
            scenarioNameForAgentAndEnglish in {
              implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
              implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(None, Some("113.91")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedOptionTupleAmountPageContents(
                  title = "Error: Pension payments",
                  header = "Pension payments",
                  caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection1 = ExpectedAmountSection("Amount before tax", "For example, £193.54", ""),
                  amountSection2 = ExpectedAmountSection("Non-UK tax paid", "For example, £193.54", "113.91"),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "Enter an amount before tax",
                      link = "#amount-1"),

                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter an amount before tax",
                      idOpt = Some("amount-1")
                    )
                  ),
                  formUrl = formUrl()

                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
            scenarioNameForAgentAndWelsh in {
              implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(None, Some("113.91")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedOptionTupleAmountPageContents(
                  title = "Error: Pension payments",
                  header = "Pension payments",
                  caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection1 = ExpectedAmountSection("Amount before tax", "For example, £193.54", ""),
                  amountSection2 = ExpectedAmountSection("Non-UK tax paid", "For example, £193.54", "113.91"),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "Enter an amount before tax",
                      link = "#amount-1"),

                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter an amount before tax",
                      idOpt = Some("amount-1")
                    )
                  ),
                  formUrl = formUrl()

                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
          }
          "no amount before tax with no non uk tax paid" when {
            scenarioNameForIndividualAndEnglish in {
              implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
              implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(None, None))


              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedOptionTupleAmountPageContents(
                  title = "Error: Pension payments",
                  header = "Pension payments",
                  caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection1 = ExpectedAmountSection("Amount before tax", "For example, £193.54", ""),
                  amountSection2 = ExpectedAmountSection("Non-UK tax paid", "For example, £193.54", ""),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection("There is a problem", Seq(
                      ErrorSummaryMessage("Enter an amount before tax", "#amount-1"),
                      ErrorSummaryMessage("Enter the amount of non-UK tax paid", "#amount-2")
                    ))
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter the amount of non-UK tax paid",
                      idOpt = Some("amount-2")
                    )
                  ),
                  formUrl = formUrl()
                )
              )

              getViewModel mustBe Some(expectedViewModel)
            }
            scenarioNameForIndividualAndWelsh in {
              implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(None, None))


              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedOptionTupleAmountPageContents(
                  title = "Error: Pension payments",
                  header = "Pension payments",
                  caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection1 = ExpectedAmountSection("Amount before tax", "For example, £193.54", ""),
                  amountSection2 = ExpectedAmountSection("Non-UK tax paid", "For example, £193.54", ""),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection("Mae problem wedi codi", Seq(
                      ErrorSummaryMessage("Enter an amount before tax", "#amount-1"),
                      ErrorSummaryMessage("Enter the amount of non-UK tax paid", "#amount-2")
                    ))
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter the amount of non-UK tax paid",
                      idOpt = Some("amount-2")
                    )
                  ),
                  formUrl = formUrl()
                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
            scenarioNameForAgentAndEnglish in {
              implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
              implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(None, None))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedOptionTupleAmountPageContents(
                  title = "Error: Pension payments",
                  header = "Pension payments",
                  caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection1 = ExpectedAmountSection("Amount before tax", "For example, £193.54", ""),
                  amountSection2 = ExpectedAmountSection("Non-UK tax paid", "For example, £193.54", ""),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection("There is a problem", Seq(
                      ErrorSummaryMessage("Enter an amount before tax", "#amount-1"),
                      ErrorSummaryMessage("Enter the amount of non-UK tax paid", "#amount-2")
                    ))
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter the amount of non-UK tax paid",
                      idOpt = Some("amount-2")
                    )
                  ),
                  formUrl = formUrl()
                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
            scenarioNameForAgentAndWelsh in {
              implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(None, None))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedOptionTupleAmountPageContents(
                  title = "Error: Pension payments",
                  header = "Pension payments",
                  caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection1 = ExpectedAmountSection("Amount before tax", "For example, £193.54", ""),
                  amountSection2 = ExpectedAmountSection("Non-UK tax paid", "For example, £193.54", ""),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection("Mae problem wedi codi", Seq(
                      ErrorSummaryMessage("Enter an amount before tax", "#amount-1"),
                      ErrorSummaryMessage("Enter the amount of non-UK tax paid", "#amount-2")
                    ))
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter the amount of non-UK tax paid",
                      idOpt = Some("amount-2")
                    )
                  ),
                  formUrl = formUrl()
                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
          }
          "incorrect amount before tax with non uk tax paid" when {
            scenarioNameForIndividualAndEnglish in {
              implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
              implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(Some("$342.23"), Some("1233.41")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedOptionTupleAmountPageContents(
                  title = "Error: Pension payments",
                  header = "Pension payments",
                  caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection1 = ExpectedAmountSection("Amount before tax", "For example, £193.54", "$342.23"),
                  amountSection2 = ExpectedAmountSection("Non-UK tax paid", "For example, £193.54", "1,233.41"),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "Enter an amount before tax in the correct format",
                      link = "#amount-1"),

                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter an amount before tax in the correct format",
                      idOpt = Some("amount-1")
                    )
                  ),
                  formUrl = formUrl()

                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
            scenarioNameForIndividualAndWelsh in {
              implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(Some("$342.23"), Some("1233.41")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedOptionTupleAmountPageContents(
                  title = "Error: Pension payments",
                  header = "Pension payments",
                  caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection1 = ExpectedAmountSection("Amount before tax", "For example, £193.54", "$342.23"),
                  amountSection2 = ExpectedAmountSection("Non-UK tax paid", "For example, £193.54", "1,233.41"),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "Enter an amount before tax in the correct format",
                      link = "#amount-1"),

                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter an amount before tax in the correct format",
                      idOpt = Some("amount-1")
                    )
                  ),
                  formUrl = formUrl()

                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
            scenarioNameForAgentAndEnglish in {
              implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
              implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(Some("$342.23"), Some("1233.41")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedOptionTupleAmountPageContents(
                  title = "Error: Pension payments",
                  header = "Pension payments",
                  caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection1 = ExpectedAmountSection("Amount before tax", "For example, £193.54", "$342.23"),
                  amountSection2 = ExpectedAmountSection("Non-UK tax paid", "For example, £193.54", "1,233.41"),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "Enter the amount before tax in the correct format",
                      link = "#amount-1"),

                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter the amount before tax in the correct format",
                      idOpt = Some("amount-1")
                    )
                  ),
                  formUrl = formUrl()

                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
            scenarioNameForAgentAndWelsh in {
              implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(Some("$342.23"), Some("1233.41")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedOptionTupleAmountPageContents(
                  title = "Error: Pension payments",
                  header = "Pension payments",
                  caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection1 = ExpectedAmountSection("Amount before tax", "For example, £193.54", "$342.23"),
                  amountSection2 = ExpectedAmountSection("Non-UK tax paid", "For example, £193.54", "1,233.41"),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "Enter the amount before tax in the correct format",
                      link = "#amount-1"),

                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter the amount before tax in the correct format",
                      idOpt = Some("amount-1")
                    )
                  ),
                  formUrl = formUrl()

                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
          }
          "incorrect non uk tax paid with amount before tax" when {
            scenarioNameForIndividualAndEnglish in {
              implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
              implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(Some("1231.26"), Some("asd")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedOptionTupleAmountPageContents(
                  title = "Error: Pension payments",
                  header = "Pension payments",
                  caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection1 = ExpectedAmountSection("Amount before tax", "For example, £193.54", "1,231.26"),
                  amountSection2 = ExpectedAmountSection("Non-UK tax paid", "For example, £193.54", "asd"),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "Enter the amount of non-UK tax paid in the correct format",
                      link = "#amount-2"),

                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter the amount of non-UK tax paid in the correct format",
                      idOpt = Some("amount-2")
                    )
                  ),
                  formUrl = formUrl()

                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
            scenarioNameForIndividualAndWelsh in {
              implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(Some("1231.26"), Some("asd")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedOptionTupleAmountPageContents(
                  title = "Error: Pension payments",
                  header = "Pension payments",
                  caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection1 = ExpectedAmountSection("Amount before tax", "For example, £193.54", "1,231.26"),
                  amountSection2 = ExpectedAmountSection("Non-UK tax paid", "For example, £193.54", "asd"),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "Enter the amount of non-UK tax paid in the correct format",
                      link = "#amount-2"),

                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter the amount of non-UK tax paid in the correct format",
                      idOpt = Some("amount-2")
                    )
                  ),
                  formUrl = formUrl()

                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
            scenarioNameForAgentAndEnglish in {
              implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
              implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(Some("1231.26"), Some("asd")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedOptionTupleAmountPageContents(
                  title = "Error: Pension payments",
                  header = "Pension payments",
                  caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection1 = ExpectedAmountSection("Amount before tax", "For example, £193.54", "1,231.26"),
                  amountSection2 = ExpectedAmountSection("Non-UK tax paid", "For example, £193.54", "asd"),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "Enter the amount of non-UK tax paid in the correct format",
                      link = "#amount-2"),

                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter the amount of non-UK tax paid in the correct format",
                      idOpt = Some("amount-2")
                    )
                  ),
                  formUrl = formUrl()

                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
            scenarioNameForAgentAndWelsh in {
              implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(Some("1231.26"), Some("asd")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedOptionTupleAmountPageContents(
                  title = "Error: Pension payments",
                  header = "Pension payments",
                  caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection1 = ExpectedAmountSection("Amount before tax", "For example, £193.54", "1,231.26"),
                  amountSection2 = ExpectedAmountSection("Non-UK tax paid", "For example, £193.54", "asd"),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "Enter the amount of non-UK tax paid in the correct format",
                      link = "#amount-2"),

                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter the amount of non-UK tax paid in the correct format",
                      idOpt = Some("amount-2")
                    )
                  ),
                  formUrl = formUrl()

                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
          }
          "negative amount before tax with non uk tax paid" when {
            scenarioNameForIndividualAndEnglish in {
              implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
              implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(Some("-1"), Some("1233.41")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedOptionTupleAmountPageContents(
                  title = "Error: Pension payments",
                  header = "Pension payments",
                  caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection1 = ExpectedAmountSection("Amount before tax", "For example, £193.54", "-1"),
                  amountSection2 = ExpectedAmountSection("Non-UK tax paid", "For example, £193.54", "1,233.41"),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "Enter an amount before tax in the correct format",
                      link = "#amount-1"),

                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter an amount before tax in the correct format",
                      idOpt = Some("amount-1")
                    )
                  ),
                  formUrl = formUrl()

                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
            scenarioNameForIndividualAndWelsh in {
              implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(Some("-1"), Some("1233.41")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedOptionTupleAmountPageContents(
                  title = "Error: Pension payments",
                  header = "Pension payments",
                  caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection1 = ExpectedAmountSection("Amount before tax", "For example, £193.54", "-1"),
                  amountSection2 = ExpectedAmountSection("Non-UK tax paid", "For example, £193.54", "1,233.41"),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "Enter an amount before tax in the correct format",
                      link = "#amount-1"),

                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter an amount before tax in the correct format",
                      idOpt = Some("amount-1")
                    )
                  ),
                  formUrl = formUrl()

                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
            scenarioNameForAgentAndEnglish in {
              implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
              implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(Some("-1"), Some("1233.41")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedOptionTupleAmountPageContents(
                  title = "Error: Pension payments",
                  header = "Pension payments",
                  caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection1 = ExpectedAmountSection("Amount before tax", "For example, £193.54", "-1"),
                  amountSection2 = ExpectedAmountSection("Non-UK tax paid", "For example, £193.54", "1,233.41"),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "Enter the amount before tax in the correct format",
                      link = "#amount-1"),

                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter the amount before tax in the correct format",
                      idOpt = Some("amount-1")
                    )
                  ),
                  formUrl = formUrl()

                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
            scenarioNameForAgentAndWelsh in {
              implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(Some("-1"), Some("1233.41")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedOptionTupleAmountPageContents(
                  title = "Error: Pension payments",
                  header = "Pension payments",
                  caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection1 = ExpectedAmountSection("Amount before tax", "For example, £193.54", "-1"),
                  amountSection2 = ExpectedAmountSection("Non-UK tax paid", "For example, £193.54", "1,233.41"),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "Enter the amount before tax in the correct format",
                      link = "#amount-1"),

                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter the amount before tax in the correct format",
                      idOpt = Some("amount-1")
                    )
                  ),
                  formUrl = formUrl()

                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
          }
          "negative non uk tax paid with amount before tax" when {
            scenarioNameForIndividualAndEnglish in {
              implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
              implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(Some("1231.26"), Some("-1")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedOptionTupleAmountPageContents(
                  title = "Error: Pension payments",
                  header = "Pension payments",
                  caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection1 = ExpectedAmountSection("Amount before tax", "For example, £193.54", "1,231.26"),
                  amountSection2 = ExpectedAmountSection("Non-UK tax paid", "For example, £193.54", "-1"),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "Enter the amount of non-UK tax paid in the correct format",
                      link = "#amount-2"),

                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter the amount of non-UK tax paid in the correct format",
                      idOpt = Some("amount-2")
                    )
                  ),
                  formUrl = formUrl()

                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
            scenarioNameForIndividualAndWelsh in {
              implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(Some("1231.26"), Some("-1")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedOptionTupleAmountPageContents(
                  title = "Error: Pension payments",
                  header = "Pension payments",
                  caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection1 = ExpectedAmountSection("Amount before tax", "For example, £193.54", "1,231.26"),
                  amountSection2 = ExpectedAmountSection("Non-UK tax paid", "For example, £193.54", "-1"),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "Enter the amount of non-UK tax paid in the correct format",
                      link = "#amount-2"),

                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter the amount of non-UK tax paid in the correct format",
                      idOpt = Some("amount-2")
                    )
                  ),
                  formUrl = formUrl()

                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
            scenarioNameForAgentAndEnglish in {
              implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
              implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(Some("1231.26"), Some("-1")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedOptionTupleAmountPageContents(
                  title = "Error: Pension payments",
                  header = "Pension payments",
                  caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection1 = ExpectedAmountSection("Amount before tax", "For example, £193.54", "1,231.26"),
                  amountSection2 = ExpectedAmountSection("Non-UK tax paid", "For example, £193.54", "-1"),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "Enter the amount of non-UK tax paid in the correct format",
                      link = "#amount-2"),

                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter the amount of non-UK tax paid in the correct format",
                      idOpt = Some("amount-2")
                    )
                  ),
                  formUrl = formUrl()

                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
            scenarioNameForAgentAndWelsh in {
              implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(Some("1231.26"), Some("-1")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedOptionTupleAmountPageContents(
                  title = "Error: Pension payments",
                  header = "Pension payments",
                  caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection1 = ExpectedAmountSection("Amount before tax", "For example, £193.54", "1,231.26"),
                  amountSection2 = ExpectedAmountSection("Non-UK tax paid", "For example, £193.54", "-1"),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "Enter the amount of non-UK tax paid in the correct format",
                      link = "#amount-2"),

                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter the amount of non-UK tax paid in the correct format",
                      idOpt = Some("amount-2")
                    )
                  ),
                  formUrl = formUrl()

                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
          }
          "too big amount before tax with non uk tax paid" when {
            scenarioNameForIndividualAndEnglish in {
              implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
              implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(Some("1000000000001"), Some("1233.41")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedOptionTupleAmountPageContents(
                  title = "Error: Pension payments",
                  header = "Pension payments",
                  caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection1 = ExpectedAmountSection("Amount before tax", "For example, £193.54", "1000000000001"),
                  amountSection2 = ExpectedAmountSection("Non-UK tax paid", "For example, £193.54", "1,233.41"),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "The amount before tax must be less than £100,000,000,000",
                      link = "#amount-1"),

                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: The amount before tax must be less than £100,000,000,000",
                      idOpt = Some("amount-1")
                    )
                  ),
                  formUrl = formUrl()

                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
            scenarioNameForIndividualAndWelsh in {
              implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(Some("1000000000001"), Some("1233.41")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedOptionTupleAmountPageContents(
                  title = "Error: Pension payments",
                  header = "Pension payments",
                  caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection1 = ExpectedAmountSection("Amount before tax", "For example, £193.54", "1000000000001"),
                  amountSection2 = ExpectedAmountSection("Non-UK tax paid", "For example, £193.54", "1,233.41"),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "The amount before tax must be less than £100,000,000,000",
                      link = "#amount-1"),

                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: The amount before tax must be less than £100,000,000,000",
                      idOpt = Some("amount-1")
                    )
                  ),
                  formUrl = formUrl()

                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
            scenarioNameForAgentAndEnglish in {
              implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
              implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(Some("1000000000001"), Some("1233.41")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedOptionTupleAmountPageContents(
                  title = "Error: Pension payments",
                  header = "Pension payments",
                  caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection1 = ExpectedAmountSection("Amount before tax", "For example, £193.54", "1000000000001"),
                  amountSection2 = ExpectedAmountSection("Non-UK tax paid", "For example, £193.54", "1,233.41"),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "The amount before tax must be less than £100,000,000,000",
                      link = "#amount-1"),

                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: The amount before tax must be less than £100,000,000,000",
                      idOpt = Some("amount-1")
                    )
                  ),
                  formUrl = formUrl()

                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
            scenarioNameForAgentAndWelsh in {
              implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(Some("1000000000001"), Some("1233.41")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedOptionTupleAmountPageContents(
                  title = "Error: Pension payments",
                  header = "Pension payments",
                  caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection1 = ExpectedAmountSection("Amount before tax", "For example, £193.54", "1000000000001"),
                  amountSection2 = ExpectedAmountSection("Non-UK tax paid", "For example, £193.54", "1,233.41"),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "The amount before tax must be less than £100,000,000,000",
                      link = "#amount-1"),

                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: The amount before tax must be less than £100,000,000,000",
                      idOpt = Some("amount-1")
                    )
                  ),
                  formUrl = formUrl()

                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
          }
          "too big non uk tax paid with amount before tax" when {
            scenarioNameForIndividualAndEnglish in {
              implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
              implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(Some("1233.41"), Some("1000000000001")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedOptionTupleAmountPageContents(
                  title = "Error: Pension payments",
                  header = "Pension payments",
                  caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection1 = ExpectedAmountSection("Amount before tax", "For example, £193.54", "1,233.41"),
                  amountSection2 = ExpectedAmountSection("Non-UK tax paid", "For example, £193.54", "1000000000001"),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "The amount of non-UK tax paid must be less than £100,000,000,000",
                      link = "#amount-2"),

                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: The amount of non-UK tax paid must be less than £100,000,000,000",
                      idOpt = Some("amount-2")
                    )
                  ),
                  formUrl = formUrl()

                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
            scenarioNameForIndividualAndWelsh in {
              implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(Some("1233.41"), Some("1000000000001")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedOptionTupleAmountPageContents(
                  title = "Error: Pension payments",
                  header = "Pension payments",
                  caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection1 = ExpectedAmountSection("Amount before tax", "For example, £193.54", "1,233.41"),
                  amountSection2 = ExpectedAmountSection("Non-UK tax paid", "For example, £193.54", "1000000000001"),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "The amount of non-UK tax paid must be less than £100,000,000,000",
                      link = "#amount-2"),

                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: The amount of non-UK tax paid must be less than £100,000,000,000",
                      idOpt = Some("amount-2")
                    )
                  ),
                  formUrl = formUrl()

                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
            scenarioNameForAgentAndEnglish in {
              implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
              implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(Some("1233.41"), Some("1000000000001")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedOptionTupleAmountPageContents(
                  title = "Error: Pension payments",
                  header = "Pension payments",
                  caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection1 = ExpectedAmountSection("Amount before tax", "For example, £193.54", "1,233.41"),
                  amountSection2 = ExpectedAmountSection("Non-UK tax paid", "For example, £193.54", "1000000000001"),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "The amount of non-UK tax paid must be less than £100,000,000,000",
                      link = "#amount-2"),

                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: The amount of non-UK tax paid must be less than £100,000,000,000",
                      idOpt = Some("amount-2")
                    )
                  ),
                  formUrl = formUrl()

                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
            scenarioNameForAgentAndWelsh in {
              implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForOptionTupleAmountPage(Some("1233.41"), Some("1000000000001")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedOptionTupleAmountPageContents(
                  title = "Error: Pension payments",
                  header = "Pension payments",
                  caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection1 = ExpectedAmountSection("Amount before tax", "For example, £193.54", "1,233.41"),
                  amountSection2 = ExpectedAmountSection("Non-UK tax paid", "For example, £193.54", "1000000000001"),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "The amount of non-UK tax paid must be less than £100,000,000,000",
                      link = "#amount-2"),

                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: The amount of non-UK tax paid must be less than £100,000,000,000",
                      idOpt = Some("amount-2")
                    )
                  ),
                  formUrl = formUrl()

                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
          }
        }
      }
    }
  }

  private def getViewModel(implicit userConfig: UserConfig): Option[IncomeFromOverseasPensionsViewModel] =
    loadPensionUserData.map(_.pensions.incomeFromOverseasPensions)


  private def getPageWithIndex(index: Int = 0)(implicit userConfig: UserConfig, wsClient: WSClient): WSResponse = {
    getPage(getMap(index))
  }

  private def formUrl(index: Int = 0): Option[String] =
    Some(relativeUrlForThisPage + "?index=" + index)

  private def submitFormWithIndex(submittedFormData: SubmittedFormData, index: Int = 0)(implicit userConfig: UserConfig, wsClient: WSClient): WSResponse = {
    submitForm(submittedFormData, getMap(index))
  }

  private def getMap(index: Int): Map[String, String] = {
    Map("index" -> index.toString)
  }
}
