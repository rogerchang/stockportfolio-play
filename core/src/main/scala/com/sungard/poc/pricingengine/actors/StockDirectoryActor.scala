package com.sungard.poc.pricingengine.actors

import akka.actor._
import com.sungard.poc.pricingengine.actors.StockDirectoryActor.{StockPricerRetrieved, GetStockPricer}
import scala.concurrent.duration._
import akka.actor.SupervisorStrategy.{Stop, Escalate, Restart}
import akka.actor.OneForOneStrategy
import akka.actor.ActorKilledException
import com.sungard.poc.pricingengine.actors.StockPricerActor.{Initialize, DefaultStockQuoteGeneratorProvider}
import akka.event.Logging
import akka.routing.ConsistentHashingRouter.ConsistentHashable
import com.sungard.poc.pricingengine.cluster.ClusterUtils._
import com.sungard.poc.pricingengine.cluster.ClusterNodeParameters
import akka.contrib.pattern.DistributedPubSubExtension

/**
 * A StockDirectoryActor maintains references to a series of StockPricerActor(s), used to determine prices
 *     for a stock.  This actor will return these references upon request.   It will create a StockPricer
 *     if none exists for a particular ticker, otherwise it will create a new one for that stock ticker.
 *
 * The StockDirectoryActor is designed to exist with multiple copies but each copy is responsible for
 *     consistently routing requests for a particular stock ticker.  It is also designed to function as
 *     a supervisor, stopping the StockPricer(s) as they feel (which is fine because a new one will be
 *     started upon the next request).
 *
 * Collaborators:
 *    - StockPricerActor:  The actors created/maintained by this actor used to generate a random pricer for
 *          a given stock
 *    - StockDirectoryRouterActor:  A router in front of this actor.  There will be multiple StockDirectoryActor(s)
 *           and the StockDirectoryRouterActor will route appropriately between them
 *
 * User: admin
 * Date: 2/11/14
 * Time: 11:22 PM
 */

object StockDirectoryActor {

  /**
   * Retrieve a reference to the StockPricerActor corresponding to the stock ticker, creating a new
   *    StockPricer or returning an existing reference accordingly
   *
   * @param stock
   */
    case class GetStockPricer(stock : String) extends ConsistentHashable {
        override def consistentHashKey: Any = {
             stock
        }
    }

  /**
   * Returns a reference to the StockPricerActor corresponding to the stock ticker used to price the
   *    stock.
   *
   * @param ticker
   * @param stockPricer
   */
    case class StockPricerRetrieved(ticker : String, stockPricer : ActorRef)

  /**
   *  Mixin trait used to create a stock pricer actor and then initialize it
   */
    trait DefaultStockPricerActorProvider extends StockPricerActorProvider {
        def createStockPricerActor(context : ActorContext, ticker : String) : ActorRef = {
            context.actorOf(StockPricerActor.singleNodeProps(ticker, context.system),
                      ticker)
        }
    }

  /**
   * Mixin trait used to create a stock pricer actor that is enabled to run on a cluster
   */
  trait ClusterStockPricerActorProvider extends StockPricerActorProvider {
    /**
     * Create a stock pricer actor on the same cluster node as the stock directory
     *
     * @param context
     * @param ticker  The ticker of the stock for the stock pricer we are creating
     * @return
     */
    def createStockPricerActor(context : ActorContext, ticker : String) : ActorRef = {
      context.actorOf(StockPricerActor.clusteredProps(ticker, context.system),
        ticker)
    }

    /**
     * Allow for initialization of the context or actor system of the cluster before we
     *   actually create any stock pricer actors.  In this case, I just initialize the
     *   Distributed pub sub mediator so it will all be ready before we actually send
     *   any publish or subscribe methods.
     *
     * @param context
     */
    override def prepareForStockPricers(context : ActorContext) : Unit = {
        DistributedPubSubExtension(context.system).mediator
        return
    }
  }

  /**
   * Create a non-clustered version of the properties for the StockDirectoryActor used to create it
   *
   * @return
   */
  def props() : Props = Props(new StockDirectoryActor with DefaultStockPricerActorProvider)

  /**
   * Create properties for a version of this actor that can run on the cluster.  Basically, this
   *     is done by layering in a provider of the clustered stock pricer actor
   *
   * @return
   */
  def clusteredProps() = Props(new StockDirectoryActor with ClusterStockPricerActorProvider)
}

class StockDirectoryActor extends Actor {
  // Force mixing in a trait for creating StockPricer actors corresponding to a given stock ticker
  this : StockPricerActorProvider =>
  private val log = Logging(context.system, this)
  private val config = context.system.settings.config

  // Supervisor strategy that basically will stop a StockPricer whenever most types of normal exceptions
  //    occur during their lifetimes.  As discussed above, this is OK because a new request for that
  //    stock ticker will just recreate the stock pricer actor
  override val supervisorStrategy =  OneForOneStrategy(config.getInt("server_app.max.pricer.failures.count"),
      config.getInt("server_app.max.pricer.failures.within.time.in.seconds").seconds) {
    case _ : ActorInitializationException => Stop
    case _ : ActorKilledException => Stop
    case _ : Exception => Stop
    case _ => Escalate
  }

  /**
   * Add a hook for initializing the context used to create the stock pricer actors before
   *   any actual stock pricer actors get created
   */
  override def preStart() {
      prepareForStockPricers(context)
  }

  def receive : Receive = {
    // Retrieve a stock pricer for a stock ticker
    case GetStockPricer(stock : String) =>  {
      log.info("GetStockPricer received for stock " + stock)
      // See if a child StockPricer has already been created for that stock, if so, return it, otherwise
      //    create it.
      val childOption = context.child(stock);
      childOption match {
        case Some(child) => {
            log.info("Retrieved existing child for stock " + stock + ": " + child)
            // Return the existing stock pricer
            sender ! StockPricerRetrieved(stock, child)
        }
        case _ => {
            // No child for that stock ticker yet exists, create it and then return it
            val stockPricerActor = createStockPricerActor(context, stock)
            log.info("Creating new stock pricer actor for stock " + stock + ": " + stockPricerActor)
            sender ! StockPricerRetrieved(stock, stockPricerActor)
        }
      }
    }
    case _ =>  throw new RuntimeException("Unexpected message")
  }
}
