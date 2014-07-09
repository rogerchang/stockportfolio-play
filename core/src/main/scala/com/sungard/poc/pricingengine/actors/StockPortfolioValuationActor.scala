package com.sungard.poc.pricingengine.actors

import akka.actor.{ActorSystem, Props, ActorRef, Actor}
import akka.pattern.{ask,pipe}
import com.sungard.poc.pricingengine.portfolio.StockPortfolio
import com.sungard.poc.pricingengine.actors.StockPortfolioValuationActor.{ClusterClientMessageProducer, PortfolioValued, GetPortfolioValuation}
import com.sungard.poc.pricingengine.pricing.StockQuote
import com.sungard.poc.pricingengine.actors.StockDirectoryActor.{StockPricerRetrieved, GetStockPricer}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import com.sungard.poc.pricingengine.actors.StockPricerActor.{RetrieveStockQuote, CurrentStockQuote}
import com.sungard.poc.pricingengine.cluster.{ClusterNodeParameters, ClusterClientAccessible}
import akka.event.Logging
import com.sungard.poc.pricingengine.actors.TimeoutConstants._

/**
 * Actor used to value a portfolio of stocks. Right now there is only 1 of these actors exposed by the
 *   ServiceLookupActor but this actor has very little state and could be easily modified to add routing or
 *   clustering.
 *
 *   Collaborators:
 *      - StockDirectoryActor - for looking up the appropriate StockPricerActor for a given stock ticker
 *      - StockPricerActor - for getting a random quote for a particular stock ticker contained within the
 *                  portfolio
 *      - StockPortfolio - an object which is able to value itself based on the stock tickers it contains and
 *              and the random stock quotes obtained for these tickers from the StockPricerActor
 * User: bposerow
 * Date: 2/10/14
 * Time: 11:26 PM
 */

object StockPortfolioValuationActor {
  trait ClusterClientMessageProducer extends ClusterClientAccessible {
    this : Actor =>
    val targetActorPath = "/user/StockDirectoryRouterActor"
  }

  /**
   *
   * @param stockDirectoryActor Stock directory actor used to lookup stock pricer actors
   * @return Props used to create StockPortfolioValuationActor
   */
  def props(stockDirectoryActor : ActorRef) : Props = Props(
    new StockPortfolioValuationActor(stockDirectoryActor, StockQuoteRetrieverActor.props(stockDirectoryActor))
      with WrappedMessageProducer)

  /**
   * Create a clustered version of this actor
   *
   * @param contactPoints Configuration info about the nodes in the cluster that are the main
   *                        contact points of the cluster
   * @param system The
   * @return Props used to create StockPortfolioValuationActor
   */
  def propsWithCluster(contactPoints : Seq[ClusterNodeParameters],
                       system : ActorSystem) : Props = {
    val receptionistActor = ClusterClientAccessible.lookupClient(contactPoints, system)

    Props(new StockPortfolioValuationActor(
      receptionistActor,
      StockQuoteRetrieverActor.clusteredProps(receptionistActor)
    )
      with ClusterClientMessageProducer)
  }

  def clusterClientSystem(systemName : String, host : String = "127.0.0.1", port : Integer = 0) = {
    ActorSystem(systemName, ClusterClientAccessible.config(host, port))
  }

  /**
   * Message used to obtain a valuation (pricing) of a stock portfolio for this actor
   *
   * @param stockPortfolio Portfolio of stocks, i.e. stock tickers that make up the portfolio as well as
   *                        their relative percentages
   */
  case class GetPortfolioValuation(stockPortfolio : StockPortfolio)

  /**
   * Return message with a stock portfolio and its determined value
   *
   * @param stockPortfolio  Portfolio of stocks
   * @param value Total value of the portfolio
   */
  case class PortfolioValued(stockPortfolio : StockPortfolio, value : Double)
}

private object StockQuoteRetrieverActor {
  def props(stockDirectoryActor : ActorRef) : Props = Props(
    new StockQuoteRetrieverActor(stockDirectoryActor) with WrappedMessageProducer)

  def clusteredProps(receptionistActor : ActorRef) : Props = Props(
    new StockQuoteRetrieverActor(receptionistActor) with ClusterClientMessageProducer)


  /**
   * Internal message used to retrieve the price quote for a particular stock
   *
   * @param stock Stock ticker for which random price should be retrieved
   */
  case class RetrieveStockQuoteForTicker(stock : String)
}

import StockQuoteRetrieverActor._

/**
 *  This helper actor is used to retrieve the price for a particular stock
 *
 * @param stockDirectory
 */
private class StockQuoteRetrieverActor(val stockDirectory : ActorRef) extends Actor {

  // This class does request-response style ask operations, so these require an implicit timeout
  this : WrappedMessageProducer =>
  private val log = Logging(context.system, this)

  def printStockPricerRetrieved(stockPricerRetrieved : StockPricerRetrieved) : StockPricerRetrieved = {
    log.info("StockPricerRetrieved = " + stockPricerRetrieved)
    stockPricerRetrieved
  }

  def printCurrentStockQuote(currentStockQuote : CurrentStockQuote) = {
    log.info("CurrentStockQuote = " + currentStockQuote)
    currentStockQuote
  }


  def receive : Receive = {
    // Internal request to retrieve price for a particular stock
    case RetrieveStockQuoteForTicker(stock) => {
      log.info("In StockQuoteRetrieverActor, received message RetrieveStockQuoteForTicker")
      log.info("StockDirectory actor = " + stockDirectory)
      val stockQuote = for {
      // Ask the stock directory actor for the particular stock pricer actor for the stock ticker
      //   requested; note the ask syntax used to return the StockPricerActor, which returns a future
        stockPricer <- (stockDirectory ? wrap(GetStockPricer(stock))).mapTo[StockPricerRetrieved].map(printStockPricerRetrieved)
        // Next ask the stock pricer actor for the next random stock quote
        stockQuote <- (stockPricer.stockPricer ? RetrieveStockQuote).mapTo[CurrentStockQuote].map(printCurrentStockQuote)
      }  yield (stockQuote.stockQuote)

      // Return the stock quote within this actor so it can be used to complete the calculation of
      //   the portfolio value
      val theSender = sender
      stockQuote.onComplete(finalQuote => {log.info("Sending stockQuote " + finalQuote + " back to actor " + theSender)})

      stockQuote pipeTo sender
    }
  }
}

/**
 *
 * @param stockDirectory Stock directory actor used to obtain the appropriate stock pricer actors used
 *                        to value the components of the portfolio
 */
class StockPortfolioValuationActor(val stockDirectory : ActorRef, val stockQuoteRetrieverActorProps : Props) extends Actor {
  private val log = Logging(context.system, this)

  private val stockQuoteRetrieverActor = context.actorOf(stockQuoteRetrieverActorProps, "StockPortfolioValuationStockQuoteRetrieverActor")

  /**
   * Returns a future that, when completed, will return a list of stock quotes, one for each stock
   *     ticker making up the portfolio
   *
   * @param stockPortfolio The stock portfolio which needs to be valued
   * @return
   */
  def getStockQuotesFuture(stockPortfolio : StockPortfolio) = {
    // For each stock ticker in the portfolio, use the internal actor above to retrieve a random stock
    //   quote, and then return this as a Future[Seq[StockQuote]], a future that will return a list of
    //   stock quotes
    val stockQuoteFutures  = (stockPortfolio.portfolioElements.map {
      case (stock, _) =>  {
        log.info("Sending RetrieveStockQuoteForTicker to internal stockQuoteRetrieverActor")
        (stockQuoteRetrieverActor ? RetrieveStockQuoteForTicker(stock)).mapTo[StockQuote]
      }
    })
    Future.sequence(stockQuoteFutures)
  }

  def receive : Receive = {
    // Get the valuation of this portfolio
    case GetPortfolioValuation(stockPortfolio) => {
      log.info("Received GetPortfolioValuation with stockPortfolio")

      // Use a future containing the stock quotes for the stocks making up the portfolio to get the
      //   portfolio value.  This is done with the use of the StockPortfolio itself, which is able to
      //   value itself given a list of StockQuote(s)
      val portfolioValuedMessage = getStockQuotesFuture(stockPortfolio).
        map(stockQuotes => PortfolioValued(stockPortfolio, stockPortfolio.getPortfolioValue(stockQuotes)))

      val theSender = sender
      portfolioValuedMessage.onComplete(finalValue => {log.info("Sending portfolioValuedMessage " + finalValue + " back to actor " + theSender)})

      // Returns the value of the portfolio in the PortfolioValued message
      portfolioValuedMessage pipeTo sender
    }
  }
}
