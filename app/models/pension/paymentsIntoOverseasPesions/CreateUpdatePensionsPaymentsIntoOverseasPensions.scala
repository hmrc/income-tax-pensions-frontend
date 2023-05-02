
package models.pension.paymentsIntoOverseasPesions

import models.pension.income.PensionIncome
import models.pension.reliefs.PensionReliefs
import play.api.libs.json.{Json, OFormat}

case class CreateUpdatePensionsPaymentsIntoOverseasPensions
                                              (reliefs: Option[PensionReliefs],
                                               income: Option[PensionIncome]
                                              )

object CreateUpdatePensionsPaymentsIntoOverseasPensions {
  implicit val format: OFormat[CreateUpdatePensionsPaymentsIntoOverseasPensions] = Json.format[CreateUpdatePensionsPaymentsIntoOverseasPensions]
}