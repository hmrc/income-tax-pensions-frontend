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

package controllers.predicates.actions

import common.TaxYear
import config.ErrorHandler
import models.pension.Journey
import models.requests.UserSessionDataRequest
import play.api.mvc.{ActionRefiner, Result}
import services.PensionSessionService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider
import utils.Logging

import scala.concurrent.{ExecutionContext, Future}

case class LoadPriorDataToSessionAction(
    taxYear: TaxYear,
    journey: Journey,
    service: PensionSessionService,
    errorHandler: ErrorHandler
)(implicit ec: ExecutionContext)
    extends ActionRefiner[UserSessionDataRequest, UserSessionDataRequest]
    with FrontendHeaderCarrierProvider
    with Logging {

  override protected[predicates] def executionContext: ExecutionContext = ec

  override protected[predicates] def refine[A](input: UserSessionDataRequest[A]): Future[Either[Result, UserSessionDataRequest[A]]] = {
    implicit val headerCarrier: HeaderCarrier = input.user.withDownstreamHc(hc(input.request))

    if (input.sessionData.pensions.hasSessionData(journey)) {
      logger.debug(s"Session data for $journey exists")
      Future.successful(Right(input))
    } else {
      logger.debug(s"Session data for $journey DOES NOT exist. Loading prior data for this journey")

      val result = for {
        priorData          <- service.loadOneJourneyPriorData(taxYear, input.user, journey)
        updatedSessionData <- service.updateSessionData(input.user, taxYear, input.sessionData, priorData)
      } yield input.copy(sessionData = updatedSessionData)

      result
        .leftMap(error => errorHandler.handleError(error.status)(input.request))
        .value
    }
  }
}
