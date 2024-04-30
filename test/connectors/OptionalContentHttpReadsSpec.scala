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

import cats.implicits._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.{JsNumber, JsString, Json}
import uk.gov.hmrc.http.HttpResponse

class OptionalContentHttpReadsSpec extends AnyWordSpecLike with Matchers {
  val underTest = new OptionalContentHttpReads[String]

  "read" should {
    "return None when no content" in {
      val res = underTest.read("method", "url", HttpResponse(204, ""))
      res shouldBe None.asRight
    }

    "return Some when there is successful response with a content" in {
      val res = underTest.read("method", "url", HttpResponse(200, Json.stringify(JsString("some content"))))
      res shouldBe "some content".some.asRight
    }

    "fail to parse incorrect value" in {
      val res = underTest.read("method", "url", HttpResponse(200, Json.stringify(JsNumber(42))))
      res shouldBe a[Left[_, _]]
    }
  }
}
