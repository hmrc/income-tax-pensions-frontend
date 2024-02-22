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

package services.redirects

import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.pension.PensionCYABaseModel
import play.api.Logging
import play.api.mvc.Results.Redirect
import play.api.mvc.{Call, Result}

import scala.concurrent.Future

object SimpleRedirectService extends Logging {

  def redirectBasedOnCurrentAnswers(taxYear: Int, data: Option[PensionsUserData], cyaPage: Call)(shouldRedirect: PensionsCYAModel => Option[Result])(
      continue: PensionsUserData => Future[Result]): Future[Result] = {

    val redirectOrData = data match {
      case Some(cya) =>
        shouldRedirect(cya.pensions) match {
          case None => Right(cya)
          case Some(redirect) =>
            logger.info(
              s"[RedirectService][calculateRedirect]" +
                s" Some data is missing / in the wrong state for the requested page. Routing to ${redirect.header.headers.getOrElse("Location", "")}")
            Left(redirect)
        }
      case None =>
        Left(Redirect(cyaPage))
    }

    redirectOrData match {
      case Right(cya)     => continue(cya)
      case Left(redirect) => Future.successful(redirect)
    }
  }

  def isFinishedCheck(cya: PensionCYABaseModel, taxYear: Int, continueRedirect: Call, cyaRedirectFn: Int => Call): Result =
    if (cya.isFinished) {
      println()
      println("hhh")
      println()
      Redirect(cyaRedirectFn(taxYear))
    } else {
      println()
      println("vvv")
      println()
      Redirect(continueRedirect)
    }

  def checkForExistingSchemes[T](nextPage: Call, summaryPage: Call, schemes: Seq[T]): Call =
    if (schemes.isEmpty) {
      println()
      println("isempty")
      println()
      nextPage
    } else {
      println()
      println("not empty")
      println()
      summaryPage
    }

}
