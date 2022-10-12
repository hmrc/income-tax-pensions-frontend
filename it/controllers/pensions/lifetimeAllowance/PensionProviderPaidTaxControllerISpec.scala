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

package controllers.pensions.lifetimeAllowance

import builders.PensionsCYAModelBuilder.{aPensionsCYAEmptyModel, aPensionsCYAModel}
import controllers.ControllerSpec.PreferredLanguages.{English, Welsh}
import controllers.ControllerSpec.UserTypes.{Agent, Individual}
import controllers.ControllerSpec._
import controllers.YesNoAmountControllerSpec
import models.mongo.PensionsUserData
import models.pension.charges.PensionAnnualAllowancesViewModel
import org.jsoup.Jsoup.parse
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}

class PensionProviderPaidTaxControllerISpec
  extends YesNoAmountControllerSpec("/annual-lifetime-allowances/pension-provider-paid-tax") {

  "This page" when {
    "requested to be shown" should {
      "redirect to the summary page" when {
        "the user has no stored session data at all" in {

          val response = getPage(UserConfig(Individual, English, None))

          response must haveStatus(SEE_OTHER)
          response must haveALocationHeaderValue(relativeUrlForPensionSummaryPage)

        }
      }
      "appear as expected" when {
        "the user has no relevant session data and" when {

          val sessionData = pensionsUserData(aPensionsCYAEmptyModel)

          scenarioNameForIndividualAndEnglish in {

            val response = getPage(UserConfig(Individual, English, Some(sessionData)))

            response must haveStatus(OK)
            assertPageAsExpected(
              parse(response.body),
              ExpectedYesNoAmountPageContents(
                title = "Did your pension schemes pay or agree to pay the tax?",
                header = "Did your pension schemes pay or agree to pay the tax?",
                caption = "Annual and lifetime allowances for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButtonForContinue("Continue", ""),
                amountSection = ExpectedAmountSection("Amount they paid or agreed to pay, in pounds", "For example, £193.52", "")
              ),
              English)

          }
          scenarioNameForIndividualAndWelsh in {

            val response = getPage(UserConfig(Individual, Welsh, Some(sessionData)))

            response must haveStatus(OK)
            assertPageAsExpected(
              parse(response.body),
              ExpectedYesNoAmountPageContents(
                title = "Did your pension schemes pay or agree to pay the tax?",
                header = "Did your pension schemes pay or agree to pay the tax?",
                caption = "Annual and lifetime allowances for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButtonForContinue("Continue", ""),
                amountSection = ExpectedAmountSection("Amount they paid or agreed to pay, in pounds", "For example, £193.52", "")
              ),
              Welsh)

          }
          scenarioNameForAgentAndEnglish in {

            val response = getPage(UserConfig(Agent, English, Some(sessionData)))

            response must haveStatus(OK)
            assertPageAsExpected(
              parse(response.body),
              ExpectedYesNoAmountPageContents(
                title = "Did your client’s pension schemes pay or agree to pay the tax?",
                header = "Did your client’s pension schemes pay or agree to pay the tax?",
                caption = "Annual and lifetime allowances for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButtonForContinue("Continue", ""),
                amountSection = ExpectedAmountSection("Amount they paid or agreed to pay, in pounds", "For example, £193.52", "")
              ),
              English)

          }
          scenarioNameForAgentAndWelsh in {

            val response = getPage(UserConfig(Agent, Welsh, Some(sessionData)))

            response must haveStatus(OK)
            assertPageAsExpected(
              parse(response.body),
              ExpectedYesNoAmountPageContents(
                title = "Did your client’s pension schemes pay or agree to pay the tax?",
                header = "Did your client’s pension schemes pay or agree to pay the tax?",
                caption = "Annual and lifetime allowances for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButtonForContinue("Continue", ""),
                amountSection = ExpectedAmountSection("Amount they paid or agreed to pay, in pounds", "For example, £193.52", "")
              ),
              Welsh)

          }
        }
        "the user had previously answered 'Yes' with a valid amount, and" when {

          val sessionData: PensionsUserData =
            pensionsUserData(aPensionsCYAModel.copy(pensionsAnnualAllowances =
              PensionAnnualAllowancesViewModel(
                pensionProvidePaidAnnualAllowanceQuestion = Some(true),
                taxPaidByPensionProvider = Some(42.64))
            ))

          scenarioNameForIndividualAndEnglish in {

            val response = getPage(UserConfig(Individual, English, Some(sessionData)))

            response must haveStatus(OK)
            assertPageAsExpected(
              parse(response.body),
              ExpectedYesNoAmountPageContents(
                title = "Did your pension schemes pay or agree to pay the tax?",
                header = "Did your pension schemes pay or agree to pay the tax?",
                caption = "Annual and lifetime allowances for 6 April 2021 to 5 April 2022",
                radioButtonForYes = checkedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButtonForContinue("Continue", ""),
                amountSection = ExpectedAmountSection("Amount they paid or agreed to pay, in pounds", "For example, £193.52", "42.64")
              ),
              English)

          }
          scenarioNameForIndividualAndWelsh in {

            val response = getPage(UserConfig(Individual, Welsh, Some(sessionData)))

            response must haveStatus(OK)
            assertPageAsExpected(
              parse(response.body),
              ExpectedYesNoAmountPageContents(
                title = "Did your pension schemes pay or agree to pay the tax?",
                header = "Did your pension schemes pay or agree to pay the tax?",
                caption = "Annual and lifetime allowances for 6 April 2021 to 5 April 2022",
                radioButtonForYes = checkedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButtonForContinue("Continue", ""),
                amountSection = ExpectedAmountSection("Amount they paid or agreed to pay, in pounds", "For example, £193.52", "42.64")
              ),
              Welsh)

          }
          scenarioNameForAgentAndEnglish in {

            val response = getPage(UserConfig(Agent, English, Some(sessionData)))

            response must haveStatus(OK)
            assertPageAsExpected(
              parse(response.body),
              ExpectedYesNoAmountPageContents(
                title = "Did your client’s pension schemes pay or agree to pay the tax?",
                header = "Did your client’s pension schemes pay or agree to pay the tax?",
                caption = "Annual and lifetime allowances for 6 April 2021 to 5 April 2022",
                radioButtonForYes = checkedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButtonForContinue("Continue", ""),
                amountSection = ExpectedAmountSection("Amount they paid or agreed to pay, in pounds", "For example, £193.52", "42.64")
              ),
              English)

          }
          scenarioNameForAgentAndWelsh in {

            val response = getPage(UserConfig(Agent, Welsh, Some(sessionData)))

            response must haveStatus(OK)
            assertPageAsExpected(
              parse(response.body),
              ExpectedYesNoAmountPageContents(
                title = "Did your client’s pension schemes pay or agree to pay the tax?",
                header = "Did your client’s pension schemes pay or agree to pay the tax?",
                caption = "Annual and lifetime allowances for 6 April 2021 to 5 April 2022",
                radioButtonForYes = checkedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButtonForContinue("Continue", ""),
                amountSection = ExpectedAmountSection("Amount they paid or agreed to pay, in pounds", "For example, £193.52", "42.64")
              ),
              Welsh)

          }

        }
        "the user had previously answered 'No' without an amount, and" when {

          val sessionData: PensionsUserData =
            pensionsUserData(aPensionsCYAModel.copy(pensionsAnnualAllowances =
              PensionAnnualAllowancesViewModel(
                pensionProvidePaidAnnualAllowanceQuestion = Some(false),
                taxPaidByPensionProvider = None)
            ))

          scenarioNameForIndividualAndEnglish in {

            val response = getPage(UserConfig(Individual, English, Some(sessionData)))

            response must haveStatus(OK)
            assertPageAsExpected(
              parse(response.body),
              ExpectedYesNoAmountPageContents(
                title = "Did your pension schemes pay or agree to pay the tax?",
                header = "Did your pension schemes pay or agree to pay the tax?",
                caption = "Annual and lifetime allowances for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = checkedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButtonForContinue("Continue", ""),
                amountSection = ExpectedAmountSection("Amount they paid or agreed to pay, in pounds", "For example, £193.52", "")
              ),
              English)

          }
          scenarioNameForIndividualAndWelsh in {

            val response = getPage(UserConfig(Individual, Welsh, Some(sessionData)))

            response must haveStatus(OK)
            assertPageAsExpected(
              parse(response.body),
              ExpectedYesNoAmountPageContents(
                title = "Did your pension schemes pay or agree to pay the tax?",
                header = "Did your pension schemes pay or agree to pay the tax?",
                caption = "Annual and lifetime allowances for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = checkedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButtonForContinue("Continue", ""),
                amountSection = ExpectedAmountSection("Amount they paid or agreed to pay, in pounds", "For example, £193.52", "")
              ),
              Welsh)

          }
          scenarioNameForAgentAndEnglish in {

            val response = getPage(UserConfig(Agent, English, Some(sessionData)))

            response must haveStatus(OK)
            assertPageAsExpected(
              parse(response.body),
              ExpectedYesNoAmountPageContents(
                title = "Did your client’s pension schemes pay or agree to pay the tax?",
                header = "Did your client’s pension schemes pay or agree to pay the tax?",
                caption = "Annual and lifetime allowances for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = checkedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButtonForContinue("Continue", ""),
                amountSection = ExpectedAmountSection("Amount they paid or agreed to pay, in pounds", "For example, £193.52", "")
              ),
              English)

          }
          scenarioNameForAgentAndWelsh in {

            val response = getPage(UserConfig(Agent, Welsh, Some(sessionData)))

            response must haveStatus(OK)
            assertPageAsExpected(
              parse(response.body),
              ExpectedYesNoAmountPageContents(
                title = "Did your client’s pension schemes pay or agree to pay the tax?",
                header = "Did your client’s pension schemes pay or agree to pay the tax?",
                caption = "Annual and lifetime allowances for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = checkedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButtonForContinue("Continue", ""),
                amountSection = ExpectedAmountSection("Amount they paid or agreed to pay, in pounds", "For example, £193.52", "")
              ),
              Welsh)

          }

        }
      }
    }
    "submitted" should {
      "succeed" when {
        "the user has relevant session data and" when {

          val sessionData = pensionsUserData(aPensionsCYAModel)

          "the user has selected 'No' and" when {

            val expectedViewModel =
              sessionData.pensions.pensionsAnnualAllowances.copy(
                pensionProvidePaidAnnualAllowanceQuestion = Some(false),
                taxPaidByPensionProvider = None
              )

            scenarioNameForIndividualAndEnglish in {

              val response = submitForm(UserConfig(Individual, English, Some(sessionData)), SubmittedFormDataForYesNoAmountPage(Some(false), None))

              response must haveStatus(SEE_OTHER)
              response must haveALocationHeaderValue(relativeUrlForThisPage)
              getViewModel(sessionData) mustBe expectedViewModel

            }
            scenarioNameForIndividualAndWelsh in {

              val response = submitForm(UserConfig(Individual, Welsh, Some(sessionData)), SubmittedFormDataForYesNoAmountPage(Some(false), None))

              response must haveStatus(SEE_OTHER)
              response must haveALocationHeaderValue(relativeUrlForThisPage)
              getViewModel(sessionData) mustBe expectedViewModel

            }
            scenarioNameForAgentAndEnglish in {

              val response = submitForm(UserConfig(Agent, English, Some(sessionData)), SubmittedFormDataForYesNoAmountPage(Some(false), None))

              response must haveStatus(SEE_OTHER)
              response must haveALocationHeaderValue(relativeUrlForThisPage)
              getViewModel(sessionData) mustBe expectedViewModel

            }
            scenarioNameForAgentAndWelsh in {

              val response = submitForm(UserConfig(Agent, Welsh, Some(sessionData)), SubmittedFormDataForYesNoAmountPage(Some(false), None))

              response must haveStatus(SEE_OTHER)
              response must haveALocationHeaderValue(relativeUrlForThisPage)
              getViewModel(sessionData) mustBe expectedViewModel

            }
          }
          "the user has selected 'Yes' as well as a valid amount (unformatted), and" when {

            val expectedViewModel =
              sessionData.pensions.pensionsAnnualAllowances.copy(
                pensionProvidePaidAnnualAllowanceQuestion = Some(true),
                taxPaidByPensionProvider = Some(BigDecimal(42.64))
              )

            scenarioNameForIndividualAndEnglish in {

              val response = submitForm(UserConfig(Individual, English, Some(sessionData)), SubmittedFormDataForYesNoAmountPage(Some(true), Some("42.64")))

              response must haveStatus(SEE_OTHER)
              response must haveALocationHeaderValue(relativeUrlForThisPage)
              getViewModel(sessionData) mustBe expectedViewModel

            }
            scenarioNameForIndividualAndWelsh in {

              val response = submitForm(UserConfig(Individual, Welsh, Some(sessionData)), SubmittedFormDataForYesNoAmountPage(Some(true), Some("42.64")))

              response must haveStatus(SEE_OTHER)
              response must haveALocationHeaderValue(relativeUrlForThisPage)
              getViewModel(sessionData) mustBe expectedViewModel

            }
            scenarioNameForAgentAndEnglish in {

              val response = submitForm(UserConfig(Agent, English, Some(sessionData)), SubmittedFormDataForYesNoAmountPage(Some(true), Some("42.64")))

              response must haveStatus(SEE_OTHER)
              response must haveALocationHeaderValue(relativeUrlForThisPage)
              getViewModel(sessionData) mustBe expectedViewModel

            }
            scenarioNameForAgentAndWelsh in {

              val response = submitForm(UserConfig(Agent, Welsh, Some(sessionData)), SubmittedFormDataForYesNoAmountPage(Some(true), Some("42.64")))

              response must haveStatus(SEE_OTHER)
              response must haveALocationHeaderValue(relativeUrlForThisPage)
              getViewModel(sessionData) mustBe expectedViewModel

            }
          }
          "the user has selected 'Yes' as well as a valid amount (formatted), and" when {

            val expectedViewModel =
              sessionData.pensions.pensionsAnnualAllowances.copy(
                pensionProvidePaidAnnualAllowanceQuestion = Some(true),
                taxPaidByPensionProvider = Some(BigDecimal(1042.64))
              )

            scenarioNameForIndividualAndEnglish in {

              val response = submitForm(UserConfig(Individual, English, Some(sessionData)), SubmittedFormDataForYesNoAmountPage(Some(true), Some("£1,042.64")))

              response must haveStatus(SEE_OTHER)
              response must haveALocationHeaderValue(relativeUrlForThisPage)
              getViewModel(sessionData) mustBe expectedViewModel

            }
            scenarioNameForIndividualAndWelsh in {

              val response = submitForm(UserConfig(Individual, Welsh, Some(sessionData)), SubmittedFormDataForYesNoAmountPage(Some(true), Some("£1,042.64")))

              response must haveStatus(SEE_OTHER)
              response must haveALocationHeaderValue(relativeUrlForThisPage)
              getViewModel(sessionData) mustBe expectedViewModel

            }
            scenarioNameForAgentAndEnglish in {

              val response = submitForm(UserConfig(Agent, English, Some(sessionData)), SubmittedFormDataForYesNoAmountPage(Some(true), Some("£1,042.64")))

              response must haveStatus(SEE_OTHER)
              response must haveALocationHeaderValue(relativeUrlForThisPage)
              getViewModel(sessionData) mustBe expectedViewModel

            }
            scenarioNameForAgentAndWelsh in {

              val response = submitForm(UserConfig(Agent, Welsh, Some(sessionData)), SubmittedFormDataForYesNoAmountPage(Some(true), Some("£1,042.64")))

              response must haveStatus(SEE_OTHER)
              response must haveALocationHeaderValue(relativeUrlForThisPage)
              getViewModel(sessionData) mustBe expectedViewModel

            }
          }

        }
      }
      "fail" when {
        "the user has relevant session data and" when {

          val sessionData = pensionsUserData(aPensionsCYAModel)

          "the user has selected neither 'Yes' nor 'No' and" when {
            scenarioNameForIndividualAndEnglish in {

              val response = submitForm(UserConfig(Individual, English, Some(sessionData)), SubmittedFormDataForYesNoAmountPage(None, None))

              response must haveStatus(BAD_REQUEST)
              assertPageAsExpected(
                parse(response.body),
                ExpectedYesNoAmountPageContents(
                  title = "Error: Did your pension schemes pay or agree to pay the tax?",
                  header = "Did your pension schemes pay or agree to pay the tax?",
                  caption = "Annual and lifetime allowances for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButtonForContinue("Continue", ""),
                  amountSection = ExpectedAmountSection("Amount they paid or agreed to pay, in pounds", "For example, £193.52", ""),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "Select yes if your pension provider paid or agreed to pay your annual allowance tax",
                      link = "#value")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Select yes if your pension provider paid or agreed to pay your annual allowance tax",
                      idOpt = Some("value")
                    )
                  )
                ),
                English)

            }
            scenarioNameForIndividualAndWelsh in {

              val response = submitForm(UserConfig(Individual, Welsh, Some(sessionData)), SubmittedFormDataForYesNoAmountPage(None, None))

              response must haveStatus(BAD_REQUEST)
              assertPageAsExpected(
                parse(response.body),
                ExpectedYesNoAmountPageContents(
                  title = "Error: Did your pension schemes pay or agree to pay the tax?",
                  header = "Did your pension schemes pay or agree to pay the tax?",
                  caption = "Annual and lifetime allowances for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButtonForContinue("Continue", ""),
                  amountSection = ExpectedAmountSection("Amount they paid or agreed to pay, in pounds", "For example, £193.52", ""),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "Select yes if your pension provider paid or agreed to pay your annual allowance tax",
                      link = "#value")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Select yes if your pension provider paid or agreed to pay your annual allowance tax",
                      idOpt = Some("value")
                    )
                  )
                ),
                Welsh)

            }
            scenarioNameForAgentAndEnglish in {

              val response = submitForm(UserConfig(Agent, English, Some(sessionData)), SubmittedFormDataForYesNoAmountPage(None, None))

              response must haveStatus(BAD_REQUEST)
              assertPageAsExpected(
                parse(response.body),
                ExpectedYesNoAmountPageContents(
                  title = "Error: Did your client’s pension schemes pay or agree to pay the tax?",
                  header = "Did your client’s pension schemes pay or agree to pay the tax?",
                  caption = "Annual and lifetime allowances for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButtonForContinue("Continue", ""),
                  amountSection = ExpectedAmountSection("Amount they paid or agreed to pay, in pounds", "For example, £193.52", ""),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "Select yes if your client’s pension provider paid or agreed to pay the annual allowance tax",
                      link = "#value")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Select yes if your client’s pension provider paid or agreed to pay the annual allowance tax",
                      idOpt = Some("value")
                    )
                  )
                ),
                English)

            }
            scenarioNameForAgentAndWelsh in {

              val response = submitForm(UserConfig(Agent, Welsh, Some(sessionData)), SubmittedFormDataForYesNoAmountPage(None, None))

              response must haveStatus(BAD_REQUEST)
              assertPageAsExpected(
                parse(response.body),
                ExpectedYesNoAmountPageContents(
                  title = "Error: Did your client’s pension schemes pay or agree to pay the tax?",
                  header = "Did your client’s pension schemes pay or agree to pay the tax?",
                  caption = "Annual and lifetime allowances for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButtonForContinue("Continue", ""),
                  amountSection = ExpectedAmountSection("Amount they paid or agreed to pay, in pounds", "For example, £193.52", ""),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "Select yes if your client’s pension provider paid or agreed to pay the annual allowance tax",
                      link = "#value")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Select yes if your client’s pension provider paid or agreed to pay the annual allowance tax",
                      idOpt = Some("value")
                    )
                  )
                ),
                Welsh)

            }
          }
          "the user has selected 'Yes' but have not provided an amount, and" when {
            scenarioNameForIndividualAndEnglish in {

              val response = submitForm(UserConfig(Individual, English, Some(sessionData)), SubmittedFormDataForYesNoAmountPage(Some(true), None))

              response must haveStatus(BAD_REQUEST)
              assertPageAsExpected(
                parse(response.body),
                ExpectedYesNoAmountPageContents(
                  title = "Error: Did your pension schemes pay or agree to pay the tax?",
                  header = "Did your pension schemes pay or agree to pay the tax?",
                  caption = "Annual and lifetime allowances for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButtonForContinue("Continue", ""),
                  amountSection = ExpectedAmountSection("Amount they paid or agreed to pay, in pounds", "For example, £193.52", ""),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "Enter the amount of tax your pension provider paid or agreed to pay",
                      link = "#amount-2")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter the amount of tax your pension provider paid or agreed to pay",
                      idOpt = Some("amount-2")
                    )
                  )
                ),
                English)

            }
            scenarioNameForIndividualAndWelsh in {

              val response = submitForm(UserConfig(Individual, Welsh, Some(sessionData)), SubmittedFormDataForYesNoAmountPage(Some(true), None))

              response must haveStatus(BAD_REQUEST)
              assertPageAsExpected(
                parse(response.body),
                ExpectedYesNoAmountPageContents(
                  title = "Error: Did your pension schemes pay or agree to pay the tax?",
                  header = "Did your pension schemes pay or agree to pay the tax?",
                  caption = "Annual and lifetime allowances for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButtonForContinue("Continue", ""),
                  amountSection = ExpectedAmountSection("Amount they paid or agreed to pay, in pounds", "For example, £193.52", ""),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "Enter the amount of tax your pension provider paid or agreed to pay",
                      link = "#amount-2")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter the amount of tax your pension provider paid or agreed to pay",
                      idOpt = Some("amount-2")
                    )
                  )
                ),
                Welsh)

            }
            scenarioNameForAgentAndEnglish in {

              val response = submitForm(UserConfig(Agent, English, Some(sessionData)), SubmittedFormDataForYesNoAmountPage(Some(true), None))

              response must haveStatus(BAD_REQUEST)
              assertPageAsExpected(
                parse(response.body),
                ExpectedYesNoAmountPageContents(
                  title = "Error: Did your client’s pension schemes pay or agree to pay the tax?",
                  header = "Did your client’s pension schemes pay or agree to pay the tax?",
                  caption = "Annual and lifetime allowances for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButtonForContinue("Continue", ""),
                  amountSection = ExpectedAmountSection("Amount they paid or agreed to pay, in pounds", "For example, £193.52", ""),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "Enter the amount of tax your client’s pension provider paid or agreed to pay",
                      link = "#amount-2")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter the amount of tax your client’s pension provider paid or agreed to pay",
                      idOpt = Some("amount-2")
                    )
                  )
                ),
                English)

            }
            scenarioNameForAgentAndWelsh in {

              val response = submitForm(UserConfig(Agent, Welsh, Some(sessionData)), SubmittedFormDataForYesNoAmountPage(Some(true), None))

              response must haveStatus(BAD_REQUEST)
              assertPageAsExpected(
                parse(response.body),
                ExpectedYesNoAmountPageContents(
                  title = "Error: Did your client’s pension schemes pay or agree to pay the tax?",
                  header = "Did your client’s pension schemes pay or agree to pay the tax?",
                  caption = "Annual and lifetime allowances for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButtonForContinue("Continue", ""),
                  amountSection = ExpectedAmountSection("Amount they paid or agreed to pay, in pounds", "For example, £193.52", ""),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "Enter the amount of tax your client’s pension provider paid or agreed to pay",
                      link = "#amount-2")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter the amount of tax your client’s pension provider paid or agreed to pay",
                      idOpt = Some("amount-2")
                    )
                  )
                ),
                Welsh)

            }
          }
          "the user has selected 'Yes' but has provided an amount of an invalid format, and" when {
            scenarioNameForIndividualAndEnglish in {

              val response = submitForm(UserConfig(Individual, English, Some(sessionData)), SubmittedFormDataForYesNoAmountPage(Some(true), Some("x2.64")))

              response must haveStatus(BAD_REQUEST)
              assertPageAsExpected(
                parse(response.body),
                ExpectedYesNoAmountPageContents(
                  title = "Error: Did your pension schemes pay or agree to pay the tax?",
                  header = "Did your pension schemes pay or agree to pay the tax?",
                  caption = "Annual and lifetime allowances for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButtonForContinue("Continue", ""),
                  amountSection = ExpectedAmountSection("Amount they paid or agreed to pay, in pounds", "For example, £193.52", "x2.64"),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "Enter the amount of tax your pension provider paid or agreed to pay in the correct format",
                      link = "#amount-2")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter the amount of tax your pension provider paid or agreed to pay in the correct format",
                      idOpt = Some("amount-2")
                    )
                  )
                ),
                English)

            }
            scenarioNameForIndividualAndWelsh in {

              val response = submitForm(UserConfig(Individual, Welsh, Some(sessionData)), SubmittedFormDataForYesNoAmountPage(Some(true), Some("x2.64")))

              response must haveStatus(BAD_REQUEST)
              assertPageAsExpected(
                parse(response.body),
                ExpectedYesNoAmountPageContents(
                  title = "Error: Did your pension schemes pay or agree to pay the tax?",
                  header = "Did your pension schemes pay or agree to pay the tax?",
                  caption = "Annual and lifetime allowances for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButtonForContinue("Continue", ""),
                  amountSection = ExpectedAmountSection("Amount they paid or agreed to pay, in pounds", "For example, £193.52", "x2.64"),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "Enter the amount of tax your pension provider paid or agreed to pay in the correct format",
                      link = "#amount-2")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter the amount of tax your pension provider paid or agreed to pay in the correct format",
                      idOpt = Some("amount-2")
                    )
                  )
                ),
                Welsh)

            }
            scenarioNameForAgentAndEnglish in {

              val response = submitForm(UserConfig(Agent, English, Some(sessionData)), SubmittedFormDataForYesNoAmountPage(Some(true), Some("x2.64")))

              response must haveStatus(BAD_REQUEST)
              assertPageAsExpected(
                parse(response.body),
                ExpectedYesNoAmountPageContents(
                  title = "Error: Did your client’s pension schemes pay or agree to pay the tax?",
                  header = "Did your client’s pension schemes pay or agree to pay the tax?",
                  caption = "Annual and lifetime allowances for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButtonForContinue("Continue", ""),
                  amountSection = ExpectedAmountSection("Amount they paid or agreed to pay, in pounds", "For example, £193.52", "x2.64"),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "Enter the amount of tax your client’s pension provider paid or agreed to pay in the correct format",
                      link = "#amount-2")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter the amount of tax your client’s pension provider paid or agreed to pay in the correct format",
                      idOpt = Some("amount-2")
                    )
                  )
                ),
                English)

            }
            scenarioNameForAgentAndWelsh in {

              val response = submitForm(UserConfig(Agent, Welsh, Some(sessionData)), SubmittedFormDataForYesNoAmountPage(Some(true), Some("x2.64")))

              response must haveStatus(BAD_REQUEST)
              assertPageAsExpected(
                parse(response.body),
                ExpectedYesNoAmountPageContents(
                  title = "Error: Did your client’s pension schemes pay or agree to pay the tax?",
                  header = "Did your client’s pension schemes pay or agree to pay the tax?",
                  caption = "Annual and lifetime allowances for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButtonForContinue("Continue", ""),
                  amountSection = ExpectedAmountSection("Amount they paid or agreed to pay, in pounds", "For example, £193.52", "x2.64"),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "Enter the amount of tax your client’s pension provider paid or agreed to pay in the correct format",
                      link = "#amount-2")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter the amount of tax your client’s pension provider paid or agreed to pay in the correct format",
                      idOpt = Some("amount-2")
                    )
                  )
                ),
                Welsh)

            }
          }
          "the user has selected 'Yes' but has provided an excessive amount, and" when {
            scenarioNameForIndividualAndEnglish in {

              val response = submitForm(UserConfig(Individual, English, Some(sessionData)), SubmittedFormDataForYesNoAmountPage(Some(true), Some("100000000002")))

              response must haveStatus(BAD_REQUEST)
              assertPageAsExpected(
                parse(response.body),
                ExpectedYesNoAmountPageContents(
                  title = "Error: Did your pension schemes pay or agree to pay the tax?",
                  header = "Did your pension schemes pay or agree to pay the tax?",
                  caption = "Annual and lifetime allowances for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButtonForContinue("Continue", ""),
                  amountSection = ExpectedAmountSection("Amount they paid or agreed to pay, in pounds", "For example, £193.52", "100000000002"),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "The amount of tax your pension provider paid or agreed to pay must be less than £100,000,000,000",
                      link = "#amount-2")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: The amount of tax your pension provider paid or agreed to pay must be less than £100,000,000,000",
                      idOpt = Some("amount-2")
                    )
                  )
                ),
                English)

            }
            scenarioNameForIndividualAndWelsh in {

              val response = submitForm(UserConfig(Individual, Welsh, Some(sessionData)), SubmittedFormDataForYesNoAmountPage(Some(true), Some("100000000002")))

              response must haveStatus(BAD_REQUEST)
              assertPageAsExpected(
                parse(response.body),
                ExpectedYesNoAmountPageContents(
                  title = "Error: Did your pension schemes pay or agree to pay the tax?",
                  header = "Did your pension schemes pay or agree to pay the tax?",
                  caption = "Annual and lifetime allowances for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButtonForContinue("Continue", ""),
                  amountSection = ExpectedAmountSection("Amount they paid or agreed to pay, in pounds", "For example, £193.52", "100000000002"),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "The amount of tax your pension provider paid or agreed to pay must be less than £100,000,000,000",
                      link = "#amount-2")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: The amount of tax your pension provider paid or agreed to pay must be less than £100,000,000,000",
                      idOpt = Some("amount-2")
                    )
                  )
                ),
                Welsh)

            }
            scenarioNameForAgentAndEnglish in {

              val response = submitForm(UserConfig(Agent, English, Some(sessionData)), SubmittedFormDataForYesNoAmountPage(Some(true), Some("100000000002")))

              response must haveStatus(BAD_REQUEST)
              assertPageAsExpected(
                parse(response.body),
                ExpectedYesNoAmountPageContents(
                  title = "Error: Did your client’s pension schemes pay or agree to pay the tax?",
                  header = "Did your client’s pension schemes pay or agree to pay the tax?",
                  caption = "Annual and lifetime allowances for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButtonForContinue("Continue", ""),
                  amountSection = ExpectedAmountSection("Amount they paid or agreed to pay, in pounds", "For example, £193.52", "100000000002"),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "The amount of tax your client’s pension provider paid or agreed to pay must be less than £100,000,000,000",
                      link = "#amount-2")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: The amount of tax your client’s pension provider paid or agreed to pay must be less than £100,000,000,000",
                      idOpt = Some("amount-2")
                    )
                  )
                ),
                English)

            }
            scenarioNameForAgentAndWelsh in {

              val response = submitForm(UserConfig(Agent, Welsh, Some(sessionData)), SubmittedFormDataForYesNoAmountPage(Some(true), Some("100000000002")))

              response must haveStatus(BAD_REQUEST)
              assertPageAsExpected(
                parse(response.body),
                ExpectedYesNoAmountPageContents(
                  title = "Error: Did your client’s pension schemes pay or agree to pay the tax?",
                  header = "Did your client’s pension schemes pay or agree to pay the tax?",
                  caption = "Annual and lifetime allowances for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButtonForContinue("Continue", ""),
                  amountSection = ExpectedAmountSection("Amount they paid or agreed to pay, in pounds", "For example, £193.52", "100000000002"),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "The amount of tax your client’s pension provider paid or agreed to pay must be less than £100,000,000,000",
                      link = "#amount-2")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: The amount of tax your client’s pension provider paid or agreed to pay must be less than £100,000,000,000",
                      idOpt = Some("amount-2")
                    )
                  )
                ),
                Welsh)

            }
          }
        }
      }
    }
  }

  private def getViewModel(pensionsUserData: PensionsUserData): PensionAnnualAllowancesViewModel
  = loadPensionUserData(pensionsUserData).pensions.pensionsAnnualAllowances


}



