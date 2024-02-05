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

import play.core.PlayVersion.current
import sbt._

object AppDependencies {

  private val bootstrapPlay28Version = "8.4.0"
  private val hmrcMongoPlayVersion   = "1.2.0"
  private val hmrcPlayVersion        = "7.29.0-play-28"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                  %% "bootstrap-frontend-play-28" % bootstrapPlay28Version,
    "uk.gov.hmrc"                  %% "play-frontend-hmrc"         % hmrcPlayVersion,
    "uk.gov.hmrc.mongo"            %% "hmrc-mongo-play-28"         % hmrcMongoPlayVersion,
    "org.typelevel"                %% "cats-core"                  % "2.9.0",
    "com.fasterxml.jackson.module" %% "jackson-module-scala"       % "2.14.2",
    "org.codehaus.janino"           % "janino"                     % "3.1.11" // it's required by logback for conditional logging
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-28"  % bootstrapPlay28Version % Test,
    "org.scalatest"          %% "scalatest"               % "3.2.15"               % Test,
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-28" % hmrcMongoPlayVersion   % Test,
    "org.jsoup"               % "jsoup"                   % "1.15.3"               % Test,
    "com.vladsch.flexmark"    % "flexmark-all"            % "0.62.2"               % "test, it",
    "com.typesafe.play"      %% "play-test"               % current                % Test,
    "org.scalatestplus.play" %% "scalatestplus-play"      % "5.1.0"                % "test, it",
    "com.github.tomakehurst"  % "wiremock-jre8"           % "2.35.0"               % "test, it",
    "org.scalamock"          %% "scalamock"               % "5.2.0"                % Test
  )
  // note com.vladsch.flexmark"    %  "flexmark-all" cannot be upgraded above 0.62.2 until we use Java 11 and above
}
