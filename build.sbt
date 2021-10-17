ThisBuild / versionScheme := Some("early-semver")

lazy val commonSettings: Seq[Setting[_]] = Seq(
  organization := "de.lolhens",
  version := "0.0.1-SNAPSHOT",

  scalaVersion := "2.13.6",
  crossScalaVersions := Seq("2.12.15", scalaVersion.value),

  licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0")),

  homepage := Some(url("https://github.com/LolHens/http4s-play")),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/LolHens/http4s-play"),
      "scm:git@github.com:LolHens/http4s-play.git"
    )
  ),
  developers := List(
    Developer(id = "LolHens", name = "Pierre Kisters", email = "pierrekisters@gmail.com", url = url("https://github.com/LolHens/"))
  ),

  Compile / doc / sources := Seq.empty,

  version := {
    val tagPrefix = "refs/tags/"
    sys.env.get("CI_VERSION").filter(_.startsWith(tagPrefix)).map(_.drop(tagPrefix.length)).getOrElse(version.value)
  },

  publishMavenStyle := true,

  publishTo := sonatypePublishToBundle.value,

  credentials ++= (for {
    username <- sys.env.get("SONATYPE_USERNAME")
    password <- sys.env.get("SONATYPE_PASSWORD")
  } yield Credentials(
    "Sonatype Nexus Repository Manager",
    "oss.sonatype.org",
    username,
    password
  )).toList
)

lazy val root = project.in(file("."))
  .settings(commonSettings)
  .settings(
    publish / skip := true
  )
  .aggregate(
    `play-route`,
    `examples-play`
  )

lazy val `play-route` = project
  .settings(commonSettings)
  .settings(
    name := "http4s-play",
    description := "Play wrapper of http4s services",

    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play" % "2.8.8",
      "com.typesafe.play" %% "play-akka-http-server" % "2.8.8" % "test",
      "co.fs2" %% "fs2-core" % "3.1.6",
      "co.fs2" %% "fs2-reactive-streams" % "3.1.6",
      "org.http4s" %% "http4s-core" % "0.23.6",
      "org.http4s" %% "http4s-server" % "0.23.6" % Test,
    )
  )

lazy val `examples-play` = project.in(file("example"))
  .enablePlugins(PlayScala)
  .dependsOn(`play-route`)
  .settings(commonSettings)
  .settings(
    publish / skip := true,

    description := "Example of http4s on Play",

    scalacOptions in Compile -= "-Xfatal-warnings",
    libraryDependencies ++= Seq(
      guice,
      "javax.xml.bind" % "jaxb-api" % "2.3.1",
      "org.http4s" %% "http4s-dsl" % "0.23.6",
    )
  )
