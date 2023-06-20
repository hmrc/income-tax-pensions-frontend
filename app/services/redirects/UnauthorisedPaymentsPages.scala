
package services.redirects

sealed trait UnauthorisedPaymentsPages {
  val journeyNo: Int
}

object UnauthorisedPaymentsPages {

  case object DidYouGetAnUnauthorisedPaymentsPage extends UnauthorisedPaymentsPages {
    override val journeyNo: Int = 1
  }

  case object AmountSurchargedPage extends UnauthorisedPaymentsPages {
    override val journeyNo: Int = 2
  }

  case object NonUkTaxOnSurchargedAmountPage extends UnauthorisedPaymentsPages {
    override val journeyNo: Int = 3
  }

  case object AmountNotSurchargedPage extends UnauthorisedPaymentsPages {
    override val journeyNo: Int = 4
  }

  case object NonUkTaxOnNotSurchargedAmountPage extends UnauthorisedPaymentsPages {
    override val journeyNo: Int = 5
  }

  case object WereAnyUnauthPaymentsFromUkPensionSchemePage extends UnauthorisedPaymentsPages {
    override val journeyNo: Int = 6
  }

  case object PSTRPage extends UnauthorisedPaymentsPages {
    override val journeyNo: Int = 7
  }

  case object PSTRSummaryPage extends UnauthorisedPaymentsPages {
    override val journeyNo: Int = 8
  }

  case object CYAPage extends UnauthorisedPaymentsPages {
    override val journeyNo: Int = 9
  }
}
