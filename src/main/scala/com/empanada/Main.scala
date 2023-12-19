package com.empanada

import cats.effect._
import com.empanada.Config.{BotKey, OpenRouterApiKey, OpenRouterUri, config}
import org.http4s.blaze.client.BlazeClientBuilder
import telegramium.bots.high._

object Main extends IOApp {
  def run(args: List[String]): IO[ExitCode] = {
    val whitelist = List("1359866750")
    config
      .load[IO]
      .flatMap { cfg =>
        val botKey = cfg.botKey.value
        val openRouterApiKey = cfg.openRouterConfig.apikey.value
        val openRouteUrl = cfg.openRouterConfig.uri
        longPollingBot(botKey, openRouterApiKey, openRouteUrl, whitelist)
      }
      .as(ExitCode.Success)
  }

  private def longPollingBot(
      botKey: BotKey,
      openRouterApiKey: OpenRouterApiKey,
      openRouterUrl: OpenRouterUri,
      whitelistId: List[String]
  ): IO[Unit] = {
    BlazeClientBuilder[IO].resource.use { httpClient =>
      implicit val api: Api[IO] =
        BotApi(httpClient, s"https://api.telegram.org/bot${botKey.value}")
      implicit val llm: Llm[IO] =
        new Mixtral8x7B[IO](httpClient, openRouterApiKey.value, openRouterUrl.value)

      for {
        bot <- TelegramBot.make[IO](whitelistId)
        _ <- bot.start()
      } yield ()
    }
  }
}
