/*
 * Copyright (c) 2013-2022 Snowplow Analytics Ltd. All rights reserved.
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

import scala.concurrent.duration.FiniteDuration

import akka.http.scaladsl.model.headers.HttpCookiePair

import com.snowplowanalytics.snowplow.collectors.scalastream.sinks.Sink

import io.circe.Json

package model {

  import scala.concurrent.duration.DurationInt

  /**
    * Case class for holding both good and
    * bad sinks for the Stream Collector.
    */
  final case class CollectorSinks(good: Sink, bad: Sink)

  /**
    * Case class for holding the results of
    * splitAndSerializePayload.
    *
    * @param good All good results
    * @param bad All bad results
    */
  final case class EventSerializeResult(good: List[Array[Byte]], bad: List[Array[Byte]])

  /**
    * Class for the result of splitting a too-large array of events in the body of a POST request
    *
    * @param goodBatches List of batches of events
    * @param failedBigEvents List of events that were too large
    */
  final case class SplitBatchResult(goodBatches: List[List[Json]], failedBigEvents: List[Json])

  final case class CookieConfig(
    enabled: Boolean,
    name: String,
    expiration: FiniteDuration,
    domains: Option[List[String]],
    fallbackDomain: Option[String],
    secure: Boolean,
    httpOnly: Boolean,
    sameSite: Option[String]
  )
  final case class DoNotTrackCookieConfig(
    enabled: Boolean,
    name: String,
    value: String
  )
  final case class DntCookieMatcher(name: String, value: String) {
    private val pattern                                  = value.r.pattern
    def matches(httpCookiePair: HttpCookiePair): Boolean = pattern.matcher(httpCookiePair.value).matches()
  }
  final case class CookieBounceConfig(
    enabled: Boolean,
    name: String,
    fallbackNetworkUserId: String,
    forwardedProtocolHeader: Option[String]
  )
  final case class RedirectMacroConfig(
    enabled: Boolean,
    placeholder: Option[String]
  )
  final case class RootResponseConfig(
    enabled: Boolean,
    statusCode: Int,
    headers: Map[String, String] = Map.empty[String, String],
    body: String                 = ""
  )
  final case class P3PConfig(policyRef: String, CP: String)
  final case class CrossDomainConfig(enabled: Boolean, domains: List[String], secure: Boolean)
  final case class CORSConfig(accessControlMaxAge: FiniteDuration)
  final case class KinesisBackoffPolicyConfig(minBackoff: Long, maxBackoff: Long)
  final case class SqsBackoffPolicyConfig(minBackoff: Long, maxBackoff: Long)
  final case class GooglePubSubBackoffPolicyConfig(
    minBackoff: Long,
    maxBackoff: Long,
    totalBackoff: Long,
    multiplier: Double,
    initialRpcTimeout: Long,
    maxRpcTimeout: Long,
    rpcTimeoutMultiplier: Double
  )
  sealed trait SinkConfig
  final case class AWSConfig(accessKey: String, secretKey: String)
  final case class Kinesis(
    region: String,
    threadPoolSize: Int,
    aws: AWSConfig,
    backoffPolicy: KinesisBackoffPolicyConfig,
    customEndpoint: Option[String],
    sqsGoodBuffer: Option[String],
    sqsBadBuffer: Option[String]
  ) extends SinkConfig {
    val endpoint = customEndpoint.getOrElse(region match {
      case cn @ "cn-north-1"     => s"https://kinesis.$cn.amazonaws.com.cn"
      case cn @ "cn-northwest-1" => s"https://kinesis.$cn.amazonaws.com.cn"
      case _                     => s"https://kinesis.$region.amazonaws.com"
    })
  }
  final case class Sqs(region: String, threadPoolSize: Int, aws: AWSConfig, backoffPolicy: SqsBackoffPolicyConfig)
      extends SinkConfig
  final case class GooglePubSub(
    googleProjectId: String,
    backoffPolicy: GooglePubSubBackoffPolicyConfig
  ) extends SinkConfig
  final case class Kafka(
    brokers: String,
    retries: Int,
    producerConf: Option[Map[String, String]]
  ) extends SinkConfig
  final case class Pulsar(
    brokers: String,
  ) extends SinkConfig
  final case class Nsq(host: String, port: Int) extends SinkConfig
  case object Stdout extends SinkConfig
  final case class BufferConfig(byteLimit: Long, recordLimit: Long, timeLimit: Long)
  final case class StreamsConfig(
    good: String,
    bad: String,
    useIpAddressAsPartitionKey: Boolean,
    sink: SinkConfig,
    buffer: BufferConfig
  )
  final case class PrometheusMetricsConfig(
    enabled: Boolean,
    durationBucketsInSeconds: Option[List[Double]]
  )
  final case class TelemetryConfig(
    // General params
    disable: Boolean         = false,
    interval: FiniteDuration = 60.minutes,
    // http params
    method: String  = "POST",
    url: String     = "telemetry-g.snowplowanalytics.com",
    port: Int       = 443,
    secure: Boolean = true,
    // Params injected by deployment scripts
    userProvidedId: Option[String]  = None,
    moduleName: Option[String]      = None,
    moduleVersion: Option[String]   = None,
    instanceId: Option[String]      = None,
    autoGeneratedId: Option[String] = None
  )

  final case class SSLConfig(
    enable: Boolean   = false,
    redirect: Boolean = false,
    port: Int         = 443
  )
  final case class CollectorConfig(
    interface: String,
    port: Int,
    paths: Map[String, String],
    p3p: P3PConfig,
    crossDomain: CrossDomainConfig,
    cookie: CookieConfig,
    doNotTrackCookie: DoNotTrackCookieConfig,
    cookieBounce: CookieBounceConfig,
    redirectMacro: RedirectMacroConfig,
    rootResponse: RootResponseConfig,
    cors: CORSConfig,
    streams: StreamsConfig,
    prometheusMetrics: PrometheusMetricsConfig,
    telemetry: Option[TelemetryConfig],
    ssl: SSLConfig = SSLConfig(),
    enableDefaultRedirect: Boolean,
    redirectDomains: Set[String],
    enableStartupChecks: Boolean,
    terminationDeadline: FiniteDuration,
    preTerminationPeriod: FiniteDuration,
    preTerminationUnhealthy: Boolean
  ) {
    val cookieConfig = if (cookie.enabled) Some(cookie) else None
    val doNotTrackHttpCookie =
      if (doNotTrackCookie.enabled)
        Some(DntCookieMatcher(name = doNotTrackCookie.name, value = doNotTrackCookie.value))
      else
        None

    def cookieName       = cookieConfig.map(_.name)
    def cookieDomain     = cookieConfig.flatMap(_.domains)
    def fallbackDomain   = cookieConfig.flatMap(_.fallbackDomain)
    def cookieExpiration = cookieConfig.map(_.expiration)
  }

}
