package com.sungard.poc.pricingengine.calculate

import scala.util.Random
import com.sungard.poc.pricingengine.random.{GaussianGenerator}
import scalaz._

/**
 * Represents a series of historical prices (e.g. observed on Yahoo Finance) from which historical measures
 *     can be calculated
 *
 * User: bposerow
 * Date: 11/11/13
 * Time: 3:10 PM
 */
case class StockPriceSeries(val prices : Seq[Double]) {
      /**
       * Convert the raw prices into returns by computing the fractional increase between successive prices
       * The trick below with zipping the list without the first element with the same list without the
       *   last element gets pairs of successive elements which can be used to accomplish this.
       */
      val returns : Seq[Double] = {
           prices.tail.zip(prices.init).map {
             case (endPrice, startPrice) => (endPrice - startPrice) / startPrice
           };
      }

      /**
       * Get the average return across all of the historical returns
       */
      val meanReturn : Double = {
            returns.sum / returns.size
      };

      /**
       * Get the standard deviation of the returns.
       */
      val stdDevReturn : Double = math.sqrt(returns.map(aReturn => math.pow(aReturn - meanReturn, 2.0)).sum / returns.size)

      /**
       * Get the current return which is just the last return observed
       */
      val currReturn = returns.last
}
