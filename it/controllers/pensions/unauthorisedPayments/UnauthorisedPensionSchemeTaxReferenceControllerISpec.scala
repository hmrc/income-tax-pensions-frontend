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

import builders.PensionsCYAModelBuilder.aPensionsCYAEmptyModel
import controllers.ControllerSpec
import controllers.ControllerSpec.PreferredLanguages.{English, Welsh}
import controllers.ControllerSpec.UserTypes.{Agent, Individual}
import controllers.ControllerSpec._
import models.mongo.PensionsCYAModel
import models.pension.charges.UnauthorisedPaymentsViewModel
import org.jsoup.Jsoup.parse
import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.libs.ws.WSResponse

class UnauthorisedPensionSchemeTaxReferenceControllerISpec
  extends ControllerSpec("/unauthorised-payments-from-pensions/pension-scheme-tax-reference") {

  private val minimalSessionDataToAccessThisPage: PensionsCYAModel = aPensionsCYAEmptyModel
  private val selectorForFirstParagraph = "#main-content > div > div > p:nth-of-type(1)"
  private val selectorForSecondParagraph = "#main-content > div > div > p:nth-of-type(2)"
  private val selectorForHint = "#taxReferenceId-hint"

  "This page" when {
    "requested to be shown" should {
      "redirect to the expected page" when {
        "the user has no stored session data at all" in {

          implicit val response: WSResponse = getPage(None)

          assertRedirectionAsExpected(PageRelativeURLs.unauthorisedPaymentsCYAPage)
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
              ExpectedPageContents(
                title = "Pension Scheme Tax Reference (PSTR)",
                header = "Pension Scheme Tax Reference (PSTR)",
                caption = s"Unauthorised payments from pensions for 6 April ${taxYear - 1} to 5 April $taxYear",
                buttonForContinue = ExpectedButton("Continue", ""),
                inputField = ExpectedInputField("#taxReferenceId", "taxReferenceId", ""),
                text = Set(
                  ExpectedText(selectorForFirstParagraph, "You can get this information from your pension provider."),
                  ExpectedText(selectorForSecondParagraph,
                    "If you got unauthorised payments from more than one UK pension provider, you can add the references later."),
                  ExpectedText(selectorForHint, "For example, ‘12345678RA’")
                )
              )
            )
          }
          scenarioNameForIndividualAndWelsh ignore {

            implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPageAsExpected(
              OK,
              ExpectedPageContents(
                title = "Pension Scheme Tax Reference (PSTR)",
                header = "Pension Scheme Tax Reference (PSTR)",
                caption = "Taliadau heb awdurdod o bensiynau ar gyfer 6 Ebrill 2021 i 5 Ebrill 2022",
                buttonForContinue = ExpectedButton("Continue", ""),
                inputField = ExpectedInputField("#taxReferenceId", "taxReferenceId", ""),
                text = Set(
                  ExpectedText(selectorForFirstParagraph, "You can get this information from your pension provider."),
                  ExpectedText(selectorForSecondParagraph,
                    "If you got unauthorised payments from more than one UK pension provider, you can add the references later."),
                  ExpectedText(selectorForHint, "For example, ‘12345678RA’")
                )
              )
            )
          }
          scenarioNameForAgentAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPageAsExpected(
              OK,
              ExpectedPageContents(
                title = "Pension Scheme Tax Reference (PSTR)",
                header = "Pension Scheme Tax Reference (PSTR)",
                caption = s"Unauthorised payments from pensions for 6 April ${taxYear - 1} to 5 April $taxYear",
                buttonForContinue = ExpectedButton("Continue", ""),
                inputField = ExpectedInputField("#taxReferenceId", "taxReferenceId", ""),
                text = Set(
                  ExpectedText(selectorForFirstParagraph, "Your client can get this information from their pension provider."),
                  ExpectedText(selectorForSecondParagraph,
                    "If your client got unauthorised payments from more than UK pension provider, you can add the references later."),
                  ExpectedText(selectorForHint, "For example, ‘12345678RA’")
                )
              )
            )
          }
          scenarioNameForAgentAndWelsh ignore {

            implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPageAsExpected(
              OK,
              ExpectedPageContents(
                title = "Pension Scheme Tax Reference (PSTR)",
                header = "Pension Scheme Tax Reference (PSTR)",
                caption = "Taliadau heb awdurdod o bensiynau ar gyfer 6 Ebrill 2021 i 5 Ebrill 2022",
                buttonForContinue = ExpectedButton("Continue", ""),
                inputField = ExpectedInputField("#taxReferenceId", "taxReferenceId", ""),
                text = Set(
                  ExpectedText(selectorForFirstParagraph, "Your client can get this information from their pension provider."),
                  ExpectedText(selectorForSecondParagraph,
                    "If your client got unauthorised payments from more than UK pension provider, you can add the references later."),
                  ExpectedText(selectorForHint, "For example, ‘12345678RA’")
                )
              )
            )
          }
        }
        "the user had previously entered multiple, valid PSTRs" when {

          val sessionData = pensionsUserData(
            minimalSessionDataToAccessThisPage.copy(
              unauthorisedPayments = UnauthorisedPaymentsViewModel(
                pensionSchemeTaxReference = Some(
                  Seq("12345678RA", "22446688RA", "0000000RX"))
              )
            )
          )

          scenarioNameForIndividualAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPageAsExpected(
              OK,
              ExpectedPageContents(
                title = "Pension Scheme Tax Reference (PSTR)",
                header = "Pension Scheme Tax Reference (PSTR)",
                caption = s"Unauthorised payments from pensions for 6 April ${taxYear - 1} to 5 April $taxYear",
                buttonForContinue = ExpectedButton("Continue", ""),
                inputField = ExpectedInputField("#taxReferenceId", "taxReferenceId", ""),
                text = Set(
                  ExpectedText(selectorForFirstParagraph, "You can get this information from your pension provider."),
                  ExpectedText(selectorForSecondParagraph,
                    "If you got unauthorised payments from more than one UK pension provider, you can add the references later."),
                  ExpectedText(selectorForHint, "For example, ‘12345678RA’")
                )
              )
            )
          }
          scenarioNameForIndividualAndWelsh ignore {

            implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPageAsExpected(
              OK,
              ExpectedPageContents(
                title = "Pension Scheme Tax Reference (PSTR)",
                header = "Pension Scheme Tax Reference (PSTR)",
                caption = "Taliadau heb awdurdod o bensiynau ar gyfer 6 Ebrill 2021 i 5 Ebrill 2022",
                buttonForContinue = ExpectedButton("Continue", ""),
                inputField = ExpectedInputField("#taxReferenceId", "taxReferenceId", ""),
                text = Set(
                  ExpectedText(selectorForFirstParagraph, "You can get this information from your pension provider."),
                  ExpectedText(selectorForSecondParagraph,
                    "If you got unauthorised payments from more than one UK pension provider, you can add the references later."),
                  ExpectedText(selectorForHint, "For example, ‘12345678RA’")
                )
              )
            )
          }
          scenarioNameForAgentAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPageAsExpected(
              OK,
              ExpectedPageContents(
                title = "Pension Scheme Tax Reference (PSTR)",
                header = "Pension Scheme Tax Reference (PSTR)",
                caption = s"Unauthorised payments from pensions for 6 April ${taxYear - 1} to 5 April $taxYear",
                buttonForContinue = ExpectedButton("Continue", ""),
                inputField = ExpectedInputField("#taxReferenceId", "taxReferenceId", ""),
                text = Set(
                  ExpectedText(selectorForFirstParagraph, "Your client can get this information from their pension provider."),
                  ExpectedText(selectorForSecondParagraph,
                    "If your client got unauthorised payments from more than UK pension provider, you can add the references later."),
                  ExpectedText(selectorForHint, "For example, ‘12345678RA’")
                )
              )
            )
          }
          scenarioNameForAgentAndWelsh ignore {

            implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
            implicit val response: WSResponse = getPage

            assertPageAsExpected(
              OK,
              ExpectedPageContents(
                title = "Pension Scheme Tax Reference (PSTR)",
                header = "Pension Scheme Tax Reference (PSTR)",
                caption = "Taliadau heb awdurdod o bensiynau ar gyfer 6 Ebrill 2021 i 5 Ebrill 2022",
                buttonForContinue = ExpectedButton("Continue", ""),
                inputField = ExpectedInputField("#taxReferenceId", "taxReferenceId", ""),
                text = Set(
                  ExpectedText(selectorForFirstParagraph, "Your client can get this information from their pension provider."),
                  ExpectedText(selectorForSecondParagraph,
                    "If your client got unauthorised payments from more than UK pension provider, you can add the references later."),
                  ExpectedText(selectorForHint, "For example, ‘12345678RA’")
                )
              )
            )
          }
        }
      }
    }
    "submitted" should {
      "redirect to the expected page" when {
        "the user has no stored session data at all" in {

          implicit val userConfig: UserConfig = userConfigWhenIrrelevant(None)
          implicit val response: WSResponse = submitForm(SubmittedFormDataForPage(Some("12345678RA")))

          assertRedirectionAsExpected(PageRelativeURLs.unauthorisedPaymentsCYAPage)
          getViewModel mustBe None
        }
      }
      "succeed" when {
        "the user hasn't entered any PSTRs, previously and" when {

          val sessionData = pensionsUserData(minimalSessionDataToAccessThisPage)

          "they enter a valid one" in {

            val expectedViewModel =
              sessionData.pensions.unauthorisedPayments.copy(
                pensionSchemeTaxReference = Some(Seq("12345678RT"))
              )

            implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
            implicit val response: WSResponse = submitForm(SubmittedFormDataForPage(Some("12345678RT")))

            assertRedirectionAsExpected(relativeUrl("/unauthorised-payments-from-pensions/uk-pension-scheme-details"))
            getViewModel mustBe Some(expectedViewModel)
          }
        }
        "the user had entered PSTRs, previously and" when {

          val sessionData = pensionsUserData(
            minimalSessionDataToAccessThisPage.copy(
              unauthorisedPayments = UnauthorisedPaymentsViewModel(
                pensionSchemeTaxReference = Some(
                  Seq("12345678RA", "22446688RA", "0000000RX"))
              )
            )
          )


          "they enter a valid one, which they haven't entered before" in {

            val expectedViewModel =
              sessionData.pensions.unauthorisedPayments.copy(
                pensionSchemeTaxReference = Some(Seq("12345678RA", "22446688RA", "0000000RX", "88888888RY"))
              )

            implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
            implicit val response: WSResponse = submitForm(SubmittedFormDataForPage(Some("88888888RY")))

            assertRedirectionAsExpected(relativeUrl("/unauthorised-payments-from-pensions/uk-pension-scheme-details"))
            getViewModel mustBe Some(expectedViewModel)
          }
          "they enter a valid one (but with extra padding), which they haven't entered before" in {

            val expectedViewModel =
              sessionData.pensions.unauthorisedPayments.copy(
                pensionSchemeTaxReference = Some(Seq("12345678RA", "22446688RA", "0000000RX", "88888888RY"))
              )

            implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
            implicit val response: WSResponse = submitForm(SubmittedFormDataForPage(Some("  88888888RY  ")))

            assertRedirectionAsExpected(relativeUrl("/unauthorised-payments-from-pensions/uk-pension-scheme-details"))
            getViewModel mustBe Some(expectedViewModel)
          }
          "they enter a valid one, which they have had previously entered" in {

            val expectedViewModel =
              sessionData.pensions.unauthorisedPayments.copy(
                pensionSchemeTaxReference = Some(Seq("12345678RA", "22446688RA", "0000000RX", "22446688RA"))
              )

            implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
            implicit val response: WSResponse = submitForm(SubmittedFormDataForPage(Some("22446688RA")))

            assertRedirectionAsExpected(relativeUrl("/unauthorised-payments-from-pensions/uk-pension-scheme-details"))
            getViewModel mustBe Some(expectedViewModel)
          }
        }
      }
      "fail" when {

        "the user hasn't entered any PSTRs, previously and" when {

          val sessionData = pensionsUserData(minimalSessionDataToAccessThisPage)
          val expectedViewModel = sessionData.pensions.unauthorisedPayments

          "the user has entered an entirely empty PSTR" when {

            scenarioNameForIndividualAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForPage(Some("")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedPageContents(
                  title = "Error: Pension Scheme Tax Reference (PSTR)",
                  header = "Pension Scheme Tax Reference (PSTR)",
                  caption = s"Unauthorised payments from pensions for 6 April ${taxYear - 1} to 5 April $taxYear",
                  buttonForContinue = ExpectedButton("Continue", ""),
                  inputField = ExpectedInputField("#taxReferenceId", "taxReferenceId", ""),
                  text = Set(
                    ExpectedText(selectorForFirstParagraph, "You can get this information from your pension provider."),
                    ExpectedText(selectorForSecondParagraph,
                      "If you got unauthorised payments from more than one UK pension provider, you can add the references later."),
                    ExpectedText(selectorForHint, "For example, ‘12345678RA’")
                  ),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "Enter your Pension Scheme Tax Reference",
                      link = "#taxReferenceId")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter your Pension Scheme Tax Reference",
                      idOpt = Some("taxReferenceId")
                    )
                  )
                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
            scenarioNameForIndividualAndWelsh ignore {

              implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForPage(Some("")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedPageContents(
                  title = "Error: Pension Scheme Tax Reference (PSTR)",
                  header = "Pension Scheme Tax Reference (PSTR)",
                  caption = "Taliadau heb awdurdod o bensiynau ar gyfer 6 Ebrill 2021 i 5 Ebrill 2022",
                  buttonForContinue = ExpectedButton("Continue", ""),
                  inputField = ExpectedInputField("#taxReferenceId", "taxReferenceId", ""),
                  text = Set(
                    ExpectedText(selectorForFirstParagraph, "You can get this information from your pension provider."),
                    ExpectedText(selectorForSecondParagraph,
                      "If you got unauthorised payments from more than one UK pension provider, you can add the references later."),
                    ExpectedText(selectorForHint, "For example, ‘12345678RA’")
                  ),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "Enter your Pension Scheme Tax Reference",
                      link = "#taxReferenceId")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter your Pension Scheme Tax Reference",
                      idOpt = Some("taxReferenceId")
                    )
                  )
                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
            scenarioNameForAgentAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForPage(Some("")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedPageContents(
                  title = "Error: Pension Scheme Tax Reference (PSTR)",
                  header = "Pension Scheme Tax Reference (PSTR)",
                  caption = s"Unauthorised payments from pensions for 6 April ${taxYear - 1} to 5 April $taxYear",
                  buttonForContinue = ExpectedButton("Continue", ""),
                  inputField = ExpectedInputField("#taxReferenceId", "taxReferenceId", ""),
                  text = Set(
                    ExpectedText(selectorForFirstParagraph, "Your client can get this information from their pension provider."),
                    ExpectedText(selectorForSecondParagraph,
                      "If your client got unauthorised payments from more than UK pension provider, you can add the references later."),
                    ExpectedText(selectorForHint, "For example, ‘12345678RA’")
                  ),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "Enter your client’s Pension Scheme Tax Reference",
                      link = "#taxReferenceId")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter your client’s Pension Scheme Tax Reference",
                      idOpt = Some("taxReferenceId")
                    )
                  )
                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
            scenarioNameForAgentAndWelsh ignore {

              implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForPage(Some("")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedPageContents(
                  title = "Error: Pension Scheme Tax Reference (PSTR)",
                  header = "Pension Scheme Tax Reference (PSTR)",
                  caption = "Taliadau heb awdurdod o bensiynau ar gyfer 6 Ebrill 2021 i 5 Ebrill 2022",
                  buttonForContinue = ExpectedButton("Continue", ""),
                  inputField = ExpectedInputField("#taxReferenceId", "taxReferenceId", ""),
                  text = Set(
                    ExpectedText(selectorForFirstParagraph, "Your client can get this information from their pension provider."),
                    ExpectedText(selectorForSecondParagraph,
                      "If your client got unauthorised payments from more than UK pension provider, you can add the references later."),
                    ExpectedText(selectorForHint, "For example, ‘12345678RA’")
                  ),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "Enter your client’s Pension Scheme Tax Reference",
                      link = "#taxReferenceId")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter your client’s Pension Scheme Tax Reference",
                      idOpt = Some("taxReferenceId")
                    )
                  )
                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
          }
          "the user has entered a PSTR consisting of only spaces" when {
            scenarioNameForIndividualAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForPage(Some("   ")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedPageContents(
                  title = "Error: Pension Scheme Tax Reference (PSTR)",
                  header = "Pension Scheme Tax Reference (PSTR)",
                  caption = s"Unauthorised payments from pensions for 6 April ${taxYear - 1} to 5 April $taxYear",
                  buttonForContinue = ExpectedButton("Continue", ""),
                  inputField = ExpectedInputField("#taxReferenceId", "taxReferenceId", "   "),
                  text = Set(
                    ExpectedText(selectorForFirstParagraph, "You can get this information from your pension provider."),
                    ExpectedText(selectorForSecondParagraph,
                      "If you got unauthorised payments from more than one UK pension provider, you can add the references later."),
                    ExpectedText(selectorForHint, "For example, ‘12345678RA’")
                  ),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "Enter your Pension Scheme Tax Reference",
                      link = "#taxReferenceId")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter your Pension Scheme Tax Reference",
                      idOpt = Some("taxReferenceId")
                    )
                  )
                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
            scenarioNameForIndividualAndWelsh ignore {

              implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForPage(Some("   ")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedPageContents(
                  title = "Error: Pension Scheme Tax Reference (PSTR)",
                  header = "Pension Scheme Tax Reference (PSTR)",
                  caption = "Taliadau heb awdurdod o bensiynau ar gyfer 6 Ebrill 2021 i 5 Ebrill 2022",
                  buttonForContinue = ExpectedButton("Continue", ""),
                  inputField = ExpectedInputField("#taxReferenceId", "taxReferenceId", "   "),
                  text = Set(
                    ExpectedText(selectorForFirstParagraph, "You can get this information from your pension provider."),
                    ExpectedText(selectorForSecondParagraph,
                      "If you got unauthorised payments from more than one UK pension provider, you can add the references later."),
                    ExpectedText(selectorForHint, "For example, ‘12345678RA’")
                  ),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "Enter your Pension Scheme Tax Reference",
                      link = "#taxReferenceId")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter your Pension Scheme Tax Reference",
                      idOpt = Some("taxReferenceId")
                    )
                  )
                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
            scenarioNameForAgentAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForPage(Some("   ")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedPageContents(
                  title = "Error: Pension Scheme Tax Reference (PSTR)",
                  header = "Pension Scheme Tax Reference (PSTR)",
                  caption = s"Unauthorised payments from pensions for 6 April ${taxYear - 1} to 5 April $taxYear",
                  buttonForContinue = ExpectedButton("Continue", ""),
                  inputField = ExpectedInputField("#taxReferenceId", "taxReferenceId", "   "),
                  text = Set(
                    ExpectedText(selectorForFirstParagraph, "Your client can get this information from their pension provider."),
                    ExpectedText(selectorForSecondParagraph,
                      "If your client got unauthorised payments from more than UK pension provider, you can add the references later."),
                    ExpectedText(selectorForHint, "For example, ‘12345678RA’")
                  ),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "Enter your client’s Pension Scheme Tax Reference",
                      link = "#taxReferenceId")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter your client’s Pension Scheme Tax Reference",
                      idOpt = Some("taxReferenceId")
                    )
                  )
                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
            scenarioNameForAgentAndWelsh ignore {

              implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForPage(Some("   ")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedPageContents(
                  title = "Error: Pension Scheme Tax Reference (PSTR)",
                  header = "Pension Scheme Tax Reference (PSTR)",
                  caption = "Taliadau heb awdurdod o bensiynau ar gyfer 6 Ebrill 2021 i 5 Ebrill 2022",
                  buttonForContinue = ExpectedButton("Continue", ""),
                  inputField = ExpectedInputField("#taxReferenceId", "taxReferenceId", "   "),
                  text = Set(
                    ExpectedText(selectorForFirstParagraph, "Your client can get this information from their pension provider."),
                    ExpectedText(selectorForSecondParagraph,
                      "If your client got unauthorised payments from more than UK pension provider, you can add the references later."),
                    ExpectedText(selectorForHint, "For example, ‘12345678RA’")
                  ),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "Enter your client’s Pension Scheme Tax Reference",
                      link = "#taxReferenceId")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter your client’s Pension Scheme Tax Reference",
                      idOpt = Some("taxReferenceId")
                    )
                  )
                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
          }
          "the user has entered a PSTR with an invalid format" when {

            val expectedViewModel = sessionData.pensions.unauthorisedPayments

            scenarioNameForIndividualAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForPage(Some("123456RA")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedPageContents(
                  title = "Error: Pension Scheme Tax Reference (PSTR)",
                  header = "Pension Scheme Tax Reference (PSTR)",
                  caption = s"Unauthorised payments from pensions for 6 April ${taxYear - 1} to 5 April $taxYear",
                  buttonForContinue = ExpectedButton("Continue", ""),
                  inputField = ExpectedInputField("#taxReferenceId", "taxReferenceId", "123456RA"),
                  text = Set(
                    ExpectedText(selectorForFirstParagraph, "You can get this information from your pension provider."),
                    ExpectedText(selectorForSecondParagraph,
                      "If you got unauthorised payments from more than one UK pension provider, you can add the references later."),
                    ExpectedText(selectorForHint, "For example, ‘12345678RA’")
                  ),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "Enter your Pension Scheme Tax Reference in the correct format",
                      link = "#taxReferenceId")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter your Pension Scheme Tax Reference in the correct format",
                      idOpt = Some("taxReferenceId")
                    )
                  )
                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
            scenarioNameForIndividualAndWelsh ignore {

              implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForPage(Some("123456RA")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedPageContents(
                  title = "Error: Pension Scheme Tax Reference (PSTR)",
                  header = "Pension Scheme Tax Reference (PSTR)",
                  caption = "Taliadau heb awdurdod o bensiynau ar gyfer 6 Ebrill 2021 i 5 Ebrill 2022",
                  buttonForContinue = ExpectedButton("Continue", ""),
                  inputField = ExpectedInputField("#taxReferenceId", "taxReferenceId", "123456RA"),
                  text = Set(
                    ExpectedText(selectorForFirstParagraph, "You can get this information from your pension provider."),
                    ExpectedText(selectorForSecondParagraph,
                      "If you got unauthorised payments from more than one UK pension provider, you can add the references later."),
                    ExpectedText(selectorForHint, "For example, ‘12345678RA’")
                  ),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "Enter your Pension Scheme Tax Reference in the correct format",
                      link = "#taxReferenceId")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter your Pension Scheme Tax Reference in the correct format",
                      idOpt = Some("taxReferenceId")
                    )
                  )
                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
            scenarioNameForAgentAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForPage(Some("123456RA")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedPageContents(
                  title = "Error: Pension Scheme Tax Reference (PSTR)",
                  header = "Pension Scheme Tax Reference (PSTR)",
                  caption = s"Unauthorised payments from pensions for 6 April ${taxYear - 1} to 5 April $taxYear",
                  buttonForContinue = ExpectedButton("Continue", ""),
                  inputField = ExpectedInputField("#taxReferenceId", "taxReferenceId", "123456RA"),
                  text = Set(
                    ExpectedText(selectorForFirstParagraph, "Your client can get this information from their pension provider."),
                    ExpectedText(selectorForSecondParagraph,
                      "If your client got unauthorised payments from more than UK pension provider, you can add the references later."),
                    ExpectedText(selectorForHint, "For example, ‘12345678RA’")
                  ),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "Enter your client’s Pension Scheme Tax Reference in the correct format",
                      link = "#taxReferenceId")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter your client’s Pension Scheme Tax Reference in the correct format",
                      idOpt = Some("taxReferenceId")
                    )
                  )
                )
              )
              getViewModel mustBe Some(expectedViewModel)
            }
            scenarioNameForAgentAndWelsh ignore {

              implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
              implicit val response: WSResponse = submitForm(SubmittedFormDataForPage(Some("123456RA")))

              assertPageAsExpected(
                BAD_REQUEST,
                ExpectedPageContents(
                  title = "Error: Pension Scheme Tax Reference (PSTR)",
                  header = "Pension Scheme Tax Reference (PSTR)",
                  caption = "Taliadau heb awdurdod o bensiynau ar gyfer 6 Ebrill 2021 i 5 Ebrill 2022",
                  buttonForContinue = ExpectedButton("Continue", ""),
                  inputField = ExpectedInputField("#taxReferenceId", "taxReferenceId", "123456RA"),
                  text = Set(
                    ExpectedText(selectorForFirstParagraph, "Your client can get this information from their pension provider."),
                    ExpectedText(selectorForSecondParagraph,
                      "If your client got unauthorised payments from more than UK pension provider, you can add the references later."),
                    ExpectedText(selectorForHint, "For example, ‘12345678RA’")
                  ),
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "Enter your client’s Pension Scheme Tax Reference in the correct format",
                      link = "#taxReferenceId")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Enter your client’s Pension Scheme Tax Reference in the correct format",
                      idOpt = Some("taxReferenceId")
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

  private def assertPageAsExpected(expectedStatusCode: Int, expectedPageContents: ExpectedPageContents)
                                  (implicit userConfig: UserConfig, response: WSResponse): Unit = {
    response must haveStatus(expectedStatusCode)
    val document = parse(response.body)
    super.assertPageAsExpected(document, expectedPageContents)
    assertContinueButtonAsExpected(document, expectedPageContents.buttonForContinue)
    assertTextInputAsExpected(document, expectedPageContents.inputField)
  }

  private def getViewModel(implicit userConfig: UserConfig): Option[UnauthorisedPaymentsViewModel] =
    loadPensionUserData.map(_.pensions.unauthorisedPayments)
}

case class ExpectedPageContents(
                                 title: String,
                                 header: String,
                                 caption: String,
                                 buttonForContinue: ExpectedButton,
                                 inputField: ExpectedInputField,
                                 errorSummarySectionOpt: Option[ErrorSummarySection] = None,
                                 errorAboveElementCheckSectionOpt: Option[ErrorAboveElementCheckSection] = None,
                                 links: Set[ExpectedLink] = Set.empty,
                                 text: Set[ExpectedText] = Set.empty,
                                 formUrl: Option[String] = None)

  extends BaseExpectedPageContents

case class SubmittedFormDataForPage(pstrOpt: Option[String]) extends SubmittedFormData {

  val asMap: Map[String, String] = pstrOpt.map(pstr => Map("taxReferenceId" -> pstr)).getOrElse(Map.empty)
}




