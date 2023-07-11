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

package builders

import models.pension.charges.Relief
import models.pension.charges.TaxReliefQuestion.{DoubleTaxationRelief, MigrantMemberRelief, NoTaxRelief, TransitionalCorrespondingRelief}

object ReliefBuilder {

  val aTransitionalCorrespondingRelief: Relief = Relief(
      reliefType = Some(TransitionalCorrespondingRelief),
      customerReference = Some("tcrPENSIONINCOME2000"),
      employerPaymentsAmount = Some(1999.99),
      qopsReference = None,
      alphaTwoCountryCode = None,
      alphaThreeCountryCode = None,
      doubleTaxationArticle = None,
      doubleTaxationTreaty = None,
      doubleTaxationReliefAmount = None,
      sf74Reference = Some("SF74-123456"))

  val aMigrantMemberRelief: Relief = Relief(
      reliefType = Some(MigrantMemberRelief),
      customerReference = Some("mmrPENSIONINCOME356"),
      employerPaymentsAmount = Some(356.00),
      qopsReference = Some("123456"),
      alphaTwoCountryCode = None,
      alphaThreeCountryCode = None,
      doubleTaxationArticle = None,
      doubleTaxationTreaty = None,
      doubleTaxationReliefAmount = None,
      sf74Reference = None)

  val aDoubleTaxationRelief: Relief = Relief(
      reliefType = Some(DoubleTaxationRelief),
      customerReference = Some("dtrPENSIONINCOME550"),
      employerPaymentsAmount = Some(550.00),
      qopsReference = Some("123456"),
      alphaTwoCountryCode = Some("AG"),
      alphaThreeCountryCode = Some("ATG"),
      doubleTaxationArticle = None,
      doubleTaxationTreaty = None,
      doubleTaxationReliefAmount = Some(55),
      sf74Reference = None)

  val aNoTaxRelief: Relief = Relief(
      reliefType = Some(NoTaxRelief),
      customerReference = Some("noPENSIONINCOME100"),
      employerPaymentsAmount = Some(100),
      qopsReference = None,
      alphaTwoCountryCode = None,
      alphaThreeCountryCode = None,
      doubleTaxationArticle = None,
      doubleTaxationTreaty = None,
      doubleTaxationReliefAmount = None,
      sf74Reference = None)

}
