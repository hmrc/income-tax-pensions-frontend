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

package services.redirects

import models.pension.charges.ShortServiceRefundsViewModel

import scala.util.{Failure, Success, Try}

sealed trait ShortServiceRefundsPages {
  def isValidInCurrentState(state: ShortServiceRefundsViewModel, maybeIndex: Option[Int] = None): Boolean
}

object ShortServiceRefundsPages {

  final case object NonUkTaxRefundsAmountPage extends ShortServiceRefundsPages {
    override def isValidInCurrentState(state: ShortServiceRefundsViewModel, maybeIndex: Option[Int] = None): Boolean =
      state.shortServiceRefund.contains(true) && state.shortServiceRefundCharge.isDefined
  }

  final case class SchemeDetailsPage() extends ShortServiceRefundsPages {
    override def isValidInCurrentState(state: ShortServiceRefundsViewModel, maybeIndex: Option[Int] = None): Boolean =
      state.shortServiceRefund.contains(true) && state.shortServiceRefundCharge.isDefined
  }

  final case object RefundSchemesSummaryPage extends ShortServiceRefundsPages {
    override def isValidInCurrentState(state: ShortServiceRefundsViewModel, maybeIndex: Option[Int] = None): Boolean = {
      val noPartiallyCompletedSchemes = state.refundPensionScheme.forall(_.isFinished) || state.refundPensionScheme.isEmpty
      state.shortServiceRefund.contains(true) && noPartiallyCompletedSchemes
    }
  }

  final case class RemoveRefundSchemePage() extends ShortServiceRefundsPages {
    override def isValidInCurrentState(state: ShortServiceRefundsViewModel, maybeIndex: Option[Int] = None): Boolean =
      maybeIndex.exists { index =>
        Try(state.refundPensionScheme(index)) match {
          case Success(scheme) => scheme.isFinished
          case Failure(_)      => false
        }
      }
  }

  final case object CYAPage extends ShortServiceRefundsPages {
    override def isValidInCurrentState(state: ShortServiceRefundsViewModel, maybeIndex: Option[Int] = None): Boolean =
      state.isFinished
  }

}
