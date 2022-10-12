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

  private val fieldNameForYesNoSelection = "value"
  private val valueForYesSelection = "true"
  private val valueForNoSelection = "false"
  private val indexOfRadioButtonForYes = 0
  private val indexOfRadioButtonForNo = 1

  private val fieldNameForAmountInput = "amount-2"

  def assertPageAsExpected(document: Document, expectedPageContents: ExpectedYesNoAmountPageContents, preferredLanguage: PreferredLanguages.PreferredLanguage): Unit = {

    super.assertPageAsExpected(document, expectedPageContents, preferredLanguage)

    document must haveARadioButtonAtIndex(indexOfRadioButtonForYes)
    document must haveARadioButtonAtIndexWithLabel(indexOfRadioButtonForYes, expectedPageContents.radioButtonForYes.label)
    if (expectedPageContents.radioButtonForYes.isChecked) document must haveACheckedRadioButtonAtIndex(indexOfRadioButtonForYes)
    else document must not(haveACheckedRadioButtonAtIndex(indexOfRadioButtonForYes))

    document must haveARadioButtonAtIndex(indexOfRadioButtonForNo)
    document must haveARadioButtonAtIndexWithLabel(indexOfRadioButtonForNo, expectedPageContents.radioButtonForNo.label)
    if (expectedPageContents.radioButtonForNo.isChecked) document must haveACheckedRadioButtonAtIndex(indexOfRadioButtonForNo)
    else document must not(haveACheckedRadioButtonAtIndex(indexOfRadioButtonForNo))

    document must haveAContinueButtonWithLabel(expectedPageContents.buttonForContinue.label)
    document must haveAContinueButtonWithLink(expectedPageContents.buttonForContinue.link)

    document must haveAnAmountLabel(expectedPageContents.amountSection.label)
    document must haveAnAmountHint(expectedPageContents.amountSection.hint)
    document must haveAnAmountValue(expectedPageContents.amountSection.value)
    document must haveTextContents(".govuk-input__prefix", "£")
    document must haveAnAmountName(fieldNameForAmountInput)

  }

  case class ExpectedYesNoAmountPageContents(title: String,
                                             header: String,
                                             caption: String,
                                             radioButtonForYes: ExpectedRadioButton,
                                             radioButtonForNo: ExpectedRadioButton,
                                             buttonForContinue: ExpectedButtonForContinue,
                                             amountSection: ExpectedAmountSection,
                                             errorSummarySectionOpt: Option[ErrorSummarySection] = None,
                                             errorAboveElementCheckSectionOpt: Option[ErrorAboveElementCheckSection] = None,
                                             links: Set[ExpectedLink] = Set.empty,
                                             text: Set[ExpectedText] = Set.empty
                                            ) extends ExpectedPageContents


  case class SubmittedFormDataForYesNoAmountPage(yesOrNoOpt: Option[Boolean], amountOpt: Option[String]) extends SubmittedFormData {

    val yesOrNoAsMap: Map[String, String] = yesOrNoOpt match {
      case Some(true) => Map(fieldNameForYesNoSelection -> valueForYesSelection)
      case Some(false) => Map(fieldNameForYesNoSelection -> valueForNoSelection)
      case None => Map.empty
    }

    val amountAsMap: Map[String, String] = amountOpt match {
      case Some(amount) => Map(fieldNameForAmountInput -> amount)
      case None => Map.empty
    }

    val asMap: Map[String, String] = yesOrNoAsMap ++ amountAsMap

  }

}
