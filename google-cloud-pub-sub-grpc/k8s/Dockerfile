# Licensed to the Apache Software Foundation (ASF) under one or more license agreements; and to You under the Apache License, version 2.0: https://www.apache.org/licenses/LICENSE-2.0
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY lib/ lib/
COPY gke-auth-test.jar .

ENTRYPOINT ["java", "-cp", "gke-auth-test.jar:lib/*", "org.apache.pekko.stream.connectors.googlecloud.pubsub.grpc.gke.GkeAuthTest"]
