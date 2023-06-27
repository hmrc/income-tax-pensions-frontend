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

import services.redirects.SimpleRedirectService.checkForExistingSchemes
import play.api.mvc.Call
import controllers.pensions.lifetimeAllowances.routes.{PensionSchemeTaxReferenceLifetimeController, LifetimePstrSummaryController}

object LifetimeAllowancesRedirects {

  def redirectForSchemeLoop(schemes: Seq[String], taxYear: Int): Call = {
    checkForExistingSchemes(
      nextPage = PensionSchemeTaxReferenceLifetimeController.show(taxYear, None),
      summaryPage = LifetimePstrSummaryController.show(taxYear),
      schemes = schemes
    )
  }

  def journeyCheck(): Unit = {}

}
