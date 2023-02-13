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

import builders.PensionsCYAModelBuilder.aPensionsCYAEmptyModel
import controllers.ControllerSpec.PreferredLanguages.{English, Welsh}
import controllers.ControllerSpec.UserTypes.{Agent, Individual}
import controllers.ControllerSpec._
import controllers.YesNoControllerSpec
import models.mongo.PensionsCYAModel
import models.pension.charges.{TransferPensionScheme, TransfersIntoOverseasPensionsViewModel}
import org.jsoup.Jsoup.parse
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.{WSClient, WSResponse}

class OverseasTransferChargePaidControllerISpec
  extends YesNoControllerSpec("/overseas-pensions/overseas-transfer-charges/overseas-transfer-charge-paid") {

  val minimalSessionDataToAccessThisPage: PensionsCYAModel = aPensionsCYAEmptyModel

  "This page" when {
    "requested to be shown" should {
      "redirect to the expected page" when {
        "the user has no stored session data at all" in {

          val response = getPage(None)

          response must haveStatus(SEE_OTHER)
          response must haveALocationHeaderValue(PageRelativeURLs.overseasPensionsSummary)

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
                  title = "Did a UK pension scheme pay the transfer charge to HMRC?",
                  header = "Did a UK pension scheme pay the transfer charge to HMRC?",
                  caption = "Transfers into overseas pensions for 6 April 2021 to 5 April 2022",
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
                  title = "Did a UK pension scheme pay the transfer charge to HMRC?",
                  header = "Did a UK pension scheme pay the transfer charge to HMRC?",
                  caption = "Transfers into overseas pensions for 6 April 2021 to 5 April 2022",
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
                  title = "Did a UK pension scheme pay the transfer charge to HMRC?",
                  header = "Did a UK pension scheme pay the transfer charge to HMRC?",
                  caption = "Transfers into overseas pensions for 6 April 2021 to 5 April 2022",
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
                  title = "Did a UK pension scheme pay the transfer charge to HMRC?",
                  header = "Did a UK pension scheme pay the transfer charge to HMRC?",
                  caption = "Transfers into overseas pensions for 6 April 2021 to 5 April 2022",
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
                transfersIntoOverseasPensions = minimalSessionDataToAccessThisPage.transfersIntoOverseasPensions.copy(
                  transferPensionScheme = Seq(TransferPensionScheme(ukTransferCharge = Some(true))
                ))
              )
            )

            scenarioNameForIndividualAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
              val response = getPageWithIndex()

              response must haveStatus(OK)
              assertPageAsExpected(
                parse(response.body),
                ExpectedYesNoPageContents(
                  title = "Did a UK pension scheme pay the transfer charge to HMRC?",
                  header = "Did a UK pension scheme pay the transfer charge to HMRC?",
                  caption = "Transfers into overseas pensions for 6 April 2021 to 5 April 2022",
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
              val response = getPageWithIndex()

              response must haveStatus(OK)
              assertPageAsExpected(
                parse(response.body),
                ExpectedYesNoPageContents(
                  title = "Did a UK pension scheme pay the transfer charge to HMRC?",
                  header = "Did a UK pension scheme pay the transfer charge to HMRC?",
                  caption = "Transfers into overseas pensions for 6 April 2021 to 5 April 2022",
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
              val response = getPageWithIndex()

              response must haveStatus(OK)
              assertPageAsExpected(
                parse(response.body),
                ExpectedYesNoPageContents(
                  title = "Did a UK pension scheme pay the transfer charge to HMRC?",
                  header = "Did a UK pension scheme pay the transfer charge to HMRC?",
                  caption = "Transfers into overseas pensions for 6 April 2021 to 5 April 2022",
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
              val response = getPageWithIndex()

              response must haveStatus(OK)
              assertPageAsExpected(
                parse(response.body),
                ExpectedYesNoPageContents(
                  title = "Did a UK pension scheme pay the transfer charge to HMRC?",
                  header = "Did a UK pension scheme pay the transfer charge to HMRC?",
                  caption = "Transfers into overseas pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  links = Set.empty,
                  text = Set.empty,
                  formUrl = formUrl()
                ))

            }
          }
          "the user had previously answered 'No', and" when {

            val sessionData = pensionsUserData(
              minimalSessionDataToAccessThisPage.copy(
                transfersIntoOverseasPensions = minimalSessionDataToAccessThisPage.transfersIntoOverseasPensions.copy(
                  transferPensionScheme = Seq(TransferPensionScheme(ukTransferCharge = Some(false))
                  ))
              )
            )

            scenarioNameForIndividualAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
              val response = getPageWithIndex()

              response must haveStatus(OK)
              assertPageAsExpected(
                parse(response.body),
                ExpectedYesNoPageContents(
                  title = "Did a UK pension scheme pay the transfer charge to HMRC?",
                  header = "Did a UK pension scheme pay the transfer charge to HMRC?",
                  caption = "Transfers into overseas pensions for 6 April 2021 to 5 April 2022",
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
              val response = getPageWithIndex()

              response must haveStatus(OK)
              assertPageAsExpected(
                parse(response.body),
                ExpectedYesNoPageContents(
                  title = "Did a UK pension scheme pay the transfer charge to HMRC?",
                  header = "Did a UK pension scheme pay the transfer charge to HMRC?",
                  caption = "Transfers into overseas pensions for 6 April 2021 to 5 April 2022",
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
              val response = getPageWithIndex()

              response must haveStatus(OK)
              assertPageAsExpected(
                parse(response.body),
                ExpectedYesNoPageContents(
                  title = "Did a UK pension scheme pay the transfer charge to HMRC?",
                  header = "Did a UK pension scheme pay the transfer charge to HMRC?",
                  caption = "Transfers into overseas pensions for 6 April 2021 to 5 April 2022",
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
              val response = getPageWithIndex()

              response must haveStatus(OK)
              assertPageAsExpected(
                parse(response.body),
                ExpectedYesNoPageContents(
                  title = "Did a UK pension scheme pay the transfer charge to HMRC?",
                  header = "Did a UK pension scheme pay the transfer charge to HMRC?",
                  caption = "Transfers into overseas pensions for 6 April 2021 to 5 April 2022",
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
      "submitted" should {
        "succeed" when {
          "the user has selected 'No' and" when {

            val sessionData = pensionsUserData(minimalSessionDataToAccessThisPage)

            val expectedScheme = TransferPensionScheme(ukTransferCharge = Some(false))
            val expectedViewModel =
              sessionData.pensions.transfersIntoOverseasPensions.copy(transferPensionScheme = Seq(expectedScheme))

            scenarioNameForIndividualAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
              val response = submitForm(SubmittedFormDataForYesNoPage(Some(false)))

              response must haveStatus(SEE_OTHER)
              response must haveALocationHeaderValue(formUrl(0).get)
              getViewModel mustBe Some(expectedViewModel)

            }
            scenarioNameForIndividualAndWelsh in {

              implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
              val response = submitForm(SubmittedFormDataForYesNoPage(Some(false)))

              response must haveStatus(SEE_OTHER)
              response must haveALocationHeaderValue(formUrl(0).get)
              getViewModel mustBe Some(expectedViewModel)

            }
            scenarioNameForAgentAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
              val response = submitForm(SubmittedFormDataForYesNoPage(Some(false)))

              response must haveStatus(SEE_OTHER)
              response must haveALocationHeaderValue(formUrl(0).get)
              getViewModel mustBe Some(expectedViewModel)

            }
            scenarioNameForAgentAndWelsh in {

              implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
              val response = submitForm(SubmittedFormDataForYesNoPage(Some(false)))

              response must haveStatus(SEE_OTHER)
              response must haveALocationHeaderValue(formUrl(0).get)
              getViewModel mustBe Some(expectedViewModel)

            }
          }
          "the user has selected 'Yes'" when {

            val sessionData = pensionsUserData(minimalSessionDataToAccessThisPage)

            val expectedScheme = TransferPensionScheme(ukTransferCharge = Some(true))
            val expectedViewModel =
              sessionData.pensions.transfersIntoOverseasPensions.copy(transferPensionScheme = Seq(expectedScheme))


            scenarioNameForIndividualAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
              val response = submitForm(SubmittedFormDataForYesNoPage(Some(true)))

              response must haveStatus(SEE_OTHER)
              response must haveALocationHeaderValue(formUrl(0).get)
              getViewModel mustBe Some(expectedViewModel)

            }
            scenarioNameForIndividualAndWelsh in {

              implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
              val response = submitForm(SubmittedFormDataForYesNoPage(Some(true)))

              response must haveStatus(SEE_OTHER)
              response must haveALocationHeaderValue(formUrl(0).get)
              getViewModel mustBe Some(expectedViewModel)

            }
            scenarioNameForAgentAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
              val response = submitForm(SubmittedFormDataForYesNoPage(Some(true)))

              response must haveStatus(SEE_OTHER)
              response must haveALocationHeaderValue(formUrl(0).get)
              getViewModel mustBe Some(expectedViewModel)

            }
            scenarioNameForAgentAndWelsh in {

              implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
              val response = submitForm(SubmittedFormDataForYesNoPage(Some(true)))

              response must haveStatus(SEE_OTHER)
              response must haveALocationHeaderValue(formUrl(0).get)
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
                  title = "Error: Did a UK pension scheme pay the transfer charge to HMRC?",
                  header = "Did a UK pension scheme pay the transfer charge to HMRC?",
                  caption = "Transfers into overseas pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  links = Set.empty,
                  text = Set.empty,
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "Select yes if a UK pension scheme paid the transfer charge to HMRC",
                      link = "#value")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Select yes if a UK pension scheme paid the transfer charge to HMRC",
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
                  title = "Error: Did a UK pension scheme pay the transfer charge to HMRC?",
                  header = "Did a UK pension scheme pay the transfer charge to HMRC?",
                  caption = "Transfers into overseas pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  links = Set.empty,
                  text = Set.empty,
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "Select yes if a UK pension scheme paid the transfer charge to HMRC",
                      link = "#value")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Select yes if a UK pension scheme paid the transfer charge to HMRC",
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
                  title = "Error: Did a UK pension scheme pay the transfer charge to HMRC?",
                  header = "Did a UK pension scheme pay the transfer charge to HMRC?",
                  caption = "Transfers into overseas pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  links = Set.empty,
                  text = Set.empty,
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "There is a problem",
                      body = "Select yes if a UK pension scheme paid the transfer charge to HMRC",
                      link = "#value")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Select yes if a UK pension scheme paid the transfer charge to HMRC",
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
                  title = "Error: Did a UK pension scheme pay the transfer charge to HMRC?",
                  header = "Did a UK pension scheme pay the transfer charge to HMRC?",
                  caption = "Transfers into overseas pensions for 6 April 2021 to 5 April 2022",
                  radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  links = Set.empty,
                  text = Set.empty,
                  errorSummarySectionOpt = Some(
                    ErrorSummarySection(
                      title = "Mae problem wedi codi",
                      body = "Select yes if a UK pension scheme paid the transfer charge to HMRC",
                      link = "#value")
                  ),
                  errorAboveElementCheckSectionOpt = Some(
                    ErrorAboveElementCheckSection(
                      title = "Error: Select yes if a UK pension scheme paid the transfer charge to HMRC",
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

  private def getViewModel(implicit userConfig: UserConfig): Option[TransfersIntoOverseasPensionsViewModel] =
    loadPensionUserData.map(_.pensions.transfersIntoOverseasPensions)

  override def getPageWithIndex(index: Int = 0)(implicit userConfig: UserConfig, wsClient: WSClient): WSResponse = {
    getPage(getMap(index))
  }
  
  private def formUrl(index: Int = 0): Option[String] =
    Some(relativeUrlForThisPage + "?pensionSchemeIndex=" + index)

  private def getMap(index: Int): Map[String, String] = {
    Map("pensionSchemeIndex" -> index.toString)
  }
}







