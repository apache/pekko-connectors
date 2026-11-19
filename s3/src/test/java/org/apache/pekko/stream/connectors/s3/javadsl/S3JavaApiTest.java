/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.pekko.stream.connectors.s3.javadsl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.typesafe.config.ConfigFactory;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import org.apache.pekko.Done;
import org.apache.pekko.NotUsed;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.http.javadsl.model.ContentTypes;
import org.apache.pekko.http.javadsl.model.HttpResponse;
import org.apache.pekko.http.javadsl.model.Uri;
import org.apache.pekko.japi.Pair;
import org.apache.pekko.japi.tuple.Tuple3;
import org.apache.pekko.stream.connectors.s3.*;
import org.apache.pekko.stream.connectors.s3.headers.CannedAcl;
import org.apache.pekko.stream.connectors.s3.headers.ServerSideEncryption;
import org.apache.pekko.stream.javadsl.RunnableGraph;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.stream.javadsl.Source;
import org.apache.pekko.util.ByteString;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;

/** Exercises the Java API of the S3 settings and model without S3. */
public class S3JavaApiTest {

  @Test
  public void headersFromJavaCollections() {
    Map<String, String> custom = Collections.singletonMap("x-custom", "1");
    Map<String, String> meta = Collections.singletonMap("key", "value");
    S3Headers headers =
        S3Headers.empty()
            .withCannedAcl(CannedAcl.Private())
            .withCustomHeaders(custom)
            .withMetaHeaders(MetaHeaders.create(Collections.emptyMap()).withMetaHeaders(meta))
            .withOptionalServerSideEncryption(Optional.<ServerSideEncryption>empty());

    assertEquals(custom, headers.getCustomHeaders());
    assertEquals(meta, headers.getMetaHeaders().get().getMetaHeaders());
    assertEquals(Optional.of(CannedAcl.Private()), headers.getCannedAcl());
    assertEquals(Optional.empty(), headers.getStorageClass());
    assertEquals(Optional.empty(), headers.getServerSideEncryption());
  }

  @Test
  public void serverSideEncryptionGetters() {
    assertEquals(
        Optional.of("context"),
        ServerSideEncryption.kms("key").withContext("context").getContext());
    assertEquals(
        Optional.of("md5"), ServerSideEncryption.customerKeys("key").withMd5("md5").getMd5());
  }

  @Test
  public void bucketVersioningResultGetters() {
    BucketVersioningResult result =
        BucketVersioningResult.create(
            Optional.of(BucketVersioningStatus.enabled()), Optional.of(true));
    Optional<Boolean> mfaDelete = result.getMfaDelete();
    assertEquals(Optional.of(true), mfaDelete);
    assertEquals(Optional.of(BucketVersioningStatus.enabled()), result.getStatusOptional());
  }

  @Test
  public void retrySettingsDefaults() {
    assertSame(RetrySettings.defaults(), RetrySettings.defaults());
    assertEquals(
        2,
        RetrySettings.create(
                ConfigFactory.parseString(
                    "max-retries = 2\nmin-backoff = 1s\nmax-backoff = 2s\nrandom-factor = 0.1"))
            .maxRetries());
  }

  @Test
  public void allowedHeadersFromJava() {
    Map<String, Set<String>> allowed =
        Collections.singletonMap("GetObject", Collections.singleton("x-header"));
    AwsRegionProvider region = () -> Region.US_EAST_1;
    S3Settings settings =
        S3Settings.create(
            MemoryBufferType.getInstance(),
            AnonymousCredentialsProvider.create(),
            region,
            ApiVersion.getListBucketVersion2(),
            allowed);
    assertEquals(allowed, settings.getAllowedHeaders());
    assertEquals(
        Collections.emptyMap(),
        settings
            .withAllowedHeaders(Collections.<String, Set<String>>emptyMap())
            .getAllowedHeaders());
  }

  @Test
  public void modelOverloads() {
    MultipartUploadResult result =
        MultipartUploadResult.create(
                Uri.create("http://a"), "bucket", "key", "etag", Optional.empty())
            .withLocation(Uri.create("http://b"));
    assertEquals(Uri.create("http://b"), result.getLocation());

    RuntimeException reason = new RuntimeException("boom");
    List<RuntimeException> reasons = Collections.singletonList(reason);
    assertEquals(reason, FailedUpload.create(reasons).getReasons().get(0));
  }

  /** Compile-only check: S3 operations can be called without Scala defaults or implicits. */
  @SuppressWarnings("unused")
  private static void operationsCompile(ActorSystem system, BucketVersioning versioning) {
    S3Headers headers = S3Headers.empty();
    Source<HttpResponse, NotUsed> request = S3.request("bucket", "key");
    Source<HttpResponse, NotUsed> requestVersion = S3.request("bucket", "key", Optional.of("v1"));
    Source<
            Tuple3<
                List<ListObjectVersionsResultVersions>, List<DeleteMarkers>, List<CommonPrefixes>>,
            NotUsed>
        versions = S3.listObjectVersionsAndCommonPrefixes("bucket", "/", Optional.of("p"), headers);
    Source<Pair<List<ListMultipartUploadResultUploads>, List<CommonPrefixes>>, NotUsed> uploads =
        S3.listMultipartUploadAndCommonPrefixes("bucket", "/", Optional.empty());
    Source<ByteString, CompletionStage<ObjectMetadata>> object =
        S3.getObject("bucket", "key", Optional.of("v1"), headers);
    Source<Optional<ObjectMetadata>, NotUsed> metadata =
        S3.getObjectMetadataWithHeaders("bucket", "key", Optional.of("v1"), headers);
    CompletionStage<MultipartUploadResult> completed =
        S3.completeMultipartUpload("bucket", "key", "id", Collections.<Part>emptyList(), system);
    CompletionStage<Done> deleted = S3.deleteUpload("bucket", "key", "id", system);
    CompletionStage<Done> put = S3.putBucketVersioning("bucket", versioning, system);
    Sink<ByteString, CompletionStage<MultipartUploadResult>> upload =
        S3.multipartUpload(
            "bucket", "key", ContentTypes.APPLICATION_OCTET_STREAM, headers, 6000000, 2);
    Sink<ByteString, CompletionStage<MultipartUploadResult>> resume =
        S3.resumeMultipartUpload(
            "bucket",
            "key",
            "id",
            Collections.<Part>emptyList(),
            ContentTypes.APPLICATION_OCTET_STREAM,
            headers,
            6000000,
            2);
    RunnableGraph<CompletionStage<MultipartUploadResult>> copy =
        S3.multipartCopy(
            "a",
            "b",
            "c",
            "d",
            Optional.empty(),
            ContentTypes.APPLICATION_OCTET_STREAM,
            headers,
            6000000,
            2);
  }
}
