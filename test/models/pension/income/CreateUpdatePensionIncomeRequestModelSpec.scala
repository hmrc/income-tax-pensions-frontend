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

package models.pension.income

import builders.PensionIncomeBuilder.aCreateUpdatePensionIncomeModel
import utils.UnitTest

class CreateUpdatePensionIncomeRequestModelSpec extends UnitTest {

  ".otherSubModelsEmpty" should {

    Seq(
      aCreateUpdatePensionIncomeModel.foreignPension,
      aCreateUpdatePensionIncomeModel.overseasPensionContribution
    ).foreach { subModel =>
      s"be false when subModel is non empty ${subModel.get.getClass.getName} and other models are not empty" in {
        val actualResult = CreateUpdatePensionIncomeRequestModel(
          aCreateUpdatePensionIncomeModel.foreignPension,
          aCreateUpdatePensionIncomeModel.overseasPensionContribution
        ).otherSubRequestModelsEmpty(subModel)
        actualResult shouldBe false
      }

      s"be true when subModel is non empty ${subModel.get.getClass.getName} and other models are empty" in {
        val actualResult = CreateUpdatePensionIncomeRequestModel(
          None,
          None
        ).otherSubRequestModelsEmpty(subModel)
        actualResult shouldBe true
      }

      s"be true when subModel is non empty ${subModel.get.getClass.getName} and other models are empty and foreignPension model contents is empty" in {
        val actualResult = CreateUpdatePensionIncomeRequestModel(
          aCreateUpdatePensionIncomeModel.foreignPension.map(_.copy(Seq.empty)),
          aCreateUpdatePensionIncomeModel.overseasPensionContribution
        ).otherSubRequestModelsEmpty(subModel)

        actualResult shouldBe (if (subModel.get.isInstanceOf[ForeignPensionContainer]) false else true)
      }

      s"be false when subModel is non empty ${subModel.get.getClass.getName} and other models are empty but foreignPension model contents are non empty" in {
        val actualResult = CreateUpdatePensionIncomeRequestModel(
          aCreateUpdatePensionIncomeModel.foreignPension.map(_.copy(aCreateUpdatePensionIncomeModel.foreignPension.get.fp)),
          aCreateUpdatePensionIncomeModel.overseasPensionContribution
        ).otherSubRequestModelsEmpty(subModel)
        actualResult shouldBe false
      }
    }
  }
}
