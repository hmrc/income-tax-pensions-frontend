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

package controllers

import controllers.ControllerSpec._
import org.jsoup.Jsoup.parse
import org.jsoup.nodes.Document
import play.api.libs.ws.WSResponse

class TwoAmountsControllerISpec(override val pathForThisPage: String) extends ControllerSpec(pathForThisPage) {

  private val amount1FieldName = "amount-1"
  private val amount2FieldName = "amount-2"

  def assertPageAsExpected(expectedStatusCode: Int, expectedPageContents: ExpectedOptionTupleAmountPageContents)(implicit userConfig: UserConfig, response: WSResponse): Unit = {

    val document = parse(response.body)
    response must haveStatus(expectedStatusCode)
    super.assertPageAsExpected(document, expectedPageContents)
    assertContinueButtonAsExpected(document, expectedPageContents.buttonForContinue)
    assertAmountSectionAsExpected(document, expectedPageContents.amountSection1, amount1FieldName)
    assertAmountSectionAsExpected(document, expectedPageContents.amountSection2, amount2FieldName)
  }

  private def assertAmountSectionAsExpected(document: Document, expectedAmountSection: ExpectedAmountSection, fieldName: String): Unit = {

    document must haveAnAmountValue(expectedAmountSection.value, fieldName)
    document must haveTextContents(".govuk-input__prefix", "£ £")
    document must haveAnAmountName(fieldName)
  }

  case class ExpectedOptionTupleAmountPageContents(
                                                    title: String,
                                                    header: String,
                                                    caption: String,
                                                    buttonForContinue: ExpectedButton,
                                                    amountSection1: ExpectedAmountSection,
                                                    amountSection2: ExpectedAmountSection,
                                                    errorSummarySectionOpt: Option[ErrorSummarySection] = None,
                                                    errorAboveElementCheckSectionOpt: Option[ErrorAboveElementCheckSection] = None,
                                                    links: Set[ExpectedLink] = Set.empty,
                                                    text: Set[ExpectedText] = Set.empty,
                                                    formUrl:Option[String] = None
                                                  ) extends BaseExpectedPageContents
}

case class SubmittedFormDataForOptionTupleAmountPage(amount1: Option[String], amount2: Option[String]) extends SubmittedOptionTupleAmount {
  val asMap: Map[String, String] = amountsAsMap(amount1, amount2)

}