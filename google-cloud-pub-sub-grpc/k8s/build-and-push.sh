#!/bin/bash
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

set -euo pipefail

PROJECT_ID="pekko-connectors"
REGION="us-central1"
REPO="pekko-test"
IMAGE="${REGION}-docker.pkg.dev/${PROJECT_ID}/${REPO}/gke-auth-test:latest"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"

echo "=== Creating Artifact Registry repo (if needed) ==="
gcloud artifacts repositories create "${REPO}" \
  --repository-format=docker \
  --location="${REGION}" \
  --project="${PROJECT_ID}" 2>/dev/null || true

echo "=== Configuring Docker auth ==="
gcloud auth configure-docker "${REGION}-docker.pkg.dev" --quiet

echo "=== Building with sbt ==="
cd "${ROOT_DIR}"

# Compile the GKE test app along with the connector
# We need to compile it as part of the project to get all dependencies
mkdir -p google-cloud-pub-sub-grpc/src/main/scala/org/apache/pekko/stream/connectors/googlecloud/pubsub/grpc/gke
cp "${SCRIPT_DIR}/GkeAuthTest.scala" google-cloud-pub-sub-grpc/src/main/scala/org/apache/pekko/stream/connectors/googlecloud/pubsub/grpc/gke/
cp "${SCRIPT_DIR}/GkeFullFeatureTest.scala" google-cloud-pub-sub-grpc/src/main/scala/org/apache/pekko/stream/connectors/googlecloud/pubsub/grpc/gke/
cp "${SCRIPT_DIR}/application.conf" google-cloud-pub-sub-grpc/src/main/resources/gke-application.conf

sbt "google-cloud-pub-sub-grpc/compile"

# Export classpath and build the jar
echo "=== Packaging ==="
FULL_CP=$(sbt --error "print google-cloud-pub-sub-grpc/fullClasspath" | tr ',' '\n' | sed 's/.*Attributed(\(.*\))/\1/')
CLASSES_DIR=$(sbt --error "print google-cloud-pub-sub-grpc/classDirectory" | tr -d '[:space:]')

# Create staging directory
STAGING="${SCRIPT_DIR}/staging"
rm -rf "${STAGING}"
mkdir -p "${STAGING}/lib"

# Copy the compiled classes as a jar
cd "${CLASSES_DIR}"
jar cf "${STAGING}/gke-auth-test.jar" .
cd "${ROOT_DIR}"

# Copy dependency jars and package inter-project class directories as jars
echo "${FULL_CP}" | tr ':' '\n' | while read -r entry; do
  entry=$(echo "$entry" | tr -d '[:space:]')
  [ -z "$entry" ] && continue
  if [ -f "$entry" ] && echo "$entry" | grep -q '\.jar$'; then
    # External jar dependency
    cp "$entry" "${STAGING}/lib/"
  elif [ -d "$entry" ] && [ "$entry" != "$CLASSES_DIR" ]; then
    # Inter-project classes directory — package as a jar
    dir_name=$(echo "$entry" | sed 's|.*/\([^/]*\)/target/.*|\1|')
    cd "$entry"
    jar cf "${STAGING}/lib/${dir_name}.jar" .
    cd "${ROOT_DIR}"
  fi
done

# Copy the GKE-specific application.conf into the jar
cd "${STAGING}"
mkdir -p tmp
cd tmp
jar xf ../gke-auth-test.jar
cp "${SCRIPT_DIR}/application.conf" .
jar cf ../gke-auth-test.jar .
cd "${STAGING}"
rm -rf tmp

echo "=== Building Docker image ==="
cp "${SCRIPT_DIR}/Dockerfile" "${STAGING}/"
docker build -t "${IMAGE}" "${STAGING}"

echo "=== Pushing to Artifact Registry ==="
docker push "${IMAGE}"

echo "=== Cleaning up ==="
rm -rf "${STAGING}"
rm -f "${ROOT_DIR}/google-cloud-pub-sub-grpc/src/main/scala/org/apache/pekko/stream/connectors/googlecloud/pubsub/grpc/gke/GkeAuthTest.scala"
rm -f "${ROOT_DIR}/google-cloud-pub-sub-grpc/src/main/scala/org/apache/pekko/stream/connectors/googlecloud/pubsub/grpc/gke/GkeFullFeatureTest.scala"
rm -f "${ROOT_DIR}/google-cloud-pub-sub-grpc/src/main/resources/gke-application.conf"

echo "=== Done: ${IMAGE} ==="
