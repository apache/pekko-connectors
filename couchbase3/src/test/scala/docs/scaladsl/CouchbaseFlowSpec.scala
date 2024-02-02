package docs.scaladsl

import org.apache.pekko.stream.connectors.couchbase3.CouchbaseSupport
import org.apache.pekko.stream.connectors.testkit.scaladsl.LogCapturing
import org.apache.pekko.stream.testkit.scaladsl.StreamTestKit.assertAllStagesStopped
import org.scalatest.{ BeforeAndAfterAll, BeforeAndAfterEach, Inspectors }
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class CouchbaseFlowSpec extends AnyWordSpec
    with CouchbaseSupport
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with Matchers
    with ScalaFutures
    with Inspectors
    with LogCapturing {

  override protected def beforeAll(): Unit = {
    CouchbaseSupport.cluster

  }

  override protected def afterAll(): Unit = {}



}
