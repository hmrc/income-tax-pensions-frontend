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

package controllers.pensions.unauthorisedPayments

import builders.PensionsCYAModelBuilder.aPensionsCYAModel
import controllers.ControllerSpec.PreferredLanguages.{English, Welsh}
import controllers.ControllerSpec.UserTypes.{Agent, Individual}
import controllers.ControllerSpec._
import controllers.YesNoAmountControllerSpec
import models.mongo.PensionsUserData
import models.pension.charges.UnauthorisedPaymentsViewModel
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK}
import play.api.libs.ws.WSResponse

class NonUkTaxOnAmountNotSurchargeControllerISpec extends YesNoAmountControllerSpec("/unauthorised-payments-from-pensions/tax-on-amount-not-surcharged") {

  "This page" when {
    "requested to be shown" should {
      "redirect to the summary page" when {
        "the user has no stored session data at all" in {

          implicit val userConfig: UserConfig = userConfigWhenIrrelevant(None)
          implicit val response: WSResponse = getPage

          assertRedirectionAsExpected(PageRelativeURLs.summaryPage)

        }
      }
      "appear as expected" when {
        // TODO (SASS-3850): Similar to what we do with the submission,
        // we should not permit the user to access this page unless they've already provided the surcharge amount
        "even when the user had not previously specified the surcharge amount" in {

          val sessionData = pensionsUserData(aPensionsCYAModel.copy(
            unauthorisedPayments = aPensionsCYAModel.unauthorisedPayments.copy(
              noSurchargeAmount = None
            )
          ))

          implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
          implicit val response: WSResponse = getPage

          response must haveStatus(OK)


        }
        "the user has no session data relevant to this page and" when {

          val sessionData = pensionsUserData(aPensionsCYAModel.copy(
            unauthorisedPayments = aPensionsCYAModel.unauthorisedPayments.copy(
              noSurchargeTaxAmountQuestion = None,
              noSurchargeTaxAmount = None
            )
          ))

          scenarioNameForIndividualAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPageAsExpected(
              OK,
              ExpectedYesNoAmountPageContents(
                title = "Did you pay non-UK tax on the amount that did not result in a surcharge?",
                header = "Did you pay non-UK tax on the amount that did not result in a surcharge?",
                caption = "Unauthorised payments from pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection = ExpectedAmountSection("Total non-UK tax in pounds", "")
              ))

          }
          scenarioNameForIndividualAndWelsh in {

            implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPageAsExpected(
              OK,
              ExpectedYesNoAmountPageContents(
                title = "Did you pay non-UK tax on the amount that did not result in a surcharge?",
                header = "Did you pay non-UK tax on the amount that did not result in a surcharge?",
                caption = "Taliadau heb awdurdod o bensiynau ar gyfer 6 Ebrill 2021 i 5 Ebrill 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection = ExpectedAmountSection("Total non-UK tax in pounds", "")
              ))

          }
          scenarioNameForAgentAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPageAsExpected(
              OK,
              ExpectedYesNoAmountPageContents(
                title = "Did your client pay non-UK tax on the amount that did not result in a surcharge?",
                header = "Did your client pay non-UK tax on the amount that did not result in a surcharge?",
                caption = "Unauthorised payments from pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection = ExpectedAmountSection("Total non-UK tax in pounds", "")
              ))

          }
          scenarioNameForAgentAndWelsh in {

            implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPageAsExpected(
              OK,
              ExpectedYesNoAmountPageContents(
                title = "Did your client pay non-UK tax on the amount that did not result in a surcharge?",
                header = "Did your client pay non-UK tax on the amount that did not result in a surcharge?",
                caption = "Taliadau heb awdurdod o bensiynau ar gyfer 6 Ebrill 2021 i 5 Ebrill 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection = ExpectedAmountSection("Total non-UK tax in pounds", "")
              ))

          }
        }
        "the user had previously answered 'Yes' with a valid amount, and" when {

          val sessionData: PensionsUserData =
            pensionsUserData(aPensionsCYAModel.copy(unauthorisedPayments =
              aPensionsCYAModel.unauthorisedPayments.copy(
                noSurchargeTaxAmountQuestion = Some(true),
                noSurchargeTaxAmount = Some(42.64)
              )
            ))

          scenarioNameForIndividualAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPageAsExpected(
              OK,
              ExpectedYesNoAmountPageContents(
                title = "Did you pay non-UK tax on the amount that did not result in a surcharge?",
                header = "Did you pay non-UK tax on the amount that did not result in a surcharge?",
                caption = "Unauthorised payments from pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = checkedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection = ExpectedAmountSection("Total non-UK tax in pounds", "42.64")
              ))

          }
          scenarioNameForIndividualAndWelsh in {

            implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPageAsExpected(
              OK,
              ExpectedYesNoAmountPageContents(
                title = "Did you pay non-UK tax on the amount that did not result in a surcharge?",
                header = "Did you pay non-UK tax on the amount that did not result in a surcharge?",
                caption = "Taliadau heb awdurdod o bensiynau ar gyfer 6 Ebrill 2021 i 5 Ebrill 2022",
                radioButtonForYes = checkedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection = ExpectedAmountSection("Total non-UK tax in pounds", "42.64")
              ))

          }
          scenarioNameForAgentAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPageAsExpected(
              OK,
              ExpectedYesNoAmountPageContents(
                title = "Did your client pay non-UK tax on the amount that did not result in a surcharge?",
                header = "Did your client pay non-UK tax on the amount that did not result in a surcharge?",
                caption = "Unauthorised payments from pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = checkedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection = ExpectedAmountSection("Total non-UK tax in pounds", "42.64")
              ))

          }
          scenarioNameForAgentAndWelsh in {

            implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPageAsExpected(
              OK,
              ExpectedYesNoAmountPageContents(
                title = "Did your client pay non-UK tax on the amount that did not result in a surcharge?",
                header = "Did your client pay non-UK tax on the amount that did not result in a surcharge?",
                caption = "Taliadau heb awdurdod o bensiynau ar gyfer 6 Ebrill 2021 i 5 Ebrill 2022",
                radioButtonForYes = checkedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection = ExpectedAmountSection("Total non-UK tax in pounds", "42.64")
              ))

          }

        }
        // TODO (SASS-3850): When we've previously answered 'No', we should always preselect it -
        // regardless of the amount; and we shouldn't have any value in the amount box.
        "the user had previously answered 'No' without an amount, and" when {

          val sessionData: PensionsUserData =
            pensionsUserData(aPensionsCYAModel.copy(unauthorisedPayments =
              aPensionsCYAModel.unauthorisedPayments.copy(
                noSurchargeTaxAmountQuestion = Some(false),
                noSurchargeTaxAmount = None
              )
            ))

          scenarioNameForIndividualAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPageAsExpected(
              OK,
              ExpectedYesNoAmountPageContents(
                title = "Did you pay non-UK tax on the amount that did not result in a surcharge?",
                header = "Did you pay non-UK tax on the amount that did not result in a surcharge?",
                caption = "Unauthorised payments from pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection = ExpectedAmountSection("Total non-UK tax in pounds", "")
              ))

          }
          scenarioNameForIndividualAndWelsh in {

            implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPageAsExpected(
              OK,
              ExpectedYesNoAmountPageContents(
                title = "Did you pay non-UK tax on the amount that did not result in a surcharge?",
                header = "Did you pay non-UK tax on the amount that did not result in a surcharge?",
                caption = "Taliadau heb awdurdod o bensiynau ar gyfer 6 Ebrill 2021 i 5 Ebrill 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection = ExpectedAmountSection("Total non-UK tax in pounds", "")
              ))

          }
          scenarioNameForAgentAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPageAsExpected(
              OK,
              ExpectedYesNoAmountPageContents(
                title = "Did your client pay non-UK tax on the amount that did not result in a surcharge?",
                header = "Did your client pay non-UK tax on the amount that did not result in a surcharge?",
                caption = "Unauthorised payments from pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection = ExpectedAmountSection("Total non-UK tax in pounds", "")
              ))

          }
          scenarioNameForAgentAndWelsh in {

            implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPageAsExpected(
              OK,
              ExpectedYesNoAmountPageContents(
                title = "Did your client pay non-UK tax on the amount that did not result in a surcharge?",
                header = "Did your client pay non-UK tax on the amount that did not result in a surcharge?",
                caption = "Taliadau heb awdurdod o bensiynau ar gyfer 6 Ebrill 2021 i 5 Ebrill 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection = ExpectedAmountSection("Total non-UK tax in pounds", "")))
          }

        }
        // TODO (SASS-3850): When we've previously answered 'No', we should always preselect it -
        // regardless of the amount; and we shouldn't have any value in the amount box.
        "the user had previously answered 'No' with an amount of zero, and" when {

          val sessionData: PensionsUserData =
            pensionsUserData(aPensionsCYAModel.copy(unauthorisedPayments =
              aPensionsCYAModel.unauthorisedPayments.copy(
                noSurchargeTaxAmountQuestion = Some(false),
                noSurchargeTaxAmount = Some(BigDecimal(0))
              )
            ))

          scenarioNameForIndividualAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPageAsExpected(
              OK,
              ExpectedYesNoAmountPageContents(
                title = "Did you pay non-UK tax on the amount that did not result in a surcharge?",
                header = "Did you pay non-UK tax on the amount that did not result in a surcharge?",
                caption = "Unauthorised payments from pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = checkedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection = ExpectedAmountSection("Total non-UK tax in pounds", "0")
              ))

          }
          scenarioNameForIndividualAndWelsh in {

            implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPageAsExpected(
              OK,
              ExpectedYesNoAmountPageContents(
                title = "Did you pay non-UK tax on the amount that did not result in a surcharge?",
                header = "Did you pay non-UK tax on the amount that did not result in a surcharge?",
                caption = "Taliadau heb awdurdod o bensiynau ar gyfer 6 Ebrill 2021 i 5 Ebrill 2022",
                radioButtonForYes = checkedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection = ExpectedAmountSection("Total non-UK tax in pounds", "0")
              ))

          }
          scenarioNameForAgentAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPageAsExpected(
              OK,
              ExpectedYesNoAmountPageContents(
                title = "Did your client pay non-UK tax on the amount that did not result in a surcharge?",
                header = "Did your client pay non-UK tax on the amount that did not result in a surcharge?",
                caption = "Unauthorised payments from pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = checkedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection = ExpectedAmountSection("Total non-UK tax in pounds", "0")
              ))

          }
          scenarioNameForAgentAndWelsh in {

            implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPageAsExpected(
              OK,
              ExpectedYesNoAmountPageContents(
                title = "Did your client pay non-UK tax on the amount that did not result in a surcharge?",
                header = "Did your client pay non-UK tax on the amount that did not result in a surcharge?",
                caption = "Taliadau heb awdurdod o bensiynau ar gyfer 6 Ebrill 2021 i 5 Ebrill 2022",
                radioButtonForYes = checkedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection = ExpectedAmountSection("Total non-UK tax in pounds", "0")))
          }

        }
        // TODO: When we've previously answered 'No', we should always preselect it -
        // regardless of the amount; and we shouldn't have any value in the amount box.
        "the user had previously answered 'No' with a negative amount, and" when {

          val sessionData: PensionsUserData =
            pensionsUserData(aPensionsCYAModel.copy(unauthorisedPayments =
              aPensionsCYAModel.unauthorisedPayments.copy(
                noSurchargeTaxAmountQuestion = Some(false),
                noSurchargeTaxAmount = Some(BigDecimal(-42.64))
              )
            ))

          scenarioNameForIndividualAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPageAsExpected(
              OK,
              ExpectedYesNoAmountPageContents(
                title = "Did you pay non-UK tax on the amount that did not result in a surcharge?",
                header = "Did you pay non-UK tax on the amount that did not result in a surcharge?",
                caption = "Unauthorised payments from pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = checkedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection = ExpectedAmountSection("Total non-UK tax in pounds", "-42.64")
              ))

          }
          scenarioNameForIndividualAndWelsh in {

            implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPageAsExpected(
              OK,
              ExpectedYesNoAmountPageContents(
                title = "Did you pay non-UK tax on the amount that did not result in a surcharge?",
                header = "Did you pay non-UK tax on the amount that did not result in a surcharge?",
                caption = "Taliadau heb awdurdod o bensiynau ar gyfer 6 Ebrill 2021 i 5 Ebrill 2022",
                radioButtonForYes = checkedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection = ExpectedAmountSection("Total non-UK tax in pounds", "-42.64")
              ))

          }
          scenarioNameForAgentAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPageAsExpected(
              OK,
              ExpectedYesNoAmountPageContents(
                title = "Did your client pay non-UK tax on the amount that did not result in a surcharge?",
                header = "Did your client pay non-UK tax on the amount that did not result in a surcharge?",
                caption = "Unauthorised payments from pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = checkedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection = ExpectedAmountSection("Total non-UK tax in pounds", "-42.64")
              ))

          }
          scenarioNameForAgentAndWelsh in {

            implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPageAsExpected(
              OK,
              ExpectedYesNoAmountPageContents(
                title = "Did your client pay non-UK tax on the amount that did not result in a surcharge?",
                header = "Did your client pay non-UK tax on the amount that did not result in a surcharge?",
                caption = "Taliadau heb awdurdod o bensiynau ar gyfer 6 Ebrill 2021 i 5 Ebrill 2022",
                radioButtonForYes = checkedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection = ExpectedAmountSection("Total non-UK tax in pounds", "-42.64")))
          }

        }
      }
    }
    "submitted" should {
      "redirect to the expected page" when {
        "the user had not previously specified the 'no surcharge' amount" in {

          val sessionData: PensionsUserData =
            pensionsUserData(aPensionsCYAModel.copy(unauthorisedPayments =
              aPensionsCYAModel.unauthorisedPayments.copy(
                noSurchargeAmount = None,
                noSurchargeTaxAmountQuestion = None,
                noSurchargeTaxAmount = None
              )
            ))
          val expectedViewModel = sessionData.pensions.unauthorisedPayments

          implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
          implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(false), None))

          assertRedirectionAsExpected(PageRelativeURLs.summaryPage)
          getViewModel mustBe Some(expectedViewModel)

        }
      }
      "succeed" when {
        "the user has relevant session data and" when {

          val sessionData = pensionsUserData(aPensionsCYAModel)

          // TODO (SASS-3850): When selecting 'No', we should always store the amount as 'None'.
          "the user has selected 'No' with a blank amount" in {

            val expectedViewModel =
              sessionData.pensions.unauthorisedPayments.copy(
                noSurchargeTaxAmountQuestion = Some(false),
                noSurchargeTaxAmount = Some(BigDecimal(0))
              )

            implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
            implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(false), Some("")))

            assertRedirectionAsExpected(relativeUrl("/unauthorised-payments-from-pensions/uk-pension-scheme"))
            getViewModel mustBe Some(expectedViewModel)

          }
          "the user has selected 'Yes' as well as an amount of zero, and" in {

            val expectedViewModel =
              sessionData.pensions.unauthorisedPayments.copy(
                noSurchargeTaxAmountQuestion = Some(true),
                noSurchargeTaxAmount = Some(BigDecimal(0))
              )

            implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
            implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), Some("0")))

            assertRedirectionAsExpected(relativeUrl("/unauthorised-payments-from-pensions/uk-pension-scheme"))
            getViewModel mustBe Some(expectedViewModel)

          }
          "the user has selected 'Yes' as well as a valid amount (unformatted), and" in {

            val expectedViewModel =
              sessionData.pensions.unauthorisedPayments.copy(
                noSurchargeTaxAmountQuestion = Some(true),
                noSurchargeTaxAmount = Some(BigDecimal(42.64))
              )

            implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
            implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), Some("42.64")))

            assertRedirectionAsExpected(relativeUrl("/unauthorised-payments-from-pensions/uk-pension-scheme"))
            getViewModel mustBe Some(expectedViewModel)

          }
          "the user has selected 'Yes' as well as a valid amount (formatted), and" in {

            val expectedViewModel =
              sessionData.pensions.unauthorisedPayments.copy(
                noSurchargeTaxAmountQuestion = Some(true),
                noSurchargeTaxAmount = Some(BigDecimal(1042.64))
              )

            implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
            implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), Some("£1,042.64")))

            assertRedirectionAsExpected(relativeUrl("/unauthorised-payments-from-pensions/uk-pension-scheme"))
            getViewModel mustBe Some(expectedViewModel)

          }

        }
      }
      "fail" when {
        // TODO (SASS-3850): Typically, we'd redirect to the summary page is this case.
        "the user has no stored session data at all" in {

          implicit val userConfig: UserConfig = userConfigWhenIrrelevant(None)
          implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(false), None))

          response must haveStatus(INTERNAL_SERVER_ERROR)

        }
        "the user has no session data relevant to this page and" when {

          val sessionData = pensionsUserData(aPensionsCYAModel.copy(
            unauthorisedPayments = aPensionsCYAModel.unauthorisedPayments.copy(
              noSurchargeTaxAmountQuestion = None,
              noSurchargeTaxAmount = None
            )
          ))

          val expectedViewModel = sessionData.pensions.unauthorisedPayments

          "the user has selected neither 'Yes' nor 'No' and" when {
            scenarioNameForIndividualAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(None, None))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedYesNoAmountPageContents(
                  title = "Error: Did you pay non-UK tax on the amount that did not result in a surcharge?",
                  header = "Did you pay non-UK tax on the amount that did not result in a surcharge?",
                  caption = "Unauthorised payments from pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection = ExpectedAmountSection("Total non-UK tax in pounds", ""),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "Select yes if you paid non-UK tax on the amount not surcharged",
                      link = "#value")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Select yes if you paid non-UK tax on the amount not surcharged",
                      idOpt = Some("value")
                    )
                  )
                ))
              getViewModel mustBe Some(expectedViewModel)

            }
            scenarioNameForIndividualAndWelsh in {

              implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(None, None))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedYesNoAmountPageContents(
                  title = "Error: Did you pay non-UK tax on the amount that did not result in a surcharge?",
                  header = "Did you pay non-UK tax on the amount that did not result in a surcharge?",
                  caption = "Taliadau heb awdurdod o bensiynau ar gyfer 6 Ebrill 2021 i 5 Ebrill 2022",
                  radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection = ExpectedAmountSection("Total non-UK tax in pounds", ""),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "Select yes if you paid non-UK tax on the amount not surcharged",
                      link = "#value")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Select yes if you paid non-UK tax on the amount not surcharged",
                      idOpt = Some("value")
                    )
                  )
                ))
              getViewModel mustBe Some(expectedViewModel)

            }
            scenarioNameForAgentAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(None, None))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedYesNoAmountPageContents(
                  title = "Error: Did your client pay non-UK tax on the amount that did not result in a surcharge?",
                  header = "Did your client pay non-UK tax on the amount that did not result in a surcharge?",
                  caption = "Unauthorised payments from pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection = ExpectedAmountSection("Total non-UK tax in pounds", ""),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "Select yes if you paid non-UK tax on the amount not surcharged",
                      link = "#value")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Select yes if you paid non-UK tax on the amount not surcharged",
                      idOpt = Some("value")
                    )
                  )
                ))
              getViewModel mustBe Some(expectedViewModel)

            }
            scenarioNameForAgentAndWelsh in {

              implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(None, None))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedYesNoAmountPageContents(
                  title = "Error: Did your client pay non-UK tax on the amount that did not result in a surcharge?",
                  header = "Did your client pay non-UK tax on the amount that did not result in a surcharge?",
                  caption = "Taliadau heb awdurdod o bensiynau ar gyfer 6 Ebrill 2021 i 5 Ebrill 2022",
                  radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection = ExpectedAmountSection("Total non-UK tax in pounds", ""),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "Select yes if you paid non-UK tax on the amount not surcharged",
                      link = "#value")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Select yes if you paid non-UK tax on the amount not surcharged",
                      idOpt = Some("value")
                    )
                  )
                ))
              getViewModel mustBe Some(expectedViewModel)

            }
          }
          "the user has selected 'Yes' but has provided an empty amount, and" when {
            scenarioNameForIndividualAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), Some("")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedYesNoAmountPageContents(
                  title = "Error: Did you pay non-UK tax on the amount that did not result in a surcharge?",
                  header = "Did you pay non-UK tax on the amount that did not result in a surcharge?",
                  caption = "Unauthorised payments from pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection = ExpectedAmountSection("Total non-UK tax in pounds", ""),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "Enter the amount of non-UK tax paid",
                      link = "#amount-2")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter the amount of non-UK tax paid",
                      idOpt = Some("amount-2")
                    )
                  )
                ))
              getViewModel mustBe Some(expectedViewModel)

            }
            scenarioNameForIndividualAndWelsh in {

              implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), None))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedYesNoAmountPageContents(
                  title = "Error: Did you pay non-UK tax on the amount that did not result in a surcharge?",
                  header = "Did you pay non-UK tax on the amount that did not result in a surcharge?",
                  caption = "Taliadau heb awdurdod o bensiynau ar gyfer 6 Ebrill 2021 i 5 Ebrill 2022",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection = ExpectedAmountSection("Total non-UK tax in pounds", ""),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "Enter the amount of non-UK tax paid",
                      link = "#amount-2")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter the amount of non-UK tax paid",
                      idOpt = Some("amount-2")
                    )
                  )
                ))
              getViewModel mustBe Some(expectedViewModel)

            }
            scenarioNameForAgentAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), None))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedYesNoAmountPageContents(
                  title = "Error: Did your client pay non-UK tax on the amount that did not result in a surcharge?",
                  header = "Did your client pay non-UK tax on the amount that did not result in a surcharge?",
                  caption = "Unauthorised payments from pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection = ExpectedAmountSection("Total non-UK tax in pounds", ""),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "Enter the amount of non-UK tax paid",
                      link = "#amount-2")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter the amount of non-UK tax paid",
                      idOpt = Some("amount-2")
                    )
                  )
                ))
              getViewModel mustBe Some(expectedViewModel)

            }
            scenarioNameForAgentAndWelsh in {

              implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), None))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedYesNoAmountPageContents(
                  title = "Error: Did your client pay non-UK tax on the amount that did not result in a surcharge?",
                  header = "Did your client pay non-UK tax on the amount that did not result in a surcharge?",
                  caption = "Taliadau heb awdurdod o bensiynau ar gyfer 6 Ebrill 2021 i 5 Ebrill 2022",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection = ExpectedAmountSection("Total non-UK tax in pounds", ""),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "Enter the amount of non-UK tax paid",
                      link = "#amount-2")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter the amount of non-UK tax paid",
                      idOpt = Some("amount-2")
                    )
                  )
                ))
              getViewModel mustBe Some(expectedViewModel)

            }
          }
          "the user has selected 'Yes' but has provided an amount of an invalid format, and" when {
            scenarioNameForIndividualAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), Some("x2.64")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedYesNoAmountPageContents(
                  title = "Error: Did you pay non-UK tax on the amount that did not result in a surcharge?",
                  header = "Did you pay non-UK tax on the amount that did not result in a surcharge?",
                  caption = "Unauthorised payments from pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection = ExpectedAmountSection("Total non-UK tax in pounds", "x2.64"),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "Enter the amount of non-UK tax in the correct format",
                      link = "#amount-2")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter the amount of non-UK tax in the correct format",
                      idOpt = Some("amount-2")
                    )
                  )
                ))
              getViewModel mustBe Some(expectedViewModel)

            }
            scenarioNameForIndividualAndWelsh in {

              implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), Some("x2.64")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedYesNoAmountPageContents(
                  title = "Error: Did you pay non-UK tax on the amount that did not result in a surcharge?",
                  header = "Did you pay non-UK tax on the amount that did not result in a surcharge?",
                  caption = "Taliadau heb awdurdod o bensiynau ar gyfer 6 Ebrill 2021 i 5 Ebrill 2022",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection = ExpectedAmountSection("Total non-UK tax in pounds", "x2.64"),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "Enter the amount of non-UK tax in the correct format",
                      link = "#amount-2")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter the amount of non-UK tax in the correct format",
                      idOpt = Some("amount-2")
                    )
                  )
                ))
              getViewModel mustBe Some(expectedViewModel)

            }
            scenarioNameForAgentAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), Some("x2.64")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedYesNoAmountPageContents(
                  title = "Error: Did your client pay non-UK tax on the amount that did not result in a surcharge?",
                  header = "Did your client pay non-UK tax on the amount that did not result in a surcharge?",
                  caption = "Unauthorised payments from pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection = ExpectedAmountSection("Total non-UK tax in pounds", "x2.64"),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "Enter the amount of non-UK tax in the correct format",
                      link = "#amount-2")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter the amount of non-UK tax in the correct format",
                      idOpt = Some("amount-2")
                    )
                  )
                ))
              getViewModel mustBe Some(expectedViewModel)

            }
            scenarioNameForAgentAndWelsh in {

              implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), Some("x2.64")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedYesNoAmountPageContents(
                  title = "Error: Did your client pay non-UK tax on the amount that did not result in a surcharge?",
                  header = "Did your client pay non-UK tax on the amount that did not result in a surcharge?",
                  caption = "Taliadau heb awdurdod o bensiynau ar gyfer 6 Ebrill 2021 i 5 Ebrill 2022",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection = ExpectedAmountSection("Total non-UK tax in pounds", "x2.64"),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "Enter the amount of non-UK tax in the correct format",
                      link = "#amount-2")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter the amount of non-UK tax in the correct format",
                      idOpt = Some("amount-2")
                    )
                  )
                ))
              getViewModel mustBe Some(expectedViewModel)

            }
          }
          "the user has selected 'Yes' but has provided an amount of a negative format, and" when {
            scenarioNameForIndividualAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), Some("-42.64")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedYesNoAmountPageContents(
                  title = "Error: Did you pay non-UK tax on the amount that did not result in a surcharge?",
                  header = "Did you pay non-UK tax on the amount that did not result in a surcharge?",
                  caption = "Unauthorised payments from pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection = ExpectedAmountSection("Total non-UK tax in pounds", "-42.64"),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "Enter the amount of non-UK tax in the correct format",
                      link = "#amount-2")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter the amount of non-UK tax in the correct format",
                      idOpt = Some("amount-2")
                    )
                  )
                ))
              getViewModel mustBe Some(expectedViewModel)

            }
            scenarioNameForIndividualAndWelsh in {

              implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), Some("-42.64")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedYesNoAmountPageContents(
                  title = "Error: Did you pay non-UK tax on the amount that did not result in a surcharge?",
                  header = "Did you pay non-UK tax on the amount that did not result in a surcharge?",
                  caption = "Taliadau heb awdurdod o bensiynau ar gyfer 6 Ebrill 2021 i 5 Ebrill 2022",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection = ExpectedAmountSection("Total non-UK tax in pounds", "-42.64"),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "Enter the amount of non-UK tax in the correct format",
                      link = "#amount-2")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter the amount of non-UK tax in the correct format",
                      idOpt = Some("amount-2")
                    )
                  )
                ))
              getViewModel mustBe Some(expectedViewModel)

            }
            scenarioNameForAgentAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), Some("-42.64")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedYesNoAmountPageContents(
                  title = "Error: Did your client pay non-UK tax on the amount that did not result in a surcharge?",
                  header = "Did your client pay non-UK tax on the amount that did not result in a surcharge?",
                  caption = "Unauthorised payments from pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection = ExpectedAmountSection("Total non-UK tax in pounds", "-42.64"),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "Enter the amount of non-UK tax in the correct format",
                      link = "#amount-2")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter the amount of non-UK tax in the correct format",
                      idOpt = Some("amount-2")
                    )
                  )
                ))
              getViewModel mustBe Some(expectedViewModel)

            }
            scenarioNameForAgentAndWelsh in {

              implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), Some("-42.64")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedYesNoAmountPageContents(
                  title = "Error: Did your client pay non-UK tax on the amount that did not result in a surcharge?",
                  header = "Did your client pay non-UK tax on the amount that did not result in a surcharge?",
                  caption = "Taliadau heb awdurdod o bensiynau ar gyfer 6 Ebrill 2021 i 5 Ebrill 2022",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection = ExpectedAmountSection("Total non-UK tax in pounds", "-42.64"),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "Enter the amount of non-UK tax in the correct format",
                      link = "#amount-2")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter the amount of non-UK tax in the correct format",
                      idOpt = Some("amount-2")
                    )
                  )
                ))
              getViewModel mustBe Some(expectedViewModel)

            }
          }
          "the user has selected 'Yes' but has provided an excessive amount, and" when {
            scenarioNameForIndividualAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), Some("100000000002")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedYesNoAmountPageContents(
                  title = "Error: Did you pay non-UK tax on the amount that did not result in a surcharge?",
                  header = "Did you pay non-UK tax on the amount that did not result in a surcharge?",
                  caption = "Unauthorised payments from pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection = ExpectedAmountSection("Total non-UK tax in pounds", "100000000002"),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "The amount of non-UK tax paid must be less than £100,000,000,000",
                      link = "#amount-2")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: The amount of non-UK tax paid must be less than £100,000,000,000",
                      idOpt = Some("amount-2")
                    )
                  )
                ))
              getViewModel mustBe Some(expectedViewModel)

            }
            scenarioNameForIndividualAndWelsh in {

              implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), Some("100000000002")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedYesNoAmountPageContents(
                  title = "Error: Did you pay non-UK tax on the amount that did not result in a surcharge?",
                  header = "Did you pay non-UK tax on the amount that did not result in a surcharge?",
                  caption = "Taliadau heb awdurdod o bensiynau ar gyfer 6 Ebrill 2021 i 5 Ebrill 2022",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection = ExpectedAmountSection("Total non-UK tax in pounds", "100000000002"),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "The amount of non-UK tax paid must be less than £100,000,000,000",
                      link = "#amount-2")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: The amount of non-UK tax paid must be less than £100,000,000,000",
                      idOpt = Some("amount-2")
                    )
                  )
                ))
              getViewModel mustBe Some(expectedViewModel)

            }
            scenarioNameForAgentAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), Some("100000000002")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedYesNoAmountPageContents(
                  title = "Error: Did your client pay non-UK tax on the amount that did not result in a surcharge?",
                  header = "Did your client pay non-UK tax on the amount that did not result in a surcharge?",
                  caption = "Unauthorised payments from pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection = ExpectedAmountSection("Total non-UK tax in pounds", "100000000002"),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "The amount of non-UK tax paid must be less than £100,000,000,000",
                      link = "#amount-2")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: The amount of non-UK tax paid must be less than £100,000,000,000",
                      idOpt = Some("amount-2")
                    )
                  )
                ))
              getViewModel mustBe Some(expectedViewModel)

            }
            scenarioNameForAgentAndWelsh in {

              implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), Some("100000000002")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedYesNoAmountPageContents(
                  title = "Error: Did your client pay non-UK tax on the amount that did not result in a surcharge?",
                  header = "Did your client pay non-UK tax on the amount that did not result in a surcharge?",
                  caption = "Taliadau heb awdurdod o bensiynau ar gyfer 6 Ebrill 2021 i 5 Ebrill 2022",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection = ExpectedAmountSection("Total non-UK tax in pounds", "100000000002"),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "The amount of non-UK tax paid must be less than £100,000,000,000",
                      link = "#amount-2")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: The amount of non-UK tax paid must be less than £100,000,000,000",
                      idOpt = Some("amount-2")
                    )
                  )
                ))
              getViewModel mustBe Some(expectedViewModel)

            }
          }
        }
      }
    }
  }

  private def getViewModel(implicit userConfig: UserConfig): Option[UnauthorisedPaymentsViewModel] =
    loadPensionUserData.map(_.pensions.unauthorisedPayments)


}
