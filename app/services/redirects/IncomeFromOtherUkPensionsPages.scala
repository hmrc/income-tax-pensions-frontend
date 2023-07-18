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

trait IncomeFromOtherUkPensionsPages {
  val pageId: Int
}

object IncomeFromOtherUkPensionsPages {

  case object DoYouGetUkPensionSchemePaymentsPage extends IncomeFromOtherUkPensionsPages {
    override val pageId: Int = 1
  }

  case object PensionSchemeDetailsPage extends IncomeFromOtherUkPensionsPages {
    override val pageId: Int = 2
  }

  case object HowMuchPensionDidYouGetPaidPage extends IncomeFromOtherUkPensionsPages {
    override val pageId: Int = 3
  }

  case object WhenDidYouStartGettingPaymentsPage extends IncomeFromOtherUkPensionsPages {
    override val pageId: Int = 4
  }

  case object CheckPensionSchemeDetailsPage extends IncomeFromOtherUkPensionsPages {
    override val pageId: Int = 5
  }

  case object UkPensionIncomePage extends IncomeFromOtherUkPensionsPages {
    override val pageId: Int = 6
  }

  case object RemovePensionIncomePage extends IncomeFromOtherUkPensionsPages {
    override val pageId: Int = 7
  }

  case object CheckUkPensionIncomeCYAPage extends IncomeFromOtherUkPensionsPages {
    override val pageId: Int = 8
  }
}
