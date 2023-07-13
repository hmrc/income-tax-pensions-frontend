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

import config.AppConfig
import models.AuthorisationRequest
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionFilter, Result}
import utils.TaxYearHelper

import scala.concurrent.{ExecutionContext, Future}

case class EndOfYearFilterAction(override val taxYear: Int,
                                 appConfig: AppConfig)
                                (implicit ec: ExecutionContext) extends ActionFilter[AuthorisationRequest] with TaxYearHelper {

  override protected[predicates] def executionContext: ExecutionContext = ec

  override protected[predicates] def filter[A](request: AuthorisationRequest[A]): Future[Option[Result]] = Future.successful {
    if (inYear(taxYear)) {
      Some(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
    } else {
      None
    }
  }
}

