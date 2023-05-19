
package connectors

import config.AppConfig
import connectors.httpParsers.PensionReliefsSessionHttpParser.PensionReliefsSessionHttpReads
import connectors.httpParsers.StateBenefitsSessionHttpParser.StateBenefitsSessionResponse
import models.mongo.StateBenefitsUserData
import models.pension.reliefs.CreateOrUpdatePensionReliefsModel
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class StateBenefitsConnector @Inject()(val http: HttpClient,
                                      val appConfig: AppConfig) {

  def createSessionDataID(model: StateBenefitsUserData)
                         (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[StateBenefitsSessionResponse] = {
    val url = appConfig.statePensionBEBaseUrl + "/session-data"
    http.POST[StateBenefitsUserData, StateBenefitsSessionResponse](
      url, model)(StateBenefitsUserData.format.writes, PensionReliefsSessionHttpReads, hc, ec)
  }

  def saveSessionData(nino: String, sessionDataId: Int, model: CreateOrUpdatePensionReliefsModel)
                     (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[StateBenefitsSessionResponse] = {
    val url = appConfig.statePensionBEBaseUrl + s"/session-data/nino/$nino/session/$sessionDataId"
    http.PUT[CreateOrUpdatePensionReliefsModel, StateBenefitsSessionResponse](
      url, model)(CreateOrUpdatePensionReliefsModel.format.writes, PensionReliefsSessionHttpReads, hc, ec)
  }

  def saveClaimData(nino: String, sessionDataId: Int, model: CreateOrUpdatePensionReliefsModel)
                     (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[StateBenefitsSessionResponse] = {
    val url = appConfig.statePensionBEBaseUrl + s"/claim-data/nino/$nino/session/$sessionDataId"
    http.PUT[CreateOrUpdatePensionReliefsModel, StateBenefitsSessionResponse](
      url, model)(CreateOrUpdatePensionReliefsModel.format.writes, PensionReliefsSessionHttpReads, hc, ec)
  }

}
