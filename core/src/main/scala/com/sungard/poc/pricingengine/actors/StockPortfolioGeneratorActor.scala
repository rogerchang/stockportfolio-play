package com.sungard.poc.pricingengine.actors

import akka.actor.{Props, Actor}
import com.sungard.poc.pricingengine.actors.StockPortfolioGeneratorActor.{PortfolioGenerated, GeneratePortfolio}
import com.sungard.poc.pricingengine.portfolio.{StockPortfolio, RandomStockPortfolioStream}

/**
 * Actor used to generate a random stock portfolio upon request
 *
 *
 *
 * User: bposerow
 * Date: 2/4/14
 * Time: 11:17 PM
 */
object StockPortfolioGeneratorActor {

    /**
     * External request to generate a random portfolio
     */
     case object GeneratePortfolio

    /**
     * Returns a randomly generated portfolio to the requester
     *
     * @param portfolio Stock portfolio returned, i.e. the list of stock tickers and relative fractions of
     *                      each stock
     */
     case class PortfolioGenerated(portfolio : StockPortfolio)

     def props() : Props = Props(new StockPortfolioGeneratorActor() with SystemSettingsStockPortfolioConfig)
}

/**
 * External config parameters that are used to constrain the random generation of portfolios
 */
trait StockPortfolioConfig {
    /*
     * The minimum number of stock tickers to be randomly generated within the portfolio
     */
    def minStocks : Int
    /*
     * The maximum number of stock tickers to be randomly generated within the portfolio
     */
    def maxStocks : Int
    /*
    * The minimum number of total number of shares of stock to be randomly generated within the portfolio
    */
    def minShares : Int
    /*
    * The maximum number of total number of shares of stock to be randomly generated within the portfolio
    */
    def maxShares : Int
}

/**
 * Implementation of external config parameters that are used to constrain the random generation of
 *   portfolios, in this case obtained from standard Akka config
 */
trait SystemSettingsStockPortfolioConfig extends StockPortfolioConfig {
   this : StockPortfolioGeneratorActor =>
  def config = context.system.settings.config

   override def minStocks =
   {
     config.getInt("client_app.min.portfolio.stocks")
   }
   override def maxStocks = config.getInt("client_app.max.portfolio.stocks")
   override def minShares = config.getInt("client_app.min.portfolio.shares")
   override def maxShares = config.getInt("client_app.max.portfolio.shares")
}

class StockPortfolioGeneratorActor extends Actor {
  this : StockPortfolioConfig =>
  // A stream that produces one random portfolio at a time constrained by the configured parameters
  val portfolioStream = new RandomStockPortfolioStream(
        minStocks,
        maxStocks,
        minShares,
        maxShares
  )

  def receive: Receive = {
    // Request to generate a new portfolio
    case GeneratePortfolio => {
      val nextElem = portfolioStream.nextPortfolio

      // Return the randomly generated portfolio
      sender ! PortfolioGenerated(nextElem.get)
    }
  }
}


