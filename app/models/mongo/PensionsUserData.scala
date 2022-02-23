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

package models.mongo

import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.mongo.play.json.formats.MongoJodaFormats

case class PensionsUserData(sessionId: String,
                              mtdItId: String,
                              nino: String,
                              taxYear: Int,
                              isPriorSubmission: Boolean,
                              pensions: PensionsCYAModel,
                              lastUpdated: DateTime = DateTime.now(DateTimeZone.UTC))

object PensionsUserData extends MongoJodaFormats {

  implicit val mongoJodaDateTimeFormats: Format[DateTime] = dateTimeFormat

  implicit val formats: Format[PensionsUserData] = Json.format[PensionsUserData]
}

case class EncryptedPensionsUserData(sessionId: String,
                                mtdItId: String,
                                nino: String,
                                taxYear: Int,
                                isPriorSubmission: Boolean,
                                pensions: EncryptedPensionCYAModel,
                                lastUpdated: DateTime = DateTime.now(DateTimeZone.UTC))

object EncryptedPensionsUserData extends MongoJodaFormats {

  implicit val mongoJodaDateTimeFormats: Format[DateTime] = dateTimeFormat

  implicit val formats: Format[EncryptedPensionsUserData] = Json.format[EncryptedPensionsUserData]
}
