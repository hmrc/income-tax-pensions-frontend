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

import models.pension.statebenefits.CustomerAddedStateBenefit

import java.time.{Instant, LocalDate}
import java.util.UUID

object CustomerAddedStateBenefitBuilder {

  val aCustomerAddedStateBenefitOne: CustomerAddedStateBenefit = CustomerAddedStateBenefit(
    benefitId = UUID.fromString("a9e8057e-fbbc-47a8-a8b4-78d9f015c934"),
    startDate = LocalDate.parse("2019-11-13"),
    submittedOn = Some(Instant.parse("2020-09-11T17:23:00Z")),
    endDate = Some(LocalDate.parse("2020-08-23")),
    amount = Some(55.11),
    taxPaid = Some(14.53)
  )

  val aCustomerAddedStateBenefitTwo: CustomerAddedStateBenefit = CustomerAddedStateBenefit(
    benefitId = UUID.fromString("a8e8057e-fbbc-47a8-a8b4-78d9f015c934"),
    startDate = LocalDate.parse("2019-11-12"),
    submittedOn = Some(Instant.parse("2020-09-11T17:22:00Z")),
    endDate = Some(LocalDate.parse("2020-08-22")),
    amount = Some(44.11),
    taxPaid = Some(13.53)
  )

  val aCustomerAddedStateBenefitThree: CustomerAddedStateBenefit = CustomerAddedStateBenefit(
    benefitId = UUID.fromString("a7e8057e-fbbc-47a8-a8b4-78d9f015c934"),
    startDate = LocalDate.parse("2019-11-11"),
    submittedOn = Some(Instant.parse("2020-09-11T17:21:00Z")),
    endDate = Some(LocalDate.parse("2020-08-21")),
    amount = Some(33.11),
    taxPaid = Some(12.53)
  )

  val aCustomerAddedStateBenefitFour: CustomerAddedStateBenefit = CustomerAddedStateBenefit(
    benefitId = UUID.fromString("a6e8057e-fbbc-47a8-a8b4-78d9f015c934"),
    startDate = LocalDate.parse("2019-11-10"),
    submittedOn = Some(Instant.parse("2020-09-11T17:20:00Z")),
    endDate = Some(LocalDate.parse("2020-08-22")),
    amount = Some(22.11),
    taxPaid = Some(11.53)
  )

  val aCustomerAddedStateBenefitFive: CustomerAddedStateBenefit = CustomerAddedStateBenefit(
    benefitId = UUID.fromString("a5e8057e-fbbc-47a8-a8b4-78d9f015c934"),
    startDate = LocalDate.parse("2019-11-09"),
    submittedOn = Some(Instant.parse("2020-09-11T17:19:00Z")),
    endDate = Some(LocalDate.parse("2020-08-21")),
    amount = Some(11.11),
    taxPaid = Some(9.53)
  )

  val aCustomerAddedStateBenefitSix: CustomerAddedStateBenefit = CustomerAddedStateBenefit(
    benefitId = UUID.fromString("a4e8057e-fbbc-47a8-a8b4-78d9f015c934"),
    startDate = LocalDate.parse("2019-11-08"),
    submittedOn = Some(Instant.parse("2020-09-11T17:18:00Z")),
    endDate = Some(LocalDate.parse("2020-08-20")),
    amount = Some(10.11),
    taxPaid = Some(8.53)
  )

  val aCustomerAddedStateBenefitSeven: CustomerAddedStateBenefit = CustomerAddedStateBenefit(
    benefitId = UUID.fromString("a3e8057e-fbbc-47a8-a8b4-78d9f015c934"),
    startDate = LocalDate.parse("2019-11-07"),
    submittedOn = Some(Instant.parse("2020-09-11T17:17:00Z")),
    endDate = Some(LocalDate.parse("2020-08-19")),
    amount = Some(9.11),
    taxPaid = Some(7.53)
  )

  val aCustomerAddedStateBenefitEight: CustomerAddedStateBenefit = CustomerAddedStateBenefit(
    benefitId = UUID.fromString("a2e8057e-fbbc-47a8-a8b4-78d9f015c934"),
    startDate = LocalDate.parse("2019-11-06"),
    submittedOn = Some(Instant.parse("2020-09-11T17:16:00Z")),
    endDate = Some(LocalDate.parse("2020-08-18")),
    amount = Some(9.11),
    taxPaid = Some(7.53)
  )

  val aCustomerAddedStateBenefitNine: CustomerAddedStateBenefit = CustomerAddedStateBenefit(
    benefitId = UUID.fromString("a1e8057e-fbbc-47a8-a8b4-78d9f015c934"),
    startDate = LocalDate.parse("2019-11-05"),
    submittedOn = Some(Instant.parse("2020-09-11T17:15:00Z")),
    endDate = Some(LocalDate.parse("2020-08-17")),
    amount = Some(8.11),
    taxPaid = Some(6.53)
  )

  val aCustomerAddedStateBenefitTen: CustomerAddedStateBenefit = CustomerAddedStateBenefit(
    benefitId = UUID.fromString("a1e7057e-fbbc-47a8-a8b4-78d9f015c934"),
    startDate = LocalDate.parse("2019-11-04"),
    submittedOn = Some(Instant.parse("2020-09-11T17:14:00Z")),
    endDate = Some(LocalDate.parse("2020-08-16")),
    amount = Some(7.11),
    taxPaid = Some(5.53)
  )
}
