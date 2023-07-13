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

import models.pension.charges.OverseasRefundPensionScheme

object OverseasRefundPensionSchemeBuilder {

  val anOverseasRefundPensionSchemeWithUkRefundCharge: OverseasRefundPensionScheme =
    OverseasRefundPensionScheme(
      ukRefundCharge = Some(true),
      name = Some("Scheme Name with UK charge"),
      pensionSchemeTaxReference = Some("12345678RA"),
      qualifyingRecognisedOverseasPensionScheme = None,
      providerAddress = Some("Scheme Address 1"),
      alphaTwoCountryCode = None,
      alphaThreeCountryCode = None
    )

  val anOverseasRefundPensionSchemeWithoutUkRefundCharge: OverseasRefundPensionScheme =
    OverseasRefundPensionScheme(
      ukRefundCharge = Some(false),
      name = Some("Scheme Name without UK charge"),
      pensionSchemeTaxReference = None,
      qualifyingRecognisedOverseasPensionScheme = Some("123456"),
      providerAddress = Some("Scheme Address 2"),
      alphaTwoCountryCode = Some("FR"),
      alphaThreeCountryCode = Some("FRA")
    )

  val anEmptyOverseasRefundPensionScheme: OverseasRefundPensionScheme = OverseasRefundPensionScheme()

}
