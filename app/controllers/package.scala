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

import common.TaxYear
import config.ErrorHandler
import models.domain.ApiResultT
import models.pension.Journey
import models.redirects.AppLocations.SECTION_COMPLETED_PAGE
import play.api.Logger
import play.api.mvc.Results.Redirect
import cats.implicits._
import play.api.mvc.Request

import scala.concurrent.ExecutionContext

package object controllers {

  def validatedIndex(index: Option[Int], collectionSize: Int): Option[Int] =
    index.filter(i => i >= 0 && i < collectionSize)

  def validatedSchemes[T](index: Option[Int], listItem: Seq[T]): Either[Unit, Option[T]] =
    index match {
      case Some(value) if listItem.indices contains value => Right(Some(listItem(value)))
      case None                                           => Right(None)
      case _                                              => Left(())
    }

  def handleResult(errorHandler: ErrorHandler, taxYear: TaxYear, journey: Journey, res: ApiResultT[Unit])(implicit
      logger: Logger,
      request: Request[_],
      ec: ExecutionContext) = {
    val result = res.map(_ => Redirect(SECTION_COMPLETED_PAGE(taxYear.endYear, journey))).leftMap { err =>
      logger.info(s"[PaymentIntoPensionsCYAController][submit] Failed to create or update session: ${err}")
      errorHandler.handleError(err.status)
    }

    result.merge
  }
}
