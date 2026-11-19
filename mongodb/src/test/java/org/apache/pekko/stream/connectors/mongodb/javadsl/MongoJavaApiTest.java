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

package org.apache.pekko.stream.connectors.mongodb.javadsl;

import com.mongodb.client.result.UpdateResult;
import com.mongodb.reactivestreams.client.MongoCollection;
import org.apache.pekko.NotUsed;
import org.apache.pekko.japi.Pair;
import org.apache.pekko.stream.connectors.mongodb.DocumentReplace;
import org.apache.pekko.stream.javadsl.Flow;
import org.bson.Document;

/** Compile-only checks for the MongoDB Java API. */
public class MongoJavaApiTest {

  @SuppressWarnings("unused")
  private static void replaceOneWithoutOptions(MongoCollection<Document> collection) {
    Flow<DocumentReplace<Document>, Pair<UpdateResult, DocumentReplace<Document>>, NotUsed> flow =
        MongoFlow.replaceOne(collection);
  }
}
