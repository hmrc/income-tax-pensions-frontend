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

package controllers.pensions.paymentsIntoOverseasPensions

import builders.PaymentsIntoOverseasPensionsViewModelBuilder.aPaymentsIntoOverseasPensionsViewModel
import builders.PensionsUserDataBuilder.{aPensionsUserData, anPensionsUserDataEmptyCya, pensionUserDataWithOverseasPensions}
import builders.UserBuilder.{aUser, aUserRequest}
import forms.PensionCustomerReferenceNumberForm
import models.pension.charges.Relief
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.libs.ws.WSResponse
import utils.{CommonUtils, IntegrationTest, PensionsDatabaseHelper, ViewHelpers}
import utils.PageUrls.{PaymentIntoOverseasPensions, pensionSummaryUrl}
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import models.mongo.{PensionsCYAModel, PensionsUserData}
import builders.PensionsUserDataBuilder
import builders.IncomeFromPensionsViewModelBuilder.anIncomeFromPensionsViewModel
import builders.AllPensionsDataBuilder.anAllPensionsData
import builders.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import builders.PensionsCYAModelBuilder._
import builders.PensionsUserDataBuilder
import builders.StateBenefitViewModelBuilder.anStateBenefitViewModelOne
import builders.UserBuilder._
import forms.YesNoForm
import forms.RadioButtonAmountForm
import models.mongo.{PensionsCYAModel, PensionsUserData}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.data.Form
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.IncomeFromPensionsPages.{statePension, statePensionLumpSumUrl, ukPensionSchemePayments}
import utils.PageUrls.IncomeFromPensionsPages.statePension
import utils.PageUrls._
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

class PensionsCustomerReferenceNumberControllerISpec extends IntegrationTest with ViewHelpers with BeforeAndAfterEach with PensionsDatabaseHelper {

  override val userScenarios: Seq[UserScenario[_,_]] = Seq.empty
  val inputName: String = "pensionsCustomerReferenceNumberId"
  implicit val pensionCustomerReferenceNumberUrl: (Int, Int) => String = (taxYear: Int, index: Int) => PaymentIntoOverseasPensions.pensionCustomerReferenceNumberUrl(taxYear, index)
  private def pensionsUsersData(pensionsCyaModel: PensionsCYAModel): PensionsUserData = {
    PensionsUserDataBuilder.aPensionsUserData.copy(
      isPriorSubmission = false,
      pensions = pensionsCyaModel
    )
  }

  ".show" should {

    "return Ok response with Customer Reference Number page if there is no data" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        insertCyaData(pensionsUsersData(aPensionsCYAModel), aUserRequest)
        urlGet(fullUrl(pensionCustomerReferenceNumberUrl(taxYearEOY, 0)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
        )
      }
      result.status shouldBe OK
    }

    "return Ok with Customer Reference Number page when user data exists" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        insertCyaData(pensionsUsersData(aPensionsCYAModel), aUserRequest)
        userDataStub(anIncomeTaxUserData.copy(pensions = Some(anAllPensionsData)), nino, taxYearEOY)
        urlGet(fullUrl(pensionCustomerReferenceNumberUrl(taxYearEOY, 1)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
        )
      }
      result.status shouldBe OK
    }

    "Redirect to the pension summary page if index is not valid" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        userDataStub(anIncomeTaxUserData.copy(pensions = Some(anAllPensionsData)), nino, taxYearEOY)
        urlGet(fullUrl(pensionCustomerReferenceNumberUrl(taxYearEOY, -1)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
        )
      }
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(overseasPensionsSummaryUrl(taxYearEOY))
    }
  }

  ".submit" should {

    "Redirect to CRN page and add to user data when user submits a valid CRN with no prior data" in {
      //todo - redirect to untaxed-employer-payments SASS-3099
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        val form: Map[String, String] = Map(PensionCustomerReferenceNumberForm.pensionsCustomerReferenceNumberId -> "PENSIONSINCOME25")
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy()), aUserRequest)
        urlPost(fullUrl(pensionCustomerReferenceNumberUrl(taxYearEOY, 0)), body = form,
          follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      result.status shouldBe SEE_OTHER
      result.header("location").contains(pensionCustomerReferenceNumberUrl(taxYearEOY, 0)) shouldBe true

      lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
      cyaModel.pensions.paymentsIntoOverseasPensions.reliefs.head.customerReferenceNumberQuestion.get shouldBe "PENSIONSINCOME25"
    }

//    "Redirect to CRN page and update user data when user submits a valid CRN with prior data" in {
//      //todo - redirect to untaxed-employer-payments SASS-3099
//      implicit val result: WSResponse = {
//        dropPensionsDB()
//        authoriseAgentOrIndividual(aUser.isAgent)
//        val relief = Relief(customerReferenceNumberQuestion = Some("PENSIONSINCOME480"))
//        val pensionsViewModel = aPaymentsIntoOverseasPensionsViewModel.copy(reliefs = Seq(relief))
//        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(paymentsIntoOverseasPensions = pensionsViewModel)), aUserRequest)
//        val form: Map[String, String] = Map(PensionCustomerReferenceNumberForm.pensionsCustomerReferenceNumberId -> "PENSIONSINCOME24")
//        urlPost(fullUrl(pensionCustomerReferenceNumberUrl(taxYearEOY, 5)), body = form,
//          follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
//      }
//
//      result.status shouldBe SEE_OTHER
//      result.header("location").contains(pensionCustomerReferenceNumberUrl(taxYearEOY, 5)) shouldBe true
//
//      lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
//      cyaModel.pensions.paymentsIntoOverseasPensions.reliefs.head.customerReferenceNumberQuestion.get shouldBe "PENSIONSINCOME24"
//    }

    "return BadRequest error when an empty CRN value is submitted" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        lazy val form: Map[String, String] = Map(PensionCustomerReferenceNumberForm.pensionsCustomerReferenceNumberId -> "")
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy()), aUserRequest)
        urlPost(
          fullUrl(pensionCustomerReferenceNumberUrl(taxYearEOY, 0)),
          body = form,
          follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
        )
      }

      result.status shouldBe BAD_REQUEST
    }
  }
}