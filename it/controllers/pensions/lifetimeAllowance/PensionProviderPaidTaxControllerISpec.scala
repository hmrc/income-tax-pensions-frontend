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
import controllers.PreferredLanguages.{English, Welsh}
import controllers.UserTypes.{Agent, Individual}
import controllers.{ControllerSpec, UserConfig}
import models.mongo.PensionsUserData
import models.pension.charges.PensionAnnualAllowancesViewModel
import org.jsoup.Jsoup.parse
import org.jsoup.nodes.Document
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.{WSClient, WSResponse}

class PensionProviderPaidTaxControllerISpec extends ControllerSpec {

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
              ExpectedPageContents(
                title = "Did your pension schemes pay or agree to pay the tax?",
                header = "Did your pension schemes pay or agree to pay the tax?",
                caption = "Annual and lifetime allowances for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButtonForContinue("Continue", ""),
                amountSection = ExpectedAmountSection("Amount they paid or agreed to pay, in pounds", "For example, £193.52", "")
              ))

          }
          scenarioNameForIndividualAndWelsh in {

            val response = getPage(UserConfig(Individual, Welsh, Some(sessionData)))

            response must haveStatus(OK)
            assertPageAsExpected(
              parse(response.body),
              ExpectedPageContents(
                title = "Did your pension schemes pay or agree to pay the tax?",
                header = "Did your pension schemes pay or agree to pay the tax?",
                caption = "Annual and lifetime allowances for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButtonForContinue("Continue", ""),
                amountSection = ExpectedAmountSection("Amount they paid or agreed to pay, in pounds", "For example, £193.52", "")
              ))

          }
          scenarioNameForAgentAndEnglish in {

            val response = getPage(UserConfig(Agent, English, Some(sessionData)))

            response must haveStatus(OK)
            assertPageAsExpected(
              parse(response.body),
              ExpectedPageContents(
                title = "Did your client’s pension schemes pay or agree to pay the tax?",
                header = "Did your client’s pension schemes pay or agree to pay the tax?",
                caption = "Annual and lifetime allowances for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButtonForContinue("Continue", ""),
                amountSection = ExpectedAmountSection("Amount they paid or agreed to pay, in pounds", "For example, £193.52", "")
              ))

          }
          scenarioNameForAgentAndWelsh in {

            val response = getPage(UserConfig(Agent, Welsh, Some(sessionData)))

            response must haveStatus(OK)
            assertPageAsExpected(
              parse(response.body),
              ExpectedPageContents(
                title = "Did your client’s pension schemes pay or agree to pay the tax?",
                header = "Did your client’s pension schemes pay or agree to pay the tax?",
                caption = "Annual and lifetime allowances for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButtonForContinue("Continue", ""),
                amountSection = ExpectedAmountSection("Amount they paid or agreed to pay, in pounds", "For example, £193.52", "")
              ))

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
              ExpectedPageContents(
                title = "Did your pension schemes pay or agree to pay the tax?",
                header = "Did your pension schemes pay or agree to pay the tax?",
                caption = "Annual and lifetime allowances for 6 April 2021 to 5 April 2022",
                radioButtonForYes = checkedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButtonForContinue("Continue", ""),
                amountSection = ExpectedAmountSection("Amount they paid or agreed to pay, in pounds", "For example, £193.52", "42.64")
              ))

          }
          scenarioNameForIndividualAndWelsh in {

            val response = getPage(UserConfig(Individual, Welsh, Some(sessionData)))

            response must haveStatus(OK)
            assertPageAsExpected(
              parse(response.body),
              ExpectedPageContents(
                title = "Did your pension schemes pay or agree to pay the tax?",
                header = "Did your pension schemes pay or agree to pay the tax?",
                caption = "Annual and lifetime allowances for 6 April 2021 to 5 April 2022",
                radioButtonForYes = checkedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButtonForContinue("Continue", ""),
                amountSection = ExpectedAmountSection("Amount they paid or agreed to pay, in pounds", "For example, £193.52", "42.64")
              ))

          }
          scenarioNameForAgentAndEnglish in {

            val response = getPage(UserConfig(Agent, English, Some(sessionData)))

            response must haveStatus(OK)
            assertPageAsExpected(
              parse(response.body),
              ExpectedPageContents(
                title = "Did your client’s pension schemes pay or agree to pay the tax?",
                header = "Did your client’s pension schemes pay or agree to pay the tax?",
                caption = "Annual and lifetime allowances for 6 April 2021 to 5 April 2022",
                radioButtonForYes = checkedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButtonForContinue("Continue", ""),
                amountSection = ExpectedAmountSection("Amount they paid or agreed to pay, in pounds", "For example, £193.52", "42.64")
              ))

          }
          scenarioNameForAgentAndWelsh in {

            val response = getPage(UserConfig(Agent, Welsh, Some(sessionData)))

            response must haveStatus(OK)
            assertPageAsExpected(
              parse(response.body),
              ExpectedPageContents(
                title = "Did your client’s pension schemes pay or agree to pay the tax?",
                header = "Did your client’s pension schemes pay or agree to pay the tax?",
                caption = "Annual and lifetime allowances for 6 April 2021 to 5 April 2022",
                radioButtonForYes = checkedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButtonForContinue("Continue", ""),
                amountSection = ExpectedAmountSection("Amount they paid or agreed to pay, in pounds", "For example, £193.52", "42.64")
              ))

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
              ExpectedPageContents(
                title = "Did your pension schemes pay or agree to pay the tax?",
                header = "Did your pension schemes pay or agree to pay the tax?",
                caption = "Annual and lifetime allowances for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = checkedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButtonForContinue("Continue", ""),
                amountSection = ExpectedAmountSection("Amount they paid or agreed to pay, in pounds", "For example, £193.52", "")
              ))

          }
          scenarioNameForIndividualAndWelsh in {

            val response = getPage(UserConfig(Individual, Welsh, Some(sessionData)))

            response must haveStatus(OK)
            assertPageAsExpected(
              parse(response.body),
              ExpectedPageContents(
                title = "Did your pension schemes pay or agree to pay the tax?",
                header = "Did your pension schemes pay or agree to pay the tax?",
                caption = "Annual and lifetime allowances for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = checkedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButtonForContinue("Continue", ""),
                amountSection = ExpectedAmountSection("Amount they paid or agreed to pay, in pounds", "For example, £193.52", "")
              ))

          }
          scenarioNameForAgentAndEnglish in {

            val response = getPage(UserConfig(Agent, English, Some(sessionData)))

            response must haveStatus(OK)
            assertPageAsExpected(
              parse(response.body),
              ExpectedPageContents(
                title = "Did your client’s pension schemes pay or agree to pay the tax?",
                header = "Did your client’s pension schemes pay or agree to pay the tax?",
                caption = "Annual and lifetime allowances for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = checkedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButtonForContinue("Continue", ""),
                amountSection = ExpectedAmountSection("Amount they paid or agreed to pay, in pounds", "For example, £193.52", "")
              ))

          }
          scenarioNameForAgentAndWelsh in {

            val response = getPage(UserConfig(Agent, Welsh, Some(sessionData)))

            response must haveStatus(OK)
            assertPageAsExpected(
              parse(response.body),
              ExpectedPageContents(
                title = "Did your client’s pension schemes pay or agree to pay the tax?",
                header = "Did your client’s pension schemes pay or agree to pay the tax?",
                caption = "Annual and lifetime allowances for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = checkedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButtonForContinue("Continue", ""),
                amountSection = ExpectedAmountSection("Amount they paid or agreed to pay, in pounds", "For example, £193.52", "")
              ))

          }

        }
      }
    }
    "submitted" should {
      "succeed" when {
        "the user has relevant session data and" when {

          val sessionData = pensionsUserData(aPensionsCYAModel)

          "the user has selected 'No' and" when {

            val expectedPensionAnnualAllowancesViewModel =
              sessionData.pensions.pensionsAnnualAllowances.copy(
                pensionProvidePaidAnnualAllowanceQuestion = Some(false),
                taxPaidByPensionProvider = None
              )

            scenarioNameForIndividualAndEnglish in {

              val response = submitForm(UserConfig(Individual, English, Some(sessionData)), SubmittedFormData(Some(false), None))

              response must haveStatus(SEE_OTHER)
              response must haveALocationHeaderValue(relativeUrlForPensionProviderPaidTaxPage)
              getPensionAnnualAllowancesViewModel(sessionData) mustBe expectedPensionAnnualAllowancesViewModel

            }
            scenarioNameForIndividualAndWelsh in {

              val response = submitForm(UserConfig(Individual, Welsh, Some(sessionData)), SubmittedFormData(Some(false), None))

              response must haveStatus(SEE_OTHER)
              response must haveALocationHeaderValue(relativeUrlForPensionProviderPaidTaxPage)
              getPensionAnnualAllowancesViewModel(sessionData) mustBe expectedPensionAnnualAllowancesViewModel

            }
            scenarioNameForAgentAndEnglish in {

              val response = submitForm(UserConfig(Agent, English, Some(sessionData)), SubmittedFormData(Some(false), None))

              response must haveStatus(SEE_OTHER)
              response must haveALocationHeaderValue(relativeUrlForPensionProviderPaidTaxPage)
              getPensionAnnualAllowancesViewModel(sessionData) mustBe expectedPensionAnnualAllowancesViewModel

            }
            scenarioNameForAgentAndWelsh in {

              val response = submitForm(UserConfig(Agent, Welsh, Some(sessionData)), SubmittedFormData(Some(false), None))

              response must haveStatus(SEE_OTHER)
              response must haveALocationHeaderValue(relativeUrlForPensionProviderPaidTaxPage)
              getPensionAnnualAllowancesViewModel(sessionData) mustBe expectedPensionAnnualAllowancesViewModel

            }
          }
          "the user has selected 'Yes' as well as a valid amount (unformatted), and" when {

            val expectedPensionAnnualAllowancesViewModel =
              sessionData.pensions.pensionsAnnualAllowances.copy(
                pensionProvidePaidAnnualAllowanceQuestion = Some(true),
                taxPaidByPensionProvider = Some(BigDecimal(42.64))
              )

            scenarioNameForIndividualAndEnglish in {

              val response = submitForm(UserConfig(Individual, English, Some(sessionData)), SubmittedFormData(Some(true), Some("42.64")))

              response must haveStatus(SEE_OTHER)
              response must haveALocationHeaderValue(relativeUrlForPensionProviderPaidTaxPage)
              getPensionAnnualAllowancesViewModel(sessionData) mustBe expectedPensionAnnualAllowancesViewModel

            }
            scenarioNameForIndividualAndWelsh in {

              val response = submitForm(UserConfig(Individual, Welsh, Some(sessionData)), SubmittedFormData(Some(true), Some("42.64")))

              response must haveStatus(SEE_OTHER)
              response must haveALocationHeaderValue(relativeUrlForPensionProviderPaidTaxPage)
              getPensionAnnualAllowancesViewModel(sessionData) mustBe expectedPensionAnnualAllowancesViewModel

            }
            scenarioNameForAgentAndEnglish in {

              val response = submitForm(UserConfig(Agent, English, Some(sessionData)), SubmittedFormData(Some(true), Some("42.64")))

              response must haveStatus(SEE_OTHER)
              response must haveALocationHeaderValue(relativeUrlForPensionProviderPaidTaxPage)
              getPensionAnnualAllowancesViewModel(sessionData) mustBe expectedPensionAnnualAllowancesViewModel

            }
            scenarioNameForAgentAndWelsh in {

              val response = submitForm(UserConfig(Agent, Welsh, Some(sessionData)), SubmittedFormData(Some(true), Some("42.64")))

              response must haveStatus(SEE_OTHER)
              response must haveALocationHeaderValue(relativeUrlForPensionProviderPaidTaxPage)
              getPensionAnnualAllowancesViewModel(sessionData) mustBe expectedPensionAnnualAllowancesViewModel

            }
          }
          "the user has selected 'Yes' as well as a valid amount (formatted), and" when {

            val expectedPensionAnnualAllowancesViewModel =
              sessionData.pensions.pensionsAnnualAllowances.copy(
                pensionProvidePaidAnnualAllowanceQuestion = Some(true),
                taxPaidByPensionProvider = Some(BigDecimal(1042.64))
              )

            scenarioNameForIndividualAndEnglish in {

              val response = submitForm(UserConfig(Individual, English, Some(sessionData)), SubmittedFormData(Some(true), Some("£1,042.64")))

              response must haveStatus(SEE_OTHER)
              response must haveALocationHeaderValue(relativeUrlForPensionProviderPaidTaxPage)
              getPensionAnnualAllowancesViewModel(sessionData) mustBe expectedPensionAnnualAllowancesViewModel

            }
            scenarioNameForIndividualAndWelsh in {

              val response = submitForm(UserConfig(Individual, Welsh, Some(sessionData)), SubmittedFormData(Some(true), Some("£1,042.64")))

              response must haveStatus(SEE_OTHER)
              response must haveALocationHeaderValue(relativeUrlForPensionProviderPaidTaxPage)
              getPensionAnnualAllowancesViewModel(sessionData) mustBe expectedPensionAnnualAllowancesViewModel

            }
            scenarioNameForAgentAndEnglish in {

              val response = submitForm(UserConfig(Agent, English, Some(sessionData)), SubmittedFormData(Some(true), Some("£1,042.64")))

              response must haveStatus(SEE_OTHER)
              response must haveALocationHeaderValue(relativeUrlForPensionProviderPaidTaxPage)
              getPensionAnnualAllowancesViewModel(sessionData) mustBe expectedPensionAnnualAllowancesViewModel

            }
            scenarioNameForAgentAndWelsh in {

              val response = submitForm(UserConfig(Agent, Welsh, Some(sessionData)), SubmittedFormData(Some(true), Some("£1,042.64")))

              response must haveStatus(SEE_OTHER)
              response must haveALocationHeaderValue(relativeUrlForPensionProviderPaidTaxPage)
              getPensionAnnualAllowancesViewModel(sessionData) mustBe expectedPensionAnnualAllowancesViewModel

            }
          }

        }
      }
      "fail" when {
        "the user has relevant session data and" when {

          val sessionData = pensionsUserData(aPensionsCYAModel)

          "the user has selected neither 'Yes' nor 'No' and" when {
            scenarioNameForIndividualAndEnglish in {

              val response = submitForm(UserConfig(Individual, English, Some(sessionData)), SubmittedFormData(None, None))

              response must haveStatus(BAD_REQUEST)
              assertPageAsExpected(
                parse(response.body),
                ExpectedPageContents(
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
                      title = "Select yes if your pension provider paid or agreed to pay your annual allowance tax",
                      idOpt = Some("value")
                    )
                  )
                ))

            }
            scenarioNameForIndividualAndWelsh in {

              val response = submitForm(UserConfig(Individual, Welsh, Some(sessionData)), SubmittedFormData(None, None))

              response must haveStatus(BAD_REQUEST)
              assertPageAsExpected(
                parse(response.body),
                ExpectedPageContents(
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
                      title = "Select yes if your pension provider paid or agreed to pay your annual allowance tax",
                      idOpt = Some("value")
                    )
                  )
                ))

            }
            scenarioNameForAgentAndEnglish in {

              val response = submitForm(UserConfig(Agent, English, Some(sessionData)), SubmittedFormData(None, None))

              response must haveStatus(BAD_REQUEST)
              assertPageAsExpected(
                parse(response.body),
                ExpectedPageContents(
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
                      title = "Select yes if your client’s pension provider paid or agreed to pay the annual allowance tax",
                      idOpt = Some("value")
                    )
                  )
                ))

            }
            scenarioNameForAgentAndWelsh in {

              val response = submitForm(UserConfig(Agent, Welsh, Some(sessionData)), SubmittedFormData(None, None))

              response must haveStatus(BAD_REQUEST)
              assertPageAsExpected(
                parse(response.body),
                ExpectedPageContents(
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
                      title = "Select yes if your client’s pension provider paid or agreed to pay the annual allowance tax",
                      idOpt = Some("value")
                    )
                  )
                ))

            }
          }
          "the user has selected 'Yes' but have not provided an amount, and" when {
            scenarioNameForIndividualAndEnglish in {

              val response = submitForm(UserConfig(Individual, English, Some(sessionData)), SubmittedFormData(Some(true), None))

              response must haveStatus(BAD_REQUEST)
              assertPageAsExpected(
                parse(response.body),
                ExpectedPageContents(
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
                      title = "Enter the amount of tax your pension provider paid or agreed to pay",
                      idOpt = Some("amount-2")
                    )
                  )
                ))

            }
            scenarioNameForIndividualAndWelsh in {

              val response = submitForm(UserConfig(Individual, Welsh, Some(sessionData)), SubmittedFormData(Some(true), None))

              response must haveStatus(BAD_REQUEST)
              assertPageAsExpected(
                parse(response.body),
                ExpectedPageContents(
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
                      title = "Enter the amount of tax your pension provider paid or agreed to pay",
                      idOpt = Some("amount-2")
                    )
                  )
                ))

            }
            scenarioNameForAgentAndEnglish in {

              val response = submitForm(UserConfig(Agent, English, Some(sessionData)), SubmittedFormData(Some(true), None))

              response must haveStatus(BAD_REQUEST)
              assertPageAsExpected(
                parse(response.body),
                ExpectedPageContents(
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
                      title = "Enter the amount of tax your client’s pension provider paid or agreed to pay",
                      idOpt = Some("amount-2")
                    )
                  )
                ))

            }
            scenarioNameForAgentAndWelsh in {

              val response = submitForm(UserConfig(Agent, Welsh, Some(sessionData)), SubmittedFormData(Some(true), None))

              response must haveStatus(BAD_REQUEST)
              assertPageAsExpected(
                parse(response.body),
                ExpectedPageContents(
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
                      title = "Enter the amount of tax your client’s pension provider paid or agreed to pay",
                      idOpt = Some("amount-2")
                    )
                  )
                ))

            }
          }
          "the user has selected 'Yes' but has provided an amount of an invalid format, and" when {
            scenarioNameForIndividualAndEnglish in {

              val response = submitForm(UserConfig(Individual, English, Some(sessionData)), SubmittedFormData(Some(true), Some("x2.64")))

              response must haveStatus(BAD_REQUEST)
              assertPageAsExpected(
                parse(response.body),
                ExpectedPageContents(
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
                      title = "Enter the amount of tax your pension provider paid or agreed to pay in the correct format",
                      idOpt = Some("amount-2")
                    )
                  )
                ))

            }
            scenarioNameForIndividualAndWelsh in {

              val response = submitForm(UserConfig(Individual, Welsh, Some(sessionData)), SubmittedFormData(Some(true), Some("x2.64")))

              response must haveStatus(BAD_REQUEST)
              assertPageAsExpected(
                parse(response.body),
                ExpectedPageContents(
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
                      title = "Enter the amount of tax your pension provider paid or agreed to pay in the correct format",
                      idOpt = Some("amount-2")
                    )
                  )
                ))

            }
            scenarioNameForAgentAndEnglish in {

              val response = submitForm(UserConfig(Agent, English, Some(sessionData)), SubmittedFormData(Some(true), Some("x2.64")))

              response must haveStatus(BAD_REQUEST)
              assertPageAsExpected(
                parse(response.body),
                ExpectedPageContents(
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
                      title = "Enter the amount of tax your client’s pension provider paid or agreed to pay in the correct format",
                      idOpt = Some("amount-2")
                    )
                  )
                ))

            }
            scenarioNameForAgentAndWelsh in {

              val response = submitForm(UserConfig(Agent, Welsh, Some(sessionData)), SubmittedFormData(Some(true), Some("x2.64")))

              response must haveStatus(BAD_REQUEST)
              assertPageAsExpected(
                parse(response.body),
                ExpectedPageContents(
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
                      title = "Enter the amount of tax your client’s pension provider paid or agreed to pay in the correct format",
                      idOpt = Some("amount-2")
                    )
                  )
                ))

            }
          }
          "the user has selected 'Yes' but has provided an excessive amount, and" when {
            scenarioNameForIndividualAndEnglish in {

              val response = submitForm(UserConfig(Individual, English, Some(sessionData)), SubmittedFormData(Some(true), Some("100000000002")))

              response must haveStatus(BAD_REQUEST)
              assertPageAsExpected(
                parse(response.body),
                ExpectedPageContents(
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
                      title = "The amount of tax your pension provider paid or agreed to pay must be less than £100,000,000,000",
                      idOpt = Some("amount-2")
                    )
                  )
                ))

            }
            scenarioNameForIndividualAndWelsh in {

              val response = submitForm(UserConfig(Individual, Welsh, Some(sessionData)), SubmittedFormData(Some(true), Some("100000000002")))

              response must haveStatus(BAD_REQUEST)
              assertPageAsExpected(
                parse(response.body),
                ExpectedPageContents(
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
                      title = "The amount of tax your pension provider paid or agreed to pay must be less than £100,000,000,000",
                      idOpt = Some("amount-2")
                    )
                  )
                ))

            }
            scenarioNameForAgentAndEnglish in {

              val response = submitForm(UserConfig(Agent, English, Some(sessionData)), SubmittedFormData(Some(true), Some("100000000002")))

              response must haveStatus(BAD_REQUEST)
              assertPageAsExpected(
                parse(response.body),
                ExpectedPageContents(
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
                      title = "The amount of tax your client’s pension provider paid or agreed to pay must be less than £100,000,000,000",
                      idOpt = Some("amount-2")
                    )
                  )
                ))

            }
            scenarioNameForAgentAndWelsh in {

              val response = submitForm(UserConfig(Agent, Welsh, Some(sessionData)), SubmittedFormData(Some(true), Some("100000000002")))

              response must haveStatus(BAD_REQUEST)
              assertPageAsExpected(
                parse(response.body),
                ExpectedPageContents(
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
                      title = "The amount of tax your client’s pension provider paid or agreed to pay must be less than £100,000,000,000",
                      idOpt = Some("amount-2")
                    )
                  )
                ))

            }
          }
        }
      }
    }
  }

  private def getPensionAnnualAllowancesViewModel(pensionsUserData: PensionsUserData): PensionAnnualAllowancesViewModel
  = loadPensionUserData(pensionsUserData).pensions.pensionsAnnualAllowances

  private def assertPageAsExpected(document: Document, expectedPageContents: ExpectedPageContents): Unit = {

    document must haveTitle(expectedPageContents.title)
    document must haveHeader(expectedPageContents.header)
    document must haveCaption(expectedPageContents.caption)

    val indexOfRadioButtonForYes = 0
    document must haveARadioButtonAtIndex(indexOfRadioButtonForYes)
    document must haveARadioButtonAtIndexWithLabel(indexOfRadioButtonForYes, expectedPageContents.radioButtonForYes.label)
    if (expectedPageContents.radioButtonForYes.isChecked) document must haveACheckedRadioButtonAtIndex(indexOfRadioButtonForYes)
    else document must not(haveACheckedRadioButtonAtIndex(indexOfRadioButtonForYes))

    val indexOfRadioButtonForNo = 1
    document must haveARadioButtonAtIndex(indexOfRadioButtonForNo)
    document must haveARadioButtonAtIndexWithLabel(indexOfRadioButtonForNo, expectedPageContents.radioButtonForNo.label)
    if (expectedPageContents.radioButtonForNo.isChecked) document must haveACheckedRadioButtonAtIndex(indexOfRadioButtonForNo)
    else document must not(haveACheckedRadioButtonAtIndex(indexOfRadioButtonForNo))

    document must haveAContinueButtonWithLabel(expectedPageContents.buttonForContinue.label)
    document must haveAContinueButtonWithLink(expectedPageContents.buttonForContinue.link)

    document must haveAnAmountLabel(expectedPageContents.amountSection.label)
    document must haveAnAmountHint(expectedPageContents.amountSection.hint)
    document must haveAnAmountValue(expectedPageContents.amountSection.value)
    document must haveAnAmountName("amount-2")

    document must haveAFormWithTargetAction(relativeUrlForPensionProviderPaidTaxPage)

    expectedPageContents.errorSummarySectionOpt match {

      case Some(expectedErrorSummarySection) =>
        document must haveAnErrorSummarySection
        document must haveAnErrorSummaryTitle(expectedErrorSummarySection.title)
        document must haveAnErrorSummaryBody(expectedErrorSummarySection.body)
        document must haveAnErrorSummaryLink(expectedErrorSummarySection.link)
      case None =>
        document must not(haveAnErrorSummarySection)
    }

    expectedPageContents.errorAboveElementCheckSectionOpt match {
      case Some(errorAboveElementSection) =>
        document must haveAnErrorAboveElementSection(errorAboveElementSection.idOpt)
        document must haveAnErrorAboveElementTitle(errorAboveElementSection.idOpt, errorAboveElementSection.title)
      case None =>
        document must not(haveAnErrorAboveElementSection())
    }

  }

  private def getPage(userConfig: UserConfig)(implicit wsClient: WSClient): WSResponse = {
    getPage(userConfig, "/annual-lifetime-allowances/pension-provider-paid-tax")
  }

  private def submitForm(userConfig: UserConfig, submittedFormData: SubmittedFormData)(implicit wsClient: WSClient): WSResponse =
    submitForm(userConfig, submittedFormData.asMap, "/annual-lifetime-allowances/pension-provider-paid-tax")


  private def relativeUrlForPensionSummaryPage: String = relativeUrl("/pensions-summary")

  private def relativeUrlForPensionProviderPaidTaxPage: String = relativeUrl("/annual-lifetime-allowances/pension-provider-paid-tax")

  private def checkedExpectedRadioButton(label: String) = ExpectedRadioButton(label, isChecked = true)

  private def uncheckedExpectedRadioButton(label: String) = ExpectedRadioButton(label, isChecked = false)

}

case class SubmittedFormData(yesOrNoOpt: Option[Boolean], amountOpt: Option[String]) {

  val yesOrNoAsMap: Map[String, String] = yesOrNoOpt match {
    case Some(true) => Map("value" -> "true")
    case Some(false) => Map("value" -> "false")
    case None => Map.empty
  }

  val amountAsMap: Map[String, String] = amountOpt match {
    case Some(amount) => Map("amount-2" -> amount)
    case None => Map.empty
  }

  val asMap: Map[String, String] = yesOrNoAsMap ++ amountAsMap

}

case class ExpectedPageContents(title: String,
                                header: String,
                                caption: String,
                                radioButtonForYes: ExpectedRadioButton,
                                radioButtonForNo: ExpectedRadioButton,
                                buttonForContinue: ExpectedButtonForContinue,
                                amountSection: ExpectedAmountSection,
                                errorSummarySectionOpt: Option[ErrorSummarySection] = None,
                                errorAboveElementCheckSectionOpt: Option[ErrorAboveElementCheckSection] = None
                               )

case class ExpectedAmountSection(label: String, hint: String, value: String)

case class ExpectedRadioButton(label: String, isChecked: Boolean)

case class ExpectedButtonForContinue(label: String, link: String)

case class ErrorSummarySection(title: String, body: String, link: String)

case class ErrorAboveElementCheckSection(title: String, idOpt: Option[String])



