/*
 * Copyright 2025 HM Revenue & Customs
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

import sbt.*

object AppDependencies {

  private val bootstrapVersion = "10.3.0"
  private val hmrcMongoVersion = "2.10.0"

  val compile: Seq[ModuleID] = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"                  %% "play-frontend-hmrc-play-30"            % "12.17.0",
    "uk.gov.hmrc"                  %% "play-conditional-form-mapping-play-30" % "3.3.0",
    "uk.gov.hmrc"                  %% "bootstrap-frontend-play-30"            % bootstrapVersion,
    "uk.gov.hmrc.mongo"            %% "hmrc-mongo-play-30"                    % hmrcMongoVersion,
    "org.typelevel"                %% "cats-core"                             % "2.13.0",
    "com.fasterxml.jackson.module" %% "jackson-module-scala"                  % "2.20.0",
    "org.codehaus.janino"           % "janino"                                % "3.1.12" // it's required by logback for conditional logging
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-30"  % bootstrapVersion,
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-30" % hmrcMongoVersion,
    "org.scalacheck"         %% "scalacheck"              % "1.19.0",
    "org.scalatestplus"      %% "scalacheck-1-18"         % "3.2.19.0",
    "org.mockito"            %% "mockito-scala-scalatest" % "2.0.0",
    "org.jsoup"               % "jsoup"                   % "1.21.2",
    "com.vladsch.flexmark"    % "flexmark-all"            % "0.64.8",
    "org.playframework"      %% "play-test"               % "3.0.9",
    "org.scalatestplus.play" %% "scalatestplus-play"      % "7.0.2",
    "org.scalamock"          %% "scalamock"               % "7.5.0"
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test
}
