/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */

package org.apache.pekko.stream.contrib

import org.apache.pekko.stream.scaladsl.{ Keep, Sink, Source }
import org.apache.pekko.stream.testkit.scaladsl.{ TestSink, TestSource }
import org.apache.pekko.testkit.TestDuration
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.duration._

class PulseSpec extends BaseStreamSpec with ScalaFutures {
  private val pulseInterval = 20.milliseconds

  "Pulse Stage" should {

    "signal demand once every interval" in {
      val (probe, future) = TestSource
        .probe[Int]
        .via(new Pulse[Int](pulseInterval.dilated))
        .toMat(Sink.seq)(Keep.both)
        .run()

      probe.sendNext(1)
      probe.expectNoMessage(pulseInterval)
      probe.sendNext(2)
      probe.expectNoMessage(pulseInterval)
      probe.sendComplete()

      whenReady(future) {
        _ should contain inOrderOnly (1, 2)
      }
    }

    "keep backpressure if there is no demand from downstream" in {
      val elements = 1 to 10

      val probe = Source(elements)
        .via(new Pulse[Int](pulseInterval.dilated))
        .runWith(TestSink.probe)

      probe.ensureSubscription()
      // lets waste some time without a demand and let pulse run its timer
      probe.expectNoMessage(pulseInterval * 10)

      probe.request(elements.length.toLong)
      elements.foreach(probe.expectNext)
    }

  }

  "An initially-opened Pulse Stage" should {

    "emit the first available element" in {
      val future = Source
        .repeat(1)
        .via(new Pulse[Int](pulseInterval.dilated, initiallyOpen = true))
        .initialTimeout(2.milliseconds.dilated)
        .runWith(Sink.headOption)

      whenReady(future) {
        _ shouldBe Some(1)
      }
    }

    "signal demand once every interval" in {
      val (probe, future) = TestSource
        .probe[Int]
        .via(new Pulse[Int](pulseInterval.dilated, initiallyOpen = true))
        .toMat(Sink.seq)(Keep.both)
        .run()

      probe.sendNext(1)
      probe.expectNoMessage(pulseInterval)
      probe.sendNext(2)
      probe.expectNoMessage(pulseInterval)
      probe.sendComplete()

      whenReady(future) {
        _ should contain inOrderOnly (1, 2)
      }
    }

  }
}
