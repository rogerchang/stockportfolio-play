package com.sungard.poc.pricingengine.actors

import akka.actor._
import com.sungard.poc.pricingengine.actors.PricingStreamActor._
import scala.concurrent.duration._
import com.sungard.poc.pricingengine.actors.PriceSubscriptionActor.{GeneratePrice}
import com.sungard.poc.pricingengine.actors.StockPortfolioValuationActor.GetPortfolioValuation
import com.sungard.poc.pricingengine.actors.StockPortfolioValuationActor.PortfolioValued
import com.sungard.poc.pricingengine.portfolio.StockPortfolio
import com.sungard.poc.pricingengine.actors.PricingStreamActor.RegisterPricingStream
import akka.routing.FromConfig
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Actor used to stream pricing data to arbitrary client actors.  Client actors register
 *     with this actor by passing themselves and a portfolio that they want to price. The actor
 *     is returned a PricingEventSubscription which can be used to cancel the subscription.
 *     Once they register, prices are generated for the registered portfolio at a specified interval
 *     (that is part of Akka configuration by default).  These prices are calculated by the
 *     StockPortfolioValuationActor with which it interacts at timed intervals.  The result is a
 *     stream of PricingEvent(s) sent back to the subscribing actor which contains the stock portfolio
 *     and the stock portfolio value message returned from the StockPortfolioValuationActor.
 *
 *     The PricingStreamEventBus supports this functionality by allowing subscribers to register
 *       themselves to receive updates of price for a particular stock portfolio.
 *
 *     By default, this actor has a router in front of it.  This actor is mostly stateless except for
 *        the event bus itself.  But actors really only interact with this actor itself to initially
 *        register themselves, after that point a separate hidden actor is spawned to handle the
 *        streaming of price data.
 *
 * User: bposerow
 * Date: 3/5/14
 * Time: 10:31 PM
 */
object PricingStreamActor {

  /**
   * Allows a subscriber to register to receive a stream of prices for the specified portfolio
   *
   * @param subscriber The client actor which wants to receive price updates, can be any actor
   *                     that follows the contract
   * @param portfolio
   */
    case class RegisterPricingStream(subscriber : ActorRef, portfolio : StockPortfolio)

  /**
   * Allows for a particular pricing stream for a particular portfolio and client to be killed gracefully.
   *     This message has no parameters, but the portfolio and client are obvious from context
   */
    case object KillPricingStream

  /**
   * Create the Actor properties used to create a PricingStream actor.
   *
   * @param valuationActor The actor that performs valuation of a stock portfolio passed to it,
   *                         returning one price at a time
   * @param eventBus Event bus with which client subscribers register to receive updates in price
   *                    for a particular portfolio.  Each pricing event in the stream is published
   *                    to the event bus
   * @param priceInterval The interval with which the subscriber actor will receive pricing updates
   * @return
   */
    def props(valuationActor : ActorRef, eventBus : PricingStreamEventBus,
              priceInterval : FiniteDuration) = {
        Props(new PricingStreamActor(valuationActor, eventBus, priceInterval))
    }

  /**
   * Create the Actor properties used to create a PricingStream actor.  This overload of the props
   *    method retrieves various settings from Akka config, including the number of expected streams
   *    (used for initialization of the event bus) and the time interval between price updates.
   *
   *    This overload also uses default settings to setup a router for this actor.
   *
   * @param valuationActor The actor that performs valuation of a stock portfolio passed to it,
   *                         returning one price at a time
   * @param context  The actor context in which this PricingStream actor will be created.  Used
   *                   to retrieve appropriate config
   * @return
   */
  def props(valuationActor : ActorRef, context : ActorContext) : Props = {
      val config = context.system.settings.config

      val initNumStreams = config.getInt("server_app.stock.pricing.stream.num_streams")
      val pricingInterval = config.getInt("server_app.stock.pricing.stream.interval_milliseconds")

      props(valuationActor, new PricingStreamEventBus(initNumStreams), pricingInterval.milliseconds)
                                    .withRouter(FromConfig())
    }
}

/**
 * The PriceSubscriptionActor is a hidden actor which actually sends the pricing data.  There will
 *    be one actor per portfolio/subscriber combination, i.e. when a particular client actor registers
 *    to receive price updates for a particular portfolio, one of these actors is created.  When
 *    a client registers with the PricingStreamActor, it receives a PricingEventSubscription.  The
 *    client can cancel the stream at any time by calling the close method on the PricingEventSubscription.
 *    (Calling the close() method sends a  KillPricingStream message to this actor, which gracefully
 *      shuts it down.)
 *    Before the stream is closed, the subscriber actor will receive a message of PricingEvent at
 *    each specified timing interval.  These events contain the StockPortfolio and its associated
 *    price.  This actor interacts directly with the StockPortfolioValuationActor at timed intervals
 *    to get the prices for the portfolio.
 */
private object PriceSubscriptionActor {

  /**
   * Periodic messages sent by the timer to prompt this actor to get a price
   */
    case object GeneratePrice


  /**
   * Create the properties for this helper hidden actor
   *
   * @param valuationActor The actor with which this actor communicates to get the current
   *                          price for the stock portfolio
   * @param eventBus The event bus through which the PricingEvent(s) are sent
   * @param subscriber  The client actor which will receive the events
   * @param portfolio The stock portfolio being priced
   * @param priceInterval The interval between prices in the stream
   * @return
   */
  def props( valuationActor : ActorRef,
               eventBus : PricingStreamEventBus, subscriber : ActorRef,
               portfolio : StockPortfolio,
               priceInterval : FiniteDuration) = {
     Props(new PriceSubscriptionActor(valuationActor, eventBus, subscriber, portfolio,
                priceInterval))
  }
}

private class PriceSubscriptionActor(
                                  val valuationActor : ActorRef,
                                  val eventBus : PricingStreamEventBus,
                                  val subscriber : ActorRef,
                                  val portfolio : StockPortfolio,
                                  val priceInterval : FiniteDuration) extends Actor {
     // Used to send the periodic events to the StockPortfolioValuationActor
     private var timer : Cancellable = null

     override def preStart() = {
         super.preStart()

         // Start the timer going on initialization
         timer = context.system.scheduler.schedule(0.seconds, priceInterval, self, GeneratePrice)
     }

    def receive : Receive  = {
      // Based on messages at specified periodic interval, send message to get valuation from
      //     valuation actor
      case GeneratePrice => {
             valuationActor ! GetPortfolioValuation(portfolio)
      }
      // When we get a message back from the StockPortfolioValuationActor, we publish it through
      //    the event bus so it will be sent to the right subscriber
      case (valuedMsg @ PortfolioValued(stockPortfolio, value)) => {
            eventBus.publish(PricingEvent(portfolio = stockPortfolio, portfolioValueMsg = valuedMsg))
      }
      // When PricingEventSubscription.close() is called, this message is sent to gracefully shut it down
      //    and stop the pricing stream
      case KillPricingStream => {
            timer.cancel()
            context.stop(self)
      }
    }
}

class PricingStreamActor(
                         val valuationActor : ActorRef,
                         val eventBus : PricingStreamEventBus,
                         val priceInterval : FiniteDuration = 1.second) extends Actor {
      def receive : Receive = {
          // A subscriber client will send this message to indicate that the client actor
          //     (the subscriber) wants to subscribe to price updates for the portfolio
          case RegisterPricingStream(subscriber, portfolio) => {
              // This subscriber will receive pricing updates for this portfolio through the event bus
              eventBus.subscribe(subscriber, portfolio)
              // Create the actor that is actually responsible for maintaining the pricing stream
              //   (the PriceSubscriptionActor); this is wrapped in a PricingEventSubscription that
              //   is passed back to the client which can then use it to cancel the stream
              val subscriptionActor = context.actorOf(PriceSubscriptionActor.props(valuationActor, eventBus, subscriber,
                    portfolio, priceInterval))
              val subscription = PricingEventSubscription(subscriber, subscriptionActor, portfolio)
              sender ! subscription
           }

      }

}
