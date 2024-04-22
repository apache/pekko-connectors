package org.apache.pekko.stream.connectors.couchbase3

import com.couchbase.client.core.io.CollectionIdentifier
import com.couchbase.client.java._
import com.couchbase.client.java.codec.JacksonJsonSerializer
import com.couchbase.client.java.env.ClusterEnvironment
import com.couchbase.client.java.json.JsonObject
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.connectors.couchbase3.CouchbaseSupport.bucketName

object CouchbaseSupport {

  private val connectionString = "localhost"
  private val username = "Administrator"
  private val password = "password"
  private lazy val bucketName = "pekko"

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
  lazy val scope: AsyncScope = bucket.defaultScope()
  lazy val collection: AsyncCollection = bucket.defaultCollection()
  // used for mock and clear data
  lazy val mock: Collection = CouchbaseSupport.cluster.bucket(bucketName).defaultCollection()

}

trait CouchbaseSupport {
  implicit val actorSystem: ActorSystem = ActorSystem()
  val simpleContext = new SpecContext(bucketName)
  val defaultScope = CollectionIdentifier.fromDefault(bucketName).scope().get()
  val defaultCollection = CollectionIdentifier.fromDefault(bucketName).collection().get()

  val jsonId: String = "pekko-couchbase-json"
  val docId: String = "pekko-couchbase-doc"
  val typeId: String = "pekko-couchbase-type"
  val binaryId: String = "pekko-couchbase-binary"

  val jsonObject: JsonObject = JsonObject.create().put("id", jsonId).put("value", jsonId)
  val document = Document(docId, docId)
  val typeDocument = TypeDocument[String](typeId, List(typeId))
  val binaryDocument = BinaryDocument(binaryId, binaryId.getBytes)
  // idSet dataSet should be associated
  val idSet = Seq(jsonId, docId, typeId, binaryId)
  val dataSet = Set(jsonObject, document, typeDocument, binaryDocument)

  def mockData(specContext: SpecContext): Unit = {
    idSet.zip(dataSet).foreach { e =>
      specContext.mock.insert(e._1, e._2)
    }
    simpleContext.mock.queryIndexes().createPrimaryIndex()
  }

  def clearData(specContext: SpecContext): Unit = {
    idSet.foreach(specContext.mock.remove)
    simpleContext.mock.queryIndexes().dropPrimaryIndex()
  }
}

case class Document(id: String, value: String)
case class TypeDocument[T](id: String, value: List[T])
case class BinaryDocument(id: String, value: Array[Byte])
