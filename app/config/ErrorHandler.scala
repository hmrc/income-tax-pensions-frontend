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

package config

import models.logging.CorrelationIdMdc
import models.logging.CorrelationIdMdc.maybeCorrelationIdFromMdc
import models.logging.HeaderCarrierExtensions.CorrelationIdHeaderKey
import play.api.http.Status._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results._
import play.api.mvc.{Request, RequestHeader, Result}
import play.api.{Logger, PlayException}
import play.twirl.api.Html
import uk.gov.hmrc.play.bootstrap.frontend.http.FrontendErrorHandler
import views.html.templates.{InternalServerErrorTemplate, NotFoundTemplate, ServiceUnavailableTemplate}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ErrorHandler @Inject() (internalServerErrorTemplate: InternalServerErrorTemplate,
                              serviceUnavailableTemplate: ServiceUnavailableTemplate,
                              val messagesApi: MessagesApi,
                              notFoundTemplate: NotFoundTemplate)(implicit appConfig: AppConfig)
    extends FrontendErrorHandler
    with I18nSupport {

  private val logger = Logger(getClass)

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit
                                                                                          request: RequestHeader
  ): Future[Html] =
    Future.successful(internalServerErrorTemplate())

  override def notFoundTemplate(implicit request: RequestHeader): Future[Html] =
    Future.successful(notFoundTemplate())

  def internalServerError()(implicit request: Request[_]): Result = {
    val errorLogMessage = s"[$CorrelationIdHeaderKey=${maybeCorrelationIdFromMdc()}] error occurred: for ${request.method} [${request.uri}]"
    logger.error(errorLogMessage)

    InternalServerError(internalServerErrorTemplate())
  }

  def futureInternalServerError()(implicit request: Request[_]): Future[Result] =
    Future.successful(internalServerError())

  def handleError(status: Int)(implicit request: Request[_]): Result = {
    val errorLogMessage = s"[$CorrelationIdHeaderKey=${maybeCorrelationIdFromMdc()}] for ${request.method} [${request.uri}] - status=$status"
    logger.error(errorLogMessage)

    status match {
      case SERVICE_UNAVAILABLE => ServiceUnavailable(serviceUnavailableTemplate())
      case _                   => InternalServerError(internalServerErrorTemplate())
    }
  }

  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    val clientErrorLogMessage =
      s"[$CorrelationIdHeaderKey=${maybeCorrelationIdFromMdc()}] Client error occurred: $statusCode for ${request.method} [${request.uri}] - $message"

    statusCode match {
      case NOT_FOUND =>
        logger.debug(clientErrorLogMessage) // May be lots of Not found errors (broken links etc. Don't want to clutter logging)
        Future.successful(NotFound(notFoundTemplate()(request.withBody(""), request2Messages(request), appConfig)))
      case _ =>
        logger.error(clientErrorLogMessage)
        Future.successful(InternalServerError(internalServerErrorTemplate()(request.withBody(""), request2Messages(request), appConfig)))
    }
  }

  override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    logErrorWithmaybeCorrelationIdFromMdc(request, exception)
    resolveError(request, exception)
  }

  private def logErrorWithmaybeCorrelationIdFromMdc(request: RequestHeader, ex: Throwable): Unit =
    logger.error(
      """
        |
        |! %sInternal server error, [%s=%s], for (%s) [%s] ->
        | """.stripMargin.format(
        ex match {
          case p: PlayException => "@" + p.id + " - "
          case _                => ""
        },
        CorrelationIdHeaderKey,
        CorrelationIdMdc.maybeCorrelationIdFromMdc(),
        request.method,
        request.uri),
      ex
    )

  override protected implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
}
