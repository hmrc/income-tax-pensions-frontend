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

import builders.OverseasRefundPensionSchemeBuilder.anOverseasRefundPensionScheme
import builders.PensionsCYAModelBuilder.aPensionsCYAModel
import builders.ShortServiceRefundsViewModelBuilder.{
  aShortServiceRefundsEmptySchemeViewModel,
  emptyShortServiceRefundsViewModel,
  minimalShortServiceRefundsViewModel
}
import controllers.ControllerSpec
import controllers.ControllerSpec.UserConfig
import models.pension.charges.{OverseasRefundPensionScheme, ShortServiceRefundsViewModel}
import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.libs.ws.WSResponse

class ShortServicePensionsSchemeControllerISpec
    extends ControllerSpec("/overseas-pensions/short-service-refunds/short-service-refunds-pension-scheme") {

  val providerNameIF    = "providerName"
  val schemeRefIF       = "schemeReference"
  val providerAddressIF = "providerAddress"
  val countryIF         = "countryId"

  // scalastyle:off magic.number line.size.limit

  "show" should {

    "show the page when the user has relevant session data and valid index" when {

      for (hasPriorData <- Seq(true, false))
        s"the user submits a correct pension scheme form with ${if (hasPriorData) "" else "no "}prior pensions scheme data to update and redirects to the relevant page" in {
          val nonUkModel =
            aShortServiceRefundsEmptySchemeViewModel.copy(refundPensionScheme = Seq(OverseasRefundPensionScheme()))
          val nonUkModelWithScheme =
            aShortServiceRefundsEmptySchemeViewModel.copy(refundPensionScheme = Seq(anOverseasRefundPensionScheme))
          val viewModel = if (hasPriorData) nonUkModelWithScheme else nonUkModel
          implicit val userConfig: UserConfig =
            userConfigWhenIrrelevant(Some(pensionsUserData(aPensionsCYAModel.copy(shortServiceRefunds = viewModel))))
          implicit val response: WSResponse = getPageWithIndex(0)

          response.status equals OK
          getShortServicePensionsViewModel mustBe Some(viewModel)
        }
    }

    "redirect to the summary page" when {
      "the user has no stored session data at all" in {
        implicit val userConfig: UserConfig = userConfigWhenIrrelevant(None)
        implicit val response: WSResponse   = getPageWithIndex()
        assertRedirectionAsExpected(PageRelativeURLs.pensionsSummaryPage)
      }
    }

    "redirect to the first page in journey" when {
      "previous questions have not been answered" in {
        val incompleteCYAModel              = aPensionsCYAModel.copy(shortServiceRefunds = emptyShortServiceRefundsViewModel)
        val sessionData                     = pensionsUserData(incompleteCYAModel)
        implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
        implicit val response: WSResponse   = getPageWithIndex(0)

        assertRedirectionAsExpected(PageRelativeURLs.taxableShortServiceRefunds)
      }
      "page is invalid in journey" in {
        val invalidCYAModel                 = aPensionsCYAModel.copy(shortServiceRefunds = minimalShortServiceRefundsViewModel)
        val sessionData                     = pensionsUserData(invalidCYAModel)
        implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
        implicit val response: WSResponse   = getPageWithIndex(0)

        assertRedirectionAsExpected(PageRelativeURLs.taxableShortServiceRefunds)
      }
    }

    "redirect to first page of scheme loop when index is invalid and there are no schemes" in {
      val sessionData                     = pensionsUserData(aPensionsCYAModel.copy(shortServiceRefunds = aShortServiceRefundsEmptySchemeViewModel))
      implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
      implicit val response: WSResponse   = getPageWithIndex(10)

      assertRedirectionAsExpected(PageRelativeURLs.shortServiceRefundSummary)
    }

    "redirect to scheme summary page when submission index is invalid and there are existing schemes" in {
      val sessionData                     = pensionsUserData(aPensionsCYAModel)
      implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
      implicit val response: WSResponse   = getPageWithIndex(-1)

      assertRedirectionAsExpected(PageRelativeURLs.shortServiceRefundSummary)
    }
  }

  "submit" should {

    "succeed when the user has relevant session data and valid index" when {
      "submitting a scheme" in {
        val formData = setFormData("Scheme Name without UK charge", "123456", "Scheme Address 2", Some("FR"))
        val sessionData = pensionsUserData(
          aPensionsCYAModel.copy(shortServiceRefunds = aShortServiceRefundsEmptySchemeViewModel.copy(
            refundPensionScheme = Seq(OverseasRefundPensionScheme())
          )))
        implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
        implicit val response: WSResponse   = submitForm(formData, Map("index" -> "0"))
        val expectedViewModel: ShortServiceRefundsViewModel = sessionData.pensions.shortServiceRefunds.copy(
          refundPensionScheme = sessionData.pensions.shortServiceRefunds.refundPensionScheme.updated(0, anOverseasRefundPensionScheme)
        )

        assertRedirectionAsExpected(PageRelativeURLs.shortServiceRefundSummary)
        getShortServiceViewModel mustBe Some(expectedViewModel)
      }
    }

    "return BAD_REQUEST the user submits an incorrect form" in {
      val formData                        = setFormData("", "", "", Some(""))
      implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(pensionsUserData(aPensionsCYAModel)))
      implicit val response: WSResponse   = submitForm(formData, Map("index" -> "0"))

      response must haveStatus(BAD_REQUEST)
    }

    "redirect to first page of journey" when {
      "page is invalid" in {
        val formData                        = setFormData("Scheme Name", "123456", "Scheme Address", Some("FR"))
        val sessionData                     = pensionsUserData(aPensionsCYAModel.copy(shortServiceRefunds = minimalShortServiceRefundsViewModel))
        implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
        implicit val response: WSResponse   = submitForm(formData, Map("refundPensionSchemeIndex" -> "0"))

        assertRedirectionAsExpected(PageRelativeURLs.taxableShortServiceRefunds)
      }
      "previous questions are unanswered" in {
        val formData                        = setFormData("Scheme Name", "123456", "Scheme Address", Some("FR"))
        val sessionData                     = pensionsUserData(aPensionsCYAModel.copy(shortServiceRefunds = emptyShortServiceRefundsViewModel))
        implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
        implicit val response: WSResponse   = submitForm(formData, Map("refundPensionSchemeIndex" -> "0"))

        assertRedirectionAsExpected(PageRelativeURLs.taxableShortServiceRefunds)
      }
    }

    "redirect to first page of scheme loop when index is invalid and there are no completed schemes" in {
      val formData                        = setFormData("Scheme Name", "123456", "Scheme Address", Some("FR"))
      val sessionData                     = pensionsUserData(aPensionsCYAModel.copy(shortServiceRefunds = aShortServiceRefundsEmptySchemeViewModel))
      implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
      implicit val response: WSResponse   = submitForm(formData, Map("refundPensionSchemeIndex" -> "10"))

      assertRedirectionAsExpected(PageRelativeURLs.shortServiceRefundSummary)
    }

    "redirect to scheme summary page when submission index is invalid and there are existing schemes" in {
      val formData                        = setFormData("Scheme Name", "123456", "Scheme Address", Some("FR"))
      val sessionData                     = pensionsUserData(aPensionsCYAModel)
      implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
      implicit val response: WSResponse   = submitForm(formData, Map("refundPensionSchemeIndex" -> "-1"))

      assertRedirectionAsExpected(PageRelativeURLs.shortServiceRefundSummary)
    }

    "redirect to the Pensions Summary page when the user has no stored session data at all" in {
      val formData                        = setFormData("Scheme Name", "123456", "Scheme Address", Some(""))
      implicit val userConfig: UserConfig = userConfigWhenIrrelevant(None)
      implicit val response: WSResponse   = submitForm(formData, Map("index" -> "0"))

      assertRedirectionAsExpected(PageRelativeURLs.pensionsSummaryPage)
      getTransferPensionsViewModel mustBe None
    }
  }

  def setFormData(pName: String, tRef: String, pAddress: String, countryOpt: Option[String]): Map[String, String] =
    Map(providerNameIF -> pName, schemeRefIF -> tRef, providerAddressIF -> pAddress) ++
      countryOpt.fold(Map[String, String]())(cc => Map(countryIF -> cc))
}
