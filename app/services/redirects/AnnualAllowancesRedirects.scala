package services.redirects

import controllers.pensions.annualAllowances.routes.{PensionSchemeTaxReferenceController, PstrSummaryController}
import models.pension.statebenefits.UkPensionIncomeViewModel
import play.api.mvc.Call
import services.redirects.SimpleRedirectService.checkForExistingSchemes

object AnnualAllowancesRedirects {

  def redirectForSchemeLoop(schemes: Seq[String], taxYear: Int): Call = {
    checkForExistingSchemes(
      nextPage = PensionSchemeTaxReferenceController.show(taxYear, None),
      summaryPage = PstrSummaryController.show(taxYear),
      schemes = schemes
    )
  }

  def journeyCheck {}

}
