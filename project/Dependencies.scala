import sbt._

object Dependencies {
  private lazy val silencerVersion = "1.3.1"
  
  lazy val playWs = "com.typesafe.play" %% "play-ws" % "2.6.20"
  lazy val registersApiTest =  "uk.gov.ons" % "br-api-test-common_2.12" % "1.1"
  lazy val scalaMock = "org.scalamock" %% "scalamock" % "4.1.0"
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.4"
  lazy val silencerLib = "com.github.ghik" %% "silencer-lib" % silencerVersion
  lazy val silencerPlugin = "com.github.ghik" %% "silencer-plugin" % silencerVersion
  lazy val slf4jApi = "org.slf4j" % "slf4j-api" % "1.7.25"
}

/*
 * For explicit resolution of transitive dependency conflicts.
 * Favour versions required by Play where possible
 */
object DependencyOverrides {
  lazy val commonsLang = "org.apache.commons" % "commons-lang3" % "3.6"
  lazy val findBugs = "com.google.code.findbugs" % "jsr305" % "3.0.2"
  lazy val jacksonDatabind = "com.fasterxml.jackson.core" % "jackson-databind" % "2.8.11.2"
  lazy val scalaParserCombinators = "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.6"
  lazy val scalaXml = "org.scala-lang.modules" %% "scala-xml" % "1.0.6"
  lazy val seleniumRemoteDriver = "org.seleniumhq.selenium" % "selenium-remote-driver" % "3.5.3"
  lazy val guava = "com.google.guava" % "guava" % "22.0"

  // we are not using selenium - favour the jetty components needed by wiremock
  lazy val jettyHttp = "org.eclipse.jetty" % "jetty-http" % "9.2.24.v20180105"
  lazy val jettyIo = "org.eclipse.jetty" % "jetty-io" % "9.2.24.v20180105"
  lazy val jettyUtil = "org.eclipse.jetty" % "jetty-util" % "9.2.24.v20180105"
}