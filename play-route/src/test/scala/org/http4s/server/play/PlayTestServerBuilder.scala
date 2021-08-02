package org.http4s.server.play

import cats.effect._
import cats.effect.std.Dispatcher
import org.http4s.HttpRoutes
import org.http4s.server.defaults.IPv4SocketAddress
import org.http4s.server.{Server, ServerBuilder, ServiceErrorHandler}
import play.api.{Configuration, Environment, Mode}

import java.net.InetSocketAddress
import scala.collection.immutable
import scala.concurrent.ExecutionContext

class PlayTestServerBuilder[F[_]](hostname: String,
                                  services: Vector[(HttpRoutes[F], String)],
                                  executionContext: ExecutionContext,
                                  port: Int)
                                 (implicit val F: Async[F])
  extends ServerBuilder[F] {
  type Self = PlayTestServerBuilder[F]

  private implicit val ec: ExecutionContext = executionContext

  private def copy(hostname: String = hostname,
                   port: Int = port,
                   executionContext: ExecutionContext = executionContext,
                   services: Vector[(HttpRoutes[F], String)] = services): Self =
    new PlayTestServerBuilder(hostname, services, executionContext, port)

  override def resource: Resource[F, Server] =
    Dispatcher[F].flatMap { implicit dispatcher =>
      Resource.make {
        F.delay {
          val serverA = {
            import play.core.server.{AkkaHttpServer, _}
            val serverConfig =
              ServerConfig(
                port = Some(port),
                address = hostname
              ).copy(configuration = Configuration.load(Environment.simple()), mode = Mode.Test)
            AkkaHttpServer.fromRouterWithComponents(serverConfig) { _ =>
              services
                .map {
                  case (routes, prefix) =>
                    PlayRouteBuilder
                      .withPrefix(prefix, PlayRouteBuilder(routes).build)
                }
                .foldLeft(PartialFunction.empty: _root_.play.api.routing.Router.Routes)(_.orElse(_))
            }
          }

          serverA
        }
      } { serverA =>
        F.delay {
          serverA.stop()
        }
      }.flatMap { _ =>
        Resource.eval(F.delay {
          new Server {
            override def toString: String = s"PlayServer($address)"

            override def address: InetSocketAddress = new InetSocketAddress(hostname, port)

            override def isSecure: Boolean = false
          }
        })
      }
    }

  override def bindSocketAddress(socketAddress: InetSocketAddress): PlayTestServerBuilder[F] = this

  override def withServiceErrorHandler(serviceErrorHandler: ServiceErrorHandler[F]): PlayTestServerBuilder[F] = this

  override def withBanner(banner: immutable.Seq[String]): PlayTestServerBuilder[F] = this
}

object PlayTestServerBuilder {
  def apply[F[_]](implicit F: Async[F]): PlayTestServerBuilder[F] =
    new PlayTestServerBuilder(
      hostname = IPv4SocketAddress.getHostString,
      services = Vector.empty,
      port = org.http4s.server.defaults.HttpPort,
      executionContext = ExecutionContext.global
    )
}
