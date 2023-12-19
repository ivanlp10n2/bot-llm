ThisBuild / organization := "com.example"
ThisBuild / scalaVersion := "2.13.5"

val streams = List(
  "co.fs2" %% "fs2-core" % "3.9.2",
  "co.fs2" %% "fs2-io" % "3.9.2"
)
val betterMonadicPlugin = compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")

val telegramBots = {
  val telegram4sVersion = "8.69.0"
  List(
    "io.github.apimorphism" %% "telegramium-core",
    "io.github.apimorphism" %% "telegramium-high"
  ).map(_ % telegram4sVersion)
}

val htt4s = {
  val http4sVersion = "0.23.24"
  List(
    "org.http4s" %% "http4s-ember-client",
    "org.http4s" %% "http4s-ember-server",
    "org.http4s" %% "http4s-dsl"
  ).map(_ % http4sVersion)
}

val configs = {
  val cirisVersion = "3.5.0"
  List(
    "is.cir" %% "ciris",
    "is.cir" %% "ciris-circe",
    "is.cir" %% "ciris-enumeratum",
    "is.cir" %% "ciris-refined"
  ).map(_ % cirisVersion)
}

val tests = List(
  "org.scalatestplus" %% "mockito-4-11" % "3.2.17.0",
  "org.scalactic" %% "scalactic" % "3.2.17",
  "org.scalatest" %% "scalatest" % "3.2.17"
).map(_ % Test)

lazy val root = (project in file(".")).settings(
  name := "llm-api",
  libraryDependencies ++= betterMonadicPlugin :: configs ::: streams ::: htt4s ::: telegramBots ::: tests
)
Compile / run / fork := true
