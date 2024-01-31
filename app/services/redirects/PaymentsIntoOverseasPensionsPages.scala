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

sealed trait PaymentsIntoOverseasPensionsPages {
  val journeyNo: Int
  val hasIndex: Boolean
}

object PaymentsIntoOverseasPensionsPages {

  case object PaymentIntoPensionSchemePage extends PaymentsIntoOverseasPensionsPages {
    override val journeyNo: Int = 1
    override val hasIndex       = false
  }

  case object EmployerPayOverseasPensionPage extends PaymentsIntoOverseasPensionsPages {
    override val journeyNo: Int = 2
    override val hasIndex       = false
  }

  case object TaxEmployerPaymentsPage extends PaymentsIntoOverseasPensionsPages {
    override val journeyNo: Int = 3
    override val hasIndex       = false
  }

  case object PensionsCustomerReferenceNumberPage extends PaymentsIntoOverseasPensionsPages {
    override val journeyNo: Int = 4
    override val hasIndex       = true
  }

  case object UntaxedEmployerPaymentsPage extends PaymentsIntoOverseasPensionsPages {
    override val journeyNo: Int = 5
    override val hasIndex       = true
  }

  case object PensionReliefTypePage extends PaymentsIntoOverseasPensionsPages {
    override val journeyNo: Int = 6
    override val hasIndex       = true
  }

  case object QOPSReferencePage extends PaymentsIntoOverseasPensionsPages {
    override val journeyNo: Int = 7
    override val hasIndex       = true
  }

  case object DoubleTaxationAgreementPage extends PaymentsIntoOverseasPensionsPages {
    override val journeyNo: Int = 8
    override val hasIndex       = true
  }

  case object SF74ReferencePage extends PaymentsIntoOverseasPensionsPages {
    override val journeyNo: Int = 9
    override val hasIndex       = true
  }

  case object ReliefsSchemeDetailsPage extends PaymentsIntoOverseasPensionsPages {
    override val journeyNo: Int = 10
    override val hasIndex       = true
  }

  case object ReliefsSchemeSummaryPage extends PaymentsIntoOverseasPensionsPages {
    override val journeyNo: Int = 11
    override val hasIndex       = false
  }

  case object RemoveReliefsSchemePage extends PaymentsIntoOverseasPensionsPages {
    override val journeyNo: Int = 12
    override val hasIndex       = true
  }

  case object PaymentsIntoOverseasPensionsCYAPage extends PaymentsIntoOverseasPensionsPages {
    override val journeyNo: Int = 13
    override val hasIndex       = false
  }

}
