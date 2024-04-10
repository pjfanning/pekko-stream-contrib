/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */

package org.apache.pekko.stream.contrib

import org.apache.pekko.stream.scaladsl.{ Flow, Sink, Source }
import org.apache.pekko.stream.testkit.TestSubscriber
import org.apache.pekko.testkit.TestProbe
import org.reactivestreams.{ Publisher, Subscriber }

import scala.concurrent.duration.{ Duration, FiniteDuration }

class TimedSpec extends BaseStreamSpec {

  "Timed Source" should {

    import Implicits.TimedSourceDsl

    "measure time it between elements matching a predicate" in {
      val testActor = TestProbe()

      val measureBetweenEvery = 5
      val n = 20

      val printInfo = (interval: Duration) => {
        testActor.ref ! interval
        info(s"Measured interval between $measureBetweenEvery elements was: $interval")
      }

      val source = Source(1 to n).timedIntervalBetween(_ % measureBetweenEvery == 0, printInfo)

      source.runWith(Sink.ignore)
      (1 until n / measureBetweenEvery).foreach { _ =>
        val duration = testActor.expectMsgType[FiniteDuration]
        assert(duration.toMillis >= 0, s"$duration is not a positive duration")
      }
    }

    "measure time it takes from start to complete, by wrapping operations" in {
      val testActor = TestProbe()

      val n = 50
      val printInfo = (d: FiniteDuration) => {
        testActor.ref ! d
        info(s"Processing $n elements took $d")
      }

      Source(1 to n).timed(_.map(identity), onComplete = printInfo).runWith(Sink.ignore)
      val duration = testActor.expectMsgType[FiniteDuration]
      assert(duration.toMillis >= 0, s"$duration is not a positive duration")
    }

  }

  "Timed Flow" should {

    import Implicits.TimedFlowDsl

    "measure time it between elements matching a predicate" in {
      val probe = TestProbe()

      val flow: Flow[Int, Long, _] = Flow[Int].map(_.toLong).timedIntervalBetween(in => in % 2 == 1, d => probe.ref ! d)

      val c1 = TestSubscriber.manualProbe[Long]()
      Source(List(1, 2, 3)).via(flow).runWith(Sink.fromSubscriber(c1))

      val s = c1.expectSubscription()
      s.request(100)
      c1.expectNext(1L)
      c1.expectNext(2L)
      c1.expectNext(3L)
      c1.expectComplete()

      val duration = probe.expectMsgType[Duration]
      info(s"Got duration (first): $duration")
      assert(duration.toMillis >= 0, s"$duration is not a positive duration")
    }

    "measure time from start to complete, by wrapping operations" in {
      val probe = TestProbe()

      // making sure the types come out as expected
      val flow: Flow[Int, String, _] =
        Flow[Int].timed(_.map(_.toDouble).map(_.toInt).map(_.toString), duration => probe.ref ! duration).map {
          (s: String) =>
            s + "!"
        }

      val (flowIn: Subscriber[Int], flowOut: Publisher[String]) =
        flow.runWith(Source.asSubscriber[Int], Sink.asPublisher[String](false))

      val c1 = TestSubscriber.manualProbe[String]()
      val c2 = flowOut.subscribe(c1)

      val p = Source(0 to 100).runWith(Sink.asPublisher(false))
      p.subscribe(flowIn)

      val s = c1.expectSubscription()
      s.request(200)
      (0 to 100).foreach { i =>
        c1.expectNext(i.toString + "!")
      }
      c1.expectComplete()

      val duration = probe.expectMsgType[Duration]
      info(s"Took: $duration")
      assert(duration.toMillis >= 0, s"$duration is not a positive duration")
    }
  }
}
