/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, which was derived from Akka.
 */

package org.apache.pekko.stream.connectors.s3.scaladsl

import org.scalacheck.Gen

import scala.annotation.nowarn
import scala.language.postfixOps
object Generators {
  val MaxBucketLength: Int = 63

  // See https://docs.aws.amazon.com/AmazonS3/latest/userguide/bucketnamingrules.html for valid
  // bucketnames

  lazy val bucketLetterOrNumberCharGen: Gen[Char] = Gen.frequency(
    (1, Gen.numChar),
    (1, Gen.alphaLowerChar))

  def bucketAllCharGen(useVirtualDotHost: Boolean): Gen[Char] = {
    val base = List(
      (10, Gen.alphaLowerChar),
      (1, Gen.const('-')),
      (1, Gen.numChar))

    val frequency = if (useVirtualDotHost) (1, Gen.const('.')) +: base else base

    Gen.frequency(frequency: _*)
  }

  @nowarn("msg=not.*?exhaustive")
  private def checkInvalidDuplicateChars(chars: List[Char]): Boolean =
    chars.sliding(2).forall { case Seq(before, after) =>
      !(before == '.' && after == '.' || before == '-' && after == '.' || before == '.' && after == '-')
    }

  private def checkAlphaChar(c: Char): Boolean =
    c >= 'a' && c <= 'z'

  private def allCharCheck(useVirtualDotHost: Boolean, string: String): Boolean =
    if (useVirtualDotHost) {
      string.forall(char => Character.isDigit(char) || checkAlphaChar(char) || char == '-' || char == '.') &&
      checkInvalidDuplicateChars(string.toList)
    } else
      string.forall(char => Character.isDigit(char) || checkAlphaChar(char) || char == '-')

  def validatePrefix(useVirtualDotHost: Boolean, prefix: Option[String]): Option[String] = {
    val withoutWhitespace = prefix match {
      case Some(value) if value.trim == "" => None
      case Some(value)                     => Some(value)
      case None                            => None
    }

    withoutWhitespace match {
      case Some(value) if !(Character.isDigit(value.head) || checkAlphaChar(value.head)) =>
        throw new IllegalArgumentException(
          s"Invalid starting digit for prefix $value, ${value.head} needs to be an alpha char or digit")
      case Some(value) if value.length > 1 =>
        if (!allCharCheck(useVirtualDotHost, value.drop(1)))
          throw new IllegalArgumentException(
            s"Prefix $value contains invalid characters")
      case Some(value) if value.length > MaxBucketLength - 1 =>
        throw new IllegalArgumentException(
          s"Prefix is too long, it has size ${value.length} where as the max bucket size is $MaxBucketLength")
      case _ => ()
    }

    withoutWhitespace
  }

  def bucketNameGen(useVirtualDotHost: Boolean, prefix: Option[String] = None): Gen[String] = {
    val finalPrefix = validatePrefix(useVirtualDotHost, prefix)

    for {
      range <- {
        val maxLength = finalPrefix match {
          case Some(p) => MaxBucketLength - p.length
          case None    => MaxBucketLength
        }

        if (maxLength > 3)
          Gen.choose(3, maxLength)
        else
          Gen.const(maxLength)
      }
      startString = finalPrefix.getOrElse("")

      bucketName <- range match {
        case 3 =>
          for {
            first <- bucketLetterOrNumberCharGen
            second <- bucketAllCharGen(useVirtualDotHost)
            third <- bucketLetterOrNumberCharGen
          } yield startString ++ List(first, second, third).mkString
        case _ =>
          for {
            first <- bucketLetterOrNumberCharGen
            last <- bucketLetterOrNumberCharGen
            middle <- {
              val gen = Gen.listOfN(range - 2, bucketAllCharGen(useVirtualDotHost))
              if (useVirtualDotHost) gen.filter(checkInvalidDuplicateChars) else gen
            }
          } yield startString ++ first.toString ++ middle.mkString ++ last.toString
      }
    } yield bucketName
  }

}
