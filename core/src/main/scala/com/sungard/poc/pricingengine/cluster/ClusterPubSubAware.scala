package com.sungard.poc.pricingengine.cluster

import akka.actor.{Actor, ActorRef}
import akka.contrib.pattern.DistributedPubSubMediator.{Send, Put, Publish, Subscribe}
import akka.contrib.pattern.DistributedPubSubExtension

/**
 * Created by admin on 5/1/14.
 */
trait ClusterPubSubAware {
  this : Actor =>
    var mediator : ActorRef = null//= DistributedPubSubExtension(context.system).mediator

    def initializePubSub() = {
      println("Requesting DistributedPubSubMediator to start")
      mediator = DistributedPubSubExtension(context.system).mediator
    }

    def subscribe(topic : String) = {
      mediator ! Subscribe(topic, self)
    }

    def publish(topic : String, msg : Any) = {
      mediator ! Publish(topic, msg)
    }

    def register() = {
      mediator ! Put(self)
    }

    def send(actorPath : String, msg : Any) = {
       //val mediator = DistributedPubSubExtension(context.system).mediator
       mediator ! Send(actorPath, msg, true)
    }
}
