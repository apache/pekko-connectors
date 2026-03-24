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

package org.apache.pekko.stream.connectors.googlecloud.pubsub.grpc

import org.apache.pekko.annotation.ApiMayChange

/**
 * Thrown when the background ack deadline extension ticker fails.
 *
 * This indicates that the `ModifyAckDeadline` RPC failed and lease management
 * has stopped. Messages currently being processed may be redelivered by Pub/Sub
 * after their ack deadline expires.
 *
 * @since 2.0.0
 */
@ApiMayChange
class AckDeadlineExtensionException(message: String, cause: Throwable)
    extends RuntimeException(message, cause)
