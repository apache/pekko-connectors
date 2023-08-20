/*
 * Copyright 2010-2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

/*
 * Copyright (C) since 2016 Lightbend Inc. <https://www.lightbend.com>
 */

package org.apache.pekko.stream.connectors.s3

private[s3] object Utils {

  /**
   * Removes S3 ETag quotes in the same way the official AWS tooling does. See
   * https://github.com/aws/aws-sdk-java/blob/f935a3758b771a25f628f1d296cb61044a82b4ac/aws-java-sdk-s3/src/main/java/com/amazonaws/services/s3/internal/ServiceUtils.java#L122
   */
  def removeQuotes(string: String): String = {
    val trimmed = string.trim()
    val tail = if (trimmed.startsWith("\"")) trimmed.drop(1) else trimmed
    if (tail.endsWith("\"")) tail.dropRight(1) else tail
  }

  /**
   * This method returns `None` if given an empty `String`. This is typically used when parsing
   * XML since its common to have XML elements with an empty text value inside.
   */
  def emptyStringToOption(value: String): Option[String] =
    if (value == "")
      None
    else
      Some(value)

}
