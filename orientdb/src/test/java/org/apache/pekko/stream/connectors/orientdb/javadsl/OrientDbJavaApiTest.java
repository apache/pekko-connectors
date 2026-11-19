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

package org.apache.pekko.stream.connectors.orientdb.javadsl;

import com.orientechnologies.orient.core.db.ODatabasePool;
import org.apache.pekko.NotUsed;
import org.apache.pekko.stream.connectors.orientdb.OrientDbReadResult;
import org.apache.pekko.stream.connectors.orientdb.OrientDbSourceSettings;
import org.apache.pekko.stream.connectors.orientdb.OrientDbWriteSettings;
import org.apache.pekko.stream.javadsl.Source;

/** Compile-only checks for the OrientDB Java API. */
public class OrientDbJavaApiTest {

  @SuppressWarnings("unused")
  private static void apiCompiles(ODatabasePool pool, ODatabasePool otherPool) {
    OrientDbSourceSettings sourceSettings =
        OrientDbSourceSettings.create(pool).withODatabasePool(otherPool);
    OrientDbWriteSettings writeSettings =
        OrientDbWriteSettings.create(pool).withODatabasePool(otherPool);
    Source<OrientDbReadResult<String>, NotUsed> typed =
        OrientDbSource.typed("className", sourceSettings, String.class);
  }
}
