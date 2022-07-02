/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers.pensions.paymentsIntoPensions

import builders.PaymentsIntoPensionVewModelBuilder.aPaymentsIntoPensionViewModel
import builders.PensionsUserDataBuilder.{anPensionsUserDataEmptyCya, pensionsUserDataWithPaymentsIntoPensions}
import builders.UserBuilder.aUserRequest
import controllers.pensions.paymentsIntoPension.routes.{
  PensionsTaxReliefNotClaimedController,
  ReliefAtSourcePaymentsAndTaxReliefAmountController,
  PaymentsIntoPensionsCYAController
}
import forms.YesNoForm
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.PaymentIntoPensions.reliefAtSourcePensionsUrl
import utils.PageUrls.fullUrl
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

// scalastyle:off magic.number
class ReliefAtSourcePensionsControllerISpec extends IntegrationTest with BeforeAndAfterEach with ViewHelpers with PensionsDatabaseHelper {

  val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  ".show" should {
    "render 'Relief at source (RAS) pensions' page " in {
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(isAgent = false)
        dropPensionsDB()
        insertCyaData(anPensionsUserDataEmptyCya, aUserRequest)
        urlGet(fullUrl(reliefAtSourcePensionsUrl(taxYearEOY)), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }
      result.status shouldBe OK
    }
  }
  ".submit" should {

    "redirect and update question to 'Yes' when user selects yes when there is no cya data" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

      lazy val result: WSResponse = {
        dropPensionsDB()
        insertCyaData(anPensionsUserDataEmptyCya, aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(reliefAtSourcePensionsUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(ReliefAtSourcePaymentsAndTaxReliefAmountController.show(taxYearEOY).url)
      }

      "updates rasPensionPaymentQuestion to Some(true)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.paymentsIntoPension.rasPensionPaymentQuestion shouldBe Some(true)
      }
    }

    "redirect and update question to 'Yes' when user selects yes and cya data exists" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)

      lazy val result: WSResponse = {
        dropPensionsDB()
        insertCyaData(pensionsUserDataWithPaymentsIntoPensions(aPaymentsIntoPensionViewModel.copy(
          rasPensionPaymentQuestion = Some(false), totalRASPaymentsAndTaxRelief = None)), aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(reliefAtSourcePensionsUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(ReliefAtSourcePaymentsAndTaxReliefAmountController.show(taxYearEOY).url)
      }

      "updates rasPensionPaymentQuestion to Some(true)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.paymentsIntoPension.rasPensionPaymentQuestion shouldBe Some(true)
      }
    }

    "redirect and update question to 'No' when user selects no and there is no cya data" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)

      lazy val result: WSResponse = {
        dropPensionsDB()
        insertCyaData(anPensionsUserDataEmptyCya, aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(reliefAtSourcePensionsUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(PensionsTaxReliefNotClaimedController.show(taxYearEOY).url)
      }

      "updates rasPensionPaymentQuestion to Some(true)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.paymentsIntoPension.rasPensionPaymentQuestion shouldBe Some(false)
        cyaModel.pensions.paymentsIntoPension.totalRASPaymentsAndTaxRelief shouldBe None
      }
    }

    "redirect to Payment Into Pensions CYA page when user selects No which completes the CYA model" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)

      lazy val result: WSResponse = {
        dropPensionsDB()
        val paymentsIntoPensionsViewModel = aPaymentsIntoPensionViewModel.copy(rasPensionPaymentQuestion = Some(true),
          totalRASPaymentsAndTaxRelief = Some(123.12))
        insertCyaData(pensionsUserDataWithPaymentsIntoPensions(paymentsIntoPensionsViewModel), aUserRequest)
        authoriseAgentOrIndividual(isAgent = false)
        urlPost(fullUrl(reliefAtSourcePensionsUrl(taxYearEOY)), body = form, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(PaymentsIntoPensionsCYAController.show(taxYearEOY).url)
      }

      "updates rasPensionPaymentQuestion to Some(false) and remove totalRASPaymentsAndTaxRelief" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.paymentsIntoPension.rasPensionPaymentQuestion shouldBe Some(false)
        cyaModel.pensions.paymentsIntoPension.totalRASPaymentsAndTaxRelief shouldBe None
        cyaModel.pensions.paymentsIntoPension.oneOffRasPaymentPlusTaxReliefQuestion shouldBe None
        cyaModel.pensions.paymentsIntoPension.totalOneOffRasPaymentPlusTaxRelief shouldBe None
        cyaModel.pensions.paymentsIntoPension.totalRASPaymentsAndTaxRelief shouldBe None
      }
    }
  }
}
// scalastyle:on magic.number
