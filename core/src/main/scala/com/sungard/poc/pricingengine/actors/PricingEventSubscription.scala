package com.sungard.poc.pricingengine.actors

import akka.actor.ActorRef
import com.sungard.poc.pricingengine.portfolio.StockPortfolio
import com.sungard.poc.pricingengine.actors.PricingStreamActor.KillPricingStream
import com.sungard.poc.pricingengine.io.Disposable

/**
 * An object handed back to the subscribing client actor which is used to stop the pricing stream
 *    when appropriate
 *
 *  Parameters:
 *     - client:  The subscribing client actor who will now receive pricing events
 *     - pricingStreamActor - The hidden actor that actually send PricingEvent(s) to the subscribing
 *                                actor.  It is private so the client can't get to it directly
 *     - portfolio - The stock portfolio being priced
 *
 * User: bposerow
 */
case class PricingEventSubscription(val client : ActorRef,
                                    private val pricingStreamActor : ActorRef,
                                    val portfolio : StockPortfolio) extends Disposable {
  /**
   * Close the pricing stream by gracefully stopping the associated hidden actor
   */
      def close() = {
          pricingStreamActor ! KillPricingStream
      }

  /**
   * Convenience method for using this object within a closure and then immediately closing the stream
   *
   *
   * @param process A method which gets the subscription and can do whatever it wants with it, after
   *                  which time the stream automatically closes
   */
      def use(process : PricingEventSubscription => Unit) = {
        using(this) {
            process(_)
        }
      }
}
