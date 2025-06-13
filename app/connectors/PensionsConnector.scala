/*
 * Copyright 2025 HM Revenue & Customs
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

package connectors

import cats.data.EitherT
import common.{Nino, TaxYear}
import config.AppConfig
import connectors.httpParsers.GetJourneyStatusesHttpParser.GetJourneyStatusesHttpReads
import models.domain.ApiResultT
import models.logging.ConnectorRequestInfo
import models.mongo.{JourneyContext, JourneyStatus}
import models.pension.Journey._
import models.pension.charges._
import models.pension.reliefs.PaymentsIntoPensionsViewModel
import models.pension.statebenefits.IncomeFromPensionsViewModel
import models.pension.{IncomeFromPensionsStatePensionAnswers, JourneyNameAndStatus}
import play.api.Logging
import play.api.libs.json.Json
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads}

import java.net.URI
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class PensionsConnector @Inject() (val httpClientV2: HttpClientV2, val appConfig: AppConfig) extends Logging {

  private val apiId                 = "income-tax-pensions"
  private def buildUrl(url: String) = s"${appConfig.pensionBEBaseUrl}$url"

  def savePaymentsIntoPensions(nino: Nino,
                               taxYear: TaxYear,
                               answers: PaymentsIntoPensionsViewModel
                              )(implicit hc: HeaderCarrier, ec: ExecutionContext): ApiResultT[Unit] = {
    implicit val reads: HttpReads[DownstreamErrorOr[Unit]] = NoContentHttpReads
    val url: URI = new URI(appConfig.journeyAnswersUrl(taxYear, nino, PaymentsIntoPensions))

    ConnectorRequestInfo("PUT", url.toString, apiId).logRequestWithBody(logger, answers)

      EitherT{
        httpClientV2
          .put(url.toURL)
          .withBody(Json.toJson(answers))
          .execute[DownstreamErrorOr[Unit]](reads, ec)
      }
  }

  def getPaymentsIntoPensions(nino: Nino,
                              taxYear: TaxYear
                             )(implicit hc: HeaderCarrier, ec: ExecutionContext): ApiResultT[Option[PaymentsIntoPensionsViewModel]] = {
    val url: URI = new URI(appConfig.journeyAnswersUrl(taxYear, nino, PaymentsIntoPensions))

    ConnectorRequestInfo("GET", url.toString, apiId).logRequest(logger)

    EitherT{
      httpClientV2
        .get(url.toURL)
        .execute[DownstreamErrorOr[Option[PaymentsIntoPensionsViewModel]]]
    }
  }

  def getUkPensionIncome(nino: Nino,
                         taxYear: TaxYear
                        )(implicit hc: HeaderCarrier, ec: ExecutionContext): ApiResultT[Option[IncomeFromPensionsViewModel]] = {
    val url: URI = new URI(appConfig.journeyAnswersUrl(taxYear, nino, UkPensionIncome))
    ConnectorRequestInfo("GET", url.toString, apiId).logRequest(logger)

    EitherT{
      httpClientV2
        .get(url.toURL)
        .execute[DownstreamErrorOr[Option[IncomeFromPensionsViewModel]]]
    }
  }

  def saveUkPensionIncome(nino: Nino,
                          taxYear: TaxYear,
                          answers: IncomeFromPensionsViewModel
                         )(implicit hc: HeaderCarrier, ec: ExecutionContext): ApiResultT[Unit] = {
    implicit val reads: HttpReads[DownstreamErrorOr[Unit]] = NoContentHttpReads
    val url: URI = new URI(appConfig.journeyAnswersUrl(taxYear, nino, UkPensionIncome))

    ConnectorRequestInfo("PUT", url.toString, "income-tax-pensions").logRequestWithBody(logger, answers)

    EitherT{
      httpClientV2
        .put(url.toURL)
        .withBody(Json.toJson(answers))
        .execute[DownstreamErrorOr[Unit]](reads, ec)
    }
  }

  def getStatePension(nino: Nino,
                      taxYear: TaxYear
                     )(implicit hc: HeaderCarrier, ec: ExecutionContext): ApiResultT[Option[IncomeFromPensionsStatePensionAnswers]] = {
    val url: URI = new URI(appConfig.journeyAnswersUrl(taxYear, nino, StatePension))
    ConnectorRequestInfo("GET", url.toString, apiId).logRequest(logger)

    EitherT{
      httpClientV2
        .get(url.toURL)
        .execute[DownstreamErrorOr[Option[IncomeFromPensionsStatePensionAnswers]]]
    }
  }

  def saveStatePension(nino: Nino,
                       taxYear: TaxYear,
                       answers: IncomeFromPensionsStatePensionAnswers
                      )(implicit hc: HeaderCarrier, ec: ExecutionContext): ApiResultT[Unit] = {
    implicit val reads: HttpReads[DownstreamErrorOr[Unit]] = NoContentHttpReads
    val url: URI = new URI(appConfig.journeyAnswersUrl(taxYear, nino, StatePension))

    ConnectorRequestInfo("PUT", url.toString, apiId).logRequestWithBody(logger, answers)

    EitherT{
      httpClientV2
        .put(url.toURL)
        .withBody(Json.toJson(answers))
        .execute[DownstreamErrorOr[Unit]](reads, ec)
    }
  }

  def saveAnnualAllowances(nino: Nino,
                           taxYear: TaxYear,
                           answers: PensionAnnualAllowancesViewModel
                          )(implicit hc: HeaderCarrier, ec: ExecutionContext): ApiResultT[Unit] = {
    implicit val reads: HttpReads[DownstreamErrorOr[Unit]] = NoContentHttpReads
    val url: URI = new URI(appConfig.journeyAnswersUrl(taxYear, nino, AnnualAllowances))

    ConnectorRequestInfo("PUT", url.toString, apiId).logRequestWithBody(logger, answers)

    EitherT{
      httpClientV2
        .put(url.toURL)
        .withBody(Json.toJson(answers))
        .execute[DownstreamErrorOr[Unit]](reads, ec)
    }
  }

  def getAnnualAllowances(nino: Nino,
                          taxYear: TaxYear
                         )(implicit hc: HeaderCarrier, ec: ExecutionContext): ApiResultT[Option[PensionAnnualAllowancesViewModel]] = {
    val url: URI = new URI(appConfig.journeyAnswersUrl(taxYear, nino, AnnualAllowances))

    ConnectorRequestInfo("GET", url.toString, apiId).logRequest(logger)

    EitherT{
      httpClientV2
        .get(url.toURL)
        .execute[DownstreamErrorOr[Option[PensionAnnualAllowancesViewModel]]]
    }
  }

  def saveUnauthorisedPaymentsFromPensions(nino: Nino,
                                           taxYear: TaxYear,
                                           answers: UnauthorisedPaymentsViewModel
                                          )(implicit hc: HeaderCarrier, ec: ExecutionContext): ApiResultT[Unit] = {
    implicit val reads: HttpReads[DownstreamErrorOr[Unit]]= NoContentHttpReads
    val url: URI = new URI(appConfig.journeyAnswersUrl(taxYear, nino, UnauthorisedPayments))

    ConnectorRequestInfo("PUT", url.toString, apiId).logRequestWithBody(logger, answers)

    EitherT{
      httpClientV2
        .put(url.toURL)
        .withBody(Json.toJson(answers))
        .execute[DownstreamErrorOr[Unit]](reads, ec)
    }
  }

  def getUnauthorisedPaymentsFromPensions(nino: Nino,
                                          taxYear: TaxYear
                                         )(implicit hc: HeaderCarrier, ec: ExecutionContext): ApiResultT[Option[UnauthorisedPaymentsViewModel]] = {
    val url: URI = new URI(appConfig.journeyAnswersUrl(taxYear, nino, UnauthorisedPayments))

    ConnectorRequestInfo("GET", url.toString, apiId).logRequest(logger)

    EitherT{
      httpClientV2
        .get(url.toURL)
        .execute[DownstreamErrorOr[Option[UnauthorisedPaymentsViewModel]]]
    }
  }

  def getPaymentsIntoOverseasPensions(nino: Nino,
                                      taxYear: TaxYear
                                     )(implicit hc: HeaderCarrier, ec: ExecutionContext): ApiResultT[Option[PaymentsIntoOverseasPensionsViewModel]] = {
    val url: URI = new URI(appConfig.journeyAnswersUrl(taxYear, nino, PaymentsIntoOverseasPensions))

    ConnectorRequestInfo("GET", url.toString, apiId).logRequest(logger)

    EitherT{
      httpClientV2
        .get(url.toURL)
        .execute[DownstreamErrorOr[Option[PaymentsIntoOverseasPensionsViewModel]]]
    }
  }

  def savePaymentsIntoOverseasPensions(nino: Nino,
                                       taxYear: TaxYear,
                                       answers: PaymentsIntoOverseasPensionsViewModel
                                      )(implicit hc: HeaderCarrier, ec: ExecutionContext): ApiResultT[Unit] = {
    implicit val reads: HttpReads[DownstreamErrorOr[Unit]] = NoContentHttpReads
    val url: URI = new URI(appConfig.journeyAnswersUrl(taxYear, nino, PaymentsIntoOverseasPensions))

    ConnectorRequestInfo("PUT", url.toString, apiId).logRequestWithBody(logger, answers)

    EitherT{
      httpClientV2
        .put(url.toURL)
        .withBody(Json.toJson(answers))
        .execute[DownstreamErrorOr[Unit]](reads, ec)
    }
  }

  def saveTransfersIntoOverseasPensions(nino: Nino,
                                        taxYear: TaxYear,
                                        answers: TransfersIntoOverseasPensionsViewModel
                                       )(implicit hc: HeaderCarrier, ec: ExecutionContext): ApiResultT[Unit] = {
    implicit val reads: HttpReads[DownstreamErrorOr[Unit]] = NoContentHttpReads
    val url: URI = new URI(appConfig.journeyAnswersUrl(taxYear, nino, TransferIntoOverseasPensions))

    ConnectorRequestInfo("PUT", url.toString, apiId).logRequestWithBody(logger, answers)

    EitherT{
      httpClientV2
        .put(url.toURL)
        .withBody(Json.toJson(answers))
        .execute[DownstreamErrorOr[Unit]](reads, ec)
    }
  }

  def getTransfersIntoOverseasPensions(nino: Nino,
                                       taxYear: TaxYear
                                      )(implicit hc: HeaderCarrier, ec: ExecutionContext): ApiResultT[Option[TransfersIntoOverseasPensionsViewModel]] = {
    val url: URI = new URI(appConfig.journeyAnswersUrl(taxYear, nino, TransferIntoOverseasPensions))

    ConnectorRequestInfo("GET", url.toString, apiId).logRequest(logger)

    EitherT{
      httpClientV2
        .get(url.toURL)
        .execute[DownstreamErrorOr[Option[TransfersIntoOverseasPensionsViewModel]]]
    }
  }

  def getIncomeFromOverseasPensions(nino: Nino,
                                    taxYear: TaxYear
                                   )(implicit hc: HeaderCarrier, ec: ExecutionContext): ApiResultT[Option[IncomeFromOverseasPensionsViewModel]] = {
    val url: URI = new URI(appConfig.journeyAnswersUrl(taxYear, nino, IncomeFromOverseasPensions))
    ConnectorRequestInfo("GET", url.toString, apiId).logRequest(logger)

    EitherT{
      httpClientV2
        .get(url.toURL)
        .execute[DownstreamErrorOr[Option[IncomeFromOverseasPensionsViewModel]]]
    }
  }

  def saveIncomeFromOverseasPensions(nino: Nino,
                                     taxYear: TaxYear,
                                     answers: IncomeFromOverseasPensionsViewModel
                                    )(implicit hc: HeaderCarrier, ec: ExecutionContext): ApiResultT[Unit] = {
    implicit val reads: HttpReads[DownstreamErrorOr[Unit]] = NoContentHttpReads
    val url: URI = new URI(appConfig.journeyAnswersUrl(taxYear, nino, IncomeFromOverseasPensions))

    ConnectorRequestInfo("PUT", url.toString, "income-tax-pensions").logRequestWithBody(logger, answers)

    EitherT{
      httpClientV2
        .put(url.toURL)
        .withBody(Json.toJson(answers))
        .execute[DownstreamErrorOr[Unit]](reads, ec)
    }
  }

  def getShortServiceRefunds(nino: Nino,
                             taxYear: TaxYear
                            )(implicit hc: HeaderCarrier, ec: ExecutionContext): ApiResultT[Option[ShortServiceRefundsViewModel]] = {
    val url: URI = new URI(appConfig.journeyAnswersUrl(taxYear, nino, ShortServiceRefunds))
    ConnectorRequestInfo("GET", url.toString, apiId).logRequest(logger)

    EitherT{
      httpClientV2
        .get(url.toURL)
        .execute[DownstreamErrorOr[Option[ShortServiceRefundsViewModel]]]
    }
  }

  def saveShortServiceRefunds(nino: Nino,
                              taxYear: TaxYear,
                              answers: ShortServiceRefundsViewModel
                             )(implicit hc: HeaderCarrier, ec: ExecutionContext): ApiResultT[Unit] = {
    implicit val reads: HttpReads[DownstreamErrorOr[Unit]] = NoContentHttpReads
    val url: URI = new URI(appConfig.journeyAnswersUrl(taxYear, nino, ShortServiceRefunds))

    ConnectorRequestInfo("PUT", url.toString, apiId).logRequestWithBody(logger, answers)

    EitherT{
      httpClientV2
        .put(url.toURL)
        .withBody(Json.toJson(answers))
        .execute[DownstreamErrorOr[Unit]](reads, ec)
    }
  }

  def getAllJourneyStatuses(taxYear: TaxYear
                           )(implicit hc: HeaderCarrier, ec: ExecutionContext): DownstreamOutcome[List[JourneyNameAndStatus]] = {
    val url: URI = new URI(buildUrl(s"/journey-statuses/taxYear/${taxYear.endYear}"))

    ConnectorRequestInfo("GET", url.toString, apiId).logRequest(logger)

    EitherT{
      httpClientV2
        .get(url.toURL)
        .execute[DownstreamErrorOr[List[JourneyNameAndStatus]]]
    }.value
  }

  def getJourneyStatus(ctx: JourneyContext
                      )(implicit hc: HeaderCarrier, ec: ExecutionContext): DownstreamOutcome[List[JourneyNameAndStatus]] = {
    implicit val reads: HttpReads[DownstreamErrorOr[List[JourneyNameAndStatus]]] = GetJourneyStatusesHttpReads
    val url: URI = new URI(buildUrl(s"/journey-status/${ctx.journey}/taxYear/${ctx.taxYear}"))

    ConnectorRequestInfo("GET", url.toString, apiId).logRequest(logger)

    EitherT{
      httpClientV2
        .get(url.toURL)
        .transform(_.addHttpHeaders("mtditid" -> ctx.mtditid.value))
        .execute[DownstreamErrorOr[List[JourneyNameAndStatus]]](reads, ec)
    }.value
  }

  def saveJourneyStatus(ctx: JourneyContext,
                        status: JourneyStatus
                       )(implicit hc: HeaderCarrier, ec: ExecutionContext): DownstreamOutcome[Unit] = {
    implicit val reads: HttpReads[DownstreamErrorOr[Unit]] = NoContentHttpReads

    val url: URI = new URI(buildUrl(s"/journey-status/${ctx.journey}/taxYear/${ctx.taxYear}"))
    ConnectorRequestInfo("POST", url.toString, apiId).logRequest(logger)

    EitherT{
      httpClientV2
        .put(url.toURL)
        .withBody(Json.toJson(status))
        .execute[DownstreamErrorOr[Unit]](reads, ec)
    }.value
  }

}
