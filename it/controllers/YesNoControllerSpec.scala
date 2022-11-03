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
import play.api.libs.ws.WSResponse

class YesNoControllerSpec(override val pathForThisPage: String) extends ControllerSpec(pathForThisPage) {

  def assertPageAsExpected(expectedStatusCode: Int, expectedPageContents: ExpectedYesNoPageContents)(implicit userConfig: UserConfig, response: WSResponse): Unit = {
    val document = parse(response.body)
    response must haveStatus(expectedStatusCode)
    super.assertPageAsExpected(document, expectedPageContents)
    assertRadioButtonAsExpected(document, 0, expectedPageContents.radioButtonForYes)
    assertRadioButtonAsExpected(document, 1, expectedPageContents.radioButtonForNo)
    assertContinueButtonAsExpected(document, expectedPageContents.buttonForContinue)
  }

  case class ExpectedYesNoPageContents(title: String,
                                       header: String,
                                       caption: String,
                                       radioButtonForYes: ExpectedRadioButton,
                                       radioButtonForNo: ExpectedRadioButton,
                                       buttonForContinue: ExpectedButton,
                                       errorSummarySectionOpt: Option[ErrorSummarySection] = None,
                                       errorAboveElementCheckSectionOpt: Option[ErrorAboveElementCheckSection] = None,
                                       links: Set[ExpectedLink] = Set.empty,
                                       text: Set[ExpectedText] = Set.empty,
                                       formUrl:Option[String] = None
                                      ) extends BaseExpectedPageContents


  case class SubmittedFormDataForYesNoPage(yesOrNoOpt: Option[Boolean]) extends SubmittedFormDataWithYesNo {

    val asMap: Map[String, String] = yesOrNoAsMap(yesOrNoOpt)

  }

}
