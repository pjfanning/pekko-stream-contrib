/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */

package org.apache.pekko.stream.contrib

import java.util.concurrent.ThreadFactory

import org.apache.pekko.event.LoggingAdapter
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.stream.testkit.scaladsl.TestSink
import com.github.pjfanning.pekko.scheduler.mock.{ MockScheduler, VirtualTime }
import com.typesafe.config.{ Config, ConfigFactory }

import scala.concurrent.duration._

class PekkoMockScheduler extends {
      val time = new VirtualTime
    } with MockScheduler(time) {
  def this(config: Config, adapter: LoggingAdapter, tf: ThreadFactory) = this()
}

class TimeWindowSpec extends BaseStreamSpec {

  override def config = ConfigFactory.parseString(s"""
      |pekko.scheduler.implementation = ${classOf[PekkoMockScheduler].getName}
    """.stripMargin)

  private val timeWindow = 100.millis
  private val epsilonTime = 10.millis

  private val scheduler = system.scheduler.asInstanceOf[PekkoMockScheduler]

  "TimeWindow flow" should {
    "aggregate data for predefined amount of time" in {
      val summingWindow = TimeWindow(timeWindow, eager = false)(identity[Int])(_ + _)

      val sub = Source
        .repeat(1)
        .via(summingWindow)
        .runWith(TestSink.probe)

      sub.request(2)

      sub.expectNoMsg(timeWindow + epsilonTime)
      scheduler.time.advance(timeWindow + epsilonTime)
      scheduler.tick()
      sub.expectNext()

      sub.expectNoMsg(timeWindow + epsilonTime)
      scheduler.time.advance(timeWindow + epsilonTime)
      scheduler.tick()
      sub.expectNext()
    }

    "emit the first seed if eager" in {
      val summingWindow = TimeWindow(timeWindow, eager = true)(identity[Int])(_ + _)

      val sub = Source
        .repeat(1)
        .via(summingWindow)
        .runWith(TestSink.probe)

      sub.request(2)

      sub.expectNext()

      sub.expectNoMsg(timeWindow + epsilonTime)
      scheduler.time.advance(timeWindow + epsilonTime)
      scheduler.tick()
      sub.expectNext()
    }
  }
}
