package com.empanada

import cats.Show
import cats.syntax.all._
import ciris._
import ciris.refined._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.{MatchesRegex, Url}

object Config {
  type BotKey = String Refined MatchesRegex["[a-zA-Z0-9]{10}:[a-zA-Z0-9]{35}"]
  type OpenRouterApiKey = String Refined MatchesRegex[
    "[a-zA-Z0-9]{2}-[a-zA-Z0-9]{2}-[a-zA-Z0-9]{2}-[a-zA-Z0-9]{64}"
  ]
  type OpenRouterUri = String Refined Url
  implicit val botKeyShow: Show[BotKey] = Show.show(_.value)
  implicit val apikeyShow: Show[OpenRouterApiKey] = Show.show(_.value)
  implicit val uriShow: Show[OpenRouterUri] = Show.show(_.value)

  final case class Config(
      botKey: Secret[BotKey],
      openRouterConfig: OpenRouterConfig
  )

  final case class OpenRouterConfig(
      apikey: Secret[OpenRouterApiKey],
      uri: OpenRouterUri
  )

  val config = (
    env("BOT_KEY").as[BotKey].secret,
    (
      env("OPEN_ROUTER_TOKEN").as[OpenRouterApiKey].secret,
      env("OPEN_ROUTER_API")
        .default("https://openrouter.ai/api/v1/chat/completions")
        .as[OpenRouterUri]
    ).parMapN(OpenRouterConfig)
  ).parMapN(Config)

}
