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

package controllers.pensions.paymentsIntoPensions

import builders.PensionsCYAModelBuilder.{aPensionsCYAEmptyModel, aPensionsCYAModel}
import controllers.ControllerSpec.PreferredLanguages.{English, Welsh}
import controllers.ControllerSpec.UserTypes.{Agent, Individual}
import controllers.ControllerSpec._
import controllers.YesNoControllerSpec
import models.mongo.PensionsCYAModel
import models.pension.reliefs.PaymentsIntoPensionViewModel
import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.libs.ws.WSResponse

class PensionsTaxReliefNotClaimedControllerISpec
  extends YesNoControllerSpec("/payments-into-pensions/no-tax-relief") {

  private val selectorForFirstParagraph = "#main-content > div > div > p:nth-of-type(1)"
  private val selectorForSecondParagraph = "#main-content > div > div > p:nth-of-type(2)"
  private val selectorForSubHeading = "#main-content > div > div > form > div > fieldset > legend"

  val minimalSessionDataToAccessThisPage: PensionsCYAModel = aPensionsCYAEmptyModel
    .copy(paymentsIntoPension =
      PaymentsIntoPensionViewModel(
        rasPensionPaymentQuestion = Some(true),
        totalRASPaymentsAndTaxRelief = Some(BigDecimal("1.00")),
        oneOffRasPaymentPlusTaxReliefQuestion = Some(true),
        totalOneOffRasPaymentPlusTaxRelief = Some(BigDecimal("1.00")),
        totalPaymentsIntoRASQuestion = Some(true)
      ))

  "This page" when {
    "requested to be shown" should {
      "redirect to the expected page" when {
        "the user has no stored session data at all" in {

          implicit val response: WSResponse = getPage(None)

          assertRedirectionAsExpected(PageRelativeURLs.paymentsIntoPensionsCYAPage)

        }
        "the user has a session with an empty pensions model" in {

          implicit val response: WSResponse = getPage(Some(pensionsUserData(aPensionsCYAEmptyModel)))

          assertRedirectionAsExpected(PageRelativeURLs.paymentsIntoPensionsReliefAtSourcePage)

        }
        "the user has a session with an insufficient pensions model where" when {
          "only the RAS question has been answered" in {

            val sessionData = pensionsUserData(
              aPensionsCYAEmptyModel
                .copy(paymentsIntoPension =
                  PaymentsIntoPensionViewModel(
                    rasPensionPaymentQuestion = Some(true)
                  ))
            )

            implicit val response: WSResponse = getPage(Some(sessionData))

            assertRedirectionAsExpected(PageRelativeURLs.paymentsIntoPensionsReliefAtSourceAmountPage)

          }
          "only the RAS question, and the RAS amount has been answered" in {

            val sessionData = pensionsUserData(
              aPensionsCYAEmptyModel
                .copy(paymentsIntoPension =
                  PaymentsIntoPensionViewModel(
                    rasPensionPaymentQuestion = Some(true),
                    totalRASPaymentsAndTaxRelief = Some(BigDecimal("1.00"))
                  ))
            )

            implicit val response: WSResponse = getPage(Some(sessionData))

            assertRedirectionAsExpected(PageRelativeURLs.paymentsIntoPensionsOneOffPaymentsPage)

          }
          "only the RAS question, the RAS amount, and the tax relief question has been answered" in {

            val sessionData = pensionsUserData(
              aPensionsCYAEmptyModel
                .copy(paymentsIntoPension =
                  PaymentsIntoPensionViewModel(
                    rasPensionPaymentQuestion = Some(true),
                    totalRASPaymentsAndTaxRelief = Some(BigDecimal("1.00")),
                    oneOffRasPaymentPlusTaxReliefQuestion = Some(true)
                  ))
            )

            implicit val response: WSResponse = getPage(Some(sessionData))

            assertRedirectionAsExpected(PageRelativeURLs.paymentsIntoPensionsOneOffPaymentsAmountPage)

          }
          "only the RAS question, the RAS amount, the tax relief question, and tax relief amount has been answered" in {

            val sessionData = pensionsUserData(
              aPensionsCYAEmptyModel
                .copy(paymentsIntoPension =
                  PaymentsIntoPensionViewModel(
                    rasPensionPaymentQuestion = Some(true),
                    totalRASPaymentsAndTaxRelief = Some(BigDecimal("1.00")),
                    oneOffRasPaymentPlusTaxReliefQuestion = Some(true),
                    totalOneOffRasPaymentPlusTaxRelief = Some(BigDecimal("1.00"))
                  ))
            )

            implicit val response: WSResponse = getPage(Some(sessionData))

            assertRedirectionAsExpected(PageRelativeURLs.paymentsIntoPensionsTotalReliefAtSourceCheckPage)

          }
        }
      }

      "appear as expected" when {
        "the user has only the minimal session data for accessing this page and" when {

          val sessionData = pensionsUserData(minimalSessionDataToAccessThisPage)

          scenarioNameForIndividualAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPageAsExpected(
              OK,
              ExpectedYesNoPageContents(
                title = "Pensions where tax relief is not claimed",
                header = "Pensions where tax relief is not claimed",
                caption = "Payments into pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                links = Set.empty,
                text = Set(
                  ExpectedText(selectorForFirstParagraph, "These questions are about pensions you pay into where tax relief is not claimed for you."),
                  ExpectedText(selectorForSecondParagraph, "You can check your pension statements or contact your pension provider to find the information you need."),
                  ExpectedText(selectorForSubHeading, "Did you pay into a pension where tax relief was not claimed for you?")
                )
              ))

          }
          scenarioNameForIndividualAndWelsh in {

            implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPageAsExpected(
              OK,
              ExpectedYesNoPageContents(
                title = "Pensions where tax relief is not claimed",
                header = "Pensions where tax relief is not claimed",
                caption = "Payments into pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                links = Set.empty,
                text = Set(
                  ExpectedText(selectorForFirstParagraph, "These questions are about pensions you pay into where tax relief is not claimed for you."),
                  ExpectedText(selectorForSecondParagraph, "You can check your pension statements or contact your pension provider to find the information you need."),
                  ExpectedText(selectorForSubHeading, "Did you pay into a pension where tax relief was not claimed for you?")
                )
              ))

          }
          scenarioNameForAgentAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPageAsExpected(
              OK,
              ExpectedYesNoPageContents(
                title = "Pensions where tax relief is not claimed",
                header = "Pensions where tax relief is not claimed",
                caption = "Payments into pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                links = Set.empty,
                text = Set(
                  ExpectedText(selectorForFirstParagraph, "These questions are about pensions your client pays into where tax relief is not claimed for them."),
                  ExpectedText(selectorForSecondParagraph, "You can check your client’s pension statements or contact your client’s pension provider to find the information you need."),
                  ExpectedText(selectorForSubHeading, "Did your client pay into a pension where tax relief was not claimed for them?")
                )
              ))

          }
          scenarioNameForAgentAndWelsh in {

            implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPageAsExpected(
              OK,
              ExpectedYesNoPageContents(
                title = "Pensions where tax relief is not claimed",
                header = "Pensions where tax relief is not claimed",
                caption = "Payments into pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                links = Set.empty,
                text = Set(
                  ExpectedText(selectorForFirstParagraph, "These questions are about pensions your client pays into where tax relief is not claimed for them."),
                  ExpectedText(selectorForSecondParagraph, "You can check your client’s pension statements or contact your client’s pension provider to find the information you need."),
                  ExpectedText(selectorForSubHeading, "Did your client pay into a pension where tax relief was not claimed for them?")
                )
              ))

          }
        }
        "the user had previously answered 'Yes', and" when {

          val sessionData = pensionsUserData(
            minimalSessionDataToAccessThisPage.copy(
              paymentsIntoPension = minimalSessionDataToAccessThisPage.paymentsIntoPension.copy(
                pensionTaxReliefNotClaimedQuestion = Some(true)
              )
            )
          )

          scenarioNameForIndividualAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPageAsExpected(
              OK,
              ExpectedYesNoPageContents(
                title = "Pensions where tax relief is not claimed",
                header = "Pensions where tax relief is not claimed",
                caption = "Payments into pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = checkedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                links = Set.empty,
                text = Set(
                  ExpectedText(selectorForFirstParagraph, "These questions are about pensions you pay into where tax relief is not claimed for you."),
                  ExpectedText(selectorForSecondParagraph, "You can check your pension statements or contact your pension provider to find the information you need."),
                  ExpectedText(selectorForSubHeading, "Did you pay into a pension where tax relief was not claimed for you?")
                )
              ))

          }
          scenarioNameForIndividualAndWelsh in {

            implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPageAsExpected(
              OK,
              ExpectedYesNoPageContents(
                title = "Pensions where tax relief is not claimed",
                header = "Pensions where tax relief is not claimed",
                caption = "Payments into pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = checkedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                links = Set.empty,
                text = Set(
                  ExpectedText(selectorForFirstParagraph, "These questions are about pensions you pay into where tax relief is not claimed for you."),
                  ExpectedText(selectorForSecondParagraph, "You can check your pension statements or contact your pension provider to find the information you need."),
                  ExpectedText(selectorForSubHeading, "Did you pay into a pension where tax relief was not claimed for you?")
                )
              ))

          }
          scenarioNameForAgentAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPageAsExpected(
              OK,
              ExpectedYesNoPageContents(
                title = "Pensions where tax relief is not claimed",
                header = "Pensions where tax relief is not claimed",
                caption = "Payments into pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = checkedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                links = Set.empty,
                text = Set(
                  ExpectedText(selectorForFirstParagraph, "These questions are about pensions your client pays into where tax relief is not claimed for them."),
                  ExpectedText(selectorForSecondParagraph, "You can check your client’s pension statements or contact your client’s pension provider to find the information you need."),
                  ExpectedText(selectorForSubHeading, "Did your client pay into a pension where tax relief was not claimed for them?")
                )
              ))

          }
          scenarioNameForAgentAndWelsh in {

            implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPageAsExpected(
              OK,
              ExpectedYesNoPageContents(
                title = "Pensions where tax relief is not claimed",
                header = "Pensions where tax relief is not claimed",
                caption = "Payments into pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = checkedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                links = Set.empty,
                text = Set(
                  ExpectedText(selectorForFirstParagraph, "These questions are about pensions your client pays into where tax relief is not claimed for them."),
                  ExpectedText(selectorForSecondParagraph, "You can check your client’s pension statements or contact your client’s pension provider to find the information you need."),
                  ExpectedText(selectorForSubHeading, "Did your client pay into a pension where tax relief was not claimed for them?")
                )
              ))

          }

        }
        "the user had previously answered 'No', and" when {

          val sessionData = pensionsUserData(
            minimalSessionDataToAccessThisPage.copy(
              paymentsIntoPension = minimalSessionDataToAccessThisPage.paymentsIntoPension.copy(
                pensionTaxReliefNotClaimedQuestion = Some(false)
              )
            )
          )

          scenarioNameForIndividualAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPageAsExpected(
              OK,
              ExpectedYesNoPageContents(
                title = "Pensions where tax relief is not claimed",
                header = "Pensions where tax relief is not claimed",
                caption = "Payments into pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = checkedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                links = Set.empty,
                text = Set(
                  ExpectedText(selectorForFirstParagraph, "These questions are about pensions you pay into where tax relief is not claimed for you."),
                  ExpectedText(selectorForSecondParagraph, "You can check your pension statements or contact your pension provider to find the information you need."),
                  ExpectedText(selectorForSubHeading, "Did you pay into a pension where tax relief was not claimed for you?")
                )
              ))

          }
          scenarioNameForIndividualAndWelsh in {

            implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPageAsExpected(
              OK,
              ExpectedYesNoPageContents(
                title = "Pensions where tax relief is not claimed",
                header = "Pensions where tax relief is not claimed",
                caption = "Payments into pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = checkedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                links = Set.empty,
                text = Set(
                  ExpectedText(selectorForFirstParagraph, "These questions are about pensions you pay into where tax relief is not claimed for you."),
                  ExpectedText(selectorForSecondParagraph, "You can check your pension statements or contact your pension provider to find the information you need."),
                  ExpectedText(selectorForSubHeading, "Did you pay into a pension where tax relief was not claimed for you?")
                )
              ))


          }
          scenarioNameForAgentAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPageAsExpected(
              OK,
              ExpectedYesNoPageContents(
                title = "Pensions where tax relief is not claimed",
                header = "Pensions where tax relief is not claimed",
                caption = "Payments into pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = checkedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                links = Set.empty,
                text = Set(
                  ExpectedText(selectorForFirstParagraph, "These questions are about pensions your client pays into where tax relief is not claimed for them."),
                  ExpectedText(selectorForSecondParagraph, "You can check your client’s pension statements or contact your client’s pension provider to find the information you need."),
                  ExpectedText(selectorForSubHeading, "Did your client pay into a pension where tax relief was not claimed for them?")
                )
              ))

          }
          scenarioNameForAgentAndWelsh in {

            implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPageAsExpected(
              OK,
              ExpectedYesNoPageContents(
                title = "Pensions where tax relief is not claimed",
                header = "Pensions where tax relief is not claimed",
                caption = "Payments into pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = checkedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                links = Set.empty,
                text = Set(
                  ExpectedText(selectorForFirstParagraph, "These questions are about pensions your client pays into where tax relief is not claimed for them."),
                  ExpectedText(selectorForSecondParagraph, "You can check your client’s pension statements or contact your client’s pension provider to find the information you need."),
                  ExpectedText(selectorForSubHeading, "Did your client pay into a pension where tax relief was not claimed for them?")
                )
              ))

          }

        }
      }
    }
    "submitted" should {
      "redirect to the expected page" when {
        "the user has no stored session data at all" in {

          implicit val userConfig: UserConfig = userConfigWhenIrrelevant(None)
          implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoPage(Some(false)))

          assertRedirectionAsExpected(PageRelativeURLs.paymentsIntoPensionsCYAPage)
          getViewModel mustBe None

        }
      }
      "succeed when" when {
        "the user has selected 'No'" in {

          val sessionData = pensionsUserData(minimalSessionDataToAccessThisPage)

          val expectedViewModel =
            sessionData.pensions.paymentsIntoPension.copy(
              pensionTaxReliefNotClaimedQuestion = Some(false),
              retirementAnnuityContractPaymentsQuestion = None,
              totalRetirementAnnuityContractPayments = None,
              workplacePensionPaymentsQuestion = None,
              totalWorkplacePensionPayments = None
            )

          implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
          implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoPage(Some(false)))

          assertRedirectionAsExpected(PageRelativeURLs.paymentsIntoPensionsCYAPage)
          getViewModel mustBe Some(expectedViewModel)

        }
        "the user has selected 'Yes', they've already answered 'Yes' about the 'retirement annuity' contract" in {

          val sessionData = pensionsUserData(
            aPensionsCYAModel.copy(
              paymentsIntoPension = aPensionsCYAModel.paymentsIntoPension.copy(
                retirementAnnuityContractPaymentsQuestion = Some(true)
              )
            ))

          val expectedViewModel =
            sessionData.pensions.paymentsIntoPension.copy(
              pensionTaxReliefNotClaimedQuestion = Some(true),
              retirementAnnuityContractPaymentsQuestion = Some(true)
            )

          implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
          implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoPage(Some(true)))

          assertRedirectionAsExpected(PageRelativeURLs.paymentsIntoPensionsCYAPage)
          getViewModel mustBe Some(expectedViewModel)

        }
        "the user has selected 'Yes', but they have to answer about the 'retirement annuity' contract" in {

          val sessionData = pensionsUserData(
            aPensionsCYAModel.copy(
              paymentsIntoPension = aPensionsCYAModel.paymentsIntoPension.copy(
                retirementAnnuityContractPaymentsQuestion = None
              )
            ))

          val expectedViewModel =
            sessionData.pensions.paymentsIntoPension.copy(
              pensionTaxReliefNotClaimedQuestion = Some(true)
            )

          implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
          implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoPage(Some(true)))

          assertRedirectionAsExpected(PageRelativeURLs.paymentsIntoPensionsRetirementAnnuityPage)
          getViewModel mustBe Some(expectedViewModel)

        }
      }
      "fail" when {
        "the user has selected neither 'Yes' nor 'No' and" when {

          val sessionData = pensionsUserData(minimalSessionDataToAccessThisPage)
          val expectedViewModel = sessionData.pensions.paymentsIntoPension

          scenarioNameForIndividualAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoPage(None))

            assertPageAsExpected(
              BAD_REQUEST,
              ExpectedYesNoPageContents(
                title = "Error: Pensions where tax relief is not claimed",
                header = "Pensions where tax relief is not claimed",
                caption = "Payments into pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                links = Set.empty,
                text = Set(
                  ExpectedText(selectorForFirstParagraph, "These questions are about pensions you pay into where tax relief is not claimed for you."),
                  ExpectedText(selectorForSecondParagraph, "You can check your pension statements or contact your pension provider to find the information you need."),
                  ExpectedText(selectorForSubHeading, "Did you pay into a pension where tax relief was not claimed for you?")
                ),
                errorSummarySectionOpt = Some(
                  ErrorSummarySection(
                    title = "There is a problem",
                    body = "Select yes if you paid into a pension where tax relief was not claimed for you",
                    link = "#value")
                ),
                errorAboveElementCheckSectionOpt = Some(
                  ErrorAboveElementCheckSection(
                    title = "Error: Select yes if you paid into a pension where tax relief was not claimed for you",
                    idOpt = Some("value")
                  )
                )
              ))
            getViewModel mustBe Some(expectedViewModel)

          }
          scenarioNameForIndividualAndWelsh in {

            implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
            implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoPage(None))

            assertPageAsExpected(
              BAD_REQUEST,
              ExpectedYesNoPageContents(
                title = "Error: Pensions where tax relief is not claimed",
                header = "Pensions where tax relief is not claimed",
                caption = "Payments into pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                links = Set.empty,
                text = Set(
                  ExpectedText(selectorForFirstParagraph, "These questions are about pensions you pay into where tax relief is not claimed for you."),
                  ExpectedText(selectorForSecondParagraph, "You can check your pension statements or contact your pension provider to find the information you need."),
                  ExpectedText(selectorForSubHeading, "Did you pay into a pension where tax relief was not claimed for you?")
                ),
                errorSummarySectionOpt = Some(
                  ErrorSummarySection(
                    title = "Mae problem wedi codi",
                    body = "Select yes if you paid into a pension where tax relief was not claimed for you",
                    link = "#value")
                ),
                errorAboveElementCheckSectionOpt = Some(
                  ErrorAboveElementCheckSection(
                    title = "Error: Select yes if you paid into a pension where tax relief was not claimed for you",
                    idOpt = Some("value")
                  )
                )
              ))
            getViewModel mustBe Some(expectedViewModel)

          }
          scenarioNameForAgentAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
            implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoPage(None))

            assertPageAsExpected(
              BAD_REQUEST,
              ExpectedYesNoPageContents(
                title = "Error: Pensions where tax relief is not claimed",
                header = "Pensions where tax relief is not claimed",
                caption = "Payments into pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                links = Set.empty,
                text = Set(
                  ExpectedText(selectorForFirstParagraph, "These questions are about pensions your client pays into where tax relief is not claimed for them."),
                  ExpectedText(selectorForSecondParagraph, "You can check your client’s pension statements or contact your client’s pension provider to find the information you need."),
                  ExpectedText(selectorForSubHeading, "Did your client pay into a pension where tax relief was not claimed for them?")
                ),
                errorSummarySectionOpt = Some(
                  ErrorSummarySection(
                    title = "There is a problem",
                    body = "Select yes if your client paid into a pension where tax relief was not claimed for them",
                    link = "#value")
                ),
                errorAboveElementCheckSectionOpt = Some(
                  ErrorAboveElementCheckSection(
                    title = "Error: Select yes if your client paid into a pension where tax relief was not claimed for them",
                    idOpt = Some("value")
                  )
                )
              ))
            getViewModel mustBe Some(expectedViewModel)

          }
          scenarioNameForAgentAndWelsh in {

            implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
            implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoPage(None))

            assertPageAsExpected(
              BAD_REQUEST,
              ExpectedYesNoPageContents(
                title = "Error: Pensions where tax relief is not claimed",
                header = "Pensions where tax relief is not claimed",
                caption = "Payments into pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                links = Set.empty,
                text = Set(
                  ExpectedText(selectorForFirstParagraph, "These questions are about pensions your client pays into where tax relief is not claimed for them."),
                  ExpectedText(selectorForSecondParagraph, "You can check your client’s pension statements or contact your client’s pension provider to find the information you need."),
                  ExpectedText(selectorForSubHeading, "Did your client pay into a pension where tax relief was not claimed for them?")
                ),
                errorSummarySectionOpt = Some(
                  ErrorSummarySection(
                    title = "Mae problem wedi codi",
                    body = "Select yes if your client paid into a pension where tax relief was not claimed for them",
                    link = "#value")
                ),
                errorAboveElementCheckSectionOpt = Some(
                  ErrorAboveElementCheckSection(
                    title = "Error: Select yes if your client paid into a pension where tax relief was not claimed for them",
                    idOpt = Some("value")
                  )
                )
              ))
            getViewModel mustBe Some(expectedViewModel)

          }
        }

      }
    }
  }

  private def getViewModel(implicit userConfig: UserConfig): Option[PaymentsIntoPensionViewModel] =
    loadPensionUserData.map(_.pensions.paymentsIntoPension)

}







