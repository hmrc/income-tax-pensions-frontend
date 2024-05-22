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
import builders.PensionsUserDataBuilder.{pensionUserDataWithPaymentsIntoOverseasPensions, taxYearEOY}
import builders.ReliefBuilder.aTransitionalCorrespondingRelief
import controllers.ControllerSpec
import controllers.ControllerSpec.UserConfig
import models.pension.charges.OverseasPensionScheme
import play.api.libs.ws.WSResponse
import utils.PageUrls.PaymentIntoOverseasPensions.{pensionReliefSchemeDetailsUrl, pensionReliefTypeUrl}

class UntaxedEmployerPaymentsControllerISpec extends ControllerSpec("/overseas-pensions/payments-into-overseas-pensions/untaxed-employer-payments") {

  "This page" when {

    "shown" should {
      "redirect to pension scheme summary page or customer reference page" which {

        "the user accesses page with index out of bounds and there are no complete relief schemes" in {
          val schemeIndex10 = 10
          val pensionsNoSchemesViewModel =
            aPaymentsIntoOverseasPensionsViewModel.copy(schemes = Seq(OverseasPensionScheme(customerReference = Some("PENSIONINCOME2000"))))
          val sessionData                     = pensionUserDataWithPaymentsIntoOverseasPensions(pensionsNoSchemesViewModel)
          implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
          implicit val response: WSResponse   = getPageWithIndex(schemeIndex10)

          assertRedirectionAsExpected(PageRelativeURLs.piopCustomerReferencePage)
        }
        "the user accesses page with index out of bounds and there are pensions schemes" in {
          val schemeIndex10                   = 10
          val sessionData                     = pensionsUserData(aPensionsCYAModel)
          implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
          implicit val response: WSResponse   = getPageWithIndex(schemeIndex10)

          assertRedirectionAsExpected(PageRelativeURLs.piopSchemeSummaryPage)
        }
      }
    }
    "submitted" should {

      val schemeIndex0  = 0
      val schemeIndex10 = 10
      val amount1001    = 1001

      "successfully submits the untaxed employer amount and redirects to the pensions relief type page" in {
        val viewModel = aPaymentsIntoOverseasPensionsViewModel.copy(schemes =
          Seq(aTransitionalCorrespondingRelief, OverseasPensionScheme(customerReference = Some("tcrPENSIONINCOME2000"))))
        val sessionData                     = pensionUserDataWithPaymentsIntoOverseasPensions(viewModel)
        implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
        implicit val response: WSResponse   = submitForm(Map("amount" -> amount1001.toString), Map("index" -> "1"))

        val expectedViewModel = viewModel.copy(schemes = Seq(
          aTransitionalCorrespondingRelief,
          OverseasPensionScheme(customerReference = Some("tcrPENSIONINCOME2000"), employerPaymentsAmount = Some(1001))))

        getPaymentsIntoOverseasPensionsViewModel mustBe Some(expectedViewModel)
        assertRedirectionAsExpected(pensionReliefTypeUrl(taxYearEOY, 1))
      }

      "successfully updates the untaxed employer amount and redirects to the scheme's summary page" in {

        val sessionData                     = pensionsUserData(aPensionsCYAModel)
        implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
        implicit val response: WSResponse   = submitForm(Map("amount" -> amount1001.toString), Map("index" -> schemeIndex0.toString))

        val expectedRelief = sessionData.pensions.paymentsIntoOverseasPensions.schemes.head.copy(employerPaymentsAmount = Some(amount1001))
        val expectedViewModel = sessionData.pensions.paymentsIntoOverseasPensions
          .copy(schemes = sessionData.pensions.paymentsIntoOverseasPensions.schemes.updated(0, expectedRelief))

        getPaymentsIntoOverseasPensionsViewModel.get.schemes mustBe expectedViewModel.schemes
        assertRedirectionAsExpected(pensionReliefSchemeDetailsUrl(taxYearEOY, 0))
      }

      "redirects to the customer reference page when the index is out of bounds and there are no complete relief schemes" in {
        val pensionsNoSchemesViewModel =
          aPaymentsIntoOverseasPensionsViewModel.copy(schemes = Seq(OverseasPensionScheme(customerReference = Some("PENSIONINCOME2000"))))
        val sessionData                     = pensionUserDataWithPaymentsIntoOverseasPensions(pensionsNoSchemesViewModel)
        implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
        implicit val response: WSResponse   = submitForm(Map("amount" -> amount1001.toString), Map("index" -> schemeIndex10.toString))

        assertRedirectionAsExpected(PageRelativeURLs.piopCustomerReferencePage)
      }

      "redirects to the pension scheme summary page when the index is out of bounds and there are pensions schemes" in {
        val sessionData                     = pensionsUserData(aPensionsCYAModel)
        implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
        implicit val response: WSResponse   = submitForm(Map("amount" -> amount1001.toString), Map("index" -> schemeIndex10.toString))

        assertRedirectionAsExpected(PageRelativeURLs.piopSchemeSummaryPage)
      }
    }
  }
}
