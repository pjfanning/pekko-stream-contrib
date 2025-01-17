/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */

package org.apache.pekko.stream.contrib

import java.util.concurrent.atomic.AtomicInteger
import scala.util.Random
import org.apache.pekko.stream.scaladsl.Keep
import org.apache.pekko.stream.testkit.scaladsl.TestSink

class SourceRepeatEvalSpec extends BaseStreamSpec {

  "SourceRepeatEval" should {
    "generate elements" in {
      val int = new AtomicInteger(0)

      val probe = SourceRepeatEval(() => int.getAndIncrement())
        .take(10)
        .toMat(TestSink.probe)(Keep.right)
        .run()

      assert(probe.request(10).expectNextN(10) == (0 until 10))

      assert(int.get() == 10)
    }

    "support cancellation" in {
      val (c, probe) = SourceRepeatEval(() => Random.nextInt)
        .toMat(TestSink.probe)(Keep.both)
        .run()

      probe.requestNext()
      probe.requestNext()
      probe.requestNext()

      c.cancel()

      probe.request(1)
      probe.expectComplete()
    }

    "report correct cancellation state" in {
      val int = new AtomicInteger(0)

      val (c, probe) = SourceRepeatEval(() => int.getAndIncrement())
        .toMat(TestSink.probe)(Keep.both)
        .run()

      assert(probe.requestNext() == 0)
      assert(!c.isCancelled)

      assert(c.cancel())

      probe.request(1).expectComplete()

      assert(c.isCancelled)
      assert(!c.cancel())
    }
  }
}
