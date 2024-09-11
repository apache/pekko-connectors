# FTP

The FTP connector provides Apache Pekko Stream sources to connect to FTP, FTPS and SFTP servers. Currently, two kinds of sources are provided:

* one for browsing or traversing the server recursively and,
* another for retrieving files as a stream of bytes.

@@project-info{ projectId="ftp" }

## Artifacts

@@dependency [sbt,Maven,Gradle] {
  group=org.apache.pekko
  artifact=pekko-connectors-ftp_$scala.binary.version$
  version=$project.version$
  symbol2=PekkoVersion
  value2=$pekko.version$
  group2=org.apache.pekko
  artifact2=pekko-stream_$scala.binary.version$
  version2=PekkoVersion
}

The table below shows direct dependencies of this module and the second tab shows all libraries it depends on transitively.

@@dependencies { projectId="ftp" }


## Configuring the connection settings

In order to establish a connection with the remote server, you need to provide a specialized version of a @scaladoc[RemoteFileSettings](org.apache.pekko.stream.connectors.ftp.RemoteFileSettings) instance. It's specialized as it depends on the kind of server you're connecting to: FTP, FTPS or SFTP.

Scala
: @@snip [snip](/ftp/src/test/scala/docs/scaladsl/FtpExamplesSpec.scala) { #create-settings }

Java
: @@snip [snip](/ftp/src/test/java/docs/javadsl/FtpWritingTest.java) { #create-settings }

The configuration above will create an anonymous connection with a remote FTP server in passive mode. For both FTPS and SFTP servers, you will need to provide the specialized versions of these settings: @scaladoc[FtpsSettings](org.apache.pekko.stream.connectors.ftp.FtpsSettings) or @scaladoc[SftpSettings](org.apache.pekko.stream.connectors.ftp.SftpSettings)
respectively.

The example demonstrates optional use of `configureConnection` option available on FTP and FTPS clients. Use it to configure any custom parameters the server may require, such as explicit or implicit data transfer encryption.

For non-anonymous connection, please provide an instance of @scaladoc[NonAnonFtpCredentials](org.apache.pekko.stream.connectors.ftp.FtpCredentials$$NonAnonFtpCredentials) instead.

For connection via a proxy, please provide an instance of `java.net.Proxy` by using the `withProxy` method.

For connection using a private key, please provide an instance of @scaladoc[SftpIdentity](org.apache.pekko.stream.connectors.ftp.SftpIdentity) to @scaladoc[SftpSettings](org.apache.pekko.stream.connectors.ftp.SftpSettings).

In order to use a custom SSH client for SFTP please provide an instance of [SSHClient](https://www.javadoc.io/doc/com.hierynomus/sshj/0.27.0/net/schmizz/sshj/SSHClient.html).

Scala
: @@snip [snip](/ftp/src/test/scala/docs/scaladsl/scalaExamples.scala) { #configure-custom-ssh-client }

Java
: @@snip [snip](/ftp/src/test/java/docs/javadsl/ConfigureCustomSSHClient.java) { #configure-custom-ssh-client }

### Improving SFTP throughput
For SFTP connections allowing more than one unconfirmed read request to be sent by the client you can use `withMaxUnconfirmedReads` on @scaladoc[SftpSettings](org.apache.pekko.stream.connectors.ftp.SftpSettings)  
The command-line tool `sftp` uses a value of `64` by default.  This can significantly improve throughput by reducing the impact of latency.

Scala
: @@snip [snip](/ftp/src/test/scala/docs/scaladsl/scalaExamples.scala) { #retrieving-with-unconfirmed-reads }

Java
: @@snip [snip](/ftp/src/test/java/docs/javadsl/SftpRetrievingExample.java) { #retrieving-with-unconfirmed-reads }

## Traversing a remote FTP folder recursively

In order to traverse a remote folder recursively, you need to use the `ls` method in the FTP API:

Scala
: @@snip [snip](/ftp/src/test/scala/docs/scaladsl/scalaExamples.scala) { #traversing }

Java
: @@snip [snip](/ftp/src/test/java/docs/javadsl/FtpTraversingExample.java) { #traversing }

This source will emit @scaladoc[FtpFile](org.apache.pekko.stream.connectors.ftp.FtpFile) elements with no significant materialization.

For both FTPS and SFTP servers, you will need to use the `FTPS` and `SFTP` API respectively.

## Retrieving files

In order to retrieve a remote file as a stream of bytes, you need to use the `fromPath` method in the FTP API:

Scala
: @@snip [snip](/ftp/src/test/scala/docs/scaladsl/scalaExamples.scala) { #retrieving }

Java
: @@snip [snip](/ftp/src/test/java/docs/javadsl/FtpRetrievingExample.java) { #retrieving }

This source will emit @scaladoc[ByteString](org.apache.pekko.util.ByteString) elements and materializes to @scaladoc[Future](scala.concurrent.Future) in Scala API and @javadoc[CompletionStage](java/util/concurrent/CompletionStage) in Java API of @scaladoc[IOResult](org.apache.pekko.stream.IOResult) when the stream finishes.

For both FTPS and SFTP servers, you will need to use the `FTPS` and `SFTP` API respectively.

## Writing files

In order to store a remote file from a stream of bytes, you need to use the `toPath` method in the FTP API:

Scala
: @@snip [snip](/ftp/src/test/scala/docs/scaladsl/FtpExamplesSpec.scala) { #storing }

Java
: @@snip [snip](/ftp/src/test/java/docs/javadsl/FtpWritingTest.java) { #storing }

This sink will consume @scaladoc[ByteString](org.apache.pekko.util.ByteString) elements and materializes to @scaladoc[Future](scala.concurrent.Future) in Scala API and @javadoc[CompletionStage](java/util/concurrent/CompletionStage) in Java API of @scaladoc[IOResult](org.apache.pekko.stream.IOResult) when the stream finishes.

For both FTPS and SFTP servers, you will need to use the `FTPS` and `SFTP` API respectively.

## Removing files

In order to remove a remote file, you need to use the `remove` method in the FTP API:

Scala
: @@snip [snip](/ftp/src/test/scala/docs/scaladsl/scalaExamples.scala) { #removing }

Java
: @@snip [snip](/ftp/src/test/java/docs/javadsl/FtpRemovingExample.java) { #removing }

This sink will consume @scaladoc[FtpFile](org.apache.pekko.stream.connectors.ftp.FtpFile) elements and materializes to @scaladoc[Future](scala.concurrent.Future) in Scala API and @javadoc[CompletionStage](java/util/concurrent/CompletionStage) in Java API of @scaladoc[IOResult](org.apache.pekko.stream.IOResult) when the stream finishes.

## Moving files

In order to move a remote file, you need to use the `move` method in the FTP API. The `move` method takes a function to calculate the path to which the file should be moved based on the consumed @scaladoc[FtpFile](org.apache.pekko.stream.connectors.ftp.FtpFile).   

Scala
: @@snip [snip](/ftp/src/test/scala/docs/scaladsl/scalaExamples.scala) { #moving }

Java
: @@snip [snip](/ftp/src/test/java/docs/javadsl/FtpMovingExample.java) { #moving }

This sink will consume @scaladoc[FtpFile](org.apache.pekko.stream.connectors.ftp.FtpFile) elements and materializes to @scaladoc[Future](scala.concurrent.Future) in Scala API and @javadoc[CompletionStage](java/util/concurrent/CompletionStage) in Java API of @scaladoc[IOResult](org.apache.pekko.stream.IOResult) when the stream finishes.

Typical use-case for this would be listing files from a ftp location, do some processing and move the files when done. An example of this use case can be found below.

## Creating directory

In order to create a directory the user has to specify a parent directory (also known as base path) and directory's name.

Apache Pekko Connectors provides a materialized API `mkdirAsync` (based on @scala[Future]@java[Completion Stage]) and unmaterialized API `mkdir` (using Sources) to let the user choose when the action will be executed.

Scala
: @@snip [snip](/ftp/src/test/scala/docs/scaladsl/scalaExamples.scala) { #mkdir-source }

Java
: @@snip [snip](/ftp/src/test/java/docs/javadsl/FtpMkdirExample.java){ #mkdir-source }

Please note that to include a subdirectory in result of `ls` the `emitTraversedDirectories` has to be set to `true`.

### Example: downloading files from an FTP location and move the original files  

Scala
: @@snip [snip](/ftp/src/test/scala/docs/scaladsl/scalaExamples.scala) { #processAndMove }

Java
: @@snip [snip](/ftp/src/test/java/docs/javadsl/FtpProcessAndMoveExample.java) { #processAndMove }

### Running the example code

The code in this guide is part of runnable tests of this project. You are welcome to browse the code, edit and run it in sbt.

    ```
    docker compose up -d ftp sftp
    sbt
    > ftp/test
    ```

@@@ warning
When using the `SFTP` API, take into account that JVM relies on `/dev/random` for random number generation by default. This might potentially block the process on some operating systems as `/dev/random` waits for a certain amount of entropy to be generated on the host machine before returning a result. In such case, please consider providing the parameter `-Djava.security.egd = file:/dev/./urandom` into the execution context. Further information can be found [here](https://www.2uo.de/myths-about-urandom/).
@@@
