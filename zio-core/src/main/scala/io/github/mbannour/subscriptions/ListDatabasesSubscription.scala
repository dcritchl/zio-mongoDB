package io.github.mbannour.subscriptions

import com.mongodb.reactivestreams.client.ListDatabasesPublisher
import org.bson.conversions.Bson
import zio._

import java.lang
import java.util.concurrent.TimeUnit
import scala.collection.mutable.ArrayBuffer

case class ListDatabasesSubscription[T](p: ListDatabasesPublisher[T]) extends Subscription[Iterable[T]] {

  override def fetch[_]: IO[Throwable, Iterable[T]] =
    ZIO.async[Any, Throwable, Iterable[T]] { callback =>
      p.subscribe {
        new JavaSubscriber[T] {

          val items = new ArrayBuffer[T]()

          override def onSubscribe(s: JavaSubscription): Unit = s.request(Long.MaxValue)

          override def onNext(t: T): Unit = items += t

          override def onError(t: Throwable): Unit = callback(ZIO.fail(t))

          override def onComplete(): Unit = callback(ZIO.succeed(items.toSeq))
        }
      }
    }

  def maxTime(maxTime: Long, timeUnit: TimeUnit): ListDatabasesSubscription[T] = copy(p.maxTime(maxTime, timeUnit))

  def filter(filter: Bson): ListDatabasesSubscription[T] = copy(p.filter(filter))

  def nameOnly(nameOnly: lang.Boolean): ListDatabasesSubscription[T] = copy(p.nameOnly(nameOnly))

  def authorizedDatabasesOnly(authorizedDatabasesOnly: lang.Boolean): ListDatabasesSubscription[T] =
    copy(p.authorizedDatabasesOnly(authorizedDatabasesOnly))

  def batchSize(batchSize: Int): ListDatabasesSubscription[T] = copy(p.batchSize(batchSize))

  def first(): SingleItemSubscription[T] = SingleItemSubscription(p.first())

}
