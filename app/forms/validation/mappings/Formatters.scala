/*
 * Copyright 2024 HM Revenue & Customs
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

package forms.validation.mappings

import play.api.data.FormError
import play.api.data.format.Formatter

import scala.util.control.Exception.nonFatalCatch

trait Formatters {

  private val maxAmount = "100000000000"

  private[mappings] def stringFormatter(errorKey: String, optional: Boolean = false): Formatter[String] = new Formatter[String] {

    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], String] =
      data.get(key) match {
        case None => Left(Seq(FormError(key, errorKey)))
        case Some(x) if x.trim.isEmpty && optional => Left(Seq(FormError(key, errorKey)))
        case Some(x) if x.trim.isEmpty && optional => Right(x.trim)
        case Some(s) => Right(s.trim)
      }

    override def unbind(key: String, value: String): Map[String, String] =
      Map(key -> value.trim)
  }

  private[mappings] def currencyFormatter(requiredKey: String,
                                          invalidNumericKey: String,
                                          maxAmountKey: String,
                                          args: Seq[String] = Seq.empty[String]
                                         ): Formatter[BigDecimal] =
    new Formatter[BigDecimal] {

      val is2dp = """\d+|\d*\.\d{1,2}"""
      val validNumeric = """[0-9.]*"""


      private val baseFormatter = stringFormatter(requiredKey)

      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], BigDecimal] = {
        baseFormatter
          .bind(key, data)
          .map(_.replace(",", ""))
          .map(_.replace("£", ""))
          .map(_.replaceAll("""\s""", ""))
          .flatMap {
            case s if s.isEmpty => Left(Seq(FormError(key, requiredKey, args)))
            case s if !s.matches(validNumeric) => Left(Seq(FormError(key, invalidNumericKey, args)))
            case s if !s.matches(is2dp) => Left(Seq(FormError(key, invalidNumericKey, args)))
            case s =>
              nonFatalCatch
                .either(BigDecimal(s.replaceAll("£", "")))
                .left.map(_ => Seq(FormError(key, invalidNumericKey, args)))
          }
          .flatMap { bigDecimal =>
            if (bigDecimal < BigDecimal(maxAmount)) Right(bigDecimal) else Left(Seq(FormError(key, maxAmountKey, args)))
          }
      }

      override def unbind(key: String, value: BigDecimal): Map[String, String] =
        baseFormatter.unbind(key, value.toString)
    }


  private def checksWithMinAmount(x: BigDecimal,
                                  key: String,
                                  minAmountKey: String,
                                  maxAmountKey: String,
                                  args: Seq[String]): Either[Seq[FormError], Option[BigDecimal]] = x match {
    case bigDecimal if bigDecimal == BigDecimal("0") => Left(Seq(FormError(key, minAmountKey, args)))
    case bigDecimal if bigDecimal >= BigDecimal(maxAmount) => Left(Seq(FormError(key, maxAmountKey, args)))
    case bigDecimal => Right(Some(bigDecimal))
  }

  private def checksWithOutMinAmount(x: BigDecimal,
                                     key: String,
                                     maxAmountKey: String,
                                     args: Seq[String]): Either[Seq[FormError], Option[BigDecimal]] = x match {
    case bigDecimal if bigDecimal >= BigDecimal(maxAmount) => Left(Seq(FormError(key, maxAmountKey, args)))
    case bigDecimal => Right(Some(bigDecimal))
  }

  private def checkIfValidString(inputString: String, key: String, requiredKey: String, invalidNumericKey: String,
                                 args: Seq[String]): Either[Seq[FormError], BigDecimal] = {

    val is2dp = """\d+|\d*\.\d{1,2}"""
    val validNumeric = """[0-9.]*"""

    inputString match {
      case s if s.isEmpty => Left(Seq(FormError(key, requiredKey, args)))
      case s if !s.matches(validNumeric) => Left(Seq(FormError(key, invalidNumericKey, args)))
      case s if !s.matches(is2dp) => Left(Seq(FormError(key, invalidNumericKey, args)))
      case s =>
        nonFatalCatch
          .either(BigDecimal(s.replaceAll("£", "")))
          .left.map(_ => Seq(FormError(key, invalidNumericKey, args)))
    }
  }

  private[mappings] def optionCurrencyFormatter(requiredKey: String,
                                                invalidNumericKey: String,
                                                maxAmountKey: String = "",
                                                minAmountkey: String,
                                                args: Seq[String] = Seq.empty[String]
                                               ): Formatter[Option[BigDecimal]] =
    new Formatter[Option[BigDecimal]] {

      private val baseFormatter = stringFormatter(requiredKey)

      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Option[BigDecimal]] = {
        baseFormatter
          .bind(key, data)
          .map(_.replace(",", ""))
          .map(_.replace("£", ""))
          .map(_.replaceAll("""\s""", ""))
          .flatMap(s => checkIfValidString(s, key, requiredKey, invalidNumericKey, args))
          .flatMap(bd =>
            if (minAmountkey != "") {
              checksWithMinAmount(bd, key, minAmountkey, maxAmountKey, args)
            } else {
              checksWithOutMinAmount(bd, key, maxAmountKey, args)
            }
          )
      }

      override def unbind(key: String, value: Option[BigDecimal]): Map[String, String] =
        baseFormatter.unbind(key, value.getOrElse("").toString)
    }

}
