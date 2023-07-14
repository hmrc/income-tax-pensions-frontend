package services.redirects

import builders.PensionAnnualAllowanceViewModelBuilder.aPensionAnnualAllowanceViewModel
import controllers.pensions.annualAllowances.routes.AnnualAllowanceCYAController
import models.mongo.PensionsCYAModel
import play.api.mvc.Results.Redirect
import services.redirects.AnnualAllowancesPages.{AboveAnnualAllowancePage, PSTRPage, PensionProviderPaidTaxPage}
import services.redirects.AnnualAllowancesRedirects.{cyaPageCall, journeyCheck}
import utils.UnitTest

class AnnualAllowancesRedirectsSpec extends UnitTest {

  private val cyaData: PensionsCYAModel = PensionsCYAModel.emptyModels
//  private val someRedirect = Some(Redirect(pensionsAnnualAllowancesController.show(taxYear)))

  ".cyaPageCall" should {
    "return a redirect call to the cya page" in {
      cyaPageCall(taxYear) shouldBe AnnualAllowanceCYAController.show(taxYear)
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
        val data2 = cyaData.copy(
          pensionsAnnualAllowances = aPensionAnnualAllowanceViewModel.copy(
            pensionSchemeTaxReferences = None
          )
        )
        val data3 = cyaData.copy(pensionsAnnualAllowances = aPensionAnnualAllowanceViewModel)
        val result1 = journeyCheck(PensionProviderPaidTaxPage, data1, taxYear)
        val result2 = journeyCheck(PSTRPage, data2, taxYear)
        val result3 = journeyCheck(PSTRPage, data3, taxYear)

        result1 shouldBe None
        result2 shouldBe None
        result3 shouldBe None
      }
      "current page is pre-filled and at end of journey so far" in {
        val data1 = cyaData.copy(
          pensionsAnnualAllowances = aPensionAnnualAllowanceViewModel.copy(
            pensionProvidePaidAnnualAllowanceQuestion = None,
            taxPaidByPensionProvider = None,
            pensionSchemeTaxReferences = None
          )
        )
        val data2 = cyaData.copy(
          pensionsAnnualAllowances = aPensionAnnualAllowanceViewModel.copy(
            pensionSchemeTaxReferences = Some(Seq("1234567CRC"))
          )
        )
        val data3 = cyaData.copy(pensionsAnnualAllowances = aPensionAnnualAllowanceViewModel)
        val result1 = journeyCheck(AboveAnnualAllowancePage, data1, taxYear)
        val result2 = journeyCheck(PSTRPage, data2, taxYear, Some(0))
        val result3 = journeyCheck(PSTRPage, data3, taxYear)

        result1 shouldBe None
        result2 shouldBe None
        result3 shouldBe None
      }
      "current page is pre-filled and mid-journey" in {
        val data = cyaData.copy(pensionsAnnualAllowances = aPensionAnnualAllowanceViewModel)
        val result = journeyCheck(PSTRPage, data, taxYear)

        result shouldBe None
      }
      "previous page is unanswered but invalid and previous valid question has been answered" in {
        val data = cyaData.copy(
          pensionsAnnualAllowances = anpensionsAnnualAllowancesEmptyViewModel.copy(
            surchargeQuestion = Some(false),
            noSurchargeQuestion = Some(true),
            surchargeAmount = None,
            surchargeTaxAmountQuestion = None,
            surchargeTaxAmount = None,
            noSurchargeAmount = None,
          )
        )
        val result = journeyCheck(NotSurchargedAmountPage, data, taxYear)

        result shouldBe None
      }
    }

    "return Some(redirect) with redirect to the first page in journey page" when {
      "previous question is unanswered" in {
        val data = cyaData.copy(
          pensionsAnnualAllowances = anpensionsAnnualAllowancesEmptyViewModel.copy(
            surchargeQuestion = Some(true),
            surchargeAmount = None
          )
        )
        val result = journeyCheck(NonUkTaxOnSurchargedAmountPage, data, taxYear)

        result shouldBe someRedirect
      }
      "current page is invalid in journey" in {
        val data = cyaData.copy(
          pensionsAnnualAllowances = anpensionsAnnualAllowancesViewModel.copy(
            ukPensionSchemesQuestion = None,
            pensionSchemeTaxReference = None
          )
        )
        val result = journeyCheck(RemovePSTRPage, data, taxYear)

        result shouldBe someRedirect
      }
    }
  }

}
