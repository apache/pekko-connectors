/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, which was derived from Akka.
 */

/*
 * Copyright (C) since 2016 Lightbend Inc. <https://www.lightbend.com>
 */

package org.apache.pekko.stream.connectors.influxdb.impl

import java.lang.invoke.{ MethodHandle, MethodHandles, MethodType, VarHandle }
import java.time.Instant
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.util.function.Function
import java.util.concurrent.{ ConcurrentHashMap, ConcurrentMap }
import java.util.concurrent.TimeUnit

import org.apache.pekko
import pekko.annotation.InternalApi
import org.influxdb.InfluxDBMapperException
import org.influxdb.annotation.{ Column, Measurement }
import org.influxdb.dto.Point
import org.influxdb.dto.QueryResult

import scala.jdk.CollectionConverters._

/**
 * Internal API.
 */
@InternalApi
private[impl] class PekkoConnectorsResultMapperHelper {

  import PekkoConnectorsResultMapperHelper.{ CLASS_COLUMN_CACHE, CONSTRUCTOR_CACHE, ColumnInfo, LOOKUP }

  private val FRACTION_MIN_WIDTH = 0
  private val FRACTION_MAX_WIDTH = 9
  private val ADD_DECIMAL_POINT = true

  private val RFC3339_FORMATTER = new DateTimeFormatterBuilder()
    .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
    .appendFraction(ChronoField.NANO_OF_SECOND, FRACTION_MIN_WIDTH, FRACTION_MAX_WIDTH, ADD_DECIMAL_POINT)
    .appendZoneOrOffsetId
    .toFormatter

  private[impl] def databaseName(point: Class[?]): String =
    point.getAnnotation(classOf[Measurement]).database();

  private[impl] def retentionPolicy(point: Class[?]): String =
    point.getAnnotation(classOf[Measurement]).retentionPolicy();

  private[impl] def convertModelToPoint[T](model: T): Point = {
    throwExceptionIfMissingAnnotation(model.getClass)
    cacheClassFields(model.getClass)

    val colMap: ConcurrentMap[String, ColumnInfo] = CLASS_COLUMN_CACHE.get(model.getClass)

    try {
      val modelType = model.getClass();
      val measurement = measurementName(modelType);
      val timeUnit: TimeUnit = this.timeUnit(modelType);
      val time = timeUnit.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
      val pointBuilder: Point.Builder = Point.measurement(measurement).time(time, timeUnit);

      for (key <- colMap.keySet().asScala) {
        val colInfo = colMap.get(key)
        val value = colInfo.varHandle.get(model);

        if (colInfo.isTag) {
          pointBuilder.tag(colInfo.columnName, value.toString());
        } else if ("time".equals(colInfo.columnName)) {
          if (value != null) {
            setTime(pointBuilder, colInfo.fieldType, timeUnit, value);
          }
        } else {
          setField(pointBuilder, colInfo.fieldType, colInfo.columnName, value);
        }
      }

      pointBuilder.build();
    } catch {
      case e: IllegalArgumentException => throw new InfluxDBMapperException(e);
    }
  }

  private[impl] def cacheClassFields(clazz: Class[?]): Unit =
    CLASS_COLUMN_CACHE.computeIfAbsent(
      clazz,
      new Function[Class[?], ConcurrentMap[String, ColumnInfo]] {
        override def apply(clazz: Class[?]): ConcurrentMap[String, ColumnInfo] =
          PekkoConnectorsResultMapperHelper.classColumnMap(clazz)
      })

  private[impl] def parseSeriesAs[T](clazz: Class[T], series: QueryResult.Series, precision: TimeUnit): List[T] = {
    cacheClassFields(clazz)
    series.getValues.asScala
      .map((v: java.util.List[AnyRef]) => parseRowAs(clazz, series.getColumns, v, precision))
      .toList
  }

  private def measurementName(point: Class[?]): String =
    point.getAnnotation(classOf[Measurement]).name();

  private def timeUnit(point: Class[?]): TimeUnit =
    point.getAnnotation(classOf[Measurement]).timeUnit()

  private def setTime(pointBuilder: Point.Builder, fieldType: Class[?], timeUnit: TimeUnit, value: Any): Unit =
    if (classOf[Instant].isAssignableFrom(fieldType)) {
      val instant = value.asInstanceOf[Instant]
      val time = timeUnit.convert(instant.toEpochMilli, TimeUnit.MILLISECONDS)
      pointBuilder.time(time, timeUnit)
    } else throw new InfluxDBMapperException("Unsupported type " + fieldType + " for time: should be of Instant type")

  private def setField(pointBuilder: Point.Builder, fieldType: Class[?], columnName: String, value: Any): Unit =
    if (classOf[java.lang.Boolean].isAssignableFrom(fieldType) || classOf[Boolean].isAssignableFrom(fieldType))
      pointBuilder.addField(columnName, value.asInstanceOf[Boolean])
    else if (classOf[java.lang.Long].isAssignableFrom(fieldType) || classOf[Long].isAssignableFrom(fieldType))
      pointBuilder.addField(columnName, value.asInstanceOf[Long])
    else if (classOf[java.lang.Double].isAssignableFrom(fieldType) || classOf[Double].isAssignableFrom(fieldType))
      pointBuilder.addField(columnName, value.asInstanceOf[Double])
    else if (classOf[java.lang.Integer].isAssignableFrom(fieldType) || classOf[Integer].isAssignableFrom(fieldType))
      pointBuilder.addField(columnName, value.asInstanceOf[Int])
    else if (classOf[String].isAssignableFrom(fieldType)) pointBuilder.addField(columnName, value.asInstanceOf[String])
    else throw new InfluxDBMapperException("Unsupported type " + fieldType + " for column " + columnName)

  private def throwExceptionIfMissingAnnotation(clazz: Class[?]): Unit =
    if (!clazz.isAnnotationPresent(classOf[Measurement]))
      throw new IllegalArgumentException(
        "Class " + clazz.getName + " is not annotated with @" + classOf[Measurement].getSimpleName)

  private def getOrCreateConstructorHandle(clazz: Class[?]): MethodHandle =
    CONSTRUCTOR_CACHE.computeIfAbsent(
      clazz,
      new Function[Class[?], MethodHandle] {
        override def apply(clazz: Class[?]): MethodHandle =
          MethodHandles.privateLookupIn(clazz, LOOKUP).findConstructor(clazz, MethodType.methodType(Void.TYPE))
      })

  private def parseRowAs[T](clazz: Class[T],
      columns: java.util.List[String],
      values: java.util.List[AnyRef],
      precision: TimeUnit): T =
    try {
      val colMap = CLASS_COLUMN_CACHE.get(clazz)

      val handle = getOrCreateConstructorHandle(clazz)
      val obj: T = handle.invokeWithArguments().asInstanceOf[T]
      for (i <- 0 until columns.size()) {
        val colInfo = colMap.get(columns.get(i))
        if (colInfo != null) {
          setFieldValue(obj, colInfo, values.get(i), precision)
        }
      }
      obj
    } catch {
      case e: InfluxDBMapperException => throw e
      case e: Exception               =>
        throw new InfluxDBMapperException(e)
    }

  private def setFieldValue[T](obj: T, colInfo: ColumnInfo, value: Any, precision: TimeUnit): Unit = {
    if (value == null) return
    val fieldType = colInfo.fieldType
    try {
      if (fieldValueModified(fieldType, colInfo, obj, value, precision) ||
        fieldValueForPrimitivesModified(fieldType, colInfo, obj, value) ||
        fieldValueForPrimitiveWrappersModified(fieldType, colInfo, obj, value)) return
      val msg =
        s"""Class '${obj.getClass.getName}' field '${colInfo.fieldName}' is from an unsupported type '${colInfo.fieldType}'."""
      throw new InfluxDBMapperException(msg)
    } catch {
      case e: ClassCastException =>
        val msg =
          s"""Class '${obj.getClass.getName}' field '${colInfo.fieldName}' was defined with a different field type and caused a ClassCastException.
             |The correct type is '${value.getClass.getName}' (current field value: '${value}')""".stripMargin
        throw new InfluxDBMapperException(msg)
    }
  }

  private def fieldValueForPrimitivesModified[T](
      fieldType: Class[?],
      colInfo: ColumnInfo,
      obj: T,
      value: Any): Boolean =
    if (classOf[Double].isAssignableFrom(fieldType)) {
      colInfo.varHandle.set(obj, value.asInstanceOf[Double].doubleValue)
      true
    } else if (classOf[Long].isAssignableFrom(fieldType)) {
      colInfo.varHandle.set(obj, value.asInstanceOf[Double].longValue)
      true
    } else if (classOf[Int].isAssignableFrom(fieldType)) {
      colInfo.varHandle.set(obj, value.asInstanceOf[Double].intValue)
      true
    } else if (classOf[Boolean].isAssignableFrom(fieldType)) {
      colInfo.varHandle.set(obj, String.valueOf(value).toBoolean)
      true
    } else {
      false
    }

  private def fieldValueForPrimitiveWrappersModified[T](
      fieldType: Class[?],
      colInfo: ColumnInfo,
      obj: T,
      value: Any): Boolean =
    if (classOf[java.lang.Double].isAssignableFrom(fieldType)) {
      colInfo.varHandle.set(obj, value)
      true
    } else if (classOf[java.lang.Long].isAssignableFrom(fieldType)) {
      colInfo.varHandle.set(obj, value.asInstanceOf[Double].longValue())
      true
    } else if (classOf[Integer].isAssignableFrom(fieldType)) {
      colInfo.varHandle.set(obj, value.asInstanceOf[java.lang.Integer])
      true
    } else if (classOf[java.lang.Boolean].isAssignableFrom(fieldType)) {
      colInfo.varHandle.set(obj, value.asInstanceOf[java.lang.Boolean])
      true
    } else {
      false
    }

  private def fieldValueModified[T](
      fieldType: Class[?],
      colInfo: ColumnInfo,
      obj: T,
      value: Any,
      precision: TimeUnit): Boolean =
    if (classOf[String].isAssignableFrom(fieldType)) {
      colInfo.varHandle.set(obj, String.valueOf(value))
      true
    } else if (classOf[Instant].isAssignableFrom(fieldType)) {
      val instant: Instant = getInstant(colInfo, value, precision)
      colInfo.varHandle.set(obj, instant)
      true
    } else {
      false
    }

  private def getInstant(colInfo: ColumnInfo, value: Any, precision: TimeUnit): Instant =
    if (value.isInstanceOf[String]) Instant.from(RFC3339_FORMATTER.parse(String.valueOf(value)))
    else if (value.isInstanceOf[java.lang.Long]) Instant.ofEpochMilli(toMillis(value.asInstanceOf[Long], precision))
    else if (value.isInstanceOf[java.lang.Double])
      Instant.ofEpochMilli(toMillis(value.asInstanceOf[java.lang.Double].longValue, precision))
    else if (value.isInstanceOf[java.lang.Integer])
      Instant.ofEpochMilli(toMillis(value.asInstanceOf[Integer].longValue, precision))
    else {
      throw new InfluxDBMapperException(s"""Unsupported type for field ${colInfo.fieldName}""")
    }

  private def toMillis(value: Long, precision: TimeUnit) = TimeUnit.MILLISECONDS.convert(value, precision)

}

@InternalApi
private[impl] object PekkoConnectorsResultMapperHelper {
  private val CLASS_COLUMN_CACHE: ConcurrentHashMap[Class[?], ConcurrentMap[String, ColumnInfo]] =
    new ConcurrentHashMap()

  private val CONSTRUCTOR_CACHE: ConcurrentHashMap[Class[?], MethodHandle] = new ConcurrentHashMap()

  private val LOOKUP = MethodHandles.lookup()

  final class ColumnInfo(
      val columnName: String,
      val isTag: Boolean,
      val fieldType: Class[?],
      val varHandle: VarHandle,
      val fieldName: String)

  private def classColumnMap(clazz: Class[?]): ConcurrentMap[String, ColumnInfo] = {
    val columnMap: ConcurrentMap[String, ColumnInfo] = new ConcurrentHashMap()
    var c = clazz

    while (c != null) {
      val fields = c.getDeclaredFields
      if (fields.nonEmpty) {
        val privateLookup = MethodHandles.privateLookupIn(c, LOOKUP)
        for (field <- fields) {
          val colAnnotation = field.getAnnotation(classOf[Column])
          if (colAnnotation != null) {
            val varHandle = privateLookup.findVarHandle(c, field.getName, field.getType)
            val colInfo = new ColumnInfo(
              colAnnotation.name(),
              colAnnotation.tag(),
              field.getType,
              varHandle,
              field.getName)
            columnMap.put(colAnnotation.name(), colInfo)
          }
        }
      }
      c = c.getSuperclass
    }

    columnMap
  }
}
