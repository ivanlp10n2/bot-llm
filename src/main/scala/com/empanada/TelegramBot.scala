package com.empanada
import cats.Parallel
import cats.effect._
import cats.syntax.all._
import telegramium.bots.high.implicits._
import telegramium.bots.high.{Api => TelegramApi, _}
import telegramium.bots.{Audio, ChatIntId, Message}

class TelegramBot[F[_]: Async: Parallel] private (
    whiteListUserIds: List[String],
    llm: LlmApi[F]
)(implicit
    api: TelegramApi[F]
) extends LongPollBot[F](api) {

  override def onMessage(msg: Message): F[Unit] = {
    implicit val msgId = ChatIntId(msg.chat.id)
    println("Received msg from id: " + msg.chat.id)
    if (whiteListUserIds.contains(msgId.id.toString))
      (msg.text, msg.audio) match {
        case (Some(text), Some(_)) => handleText(text).void
        case (Some(text), None)    => handleText(text).void
        case (None, Some(audio))   => handeAudio(audio)
        case (None, None)          => ().pure[F]
      }
    else {
      println(s"not contained in whitelist: [$msg]")
      ().pure[F]
    }
  }

  private def handleText(msg: String)(implicit id: ChatIntId): F[Message] =
    llm
      .ask(msg)
      .flatMap { llmResponse =>
        Methods.sendMessage(id, llmResponse).exec
      }
      .handleErrorWith(e =>
        Methods.sendMessage(id, s"Something unexpected happened: ${e.getMessage}").exec
      )

  private def handeAudio(
      audio: Audio
  )(implicit chatId: ChatIntId): F[Unit] = {
    val transcribedText: String = decodeAudio(audio)
    llm
      .ask(transcribedText)
      .flatMap { llmResponse =>
        val formattedMsg: String = formatMsg(transcribedText, llmResponse)
        Methods.sendMessage(chatId = chatId, text = formattedMsg).exec
      }
      .void
  }

  private def formatMsg(
      transcribedText: String,
      llmResponse: String
  ): String =
    s"Transcribed text: $transcribedText\n\n" +
      s"LLM response: $llmResponse"

  private def decodeAudio(audio: Audio): String = {
//      val audioFileId = audio.fileId
//      val audioFile = Methods.getFile(audioFileId).exec
//      val audioFileUrl = audioFile.fileUrl
//      val audioFileContent = Methods.gdownloadFile(audioFileUrl).exec
//      val audioFileContentString = audioFileContent.decodeString("UTF-8")
//      audioFileContentString
    ???
  }
}

object TelegramBot {
  def make[F[_]: Async: Parallel](
      whiteListUserIds: List[String],
      api: TelegramApi[F],
      llm: LlmApi[F]
  ): F[TelegramBot[F]] = {
    implicit val apiImpl = api
    Sync[F].delay(new TelegramBot[F](whiteListUserIds, llm))
  }
}
