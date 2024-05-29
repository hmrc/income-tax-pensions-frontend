/*
 * Copyright 2024 HM Revenue & Customs
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
import connectors.Connector.hcWithCorrelationId
import connectors.httpParsers.DeletePensionChargesHttpParser.DeletePensionChargesHttpReads
import connectors.httpParsers.DeletePensionIncomeHttpParser.DeletePensionIncomeHttpReads
import connectors.httpParsers.DeletePensionReliefsHttpParser.DeletePensionReliefsHttpReads
import connectors.httpParsers.GetJourneyStatusesHttpParser.GetJourneyStatusesHttpReads
import connectors.httpParsers.LoadPriorEmploymentHttpParser.LoadPriorEmploymentHttpReads
import connectors.httpParsers.PensionChargesSessionHttpParser.PensionChargesSessionHttpReads
import connectors.httpParsers.PensionIncomeSessionHttpParser.PensionIncomeSessionHttpReads
import connectors.httpParsers.PensionReliefsSessionHttpParser.PensionReliefsSessionHttpReads
import models.domain.ApiResultT
import models.logging.ConnectorRequestInfo
import models.mongo.{JourneyContext, JourneyStatus}
import models.pension.Journey._
import models.pension.JourneyNameAndStatus
import models.pension.charges._
import models.pension.employmentPensions.EmploymentPensions
import models.pension.income.CreateUpdatePensionIncomeRequestModel
import models.pension.reliefs.{CreateUpdatePensionReliefsModel, PaymentsIntoPensionsViewModel}
import models.pension.statebenefits.IncomeFromPensionsViewModel
import play.api.Logging
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class PensionsConnector @Inject() (val http: HttpClient, val appConfig: AppConfig) extends Logging {
  private val apiId                 = "income-tax-pensions"
  private def buildUrl(url: String) = s"${appConfig.pensionBEBaseUrl}$url"

  def loadPriorEmployment(nino: String, taxYear: TaxYear)(implicit hc: HeaderCarrier, ec: ExecutionContext): DownstreamOutcome[EmploymentPensions] = {
    val url = buildUrl(s"/employment-pension/nino/$nino/taxYear/${taxYear.endYear}")
    http.GET[DownstreamErrorOr[EmploymentPensions]](url)(LoadPriorEmploymentHttpReads, hcWithCorrelationId(hc), ec)
  }

  def savePaymentsIntoPensions(nino: Nino, taxYear: TaxYear, answers: PaymentsIntoPensionsViewModel)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): ApiResultT[Unit] = {
    val url = appConfig.journeyAnswersUrl(taxYear, nino, PaymentsIntoPensions)
    ConnectorRequestInfo("PUT", url, apiId).logRequestWithBody(logger, answers)

    val res =
      http.PUT[PaymentsIntoPensionsViewModel, DownstreamErrorOr[Unit]](url, answers)(PaymentsIntoPensionsViewModel.format, NoContentHttpReads, hc, ec)

    EitherT(res)
  }

  def getPaymentsIntoPensions(nino: Nino, taxYear: TaxYear)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): ApiResultT[Option[PaymentsIntoPensionsViewModel]] = {
    val url = appConfig.journeyAnswersUrl(taxYear, nino, PaymentsIntoPensions)
    ConnectorRequestInfo("GET", url, apiId).logRequest(logger)
    val res = http.GET[DownstreamErrorOr[Option[PaymentsIntoPensionsViewModel]]](url)
    EitherT(res)
  }

  def getUkPensionIncome(nino: Nino, taxYear: TaxYear)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): ApiResultT[Option[IncomeFromPensionsViewModel]] = {
    val url = appConfig.journeyAnswersUrl(taxYear, nino, UkPensionIncome)
    ConnectorRequestInfo("GET", url, apiId).logRequest(logger)
    val res = http.GET[DownstreamErrorOr[Option[IncomeFromPensionsViewModel]]](url)
    EitherT(res)
  }

  def saveUkPensionIncome(nino: Nino, taxYear: TaxYear, answers: IncomeFromPensionsViewModel)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): ApiResultT[Unit] = {
    val url = appConfig.journeyAnswersUrl(taxYear, nino, UkPensionIncome)
    ConnectorRequestInfo("PUT", url, "income-tax-pensions").logRequestWithBody(logger, answers)

    val res =
      http.PUT[IncomeFromPensionsViewModel, DownstreamErrorOr[Unit]](url, answers)(IncomeFromPensionsViewModel.format, NoContentHttpReads, hc, ec)

    EitherT(res)
  }

  def saveAnnualAllowances(nino: Nino, taxYear: TaxYear, answers: PensionAnnualAllowancesViewModel)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): ApiResultT[Unit] = {
    val url = appConfig.journeyAnswersUrl(taxYear, nino, AnnualAllowances)
    ConnectorRequestInfo("PUT", url, apiId).logRequestWithBody(logger, answers)

    val res =
      http.PUT[PensionAnnualAllowancesViewModel, DownstreamErrorOr[Unit]](url, answers)(
        PensionAnnualAllowancesViewModel.format,
        NoContentHttpReads,
        hc,
        ec)
    EitherT(res)
  }

  def getAnnualAllowances(nino: Nino, taxYear: TaxYear)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): ApiResultT[Option[PensionAnnualAllowancesViewModel]] = {
    val url = appConfig.journeyAnswersUrl(taxYear, nino, AnnualAllowances)
    ConnectorRequestInfo("GET", url, apiId).logRequest(logger)
    val res = http.GET[DownstreamErrorOr[Option[PensionAnnualAllowancesViewModel]]](url)
    EitherT(res)
  }

  def saveUnauthorisedPaymentsFromPensions(nino: Nino, taxYear: TaxYear, answers: UnauthorisedPaymentsViewModel)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): ApiResultT[Unit] = {
    val url = appConfig.journeyAnswersUrl(taxYear, nino, UnauthorisedPayments)
    ConnectorRequestInfo("PUT", url, apiId).logRequestWithBody(logger, answers)

    val res =
      http.PUT[UnauthorisedPaymentsViewModel, DownstreamErrorOr[Unit]](url, answers)(UnauthorisedPaymentsViewModel.format, NoContentHttpReads, hc, ec)
    EitherT(res)
  }

  def getUnauthorisedPaymentsFromPensions(nino: Nino, taxYear: TaxYear)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): ApiResultT[Option[UnauthorisedPaymentsViewModel]] = {
    val url = appConfig.journeyAnswersUrl(taxYear, nino, UnauthorisedPayments)
    ConnectorRequestInfo("GET", url, apiId).logRequest(logger)
    val res = http.GET[DownstreamErrorOr[Option[UnauthorisedPaymentsViewModel]]](url)
    EitherT(res)
  }

  def getPaymentsIntoOverseasPensions(nino: Nino, taxYear: TaxYear)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): ApiResultT[Option[PaymentsIntoOverseasPensionsViewModel]] = {
    val url = appConfig.journeyAnswersUrl(taxYear, nino, PaymentsIntoOverseasPensions)
    ConnectorRequestInfo("GET", url, apiId).logRequest(logger)
    val res = http.GET[DownstreamErrorOr[Option[PaymentsIntoOverseasPensionsViewModel]]](url)
    EitherT(res)
  }

  def savePaymentsIntoOverseasPensions(nino: Nino, taxYear: TaxYear, answers: PaymentsIntoOverseasPensionsViewModel)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): ApiResultT[Unit] = {
    val url = appConfig.journeyAnswersUrl(taxYear, nino, PaymentsIntoOverseasPensions)
    ConnectorRequestInfo("PUT", url, apiId).logRequestWithBody(logger, answers)

    val res =
      http.PUT[PaymentsIntoOverseasPensionsViewModel, DownstreamErrorOr[Unit]](url, answers)(
        PaymentsIntoOverseasPensionsViewModel.format,
        NoContentHttpReads,
        hc,
        ec)
    EitherT(res)
  }

  def saveTransfersIntoOverseasPensions(nino: Nino, taxYear: TaxYear, answers: TransfersIntoOverseasPensionsViewModel)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): ApiResultT[Unit] = {
    val url = appConfig.journeyAnswersUrl(taxYear, nino, TransferIntoOverseasPensions)
    ConnectorRequestInfo("PUT", url, apiId).logRequestWithBody(logger, answers)
    val res =
      http.PUT[TransfersIntoOverseasPensionsViewModel, DownstreamErrorOr[Unit]](url, answers)(
        TransfersIntoOverseasPensionsViewModel.format,
        NoContentHttpReads,
        hc,
        ec)
    EitherT(res)
  }

  def getTransfersIntoOverseasPensions(nino: Nino, taxYear: TaxYear)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): ApiResultT[Option[TransfersIntoOverseasPensionsViewModel]] = {
    val url = appConfig.journeyAnswersUrl(taxYear, nino, TransferIntoOverseasPensions)
    ConnectorRequestInfo("GET", url, apiId).logRequest(logger)
    val res = http.GET[DownstreamErrorOr[Option[TransfersIntoOverseasPensionsViewModel]]](url)
    EitherT(res)
  }

  def getIncomeFromOverseasPensions(nino: Nino, taxYear: TaxYear)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): ApiResultT[Option[IncomeFromOverseasPensionsViewModel]] = {
    val url = appConfig.journeyAnswersUrl(taxYear, nino, IncomeFromOverseasPensions)
    ConnectorRequestInfo("GET", url, apiId).logRequest(logger)
    val res = http.GET[DownstreamErrorOr[Option[IncomeFromOverseasPensionsViewModel]]](url)
    EitherT(res)
  }

  def saveIncomeFromOverseasPensions(nino: Nino, taxYear: TaxYear, answers: IncomeFromOverseasPensionsViewModel)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): ApiResultT[Unit] = {
    val url = appConfig.journeyAnswersUrl(taxYear, nino, IncomeFromOverseasPensions)
    ConnectorRequestInfo("PUT", url, "income-tax-pensions").logRequestWithBody(logger, answers)

    val res = http.PUT[IncomeFromOverseasPensionsViewModel, DownstreamErrorOr[Unit]](url, answers)(
      IncomeFromOverseasPensionsViewModel.format,
      NoContentHttpReads,
      hc,
      ec)
    EitherT(res)
  }

  def getShortServiceRefunds(nino: Nino, taxYear: TaxYear)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): ApiResultT[Option[ShortServiceRefundsViewModel]] = {
    val url = appConfig.journeyAnswersUrl(taxYear, nino, ShortServiceRefunds)
    ConnectorRequestInfo("GET", url, apiId).logRequest(logger)
    val res = http.GET[DownstreamErrorOr[Option[ShortServiceRefundsViewModel]]](url)
    EitherT(res)
  }

  def saveShortServiceRefunds(nino: Nino, taxYear: TaxYear, answers: ShortServiceRefundsViewModel)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): ApiResultT[Unit] = {
    val url = appConfig.journeyAnswersUrl(taxYear, nino, ShortServiceRefunds)
    ConnectorRequestInfo("PUT", url, apiId).logRequestWithBody(logger, answers)

    val res =
      http.PUT[ShortServiceRefundsViewModel, DownstreamErrorOr[Unit]](url, answers)(ShortServiceRefundsViewModel.format, NoContentHttpReads, hc, ec)
    EitherT(res)
  }

  def savePensionCharges(nino: String, taxYear: Int, model: CreateUpdatePensionChargesRequestModel)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): DownstreamOutcome[Unit] = {
    val url = buildUrl(s"/pension-charges/session-data/nino/$nino/taxYear/${taxYear.toString}")
    ConnectorRequestInfo("PUT", url, apiId).logRequestWithBody(logger, model)
    http.PUT[CreateUpdatePensionChargesRequestModel, DownstreamErrorOr[Unit]](url, model)(
      CreateUpdatePensionChargesRequestModel.format,
      PensionChargesSessionHttpReads,
      hcWithCorrelationId(hc),
      ec)
  }

  def deletePensionCharges(nino: String, taxYear: Int)(implicit hc: HeaderCarrier, ec: ExecutionContext): DownstreamOutcome[Unit] = {
    val url = buildUrl(s"/pension-charges/session-data/nino/$nino/taxYear/${taxYear.toString}")
    ConnectorRequestInfo("DELETE", url, apiId).logRequest(logger)
    http.DELETE[DownstreamErrorOr[Unit]](url)(DeletePensionChargesHttpReads, hcWithCorrelationId(hc), ec)
  }

  def savePensionIncome(nino: String, taxYear: Int, model: CreateUpdatePensionIncomeRequestModel)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): DownstreamOutcome[Unit] = {
    val url = buildUrl(s"/pension-income/session-data/nino/$nino/taxYear/${taxYear.toString}")
    ConnectorRequestInfo("PUT", url, apiId).logRequestWithBody(logger, model)
    http.PUT[CreateUpdatePensionIncomeRequestModel, DownstreamErrorOr[Unit]](url, model)(
      CreateUpdatePensionIncomeRequestModel.writes,
      PensionIncomeSessionHttpReads,
      hcWithCorrelationId(hc),
      ec)
  }

  def deletePensionIncome(nino: String, taxYear: Int)(implicit hc: HeaderCarrier, ec: ExecutionContext): DownstreamOutcome[Unit] = {
    val url = buildUrl(s"/pension-income/session-data/nino/$nino/taxYear/${taxYear.toString}")
    ConnectorRequestInfo("DELETE", url, apiId).logRequest(logger)
    http.DELETE[DownstreamErrorOr[Unit]](url)(DeletePensionIncomeHttpReads, hc, ec)
  }

  def savePensionReliefs(nino: String, taxYear: Int, model: CreateUpdatePensionReliefsModel)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext): DownstreamOutcome[Unit] = {
    val url = buildUrl(s"/pension-reliefs/nino/$nino/taxYear/${taxYear.toString}")
    ConnectorRequestInfo("PUT", url, apiId).logRequestWithBody(logger, model)
    http.PUT[CreateUpdatePensionReliefsModel, DownstreamErrorOr[Unit]](url, model)(
      CreateUpdatePensionReliefsModel.format,
      PensionReliefsSessionHttpReads,
      hcWithCorrelationId(hc),
      ec
    )
  }

  def deletePensionReliefData(nino: String, taxYear: Int)(implicit hc: HeaderCarrier, ec: ExecutionContext): DownstreamOutcome[Unit] = {
    val url = buildUrl(s"/pension-reliefs/session-data/nino/$nino/taxYear/${taxYear.toString}")
    ConnectorRequestInfo("DELETE", url, apiId).logRequest(logger)
    http.DELETE[DownstreamErrorOr[Unit]](url)(DeletePensionReliefsHttpReads, hcWithCorrelationId(hc), ec)
  }

  def getAllJourneyStatuses(taxYear: TaxYear)(implicit hc: HeaderCarrier, ec: ExecutionContext): DownstreamOutcome[List[JourneyNameAndStatus]] = {
    val url = buildUrl(s"/journey-statuses/taxYear/${taxYear.endYear}")
    ConnectorRequestInfo("GET", url, apiId).logRequest(logger)
    http.GET[DownstreamErrorOr[List[JourneyNameAndStatus]]](url)
  }

  def getJourneyStatus(ctx: JourneyContext)(implicit hc: HeaderCarrier, ec: ExecutionContext): DownstreamOutcome[List[JourneyNameAndStatus]] = {
    val url = buildUrl(s"/journey-status/${ctx.journey}/taxYear/${ctx.taxYear}")
    ConnectorRequestInfo("GET", url, apiId).logRequest(logger)
    http.GET[DownstreamErrorOr[List[JourneyNameAndStatus]]](url)(
      GetJourneyStatusesHttpReads,
      hc.withExtraHeaders(("mtditid", ctx.mtditid.value)),
      ec
    )
  }

  def saveJourneyStatus(ctx: JourneyContext, status: JourneyStatus)(implicit hc: HeaderCarrier, ec: ExecutionContext): DownstreamOutcome[Unit] = {
    val url = buildUrl(s"/journey-status/${ctx.journey}/taxYear/${ctx.taxYear}")
    ConnectorRequestInfo("POST", url, apiId).logRequest(logger)
    http.PUT[JourneyStatus, DownstreamErrorOr[Unit]](url, status)(
      JourneyStatus.format,
      NoContentHttpReads,
      hcWithCorrelationId(hc).withExtraHeaders(("mtditid", ctx.mtditid.value)),
      ec)
  }
}
