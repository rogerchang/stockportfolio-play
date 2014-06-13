package com.sungard.poc.pricingengine.actors.cluster

import akka.actor.Actor
import akka.remote.testkit.MultiNodeSpec
import org.scalatest.{Matchers, BeforeAndAfterAll, WordSpecLike}
import akka.testkit.ImplicitSender

/**
 * Created by admin on 5/24/14.
 */
trait MultiNodeTestable {
  this : MultiNodeSpec with WordSpecLike with Matchers
    with BeforeAndAfterAll with ImplicitSender  =>

  override def initialParticipants = roles.size

  override def beforeAll() = multiNodeSpecBeforeAll()

  override def afterAll() = multiNodeSpecAfterAll()

}
