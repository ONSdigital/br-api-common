import Dependencies._
import DependencyOverrides._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "uk.gov.ons",
      scalaVersion := "2.12.7",
      licenses := Seq("MIT" -> url("http://opensource.org/licenses/MIT")),
      homepage := Some(url("https://github.com/ONSdigital/br-api-common")),
      scmInfo := Some(ScmInfo(url("https://github.com/ONSdigital/br-api-common"), "scm:git:git@github.com:ONSDigital/br-api-common.git")),
      developers := List(Developer("awharris", "Adrian Harris", "adrian.harris@ons.gov.uk", url("https://github.com/awharris")), Developer("nigelhp", "Nigel Perkins", "nigel.perkins@ext.ons.gov.uk", url("https://github.com/nigelhp"))),
      // These are the sbt-release-early settings to configure
      pgpPublicRing := file("./travis/local.pubring.asc"),
      pgpSecretRing := file("./travis/local.secring.asc"),
      releaseEarlyWith := BintrayPublisher,
      releaseEarlyEnableSyncToMaven := false,
      bintrayOrganization := Some("ons"),
    )),
    name := "br-api-common",
    resolvers += Resolver.bintrayRepo("ons", "ONS-Registers"),
    scalacOptions ++= Seq(
      "-target:jvm-1.8",
      "-encoding", "utf8",
      "-deprecation",
      "-feature",
      "-unchecked",
      "-Xcheckinit",
      "-Xlint:_",
      "-Xfatal-warnings",
      "-Yno-adapted-args",
      "-Ywarn-dead-code",
      "-Ywarn-value-discard",
      "-Ywarn-unused"
    ),
    scapegoatVersion in ThisBuild := "1.3.8",
    conflictManager := ConflictManager.strict,
    libraryDependencies ++= Seq(
      compilerPlugin(silencerPlugin),
      playWs,
      silencerLib % Provided,
      slf4jApi,
      
      // test dependencies
      registersApiTest % Test,
      scalaMock % Test,
      scalaTest % Test
    ),
    dependencyOverrides ++= Seq(
      commonsLang,
      findBugs,
      jacksonDatabind,
      jettyHttp,
      jettyIo,
      jettyUtil,
      scalaParserCombinators,
      scalaXml,
      seleniumRemoteDriver,
      guava
    )
  )

lazy val publishSettings = Seq(
  resolvers += Resolver.jcenterRepo,
  resolvers += Resolver.bintrayRepo("ons", "ONS-Registers"),
  updateOptions := updateOptions.value.withCachedResolution(true),
  // Bintray settings -- These ones have to be redefined in the projects
  bintrayRepository := "ONS-Registers",
  bintrayPackageLabels := Seq("scala", "sbt")
)