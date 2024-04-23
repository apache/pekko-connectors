package docs.scaladsl

import com.couchbase.client.java.kv.{ DecrementOptions, IncrementOptions, MutateInSpec, ReplaceOptions }
import com.couchbase.client.java.AsyncCollection
import org.apache.pekko.stream.connectors.couchbase3.{ CouchbaseSupport, Document }
import org.apache.pekko.stream.connectors.couchbase3.scaladsl.{ CouchbaseFlow, CouchbaseSink, CouchbaseSource }
import org.apache.pekko.stream.connectors.testkit.scaladsl.LogCapturing
import org.apache.pekko.stream.scaladsl.{ Flow, Sink, Source }
import org.apache.pekko.stream.testkit.scaladsl.StreamTestKit.assertAllStagesStopped
import org.scalatest.{ BeforeAndAfterAll, BeforeAndAfterEach, Inspectors }
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.Duration
import java.util.Collections
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.util.Random

class CouchbaseFlowSpec extends AnyWordSpec
    with CouchbaseSupport
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with Matchers
    with ScalaFutures
    with Inspectors
    with LogCapturing {

  private implicit val collection: AsyncCollection = simpleContext.collection

  override protected def beforeAll(): Unit = {
    mockData(simpleContext)
  }

  override protected def afterAll(): Unit = {
    clearData(simpleContext)
  }

  "insert-get-remove document" in assertAllStagesStopped {
    val data = Seq(document.copy(id = s"$docId-1"), document.copy(id = s"$docId-2"))
    // insert get, because we add one,we need clear it
    val insertFlow = CouchbaseFlow.insert[Document](_.id)
    val getFlow = CouchbaseFlow.getObject(classOf[Document])
    val removeFlow = CouchbaseFlow.remove[Document](_.id)
    val insertFuture = Source(data)
      .via(insertFlow)
      .map(_.id)
      .via(getFlow)
      .via(removeFlow)
      .runWith(Sink.seq)

    insertFuture.futureValue shouldBe data
  }

  "upsert-get document" in assertAllStagesStopped {
    val upsertDocument = document.copy(value = document.value + "-upsert")
    val upsertFlow = CouchbaseFlow.upsert[Document](_.id)
    val getFlow = CouchbaseFlow.getObject(classOf[Document])
    val upsertFuture = Source.single(upsertDocument)
      .via(upsertFlow)
      .map(_.id)
      .via(getFlow)
      .runWith(Sink.head)
    upsertFuture.futureValue shouldBe upsertDocument
  }

  "replace data" in assertAllStagesStopped {
    val replaceDocument = document.copy(value = document.value + "-upsert")
    // replace with cas, last remove
    val replaceFlow = CouchbaseSource.get(replaceDocument.id)
      .map(_.cas())
      .map(ReplaceOptions.replaceOptions().cas(_))
      .map { e =>
        CouchbaseFlow.replace[Document](_.id, e)
      }.runWith(Sink.head)

    val future = Source.single(replaceDocument)
      .via(Flow.futureFlow(replaceFlow))
      .map(_.id)
      .via(CouchbaseFlow.getObject(classOf[Document]))
      .runWith(Sink.head)

    future.futureValue shouldBe replaceDocument
  }

  "check exists" in assertAllStagesStopped {
    val existsFlow = CouchbaseFlow.exists[Document](_.id)
    val existFuture = Source.single(document)
      .via(existsFlow)
      .runWith(Sink.head)
    existFuture.futureValue shouldBe true
  }

  "touch 1s, wait 2s and check exists" in assertAllStagesStopped {
    val touchDocument = document.copy(id = document.id + "-touch")
    // touch 1s, sleep 1s, check exists = false
    val insertFuture = Source.single(touchDocument)
      .runWith(CouchbaseSink.insert[Document](_.id))
    Await.result(insertFuture, 10.seconds)
    val touchFlow = CouchbaseFlow.touchDuration[Document](_.id, Duration.ofSeconds(1))
    val touchFuture = Source.single(touchDocument)
      .via(touchFlow)
      .runWith(Sink.ignore)
    Await.result(touchFuture, 10.seconds)
    // wait the doc was deleted by couchbase
    Thread.sleep(2000)
    val future = Source.single(touchDocument.id).runWith(CouchbaseSink.exists[String](e => e))
    future.futureValue shouldBe false
  }

  "mutateIn data: add field to JsonDoc" in assertAllStagesStopped {
    // scala users can use convert. We support 2.12,so use java.util.Collections
    val insertOptions: java.util.List[MutateInSpec] =
      Collections.singletonList(MutateInSpec.insert("mutate", "mutate"))
    val mutateInFlow = CouchbaseFlow.mutateIn(insertOptions)
    val mutateFuture = Source.single(jsonId)
      .via(mutateInFlow)
      .runWith(Sink.head)
    Await.result(mutateFuture, 10.seconds)
    val future = CouchbaseSource.getJson(jsonId).runWith(Sink.head)
    future.futureValue.getString("mutate") shouldBe "mutate"
  }

  "increment and decrement on Number document" in assertAllStagesStopped {
    val id = Random.nextString(10)
    registerBeClear.add(id)
    val value: Long = 0
    val increment = 10
    val insertNum = Source.single(value).runWith(CouchbaseSink.insert[Long](_ => id))
    Await.result(insertNum, 10.seconds)
    val incrementFlow = CouchbaseFlow.increment(IncrementOptions.incrementOptions().delta(increment))
    val incrementFuture = Source.single(id)
      .via(incrementFlow)
      .runWith(Sink.head)

    incrementFuture.futureValue.content() shouldBe (value + increment)
    val decrement: Long = 5
    val decrementFlow = CouchbaseFlow.decrement(DecrementOptions.decrementOptions().delta(decrement))
    val decrementFuture = Source.single(id)
      .via(decrementFlow)
      .runWith(Sink.head)

    decrementFuture.futureValue.content() shouldBe (value + increment - decrement)
  }

}
