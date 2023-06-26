package services.redirects

import services.redirects.SimpleRedirectService.checkForExistingSchemes
import play.api.mvc.Call
import controllers.pensions.lifetimeAllowances.routes.{PensionSchemeTaxReferenceLifetimeController, AnnualLifetimeAllowanceCYAController}

object LifetimeAllowancesRedirects {

  def redirectForSchemeLoop(schemes: Seq[String], taxYear: Int): Call = {
    checkForExistingSchemes(
      nextPage = PensionSchemeTaxReferenceLifetimeController.show(taxYear, None),
      summaryPage = AnnualLifetimeAllowanceCYAController.show(taxYear), // TODO replace with PensionsSchemesThatPaidTheLifetimeAllowanceTax summary page when created
      schemes = schemes
    )
  }

  def journeyCheck {}

}
