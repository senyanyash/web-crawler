ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.16"

lazy val root = (project in file("."))
  .settings(
    name := "web-crawler"
  )

libraryDependencies ++= Seq(
  "dev.zio" %% "zio-http" % "3.0.1",
  "com.typesafe" % "config" % "1.4.3",
  "org.jsoup" % "jsoup" % "1.18.3"
)
