/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
