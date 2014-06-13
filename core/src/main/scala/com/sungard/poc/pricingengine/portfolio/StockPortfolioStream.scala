package com.sungard.poc.pricingengine.portfolio

/**
 * Base class for constructing a stream of portfolios that are generated somehow
 *
 * User: bposerow
 * Date: 1/22/14
 * Time: 11:09 PM
 * To change this template use File | Settings | File Templates.
 */
trait StockPortfolioStream {
  /**
   *
   * @return A potentially infinite stream of stock portfolios, i.e. bags of tickers with a fractional
   *          allocation of the stocks within the portfolio and a total number of shares
   */
    def stockPortfolios : Stream[StockPortfolio] = {
          nextPortfolio match {
            case Some(portfolio) => portfolio #:: stockPortfolios
            // Use Option type to allow termination of the stream
            case None => Stream.empty
          }
    }

    def nextPortfolio : Option[StockPortfolio]
}
