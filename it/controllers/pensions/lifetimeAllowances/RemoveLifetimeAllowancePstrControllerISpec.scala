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

package controllers.pensions.lifetimeAllowances

import builders.PensionLifetimeAllowancesViewModelBuilder.aPensionLifetimeAllowancesViewModel
import builders.PensionsUserDataBuilder.{aPensionsUserData, pensionsUserDataWithLifetimeAllowance}
import builders.UserBuilder.aUserRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.PensionLifetimeAllowance._
import utils.PageUrls.{fullUrl, pensionSummaryUrl}
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

class RemoveLifetimeAllowancePstrControllerISpec extends IntegrationTest with ViewHelpers with BeforeAndAfterEach with PensionsDatabaseHelper {
  //scalastyle:off magic.number

  object Selectors {
    val captionSelector: String = "#main-content > div > div > form > header > p"
    val cancelLinkSelector: String = "#cancel-link-id"
    val insetSpanText: String = "#main-content > div > div > form > div.govuk-inset-text > span"
  }

  trait CommonExpectedResults {
    val expectedTitle: String
    lazy val expectedHeading: String = expectedTitle
    val expectedCaption: Int => String
    val buttonText: String
    val cancelText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedTitle = s"Do you want to remove this Pension Scheme Tax Reference?"
    val expectedCaption: Int => String = (taxYear: Int) => s"Lifetime allowances for 6 April ${taxYear - 1} to 5 April $taxYear"
    val buttonText = "Remove reference"
    val cancelText = "Don’t remove"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedTitle = s"A hoffech ddileu’r Cyfeirnod Treth ar gyfer y Cynllun Pensiwn hwn?"
    val expectedCaption: Int => String = (taxYear: Int) => s"Lwfansau oes ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val buttonText = "Dileu cyfeirnod"
    val cancelText = "Peidiwch â dileu"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, String]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, None),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, None)
  )

  ".show" should {
    userScenarios.foreach { user =>
      import Selectors._
      import user.commonExpectedResults._

      s"language is ${welshTest(user.isWelsh)}" should {

        "render the Remove Pension Scheme page" which {

          implicit lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(user.isAgent)
            insertCyaData(aPensionsUserData)
            urlGet(fullUrl(removePstrUrl(taxYearEOY, 1)), user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK status" in {
            result.status shouldBe OK
          }

          captionCheck(expectedCaption(taxYearEOY), captionSelector)
          titleCheck(expectedTitle, user.isWelsh)
          h1Check(expectedHeading)
          buttonCheck(buttonText)
          textOnPageCheck("12345678RB", insetSpanText)
          linkCheck(cancelText, cancelLinkSelector, s"${lifetimeAllowancePstrSummaryUrl(taxYearEOY)}")
        }
      }
    }

    "redirect to the PSTR summary page" when {
      "there are no schemes" should {
        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual()
          insertCyaData(pensionsUserDataWithLifetimeAllowance(aPensionLifetimeAllowancesViewModel.copy(pensionSchemeTaxReferences = None)))
          urlGet(fullUrl(removePstrUrl(taxYearEOY, 0)), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        s"has a SEE_OTHER ($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(lifetimeAllowancePstrSummaryUrl(taxYearEOY))
        }
      }

      "there are schemes but index is invalid" should {
        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual()
          insertCyaData(aPensionsUserData)
          urlGet(fullUrl(removePstrUrl(taxYearEOY, 12)), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        s"has a SEE_OTHER ($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(lifetimeAllowancePstrSummaryUrl(taxYearEOY))
        }
      }
    }

    "redirect to reduced annual allowance page" when {
      "page is invalid in journey" which {
        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual()
          val pensionsViewModel = aPensionLifetimeAllowancesViewModel.copy(aboveLifetimeAllowanceQuestion = Some(false))
          insertCyaData(pensionsUserDataWithLifetimeAllowance(pensionsViewModel))
          urlGet(fullUrl(removePstrUrl(taxYearEOY, 0)), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        "has a SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(pensionAboveAnnualLifetimeAllowanceUrl(taxYearEOY))
        }
      }
      "previous questions are unanswered" which {
        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual()
          val pensionsViewModel = aPensionLifetimeAllowancesViewModel.copy(pensionPaidAnotherWay = None)
          insertCyaData(pensionsUserDataWithLifetimeAllowance(pensionsViewModel))
          urlGet(fullUrl(removePstrUrl(taxYearEOY, 0)), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        "has a SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(pensionAboveAnnualLifetimeAllowanceUrl(taxYearEOY))
        }
      }
    }
  }

  ".submit" should {

    "persist scheme removal and redirect to the PSTR summary page when a valid index is used" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        insertCyaData(aPensionsUserData)
        urlPost(fullUrl(removePstrUrl(taxYearEOY, 0)), body = "", follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      s"has a SEE_OTHER ($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(lifetimeAllowancePstrSummaryUrl(taxYearEOY))
      }

      s"removes the scheme from the view model" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.pensionLifetimeAllowances.pensionSchemeTaxReferences shouldBe Some(Seq("12345678RB", "1234567DRD"))
      }
    }

    "redirect to the PSTR summary page without removing a scheme" when {
      "an invalid index is used" which {
        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual()
          insertCyaData(aPensionsUserData)
          urlPost(fullUrl(removePstrUrl(taxYearEOY, 12)), body = "", follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        s"has a SEE_OTHER ($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(lifetimeAllowancePstrSummaryUrl(taxYearEOY))
        }

        s"hasn't removed any schemes from the view model" in {
          lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
          cyaModel.pensions.pensionLifetimeAllowances.pensionSchemeTaxReferences shouldBe Some(Seq("1234567CRC", "12345678RB", "1234567DRD"))
        }
      }
      "there are no schemes" which {
        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual()
          insertCyaData(pensionsUserDataWithLifetimeAllowance(aPensionLifetimeAllowancesViewModel.copy(pensionSchemeTaxReferences = None)))
          urlPost(fullUrl(removePstrUrl(taxYearEOY, 12)), body = "", follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        s"has a SEE_OTHER ($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(lifetimeAllowancePstrSummaryUrl(taxYearEOY))
        }
      }
    }

    "redirect to first page in journey when page is invalid" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        val pensionsViewModel = aPensionLifetimeAllowancesViewModel.copy(pensionPaidAnotherWayQuestion = Some(false))
        insertCyaData(pensionsUserDataWithLifetimeAllowance(pensionsViewModel))

        urlPost(fullUrl(removePstrUrl(taxYearEOY, 0)), body = "", follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      "has a SEE_OTHER status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionAboveAnnualLifetimeAllowanceUrl(taxYearEOY))
      }
    }

    "redirect to the Pensions summary page when there is no session data" which {
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        urlPost(fullUrl(removePstrUrl(taxYearEOY, 0)), body = "", follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      s"has a SEE_OTHER ($SEE_OTHER) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionSummaryUrl(taxYearEOY))
      }
    }
  }

}
