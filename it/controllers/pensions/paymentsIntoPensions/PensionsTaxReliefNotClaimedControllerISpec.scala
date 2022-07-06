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

import builders.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import builders.PaymentsIntoPensionVewModelBuilder.aPaymentsIntoPensionViewModel
import builders.PensionsCYAModelBuilder._
import builders.PensionsUserDataBuilder
import builders.UserBuilder._
import forms.YesNoForm
import models.mongo.{PensionsCYAModel, PensionsUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.PaymentIntoPensions.{checkPaymentsIntoPensionCyaUrl, pensionTaxReliefNotClaimedUrl, retirementAnnuityUrl}
import utils.PageUrls._
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}
import views.PensionsTaxReliefNotClaimedTestSupport.Selectors._
import views.PensionsTaxReliefNotClaimedTestSupport.ExpectedIndividualEN._
import views.PensionsTaxReliefNotClaimedTestSupport.CommonExpectedEN._
import views.PensionsTaxReliefNotClaimedTestSupport.Selectors


class PensionsTaxReliefNotClaimedControllerISpec extends IntegrationTest with ViewHelpers with BeforeAndAfterEach with PensionsDatabaseHelper {

  private def pensionsUsersData(pensionsCyaModel: PensionsCYAModel): PensionsUserData = {
    PensionsUserDataBuilder.aPensionsUserData.copy(
      isPriorSubmission = false,
      pensions = pensionsCyaModel
    )
  }
  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  ".show" should {

    "render the pensions where tax relief is not claimed question page with no pre-filled radio buttons if no CYA question data" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(pensionTaxReliefNotClaimedQuestion = None)
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)), aUserRequest)
        urlGet(
          fullUrl(
            pensionTaxReliefNotClaimedUrl(taxYearEOY)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an OK status" in {
        result.status shouldBe OK
      }

      implicit def document: () => Document = () => Jsoup.parse(result.body)

      titleCheck(expectedTitle)
      h1Check(expectedHeading)
      captionCheck(expectedCaption(taxYearEOY), captionSelector)
      textOnPageCheck(expectedQuestionsInfoText, paragraphSelector(1))
      textOnPageCheck(expectedWhereToCheck, paragraphSelector(2))
      textOnPageCheck(expectedSubHeading, h2Selector)
      radioButtonCheck(yesText, 1, checked = Some(false))
      radioButtonCheck(noText, 2, checked = Some(false))
      buttonCheck(buttonText, continueButtonSelector)
      formPostLinkCheck(pensionTaxReliefNotClaimedUrl(taxYearEOY), formSelector)
      welshToggleCheck(isWelsh = false)
    }

    "render the pensions where tax relief is not claimed question page with 'Yes' pre-filled when CYA data exists" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(pensionsUsersData(aPensionsCYAModel), aUserRequest)
        urlGet(fullUrl(pensionTaxReliefNotClaimedUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an OK status" in {
        result.status shouldBe OK
      }

      implicit def document: () => Document = () => Jsoup.parse(result.body)

      titleCheck(expectedTitle)
      h1Check(expectedHeading)
      captionCheck(expectedCaption(taxYearEOY), captionSelector)
      textOnPageCheck(expectedQuestionsInfoText, paragraphSelector(1))
      textOnPageCheck(expectedWhereToCheck, paragraphSelector(2))
      textOnPageCheck(expectedSubHeading, h2Selector)
      radioButtonCheck(yesText, 1, checked = Some(true))
      radioButtonCheck(noText, 2, checked = Some(false))
      buttonCheck(buttonText, continueButtonSelector)
      formPostLinkCheck(pensionTaxReliefNotClaimedUrl(taxYearEOY), formSelector)
      welshToggleCheck(isWelsh = false)
    }

    "render the pensions where tax relief is not claimed question page with 'No' pre-filled when CYA data exists" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val paymentsIntoPensionsViewModel = aPaymentsIntoPensionViewModel.copy(pensionTaxReliefNotClaimedQuestion = Some(false))
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(paymentsIntoPensionsViewModel)), aUserRequest)
        urlGet(fullUrl(pensionTaxReliefNotClaimedUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an OK status" in {
        result.status shouldBe OK
      }
      implicit def document: () => Document = () => Jsoup.parse(result.body)

      titleCheck(expectedTitle)
      h1Check(expectedHeading)
      captionCheck(expectedCaption(taxYearEOY), captionSelector)
      textOnPageCheck(expectedQuestionsInfoText, paragraphSelector(1))
      textOnPageCheck(expectedWhereToCheck, paragraphSelector(2))
      textOnPageCheck(expectedSubHeading, h2Selector)
      radioButtonCheck(yesText, 1, checked = Some(false))
      radioButtonCheck(noText, 2, checked = Some(true))
      buttonCheck(buttonText, continueButtonSelector)
      formPostLinkCheck(pensionTaxReliefNotClaimedUrl(taxYearEOY), formSelector)
      welshToggleCheck(isWelsh = false)
    }
  }


  "redirect to the CYA page if there is no session data" which {
    lazy val result: WSResponse = {
      dropPensionsDB()
      authoriseAgentOrIndividual(isAgent = false)
      // no cya insert
      urlGet(fullUrl(pensionTaxReliefNotClaimedUrl(taxYearEOY)), follow = false,
        headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
    }

    "has an SEE_OTHER status" in {
      result.status shouldBe SEE_OTHER
      result.header("location").contains(checkPaymentsIntoPensionCyaUrl(taxYearEOY)) shouldBe true
    }
  }


  ".submit" should {

    val validFormYes: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
    val validFormNo: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
    val invalidForm: Map[String, String] = Map(YesNoForm.yesNo -> "")

    "return an error when form is submitted with no entry" which {

      lazy val result: WSResponse = {
        dropPensionsDB()
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(
                  aPaymentsIntoPensionViewModel.copy(pensionTaxReliefNotClaimedQuestion = None))), aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(pensionTaxReliefNotClaimedUrl(taxYearEOY)), body = invalidForm, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has the correct status" in {
        result.status shouldBe BAD_REQUEST
      }
      implicit def document: () => Document = () => Jsoup.parse(result.body)

      titleCheck(expectedErrorTitle)
      h1Check(expectedHeading)
      captionCheck(expectedCaption(taxYearEOY), captionSelector)
      textOnPageCheck(expectedQuestionsInfoText, paragraphSelector(1))
      textOnPageCheck(expectedWhereToCheck, paragraphSelector(2))
      textOnPageCheck(expectedSubHeading, h2Selector)
      radioButtonCheck(yesText, 1, checked = Some(false))
      radioButtonCheck(noText, 2, checked = Some(false))
      buttonCheck(buttonText, continueButtonSelector)
      formPostLinkCheck(pensionTaxReliefNotClaimedUrl(taxYearEOY), formSelector)
      welshToggleCheck(isWelsh = false)
      errorSummaryCheck(expectedErrorMessage, Selectors.yesSelector)
      errorAboveElementCheck(expectedErrorMessage, Some("value"))
    }

    "redirect to Retirement Annuity Question page when user submits a 'yes' answer which doesnt complete CYA model and updates the session value to yes" which {

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val paymentsIntoPensionsViewModel = aPaymentsIntoPensionViewModel.copy(
          pensionTaxReliefNotClaimedQuestion = None, retirementAnnuityContractPaymentsQuestion = None)
        insertCyaData(pensionsUsersData(paymentsIntoPensionOnlyCYAModel(paymentsIntoPensionsViewModel)), aUserRequest)
        urlPost(fullUrl(pensionTaxReliefNotClaimedUrl(taxYearEOY)), body = validFormYes, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))

      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(retirementAnnuityUrl(taxYearEOY))
      }

      "updates retirement annuity contract payments question to Some(true)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.paymentsIntoPension.pensionTaxReliefNotClaimedQuestion shouldBe Some(true)
      }
    }

    "redirect to CYA page when user submits a 'yes' answer which completes CYA model and updates the session value to yes" which {

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val paymentsIntoPensionsViewModel = aPaymentsIntoPensionViewModel.copy(pensionTaxReliefNotClaimedQuestion = None)
        insertCyaData(pensionsUsersData(paymentsIntoPensionOnlyCYAModel(paymentsIntoPensionsViewModel)), aUserRequest)
        urlPost(fullUrl(pensionTaxReliefNotClaimedUrl(taxYearEOY)), body = validFormYes, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))

      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(checkPaymentsIntoPensionCyaUrl(taxYearEOY))
      }

      "updates retirement annuity contract payments question to Some(true)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.paymentsIntoPension.pensionTaxReliefNotClaimedQuestion shouldBe Some(true)
      }
    }

    "redirect to CYA page when user submits a 'no' answer and updates the session value to no" which {

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val paymentsIntoPensionsViewModel = aPaymentsIntoPensionViewModel.copy(pensionTaxReliefNotClaimedQuestion = None)
        insertCyaData(pensionsUsersData(paymentsIntoPensionOnlyCYAModel(paymentsIntoPensionsViewModel)), aUserRequest)
        urlPost(fullUrl(pensionTaxReliefNotClaimedUrl(taxYearEOY)), body = validFormNo, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))

      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(checkPaymentsIntoPensionCyaUrl(taxYearEOY))
      }

      "updates retirement annuity contract payments question to Some(false) and clears retirements and workplace answers" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.paymentsIntoPension.pensionTaxReliefNotClaimedQuestion shouldBe Some(false)
        cyaModel.pensions.paymentsIntoPension.retirementAnnuityContractPaymentsQuestion shouldBe None
        cyaModel.pensions.paymentsIntoPension.totalRetirementAnnuityContractPayments shouldBe None
        cyaModel.pensions.paymentsIntoPension.workplacePensionPaymentsQuestion shouldBe None
        cyaModel.pensions.paymentsIntoPension.totalWorkplacePensionPayments shouldBe None
      }
    }

  }
}
