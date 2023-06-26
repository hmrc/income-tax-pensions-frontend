package services.redirects


import controllers.pensions.incomeFromOverseasPensions.routes.{CountrySummaryListController, PensionOverseasIncomeCountryController}
import models.pension.charges.{PensionScheme, Relief}
import play.api.mvc.Call
import services.redirects.SimpleRedirectService.checkForExistingSchemes

object IncomeFromOverseasPensionsRedirects {

  def redirectForSchemeLoop(schemes: Seq[PensionScheme], taxYear: Int): Call = {
    checkForExistingSchemes(
      nextPage = PensionOverseasIncomeCountryController.show(taxYear, None),
      summaryPage = CountrySummaryListController.show(taxYear),
      schemes = schemes
    )
  }

  def journeyCheck {}

}
