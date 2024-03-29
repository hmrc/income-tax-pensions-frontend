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

import builders.PensionsCYAModelBuilder.{aPensionsCYAModel, emptyPensionsData}
import controllers.ControllerSpec.PreferredLanguages.{English, Welsh}
import controllers.ControllerSpec.UserTypes.{Agent, Individual}
import controllers.ControllerSpec._
import controllers.YesNoControllerSpec
import models.mongo.PensionsCYAModel
import models.pension.charges.IncomeFromOverseasPensionsViewModel
import org.jsoup.Jsoup.parse
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import utils.PageUrls.pensionSummaryUrl

class PensionOverseasIncomeStatusISpec
    extends YesNoControllerSpec("/overseas-pensions/income-from-overseas-pensions/pension-overseas-income-status") {

  val minimalSessionDataToAccessThisPage: PensionsCYAModel = emptyPensionsData

  val expectedYesNoPageContentsIndividual: ExpectedYesNoPageContents = ExpectedYesNoPageContents(
    title = "Did you get payments from an overseas pension scheme?",
    header = "Did you get payments from an overseas pension scheme?",
    caption = s"Income from overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear",
    radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
    radioButtonForNo = uncheckedExpectedRadioButton("No"),
    buttonForContinue = ExpectedButton("Continue", ""),
    links = Set.empty,
    text = Set.empty
  )
  val expectedYesNoPageContentsAgent: ExpectedYesNoPageContents = expectedYesNoPageContentsIndividual
    .copy(
      title = "Did your client get payments from an overseas pension scheme?",
      header = "Did your client get payments from an overseas pension scheme?")

  "This page" when {
    "requested to be shown" should {

      "appear as expected" when {

        "the user has no stored session data at all" when {

          scenarioNameForIndividualAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Individual, English, None)
            val response                        = getPage

            response must haveStatus(SEE_OTHER)
            response.header("location").value mustBe pensionSummaryUrl(taxYear)
          }
        }

        "the user has only the minimal session data for accessing this page and" when {

          val sessionData = pensionsUserData(minimalSessionDataToAccessThisPage)

          scenarioNameForIndividualAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            val response                        = getPage

            response must haveStatus(OK)
            assertPageAsExpected(parse(response.body), expectedYesNoPageContentsIndividual)
          }
          scenarioNameForIndividualAndWelsh ignore {

            implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
            val response                        = getPage

            response must haveStatus(OK)
            assertPageAsExpected(parse(response.body), expectedYesNoPageContentsIndividual)

          }
          scenarioNameForAgentAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
            val response                        = getPage

            response must haveStatus(OK)
            assertPageAsExpected(parse(response.body), expectedYesNoPageContentsAgent)

          }
          scenarioNameForAgentAndWelsh ignore {

            implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
            val response                        = getPage

            response must haveStatus(OK)
            assertPageAsExpected(parse(response.body), expectedYesNoPageContentsAgent)

          }
        }
        "the user had previously answered 'Yes', and" when {

          val sessionData = pensionsUserData(
            minimalSessionDataToAccessThisPage.copy(
              incomeFromOverseasPensions = minimalSessionDataToAccessThisPage.incomeFromOverseasPensions.copy(
                paymentsFromOverseasPensionsQuestion = Some(true)
              )
            )
          )

          scenarioNameForIndividualAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            val response                        = getPage

            response must haveStatus(OK)
            assertPageAsExpected(parse(response.body), expectedYesNoPageContentsIndividual)

          }
          scenarioNameForIndividualAndWelsh ignore {

            implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
            val response                        = getPage

            response must haveStatus(OK)
            assertPageAsExpected(parse(response.body), expectedYesNoPageContentsIndividual)

          }
          scenarioNameForAgentAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
            val response                        = getPage

            response must haveStatus(OK)
            assertPageAsExpected(parse(response.body), expectedYesNoPageContentsAgent)

          }
          scenarioNameForAgentAndWelsh ignore {

            implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
            val response                        = getPage

            response must haveStatus(OK)
            assertPageAsExpected(parse(response.body), expectedYesNoPageContentsAgent)

          }
        }

        "the user had previously answered 'No', and" when {

          val sessionData = pensionsUserData(
            minimalSessionDataToAccessThisPage.copy(
              incomeFromOverseasPensions = minimalSessionDataToAccessThisPage.incomeFromOverseasPensions.copy(
                paymentsFromOverseasPensionsQuestion = Some(false)
              )
            )
          )

          scenarioNameForIndividualAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            val response                        = getPage

            response must haveStatus(OK)
            assertPageAsExpected(parse(response.body), expectedYesNoPageContentsIndividual)
          }

          scenarioNameForIndividualAndWelsh ignore {

            implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
            val response                        = getPage

            response must haveStatus(OK)
            assertPageAsExpected(parse(response.body), expectedYesNoPageContentsIndividual)
          }

          scenarioNameForAgentAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
            val response                        = getPage

            response must haveStatus(OK)
            assertPageAsExpected(parse(response.body), expectedYesNoPageContentsAgent)
          }

          scenarioNameForAgentAndWelsh ignore {

            implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
            val response                        = getPage

            response must haveStatus(OK)
            assertPageAsExpected(parse(response.body), expectedYesNoPageContentsAgent)
          }
        }
      }

      "submitted" should {
        "succeed" when {
          "the user has selected 'No' and" when {

            val sessionData = pensionsUserData(minimalSessionDataToAccessThisPage)

            val expectedViewModel =
              sessionData.pensions.incomeFromOverseasPensions.copy(
                paymentsFromOverseasPensionsQuestion = Some(false),
                overseasIncomePensionSchemes = Nil
              )

            scenarioNameForIndividualAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
              val response                        = submitForm(SubmittedFormDataForYesNoPage(Some(false)))

              response must haveStatus(SEE_OTHER)
              response must haveALocationHeaderValue(PageRelativeURLs.incomeFromOverseasPensionsCya)
              getViewModel mustBe Some(expectedViewModel)

            }
            scenarioNameForIndividualAndWelsh ignore {

              implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
              val response                        = submitForm(SubmittedFormDataForYesNoPage(Some(false)))

              response must haveStatus(SEE_OTHER)
              response must haveALocationHeaderValue(PageRelativeURLs.incomeFromOverseasPensionsCya)
              getViewModel mustBe Some(expectedViewModel)

            }
            scenarioNameForAgentAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
              val response                        = submitForm(SubmittedFormDataForYesNoPage(Some(false)))

              response must haveStatus(SEE_OTHER)
              response must haveALocationHeaderValue(PageRelativeURLs.incomeFromOverseasPensionsCya)
              getViewModel mustBe Some(expectedViewModel)

            }
            scenarioNameForAgentAndWelsh ignore {
              val sessionData                     = pensionsUserData(aPensionsCYAModel)
              val expectedViewModel               = IncomeFromOverseasPensionsViewModel(paymentsFromOverseasPensionsQuestion = Some(false))
              implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
              val response                        = submitForm(SubmittedFormDataForYesNoPage(Some(false)))

              response must haveStatus(SEE_OTHER)
              response must haveALocationHeaderValue(PageRelativeURLs.incomeFromOverseasPensionsCya)
              getViewModel mustBe Some(expectedViewModel)

            }
          }
          "the user has selected 'Yes'" when {

            val sessionData = pensionsUserData(minimalSessionDataToAccessThisPage)

            val expectedViewModel =
              sessionData.pensions.incomeFromOverseasPensions.copy(
                paymentsFromOverseasPensionsQuestion = Some(true)
              )

            scenarioNameForIndividualAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
              val response                        = submitForm(SubmittedFormDataForYesNoPage(Some(true)))

              response must haveStatus(SEE_OTHER)
              response must haveALocationHeaderValue(PageRelativeURLs.incomeFromOverseasPensionsCountry)
              getViewModel mustBe Some(expectedViewModel)

            }
            scenarioNameForIndividualAndWelsh ignore {

              implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
              val response                        = submitForm(SubmittedFormDataForYesNoPage(Some(true)))

              response must haveStatus(SEE_OTHER)
              response must haveALocationHeaderValue(PageRelativeURLs.incomeFromOverseasPensionsCountry)
              getViewModel mustBe Some(expectedViewModel)

            }
            scenarioNameForAgentAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
              val response                        = submitForm(SubmittedFormDataForYesNoPage(Some(true)))

              response must haveStatus(SEE_OTHER)
              response must haveALocationHeaderValue(PageRelativeURLs.incomeFromOverseasPensionsCountry)
              getViewModel mustBe Some(expectedViewModel)

            }
            scenarioNameForAgentAndWelsh ignore {

              implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
              val response                        = submitForm(SubmittedFormDataForYesNoPage(Some(true)))

              response must haveStatus(SEE_OTHER)
              response must haveALocationHeaderValue(PageRelativeURLs.incomeFromOverseasPensionsCountry)
              getViewModel mustBe Some(expectedViewModel)

            }
          }

        }
        "fail" when {

          val expectedYesNoPageContentsIndividual = ExpectedYesNoPageContents(
            title = "Error: Did you get payments from an overseas pension scheme?",
            header = "Did you get payments from an overseas pension scheme?",
            caption = s"Income from overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear",
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
          )

          val expectedYesNoPageContentsAgent = expectedYesNoPageContentsIndividual.copy(
            title = "Error: Did your client get payments from an overseas pension scheme?",
            header = "Did your client get payments from an overseas pension scheme?",
            errorSummarySectionOpt = Some(
              ErrorSummarySection(
                title = "There is a problem",
                body = "Select yes if your client had income from an overseas pension scheme",
                link = "#value")),
            errorAboveElementCheckSectionOpt = Some(
              ErrorAboveElementCheckSection(
                title = "Error: Select yes if your client had income from an overseas pension scheme",
                idOpt = Some("value")))
          )

          def welshTitle(epc: ExpectedYesNoPageContents): Option[ErrorSummarySection] =
            epc.errorSummarySectionOpt.map(ess => ess.copy(title = "Mae problem wedi codi"))

          "the user has selected neither 'Yes' nor 'No' and" when {

            val sessionData = pensionsUserData(minimalSessionDataToAccessThisPage)

            scenarioNameForIndividualAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
              val response                        = submitForm(SubmittedFormDataForYesNoPage(None))

              response must haveStatus(BAD_REQUEST)
              assertPageAsExpected(parse(response.body), expectedYesNoPageContentsIndividual)
            }

            scenarioNameForIndividualAndWelsh ignore {

              implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
              val response                        = submitForm(SubmittedFormDataForYesNoPage(None))

              response must haveStatus(BAD_REQUEST)
              assertPageAsExpected(
                parse(response.body),
                expectedYesNoPageContentsIndividual.copy(errorSummarySectionOpt = welshTitle(expectedYesNoPageContentsIndividual)))
            }

            scenarioNameForAgentAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
              val response                        = submitForm(SubmittedFormDataForYesNoPage(None))

              response must haveStatus(BAD_REQUEST)
              assertPageAsExpected(parse(response.body), expectedYesNoPageContentsAgent)

            }
            scenarioNameForAgentAndWelsh ignore {

              implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
              val response                        = submitForm(SubmittedFormDataForYesNoPage(None))

              response must haveStatus(BAD_REQUEST)
              assertPageAsExpected(
                parse(response.body),
                expectedYesNoPageContentsAgent.copy(errorSummarySectionOpt = welshTitle(expectedYesNoPageContentsAgent)))

            }
          }
        }
      }
    }
  }

  private def getViewModel(implicit userConfig: UserConfig): Option[IncomeFromOverseasPensionsViewModel] =
    loadPensionUserData.map(_.pensions.incomeFromOverseasPensions)

}
