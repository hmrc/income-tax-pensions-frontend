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

package controllers.pensions.paymentsIntoPensions

import builders.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import builders.PaymentsIntoPensionVewModelBuilder._
import builders.PensionsCYAModelBuilder._
import builders.PensionsUserDataBuilder
import builders.PensionsUserDataBuilder.pensionsUserDataWithPaymentsIntoPensions
import builders.UserBuilder._
import forms.YesNoForm
import models.mongo.{PensionsCYAModel, PensionsUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.PaymentIntoPensions.{checkPaymentsIntoPensionCyaUrl, retirementAnnuityUrl, workplacePensionUrl}
import utils.PageUrls._
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}
import views.RetirementAnnuitySpec.Selectors._
import views.RetirementAnnuitySpec._
import views.RetirementAnnuitySpec.CommonExpectedEN._
import views.RetirementAnnuitySpec.ExpectedIndividualEN._

class RetirementAnnuityControllerISpec extends IntegrationTest with ViewHelpers with BeforeAndAfterEach with PensionsDatabaseHelper {


  private def pensionsUsersData(pensionsCyaModel: PensionsCYAModel): PensionsUserData = {
    PensionsUserDataBuilder.aPensionsUserData.copy(
      isPriorSubmission = false,
      pensions = pensionsCyaModel
    )
  }

  val userScenarios: Seq[UserScenario[_, _]] = Seq.empty
  ".show" should {
    "render the retirement annuity contract question page with no pre-filled radio buttons" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(retirementAnnuityContractPaymentsQuestion = None)
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)), aUserRequest)
        urlGet(fullUrl(retirementAnnuityUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an OK status" in {
        result.status shouldBe OK
      }
      implicit def document: () => Document = () => Jsoup.parse(result.body)

      titleCheck(expectedTitle)
      h1Check(expectedHeading)
      captionCheck(expectedCaption(taxYearEOY), captionSelector)
      textOnPageCheck(expectedParagraphText, paragraphSelector(1))
      textOnPageCheck(expectedYouCanFindThisOut, paragraphSelector(2))
      radioButtonCheck(yesText, 1, checked = Some(false))
      radioButtonCheck(noText, 2, checked = Some(false))
      buttonCheck(buttonText, continueButtonSelector)

      textOnPageCheck(expectedDetailsTitle, detailsSelector)
      textOnPageCheck(expectedDetails, detailsParagraphSelector(1))
      formPostLinkCheck(retirementAnnuityUrl(taxYearEOY), formSelector)
      welshToggleCheck(isWelsh = false)
    }

    "render the retirement annuity contract question page with 'Yes' pre-filled when CYA data exists" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(pensionsUsersData(aPensionsCYAModel), aUserRequest)
        urlGet(fullUrl(retirementAnnuityUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }


      "has an OK status" in {
        result.status shouldBe OK
      }

      implicit def document: () => Document = () => Jsoup.parse(result.body)

      titleCheck(expectedTitle)
      h1Check(expectedHeading)
      captionCheck(expectedCaption(taxYearEOY), captionSelector)
      textOnPageCheck(expectedParagraphText, paragraphSelector(1))
      textOnPageCheck(expectedYouCanFindThisOut, paragraphSelector(2))
      radioButtonCheck(yesText, 1, checked = Some(true))
      radioButtonCheck(noText, 2, checked = Some(false))
      buttonCheck(buttonText, continueButtonSelector)

      textOnPageCheck(expectedDetailsTitle, detailsSelector)
      textOnPageCheck(expectedDetails, detailsParagraphSelector(1))
      formPostLinkCheck(retirementAnnuityUrl(taxYearEOY), formSelector)
      welshToggleCheck(isWelsh = false)
    }

    "render the retirement annuity contract question page with 'No' pre-filled and not a prior submission" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        val paymentsIntoPensionsViewModel = aPaymentsIntoPensionViewModel.copy(retirementAnnuityContractPaymentsQuestion = Some(false))
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(paymentsIntoPensionsViewModel)), aUserRequest)
        urlGet(fullUrl(retirementAnnuityUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an OK status" in {
        result.status shouldBe OK
      }

      implicit def document: () => Document = () => Jsoup.parse(result.body)

      titleCheck(expectedTitle)
      h1Check(expectedHeading)
      captionCheck(expectedCaption(taxYearEOY), captionSelector)
      textOnPageCheck(expectedParagraphText, paragraphSelector(1))
      textOnPageCheck(expectedYouCanFindThisOut, paragraphSelector(2))
      radioButtonCheck(yesText, 1, checked = Some(false))
      radioButtonCheck(noText, 2, checked = Some(true))
      buttonCheck(buttonText, continueButtonSelector)

      textOnPageCheck(expectedDetailsTitle, detailsSelector)
      textOnPageCheck(expectedDetails, detailsParagraphSelector(1))
      formPostLinkCheck(retirementAnnuityUrl(taxYearEOY), formSelector)
      welshToggleCheck(isWelsh = false)
    }

    "redirect to the CYA page if there is no session data" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        // no cya insert
        urlGet(fullUrl(retirementAnnuityUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkPaymentsIntoPensionCyaUrl(taxYearEOY)) shouldBe true
      }
    }
  }

  ".submit" should {
    "return an error when form is submitted with no entry" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> "")

      lazy val result: WSResponse = {
        dropPensionsDB()
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(
                  aPaymentsIntoPensionViewModel.copy(retirementAnnuityContractPaymentsQuestion = None))), aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(retirementAnnuityUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has the correct status" in {
        result.status shouldBe BAD_REQUEST
      }

      implicit def document: () => Document = () => Jsoup.parse(result.body)

      titleCheck(expectedErrorTitle)
      h1Check(expectedHeading)
      captionCheck(expectedCaption(taxYearEOY), captionSelector)
      textOnPageCheck(expectedParagraphText, paragraphSelector(1))
      textOnPageCheck(expectedYouCanFindThisOut, paragraphSelector(2))
      radioButtonCheck(yesText, 1, checked = Some(false))
      radioButtonCheck(noText, 2, checked = Some(false))
      buttonCheck(buttonText, continueButtonSelector)

      textOnPageCheck(expectedDetailsTitle, detailsSelector)
      textOnPageCheck(expectedDetails, detailsParagraphSelector(1))
      formPostLinkCheck(retirementAnnuityUrl(taxYearEOY), formSelector)
      welshToggleCheck(isWelsh = false)
      errorSummaryCheck(expectedErrorMessage, Selectors.yesSelector)
      errorAboveElementCheck(expectedErrorMessage, Some("value"))

    }

    "redirect to Retirement Annuity Amount page when user selects 'yes' and not a prior submission" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val paymentsIntoPensionsViewModel = aPaymentsIntoPensionViewModel.copy(
          retirementAnnuityContractPaymentsQuestion = Some(false), totalRetirementAnnuityContractPayments = None)
        insertCyaData(pensionsUsersData(paymentsIntoPensionOnlyCYAModel(paymentsIntoPensionsViewModel)), aUserRequest)
        urlPost(fullUrl(retirementAnnuityUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(controllers.pensions.paymentsIntoPensions.routes.RetirementAnnuityAmountController.show(taxYearEOY).url)
      }

      "updates retirement annuity contract payments question to Some(true)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.paymentsIntoPension.retirementAnnuityContractPaymentsQuestion shouldBe Some(true)
        cyaModel.pensions.paymentsIntoPension.totalRetirementAnnuityContractPayments shouldBe None
      }
    }

    "redirect to Workplace pension page when user selects 'no' and doesnt complete CYA model" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val paymentsIntoPensionsViewModel = aPaymentsIntoPensionViewModel.copy(
          retirementAnnuityContractPaymentsQuestion = Some(true), workplacePensionPaymentsQuestion = None)
        insertCyaData(pensionsUserDataWithPaymentsIntoPensions(paymentsIntoPensionsViewModel), aUserRequest)
        urlPost(fullUrl(retirementAnnuityUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(workplacePensionUrl(taxYearEOY))
      }

      "updates retirement annuity contract payments question to Some(false) and deletes the totalRetirementAnnuityContractPayments amount" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.paymentsIntoPension.retirementAnnuityContractPaymentsQuestion shouldBe Some(false)
        cyaModel.pensions.paymentsIntoPension.totalRetirementAnnuityContractPayments shouldBe None
      }
    }

    "redirect to CYA page when user selects 'no' which completes CYA model" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        val paymentsIntoPensionsViewModel = aPaymentsIntoPensionViewModel.copy(
          retirementAnnuityContractPaymentsQuestion = Some(true))
        insertCyaData(pensionsUserDataWithPaymentsIntoPensions(paymentsIntoPensionsViewModel), aUserRequest)
        urlPost(fullUrl(retirementAnnuityUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(checkPaymentsIntoPensionCyaUrl(taxYearEOY))
      }

      "updates retirement annuity contract payments question to Some(false) and deletes the totalRetirementAnnuityContractPayments amount" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.paymentsIntoPension.retirementAnnuityContractPaymentsQuestion shouldBe Some(false)
        cyaModel.pensions.paymentsIntoPension.totalRetirementAnnuityContractPayments shouldBe None
      }
    }
  }
}
