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

import builders.OverseasRefundPensionSchemeBuilder.{anOverseasRefundPensionSchemeWithUkRefundCharge, anOverseasRefundPensionSchemeWithoutUkRefundCharge}
import builders.PensionsCYAModelBuilder.aPensionsCYAModel
import builders.PensionsUserDataBuilder
import builders.ShortServiceRefundsViewModelBuilder.{aShortServiceRefundsViewModel, emptyShortServiceRefundsViewModel, minimalShortServiceRefundsViewModel}
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

  private def pensionsUsersData(pensionsCyaModel: PensionsCYAModel) = {
    PensionsUserDataBuilder.aPensionsUserData.copy(isPriorSubmission = false, pensions = pensionsCyaModel)
  }

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  ".show" should {
    "redirect to Overview Page when in year" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        insertCyaData(pensionsUsersData(aPensionsCYAModel))
        urlGet(fullUrl(shortServiceTaxableRefundUrl(taxYear)), !aUser.isAgent, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)))
      }

      result.status shouldBe SEE_OTHER
      result.headers("Location").head shouldBe overviewUrl(taxYear)
    }

    "show page when EOY" which {
      val incompleteViewModel = aShortServiceRefundsViewModel.copy(refundPensionScheme = Seq(
        anOverseasRefundPensionSchemeWithUkRefundCharge, anOverseasRefundPensionSchemeWithoutUkRefundCharge.copy(providerAddress = None)
      ))
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(shortServiceRefunds = incompleteViewModel)))
        urlGet(fullUrl(shortServiceTaxableRefundUrl(taxYearEOY)), !aUser.isAgent, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }
      "returns an Ok status" in {
        result.status shouldBe OK
      }
      "filters out any incomplete schemes and updates the session data" in {
        val filteredSchemes = incompleteViewModel.copy(refundPensionScheme = Seq(anOverseasRefundPensionSchemeWithUkRefundCharge))
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get

        cyaModel.pensions.shortServiceRefunds shouldBe filteredSchemes
      }
    }
  }

  ".submit" should {
    "redirect to overview when in year" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        val formData = Map(s" $yesNo -> true, $amount2" -> "100")
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(shortServiceRefunds = emptyShortServiceRefundsViewModel)))
        urlPost(
          fullUrl(shortServiceTaxableRefundUrl(taxYear)),
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)),
          follow = false,
          body = formData)
      }
      result.status shouldBe SEE_OTHER
      result.headers("location").head shouldBe overviewUrl(taxYear)
    }

    "persist amount and redirect to next page when user selects yes" which {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        val formData = Map(RadioButtonAmountForm.yesNo -> "true", RadioButtonAmountForm.amount2 -> "100")
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(shortServiceRefunds = emptyShortServiceRefundsViewModel)))
        urlPost(
          fullUrl(shortServiceTaxableRefundUrl(taxYearEOY)),
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)),
          follow = false,
          body = formData)
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.headers("location").head shouldBe nonUkTaxRefundsUrl(taxYearEOY)
      }

      "submitted data is persisted" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.shortServiceRefunds shouldBe emptyShortServiceRefundsViewModel.copy(
          shortServiceRefund = Some(true), shortServiceRefundCharge = Some(100))
      }
    }

    "persist amount and redirect to the CYA page" when {
      "user selects no" which {
        lazy implicit val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual(aUser.isAgent)
          val formData = Map(RadioButtonAmountForm.yesNo -> "false")
          insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(shortServiceRefunds = emptyShortServiceRefundsViewModel)))
          urlPost(
            fullUrl(shortServiceTaxableRefundUrl(taxYearEOY)),
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)),
            follow = false,
            body = formData)
        }

        "has a SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.headers("location").head shouldBe shortServiceRefundsCYAUrl(taxYearEOY)
        }

        "correct data is persisted" in {
          lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
          cyaModel.pensions.shortServiceRefunds shouldBe minimalShortServiceRefundsViewModel
        }
      }

      "user selects no and existing shortServiceRefunds data is cleared" which {
        lazy implicit val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual(aUser.isAgent)
          val formData = Map(RadioButtonAmountForm.yesNo -> "false")
          insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(shortServiceRefunds = aShortServiceRefundsViewModel)))
          urlPost(
            fullUrl(shortServiceTaxableRefundUrl(taxYearEOY)),
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)),
            follow = false,
            body = formData)
        }

        "has a SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.headers("location").head shouldBe shortServiceRefundsCYAUrl(taxYearEOY)
        }

        "clears the existing data" in {
          lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
          cyaModel.pensions.shortServiceRefunds shouldBe minimalShortServiceRefundsViewModel
        }
      }

      "user selects yes with new amount and the submission model is now complete" which {
        lazy implicit val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual(aUser.isAgent)
          val formData = Map(RadioButtonAmountForm.yesNo -> "true", RadioButtonAmountForm.amount2 -> "100")
          insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(shortServiceRefunds = aShortServiceRefundsViewModel)))
          urlPost(
            fullUrl(shortServiceTaxableRefundUrl(taxYearEOY)),
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)),
            follow = false,
            body = formData)
        }

        "has a SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.headers("location").head shouldBe shortServiceRefundsCYAUrl(taxYearEOY)
        }

        "updated data is persisted" in {
          lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
          cyaModel.pensions.shortServiceRefunds shouldBe aShortServiceRefundsViewModel.copy(shortServiceRefundCharge = Some(100))
        }
      }
    }

    "return an error" when {
      "form is submitted with no entry" which {
        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual(aUser.isAgent)
          val shortServiceRefundViewModel = aShortServiceRefundsViewModel.copy(
            shortServiceRefundCharge = Some(BigDecimal("100")), shortServiceRefund = Some(true))
          val form = Map(RadioButtonAmountForm.yesNo -> "", RadioButtonAmountForm.amount2 -> "")
          insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(shortServiceRefunds = shortServiceRefundViewModel)))
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

      "form is submitted with the wrong format" which {
        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual(aUser.isAgent)
          val shortServiceRefundViewModel = aShortServiceRefundsViewModel.copy(
            shortServiceRefundCharge = Some(BigDecimal("100")), shortServiceRefund = Some(true))
          val form = Map(RadioButtonAmountForm.yesNo -> "true", RadioButtonAmountForm.amount2 -> "jhvgfxk")
          insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(shortServiceRefunds = shortServiceRefundViewModel)))
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
}
