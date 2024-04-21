package docs.scaladsl

import com.couchbase.client.java.kv.GetResult
import org.apache.pekko.stream.connectors.couchbase3.{CouchbaseSupport, Document}
import org.apache.pekko.stream.connectors.couchbase3.scaladsl.CouchbaseSource
import org.apache.pekko.stream.connectors.testkit.scaladsl.LogCapturing
import org.apache.pekko.stream.scaladsl.Sink
import org.apache.pekko.stream.testkit.scaladsl.StreamTestKit.assertAllStagesStopped
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Inspectors}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

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
    mockData(simpleContext)
  }

  override protected def afterAll(): Unit = {
    clearData(simpleContext)
  }

  "get jsonObject of Couchbase Document" in assertAllStagesStopped {
    implicit val collection = simpleContext.collection
    val future: Future[GetResult] = CouchbaseSource.get(jsonId)
      .runWith(Sink.head)
    future.futureValue.contentAsObject().get("id") shouldBe jsonId
  }

  "get scala case class document" in assertAllStagesStopped {
    implicit val collection = simpleContext.collection
    val future: Future[Document] = CouchbaseSource.getClass(docId, classOf[Document])
      .runWith(Sink.head)
    val futureValue = future.futureValue
    futureValue shouldBe Document(docId, docId)
  }

}
