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

package views.pensions.shortServiceRefunds

import models.requests.UserSessionDataRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import views.html.pensions.shortServiceRefunds.RemoveRefundSchemeView
import controllers.pensions.shortServiceRefunds.routes._

class RemoveRefundSchemeViewSpec extends ViewUnitTest {

  private val pensionName: String = "pension name 1"

  object Selectors {
    val captionSelector: String               = "#main-content > div > div > form > header > p"
    val cancelLinkSelector: String            = "#cancel-link-id"
    def paragraphSelector(index: Int): String = s"#main-content > div > div > form > p:nth-of-type($index)"
    def bulletSelector(index: Int): String    = s"#main-content > div > div > form > ul > li:nth-child($index)"
  }

  trait CommonExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedCaption: Int => String
    val expectedParagraph1: String
    val expectedBullet1: String
    val expectedBullet2: String
    val expectedBullet3: String
    val buttonText: String
    val cancelText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedTitle                  = s"Are you sure you want to remove $pensionName?"
    val expectedHeading                = expectedTitle
    val expectedCaption: Int => String = (taxYear: Int) => s"Short service refunds for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedParagraph1             = "This will remove:"
    val expectedBullet1                = "The name of the pension scheme"
    val expectedBullet2                = "Pensions scheme tax reference"
    val expectedBullet3                = "Pensions provider address"
    val buttonText                     = "Remove"
    val cancelText                     = "Cancel"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedTitle                  = s"A ydych yn siŵr eich bod am dynnu $pensionName?"
    val expectedHeading                = expectedTitle
    val expectedCaption: Int => String = (taxYear: Int) => s"Ad-daliadau am wasanaeth byr ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val expectedParagraph1             = "Bydd hyn yn tynnu:"
    val expectedBullet1                = "Enw’r cynllun pensiwn"
    val expectedBullet2                = "Cyfeirnod treth y cynllun pensiwn"
    val expectedBullet3                = "Cyfeiriad y darparwr pensiwn"
    val buttonText                     = "Tynnu"
    val cancelText                     = "Canslo"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, String]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, None),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, None),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, None),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, None)
  )

  private lazy val underTest = inject[RemoveRefundSchemeView]
  "on show" should {
    userScenarios.foreach { userScenario =>
      import Selectors._
      import userScenario.commonExpectedResults._

      implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = getUserSession(userScenario.isAgent)
      implicit val messages: Messages                                         = getMessages(userScenario.isWelsh)
      s"language is ${welshTest(userScenario.isWelsh)} and request is from an ${agentTest(userScenario.isAgent)}" which {
        "render the remove short service refund pension scheme page" which {

          val htmlFormat                  = underTest(taxYearEOY, pensionName, Some(1))
          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          val url = RefundSummaryController.show(taxYearEOY).url

          titleCheck(expectedTitle, userScenario.isWelsh)
          h1Check(expectedHeading)
          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          textOnPageCheck(expectedParagraph1, paragraphSelector(1))
          textOnPageCheck(expectedBullet1, bulletSelector(1))
          textOnPageCheck(expectedBullet2, bulletSelector(2))
          textOnPageCheck(expectedBullet3, bulletSelector(3))
          buttonCheck(buttonText)
          linkCheck(cancelText, cancelLinkSelector, s"$url")
        }
      }
    }
  }
}
