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

package models.pension.statebenefits

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.{JsPath, OWrites, Reads}
import utils.JsonUtils.jsonObjNoNulls

// I think the only fields we are interested in are `statePension` and `statePensionLumpSum`
case class StateBenefitsData(incapacityBenefits: Option[Set[StateBenefit]] = None,
                             statePension: Option[StateBenefit] = None,
                             statePensionLumpSum: Option[StateBenefit] = None,
                             employmentSupportAllowances: Option[Set[StateBenefit]] = None,
                             jobSeekersAllowances: Option[Set[StateBenefit]] = None,
                             bereavementAllowance: Option[StateBenefit] = None,
                             other: Option[StateBenefit] = None)

object StateBenefitsData {

  implicit val stateBenefitsDataWrites: OWrites[StateBenefitsData] = (stateBenefitsData: StateBenefitsData) =>
    jsonObjNoNulls(
      "incapacityBenefit"          -> stateBenefitsData.incapacityBenefits,
      "statePension"               -> stateBenefitsData.statePension,
      "statePensionLumpSum"        -> stateBenefitsData.statePensionLumpSum,
      "employmentSupportAllowance" -> stateBenefitsData.employmentSupportAllowances,
      "jobSeekersAllowance"        -> stateBenefitsData.jobSeekersAllowances,
      "bereavementAllowance"       -> stateBenefitsData.bereavementAllowance,
      "otherStateBenefits"         -> stateBenefitsData.other
    )

  implicit val stateBenefitsDataReads: Reads[StateBenefitsData] = (
    (JsPath \ "incapacityBenefit").readNullable[Set[StateBenefit]] and
      (JsPath \ "statePension").readNullable[StateBenefit] and
      (JsPath \ "statePensionLumpSum").readNullable[StateBenefit] and
      (JsPath \ "employmentSupportAllowance").readNullable[Set[StateBenefit]] and
      (JsPath \ "jobSeekersAllowance").readNullable[Set[StateBenefit]] and
      (JsPath \ "bereavementAllowance").readNullable[StateBenefit] and
      (JsPath \ "otherStateBenefits").readNullable[StateBenefit]
  )(StateBenefitsData.apply _)
}
