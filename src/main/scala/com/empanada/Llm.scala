package com.empanada

import cats.effect.Async
import cats.syntax.all._
import com.empanada.Mixtral8x7B.{ApiResponse, MixtralRequest}
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, Json}
import iozhik.DecodingError
import org.http4s.Method.POST
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.jsonOf
import org.http4s.client.Client
import org.http4s.{Header, Request, Uri}
import org.typelevel.ci.CIString
import telegramium.bots.high.ResponseDecodingError

trait Llm[F[_]] {
  def ask(message: String): F[String]
}
class Mixtral8x7B[F[_]](
    http: Client[F],
    apiUrl: String,
    token: String
)(implicit F: Async[F])
    extends Llm[F] {
  import Mixtral8x7B.Codecs._

  override def ask(message: String): F[String] = {
    for {
      uri <- F.fromEither[Uri](Uri.fromString(s"$apiUrl"))
      req <- mkRequest(uri, message)
      res <- handleResponse[ApiResponse](req)
      content = res.choices.head.message.content
    } yield content
  }

  private def mkRequest(uri: Uri, message: String) =
    Request[F]()
      .withHeaders(
        Header.Raw(CIString("Authorization"), s"Bearer $token")
      )
      .withMethod(POST)
      .withEntity(MixtralRequest(message))
      .withUri(uri)
      .pure[F]

  private def handleResponse[A: io.circe.Decoder](req: Request[F]): F[A] =
    for {
      response <- http
        .fetchAs(req)(jsonOf[F, A])
        .adaptError { case e @ DecodingError(message) =>
          ResponseDecodingError.default(message, e.some)
        }
      _ = println(s"Result of request [$response]")
    } yield response

}

object Mixtral8x7B {
  val ModelName = "mistralai/mixtral-8x7b-instruct"

  case class Message(role: String, content: String)

  case class ApiResponse(
      id: String,
      model: String,
      created: Long,
      objectType: String,
      choices: List[Choice]
  )
  case class Choice(message: Message)
  case class MixtralRequest(content: String)

  object Codecs {
    implicit val encoderMessage: Encoder[Message] = Encoder.instance { m =>
      Json.obj(
        ("role" -> m.role.asJson),
        ("content" -> m.content.asJson)
      )
    }
    implicit val encoderRequest: Encoder[MixtralRequest] = Encoder.instance { j =>
      Json.obj(
        ("model", ModelName.asJson),
        ("messages" -> List(Message("user", j.content)).asJson)
      )
    }
    implicit val responseDecoder: Decoder[ApiResponse] = {

      implicit val message: Decoder[Message] = Decoder.instance { h =>
        for {
          _content <- h.get[String]("content")
        } yield Message("user", _content)
      }

      implicit val choiceDecoder: Decoder[Choice] = Decoder.instance { j =>
        j.get[Message]("message").map(msgs => Choice(msgs))
      }

      Decoder.instance { h =>
        for {
          _id <- h.get[String]("id")
          _model <- h.get[String]("model")
          _created <- h.get[Long]("created")
          _object <- h.get[Option[String]]("object")
          _message <- h.get[List[Choice]]("choices")
        } yield {
          ApiResponse(
            _id,
            _model,
            _created,
            _object.getOrElse(""),
            _message
          )
        }
      }
    }

  }

}
