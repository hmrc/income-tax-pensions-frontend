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

import builders.PensionsCYAModelBuilder.{aPensionsCYAEmptyModel, aPensionsCYAModel}
import controllers.ControllerSpec.PreferredLanguages.{English, Welsh}
import controllers.ControllerSpec.UserTypes.{Agent, Individual}
import controllers.ControllerSpec._
import controllers.YesNoAmountControllerSpec
import models.mongo.PensionsUserData
import models.pension.charges.PensionScheme
import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.libs.ws.{WSClient, WSResponse}

class SpecialWithholdingTaxControllerISpec   extends YesNoAmountControllerSpec("/overseas-pensions/income-from-overseas-pensions/pension-overseas-income-swt") {

  "This page" when {
    "requested to be shown" should {
      "redirect to the summary page" when {
        "the user has no stored session data at all" in {

          implicit val userConfig: UserConfig = userConfigWhenIrrelevant(None)
          implicit val response: WSResponse = getPageWithIndex()

          assertRedirectionAsExpected(PageRelativeURLs.overseasSummaryPage)

        }
        "the user had not previously specified the  amount" in {

          val sessionData: PensionsUserData = pensionsUserData(aPensionsCYAEmptyModel)

          implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
          implicit val response: WSResponse = getPageWithIndex()

          assertRedirectionAsExpected(PageRelativeURLs.overseasSummaryPage)
        }
        "incorrect index provided" in {
          val sessionData = pensionsUserData(aPensionsCYAModel)

          implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
          implicit val response: WSResponse = getPageWithIndex(7)
          assertRedirectionAsExpected(PageRelativeURLs.overseasSummaryPage)
        }
        "the user provides a negative index number" in {
          val sessionData = pensionsUserData(aPensionsCYAModel)
          implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
          implicit val response: WSResponse = getPageWithIndex(-1)

          assertRedirectionAsExpected(PageRelativeURLs.overseasSummaryPage)
        }
      }
      "appear as expected" when {

        val expectedContentIndividual = ExpectedYesNoAmountPageContents(
          title = "Did you have Special Withholding Tax (SWT) deducted from your pension?",
          header = "Did you have Special Withholding Tax (SWT) deducted from your pension?",
          caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
          radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
          radioButtonForNo = uncheckedExpectedRadioButton("No"),
          buttonForContinue = ExpectedButton("Continue", ""),
          amountSection = ExpectedAmountSection("Amount of SWT, in pounds", "", Some("For example, £193.54")),
          formUrl = formUrl()
        )
        
        val expectedContentAgent = expectedContentIndividual.copy(
          title = "Did your client have Special Withholding Tax (SWT) deducted from their pension?",
          header = "Did your client have Special Withholding Tax (SWT) deducted from their pension?",
        )
        
        "the user has no session data relevant to this page and" when {

          val updatedPensionScheme = aPensionsCYAModel.incomeFromOverseasPensions.overseasIncomePensionSchemes.updated(
            0, PensionScheme(
              alphaTwoCode = Some("FR"),
              alphaThreeCode = Some("FRA"),
              pensionPaymentAmount = Some(1999.99),
              pensionPaymentTaxPaid = Some(1999.99),
              specialWithholdingTaxQuestion = None,
              specialWithholdingTaxAmount = None,
              foreignTaxCreditReliefQuestion = Some(false),
              taxableAmount = Some(1999.99)
            ))

          val sessionData: PensionsUserData =
            pensionsUserData(aPensionsCYAModel.copy(
              incomeFromOverseasPensions = aPensionsCYAModel.incomeFromOverseasPensions.copy(overseasIncomePensionSchemes =
                updatedPensionScheme)
            ))

          scenarioNameForIndividualAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            implicit val response: WSResponse = getPageWithIndex()

            assertPageAsExpected(OK, expectedContentIndividual)
          }
          
          scenarioNameForIndividualAndWelsh in {

            implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
            implicit val response: WSResponse = getPageWithIndex()

            assertPageAsExpected(OK, expectedContentIndividual)
          }
          
          scenarioNameForAgentAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
            implicit val response: WSResponse = getPageWithIndex()

            assertPageAsExpected(OK, expectedContentAgent)
          }
          scenarioNameForAgentAndWelsh in {

            implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
            implicit val response: WSResponse = getPageWithIndex()

            assertPageAsExpected(OK, expectedContentAgent)
          }
        }
        "the user had previously answered 'Yes' with a valid amount, and" when {

          val sessionData: PensionsUserData =
            pensionsUserData(aPensionsCYAModel.copy(
              incomeFromOverseasPensions = aPensionsCYAModel.incomeFromOverseasPensions.copy(overseasIncomePensionSchemes =
                aPensionsCYAModel.incomeFromOverseasPensions.overseasIncomePensionSchemes.updated(
                  0, PensionScheme(
                    alphaTwoCode = Some("FR"),
                    alphaThreeCode = Some("FRA"),
                    pensionPaymentAmount = Some(1999.99),
                    pensionPaymentTaxPaid = Some(1999.99),
                    specialWithholdingTaxQuestion = Some(true),
                    specialWithholdingTaxAmount = Some(1999.99),
                    foreignTaxCreditReliefQuestion = Some(false),
                    taxableAmount = Some(1999.99)
                  )))
            ))

          val expContentIndividual = expectedContentIndividual.copy(
            radioButtonForYes = checkedExpectedRadioButton("Yes"),
            amountSection = ExpectedAmountSection("Amount of SWT, in pounds", "1,999.99", Some("For example, £193.54"))
          )
          
          val expContentAgent = expectedContentAgent.copy(
            radioButtonForYes = checkedExpectedRadioButton("Yes"),
            amountSection = ExpectedAmountSection("Amount of SWT, in pounds", "1,999.99", Some("For example, £193.54"))
          )

          scenarioNameForIndividualAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            implicit val response: WSResponse = getPageWithIndex()

            assertPageAsExpected(OK, expContentIndividual)
          }
          
          scenarioNameForIndividualAndWelsh in {

            implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
            implicit val response: WSResponse = getPageWithIndex()

            assertPageAsExpected(OK, expContentIndividual)
          }
          
          scenarioNameForAgentAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
            implicit val response: WSResponse = getPageWithIndex()

            assertPageAsExpected(OK, expContentAgent)
          }
          
          scenarioNameForAgentAndWelsh in {

            implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
            implicit val response: WSResponse = getPageWithIndex()

            assertPageAsExpected(OK, expContentAgent)
          }

        }
        "the user has multiple pensionSchemes, and" when {

          val newSequence = PensionScheme(
            alphaTwoCode = Some("GB"),
            alphaThreeCode = Some("GBR"),
            pensionPaymentAmount = Some(1999.99),
            pensionPaymentTaxPaid = Some(1999.99),
            specialWithholdingTaxQuestion = Some(true),
            specialWithholdingTaxAmount = Some(1999.99),
            foreignTaxCreditReliefQuestion = Some(false),
            taxableAmount = Some(1999.99)
          ) +: aPensionsCYAModel.incomeFromOverseasPensions.overseasIncomePensionSchemes
          
          val sessionData: PensionsUserData =
            pensionsUserData(aPensionsCYAModel.copy(
              incomeFromOverseasPensions = aPensionsCYAModel.incomeFromOverseasPensions.copy(overseasIncomePensionSchemes = newSequence)
            ))
            
          val expContentIndividual = expectedContentIndividual.copy(
            radioButtonForYes = checkedExpectedRadioButton("Yes"),
            amountSection = ExpectedAmountSection("Amount of SWT, in pounds", "1,999.99", Some("For example, £193.54")),
            formUrl = formUrl(1)
          )
          
          val expContentAgent = expectedContentAgent.copy(
            radioButtonForYes = checkedExpectedRadioButton("Yes"),
            amountSection = ExpectedAmountSection("Amount of SWT, in pounds", "1,999.99", Some("For example, £193.54")),
            formUrl = formUrl(1)
          )

          scenarioNameForIndividualAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            implicit val response: WSResponse = getPageWithIndex(1)

            assertPageAsExpected(OK, expContentIndividual)
          }
          
          scenarioNameForIndividualAndWelsh in {

            implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
            implicit val response: WSResponse = getPageWithIndex(1)

            assertPageAsExpected(OK, expContentIndividual)
          }
          
          scenarioNameForAgentAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
            implicit val response: WSResponse = getPageWithIndex(1)

            assertPageAsExpected(OK, expContentAgent)
          }
          scenarioNameForAgentAndWelsh in {

            implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
            implicit val response: WSResponse = getPageWithIndex(1)

            assertPageAsExpected(OK, expContentAgent)
          }

        }
        "the user had previously answered 'No' without an amount, and" when {


          val updatedPensionScheme = aPensionsCYAModel.incomeFromOverseasPensions.overseasIncomePensionSchemes.updated(
            0, PensionScheme(
              alphaTwoCode = Some("FR"),
              alphaThreeCode = Some("FRA"),
              pensionPaymentAmount = Some(1999.99),
              pensionPaymentTaxPaid = Some(1999.99),
              specialWithholdingTaxQuestion = Some(false),
              specialWithholdingTaxAmount = None,
              foreignTaxCreditReliefQuestion = Some(false),
              taxableAmount = Some(1999.99)
            ))

          val sessionData: PensionsUserData =
            pensionsUserData(aPensionsCYAModel.copy(
              incomeFromOverseasPensions = aPensionsCYAModel.incomeFromOverseasPensions.copy(overseasIncomePensionSchemes =
                updatedPensionScheme)
            ))

          val expContentIndividual = expectedContentIndividual.copy(
            radioButtonForNo = checkedExpectedRadioButton("No"),
            amountSection = ExpectedAmountSection("Amount of SWT, in pounds", "", Some("For example, £193.54")),
          )

          val expContentAgent = expectedContentAgent.copy(
            radioButtonForNo = checkedExpectedRadioButton("No"),
            amountSection = ExpectedAmountSection("Amount of SWT, in pounds", "", Some("For example, £193.54")),
          )

          scenarioNameForIndividualAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            implicit val response: WSResponse = getPageWithIndex()

            assertPageAsExpected(OK, expContentIndividual)
          }

          scenarioNameForIndividualAndWelsh in {

            implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
            implicit val response: WSResponse = getPageWithIndex()


            assertPageAsExpected(OK, expContentIndividual)
          }

          scenarioNameForAgentAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
            implicit val response: WSResponse = getPageWithIndex()

            assertPageAsExpected(OK, expContentAgent)
          }

          scenarioNameForAgentAndWelsh in {

            implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
            implicit val response: WSResponse = getPageWithIndex()

            assertPageAsExpected(OK, expContentAgent)
          }
          
        }
        "the user had previously answered 'No' with an amount of zero, and" when {

          val updatedPensionScheme = aPensionsCYAModel.incomeFromOverseasPensions.overseasIncomePensionSchemes.updated(
            0, PensionScheme(
              alphaTwoCode = Some("FR"),
              alphaThreeCode = Some("FRA"),
              pensionPaymentAmount = Some(1999.99),
              pensionPaymentTaxPaid = Some(1999.99),
              specialWithholdingTaxQuestion = Some(false),
              specialWithholdingTaxAmount = Some(BigDecimal(0)),
              foreignTaxCreditReliefQuestion = Some(false),
              taxableAmount = Some(1999.99)
            ))

          val sessionData: PensionsUserData =
            pensionsUserData(aPensionsCYAModel.copy(
              incomeFromOverseasPensions = aPensionsCYAModel.incomeFromOverseasPensions.copy(overseasIncomePensionSchemes =
                updatedPensionScheme)
            ))

          val expContentIndividual = expectedContentIndividual.copy(
            radioButtonForNo = checkedExpectedRadioButton("No"),
            amountSection = ExpectedAmountSection("Amount of SWT, in pounds", "", Some("For example, £193.54")),
          )

          val expContentAgent = expectedContentAgent.copy(
            radioButtonForNo = checkedExpectedRadioButton("No"),
            amountSection = ExpectedAmountSection("Amount of SWT, in pounds", "", Some("For example, £193.54")),
          )

          scenarioNameForIndividualAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            implicit val response: WSResponse = getPageWithIndex()

            assertPageAsExpected(OK, expContentIndividual)
          }
          scenarioNameForIndividualAndWelsh in {

            implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
            implicit val response: WSResponse = getPageWithIndex()


            assertPageAsExpected(OK, expContentIndividual)
          }
          scenarioNameForAgentAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
            implicit val response: WSResponse = getPageWithIndex()

            assertPageAsExpected(OK, expContentAgent)
          }
          scenarioNameForAgentAndWelsh in {

            implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
            implicit val response: WSResponse = getPageWithIndex()

            assertPageAsExpected(OK, expContentAgent)
          }
          
        }
        "the user had previously answered 'No' with a negative amount, and" when {


          val updatedPensionScheme = aPensionsCYAModel.incomeFromOverseasPensions.overseasIncomePensionSchemes.updated(
            0, PensionScheme(
              alphaTwoCode = Some("FR"),
              alphaThreeCode = Some("FRA"),
              pensionPaymentAmount = Some(1999.99),
              pensionPaymentTaxPaid = Some(1999.99),
              specialWithholdingTaxQuestion = Some(false),
              specialWithholdingTaxAmount = Some(BigDecimal(-42.64)),
              foreignTaxCreditReliefQuestion = Some(false),
              taxableAmount = Some(1999.99)
            ))

          val sessionData: PensionsUserData =
            pensionsUserData(aPensionsCYAModel.copy(
              incomeFromOverseasPensions = aPensionsCYAModel.incomeFromOverseasPensions.copy(overseasIncomePensionSchemes =
                updatedPensionScheme)
            ))
            
          val expContentIndividual = expectedContentIndividual.copy(
            radioButtonForNo = checkedExpectedRadioButton("No"),
            amountSection = ExpectedAmountSection("Amount of SWT, in pounds", "", Some("For example, £193.54")),
          )

          val expContentAgent = expectedContentAgent.copy(
            radioButtonForNo = checkedExpectedRadioButton("No"),
            amountSection = ExpectedAmountSection("Amount of SWT, in pounds", "", Some("For example, £193.54")),
          )

          scenarioNameForIndividualAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            implicit val response: WSResponse = getPageWithIndex()

            assertPageAsExpected(OK, expContentIndividual)
          }
          scenarioNameForIndividualAndWelsh in {

            implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
            implicit val response: WSResponse = getPageWithIndex()


            assertPageAsExpected(OK, expContentIndividual)
          }
          scenarioNameForAgentAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
            implicit val response: WSResponse = getPageWithIndex()

            assertPageAsExpected(OK, expContentAgent)
          }
          
          scenarioNameForAgentAndWelsh in {

            implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
            implicit val response: WSResponse = getPageWithIndex()

            assertPageAsExpected(OK, expContentAgent)
          }

        }

      }
    }
    "submitted" should {
      "redirect to the expected page" when {
        "the user had not previously specified the 'no surcharge' amount" in {

          val sessionData: PensionsUserData =
            pensionsUserData(aPensionsCYAModel.copy(
              incomeFromOverseasPensions = aPensionsCYAModel.incomeFromOverseasPensions.copy(overseasIncomePensionSchemes =
                aPensionsCYAModel.incomeFromOverseasPensions.overseasIncomePensionSchemes.updated(
                  0, PensionScheme(
                    alphaTwoCode = Some("FR"),
                    alphaThreeCode = Some("FRA"),
                    pensionPaymentAmount = None,
                    pensionPaymentTaxPaid = None,
                    specialWithholdingTaxQuestion = None,
                    specialWithholdingTaxAmount = None,
                    foreignTaxCreditReliefQuestion = None,
                    taxableAmount = None
                  )))
            ))

          val expectedViewModel = sessionData.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes(0)

          implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
          implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(false), None))
          implicit val index = 0

          assertRedirectionAsExpected(PageRelativeURLs.overseasSummaryPage)
          getViewModel mustBe Some(expectedViewModel)

        }
        "the user has no stored session data at all" in {

          implicit val userConfig: UserConfig = userConfigWhenIrrelevant(None)
          implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(false), None))
          implicit val index = 0

          assertRedirectionAsExpected(PageRelativeURLs.overseasSummaryPage)
          getViewModel mustBe None

        }
      }
      "succeed" when {
        "the user has relevant session data and" when {

          val sessionData = pensionsUserData(aPensionsCYAModel)

          "the user has selected 'No' with a blank amount" in {

            val updatedModel: PensionsUserData =
              pensionsUserData(sessionData.pensions.copy(
                incomeFromOverseasPensions = sessionData.pensions.incomeFromOverseasPensions.copy(overseasIncomePensionSchemes =
                  sessionData.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes.updated(
                    0, PensionScheme(
                      alphaTwoCode = Some("FR"),
                      alphaThreeCode = Some("FRA"),
                      pensionPaymentAmount = Some(1999.99),
                      pensionPaymentTaxPaid = Some(1999.99),
                      specialWithholdingTaxQuestion =  Some(false),
                      specialWithholdingTaxAmount = None,
                      foreignTaxCreditReliefQuestion = Some(false),
                      taxableAmount = Some(1999.99)
                    )))
              ))

            val expectedViewModel = updatedModel.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes(0)


            implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
            implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoAmountPage(Some(false), Some("")))
            implicit val index = 0

            assertRedirectionAsExpected(relativeUrl("/overseas-pensions/income-from-overseas-pensions/pension-overseas-income-ftcr?index=0"))
            getViewModel mustBe Some(expectedViewModel)

          }
          "the user has selected 'Yes' as well as a valid amount (unformatted), and" in {

            val updatedPensionScheme = aPensionsCYAModel.incomeFromOverseasPensions.overseasIncomePensionSchemes.updated(
              0, PensionScheme(
                alphaTwoCode = Some("FR"),
                alphaThreeCode = Some("FRA"),
                pensionPaymentAmount = Some(1999.99),
                pensionPaymentTaxPaid = Some(1999.99),
                specialWithholdingTaxQuestion =  Some(true),
                specialWithholdingTaxAmount =  Some(BigDecimal(42.64)),
                foreignTaxCreditReliefQuestion = Some(false),
                taxableAmount = Some(1999.99)
              ))

            val updatedModel: PensionsUserData =
              pensionsUserData(aPensionsCYAModel.copy(
                incomeFromOverseasPensions = aPensionsCYAModel.incomeFromOverseasPensions.copy(overseasIncomePensionSchemes =
                  updatedPensionScheme)
              ))

            val expectedViewModel = updatedModel.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes(0)


            implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
            implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoAmountPage(Some(true), Some("42.64")))
            implicit val index = 0

            assertRedirectionAsExpected(relativeUrl("/overseas-pensions/income-from-overseas-pensions/pension-overseas-income-ftcr?index=0"))
            getViewModel mustBe Some(expectedViewModel)

          }
          "the user has selected 'Yes' as well as a valid amount (formatted), and" in {

            val updatedPensionScheme = aPensionsCYAModel.incomeFromOverseasPensions.overseasIncomePensionSchemes.updated(
              0, PensionScheme(
                alphaTwoCode = Some("FR"),
                alphaThreeCode = Some("FRA"),
                pensionPaymentAmount = Some(1999.99),
                pensionPaymentTaxPaid = Some(1999.99),
                specialWithholdingTaxQuestion =  Some(true),
                specialWithholdingTaxAmount =  Some(BigDecimal(1042.64)),
                foreignTaxCreditReliefQuestion = Some(false),
                taxableAmount = Some(1999.99)
              ))

            val updatedModel: PensionsUserData =
              pensionsUserData(aPensionsCYAModel.copy(
                incomeFromOverseasPensions = aPensionsCYAModel.incomeFromOverseasPensions.copy(overseasIncomePensionSchemes =
                  updatedPensionScheme)
              ))

            val expectedViewModel = updatedModel.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes(0)

            implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
            implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoAmountPage(Some(true), Some("£1,042.64")))
            implicit val index = 0

            assertRedirectionAsExpected(relativeUrl("/overseas-pensions/income-from-overseas-pensions/pension-overseas-income-ftcr?index=0"))
            getViewModel mustBe Some(expectedViewModel)

          }

        }
      }
      "fail" when {
        
        val expectedContentsIndividual = ExpectedYesNoAmountPageContents(
          title = "Error: Did you have Special Withholding Tax (SWT) deducted from your pension?",
          header = "Did you have Special Withholding Tax (SWT) deducted from your pension?",
          caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
          radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
          radioButtonForNo = uncheckedExpectedRadioButton("No"),
          buttonForContinue = ExpectedButton("Continue", ""),
          amountSection = ExpectedAmountSection("Amount of SWT, in pounds", "", Some("For example, £193.54")),
          errorSummarySectionOpt = Some(
            ErrorSummarySection(
              title = "There is a problem",
              body = "Select yes or no if you had Special Withholding Tax deducted from your pension.",
              link = "#value")
          ),
          errorAboveElementCheckSectionOpt = Some(
            ErrorAboveElementCheckSection(
              title = "Error: Select yes or no if you had Special Withholding Tax deducted from your pension.",
              idOpt = Some("value")
            )
          ),
          formUrl = formUrl()
        )
        val expectedContentsAgent = expectedContentsIndividual.copy(
          title = "Error: Did your client have Special Withholding Tax (SWT) deducted from their pension?",
          header = "Did your client have Special Withholding Tax (SWT) deducted from their pension?",
          errorSummarySectionOpt = Some(ErrorSummarySection(
            title = "There is a problem", body = "Select yes or no if your client had Special Withholding Tax deducted from their pension.", link = "#value")
          ),
          errorAboveElementCheckSectionOpt = Some(ErrorAboveElementCheckSection(
            title = "Error: Select yes or no if your client had Special Withholding Tax deducted from their pension.", idOpt = Some("value"))
          )
        )

        def welshTitle(epc:ExpectedYesNoAmountPageContents): Option[ErrorSummarySection] =
          epc.errorSummarySectionOpt.map(ess => ess.copy(title = "Mae problem wedi codi"))


        "the user has no session data relevant to this page and" when {

          val updatedPensionScheme = aPensionsCYAModel.incomeFromOverseasPensions.overseasIncomePensionSchemes.updated(
            0, PensionScheme(
              alphaTwoCode = Some("FR"),
              alphaThreeCode = Some("FRA"),
              pensionPaymentAmount = Some(1999.99),
              pensionPaymentTaxPaid = Some(1999.99),
              specialWithholdingTaxQuestion = None,
              specialWithholdingTaxAmount = None,
              foreignTaxCreditReliefQuestion = Some(false),
              taxableAmount = Some(1999.99)
            ))

          val sessionData: PensionsUserData =
            pensionsUserData(aPensionsCYAModel.copy(
              incomeFromOverseasPensions = aPensionsCYAModel.incomeFromOverseasPensions.copy(overseasIncomePensionSchemes =
                updatedPensionScheme)
            ))

          val expectedViewModel = sessionData.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes(0)
          implicit val index = 0
          
          
          "the user has selected neither 'Yes' nor 'No' and" when {
            scenarioNameForIndividualAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
              implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoAmountPage(None, None))

              assertPageAsExpected(BAD_REQUEST, expectedContentsIndividual)
              getViewModel mustBe Some(expectedViewModel)

            }
            scenarioNameForIndividualAndWelsh in {

              implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoAmountPage(None, None))

              assertPageAsExpected(BAD_REQUEST, expectedContentsIndividual.copy(errorSummarySectionOpt = welshTitle(expectedContentsIndividual)))
              getViewModel mustBe Some(expectedViewModel)

            }
            scenarioNameForAgentAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
              implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoAmountPage(None, None))

              assertPageAsExpected(BAD_REQUEST, expectedContentsAgent)
              getViewModel mustBe Some(expectedViewModel)

            }
            scenarioNameForAgentAndWelsh in {

              implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoAmountPage(None, None))

              assertPageAsExpected(BAD_REQUEST, expectedContentsAgent.copy(errorSummarySectionOpt = welshTitle(expectedContentsAgent)))
              getViewModel mustBe Some(expectedViewModel)
            }
          }
          "the user has selected 'Yes' but has provided an empty amount, and" when {
            
            val expContentsIndividual = expectedContentsIndividual.copy(
              radioButtonForYes = checkedExpectedRadioButton("Yes"),
              errorSummarySectionOpt = Some(ErrorSummarySection(
                title = "There is a problem", body = "Enter an amount in pounds for the amount of Special Withholding Tax deducted",link = "#amount-2")
              ),
               errorAboveElementCheckSectionOpt = Some( ErrorAboveElementCheckSection(
                 title = "Error: Enter an amount in pounds for the amount of Special Withholding Tax deducted", idOpt = Some("amount-2")))
            )
            val expContentsAgent = expContentsIndividual.copy(
              title = "Error: Did your client have Special Withholding Tax (SWT) deducted from their pension?",
              header = "Did your client have Special Withholding Tax (SWT) deducted from their pension?",
            )
            
            scenarioNameForIndividualAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
              implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoAmountPage(Some(true), Some("")))

              assertPageAsExpected(BAD_REQUEST, expContentsIndividual)
              getViewModel mustBe Some(expectedViewModel)
            }
            
            scenarioNameForIndividualAndWelsh in {

              implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoAmountPage(Some(true), None))

              assertPageAsExpected(BAD_REQUEST, expContentsIndividual.copy(errorSummarySectionOpt = welshTitle(expContentsIndividual)))
              getViewModel mustBe Some(expectedViewModel)
            }
            
            scenarioNameForAgentAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
              implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoAmountPage(Some(true), None))

              assertPageAsExpected(BAD_REQUEST, expContentsAgent)
              getViewModel mustBe Some(expectedViewModel)
            }
            
            scenarioNameForAgentAndWelsh in {

              implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoAmountPage(Some(true), None))

              assertPageAsExpected(BAD_REQUEST, expContentsAgent.copy(errorSummarySectionOpt = welshTitle(expContentsAgent)))
              getViewModel mustBe Some(expectedViewModel)
            }
            
          }
          "the user has selected 'Yes' but has provided an amount of an invalid format, and" when {

            val expContentsIndividual = expectedContentsIndividual.copy(
              radioButtonForYes = checkedExpectedRadioButton("Yes"),
              amountSection = ExpectedAmountSection("Amount of SWT, in pounds", "x2.64", Some("For example, £193.54")),
              errorSummarySectionOpt = Some(ErrorSummarySection(
                title = "There is a problem", body = "Enter the amount of Special Withholding Tax deducted in the correct format",link = "#amount-2")
              ),
              errorAboveElementCheckSectionOpt = Some( ErrorAboveElementCheckSection(
                title = "Error: Enter the amount of Special Withholding Tax deducted in the correct format", idOpt = Some("amount-2")))
            )
            val expContentsAgent = expContentsIndividual.copy(
              title = "Error: Did your client have Special Withholding Tax (SWT) deducted from their pension?",
              header = "Did your client have Special Withholding Tax (SWT) deducted from their pension?",
            )
            
            scenarioNameForIndividualAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
              implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoAmountPage(Some(true), Some("x2.64")))

              assertPageAsExpected(BAD_REQUEST, expContentsIndividual)
              getViewModel mustBe Some(expectedViewModel)
            }
            
            scenarioNameForIndividualAndWelsh in {

              implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoAmountPage(Some(true), Some("x2.64")))

              assertPageAsExpected(BAD_REQUEST, expContentsIndividual.copy(errorSummarySectionOpt = welshTitle(expContentsIndividual)))
              getViewModel mustBe Some(expectedViewModel)
            }
            
            scenarioNameForAgentAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
              implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoAmountPage(Some(true), Some("x2.64")))

              assertPageAsExpected(BAD_REQUEST, expContentsAgent)
              getViewModel mustBe Some(expectedViewModel)

            }
            scenarioNameForAgentAndWelsh in {

              implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoAmountPage(Some(true), Some("x2.64")))

              assertPageAsExpected(BAD_REQUEST, expContentsAgent.copy(errorSummarySectionOpt = welshTitle(expContentsAgent)))
              getViewModel mustBe Some(expectedViewModel)

            }
          }
          "the user has selected 'Yes' but has provided an amount of a negative format, and" when {

            val expContentsIndividual = expectedContentsIndividual.copy(
              radioButtonForYes = checkedExpectedRadioButton("Yes"),
              amountSection = ExpectedAmountSection("Amount of SWT, in pounds", "-42.64", Some("For example, £193.54")),
              errorSummarySectionOpt = Some(ErrorSummarySection(
                title = "There is a problem", body = "Enter the amount of Special Withholding Tax deducted in the correct format",link = "#amount-2")
              ),
              errorAboveElementCheckSectionOpt = Some( ErrorAboveElementCheckSection(
                title = "Error: Enter the amount of Special Withholding Tax deducted in the correct format", idOpt = Some("amount-2")))
            )
            val expContentsAgent = expContentsIndividual.copy(
              title = "Error: Did your client have Special Withholding Tax (SWT) deducted from their pension?",
              header = "Did your client have Special Withholding Tax (SWT) deducted from their pension?",
            )
            
            scenarioNameForIndividualAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
              implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoAmountPage(Some(true), Some("-42.64")))

              assertPageAsExpected(BAD_REQUEST, expContentsIndividual)
              getViewModel mustBe Some(expectedViewModel)
            }
            
            scenarioNameForIndividualAndWelsh in {

              implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoAmountPage(Some(true), Some("-42.64")))

              assertPageAsExpected(BAD_REQUEST, expContentsIndividual.copy(errorSummarySectionOpt = welshTitle(expContentsIndividual)))
              getViewModel mustBe Some(expectedViewModel)

            }
            scenarioNameForAgentAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
              implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoAmountPage(Some(true), Some("-42.64")))

              assertPageAsExpected(BAD_REQUEST, expContentsAgent)
              getViewModel mustBe Some(expectedViewModel)
            }
            
            scenarioNameForAgentAndWelsh in {

              implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoAmountPage(Some(true), Some("-42.64")))

              assertPageAsExpected(BAD_REQUEST, expContentsAgent.copy(errorSummarySectionOpt = welshTitle(expContentsAgent)))
              getViewModel mustBe Some(expectedViewModel)
            }
            
          }
          "the user has selected 'Yes' but has provided an excessive amount, and" when {

            val expContentsIndividual = expectedContentsIndividual.copy(
              radioButtonForYes = checkedExpectedRadioButton("Yes"),
              amountSection = ExpectedAmountSection("Amount of SWT, in pounds", "100000000002", Some("For example, £193.54")),
              errorSummarySectionOpt = Some(ErrorSummarySection(
                title = "There is a problem", body = "Amount of Special Withholding Tax deducted must be less than £100,000,000,000",link = "#amount-2")
              ),
              errorAboveElementCheckSectionOpt = Some( ErrorAboveElementCheckSection(
                title = "Error: Amount of Special Withholding Tax deducted must be less than £100,000,000,000", idOpt = Some("amount-2")))
            )
            val expContentsAgent = expContentsIndividual.copy(
              title = "Error: Did your client have Special Withholding Tax (SWT) deducted from their pension?",
              header = "Did your client have Special Withholding Tax (SWT) deducted from their pension?",
            )
            
            scenarioNameForIndividualAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
              implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoAmountPage(Some(true), Some("100000000002")))

              assertPageAsExpected(BAD_REQUEST, expContentsIndividual)
              getViewModel mustBe Some(expectedViewModel)
            }
            
            scenarioNameForIndividualAndWelsh in {

              implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoAmountPage(Some(true), Some("100000000002")))

              assertPageAsExpected(BAD_REQUEST, expContentsIndividual.copy(errorSummarySectionOpt = welshTitle(expContentsIndividual)))
              getViewModel mustBe Some(expectedViewModel)
            }
            
            scenarioNameForAgentAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
              implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoAmountPage(Some(true), Some("100000000002")))

              assertPageAsExpected(BAD_REQUEST, expContentsAgent)
              getViewModel mustBe Some(expectedViewModel)
            }
            
            scenarioNameForAgentAndWelsh in {

              implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoAmountPage(Some(true), Some("100000000002")))

              assertPageAsExpected(BAD_REQUEST, expContentsAgent.copy(errorSummarySectionOpt = welshTitle(expContentsAgent)))
              getViewModel mustBe Some(expectedViewModel)
            }
          }

          "the user has selected 'Yes' but has provided a zero for amount, and" when {

            val expContentsIndividual = expectedContentsIndividual.copy(
              radioButtonForYes = checkedExpectedRadioButton("Yes"),
              amountSection = ExpectedAmountSection("Amount of SWT, in pounds", "0", Some("For example, £193.54")),
              errorSummarySectionOpt = Some(ErrorSummarySection(
                title = "There is a problem", body = "Enter an amount greater than zero",link = "#amount-2")
              ),
              errorAboveElementCheckSectionOpt = Some( ErrorAboveElementCheckSection(
                title = "Error: Enter an amount greater than zero", idOpt = Some("amount-2")))
            )
            val expContentsAgent = expContentsIndividual.copy(
              title = "Error: Did your client have Special Withholding Tax (SWT) deducted from their pension?",
              header = "Did your client have Special Withholding Tax (SWT) deducted from their pension?",
            )
            
            scenarioNameForIndividualAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
              implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoAmountPage(Some(true), Some("0")))

              assertPageAsExpected(BAD_REQUEST, expContentsIndividual)
              getViewModel mustBe Some(expectedViewModel)
            }
            
            scenarioNameForIndividualAndWelsh in {

              implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoAmountPage(Some(true), Some("0")))

              assertPageAsExpected(BAD_REQUEST, expContentsIndividual.copy(errorSummarySectionOpt = welshTitle(expContentsIndividual)))
              getViewModel mustBe Some(expectedViewModel)
            }
            
            scenarioNameForAgentAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
              implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoAmountPage(Some(true), Some("0")))

              assertPageAsExpected(BAD_REQUEST, expContentsAgent)
              getViewModel mustBe Some(expectedViewModel)
            }
            
            scenarioNameForAgentAndWelsh in {

              implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoAmountPage(Some(true), Some("0")))

              assertPageAsExpected(BAD_REQUEST, expContentsAgent.copy(errorSummarySectionOpt = welshTitle(expContentsAgent)))
              getViewModel mustBe Some(expectedViewModel)
            }
          }
        }
      }
    }
  }

  private def getViewModel(implicit userConfig: UserConfig, index : Int): Option[PensionScheme] =
    loadPensionUserData.map(_.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes(index))

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

