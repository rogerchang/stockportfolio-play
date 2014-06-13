package com.sungard.poc.pricingengine.actors

import akka.actor._
import com.sungard.poc.pricingengine.actors.ServiceLookupActor._
import com.sungard.poc.pricingengine.actors.ServiceLookupActor.StockDirectoryReturned
import com.sungard.poc.pricingengine.actors.ServiceLookupActor.DataRetrieverReturned

/**
 * Relatively top level actor for looking up some key router actors
 * User: bposerow
 * Date: 3/27/14
 * Time: 8:50 PM
 */

object ServiceLookupActor {

  /**
   * Request the data retriever router actor, used to retrieve historical stock data info about a particular
   *    stock ticker
   */
   case object GetDataRetriever

  /**
   * Return the data retriever router actor
   *
   * @param dataRetriever
   */
   case class  DataRetrieverReturned(dataRetriever : ActorRef)

  /**
   * Request to return the router for the StockDirectory actor, used to lookup StockPricer actors for a particular ticker
   */
   case object GetStockDirectory
   case class StockDirectoryReturned(stockDirectory : ActorRef)

  /**
   * Request to return the stock portfolio valuation actor, used to obtain a value for a portfolio
   */
   case object GetStockPortfolioValuer
   case class StockPortfolioValuerReturned(portfolioValuer : ActorRef)

  /**
   * Request to return a stock price streamer, that returns a stream of stock price for a particular
   *    stock portfolio to a client actor which registers with it
   */
   case object GetStockPriceStreamer
   case class StockPriceStreamerReturned(stockPriceStreamer : ActorRef)

   private def props() : Props = Props(new ServiceLookupActor())

   // Name of this actor
   val serviceLookupName = "ServiceLookup"

  /**
   * Create a new instance of ServiceLookupActor.  For now, I expected there will be only one instance of
   *   this actor.
   *
   * @param system
   * @return
   */
   def createServiceLookupActor(system : ActorSystem) = system.actorOf(props(), serviceLookupName)

  /**
   * Retrieve existing service lookup actor
   *
   * @param system
   * @return
   */
   def getServiceLookupActor(system : ActorSystem) = system.actorSelection("/user/" + serviceLookupName)
}

trait ServiceLookupProvider {
    def serviceLookupActor(system : ActorSystem) = ServiceLookupActor.getServiceLookupActor(system)
}

class ServiceLookupActor extends Actor {
    private var dataRetrieverActor  = context.system.deadLetters;
    private var stockDirectoryActor = context.system.deadLetters;
    private var stockPortfolioValuationActor = context.system.deadLetters;
    private var pricingStreamActor = context.system.deadLetters;

    override def preStart() = {
        dataRetrieverActor = StockDataRetrieverActor.createRouter(context)
        stockDirectoryActor = context.actorOf(StockDirectoryRouterActor.props(), "StockDirectoryRouterActor")
        stockPortfolioValuationActor = context.actorOf(StockPortfolioValuationActor.props(stockDirectoryActor), "StockPortfolioValuationActor")
        pricingStreamActor = context.actorOf(PricingStreamActor.props(stockPortfolioValuationActor, context), "StockPricingActor")
    }

    def receive : Receive = {
        case GetDataRetriever => {
            sender ! DataRetrieverReturned(dataRetrieverActor)
        }
        case GetStockDirectory => {
            sender ! StockDirectoryReturned(stockDirectoryActor)
        }
        case GetStockPortfolioValuer => {
            sender ! StockPortfolioValuerReturned(stockPortfolioValuationActor)
        }
        case GetStockPriceStreamer => {
            sender ! StockPriceStreamerReturned(pricingStreamActor)
        }

    }
}
