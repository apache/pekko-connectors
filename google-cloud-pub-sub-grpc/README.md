# Google Cloud Pub/Sub gRPC Connector - GCP Integration Testing

## Overview

The integration tests in this module run against the Pub/Sub emulator by default.
The emulator does not require authentication, so it does not exercise the
`google-application-default` credential provider or any authenticated code paths.

To verify that authentication works end-to-end, you must run the tests against a
real GCP project.

## Prerequisites

- [Google Cloud SDK](https://cloud.google.com/sdk/docs/install) (`gcloud` CLI)
- A GCP project with billing enabled
- Pub/Sub API enabled on that project

## GCP Setup

```bash
# Set your project
gcloud config set project <YOUR_PROJECT_ID>

# Enable the Pub/Sub API
gcloud services enable pubsub.googleapis.com

# Create topics and subscriptions used by the integration tests
gcloud pubsub topics create simpleTopic
gcloud pubsub subscriptions create simpleSubscription --topic=simpleTopic

gcloud pubsub topics create testTopic
gcloud pubsub subscriptions create testSubscription --topic=testTopic

# Authenticate for application-default credentials
gcloud auth application-default login
```

## Application Config

Create or update `src/test/resources/application.conf` to point to real GCP
instead of the emulator. Comment out the emulator overrides and configure the
credential provider:

```hocon
pekko.connectors.google.cloud.pubsub.grpc {
  # Comment out or remove these emulator settings:
  # host = "localhost"
  # port = 8538
  # use-tls = false
  # callCredentials = "none"
}

pekko.connectors.google {
  credentials {
    provider = google-application-default
    google-application-default {
      project-id = "<YOUR_PROJECT_ID>"
    }
  }
}
```

You also need to update the `projectId` in `IntegrationSpec.scala` (and the Java
`IntegrationTest`) to match your GCP project ID, or extract it into config.

## Running the Tests

```bash
sbt "google-cloud-pub-sub-grpc/test"
```

## Cleanup

```bash
gcloud pubsub subscriptions delete simpleSubscription
gcloud pubsub subscriptions delete testSubscription
gcloud pubsub topics delete simpleTopic
gcloud pubsub topics delete testTopic
```

## Test Report

| Date | Environment | Credential Provider | Result | Notes |
|------|-------------|---------------------|--------|-------|
| 2026-03-13 | GCP (project: pekko-connectors-test) | google-application-default | 14/14 passed | Scala 8/8, Java 6/6. User credentials via `gcloud auth application-default login`. |

After running against real GCP, add a row to the table above to record the result.