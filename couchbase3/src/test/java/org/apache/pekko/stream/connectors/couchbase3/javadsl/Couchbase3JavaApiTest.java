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

package org.apache.pekko.stream.connectors.couchbase3.javadsl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.couchbase.client.java.AsyncCollection;
import com.couchbase.client.java.AsyncScope;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.kv.ExistsOptions;
import com.couchbase.client.java.kv.GetOptions;
import com.couchbase.client.java.kv.GetResult;
import com.couchbase.client.java.kv.InsertOptions;
import com.couchbase.client.java.kv.ReplaceOptions;
import com.couchbase.client.java.kv.RemoveOptions;
import com.couchbase.client.java.kv.UpsertOptions;
import com.couchbase.client.java.query.QueryResult;
import org.apache.pekko.NotUsed;
import org.apache.pekko.stream.connectors.couchbase3.MutationBinaryDocument;
import org.apache.pekko.stream.connectors.couchbase3.MutationDocument;
import org.apache.pekko.stream.javadsl.Flow;
import org.apache.pekko.stream.javadsl.Source;
import org.junit.jupiter.api.Test;

/** Exercises the couchbase3 Java API without a Couchbase cluster. */
public class Couchbase3JavaApiTest {

  @Test
  public void mutationDocumentFactories() {
    MutationDocument<String> document = MutationDocument.create("id", "doc");
    assertEquals("id", document.id());
    assertEquals("doc", document.doc());
    assertFalse(document.getResult().isPresent());

    MutationBinaryDocument binary = MutationBinaryDocument.create("id", new byte[] {1, 2});
    assertEquals("id", binary.id());
    assertFalse(binary.getResult().isPresent());
  }

  /** Compile-only check: lambdas resolve to a single overload and results use Java types. */
  @SuppressWarnings("unused")
  private static void flowsCompile(AsyncCollection collection) {
    Flow<JsonObject, JsonObject, NotUsed> insert =
        CouchbaseFlow.insert(
            (JsonObject doc) -> doc.getString("id"), InsertOptions.insertOptions(), collection);
    Flow<JsonObject, JsonObject, NotUsed> replace =
        CouchbaseFlow.replace(
            (JsonObject doc) -> doc.getString("id"), ReplaceOptions.replaceOptions(), collection);
    Flow<JsonObject, JsonObject, NotUsed> upsert =
        CouchbaseFlow.upsert(
            (JsonObject doc) -> doc.getString("id"), UpsertOptions.upsertOptions(), collection);
    Flow<JsonObject, JsonObject, NotUsed> remove =
        CouchbaseFlow.remove(
            (JsonObject doc) -> doc.getString("id"), RemoveOptions.removeOptions(), collection);
    Flow<String, Boolean, NotUsed> exists = CouchbaseFlow.exists(id -> id, collection);
    Flow<String, Boolean, NotUsed> existsWithOptions =
        CouchbaseFlow.exists(id -> id, ExistsOptions.existsOptions(), collection);
  }

  /** Compile-only check: sources can be created without Scala default or implicit arguments. */
  @SuppressWarnings("unused")
  private static void sourcesCompile(AsyncCollection collection, AsyncScope scope) {
    Source<GetResult, NotUsed> get = CouchbaseSource.get("id", collection);
    Source<GetResult, NotUsed> getWithOptions =
        CouchbaseSource.get("id", GetOptions.getOptions(), collection);
    Source<JsonObject, NotUsed> getJson = CouchbaseSource.getJson("id", collection);
    Source<QueryResult, NotUsed> query = CouchbaseSource.query("select 1", scope);
    Source<JsonObject, NotUsed> queryJson = CouchbaseSource.queryJson("select 1", scope);
  }
}
