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

import models.pension.charges.{PaymentsIntoOverseasPensionsViewModel, Relief}
import models.pension.charges.TaxReliefQuestion.{MigrantMemberRelief, TransitionalCorrespondingRelief}

object PaymentsIntoOverseasPensionsViewModelBuilder {

  val aPaymentsIntoOverseasPensionsViewModel: PaymentsIntoOverseasPensionsViewModel = PaymentsIntoOverseasPensionsViewModel(
    paymentsIntoOverseasPensionsQuestions = Some(true),
    paymentsIntoOverseasPensionsAmount = Some(1999.99),
    employerPaymentsQuestion = Some(true),
    taxPaidOnEmployerPaymentsQuestion = Some(false),
    reliefs = Seq(
      Relief(
        reliefType = Some(TransitionalCorrespondingRelief),
        customerReferenceNumberQuestion = Some("PENSIONINCOME245"),
        employerPaymentsAmount = Some(1999.99),
        qualifyingOverseasPensionSchemeReferenceNumber = None,
        alphaTwoCountryCode = None,
        alphaThreeCountryCode = None,
        doubleTaxationCountryArticle = None,
        doubleTaxationCountryTreaty = None,
        doubleTaxationReliefAmount = None,
        sf74Reference = Some("SF74-123456")),
      Relief(
        reliefType = Some(MigrantMemberRelief),
        customerReferenceNumberQuestion = None,
        employerPaymentsAmount = Some(100.00),
        qualifyingOverseasPensionSchemeReferenceNumber = Some("123456"),
        alphaTwoCountryCode = None,
        alphaThreeCountryCode = None,
        doubleTaxationCountryArticle = None,
        doubleTaxationCountryTreaty = None,
        doubleTaxationReliefAmount = None,
        sf74Reference = None)
    )
  )


  val aPaymentsIntoOverseasPensionsEmptyViewModel: PaymentsIntoOverseasPensionsViewModel = PaymentsIntoOverseasPensionsViewModel()

}
