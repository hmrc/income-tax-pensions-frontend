/*
 * Copyright 2022 HM Revenue & Customs
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

package services

import forms.{Countries, Country}
import utils.UnitTest

class CountryServiceTest extends UnitTest {

  "Countries" must {

    "give all countries with codes in alphabetical order of country name with filtering according to permitted MDG values" in {

      Countries.all should contain(Country("Afghanistan", "AF"))
      Countries.all should contain(Country("Curaçao", "CW"))
      Countries.all should contain(Country("Réunion", "RE"))
      Countries.all should contain(Country("Zimbabwe", "ZW"))
      Countries.all should contain(Country("Åland Islands", "AX"))
      Countries.all.size shouldBe(247)
    }

    "give all eu countries with codes in alphabetical order of country name with filtering according to permitted MDG EU values" in {
      Countries.eu should contain(Country("France", "FR"))
      Countries.eu should contain(Country("Germany", "DE"))
      Countries.eu.size should be(27)
    }
  }
}