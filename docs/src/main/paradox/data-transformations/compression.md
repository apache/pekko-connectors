<!--
  Licensed to the Apache Software Foundation (ASF) under one or more
  license agreements; and to You under the Apache License, version 2.0:

    https://www.apache.org/licenses/LICENSE-2.0

  This file is part of the Apache Pekko project, which was derived from Akka.
-->

## Compressing/decompressing

Apache Pekko Stream contains support for compressing and decompressing streams of @apidoc[ByteString](org.apache.pekko.util.ByteString)
elements.

Use @apidoc[Compression](Compression$) as described in the Apache Pekko Stream documentation:

@extref:[Apache Pekko documentation](pekko:stream/stream-cookbook.html#dealing-with-compressed-data-streams)

### ZIP

@ref[Apache Pekko Connectors File](../file.md#zip-archive) supports creating flows in ZIP-format.
