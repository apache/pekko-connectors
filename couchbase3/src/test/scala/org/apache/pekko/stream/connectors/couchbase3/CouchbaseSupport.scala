package org.apache.pekko.stream.connectors.couchbase3

import com.couchbase.client.java._
import com.couchbase.client.java.codec.JacksonJsonSerializer
import com.couchbase.client.java.env.ClusterEnvironment
import com.couchbase.client.java.json.JsonObject
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.apache.pekko.actor.ActorSystem

object CouchbaseSupport {

  private val connectionString = "couchbase://localhost"
  private val username = "Administrator"
  private val password = "password"

  lazy val queryBucket = "pekkoquery"
  lazy val bucket = "pekko"

  private val jacksonMapper = JsonMapper.builder()
    .addModule(DefaultScalaModule)
    .build()
  private val serializer: JacksonJsonSerializer = JacksonJsonSerializer.create(jacksonMapper)
  private val environment = ClusterEnvironment.builder().jsonSerializer(serializer).build()
  lazy val cluster: Cluster = Cluster.connect(connectionString,
    ClusterOptions.clusterOptions(username, password).environment(environment))
  lazy val asyncCluster: AsyncCluster = cluster.async()
}

class SpecContext(bucketName: String) {
  import CouchbaseSupport._
  lazy val bucket: AsyncBucket = asyncCluster.bucket(bucketName)
  lazy val collection: AsyncCollection = bucket.defaultCollection()
  // used for mock and clear data
  lazy val mock: Collection = cluster.bucket(bucketName).defaultCollection()

}

trait CouchbaseSupport {
  implicit val actorSystem: ActorSystem = ActorSystem()
  protected val querySpecContext = new SpecContext(CouchbaseSupport.queryBucket)
  protected val specContext = new SpecContext(CouchbaseSupport.bucket)

  val jsonId: String = "pekko-couchbase-json"
  val docId: String = "pekko-couchbase-doc"
  val binaryId: String = "pekko-couchbase-binary"

  val jsonObject: JsonObject = JsonObject.create().put("id", jsonId).put("value", jsonId)
  val document = Document(docId, docId)
  val binaryDocument = BinaryDocument(binaryId, binaryId.getBytes)

  def mockData(specContext: SpecContext): Unit = {
    specContext.mock.insert(jsonId, jsonObject)
    specContext.mock.insert(docId, document)
    specContext.mock.insert(binaryId, binaryDocument)
  }

  def clearData(specContext: SpecContext): Unit = {
    specContext.mock.remove(jsonId)
    specContext.mock.remove(docId)
    specContext.mock.remove(binaryId)
  }
}

case class Document(id: String, value: String)

case class BinaryDocument(id: String, array: Array[Byte])
