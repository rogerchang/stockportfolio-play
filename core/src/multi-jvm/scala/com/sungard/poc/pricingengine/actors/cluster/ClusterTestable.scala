package com.sungard.poc.pricingengine.actors.cluster

import akka.actor.{Actor, ActorPath}
import akka.cluster.ClusterEvent.MemberUp
import akka.cluster.Cluster
import akka.remote.testkit.MultiNodeSpec
import org.scalatest.{Matchers, BeforeAndAfterAll, WordSpecLike}
import akka.testkit.ImplicitSender
import akka.remote.testconductor.RoleName

/**
 * Created by admin on 5/24/14.
 */
trait ClusterTestable {
  this : MultiNodeSpec with WordSpecLike with Matchers
       with BeforeAndAfterAll with ImplicitSender  =>

  def joinOnly(seedNode : RoleName) : Unit = {
    Cluster(system) join node(seedNode).address
  }

  def waitForUp(nodePath : ActorPath) : Unit = {
    awaitAssert {
      expectMsgPF[Unit]() {
        case MemberUp(m) => assert(nodePath.address === m.address)
      }
    }
  }

  def join(nodePath : ActorPath, seedNode : RoleName) : Unit = {
    joinOnly(seedNode)

    waitForUp(nodePath)
  }
}
