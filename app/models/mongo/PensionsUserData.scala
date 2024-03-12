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

package models.mongo

import common.TaxYear
import models.User
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.{Clock, LocalDate, ZoneOffset, ZonedDateTime}

case class PensionsUserData(sessionId: String,
                            mtdItId: String,
                            nino: String,
                            taxYear: Int,
                            isPriorSubmission: Boolean,
                            pensions: PensionsCYAModel,
                            lastUpdated: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC))

object PensionsUserData extends MongoJavatimeFormats {

  implicit val mongoJodaDateTimeFormats: Format[LocalDate] = localDateFormat

  implicit val formats: Format[PensionsUserData] = Json.format[PensionsUserData]

  def empty(user: User, taxYear: TaxYear): PensionsUserData =
    PensionsUserData(
      user.sessionId,
      user.mtditid,
      user.nino,
      taxYear.endYear,
      isPriorSubmission = false,
      PensionsCYAModel.emptyModels,
      Clock.systemUTC().instant().atZone(ZoneOffset.UTC)
    )

}

case class EncryptedPensionsUserData(sessionId: String,
                                     mtdItId: String,
                                     nino: String,
                                     taxYear: Int,
                                     isPriorSubmission: Boolean,
                                     pensions: EncryptedPensionCYAModel,
                                     lastUpdated: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC))

object EncryptedPensionsUserData extends MongoJavatimeFormats {

  implicit val mongoJodaDateTimeFormats: Format[LocalDate] = localDateFormat

  implicit val formats: Format[EncryptedPensionsUserData] = Json.format[EncryptedPensionsUserData]
}
