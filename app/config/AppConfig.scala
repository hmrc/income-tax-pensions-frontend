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

package config

import common.{Nino, TaxYear}
import play.api.Logging
import play.api.i18n.Lang
import play.api.mvc.{Call, RequestHeader}
import uk.gov.hmrc.play.bootstrap.binders.SafeRedirectUrl
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.Duration

// TODO The lazy here are risky, you realize they are not set in PROD only when you'll receive the first request. Change them to not be lazy
@Singleton
class AppConfig @Inject() (servicesConfig: ServicesConfig) extends Logging {

  private lazy val signInBaseUrl: String = servicesConfig.getString(ConfigKeys.signInUrl)

  private lazy val signInContinueBaseUrl: String = servicesConfig.getString(ConfigKeys.signInContinueUrl)
  lazy val signInContinueUrl: String             = SafeRedirectUrl(signInContinueBaseUrl).encodedUrl // TODO add redirect to overview page
  private lazy val signInOrigin                  = servicesConfig.getString("appName")
  lazy val signInUrl: String                     = s"$signInBaseUrl?continue=$signInContinueUrl&origin=$signInOrigin"

  def defaultTaxYear: Int = servicesConfig.getInt(ConfigKeys.defaultTaxYear)

  lazy val incomeTaxSubmissionBEBaseUrl: String = servicesConfig.getString(ConfigKeys.incomeTaxSubmissionUrl) + "/income-tax-submission-service"

  lazy val pensionBEBaseUrl: String = servicesConfig.getString(ConfigKeys.incomeTaxPensionsUrl) + "/income-tax-pensions"

  def paymentsIntoPensionsAnswersUrl(taxYear: TaxYear, nino: Nino): String =
    pensionBEBaseUrl + s"/${taxYear.endYear}/payments-into-pensions/${nino.value}/answers"

  def upPensionIncomeAnswersUrl(taxYear: TaxYear, nino: Nino): String =
    pensionBEBaseUrl + s"/${taxYear.endYear}/pension-income/uk-pension-income/${nino.value}/answers"

  def statePensionAnswersUrl(taxYear: TaxYear, nino: Nino): String =
    pensionBEBaseUrl + s"/${taxYear.endYear}/pension-income/state-pension/${nino.value}/answers"

  def annualAllowancesAnswersUrl(taxYear: TaxYear, nino: Nino): String =
    pensionBEBaseUrl + s"/${taxYear.endYear}/annual-allowances/${nino.value}/answers"

  def unauthorisedPaymentsAnswersUrl(taxYear: TaxYear, nino: Nino): String =
    pensionBEBaseUrl + s"/${taxYear.endYear}/unauthorised-payments-from-pensions/${nino.value}/answers"

  def paymentsIntoOverseasPensionsUrl(taxYear: TaxYear, nino: Nino): String =
    pensionBEBaseUrl + s"/${taxYear.endYear}/payments-into-overseas-pensions/${nino.value}/answers"

  def transfersIntoOverseasPensionsAnswersUrl(taxYear: TaxYear, nino: Nino): String =
    pensionBEBaseUrl + s"/${taxYear.endYear}/transfer-into-overseas-pensions/${nino.value}/answers"

  def incomeFromOverseasPensionsAnswersUrl(taxYear: TaxYear, nino: Nino): String =
    pensionBEBaseUrl + s"/${taxYear.endYear}/income-from-overseas-pensions/${nino.value}/answers"

  lazy val employmentBEBaseUrl: String = servicesConfig.getString(ConfigKeys.incomeTaxEmploymentUrl) + "/income-tax-employment"

  lazy val statePensionBEBaseUrl: String = servicesConfig.getString(incomeTaxStateBenefitsUrl)
  private val incomeTaxStateBenefitsUrl  = "microservice.services.income-tax-state-benefits.url"

  def incomeTaxSubmissionBaseUrl: String = servicesConfig.getString(ConfigKeys.incomeTaxSubmissionFrontendUrl) +
    servicesConfig.getString("microservice.services.income-tax-submission-frontend.context")

  def incomeTaxSubmissionOverviewUrl(taxYear: Int): String = incomeTaxSubmissionBaseUrl + "/" + taxYear +
    servicesConfig.getString("microservice.services.income-tax-submission-frontend.overview")
  def incomeTaxSubmissionStartUrl(taxYear: Int): String = incomeTaxSubmissionBaseUrl + "/" + taxYear +
    "/start"
  def incomeTaxSubmissionIvRedirect: String = incomeTaxSubmissionBaseUrl +
    servicesConfig.getString("microservice.services.income-tax-submission-frontend.iv-redirect")

  private lazy val vcBaseUrl: String   = servicesConfig.getString(ConfigKeys.viewAndChangeUrl)
  def viewAndChangeEnterUtrUrl: String = s"$vcBaseUrl/report-quarterly/income-and-expenses/view/agents/client-utr"

  lazy private val appUrl: String     = servicesConfig.getString("microservice.url")
  lazy private val contactFrontEndUrl = servicesConfig.getString(ConfigKeys.contactFrontendUrl)

  lazy private val contactFormServiceIndividual                               = "update-and-submit-income-tax-return"
  lazy private val contactFormServiceAgent                                    = "update-and-submit-income-tax-return-agent"
  private def contactFormServiceIdentifier(implicit isAgent: Boolean): String = if (isAgent) contactFormServiceAgent else contactFormServiceIndividual

  private def requestUri(implicit request: RequestHeader): String = SafeRedirectUrl(appUrl + request.uri).encodedUrl

  private lazy val feedbackFrontendUrl = servicesConfig.getString(ConfigKeys.feedbackFrontendUrl)

  def feedbackSurveyUrl(implicit isAgent: Boolean): String = s"$feedbackFrontendUrl/feedback/$contactFormServiceIdentifier"

  def betaFeedbackUrl(implicit request: RequestHeader, isAgent: Boolean): String =
    s"$contactFrontEndUrl/contact/beta-feedback?service=$contactFormServiceIdentifier&backUrl=$requestUri"

  def contactUrl(implicit isAgent: Boolean): String = s"$contactFrontEndUrl/contact/contact-hmrc?service=$contactFormServiceIdentifier"

  private lazy val basGatewayUrl = servicesConfig.getString(ConfigKeys.basGatewayFrontendUrl)

  lazy val signOutUrl: String = s"$basGatewayUrl/bas-gateway/sign-out-without-state"

  lazy val timeoutDialogTimeout: Int   = servicesConfig.getInt("timeoutDialogTimeout")
  lazy val timeoutDialogCountdown: Int = servicesConfig.getInt("timeoutDialogCountdown")

  // Mongo config
  lazy val encryptionKey: String = servicesConfig.getString("mongodb.encryption.key")
  lazy val mongoTTL: Int         = Duration(servicesConfig.getString("mongodb.timeToLive")).toMinutes.toInt

  def taxYearErrorFeature: Boolean = servicesConfig.getBoolean("taxYearErrorFeatureSwitch")

  def languageMap: Map[String, Lang] = Map(
    "english" -> Lang("en"),
    "cymraeg" -> Lang("cy")
  )

  def routeToSwitchLanguage: String => Call =
    (lang: String) => controllers.routes.LanguageSwitchController.switchToLanguage(lang)

  lazy val welshToggleEnabled: Boolean = servicesConfig.getBoolean("feature-switch.welshToggleEnabled")

  lazy val useEncryption: Boolean = {
    logger.warn("[SecureGCMCipher][decrypt] Encryption is turned off")
    servicesConfig.getBoolean("useEncryption")
  }

}
