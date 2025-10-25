organization := "com.github.pjfanning"
name := "pekko-stream-contrib"

crossScalaVersions := Seq("2.13.16", "2.12.20")
scalaVersion := crossScalaVersions.value.head

val PekkoVersion = "1.0.2"

libraryDependencies ++= Seq(
  "org.apache.pekko" %% "pekko-stream" % PekkoVersion,
  "org.apache.pekko" %% "pekko-stream-testkit" % PekkoVersion % Test,
  "junit" % "junit" % "4.13.2" % Test, // Common Public License 1.0
  "com.novocode" % "junit-interface" % "0.11" % Test, // BSD-like
  "com.google.jimfs" % "jimfs" % "1.3.1" % Test, // ApacheV2
  "org.scalatest" %% "scalatest" % "3.2.19" % Test, // ApacheV2
  "org.scalamock" %% "scalamock" % "4.4.0" % Test, // ApacheV2
  "com.github.pjfanning" %% "pekko-mock-scheduler" % "0.6.0" % Test // ApacheV2
)

homepage := Some(url("https://github.com/pjfanning/pekko-stream-contrib"))
licenses := Seq(("Apache License, Version 2.0", url("http://www.apache.org/licenses/LICENSE-2.0")))
scmInfo := Some(
  ScmInfo(url("https://github.com/pjfanning/pekko-stream-contrib"),
    "git@github.com:pjfanning/pekko-stream-contrib.git"))
developers += Developer("contributors",
  "Contributors",
  "",
  url("https://github.com/pjfanning/pekko-stream-contrib/graphs/contributors"))

scalacOptions ++=
  Seq("-encoding", "UTF-8", "-feature", "-unchecked", "-deprecation", "-Xlint") ++ (
    if (scalaVersion.value.startsWith("2.13."))
      Seq(
        "-Wdead-code",
        "-Wnumeric-widen",
        "-Xsource:2.14")
    else
      Seq(
        // "-Xfatal-warnings",
        "-Xlint",
        "-Yno-adapted-args",
        "-Ywarn-dead-code",
        "-Ywarn-numeric-widen",
        "-Xfuture")
  )

// By default scalatest futures time out in 150 ms, dilate that to 600ms.
// This should not impact the total test time as we don't expect to hit this
// timeout, and indeed it doesn't appear to.
Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-F", "4")

// show full stack traces and test case durations
Test / testOptions += Tests.Argument("-oDF")

// -v Log "test run started" / "test started" / "test run finished" events on log level "info" instead of "debug".
// -a Show stack traces and exception class name for AssertionErrors.
testOptions += Tests.Argument(TestFrameworks.JUnit, "-v", "-a")

enablePlugins(AutomateHeaderPlugin)
headerLicense := Some(HeaderLicense.Custom(s"Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>"))
scalafmtOnCompile := true
