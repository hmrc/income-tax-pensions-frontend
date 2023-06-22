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

package controllers.pensions.lifetimeAllowances

import builders.PensionsCYAModelBuilder.{aPensionsCYAEmptyModel, aPensionsCYAModel}
import controllers.ControllerSpec.PreferredLanguages.{English, Welsh}
import controllers.ControllerSpec.UserTypes.{Agent, Individual}
import controllers.ControllerSpec._
import controllers.YesNoAmountControllerSpec
import models.mongo.PensionsUserData
import models.pension.charges.PensionAnnualAllowancesViewModel
import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.libs.ws.WSResponse

class PensionProviderPaidTaxControllerISpec
  extends YesNoAmountControllerSpec("/annual-lifetime-allowances/pension-provider-paid-tax") {

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedAmountText: String
    val expectedEG: String
    val expectedYesText: String
    val expectedNoText: String
    val expectedButtonText: String
    val expectedErrorTitle: String
    val expectedErrorAmountIdOpt: String
    val expectedErrorValueIdOpt: String
  }
  
  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption = (taxYear: Int) => s"Lifetime allowances for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedAmountText = "Amount they paid or agreed to pay, in pounds"
    val expectedEG = "For example, £193.52"
    val expectedYesText = "Yes"
    val expectedNoText = "No"
    val expectedButtonText = "Continue"
    val expectedErrorTitle = "There is a problem"
    val expectedErrorValueIdOpt = "value"
    val expectedErrorAmountIdOpt = "amount-2"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption = (taxYear: Int) => s"Lifetime allowances for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedAmountText = "Y swm a dalwyd ganddo, neu’r swm a gytunodd i’w dalu, mewn punnoedd"
    val expectedEG = "Er enghraifft, £193.52"
    val expectedYesText = "Iawn"
    val expectedNoText = "Na"
    val expectedButtonText = "Yn eich blaen"
    val expectedErrorTitle = "Mae problem wedi codi"
    val expectedErrorValueIdOpt = "value"
    val expectedErrorAmountIdOpt = "amount-2"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    lazy val expectedHeader = expectedTitle
    val expectedErrorSelectText: String
    val amountTaxPaid: String
    val expectedErrorAmountText: String
    val expectedAmountFormatText: String
    val expectedAmountLessThan: String
    val errorFromTitle: String
  }
  
  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Did your pension schemes pay or agree to pay the tax?"
    val expectedErrorSelectText = "Select yes if your pension provider paid or agreed to pay your annual allowance tax"
    val amountTaxPaid = "amount of tax your pension provider paid or agreed to pay"
    val expectedErrorAmountText = s"Enter the $amountTaxPaid"
    val expectedAmountFormatText = s"$expectedErrorAmountText in the correct format"
    val expectedAmountLessThan = s"The $amountTaxPaid must be less than £100,000,000,000"
    val errorFromTitle = s"Error: $expectedTitle"
  }
  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "A wnaeth eich cynlluniau pensiwn dalu’r dreth neu gytuno i dalu’r dreth?"
    val expectedErrorSelectText = "Dewiswch ‘Iawn’ os gwnaeth eich darparwr pensiwn dalu’ch treth lwfans blynyddol neu gytuno i wneud hynny"
    val amountTaxPaid = "swm y dreth a dalwyd gan eich darparwr pensiwn, neu’r swm a gytunodd i’w dalu"
    val expectedErrorAmountText = s"Nodwch $amountTaxPaid"
    val expectedAmountFormatText = s"$expectedErrorAmountText, yn y fformat cywir"
    val expectedAmountLessThan = "Mae’n rhaid i swm y dreth a dalwyd gan eich darparwr pensiwn, " +
      "neu’r swm a gytunodd i’w dalu, fod yn llai na £100,000,000,000"
    val errorFromTitle = s"Gwall: $expectedTitle"
  }
  
  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle =  "Did your client’s pension schemes pay or agree to pay the tax?"
    val expectedErrorSelectText = "Select yes if your client’s pension provider paid or agreed to pay the annual allowance tax"
    val amountTaxPaid = "amount of tax your client’s pension provider paid or agreed to pay"
    val expectedErrorAmountText = s"Enter the $amountTaxPaid"
    val expectedAmountFormatText = s"$expectedErrorAmountText in the correct format"
    val expectedAmountLessThan = s"The $amountTaxPaid must be less than £100,000,000,000"
    val errorFromTitle = s"Error: $expectedTitle"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "A wnaeth cynlluniau pensiwn eich cleient dalu’r dreth neu gytuno i wneud hynny?"
    val expectedErrorSelectText = "Dewiswch ‘Iawn’ os gwnaeth darparwr pensiwn eich cleient dalu’r dreth lwfans blynyddol neu gytuno i wneud hynny"
    val amountTaxPaid = "swm y dreth a dalwyd gan ddarparwr pensiwn eich cleient, neu’r swm a gytunodd i’w dalu"
    val expectedErrorAmountText = s"Nodwch $amountTaxPaid"
    val expectedAmountFormatText = s"$expectedErrorAmountText, yn y fformat cywir"
    val expectedAmountLessThan = "Mae’n rhaid i swm y dreth a dalwyd gan ddarparwr pensiwn eich cleient, " +
      "neu’r swm a gytunodd i’w dalu, fod yn llai na £100,000,000,000"
    val errorFromTitle = s"Gwall: $expectedTitle"
  }

  "This page" when {
      
    "requested to be shown" should {
      
      "redirect to the summary page" when {
        "the user has no stored session data at all" in {

          implicit val userConfig: UserConfig = UserConfig(Individual, English, None)
          implicit val response: WSResponse = getPage
          assertRedirectionAsExpected(PageRelativeURLs.pensionsSummaryPage)
        }
      }
      "appear as expected" when {
        "the user has no pension-related session data and" when {

          val sessionData = pensionsUserData(aPensionsCYAEmptyModel)

          scenarioNameForIndividualAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPPPTPageAsExpected(
              OK, expectedContent(CommonExpectedEN, ExpectedIndividualEN)()
            )
          }
          scenarioNameForIndividualAndWelsh in {
            
            implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPPPTPageAsExpected(
              OK, expectedContent(CommonExpectedCY, ExpectedIndividualCY)(), isWelsh = true
            )
          }
          scenarioNameForAgentAndEnglish in {
            
            implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPPPTPageAsExpected(
              OK, expectedContent(CommonExpectedEN, ExpectedAgentEN)()
            )
          }
          scenarioNameForAgentAndWelsh in {

            implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPPPTPageAsExpected(
              OK, expectedContent(CommonExpectedCY, ExpectedAgentCY)(), isWelsh = true
            )
          }
        }
        
        "the user had previously answered 'Yes' with a valid amount, and" when {

          val sessionData: PensionsUserData =
            pensionsUserData(aPensionsCYAModel.copy(pensionsAnnualAllowances =
              PensionAnnualAllowancesViewModel(
                pensionProvidePaidAnnualAllowanceQuestion = Some(true),
                taxPaidByPensionProvider = Some(1042.64))
            ))

          scenarioNameForIndividualAndEnglish in {
            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPPPTPageAsExpected(
              OK, expectedContent(CommonExpectedEN, ExpectedIndividualEN, "1,042.64")(yesUnchecked = false)
            )
          }
          scenarioNameForIndividualAndWelsh in {
            implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPPPTPageAsExpected(
              OK, expectedContent(CommonExpectedCY, ExpectedIndividualCY, "1,042.64")(yesUnchecked = false), isWelsh = true
            )
          }
          scenarioNameForAgentAndEnglish in {
            implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPPPTPageAsExpected(
             OK, expectedContent(CommonExpectedEN, ExpectedAgentEN, "1,042.64")(yesUnchecked = false)
            )
          }
          scenarioNameForAgentAndWelsh in {
            implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPPPTPageAsExpected(
              OK, expectedContent(CommonExpectedCY, ExpectedAgentCY, "1,042.64")(yesUnchecked = false), isWelsh = true
            )
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
            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPPPTPageAsExpected(
              OK, expectedContent(CommonExpectedEN, ExpectedIndividualEN)( noUnchecked = false)
            )
          }
          scenarioNameForIndividualAndWelsh in {
            implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPPPTPageAsExpected(
              OK, expectedContent(CommonExpectedCY, ExpectedIndividualCY)(noUnchecked = false), isWelsh = true
            )
          }
          scenarioNameForAgentAndEnglish in {
            implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPPPTPageAsExpected(
              OK, expectedContent(CommonExpectedEN, ExpectedAgentEN)(noUnchecked = false)
            )
          }
          scenarioNameForAgentAndWelsh in {
            implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPPPTPageAsExpected(
              OK, expectedContent(CommonExpectedCY, ExpectedAgentCY)(noUnchecked = false), isWelsh = true
            )
          }
          
        }
      }
    }
    "submitted" should {
      "redirect to the expected page" when {
        "the user has no stored session data at all" in {

          implicit val userConfig: UserConfig = userConfigWhenIrrelevant(None)
          implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(false), None))

          assertRedirectionAsExpected(PageRelativeURLs.pensionsSummaryPage)
          getViewModel mustBe None

        }
      }
      "succeed" when {
        "the user has relevant session data and" when {

          val sessionData = pensionsUserData(aPensionsCYAModel)

          "the user has selected 'No' and" in {

            val expectedViewModel =
              sessionData.pensions.pensionsAnnualAllowances.copy(
                pensionProvidePaidAnnualAllowanceQuestion = Some(false),
                taxPaidByPensionProvider = None
              )

            implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
            implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(false), None))

            assertRedirectionAsExpected(PageRelativeURLs.pensionSchemeTaxReferenceSummary)
            getViewModel mustBe Some(expectedViewModel)

          }
          "the user has selected 'Yes' as well as a valid amount (unformatted), and" in {

            val expectedViewModel =
              sessionData.pensions.pensionsAnnualAllowances.copy(
                pensionProvidePaidAnnualAllowanceQuestion = Some(true),
                taxPaidByPensionProvider = Some(BigDecimal(42.64))
              )

            implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
            implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), Some("42.64")))

            assertRedirectionAsExpected(PageRelativeURLs.pensionSchemeTaxReferenceSummary)
            getViewModel mustBe Some(expectedViewModel)

          }
          "the user has selected 'Yes' as well as a valid amount (formatted), and" in {

            val expectedViewModel =
              sessionData.pensions.pensionsAnnualAllowances.copy(
                pensionProvidePaidAnnualAllowanceQuestion = Some(true),
                taxPaidByPensionProvider = Some(BigDecimal(1042.64))
              )

            implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
            implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), Some("£1,042.64")))

            assertRedirectionAsExpected(PageRelativeURLs.pensionSchemeTaxReferenceSummary)
            getViewModel mustBe Some(expectedViewModel)

          }
        }
        
        "the user has no pension-related session data and" when {

          val sessionData = pensionsUserData(aPensionsCYAEmptyModel)

          "the user has selected 'No' and" in {

            val expectedViewModel =
              sessionData.pensions.pensionsAnnualAllowances.copy(
                pensionProvidePaidAnnualAllowanceQuestion = Some(false),
                taxPaidByPensionProvider = None
              )

            implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
            implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(false), None))

            assertRedirectionAsExpected(PageRelativeURLs.pensionSchemeTaxReference)
            getViewModel mustBe Some(expectedViewModel)

          }
          "the user has selected 'Yes' as well as a valid amount (unformatted), and" in {

            val expectedViewModel =
              sessionData.pensions.pensionsAnnualAllowances.copy(
                pensionProvidePaidAnnualAllowanceQuestion = Some(true),
                taxPaidByPensionProvider = Some(BigDecimal(42.64))
              )

            implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
            implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), Some("42.64")))

            assertRedirectionAsExpected(PageRelativeURLs.pensionSchemeTaxReference)
            getViewModel mustBe Some(expectedViewModel)

          }
          "the user has selected 'Yes' as well as a valid amount (formatted), and" in {

            val expectedViewModel =
              sessionData.pensions.pensionsAnnualAllowances.copy(
                pensionProvidePaidAnnualAllowanceQuestion = Some(true),
                taxPaidByPensionProvider = Some(BigDecimal(1042.64))
              )

            implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
            implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), Some("£1,042.64")))

            assertRedirectionAsExpected(PageRelativeURLs.pensionSchemeTaxReference)
            getViewModel mustBe Some(expectedViewModel)
          }
        }
      }
      "fail" when {
        "the user has relevant session data and" when {
          val sessionData = pensionsUserData(aPensionsCYAModel)
          val expectedViewModel = sessionData.pensions.pensionsAnnualAllowances

          "the user has selected neither 'Yes' nor 'No' and" when {
            scenarioNameForIndividualAndEnglish in {
              implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(None, None))

              assertPPPTPageAsExpected(
                BAD_REQUEST, expectedContentError(
                  CommonExpectedEN, ExpectedIndividualEN, ExpectedIndividualEN.expectedErrorSelectText, CommonExpectedEN.expectedErrorValueIdOpt
                )()
              )
              getViewModel mustBe Some(expectedViewModel)
            }
            
            scenarioNameForIndividualAndWelsh in {
              implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(None, None))

              assertPPPTPageAsExpected(
                BAD_REQUEST, expectedContentError(CommonExpectedCY,
                  ExpectedIndividualCY, ExpectedIndividualCY.expectedErrorSelectText, CommonExpectedCY.expectedErrorValueIdOpt)(), isWelsh = true
              )
              getViewModel mustBe Some(expectedViewModel)
            }
            
            scenarioNameForAgentAndEnglish in {
              implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(None, None))

              assertPPPTPageAsExpected(
                BAD_REQUEST, expectedContentError(
                 CommonExpectedEN, ExpectedAgentEN, ExpectedAgentEN.expectedErrorSelectText, CommonExpectedEN.expectedErrorValueIdOpt
                )()
              )
              getViewModel mustBe Some(expectedViewModel)
            }
            
            scenarioNameForAgentAndWelsh in {
              implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(None, None))

              assertPPPTPageAsExpected(
                BAD_REQUEST, expectedContentError(
                  CommonExpectedCY, ExpectedAgentCY, ExpectedAgentCY.expectedErrorSelectText, CommonExpectedCY.expectedErrorValueIdOpt)(), isWelsh = true
              )
              getViewModel mustBe Some(expectedViewModel)
            }
          }
          
          "the user has selected 'Yes' but have not provided an amount, and" when {
            scenarioNameForIndividualAndEnglish in {
              implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), None))

              assertPPPTPageAsExpected(
                BAD_REQUEST, expectedContentError(CommonExpectedEN, ExpectedIndividualEN,
                  ExpectedIndividualEN.expectedErrorAmountText, CommonExpectedEN.expectedErrorAmountIdOpt)(yesUnchecked = false)
              )
              getViewModel mustBe Some(expectedViewModel)
            }
            
            scenarioNameForIndividualAndWelsh in {
              implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), None))

              assertPPPTPageAsExpected(
                BAD_REQUEST, expectedContentError(CommonExpectedCY, ExpectedIndividualCY, ExpectedIndividualCY.expectedErrorAmountText,
                  CommonExpectedCY.expectedErrorAmountIdOpt)(yesUnchecked = false), isWelsh = true
              )
              getViewModel mustBe Some(expectedViewModel)
            }
            
            scenarioNameForAgentAndEnglish in {
              implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), None))

              assertPPPTPageAsExpected(
                BAD_REQUEST, expectedContentError(
                  CommonExpectedEN, ExpectedAgentEN, ExpectedAgentEN.expectedErrorAmountText, CommonExpectedEN.expectedErrorAmountIdOpt)(yesUnchecked = false)
              )
              getViewModel mustBe Some(expectedViewModel)
            }
            
            scenarioNameForAgentAndWelsh in {
              implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), None))

              assertPPPTPageAsExpected(
                BAD_REQUEST, expectedContentError(CommonExpectedCY, ExpectedAgentCY, ExpectedAgentCY.expectedErrorAmountText,
                  CommonExpectedCY.expectedErrorAmountIdOpt)(yesUnchecked = false), isWelsh = true
              )
              getViewModel mustBe Some(expectedViewModel)
            }
          }
          
          "the user has selected 'Yes' but has provided an amount of an invalid format, and" when {
            scenarioNameForIndividualAndEnglish in {
              implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), Some("x2.64")))

              assertPPPTPageAsExpected(
                BAD_REQUEST, expectedContentError(CommonExpectedEN, ExpectedIndividualEN, ExpectedIndividualEN.expectedAmountFormatText,
                  CommonExpectedEN.expectedErrorAmountIdOpt, amount = "x2.64")(yesUnchecked = false)
              )
              getViewModel mustBe Some(expectedViewModel)
            }
            
            scenarioNameForIndividualAndWelsh in {
              implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), Some("x2.64")))

              assertPPPTPageAsExpected(
                BAD_REQUEST, expectedContentError(CommonExpectedCY,  ExpectedIndividualCY, ExpectedIndividualCY.expectedAmountFormatText,
                  CommonExpectedCY.expectedErrorAmountIdOpt, amount = "x2.64")(yesUnchecked = false), isWelsh = true
              )
              getViewModel mustBe Some(expectedViewModel)
            }
            
            scenarioNameForAgentAndEnglish in {
              implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), Some("x2.64")))
              
              assertPPPTPageAsExpected(
                BAD_REQUEST, expectedContentError(CommonExpectedEN,
                  ExpectedAgentEN, ExpectedAgentEN.expectedAmountFormatText, CommonExpectedEN.expectedErrorAmountIdOpt, amount = "x2.64")(yesUnchecked = false)
              )
              getViewModel mustBe Some(expectedViewModel)
            }
            
            scenarioNameForAgentAndWelsh in {
              implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), Some("x2.64")))

              assertPPPTPageAsExpected(
                BAD_REQUEST, expectedContentError(CommonExpectedCY,  ExpectedAgentCY, ExpectedAgentCY.expectedAmountFormatText,
                  CommonExpectedCY.expectedErrorAmountIdOpt, amount = "x2.64")(yesUnchecked = false), isWelsh = true
              )
              getViewModel mustBe Some(expectedViewModel)
            }
          }
          
          "the user has selected 'Yes' but has provided an excessive amount, and" when {
            scenarioNameForIndividualAndEnglish in {
              implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), Some("100000000002")))

              assertPPPTPageAsExpected(
                BAD_REQUEST, expectedContentError(CommonExpectedEN, ExpectedIndividualEN, ExpectedIndividualEN.expectedAmountLessThan,
                  CommonExpectedEN.expectedErrorAmountIdOpt, amount = "100000000002")(yesUnchecked = false)
              )
              getViewModel mustBe Some(expectedViewModel)
            }
            
            scenarioNameForIndividualAndWelsh in {
              implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), Some("100000000002")))

              assertPPPTPageAsExpected(
                BAD_REQUEST, expectedContentError(CommonExpectedCY, ExpectedIndividualCY, ExpectedIndividualCY.expectedAmountLessThan,
                  CommonExpectedCY.expectedErrorAmountIdOpt, amount = "100000000002")(yesUnchecked = false), isWelsh = true
              )
              getViewModel mustBe Some(expectedViewModel)
            }
            
            scenarioNameForAgentAndEnglish in {
              implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), Some("100000000002")))

              assertPPPTPageAsExpected(
                BAD_REQUEST, expectedContentError(CommonExpectedEN, ExpectedAgentEN, ExpectedAgentEN.expectedAmountLessThan,
                  CommonExpectedEN.expectedErrorAmountIdOpt, amount = "100000000002")(yesUnchecked = false)
              )
              getViewModel mustBe Some(expectedViewModel)
            }
            
            scenarioNameForAgentAndWelsh in {
              implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(true), Some("100000000002")))

              assertPPPTPageAsExpected(
                BAD_REQUEST, expectedContentError(CommonExpectedCY, ExpectedAgentCY, ExpectedAgentCY.expectedAmountLessThan,
                  CommonExpectedCY.expectedErrorAmountIdOpt, amount = "100000000002")(yesUnchecked = false), isWelsh = true
              )
              getViewModel mustBe Some(expectedViewModel)
            }
          }
        }
      }
    }
  }

  private def getViewModel(implicit userConfig: UserConfig): Option[PensionAnnualAllowancesViewModel] =
    loadPensionUserData.map(_.pensions.pensionsAnnualAllowances)
  
  private def assertPPPTPageAsExpected(expectedStatusCode: Int, expectedPageContents: ExpectedYesNoAmountPageContents, isWelsh: Boolean = false)
                                      (implicit userConfig: UserConfig, response: WSResponse): Unit = {
   assertPageAsExpected(expectedStatusCode, expectedPageContents)(userConfig, response, isWelsh)
  }
  
  private def expectedContent(cer: CommonExpectedResults, ser: SpecificExpectedResults, amount: String = "")
                             (yesUnchecked: Boolean = true, noUnchecked: Boolean = true)= {
    import cer._
    import ser._
    ExpectedYesNoAmountPageContents(
      title = expectedTitle,
      header = expectedHeader,
      caption = expectedCaption(taxYear),
      radioButtonForYes = if (yesUnchecked) uncheckedExpectedRadioButton(expectedYesText) else checkedExpectedRadioButton(expectedYesText),
      radioButtonForNo = if (noUnchecked) uncheckedExpectedRadioButton(expectedNoText) else checkedExpectedRadioButton(expectedNoText),
      buttonForContinue = ExpectedButton(expectedButtonText, ""),
      amountSection = ExpectedAmountSection(expectedAmountText, amount, Some(expectedEG))
    )
  }
  
 private def expectedContentError(cer: CommonExpectedResults, ser: SpecificExpectedResults,
                                  errorText: String, idOpt: String, amount: String = "")
                                 (yesUnchecked: Boolean = true, noUnchecked: Boolean = true)= {
   import cer._
   
   expectedContent(cer, ser, amount)(yesUnchecked, noUnchecked).copy(
     title = ser.errorFromTitle,
     errorSummarySectionOpt = Some(
       ErrorSummarySection(expectedErrorTitle, body = errorText, link = s"#$idOpt")
     ),
     errorAboveElementCheckSectionOpt = Some(
       ErrorAboveElementCheckSection(s"Error: $errorText", Some(idOpt))  //TODO: why not also  in Welsh
     )
   )
 }
  
 
}



