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

package controllers.pensions.transferIntoOverseasPension

import builders.PensionsCYAModelBuilder.aPensionsCYAModel
import builders.PensionsUserDataBuilder
import builders.TransfersIntoOverseasPensionsViewModelBuilder.aTransfersIntoOverseasPensionsViewModel
import builders.UserBuilder.{aUser, aUserRequest}
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}
import forms.RadioButtonAmountForm.{amount2, yesNo}
import forms.{RadioButtonAmountForm}
import models.mongo.PensionsCYAModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import utils.PageUrls.PensionAnnualAllowancePages.transferPensionSchemeTaxUrl
import utils.PageUrls.{fullUrl, overviewUrl}


class OverseasPensionTransferTaxChargeSchemeISpec
  extends IntegrationTest with ViewHelpers with PensionsDatabaseHelper{

  private def pensionsUsersData(isPrior: Boolean, pensionsCyaModel: PensionsCYAModel) = {
    PensionsUserDataBuilder.aPensionsUserData.copy(isPriorSubmission = isPrior, pensions = pensionsCyaModel)
  }
  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  ".show" should {
      "redirect to Overview Page when in year" in {
        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual(aUser.isAgent)
          val transferViewModel = aTransfersIntoOverseasPensionsViewModel.copy(pensionSchemeTransferChargeAmount = None, pensionSchemeTransferCharge = None)
          insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(transfersIntoOverseasPensions = transferViewModel)), aUserRequest)
          urlGet(fullUrl(transferPensionSchemeTaxUrl(taxYear)), !aUser.isAgent, follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)))
        }

        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe overviewUrl(taxYear)
      }

      "show page when EOY" in {
        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual(aUser.isAgent)
          insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel), aUserRequest)
          urlGet(fullUrl(transferPensionSchemeTaxUrl(taxYearEOY)), !aUser.isAgent, follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }
        result.status shouldBe OK
      }

      }

      ".submit" should {
        "redirect to overview when in year" in {
          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(aUser.isAgent)
            val formData = Map(s" $yesNo -> true, $amount2" -> "100")
            val transferViewModel = aTransfersIntoOverseasPensionsViewModel.copy(pensionSchemeTransferChargeAmount = None, pensionSchemeTransferCharge = None)
            insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(transfersIntoOverseasPensions = transferViewModel)), aUserRequest)
            urlPost(
              fullUrl(transferPensionSchemeTaxUrl(taxYear)),
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)),
              follow = false,
              body = formData)
          }
          result.status shouldBe SEE_OTHER
          result.headers("location").head shouldBe overviewUrl(taxYear)
        }

        "persist amount and redirect to next page" in {
          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(aUser.isAgent)
            val transferViewModel = aTransfersIntoOverseasPensionsViewModel.copy(
              pensionSchemeTransferChargeAmount = Some(100), pensionSchemeTransferCharge = Some(true))
            val formData = Map(RadioButtonAmountForm.yesNo -> "true", RadioButtonAmountForm.amount2 -> "100")
            insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(transfersIntoOverseasPensions = transferViewModel)), aUserRequest)
            urlPost(
              fullUrl(transferPensionSchemeTaxUrl(taxYearEOY)),
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)),
              follow = false,
              body = formData)
          }
          result.status shouldBe SEE_OTHER
        }

        "return an error when form is submitted with no entry" which {
          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(aUser.isAgent)
            val transferViewModel = aTransfersIntoOverseasPensionsViewModel.copy(
              pensionSchemeTransferChargeAmount = Some(100), pensionSchemeTransferCharge = Some(true))
            lazy val form: Map[String, String] = Map(RadioButtonAmountForm.yesNo -> "")
            insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(transfersIntoOverseasPensions = transferViewModel)), aUserRequest)
            urlPost(
              fullUrl(transferPensionSchemeTaxUrl(taxYearEOY)),
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)),
              follow = false,
              body = form)
          }
          "status is bad request" in {
            result.status shouldBe BAD_REQUEST
          }
          implicit def document: () => Document = () => Jsoup.parse(result.body)
          errorSummaryCheck("Select yes if your pension schemes paid tax on the amount on which you paid an overseas transfer charge", "#value")
        }

        "return an error when form is submitted with the wrong format" which {
          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(aUser.isAgent)
            val transferViewModel = aTransfersIntoOverseasPensionsViewModel.copy(
              pensionSchemeTransferChargeAmount = Some(100), pensionSchemeTransferCharge = Some(true))
            lazy val form: Map[String, String] = Map(RadioButtonAmountForm.yesNo -> "true", RadioButtonAmountForm.amount2 -> "dhjsvjdsg")
            insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(transfersIntoOverseasPensions = transferViewModel)), aUserRequest)
            urlPost(
              fullUrl(transferPensionSchemeTaxUrl(taxYearEOY)),
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)),
              follow = false,
              body = form)
          }
         "status is bad request" in {
           result.status shouldBe BAD_REQUEST
         }
          implicit def document: () => Document = () => Jsoup.parse(result.body)
          errorSummaryCheck("Enter the tax paid on the amount on which you paid an overseas transfer charge in the correct format", "#amount-2")
        }

      }
}
