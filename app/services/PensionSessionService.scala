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

package services

import config.{AppConfig, ErrorHandler}
import connectors.{IncomeSourceConnector, IncomeTaxUserDataConnector}
import connectors.httpParsers.IncomeTaxUserDataHttpParser.IncomeTaxUserDataResponse
import models.User
import models.mongo.{DatabaseError, PensionsCYAModel, PensionsUserData}
import models.pension.AllPensionsData
import org.joda.time.DateTimeZone
import play.api.Logging
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.mvc.Results.Redirect
import play.api.mvc.{Request, Result}
import repositories.PensionsUserDataRepository
import uk.gov.hmrc.http.HeaderCarrier
import utils.Clock

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PensionSessionService @Inject()(pensionUserDataRepository: PensionsUserDataRepository,
                                      incomeTaxUserDataConnector: IncomeTaxUserDataConnector,
                                      incomeSourceConnector: IncomeSourceConnector,
                                      implicit private val appConfig: AppConfig,
                                      errorHandler: ErrorHandler,
                                      implicit val ec: ExecutionContext) extends Logging {


  def getPriorData(taxYear: Int, user: User)(implicit hc: HeaderCarrier): Future[IncomeTaxUserDataResponse] = {
    incomeTaxUserDataConnector.getUserData(user.nino, taxYear)(hc.withExtraHeaders("mtditid" -> user.mtditid))
  }

  private def getSessionData(taxYear: Int, user: User)(implicit request: Request[_]): Future[Either[Result, Option[PensionsUserData]]] = {
    pensionUserDataRepository.find(taxYear, user).map {
      case Left(_) => Left(errorHandler.handleError(INTERNAL_SERVER_ERROR))
      case Right(value) => Right(value)
    }
  }

  def clear[R](taxYear: Int)(onFail: R)(onSuccess: R)(implicit user: User, ec: ExecutionContext, hc: HeaderCarrier): Future[R] = {
    incomeSourceConnector.put(taxYear, user.nino, "pensions")(hc.withExtraHeaders("mtditid" -> user.mtditid)).flatMap {
      case Left(_) => Future.successful(onFail)
      case _ =>
        pensionUserDataRepository.clear(taxYear, user).map {
          case true => onSuccess
          case false => onFail
        }
    }
  }

  def getPensionSessionData(taxYear: Int, user: User): Future[Either[Unit, Option[PensionsUserData]]] = {
    pensionUserDataRepository.find(taxYear, user).map {
      case Left(_) => Left(())
      case Right(data) => Right(data)
    }
  }

  @deprecated("We should avoid using this method, as it's more difficult to mock. use 'getPensionSessionData' above")
  def getPensionsSessionDataResult(taxYear: Int, user: User)(result: Option[PensionsUserData] => Future[Result])
                                  (implicit request: Request[_]): Future[Result] = {
    pensionUserDataRepository.find(taxYear, user).flatMap {
      case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
      case Right(value) => result(value)
    }
  }

  def getAndHandle(taxYear: Int, user: User, redirectWhenNoPrior: Boolean = false)
                  (block: (Option[PensionsUserData], Option[AllPensionsData]) => Future[Result])
                  (implicit request: Request[_], hc: HeaderCarrier): Future[Result] = {
    val result = for {
      optionalCya <- getSessionData(taxYear, user)
      priorDataResponse <- getPriorData(taxYear, user)
    } yield {
      if (optionalCya.isRight) {
        if (optionalCya.toOption.get.isEmpty) {
          logger.info(s"[PensionSessionService][getAndHandle] No pension CYA data found for user. SessionId: ${user.sessionId}")
        }
      }
      val pensionDataResponse = priorDataResponse.map(_.pensions)
      (optionalCya, pensionDataResponse) match {
        case (Right(None), Right(None)) if redirectWhenNoPrior => logger.info(s"[PensionSessionService][getAndHandle] No pension data found for user." +
          s"Redirecting to overview page. SessionId: ${user.sessionId}")
          Future(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
        case (Right(optionalCya), Right(pensionData)) => block(optionalCya, pensionData)
        case (_, Left(error)) => Future(errorHandler.handleError(error.status))
        case (Left(_), _) => Future(errorHandler.handleError(INTERNAL_SERVER_ERROR))
      }
    }
    result.flatten
  }

  //scalastyle:off
  def createOrUpdateSessionData[A](user: User, cyaModel: PensionsCYAModel, taxYear: Int, isPriorSubmission: Boolean)
                                  (onFail: A)(onSuccess: A)(implicit clock: Clock): Future[A] = {

    val userData = PensionsUserData(
      user.sessionId,
      user.mtditid,
      user.nino,
      taxYear,
      isPriorSubmission,
      cyaModel,
      clock.now(DateTimeZone.UTC)
    )

    pensionUserDataRepository.createOrUpdate(userData).map {
      case Right(_) => onSuccess
      case Left(_) => onFail
    }
  }


  def createOrUpdateSessionData(pensionsUserData: PensionsUserData): Future[Either[DatabaseError, Unit]] = {
    pensionUserDataRepository.createOrUpdate(pensionsUserData).map {
      case Left(error: DatabaseError) => Left(error)
      case Right(_) => Right(())
    }
  }
}

