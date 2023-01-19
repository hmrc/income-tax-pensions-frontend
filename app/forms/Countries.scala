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

  private def mdgCountryCodes(fileName: String): List[String] =
    Source
      .fromInputStream(getClass.getResourceAsStream(fileName))
      .getLines()
      .mkString
      .split(',')
      .map(_.replace("\"", ""))
      .toList

  val countriesThreeDigitMap : Map[String, String] = {
    val filename = "conf/three-digit-country-code-to-country-name.csv"
    val lines = Source.fromFile(filename).getLines.toList
    lines.foldLeft(List[(String, String)]()) {
      (acc, item) =>
        val lineArray = item.split("=")
        val (countryCode, countryName) = (lineArray.head, lineArray.last)
        (countryCode -> countryName) :: acc
    }.toMap
  }

  val countriesThreeDigitMapFromCountryName : Map[String, String] = {
    countriesThreeDigitMap.map(_.swap)
  }

  private val countries: List[Country] = {
    def fromJsonFile: List[Country] =
      Json.parse(getClass.getResourceAsStream("/location-autocomplete-canonical-list.json")) match {
        case JsArray(cs) =>
          cs.toList.collect {
            case JsArray(Seq(c: JsString, cc: JsString)) =>
              Country(c.value, alphaTwoCode(cc.value), countriesThreeDigitMapFromCountryName.getOrElse(c.value, "N/A"))
          }
        case _ =>
          throw new IllegalArgumentException(
            "Could not read JSON array of countries from : location-autocomplete-canonical-list.json"
          )
      }

    fromJsonFile.sortBy(_.countryName)
  }



  val countriesTwoDigitMap : Map[String, String] = {
    Json.parse(getClass.getResourceAsStream("/location-autocomplete-canonical-list.json")) match {
      case JsArray(cs) =>
        cs.toList.collect {
          case JsArray(Seq(c: JsString, cc: JsString)) =>
            (alphaTwoCode(cc.value), c.value)
        }.toMap
      case _ =>
        throw new IllegalArgumentException(
          "Could not read JSON array of countries from : location-autocomplete-canonical-list.json"
        )
    }
  }

  val countriesTwoDigitMapFromCountryName : Map[String, String] = {
    countriesTwoDigitMap.filter{ case (k,_) => mdgCountryCodes("/mdg-country-codes.csv") contains(k)}.map(_.swap)
  }

  private def alphaTwoCode: String => String = cc => cc.split(":")(1).trim

  val all: List[Country] = countries filter (c => mdgCountryCodes("/mdg-country-codes.csv") contains c.alphaTwoCode)

  val eu: List[Country] = countries filter (c => mdgCountryCodes("/mdg-country-codes-eu.csv") contains c.alphaTwoCode)


  def getCountryParametersForAllCountries(): List[Country] = all
  def getOverseasCountries(): List[Country] = all.filter(_.alphaTwoCode != "GB")

  def getCountryFromCode(alphaTwoCode: Option[String]): Option[Country] = {
    alphaTwoCode.flatMap(
      x => all.find(_.alphaTwoCode == x)
    )
  }
  
  def getCountryFromCodeWithDefault(countryCode: Option[String], defaultStr: String = "N/A"): String = {
    getCountryFromCode(countryCode).fold(countryCode.getOrElse(defaultStr))(_.countryName)
  }

  def get2dCountryCodeFrom3d(alphaThreeCode : String) : Option[String] = {
    countries.filter(country => country.alphaThreeCode == alphaThreeCode).headOption.map(_.alphaTwoCode)
  }

  def get3dCountryCodeFrom2d(alphaTwoCode : Option[String]) : Option[String] = {
    alphaTwoCode.map(cc => countries.filter(countryNamesWithCodes => cc == countryNamesWithCodes.alphaTwoCode).head).map(_.alphaThreeCode)
  }
}