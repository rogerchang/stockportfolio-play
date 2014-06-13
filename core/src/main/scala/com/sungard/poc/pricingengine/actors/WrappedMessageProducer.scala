package com.sungard.poc.pricingengine.actors

import akka.actor.ActorRef

/**
 * Created by admin on 5/2/14.
 */
trait WrappedMessageProducer {
  def wrap(rawMsg : Any) = rawMsg
}
