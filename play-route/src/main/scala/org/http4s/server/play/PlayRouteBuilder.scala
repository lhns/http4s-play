package org.http4s.server.play

import cats.data.OptionT
import cats.effect.Async
import cats.effect.std.Dispatcher
import cats.syntax.all._
import org.http4s.server.play.PlayRouteBuilder.PlayRouting
import org.http4s.{HttpApp, HttpRoutes, Method, Response}
import play.api.mvc._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

trait PlayRouteBuilder[F[_]] {
  protected def routeMatches(requestHeader: RequestHeader, method: Method): Boolean

  protected def handler: Handler

  def build: PlayRouting = new PartialFunction[RequestHeader, Handler]() {
    override def isDefinedAt(requestHeader: RequestHeader): Boolean =
      Method.fromString(requestHeader.method) match {
        case Right(method) =>
          routeMatches(requestHeader, method)

        case _ =>
          false
      }

    override def apply(requestHeader: RequestHeader): Handler = handler
  }
}

object PlayRouteBuilder {
  def apply[F[_] : Async](routes: HttpRoutes[F])
                         (implicit dispatcher: Dispatcher[F]): PlayRouteBuilder[F] =
    new HttpRoutesBuilder[F](routes)

  def fromHttpApp[F[_] : Async](httpApp: HttpApp[F])
                               (implicit dispatcher: Dispatcher[F]): PlayRouteBuilder[F] =
    new HttpAppBuilder[F](httpApp)


  private class HttpRoutesBuilder[F[_]](routes: HttpRoutes[F])
                                       (implicit
                                        F: Async[F],
                                        dispatcher: Dispatcher[F]) extends PlayRouteBuilder[F] {
    override protected def routeMatches(requestHeader: RequestHeader, method: Method): Boolean = {
      val matches: F[Boolean] = F.defer {
        val http4sRequest = requestHeaderToRequest[F](requestHeader, method)
        val optionalResponse: OptionT[F, Response[F]] = routes(http4sRequest)
        optionalResponse.value.map(_.isDefined)
      }

      Await.result(dispatcher.unsafeToFuture(matches), Duration.Inf)
    }

    override protected def handler: Handler = {
      /** The .get here is safe because this was already proven in the pattern match of the caller * */
      Http4sHandler(routes.mapF(_.value.map(_.get)))
    }
  }


  private class HttpAppBuilder[F[_]](httpApp: HttpApp[F])
                                    (implicit
                                     F: Async[F],
                                     dispatcher: Dispatcher[F]) extends PlayRouteBuilder[F] {
    override protected def routeMatches(requestHeader: RequestHeader, method: Method): Boolean = true

    override protected def handler: Handler = Http4sHandler(httpApp)
  }


  type PlayRouting = PartialFunction[RequestHeader, Handler]

  /** Borrowed from Play for now * */
  def withPrefix(prefix: String,
                 t: _root_.play.api.routing.Router.Routes): _root_.play.api.routing.Router.Routes =
    if (prefix == "/") {
      t
    } else {
      val p = if (prefix.endsWith("/")) prefix else prefix + "/"
      val prefixed: PartialFunction[RequestHeader, RequestHeader] = {
        case rh: RequestHeader if rh.path.startsWith(p) =>
          val newPath = rh.path.drop(p.length - 1)
          rh.withTarget(rh.target.withPath(newPath))
      }
      Function.unlift(prefixed.lift.andThen(_.flatMap(t.lift)))
    }
}
