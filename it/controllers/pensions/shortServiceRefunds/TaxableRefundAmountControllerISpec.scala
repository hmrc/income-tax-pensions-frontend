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

package controllers.pensions.shortServiceRefunds

import builders.PensionsCYAModelBuilder.aPensionsCYAModel
import builders.PensionsUserDataBuilder
import builders.ShortServiceRefundsViewModelBuilder.{aShortServiceRefundsViewModel, emptyShortServiceRefundsViewModel}
import builders.UserBuilder.{aUser, aUserRequest}
import forms.RadioButtonAmountForm
import forms.RadioButtonAmountForm.{amount2, yesNo}
import models.mongo.PensionsCYAModel
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.ShortServiceRefunds.{nonUkTaxRefundsUrl, shortServiceRefundsCYAUrl, shortServiceTaxableRefundUrl}
import utils.PageUrls.{fullUrl, overviewUrl}
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}


class TaxableRefundAmountControllerISpec
  extends IntegrationTest with ViewHelpers with PensionsDatabaseHelper {

  private def pensionsUsersData(isPrior: Boolean, pensionsCyaModel: PensionsCYAModel) = {
    PensionsUserDataBuilder.aPensionsUserData.copy(isPriorSubmission = isPrior, pensions = pensionsCyaModel)
  }

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  ".show" should {
    "redirect to Overview Page when in year" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel), aUserRequest)
        urlGet(fullUrl(shortServiceTaxableRefundUrl(taxYear)), !aUser.isAgent, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)))
      }

      result.status shouldBe SEE_OTHER
      result.headers("Location").head shouldBe overviewUrl(taxYear)
    }

    "show page when EOY" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel), aUserRequest)
        urlGet(fullUrl(shortServiceTaxableRefundUrl(taxYearEOY)), !aUser.isAgent, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }
      result.status shouldBe OK
    }

  }

  ".submit" should {
    "redirect to overview when in year" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        val formData = Map(s" $yesNo -> true, $amount2" -> "100")
        insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(shortServiceRefunds = emptyShortServiceRefundsViewModel)), aUserRequest)
        urlPost(
          fullUrl(shortServiceTaxableRefundUrl(taxYear)),
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)),
          follow = false,
          body = formData)
      }
      result.status shouldBe SEE_OTHER
      result.headers("location").head shouldBe overviewUrl(taxYear)
    }

    "persist amount and redirect to next page when user selects yes" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        val shortServiceRefundViewModel = aShortServiceRefundsViewModel.copy(
          shortServiceRefundCharge = Some(BigDecimal("100")), shortServiceRefund = Some(true))
        val formData = Map(RadioButtonAmountForm.yesNo -> "true", RadioButtonAmountForm.amount2 -> "100")
        insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(shortServiceRefunds = shortServiceRefundViewModel)), aUserRequest)
        urlPost(
          fullUrl(shortServiceTaxableRefundUrl(taxYearEOY)),
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)),
          follow = false,
          body = formData)
      }
      result.status shouldBe SEE_OTHER
      result.headers("location").head shouldBe nonUkTaxRefundsUrl(taxYearEOY)
    }

    "persist amount and redirect to next page when user selects no" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        val shortServiceRefundViewModel = aShortServiceRefundsViewModel.copy(
          shortServiceRefundCharge = None, shortServiceRefund = Some(false))
        val formData = Map(RadioButtonAmountForm.yesNo -> "false")
        insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(shortServiceRefunds = shortServiceRefundViewModel)), aUserRequest)
        urlPost(
          fullUrl(shortServiceTaxableRefundUrl(taxYearEOY)),
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)),
          follow = false,
          body = formData)
      }
      result.status shouldBe SEE_OTHER
      result.headers("location").head shouldBe shortServiceRefundsCYAUrl(taxYearEOY)
    }

    //TODO: A bit confusing what these IT tests are doing. tests are saving values directly in DB using `insertCyaData`
    //That shouldn't be the case or if its saved directly then IT tests should assert values what urlPost has done,
    //because insertCyaData is unvalidated data saved that has not gone through service logic
    "clears shortServiceRefunds and redirect to next page when user selects no" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        val shortServiceRefundViewModel = aShortServiceRefundsViewModel
        val formData = Map(RadioButtonAmountForm.yesNo -> "false")
        insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(shortServiceRefunds = shortServiceRefundViewModel)), aUserRequest)
        urlPost(
          fullUrl(shortServiceTaxableRefundUrl(taxYearEOY)),
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)),
          follow = false,
          body = formData)
      }
      result.status shouldBe SEE_OTHER
      result.headers("location").head shouldBe shortServiceRefundsCYAUrl(taxYearEOY)
      //TODO: We should verify that data saved is what we expect in this case Line:130 `aShortServiceRefundsViewModel`
      //should be become empty as we submitted with false
    }

    "return an error when form is submitted with no entry" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        val shortServiceRefundViewModel = aShortServiceRefundsViewModel.copy(
          shortServiceRefundCharge = Some(BigDecimal("100")), shortServiceRefund = Some(true))
        val form = Map(RadioButtonAmountForm.yesNo -> "", RadioButtonAmountForm.amount2 -> "")
        insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(shortServiceRefunds = shortServiceRefundViewModel)), aUserRequest)
        urlPost(
          fullUrl(shortServiceTaxableRefundUrl(taxYearEOY)),
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
        val shortServiceRefundViewModel = aShortServiceRefundsViewModel.copy(
          shortServiceRefundCharge = Some(BigDecimal("100")), shortServiceRefund = Some(true))
        val form = Map(RadioButtonAmountForm.yesNo -> "true", RadioButtonAmountForm.amount2 -> "jhvgfxk")
        insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel.copy(shortServiceRefunds = shortServiceRefundViewModel)), aUserRequest)
        urlPost(
          fullUrl(shortServiceTaxableRefundUrl(taxYearEOY)),
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
