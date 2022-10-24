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

package controllers.pensions.paymentsIntoOverseasPensions

import builders.PensionsCYAModelBuilder.{aPensionsCYAEmptyModel, aPensionsCYAModel}
import controllers.ControllerSpec.PreferredLanguages.{English, Welsh}
import controllers.ControllerSpec.UserTypes.{Agent, Individual}
import controllers.ControllerSpec._
import controllers.YesNoAmountControllerSpec
import models.mongo.PensionsUserData
import models.pension.charges.PaymentsIntoOverseasPensionsViewModel
import org.jsoup.Jsoup.parse
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}

class PaymentIntoPensionSchemeControllerISpec
  extends YesNoAmountControllerSpec("/overseas-pensions/payments-into-overseas-pensions/payments-into-schemes") {

  private val selectorForFirstParagraph = "#main-content > div > div > p:nth-child(2)"
  private val selectorForSecondParagraph = "#main-content > div > div > p:nth-child(3)"
  private val selectorForBulletPoint = "#main-content > div > div > ul > li:nth-child(2)"

  "This page" when {
    "requested to be shown" should {
      "redirect to the summary page" when {
        "the user has no stored session data at all" in {

          implicit val userConfig: UserConfig = UserConfig(Individual, English, None)
          val response = getPage

          response must haveStatus(SEE_OTHER)
          response must haveALocationHeaderValue(PageRelativeURLs.summaryPage)

        }
      }
      "appear as expected" when {
        "the user has no relevant session data and" when {

          val sessionData = pensionsUserData(aPensionsCYAEmptyModel)

          scenarioNameForIndividualAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            val response = getPage

            response must haveStatus(OK)
            assertPageAsExpected(
              parse(response.body),
              ExpectedYesNoAmountPageContents(
                title = "Payments into overseas pension schemes",
                header = "Payments into overseas pension schemes",
                caption = "Payments into overseas pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection = ExpectedAmountSection("Total amount, in pounds", "For example, £193.52 Do not include payments your employer made", ""),
                links = Set(
                  ExpectedLink(
                    "eligibleForTaxRelief-link",
                    "eligible for tax relief (opens in new tab)",
                    "https://www.gov.uk/guidance/overseas-pensions-tax-relief-on-your-contributions"),
                ),
                text = Set(
                  ExpectedText(selectorForFirstParagraph, "Your overseas pension schemes must not be registered in the UK."),
                  ExpectedText(selectorForSecondParagraph, "They must also be:"),
                  ExpectedText(selectorForBulletPoint, "paid into after tax (net income)")
                )
              ))

          }
          scenarioNameForIndividualAndWelsh in {

            implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
            val response = getPage

            response must haveStatus(OK)
            assertPageAsExpected(
              parse(response.body),
              ExpectedYesNoAmountPageContents(
                title = "Payments into overseas pension schemes",
                header = "Payments into overseas pension schemes",
                caption = "Payments into overseas pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection = ExpectedAmountSection("Total amount, in pounds", "For example, £193.52 Do not include payments your employer made", ""),
                links = Set(
                  ExpectedLink(
                    "eligibleForTaxRelief-link",
                    "eligible for tax relief (opens in new tab)",
                    "https://www.gov.uk/guidance/overseas-pensions-tax-relief-on-your-contributions"),
                ),
                text = Set(
                  ExpectedText(selectorForFirstParagraph, "Your overseas pension schemes must not be registered in the UK."),
                  ExpectedText(selectorForSecondParagraph, "They must also be:"),
                  ExpectedText(selectorForBulletPoint, "paid into after tax (net income)")
                )
              ))

          }
          scenarioNameForAgentAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
            val response = getPage

            response must haveStatus(OK)
            assertPageAsExpected(
              parse(response.body),
              ExpectedYesNoAmountPageContents(
                title = "Payments into overseas pension schemes",
                header = "Payments into overseas pension schemes",
                caption = "Payments into overseas pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection = ExpectedAmountSection("Total amount, in pounds", "For example, £193.52 Do not include payments your client’s employer made", ""),
                links = Set(
                  ExpectedLink(
                    "eligibleForTaxRelief-link",
                    "eligible for tax relief (opens in new tab)",
                    "https://www.gov.uk/guidance/overseas-pensions-tax-relief-on-your-contributions"),
                ),
                text = Set(
                  ExpectedText(selectorForFirstParagraph, "Your client’s overseas pension schemes must not be registered in the UK."),
                  ExpectedText(selectorForSecondParagraph, "They must also be:"),
                  ExpectedText(selectorForBulletPoint, "paid into after tax (net income)")
                )
              )
            )

          }
          scenarioNameForAgentAndWelsh in {

            implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
            val response = getPage

            response must haveStatus(OK)
            assertPageAsExpected(
              parse(response.body),
              ExpectedYesNoAmountPageContents(
                title = "Payments into overseas pension schemes",
                header = "Payments into overseas pension schemes",
                caption = "Payments into overseas pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection = ExpectedAmountSection("Total amount, in pounds", "For example, £193.52 Do not include payments your client’s employer made", ""),
                links = Set(
                  ExpectedLink(
                    "eligibleForTaxRelief-link",
                    "eligible for tax relief (opens in new tab)",
                    "https://www.gov.uk/guidance/overseas-pensions-tax-relief-on-your-contributions"),
                ),
                text = Set(
                  ExpectedText(selectorForFirstParagraph, "Your client’s overseas pension schemes must not be registered in the UK."),
                  ExpectedText(selectorForSecondParagraph, "They must also be:"),
                  ExpectedText(selectorForBulletPoint, "paid into after tax (net income)")
                )
              ))

          }
        }
        "the user had previously answered 'Yes' with a valid amount, and" when {

          val sessionData: PensionsUserData =
            pensionsUserData(aPensionsCYAModel.copy(paymentsIntoOverseasPensions =
              PaymentsIntoOverseasPensionsViewModel(
                paymentsIntoOverseasPensionsQuestions = Some(true),
                paymentsIntoOverseasPensionsAmount = Some(42.64))
            ))

          scenarioNameForIndividualAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            val response = getPage

            response must haveStatus(OK)
            assertPageAsExpected(
              parse(response.body),
              ExpectedYesNoAmountPageContents(
                title = "Payments into overseas pension schemes",
                header = "Payments into overseas pension schemes",
                caption = "Payments into overseas pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = checkedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection = ExpectedAmountSection("Total amount, in pounds", "For example, £193.52 Do not include payments your employer made", "42.64"),
                links = Set(
                  ExpectedLink(
                    "eligibleForTaxRelief-link",
                    "eligible for tax relief (opens in new tab)",
                    "https://www.gov.uk/guidance/overseas-pensions-tax-relief-on-your-contributions"),
                ),
                text = Set(
                  ExpectedText(selectorForFirstParagraph, "Your overseas pension schemes must not be registered in the UK."),
                  ExpectedText(selectorForSecondParagraph, "They must also be:"),
                  ExpectedText(selectorForBulletPoint, "paid into after tax (net income)")
                )
              ))

          }
          scenarioNameForIndividualAndWelsh in {

            implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
            val response = getPage

            response must haveStatus(OK)
            assertPageAsExpected(
              parse(response.body),
              ExpectedYesNoAmountPageContents(
                title = "Payments into overseas pension schemes",
                header = "Payments into overseas pension schemes",
                caption = "Payments into overseas pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = checkedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection = ExpectedAmountSection("Total amount, in pounds", "For example, £193.52 Do not include payments your employer made", "42.64"),
                links = Set(
                  ExpectedLink(
                    "eligibleForTaxRelief-link",
                    "eligible for tax relief (opens in new tab)",
                    "https://www.gov.uk/guidance/overseas-pensions-tax-relief-on-your-contributions"),
                ),
                text = Set(
                  ExpectedText(selectorForFirstParagraph, "Your overseas pension schemes must not be registered in the UK."),
                  ExpectedText(selectorForSecondParagraph, "They must also be:"),
                  ExpectedText(selectorForBulletPoint, "paid into after tax (net income)")
                )
              ))

          }
          scenarioNameForAgentAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
            val response = getPage

            response must haveStatus(OK)
            assertPageAsExpected(
              parse(response.body),
              ExpectedYesNoAmountPageContents(
                title = "Payments into overseas pension schemes",
                header = "Payments into overseas pension schemes",
                caption = "Payments into overseas pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = checkedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection = ExpectedAmountSection("Total amount, in pounds", "For example, £193.52 Do not include payments your client’s employer made", "42.64"),
                links = Set(
                  ExpectedLink(
                    "eligibleForTaxRelief-link",
                    "eligible for tax relief (opens in new tab)",
                    "https://www.gov.uk/guidance/overseas-pensions-tax-relief-on-your-contributions"),
                ),
                text = Set(
                  ExpectedText(selectorForFirstParagraph, "Your client’s overseas pension schemes must not be registered in the UK."),
                  ExpectedText(selectorForSecondParagraph, "They must also be:"),
                  ExpectedText(selectorForBulletPoint, "paid into after tax (net income)")
                )
              ))

          }
          scenarioNameForAgentAndWelsh in {

            implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
            val response = getPage

            response must haveStatus(OK)
            assertPageAsExpected(
              parse(response.body),
              ExpectedYesNoAmountPageContents(
                title = "Payments into overseas pension schemes",
                header = "Payments into overseas pension schemes",
                caption = "Payments into overseas pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = checkedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection = ExpectedAmountSection("Total amount, in pounds", "For example, £193.52 Do not include payments your client’s employer made", "42.64"),
                links = Set(
                  ExpectedLink(
                    "eligibleForTaxRelief-link",
                    "eligible for tax relief (opens in new tab)",
                    "https://www.gov.uk/guidance/overseas-pensions-tax-relief-on-your-contributions"),
                ),
                text = Set(
                  ExpectedText(selectorForFirstParagraph, "Your client’s overseas pension schemes must not be registered in the UK."),
                  ExpectedText(selectorForSecondParagraph, "They must also be:"),
                  ExpectedText(selectorForBulletPoint, "paid into after tax (net income)")
                )
              ))

          }

        }
        "the user had previously answered 'No' without an amount, and" when {

          val sessionData =
            pensionsUserData(aPensionsCYAModel.copy(paymentsIntoOverseasPensions =
              PaymentsIntoOverseasPensionsViewModel(
                paymentsIntoOverseasPensionsQuestions = Some(false),
                paymentsIntoOverseasPensionsAmount = None)
            ))

          scenarioNameForIndividualAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            val response = getPage

            response must haveStatus(OK)
            assertPageAsExpected(
              parse(response.body),
              ExpectedYesNoAmountPageContents(
                title = "Payments into overseas pension schemes",
                header = "Payments into overseas pension schemes",
                caption = "Payments into overseas pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = checkedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection = ExpectedAmountSection("Total amount, in pounds", "For example, £193.52 Do not include payments your employer made", ""),
                links = Set(
                  ExpectedLink(
                    "eligibleForTaxRelief-link",
                    "eligible for tax relief (opens in new tab)",
                    "https://www.gov.uk/guidance/overseas-pensions-tax-relief-on-your-contributions"),
                ),
                text = Set(
                  ExpectedText(selectorForFirstParagraph, "Your overseas pension schemes must not be registered in the UK."),
                  ExpectedText(selectorForSecondParagraph, "They must also be:"),
                  ExpectedText(selectorForBulletPoint, "paid into after tax (net income)")
                )
              ))

          }
          scenarioNameForIndividualAndWelsh in {

            implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
            val response = getPage

            response must haveStatus(OK)
            assertPageAsExpected(
              parse(response.body),
              ExpectedYesNoAmountPageContents(
                title = "Payments into overseas pension schemes",
                header = "Payments into overseas pension schemes",
                caption = "Payments into overseas pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = checkedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection = ExpectedAmountSection("Total amount, in pounds", "For example, £193.52 Do not include payments your employer made", ""),
                links = Set(
                  ExpectedLink(
                    "eligibleForTaxRelief-link",
                    "eligible for tax relief (opens in new tab)",
                    "https://www.gov.uk/guidance/overseas-pensions-tax-relief-on-your-contributions"),
                ),
                text = Set(
                  ExpectedText(selectorForFirstParagraph, "Your overseas pension schemes must not be registered in the UK."),
                  ExpectedText(selectorForSecondParagraph, "They must also be:"),
                  ExpectedText(selectorForBulletPoint, "paid into after tax (net income)")
                )
              ))

          }
          scenarioNameForAgentAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
            val response = getPage

            response must haveStatus(OK)
            assertPageAsExpected(
              parse(response.body),
              ExpectedYesNoAmountPageContents(
                title = "Payments into overseas pension schemes",
                header = "Payments into overseas pension schemes",
                caption = "Payments into overseas pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = checkedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection = ExpectedAmountSection("Total amount, in pounds", "For example, £193.52 Do not include payments your client’s employer made", ""),
                links = Set(
                  ExpectedLink(
                    "eligibleForTaxRelief-link",
                    "eligible for tax relief (opens in new tab)",
                    "https://www.gov.uk/guidance/overseas-pensions-tax-relief-on-your-contributions"),
                ),
                text = Set(
                  ExpectedText(selectorForFirstParagraph, "Your client’s overseas pension schemes must not be registered in the UK."),
                  ExpectedText(selectorForSecondParagraph, "They must also be:"),
                  ExpectedText(selectorForBulletPoint, "paid into after tax (net income)")
                )
              ))

          }
          scenarioNameForAgentAndWelsh in {

            implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
            val response = getPage

            response must haveStatus(OK)
            assertPageAsExpected(
              parse(response.body),
              ExpectedYesNoAmountPageContents(
                title = "Payments into overseas pension schemes",
                header = "Payments into overseas pension schemes",
                caption = "Payments into overseas pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = checkedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection = ExpectedAmountSection("Total amount, in pounds", "For example, £193.52 Do not include payments your client’s employer made", ""),
                links = Set(
                  ExpectedLink(
                    "eligibleForTaxRelief-link",
                    "eligible for tax relief (opens in new tab)",
                    "https://www.gov.uk/guidance/overseas-pensions-tax-relief-on-your-contributions"),
                ),
                text = Set(
                  ExpectedText(selectorForFirstParagraph, "Your client’s overseas pension schemes must not be registered in the UK."),
                  ExpectedText(selectorForSecondParagraph, "They must also be:"),
                  ExpectedText(selectorForBulletPoint, "paid into after tax (net income)")
                )
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

            val expectedViewModel =
              sessionData.pensions.paymentsIntoOverseasPensions.copy(
                paymentsIntoOverseasPensionsQuestions = Some(false),
                paymentsIntoOverseasPensionsAmount = None)

            scenarioNameForIndividualAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
              val response = submitForm(SubmittedFormDataForYesNoAmountPage(Some(false), None))

              response must haveStatus(SEE_OTHER)
              response must haveALocationHeaderValue(relativeUrlForThisPage)
              getViewModel mustBe expectedViewModel

            }
            scenarioNameForIndividualAndWelsh in {

              implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
              val response = submitForm(SubmittedFormDataForYesNoAmountPage(Some(false), None))

              response must haveStatus(SEE_OTHER)
              response must haveALocationHeaderValue(relativeUrlForThisPage)
              getViewModel mustBe expectedViewModel

            }
            scenarioNameForAgentAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
              val response = submitForm(SubmittedFormDataForYesNoAmountPage(Some(false), None))

              response must haveStatus(SEE_OTHER)
              response must haveALocationHeaderValue(relativeUrlForThisPage)
              getViewModel mustBe expectedViewModel

            }
            scenarioNameForAgentAndWelsh in {

              implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
              val response = submitForm(SubmittedFormDataForYesNoAmountPage(Some(false), None))

              response must haveStatus(SEE_OTHER)
              response must haveALocationHeaderValue(relativeUrlForThisPage)
              getViewModel mustBe expectedViewModel

            }
          }
          "the user has selected 'Yes' as well as a valid amount (unformatted), and" when {

            val expectedViewModel =
              sessionData.pensions.paymentsIntoOverseasPensions.copy(
                paymentsIntoOverseasPensionsQuestions = Some(true),
                paymentsIntoOverseasPensionsAmount = Some(BigDecimal(42.64)))

            scenarioNameForIndividualAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
              val response = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), Some("42.64")))

              response must haveStatus(SEE_OTHER)
              response must haveALocationHeaderValue(relativeUrlForThisPage)
              getViewModel mustBe expectedViewModel

            }
            scenarioNameForIndividualAndWelsh in {

              implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
              val response = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), Some("42.64")))

              response must haveStatus(SEE_OTHER)
              response must haveALocationHeaderValue(relativeUrlForThisPage)
              getViewModel mustBe expectedViewModel

            }
            scenarioNameForAgentAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
              val response = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), Some("42.64")))

              response must haveStatus(SEE_OTHER)
              response must haveALocationHeaderValue(relativeUrlForThisPage)
              getViewModel mustBe expectedViewModel

            }
            scenarioNameForAgentAndWelsh in {

              implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
              val response = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), Some("42.64")))

              response must haveStatus(SEE_OTHER)
              response must haveALocationHeaderValue(relativeUrlForThisPage)
              getViewModel mustBe expectedViewModel

            }
          }
          "the user has selected 'Yes' as well as a valid amount (formatted), and" when {

            val expectedViewModel =
              sessionData.pensions.paymentsIntoOverseasPensions.copy(
                paymentsIntoOverseasPensionsQuestions = Some(true),
                paymentsIntoOverseasPensionsAmount = Some(BigDecimal(1042.64)))

            scenarioNameForIndividualAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
              val response = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), Some("£1,042.64")))

              response must haveStatus(SEE_OTHER)
              response must haveALocationHeaderValue(relativeUrlForThisPage)
              getViewModel mustBe expectedViewModel

            }
            scenarioNameForIndividualAndWelsh in {

              implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
              val response = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), Some("£1,042.64")))

              response must haveStatus(SEE_OTHER)
              response must haveALocationHeaderValue(relativeUrlForThisPage)
              getViewModel mustBe expectedViewModel

            }
            scenarioNameForAgentAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
              val response = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), Some("£1,042.64")))

              response must haveStatus(SEE_OTHER)
              response must haveALocationHeaderValue(relativeUrlForThisPage)
              getViewModel mustBe expectedViewModel

            }
            scenarioNameForAgentAndWelsh in {

              implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
              val response = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), Some("£1,042.64")))

              response must haveStatus(SEE_OTHER)
              response must haveALocationHeaderValue(relativeUrlForThisPage)
              getViewModel mustBe expectedViewModel

            }
          }

        }
      }
      "fail" when {
        "the user has relevant session data and" when {

          val sessionData = pensionsUserData(aPensionsCYAModel)

          "the user has selected neither 'Yes' nor 'No' and" when {
            scenarioNameForIndividualAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
              val response = submitForm(SubmittedFormDataForYesNoAmountPage(None, None))

              response must haveStatus(BAD_REQUEST)
              assertPageAsExpected(
                parse(response.body),
                ExpectedYesNoAmountPageContents(
                  title = "Error: Payments into overseas pension schemes",
                  header = "Payments into overseas pension schemes",
                  caption = "Payments into overseas pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection = ExpectedAmountSection("Total amount, in pounds", "For example, £193.52 Do not include payments your employer made", ""),
                  links = Set(
                    ExpectedLink(
                      "eligibleForTaxRelief-link",
                      "eligible for tax relief (opens in new tab)",
                      "https://www.gov.uk/guidance/overseas-pensions-tax-relief-on-your-contributions"),
                  ),
                  text = Set(
                    ExpectedText(selectorForSecondParagraph, "Your overseas pension schemes must not be registered in the UK."),
                    ExpectedText("#main-content > div > div > p:nth-child(4)", "They must also be:"),
                    ExpectedText(selectorForBulletPoint, "paid into after tax (net income)")
                  ),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "Select yes if you paid into an overseas pension scheme",
                      link = "#value")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Select yes if you paid into an overseas pension scheme",
                      idOpt = Some("value")
                    )
                  )
                ))

            }
            scenarioNameForIndividualAndWelsh in {

              implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
              val response = submitForm(SubmittedFormDataForYesNoAmountPage(None, None))

              response must haveStatus(BAD_REQUEST)
              assertPageAsExpected(
                parse(response.body),
                ExpectedYesNoAmountPageContents(
                  title = "Error: Payments into overseas pension schemes",
                  header = "Payments into overseas pension schemes",
                  caption = "Payments into overseas pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection = ExpectedAmountSection("Total amount, in pounds", "For example, £193.52 Do not include payments your employer made", ""),
                  links = Set(
                    ExpectedLink(
                      "eligibleForTaxRelief-link",
                      "eligible for tax relief (opens in new tab)",
                      "https://www.gov.uk/guidance/overseas-pensions-tax-relief-on-your-contributions"),
                  ),
                  text = Set(
                    ExpectedText(selectorForSecondParagraph, "Your overseas pension schemes must not be registered in the UK."),
                    ExpectedText("#main-content > div > div > p:nth-child(4)", "They must also be:"),
                    ExpectedText(selectorForBulletPoint, "paid into after tax (net income)")
                  ),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "Select yes if you paid into an overseas pension scheme",
                      link = "#value")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Select yes if you paid into an overseas pension scheme",
                      idOpt = Some("value")
                    )
                  )
                ))

            }
            scenarioNameForAgentAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
              val response = submitForm(SubmittedFormDataForYesNoAmountPage(None, None))

              response must haveStatus(BAD_REQUEST)
              assertPageAsExpected(
                parse(response.body),
                ExpectedYesNoAmountPageContents(
                  title = "Error: Payments into overseas pension schemes",
                  header = "Payments into overseas pension schemes",
                  caption = "Payments into overseas pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection = ExpectedAmountSection("Total amount, in pounds", "For example, £193.52 Do not include payments your client’s employer made", ""),
                  links = Set(
                    ExpectedLink(
                      "eligibleForTaxRelief-link",
                      "eligible for tax relief (opens in new tab)",
                      "https://www.gov.uk/guidance/overseas-pensions-tax-relief-on-your-contributions"),
                  ),
                  text = Set(
                    ExpectedText(selectorForSecondParagraph, "Your client’s overseas pension schemes must not be registered in the UK."),
                    ExpectedText("#main-content > div > div > p:nth-child(4)", "They must also be:"),
                    ExpectedText(selectorForBulletPoint, "paid into after tax (net income)")
                  ),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "Select yes if your client paid into an overseas pension scheme",
                      link = "#value")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Select yes if your client paid into an overseas pension scheme",
                      idOpt = Some("value")
                    )
                  )
                ))

            }
            scenarioNameForAgentAndWelsh in {

              implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
              val response = submitForm(SubmittedFormDataForYesNoAmountPage(None, None))

              response must haveStatus(BAD_REQUEST)
              assertPageAsExpected(
                parse(response.body),
                ExpectedYesNoAmountPageContents(
                  title = "Error: Payments into overseas pension schemes",
                  header = "Payments into overseas pension schemes",
                  caption = "Payments into overseas pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection = ExpectedAmountSection("Total amount, in pounds", "For example, £193.52 Do not include payments your client’s employer made", ""),
                  links = Set(
                    ExpectedLink(
                      "eligibleForTaxRelief-link",
                      "eligible for tax relief (opens in new tab)",
                      "https://www.gov.uk/guidance/overseas-pensions-tax-relief-on-your-contributions"),
                  ),
                  text = Set(
                    ExpectedText(selectorForSecondParagraph, "Your client’s overseas pension schemes must not be registered in the UK."),
                    ExpectedText("#main-content > div > div > p:nth-child(4)", "They must also be:"),
                    ExpectedText(selectorForBulletPoint, "paid into after tax (net income)")
                  ),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "Select yes if your client paid into an overseas pension scheme",
                      link = "#value")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Select yes if your client paid into an overseas pension scheme",
                      idOpt = Some("value")
                    )
                  )
                ))

            }
          }
          "the user has selected 'Yes' but have not provided an amount, and" when {
            scenarioNameForIndividualAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
              val response = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), None))

              response must haveStatus(BAD_REQUEST)
              assertPageAsExpected(
                parse(response.body),
                ExpectedYesNoAmountPageContents(
                  title = "Error: Payments into overseas pension schemes",
                  header = "Payments into overseas pension schemes",
                  caption = "Payments into overseas pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection = ExpectedAmountSection("Total amount, in pounds", "For example, £193.52 Do not include payments your employer made", ""),
                  links = Set(
                    ExpectedLink(
                      "eligibleForTaxRelief-link",
                      "eligible for tax relief (opens in new tab)",
                      "https://www.gov.uk/guidance/overseas-pensions-tax-relief-on-your-contributions"),
                  ),
                  text = Set(
                    ExpectedText(selectorForSecondParagraph, "Your overseas pension schemes must not be registered in the UK."),
                    ExpectedText("#main-content > div > div > p:nth-child(4)", "They must also be:"),
                    ExpectedText(selectorForBulletPoint, "paid into after tax (net income)")
                  ),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "Enter the amount you paid into overseas pension schemes",
                      link = "#amount-2")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter the amount you paid into overseas pension schemes",
                      idOpt = Some("amount-2")
                    )
                  )
                ))

            }
            scenarioNameForIndividualAndWelsh in {

              implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
              val response = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), None))

              response must haveStatus(BAD_REQUEST)
              assertPageAsExpected(
                parse(response.body),
                ExpectedYesNoAmountPageContents(
                  title = "Error: Payments into overseas pension schemes",
                  header = "Payments into overseas pension schemes",
                  caption = "Payments into overseas pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection = ExpectedAmountSection("Total amount, in pounds", "For example, £193.52 Do not include payments your employer made", ""),
                  links = Set(
                    ExpectedLink(
                      "eligibleForTaxRelief-link",
                      "eligible for tax relief (opens in new tab)",
                      "https://www.gov.uk/guidance/overseas-pensions-tax-relief-on-your-contributions"),
                  ),
                  text = Set(
                    ExpectedText(selectorForSecondParagraph, "Your overseas pension schemes must not be registered in the UK."),
                    ExpectedText("#main-content > div > div > p:nth-child(4)", "They must also be:"),
                    ExpectedText(selectorForBulletPoint, "paid into after tax (net income)")
                  ),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "Enter the amount you paid into overseas pension schemes",
                      link = "#amount-2")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter the amount you paid into overseas pension schemes",
                      idOpt = Some("amount-2")
                    )
                  )
                ))

            }
            scenarioNameForAgentAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
              val response = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), None))

              response must haveStatus(BAD_REQUEST)
              assertPageAsExpected(
                parse(response.body),
                ExpectedYesNoAmountPageContents(
                  title = "Error: Payments into overseas pension schemes",
                  header = "Payments into overseas pension schemes",
                  caption = "Payments into overseas pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection = ExpectedAmountSection("Total amount, in pounds", "For example, £193.52 Do not include payments your client’s employer made", ""),
                  links = Set(
                    ExpectedLink(
                      "eligibleForTaxRelief-link",
                      "eligible for tax relief (opens in new tab)",
                      "https://www.gov.uk/guidance/overseas-pensions-tax-relief-on-your-contributions"),
                  ),
                  text = Set(
                    ExpectedText(selectorForSecondParagraph, "Your client’s overseas pension schemes must not be registered in the UK."),
                    ExpectedText("#main-content > div > div > p:nth-child(4)", "They must also be:"),
                    ExpectedText(selectorForBulletPoint, "paid into after tax (net income)")
                  ),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "Enter the amount your client paid into overseas pension schemes",
                      link = "#amount-2")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter the amount your client paid into overseas pension schemes",
                      idOpt = Some("amount-2")
                    )
                  )
                ))

            }
            scenarioNameForAgentAndWelsh in {

              implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
              val response = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), None))

              response must haveStatus(BAD_REQUEST)
              assertPageAsExpected(
                parse(response.body),
                ExpectedYesNoAmountPageContents(
                  title = "Error: Payments into overseas pension schemes",
                  header = "Payments into overseas pension schemes",
                  caption = "Payments into overseas pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection = ExpectedAmountSection("Total amount, in pounds", "For example, £193.52 Do not include payments your client’s employer made", ""),
                  links = Set(
                    ExpectedLink(
                      "eligibleForTaxRelief-link",
                      "eligible for tax relief (opens in new tab)",
                      "https://www.gov.uk/guidance/overseas-pensions-tax-relief-on-your-contributions"),
                  ),
                  text = Set(
                    ExpectedText(selectorForSecondParagraph, "Your client’s overseas pension schemes must not be registered in the UK."),
                    ExpectedText("#main-content > div > div > p:nth-child(4)", "They must also be:"),
                    ExpectedText(selectorForBulletPoint, "paid into after tax (net income)")
                  ),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "Enter the amount your client paid into overseas pension schemes",
                      link = "#amount-2")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter the amount your client paid into overseas pension schemes",
                      idOpt = Some("amount-2")
                    )
                  )
                ))

            }
          }
          "the user has selected 'Yes' but has provided an amount of an invalid format, and" when {
            scenarioNameForIndividualAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
              val response = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), Some("x2.64")))

              response must haveStatus(BAD_REQUEST)
              assertPageAsExpected(
                parse(response.body),
                ExpectedYesNoAmountPageContents(
                  title = "Error: Payments into overseas pension schemes",
                  header = "Payments into overseas pension schemes",
                  caption = "Payments into overseas pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection = ExpectedAmountSection("Total amount, in pounds", "For example, £193.52 Do not include payments your employer made", "x2.64"),
                  links = Set(
                    ExpectedLink(
                      "eligibleForTaxRelief-link",
                      "eligible for tax relief (opens in new tab)",
                      "https://www.gov.uk/guidance/overseas-pensions-tax-relief-on-your-contributions"),
                  ),
                  text = Set(
                    ExpectedText(selectorForSecondParagraph, "Your overseas pension schemes must not be registered in the UK."),
                    ExpectedText("#main-content > div > div > p:nth-child(4)", "They must also be:"),
                    ExpectedText(selectorForBulletPoint, "paid into after tax (net income)")
                  ),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "Enter the amount you paid into overseas pension schemes in the correct format",
                      link = "#amount-2")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter the amount you paid into overseas pension schemes in the correct format",
                      idOpt = Some("amount-2")
                    )
                  )
                ))

            }
            scenarioNameForIndividualAndWelsh in {

              implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
              val response = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), Some("x2.64")))

              response must haveStatus(BAD_REQUEST)
              assertPageAsExpected(
                parse(response.body),
                ExpectedYesNoAmountPageContents(
                  title = "Error: Payments into overseas pension schemes",
                  header = "Payments into overseas pension schemes",
                  caption = "Payments into overseas pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection = ExpectedAmountSection("Total amount, in pounds", "For example, £193.52 Do not include payments your employer made", "x2.64"),
                  links = Set(
                    ExpectedLink(
                      "eligibleForTaxRelief-link",
                      "eligible for tax relief (opens in new tab)",
                      "https://www.gov.uk/guidance/overseas-pensions-tax-relief-on-your-contributions"),
                  ),
                  text = Set(
                    ExpectedText(selectorForSecondParagraph, "Your overseas pension schemes must not be registered in the UK."),
                    ExpectedText("#main-content > div > div > p:nth-child(4)", "They must also be:"),
                    ExpectedText(selectorForBulletPoint, "paid into after tax (net income)")
                  ),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "Enter the amount you paid into overseas pension schemes in the correct format",
                      link = "#amount-2")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter the amount you paid into overseas pension schemes in the correct format",
                      idOpt = Some("amount-2")
                    )
                  )
                ))

            }
            scenarioNameForAgentAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
              val response = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), Some("x2.64")))

              response must haveStatus(BAD_REQUEST)
              assertPageAsExpected(
                parse(response.body),
                ExpectedYesNoAmountPageContents(
                  title = "Error: Payments into overseas pension schemes",
                  header = "Payments into overseas pension schemes",
                  caption = "Payments into overseas pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection = ExpectedAmountSection("Total amount, in pounds", "For example, £193.52 Do not include payments your client’s employer made", "x2.64"),
                  links = Set(
                    ExpectedLink(
                      "eligibleForTaxRelief-link",
                      "eligible for tax relief (opens in new tab)",
                      "https://www.gov.uk/guidance/overseas-pensions-tax-relief-on-your-contributions"),
                  ),
                  text = Set(
                    ExpectedText(selectorForSecondParagraph, "Your client’s overseas pension schemes must not be registered in the UK."),
                    ExpectedText("#main-content > div > div > p:nth-child(4)", "They must also be:"),
                    ExpectedText(selectorForBulletPoint, "paid into after tax (net income)")
                  ),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "Enter the amount your client paid into overseas pension schemes in the correct format",
                      link = "#amount-2")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter the amount your client paid into overseas pension schemes in the correct format",
                      idOpt = Some("amount-2")
                    )
                  )
                ))

            }
            scenarioNameForAgentAndWelsh in {

              implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
              val response = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), Some("x2.64")))

              response must haveStatus(BAD_REQUEST)
              assertPageAsExpected(
                parse(response.body),
                ExpectedYesNoAmountPageContents(
                  title = "Error: Payments into overseas pension schemes",
                  header = "Payments into overseas pension schemes",
                  caption = "Payments into overseas pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection = ExpectedAmountSection("Total amount, in pounds", "For example, £193.52 Do not include payments your client’s employer made", "x2.64"),
                  links = Set(
                    ExpectedLink(
                      "eligibleForTaxRelief-link",
                      "eligible for tax relief (opens in new tab)",
                      "https://www.gov.uk/guidance/overseas-pensions-tax-relief-on-your-contributions"),
                  ),
                  text = Set(
                    ExpectedText(selectorForSecondParagraph, "Your client’s overseas pension schemes must not be registered in the UK."),
                    ExpectedText("#main-content > div > div > p:nth-child(4)", "They must also be:"),
                    ExpectedText(selectorForBulletPoint, "paid into after tax (net income)")
                  ),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "Enter the amount your client paid into overseas pension schemes in the correct format",
                      link = "#amount-2")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter the amount your client paid into overseas pension schemes in the correct format",
                      idOpt = Some("amount-2")
                    )
                  )
                ))

            }
          }
          "the user has selected 'Yes' but has provided an excessive amount, and" when {
            scenarioNameForIndividualAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
              val response = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), Some("100,000,000,000")))

              response must haveStatus(BAD_REQUEST)
              assertPageAsExpected(
                parse(response.body),
                ExpectedYesNoAmountPageContents(
                  title = "Error: Payments into overseas pension schemes",
                  header = "Payments into overseas pension schemes",
                  caption = "Payments into overseas pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection = ExpectedAmountSection("Total amount, in pounds", "For example, £193.52 Do not include payments your employer made", "100,000,000,000"),
                  links = Set(
                    ExpectedLink(
                      "eligibleForTaxRelief-link",
                      "eligible for tax relief (opens in new tab)",
                      "https://www.gov.uk/guidance/overseas-pensions-tax-relief-on-your-contributions"),
                  ),
                  text = Set(
                    ExpectedText(selectorForSecondParagraph, "Your overseas pension schemes must not be registered in the UK."),
                    ExpectedText("#main-content > div > div > p:nth-child(4)", "They must also be:"),
                    ExpectedText(selectorForBulletPoint, "paid into after tax (net income)")
                  ),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "The amount you paid into overseas pension schemes must be less than £100,000,000,000",
                      link = "#amount-2")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: The amount you paid into overseas pension schemes must be less than £100,000,000,000",
                      idOpt = Some("amount-2")
                    )
                  )
                ))

            }
            scenarioNameForIndividualAndWelsh in {

              implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
              val response = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), Some("100,000,000,000")))

              response must haveStatus(BAD_REQUEST)
              assertPageAsExpected(
                parse(response.body),
                ExpectedYesNoAmountPageContents(
                  title = "Error: Payments into overseas pension schemes",
                  header = "Payments into overseas pension schemes",
                  caption = "Payments into overseas pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection = ExpectedAmountSection("Total amount, in pounds", "For example, £193.52 Do not include payments your employer made", "100,000,000,000"),
                  links = Set(
                    ExpectedLink(
                      "eligibleForTaxRelief-link",
                      "eligible for tax relief (opens in new tab)",
                      "https://www.gov.uk/guidance/overseas-pensions-tax-relief-on-your-contributions"),
                  ),
                  text = Set(
                    ExpectedText(selectorForSecondParagraph, "Your overseas pension schemes must not be registered in the UK."),
                    ExpectedText("#main-content > div > div > p:nth-child(4)", "They must also be:"),
                    ExpectedText(selectorForBulletPoint, "paid into after tax (net income)")
                  ),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "The amount you paid into overseas pension schemes must be less than £100,000,000,000",
                      link = "#amount-2")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: The amount you paid into overseas pension schemes must be less than £100,000,000,000",
                      idOpt = Some("amount-2")
                    )
                  )
                ))

            }
            scenarioNameForAgentAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
              val response = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), Some("100,000,000,000")))

              response must haveStatus(BAD_REQUEST)
              assertPageAsExpected(
                parse(response.body),
                ExpectedYesNoAmountPageContents(
                  title = "Error: Payments into overseas pension schemes",
                  header = "Payments into overseas pension schemes",
                  caption = "Payments into overseas pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection = ExpectedAmountSection("Total amount, in pounds", "For example, £193.52 Do not include payments your client’s employer made", "100,000,000,000"),
                  links = Set(
                    ExpectedLink(
                      "eligibleForTaxRelief-link",
                      "eligible for tax relief (opens in new tab)",
                      "https://www.gov.uk/guidance/overseas-pensions-tax-relief-on-your-contributions"),
                  ),
                  text = Set(
                    ExpectedText(selectorForSecondParagraph, "Your client’s overseas pension schemes must not be registered in the UK."),
                    ExpectedText("#main-content > div > div > p:nth-child(4)", "They must also be:"),
                    ExpectedText(selectorForBulletPoint, "paid into after tax (net income)")
                  ),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "The amount your client paid into overseas pension schemes must be less than £100,000,000,000",
                      link = "#amount-2")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: The amount your client paid into overseas pension schemes must be less than £100,000,000,000",
                      idOpt = Some("amount-2")
                    )
                  )
                ))

            }
            scenarioNameForAgentAndWelsh in {

              implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
              val response = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), Some("100,000,000,000")))

              response must haveStatus(BAD_REQUEST)
              assertPageAsExpected(
                parse(response.body),
                ExpectedYesNoAmountPageContents(
                  title = "Error: Payments into overseas pension schemes",
                  header = "Payments into overseas pension schemes",
                  caption = "Payments into overseas pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection = ExpectedAmountSection("Total amount, in pounds", "For example, £193.52 Do not include payments your client’s employer made", "100,000,000,000"),
                  links = Set(
                    ExpectedLink(
                      "eligibleForTaxRelief-link",
                      "eligible for tax relief (opens in new tab)",
                      "https://www.gov.uk/guidance/overseas-pensions-tax-relief-on-your-contributions"),
                  ),
                  text = Set(
                    ExpectedText(selectorForSecondParagraph, "Your client’s overseas pension schemes must not be registered in the UK."),
                    ExpectedText("#main-content > div > div > p:nth-child(4)", "They must also be:"),
                    ExpectedText(selectorForBulletPoint, "paid into after tax (net income)")
                  ),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "The amount your client paid into overseas pension schemes must be less than £100,000,000,000",
                      link = "#amount-2")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: The amount your client paid into overseas pension schemes must be less than £100,000,000,000",
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

  private def getViewModel(implicit userConfig: UserConfig): PaymentsIntoOverseasPensionsViewModel =
    loadPensionUserData.pensions.paymentsIntoOverseasPensions
}







