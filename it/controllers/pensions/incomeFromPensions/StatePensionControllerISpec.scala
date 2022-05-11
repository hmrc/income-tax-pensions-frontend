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

package controllers.pensions.incomeFromPensions

import builders.IncomeFromPensionsViewModelBuilder.anIncomeFromPensionsViewModel
import builders.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import builders.PensionsCYAModelBuilder._
import builders.PensionsUserDataBuilder
import builders.StateBenefitViewModelBuilder.anStateBenefitViewModelOne
import builders.UserBuilder._
import forms.YesNoForm
import models.mongo.PensionsCYAModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.IncomeFromPensionsPages.{statePension, statePensionLumpSumUrl, ukPensionSchemePayments}
import utils.PageUrls._
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

class StatePensionControllerISpec extends IntegrationTest with ViewHelpers with BeforeAndAfterEach with PensionsDatabaseHelper {


  private def pensionsUsersData(isPrior: Boolean = false, pensionsCyaModel: PensionsCYAModel) = {
    PensionsUserDataBuilder.aPensionsUserData.copy(
      isPriorSubmission = isPrior,
      pensions = pensionsCyaModel
    )
  }

  object Selectors {
    val captionSelector: String = "#main-content > div > div > header > p"
    val continueButtonSelector: String = "#continue"
    val formSelector: String = "#main-content > div > div > form"
    val yesSelector = "#value"
    val noSelector = "#value-no"
    val detailsSelector: String = s"#main-content > div > div > form > details > summary > span"

    def h3Selector(index: Int): String = s"#main-content > div > div > form > details > div > h3:nth-child($index)"

    def paragraphSelector(index: Int): String = s"#main-content > div > div > p:nth-of-type($index)"

    def bulletListSelector(index: Int): String = s"#main-content > div > div > ul > li:nth-child($index)"

    def detailsParagraphSelector(index: Int): String = s"#main-content > div > div > form > details > div > p:nth-child($index)"

    def detailsBulletList(index: Int): String = s"#main-content > div > div > form > details > div > ul > li:nth-child($index)"
  }


  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val yesText: String
    val noText: String
    val buttonText: String
    val expectedDetailsTitle: String
    val expectedDetailsYouCanFindThisOut: String
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedErrorTitle: String
    val expectedErrorMessage: String
    val expectedDetailsBullet1: String
    val expectedDetailsBullet2: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Income from pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val yesText = "Yes"
    val noText = "No"
    val buttonText = "Continue"
    val expectedDetailsTitle = "Where to find this information"
    val expectedDetailsYouCanFindThisOut: String = "You can find this information in:"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Income from pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val yesText = "Yes"
    val noText = "No"
    val buttonText = "Continue"
    val expectedDetailsTitle = "Where to find this information"
    val expectedDetailsYouCanFindThisOut: String = "You can find this information in:"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Did you get State Pension this year?"
    val expectedHeading = "Did you get State Pension this year?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorMessage = "Select yes if you got State Pension this year"
    val expectedDetailsBullet1 = "your P60"
    val expectedDetailsBullet2 = "the ‘About general increases in benefits’ letter the Pension Service sent you"

  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Did you get State Pension this year?"
    val expectedHeading = "Did you get State Pension this year?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorMessage = "Select yes if you got State Pension this year"
    val expectedDetailsBullet1 = "your P60"
    val expectedDetailsBullet2 = "the ‘About general increases in benefits’ letter the Pension Service sent you"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Did your client get State Pension this year?"
    val expectedHeading = "Did your client get State Pension this year?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorMessage = "Select yes if your client got State Pension this year"
    val expectedDetailsBullet1 = "your client’s P60"
    val expectedDetailsBullet2 = "the ‘About general increases in benefits’ letter the Pension Service sent your client"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Did your client get State Pension this year?"
    val expectedHeading = "Did your client get State Pension this year?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorMessage = "Select yes if your client got State Pension this year"
    val expectedDetailsBullet1 = "your client’s P60"
    val expectedDetailsBullet2 = "the ‘About general increases in benefits’ letter the Pension Service sent your client"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

    ".show" should {
      userScenarios.foreach { user =>

        import Selectors._
        import user.commonExpectedResults._

        s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
          "render the state pension question page with no pre-filled radio buttons" which {
            lazy val result: WSResponse = {
              dropPensionsDB()
              authoriseAgentOrIndividual(user.isAgent)
              val statePensionModel = anStateBenefitViewModelOne.copy(
                amountPaidQuestion = None
              )
              val pensionViewModel = anIncomeFromPensionsViewModel.copy(statePension = Some(statePensionModel))
              insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(incomeFromPensions = pensionViewModel)), aUserRequest)
              urlGet(fullUrl(statePension(taxYearEOY)), user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            "has an OK status" in {
              result.status shouldBe OK
            }

            titleCheck(user.specificExpectedResults.get.expectedTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(expectedCaption(taxYearEOY), captionSelector)
            radioButtonCheck(yesText, 1, checked = Some(false))
            radioButtonCheck(noText, 2, checked = Some(false))
            buttonCheck(buttonText, continueButtonSelector)

            textOnPageCheck(expectedDetailsTitle, detailsSelector)
            textOnPageCheck(expectedDetailsYouCanFindThisOut, detailsParagraphSelector(1))
            textOnPageCheck(user.specificExpectedResults.get.expectedDetailsBullet1, detailsBulletList(1))
            textOnPageCheck(user.specificExpectedResults.get.expectedDetailsBullet2, detailsBulletList(2))
            formPostLinkCheck(statePension(taxYearEOY), formSelector)
            welshToggleCheck(user.isWelsh)
          }
          "render the state pension question page with 'Yes' pre-filled when CYA data exists" which {
            lazy val result: WSResponse = {
              dropPensionsDB()
              authoriseAgentOrIndividual(user.isAgent)
              val statePensionModel = anStateBenefitViewModelOne.copy(
                amountPaidQuestion = Some(true)
              )
              val pensionViewModel = anIncomeFromPensionsViewModel.copy(statePension = Some(statePensionModel))
              insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(incomeFromPensions = pensionViewModel)), aUserRequest)
              urlGet(fullUrl(statePension(taxYearEOY)), user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            "has an OK status" in {
              result.status shouldBe OK
            }

            titleCheck(user.specificExpectedResults.get.expectedTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(expectedCaption(taxYearEOY), captionSelector)
            radioButtonCheck(yesText, 1, checked = Some(true))
            radioButtonCheck(noText, 2, checked = Some(false))
            buttonCheck(buttonText, continueButtonSelector)

            textOnPageCheck(expectedDetailsTitle, detailsSelector)
            textOnPageCheck(expectedDetailsYouCanFindThisOut, detailsParagraphSelector(1))
            textOnPageCheck(user.specificExpectedResults.get.expectedDetailsBullet1, detailsBulletList(1))
            textOnPageCheck(user.specificExpectedResults.get.expectedDetailsBullet2, detailsBulletList(2))
            formPostLinkCheck(statePension(taxYearEOY), formSelector)
            welshToggleCheck(user.isWelsh)
          }
          "render the state pension question page with 'No' pre-filled and not a prior submission" which {
            lazy val result: WSResponse = {
              dropPensionsDB()
              authoriseAgentOrIndividual(user.isAgent)
              val statePensionModel = anStateBenefitViewModelOne.copy(
                amountPaidQuestion = Some(false)
              )
              val pensionViewModel = anIncomeFromPensionsViewModel.copy(statePension = Some(statePensionModel))
              insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(incomeFromPensions = pensionViewModel)), aUserRequest)
              urlGet(fullUrl(statePension(taxYearEOY)), user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            "has an OK status" in {
              result.status shouldBe OK
            }

            titleCheck(user.specificExpectedResults.get.expectedTitle)
            h1Check(user.specificExpectedResults.get.expectedHeading)
            captionCheck(expectedCaption(taxYearEOY), captionSelector)
            radioButtonCheck(yesText, 1, checked = Some(false))
            radioButtonCheck(noText, 2, checked = Some(true))
            buttonCheck(buttonText, continueButtonSelector)

            textOnPageCheck(expectedDetailsTitle, detailsSelector)
            textOnPageCheck(expectedDetailsYouCanFindThisOut, detailsParagraphSelector(1))
            textOnPageCheck(user.specificExpectedResults.get.expectedDetailsBullet1, detailsBulletList(1))
            textOnPageCheck(user.specificExpectedResults.get.expectedDetailsBullet2, detailsBulletList(2))
            formPostLinkCheck(statePension(taxYearEOY), formSelector)
            welshToggleCheck(user.isWelsh)
          }
        }
      }

      "redirect to the Pension Summary page if there is no session data" which {
        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual(isAgent = false)
          urlGet(fullUrl(statePension(taxYearEOY)), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
        }

        "has an SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(pensionSummaryUrl(taxYearEOY)) shouldBe true
        }

      }

      "Redirect user to the pension summary page when in year" which {

        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual(isAgent = false)
          val statePensionModel = anStateBenefitViewModelOne.copy(
            amountPaidQuestion = None
          )
          val pensionViewModel = anIncomeFromPensionsViewModel.copy(statePension = Some(statePensionModel))
          insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(incomeFromPensions = pensionViewModel)), aUserRequest)
          urlGet(fullUrl(statePension(taxYear)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(overviewUrl(taxYear)) shouldBe true
        }
      }

    }

  ".submit" should {
    userScenarios.foreach { user =>

      import Selectors._
      import user.commonExpectedResults._

      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "return an error when form is submitted with no entry" which {
          lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> "")

          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            val statePensionModel = anStateBenefitViewModelOne.copy(
              amountPaidQuestion = None
            )
            val pensionViewModel = anIncomeFromPensionsViewModel.copy(statePension = Some(statePensionModel))
            insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(incomeFromPensions = pensionViewModel)), aUserRequest)
            urlPost(fullUrl(statePension(taxYearEOY)), body = form, welsh = user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
          }

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          radioButtonCheck(yesText, 1, checked = Some(false))
          radioButtonCheck(noText, 2, checked = Some(false))
          buttonCheck(buttonText, continueButtonSelector)

          textOnPageCheck(expectedDetailsTitle, detailsSelector)
          textOnPageCheck(expectedDetailsYouCanFindThisOut, detailsParagraphSelector(1))
          textOnPageCheck(user.specificExpectedResults.get.expectedDetailsBullet1, detailsBulletList(1))
          textOnPageCheck(user.specificExpectedResults.get.expectedDetailsBullet2, detailsBulletList(2))
          formPostLinkCheck(statePension(taxYearEOY), formSelector)
          welshToggleCheck(user.isWelsh)
          errorSummaryCheck(user.specificExpectedResults.get.expectedErrorMessage, Selectors.yesSelector)
          errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorMessage, Some("value"))

        }
      }
    }


    "redirect to Pensions Summary page when user selects 'yes' and not a prior submission" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val statePensionModel = anStateBenefitViewModelOne.copy(
          amountPaidQuestion = None
        )
        val pensionViewModel = anIncomeFromPensionsViewModel.copy(statePension = Some(statePensionModel))
        insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(incomeFromPensions = pensionViewModel)), aUserRequest)
        urlPost(fullUrl(statePension(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(ukPensionSchemePayments(taxYearEOY))
      }

      "updates amount paid question to Some(true)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.incomeFromPensions.statePension.flatMap(_.amountPaidQuestion) shouldBe Some(true)
      }
    }
  }

  "redirect to Pension Summary when user selects 'no' and doesnt complete CYA model" which {
    lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
    lazy val result: WSResponse = {
      dropPensionsDB()
      authoriseAgentOrIndividual(isAgent = false)
      userDataStub(anIncomeTaxUserData, nino, taxYearEOY)

      val statePensionModel = anStateBenefitViewModelOne.copy(
        amountPaidQuestion = Some(true),
        amount = Some(100.00)
      )
      val pensionViewModel = anIncomeFromPensionsViewModel.copy(statePension = Some(statePensionModel))
      insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(incomeFromPensions = pensionViewModel)), aUserRequest)
      urlPost(fullUrl(statePension(taxYearEOY)), body = form, follow = false,
        headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY)))
    }

    "has a SEE_OTHER(303) status" in {
      result.status shouldBe SEE_OTHER
      result.header("location") shouldBe Some(statePensionLumpSumUrl(taxYearEOY))
    }

    "updates amount paid question to Some(false) and deletes the amount paid amount" in {
      lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
      cyaModel.pensions.incomeFromPensions.statePension.flatMap(_.amountPaidQuestion) shouldBe Some(false)
      cyaModel.pensions.incomeFromPensions.statePension.flatMap(_.amount) shouldBe None
    }
  }
  "Redirect user to the pension summary page when in year" which {
    lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
    lazy val result: WSResponse = {


      dropPensionsDB()
      authoriseAgentOrIndividual(isAgent = false)
      userDataStub(anIncomeTaxUserData, nino, taxYear)

      val statePensionModel = anStateBenefitViewModelOne.copy(
        amountPaidQuestion = Some(true))
      val pensionViewModel = anIncomeFromPensionsViewModel.copy(statePension = Some(statePensionModel))
      insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(incomeFromPensions = pensionViewModel)), aUserRequest)
      urlPost(fullUrl(statePension(taxYear)), body = form, follow = false,
        headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
    }

    "has an SEE_OTHER(303) status" in {
      result.status shouldBe SEE_OTHER
      result.header("location").contains(overviewUrl(taxYear)) shouldBe true
    }
  }

}

