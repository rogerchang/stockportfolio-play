package com.sungard.poc.pricingengine.actors

import akka.actor._
import akka.routing.FromConfig
import scala.Some
import akka.actor
import com.sungard.poc.pricingengine.stock_data.{StockData, StockDataRetriever}
import com.sungard.poc.pricingengine.stock_data.yahoo.YahooStockDataRetriever
import com.sungard.poc.pricingengine.cluster.ClusterPubSubAware
import com.sungard.poc.pricingengine.actors.StockDataRetrieverActor.{StockDataRetrieved, RetrieveStockData}
import akka.event.Logging

/**
 * Actor used to retrieve stock data from Yahoo finance.  The stock data consists of a list of prices and
 * dates.  This data is then used by other actors
 * to return random stock prices based on the observed mean and standard deviation of this stock data.
 *
 * User: admin
 * Date: 3/5/14
 * Time: 10:31 PM
 */

object StockDataRetrieverActor {

  /**
   * Request to retrieve stock historical data from Yahoo finance for the specified stock ticker
   *
   * @param stock
   */
  case class RetrieveStockData(stock: String, originator: ActorRef = null)

  /**
   * Return the stock historical data retrieved from Yahoo finance
   *
   * @param stockData
   */
  case class StockDataRetrieved(stockData: StockData, originator: ActorRef = null)

  trait DefaultStockDataRetrieverProvider extends StockDataRetrieverProvider {
    val dataRetriever = new YahooStockDataRetriever
  }

  /**
   * Create a router for this stock data retriever actor.  It constructs this actor with a configured
   * StockDataRetriever, which is used to configure the way the historical stock data is to be
   * retrieved.  In this case, a YahooStockDataRetriever is used, which means that this info will
   * be obtained from Yahoo Finance.  The router is configured in Akka config, currently a random
   * router.
   *
   * @param context
   * @return
   */
  def createRouter(context: ActorContext): ActorRef = {
    createRouter(context, Props(new StockDataRetrieverActor
      with DefaultStockDataRetrieverProvider with PreInitialized
    ))
  }


  /**
   * Create a non-clustered router actor for the various StockDataRetrieverActor(s), i.e. the
   * actors responsible for retrieving stock price history information for a ticker.
   * We only create a router when we are not using clustering.  Use Akka configuration for
   * this router.
   *
   * @param context
   * @param props
   * @return
   */
  def createRouter(context: ActorContext, props: Props): ActorRef = {
    context.actorOf(props.withRouter(FromConfig()), "StockDataRetrieverRouter")
  }

  /**
   * Create a non-clustered router actor for the various StockDataRetrieverActor(s), i.e. the
   * actors responsible for retrieving stock price history information for a ticker.
   * We only create a router when we are not using clustering.  Use Akka configuration for
   * this router.
   *
   * @param system
   * @param props
   * @return
   */
  def createRouter(system: ActorSystem, props: Props): ActorRef = {
    system.actorOf(props.withRouter(FromConfig()), "StockDataRetrieverRouter")
  }

  /**
   * Create a clustered version of a StockDataRetrieverActor which uses Akka distributed
   * pub sub to send messages from other actors to a stock data retriever which will
   * pick up the message and process it.  Pub sub is used as an easy form of routing
   * within the cluster, i.e. allow a random StockDataRetrieverActor to pick up the request.
   *
   * @param system
   * @return
   */
  def createClusteredActor(system: ActorSystem): ActorRef = {
    system.actorOf(Props(new StockDataRetrieverActor with DefaultStockDataRetrieverProvider
      with ClusteredInitialized))
  }

  /**
   * Mix in this trait to properly initialize the Akka pub sub stuff used to deliver messages
   * to a random StockDataRetrieverActor.  This trait sets up the DistributedPubSubMediator
   * (an internal Akka actor which coordinates the pub sub mechanism) and then registers
   * this StockDataRetrieverActor with the mediator, which effectively makes it a subscriber
   * to messages sent to its path.
   */
  trait ClusteredInitialized extends PreInitialized
  with ClusterPubSubAware {
    this: Actor =>

    override def preStart() = {
      initializePubSub()
      println("Registering StockDataRetrieverActor for actor path " + self)
      register()
    }
  }

}

class StockDataRetrieverActor extends Actor {
  this: StockDataRetrieverProvider with PreInitialized =>
  private val config = context.system.settings.config

  private val log = Logging(context.system, this)

  // Lookback, configured from Akka config, is used to specify how many days back from the present
  //   should we look for historical data.
  private val lookback = config.getInt("server_app.stock.data.retrieve.days.lookback")

  def receive = {
    case RetrieveStockData(stock, originator) => {
      // Use the configured StockDataRetriever to retrieve historical stock data for the "lookback"
      //   specified number of days and then return this to the sender
      log.info("StockDataRetrieverActor received message RetrieveStockData for stock " + stock)
      val stockData = dataRetriever.retrieve(stock, lookback)
      log.info("Retrieved stock data information, sending back to sender " + sender)
      sender ! StockDataRetrieved(stockData, originator)
    }
  }
}