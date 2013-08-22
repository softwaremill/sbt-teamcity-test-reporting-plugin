name := "sbt-teamcity-test-reporting-plugin"

organization := "com.gu"

scalaVersion := "2.10.2"

sbtPlugin := true

releaseSettings

publishTo := Some(Resolver.url("scala-sbt-plugin-releases", new URL("http://repo.scala-sbt.org/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns))

publishMavenStyle := false

