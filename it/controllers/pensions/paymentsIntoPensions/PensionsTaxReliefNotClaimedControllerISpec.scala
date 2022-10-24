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
import org.jsoup.Jsoup.parse
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}

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

          val response = getPage(None)

          response must haveStatus(SEE_OTHER)
          response must haveALocationHeaderValue(PageRelativeURLs.paymentsIntoPensionsCYAPage)

        }
        "the user has a session with an empty pensions model" in {

          val response = getPage(Some(pensionsUserData(aPensionsCYAEmptyModel)))

          response must haveStatus(SEE_OTHER)
          response must haveALocationHeaderValue(PageRelativeURLs.paymentsIntoPensionsReliefAtSourcePage)

        }
        "the user has a session with an insufficient pensions model" when {
          "only the RAS question has been answered" in {

            val sessionData = pensionsUserData(
              aPensionsCYAEmptyModel
                .copy(paymentsIntoPension =
                  PaymentsIntoPensionViewModel(
                    rasPensionPaymentQuestion = Some(true)
                  ))
            )

            val response = getPage(Some(sessionData))

            response must haveStatus(SEE_OTHER)
            response must haveALocationHeaderValue(PageRelativeURLs.paymentsIntoPensionsReliefAtSourceAmountPage)

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

            val response = getPage(Some(sessionData))

            response must haveStatus(SEE_OTHER)
            response must haveALocationHeaderValue(PageRelativeURLs.paymentsIntoPensionsOneOffPaymentsPage)

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

            val response = getPage(Some(sessionData))

            response must haveStatus(SEE_OTHER)
            response must haveALocationHeaderValue(PageRelativeURLs.paymentsIntoPensionsOneOffPaymentsAmountPage)

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

            val response = getPage(Some(sessionData))

            response must haveStatus(SEE_OTHER)
            response must haveALocationHeaderValue(PageRelativeURLs.paymentsIntoPensionsTotalReliefAtSourceCheckPage)

          }
        }
      }

      "appear as expected" when {
        "the user has only the minimal session data for accessing this page and" when {

          val sessionData = pensionsUserData(minimalSessionDataToAccessThisPage)

          scenarioNameForIndividualAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            val response = getPage

            response must haveStatus(OK)
            assertPageAsExpected(
              parse(response.body),
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
            val response = getPage

            response must haveStatus(OK)
            assertPageAsExpected(
              parse(response.body),
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
            val response = getPage

            response must haveStatus(OK)
            assertPageAsExpected(
              parse(response.body),
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
            val response = getPage

            response must haveStatus(OK)
            assertPageAsExpected(
              parse(response.body),
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
            val response = getPage

            response must haveStatus(OK)
            assertPageAsExpected(
              parse(response.body),
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
            val response = getPage

            response must haveStatus(OK)
            assertPageAsExpected(
              parse(response.body),
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
            val response = getPage

            response must haveStatus(OK)
            assertPageAsExpected(
              parse(response.body),
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
            val response = getPage

            response must haveStatus(OK)
            assertPageAsExpected(
              parse(response.body),
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
            val response = getPage

            response must haveStatus(OK)
            assertPageAsExpected(
              parse(response.body),
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
            val response = getPage

            response must haveStatus(OK)
            assertPageAsExpected(
              parse(response.body),
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
            val response = getPage

            response must haveStatus(OK)
            assertPageAsExpected(
              parse(response.body),
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
            val response = getPage

            response must haveStatus(OK)
            assertPageAsExpected(
              parse(response.body),
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
      "succeed" when {
        "the user has selected 'No' and" when {

          val sessionData = pensionsUserData(minimalSessionDataToAccessThisPage)

          val expectedViewModel =
            sessionData.pensions.paymentsIntoPension.copy(
              pensionTaxReliefNotClaimedQuestion = Some(false),
              retirementAnnuityContractPaymentsQuestion = None,
              totalRetirementAnnuityContractPayments = None,
              workplacePensionPaymentsQuestion = None,
              totalWorkplacePensionPayments = None
            )

          scenarioNameForIndividualAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            val response = submitForm(SubmittedFormDataForYesNoPage(Some(false)))

            response must haveStatus(SEE_OTHER)
            response must haveALocationHeaderValue(PageRelativeURLs.paymentsIntoPensionsCYAPage)
            getViewModel mustBe expectedViewModel

          }
          scenarioNameForIndividualAndWelsh in {

            implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
            val response = submitForm(SubmittedFormDataForYesNoPage(Some(false)))

            response must haveStatus(SEE_OTHER)
            response must haveALocationHeaderValue(PageRelativeURLs.paymentsIntoPensionsCYAPage)
            getViewModel mustBe expectedViewModel

          }
          scenarioNameForAgentAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
            val response = submitForm(SubmittedFormDataForYesNoPage(Some(false)))

            response must haveStatus(SEE_OTHER)
            response must haveALocationHeaderValue(PageRelativeURLs.paymentsIntoPensionsCYAPage)
            getViewModel mustBe expectedViewModel

          }
          scenarioNameForAgentAndWelsh in {

            implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
            val response = submitForm(SubmittedFormDataForYesNoPage(Some(false)))

            response must haveStatus(SEE_OTHER)
            response must haveALocationHeaderValue(PageRelativeURLs.paymentsIntoPensionsCYAPage)
            getViewModel mustBe expectedViewModel

          }
        }
        "the user has selected 'Yes', they've already answered 'Yes' about the 'retirement annuity' contract, and" when {

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

          scenarioNameForIndividualAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            val response = submitForm(SubmittedFormDataForYesNoPage(Some(true)))

            response must haveStatus(SEE_OTHER)
            response must haveALocationHeaderValue(PageRelativeURLs.paymentsIntoPensionsCYAPage)
            getViewModel mustBe expectedViewModel

          }
          scenarioNameForIndividualAndWelsh in {

            implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
            val response = submitForm(SubmittedFormDataForYesNoPage(Some(true)))

            response must haveStatus(SEE_OTHER)
            response must haveALocationHeaderValue(PageRelativeURLs.paymentsIntoPensionsCYAPage)
            getViewModel mustBe expectedViewModel

          }
          scenarioNameForAgentAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
            val response = submitForm(SubmittedFormDataForYesNoPage(Some(true)))

            response must haveStatus(SEE_OTHER)
            response must haveALocationHeaderValue(PageRelativeURLs.paymentsIntoPensionsCYAPage)
            getViewModel mustBe expectedViewModel

          }
          scenarioNameForAgentAndWelsh in {

            implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
            val response = submitForm(SubmittedFormDataForYesNoPage(Some(true)))

            response must haveStatus(SEE_OTHER)
            response must haveALocationHeaderValue(PageRelativeURLs.paymentsIntoPensionsCYAPage)
            getViewModel mustBe expectedViewModel

          }
        }
        "the user has selected 'Yes', but they have to answer about the 'retirement annuity' contract, and" when {

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

          scenarioNameForIndividualAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            val response = submitForm(SubmittedFormDataForYesNoPage(Some(true)))

            response must haveStatus(SEE_OTHER)
            response must haveALocationHeaderValue(PageRelativeURLs.paymentsIntoPensionsRetirementAnnuityPage)
            getViewModel mustBe expectedViewModel

          }
          scenarioNameForIndividualAndWelsh in {

            implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
            val response = submitForm(SubmittedFormDataForYesNoPage(Some(true)))

            response must haveStatus(SEE_OTHER)
            response must haveALocationHeaderValue(PageRelativeURLs.paymentsIntoPensionsRetirementAnnuityPage)
            getViewModel mustBe expectedViewModel

          }
          scenarioNameForAgentAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
            val response = submitForm(SubmittedFormDataForYesNoPage(Some(true)))

            response must haveStatus(SEE_OTHER)
            response must haveALocationHeaderValue(PageRelativeURLs.paymentsIntoPensionsRetirementAnnuityPage)
            getViewModel mustBe expectedViewModel

          }
          scenarioNameForAgentAndWelsh in {

            implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
            val response = submitForm(SubmittedFormDataForYesNoPage(Some(true)))

            response must haveStatus(SEE_OTHER)
            response must haveALocationHeaderValue(PageRelativeURLs.paymentsIntoPensionsRetirementAnnuityPage)
            getViewModel mustBe expectedViewModel

          }
        }


      }
      "fail" when {
        "the user has selected neither 'Yes' nor 'No' and" when {

          val sessionData = pensionsUserData(minimalSessionDataToAccessThisPage)

          scenarioNameForIndividualAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            val response = submitForm(SubmittedFormDataForYesNoPage(None))

            response must haveStatus(BAD_REQUEST)
            assertPageAsExpected(
              parse(response.body),
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

          }
          scenarioNameForIndividualAndWelsh in {

            implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
            val response = submitForm(SubmittedFormDataForYesNoPage(None))

            response must haveStatus(BAD_REQUEST)
            assertPageAsExpected(
              parse(response.body),
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

          }
          scenarioNameForAgentAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
            val response = submitForm(SubmittedFormDataForYesNoPage(None))

            response must haveStatus(BAD_REQUEST)
            assertPageAsExpected(
              parse(response.body),
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

          }
          scenarioNameForAgentAndWelsh in {

            implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
            val response = submitForm(SubmittedFormDataForYesNoPage(None))

            response must haveStatus(BAD_REQUEST)
            assertPageAsExpected(
              parse(response.body),
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

          }
        }

      }
    }
  }

  private def getViewModel(implicit userConfig: UserConfig): PaymentsIntoPensionViewModel =
    loadPensionUserData.pensions.paymentsIntoPension

}







