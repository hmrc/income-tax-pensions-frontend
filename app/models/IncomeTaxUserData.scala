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

package models

import models.pension.AllPensionsData
import models.pension.statebenefits.AllStateBenefitsData
import play.api.libs.json.{Json, OFormat}

// TODO: Investigate whether we need state benefits at this level here, can we not use the one sat inside pensions?
final case class IncomeTaxUserData(pensions: Option[AllPensionsData] = None, stateBenefits: Option[AllStateBenefitsData] = None)

object IncomeTaxUserData {
  type PriorData = IncomeTaxUserData
  implicit val formats: OFormat[IncomeTaxUserData] = Json.format[IncomeTaxUserData]
}
