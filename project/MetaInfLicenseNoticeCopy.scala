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

import sbt.Keys._
import sbt._
import org.mdedetrich.apache.sonatype.ApacheSonatypePlugin
import org.mdedetrich.apache.sonatype.ApacheSonatypePlugin.autoImport._

/**
 * Copies LICENSE and NOTICE files into jar META-INF dir
 */
object MetaInfLicenseNoticeCopy extends AutoPlugin {

  private lazy val baseDir = LocalRootProject / baseDirectory

  override lazy val projectSettings = Seq(
    apacheSonatypeLicenseFile := baseDir.value / "legal" / "StandardLicense.txt",
    apacheSonatypeNoticeFile := baseDir.value / "legal" / "PekkoConnectorsNotice.txt",
    apacheSonatypeDisclaimerFile := Some(baseDir.value / "DISCLAIMER"))

  lazy val mqttStreamingSettings = Seq(
    apacheSonatypeLicenseFile := baseDir.value / "legal" / "MqttStreamingLicense.txt")

  lazy val s3Settings = Seq(
    apacheSonatypeLicenseFile := baseDir.value / "legal" / "S3License.txt",
    apacheSonatypeNoticeFile := baseDir.value / "legal" / "S3Notice.txt")

  lazy val googleCommonSettings = Seq(
    apacheSonatypeLicenseFile := baseDir.value / "legal" / "GoogleCommonLicense.txt")

  override def trigger = allRequirements

  override def requires = ApacheSonatypePlugin
}
