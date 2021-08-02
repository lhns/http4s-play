package org.http4s.server.play

import cats.effect.IO

class PlayServerSpec /*extends ServerSpec*/ {
  def builder: PlayTestServerBuilder[IO] = PlayTestServerBuilder[IO]
}
