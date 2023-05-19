
package models.pension.statebenefits

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.crypto.EncryptedValue

import java.time.{Instant, LocalDate}
import java.util.UUID

case class ClaimCYAModel(benefitId: Option[UUID] = None,
                         startDate: LocalDate,
                         endDateQuestion: Option[Boolean] = None,
                         endDate: Option[LocalDate] = None,
                         dateIgnored: Option[Instant] = None,
                         submittedOn: Option[Instant] = None,
                         amount: Option[BigDecimal] = None,
                         taxPaidQuestion: Option[Boolean] = None,
                         taxPaid: Option[BigDecimal] = None) {

  object ClaimCYAModel {
    implicit val format: OFormat[ClaimCYAModel] = Json.format[ClaimCYAModel]
  }
}
