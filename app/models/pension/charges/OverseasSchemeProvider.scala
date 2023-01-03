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

import play.api.libs.json.{Json, OFormat}
import utils.EncryptedValue

case class OverseasSchemeProvider(providerName: String,
                                  providerAddress: String,
                                  providerCountryCode: String,
                                  qualifyingRecognisedOverseasPensionScheme: Option[Seq[String]],
                                  pensionSchemeTaxReference: Option[Seq[String]]
                                 )

object OverseasSchemeProvider {
  implicit val format: OFormat[OverseasSchemeProvider] = Json.format[OverseasSchemeProvider]
}

case class EncryptedOverseasSchemeProvider(providerName: EncryptedValue,
                                           providerAddress: EncryptedValue,
                                           providerCountryCode: EncryptedValue,
                                           qualifyingRecognisedOverseasPensionScheme: Option[Seq[EncryptedValue]],
                                           pensionSchemeTaxReference: Option[Seq[EncryptedValue]]
                                          )

object EncryptedOverseasSchemeProvider {
  implicit val format: OFormat[EncryptedOverseasSchemeProvider] = Json.format[EncryptedOverseasSchemeProvider]
}
