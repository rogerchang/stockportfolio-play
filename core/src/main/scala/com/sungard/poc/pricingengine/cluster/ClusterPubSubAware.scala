package com.sungard.poc.pricingengine.cluster

import akka.actor.{Actor, ActorRef}
import akka.contrib.pattern.DistributedPubSubMediator.{Send, Put, Publish, Subscribe}
import akka.contrib.pattern.DistributedPubSubExtension

/**
 * Mixin trait for actors that want to use the Akka cluster distributed pub sub mechanism
 */
trait ClusterPubSubAware {
  this : Actor =>
  // The Akka distributed pub sub mediator actor (the built in mediator actor that coordinates
  ///   either publish or subscribe requests)
  var mediator : ActorRef = null

  /**
   * Initialize the Akka distributed pub sub mediator actor
   */
  def initializePubSub() = {
    mediator = DistributedPubSubExtension(context.system).mediator
  }

  /**
   * Subscribe to a particular topic with the Akka distributed pub sub mechanism
   *
   * @param topic
   */
  def subscribe(topic : String) = {
    mediator ! Subscribe(topic, self)
  }

  /**
   * Publish a particular message on a topic using the Akka distributed pub sub mechanism
   *
   * @param topic
   * @param msg
   */
  def publish(topic : String, msg : Any) = {
    mediator ! Publish(topic, msg)
  }

  /**
   * Registers this actor with the Akka distributed pub sub mechanism.  If you register multiple
   *   copies of a given actor with Akka distributed pub sub, you get a kind of cheap routing,
   *   in which a message delivered to the path of the actor is randomly routed to one of the
   *   copies of the actor which registered.   This is an alternative mechanism exposed in
   *   Akka distributed pub sub to that provided by subscribing and publishing to topics.
   */
  def register() = {
    mediator ! Put(self)
  }

  /**
   * Send a message to one of the actors registered with the Akka distributed pub sub mechanism
   *    at the specified actor path.
   *
   * @param actorPath
   * @param msg
   */
  def send(actorPath : String, msg : Any) = {
    mediator ! Send(actorPath, msg, true)
  }
}
