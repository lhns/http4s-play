inThisBuild(Seq(
  version := "0.0.1",

  scalaVersion := "2.13.3"
))

lazy val `examples-play` = project.in(file("example"))
  .enablePlugins(PlayScala)
  .settings(
    description := "Example of http4s on Play",
    scalacOptions in Compile -= "-Xfatal-warnings",
    libraryDependencies ++= Seq(
      guice,
      "javax.xml.bind" % "jaxb-api" % "2.3.1",
      "org.http4s" %% "http4s-dsl" % "0.21.7",
    ),
  )
  .dependsOn(`play-route`)

lazy val `play-route` = project
  .settings(
    description := "Play wrapper of http4s services",
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play" % "2.8.2",
      "com.typesafe.play" %% "play-akka-http-server" % "2.8.2" % "test",
      "co.fs2" %% "fs2-core" % "2.4.4",
      "co.fs2" %% "fs2-reactive-streams" % "2.4.4",
      "org.http4s" %% "http4s-core" % "0.21.7",
      "org.http4s" %% "http4s-server" % "0.21.7" % "test",
      "org.http4s" %% "http4s-testing" % "0.21.7" % "test",
    )
  )
