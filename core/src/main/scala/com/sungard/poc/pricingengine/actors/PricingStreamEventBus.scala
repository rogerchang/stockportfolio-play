package com.sungard.poc.pricingengine.actors

import akka.event._
import akka.actor.ActorRef
import com.sungard.poc.pricingengine.portfolio.StockPortfolio

/**
 * Used to (a) allow subscriber client actors to register that this particular actor is
 *     interested in PricingEvent(s) for a particular StockPortfolio and (b) then to
 *     push each PricingEvent through the EventBus to the interested subscribers.  The
 *     registration is based on the StockPortfolio associated with the PricingEvent and this
 *     is used to identify to which client(s) to send notifications.  The result is that
 *     the subscriber client(s) receive the appropriate PricingEvent(s).
 *
 *     A parameter of initNumStreams indicates the expected number of subscribers (client/portfolio pairs)
 *       that will be sent through the PricingStreamEventBus.  It is a required parameter of an
 *       Akka event bus so I made it configurable.  It will grow dynamically so it presumably will
 *       only have performance implications if set incorrectly.
 *
 *  User: bposerow
 */
class PricingStreamEventBus(initNumStreams : Int) extends EventBus
    with LookupClassification {
  type Event = PricingEvent
  type Classifier = StockPortfolio
  type Subscriber = ActorRef

  override protected def classify(event: Event): Classifier = {
      event.portfolio
  }

  override protected def mapSize: Int = {
       initNumStreams
  }

  override protected def publish(event: Event, subscriber: Subscriber): Unit = {
       subscriber ! event
  }

  override protected def compareSubscribers(a: Subscriber, b: Subscriber): Int = {
       a.compareTo(b)
  }
}
