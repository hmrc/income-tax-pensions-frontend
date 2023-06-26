package services.redirects

import controllers.pensions.unauthorisedPayments.routes.{UkPensionSchemeDetailsController, UnauthorisedPensionSchemeTaxReferenceController}
import play.api.mvc.Call
import services.redirects.SimpleRedirectService.checkForExistingSchemes

object UnauthorisedPaymentsRedirects {

  def redirectForSchemeLoop(schemes: Seq[String], taxYear: Int): Call = {
    checkForExistingSchemes(
      nextPage = UnauthorisedPensionSchemeTaxReferenceController.show(taxYear, None),
      summaryPage = UkPensionSchemeDetailsController.show(taxYear),
      schemes = schemes
    )
  }

  def journeyCheck {}

}
