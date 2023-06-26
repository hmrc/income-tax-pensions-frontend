package services.redirects

import controllers.pensions.incomeFromPensions.routes.{PensionSchemeDetailsController, UkPensionIncomeSummaryController}
import models.pension.statebenefits.UkPensionIncomeViewModel
import play.api.mvc.Call
import services.redirects.SimpleRedirectService.checkForExistingSchemes

object IncomeFromPensionsRedirects {

  def redirectForSchemeLoop(schemes: Seq[UkPensionIncomeViewModel], taxYear: Int): Call = {
    checkForExistingSchemes(
      nextPage = PensionSchemeDetailsController.show(taxYear, None),
      summaryPage = UkPensionIncomeSummaryController.show(taxYear),
      schemes = schemes
    )
  }

  def journeyCheck {}

}
