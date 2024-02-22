package connectors

import models.logging.HeaderCarrierExtensions
import models.logging.HeaderCarrierExtensions.HeaderCarrierOps
import uk.gov.hmrc.http.HeaderCarrier

object Connector {
  def hcWithCorrelationId(implicit hc: HeaderCarrier): HeaderCarrier = {
    val explicitHeaders = hc.maybeCorrelationId.map(HeaderCarrierExtensions.CorrelationIdHeaderKey -> _).toList
    hcWithCorrelationId.withExtraHeaders(explicitHeaders: _*)
  }
}
