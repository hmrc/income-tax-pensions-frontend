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

package common

import play.api.mvc.PathBindable

/** Represents a tax year in the format yyyy - yyyy+1, e.g. 2023-2024. We keep only the last part (2024) which is the end of the tax year.
  */
final case class TaxYear(endYear: Int) extends AnyVal {
  override def toString: String = endYear.toString
  def startYear: String         = (endYear - 1).toString
}

object TaxYear {
  implicit def pathBindable(implicit intBinder: PathBindable[Int]): PathBindable[TaxYear] = new PathBindable[TaxYear] {

    override def bind(key: String, value: String): Either[String, TaxYear] =
      intBinder.bind(key, value).map(TaxYear.apply)

    override def unbind(key: String, taxYear: TaxYear): String =
      intBinder.unbind(key, taxYear.endYear)

  }
}
