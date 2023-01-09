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

package controllers.pensions.incomeFromOverseasPension

import builders.IncomeFromOverseasPensionsViewModelBuilder.{anIncomeFromOverseasPensionsEmptyViewModel, anIncomeFromOverseasPensionsViewModel}
import builders.PensionsCYAModelBuilder.{aPensionsCYAEmptyModel, aPensionsCYAModel, paymentsIntoPensionOnlyCYAModel}
import controllers.ControllerSpec.PreferredLanguages.{English, Welsh}
import controllers.ControllerSpec.UserTypes.{Agent, Individual}
import controllers.ControllerSpec.{SubmittedFormData, _}
import controllers.YesNoControllerSpec
import models.mongo.PensionsCYAModel
import models.pension.charges.{IncomeFromOverseasPensionsViewModel, PensionScheme}
import models.pension.reliefs.PaymentsIntoPensionViewModel
import org.jsoup.Jsoup.parse
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.{WSClient, WSResponse}

class ForeignTaxCreditReliefControllerISpec extends YesNoControllerSpec("/overseas-pensions/income-from-overseas-pensions/pension-overseas-income-ftcr") {
  "This page" when {
    "show" should {
      "redirect to the summary page" when {
        "the user has no stored session data at all" in {
          implicit val userConfig: UserConfig = userConfigWhenIrrelevant(None)
          implicit val response: WSResponse = getPageWithIndex()

          assertRedirectionAsExpected(PageRelativeURLs.summaryPage)
        }
        "the user has no relevant session data and" in {
          val sessionData = pensionsUserData(aPensionsCYAEmptyModel)

          implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
          implicit val response: WSResponse = getPageWithIndex()
          assertRedirectionAsExpected(PageRelativeURLs.summaryPage)
        }
        "incorrect index provided" in {
          val sessionData = pensionsUserData(aPensionsCYAModel)

          implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
          implicit val response: WSResponse = getPageWithIndex(7)
          assertRedirectionAsExpected(PageRelativeURLs.summaryPage)
        }
        "the user provides a negative index number" in {
          val sessionData = pensionsUserData(aPensionsCYAModel)
          implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
          implicit val response: WSResponse = getPageWithIndex(-1)

          assertRedirectionAsExpected(PageRelativeURLs.summaryPage)
        }
      }

      "appear as expected" should {
        "the user has relevant session data with neither option selected" when {
          val incomeViewModel = anIncomeFromOverseasPensionsViewModel.copy(overseasIncomePensionSchemes = Seq(
            anIncomeFromOverseasPensionsViewModel.overseasIncomePensionSchemes.head
              .copy(foreignTaxCreditReliefQuestion = None)))

          val updatedUserData = aPensionsCYAModel.copy(incomeFromOverseasPensions = incomeViewModel)

          val sessionData = pensionsUserData(updatedUserData)

          scenarioNameForIndividualAndEnglish in {
            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            implicit val response: WSResponse = getPageWithIndex()

            assertPageAsExpected(
              OK,
              ExpectedYesNoPageContents(
                title = "Are you claiming Foreign Tax Credit Relief (FTCR)?",
                header = "Are you claiming Foreign Tax Credit Relief (FTCR)?",
                caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                links = Set.empty,
                text = Set.empty,
                formUrl = formUrl()
              ))
          }

          scenarioNameForIndividualAndWelsh in {
            implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
            implicit val response: WSResponse = getPageWithIndex()
            assertPageAsExpected(
              OK,
              ExpectedYesNoPageContents(
                title = "Are you claiming Foreign Tax Credit Relief (FTCR)?",
                header = "Are you claiming Foreign Tax Credit Relief (FTCR)?",
                caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                links = Set.empty,
                text = Set.empty,
                formUrl = formUrl()
              ))
          }

          scenarioNameForAgentAndEnglish in {
            implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
            implicit val response: WSResponse = getPageWithIndex()

            assertPageAsExpected(
              OK,
              ExpectedYesNoPageContents(
                title = "Is your client claiming Foreign Tax Credit Relief (FTCR)?",
                header = "Is your client claiming Foreign Tax Credit Relief (FTCR)?",
                caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                links = Set.empty,
                text = Set.empty,
                formUrl = formUrl()
              ))
          }

          scenarioNameForAgentAndWelsh in {
            implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
            implicit val response: WSResponse = getPageWithIndex()
            assertPageAsExpected(
              OK,
              ExpectedYesNoPageContents(
                title = "Is your client claiming Foreign Tax Credit Relief (FTCR)?",
                header = "Is your client claiming Foreign Tax Credit Relief (FTCR)?",
                caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                links = Set.empty,
                text = Set.empty,
                formUrl = formUrl()
              ))
          }
        }
        "the user has relevant session data with 'yes' option selected" when {
          val incomeViewModel = anIncomeFromOverseasPensionsViewModel.copy(overseasIncomePensionSchemes = Seq(
            anIncomeFromOverseasPensionsViewModel.overseasIncomePensionSchemes.head
              .copy(foreignTaxCreditReliefQuestion = Some(true))))

          val updatedUserData = aPensionsCYAModel.copy(incomeFromOverseasPensions = incomeViewModel)

          val sessionData = pensionsUserData(updatedUserData)

          scenarioNameForIndividualAndEnglish in {
            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            implicit val response: WSResponse = getPageWithIndex()

            assertPageAsExpected(
              OK,
              ExpectedYesNoPageContents(
                title = "Are you claiming Foreign Tax Credit Relief (FTCR)?",
                header = "Are you claiming Foreign Tax Credit Relief (FTCR)?",
                caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = checkedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                links = Set.empty,
                text = Set.empty,
                formUrl = formUrl()
              ))
          }

          scenarioNameForIndividualAndWelsh in {
            implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
            implicit val response: WSResponse = getPageWithIndex()
            assertPageAsExpected(
              OK,
              ExpectedYesNoPageContents(
                title = "Are you claiming Foreign Tax Credit Relief (FTCR)?",
                header = "Are you claiming Foreign Tax Credit Relief (FTCR)?",
                caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = checkedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                links = Set.empty,
                text = Set.empty,
                formUrl = formUrl()
              ))
          }

          scenarioNameForAgentAndEnglish in {
            implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
            implicit val response: WSResponse = getPageWithIndex()

            assertPageAsExpected(
              OK,
              ExpectedYesNoPageContents(
                title = "Is your client claiming Foreign Tax Credit Relief (FTCR)?",
                header = "Is your client claiming Foreign Tax Credit Relief (FTCR)?",
                caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = checkedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                links = Set.empty,
                text = Set.empty,
                formUrl = formUrl()
              ))
          }

          scenarioNameForAgentAndWelsh in {
            implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
            implicit val response: WSResponse = getPageWithIndex()
            assertPageAsExpected(
              OK,
              ExpectedYesNoPageContents(
                title = "Is your client claiming Foreign Tax Credit Relief (FTCR)?",
                header = "Is your client claiming Foreign Tax Credit Relief (FTCR)?",
                caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = checkedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                links = Set.empty,
                text = Set.empty,
                formUrl = formUrl()
              ))
          }
        }

        "the user has relevant session data with 'no' option selected" when {
          val incomeViewModel = anIncomeFromOverseasPensionsViewModel.copy(overseasIncomePensionSchemes = Seq(
            anIncomeFromOverseasPensionsViewModel.overseasIncomePensionSchemes.head
              .copy(foreignTaxCreditReliefQuestion = Some(false))))

          val updatedUserData = aPensionsCYAModel.copy(incomeFromOverseasPensions = incomeViewModel)

          val sessionData = pensionsUserData(updatedUserData)

          scenarioNameForIndividualAndEnglish in {
            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            implicit val response: WSResponse = getPageWithIndex()

            assertPageAsExpected(
              OK,
              ExpectedYesNoPageContents(
                title = "Are you claiming Foreign Tax Credit Relief (FTCR)?",
                header = "Are you claiming Foreign Tax Credit Relief (FTCR)?",
                caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = checkedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                links = Set.empty,
                text = Set.empty,
                formUrl = formUrl()
              ))
          }

          scenarioNameForIndividualAndWelsh in {
            implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
            implicit val response: WSResponse = getPageWithIndex()
            assertPageAsExpected(
              OK,
              ExpectedYesNoPageContents(
                title = "Are you claiming Foreign Tax Credit Relief (FTCR)?",
                header = "Are you claiming Foreign Tax Credit Relief (FTCR)?",
                caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = checkedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                links = Set.empty,
                text = Set.empty,
                formUrl = formUrl()
              ))
          }

          scenarioNameForAgentAndEnglish in {
            implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
            implicit val response: WSResponse = getPageWithIndex()

            assertPageAsExpected(
              OK,
              ExpectedYesNoPageContents(
                title = "Is your client claiming Foreign Tax Credit Relief (FTCR)?",
                header = "Is your client claiming Foreign Tax Credit Relief (FTCR)?",
                caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = checkedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                links = Set.empty,
                text = Set.empty,
                formUrl = formUrl()
              ))
          }

          scenarioNameForAgentAndWelsh in {
            implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
            implicit val response: WSResponse = getPageWithIndex()
            assertPageAsExpected(
              OK,
              ExpectedYesNoPageContents(
                title = "Is your client claiming Foreign Tax Credit Relief (FTCR)?",
                header = "Is your client claiming Foreign Tax Credit Relief (FTCR)?",
                caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = checkedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                links = Set.empty,
                text = Set.empty,
                formUrl = formUrl()
              ))
          }
        }
      }
    }
    "submit" should {
      "redirect to the expected page" when {
        "the user has no stored session data at all" in {
          implicit val userConfig: UserConfig = userConfigWhenIrrelevant(None)
          implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoPage(None))
          assertRedirectionAsExpected(PageRelativeURLs.summaryPage)
          getViewModel mustBe None
        }
      }
      "succeed" when {
        "the user has relevant session data and" when {
          val sessionData = getSessionData
          "selected 'Yes'" in {
            implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
            implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoPage(Some(true)))

            val incomeViewModel = anIncomeFromOverseasPensionsViewModel.copy(overseasIncomePensionSchemes = Seq(
              anIncomeFromOverseasPensionsViewModel.overseasIncomePensionSchemes.head
                .copy(foreignTaxCreditReliefQuestion = Some(true))))

            assertRedirectionAsExpected(relativeUrlForThisPage + "?index=0")
            getViewModel mustBe Some(incomeViewModel)
          }
        }
        "the user has relevant session data and" when {
          val sessionData = getSessionData
          "selected 'No'" in {
            implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
            implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoPage(Some(false)))

            val incomeViewModel = anIncomeFromOverseasPensionsViewModel.copy(overseasIncomePensionSchemes = Seq(
              anIncomeFromOverseasPensionsViewModel.overseasIncomePensionSchemes.head
                .copy(foreignTaxCreditReliefQuestion = Some(false))))

            assertRedirectionAsExpected(relativeUrlForThisPage + "?index=0")
            getViewModel mustBe Some(incomeViewModel)
          }
        }
      }

      "fail" when {
        "the user has selected neither 'Yes' nor 'No' and" when {
          val sessionData = getSessionData

          scenarioNameForIndividualAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoPage(None))

            assertPageAsExpected(
              BAD_REQUEST,
              ExpectedYesNoPageContents(
                title = "Error: Are you claiming Foreign Tax Credit Relief (FTCR)?",
                header = "Are you claiming Foreign Tax Credit Relief (FTCR)?",
                caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                buttonForContinue = ExpectedButton("Continue", ""),
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                errorSummarySectionOpt = Some(
                  ErrorSummarySection(
                    title = "There is a problem",
                    body = "Enter yes or no if you are claiming Foreign Tax Credit Relief (FTCR)",
                    link = "#value")

                ),
                errorAboveElementCheckSectionOpt = Some(
                  ErrorAboveElementCheckSection(
                    title = "Error: Enter yes or no if you are claiming Foreign Tax Credit Relief (FTCR)",
                    idOpt = Some("value")
                  )
                ),
                formUrl = formUrl()
              )
            )
          }
          scenarioNameForIndividualAndWelsh in {

            implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
            implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoPage(None))

            assertPageAsExpected(
              BAD_REQUEST,
              ExpectedYesNoPageContents(
                title = "Error: Are you claiming Foreign Tax Credit Relief (FTCR)?",
                header = "Are you claiming Foreign Tax Credit Relief (FTCR)?",
                caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                buttonForContinue = ExpectedButton("Continue", ""),
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                errorSummarySectionOpt = Some(
                  ErrorSummarySection(
                    title = "Mae problem wedi codi",
                    body = "Enter yes or no if you are claiming Foreign Tax Credit Relief (FTCR)",
                    link = "#value")

                ),
                errorAboveElementCheckSectionOpt = Some(
                  ErrorAboveElementCheckSection(
                    title = "Error: Enter yes or no if you are claiming Foreign Tax Credit Relief (FTCR)",
                    idOpt = Some("value")
                  )
                ),
                formUrl = formUrl()
              )
            )
          }
          scenarioNameForAgentAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
            val response = submitFormWithIndex(SubmittedFormDataForYesNoPage(None))

            response must haveStatus(BAD_REQUEST)
            assertPageAsExpected(
              parse(response.body),
              ExpectedYesNoPageContents(
                title = "Error: Is your client claiming Foreign Tax Credit Relief (FTCR)?",
                header = "Is your client claiming Foreign Tax Credit Relief (FTCR)?",
                caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                links = Set.empty,
                text = Set.empty,
                errorSummarySectionOpt = Some(
                  ErrorSummarySection(
                    title = "There is a problem",
                    body = "Enter yes or no if your client is claiming Foreign Tax Credit Relief (FTCR)",
                    link = "#value")
                ),
                errorAboveElementCheckSectionOpt = Some(
                  ErrorAboveElementCheckSection(
                    title = "Error: Enter yes or no if your client is claiming Foreign Tax Credit Relief (FTCR)",
                    idOpt = Some("value")
                  )
                ),
              formUrl = formUrl()
              ))
          }
          scenarioNameForAgentAndWelsh in {

            implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
            val response = submitFormWithIndex(SubmittedFormDataForYesNoPage(None))

            response must haveStatus(BAD_REQUEST)
            assertPageAsExpected(
              parse(response.body),
              ExpectedYesNoPageContents(
                title = "Error: Is your client claiming Foreign Tax Credit Relief (FTCR)?",
                header = "Is your client claiming Foreign Tax Credit Relief (FTCR)?",
                caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                links = Set.empty,
                text = Set.empty,
                errorSummarySectionOpt = Some(
                  ErrorSummarySection(
                    title = "Mae problem wedi codi",
                    body = "Enter yes or no if your client is claiming Foreign Tax Credit Relief (FTCR)",
                    link = "#value")
                ),
                errorAboveElementCheckSectionOpt = Some(
                  ErrorAboveElementCheckSection(
                    title = "Error: Enter yes or no if your client is claiming Foreign Tax Credit Relief (FTCR)",
                    idOpt = Some("value")
                  )
                ),
              formUrl = formUrl()
              ))
          }
        }
      }
    }
  }

  private def getSessionData ={
    pensionsUserData(aPensionsCYAModel.copy(incomeFromOverseasPensions = anIncomeFromOverseasPensionsViewModel.copy(
      paymentsFromOverseasPensionsQuestion = Some(true),
      overseasIncomePensionSchemes = Seq(
        PensionScheme(
          countryCode = Some("FRA"),
          pensionPaymentAmount = Some(1999.99),
          pensionPaymentTaxPaid = Some(1999.99),
          specialWithholdingTaxQuestion = Some(true),
          specialWithholdingTaxAmount = Some(1999.99),
          foreignTaxCreditReliefQuestion = None,
          taxableAmount = Some(1999.99)
        )
      )
    )))
  }

  private def getViewModel(implicit userConfig: UserConfig): Option[IncomeFromOverseasPensionsViewModel] =
    loadPensionUserData.map(_.pensions.incomeFromOverseasPensions)


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