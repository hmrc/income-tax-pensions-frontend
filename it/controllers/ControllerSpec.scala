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

package controllers

import builders.PensionsUserDataBuilder.taxYearEOY
import common.SessionValues
import controllers.ControllerSpec.PreferredLanguages.{English, PreferredLanguage}
import controllers.ControllerSpec.UserTypes.Individual
import controllers.ControllerSpec._
import helpers.{PlaySessionCookieBaker, WireMockHelper, WiremockStubHelpers}
import models.User
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.pension.charges.TransfersIntoOverseasPensionsViewModel
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.{MatchResult, Matcher}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.http.HeaderNames
import play.api.http.Status.SEE_OTHER
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import play.api.{Application, Environment, Mode}
import repositories.PensionsUserDataRepositoryImpl
import uk.gov.hmrc.http.SessionKeys

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global

// scalastyle:off magic.number number.of.methods line.size.limit method.length

class ControllerSpec(val pathForThisPage: String) extends PlaySpec
  with GuiceOneServerPerSuite
  with WiremockStubHelpers
  with WireMockHelper
  with BeforeAndAfterAll
  with DocumentMatchers
  with ResponseMatchers {

  private val validMtdItId = "1234567890"
  private val validNino = "AA123456A"
  private val languageCodes = Map(PreferredLanguages.English -> "en", PreferredLanguages.Welsh -> "cy")
  protected val taxYear = 2022
  private val database: PensionsUserDataRepositoryImpl = app.injector.instanceOf[PensionsUserDataRepositoryImpl]
  private lazy val wiremockBaseUrl = s"http://$wiremockHost:$wiremockPort"
  private val validTaxYearsHeaderValue = Seq(taxYear).mkString(",")

  protected val scenarioNameForIndividualAndEnglish = "is an individual with a preferred language of English"
  protected val scenarioNameForIndividualAndWelsh = "is an individual with a preferred language of Welsh"
  protected val scenarioNameForAgentAndEnglish = "is an agent with a preferred language of English"
  protected val scenarioNameForAgentAndWelsh = "is an agent with a preferred language of Welsh"


  protected implicit val wsClient: WSClient = app.injector.instanceOf[WSClient]

  object PageRelativeURLs {
    val summaryPage: String = relativeUrl("/pensions-summary")

    val overseasPensionsSummary: String = relativeUrl("/overseas-pensions")

    val paymentsIntoPensionsCYAPage: String = relativeUrl("/payments-into-pensions/check-payments-into-pensions")
    val paymentsIntoPensionsOneNoTaxRelief: String = relativeUrl("/payments-into-pensions/no-tax-relief")
    val paymentsIntoPensionsOneOffPaymentsPage: String = relativeUrl("/payments-into-pensions/one-off-payments")
    val paymentsIntoPensionsOneOffPaymentsAmountPage: String = relativeUrl("/payments-into-pensions/one-off-payments-amount")
    val paymentsIntoPensionsRetirementAnnuityPage: String = relativeUrl("/payments-into-pensions/no-tax-relief/retirement-annuity")
    val paymentsIntoPensionsReliefAtSourcePage: String = relativeUrl("/payments-into-pensions/relief-at-source")
    val paymentsIntoPensionsReliefAtSourceAmountPage: String = relativeUrl("/payments-into-pensions/relief-at-source-amount")
    val paymentsIntoPensionsTotalReliefAtSourceCheckPage: String = relativeUrl("/payments-into-pensions/total-relief-at-source-check")

    val unauthorisedPaymentsCYAPage: String = relativeUrl("/unauthorised-payments-from-pensions/check-unauthorised-payments")

    val overseasSummaryPage: String = relativeUrl("/overseas-pensions")

    val paymentsIntoOverseasPensions: String = relativeUrl("/overseas-pensions/payments-into-overseas-pensions/payments-into-schemes")

    val incomeFromOverseasPensionsStatus: String = relativeUrl("/overseas-pensions/income-from-overseas-pensions/pension-overseas-income-status")
    val incomeFromOverseasPensionsCountry: String = relativeUrl("/overseas-pensions/income-from-overseas-pensions/pension-overseas-income-country")
    val incomeFromOverseasPensionsAmounts: String = relativeUrl("/overseas-pensions/income-from-overseas-pensions/pension-overseas-income-amounts")
    val incomeFromOverseasPensionsSwt: String = relativeUrl("/overseas-pensions/income-from-overseas-pensions/pension-overseas-income-swt")
    val incomeFromOverseasPensionsFtcr: String = relativeUrl("/overseas-pensions/income-from-overseas-pensions/pension-overseas-income-ftcr")
    val incomeFromOverseasPensionstaxable: String = relativeUrl("/overseas-pensions/income-from-overseas-pensions/taxable-amount")
    val incomeFromOverseasPensionsScheme: String = relativeUrl("/overseas-pensions/income-from-overseas-pensions/pension-scheme-summary")
    val incomeFromOverseasPensionsCountrySummary: String = relativeUrl("/overseas-pensions/income-from-overseas-pensions/pension-overseas-income-sountry-summary")
    val incomeFromOverseasPensionsCya: String = relativeUrl("/overseas-pensions/income-from-overseas-pensions/check-overseas-pension-income-cya")
    
    val overseasTransferChargePaid: String = relativeUrl("/overseas-pensions/overseas-transfer-charges/overseas-transfer-charge-paid")
    val transferIntoOverseasPensionsScheme: String = relativeUrl("/overseas-pensions/overseas-transfer-charges/overseas-transfer-charge-pension-scheme")

  }

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

  def assertPageAsExpected(document: Document, expectedPageContents: BaseExpectedPageContents)(implicit userConfig: UserConfig): Unit =
    assertPageAsExpected(document, expectedPageContents, userConfig.preferredLanguage)

  def assertRedirectionAsExpected(expectedLocation: String)(implicit response: WSResponse): Unit = {
    response must haveStatus(SEE_OTHER)
    response must haveALocationHeaderValue(expectedLocation)
  }


  protected def assertRadioButtonAsExpected(document: Document, index: Int, expectedRadioButton: ExpectedRadioButton): Unit = {
    document must haveARadioButtonAtIndex(index)
    document must haveARadioButtonAtIndexWithLabel(index, expectedRadioButton.label)
    if (expectedRadioButton.isChecked) document must haveACheckedRadioButtonAtIndex(index)
    else document must not(haveACheckedRadioButtonAtIndex(index))
  }

  protected def assertTextInputAsExpected(document: Document, expectedInputField: ExpectedInputField): Unit = {
    document must haveATextInputForSelector(expectedInputField.selector)
    document must haveATextInputName(expectedInputField.selector, expectedInputField.name)
    document must haveATextInputValue(expectedInputField.selector, expectedInputField.value)
  }

  protected def assertContinueButtonAsExpected(document: Document, expectedButton: ExpectedButton): Unit = {
    document must haveAContinueButtonWithLabel(expectedButton.label)
    document must haveAContinueButtonWithLink(expectedButton.link)
  }

  protected def givenAuthorised(userConfig: UserConfig): Unit = {
    userConfig.userType match {
      case UserTypes.Agent => authoriseAgent()
      case UserTypes.Individual => authoriseIndividual()
    }
  }
 
  protected def submitForm(submittedFormData: SubmittedFormData, queryParams: Map[String, String] = Map.empty)
                          (implicit userConfig: UserConfig, wsClient: WSClient): WSResponse = {
    submitForm(submittedFormData.asMap, queryParams)
  }
   
  protected def submitForm(dataMap: Map[String, String] , queryParams: Map[String, String])
                          (implicit userConfig: UserConfig, wsClient: WSClient): WSResponse = {
    givenAuthorised(userConfig)
    givenStoredSessionData(userConfig)
    await(
      wsClient.url(fullUrl(pathForThisPage))
        .withFollowRedirects(false)
        .withQueryStringParameters(queryParams.toList: _*)
        .withHttpHeaders(Seq(cookieHeader(userConfig), languageHeader(userConfig)) ++ Seq("Csrf-Token" -> "nocheck"): _*)
        .post(dataMap)
    )
  }
  
  protected def checkedExpectedRadioButton(label: String): ExpectedRadioButton = ExpectedRadioButton(label, isChecked = true)

  protected def uncheckedExpectedRadioButton(label: String): ExpectedRadioButton = ExpectedRadioButton(label, isChecked = false)

  protected def relativeUrl(pathStartingWithSlash: String): String = s"/update-and-submit-income-tax-return/pensions/$taxYear$pathStartingWithSlash"

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

  protected def loadPensionUserData(implicit userConfig: UserConfig): Option[PensionsUserData] =
    userConfig.sessionDataOpt.flatMap(originalSessionData => loadPensionUserData(originalSessionData, userConfig.userType))

  protected def relativeUrlForThisPage: String = relativeUrl(pathForThisPage)

  protected def getPage(implicit userConfig: UserConfig, wsClient: WSClient): WSResponse = getPage(pathForThisPage)

  protected def getPage(queryParams: Map[String, String])(implicit userConfig: UserConfig, wsClient: WSClient): WSResponse = getPage(pathForThisPage, queryParams)

  protected def getPage(sessionDataOpt: Option[PensionsUserData])(implicit wsClient: WSClient): WSResponse = {
    implicit val userConfig: UserConfig = UserConfig(Individual, English, sessionDataOpt)
    getPage(pathForThisPage)
  }

  def getPageWithIndex(index: Int = 0)(implicit userConfig: UserConfig, wsClient: WSClient): WSResponse = {
    getPage(Map("index" -> index.toString))
  }

  protected def userConfigWhenIrrelevant(sessionDataOpt: Option[PensionsUserData]): UserConfig =
    UserConfig(Individual, English, sessionDataOpt)

  private def assertPageAsExpected(document: Document, expectedPageContents: BaseExpectedPageContents, preferredLanguage: PreferredLanguage): Unit = {

    document must haveTitle(expectedPageContents.title)
    document must haveHeader(expectedPageContents.header)
    document must haveCaption(expectedPageContents.caption)

    val expectedPreferredLanguageTitle = if (preferredLanguage == PreferredLanguages.English) "English" else "Cymraeg"
    document must havePreferredLanguageSelectorTitle(preferredLanguage, expectedPreferredLanguageTitle)
    val expectedOtherLanguage = if (preferredLanguage == PreferredLanguages.English) PreferredLanguages.Welsh else PreferredLanguages.English
    val expectedOtherLanguageTitle = if (expectedOtherLanguage == PreferredLanguages.English) "Change the language to English English" else "Newid yr iaith ir Gymraeg Cymraeg"
    document must haveOtherLanguageSelectorTitle(expectedOtherLanguage, expectedOtherLanguageTitle)
    val expectedLanguageSelectorLink = if (preferredLanguage == PreferredLanguages.English) "/update-and-submit-income-tax-return/pensions/language/cymraeg" else "/update-and-submit-income-tax-return/pensions/language/english"
    document must haveLanguageSelectorLink(expectedLanguageSelectorLink)


    document must haveAFormWithTargetAction(expectedPageContents.formUrl.getOrElse(relativeUrlForThisPage))

    expectedPageContents.errorSummarySectionOpt match {
      case Some(expectedErrorSummarySection) =>
        document must haveAnErrorSummarySection


        document must haveAnErrorSummaryTitle(expectedErrorSummarySection.title)

        document must haveAnErrorSummaryBody(expectedErrorSummarySection.errorSummaryMessage.map(_.body).mkString(" "))
        expectedErrorSummarySection.errorSummaryMessage.foreach {
          a =>
            document must haveAnErrorSummaryLink(a.link, a.body)
        }
      case None =>
        document must not(haveAnErrorSummarySection)
    }

    expectedPageContents.errorAboveElementCheckSectionOpt match {
      case Some(errorAboveElementSection) =>
        document must haveAnErrorAboveElementSection(errorAboveElementSection.idOpt)
        document must haveAnErrorAboveElementTitle(errorAboveElementSection.idOpt, errorAboveElementSection.title)
      case None =>
        document must not(haveAnErrorAboveElementSection())
    }

    expectedPageContents.links.foreach(link => {
      document must haveLinkForId(link.id)
      document must haveLinkLabel(link.id, link.label)
      document must haveLinkHref(link.id, link.href)
    }
    )

    expectedPageContents.text.foreach(text => {
      document must haveTextForSelector(text.selector)
      document must haveTextContents(text.selector, text.contents)
    })
  }

  private def getPage(path: String, queryParam: Map[String, String] = Map.empty)
                     (implicit userConfig: UserConfig, wsClient: WSClient): WSResponse = {
    givenAuthorised(userConfig)
    givenStoredSessionData(userConfig)
    await(wsClient.url(fullUrl(path))
      .withFollowRedirects(false)
      .withQueryStringParameters(queryParam.toList: _*)
      .withHttpHeaders(Seq(cookieHeader(userConfig), languageHeader(userConfig)): _*).get()
    )
  }
  
  def getTransferPensionsViewModel(implicit userConfig: UserConfig): Option[TransfersIntoOverseasPensionsViewModel] =
    loadPensionUserData.map(_.pensions.transfersIntoOverseasPensions)

  private def loadPensionUserData(pensionsUserData: PensionsUserData, userType: UserTypes.UserType): Option[PensionsUserData]
  = await(
    database.find(
      taxYear,
      User(
        mtditid = pensionsUserData.mtdItId,
        arn = None,
        nino = pensionsUserData.nino,
        sessionId = pensionsUserData.sessionId,
        affinityGroup = userType.toString))
      .map {
        case Left(problem) => fail(s"Unable to get the updated session data: $problem")
        case Right(value) => value
      })

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


  private def generateSessionId(): String = UUID.randomUUID().toString

  private def generateBearerToken(): String = s"Bearer ${UUID.randomUUID()}"

  private def givenStoredSessionData(userConfig: UserConfig): Unit = {
    userConfig.sessionDataOpt.foreach(sessionData =>
      await(database.createOrUpdate(
        sessionData))
    )
  }

  private def fullUrl(pathStartingWithSlash: String): String = s"http://localhost:$port${relativeUrl(pathStartingWithSlash)}"
}

object ControllerSpec {

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

    class HaveOtherLanguageSelectorTitle(preferredLanguage: PreferredLanguage, title: String) extends Matcher[Document] {

      override def apply(document: Document): MatchResult = {
        val listItemIndex = if (preferredLanguage == PreferredLanguages.English) 0 else 1
        val actualValue = document.select(".hmrc-language-select__list-item").get(listItemIndex).text()
        val errorMessageIfExpected = s"The page didn't have the expected language selector title '$title'. The actual title was '$actualValue'"
        val errorMessageIfNotExpected = s"The page did indeed have the expected language selector title '$title', which was not expected."
        MatchResult(
          actualValue.equals(title),
          errorMessageIfExpected,
          errorMessageIfNotExpected
        )
      }
    }

    class HavePreferredLanguageSelectorTitle(preferredLanguage: PreferredLanguage, title: String) extends Matcher[Document] {

      override def apply(document: Document): MatchResult = {
        val listItemIndex = if (preferredLanguage == PreferredLanguages.English) 0 else 1
        val actualValue = document.select(".hmrc-language-select__list-item").get(listItemIndex).text()
        val errorMessageIfExpected = s"The page didn't have the expected preferred language selector title '$title'. The actual title was '$actualValue'"
        val errorMessageIfNotExpected = s"The page did indeed have the expected preferred language selector title '$title', which was not expected."
        MatchResult(
          actualValue.equals(title),
          errorMessageIfExpected,
          errorMessageIfNotExpected
        )
      }
    }


    class HaveLanguageSelectorLink(link: String) extends Matcher[Document] {

      override def apply(document: Document): MatchResult = {
        val actualLink = document.select(".hmrc-language-select__list-item > a").attr("href")
        val errorMessageIfExpected = s"The page didn't have the expected other language selector link '$link'. The actual link was '$actualLink'"
        val errorMessageIfNotExpected = s"The page did indeed have the expected other language selector link '$link', which was not expected."
        MatchResult(
          actualLink.equals(link),
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
        val label = document.select(".govuk-radios__item > label").get(index).text()
        val errorMessageIfExpected = s"The page doesn't have a radio button at index ($index) which is checked'; the label is '$label'."
        val errorMessageIfNotExpected = s"The page does indeed have a radio button at index ($index) which is checked, which was not expected; the label is '$label'."
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
        val errorMessageIfExpected = s"The page doesn't have an amount label of '$label'; the actual value is '$actualLabel'"
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

    class HaveAnAmountValue(value: String, name: String) extends Matcher[Document] {

      override def apply(document: Document): MatchResult = {
        val actualValue = document.select("#" + name).attr("value")
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
        val actualName = document.select("#" + name).attr("name")
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
        val errorMessageIfExpected = s"The page doesn't have a form with a POST action of '$action'; the actual method is '$actualMethod' and the action is '$actualAction'."
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

    class HaveAnErrorSummaryLink(link: String, body: String) extends Matcher[Document] {
      override def apply(document: Document): MatchResult = {
        val actualLink = document.select(".govuk-error-summary__body > ul > li > a")
        val expectedLink = s"""<a href="$link">$body</a>"""
        val errorMessageIfExpected = s"The page doesn't have an error summary link '$link'; it was actually '$actualLink'"
        val errorMessageIfNotExpected = s"The page does indeed have an error summary link of '$link', which was not expected."
        MatchResult(
          actualLink.toString.contains(expectedLink),
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
          exists && actualTitle.equals(title),
          errorMessageIfExpected,
          errorMessageIfNotExpected
        )
      }
    }

    class HaveLinkForId(id: String) extends Matcher[Document] {
      override def apply(document: Document): MatchResult = {
        val exists = document.select(s"#$id").attr("href").nonEmpty
        val errorMessageIfExpected = s"The page doesn't have a link with id '$id'."
        val errorMessageIfNotExpected = s"The page does indeed have a link with id '$id', which was not expected."
        MatchResult(
          exists,
          errorMessageIfExpected,
          errorMessageIfNotExpected
        )
      }
    }

    class HaveLinkLabel(id: String, label: String) extends Matcher[Document] {
      override def apply(document: Document): MatchResult = {
        val exists = document.select(s"#$id").attr("href").nonEmpty
        val actualLabel = document.select(s"#$id").text()
        val errorMessageIfExpected = s"The page doesn't have a link with label '$label' with id '$id'; the actual label was '$actualLabel'"
        val errorMessageIfNotExpected = s"The page does indeed have a link with label '$label' with id '$id', which was not expected."
        MatchResult(
          exists && actualLabel.equals(label),
          errorMessageIfExpected,
          errorMessageIfNotExpected
        )
      }
    }

    class HaveLinkHref(id: String, href: String) extends Matcher[Document] {
      override def apply(document: Document): MatchResult = {
        val exists = document.select(s"#$id").attr("href").nonEmpty
        val actualHref = document.select(s"#$id").attr("href")
        val errorMessageIfExpected = s"The page doesn't have a link with href '$href' with id '$id'; the actual href was '$actualHref'."
        val errorMessageIfNotExpected = s"The page does indeed have a link with href '$href' with id '$id', which was not expected."
        MatchResult(
          exists && actualHref.equals(href),
          errorMessageIfExpected,
          errorMessageIfNotExpected
        )
      }
    }

    class HaveTextForSelector(selector: String) extends Matcher[Document] {
      override def apply(document: Document): MatchResult = {
        val exists = !document.select(selector).isEmpty
        val errorMessageIfExpected = s"The page doesn't have text for selector '$selector'."
        val errorMessageIfNotExpected = s"The page does indeed have text for selector '$selector'., which was not expected."
        MatchResult(
          exists,
          errorMessageIfExpected,
          errorMessageIfNotExpected
        )
      }
    }

    class HaveTextContents(selector: String, contents: String) extends Matcher[Document] {
      override def apply(document: Document): MatchResult = {
        val exists = !document.select(selector).isEmpty
        val actualContents = document.select(selector).text()
        val errorMessageIfExpected = s"The page doesn't have text contents '$contents' for selector '$selector'; the actual contents were '$actualContents'."
        val errorMessageIfNotExpected = s"The page does indeed have text for selector '$selector'., which was not expected."
        MatchResult(
          exists && actualContents.equals(contents),
          errorMessageIfExpected,
          errorMessageIfNotExpected
        )
      }
    }

    class HaveATextInputForSelector(selector: String) extends Matcher[Document] {
      override def apply(document: Document): MatchResult = {
        val exists = !document.select(selector).isEmpty
        val inputType = document.select(selector).attr("type")
        val errorMessageIfExpected = s"The page doesn't have a text input for selector '$selector'."
        val errorMessageIfNotExpected = s"The page does indeed have a text input for selector '$selector', which was not expected."
        MatchResult(
          exists && inputType.equals("text"),
          errorMessageIfExpected,
          errorMessageIfNotExpected
        )
      }
    }

    class HaveATextInputName(selector: String, name: String) extends Matcher[Document] {
      override def apply(document: Document): MatchResult = {
        val exists = !document.select(selector).isEmpty
        val actualName = document.select(selector).attr("name")
        val errorMessageIfExpected = s"The page doesn't have a text input for selector '$selector' with name '$name', actual name was '$actualName'"
        val errorMessageIfNotExpected = s"The page does indeed have a text input for selector '$selector' with name '$name', which was not expected."
        MatchResult(
          exists && actualName.equals(name),
          errorMessageIfExpected,
          errorMessageIfNotExpected
        )
      }
    }

    class HaveATextInputValue(selector: String, name: String) extends Matcher[Document] {
      override def apply(document: Document): MatchResult = {
        val exists = !document.select(selector).isEmpty
        val actualValue = document.select(selector).attr("value")
        val errorMessageIfExpected = s"The page doesn't have a text input for selector '$selector' with value '$name', actual value was '$actualValue'"
        val errorMessageIfNotExpected = s"The page does indeed have a text input for selector '$selector' with value '$name', which was not expected."
        MatchResult(
          exists && actualValue.equals(name),
          errorMessageIfExpected,
          errorMessageIfNotExpected
        )
      }
    }

    def haveTitle(partialTitle: String) = new HasTitle(partialTitle)

    def haveHeader(header: String) = new HasHeader(header)

    def haveCaption(header: String) = new HasCaption(header)

    def havePreferredLanguageSelectorTitle(preferredLanguage: PreferredLanguage, title: String) = new HavePreferredLanguageSelectorTitle(preferredLanguage: PreferredLanguage, title)

    def haveOtherLanguageSelectorTitle(preferredLanguage: PreferredLanguage, title: String) = new HaveOtherLanguageSelectorTitle(preferredLanguage: PreferredLanguage, title)

    def haveLanguageSelectorLink(link: String) = new HaveLanguageSelectorLink(link)

    def haveARadioButtonAtIndex(index: Int) = new HaveARadioButtonAtIndex(index)

    def haveARadioButtonAtIndexWithLabel(index: Int, label: String) = new HaveARadioButtonAtIndexWithLabel(index, label)

    def haveACheckedRadioButtonAtIndex(index: Int) = new HaveACheckedRadioButtonAtIndex(index)

    def haveAContinueButtonWithLabel(label: String) = new HaveAContinueButtonWithLabel(label)

    def haveAContinueButtonWithLink(link: String) = new HaveAContinueButtonWithLink(link)

    def haveAnAmountLabel(link: String) = new HaveAnAmountLabel(link)

    def haveAnAmountHint(link: String) = new HaveAnAmountHint(link)

    def haveAnAmountValue(value: String, name: String) = new HaveAnAmountValue(value, name)

    def haveAnAmountName(value: String) = new HaveAnAmountName(value)

    def haveAFormWithTargetAction(url: String) = new HaveAFormWithTargetAction(url)

    def haveAnErrorSummarySection = new HaveAnErrorSummarySection

    def haveAnErrorSummaryTitle(title: String) = new HaveAnErrorSummaryTitle(title)

    def haveAnErrorSummaryBody(body: String) = new HaveAnErrorSummaryBody(body)

    def haveAnErrorSummaryLink(link: String, body: String) = new HaveAnErrorSummaryLink(link, body)

    def haveAnErrorAboveElementSection(idOpt: Option[String] = None) = new HaveAnErrorAboveElementSection(idOpt)

    def haveAnErrorAboveElementTitle(idOpt: Option[String] = None, title: String) = new HaveAnErrorAboveElementTitle(idOpt, title)

    def haveLinkForId(id: String) = new HaveLinkForId(id)

    def haveLinkLabel(id: String, label: String) = new HaveLinkLabel(id, label)

    def haveLinkHref(id: String, href: String) = new HaveLinkHref(id, href)

    def haveTextForSelector(selector: String) = new HaveTextForSelector(selector)

    def haveTextContents(selector: String, contents: String) = new HaveTextContents(selector, contents)

    def haveATextInputForSelector(selector: String) = new HaveATextInputForSelector(selector)

    def haveATextInputName(selector: String, name: String) = new HaveATextInputName(selector, name)

    def haveATextInputValue(selector: String, value: String) = new HaveATextInputValue(selector, value)
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
                        preferredLanguage: PreferredLanguage,
                        sessionDataOpt: Option[PensionsUserData])

  object UserTypes extends Enumeration {
    type UserType = Value
    val Agent, Individual = Value
  }

  object PreferredLanguages extends Enumeration {
    type PreferredLanguage = Value
    val English, Welsh = Value
  }

  trait BaseExpectedPageContents {
    def title: String

    def header: String

    def caption: String

    def errorSummarySectionOpt: Option[ErrorSummarySection]

    def errorAboveElementCheckSectionOpt: Option[ErrorAboveElementCheckSection]

    def formUrl: Option[String]

    def links: Set[ExpectedLink]

    def text: Set[ExpectedText]
  }

  case class ExpectedAmountSection(label: String, value: String, hintOpt: Option[String] = None)

  case class ExpectedRadioButton(label: String, isChecked: Boolean)

  case class ExpectedButton(label: String, link: String)

  case class ErrorSummarySection(title: String, errorSummaryMessage: Seq[ErrorSummaryMessage])

  object ErrorSummarySection {
    def apply(title: String, body: String, link: String): ErrorSummarySection = {
      new ErrorSummarySection(title, Seq(ErrorSummaryMessage(body, link)))
    }
  }

  case class ErrorSummaryMessage(body: String, link: String)


  case class ErrorAboveElementCheckSection(title: String, idOpt: Option[String])

  case class ExpectedLink(id: String, label: String, href: String)

  case class ExpectedText(selector: String, contents: String)

  case class ExpectedInputField(selector: String, name: String, value: String)

  trait SubmittedFormData {
    def asMap: Map[String, String]
  }

  trait SubmittedFormDataWithYesNo extends SubmittedFormData {

    private val fieldNameForYesNoSelection = "value"
    private val valueForYesSelection = "true"
    private val valueForNoSelection = "false"

    def yesOrNoAsMap(yesOrNoOpt: Option[Boolean]): Map[String, String] = yesOrNoOpt match {
      case Some(true) => Map(fieldNameForYesNoSelection -> valueForYesSelection)
      case Some(false) => Map(fieldNameForYesNoSelection -> valueForNoSelection)
      case None => Map.empty
    }
  }

  trait SubmittedOptionTupleAmount extends SubmittedFormData {
    private val amount1FieldName = "amount-1"
    private val amount2FieldName = "amount-2"

    def amountsAsMap(amount1: Option[String], amount2: Option[String]): Map[String, String] =
      (amount1, amount2) match {
        case (Some(a), Some(b)) => Map(amount1FieldName -> a, amount2FieldName -> b)
        case (Some(a), None) => Map(amount1FieldName -> a)
        case (None, Some(b)) => Map(amount2FieldName -> b)
        case _ => Map.empty
      }
  }
}