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

import builders.PensionsCYAModelBuilder.aPensionsCYAModel
import builders.TransfersIntoOverseasPensionsViewModelBuilder.{aTransfersIntoOverseasPensionsViewModel, emptyTransfersIntoOverseasPensionsViewModel}
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

  val minimalSessionDataToAccessThisPage: PensionsCYAModel =
    aPensionsCYAModel.copy(transfersIntoOverseasPensions = aTransfersIntoOverseasPensionsViewModel.copy(transferPensionScheme = Seq.empty))

  ".show" should {
    "redirect to the expected page" when {
      "redirect to the start of the journey" when {
        "the user has only the minimal/incomplete session data for accessing this page and" when {

          val sessionData = pensionsUserData(aPensionsCYAModel.copy(transfersIntoOverseasPensions = emptyTransfersIntoOverseasPensionsViewModel))

          scenarioNameForIndividualAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            val response                        = getPage

            response must haveStatus(SEE_OTHER)
            response must haveALocationHeaderValue(PageRelativeURLs.transferPensionSavings)

          }
          scenarioNameForIndividualAndWelsh ignore {

            implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
            val response                        = getPage

            response must haveStatus(SEE_OTHER)
            response must haveALocationHeaderValue(PageRelativeURLs.transferPensionSavings)

          }
          scenarioNameForAgentAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
            val response                        = getPage

            response must haveStatus(SEE_OTHER)
            response must haveALocationHeaderValue(PageRelativeURLs.transferPensionSavings)

          }
          scenarioNameForAgentAndWelsh ignore {

            implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
            val response                        = getPage

            response must haveStatus(OK)
            assertPageAsExpected(
              parse(response.body),
              ExpectedYesNoPageContents(
                title = "Did a UK pension scheme pay the transfer charge to HMRC?",
                header = "Did a UK pension scheme pay the transfer charge to HMRC?",
                caption = s"Transfers into overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear",
                radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
                radioButtonForNo = uncheckedExpectedRadioButton("No"),
                buttonForContinue = ExpectedButton("Continue", ""),
                links = Set.empty,
                text = Set.empty
              )
            )

          }
        }

        "load page as expected" when {
          "the user had previously answered all questions" when {

            val sessionData = pensionsUserData(
              minimalSessionDataToAccessThisPage.copy(
                transfersIntoOverseasPensions = aTransfersIntoOverseasPensionsViewModel
              )
            )

            scenarioNameForIndividualAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
              val response                        = getPageWithIndex()

              response must haveStatus(OK)
              assertPageAsExpected(
                parse(response.body),
                ExpectedYesNoPageContents(
                  title = "Did a UK pension scheme pay the transfer charge to HMRC?",
                  header = "Did a UK pension scheme pay the transfer charge to HMRC?",
                  caption = s"Transfers into overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  links = Set.empty,
                  text = Set.empty,
                  formUrl = formUrl()
                )
              )

            }
            scenarioNameForIndividualAndWelsh ignore {

              implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
              val response                        = getPageWithIndex()

              response must haveStatus(OK)
              assertPageAsExpected(
                parse(response.body),
                ExpectedYesNoPageContents(
                  title = "Did a UK pension scheme pay the transfer charge to HMRC?",
                  header = "Did a UK pension scheme pay the transfer charge to HMRC?",
                  caption = s"Transfers into overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  links = Set.empty,
                  text = Set.empty,
                  formUrl = formUrl()
                )
              )

            }
            scenarioNameForAgentAndEnglish in {

              implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
              val response                        = getPageWithIndex()

              response must haveStatus(OK)
              assertPageAsExpected(
                parse(response.body),
                ExpectedYesNoPageContents(
                  title = "Did a UK pension scheme pay the transfer charge to HMRC?",
                  header = "Did a UK pension scheme pay the transfer charge to HMRC?",
                  caption = s"Transfers into overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  links = Set.empty,
                  text = Set.empty,
                  formUrl = formUrl()
                )
              )

            }
            scenarioNameForAgentAndWelsh ignore {

              implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
              val response                        = getPageWithIndex()

              response must haveStatus(OK)
              assertPageAsExpected(
                parse(response.body),
                ExpectedYesNoPageContents(
                  title = "Did a UK pension scheme pay the transfer charge to HMRC?",
                  header = "Did a UK pension scheme pay the transfer charge to HMRC?",
                  caption = s"Transfers into overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear",
                  radioButtonForYes = checkedExpectedRadioButton("Yes"),
                  radioButtonForNo = uncheckedExpectedRadioButton("No"),
                  buttonForContinue = ExpectedButton("Continue", ""),
                  links = Set.empty,
                  text = Set.empty,
                  formUrl = formUrl()
                )
              )
            }
          }
        }

      }
    }
  }
  ".submit" should {
    "succeed" when {
      "the user has selected 'No' and" when {

        val minimalSessionData = pensionsUserData(minimalSessionDataToAccessThisPage)

        val expectedScheme = TransferPensionScheme(ukTransferCharge = Some(false))
        val expectedViewModel =
          minimalSessionData.pensions.transfersIntoOverseasPensions.copy(transferPensionScheme = Seq(expectedScheme))

        scenarioNameForIndividualAndEnglish in {

          implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(minimalSessionData))
          implicit val response: WSResponse   = submitForm(SubmittedFormDataForYesNoPage(Some(false)))
          val redirectPage = relativeUrl("/overseas-pensions/overseas-transfer-charges/overseas-transfer-charge-pension-scheme?index=0")

          response must haveStatus(SEE_OTHER)
          assertRedirectionAsExpected(redirectPage)
          getViewModel mustBe Some(expectedViewModel)

        }
        scenarioNameForIndividualAndWelsh ignore {

          implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(minimalSessionData))
          implicit val response: WSResponse   = submitForm(SubmittedFormDataForYesNoPage(Some(false)))
          val redirectPage = relativeUrl("/overseas-pensions/overseas-transfer-charges/overseas-transfer-charge-pension-scheme?index=0")

          response must haveStatus(SEE_OTHER)
          assertRedirectionAsExpected(redirectPage)
          getViewModel mustBe Some(expectedViewModel)

        }
        scenarioNameForAgentAndEnglish in {

          implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(minimalSessionData))
          implicit val response: WSResponse   = submitForm(SubmittedFormDataForYesNoPage(Some(false)))
          val redirectPage = relativeUrl("/overseas-pensions/overseas-transfer-charges/overseas-transfer-charge-pension-scheme?index=0")

          response must haveStatus(SEE_OTHER)
          assertRedirectionAsExpected(redirectPage)
          getViewModel mustBe Some(expectedViewModel)

        }
        scenarioNameForAgentAndWelsh ignore {

          implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(minimalSessionData))
          implicit val response: WSResponse   = submitForm(SubmittedFormDataForYesNoPage(Some(false)))
          val redirectPage = relativeUrl("/overseas-pensions/overseas-transfer-charges/overseas-transfer-charge-pension-scheme?index=0")

          response must haveStatus(SEE_OTHER)
          assertRedirectionAsExpected(redirectPage)
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
          implicit val response: WSResponse   = submitForm(SubmittedFormDataForYesNoPage(Some(true)))
          val redirectPage = relativeUrl("/overseas-pensions/overseas-transfer-charges/overseas-transfer-charge-pension-scheme?index=0")

          response must haveStatus(SEE_OTHER)
          assertRedirectionAsExpected(redirectPage)
          getViewModel mustBe Some(expectedViewModel)

        }
        scenarioNameForIndividualAndWelsh ignore {

          implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
          implicit val response: WSResponse   = submitForm(SubmittedFormDataForYesNoPage(Some(true)))
          val redirectPage = relativeUrl("/overseas-pensions/overseas-transfer-charges/overseas-transfer-charge-pension-scheme?index=0")

          response must haveStatus(SEE_OTHER)
          assertRedirectionAsExpected(redirectPage)
          getViewModel mustBe Some(expectedViewModel)

        }
        scenarioNameForAgentAndEnglish in {

          implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
          implicit val response: WSResponse   = submitForm(SubmittedFormDataForYesNoPage(Some(true)))
          val redirectPage = relativeUrl("/overseas-pensions/overseas-transfer-charges/overseas-transfer-charge-pension-scheme?index=0")

          response must haveStatus(SEE_OTHER)
          assertRedirectionAsExpected(redirectPage)
          getViewModel mustBe Some(expectedViewModel)

        }
        scenarioNameForAgentAndWelsh ignore {

          implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
          implicit val response: WSResponse   = submitForm(SubmittedFormDataForYesNoPage(Some(true)))
          val redirectPage = relativeUrl("/overseas-pensions/overseas-transfer-charges/overseas-transfer-charge-pension-scheme?index=0")

          response must haveStatus(SEE_OTHER)
          assertRedirectionAsExpected(redirectPage)
          getViewModel mustBe Some(expectedViewModel)

        }
      }

    }
    "fail" when {
      "the user has selected neither 'Yes' nor 'No' and" when {

        val sessionData = pensionsUserData(minimalSessionDataToAccessThisPage)

        scenarioNameForIndividualAndEnglish in {

          implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
          val response                        = submitForm(SubmittedFormDataForYesNoPage(None))

          response must haveStatus(BAD_REQUEST)
          assertPageAsExpected(
            parse(response.body),
            ExpectedYesNoPageContents(
              title = "Error: Did a UK pension scheme pay the transfer charge to HMRC?",
              header = "Did a UK pension scheme pay the transfer charge to HMRC?",
              caption = s"Transfers into overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear",
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
            )
          )

        }
        scenarioNameForIndividualAndWelsh ignore {

          implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
          val response                        = submitForm(SubmittedFormDataForYesNoPage(None))

          response must haveStatus(BAD_REQUEST)
          assertPageAsExpected(
            parse(response.body),
            ExpectedYesNoPageContents(
              title = "Error: Did a UK pension scheme pay the transfer charge to HMRC?",
              header = "Did a UK pension scheme pay the transfer charge to HMRC?",
              caption = s"Transfers into overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear",
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
            )
          )

        }
        scenarioNameForAgentAndEnglish in {

          implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
          val response                        = submitForm(SubmittedFormDataForYesNoPage(None))

          response must haveStatus(BAD_REQUEST)
          assertPageAsExpected(
            parse(response.body),
            ExpectedYesNoPageContents(
              title = "Error: Did a UK pension scheme pay the transfer charge to HMRC?",
              header = "Did a UK pension scheme pay the transfer charge to HMRC?",
              caption = s"Transfers into overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear",
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
            )
          )

        }
        scenarioNameForAgentAndWelsh ignore {

          implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
          val response                        = submitForm(SubmittedFormDataForYesNoPage(None))

          response must haveStatus(BAD_REQUEST)
          assertPageAsExpected(
            parse(response.body),
            ExpectedYesNoPageContents(
              title = "Error: Did a UK pension scheme pay the transfer charge to HMRC?",
              header = "Did a UK pension scheme pay the transfer charge to HMRC?",
              caption = s"Transfers into overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear",
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
            )
          )

        }
      }

    }
    "redirect to first page of journey" when {
      "previous question has not been answered" in {
        val incompleteCYAModel =
          aPensionsCYAModel.copy(transfersIntoOverseasPensions = TransfersIntoOverseasPensionsViewModel(overseasTransferCharge = Some(false)))
        val sessionData                     = pensionsUserData(incompleteCYAModel)
        implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
        implicit val response: WSResponse   = getPageWithIndex()

        assertRedirectionAsExpected(PageRelativeURLs.transferPensionSavings)
      }
    }
  }

  private def getViewModel(implicit userConfig: UserConfig): Option[TransfersIntoOverseasPensionsViewModel] =
    loadPensionUserData.map(_.pensions.transfersIntoOverseasPensions)

  override def getPageWithIndex(index: Int = 0)(implicit userConfig: UserConfig, wsClient: WSClient): WSResponse =
    getPage(getMap(index))

  private def formUrl(index: Int = 0): Option[String] =
    Some(relativeUrlForThisPage + "?pensionSchemeIndex=" + index)

  private def getMap(index: Int): Map[String, String] =
    Map("pensionSchemeIndex" -> index.toString)
}
