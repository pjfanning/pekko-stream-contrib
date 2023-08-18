/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */

package org.apache.pekko.stream.contrib

import java.util.concurrent.atomic.AtomicBoolean
import org.apache.pekko.actor.Cancellable
import org.apache.pekko.stream.Attributes
import org.apache.pekko.stream.impl.Unfold
import org.apache.pekko.stream.scaladsl.Source

/**
 * Create a `Source` that will output elements of type `A`
 * given a "producer" function
 *
 * Examples:
 *
 * stream of current times:
 *
 * {{{
 *   SourceRepeatEval(() => System.currentTimeMillis)
 * }}}
 *
 * stream of random numbers:
 *
 * {{{
 *   SourceRepeatEval(() => Random.nextInt)
 * }}}
 *
 * Behavior is the same as in
 * {{{
 *   Source.repeat(()).map(_ => x)
 * }}}
 *
 * Supports cancellation via materialized `Cancellable`.
 */
object SourceRepeatEval {
  def apply[A](genElement: () => A): Source[A, Cancellable] = {
    val c: Cancellable = new Cancellable {
      private val stopped: AtomicBoolean = new AtomicBoolean(false)
      override def cancel(): Boolean = stopped.compareAndSet(false, true)
      override def isCancelled: Boolean = stopped.get()
    }

    def nextStep: Unit => Option[(Unit, A)] = { _ =>
      {
        if (c.isCancelled) {
          None
        } else {
          Some(() -> genElement())
        }
      }
    }

    Source
      .fromGraph(new Unfold[Unit, A]((), nextStep))
      .withAttributes(Attributes.name("repeat-eval"))
      .mapMaterializedValue(_ => c)
  }
}
