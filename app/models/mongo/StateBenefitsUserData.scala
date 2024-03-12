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

import models.pension.statebenefits.{ClaimCYAModel, EncryptedClaimCYAModel}
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json._
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import utils.AesGcmAdCrypto
import utils.JsonUtils.jsonObjNoNulls

import java.time.Instant
import java.util.UUID

case class StateBenefitsUserData(benefitType: String,
                                 sessionDataId: Option[UUID] = None,
                                 sessionId: String,
                                 mtdItId: String,
                                 nino: String,
                                 taxYear: Int,
                                 benefitDataType: String,
                                 claim: Option[ClaimCYAModel],
                                 lastUpdated: Instant = Instant.ofEpochMilli(Instant.now().toEpochMilli)) {

  def encrypted(implicit aesGcmAdCrypto: AesGcmAdCrypto, associatedText: String): EncryptedStateBenefitsUserData = EncryptedStateBenefitsUserData(
    benefitType: String,
    sessionDataId = sessionDataId,
    sessionId = sessionId,
    mtdItId = mtdItId,
    nino = nino,
    taxYear = taxYear,
    benefitDataType = benefitDataType,
    claim = claim.map(_.encrypted),
    lastUpdated = lastUpdated
  )
}

object StateBenefitsUserData {
  implicit val mongoLocalDateTimeFormats: Format[Instant] = MongoJavatimeFormats.instantFormat

  implicit val stateBenefitsUserDataWrites: OWrites[StateBenefitsUserData] = (data: StateBenefitsUserData) =>
    jsonObjNoNulls(
      "benefitType"     -> data.benefitType,
      "sessionDataId"   -> data.sessionDataId,
      "sessionId"       -> data.sessionId,
      "mtdItId"         -> data.mtdItId,
      "nino"            -> data.nino,
      "taxYear"         -> data.taxYear,
      "benefitDataType" -> data.benefitDataType,
      "claim"           -> data.claim,
      "lastUpdated"     -> data.lastUpdated
    )

  implicit val stateBenefitsUserDataReads: Reads[StateBenefitsUserData] = (
    (JsPath \ "benefitType").read[String] and
      (JsPath \ "sessionDataId").readNullable[UUID] and
      (JsPath \ "sessionId").read[String] and
      (JsPath \ "mtdItId").read[String] and
      (JsPath \ "nino").read[String] and
      (JsPath \ "taxYear").read[Int] and
      (JsPath \ "benefitDataType").read[String] and
      (JsPath \ "claim").readNullable[ClaimCYAModel] and
      (JsPath \ "lastUpdated").readWithDefault[Instant](Instant.now())
  )(StateBenefitsUserData.apply _)
}

case class EncryptedStateBenefitsUserData(benefitType: String,
                                          sessionDataId: Option[UUID] = None,
                                          sessionId: String,
                                          mtdItId: String,
                                          nino: String,
                                          taxYear: Int,
                                          benefitDataType: String,
                                          claim: Option[EncryptedClaimCYAModel],
                                          lastUpdated: Instant = Instant.now()) {

  def decrypted(implicit aesGcmAdCrypto: AesGcmAdCrypto, associatedText: String): StateBenefitsUserData = StateBenefitsUserData(
    benefitType = benefitType,
    sessionDataId = sessionDataId,
    sessionId = sessionId,
    mtdItId = mtdItId,
    nino = nino,
    taxYear = taxYear,
    benefitDataType = benefitDataType,
    claim = claim.map(_.decrypted),
    lastUpdated = lastUpdated
  )
}

object EncryptedStateBenefitsUserData {
  implicit val mongoLocalDateTimeFormats: Format[Instant]      = MongoJavatimeFormats.instantFormat
  implicit val format: OFormat[EncryptedStateBenefitsUserData] = Json.format[EncryptedStateBenefitsUserData]
}
