package org.apache.pekko.stream.connectors.s3

import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers
import org.apache.pekko.stream.connectors.s3.impl.S3Request
import com.typesafe.config.ConfigFactory
import scala.reflect.runtime.universe._

class S3HeadersSpec extends AnyFlatSpecLike with Matchers {
  it should "filter headers based on what's allowed" in {
val testOverrideConfig = ConfigFactory.parseString("""
      | pekko.connectors.s3.additional-allowed-headers {
      |    GetObject = [allowedExtra]
      |    HeadObject = [allowedExtra]
      |    PutObject = [allowedExtra]
      |    InitiateMultipartUpload = [allowedExtra]
      |    UploadPart = [allowedExtra]
      |    CopyPart = [allowedExtra]
      |    DeleteObject = [allowedExtra]
      |    ListBucket = [allowedExtra]
      |    MakeBucket = [allowedExtra]
      |    DeleteBucket = [allowedExtra]
      |    CheckBucket = [allowedExtra]
      |    PutBucketVersioning = [allowedExtra]
      |    GetBucketVersioning = [allowedExtra]
      | }
      |""".stripMargin)

val defaultConfig = ConfigFactory.load()
val finalConfig = testOverrideConfig.withFallback(defaultConfig)

    S3Request.allRequests.foreach{
        requestType =>
            val allowedHeaders = requestType.allowedHeaders.zipWithIndex.toMap.view.mapValues(_.toString()).toMap
            val extraHeaders = Map("allowedExtra" -> "allGood", "notAllowed" -> "shouldBeGone")
            val header = S3Headers().withCustomHeaders(allowedHeaders ++ extraHeaders)
            val s3Config = finalConfig.getConfig("pekko.connectors.s3")
            val headerFilter = header.headersFor(requestType)(S3Settings.apply(s3Config))
            val result = headerFilter.map(header => (header.name(), header.value()))
            result should contain allElementsOf (allowedHeaders.toSeq ++ Seq("allowedExtra" -> "allGood"))
    }
    
    
  }

  it should "be able to convert all headers toString and back correctly" in {
    val roundTrip = S3Request.allRequests
    .map(_.toString())
    .flatMap(S3Request.fromString(_))

    roundTrip should contain allElementsOf S3Request.allRequests
    
  }

  it should "contain all S3Request types" in {
    // Get all known subclasses of the sealed trait using reflection
    val allRequestTypes = getAllSealedTraitObjects[S3Request]
    
    // Your set to test
    val actualSet = S3Request.allRequests.toSet
    
    // Verify they match
    actualSet should contain theSameElementsAs allRequestTypes
    
    // Also verify the count
    actualSet should have size allRequestTypes.size
  }

    def getAllSealedTraitObjects[T: TypeTag]: Set[T] = {
    val tpe = typeOf[T]
    val clazz = tpe.typeSymbol.asClass
    
    require(clazz.isSealed, s"${clazz.name} must be a sealed trait/class")
    
    clazz.knownDirectSubclasses.flatMap { subclass =>
      val symbol = subclass.asClass
      if (symbol.isModuleClass) {
        val mirror = runtimeMirror(getClass.getClassLoader)
        val moduleMirror = mirror.reflectModule(symbol.module.asModule)
        Some(moduleMirror.instance.asInstanceOf[T])
      } else {
        None
      }
    }
  }
}
