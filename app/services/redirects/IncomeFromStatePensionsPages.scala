/*
 * Copyright 2023 HM Revenue & Customs
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

sealed trait IncomeFromStatePensionsPages {
  val journeyNo: Int
}

object IncomeFromStatePensionsPages {

  case object DoYouGetRegularStatePaymentsPage extends IncomeFromStatePensionsPages {
    override val journeyNo: Int = 1
  }

  case object WhenDidYouStartGettingStatePaymentsPage extends IncomeFromStatePensionsPages {
    override val journeyNo: Int = 2
  }

  case object StatePensionLumpSumPage extends IncomeFromStatePensionsPages {
    override val journeyNo: Int = 3
  }

  case object TaxOnStatePensionLumpSumPage extends IncomeFromStatePensionsPages {
    override val journeyNo: Int = 4
  }

  case object WhenDidYouGetYourStatePensionLumpSumPage extends IncomeFromStatePensionsPages {
    override val journeyNo: Int = 5
  }

  case object AddStatePensionToIncomeTaxCalcPage$State extends IncomeFromStatePensionsPages {
    override val journeyNo: Int = 6
  }

  case object StateStatePensionsCYAPage$ extends IncomeFromStatePensionsPages {
    override val journeyNo: Int = 7
  }
}
