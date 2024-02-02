package docs.scaladsl

import com.couchbase.client.java.kv.GetResult
import org.apache.pekko.stream.connectors.couchbase3.CouchbaseSupport
import org.apache.pekko.stream.connectors.couchbase3.scaladsl.CouchbaseSource
import org.apache.pekko.stream.connectors.testkit.scaladsl.LogCapturing
import org.apache.pekko.stream.scaladsl.Sink
import org.apache.pekko.stream.testkit.scaladsl.StreamTestKit.assertAllStagesStopped
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{ BeforeAndAfterAll, BeforeAndAfterEach, Inspectors }
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers

import scala.concurrent.Future

class CouchbaseSourceSpec extends AnyWordSpec
    with CouchbaseSupport
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with Matchers
    with ScalaFutures
    with Inspectors
    with LogCapturing {

  override protected def beforeAll(): Unit = {
    mockData(specContext)
  }

  override protected def afterAll(): Unit = {
    clearData(specContext)
  }

  "get jsonObject of Couchbase Document" in assertAllStagesStopped {
    val future: Future[GetResult] = CouchbaseSource.get(specContext.collection, jsonId)
      .runWith(Sink.head)
    future.futureValue.contentAsObject().get("id") shouldBe jsonId
  }

  "get scala case class document" in assertAllStagesStopped {
    val future: Future[GetResult] = CouchbaseSource.get(specContext.collection, docId)
      .runWith(Sink.head)
    future.futureValue.contentAsObject().get("id") shouldBe docId
  }


}
