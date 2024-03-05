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

package validation.pensions.shortServiceRefunds

import cats.implicits.catsSyntaxEitherId
import models.pension.charges.OverseasRefundPensionScheme
import play.api.mvc.Call
import services.redirects.ShortServiceRefundsPages
import services.redirects.ShortServiceRefundsPages.{RemoveRefundSchemePage, SchemeDetailsPage}
import services.redirects.ShortServiceRefundsRedirects.refundSummaryCall
import validation.pensions.shortServiceRefunds.IndexValidator.RedirectOrValidIndex

trait IndexValidator[A <: ShortServiceRefundsPages] {
  def validate(index: Option[Int], schemes: Seq[OverseasRefundPensionScheme], taxYear: Int): RedirectOrValidIndex
}

object IndexValidator {
  final case class RedirectLocation(location: Call)
  type RedirectOrValidIndex = Either[RedirectLocation, Int]

  implicit val addSchemeIndexValidator: IndexValidator[SchemeDetailsPage] =
    (index: Option[Int], schemes: Seq[OverseasRefundPensionScheme], taxYear: Int) =>
      index match {
        case Some(idx) =>
          val isIndexInBounds =
            if (idx >= 0 && idx <= schemes.size) true
            else false

          if (isIndexInBounds) idx.asRight
          else RedirectLocation(refundSummaryCall(taxYear)).asLeft

        // So to create a first submission. This does mean manual removal of the index from the url defaults scheme to the first one
        case None => 0.asRight
      }

  implicit val removeSchemeIndexValidator: IndexValidator[RemoveRefundSchemePage] =
    (index: Option[Int], schemes: Seq[OverseasRefundPensionScheme], taxYear: Int) =>
      index match {
        case Some(idx) =>
          val isIndexInBounds =
            if (idx >= 0 && idx < schemes.size) true
            else false

          if (isIndexInBounds) idx.asRight
          else RedirectLocation(refundSummaryCall(taxYear)).asLeft

        case None => RedirectLocation(refundSummaryCall(taxYear)).asLeft
      }
}
