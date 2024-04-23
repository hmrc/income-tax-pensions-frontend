package controllers.pensions.incomeFromPensions

import controllers.pensions.incomeFromPensions.routes._
import controllers.pensions.routes._
import play.api.mvc.Call

object AppLocations {
  val HOME: Int => Call =
    (taxYear: Int) => PensionsSummaryController.show(taxYear)

  val OVERSEAS_HOME: Int => Call =
    (taxYear: Int) => OverseasPensionsSummaryController.show(taxYear)

  val INCOME_FROM_PENSIONS_HOME: Int => Call =
    (taxYear: Int) => IncomeFromPensionsSummaryController.show(taxYear)

}
