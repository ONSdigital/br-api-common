import Dependencies._
import DependencyOverrides._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "uk.gov.ons",
      scalaVersion := "2.12.7",
      version      := "0.1.0-SNAPSHOT"
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
      seleniumRemoteDriver
    )
  )
