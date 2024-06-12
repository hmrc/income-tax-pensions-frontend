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

import builders.PensionChargesBuilder.anPensionCharges
import utils.UnitTest

class CreateUpdatePensionChargesRequestModelSpec extends UnitTest {

  ".otherSubModelsEmpty" should {

    val annualAllowancesPensionSubModel =
      AnnualAllowancesPensionCharges(anPensionCharges.pensionContributions)
    Seq(
      anPensionCharges.pensionContributions,
      anPensionCharges.overseasPensionContributions,
      anPensionCharges.pensionSchemeUnauthorisedPayments,
      anPensionCharges.pensionSchemeOverseasTransfers,
      Some(annualAllowancesPensionSubModel)
    ).foreach { subModel =>
      s"be false when subModel is non empty ${subModel.get.getClass.getName} and other models are not empty" in {
        val actualResult = CreateUpdatePensionChargesRequestModel(
          anPensionCharges.pensionSchemeOverseasTransfers,
          anPensionCharges.pensionSchemeUnauthorisedPayments,
          anPensionCharges.pensionContributions,
          anPensionCharges.overseasPensionContributions
        ).otherSubRequestModelsEmpty(subModel)
        actualResult shouldBe false
      }

      s"be true when ${subModel.get.getClass.getName} subModel is non empty and other models are empty" in {
        val actualResult = CreateUpdatePensionChargesRequestModel(
          None,
          None,
          None,
          None
        ).otherSubRequestModelsEmpty(subModel)
        actualResult shouldBe true
      }

      s"be true when ${subModel.get.getClass.getName} subModel is non empty and the other models are either empty or have empty contents" in {
        val actualResult = CreateUpdatePensionChargesRequestModel(
          None,
          anPensionCharges.pensionSchemeUnauthorisedPayments.map(_.copy(None, None, None)),
          None,
          None
        ).otherSubRequestModelsEmpty(subModel)

        actualResult shouldBe true
      }

      s"be false when ${subModel.get.getClass.getName} subModel is non empty and some other models have non-empty contents" in {
        val actualResult = CreateUpdatePensionChargesRequestModel(
          anPensionCharges.pensionSchemeOverseasTransfers,
          anPensionCharges.pensionSchemeUnauthorisedPayments.map(_.copy(Some(Seq("PSTR")), None, None)),
          None,
          None
        ).otherSubRequestModelsEmpty(subModel)
        actualResult shouldBe false
      }
    }
  }
}
