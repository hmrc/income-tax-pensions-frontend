package services.redirects

import controllers.pensions.transferIntoOverseasPensions.routes.{OverseasTransferChargePaidController, TransferChargeSummaryController}
import models.pension.charges.TransferPensionScheme
import play.api.mvc.Call
import services.redirects.SimpleRedirectService.checkForExistingSchemes

object TransfersIntoOverseasPensionsRedirects {

  def redirectOnBadIndexInSchemeLoop(reliefs: Seq[TransferPensionScheme], taxYear: Int): Call = {
    checkForExistingSchemes(
      nextPage = OverseasTransferChargePaidController.show(taxYear, None),
      summaryPage = TransferChargeSummaryController.show(taxYear),
      schemes = reliefs
    )
  }

  def journeyCheck {}

}
