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

import builders.LifetimeAllowanceBuilder.aLifetimeAllowance1
import builders.PensionLifetimeAllowancesViewModelBuilder.{aPensionLifetimeAllowancesEmptyViewModel, aPensionLifetimeAllowancesViewModel}
import controllers.pensions.lifetimeAllowances.routes.{AboveAnnualLifetimeAllowanceController, LifetimeAllowanceCYAController, LifetimePstrSummaryController, PensionSchemeTaxReferenceLifetimeController}
import models.mongo.PensionsCYAModel
import play.api.http.Status.SEE_OTHER
import play.api.mvc.Results.Redirect
import play.api.mvc.{Call, Result}
import services.redirects.LifetimeAllowancesPages._
import services.redirects.LifetimeAllowancesRedirects.{cyaPageCall, journeyCheck, redirectForSchemeLoop}
import utils.UnitTest

class LifetimeAllowancesRedirectsSpec extends UnitTest {

  private val cyaData: PensionsCYAModel = PensionsCYAModel.emptyModels
  private val schemeDetailsCall: Call = PensionSchemeTaxReferenceLifetimeController.show(taxYear, None)
  private val schemeSummaryCall: Call = LifetimePstrSummaryController.show(taxYear)
  private val journeyStartRedirect: Option[Result] = Some(Redirect(AboveAnnualLifetimeAllowanceController.show(taxYear)))

  ".cyaPageCall" should {
    "return a redirect call to the cya page" in {
      cyaPageCall(taxYear) shouldBe LifetimeAllowanceCYAController.show(taxYear)
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
          pensionLifetimeAllowances = aPensionLifetimeAllowancesViewModel.copy(
            pensionPaidAnotherWay = None, pensionSchemeTaxReferences = None
          )
        )
        val result1 = journeyCheck(LifetimeAllowanceAnotherWayAmountPage, data1, taxYear)
        val data2 = cyaData.copy(
          pensionLifetimeAllowances = aPensionLifetimeAllowancesEmptyViewModel.copy(
            aboveLifetimeAllowanceQuestion = Some(true), pensionAsLumpSumQuestion = Some(true)
          )
        )
        val result2 = journeyCheck(LumpSumAmountPage, data2, taxYear)

        result1 shouldBe None
        result2 shouldBe None
      }
      "current page is pre-filled and at end of journey so far" in {
        val data1 = cyaData.copy(
          pensionLifetimeAllowances = aPensionLifetimeAllowancesEmptyViewModel.copy(
            aboveLifetimeAllowanceQuestion = Some(true), pensionAsLumpSumQuestion = Some(true),
            pensionAsLumpSum = Some(aLifetimeAllowance1)
          )
        )
        val result1 = journeyCheck(LumpSumAmountPage, data1, taxYear)
        val data2 = cyaData.copy(pensionLifetimeAllowances = aPensionLifetimeAllowancesViewModel)
        val result2 = journeyCheck(CYAPage, data2, taxYear)

        result1 shouldBe None
        result2 shouldBe None
      }
      "current page is pre-filled and mid-journey" in {
        val data = cyaData.copy(pensionLifetimeAllowances = aPensionLifetimeAllowancesViewModel.copy(pensionSchemeTaxReferences = None))
        val result = journeyCheck(LifetimeAllowanceAnotherWayPage, data, taxYear)

        result shouldBe None
      }
      "previous page is unanswered but invalid and previous valid question has been answered" in {
        val data1 = cyaData.copy(pensionLifetimeAllowances = aPensionLifetimeAllowancesEmptyViewModel.copy(
          aboveLifetimeAllowanceQuestion = Some(true), pensionAsLumpSumQuestion = Some(false)))
        val result1 = journeyCheck(LifetimeAllowanceAnotherWayPage, data1, taxYear)
        val data2 = cyaData.copy(pensionLifetimeAllowances = aPensionLifetimeAllowancesEmptyViewModel.copy(
          aboveLifetimeAllowanceQuestion = Some(false)))
        val result2 = journeyCheck(CYAPage, data2, taxYear)

        result1 shouldBe None
        result2 shouldBe None
      }
    }

    "return Some(redirect) with redirect to the first page in journey page" when {
      "previous question is unanswered" in {
        val data1 = cyaData.copy(pensionLifetimeAllowances = aPensionLifetimeAllowancesEmptyViewModel.copy(
          aboveLifetimeAllowanceQuestion = Some(true), pensionAsLumpSumQuestion = Some(true)))
        val data2 = cyaData.copy(pensionLifetimeAllowances = aPensionLifetimeAllowancesViewModel.copy(
          pensionPaidAnotherWay = None
        ))
        val result1 = journeyCheck(LifetimeAllowanceAnotherWayAmountPage, data1, taxYear)
        val result2 = journeyCheck(RemovePSTRPage, data2, taxYear)

        result1 shouldBe journeyStartRedirect
        result2 shouldBe journeyStartRedirect
      }
      "current page is invalid in journey" in {
        val data = cyaData.copy(pensionLifetimeAllowances = aPensionLifetimeAllowancesEmptyViewModel.copy(
          aboveLifetimeAllowanceQuestion = Some(true), pensionAsLumpSumQuestion = Some(false)))
        val result = journeyCheck(LumpSumAmountPage, data, taxYear)

        result shouldBe journeyStartRedirect
      }
    }

    "return Some(redirect) with redirect to scheme summary page" when {
      "trying to remove a scheme with an invalid index and existing schemes" in {
        val data = cyaData.copy(pensionLifetimeAllowances = aPensionLifetimeAllowancesViewModel)
        val result = journeyCheck(RemovePSTRPage, data, taxYear, Some(5))
        val statusHeader = result.map(_.header.status)
        val locationHeader = result.map(_.header.headers).map(_.get("Location"))

        statusHeader shouldBe Some(SEE_OTHER)
        locationHeader.get shouldBe Some(schemeSummaryCall.url)
      }
      "trying to remove a scheme with an invalid index and no schemes" in {
        val data = cyaData.copy(pensionLifetimeAllowances = aPensionLifetimeAllowancesViewModel.copy(
          pensionSchemeTaxReferences = Some(Seq.empty)))
        val result = journeyCheck(RemovePSTRPage, data, taxYear, Some(-5))
        val statusHeader = result.map(_.header.status)
        val locationHeader = result.map(_.header.headers).map(_.get("Location"))

        statusHeader shouldBe Some(SEE_OTHER)
        locationHeader.get shouldBe Some(schemeSummaryCall.url)
      }
    }

    "when trying to render the PSTR page" should {

      "return None" when {
        "using a valid index to an existing scheme" in {
          val data = cyaData.copy(pensionLifetimeAllowances = aPensionLifetimeAllowancesViewModel)
          val result = journeyCheck(PSTRPage, data, taxYear, Some(0))

          result shouldBe None
        }
        "index is None and there are no existing schemes" in {
          val data = cyaData.copy(pensionLifetimeAllowances = aPensionLifetimeAllowancesViewModel.copy(
            pensionSchemeTaxReferences = None))
          val result = journeyCheck(PSTRPage, data, taxYear)

          result shouldBe None
        }
        "index is None and there are existing schemes" in {
          val data = cyaData.copy(pensionLifetimeAllowances = aPensionLifetimeAllowancesViewModel)
          val result = journeyCheck(PSTRPage, data, taxYear)

          result shouldBe None
        }
        "index is invalid but there are no existing schemes" in {
          val data = cyaData.copy(pensionLifetimeAllowances = aPensionLifetimeAllowancesViewModel.copy(
            pensionSchemeTaxReferences = None))
          val result = journeyCheck(PSTRPage, data, taxYear, Some(-5))

          result shouldBe None
        }
      }

      "return Some(redirect) to PSTR summary page when index is invalid and there are existing schemes" in {
        val data = cyaData.copy(pensionLifetimeAllowances = aPensionLifetimeAllowancesViewModel)
        val result = journeyCheck(PSTRPage, data, taxYear, Some(12))
        val statusHeader = result.map(_.header.status)
        val locationHeader = result.map(_.header.headers).map(_.get("Location"))

        statusHeader shouldBe Some(SEE_OTHER)
        locationHeader.get shouldBe Some(schemeSummaryCall.url)
      }
    }
  }
}
