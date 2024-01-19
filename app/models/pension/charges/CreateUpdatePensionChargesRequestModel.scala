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

package models.pension.charges

import models.pension.{PensionChargesSubRequestModel, PensionRequestModel, PensionSubRequestModel}
import play.api.libs.json.{Json, OFormat}

case class CreateUpdatePensionChargesRequestModel(pensionSavingsTaxCharges: Option[PensionSavingsTaxCharges],
                                                  pensionSchemeOverseasTransfers: Option[PensionSchemeOverseasTransfers],
                                                  pensionSchemeUnauthorisedPayments: Option[PensionSchemeUnauthorisedPayments],
                                                  pensionContributions: Option[PensionContributions],
                                                  overseasPensionContributions: Option[OverseasPensionContributions]) extends PensionRequestModel {


  
  override def otherSubRequestModelsEmpty[PensionChargesRequestSubModel <: PensionSubRequestModel]  //scalastyle:off cyclomatic.complexity
  (excludedModel: Option[PensionChargesRequestSubModel]): Boolean = {
    excludedModel match {
      case Some(_: AnnualAllowancesPensionCharges) =>
        pensionSchemeOverseasTransfers.forall(_.isEmpty) &&
          pensionSchemeUnauthorisedPayments.forall(_.isEmpty) &&
          overseasPensionContributions.forall(_.isEmpty)
      case Some(_: PensionSavingsTaxCharges) =>
        pensionSchemeOverseasTransfers.forall(_.isEmpty) &&
          pensionSchemeUnauthorisedPayments.forall(_.isEmpty) &&
          pensionContributions.forall(_.isEmpty) &&
          overseasPensionContributions.forall(_.isEmpty)
      case Some(_: PensionSchemeOverseasTransfers) =>
        pensionSavingsTaxCharges.forall(_.isEmpty) &&
          pensionSchemeUnauthorisedPayments.forall(_.isEmpty) &&
          pensionContributions.forall(_.isEmpty) &&
          overseasPensionContributions.forall(_.isEmpty)
      case Some(_: PensionSchemeUnauthorisedPayments) =>
        pensionSavingsTaxCharges.forall(_.isEmpty) &&
          pensionSchemeOverseasTransfers.forall(_.isEmpty) &&
          pensionContributions.forall(_.isEmpty) &&
          overseasPensionContributions.forall(_.isEmpty)
      case Some(_: PensionContributions) =>
        pensionSavingsTaxCharges.forall(_.isEmpty) &&
          pensionSchemeOverseasTransfers.forall(_.isEmpty) &&
          pensionSchemeUnauthorisedPayments.forall(_.isEmpty) &&
          overseasPensionContributions.forall(_.isEmpty)
      case Some(_: OverseasPensionContributions) =>
        pensionSavingsTaxCharges.forall(_.isEmpty) &&
          pensionSchemeOverseasTransfers.forall(_.isEmpty) &&
          pensionSchemeUnauthorisedPayments.forall(_.isEmpty) &&
          pensionContributions.forall(_.isEmpty)
      case _ =>
       pensionSavingsTaxCharges.forall(_.isEmpty) &&
          pensionSchemeOverseasTransfers.forall(_.isEmpty) &&
          pensionSchemeUnauthorisedPayments.forall(_.isEmpty) &&
          pensionContributions.forall(_.isEmpty) &&
          overseasPensionContributions.forall(_.isEmpty)
    }
  }
  


  def createSubModel: CreateUpdatePensionChargesRequestModel = {
    def processModel[T <: PensionChargesSubRequestModel](model: Option[T]): Option[T] = {
      if (model.exists(_.isEmpty) || model.isEmpty) {
        None
      } else {
        model
      }
    }

    CreateUpdatePensionChargesRequestModel(
      pensionSavingsTaxCharges = processModel(this.pensionSavingsTaxCharges),
      pensionSchemeOverseasTransfers = processModel(this.pensionSchemeOverseasTransfers),
      pensionSchemeUnauthorisedPayments = processModel(this.pensionSchemeUnauthorisedPayments),
      pensionContributions = processModel(this.pensionContributions),
      overseasPensionContributions = processModel(this.overseasPensionContributions)
    )
  }
}

object CreateUpdatePensionChargesRequestModel {
  implicit val format: OFormat[CreateUpdatePensionChargesRequestModel] = Json.format[CreateUpdatePensionChargesRequestModel]
}
