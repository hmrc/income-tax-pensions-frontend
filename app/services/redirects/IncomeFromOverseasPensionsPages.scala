package services.redirects

sealed trait IncomeFromOverseasPensionsPages {
  val journeyNo: Int
}

object IncomeFromOverseasPensionsPages {

  case object OverseasIncomeStatusPage extends IncomeFromOverseasPensionsPages {
    override val journeyNo: Int = 1
  }

  case object WhatCountryIsSchemeRegisteredInPage extends IncomeFromOverseasPensionsPages {
    override val journeyNo: Int = 2
  }

  case object PensionsPaymentsAmountPage extends IncomeFromOverseasPensionsPages {
    override val journeyNo: Int = 3
  }

  case object SpecialWithholdingTaxPage extends IncomeFromOverseasPensionsPages {
    override val journeyNo: Int = 4
  }

  case object ForeignTaxCreditReliefPage extends IncomeFromOverseasPensionsPages {
    override val journeyNo: Int = 5
  }

  case object YourTaxableAmountPage extends IncomeFromOverseasPensionsPages {
    override val journeyNo: Int = 6
  }

  case object PensionSchemeSummaryPage extends IncomeFromOverseasPensionsPages {
    override val journeyNo: Int = 7
  }

  case object CountrySchemeSummaryListPage extends IncomeFromOverseasPensionsPages {
    override val journeyNo: Int = 8
  }

  case object RemoveSchemePage extends IncomeFromOverseasPensionsPages {
    override val journeyNo: Int = 9
  }

  case object CYAPage extends IncomeFromOverseasPensionsPages {
    override val journeyNo: Int = 10
  }

}
