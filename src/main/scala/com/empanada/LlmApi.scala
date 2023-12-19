package com.empanada

import cats.effect.Async
import cats.syntax.all._
import com.empanada.Llm.{ApiRequest, ApiResponse}
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, Json}
import iozhik.DecodingError
import org.http4s.Method.POST
import org.http4s.circe.jsonOf
import org.http4s.client.Client
import org.http4s.{EntityEncoder, Header, Request, Uri}
import org.typelevel.ci.CIString
import telegramium.bots.high.ResponseDecodingError
import org.http4s.circe.jsonEncoderOf

trait LlmApi[F[_]] {
  def ask(message: String): F[String]
}
class Llm[F[_]](
    model: String,
    http: Client[F],
    apiUrl: String,
    token: String
)(implicit F: Async[F])
    extends LlmApi[F] {

  override def ask(message: String): F[String] =
    for {
      uri <- F.fromEither[Uri](Uri.fromString(s"$apiUrl"))
      req <- mkRequest(uri, message)
      res <- handleResponse[ApiResponse](req)
      content = res.choices.head.message.content
    } yield content

  private def mkRequest(uri: Uri, message: String) =
    Request[F]()
      .withHeaders(
        Header.Raw(CIString("Authorization"), s"Bearer $token")
      )
      .withMethod(POST)
      .withEntity(ApiRequest(model, message))(ApiRequest.encoderEntity[F, ApiRequest])
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

object Llm {
  def makeMixtral8x7B[F[_]: Async](
      http: Client[F],
      apiUrl: String,
      token: String
  ): LlmApi[F] =
    new Llm[F]("mistralai/mixtral-8x7b-instruct", http, apiUrl, token)


  case class Message(role: String, content: String)

  case class ApiResponse(
      id: String,
      model: String,
      created: Long,
      objectType: String,
      choices: List[Choice]
  )
  case class Choice(message: Message)
  final case class ApiRequest(
      modelName: String,
      content: String
  )

  // codecs
  object ApiRequest {
    implicit val encoderRequest: Encoder[ApiRequest] = Encoder.instance { j =>
      Json.obj(
        ("model", j.modelName.asJson),
        ("messages" -> List(Message("user", j.content)).asJson)
      )
    }
    implicit def encoderEntity[F[_], A: Encoder]: EntityEncoder[F, ApiRequest] =
      jsonEncoderOf(encoderRequest)
  }
  object Message {
    implicit val encoderMessage: Encoder[Message] = Encoder.instance { m =>
      Json.obj(
        ("role" -> m.role.asJson),
        ("content" -> m.content.asJson)
      )
    }
    implicit val message: Decoder[Message] = Decoder.instance { h =>
      for {
        _content <- h.get[String]("content")
      } yield Message("user", _content)
    }

  }
  object ApiResponse {
    implicit val responseDecoder: Decoder[ApiResponse] = {
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
