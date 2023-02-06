/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, derived from Akka.
 */

/*
 * Copyright (C) since 2016 Lightbend Inc. <https://www.lightbend.com>
 */

package docs.javadsl;

// #imports

import org.apache.pekko.Done;
import org.apache.pekko.NotUsed;
import org.apache.pekko.http.javadsl.marshallers.jackson.Jackson;
import org.apache.pekko.http.javadsl.marshalling.Marshaller;
import org.apache.pekko.http.javadsl.model.HttpEntity;
import org.apache.pekko.http.javadsl.model.RequestEntity;
import org.apache.pekko.http.javadsl.unmarshalling.Unmarshaller;
import org.apache.pekko.stream.connectors.google.GoogleAttributes;
import org.apache.pekko.stream.connectors.google.GoogleSettings;
import org.apache.pekko.stream.connectors.googlecloud.bigquery.InsertAllRetryPolicy;
import org.apache.pekko.stream.connectors.googlecloud.bigquery.javadsl.BigQuery;
import org.apache.pekko.stream.connectors.googlecloud.bigquery.javadsl.jackson.BigQueryMarshallers;
import org.apache.pekko.stream.connectors.googlecloud.bigquery.model.Dataset;
import org.apache.pekko.stream.connectors.googlecloud.bigquery.model.Job;
import org.apache.pekko.stream.connectors.googlecloud.bigquery.model.JobReference;
import org.apache.pekko.stream.connectors.googlecloud.bigquery.model.JobState;
import org.apache.pekko.stream.connectors.googlecloud.bigquery.model.QueryResponse;
import org.apache.pekko.stream.connectors.googlecloud.bigquery.model.Table;
import org.apache.pekko.stream.connectors.googlecloud.bigquery.model.TableDataInsertAllRequest;
import org.apache.pekko.stream.connectors.googlecloud.bigquery.model.TableDataListResponse;
import org.apache.pekko.stream.connectors.googlecloud.bigquery.model.TableFieldSchema;
import org.apache.pekko.stream.connectors.googlecloud.bigquery.model.TableFieldSchemaMode;
import org.apache.pekko.stream.connectors.googlecloud.bigquery.model.TableFieldSchemaType;
import org.apache.pekko.stream.connectors.googlecloud.bigquery.model.TableListResponse;
import org.apache.pekko.stream.connectors.googlecloud.bigquery.model.TableSchema;
import org.apache.pekko.stream.javadsl.Flow;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.stream.javadsl.Source;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;
// #imports

public class BigQueryDoc {

  org.apache.pekko.actor.ActorSystem system = null;

  // #setup
  ObjectMapper objectMapper = new ObjectMapper();

  public class Person {
    private String name;
    private Integer age;
    private List<Address> addresses;
    private Boolean isHakker;

    @JsonCreator
    public Person(@JsonProperty("f") JsonNode fields) throws IOException {
      name = fields.get(0).get("v").textValue();
      age = Integer.parseInt(fields.get(1).get("v").textValue());
      addresses = new ArrayList<>();
      ObjectReader addressReader = objectMapper.readerFor(Address.class);
      for (JsonNode node : fields.get(2).get("v")) {
        Address address = addressReader.readValue(node.get("v"));
        addresses.add(address);
      }
      isHakker = Boolean.parseBoolean(fields.get(3).get("v").textValue());
    }

    public String getName() {
      return name;
    }

    public Integer getAge() {
      return age;
    }

    public List<Address> getAddresses() {
      return addresses;
    }

    public Boolean getIsHakker() {
      return isHakker;
    }
  }

  public class Address {
    private String street;
    private String city;
    private Integer postalCode;

    @JsonCreator
    public Address(@JsonProperty("f") JsonNode fields) {
      street = fields.get(0).get("v").textValue();
      city = fields.get(1).get("v").textValue();
      postalCode =
          Optional.of(fields.get(2).get("v").textValue()).map(Integer::parseInt).orElse(null);
    }

    public String getStreet() {
      return street;
    }

    public String getCity() {
      return city;
    }

    public Integer getPostalCode() {
      return postalCode;
    }
  }

  public class NameAddressesPair {
    private String name;
    private List<Address> addresses;

    @JsonCreator
    public NameAddressesPair(@JsonProperty("f") JsonNode fields) throws IOException {
      name = fields.get(0).get("v").textValue();
      addresses = new ArrayList<>();
      ObjectReader addressReader = objectMapper.readerFor(Address.class);
      for (JsonNode node : fields.get(1).get("v")) {
        Address address = addressReader.readValue(node.get("v"));
        addresses.add(address);
      }
    }
  }
  // #setup

  String datasetId;
  String tableId;

  void docs() {

    // #run-query
    String sqlQuery =
        String.format("SELECT name, addresses FROM %s.%s WHERE age >= 100", datasetId, tableId);
    Unmarshaller<HttpEntity, QueryResponse<NameAddressesPair>> queryResponseUnmarshaller =
        BigQueryMarshallers.queryResponseUnmarshaller(NameAddressesPair.class);
    Source<NameAddressesPair, CompletionStage<QueryResponse<NameAddressesPair>>> centenarians =
        BigQuery.query(sqlQuery, false, false, queryResponseUnmarshaller);
    // #run-query

    // #dry-run-query
    Source<NameAddressesPair, CompletionStage<QueryResponse<NameAddressesPair>>>
        centenariansDryRun = BigQuery.query(sqlQuery, false, false, queryResponseUnmarshaller);
    CompletionStage<Long> bytesProcessed =
        centenariansDryRun
            .to(Sink.ignore())
            .run(system)
            .thenApply(r -> r.getTotalBytesProcessed().getAsLong());
    // #dry-run-query

    // #table-data
    Unmarshaller<HttpEntity, TableDataListResponse<Person>> tableDataListUnmarshaller =
        BigQueryMarshallers.tableDataListResponseUnmarshaller(Person.class);
    Source<Person, CompletionStage<TableDataListResponse<Person>>> everyone =
        BigQuery.listTableData(
            datasetId,
            tableId,
            OptionalLong.empty(),
            OptionalInt.empty(),
            Collections.emptyList(),
            tableDataListUnmarshaller);
    // #table-data

    // #streaming-insert
    Marshaller<TableDataInsertAllRequest<Person>, RequestEntity> tableDataInsertAllMarshaller =
        BigQueryMarshallers.tableDataInsertAllRequestMarshaller();
    Sink<List<Person>, NotUsed> peopleInsertSink =
        BigQuery.insertAll(
            datasetId,
            tableId,
            InsertAllRetryPolicy.withDeduplication(),
            Optional.empty(),
            tableDataInsertAllMarshaller);
    // #streaming-insert

    // #async-insert
    Flow<Person, Job, NotUsed> peopleLoadFlow =
        BigQuery.insertAllAsync(datasetId, tableId, Jackson.marshaller());
    // #async-insert

    List<Person> people = null;

    // #job-status
    Function<List<JobReference>, CompletionStage<Boolean>> checkIfJobsDone =
        jobReferences -> {
          GoogleSettings settings = GoogleSettings.create(system);
          CompletionStage<Boolean> allAreDone = CompletableFuture.completedFuture(true);
          for (JobReference jobReference : jobReferences) {
            CompletionStage<Job> job =
                BigQuery.getJob(jobReference.getJobId().get(), Optional.empty(), settings, system);
            CompletionStage<Boolean> jobIsDone =
                job.thenApply(
                    j ->
                        j.getStatus().map(s -> s.getState().equals(JobState.done())).orElse(false));
            allAreDone = allAreDone.thenCombine(jobIsDone, (a, b) -> a & b);
          }
          return allAreDone;
        };

    CompletionStage<List<Job>> jobs =
        Source.from(people).via(peopleLoadFlow).runWith(Sink.<Job>seq(), system);
    CompletionStage<List<JobReference>> jobReferences =
        jobs.thenApply(
            js -> js.stream().map(j -> j.getJobReference().get()).collect(Collectors.toList()));
    CompletionStage<Boolean> isDone = jobReferences.thenCompose(checkIfJobsDone);
    // #job-status

    // #dataset-methods
    GoogleSettings settings = GoogleSettings.create(system);
    Source<Dataset, NotUsed> allDatasets =
        BigQuery.listDatasets(OptionalInt.empty(), Optional.empty(), Collections.emptyMap());
    CompletionStage<Dataset> existingDataset = BigQuery.getDataset(datasetId, settings, system);
    CompletionStage<Dataset> newDataset = BigQuery.createDataset("newDatasetId", settings, system);
    CompletionStage<Done> datasetDeleted =
        BigQuery.deleteDataset(datasetId, false, settings, system);
    // #dataset-methods

    // #table-methods
    Source<Table, CompletionStage<TableListResponse>> allTablesInDataset =
        BigQuery.listTables(datasetId, OptionalInt.empty());
    CompletionStage<Table> existingTable = BigQuery.getTable(datasetId, tableId, settings, system);
    CompletionStage<Done> tableDeleted = BigQuery.deleteTable(datasetId, tableId, settings, system);
    // #table-methods

    // #create-table
    TableSchema personSchema =
        TableSchema.create(
            TableFieldSchema.create("name", TableFieldSchemaType.string(), Optional.empty()),
            TableFieldSchema.create("age", TableFieldSchemaType.integer(), Optional.empty()),
            TableFieldSchema.create(
                "addresses",
                TableFieldSchemaType.record(),
                Optional.of(TableFieldSchemaMode.repeated()),
                TableFieldSchema.create("street", TableFieldSchemaType.string(), Optional.empty()),
                TableFieldSchema.create("city", TableFieldSchemaType.string(), Optional.empty()),
                TableFieldSchema.create(
                    "postalCode",
                    TableFieldSchemaType.integer(),
                    Optional.of(TableFieldSchemaMode.nullable()))),
            TableFieldSchema.create("isHakker", TableFieldSchemaType.bool(), Optional.empty()));
    CompletionStage<Table> newTable =
        BigQuery.createTable(datasetId, "newTableId", personSchema, settings, system);
    // #create-table

    // #custom-settings
    GoogleSettings defaultSettings = GoogleSettings.create(system);
    GoogleSettings customSettings = defaultSettings.withProjectId("myOtherProjectId");
    BigQuery.query(sqlQuery, false, false, queryResponseUnmarshaller)
        .withAttributes(GoogleAttributes.settings(customSettings));
    // #custom-settings
  }
}
