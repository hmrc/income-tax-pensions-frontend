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

package services.redirects

import builders.PensionAnnualAllowanceViewModelBuilder.aPensionAnnualAllowanceViewModel
import controllers.pensions.annualAllowances.routes.{AnnualAllowanceCYAController, PensionSchemeTaxReferenceController, PstrSummaryController, ReducedAnnualAllowanceController}
import models.mongo.PensionsCYAModel
import models.pension.charges.PensionAnnualAllowancesViewModel
import play.api.http.Status.SEE_OTHER
import play.api.mvc.Results.Redirect
import play.api.mvc.{Call, Result}
import services.redirects.AnnualAllowancesPages._
import services.redirects.AnnualAllowancesRedirects.{cyaPageCall, journeyCheck, redirectForSchemeLoop}
import utils.UnitTest

class AnnualAllowancesRedirectsSpec extends UnitTest {

  private val cyaData: PensionsCYAModel = PensionsCYAModel.emptyModels
  private val schemeDetailsCall: Call = PensionSchemeTaxReferenceController.show(taxYear, None)
  private val schemeSummaryCall: Call = PstrSummaryController.show(taxYear)
  private val journeyStartRedirect: Option[Result] = Some(Redirect(ReducedAnnualAllowanceController.show(taxYear)))

  ".cyaPageCall" should {
    "return a redirect call to the cya page" in {
      cyaPageCall(taxYear) shouldBe AnnualAllowanceCYAController.show(taxYear)
    }
  }

  ".redirectForSchemeLoop" should {
    "filter empty schemes and return a Call to the scheme summary page when other schemes exist" in {
      val existingSchemes: Seq[String] = Seq("1234567CRC", "12345678RB", "", "1234567DRD", "   ")
      val result = redirectForSchemeLoop(existingSchemes, taxYear)

      result shouldBe schemeSummaryCall
    }
    "filter empty schemes and return a Call to the scheme details page when schemes is then empty" in {
      val emptySchemes: Seq[String] = Seq("", "   ")
      val result = redirectForSchemeLoop(emptySchemes, taxYear)

      result shouldBe schemeDetailsCall
    }
  }

  ".journeyCheck" should {
    "return None if page is valid and all previous questions have been answered" when {
      "current page is empty and at end of journey so far" in {
        val data1 = cyaData.copy(
          pensionsAnnualAllowances = aPensionAnnualAllowanceViewModel.copy(
            pensionProvidePaidAnnualAllowanceQuestion = None,
            taxPaidByPensionProvider = None,
            pensionSchemeTaxReferences = None
          )
        )
        val result1 = journeyCheck(PensionProviderPaidTaxPage, data1, taxYear)

        result1 shouldBe None
      }
      "current page is pre-filled and at end of journey so far" in {
        val data1 = cyaData.copy(
          pensionsAnnualAllowances = aPensionAnnualAllowanceViewModel.copy(
            pensionProvidePaidAnnualAllowanceQuestion = None,
            taxPaidByPensionProvider = None,
            pensionSchemeTaxReferences = None
          )
        )
        val data2 = cyaData.copy(pensionsAnnualAllowances = aPensionAnnualAllowanceViewModel)
        val result1 = journeyCheck(AboveAnnualAllowancePage, data1, taxYear)
        val result2 = journeyCheck(CYAPage, data2, taxYear)

        result1 shouldBe None
        result2 shouldBe None
      }
      "current page is pre-filled and mid-journey" in {
        val data = cyaData.copy(pensionsAnnualAllowances = aPensionAnnualAllowanceViewModel)
        val result = journeyCheck(PensionProviderPaidTaxPage, data, taxYear)

        result shouldBe None
      }
      "previous page is unanswered but invalid and previous valid question has been answered" in {
        val data = cyaData.copy(pensionsAnnualAllowances = PensionAnnualAllowancesViewModel(
          reducedAnnualAllowanceQuestion = Some(true),
          moneyPurchaseAnnualAllowance = Some(true),
          taperedAnnualAllowance = Some(true),
          aboveAnnualAllowanceQuestion = Some(false)
        ))
        val result = journeyCheck(CYAPage, data, taxYear)

        result shouldBe None
      }
    }

    "return Some(redirect) with redirect to the first page in journey page" when {
      "previous question is unanswered" in {
        val data = cyaData.copy(pensionsAnnualAllowances = PensionAnnualAllowancesViewModel(
          reducedAnnualAllowanceQuestion = Some(true),
          moneyPurchaseAnnualAllowance = Some(true),
          taperedAnnualAllowance = Some(true)
        ))
        val result = journeyCheck(PensionProviderPaidTaxPage, data, taxYear)

        result shouldBe journeyStartRedirect
      }
      "current page is invalid in journey" in {
        val data = cyaData.copy(pensionsAnnualAllowances = PensionAnnualAllowancesViewModel(
          reducedAnnualAllowanceQuestion = Some(false)))
        val result = journeyCheck(ReducedAnnualAllowanceTypePage, data, taxYear)

        result shouldBe journeyStartRedirect
      }
    }

    "return Some(redirect) with redirect to scheme summary page" when {
      "trying to remove a scheme with an invalid index and existing schemes" in {
        val data = cyaData.copy(pensionsAnnualAllowances = aPensionAnnualAllowanceViewModel)
        val result = journeyCheck(RemovePSTRPage, data, taxYear, Some(5))
        val statusHeader = result.map(_.header.status)
        val locationHeader = result.map(_.header.headers).map(_.get("Location"))

        statusHeader shouldBe Some(SEE_OTHER)
        locationHeader.get shouldBe Some(schemeSummaryCall.url)
      }
      "trying to remove a scheme with an invalid index and no schemes" in {
        val data = cyaData.copy(pensionsAnnualAllowances = aPensionAnnualAllowanceViewModel.copy(
          pensionSchemeTaxReferences = Some(Seq.empty)))
        val result = journeyCheck(RemovePSTRPage, data, taxYear, Some(-5))
        val statusHeader = result.map(_.header.status)
        val locationHeader = result.map(_.header.headers).map(_.get("Location"))

        statusHeader shouldBe Some(SEE_OTHER)
        locationHeader.get shouldBe Some(schemeSummaryCall.url)
      }
    }

    "when trying to render the PSTR page" should {
      "return Some(redirect) to PSTR details page" when {
        "index is None and there are no existing schemes" in {
          val data = cyaData.copy(pensionsAnnualAllowances = aPensionAnnualAllowanceViewModel.copy(
            pensionSchemeTaxReferences = None))
          val result = journeyCheck(PSTRPage, data, taxYear)
          val statusHeader = result.map(_.header.status)
          val locationHeader = result.map(_.header.headers).map(_.get("Location"))

          statusHeader shouldBe Some(SEE_OTHER)
          locationHeader.get shouldBe Some(schemeDetailsCall.url)
        }
        "index is invalid and there are no existing schemes" in {
          val data = cyaData.copy(pensionsAnnualAllowances = aPensionAnnualAllowanceViewModel.copy(
            pensionSchemeTaxReferences = None))
          val result = journeyCheck(PSTRPage, data, taxYear, Some(-5))
          val statusHeader = result.map(_.header.status)
          val locationHeader = result.map(_.header.headers).map(_.get("Location"))

          statusHeader shouldBe Some(SEE_OTHER)
          locationHeader.get shouldBe Some(schemeDetailsCall.url)
        }
      }

      "return Some(redirect) to PSTR summary page when index is invalid and there are existing schemes" in {
        val data = cyaData.copy(pensionsAnnualAllowances = aPensionAnnualAllowanceViewModel)
        val result = journeyCheck(PSTRPage, data, taxYear, Some(12))
        val statusHeader = result.map(_.header.status)
        val locationHeader = result.map(_.header.headers).map(_.get("Location"))

        statusHeader shouldBe Some(SEE_OTHER)
        locationHeader.get shouldBe Some(schemeSummaryCall.url)
      }

      "return None when using a valid index" in {
        val data = cyaData.copy(pensionsAnnualAllowances = aPensionAnnualAllowanceViewModel)
        val result = journeyCheck(PSTRPage, data, taxYear, Some(0))

        result shouldBe None
      }

      "return None when index is None and there are existing schemes" in {
        val data = cyaData.copy(pensionsAnnualAllowances = aPensionAnnualAllowanceViewModel)
        val result = journeyCheck(PSTRPage, data, taxYear)

        result shouldBe None
      }
    }
  }

}
