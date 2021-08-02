package org.http4s.server

import _root_.play.api.mvc.{RequestHeader, ResponseHeader}
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import cats.effect.Async
import cats.effect.std.Dispatcher
import fs2.interop.reactivestreams._
import fs2.{Chunk, Stream}
import org.http4s._
import org.typelevel.ci.CIString

package object play {
  private[play] def byteStreamSource[F[_] : Async](stream: Stream[F, Byte])
                                                  (implicit dispatcher: Dispatcher[F]): Source[ByteString, _] =
    Source.lazySource { () =>
      Source.fromPublisher( // TODO: allocated
        dispatcher.unsafeRunSync(stream.chunks.map(chunk => ByteString(chunk.toArray)).toUnicastPublisher.allocated)._1
      )
    }

  private[play] def byteStreamSink[F[_] : Async, E](f: Stream[F, Byte] => E): Sink[ByteString, E] =
    Sink.asPublisher[ByteString](fanout = false).mapMaterializedValue { publisher =>
      val stream = publisher.toStream.flatMap(bs => Stream.chunk(Chunk.array(bs.toArray)))
      f(stream)
    }


  def requestHeaderToRequest[F[_]](requestHeader: RequestHeader, method: Method): Request[F] =
    Request(
      method = method,
      uri = Uri.unsafeFromString(requestHeader.uri),
      headers = Headers.apply(requestHeader.headers.toMap.toList.flatMap {
        case (headerName, values) =>
          values.map { value =>
            Header.Raw(CIString(headerName), value)
          }
      }),
      body = EmptyBody
    )


  private val AkkaHttpSetsSeparately: Set[CIString] =
    Set("Content-Type", "Content-Length", "Transfer-Encoding").map(CIString(_))

  def responseToResponseHeader[F[_]](response: Response[F]): ResponseHeader =
    ResponseHeader(
      status = response.status.code,
      headers = response.headers.headers.collect {
        case header if !AkkaHttpSetsSeparately.contains(header.name) =>
          header.name.toString -> header.value
      }.toMap
    )
}
