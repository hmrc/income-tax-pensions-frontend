package services.redirects


import controllers.pensions.incomeFromOverseasPensions.routes.{CountrySummaryListController, PensionOverseasIncomeCountryController}
import models.pension.charges.{PensionScheme, Relief}
import play.api.mvc.Call
import services.redirects.SimpleRedirectService.checkForExistingSchemes

object IncomeFromOverseasPensionsRedirects {

  def redirectOnBadIndexInSchemeLoop(reliefs: Seq[PensionScheme], taxYear: Int): Call = {
    checkForExistingSchemes(
      nextPage = PensionOverseasIncomeCountryController.show(taxYear, None),
      summaryPage = CountrySummaryListController.show(taxYear),
      schemes = reliefs
    )
  }

  def journeyCheck {}

}
