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

package org.apache.pekko.stream.connectors.testkit.javadsl

import org.apache.pekko.stream.connectors.testkit.CapturingAppender
import org.junit.jupiter.api.extension.{ AfterTestExecutionCallback, BeforeTestExecutionCallback, ExtensionContext }

/**
 * See https://pekko.apache.org/docs/pekko/current/typed/testing-async.html#silence-logging-output-from-tests
 *
 * JUnit Jupiter extension to make log lines appear only when the test failed.
 *
 * Use this in test by adding `@ExtendWith` annotation:
 * {{{
 *   @ExtendWith(LogCapturingExtension.class)
 * }}}
 *
 * Requires Logback and configuration like the following the logback-test.xml:
 *
 * {{{
 *     <appender name="CapturingAppender" class="org.apache.pekko.actor.testkit.typed.internal.CapturingAppender" />
 *
 *     <logger name="org.apache.pekko.actor.testkit.typed.internal.CapturingAppenderDelegate" >
 *       <appender-ref ref="STDOUT"/>
 *     </logger>
 *
 *     <root level="DEBUG">
 *         <appender-ref ref="CapturingAppender"/>
 *     </root>
 * }}}
 */
class LogCapturingExtension extends BeforeTestExecutionCallback with AfterTestExecutionCallback {

  // eager access of CapturingAppender to fail fast if misconfigured
  private val capturingAppender = CapturingAppender.get("")

  override def beforeTestExecution(context: ExtensionContext): Unit = {
    capturingAppender.clear()
  }

  override def afterTestExecution(context: ExtensionContext): Unit = {
    if (context.getExecutionException.isPresent) {
      val error = context.getExecutionException.get().toString
      val method =
        s"[${Console.BLUE}${context.getRequiredTestClass.getName}: ${context.getRequiredTestMethod.getName}${Console.RESET}]"
      System.out.println(
        s"--> $method Start of log messages of test that failed with $error")
      capturingAppender.flush()
      System.out.println(
        s"<-- $method End of log messages of test that failed with $error")
    }
  }
}
