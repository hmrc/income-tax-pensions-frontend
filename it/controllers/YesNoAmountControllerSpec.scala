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
import org.jsoup.nodes.Document

class YesNoAmountControllerSpec(override val pathForThisPage: String) extends ControllerSpec(pathForThisPage) {

  private val fieldNameForAmountInput = "amount-2"

  def assertPageAsExpected(document: Document, expectedPageContents: ExpectedYesNoAmountPageContents)(implicit userConfig: UserConfig): Unit = {

    super.assertPageAsExpected(document, expectedPageContents)
    assertRadioButtonAsExpected(document, 0, expectedPageContents.radioButtonForYes)
    assertRadioButtonAsExpected(document, 1, expectedPageContents.radioButtonForNo)
    assertContinueButtonAsExpected(document, expectedPageContents.buttonForContinue)
    assertAmountSectionAsExpected(document, expectedPageContents.amountSection)

  }

  private def assertAmountSectionAsExpected(document: Document, expectedAmountSection: ExpectedAmountSection): Unit = {
    document must haveAnAmountLabel(expectedAmountSection.label)
    document must haveAnAmountHint(expectedAmountSection.hint)
    document must haveAnAmountValue(expectedAmountSection.value)
    document must haveTextContents(".govuk-input__prefix", "£")
    document must haveAnAmountName(fieldNameForAmountInput)
  }

  case class ExpectedYesNoAmountPageContents(title: String,
                                             header: String,
                                             caption: String,
                                             radioButtonForYes: ExpectedRadioButton,
                                             radioButtonForNo: ExpectedRadioButton,
                                             buttonForContinue: ExpectedButton,
                                             amountSection: ExpectedAmountSection,
                                             errorSummarySectionOpt: Option[ErrorSummarySection] = None,
                                             errorAboveElementCheckSectionOpt: Option[ErrorAboveElementCheckSection] = None,
                                             links: Set[ExpectedLink] = Set.empty,
                                             text: Set[ExpectedText] = Set.empty
                                            ) extends ExpectedPageContents


  case class SubmittedFormDataForYesNoAmountPage(yesOrNoOpt: Option[Boolean], amountOpt: Option[String]) extends SubmittedFormDataWithYesNo {

    val amountAsMap: Map[String, String] = amountOpt match {
      case Some(amount) => Map(fieldNameForAmountInput -> amount)
      case None => Map.empty
    }

    val asMap: Map[String, String] = yesOrNoAsMap(yesOrNoOpt) ++ amountAsMap

  }

}
