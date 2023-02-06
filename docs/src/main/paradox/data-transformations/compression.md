## Compressing/decompressing

Apache Pekko Stream contains support for compressing and decompressing streams of @apidoc[ByteString](org.apache.pekko.util.ByteString)
elements.

Use @apidoc[Compression](Compression$) as described in the Apache Pekko Stream documentation:

@extref:[Apache Pekko documentation](pekko:stream/stream-cookbook.html#dealing-with-compressed-data-streams)

### ZIP

@ref[Apache Pekko Connectors File](../file.md#zip-archive) supports creating flows in ZIP-format.
