<!--
  Licensed to the Apache Software Foundation (ASF) under one or more
  license agreements; and to You under the Apache License, version 2.0:

    https://www.apache.org/licenses/LICENSE-2.0

  This file is part of the Apache Pekko project, which was derived from Akka.
-->

## Parsing Lines

Most Apache Pekko Connectors sources stream @apidoc[org.apache.pekko.util.ByteString] elements which normally won't contain data line by line. To 
split these at line separators use @apidoc[Framing$] as described in the Apache Pekko Stream documentation.

@extref:[Apache Pekko documentation](pekko:stream/stream-cookbook.html#parsing-lines-from-a-stream-of-bytestrings)
