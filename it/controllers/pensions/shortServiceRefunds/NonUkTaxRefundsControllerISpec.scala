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
import builders.ShortServiceRefundsViewModelBuilder.aShortServiceRefundsViewModel
import builders.UserBuilder.aUser
import forms.RadioButtonAmountForm
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.pension.charges.ShortServiceRefundsViewModel
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.ShortServiceRefunds.{nonUkTaxRefundsUrl, refundSummaryUrl, shortServiceRefundsCYAUrl, shortServiceTaxableRefundUrl}
import utils.PageUrls.fullUrl
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

class NonUkTaxRefundsControllerISpec extends IntegrationTest with ViewHelpers with PensionsDatabaseHelper {

  private def pensionsUsersData(pensionsCyaModel: PensionsCYAModel): PensionsUserData =
    PensionsUserDataBuilder.aPensionsUserData.copy(isPriorSubmission = false, pensions = pensionsCyaModel)

  override val userScenarios: Seq[UserScenario[_, _]] = Nil

  ".show" should {
    "show page when EOY" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        insertCyaData(pensionsUsersData(aPensionsCYAModel))
        urlGet(
          fullUrl(nonUkTaxRefundsUrl(taxYearEOY)),
          !aUser.isAgent,
          follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }
      result.status shouldBe OK
    }

    "redirect to the first page in journey" when {
      "previous questions have not been answered" in {
        val incompleteCYAModel = aPensionsCYAModel.copy(shortServiceRefunds = ShortServiceRefundsViewModel(shortServiceRefund = Some(true)))
        lazy implicit val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual(aUser.isAgent)
          insertCyaData(pensionsUsersData(incompleteCYAModel))
          urlGet(
            fullUrl(nonUkTaxRefundsUrl(taxYearEOY)),
            !aUser.isAgent,
            follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe shortServiceTaxableRefundUrl(taxYearEOY)
      }
      "page is invalid in journey" in {
        val incompleteCYAModel = aPensionsCYAModel.copy(shortServiceRefunds = ShortServiceRefundsViewModel(shortServiceRefund = Some(false)))
        lazy implicit val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual(aUser.isAgent)
          insertCyaData(pensionsUsersData(incompleteCYAModel))
          urlGet(
            fullUrl(nonUkTaxRefundsUrl(taxYearEOY)),
            !aUser.isAgent,
            follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe shortServiceTaxableRefundUrl(taxYearEOY)
      }
    }
  }

  ".submit" should {
    "valid submission should persist amount and redirect" which {
      "directs to the CYA page when there is a short service schemes and so Submission Model is now complete " in {
        lazy implicit val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual(aUser.isAgent)
          val shortServiceRefundViewModel = aShortServiceRefundsViewModel.copy(
            shortServiceRefundTaxPaid = Some(true),
            shortServiceRefundTaxPaidCharge = Some(BigDecimal("500.00"))
          )
          val formData = Map(RadioButtonAmountForm.yesNo -> "true", RadioButtonAmountForm.amount2 -> "500")
          insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(shortServiceRefunds = shortServiceRefundViewModel)))
          urlPost(
            fullUrl(nonUkTaxRefundsUrl(taxYearEOY)),
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)),
            follow = false,
            body = formData)
        }
        result.status shouldBe SEE_OTHER
        result.headers("location").head shouldBe shortServiceRefundsCYAUrl(taxYearEOY)
      }

      "directs to the next page when there are no short service schemes" in {
        lazy implicit val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual(aUser.isAgent)
          val shortServiceRefundViewModel = aShortServiceRefundsViewModel.copy(
            refundPensionScheme = Nil
          )
          val formData = Map(RadioButtonAmountForm.yesNo -> "true", RadioButtonAmountForm.amount2 -> "500")
          insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(shortServiceRefunds = shortServiceRefundViewModel)))
          urlPost(
            fullUrl(nonUkTaxRefundsUrl(taxYearEOY)),
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)),
            follow = false,
            body = formData)
        }
        result.status shouldBe SEE_OTHER
        result.headers("location").head shouldBe refundSummaryUrl(taxYearEOY)
      }
    }

    "return an error" when {
      "form is submitted with no entry" in {
        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual(aUser.isAgent)
          val shortServiceRefundViewModel = aShortServiceRefundsViewModel.copy(
            shortServiceRefundTaxPaid = Some(true),
            shortServiceRefundTaxPaidCharge = None
          )
          val form = Map(RadioButtonAmountForm.yesNo -> "", RadioButtonAmountForm.amount2 -> "")
          insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(shortServiceRefunds = shortServiceRefundViewModel)))
          urlPost(
            fullUrl(nonUkTaxRefundsUrl(taxYearEOY)),
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)),
            follow = false,
            body = form)
        }
        result.status shouldBe BAD_REQUEST
      }

      "form is submitted with the wrong format" in {
        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual(aUser.isAgent)
          val shortServiceRefundViewModel =
            aShortServiceRefundsViewModel.copy(shortServiceRefundCharge = Some(123.00), shortServiceRefund = Some(true))
          val form = Map(RadioButtonAmountForm.yesNo -> "true", RadioButtonAmountForm.amount2 -> "jhvgfxk")
          insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(shortServiceRefunds = shortServiceRefundViewModel)))
          urlPost(
            fullUrl(nonUkTaxRefundsUrl(taxYearEOY)),
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)),
            follow = false,
            body = form)
        }
        result.status shouldBe BAD_REQUEST
      }
    }
  }
}
