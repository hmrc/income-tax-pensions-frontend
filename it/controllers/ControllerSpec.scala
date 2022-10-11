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

import builders.PensionsUserDataBuilder.taxYearEOY
import common.SessionValues
import helpers.{PlaySessionCookieBaker, WireMockHelper, WiremockStubHelpers}
import models.User
import models.mongo.{PensionsCYAModel, PensionsUserData}
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.{MatchResult, Matcher}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.http.HeaderNames
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import play.api.{Application, Environment, Mode}
import repositories.PensionsUserDataRepositoryImpl
import uk.gov.hmrc.http.SessionKeys

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global

class ControllerSpec extends PlaySpec
  with GuiceOneServerPerSuite
  with WiremockStubHelpers
  with WireMockHelper
  with BeforeAndAfterAll
  with DocumentMatchers
  with ResponseMatchers {

  private val validMtdItId = "1234567890"
  private val validNino = "AA123456A"
  private val languageCodes = Map(PreferredLanguages.English -> "en", PreferredLanguages.Welsh -> "cy")
  private val taxYear = 2022
  private val database: PensionsUserDataRepositoryImpl = app.injector.instanceOf[PensionsUserDataRepositoryImpl]
  private lazy val wiremockBaseUrl = s"http://$wiremockHost:$wiremockPort"

  protected implicit val wsClient: WSClient = app.injector.instanceOf[WSClient]
  protected val scenarioNameForIndividualAndEnglish = "is an individual with a preferred language of English"
  protected val scenarioNameForIndividualAndWelsh = "is an individual with a preferred language of Welsh"
  protected val scenarioNameForAgentAndEnglish = "is an agent with a preferred language of English"
  protected val scenarioNameForAgentAndWelsh = "is an agent with a preferred language of Welsh"

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .in(Environment.simple(mode = Mode.Dev))
    .configure(
      Map(
        "defaultTaxYear" -> taxYear.toString,
        "auditing.enabled" -> "false",
        "play.filters.csrf.header.bypassHeaders.Csrf-Token" -> "nocheck",
        "microservice.services.income-tax-submission-frontend.url" -> wiremockBaseUrl,
        "microservice.services.auth.host" -> wiremockHost,
        "microservice.services.auth.port" -> wiremockPort.toString,
        "microservice.services.income-tax-pensions.url" -> wiremockBaseUrl,
        "microservice.services.income-tax-submission.url" -> wiremockBaseUrl,
        "microservice.services.view-and-change.url" -> wiremockBaseUrl,
        "microservice.services.income-tax-nrs-proxy.url" -> wiremockBaseUrl,
        "microservice.services.sign-in.url" -> "/auth-login-stub/gg-sign-in",
        "taxYearErrorFeatureSwitch" -> "false",
        "useEncryption" -> "true"
      )
    ).build

  override def beforeAll(): Unit = {
    super.beforeAll()
    startWiremock()
  }

  override def afterAll(): Unit = {
    stopWiremock()
    super.afterAll()
  }

  protected def givenAuthorised(userConfig: UserConfig): Unit = {
    userConfig.userType match {
      case UserTypes.Agent => authoriseAgent()
      case UserTypes.Individual => authoriseIndividual()
    }
  }

  protected def getPage(userConfig: UserConfig, path: String)(implicit wsClient: WSClient): WSResponse = {
    givenAuthorised(userConfig)
    givenStoredSessionData(userConfig)
    await(
      wsClient.url(fullUrl(path))
        .withFollowRedirects(false)
        .withHttpHeaders(Seq(cookieHeader(userConfig), languageHeader(userConfig)): _*).get())
  }

  protected def submitForm(userConfig: UserConfig, submittedFormData: Map[String, String], path: String)(implicit wsClient: WSClient): WSResponse = {
    givenAuthorised(userConfig)
    givenStoredSessionData(userConfig)
    await(
      wsClient.url(fullUrl(path))
        .withFollowRedirects(false)
        .withHttpHeaders(Seq(cookieHeader(userConfig), languageHeader(userConfig)) ++ Seq("Csrf-Token" -> "nocheck"): _*)
        .post(submittedFormData))
  }

  private def cookieHeader(userConfig: UserConfig): (String, String) = {
    val cookie =
      userConfig.sessionDataOpt
        .map(sessionData => cookieContents(sessionData))
        .getOrElse(minimalCookieContentsForAccessingPensions())
    (HeaderNames.COOKIE, cookie)
  }

  private def languageHeader(userConfig: UserConfig): (String, String) =
    (HeaderNames.ACCEPT_LANGUAGE, languageCodes.getOrElse(userConfig.preferredLanguage, "?"))

  private def cookieContents(pensionsUserData: PensionsUserData): String =
    PlaySessionCookieBaker.bakeSessionCookie(Map(
      SessionKeys.authToken -> generateBearerToken(),
      SessionValues.TAX_YEAR -> taxYear.toString,
      SessionValues.VALID_TAX_YEARS -> validTaxYearsHeaderValue,
      SessionKeys.sessionId -> pensionsUserData.sessionId,
      SessionValues.CLIENT_NINO -> pensionsUserData.nino,
      SessionValues.CLIENT_MTDITID -> pensionsUserData.mtdItId
    ))

  private def minimalCookieContentsForAccessingPensions(): String =
    PlaySessionCookieBaker.bakeSessionCookie(
      Map(
        SessionKeys.authToken -> generateBearerToken(),
        SessionKeys.sessionId -> generateSessionId(),
        SessionValues.CLIENT_MTDITID -> validMtdItId,
        SessionValues.TAX_YEAR -> taxYear.toString,
        SessionValues.VALID_TAX_YEARS -> validTaxYearsHeaderValue
      ))

  private val validTaxYearsHeaderValue = Seq(taxYear).mkString(",")

  private def generateSessionId(): String = UUID.randomUUID().toString

  private def generateBearerToken(): String = s"Bearer ${UUID.randomUUID()}"

  private def givenStoredSessionData(userConfig: UserConfig): Unit = {
    userConfig.sessionDataOpt.foreach(sessionData =>
      await(database.createOrUpdate(
        sessionData,
        User(
          mtditid = sessionData.mtdItId,
          arn = None,
          nino = sessionData.nino,
          sessionId = sessionData.sessionId,
          affinityGroup = "affinityGroup")))
    )
  }

  // TODO: Private
  protected def fullUrl(pathStartingWithSlash: String): String =
    s"http://localhost:$port${relativeUrl(pathStartingWithSlash)}"


  protected def relativeUrl(pathStartingWithSlash: String): String =
    s"/update-and-submit-income-tax-return/pensions/$taxYear$pathStartingWithSlash"

  protected def pensionsUserData(pensionsSessionData: PensionsCYAModel): PensionsUserData = {
    PensionsUserData(
      sessionId = generateSessionId(),
      mtdItId = validMtdItId,
      nino = validNino,
      taxYear = taxYearEOY,
      isPriorSubmission = true,
      pensions = pensionsSessionData
    )
  }


  protected def loadPensionUserData(pensionsUserData: PensionsUserData): PensionsUserData
  = await(
    database.find(
      taxYear,
      User(
        mtditid = pensionsUserData.mtdItId,
        arn = None,
        nino = pensionsUserData.nino,
        sessionId = pensionsUserData.sessionId,
        affinityGroup = "affinityGroup"))
      .map {
        case Left(problem) => fail(s"Unable to get the updated session data: $problem")
        case Right(value) => value.getOrElse(fail("No session data found for that user"))
      })
}


trait DocumentMatchers {

  class HasTitle(partialTitle: String) extends Matcher[Document] {

    override def apply(document: Document): MatchResult = {
      val fullTitle = s"$partialTitle - Update and submit an Income Tax Return - GOV.UK"
      val errorMessageIfExpected = s"The page didn't have the expected title '$fullTitle'. The actual title was '${document.title()}'"
      val errorMessageIfNotExpected = s"The page did indeed have the title '$fullTitle', which was not expected."
      MatchResult(
        document.title().equals(fullTitle),
        errorMessageIfExpected,
        errorMessageIfNotExpected
      )
    }
  }

  class HasHeader(header: String) extends Matcher[Document] {

    override def apply(document: Document): MatchResult = {
      val actualValue = document.select(s".govuk-heading-l").text()
      val errorMessageIfExpected = s"The page didn't have the expected header '$header'. The actual header was '$actualValue'"
      val errorMessageIfNotExpected = s"The page did indeed have the header '$header', which was not expected."
      MatchResult(
        actualValue.equals(header),
        errorMessageIfExpected,
        errorMessageIfNotExpected
      )
    }

  }

  class HasCaption(caption: String) extends Matcher[Document] {

    override def apply(document: Document): MatchResult = {
      val actualValue = document.select(".govuk-caption-l").text()
      val errorMessageIfExpected = s"The page didn't have the expected caption '$caption'. The actual caption was '$actualValue'"
      val errorMessageIfNotExpected = s"The page did indeed have the caption '$caption', which was not expected."
      MatchResult(
        actualValue.equals(caption),
        errorMessageIfExpected,
        errorMessageIfNotExpected
      )
    }

  }

  class HaveARadioButtonAtIndex(index: Int) extends Matcher[Document] {

    override def apply(document: Document): MatchResult = {
      val inputType = document.select(".govuk-radios__item > input").get(index).attr("type")
      val errorMessageIfExpected = s"The page doesn't have a radio button at that index ($index)"
      val errorMessageIfNotExpected = s"The page does indeed have a radio button at that index ($index), which was not expected."
      MatchResult(
        inputType.equals("radio"),
        errorMessageIfExpected,
        errorMessageIfNotExpected
      )
    }

  }

  class HaveARadioButtonAtIndexWithLabel(index: Int, label: String) extends Matcher[Document] {

    override def apply(document: Document): MatchResult = {
      val actualLabel = document.select(".govuk-radios__item > label").get(index).text()
      val errorMessageIfExpected = s"The page doesn't have a radio button at index ($index) with the expected label '$label'"
      val errorMessageIfNotExpected = s"The page does indeed have a radio button at index ($index) with the label '$label', which was not expected."
      MatchResult(
        actualLabel.equals(label),
        errorMessageIfExpected,
        errorMessageIfNotExpected
      )
    }

  }

  class HaveACheckedRadioButtonAtIndex(index: Int) extends Matcher[Document] {

    override def apply(document: Document): MatchResult = {
      val isChecked = document.select(".govuk-radios__item > input").get(index).hasAttr("checked")
      val errorMessageIfExpected = s"The page doesn't have a radio button at index ($index) which is checked'"
      val errorMessageIfNotExpected = s"The page does indeed have a radio button at index ($index) which is checked, which was not expected."
      MatchResult(
        isChecked.equals(true),
        errorMessageIfExpected,
        errorMessageIfNotExpected
      )
    }

  }

  class HaveAContinueButtonWithLabel(label: String) extends Matcher[Document] {

    override def apply(document: Document): MatchResult = {
      val isAButton = document.select("#continue").attr("class").contains("govuk-button")
      val actualLabel = document.select("#continue").text()
      val errorMessageIfExpected = s"The page doesn't have a continue button with the label '$label'"
      val errorMessageIfNotExpected = s"The page does indeed have a continue button with the label '$label', which was not expected."
      MatchResult(
        isAButton && actualLabel.equals(label),
        errorMessageIfExpected,
        errorMessageIfNotExpected
      )
    }
  }

  class HaveAContinueButtonWithLink(link: String) extends Matcher[Document] {

    override def apply(document: Document): MatchResult = {
      val isAButton = document.select("#continue").attr("class").contains("govuk-button")
      val actualLink = document.select("#continue").attr("href")
      val errorMessageIfExpected = s"The page doesn't have a continue button with the link '$link'. It was actually '$actualLink'"
      val errorMessageIfNotExpected = s"The page does indeed have a continue button with the link '$link', which was not expected."
      MatchResult(
        isAButton && actualLink.equals(link),
        errorMessageIfExpected,
        errorMessageIfNotExpected
      )
    }
  }

  class HaveAnAmountLabel(label: String) extends Matcher[Document] {

    override def apply(document: Document): MatchResult = {
      val actualLabel = document.select("#conditional-value > div > label").text()
      val errorMessageIfExpected = s"The page doesn't have an amount label of '$actualLabel'"
      val errorMessageIfNotExpected = s"The page does indeed have an amount label of '$actualLabel', which was not expected."
      MatchResult(
        actualLabel.equals(label),
        errorMessageIfExpected,
        errorMessageIfNotExpected
      )
    }
  }

  class HaveAnAmountHint(hint: String) extends Matcher[Document] {

    override def apply(document: Document): MatchResult = {
      val actualHint = document.select("#amount-2-hint").text()
      val errorMessageIfExpected = s"The page doesn't have an amount hint of '$hint'; the actual hint is '$actualHint'."
      val errorMessageIfNotExpected = s"The page does indeed have an amount hint of '$hint', which was not expected."
      MatchResult(
        actualHint.equals(hint),
        errorMessageIfExpected,
        errorMessageIfNotExpected
      )
    }
  }

  class HaveAnAmountValue(value: String) extends Matcher[Document] {

    override def apply(document: Document): MatchResult = {
      val actualValue = document.select("#amount-2").attr("value")
      val errorMessageIfExpected = s"The page doesn't have an amount value of '$value'; the actual value is '$actualValue'."
      val errorMessageIfNotExpected = s"The page does indeed have an amount value of '$value', which was not expected."
      MatchResult(
        actualValue.equals(value),
        errorMessageIfExpected,
        errorMessageIfNotExpected
      )
    }
  }

  class HaveAnAmountName(name: String) extends Matcher[Document] {

    override def apply(document: Document): MatchResult = {
      val actualName = document.select("#amount-2").attr("name")
      val errorMessageIfExpected = s"The page doesn't have an amount name of '$name'; the actual name is '$actualName'."
      val errorMessageIfNotExpected = s"The page does indeed have an amount name of '$actualName', which was not expected."
      MatchResult(
        actualName.equals(name),
        errorMessageIfExpected,
        errorMessageIfNotExpected
      )
    }
  }

  class HaveAFormWithTargetAction(action: String) extends Matcher[Document] {

    override def apply(document: Document): MatchResult = {
      val formSelector = "#main-content > div > div > form"
      val actualAction = document.select(formSelector).attr("action")
      val actualMethod = document.select(formSelector).attr("method")
      val errorMessageIfExpected = s"The page doesn't have a form with a POST action of '$action'; the actual method is '$actualMethod'"
      val errorMessageIfNotExpected = s"The page does indeed have a form with a POST action of '$action', which was not expected."
      MatchResult(
        actualMethod.equalsIgnoreCase("POST") && actualAction.equals(action),
        errorMessageIfExpected,
        errorMessageIfNotExpected
      )
    }

  }

  class HaveAnErrorSummarySection extends Matcher[Document] {

    override def apply(document: Document): MatchResult = {
      val errorMessageIfExpected = "The page doesn't have an error summary section."
      val errorMessageIfNotExpected = "The page does indeed have an error summary section."
      MatchResult(
        !document.select(".govuk-error-summary").isEmpty,
        errorMessageIfExpected,
        errorMessageIfNotExpected
      )
    }

  }

  class HaveAnErrorSummaryTitle(title: String) extends Matcher[Document] {

    override def apply(document: Document): MatchResult = {
      val actualTitle = document.select(".govuk-error-summary__title").text()
      val errorMessageIfExpected = s"The page doesn't have an error summary title '$title'; it was actually '$actualTitle'"
      val errorMessageIfNotExpected = s"The page does indeed have an error summary title of '$title', which was not expected."
      MatchResult(
        actualTitle.equals(title),
        errorMessageIfExpected,
        errorMessageIfNotExpected
      )
    }

  }


  class HaveAnErrorSummaryBody(body: String) extends Matcher[Document] {

    override def apply(document: Document): MatchResult = {
      val actualBody = document.select(".govuk-error-summary__body").text()
      val errorMessageIfExpected = s"The page doesn't have an error summary body '$body'; it was actually '$actualBody'"
      val errorMessageIfNotExpected = s"The page does indeed have an error summary body of '$body', which was not expected."
      MatchResult(
        actualBody.equals(body),
        errorMessageIfExpected,
        errorMessageIfNotExpected
      )
    }

  }


  class HaveAnErrorSummaryLink(link: String) extends Matcher[Document] {
    override def apply(document: Document): MatchResult = {
      val actualLink = document.select(".govuk-error-summary__body > ul > li > a").attr("href")
      val errorMessageIfExpected = s"The page doesn't have an error summary link '$link'; it was actually '$actualLink'"
      val errorMessageIfNotExpected = s"The page does indeed have an error summary link of '$link', which was not expected."
      MatchResult(
        actualLink.equals(link),
        errorMessageIfExpected,
        errorMessageIfNotExpected
      )
    }
  }

  class HaveAnErrorAboveElementSection(idOpt: Option[String]) extends Matcher[Document] {
    override def apply(document: Document): MatchResult = {
      val selector = idOpt.map(id => s"#$id-error").getOrElse(".govuk-error-message")
      val exists = !document.select(selector).isEmpty
      val errorMessageIfExpected = s"The page doesn't have an error above element section"
      val errorMessageIfNotExpected = s"The page does indeed have an error above element section, which was not expected."
      MatchResult(
        exists,
        errorMessageIfExpected,
        errorMessageIfNotExpected
      )
    }
  }

  class HaveAnErrorAboveElementTitle(idOpt: Option[String], title: String) extends Matcher[Document] {
    override def apply(document: Document): MatchResult = {
      val selector = idOpt.map(id => s"#$id-error").getOrElse(".govuk-error-message")
      val exists = !document.select(selector).isEmpty
      val actualTitle = document.select(selector).text()
      val errorMessageIfExpected = s"The page doesn't have an error above element title '$title'; it was actually '$actualTitle'"
      val errorMessageIfNotExpected = s"The page does indeed have an above about element title '$title', which was not expected."
      MatchResult(
        exists,
        errorMessageIfExpected,
        errorMessageIfNotExpected
      )
    }
  }


  def haveTitle(partialTitle: String) = new HasTitle(partialTitle)

  def haveHeader(header: String) = new HasHeader(header)

  def haveCaption(header: String) = new HasCaption(header)

  def haveARadioButtonAtIndex(index: Int) = new HaveARadioButtonAtIndex(index)

  def haveARadioButtonAtIndexWithLabel(index: Int, label: String) = new HaveARadioButtonAtIndexWithLabel(index, label)

  def haveACheckedRadioButtonAtIndex(index: Int) = new HaveACheckedRadioButtonAtIndex(index)

  def haveAContinueButtonWithLabel(label: String) = new HaveAContinueButtonWithLabel(label)

  def haveAContinueButtonWithLink(link: String) = new HaveAContinueButtonWithLink(link)

  def haveAnAmountLabel(link: String) = new HaveAnAmountLabel(link)

  def haveAnAmountHint(link: String) = new HaveAnAmountHint(link)

  def haveAnAmountValue(value: String) = new HaveAnAmountValue(value)

  def haveAnAmountName(value: String) = new HaveAnAmountName(value)

  def haveAFormWithTargetAction(url: String) = new HaveAFormWithTargetAction(url)

  def haveAnErrorSummarySection = new HaveAnErrorSummarySection

  def haveAnErrorSummaryTitle(title: String) = new HaveAnErrorSummaryTitle(title)

  def haveAnErrorSummaryBody(body: String) = new HaveAnErrorSummaryBody(body)

  def haveAnErrorSummaryLink(link: String) = new HaveAnErrorSummaryLink(link)

  def haveAnErrorAboveElementSection(idOpt: Option[String] = None) = new HaveAnErrorAboveElementSection(idOpt)

  def haveAnErrorAboveElementTitle(idOpt: Option[String] = None, title: String) = new HaveAnErrorAboveElementTitle(idOpt, title)

}


trait ResponseMatchers {

  class HaveALocationHeaderValue(value: String) extends Matcher[WSResponse] {

    override def apply(response: WSResponse): MatchResult = {
      val actualLocationHeaderValue = response.header("location")
      val errorMessageIfExpected = s"The response doesn't have a location header value of '$value'. It is actually '$actualLocationHeaderValue'"
      val errorMessageIfNotExpected = s"The response does indeed have a location header value of '$value', which was not expected."
      MatchResult(
        actualLocationHeaderValue.contains(value),
        errorMessageIfExpected,
        errorMessageIfNotExpected
      )
    }

  }


  class MustHaveStatus(status: Int) extends Matcher[WSResponse] {

    override def apply(response: WSResponse): MatchResult = {
      val actualStatus = response.status
      val errorMessageIfExpected = s"The response doesn't have a status of '$status'. It is actually '$actualStatus'"
      val errorMessageIfNotExpected = s"The response does indeed have a status of '$status', which was not expected."
      MatchResult(
        actualStatus == status,
        errorMessageIfExpected,
        errorMessageIfNotExpected
      )
    }

  }

  def haveALocationHeaderValue(value: String) = new HaveALocationHeaderValue(value)

  def haveStatus(status: Int) = new MustHaveStatus(status)

}

case class UserConfig(userType: UserTypes.UserType,
                      preferredLanguage: PreferredLanguages.PreferredLanguage,
                      sessionDataOpt: Option[PensionsUserData])

object UserTypes extends Enumeration {
  type UserType = Value
  val Agent, Individual = Value
}

object PreferredLanguages extends Enumeration {
  type PreferredLanguage = Value
  val English, Welsh = Value
}