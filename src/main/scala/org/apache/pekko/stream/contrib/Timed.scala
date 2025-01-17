/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */

package org.apache.pekko.stream.contrib

import java.util.concurrent.atomic.AtomicLong

import org.apache.pekko.stream.Attributes
import org.apache.pekko.stream.impl.fusing.GraphStages.SimpleLinearGraphStage
import org.apache.pekko.stream.scaladsl.{ Flow, Source }
import org.apache.pekko.stream.stage._

import scala.concurrent.duration._

/**
 * Provides operations needed to implement the `timed` DSL
 */
trait TimedOps {

  import Timed._

  /**
   * Measures time from receiving the first element and completion events - one for each subscriber of this `Flow`.
   */
  def timed[I, O, Mat, Mat2](source: Source[I, Mat],
      measuredOps: Source[I, Mat] => Source[O, Mat2],
      onComplete: FiniteDuration => Unit): Source[O, Mat2] = {
    val ctx = new TimedFlowContext

    val startTimed = Flow[I].via(new StartTimed(ctx)).named("startTimed")
    val stopTimed = Flow[O].via(new StopTimed(ctx, onComplete)).named("stopTimed")

    measuredOps(source.via(startTimed)).via(stopTimed)
  }

  /**
   * Measures time from receiving the first element and completion events - one for each subscriber of this `Flow`.
   */
  def timed[I, O, Out, Mat, Mat2](flow: Flow[I, O, Mat],
      measuredOps: Flow[I, O, Mat] => Flow[I, Out, Mat2],
      onComplete: FiniteDuration => Unit): Flow[I, Out, Mat2] = {
    // todo is there any other way to provide this for Flow, without duplicating impl?
    // they do share a super-type (FlowOps), but all operations of FlowOps return path dependant type
    val ctx = new TimedFlowContext

    val startTimed = Flow[O].via(new StartTimed(ctx)).named("startTimed")
    val stopTimed = Flow[Out].via(new StopTimed(ctx, onComplete)).named("stopTimed")

    measuredOps(flow.via(startTimed)).via(stopTimed)
  }

}

/**
 * Provides operations needed to implement the `timedIntervalBetween` DSL
 */
trait TimedIntervalBetweenOps {

  import Timed._

  /**
   * Measures rolling interval between immediately subsequent `matching(o: O)` elements.
   */
  def timedIntervalBetween[O, Mat](source: Source[O, Mat],
      matching: O => Boolean,
      onInterval: FiniteDuration => Unit): Source[O, Mat] = {
    val timedInterval = Flow[O].via(new TimedInterval[O](matching, onInterval)).named("timedInterval")
    source.via(timedInterval)
  }

  /**
   * Measures rolling interval between immediately subsequent `matching(o: O)` elements.
   */
  def timedIntervalBetween[I, O, Mat](flow: Flow[I, O, Mat],
      matching: O => Boolean,
      onInterval: FiniteDuration => Unit): Flow[I, O, Mat] = {
    val timedInterval = Flow[O].via(new TimedInterval[O](matching, onInterval)).named("timedInterval")
    flow.via(timedInterval)
  }
}

object Timed extends TimedOps with TimedIntervalBetweenOps {

  // todo needs java DSL

  final class TimedFlowContext {
    import scala.concurrent.duration._

    private val _start = new AtomicLong
    private val _stop = new AtomicLong

    def start(): Unit =
      _start.compareAndSet(0, System.nanoTime())

    def stop(): FiniteDuration = {
      _stop.compareAndSet(0, System.nanoTime())
      compareStartAndStop()
    }

    private def compareStartAndStop(): FiniteDuration = {
      val stp = _stop.get
      if (stp <= 0) Duration.Zero
      else (stp - _start.get).nanos
    }
  }

  final class StartTimed[T](timedContext: TimedFlowContext) extends SimpleLinearGraphStage[T] {

    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
      new GraphStageLogic(shape) with InHandler with OutHandler {

        private var started = false

        override def onPush(): Unit = {
          if (!started) {
            timedContext.start()
            started = true
          }
          push(out, grab(in))
        }

        override def onPull(): Unit = pull(in)

        setHandlers(in, out, this)
      }
  }

  final class StopTimed[T](timedContext: TimedFlowContext, _onComplete: FiniteDuration => Unit)
      extends SimpleLinearGraphStage[T] {

    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
      new GraphStageLogic(shape) with InHandler with OutHandler {

        override def onPush(): Unit = push(out, grab(in))

        override def onPull(): Unit = pull(in)

        override def onUpstreamFailure(cause: Throwable): Unit = {
          stopTime()
          failStage(cause)
        }

        override def onUpstreamFinish(): Unit = {
          stopTime()
          completeStage()
        }

        private def stopTime(): Unit = {
          val d = timedContext.stop()
          _onComplete(d)
        }

        setHandlers(in, out, this)
      }
  }

  final class TimedInterval[T](matching: T => Boolean, onInterval: FiniteDuration => Unit)
      extends SimpleLinearGraphStage[T] {

    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
      new GraphStageLogic(shape) with InHandler with OutHandler {

        private var prevNanos = 0L
        private var matched = 0L

        override def onPush(): Unit = {
          val elem = grab(in)
          if (matching(elem)) {
            val d = updateInterval(elem)

            if (matched > 1)
              onInterval(d)
          }
          push(out, elem)
        }

        override def onPull(): Unit = pull(in)

        private def updateInterval(in: T): FiniteDuration = {
          matched += 1
          val nowNanos = System.nanoTime()
          val d = nowNanos - prevNanos
          prevNanos = nowNanos
          d.nanoseconds
        }

        setHandlers(in, out, this)
      }

  }

}
