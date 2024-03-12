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

import models.pension.statebenefits.IncomeFromPensionsViewModel

sealed trait StatePensionPages {
  def isValidInCurrentState(state: IncomeFromPensionsViewModel): Boolean
}

object StatePensionPages {

  case object StatePaymentsStartDatePage extends StatePensionPages {
    override def isValidInCurrentState(state: IncomeFromPensionsViewModel): Boolean =
      state.statePension.flatMap(_.amountPaidQuestion).contains(true) &&
        state.statePension.flatMap(_.amount).isDefined
  }

  case object StatePensionLumpSumPage extends StatePensionPages {
    override def isValidInCurrentState(state: IncomeFromPensionsViewModel): Boolean = {
      val areClaimingStatePension = state.statePension.flatMap(_.amountPaidQuestion).contains(true)

      if (areClaimingStatePension)
        state.statePension.flatMap(_.amountPaidQuestion).contains(true) &&
        state.statePension.flatMap(_.amount).isDefined &&
        state.statePension.flatMap(_.startDateQuestion).isDefined &&
        state.statePension.flatMap(_.startDate).isDefined
      else
        state.statePension.flatMap(_.amountPaidQuestion).isDefined // So to ensure the first question in the journey hasn't been skipped
    }
  }

  case object TaxOnStatePensionLumpSumPage extends StatePensionPages {
    override def isValidInCurrentState(state: IncomeFromPensionsViewModel): Boolean =
      state.statePensionLumpSum.flatMap(_.amountPaidQuestion).contains(true) &&
        state.statePensionLumpSum.flatMap(_.amount).isDefined
  }

  case object StatePensionLumpSumStartDatePage extends StatePensionPages {
    override def isValidInCurrentState(state: IncomeFromPensionsViewModel): Boolean =
      state.statePensionLumpSum.flatMap(_.amountPaidQuestion).contains(true) &&
        state.statePensionLumpSum.flatMap(_.amount).isDefined
  }

  case object StatePensionsCYAPage extends StatePensionPages {
    override def isValidInCurrentState(state: IncomeFromPensionsViewModel): Boolean = {
      println("***" + state.isStatePensionFinished)
      state.isStatePensionFinished
    }
  }
}
