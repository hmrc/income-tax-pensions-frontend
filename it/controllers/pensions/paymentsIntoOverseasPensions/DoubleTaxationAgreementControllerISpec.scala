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

import builders.AllPensionsDataBuilder.anAllPensionsData
import builders.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import builders.PaymentsIntoOverseasPensionsViewModelBuilder.aPaymentsIntoOverseasPensionsViewModel
import builders.PensionsCYAModelBuilder.aPensionsCYAModel
import builders.PensionsUserDataBuilder
import builders.UserBuilder.aUserRequest
import models.mongo.{PensionsCYAModel, PensionsUserData}
import play.api.http.HeaderNames
import play.api.libs.ws.WSResponse
import utils.PageUrls.PaymentIntoOverseasPensions.doubleTaxationAgreementUrl
import utils.PageUrls.{fullUrl, overseasPensionsSummaryUrl}
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}
import play.api.http.Status.{OK, SEE_OTHER, BAD_REQUEST}

class DoubleTaxationAgreementControllerISpec extends
  IntegrationTest with ViewHelpers with PensionsDatabaseHelper {

  val articleIF = "article"
  val treatyIF = "treaty"
  val amountIF = "amount-2"
  val countryIF = "countryId"

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  private def pensionsUsersData(pensionsCyaModel: PensionsCYAModel): PensionsUserData = {
    PensionsUserDataBuilder.aPensionsUserData.copy(
      isPriorSubmission = false,
      pensions = pensionsCyaModel
    )
  }

  ".show" should {

    "redirect to the pensions summary page if there is no session data" in {
      implicit val doubleTaxAgrtUrl : Int => String = doubleTaxationAgreementUrl(0)
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(doubleTaxAgrtUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(overseasPensionsSummaryUrl(taxYearEOY))
    }

    "show page when there is data " in {
      implicit val doubleTaxAgrtUrl : Int => String = doubleTaxationAgreementUrl(0)
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(pensionsUsersData(aPensionsCYAModel), aUserRequest)
        userDataStub(anIncomeTaxUserData.copy(pensions = Some(anAllPensionsData)), nino, taxYearEOY)
        urlGet(fullUrl(doubleTaxAgrtUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      result.status shouldBe OK
    }

    "show page when wrong tax year is added added " in {
      implicit val doubleTaxAgrtUrl : Int => String = doubleTaxationAgreementUrl(0)
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(pensionsUsersData(aPensionsCYAModel), aUserRequest)
        userDataStub(anIncomeTaxUserData.copy(pensions = Some(anAllPensionsData)), nino, taxYearEOY)
        urlGet(fullUrl(doubleTaxAgrtUrl(taxYear)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      result.status shouldBe SEE_OTHER
    }

    "redirect to pensions summary page when an out of bounds index is added " in {
      implicit val doubleTaxAgrtUrl : Int => String = doubleTaxationAgreementUrl(2)
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        insertCyaData(pensionsUsersData(aPensionsCYAModel), aUserRequest)
        userDataStub(anIncomeTaxUserData.copy(pensions = Some(anAllPensionsData)), nino, taxYearEOY)
        urlGet(fullUrl(doubleTaxAgrtUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      result.status shouldBe SEE_OTHER
      result.header("location") shouldBe Some(overseasPensionsSummaryUrl(taxYearEOY))
    }
  }

  "submit " should {
    "redirect to the next page " in {
      implicit val doubleTaxAgrtUrl : Int => String = doubleTaxationAgreementUrl(0)
      val form: Map[String, String] = setFormData("AB3211-10", "Test Treaty", "100",Some("FR"))
      lazy val result: WSResponse = {
        dropPensionsDB()
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(paymentsIntoOverseasPensions = aPaymentsIntoOverseasPensionsViewModel)), aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(doubleTaxAgrtUrl(taxYearEOY)), body = form,
          follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }
      result.status shouldBe SEE_OTHER
      result.header("location") shouldBe Some(doubleTaxAgrtUrl(taxYearEOY)) //TODO: update once the next page is developed
    }

    "redirect when wrong tax year is used in the url " in {
      implicit val doubleTaxAgrtUrl : Int => String = doubleTaxationAgreementUrl(0)
      val form: Map[String, String] = setFormData("AB3211-10", "Test Treaty", "100",Some("FR"))
      lazy val result: WSResponse = {
        dropPensionsDB()
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(paymentsIntoOverseasPensions = aPaymentsIntoOverseasPensionsViewModel)), aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(doubleTaxAgrtUrl(taxYear)), body = form,
          follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }
      result.status shouldBe SEE_OTHER
    }

    "throw bad request when user does not complete mandatory fields " in {
      implicit val doubleTaxAgrtUrl : Int => String = doubleTaxationAgreementUrl(0)
      val form: Map[String, String] = setFormData("", "", "", None)
      lazy val result: WSResponse = {
        dropPensionsDB()
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(paymentsIntoOverseasPensions = aPaymentsIntoOverseasPensionsViewModel)), aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(doubleTaxAgrtUrl(taxYearEOY)), body = form,
          follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }
      result.status shouldBe BAD_REQUEST
    }

    "redirect to pensions summary page when an out of bounds index is added " in {
      implicit val doubleTaxAgrtUrl : Int => String = doubleTaxationAgreementUrl(2)
      val form: Map[String, String] = setFormData("AB3211-10", "Test Treaty", "100",Some("FR"))
      lazy val result: WSResponse = {
        dropPensionsDB()
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(paymentsIntoOverseasPensions = aPaymentsIntoOverseasPensionsViewModel)), aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(doubleTaxAgrtUrl(taxYearEOY)), body = form,
          follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }
      result.status shouldBe SEE_OTHER
      result.header("location") shouldBe Some(overseasPensionsSummaryUrl(taxYearEOY))
    }
  }

  def setFormData(atricleName: String, treatyName: String, amount: String, countryOpt: Option[String]): Map[String, String] =
    Map(articleIF -> atricleName, treatyIF -> treatyName, amountIF -> amount) ++
      countryOpt.fold(Map[String, String]())(cc => Map(countryIF -> cc))

}

