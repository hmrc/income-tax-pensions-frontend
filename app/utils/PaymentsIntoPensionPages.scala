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

package utils

sealed trait PaymentsIntoPensionPages {
  val journeyNo: Int
}

object PaymentsIntoPensionPages {
  case object RasPage extends PaymentsIntoPensionPages {
    override val journeyNo: Int = 1
  }
  case object RasAmountPage extends PaymentsIntoPensionPages {
    override val journeyNo: Int = 2
  }
  case object OneOffRasPage extends PaymentsIntoPensionPages {
    override val journeyNo: Int = 3
  }
  case object OneOffRasAmountPage extends PaymentsIntoPensionPages {
    override val journeyNo: Int = 4
  }
  case object TotalRasPage extends PaymentsIntoPensionPages {
    override val journeyNo: Int = 5
  }
  case object TaxReliefNotClaimedPage extends PaymentsIntoPensionPages {
    override val journeyNo: Int = 6
  }
  case object RetirementAnnuityPage extends PaymentsIntoPensionPages {
    override val journeyNo: Int = 7
  }
  case object RetirementAnnuityAmountPage extends PaymentsIntoPensionPages {
    override val journeyNo: Int = 8
  }
  case object WorkplacePensionPage extends PaymentsIntoPensionPages {
    override val journeyNo: Int = 9
  }
  case object WorkplacePensionAmountPage extends PaymentsIntoPensionPages {
    override val journeyNo: Int = 10
  }
  case object CheckYourAnswersPage extends PaymentsIntoPensionPages {
    override val journeyNo: Int = 11
  }
}

