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

package utils

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import helpers.WireMockHelper
import org.jsoup.nodes.Document
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.HeaderNames
import play.api.libs.ws.{BodyWritable, WSClient, WSResponse}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import utils.ViewUtils.bigDecimalCurrency

// scalastyle:off number.of.methods
// scalastyle:off number.of.types

trait ViewHelpers {
  self: AnyWordSpec with Matchers with WireMockHelper =>

  val serviceName    = "Update and submit an Income Tax Return"
  val serviceNameCy  = "Diweddaru a chyflwyno Ffurflen Dreth Incwm"
  val govUkExtension = "GOV.UK"

  val ENGLISH = "English"
  val WELSH   = "Welsh"

  val errorPrefix = "Error: "

  def getServiceName(isWelsh: Boolean = false): String = if (isWelsh) serviceNameCy else serviceName

  def welshTest(isWelsh: Boolean): String = if (isWelsh) "Welsh" else "English"

  def agentTest(isAgent: Boolean): String = if (isAgent) "Agent" else "Individual"

  def authoriseAgentOrIndividual(isAgent: Boolean = false, nino: Boolean = true): StubMapping =
    if (isAgent) authoriseAgent() else authoriseIndividual(nino)

  def unauthorisedAgentOrIndividual(isAgent: Boolean): StubMapping = if (isAgent) authoriseAgentUnauthorized() else authoriseIndividualUnauthorized()

  case class UserScenario[CommonExpectedResults, SpecificExpectedResults](isWelsh: Boolean,
                                                                          isAgent: Boolean,
                                                                          commonExpectedResults: CommonExpectedResults,
                                                                          specificExpectedResults: Option[SpecificExpectedResults] = None)

  val userScenarios: Seq[UserScenario[_, _]]

  def urlGet(url: String, welsh: Boolean = false, follow: Boolean = true, headers: Seq[(String, String)] = Seq())(implicit
      wsClient: WSClient): WSResponse = {

    val newHeaders = if (welsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") ++ headers else headers
    await(wsClient.url(url).withFollowRedirects(follow).withHttpHeaders(newHeaders: _*).get())
  }

  def urlPost[T](url: String, body: T, welsh: Boolean = false, follow: Boolean = true, headers: Seq[(String, String)] = Seq())(implicit
      wsClient: WSClient,
      bodyWritable: BodyWritable[T]): WSResponse = {

    val headersWithNoCheck = headers ++ Seq("Csrf-Token" -> "nocheck")
    val newHeaders         = if (welsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") ++ headersWithNoCheck else headersWithNoCheck
    await(wsClient.url(url).withFollowRedirects(follow).withHttpHeaders(newHeaders: _*).post(body))
  }

  def elementText(selector: String)(implicit document: () => Document): String =
    document().select(selector).text()

  def elementExist(selector: String)(implicit document: () => Document): Boolean =
    !document().select(selector).isEmpty

  def titleCheck(title: String, isWelsh: Boolean = false)(implicit document: () => Document): Unit =
    s"has a title of $title" in {
      document().title() shouldBe s"$title - ${getServiceName(isWelsh)} - $govUkExtension"
    }

  def hintTextCheck(text: String, selector: String = ".govuk-hint")(implicit document: () => Document): Unit =
    s"has the hint text of '$text'" in {
      elementText(selector) shouldBe text
    }

  def h1Check(header: String, size: String = "l")(implicit document: () => Document): Unit =
    s"have a page heading of '$header'" in {
      document().select(s".govuk-heading-$size").text() shouldBe header
    }

  def captionCheck(caption: String, selector: String = ".govuk-caption-l")(implicit document: () => Document): Unit =
    s"have the caption of '$caption'" in {
      document().select(selector).text() shouldBe caption
    }

  def textOnPageCheck(text: String, selector: String, additionalTestText: String = "")(implicit document: () => Document): Unit =
    s"have text on the screen of '$text' $additionalTestText" in {
      document().select(selector).text() shouldBe text
    }

  def elementNotOnPageCheck(selector: String)(implicit document: () => Document): Unit =
    s"not have the page element for selector '$selector'" in {
      document().select(selector).isEmpty shouldBe true
    }

  def changeAmountRowCheck(item: String,
                           value: String,
                           itemSelector: String,
                           valueSelector: String,
                           changeSelector: String,
                           changeHiddenText: String,
                           href: String)(implicit document: () => Document): Unit = {
    textOnPageCheck(item, itemSelector)
    textOnPageCheck(value, valueSelector, s"for the value of the $item field")
    linkCheck(changeHiddenText, changeSelector, href)
  }

  def changeAmountRowCheck(item: String, value: String, section: Int, row: Int, changeHiddenText: String, href: String)(implicit
      document: () => Document): Unit = {

    def benefitsItemSelector(section: Int, row: Int): String = s"#main-content > div > div > dl:nth-child($section) > div:nth-child($row) > dt"

    def benefitsAmountSelector(section: Int, row: Int): String =
      s"#main-content > div > div > dl:nth-child($section) > div:nth-child($row) > dd.govuk-summary-list__value"

    def benefitsChangeLinkSelector(section: Int, row: Int): String =
      s"#main-content > div > div > dl:nth-child($section) > div:nth-child($row) > dd > a"

    textOnPageCheck(item, benefitsItemSelector(section, row))
    textOnPageCheck(value, benefitsAmountSelector(section, row), s"for the value of the $item field")
    linkCheck(changeHiddenText, benefitsChangeLinkSelector(section, row), href)

  }

  def formRadioValueCheck(selected: Boolean, selector: String)(implicit document: () => Document): Unit =
    s"have a radio button form with the value set to '$selected'" in {
      document().select(selector).attr("value") shouldBe selected.toString
    }

  def formRadioValueCheckPreFilled(isChecked: Boolean, selector: String)(implicit document: () => Document): Unit =
    s"have a radio button that ${if (isChecked) "is" else "isn't"} pre-filled for selector '$selector'" in {
      document().select(selector).hasAttr("checked") shouldBe isChecked
    }

  def formGetLinkCheck(text: String, selector: String)(implicit document: () => Document): Unit =
    s"have a form with a GET action of '$text'" in {
      document().select(selector).attr("action") shouldBe text
      document().select(selector).attr("method") shouldBe "GET"
    }

  def formPostLinkCheck(text: String, selector: String)(implicit document: () => Document): Unit =
    s"have a form with a POST action of '$text'" in {
      document().select(selector).attr("action") shouldBe text
      document().select(selector).attr("method") shouldBe "POST"
    }

  def buttonCheck(text: String, selector: String = ".govuk-button", href: Option[String] = None)(implicit document: () => Document): Unit =
    s"have a $text button" which {
      s"has the text '$text'" in {
        document().select(selector).text() shouldBe text
      }
      s"has a class of govuk-button" in {
        document().select(selector).attr("class") should include("govuk-button")
      }

      if (href.isDefined) {
        s"has a href to '${href.get}'" in {
          document().select(selector).attr("href") shouldBe href.get
        }
      }
    }

  def radioButtonCheck(text: String, radioNumber: Int, checked: Option[Boolean] = None)(implicit document: () => Document): Unit =
    s"have a $text radio button" which {
      s"is of type radio button" in {
        val selector = ".govuk-radios__item > input"
        document().select(selector).get(radioNumber - 1).attr("type") shouldBe "radio"
      }
      s"has the text $text" in {
        val selector = ".govuk-radios__item > label"
        document().select(selector).get(radioNumber - 1).text() shouldBe text
      }
      if (checked.isDefined) {
        s"has the checked value set to ${checked.get}" in {
          val selector = ".govuk-radios__item > input"
          document().select(selector).get(radioNumber - 1).hasAttr("checked") shouldBe checked.get
        }
      }
    }

  def checkBoxCheck(text: String, checkBoxNumber: Int, checked: Option[Boolean] = None)(implicit document: () => Document): Unit =
    s"have a $text checkbox" which {
      s"is of type check box" in {
        val selector = ".govuk-checkboxes__input"
        document().select(selector).get(checkBoxNumber - 1).attr("type") shouldBe "checkbox"
      }
      s"has the text $text" in {
        val selector = ".govuk-checkboxes__label"
        document().select(selector).get(checkBoxNumber - 1).text() shouldBe text
      }
      if (checked.isDefined) {
        s"has the checked value set to ${checked.get}" in {
          val selector = ".govuk-checkboxes__input"
          document().select(selector).get(checkBoxNumber - 1).hasAttr("checked") shouldBe checked.get
        }
      }
    }

  def linkCheck(text: String, selector: String, href: String, hiddenTextSelector: Option[String] = None)(implicit document: () => Document): Unit =
    s"have a $text link" which {
      s"has the text '$text' and a href to '$href'" in {

        if (hiddenTextSelector.isDefined) {
          document().select(hiddenTextSelector.get).text() shouldBe text.split(" ").drop(1).mkString(" ")
        }

        document().select(selector).text() shouldBe text
        document().select(selector).attr("href") shouldBe href
      }
    }

  def inputFieldCheck(name: String, selector: String)(implicit document: () => Document): Unit =
    s"has a name of '$name'" in {
      document().select(selector).attr("name") shouldBe name
    }

  def inputFieldValueCheck(name: String, selector: String, value: String, defaultValue: Option[String] = None)(implicit
      document: () => Document): Unit = {
    s"'$selector' has a name of '$name'" in {
      document().select(selector).attr("name") shouldBe name
    }
    s"'$selector' has a value of '$value'" in {
      document().select(selector).attr("value") shouldBe value
    }

    defaultValue.foreach(defaultValue =>
      s"'$selector' has a default value of '$value'" in {
        document().select(selector).attr("data-default-value") shouldBe defaultValue
      })
  }

  def errorSummaryCheck(text: String, href: String)(implicit document: () => Document): Unit = {
    "contains an error summary" in {
      elementExist(".govuk-error-summary")
    }
    "contains the text 'There is a problem'" in {
      document().select(".govuk-error-summary__title").text() should (be("There is a problem") or be("Mae problem wedi codi"))
    }
    s"has a $text error in the error summary" which {
      s"has the text '$text'" in {
        document().select(".govuk-error-summary__body").text() shouldBe text
      }
      s"has a href to '$href'" in {
        document().select(".govuk-error-summary__body > ul > li > a").attr("href") shouldBe href
      }
    }
  }

  def multipleSummaryErrorCheck(errors: List[(String, String)])(implicit document: () => Document): Unit = {

    "contains an error summary" in {
      elementExist(".govuk-error-summary")
    }
    "contains the text 'There is a problem'" in {
      document().select(".govuk-error-summary__title").text() should (be("There is a problem") or be("Mae problem wedi codi"))
    }

    for (error <- errors) {
      val index    = errors.indexOf(error) + 1
      val selector = s".govuk-error-summary__body > ul > li:nth-child($index) > a"

      s"has a ${error._1} error in the error summary" which {
        s"has the text '${error._1}'" in {
          document().select(selector).text() shouldBe error._1
        }
        s"has a href to '${error._2}'" in {
          document().select(selector).attr("href") shouldBe error._2
        }
      }
    }
  }

  def errorAboveElementCheck(text: String, id: Option[String] = None)(implicit document: () => Document): Unit =
    s"has a $text error above the element" which {
      s"has the text '$text'" in {
        val selector = if (id.isDefined) s"#${id.get}-error" else ".govuk-error-message"
        document().select(selector).text() shouldBe s"Error: $text"
      }
    }

  def welshToggleCheck(isWelsh: Boolean)(implicit document: () => Document): Unit =
    welshToggleCheck(if (isWelsh) WELSH else ENGLISH)

  def welshToggleCheck(activeLanguage: String)(implicit document: () => Document): Unit = {
    val otherLanguage = if (activeLanguage == "English") "Welsh" else "English"

    def selector = Map("English" -> 0, "Welsh" -> 1)

    def linkLanguage = Map("English" -> "English", "Welsh" -> "Cymraeg")

    def linkText = Map("English" -> "Change the language to English English", "Welsh" -> "Newid yr iaith ir Gymraeg Cymraeg")

    s"have the language toggle already set to $activeLanguage" which {
      s"has the text '$activeLanguage" in {
        document().select(".hmrc-language-select__list-item").get(selector(activeLanguage)).text() shouldBe linkLanguage(activeLanguage)
      }
    }
    s"has a link to change the language to $otherLanguage" which {
      s"has the text '${linkText(otherLanguage)}" in {
        document().select(".hmrc-language-select__list-item").get(selector(otherLanguage)).text() shouldBe linkText(otherLanguage)
      }
      s"has a link to change the language" in {
        document().select(".hmrc-language-select__list-item > a").attr("href") shouldBe
          s"/update-and-submit-income-tax-return/pensions/language/${linkLanguage(otherLanguage).toLowerCase}"
      }
    }
  }

  def moneyContent(number: BigDecimal): String = bigDecimalCurrency(number.toString)

  def amountAndTaxPaidContent(amount: BigDecimal, taxPaid: BigDecimal, activeLanguage: String = "English"): String =
    if (activeLanguage == "English") {
      s"Amount: ${bigDecimalCurrency(amount.toString())} Tax paid: ${bigDecimalCurrency(taxPaid.toString())}"
    } else {
      s"Amount: ${bigDecimalCurrency(amount.toString())} Tax paid: ${bigDecimalCurrency(taxPaid.toString())}"
    }

  def cyaRowCheck(expectedText: String, expectedValue: String, changeLinkHref: String, changeLinkHiddenText: String, rowNumber: Int)(implicit
      document: () => Document): Unit = {
    val keySelector        = s"#main-content > div > div > dl > div:nth-child($rowNumber) > dt"
    val valueSelector      = s"#main-content > div > div > dl > div:nth-child($rowNumber) > dd.govuk-summary-list__value"
    val changeLinkSelector = s"#main-content > div > div > dl > div:nth-child($rowNumber) > dd.govuk-summary-list__actions > a"
    val cyaHiddenChangeLink =
      s"#main-content > div > div > dl > div:nth-child($rowNumber) > dd.govuk-summary-list__actions > a > span.govuk-visually-hidden"

    s"row number $rowNumber is correct" which {

      s"has the correct row name of '$expectedText'" in {
        document().select(keySelector).text() shouldBe expectedText
      }

      s"has the correct row value of '$expectedValue'" in {
        document().select(valueSelector).text() shouldBe expectedValue
      }

      s"the change link should go to '$changeLinkHref''" in {
        document().select(changeLinkSelector).attr("href") shouldBe changeLinkHref
      }

      s"the change link should have hidden text '$changeLinkHiddenText''" in {
        document().select(cyaHiddenChangeLink).text() shouldBe changeLinkHiddenText
      }

    }
  }

}

// scalastyle:on number.of.methods
// scalastyle:on number.of.types
