# Snapshots

Snapshots are published to the Sonatype Snapshot repository after every successful build on 'main' branch.
Add the following to your project build definition to resolve Apache Pekko Connectors snapshots:

## Configure repository

Maven
:   ```xml
    <project>
    ...
      <repositories>
        <repository>
            <id>snapshots-repo</id>
            <name>Apache snapshots</name>
            <url>https://repository.apache.org/content/groups/snapshots</url>
        </repository>
      </repositories>
    ...
    </project>
    ```

sbt
:   ```scala
    resolvers += "Apache Staging" at "https://repository.apache.org/content/groups/snapshots"
    resolvers += Resolver.ApacheMavenSnapshotsRepo // use this if you are using sbt 1.9.0 or above
    ```

Gradle
:   ```gradle
    repositories {
      maven {
        url  "https://repository.apache.org/content/groups/snapshots"
      }
    }
    ```

## Documentation

The [snapshot documentation](https://pekko.apache.org/docs/pekko-connectors/snapshot/) is updated with every snapshot build.


## Versions

To find the latest published snapshot version, have a look at https://repository.apache.org/content/groups/snapshots/org/apache/pekko/pekko-connectors-csv_2.13/

The snapshot repository is cleaned from time to time with no further notice. Check https://repository.apache.org/content/groups/snapshots/org/apache/pekko/ to see what versions are currently available.
