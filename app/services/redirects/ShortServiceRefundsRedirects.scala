package services.redirects

import controllers.pensions.shortServiceRefunds.routes.{RefundSummaryController, TaxOnShortServiceRefundController}
import models.pension.charges.OverseasRefundPensionScheme
import play.api.mvc.Call
import services.redirects.SimpleRedirectService.checkForExistingSchemes

object ShortServiceRefundsRedirects {

  def redirectForSchemeLoop(refundSchemes: Seq[OverseasRefundPensionScheme], taxYear: Int): Call = {
    checkForExistingSchemes(
      nextPage = TaxOnShortServiceRefundController.show(taxYear, None),
      summaryPage = RefundSummaryController.show(taxYear),
      schemes = refundSchemes
    )
  }

  def journeyCheck {}

}
