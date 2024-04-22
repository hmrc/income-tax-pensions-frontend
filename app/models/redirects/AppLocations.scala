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

package models.redirects

import controllers.pensions
import models.pension.Journey
import play.api.mvc.Results.Redirect
import play.api.mvc.{Call, Result}

object AppLocations {
  val HOME: Int => Call =
    (taxYear: Int) => pensions.routes.PensionsSummaryController.show(taxYear)

  val OVERSEAS_HOME: Int => Call =
    (taxYear: Int) => pensions.routes.OverseasPensionsSummaryController.show(taxYear)

  val INCOME_FROM_PENSIONS_HOME: Int => Call =
    (taxYear: Int) => pensions.incomeFromPensions.routes.IncomeFromPensionsSummaryController.show(taxYear)

  val SECTION_COMPLETED_PAGE: (Int, Journey) => Result =
    (taxYear: Int, journey: Journey) => Redirect(pensions.routes.SectionCompletedStateController.show(taxYear, journey))
}
