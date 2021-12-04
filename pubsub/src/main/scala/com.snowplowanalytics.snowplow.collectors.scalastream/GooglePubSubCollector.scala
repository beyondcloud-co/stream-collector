/*
 * Copyright (c) 2013-2021 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0, and
 * you may not use this file except in compliance with the Apache License
 * Version 2.0.  You may obtain a copy of the Apache License Version 2.0 at
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License Version 2.0 is distributed on an "AS
 * IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the Apache License Version 2.0 for the specific language
 * governing permissions and limitations there under.
 */
package com.snowplowanalytics.snowplow.collectors.scalastream

import cats.syntax.either._
import com.snowplowanalytics.snowplow.collectors.scalastream.generated.BuildInfo
import com.snowplowanalytics.snowplow.collectors.scalastream.model._
import com.snowplowanalytics.snowplow.collectors.scalastream.sinks.{GooglePubSubSink, Sink}
import com.snowplowanalytics.snowplow.collectors.scalastream.telemetry.TelemetryAkkaService

object GooglePubSubCollector extends Collector {
  def appName      = BuildInfo.moduleName
  def appVersion   = BuildInfo.version
  def scalaVersion = BuildInfo.scalaVersion

  type ThrowableOr[A] = Either[Throwable, A]

  def main(args: Array[String]): Unit = {
    val (collectorConf, akkaConf) = parseConfig(args)
    val telemetry                 = TelemetryAkkaService.initWithCollector(collectorConf, appName, appVersion)
    val sinks: Either[Throwable, CollectorSinks] = for {
      pc <- collectorConf.streams.sink match {
        case pc: GooglePubSub => pc.asRight
        case _                => new IllegalArgumentException("Configured sink is not PubSub").asLeft
      }
      goodStream = collectorConf.streams.good
      badStream  = collectorConf.streams.bad
      bufferConf = collectorConf.streams.buffer
      sinks <- Sink.throttled[ThrowableOr](
        collectorConf.streams.buffer,
        GooglePubSubSink.createAndInitialize(pc, bufferConf, goodStream, collectorConf.enableStartupChecks, _),
        GooglePubSubSink.createAndInitialize(pc, bufferConf, badStream, collectorConf.enableStartupChecks, _)
      )
    } yield sinks

    sinks match {
      case Right(s) => run(collectorConf, akkaConf, s, telemetry)
      case Left(e)  => throw e
    }
  }
}
