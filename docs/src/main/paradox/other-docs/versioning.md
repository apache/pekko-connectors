# Versioning 

Apache Pekko Connectors uses the versioning scheme `major.minor.patch` (just as the rest of the Apache Pekko family of projects). The Apache Pekko family diverges a bit from Semantic Versioning in that new, compatible functionality is added in patch releases.

@@@ note 

As all Apache Pekko Connectors modules (excluding Apache Pekko Connectors Kafka) share a single version number, modules often don't contain any notable changes even though the version number has changed significantly.

@@@

Apache Pekko Connectors publishes 

* regular releases to [Maven Central](https://search.maven.org/search?q=g:org.pekko%20pekko-connectors-)
* milestone and release candidates (of major versions) to Maven Central
* @ref:[snapshots](snapshots.md) to [Sonatype](https://oss.sonatype.org/content/repositories/snapshots/org/pekko/)

### Compatibility

Our ambition is to evolve Apache Pekko Connectors modules without breaking users’ code. There are two sides to that: One is **binary-compatibility** which effectively means you can replace just the jar with a later version’s jar in your installation and everything will work. This becomes extremely important as soon as you use other libraries that rely on the same jar. They will continue to work without recompilation. The other is **source-compatibility** which, when upgrading to a later minor version, would not require any code changes. Apache Pekko and Apache Pekko Connectors strive for binary-compatibility and source-compatibility, but we do not guarantee source-compatibility.

Read about the details in the @extref:[Apache Pekko documentation](pekko:common/binary-compatibility-rules.html). 


### Mixing versions

**All connectors of Apache Pekko Connectors can be used independently**, you may mix Apache Pekko Connectors versions for different libraries. Some may share dependencies which could be incompatible, though (eg. Apache Pekko Connectors connectors to AWS services).


### Apache Pekko versions

With Apache Pekko though, it is important to be strictly using one version (never blend eg. `pekko-actor 1.0.21` and `pekko-stream 1.0.12`), and do not use a Pekko version lower than the one the Apache Pekko Connectors dependency requires (sometimes Apache Pekko Connectors modules depend on features of the latest Apache Pekko release).

Apache Pekko Connectors’s Pekko and Pekko HTTP dependencies are upgraded only if that version brings features leveraged by Apache Pekko Connectors or important fixes. As Apache Pekko itself is binary-compatible, the Pekko version may be upgraded with an Apache Pekko Connectors patch release.
See Apache Pekko's @extref:[Downstream upgrade strategy](pekko:project/downstream-upgrade-strategy.html) 

@@@ note 

Users are recommended to upgrade to the latest Apache Pekko version later than the one required by Apache Pekko Connectors at their convenience. 

Apache Pekko Connectors' nightly tests verify compatibility with the upcoming Apache Pekko's 1.0 release.

@@@


### Scala versions

The "Project information" section for every connector states which versions of Scala it is compiled with.


### Third-party dependencies

Apache Pekko Connectors depends heavily on third-party (non-Apache Pekko) libraries to integrate with other technologies via their client APIs. To keep up with the development within the technologies Apache Pekko Connectors integrates with, Apache Pekko Connectors connectors need to upgrade the client libraries regularly. 

Code using Apache Pekko Connectors will in many cases even make direct use of the client library and thus depend directly on the version Apache Pekko Connectors pulls into the project. Not all libraries apply the same rules for binary- and source-compatibility and it is very hard to track all of those rules. 

For this reason, any library upgrade must be made with care to not break user code in unexpected ways.

* An upgrade to a *patch release of a library* may be part of an Apache Pekko Connectors patch release (if nothing indicates the library upgrade would break user code).
* An upgrade to a *minor release of a library* may be part of a minor Apache Pekko Connectors release  (if nothing indicates the library upgrade would break user code).
* For *major release upgrades of a library*, the Apache Pekko Connectors team will decide case-by-case if the upgrade motivates an upgrade of the Apache Pekko Connectors major version. 
    * For source incompatible library upgrades, Apache Pekko Connectors will do a major version upgrade.
    * If the library indicates it is considered binary-compatible, Apache Pekko Connectors may upgrade within a minor release.

If a project needs to rely on the binary-compatibility rules to just replace the Apache Pekko Connectors dependency, care must be taken to replace any upgraded third-party library to the version used by Apache Pekko Connectors at the same time. This should normally work when overriding an Apache Pekko Connectors dependency of a library in a build dependency definition, as the build tool will do all the necessary evictions as long as evicted libraries are backwards binary-compatible.

