package cirrus.clients

import cirrus.internal.Headers._
import cirrus.internal.{Client, HTTPVerb, Response}
import spray.json._

import scala.concurrent.Future

object SprayHTTP {

  case class GET[T](address: String)(implicit val reader: JsonReader[T], val client: Client)
    extends EmptySprayVerb[T]

  case class PUT[T](address: String)(implicit val reader: JsonReader[T], val client: Client)
    extends LoadedSprayVerb[T]

  case class POST[T](address: String)(implicit val reader: JsonReader[T], val client: Client)
    extends LoadedSprayVerb[T]

  case class DELETE[T](address: String)(implicit val reader: JsonReader[T], val client: Client)
    extends EmptySprayVerb[T]


  trait EmptySprayVerb[T] extends HTTPVerb {

    implicit val reader: JsonReader[T]

    def send: Future[Response] = {

      implicit val ec = client.ec

      val verb = if (method == "GET") BasicHTTP GET address else BasicHTTP DELETE address

      verb withHeaders this.headers
      verb withHeader `Accept` -> `application/json`
      verb.send map ResponseBuilder.asSpray[T]
    }
  }


  trait LoadedSprayVerb[T] extends HTTPVerb {

    implicit val reader: JsonReader[T]

    def send[F: JsonWriter](payload: F) = {

      implicit val ec = client.ec

      val content = implicitly[JsonWriter[F]].write(payload).compactPrint

      val verb = if (method == "POST") BasicHTTP POST address else BasicHTTP PUT address

      verb withHeaders this.headers
      verb withHeader `Content-Type` -> `application/json` withHeader `Accept` -> `application/json`
      verb send content map ResponseBuilder.asSpray[T]
    }
  }
}
