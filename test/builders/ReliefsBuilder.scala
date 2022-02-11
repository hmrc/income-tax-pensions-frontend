/*
 * Copyright 2022 HM Revenue & Customs
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

import models.pension.reliefs.Reliefs

object ReliefsBuilder {

  val anReliefs: Reliefs = Reliefs(regularPensionContributions = Some(100.00),
    oneOffPensionContributionsPaid = Some(200.00),
    retirementAnnuityPayments = Some(300.00),
    paymentToEmployersSchemeNoTaxRelief = Some(400.00),
    overseasPensionSchemeContributions = Some(500.00)

  )
}
