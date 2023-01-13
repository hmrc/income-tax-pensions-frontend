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

package controllers.pensions.unauthorisedPayments

import builders.PensionsCYAModelBuilder.aPensionsCYAModel
import controllers.ControllerSpec.PreferredLanguages.{English, Welsh}
import controllers.ControllerSpec.UserTypes.{Agent, Individual}
import controllers.ControllerSpec._
import controllers.YesNoAmountControllerSpec
import models.mongo.PensionsUserData
import models.pension.charges.UnauthorisedPaymentsViewModel
import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.libs.ws.WSResponse

class NonUKTaxOnAmountResultedInSurchargeControllerISpec
  extends YesNoAmountControllerSpec("/unauthorised-payments-from-pensions/tax-on-amount-surcharged") {

  "This page" when {
    "requested to be shown" should {
      "redirect to the summary page" when {
        "the user has no stored session data at all" in {

          implicit val userConfig: UserConfig = userConfigWhenIrrelevant(None)
          implicit val response: WSResponse = getPage

          assertRedirectionAsExpected(PageRelativeURLs.unauthorisedPaymentsCYAPage)

        }
        "the user had not previously specified the surcharge amount" in {

          val sessionData = pensionsUserData(aPensionsCYAModel.copy(
            unauthorisedPayments = aPensionsCYAModel.unauthorisedPayments.copy(
              surchargeAmount = None,
              surchargeTaxAmountQuestion = None,
              surchargeTaxAmount = None
            )
          ))

          implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
          implicit val response: WSResponse = getPage

          assertRedirectionAsExpected(relativeUrl("/unauthorised-payments-from-pensions/amount-not-surcharged"))
        }
      }
      "appear as expected" when {
        "the user has no session data relevant to this page and" when {

          val sessionData = pensionsUserData(aPensionsCYAModel.copy(
            unauthorisedPayments = aPensionsCYAModel.unauthorisedPayments.copy(
              surchargeTaxAmountQuestion = None,
              surchargeTaxAmount = None
            )
          ))

          scenarioNameForIndividualAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPageAsExpected(
              OK,
              ExpectedYesNoAmountPageContents(
                title = "Did you pay non-UK tax on the amount that resulted in a surcharge?",
                header = "Did you pay non-UK tax on the amount that resulted in a surcharge?",
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
                title = "Did you pay non-UK tax on the amount that resulted in a surcharge?",
                header = "Did you pay non-UK tax on the amount that resulted in a surcharge?",
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
                title = "Did your client pay non-UK tax on the amount that resulted in a surcharge?",
                header = "Did your client pay non-UK tax on the amount that resulted in a surcharge?",
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
                title = "Did your client pay non-UK tax on the amount that resulted in a surcharge?",
                header = "Did your client pay non-UK tax on the amount that resulted in a surcharge?",
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
                surchargeTaxAmountQuestion = Some(true),
                surchargeTaxAmount = Some(42.64)
              )
            ))

          scenarioNameForIndividualAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPageAsExpected(
              OK,
              ExpectedYesNoAmountPageContents(
                title = "Did you pay non-UK tax on the amount that resulted in a surcharge?",
                header = "Did you pay non-UK tax on the amount that resulted in a surcharge?",
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
                title = "Did you pay non-UK tax on the amount that resulted in a surcharge?",
                header = "Did you pay non-UK tax on the amount that resulted in a surcharge?",
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
                title = "Did your client pay non-UK tax on the amount that resulted in a surcharge?",
                header = "Did your client pay non-UK tax on the amount that resulted in a surcharge?",
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
                title = "Did your client pay non-UK tax on the amount that resulted in a surcharge?",
                header = "Did your client pay non-UK tax on the amount that resulted in a surcharge?",
                caption = "Taliadau heb awdurdod o bensiynau ar gyfer 6 Ebrill 2021 i 5 Ebrill 2022",
                radioButtonForYes = checkedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection = ExpectedAmountSection("Total non-UK tax in pounds", "42.64")
              ))

          }

        }
        "the user had previously answered 'No' without an amount, and" when {

          val sessionData: PensionsUserData =
            pensionsUserData(aPensionsCYAModel.copy(unauthorisedPayments =
              aPensionsCYAModel.unauthorisedPayments.copy(
                surchargeTaxAmountQuestion = Some(false),
                surchargeTaxAmount = None
              )
            ))

          scenarioNameForIndividualAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPageAsExpected(
              OK,
              ExpectedYesNoAmountPageContents(
                title = "Did you pay non-UK tax on the amount that resulted in a surcharge?",
                header = "Did you pay non-UK tax on the amount that resulted in a surcharge?",
                caption = "Unauthorised payments from pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = checkedExpectedRadioButton("No"),
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
                title = "Did you pay non-UK tax on the amount that resulted in a surcharge?",
                header = "Did you pay non-UK tax on the amount that resulted in a surcharge?",
                caption = "Taliadau heb awdurdod o bensiynau ar gyfer 6 Ebrill 2021 i 5 Ebrill 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = checkedExpectedRadioButton("No"),
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
                title = "Did your client pay non-UK tax on the amount that resulted in a surcharge?",
                header = "Did your client pay non-UK tax on the amount that resulted in a surcharge?",
                caption = "Unauthorised payments from pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = checkedExpectedRadioButton("No"),
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
                title = "Did your client pay non-UK tax on the amount that resulted in a surcharge?",
                header = "Did your client pay non-UK tax on the amount that resulted in a surcharge?",
                caption = "Taliadau heb awdurdod o bensiynau ar gyfer 6 Ebrill 2021 i 5 Ebrill 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = checkedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection = ExpectedAmountSection("Total non-UK tax in pounds", "")))
          }

        }
        "the user had previously answered 'No' with an amount of zero, and" when {

          val sessionData: PensionsUserData =
            pensionsUserData(aPensionsCYAModel.copy(unauthorisedPayments =
              aPensionsCYAModel.unauthorisedPayments.copy(
                surchargeTaxAmountQuestion = Some(false),
                surchargeTaxAmount = Some(BigDecimal(0))
              )
            ))

          scenarioNameForIndividualAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPageAsExpected(
              OK,
              ExpectedYesNoAmountPageContents(
                title = "Did you pay non-UK tax on the amount that resulted in a surcharge?",
                header = "Did you pay non-UK tax on the amount that resulted in a surcharge?",
                caption = "Unauthorised payments from pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = checkedExpectedRadioButton("No"),
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
                title = "Did you pay non-UK tax on the amount that resulted in a surcharge?",
                header = "Did you pay non-UK tax on the amount that resulted in a surcharge?",
                caption = "Taliadau heb awdurdod o bensiynau ar gyfer 6 Ebrill 2021 i 5 Ebrill 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = checkedExpectedRadioButton("No"),
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
                title = "Did your client pay non-UK tax on the amount that resulted in a surcharge?",
                header = "Did your client pay non-UK tax on the amount that resulted in a surcharge?",
                caption = "Unauthorised payments from pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = checkedExpectedRadioButton("No"),
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
                title = "Did your client pay non-UK tax on the amount that resulted in a surcharge?",
                header = "Did your client pay non-UK tax on the amount that resulted in a surcharge?",
                caption = "Taliadau heb awdurdod o bensiynau ar gyfer 6 Ebrill 2021 i 5 Ebrill 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = checkedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection = ExpectedAmountSection("Total non-UK tax in pounds", "")))
          }

        }
        "the user had previously answered 'No' with a negative amount, and" when {

          val sessionData: PensionsUserData =
            pensionsUserData(aPensionsCYAModel.copy(unauthorisedPayments =
              aPensionsCYAModel.unauthorisedPayments.copy(
                surchargeTaxAmountQuestion = Some(false),
                surchargeTaxAmount = Some(BigDecimal(-42.64))
              )
            ))

          scenarioNameForIndividualAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPageAsExpected(
              OK,
              ExpectedYesNoAmountPageContents(
                title = "Did you pay non-UK tax on the amount that resulted in a surcharge?",
                header = "Did you pay non-UK tax on the amount that resulted in a surcharge?",
                caption = "Unauthorised payments from pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = checkedExpectedRadioButton("No"),
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
                title = "Did you pay non-UK tax on the amount that resulted in a surcharge?",
                header = "Did you pay non-UK tax on the amount that resulted in a surcharge?",
                caption = "Taliadau heb awdurdod o bensiynau ar gyfer 6 Ebrill 2021 i 5 Ebrill 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = checkedExpectedRadioButton("No"),
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
                title = "Did your client pay non-UK tax on the amount that resulted in a surcharge?",
                header = "Did your client pay non-UK tax on the amount that resulted in a surcharge?",
                caption = "Unauthorised payments from pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = checkedExpectedRadioButton("No"),
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
                title = "Did your client pay non-UK tax on the amount that resulted in a surcharge?",
                header = "Did your client pay non-UK tax on the amount that resulted in a surcharge?",
                caption = "Taliadau heb awdurdod o bensiynau ar gyfer 6 Ebrill 2021 i 5 Ebrill 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = checkedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection = ExpectedAmountSection("Total non-UK tax in pounds", "")
              ))
          }

        }

      }
    }
    "submitted" should {
      "redirect to the expected page" when {
        "the user has no stored session data at all" in {

          implicit val userConfig: UserConfig = userConfigWhenIrrelevant(None)
          implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(false), None))

          assertRedirectionAsExpected(PageRelativeURLs.unauthorisedPaymentsCYAPage)
          getViewModel mustBe None

        }
        "the user had not previously specified the surcharge amount" in {

          val sessionData: PensionsUserData =
            pensionsUserData(aPensionsCYAModel.copy(unauthorisedPayments =
              aPensionsCYAModel.unauthorisedPayments.copy(
                surchargeAmount = None,
                surchargeTaxAmountQuestion = None,
                surchargeTaxAmount = None
              )
            ))
          val expectedViewModel = sessionData.pensions.unauthorisedPayments

          implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
          implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(false), None))

          assertRedirectionAsExpected(relativeUrl("/unauthorised-payments-from-pensions/amount-not-surcharged"))
          getViewModel mustBe Some(expectedViewModel)

        }
      }
      "succeed" when {
        "the user has session data relevant to this page and" when {

          val sessionData = pensionsUserData(aPensionsCYAModel)

          "the user has selected 'No' and" in {

            val expectedViewModel =
              sessionData.pensions.unauthorisedPayments.copy(
                surchargeTaxAmountQuestion = Some(false),
                surchargeTaxAmount = None
              )

            implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
            implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(false), None))

            assertRedirectionAsExpected(relativeUrl("/unauthorised-payments-from-pensions/amount-not-surcharged"))
            getViewModel mustBe Some(expectedViewModel)

          }
          "the user has selected 'Yes' as well as a valid amount (unformatted), and" in {

            val expectedViewModel =
              sessionData.pensions.unauthorisedPayments.copy(
                surchargeTaxAmountQuestion = Some(true),
                surchargeTaxAmount = Some(BigDecimal(42.64))
              )

            implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
            implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), Some("42.64")))

            assertRedirectionAsExpected(relativeUrl("/unauthorised-payments-from-pensions/amount-not-surcharged"))
            getViewModel mustBe Some(expectedViewModel)

          }
          "the user has selected 'Yes' as well as a valid amount (formatted), and" in {

            val expectedViewModel =
              sessionData.pensions.unauthorisedPayments.copy(
                surchargeTaxAmountQuestion = Some(true),
                surchargeTaxAmount = Some(BigDecimal(1042.64))
              )

            implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
            implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), Some("£1,042.64")))

            assertRedirectionAsExpected(relativeUrl("/unauthorised-payments-from-pensions/amount-not-surcharged"))
            getViewModel mustBe Some(expectedViewModel)

          }
          "the user has selected 'Yes' as well as an amount of zero" in {

            val expectedViewModel =
              sessionData.pensions.unauthorisedPayments.copy(
                surchargeTaxAmountQuestion = Some(true),
                surchargeTaxAmount = Some(BigDecimal(0))
              )

            implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
            implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), Some("0")))

            assertRedirectionAsExpected(relativeUrl("/unauthorised-payments-from-pensions/amount-not-surcharged"))
            getViewModel mustBe Some(expectedViewModel)

          }

        }
      }
      "fail" when {
        "the user has the minimal session data and" when {

          val sessionData = pensionsUserData(aPensionsCYAModel)
          val expectedViewModel = sessionData.pensions.unauthorisedPayments

          "the user has selected neither 'Yes' nor 'No' and" when {

            scenarioNameForIndividualAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(None, None))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedYesNoAmountPageContents(
                  title = "Error: Did you pay non-UK tax on the amount that resulted in a surcharge?",
                  header = "Did you pay non-UK tax on the amount that resulted in a surcharge?",
                  caption = "Unauthorised payments from pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection = ExpectedAmountSection("Total non-UK tax in pounds", ""),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "Select yes if you paid non-UK tax on the amount surcharged",
                      link = "#value")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Select yes if you paid non-UK tax on the amount surcharged",
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
                  title = "Error: Did you pay non-UK tax on the amount that resulted in a surcharge?",
                  header = "Did you pay non-UK tax on the amount that resulted in a surcharge?",
                  caption = "Taliadau heb awdurdod o bensiynau ar gyfer 6 Ebrill 2021 i 5 Ebrill 2022",
                  radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection = ExpectedAmountSection("Total non-UK tax in pounds", ""),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "Select yes if you paid non-UK tax on the amount surcharged",
                      link = "#value")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Select yes if you paid non-UK tax on the amount surcharged",
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
                  title = "Error: Did your client pay non-UK tax on the amount that resulted in a surcharge?",
                  header = "Did your client pay non-UK tax on the amount that resulted in a surcharge?",
                  caption = "Unauthorised payments from pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection = ExpectedAmountSection("Total non-UK tax in pounds", ""),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "Select yes if you paid non-UK tax on the amount surcharged",
                      link = "#value")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Select yes if you paid non-UK tax on the amount surcharged",
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
                  title = "Error: Did your client pay non-UK tax on the amount that resulted in a surcharge?",
                  header = "Did your client pay non-UK tax on the amount that resulted in a surcharge?",
                  caption = "Taliadau heb awdurdod o bensiynau ar gyfer 6 Ebrill 2021 i 5 Ebrill 2022",
                  radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection = ExpectedAmountSection("Total non-UK tax in pounds", ""),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "Select yes if you paid non-UK tax on the amount surcharged",
                      link = "#value")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Select yes if you paid non-UK tax on the amount surcharged",
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
                  title = "Error: Did you pay non-UK tax on the amount that resulted in a surcharge?",
                  header = "Did you pay non-UK tax on the amount that resulted in a surcharge?",
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
                  title = "Error: Did you pay non-UK tax on the amount that resulted in a surcharge?",
                  header = "Did you pay non-UK tax on the amount that resulted in a surcharge?",
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
                  title = "Error: Did your client pay non-UK tax on the amount that resulted in a surcharge?",
                  header = "Did your client pay non-UK tax on the amount that resulted in a surcharge?",
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
                  title = "Error: Did your client pay non-UK tax on the amount that resulted in a surcharge?",
                  header = "Did your client pay non-UK tax on the amount that resulted in a surcharge?",
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
                  title = "Error: Did you pay non-UK tax on the amount that resulted in a surcharge?",
                  header = "Did you pay non-UK tax on the amount that resulted in a surcharge?",
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
                  title = "Error: Did you pay non-UK tax on the amount that resulted in a surcharge?",
                  header = "Did you pay non-UK tax on the amount that resulted in a surcharge?",
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
                  title = "Error: Did your client pay non-UK tax on the amount that resulted in a surcharge?",
                  header = "Did your client pay non-UK tax on the amount that resulted in a surcharge?",
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
                  title = "Error: Did your client pay non-UK tax on the amount that resulted in a surcharge?",
                  header = "Did your client pay non-UK tax on the amount that resulted in a surcharge?",
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
          "the user has selected 'Yes' but has provided an excessive amount, and" when {

            scenarioNameForIndividualAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), Some("100000000002")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedYesNoAmountPageContents(
                  title = "Error: Did you pay non-UK tax on the amount that resulted in a surcharge?",
                  header = "Did you pay non-UK tax on the amount that resulted in a surcharge?",
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
                  title = "Error: Did you pay non-UK tax on the amount that resulted in a surcharge?",
                  header = "Did you pay non-UK tax on the amount that resulted in a surcharge?",
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
                  title = "Error: Did your client pay non-UK tax on the amount that resulted in a surcharge?",
                  header = "Did your client pay non-UK tax on the amount that resulted in a surcharge?",
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
                  title = "Error: Did your client pay non-UK tax on the amount that resulted in a surcharge?",
                  header = "Did your client pay non-UK tax on the amount that resulted in a surcharge?",
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
