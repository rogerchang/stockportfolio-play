package com.sungard.poc.pricingengine.actors

import akka.actor.{Props, ActorContext, ActorRef}
import com.sungard.poc.pricingengine.actors.StockPricerActor.{Initialize, DefaultStockQuoteGeneratorProvider}

/**
 * Mix in this trait to help with the creation of StockPricerActor(s).  Used by the StockDirectoryActor
 *    to create StockPricerActor(s) to handle particular stock ticker valuation requests
 */
trait StockPricerActorProvider {
  /**
   * Hook for preinitializing prior to actually creating any stock pricers
   *
   * @param context
   */
    def prepareForStockPricers(context : ActorContext) : Unit = {}

  /**
   * Create a stock pricer actor for a given stock ticker
   *
   * @param context
   * @param ticker
   * @return
   */
    def createStockPricerActor(context : ActorContext, ticker : String) : ActorRef

  /**
   * Create and initialize the stock pricer actor for a particular ticker
   *
   * @param context
   * @param ticker
   * @return
   */
    def initializeStockPricerActor(context : ActorContext, ticker : String) : ActorRef = {
        val stockPricer = createStockPricerActor(context, ticker)
        // The Initialize message for the StockPricer is used to perform lookups used to setup the stock pricer
        stockPricer ! Initialize
        stockPricer
    }
}
