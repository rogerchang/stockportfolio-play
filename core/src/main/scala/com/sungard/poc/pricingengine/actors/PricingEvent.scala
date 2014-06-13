package com.sungard.poc.pricingengine.actors

import com.sungard.poc.pricingengine.portfolio.StockPortfolio
import java.util.UUID
import com.sungard.poc.pricingengine.actors.StockPortfolioValuationActor.PortfolioValued

/**
 * The event actually sent back to a subscribing client that subscribes for price updates
 * from the PricingStreamActor
 *
 * Parameters:
 *    eventId:  Simply a UUID that might be useful for a client to identify a message
 *    portfolio:  The portfolio being priced
 *    portfolioValueMsg:  The messages returned from the StockPortfolioValuationActor
 *
 * User: bposerow
 */
case class PricingEvent(
                        val eventId : UUID = UUID.randomUUID(),
                        val portfolio : StockPortfolio,
                        val portfolioValueMsg : PortfolioValued
                       )