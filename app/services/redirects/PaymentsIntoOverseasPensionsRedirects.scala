package services.redirects

import models.pension.charges.Relief
import play.api.mvc.Call
import controllers.pensions.paymentsIntoOverseasPensions.routes._
import services.redirects.SimpleRedirectService.checkForExistingSchemes

object PaymentsIntoOverseasPensionsRedirects {

  def redirectOnBadIndexInSchemeLoop(reliefs: Seq[Relief], taxYear: Int): Call = {
    checkForExistingSchemes(
      nextPage = PensionsCustomerReferenceNumberController.show(taxYear, None),
      summaryPage = ReliefsSchemeSummaryController.show(taxYear),
      schemes = reliefs
    )
  }

  def journeyCheck{}

}
