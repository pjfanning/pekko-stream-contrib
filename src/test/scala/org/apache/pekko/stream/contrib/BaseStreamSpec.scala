/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */

package org.apache.pekko.stream.contrib

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.ActorMaterializer
import com.typesafe.config.{ Config, ConfigFactory }
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

trait BaseStreamSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll {

  protected implicit val system: ActorSystem = {
    def systemConfig =
      config.withFallback(ConfigFactory.load())
    ActorSystem("default", systemConfig)
  }

  protected implicit val mat: ActorMaterializer = ActorMaterializer()

  override protected def afterAll() = {
    Await.ready(system.terminate(), 42.seconds)
    super.afterAll()
  }

  protected def config: Config = ConfigFactory.empty()
}
