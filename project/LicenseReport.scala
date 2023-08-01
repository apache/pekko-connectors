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
import sbtlicensereport.SbtLicenseReport
import sbtlicensereport.SbtLicenseReport.autoImportImpl._
import sbtlicensereport.license.{ DepModuleInfo, LicenseInfo, MarkDown }

object LicenseReport extends AutoPlugin {

  override lazy val projectSettings = Seq(
    licenseReportTypes := Seq(MarkDown),
    licenseReportMakeHeader := (language => language.header1("License Report")),
    licenseConfigurations := Set("compile", "test", "provided"),
    licenseDepExclusions := {
      case DepModuleInfo("org.apache.pekko", _, _) => true // Inter pekko project dependencies are pointless
      case DepModuleInfo(_, "scala-library", _)    => true // Scala library is part of Scala language
      case DepModuleInfo(_, "scala-reflect", _)    => true // Scala reflect is part of Scala language
    },
    licenseOverrides := {
      // This is here because the License URI for the unicode license isn't correct in POM
      case DepModuleInfo("com.ibm.icu", "icu4j", _) => LicenseInfo.Unicode
      // The asm # asm artifacts are missing license in POM and the org.ow2.asm # asm-* artifacts don't
      // point to correct license page
      case dep: DepModuleInfo if dep.organization.endsWith("asm") && dep.name.startsWith("asm") =>
        LicenseInfo(
          LicenseCategory.BSD,
          "BSD 3-Clause",
          "https://asm.ow2.io/license.html")
      // Missing license in POM
      case DepModuleInfo("dom4j", "dom4j", _) => LicenseInfo(
          LicenseCategory("dom4j"),
          "dom4j",
          "https://raw.githubusercontent.com/dom4j/dom4j/master/LICENSE")
      case DepModuleInfo("org.hibernate.javax.persistence", "hibernate-jpa-2.0-api", _) => LicenseInfo.EDL
      case DepModuleInfo("commons-beanutils", "commons-beanutils", _)                   => LicenseInfo.APACHE2
      case DepModuleInfo("io.netty", "netty-tcnative-boringssl-static", _)              => LicenseInfo.APACHE2
      case DepModuleInfo("org.apache.zookeeper", "zookeeper", _)                        => LicenseInfo.APACHE2
      case DepModuleInfo("org.codehaus.jettison", "jettison", _)                        => LicenseInfo.APACHE2
      case dep: DepModuleInfo if dep.organization.startsWith("javax")                   => LicenseInfo.CDDL_GPL
    },
    licenseReportColumns := Seq(
      Column.Category,
      Column.License,
      Column.Dependency,
      Column.OriginatingArtifactName,
      Column.Configuration))

  override def requires = plugins.JvmPlugin && SbtLicenseReport

  override def trigger = allRequirements

}
