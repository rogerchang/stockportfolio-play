package com.sungard.poc.pricingengine.portfolio

import com.sungard.poc.pricingengine.pricing.StockQuote

/**
 * A stock portfolio represented by:
 *    a) A list of stock tickers along with the fraction that each stock makes up of the whole portfolio
 *    b)  The total number of shares of stock in the portfolio
 *
 * User: bposerow
 * Date: 1/13/14
 * Time: 9:08 PM
 */

/**
 *
 * @param portfolioElements A list of pairs of stock tickers along with the fractional percentage that the
 *                            stock makes up of the whole portfolio
 */
case class StockPortfolio(val portfolioElements : Map[String, String]) {
  /**
   *  Calculate the total current value of the portfolio, given a set of stock quotes that were randomly
   *     generated elsewhere in the program
   *
   * @param quotes Basically a list of tickers with their current price as obtained elsewhere.  These are
   *                 used to price the portfolio
   * @return   The current value of the portfolio
   */
     def getPortfolioValue(quotes : => Iterable[StockQuote]) = {
           // Sum up the total value of the portfolio by:
           //   a) folding over all of the stocks to get a total sum of vaue
           //   b) for each stock, find the corresponding quote
           //   c) given the price in the quote, the fractional weight of the stock in the portfolio and
           //       and the total number of shares, calculate the amount of the value contributed by that stock
           portfolioElements.foldLeft(0.0)((totalVal, nextElem) => (totalVal +
             (nextElem match {
              case (ticker, numShares) => quotes.filter(_.ticker == ticker).headOption.map(_.value * numShares.toInt).getOrElse(0.0)
              case _ => 0.0
           })))
     }
}
