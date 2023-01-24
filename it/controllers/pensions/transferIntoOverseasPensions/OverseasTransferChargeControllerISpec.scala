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

package controllers.pensions.transferIntoOverseasPensions

import builders.PensionsCYAModelBuilder.{aPensionsCYAEmptyModel, aPensionsCYAModel}
import controllers.ControllerSpec.PreferredLanguages.{English, Welsh}
import controllers.ControllerSpec.UserTypes.{Agent, Individual}
import controllers.ControllerSpec._
import controllers.YesNoAmountControllerSpec
import models.pension.charges.TransfersIntoOverseasPensionsViewModel
import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.libs.ws.WSResponse

class OverseasTransferChargeControllerISpec
  extends YesNoAmountControllerSpec("/overseas-pensions/overseas-transfer-charges/transfer-charge") {

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
        "the user has no pension-related session data and" when {

          val sessionData = pensionsUserData(aPensionsCYAEmptyModel)

          scenarioNameForIndividualAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPageAsExpected(
              OK,
              ExpectedYesNoAmountPageContents(
                title = "Did the amount result in an overseas transfer charge?",
                header = "Did the amount result in an overseas transfer charge?",
                caption = "Transfers into overseas pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection = ExpectedAmountSection("Amount that resulted in a transfer charge, in pounds", "", Some("For example, £193.54")),
              ))
          }
          scenarioNameForIndividualAndWelsh in {

            implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPageAsExpected(
              OK,
              ExpectedYesNoAmountPageContents(
                title = "Did the amount result in an overseas transfer charge?",
                header = "Did the amount result in an overseas transfer charge?",
                caption = "Transfers into overseas pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection = ExpectedAmountSection("Amount that resulted in a transfer charge, in pounds", "", Some("For example, £193.54")),
              ))
          }
          scenarioNameForAgentAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPageAsExpected(
              OK,
              ExpectedYesNoAmountPageContents(
                title = "Did the amount result in an overseas transfer charge?",
                header = "Did the amount result in an overseas transfer charge?",
                caption = "Transfers into overseas pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection = ExpectedAmountSection("Amount that resulted in a transfer charge, in pounds", "", Some("For example, £193.54")),
              )
            )
          }
          scenarioNameForAgentAndWelsh in {

            implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPageAsExpected(
              OK,
              ExpectedYesNoAmountPageContents(
                title = "Did the amount result in an overseas transfer charge?",
                header = "Did the amount result in an overseas transfer charge?",
                caption = "Transfers into overseas pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection = ExpectedAmountSection("Amount that resulted in a transfer charge, in pounds", "", Some("For example, £193.54")),
              ))
          }
        }
        "the user had previously answered 'Yes' with a valid amount, and" when {

          val sessionData = pensionsUserData(aPensionsCYAModel.copy(transfersIntoOverseasPensions =
            TransfersIntoOverseasPensionsViewModel(
              transferPensionSavings = Some(true),
              overseasTransferCharge = Some(true),
              overseasTransferChargeAmount = Some(BigDecimal(99.99)))
          ))

          scenarioNameForIndividualAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPageAsExpected(
              OK,
              ExpectedYesNoAmountPageContents(
                title = "Did the amount result in an overseas transfer charge?",
                header = "Did the amount result in an overseas transfer charge?",
                caption = "Transfers into overseas pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = checkedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection = ExpectedAmountSection("Amount that resulted in a transfer charge, in pounds", "99.99", Some("For example, £193.54")),
              ))
          }
          scenarioNameForIndividualAndWelsh in {

            implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPageAsExpected(
              OK,
              ExpectedYesNoAmountPageContents(
                title = "Did the amount result in an overseas transfer charge?",
                header = "Did the amount result in an overseas transfer charge?",
                caption = "Transfers into overseas pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = checkedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection = ExpectedAmountSection("Amount that resulted in a transfer charge, in pounds", "99.99", Some("For example, £193.54")),
              ))
          }
          scenarioNameForAgentAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPageAsExpected(
              OK,
              ExpectedYesNoAmountPageContents(
                title = "Did the amount result in an overseas transfer charge?",
                header = "Did the amount result in an overseas transfer charge?",
                caption = "Transfers into overseas pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = checkedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection = ExpectedAmountSection("Amount that resulted in a transfer charge, in pounds", "99.99", Some("For example, £193.54")),
              ))
          }
          scenarioNameForAgentAndWelsh in {

            implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPageAsExpected(
              OK,
              ExpectedYesNoAmountPageContents(
                title = "Did the amount result in an overseas transfer charge?",
                header = "Did the amount result in an overseas transfer charge?",
                caption = "Transfers into overseas pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = checkedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection = ExpectedAmountSection("Amount that resulted in a transfer charge, in pounds", "99.99", Some("For example, £193.54")),
              ))
          }
        }
        "the user had previously answered 'No' without an amount, and" when {

          val sessionData = pensionsUserData(aPensionsCYAModel.copy(transfersIntoOverseasPensions =
            TransfersIntoOverseasPensionsViewModel(
              transferPensionSavings = Some(true),
              overseasTransferCharge = Some(false),
              overseasTransferChargeAmount = None)
          ))

          scenarioNameForIndividualAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPageAsExpected(
              OK,
              ExpectedYesNoAmountPageContents(
                title = "Did the amount result in an overseas transfer charge?",
                header = "Did the amount result in an overseas transfer charge?",
                caption = "Transfers into overseas pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = checkedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection = ExpectedAmountSection("Amount that resulted in a transfer charge, in pounds", "", Some("For example, £193.54")),
              ))
          }
          scenarioNameForIndividualAndWelsh in {

            implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPageAsExpected(
              OK,
              ExpectedYesNoAmountPageContents(
                title = "Did the amount result in an overseas transfer charge?",
                header = "Did the amount result in an overseas transfer charge?",
                caption = "Transfers into overseas pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = checkedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection = ExpectedAmountSection("Amount that resulted in a transfer charge, in pounds", "", Some("For example, £193.54")),
              ))
          }
          scenarioNameForAgentAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPageAsExpected(
              OK,
              ExpectedYesNoAmountPageContents(
                title = "Did the amount result in an overseas transfer charge?",
                header = "Did the amount result in an overseas transfer charge?",
                caption = "Transfers into overseas pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = checkedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection = ExpectedAmountSection("Amount that resulted in a transfer charge, in pounds", "", Some("For example, £193.54")),
              ))
          }
          scenarioNameForAgentAndWelsh in {

            implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPageAsExpected(
              OK,
              ExpectedYesNoAmountPageContents(
                title = "Did the amount result in an overseas transfer charge?",
                header = "Did the amount result in an overseas transfer charge?",
                caption = "Transfers into overseas pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = checkedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection = ExpectedAmountSection("Amount that resulted in a transfer charge, in pounds", "", Some("For example, £193.54")),
              ))
          }
        }
        "the user had previously answered 'No' with an amount via 3rd party, and" when {

          val sessionData = pensionsUserData(aPensionsCYAModel.copy(transfersIntoOverseasPensions =
              TransfersIntoOverseasPensionsViewModel(
                transferPensionSavings = Some(true),
                overseasTransferCharge = Some(false),
                overseasTransferChargeAmount = Some(BigDecimal(99.99)))
            ))

          scenarioNameForIndividualAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPageAsExpected(
              OK,
              ExpectedYesNoAmountPageContents(
                title = "Did the amount result in an overseas transfer charge?",
                header = "Did the amount result in an overseas transfer charge?",
                caption = "Transfers into overseas pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = checkedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection = ExpectedAmountSection("Amount that resulted in a transfer charge, in pounds", "99.99", Some("For example, £193.54")),
              ))
          }
          scenarioNameForIndividualAndWelsh in {

            implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPageAsExpected(
              OK,
              ExpectedYesNoAmountPageContents(
                title = "Did the amount result in an overseas transfer charge?",
                header = "Did the amount result in an overseas transfer charge?",
                caption = "Transfers into overseas pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = checkedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection = ExpectedAmountSection("Amount that resulted in a transfer charge, in pounds", "99.99", Some("For example, £193.54")),
              ))
          }
          scenarioNameForAgentAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPageAsExpected(
              OK,
              ExpectedYesNoAmountPageContents(
                title = "Did the amount result in an overseas transfer charge?",
                header = "Did the amount result in an overseas transfer charge?",
                caption = "Transfers into overseas pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = checkedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection = ExpectedAmountSection("Amount that resulted in a transfer charge, in pounds", "99.99", Some("For example, £193.54")),
              ))
          }
          scenarioNameForAgentAndWelsh in {

            implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPageAsExpected(
              OK,
              ExpectedYesNoAmountPageContents(
                title = "Did the amount result in an overseas transfer charge?",
                header = "Did the amount result in an overseas transfer charge?",
                caption = "Transfers into overseas pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = checkedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                amountSection = ExpectedAmountSection("Amount that resulted in a transfer charge, in pounds", "99.99", Some("For example, £193.54")),
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

          assertRedirectionAsExpected(PageRelativeURLs.summaryPage)
          getViewModel mustBe None
        }
      }
      "succeed" when {
        "the user has relevant session data and" when {

          val sessionData = pensionsUserData(aPensionsCYAModel)

          "the user has selected 'No'" in {

            val expectedViewModel = sessionData.pensions.transfersIntoOverseasPensions.copy(
              overseasTransferCharge = Some(false),
              overseasTransferChargeAmount = None
            )


            implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
            implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(false), None))

            assertRedirectionAsExpected(relativeUrlForThisPage)
            getViewModel mustBe Some(expectedViewModel)
          }
          "the user has selected 'Yes' as well as a valid amount (unformatted)" in {

            val expectedViewModel = sessionData.pensions.transfersIntoOverseasPensions.copy(
              overseasTransferCharge = Some(true),
              overseasTransferChargeAmount = Some(BigDecimal(42.64))
            )

            implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
            implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), Some("42.64")))

            assertRedirectionAsExpected(relativeUrlForThisPage)
            getViewModel mustBe Some(expectedViewModel)
          }
          "the user has selected 'Yes' as well as a valid amount (formatted)" in {

            val expectedViewModel = sessionData.pensions.transfersIntoOverseasPensions.copy(
              overseasTransferCharge = Some(true),
              overseasTransferChargeAmount = Some(BigDecimal(1042.64))
            )

            implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
            implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), Some("£1,042.64")))

            assertRedirectionAsExpected(relativeUrlForThisPage)
            getViewModel mustBe Some(expectedViewModel)
          }
        }
        "the user has no pension-related session data and" when {

          val sessionData = pensionsUserData(aPensionsCYAEmptyModel)

          "the user has selected 'No'" in {

            val expectedViewModel = sessionData.pensions.transfersIntoOverseasPensions.copy(
              overseasTransferCharge = Some(false),
              overseasTransferChargeAmount = None
            )

            implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
            implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(false), None))

            assertRedirectionAsExpected(relativeUrlForThisPage)
            getViewModel mustBe Some(expectedViewModel)

          }
          "the user has selected 'Yes' as well as a valid amount (unformatted)" in {

            val expectedViewModel = sessionData.pensions.transfersIntoOverseasPensions.copy(
              overseasTransferCharge = Some(true),
              overseasTransferChargeAmount = Some(BigDecimal(42.64))
            )

            implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
            implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), Some("42.64")))

            assertRedirectionAsExpected(relativeUrlForThisPage)
            getViewModel mustBe Some(expectedViewModel)

          }
          "the user has selected 'Yes' as well as a valid amount (formatted)" in {

            val expectedViewModel = sessionData.pensions.transfersIntoOverseasPensions.copy(
              overseasTransferCharge = Some(true),
              overseasTransferChargeAmount = Some(BigDecimal(1042.64))
            )

            implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
            implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), Some("£1,042.64")))

            assertRedirectionAsExpected(relativeUrlForThisPage)
            getViewModel mustBe Some(expectedViewModel)

          }

        }
      }
      "fail" when {
        "the user has relevant session data and" when {

          val sessionData = pensionsUserData(aPensionsCYAModel)
          val expectedViewModel = sessionData.pensions.transfersIntoOverseasPensions

          "the user has selected neither 'Yes' nor 'No' and" when {
            scenarioNameForIndividualAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(None, None))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedYesNoAmountPageContents(
                  title = "Error: Did the amount result in an overseas transfer charge?",
                  header = "Did the amount result in an overseas transfer charge?",
                  caption = "Transfers into overseas pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection = ExpectedAmountSection("Amount that resulted in a transfer charge, in pounds", "", Some("For example, £193.54")),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "Select yes if the amount resulted in an overseas transfer charge",
                      link = "#value")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Select yes if the amount resulted in an overseas transfer charge",
                      idOpt = Some("value")
                    )
                  )
                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
            scenarioNameForIndividualAndWelsh in {

              implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(None, None))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedYesNoAmountPageContents(
                  title = "Error: Did the amount result in an overseas transfer charge?",
                  header = "Did the amount result in an overseas transfer charge?",
                  caption = "Transfers into overseas pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection = ExpectedAmountSection("Amount that resulted in a transfer charge, in pounds", "", Some("For example, £193.54")),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "Select yes if the amount resulted in an overseas transfer charge",
                      link = "#value")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Select yes if the amount resulted in an overseas transfer charge",
                      idOpt = Some("value")
                    )
                  )
                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
            scenarioNameForAgentAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(None, None))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedYesNoAmountPageContents(
                  title = "Error: Did the amount result in an overseas transfer charge?",
                  header = "Did the amount result in an overseas transfer charge?",
                  caption = "Transfers into overseas pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection = ExpectedAmountSection("Amount that resulted in a transfer charge, in pounds", "", Some("For example, £193.54")),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "Select yes if the amount resulted in an overseas transfer charge",
                      link = "#value")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Select yes if the amount resulted in an overseas transfer charge",
                      idOpt = Some("value")
                    )
                  )
                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
            scenarioNameForAgentAndWelsh in {

              implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(None, None))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedYesNoAmountPageContents(
                  title = "Error: Did the amount result in an overseas transfer charge?",
                  header = "Did the amount result in an overseas transfer charge?",
                  caption = "Transfers into overseas pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection = ExpectedAmountSection("Amount that resulted in a transfer charge, in pounds", "", Some("For example, £193.54")),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "Select yes if the amount resulted in an overseas transfer charge",
                      link = "#value")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Select yes if the amount resulted in an overseas transfer charge",
                      idOpt = Some("value")
                    )
                  )
                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
          }
          "the user has selected 'Yes' but have not provided an amount, and" when {
            scenarioNameForIndividualAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), None))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedYesNoAmountPageContents(
                  title = "Error: Did the amount result in an overseas transfer charge?",
                  header = "Did the amount result in an overseas transfer charge?",
                  caption = "Transfers into overseas pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection = ExpectedAmountSection("Amount that resulted in a transfer charge, in pounds", "", Some("For example, £193.54")),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "Enter the amount on which you paid an overseas transfer charge",
                      link = "#amount-2")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter the amount on which you paid an overseas transfer charge",
                      idOpt = Some("amount-2")
                    )
                  )
                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
            scenarioNameForIndividualAndWelsh in {

              implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), None))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedYesNoAmountPageContents(
                  title = "Error: Did the amount result in an overseas transfer charge?",
                  header = "Did the amount result in an overseas transfer charge?",
                  caption = "Transfers into overseas pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection = ExpectedAmountSection("Amount that resulted in a transfer charge, in pounds", "", Some("For example, £193.54")),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "Enter the amount on which you paid an overseas transfer charge",
                      link = "#amount-2")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter the amount on which you paid an overseas transfer charge",
                      idOpt = Some("amount-2")
                    )
                  )
                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
            scenarioNameForAgentAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), None))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedYesNoAmountPageContents(
                  title = "Error: Did the amount result in an overseas transfer charge?",
                  header = "Did the amount result in an overseas transfer charge?",
                  caption = "Transfers into overseas pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection = ExpectedAmountSection("Amount that resulted in a transfer charge, in pounds", "", Some("For example, £193.54")),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "Enter the amount that resulted in a transfer charge",
                      link = "#amount-2")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter the amount that resulted in a transfer charge",
                      idOpt = Some("amount-2")
                    )
                  )
                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
            scenarioNameForAgentAndWelsh in {

              implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), None))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedYesNoAmountPageContents(
                  title = "Error: Did the amount result in an overseas transfer charge?",
                  header = "Did the amount result in an overseas transfer charge?",
                  caption = "Transfers into overseas pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection = ExpectedAmountSection("Amount that resulted in a transfer charge, in pounds", "", Some("For example, £193.54")),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "Enter the amount that resulted in a transfer charge",
                      link = "#amount-2")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter the amount that resulted in a transfer charge",
                      idOpt = Some("amount-2")
                    )
                  )
                )
              )
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
                  title = "Error: Did the amount result in an overseas transfer charge?",
                  header = "Did the amount result in an overseas transfer charge?",
                  caption = "Transfers into overseas pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection = ExpectedAmountSection("Amount that resulted in a transfer charge, in pounds", "x2.64", Some("For example, £193.54")),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "Enter the amount on which you paid an overseas transfer charge in the correct format",
                      link = "#amount-2")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter the amount on which you paid an overseas transfer charge in the correct format",
                      idOpt = Some("amount-2")
                    )
                  )
                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
            scenarioNameForIndividualAndWelsh in {

              implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), Some("x2.64")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedYesNoAmountPageContents(
                  title = "Error: Did the amount result in an overseas transfer charge?",
                  header = "Did the amount result in an overseas transfer charge?",
                  caption = "Transfers into overseas pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection = ExpectedAmountSection("Amount that resulted in a transfer charge, in pounds", "x2.64", Some("For example, £193.54")),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "Enter the amount on which you paid an overseas transfer charge in the correct format",
                      link = "#amount-2")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter the amount on which you paid an overseas transfer charge in the correct format",
                      idOpt = Some("amount-2")
                    )
                  )
                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
            scenarioNameForAgentAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), Some("x2.64")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedYesNoAmountPageContents(
                  title = "Error: Did the amount result in an overseas transfer charge?",
                  header = "Did the amount result in an overseas transfer charge?",
                  caption = "Transfers into overseas pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection = ExpectedAmountSection("Amount that resulted in a transfer charge, in pounds", "x2.64", Some("For example, £193.54")),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "Enter the amount that resulted in a transfer charge in the correct format",
                      link = "#amount-2")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter the amount that resulted in a transfer charge in the correct format",
                      idOpt = Some("amount-2")
                    )
                  )
                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
            scenarioNameForAgentAndWelsh in {

              implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), Some("x2.64")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedYesNoAmountPageContents(
                  title = "Error: Did the amount result in an overseas transfer charge?",
                  header = "Did the amount result in an overseas transfer charge?",
                  caption = "Transfers into overseas pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection = ExpectedAmountSection("Amount that resulted in a transfer charge, in pounds", "x2.64", Some("For example, £193.54")),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "Enter the amount that resulted in a transfer charge in the correct format",
                      link = "#amount-2")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter the amount that resulted in a transfer charge in the correct format",
                      idOpt = Some("amount-2")
                    )
                  )
                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
          }
          "the user has selected 'Yes' but has provided an excessive amount, and" when {
            scenarioNameForIndividualAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), Some("100,000,000,000")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedYesNoAmountPageContents(
                  title = "Error: Did the amount result in an overseas transfer charge?",
                  header = "Did the amount result in an overseas transfer charge?",
                  caption = "Transfers into overseas pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection = ExpectedAmountSection("Amount that resulted in a transfer charge, in pounds", "100,000,000,000", Some("For example, £193.54")),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "The amount on which you paid an overseas transfer charge must be less than £100,000,000,000",
                      link = "#amount-2")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: The amount on which you paid an overseas transfer charge must be less than £100,000,000,000",
                      idOpt = Some("amount-2")
                    )
                  )
                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
            scenarioNameForIndividualAndWelsh in {

              implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), Some("100,000,000,000")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedYesNoAmountPageContents(
                  title = "Error: Did the amount result in an overseas transfer charge?",
                  header = "Did the amount result in an overseas transfer charge?",
                  caption = "Transfers into overseas pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection = ExpectedAmountSection("Amount that resulted in a transfer charge, in pounds", "100,000,000,000", Some("For example, £193.54")),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "The amount on which you paid an overseas transfer charge must be less than £100,000,000,000",
                      link = "#amount-2")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: The amount on which you paid an overseas transfer charge must be less than £100,000,000,000",
                      idOpt = Some("amount-2")
                    )
                  )
                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
            scenarioNameForAgentAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), Some("100,000,000,000")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedYesNoAmountPageContents(
                  title = "Error: Did the amount result in an overseas transfer charge?",
                  header = "Did the amount result in an overseas transfer charge?",
                  caption = "Transfers into overseas pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection = ExpectedAmountSection("Amount that resulted in a transfer charge, in pounds", "100,000,000,000", Some("For example, £193.54")),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "The amount that resulted in a transfer charge must be less than £100,000,000,000",
                      link = "#amount-2")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: The amount that resulted in a transfer charge must be less than £100,000,000,000",
                      idOpt = Some("amount-2")
                    )
                  )
                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
            scenarioNameForAgentAndWelsh in {

              implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), Some("100,000,000,000")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedYesNoAmountPageContents(
                  title = "Error: Did the amount result in an overseas transfer charge?",
                  header = "Did the amount result in an overseas transfer charge?",
                  caption = "Transfers into overseas pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection = ExpectedAmountSection("Amount that resulted in a transfer charge, in pounds", "100,000,000,000", Some("For example, £193.54")),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "The amount that resulted in a transfer charge must be less than £100,000,000,000",
                      link = "#amount-2")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: The amount that resulted in a transfer charge must be less than £100,000,000,000",
                      idOpt = Some("amount-2")
                    )
                  )
                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
          }
          "the user has selected 'Yes' but has provided zero as an amount, and" when {
            scenarioNameForIndividualAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), Some("0")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedYesNoAmountPageContents(
                  title = "Error: Did the amount result in an overseas transfer charge?",
                  header = "Did the amount result in an overseas transfer charge?",
                  caption = "Transfers into overseas pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection = ExpectedAmountSection("Amount that resulted in a transfer charge, in pounds", "0", Some("For example, £193.54")),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "Enter an amount greater than zero",
                      link = "#amount-2")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter an amount greater than zero",
                      idOpt = Some("amount-2")
                    )
                  )
                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
            scenarioNameForIndividualAndWelsh in {

              implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), Some("0")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedYesNoAmountPageContents(
                  title = "Error: Did the amount result in an overseas transfer charge?",
                  header = "Did the amount result in an overseas transfer charge?",
                  caption = "Transfers into overseas pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection = ExpectedAmountSection("Amount that resulted in a transfer charge, in pounds", "0", Some("For example, £193.54")),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "Enter an amount greater than zero",
                      link = "#amount-2")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter an amount greater than zero",
                      idOpt = Some("amount-2")
                    )
                  )
                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
            scenarioNameForAgentAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), Some("0")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedYesNoAmountPageContents(
                  title = "Error: Did the amount result in an overseas transfer charge?",
                  header = "Did the amount result in an overseas transfer charge?",
                  caption = "Transfers into overseas pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection = ExpectedAmountSection("Amount that resulted in a transfer charge, in pounds", "0", Some("For example, £193.54")),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "Enter an amount greater than zero",
                      link = "#amount-2")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter an amount greater than zero",
                      idOpt = Some("amount-2")
                    )
                  )
                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
            scenarioNameForAgentAndWelsh in {

              implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), Some("0")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedYesNoAmountPageContents(
                  title = "Error: Did the amount result in an overseas transfer charge?",
                  header = "Did the amount result in an overseas transfer charge?",
                  caption = "Transfers into overseas pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  amountSection = ExpectedAmountSection("Amount that resulted in a transfer charge, in pounds", "0", Some("For example, £193.54")),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "Enter an amount greater than zero",
                      link = "#amount-2")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter an amount greater than zero",
                      idOpt = Some("amount-2")
                    )
                  )
                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
          }
        }
      }
    }
  }

  private def getViewModel(implicit userConfig: UserConfig): Option[TransfersIntoOverseasPensionsViewModel] =
    loadPensionUserData.map(_.pensions.transfersIntoOverseasPensions)
}







