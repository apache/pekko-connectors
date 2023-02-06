## Parsing Lines

Most Apache Pekko Connectors sources stream @apidoc[akka.util.ByteString] elements which normally won't contain data line by line. To 
split these at line separators use @apidoc[Framing$] as described in the Apache Pekko Stream documentation.

@extref:[Apache Pekko documentation](pekko:stream/stream-cookbook.html#parsing-lines-from-a-stream-of-bytestrings)
