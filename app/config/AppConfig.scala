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

import com.google.inject.ImplementedBy
import common.{Nino, TaxYear}
import models.pension.Journey
import play.api.Logging
import play.api.i18n.Lang
import play.api.mvc.{Call, RequestHeader}
import uk.gov.hmrc.play.bootstrap.binders.SafeRedirectUrl
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.Duration

@ImplementedBy(classOf[AppConfigImpl])
trait AppConfig {

  def signInBaseUrl: String

  def signInContinueBaseUrl: String

  def signInContinueUrl: String

  def signInOrigin: String

  def signInUrl: String

  def defaultTaxYear: Int

  def incomeTaxSubmissionBEBaseUrl: String

  def pensionBEBaseUrl: String

  def journeyAnswersUrl(taxYear: TaxYear, nino: Nino, journey: Journey): String

  def statePensionBEBaseUrl: String

  def incomeTaxSubmissionBaseUrl: String

  def incomeTaxSubmissionFrontendOverview: String

  def incomeTaxSubmissionOverviewUrl(taxYear: Int): String

  def incomeTaxSubmissionStartUrl(taxYear: Int): String

  def incomeTaxSubmissionIvRedirect: String

  def vcBaseUrl: String

  def viewAndChangeEnterUtrUrl: String

  def appUrl: String

  def contactFrontEndUrl: String

  def contactFormServiceIndividual: String

  def contactFormServiceAgent: String

  def contactFormServiceIdentifier(isAgent: Boolean): String

  def requestUri(implicit request: RequestHeader): String

  val feedbackFrontendUrl: String

  def feedbackSurveyUrl(implicit isAgent: Boolean): String

  def betaFeedbackUrl(implicit request: RequestHeader, isAgent: Boolean): String

  def contactUrl(implicit isAgent: Boolean): String

  def basGatewayUrl: String

  def signOutUrl: String

  def timeoutDialogTimeout: Int

  def timeoutDialogCountdown: Int

  // Mongo config
  def encryptionKey: String

  def mongoTTL: Int

  def taxYearErrorFeature: Boolean

  def languageMap: Map[String, Lang]

  def routeToSwitchLanguage: String => Call

  def welshToggleEnabled: Boolean

  def useEncryption: Boolean

  def emaSupportingAgentsEnabled: Boolean

}

@Singleton
class AppConfigImpl @Inject() (servicesConfig: ServicesConfig) extends Logging with AppConfig {
  val signInBaseUrl: String         = servicesConfig.getString(ConfigKeys.signInUrl)
  val signInContinueBaseUrl: String = servicesConfig.getString(ConfigKeys.signInContinueUrl)
  val signInContinueUrl: String     = SafeRedirectUrl(signInContinueBaseUrl).encodedUrl // TODO add redirect to overview page
  val signInOrigin                  = servicesConfig.getString("appName")
  val signInUrl: String             = s"$signInBaseUrl?continue=$signInContinueUrl&origin=$signInOrigin"

  // TODO Why we need this?
  def defaultTaxYear: Int = servicesConfig.getInt(ConfigKeys.defaultTaxYear)

  val incomeTaxSubmissionBEBaseUrl: String = servicesConfig.getString(ConfigKeys.incomeTaxSubmissionUrl) + "/income-tax-submission-service"
  val pensionBEBaseUrl: String             = servicesConfig.getString(ConfigKeys.incomeTaxPensionsUrl) + "/income-tax-pensions"

  def journeyAnswersUrl(taxYear: TaxYear, nino: Nino, journey: Journey): String =
    pensionBEBaseUrl + s"/${taxYear.endYear}/${journey.toString}/${nino.value}/answers"

  val statePensionBEBaseUrl: String = servicesConfig.getString(ConfigKeys.incomeTaxStateBenefitsUrl)

  val incomeTaxSubmissionBaseUrl: String = servicesConfig.getString(ConfigKeys.incomeTaxSubmissionFrontendUrl) +
    servicesConfig.getString("microservice.services.income-tax-submission-frontend.context")

  val incomeTaxSubmissionFrontendOverview: String =
    servicesConfig.getString("microservice.services.income-tax-submission-frontend.overview")

  def incomeTaxSubmissionOverviewUrl(taxYear: Int): String = incomeTaxSubmissionBaseUrl + "/" + taxYear + incomeTaxSubmissionFrontendOverview

  def incomeTaxSubmissionStartUrl(taxYear: Int): String = incomeTaxSubmissionBaseUrl + "/" + taxYear + "/start"

  val incomeTaxSubmissionIvRedirect: String =
    incomeTaxSubmissionBaseUrl + servicesConfig.getString("microservice.services.income-tax-submission-frontend.iv-redirect")

  val vcBaseUrl: String                = servicesConfig.getString(ConfigKeys.viewAndChangeFrontendUrl)
  val viewAndChangeEnterUtrUrl: String = s"$vcBaseUrl/report-quarterly/income-and-expenses/view/agents/client-utr"

  val appUrl: String     = servicesConfig.getString("microservice.url")
  val contactFrontEndUrl = servicesConfig.getString(ConfigKeys.contactFrontendUrl)

  val contactFormServiceIndividual = "update-and-submit-income-tax-return"
  val contactFormServiceAgent      = "update-and-submit-income-tax-return-agent"

  def contactFormServiceIdentifier(isAgent: Boolean): String = if (isAgent) contactFormServiceAgent else contactFormServiceIndividual

  def requestUri(implicit request: RequestHeader): String = SafeRedirectUrl(appUrl + request.uri).encodedUrl

  lazy val feedbackFrontendUrl = servicesConfig.getString(ConfigKeys.feedbackFrontendUrl)

  def feedbackSurveyUrl(implicit isAgent: Boolean): String = s"$feedbackFrontendUrl/feedback/${contactFormServiceIdentifier(isAgent)}"

  def betaFeedbackUrl(implicit request: RequestHeader, isAgent: Boolean): String =
    s"$contactFrontEndUrl/contact/beta-feedback?service=${contactFormServiceIdentifier(isAgent)}&backUrl=$requestUri"

  def contactUrl(implicit isAgent: Boolean): String = s"$contactFrontEndUrl/contact/contact-hmrc?service=${contactFormServiceIdentifier(isAgent)}"

  val basGatewayUrl = servicesConfig.getString(ConfigKeys.basGatewayFrontendUrl)

  val signOutUrl: String = s"$basGatewayUrl/bas-gateway/sign-out-without-state"

  val timeoutDialogTimeout: Int   = servicesConfig.getInt("timeoutDialogTimeout")
  val timeoutDialogCountdown: Int = servicesConfig.getInt("timeoutDialogCountdown")

  // Mongo config
  val encryptionKey: String = servicesConfig.getString("mongodb.encryption.key")
  val mongoTTL: Int         = Duration(servicesConfig.getString("mongodb.timeToLive")).toMinutes.toInt

  val taxYearErrorFeature: Boolean = servicesConfig.getBoolean("feature-switch.taxYearErrorFeatureSwitch") // TODO Why do we need this?

  val languageMap: Map[String, Lang] = Map(
    "english" -> Lang("en"),
    "cymraeg" -> Lang("cy")
  )

  def routeToSwitchLanguage: String => Call =
    (lang: String) => controllers.routes.LanguageSwitchController.switchToLanguage(lang)

  val welshToggleEnabled: Boolean = servicesConfig.getBoolean("feature-switch.welshToggleEnabled") // TODO Why do we need this?

  val useEncryption: Boolean = {
    logger.warn("[AesGCMCrypto][decrypt] Encryption is turned off")
    servicesConfig.getBoolean("feature-switch.useEncryption")
  }

  def emaSupportingAgentsEnabled: Boolean = servicesConfig.getBoolean("feature-switch.ema-supporting-agents-enabled")

}
