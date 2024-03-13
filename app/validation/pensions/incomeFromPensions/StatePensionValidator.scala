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

package validation.pensions.incomeFromPensions

import models.pension.statebenefits.IncomeFromPensionsViewModel
import play.api.mvc.Result
import services.redirects.StatePensionPages
import services.redirects.StatePensionRedirects.firstPageRedirect

import scala.concurrent.Future

object StatePensionValidator {

  def validateFlow(answers: IncomeFromPensionsViewModel, page: StatePensionPages, taxYear: Int)(blockIfValid: => Future[Result]): Future[Result] =
    if (page.isValidInCurrentState(answers)) blockIfValid
    else Future.successful(firstPageRedirect(taxYear))
}
