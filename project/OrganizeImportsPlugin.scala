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

import sbt._
import sbt.Keys._
import scalafix.sbt.ScalafixPlugin
import scalafix.sbt.ScalafixPlugin.autoImport.scalafix

/** Keep documentation snippet boundaries intact when organizing imports. */
object OrganizeImportsPlugin extends AutoPlugin {
  override def requires = ScalafixPlugin
  override def trigger = allRequirements

  override def projectSettings = Seq(
    Compile / scalafix / unmanagedSources ~= excludeDocumentationSnippets,
    Test / scalafix / unmanagedSources ~= excludeDocumentationSnippets)

  private def excludeDocumentationSnippets(sources: Seq[File]): Seq[File] =
    sources.filterNot(source => IO.readLines(source).exists(_.trim.matches("//\\s*#.*")))
}
