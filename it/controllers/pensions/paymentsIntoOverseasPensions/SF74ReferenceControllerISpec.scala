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
import builders.PensionsCYAModelBuilder.aPensionsCYAModel
import builders.PensionsUserDataBuilder
import builders.PensionsUserDataBuilder.{aPensionsUserData, pensionUserDataWithPaymentsIntoOverseasPensions, pensionUserDataWithOverseasPensions}
import builders.ReliefBuilder.aTransitionalCorrespondingRelief
import forms.SF74ReferenceForm
import models.mongo.{PensionsCYAModel, PensionsUserData}
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.PaymentIntoOverseasPensions._
import utils.PageUrls.{fullUrl, pensionSummaryUrl}
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

class SF74ReferenceControllerISpec extends IntegrationTest with BeforeAndAfterEach with ViewHelpers with PensionsDatabaseHelper { // scalastyle:off magic.number

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  private def pensionsUsersData(pensionsCyaModel: PensionsCYAModel): PensionsUserData =
    PensionsUserDataBuilder.aPensionsUserData.copy(
      isPriorSubmission = false,
      pensions = pensionsCyaModel
    )

  val schemeIndex0   = 0
  val schemeIndex100 = 100

  ".show" should {
    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "render the 'sf74 reference' page " which {
          val pensionsViewModel = aPaymentsIntoOverseasPensionsViewModel.copy(
            reliefs = Seq(aPaymentsIntoOverseasPensionsViewModel.reliefs.head.copy(sf74Reference = Some("1234567"))))

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropPensionsDB()
            val viewModel = pensionsViewModel
            insertCyaData(pensionUserDataWithOverseasPensions(viewModel))
            urlGet(
              fullUrl(sf74ReferenceUrl(taxYearEOY, schemeIndex0)),
              user.isWelsh,
              follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
            )
          }
          "has an OK status" in {
            result.status shouldBe OK
          }
        }
      }
    }
    "redirect to the first page in scheme loop if the index doesn't match and there are NO pension schemes" should {
      val pensionsNoSchemesViewModel =
        aPaymentsIntoOverseasPensionsViewModel.copy(reliefs = Seq(aTransitionalCorrespondingRelief.copy(sf74Reference = None)))

      lazy val result: WSResponse = {
        dropPensionsDB()
        insertCyaData(pensionUserDataWithPaymentsIntoOverseasPensions(pensionsNoSchemesViewModel))
        authoriseAgentOrIndividual()
        urlGet(
          fullUrl(sf74ReferenceUrl(taxYearEOY, 100)),
          follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }
      "have a SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionCustomerReferenceNumberUrl(taxYearEOY, None))
      }

    }

    "redirect to the pension relief scheme summary page if the index doesn't match and there are pension schemes" should {
      lazy val result: WSResponse = {
        dropPensionsDB()
        insertCyaData(aPensionsUserData)
        authoriseAgentOrIndividual()
        urlGet(
          fullUrl(sf74ReferenceUrl(taxYearEOY, 100)),
          follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }
      "have a SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionReliefSchemeSummaryUrl(taxYearEOY))
      }

    }
  }

  ".submit" should {
    "redirect to the pensions scheme details page " should {
      val form: Map[String, String] = Map(SF74ReferenceForm.sf74ReferenceId -> "1234567")
      lazy val result: WSResponse = {
        dropPensionsDB()
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(paymentsIntoOverseasPensions = aPaymentsIntoOverseasPensionsViewModel)))
        authoriseAgentOrIndividual()
        urlPost(
          fullUrl(sf74ReferenceUrl(taxYearEOY, 0)),
          body = form,
          follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }
      "have a SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionReliefSchemeDetailsUrl(taxYearEOY, 0))
      }
    }

    "redirect to the the first page in scheme loop if the index doesn't match and there are no complete relief schemes" should {
      val pensionsNoSchemesViewModel =
        aPaymentsIntoOverseasPensionsViewModel.copy(reliefs = Seq(aTransitionalCorrespondingRelief.copy(sf74Reference = None)))
      val form: Map[String, String] = Map(SF74ReferenceForm.sf74ReferenceId -> "1234567")

      lazy val result: WSResponse = {
        dropPensionsDB()
        insertCyaData(pensionUserDataWithPaymentsIntoOverseasPensions(pensionsNoSchemesViewModel))
        authoriseAgentOrIndividual()
        urlPost(
          fullUrl(sf74ReferenceUrl(taxYearEOY, 100)),
          body = form,
          follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }
      "have a SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionCustomerReferenceNumberUrl(taxYearEOY, None))
      }
    }

    "redirect to the pensions scheme summary page if the index doesn't match and there are pension schemes" should {
      val form: Map[String, String] = Map(SF74ReferenceForm.sf74ReferenceId -> "1234567")
      lazy val result: WSResponse = {
        dropPensionsDB()
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(paymentsIntoOverseasPensions = aPaymentsIntoOverseasPensionsViewModel)))
        authoriseAgentOrIndividual()
        urlPost(
          fullUrl(sf74ReferenceUrl(taxYearEOY, 100)),
          body = form,
          follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }
      "have a SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionReliefSchemeSummaryUrl(taxYearEOY))
      }
    }
  }
}
