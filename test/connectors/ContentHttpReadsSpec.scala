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

package connectors

import models.APIErrorModel
import models.pension.reliefs.PaymentsIntoPensionsViewModel
import org.scalatest.EitherValues._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.Reads._
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.http.HttpResponse

class ContentHttpReadsSpec extends AnyWordSpecLike with Matchers {

  "read" should {
    val httpReads = new ContentHttpReads[JsObject]
    val okResponse = HttpResponse(
      200,
      _: String,
      Map.empty
    )

    "parse json" in {
      val json   = """{"foo": "bar"}"""
      val result = httpReads.read("GET", "url", okResponse(json))

      result shouldBe Right(Json.parse(json))
    }

    "return an error if invalid json" in {
      val json   = """{"foo": "bar"""
      val result = httpReads.read("GET", "url", okResponse(json))

      result.left.value shouldBe a[APIErrorModel]
    }

    "return an empty object if no data" in {
      val httpReads = new ContentHttpReads[PaymentsIntoPensionsViewModel]
      val json      = """{"foo": "bar"}"""
      val result    = httpReads.read("GET", "url", okResponse(json))

      result.value shouldBe PaymentsIntoPensionsViewModel.empty
    }

  }
}
