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

package forms

import play.api.libs.json._

import scala.io.Source

object Countries {

  lazy val allCountries: List[Country] = countriesFromFile filter (c => getCountryCodesFromFile("/mdg-country-codes.csv") contains c.alphaTwoCode)

  lazy val overseasCountries: List[Country] = allCountries.filter(_.alphaTwoCode != "GB")

  lazy val countriesThreeAlphaMapFromCountryName: Map[String, String] = countriesThreeAlphaMap.map(_.swap)

  lazy val incomeFromOverseasPensionsCountryCodeList: List[Country] =
    allCountries filter (c => getCountryCodesFromFile("/incomeFromOverseasPensions-threeAlphaCountryName.csv") contains c.alphaThreeCode)

  lazy val osTransferContributionsAndOsPaymentsAndSsRefundsCountryCodeList: List[Country] =
    overseasCountries filter (c =>
      getCountryCodesFromFile("/osTransferContributionsAndOsPaymentsAndSsRefunds-threeAlphaCountryName.csv") contains c.alphaThreeCode)

  lazy val countriesTwoAlphaMapFromCountryName: Map[String, String] =
    countriesTwoAlphaMap.filter { case (k, _) => getCountryCodesFromFile("/mdg-country-codes.csv") contains k }.map(_.swap)

  private def getCountryFromCode(alphaTwoCode: Option[String]): Option[Country] =
    alphaTwoCode.flatMap(x => allCountries.find(_.alphaTwoCode == x))

  def getCountryFromCodeWithDefault(countryCode: Option[String], defaultStr: String = "no country code"): String =
    getCountryFromCode(countryCode).fold(countryCode.getOrElse(defaultStr))(_.countryName)

  def get2AlphaCodeFrom3AlphaCode(alphaThreeCode: Option[String]): Option[String] =
    countriesFromFile.find(country => alphaThreeCode.contains(country.alphaThreeCode)).map(_.alphaTwoCode)

  def maybeGet3AlphaCodeFrom2AlphaCode(alphaTwoCode: Option[String]): Option[String] =
    alphaTwoCode.map(cc => countriesFromFile.filter(countryNamesWithCodes => cc == countryNamesWithCodes.alphaTwoCode).head).map(_.alphaThreeCode)

  def get3AlphaCodeFrom2AlphaCode(alphaTwoCode: String): String =
    countriesFromFile.filter(countryNamesWithCodes => alphaTwoCode == countryNamesWithCodes.alphaTwoCode).map(_.alphaThreeCode).head

  lazy private val countriesFromFile: List[Country] = {
    def fromJsonFile: List[Country] =
      Json.parse(getClass.getResourceAsStream("/location-autocomplete-canonical-list.json")) match {
        case JsArray(cs) =>
          cs.toList.collect { case JsArray(scala.collection.Seq(c: JsString, cc: JsString)) =>
            Country(c.value, alphaTwoCode(cc.value), countriesThreeAlphaMapFromCountryName.getOrElse(c.value, "N/A"))
          }
        case _ =>
          throw new IllegalArgumentException(
            "Could not read JSON array of countries from : location-autocomplete-canonical-list.json"
          )
      }

    fromJsonFile.sortBy(_.countryName)
  }

  lazy private val countriesTwoAlphaMap: Map[String, String] =
    Json.parse(getClass.getResourceAsStream("/location-autocomplete-canonical-list.json")) match {
      case JsArray(cs) =>
        cs.toList.collect { case JsArray(scala.collection.Seq(c: JsString, cc: JsString)) =>
          (alphaTwoCode(cc.value), c.value)
        }.toMap
      case _ =>
        throw new IllegalArgumentException(
          "Could not read JSON array of countries from : location-autocomplete-canonical-list.json"
        )
    }

  lazy private val countriesThreeAlphaMap: Map[String, String] = {
    val filename = "/three-digit-country-code-to-country-name.csv"
    val lines    = Source.fromInputStream(getClass.getResourceAsStream(filename)).mkString.split("\\n").toList
    lines
      .foldLeft(List[(String, String)]()) { (acc, item) =>
        val lineArray = item.split("=")
        val (countryCode, countryName) =
          (lineArray.headOption, lineArray.lastOption) match {
            case (Some(cc), Some(cn)) => (cc, cn)
            case _                    => throw new RuntimeException("Could not read country data from : location-autocomplete-canonical-list.json ")
          }
        (countryCode -> countryName) :: acc
      }
      .toMap
  }

  private def getCountryCodesFromFile(fileName: String): List[String] =
    Source
      .fromInputStream(getClass.getResourceAsStream(fileName))
      .getLines()
      .mkString
      .split(',')
      .map(_.replace("\"", ""))
      .toList

  private def alphaTwoCode: String => String = cc => cc.split(":")(1).trim

}
