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
import utils.PageUrls.PaymentIntoPensions._
import utils.PageUrls.fullUrl
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}
import views.pensions.paymentsIntoPensions.ReliefAtSourceOneOffPaymentsSpec.CommonExpectedEN._
import views.pensions.paymentsIntoPensions.ReliefAtSourceOneOffPaymentsSpec.ExpectedIndividualEN._
import views.pensions.paymentsIntoPensions.ReliefAtSourceOneOffPaymentsSpec.Selectors
import views.pensions.paymentsIntoPensions.ReliefAtSourceOneOffPaymentsSpec.Selectors._

class ReliefAtSourceOneOffPaymentsControllerISpec extends IntegrationTest with ViewHelpers with BeforeAndAfterEach with PensionsDatabaseHelper {
  private val someRasAmount: BigDecimal = 33.33
  private val validFormYes: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
  private val validFormNo: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)

  private def pensionsUsersData(pensionsCyaModel: PensionsCYAModel): PensionsUserData = {
    PensionsUserDataBuilder.aPensionsUserData.copy(
      isPriorSubmission = false,
      pensions = pensionsCyaModel
    )
  }


  val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  ".show" should {

    "render the one-off payments into relief at source (RAS) pensions question page with no pre-filled radio buttons if no CYA question data" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(
          totalRASPaymentsAndTaxRelief = Some(someRasAmount),
          oneOffRasPaymentPlusTaxReliefQuestion = None)
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)))
        urlGet(
          fullUrl(
            reliefAtSourceOneOffPaymentsUrl(taxYearEOY)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an OK status" in {
        result.status shouldBe OK
      }
      implicit def document: () => Document = () => Jsoup.parse(result.body)

      titleCheck(expectedTitle)
      h1Check(expectedHeading)
      captionCheck(expectedCaption(taxYearEOY), captionSelector)
      textOnPageCheck(thisIncludes, paragraphSelector)
      radioButtonCheck(yesText, 1, checked = Some(false))
      radioButtonCheck(noText, 2, checked = Some(false))
      buttonCheck(buttonText, continueButtonSelector)
      formPostLinkCheck(reliefAtSourceOneOffPaymentsUrl(taxYearEOY), formSelector)
      welshToggleCheck(isWelsh = false)
    }

    "render the one-off payments into relief at source (RAS) pensions question page with 'Yes' pre-filled when CYA data exists" which {

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(
          totalRASPaymentsAndTaxRelief = Some(someRasAmount),
          oneOffRasPaymentPlusTaxReliefQuestion = Some(true))
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)))
        urlGet(
          fullUrl(
            reliefAtSourceOneOffPaymentsUrl(taxYearEOY)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an OK status" in {
        result.status shouldBe OK
      }

      implicit def document: () => Document = () => Jsoup.parse(result.body)
      
      titleCheck(expectedTitle)
      h1Check(expectedHeading)
      captionCheck(expectedCaption(taxYearEOY), captionSelector)
      textOnPageCheck(thisIncludes, paragraphSelector)
      radioButtonCheck(yesText, 1, checked = Some(true))
      radioButtonCheck(noText, 2, checked = Some(false))
      buttonCheck(buttonText, continueButtonSelector)
      formPostLinkCheck(reliefAtSourceOneOffPaymentsUrl(taxYearEOY), formSelector)
      welshToggleCheck(isWelsh = false)
    }

    "render the one-off payments into relief at source (RAS) pensions question page with 'No' pre-filled when CYA data exists" which {

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(
          totalRASPaymentsAndTaxRelief = Some(someRasAmount),
          oneOffRasPaymentPlusTaxReliefQuestion = Some(false))
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)))
        urlGet(
          fullUrl(
            reliefAtSourceOneOffPaymentsUrl(taxYearEOY)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an OK status" in {
        result.status shouldBe OK
      }

      implicit def document: () => Document = () => Jsoup.parse(result.body)
      titleCheck(expectedTitle)
      h1Check(expectedHeading)
      captionCheck(expectedCaption(taxYearEOY), captionSelector)
      textOnPageCheck(thisIncludes, paragraphSelector)
      radioButtonCheck(yesText, 1, checked = Some(false))
      radioButtonCheck(noText, 2, checked = Some(true))
      buttonCheck(buttonText, continueButtonSelector)
      formPostLinkCheck(reliefAtSourceOneOffPaymentsUrl(taxYearEOY), formSelector)
      welshToggleCheck(isWelsh = false)
    }

    "redirect to the ReliefAtSourcePensions question page if the previous totalRASPaymentsAndTaxRelief Amount has not been populated" which {

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(
          totalRASPaymentsAndTaxRelief = None,
          oneOffRasPaymentPlusTaxReliefQuestion = None)
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)))
        urlGet(
          fullUrl(
            reliefAtSourceOneOffPaymentsUrl(taxYearEOY)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(reliefAtSourcePensionsUrl(taxYearEOY)) shouldBe true
      }

    }

    "redirect to the CYA page if there is no session data" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        // no cya insert
        urlGet(fullUrl(reliefAtSourceOneOffPaymentsUrl(taxYearEOY)), follow = false,
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
      lazy val invalidForm: Map[String, String] = Map(YesNoForm.yesNo -> "")

      lazy val result: WSResponse = {
        dropPensionsDB()
        val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(
          totalRASPaymentsAndTaxRelief = Some(someRasAmount),
          oneOffRasPaymentPlusTaxReliefQuestion = None)
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)))
        authoriseAgentOrIndividual()
        urlPost(fullUrl(reliefAtSourceOneOffPaymentsUrl(taxYearEOY)), body = invalidForm, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has the correct status" in {
        result.status shouldBe BAD_REQUEST
      }

      implicit def document: () => Document = () => Jsoup.parse(result.body)

      titleCheck(expectedErrorTitle)
      h1Check(expectedHeading)
      captionCheck(expectedCaption(taxYearEOY), captionSelector)
      textOnPageCheck(thisIncludes, paragraphSelector)
      radioButtonCheck(yesText, 1, checked = Some(false))
      radioButtonCheck(noText, 2, checked = Some(false))
      buttonCheck(buttonText, continueButtonSelector)
      formPostLinkCheck(reliefAtSourceOneOffPaymentsUrl(taxYearEOY), formSelector)
      welshToggleCheck(isWelsh = false)
      errorSummaryCheck(expectedErrorMessage, Selectors.yesSelector)
      errorAboveElementCheck(expectedErrorMessage, Some("value"))

    }

    "redirect to the next page when user submits a 'yes' answer and updates the session value to yes" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(
          totalOneOffRasPaymentPlusTaxRelief = Some(someRasAmount),
          totalRASPaymentsAndTaxRelief = Some(someRasAmount),
          oneOffRasPaymentPlusTaxReliefQuestion = None,
          workplacePensionPaymentsQuestion = None)
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)))

        urlPost(fullUrl(reliefAtSourceOneOffPaymentsUrl(taxYearEOY)), body = validFormYes, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))

      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(oneOffReliefAtSourcePaymentsAmountUrl(taxYearEOY))
      }

      "updates OffRasPaymentPlusTaxReliefQuestion to Some(true) and not clear the totalOneOffRasPaymentPlusTaxRelief amount" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.paymentsIntoPension.oneOffRasPaymentPlusTaxReliefQuestion shouldBe Some(true)
        cyaModel.pensions.paymentsIntoPension.totalRASPaymentsAndTaxRelief shouldBe Some(someRasAmount)
        cyaModel.pensions.paymentsIntoPension.totalOneOffRasPaymentPlusTaxRelief shouldBe Some(someRasAmount)

      }
    }

    "redirect to correct page when user submits a 'no' answer and updates the session value to no" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()

        val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(
          totalOneOffRasPaymentPlusTaxRelief = Some(someRasAmount),
          totalRASPaymentsAndTaxRelief = Some(someRasAmount),
          oneOffRasPaymentPlusTaxReliefQuestion = None,
          workplacePensionPaymentsQuestion = None)

        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)))

        urlPost(fullUrl(reliefAtSourceOneOffPaymentsUrl(taxYearEOY)), body = validFormNo, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(totalPaymentsIntoRASUrl(taxYearEOY))
      }

      "updates OffRasPaymentPlusTaxReliefQuestion question to Some(false) and clear the totalOneOffRasPaymentPlusTaxRelief amount" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.paymentsIntoPension.oneOffRasPaymentPlusTaxReliefQuestion shouldBe Some(false)
        cyaModel.pensions.paymentsIntoPension.totalRASPaymentsAndTaxRelief shouldBe Some(someRasAmount)
        cyaModel.pensions.paymentsIntoPension.totalOneOffRasPaymentPlusTaxRelief shouldBe None

      }
    }

    "redirect to the CYA page if there is no session data" which {

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        // no cya insert
        urlPost(fullUrl(reliefAtSourceOneOffPaymentsUrl(taxYearEOY)), body = validFormYes, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(checkPaymentsIntoPensionCyaUrl(taxYearEOY)) shouldBe true
      }

    }

    "redirect to the ReliefAtSourcePensions question page if the previous totalRASPaymentsAndTaxRelief amount has not been populated" which {

      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        val pensionsViewModel = aPaymentsIntoPensionViewModel.copy(
          totalRASPaymentsAndTaxRelief = None,
          oneOffRasPaymentPlusTaxReliefQuestion = Some(true))
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(paymentsIntoPension = pensionsViewModel)))
        urlPost(fullUrl(reliefAtSourceOneOffPaymentsUrl(taxYearEOY)), body = validFormYes, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }


      "has an SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location").contains(reliefAtSourcePensionsUrl(taxYearEOY)) shouldBe true
      }

    }

  }
}
