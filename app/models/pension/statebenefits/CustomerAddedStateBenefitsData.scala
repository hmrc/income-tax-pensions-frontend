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

case class CustomerAddedStateBenefitsData(incapacityBenefits: Option[Set[CustomerAddedStateBenefit]] = None,
                                          statePensions: Option[Set[CustomerAddedStateBenefit]] = None,
                                          statePensionLumpSums: Option[Set[CustomerAddedStateBenefit]] = None,
                                          employmentSupportAllowances: Option[Set[CustomerAddedStateBenefit]] = None,
                                          jobSeekersAllowances: Option[Set[CustomerAddedStateBenefit]] = None,
                                          bereavementAllowances: Option[Set[CustomerAddedStateBenefit]] = None,
                                          otherStateBenefits: Option[Set[CustomerAddedStateBenefit]] = None)

object CustomerAddedStateBenefitsData {

  implicit val customerAddedStateBenefitsDataWrites: OWrites[CustomerAddedStateBenefitsData] = (data: CustomerAddedStateBenefitsData) =>
    jsonObjNoNulls(
      "incapacityBenefit"          -> data.incapacityBenefits,
      "statePension"               -> data.statePensions,
      "statePensionLumpSum"        -> data.statePensionLumpSums,
      "employmentSupportAllowance" -> data.employmentSupportAllowances,
      "jobSeekersAllowance"        -> data.jobSeekersAllowances,
      "bereavementAllowance"       -> data.bereavementAllowances,
      "otherStateBenefits"         -> data.otherStateBenefits
    )

  implicit val customerAddedStateBenefitsDataReads: Reads[CustomerAddedStateBenefitsData] = (
    (JsPath \ "incapacityBenefit").readNullable[Set[CustomerAddedStateBenefit]] and
      (JsPath \ "statePension").readNullable[Set[CustomerAddedStateBenefit]] and
      (JsPath \ "statePensionLumpSum").readNullable[Set[CustomerAddedStateBenefit]] and
      (JsPath \ "employmentSupportAllowance").readNullable[Set[CustomerAddedStateBenefit]] and
      (JsPath \ "jobSeekersAllowance").readNullable[Set[CustomerAddedStateBenefit]] and
      (JsPath \ "bereavementAllowance").readNullable[Set[CustomerAddedStateBenefit]] and
      (JsPath \ "otherStateBenefits").readNullable[Set[CustomerAddedStateBenefit]]
  )(CustomerAddedStateBenefitsData.apply _)
}
