package org.http4s.server

import _root_.play.api.mvc.{RequestHeader, ResponseHeader}
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import cats.effect.{Async, ConcurrentEffect, IO}
import cats.syntax.all._
import fs2.interop.reactivestreams._
import fs2.{Chunk, Stream}
import org.http4s._
import org.http4s.util.CaseInsensitiveString

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.language.higherKinds

package object play {
  private[play] def effectToFuture[F[_], T](eff: F[T])
                                           (implicit
                                            executionContext: ExecutionContext,
                                            F: ConcurrentEffect[F]): Future[T] = {
    val promise = Promise[T]

    F.runAsync(Async.shift(executionContext) *> eff) {
      case Left(bad) =>
        IO(promise.failure(bad))
      case Right(good) =>
        IO(promise.success(good))
    }
      .unsafeRunSync()

    promise.future
  }


  private[play] def byteStreamSource[F[_] : ConcurrentEffect](stream: Stream[F, Byte]): Source[ByteString, _] =
    Source.lazySource { () =>
      Source.fromPublisher(stream.chunks.map(chunk => ByteString(chunk.toArray)).toUnicastPublisher)
    }

  private[play] def byteStreamSink[F[_] : ConcurrentEffect, E](f: Stream[F, Byte] => E): Sink[ByteString, E] =
    Sink.asPublisher[ByteString](fanout = false).mapMaterializedValue { publisher =>
      val stream = publisher.toStream.flatMap(bs => Stream.chunk(Chunk.bytes(bs.toArray)))
      f(stream)
    }


  def requestHeaderToRequest[F[_]](requestHeader: RequestHeader, method: Method): Request[F] =
    Request(
      method = method,
      uri = Uri(path = requestHeader.uri),
      headers = Headers.apply(requestHeader.headers.toMap.toList.flatMap {
        case (headerName, values) =>
          values.map { value =>
            Header(headerName, value)
          }
      }),
      body = EmptyBody
    )


  private val AkkaHttpSetsSeparately: Set[CaseInsensitiveString] =
    Set("Content-Type", "Content-Length", "Transfer-Encoding").map(CaseInsensitiveString.apply)

  def responseToResponseHeader[F[_]](response: Response[F]): ResponseHeader =
    ResponseHeader(
      status = response.status.code,
      headers = response.headers.toList.collect {
        case header if !AkkaHttpSetsSeparately.contains(header.name) =>
          header.parsed.name.value -> header.parsed.value
      }.toMap
    )
}
