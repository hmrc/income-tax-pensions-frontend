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

package builders

import models.pension.statebenefits.StateBenefit

object StateBenefitBuilder {

  val anStateBenefitOne: StateBenefit = StateBenefit(
    benefitId = "a9e8057e-fbbc-47a8-a8b4-78d9f015c934",
    startDate = "2019-11-13",
    dateIgnored = None,
    submittedOn = Some("2020-09-11T17:23:00Z"),
    endDate = Some("2020-08-23"),
    amount = Some(55.11),
    taxPaid = Some(14.53)
  )

  val anStateBenefitTwo: StateBenefit = StateBenefit(
    benefitId = "a8e8057e-fbbc-47a8-a8b4-78d9f015c934",
    startDate = "2019-11-12",
    dateIgnored = None,
    submittedOn = Some("2020-09-11T17:22:00Z"),
    endDate = Some("2020-08-22"),
    amount = Some(44.11),
    taxPaid = Some(13.53)
  )

  val anStateBenefitThree: StateBenefit = StateBenefit(
    benefitId = "a7e8057e-fbbc-47a8-a8b4-78d9f015c934",
    startDate = "2019-11-11",
    dateIgnored = None,
    submittedOn = Some("2020-09-11T17:21:00Z"),
    endDate = Some("2020-08-21"),
    amount = Some(33.11),
    taxPaid = Some(12.53)
  )

  val anStateBenefitFour: StateBenefit = StateBenefit(
    benefitId = "a6e8057e-fbbc-47a8-a8b4-78d9f015c934",
    startDate = "2019-11-10",
    dateIgnored = None,
    submittedOn = Some("2020-09-11T17:20:00Z"),
    endDate = Some("2020-08-22"),
    amount = Some(22.11),
    taxPaid = Some(11.53)
  )

  val anStateBenefitFive: StateBenefit = StateBenefit(
    benefitId = "a5e8057e-fbbc-47a8-a8b4-78d9f015c934",
    startDate = "2019-11-09",
    dateIgnored = None,
    submittedOn = Some("2020-09-11T17:19:00Z"),
    endDate = Some("2020-08-21"),
    amount = Some(11.11),
    taxPaid = Some(9.53)
  )

  val anStateBenefitSix: StateBenefit = StateBenefit(
    benefitId = "a4e8057e-fbbc-47a8-a8b4-78d9f015c934",
    startDate = "2019-11-08",
    dateIgnored = None,
    submittedOn = Some("2020-09-11T17:18:00Z"),
    endDate = Some("2020-08-20"),
    amount = Some(10.11),
    taxPaid = Some(8.53)
  )

  val anStateBenefitSeven: StateBenefit = StateBenefit(
    benefitId = "a3e8057e-fbbc-47a8-a8b4-78d9f015c934",
    startDate = "2019-11-07",
    dateIgnored = None,
    submittedOn = Some("2020-09-11T17:17:00Z"),
    endDate = Some("2020-08-19"),
    amount = Some(9.11),
    taxPaid = Some(7.53)
  )

  val anStateBenefitEight: StateBenefit = StateBenefit(
    benefitId = "a2e8057e-fbbc-47a8-a8b4-78d9f015c934",
    startDate = "2019-11-06",
    dateIgnored = None,
    submittedOn = Some("2020-09-11T17:16:00Z"),
    endDate = Some("2020-08-18"),
    amount = Some(9.11),
    taxPaid = Some(7.53)
  )

  val anStateBenefitNine: StateBenefit = StateBenefit(
    benefitId = "a1e8057e-fbbc-47a8-a8b4-78d9f015c934",
    startDate = "2019-11-05",
    dateIgnored = None,
    submittedOn = Some("2020-09-11T17:15:00Z"),
    endDate = Some("2020-08-17"),
    amount = Some(8.11),
    taxPaid = Some(6.53)
  )

  val anStateBenefitTen: StateBenefit = StateBenefit(
    benefitId = "a1e7057e-fbbc-47a8-a8b4-78d9f015c934",
    startDate = "2019-11-04",
    dateIgnored = None,
    submittedOn = Some("2020-09-11T17:14:00Z"),
    endDate = Some("2020-08-16"),
    amount = Some(7.11),
    taxPaid = Some(5.53)
  )
}
