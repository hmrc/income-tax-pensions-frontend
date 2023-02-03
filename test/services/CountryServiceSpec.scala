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

package services

import forms.{Countries, Country}
import org.checkerframework.checker.units.qual.A
import utils.UnitTest

class CountryServiceSpec extends UnitTest {

  "Countries" must {

    "give all countries with codes in alphabetical order of country name with filtering according to permitted MDG values" in {
      Countries.allCountries should contain(Country("Afghanistan", "AF", "AFG"))
      Countries.allCountries should contain(Country("Curaçao", "CW", "CUW"))
      Countries.allCountries should contain(Country("Réunion", "RE", "REU"))
      Countries.allCountries should contain(Country("Zimbabwe", "ZW", "ZWE"))
      Countries.allCountries should contain(Country("Åland Islands", "AX", "AXA"))
      Countries.allCountries should contain(Country("United Kingdom", "GB", "GBR"))
      Countries.allCountries should contain(Country("Czechoslovakia", "CS", "N/A"))
      Countries.allCountries.size shouldBe(247)
    }

    "give all eu countries with codes in alphabetical order of country name with filtering according to permitted MDG EU values" in {
      Countries.euCountries should contain(Country("France", "FR", "FRA"))
      Countries.euCountries should contain(Country("Germany", "DE", "DEU"))
      Countries.euCountries.size should be(27)
    }

    "give all overseas countries with codes in alphabetical order of country name with filtering according to permitted MDG values" in {
      Countries.getOverseasCountries() should contain(Country("Afghanistan", "AF", "AFG"))
      Countries.getOverseasCountries should contain(Country("Curaçao", "CW", "CUW"))
      Countries.getOverseasCountries should contain(Country("Réunion", "RE", "REU"))
      Countries.getOverseasCountries should contain(Country("Zimbabwe", "ZW", "ZWE"))
      Countries.getOverseasCountries should contain(Country("Åland Islands", "AX", "AXA"))
      Countries.getOverseasCountries should not contain(Country("United Kingdom", "GB", "GBR"))
      Countries.getOverseasCountries.size shouldBe(246)
    }

    "since the link between the twoAlphaCountryCode map and the threeAlphaCountryCode map is country name " +
                                                        "there should be no country name missing between both " in {
      val countries = Countries
      val threeDigitCountriesNotInTwoDigitMap =
        countries.countriesThreeAlphaMapFromCountryName.keySet -- countries.countriesTwoAlphaMapFromCountryName.keySet
      threeDigitCountriesNotInTwoDigitMap.size should be(0)

      val twoDigitCountriesNotInThreeDigitMap =
        countries.countriesTwoAlphaMapFromCountryName.keySet -- countries.countriesThreeAlphaMapFromCountryName.keySet
      twoDigitCountriesNotInThreeDigitMap.size should be(1)
      //Czechoslovakia does not have three digit country code
    }


  }
}
