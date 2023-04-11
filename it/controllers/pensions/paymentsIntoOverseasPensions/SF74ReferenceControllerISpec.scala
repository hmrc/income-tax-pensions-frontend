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

import builders.PensionsUserDataBuilder.{aPensionsUserData, pensionUserDataWithOverseasPensions}
import builders.PaymentsIntoOverseasPensionsViewModelBuilder.aPaymentsIntoOverseasPensionsViewModel
import builders.PensionsCYAModelBuilder.aPensionsCYAModel
import builders.PensionsUserDataBuilder
import builders.UserBuilder.aUserRequest
import forms.SF74ReferenceForm
import models.mongo.{PensionsCYAModel, PensionsUserData}
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.libs.ws.WSResponse
import utils.PageUrls.overseasPensionsSummaryUrl
import utils.PageUrls.fullUrl
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}
import play.api.http.Status.{OK, SEE_OTHER}
import utils.PageUrls.PaymentIntoOverseasPensions.{pensionCustomerReferenceNumberUrl, sf74ReferenceUrl}


class SF74ReferenceControllerISpec extends IntegrationTest with BeforeAndAfterEach with ViewHelpers with PensionsDatabaseHelper{
  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  private def pensionsUsersData(pensionsCyaModel: PensionsCYAModel): PensionsUserData = {
    PensionsUserDataBuilder.aPensionsUserData.copy(
      isPriorSubmission = false,
      pensions = pensionsCyaModel
    )
  }


  ".show" should {
    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "render the 'sf74 reference' page " which {

          val pensionsViewModel = aPaymentsIntoOverseasPensionsViewModel.copy(
            reliefs = Seq(aPaymentsIntoOverseasPensionsViewModel.reliefs.head.copy(
              sf74Reference = Some("1234567"))))

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropPensionsDB()
            val viewModel = pensionsViewModel
            insertCyaData(pensionUserDataWithOverseasPensions(viewModel), aUserRequest)
            urlGet(fullUrl(sf74ReferenceUrl(taxYearEOY, 0)), user.isWelsh, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }
        }
      }
    }

    "redirect to the pensions summary page if there is no session data" should {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(sf74ReferenceUrl(taxYearEOY, 0)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "have a SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(overseasPensionsSummaryUrl(taxYearEOY))
      }
    }

    "redirect to the customer reference number page if the index doesn't match" should {
      lazy val result: WSResponse = {
        dropPensionsDB()
        insertCyaData(aPensionsUserData, aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlGet(fullUrl(sf74ReferenceUrl(taxYearEOY, 100)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }
      "have a SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionCustomerReferenceNumberUrl(taxYearEOY, Some(100)))
      }

  }
}

  ".submit" should {
    "redirect to the pensions scheme details page " should {
      val form: Map[String, String] = Map(SF74ReferenceForm.sf74ReferenceId -> "1234567")
      lazy val result: WSResponse = {
        dropPensionsDB()
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(paymentsIntoOverseasPensions = aPaymentsIntoOverseasPensionsViewModel)), aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(sf74ReferenceUrl(taxYearEOY, 0)), body = form,
          follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "have a SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(sf74ReferenceUrl(taxYearEOY, 0))
      }
    }

    "redirect to the pensions scheme details page if the index doesn't match" should {
      val form: Map[String, String] = Map(SF74ReferenceForm.sf74ReferenceId -> "1234567")
      lazy val result: WSResponse = {
        dropPensionsDB()
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(paymentsIntoOverseasPensions = aPaymentsIntoOverseasPensionsViewModel)), aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(sf74ReferenceUrl(taxYearEOY, 100)), body = form,
          follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "have a SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionCustomerReferenceNumberUrl(taxYearEOY, Some(100)))
      }
    }
  }
}
