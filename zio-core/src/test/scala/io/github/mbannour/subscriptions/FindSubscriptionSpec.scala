package io.github.mbannour.subscriptions

import io.github.mbannour.MongoTestClient.mongoTestClient
import org.bson.codecs.configuration.CodecRegistries.fromRegistries
import org.mongodb.scala.Document
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.model.{Filters, Projections, Sorts}
import zio._
import zio.test.Assertion.equalTo
import zio.test._

object FindSubscriptionSpec extends ZIOSpecDefault {

  val mongoClient = mongoTestClient()

  val codecRegistry = fromRegistries(DEFAULT_CODEC_REGISTRY)

  val database = mongoClient.getDatabase("mydb").map(_.withCodecRegistry(codecRegistry))

  val collection = database.flatMap(_.getCollection[Document]("test"))

  override def aspects =
    Chunk(TestAspect.executionStrategy(ExecutionStrategy.Sequential), TestAspect.timeout(Duration.fromMillis(30000)))

  def spec: Spec[TestEnvironment, Any] = suite("FindSubscriptionSpec")(
    findOptionalFirst(),
    insertDocuments(),
    findFirst(),
    findAndFilterDocuments(),
    findLimitedDocuments(),
    findSkipDocuments(),
    findProjectedDocuments(),
    findSortedDocuments(),
    close()
  )

  def insertDocuments() = {
    val documentsSize = for {
      col <- collection
      _ <- col.insertMany(
        Seq(
          Document("_id" -> 1, "content" -> "textual content1"),
          Document("_id" -> 2, "content" -> "textual content2"),
          Document("_id" -> 3, "content" -> "textual content3"),
          Document("_id" -> 5, "content" -> "textual content1"),
          Document("_id" -> 4, "content" -> "textual content2"),
          Document("_id" -> 6, "content" -> "textual content3")
        )
      )
      size <- col.countDocuments()
    } yield size

    test("insert documents") {
      assertZIO(documentsSize)(equalTo(6.toLong))
    }
  }

  def findOptionalFirst() = {
    val document = for {
      col  <- collection
      docs <- col.find().first().headOption
    } yield docs

    test("Find first returns nothing if there is No documents") {
      assertZIO(document)(equalTo(None))
    }
  }

  def findFirst() = {
    val document = for {
      col  <- collection
      docs <- col.find().first().fetch
    } yield docs

    test("Find first return a single document ") {
      assertZIO(document)(equalTo(Document("_id" -> 1, "content" -> "textual content1")))
    }
  }

  def findAndFilterDocuments() = {
    val allDocuments = for {
      col  <- collection
      docs <- col.find().filter(Filters.equal("_id", 5)).fetch
    } yield docs

    test("Find and filter documents return a single document") {
      assertZIO(allDocuments.map(_.toSeq))(
        equalTo(Seq(Document("_id" -> 5, "content" -> "textual content1")))
      )
    }
  }

  def findLimitedDocuments() = {
    val allDocuments = for {
      col  <- collection
      docs <- col.find().limit(2).fetch
    } yield docs

    test("Find the first two documents") {
      assertZIO(allDocuments.map(_.toSeq))(
        equalTo(
          Seq(
            Document("_id" -> 1, "content" -> "textual content1"),
            Document("_id" -> 2, "content" -> "textual content2")
          )
        )
      )
    }
  }

  def findSkipDocuments() = {
    val allDocuments = for {
      col  <- collection
      docs <- col.find().skip(2).fetch
    } yield docs

    test("Find and skip two documents") {
      assertZIO(allDocuments.map(_.size))(equalTo(4))
    }
  }

  def findProjectedDocuments() = {
    val allDocuments = for {
      col  <- collection
      docs <- col.find().limit(2).projection(Projections.include("_id")).fetch
    } yield docs

    test("Find and project documents by id") {
      assertZIO(allDocuments.map(_.toSeq))(equalTo(Seq(Document("_id" -> 1), Document("_id" -> 2))))
    }
  }

  def findSortedDocuments() = {
    val allDocuments = for {
      col  <- collection
      docs <- col.find().limit(2).sort(Sorts.descending("_id")).fetch
    } yield docs

    test("Find sorted documents") {
      assertZIO(allDocuments.map(_.toSeq))(
        equalTo(
          Seq(
            Document("_id" -> 6, "content" -> "textual content3"),
            Document("_id" -> 5, "content" -> "textual content1")
          )
        )
      )
    }
  }

  def close() = {
    test("Close database and clean") {
      val close = for {
        col <- collection
        _ <- col.drop()
        _ <- mongoClient.pureClose()

      } yield ()
      assertZIO(close)(equalTo(()))
    }
  }

}
