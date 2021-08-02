package controllers

import cats.effect.IO
import cats.effect.std.Dispatcher
import cats.effect.unsafe.IORuntime.global
import org.http4s.HttpRoutes
import org.http4s.dsl.io._
import org.http4s.server.play.PlayRouteBuilder
import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class Http4sRouter @Inject()(implicit executionContext: ExecutionContext) extends SimpleRouter {
  val exampleRoutes: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "hello" =>
      Ok(s"Hello World!")
  }

  override def routes: Routes = {
    // TODO: allocated
    implicit val dispatcher: Dispatcher[IO] = Dispatcher[IO].allocated.unsafeRunSync()(global)._1
    PlayRouteBuilder[IO](exampleRoutes).build
  }
}
