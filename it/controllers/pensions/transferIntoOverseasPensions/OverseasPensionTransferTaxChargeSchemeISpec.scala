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
import builders.PensionsUserDataBuilder
import builders.TransfersIntoOverseasPensionsViewModelBuilder.aTransfersIntoOverseasPensionsViewModel
import builders.UserBuilder.aUser
import forms.RadioButtonAmountForm
import models.mongo.{PensionsCYAModel, PensionsUserData}
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.TransferIntoOverseasPensions.{checkYourDetailsPensionUrl, overseasTransferChargePaidUrl, transferPensionSchemeTaxUrl}
import utils.PageUrls.fullUrl
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}


class OverseasPensionTransferTaxChargeSchemeISpec
  extends IntegrationTest with ViewHelpers with PensionsDatabaseHelper { //scalastyle:off magic.number

  private def pensionsUsersData(pensionsCyaModel: PensionsCYAModel): PensionsUserData = {
    PensionsUserDataBuilder.aPensionsUserData.copy(isPriorSubmission = false, pensions = pensionsCyaModel)
  }

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  ".show" should {
    "show page when EOY" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        insertCyaData(pensionsUsersData(aPensionsCYAModel))
        urlGet(fullUrl(transferPensionSchemeTaxUrl(taxYearEOY)), !aUser.isAgent, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }
      result.status shouldBe OK
    }

  }

  ".submit" should {
    "persist amount and redirect to CYA page when user selects 'yes' and submission data is now complete" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        val transferViewModel = aTransfersIntoOverseasPensionsViewModel.copy(
          pensionSchemeTransferChargeAmount = Some(100), pensionSchemeTransferCharge = Some(true))
        val formData = Map(RadioButtonAmountForm.yesNo -> "true", RadioButtonAmountForm.amount2 -> "100")
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(transfersIntoOverseasPensions = transferViewModel)))
        urlPost(
          fullUrl(transferPensionSchemeTaxUrl(taxYearEOY)),
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)),
          follow = false,
          body = formData)
      }
      result.status shouldBe SEE_OTHER
      result.header("location").head shouldBe checkYourDetailsPensionUrl(taxYearEOY)
    }

    "persist amount and redirect to overseas transfer charge paid page when user selects 'yes' and there is no transferPensionScheme" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        val transferViewModel = aTransfersIntoOverseasPensionsViewModel.copy(
          pensionSchemeTransferChargeAmount = Some(100),
          pensionSchemeTransferCharge = Some(true),
          transferPensionScheme = Nil
        )
        val formData = Map(RadioButtonAmountForm.yesNo -> "true", RadioButtonAmountForm.amount2 -> "100")
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(transfersIntoOverseasPensions = transferViewModel)))
        urlPost(
          fullUrl(transferPensionSchemeTaxUrl(taxYearEOY)),
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)),
          follow = false,
          body = formData)
      }
      result.status shouldBe SEE_OTHER
      result.header("location").contains(overseasTransferChargePaidUrl(taxYearEOY)) shouldBe true
    }

    "persist amount and redirect to  page when user selects 'no'" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        val transferViewModel = aTransfersIntoOverseasPensionsViewModel.copy(
          pensionSchemeTransferChargeAmount = Some(100),
          pensionSchemeTransferCharge = Some(true),
          transferPensionScheme = Nil
        )
        val formData = Map(RadioButtonAmountForm.yesNo -> "false")
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(transfersIntoOverseasPensions = transferViewModel)))
        urlPost(
          fullUrl(transferPensionSchemeTaxUrl(taxYearEOY)),
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)),
          follow = false,
          body = formData)
      }
      result.status shouldBe SEE_OTHER
      result.header("location").contains(checkYourDetailsPensionUrl(taxYearEOY)) shouldBe true
    }

    "return an error when form is submitted with no entry" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        val transferViewModel = aTransfersIntoOverseasPensionsViewModel.copy(
          pensionSchemeTransferChargeAmount = Some(100), pensionSchemeTransferCharge = Some(true))
        lazy val form: Map[String, String] = Map(RadioButtonAmountForm.yesNo -> "")
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(transfersIntoOverseasPensions = transferViewModel)))
        urlPost(
          fullUrl(transferPensionSchemeTaxUrl(taxYearEOY)),
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)),
          follow = false,
          body = form)
      }
      "status is bad request" in {
        result.status shouldBe BAD_REQUEST
      }
    }

    "return an error when form is submitted with the wrong format" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        val transferViewModel = aTransfersIntoOverseasPensionsViewModel.copy(
          pensionSchemeTransferChargeAmount = Some(100), pensionSchemeTransferCharge = Some(true))
        lazy val form: Map[String, String] = Map(RadioButtonAmountForm.yesNo -> "true", RadioButtonAmountForm.amount2 -> "dhjsvjdsg")
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(transfersIntoOverseasPensions = transferViewModel)))
        urlPost(
          fullUrl(transferPensionSchemeTaxUrl(taxYearEOY)),
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)),
          follow = false,
          body = form)
      }
      "status is bad request" in {
        result.status shouldBe BAD_REQUEST
      }
    }
  }
}
