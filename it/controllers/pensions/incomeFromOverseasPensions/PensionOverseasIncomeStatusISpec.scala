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

package controllers.pensions.incomeFromOverseasPensions

import builders.PensionsCYAModelBuilder.{aPensionsCYAEmptyModel, aPensionsCYAModel, paymentsIntoPensionOnlyCYAModel}
import controllers.ControllerSpec.PreferredLanguages.{English, Welsh}
import controllers.ControllerSpec.UserTypes.{Agent, Individual}
import controllers.ControllerSpec._
import controllers.YesNoControllerSpec
import models.mongo.PensionsCYAModel
import models.pension.charges.IncomeFromOverseasPensionsViewModel
import models.pension.reliefs.PaymentsIntoPensionViewModel
import org.jsoup.Jsoup.parse
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}

class PensionOverseasIncomeStatusISpec
  extends YesNoControllerSpec("/overseas-pensions/income-from-overseas-pensions/pension-overseas-income-status") {

  val minimalSessionDataToAccessThisPage: PensionsCYAModel = aPensionsCYAEmptyModel

  "This page" when {
    "requested to be shown" should {
      "redirect to the expected page" when {
        "the user has no stored session data at all" in {

          val response = getPage(None)

          response must haveStatus(SEE_OTHER)
          response must haveALocationHeaderValue(PageRelativeURLs.summaryPage)

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
                  title = "Did you get payments from an overseas pension scheme?",
                  header = "Did you get payments from an overseas pension scheme?",
                  caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  links = Set.empty,
                  text = Set.empty
                ))

            }
            scenarioNameForIndividualAndWelsh in {

              implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
              val response = getPage

              response must haveStatus(OK)
              assertPageAsExpected(
                parse(response.body),
                ExpectedYesNoPageContents(
                  title = "Did you get payments from an overseas pension scheme?",
                  header = "Did you get payments from an overseas pension scheme?",
                  caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  links = Set.empty,
                  text = Set.empty
                ))

            }
            scenarioNameForAgentAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
              val response = getPage

              response must haveStatus(OK)
              assertPageAsExpected(
                parse(response.body),
                ExpectedYesNoPageContents(
                  title = "Did your client get payments from an overseas pension scheme?",
                  header = "Did your client get payments from an overseas pension scheme?",
                  caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  links = Set.empty,
                  text = Set.empty
                ))

            }
            scenarioNameForAgentAndWelsh in {

              implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
              val response = getPage

              response must haveStatus(OK)
              assertPageAsExpected(
                parse(response.body),
                ExpectedYesNoPageContents(
                  title = "Did your client get payments from an overseas pension scheme?",
                  header = "Did your client get payments from an overseas pension scheme?",
                  caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  links = Set.empty,
                  text = Set.empty
                ))

            }
          }
          "the user had previously answered 'Yes', and" when {

            val sessionData = pensionsUserData(
              minimalSessionDataToAccessThisPage.copy(
                incomeFromOverseasPensionsViewModel = minimalSessionDataToAccessThisPage.incomeFromOverseasPensionsViewModel.copy(
                  paymentsFromOverseasPensionsQuestion = Some(true)
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
                  title = "Did you get payments from an overseas pension scheme?",
                  header = "Did you get payments from an overseas pension scheme?",
                  caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  links = Set.empty,
                  text = Set.empty
                ))

            }
            scenarioNameForIndividualAndWelsh in {

              implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
              val response = getPage

              response must haveStatus(OK)
              assertPageAsExpected(
                parse(response.body),
                ExpectedYesNoPageContents(
                  title = "Did you get payments from an overseas pension scheme?",
                  header = "Did you get payments from an overseas pension scheme?",
                  caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  links = Set.empty,
                  text = Set.empty
                ))

            }
            scenarioNameForAgentAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
              val response = getPage

              response must haveStatus(OK)
              assertPageAsExpected(
                parse(response.body),
                ExpectedYesNoPageContents(
                  title = "Did your client get payments from an overseas pension scheme?",
                  header = "Did your client get payments from an overseas pension scheme?",
                  caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  links = Set.empty,
                  text = Set.empty
                ))

            }
            scenarioNameForAgentAndWelsh in {

              implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
              val response = getPage

              response must haveStatus(OK)
              assertPageAsExpected(
                parse(response.body),
                ExpectedYesNoPageContents(
                  title = "Did your client get payments from an overseas pension scheme?",
                  header = "Did your client get payments from an overseas pension scheme?",
                  caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  links = Set.empty,
                  text = Set.empty
                ))

            }
          }

          "the user had previously answered 'No', and" when {

            val sessionData = pensionsUserData(
              minimalSessionDataToAccessThisPage.copy(
                incomeFromOverseasPensionsViewModel = minimalSessionDataToAccessThisPage.incomeFromOverseasPensionsViewModel.copy(
                  paymentsFromOverseasPensionsQuestion = Some(false)
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
                  title = "Did you get payments from an overseas pension scheme?",
                  header = "Did you get payments from an overseas pension scheme?",
                  caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                  radioButtonForNo = checkedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  links = Set.empty,
                  text = Set.empty
                ))

            }
            scenarioNameForIndividualAndWelsh in {

              implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
              val response = getPage

              response must haveStatus(OK)
              assertPageAsExpected(
                parse(response.body),
                ExpectedYesNoPageContents(
                  title = "Did you get payments from an overseas pension scheme?",
                  header = "Did you get payments from an overseas pension scheme?",
                  caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                  radioButtonForNo = checkedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  links = Set.empty,
                  text = Set.empty
                ))


            }
            scenarioNameForAgentAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
              val response = getPage

              response must haveStatus(OK)
              assertPageAsExpected(
                parse(response.body),
                ExpectedYesNoPageContents(
                  title = "Did your client get payments from an overseas pension scheme?",
                  header = "Did your client get payments from an overseas pension scheme?",
                  caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                  radioButtonForNo = checkedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  links = Set.empty,
                  text = Set.empty
                ))

            }
            scenarioNameForAgentAndWelsh in {

              implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
              val response = getPage

              response must haveStatus(OK)
              assertPageAsExpected(
                parse(response.body),
                ExpectedYesNoPageContents(
                  title = "Did your client get payments from an overseas pension scheme?",
                  header = "Did your client get payments from an overseas pension scheme?",
                  caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                  radioButtonForNo = checkedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  links = Set.empty,
                  text = Set.empty
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
              sessionData.pensions.incomeFromOverseasPensionsViewModel.copy(
                paymentsFromOverseasPensionsQuestion = Some(false),
                pensionSchemes = None
              )

            scenarioNameForIndividualAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
              val response = submitForm(SubmittedFormDataForYesNoPage(Some(false)))

              response must haveStatus(SEE_OTHER)
              response must haveALocationHeaderValue(PageRelativeURLs.incomeFromOverseasPensionsPage)
              getViewModel mustBe Some(expectedViewModel)

            }
            scenarioNameForIndividualAndWelsh in {

              implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
              val response = submitForm(SubmittedFormDataForYesNoPage(Some(false)))

              response must haveStatus(SEE_OTHER)
              response must haveALocationHeaderValue(PageRelativeURLs.incomeFromOverseasPensionsPage)
              getViewModel mustBe Some(expectedViewModel)

            }
            scenarioNameForAgentAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
              val response = submitForm(SubmittedFormDataForYesNoPage(Some(false)))

              response must haveStatus(SEE_OTHER)
              response must haveALocationHeaderValue(PageRelativeURLs.incomeFromOverseasPensionsPage)
              getViewModel mustBe Some(expectedViewModel)

            }
            scenarioNameForAgentAndWelsh in {

              implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
              val response = submitForm(SubmittedFormDataForYesNoPage(Some(false)))

              response must haveStatus(SEE_OTHER)
              response must haveALocationHeaderValue(PageRelativeURLs.incomeFromOverseasPensionsPage)
              getViewModel mustBe Some(expectedViewModel)

            }
          }
          "the user has selected 'Yes'" when {

            val sessionData = pensionsUserData(minimalSessionDataToAccessThisPage)

            val expectedViewModel =
              sessionData.pensions.incomeFromOverseasPensionsViewModel.copy(
                paymentsFromOverseasPensionsQuestion = Some(true)
              )

            scenarioNameForIndividualAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
              val response = submitForm(SubmittedFormDataForYesNoPage(Some(true)))

              response must haveStatus(SEE_OTHER)
              response must haveALocationHeaderValue(PageRelativeURLs.incomeFromOverseasPensionsPage)
              getViewModel mustBe Some(expectedViewModel)

            }
            scenarioNameForIndividualAndWelsh in {

              implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
              val response = submitForm(SubmittedFormDataForYesNoPage(Some(true)))

              response must haveStatus(SEE_OTHER)
              response must haveALocationHeaderValue(PageRelativeURLs.incomeFromOverseasPensionsPage)
              getViewModel mustBe Some(expectedViewModel)

            }
            scenarioNameForAgentAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
              val response = submitForm(SubmittedFormDataForYesNoPage(Some(true)))

              response must haveStatus(SEE_OTHER)
              response must haveALocationHeaderValue(PageRelativeURLs.incomeFromOverseasPensionsPage)
              getViewModel mustBe Some(expectedViewModel)

            }
            scenarioNameForAgentAndWelsh in {

              implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
              val response = submitForm(SubmittedFormDataForYesNoPage(Some(true)))

              response must haveStatus(SEE_OTHER)
              response must haveALocationHeaderValue(PageRelativeURLs.incomeFromOverseasPensionsPage)
              getViewModel mustBe Some(expectedViewModel)

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
                  title = "Error: Did you get payments from an overseas pension scheme?",
                  header = "Did you get payments from an overseas pension scheme?",
                  caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  links = Set.empty,
                  text = Set.empty,
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "Select yes if you had income from an overseas pension scheme",
                      link = "#value")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Select yes if you had income from an overseas pension scheme",
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
                  title = "Error: Did you get payments from an overseas pension scheme?",
                  header = "Did you get payments from an overseas pension scheme?",
                  caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  links = Set.empty,
                  text = Set.empty,
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "Select yes if you had income from an overseas pension scheme",
                      link = "#value")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Select yes if you had income from an overseas pension scheme",
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
                  title = "Error: Did your client get payments from an overseas pension scheme?",
                  header = "Did your client get payments from an overseas pension scheme?",
                  caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  links = Set.empty,
                  text = Set.empty,
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "Select yes if your client had income from an overseas pension scheme",
                      link = "#value")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Select yes if your client had income from an overseas pension scheme",
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
                  title = "Error: Did your client get payments from an overseas pension scheme?",
                  header = "Did your client get payments from an overseas pension scheme?",
                  caption = "Income from overseas pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  links = Set.empty,
                  text = Set.empty,
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "Select yes if your client had income from an overseas pension scheme",
                      link = "#value")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Select yes if your client had income from an overseas pension scheme",
                      idOpt = Some("value")
                    )
                  )
                ))

            }
          }

        }
      }
    }
  }

  private def getViewModel(implicit userConfig: UserConfig): Option[IncomeFromOverseasPensionsViewModel] =
    loadPensionUserData.map(_.pensions.incomeFromOverseasPensionsViewModel)

}







