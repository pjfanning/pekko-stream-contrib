/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */

package org.apache.pekko.stream.contrib

import org.apache.pekko.stream.scaladsl.{ Keep, Source }
import org.apache.pekko.stream.testkit.scaladsl.{ TestSink, TestSource }

class AccumulateSpec extends BaseStreamSpec {

  "Accumulate" should {
    "emit folded vaules starting with the result of applying the given function to the given zero and the first pushed element" in {
      val (source, sink) = TestSource
        .probe[Int]
        .via(Accumulate(0)(_ + _))
        .toMat(TestSink.probe)(Keep.both)
        .run()
      sink.request(99)
      source.sendNext(1)
      source.sendNext(2)
      source.sendNext(3)
      sink.expectNext(1, 3, 6)
      source.sendComplete()
      sink.expectComplete()
    }

    "not emit any value for an empty source" in {
      Source(Vector.empty[Int])
        .via(Accumulate(0)(_ + _))
        .runWith(TestSink.probe)
        .request(99)
        .expectComplete()
    }

    "fail on upstream failure" in {
      val (source, sink) = TestSource
        .probe[Int]
        .via(Accumulate(0)(_ + _))
        .toMat(TestSink.probe)(Keep.both)
        .run()
      sink.request(99)
      source.sendError(new Exception)
      sink.expectError()
    }
  }
}
