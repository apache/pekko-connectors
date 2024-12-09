# Azure Storage Queue

The Azure Storage Queue connector provides an Apache Pekko Stream Source and Sinks for Azure Storage Queue integration.

Azure Storage Queue is a queuing service similar to Amazon's SQS. It is designed mostly for long-running and non-time-critical tasks. For more information on Azure Storage Queue see the [Azure docs](https://azure.microsoft.com/en-us/products/storage/queues).

@@project-info{ projectId="azure-storage-queue" }

## Artifacts

@@dependency [sbt,Maven,Gradle] {
  group=org.apache.pekko
  artifact=pekko-connectors-azure-storage-queue_$scala.binary.version$
  version=$project.version$
  symbol2=PekkoVersion
  value2=$pekko.version$
  group2=org.apache.pekko
  artifact2=pekko-stream_$scala.binary.version$
  version2=PekkoVersion
}

The table below shows direct dependencies of this module and the second tab shows all libraries it depends on transitively.

@@dependencies { projectId="azure-storage-queue" }


## Init Azure Storage API

```scala
import com.microsoft.azure.storage._
import com.microsoft.azure.storage.queue._
val storageConnectionString = "DefaultEndpointsProtocol=http;AccountName=<YourAccountName>;AccountKey=<YourKey>"
val queueFactory = () => { // Since azure storage JDK is not guaranteed to be thread-safe
  val storageAccount = CloudStorageAccount.parse(storageConnectionString)
  val queueClient = storageAccount.createCloudQueueClient
  queueClient.getQueueReference("myQueue")
}
```

For more details, see [Microsoft Azure Storage Docs](https://docs.microsoft.com/en-us/azure/storage/queues/storage-java-how-to-use-queue-storage).

## Queuing a message

```scala
import one.aleph.akkzure.queue._
import one.aleph.akkzure.queue.scaladsl._

// Create an example message
val message = new CloudQueueMessage("Hello Azure")

Source.single(message).runWith(AzureQueueSink(queueFactory))
```

## Processing and deleting messages

```scala
AzureQueueSource(queueFactory).take(10)
.map({ msg: CloudQueueMessage =>
  println(msg.getMessageContentAsString) // Print the messages content
  msg                                    // Return message to the flow for deletion
}).runWith(AzureQueueDeleteSink(queueFactory))
```
