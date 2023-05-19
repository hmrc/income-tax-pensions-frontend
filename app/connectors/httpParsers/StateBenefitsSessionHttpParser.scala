
package connectors.httpParsers

import models.APIErrorModel
import play.api.http.Status._
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import utils.PagerDutyHelper.PagerDutyKeys._
import utils.PagerDutyHelper.pagerDutyLog

object StateBenefitsSessionHttpParser extends APIParser {
  type StateBenefitsSessionResponse = Either[APIErrorModel, Unit]

  override val parserName: String = "StateBenefitsSessionHttpParser"
  override val service: String = "income-tax-state-benefits"

  implicit object RefreshIncomeSourceHttpReads extends HttpReads[StateBenefitsSessionResponse] {

    override def read(method: String, url: String, response: HttpResponse): StateBenefitsSessionResponse = {
      response.status match {
        case NO_CONTENT | NOT_FOUND => Right(())
        case BAD_REQUEST =>
          pagerDutyLog(FOURXX_RESPONSE_FROM_API, logMessage(response))
          handleAPIError(response)
        case INTERNAL_SERVER_ERROR =>
          pagerDutyLog(INTERNAL_SERVER_ERROR_FROM_API, logMessage(response))
          handleAPIError(response)
        case SERVICE_UNAVAILABLE =>
          pagerDutyLog(SERVICE_UNAVAILABLE_FROM_API, logMessage(response))
          handleAPIError(response)
        case _ =>
          pagerDutyLog(UNEXPECTED_RESPONSE_FROM_API, logMessage(response))
          handleAPIError(response, Some(INTERNAL_SERVER_ERROR))
      }
    }
  }
}
