/*
 * Copyright 2024 HM Revenue & Customs
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

import play.sbt.routes.RoutesKeys

lazy val appName = "income-tax-pensions-frontend"

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "2.13.16"

lazy val coverageSettings: Seq[Setting[?]] = {
  import scoverage.ScoverageKeys

  val excludedPackages = Seq(
    "<empty>",
    ".*Reverse.*",
    ".*standardError*.*",
    ".*govuk_wrapper*.*",
    ".*main_template*.*",
    "uk.gov.hmrc.BuildInfo",
    "app.*",
    "prod.*",
    "config.*",
    "testOnly.*",
    "testOnlyDoNotUseInAppConf.*",
    ".*feedback*.*",
    "partials.*",
    "controllers.testOnly.*",
    "forms.validation.mappings",
    "views.html.*[Tt]emplate.*",
    "views.html.views.templates.helpers*",
    "views.html.views.templates.inputs*",
    "views.headerFooterTemplate"
  )

  Seq(
    ScoverageKeys.coverageExcludedFiles := "<empty>;Reverse.*;.*handlers.*;.*components.*;" +
      ".*Routes.*;.*viewmodels.govuk.*;",
    ScoverageKeys.coverageExcludedPackages  := excludedPackages.mkString(";"),
    ScoverageKeys.coverageMinimumStmtTotal  := 42,
    ScoverageKeys.coverageMinimumBranchTotal:= 42,
    ScoverageKeys.coverageFailOnMinimum     := true,
    ScoverageKeys.coverageHighlighting      := true
  )
}

lazy val twirlImports: Seq[String] = Seq(
  "config.AppConfig",
  "uk.gov.hmrc.govukfrontend.views.html.components._",
  "uk.gov.hmrc.hmrcfrontend.views.html.components._",
  "uk.gov.hmrc.hmrcfrontend.views.html.helpers._"
)

lazy val microservice = Project(appName, file("."))
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(PlayKeys.playDefaultPort := 9321)
  .settings(
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    TwirlKeys.templateImports ++= twirlImports,
    libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always,
    scalacOptions += "-Wconf:cat=unused-imports&src=html/.*:s", // suppress warnings
    scalacOptions += "-Wconf:src=routes/.*:s"
  )
  .settings(inConfig(Test)(testSettings): _*)
  .configs(IntegrationTest extend Test)
  .settings(inConfig(IntegrationTest)(itSettings): _*)
  .settings(coverageSettings: _*)
  .settings(RoutesKeys.routesImport ++= Seq(
    "models.pension._",
    "common._"
  ))
  .settings(
    // concatenate js
    Concat.groups := Seq(
      "javascripts/application.js" ->
        group(Seq(
          "lib/govuk-frontend/govuk/all.js",
          "javascripts/jquery.min.js",
          "javascripts/autocompletePolyfill.js",
          "javascripts/libraries/location-autocomplete.min.js",
          "javascripts/app.js"
        ))
    ),
    pipelineStages := Seq(digest),
    // below line required to force asset pipeline to operate in dev rather than only prod
    Assets / pipelineStages := Seq(concat)
  )

lazy val testSettings: Seq[Def.Setting[_]] = Seq(
  fork := false
)

lazy val itSettings = Defaults.itSettings ++ Seq(
  unmanagedSourceDirectories := Seq(
    baseDirectory.value / "it"
  ),
  unmanagedResourceDirectories := Seq(
    baseDirectory.value / "it" / "resources"
  ),
  parallelExecution := false,
  fork              := true
)
