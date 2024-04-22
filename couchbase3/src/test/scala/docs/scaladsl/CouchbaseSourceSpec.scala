package docs.scaladsl

import com.couchbase.client.java.{AsyncCollection, AsyncScope}
import com.couchbase.client.java.codec.TypeRef
import com.couchbase.client.java.kv.ScanType
import org.apache.pekko.stream.connectors.couchbase3.{CouchbaseSupport, Document, TypeDocument}
import org.apache.pekko.stream.connectors.couchbase3.scaladsl.CouchbaseSource
import org.apache.pekko.stream.connectors.testkit.scaladsl.LogCapturing
import org.apache.pekko.stream.scaladsl.Sink
import org.apache.pekko.stream.testkit.scaladsl.StreamTestKit.assertAllStagesStopped
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Inspectors}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class CouchbaseSourceSpec extends AnyWordSpec
    with CouchbaseSupport
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with Matchers
    with ScalaFutures
    with Inspectors
    with LogCapturing {

  private implicit val collection: AsyncCollection = simpleContext.collection
  private implicit val scope: AsyncScope = simpleContext.scope

  override protected def beforeAll(): Unit = {
    mockData(simpleContext)
  }

  override protected def afterAll(): Unit = {
    clearData(simpleContext)
  }

  "get document by jsonObject" in assertAllStagesStopped {
    val future = CouchbaseSource.getJson(jsonId)
      .runWith(Sink.head)
    future.futureValue.getString("id") shouldBe jsonId
  }

  "get document by scala case class" in assertAllStagesStopped {
    val future = CouchbaseSource.getObject(docId, classOf[Document])
      .runWith(Sink.head)
    val futureValue = future.futureValue
    futureValue shouldBe document
  }

  "get document class with type" in assertAllStagesStopped {
    val docType = new TypeRef[TypeDocument[String]] {}
    val future = CouchbaseSource.getType(typeId, docType)
      .runWith(Sink.head)
    val futureValue = future.futureValue
    futureValue shouldBe typeDocument
  }

  "scan document" in assertAllStagesStopped {
    val future = CouchbaseSource.scan(ScanType.samplingScan(1)).runWith(Sink.head)
    idSet.contains(future.futureValue.id()) shouldBe true
  }

  "query document by sql++" in assertAllStagesStopped {
    val sql = s"select * from $defaultCollection"
    val queryFuture = CouchbaseSource.query(sql).runWith(Sink.head)
    queryFuture.futureValue.rowsAsObject().size() shouldBe dataSet.size
  }

  "query json document by sql++" in assertAllStagesStopped {
    val sql = s"select * from $defaultCollection"
    val queryFuture = CouchbaseSource.queryJson(sql).runWith(Sink.seq)
    val queryDocuments = queryFuture.futureValue
    queryDocuments.size shouldBe dataSet.size
    queryDocuments.foreach { e =>
      idSet.contains(e.getString("id")) shouldBe true
    }
  }

  "query all indexes" in assertAllStagesStopped {
    // we have a primary index in afterAll
    val queryIndexFuture = CouchbaseSource.queryAllIndex().runWith(Sink.seq)
    val future = queryIndexFuture.futureValue
    future.foreach(_.primary() shouldBe true)
    future.size shouldBe 1
  }
}
