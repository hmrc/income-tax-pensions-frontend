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

import sbt._

object AppDependencies {

  private val bootstrapVersion = "8.4.0"
  private val hmrcMongoVersion = "1.7.0"

  val compile: Seq[ModuleID] = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"                               %% "play-frontend-hmrc-play-30"                     % "8.5.0",
    "uk.gov.hmrc"                               %% "play-conditional-form-mapping-play-30"  % "2.0.0",
    "uk.gov.hmrc"                               %% "bootstrap-frontend-play-30"                      % bootstrapVersion,
    "uk.gov.hmrc.mongo"                  %% "hmrc-mongo-play-30"                                 % hmrcMongoVersion,
    "org.typelevel"                              %% "cats-core"                                                    % "2.9.0",
    "com.fasterxml.jackson.module" %% "jackson-module-scala"                               % "2.14.2",
    "org.codehaus.janino"                  % "janino"                                                             % "3.1.11" // it's required by logback for conditional logging
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-30"  % bootstrapVersion   % Test,
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-30" % hmrcMongoVersion   % Test,
    "org.scalatest"          %% "scalatest"               % "3.2.15"           % Test,
    "org.jsoup"               % "jsoup"                   % "1.17.2"           % Test,
    "com.vladsch.flexmark"    % "flexmark-all"            % "0.64.8"           % "test, it",
    "org.playframework"      %% "play-test"               % "3.0.1"            % Test,
    "org.scalatestplus.play" %% "scalatestplus-play"      % "7.0.1"            % "test, it",
    "com.github.tomakehurst"  % "wiremock-jre8-standalone"           % "3.0.1"           % Test pomOnly(),
    "org.scalamock"          %% "scalamock"               % "5.2.0"            % Test
  )
}
