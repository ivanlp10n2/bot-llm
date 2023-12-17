ThisBuild / organization := "com.example"
ThisBuild / scalaVersion := "2.13.5"

val streams = List(
  "co.fs2" %% "fs2-core" % "3.9.2",
  "co.fs2" %% "fs2-io" % "3.9.2"
)
val betterMonadicPlugin = compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")

val telegramBots ={
  val telegram4sVersion = "5.7.1"
  List(
    "com.bot4s" %% "telegram-core", // Core with minimal dependencies, enough to spawn your first bot.
    "com.bot4s" %% "telegram-akka" // Extra goodies: Webhooks, support for games, bindings for actors.
  ).map(_ % telegram4sVersion)
}

val htt4s = {
  val http4sVersion = "0.23.24"
  List(
    "org.http4s" %% "http4s-ember-client",
    "org.http4s" %% "http4s-ember-server",
    "org.http4s" %% "http4s-dsl",
  ).map(_ % http4sVersion)
}

val tests= List(
  "org.scalatestplus" %% "mockito-4-11" % "3.2.17.0",
  "org.scalactic" %% "scalactic" % "3.2.17",
  "org.scalatest" %% "scalatest" % "3.2.17"
).map(_ % Test)

lazy val root = (project in file(".")).settings(
  name := "llm-api",
  libraryDependencies ++=  betterMonadicPlugin :: streams ::: htt4s ::: telegramBots ::: tests
)
