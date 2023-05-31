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

package models.pension.charges

import models.pension.{PensionChargesRequestSubModel, PensionModelRequest, PensionRequestSubModel}
import play.api.libs.json.{Json, OFormat}

case class CreateUpdatePensionChargesRequestModel(pensionSavingsTaxCharges: Option[PensionSavingsTaxCharges],
                                                  pensionSchemeOverseasTransfers: Option[PensionSchemeOverseasTransfers],
                                                  pensionSchemeUnauthorisedPayments: Option[PensionSchemeUnauthorisedPayments],
                                                  pensionContributions: Option[PensionContributions],
                                                  overseasPensionContributions: Option[OverseasPensionContributions]) extends PensionModelRequest {


  //scalastyle:off
  override def otherSubModelsEmpty[PensionChargesRequestSubModel <: PensionRequestSubModel](excludedModel: Option[PensionChargesRequestSubModel]): Boolean = {
    excludedModel match {
      case Some(_: PensionSavingsTaxCharges) =>
        (pensionSchemeOverseasTransfers.isEmpty || pensionSchemeOverseasTransfers.exists(_.isEmpty)) &&
          (pensionSchemeUnauthorisedPayments.isEmpty || pensionSchemeUnauthorisedPayments.exists(_.isEmpty)) &&
          (pensionContributions.isEmpty || pensionContributions.exists(_.isEmpty)) &&
          (overseasPensionContributions.isEmpty || overseasPensionContributions.exists(_.isEmpty))
      case Some(_: PensionSchemeOverseasTransfers) =>
        (pensionSavingsTaxCharges.isEmpty || pensionSavingsTaxCharges.exists(_.isEmpty)) &&
          (pensionSchemeUnauthorisedPayments.isEmpty || pensionSchemeUnauthorisedPayments.exists(_.isEmpty)) &&
          (pensionContributions.isEmpty || pensionContributions.exists(_.isEmpty)) &&
        (overseasPensionContributions.isEmpty || overseasPensionContributions.exists(_.isEmpty))
      case Some(_: PensionSchemeUnauthorisedPayments) =>
        (pensionSavingsTaxCharges.isEmpty || pensionSavingsTaxCharges.exists(_.isEmpty)) &&
          (pensionSchemeOverseasTransfers.isEmpty || pensionSchemeOverseasTransfers.exists(_.isEmpty)) &&
          (pensionContributions.isEmpty || pensionContributions.exists(_.isEmpty))
        (overseasPensionContributions.isEmpty || overseasPensionContributions.exists(_.isEmpty))
      case Some(_: PensionContributions) =>
        (pensionSavingsTaxCharges.isEmpty || pensionSavingsTaxCharges.exists(_.isEmpty)) &&
          (pensionSchemeOverseasTransfers.isEmpty || pensionSchemeOverseasTransfers.exists(_.isEmpty)) &&
          (pensionSchemeUnauthorisedPayments.isEmpty || pensionSchemeUnauthorisedPayments.exists(_.isEmpty)) &&
          (overseasPensionContributions.isEmpty || overseasPensionContributions.exists(_.isEmpty))
      case Some(_: OverseasPensionContributions) =>
        (pensionSavingsTaxCharges.isEmpty || pensionSavingsTaxCharges.exists(_.isEmpty)) &&
          (pensionSchemeOverseasTransfers.isEmpty || pensionSchemeOverseasTransfers.exists(_.isEmpty)) &&
          (pensionSchemeUnauthorisedPayments.isEmpty || pensionSchemeUnauthorisedPayments.exists(_.isEmpty)) &&
          pensionContributions.isEmpty
      case _ =>
        (pensionSavingsTaxCharges.isEmpty || pensionSavingsTaxCharges.exists(_.isEmpty)) &&
          (pensionSchemeOverseasTransfers.isEmpty || pensionSchemeOverseasTransfers.exists(_.isEmpty)) &&
          (pensionSchemeUnauthorisedPayments.isEmpty || pensionSchemeUnauthorisedPayments.exists(_.isEmpty)) &&
          (pensionContributions.isEmpty || pensionContributions.exists(_.isEmpty))
        (overseasPensionContributions.isEmpty || overseasPensionContributions.exists(_.isEmpty))
    }
  }
  //scalastyle:on


  def createSubModel: CreateUpdatePensionChargesRequestModel = {
    def processModel[T <: PensionChargesRequestSubModel](model: Option[T]): Option[T] = {
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
