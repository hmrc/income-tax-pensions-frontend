
resolvers += MavenRepository("HMRC-open-artefacts-maven2", "https://open.artefacts.tax.service.gov.uk/maven2")
resolvers += Resolver.url("HMRC-open-artefacts-ivy2", url("https://open.artefacts.tax.service.gov.uk/ivy2"))(Resolver.ivyStylePatterns)
resolvers += Resolver.typesafeRepo("releases")

ThisBuild / libraryDependencySchemes ++= Seq(
  "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
)


addSbtPlugin("uk.gov.hmrc"          % "sbt-auto-build"          % "3.20.0")
addSbtPlugin("uk.gov.hmrc"          % "sbt-distributables"      % "2.2.0")
addSbtPlugin("com.typesafe.play"    % "sbt-plugin"              % "2.8.21")
addSbtPlugin("com.typesafe.sbt"     % "sbt-gzip"                % "1.0.2")
addSbtPlugin("io.github.irundaia"   % "sbt-sassify"             % "1.5.2")
addSbtPlugin("org.scoverage"        % "sbt-scoverage"           % "2.0.6")
addSbtPlugin("org.scalastyle"       %% "scalastyle-sbt-plugin"  % "1.0.0")
addSbtPlugin("net.ground5hark.sbt"  % "sbt-concat"              % "0.2.0")
addSbtPlugin("com.typesafe.sbt"     % "sbt-uglify"              % "2.0.0")
addSbtPlugin("com.typesafe.sbt"     % "sbt-digest"              % "1.1.4")
