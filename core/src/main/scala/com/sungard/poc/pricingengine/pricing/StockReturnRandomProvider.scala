package com.sungard.poc.pricingengine.pricing

import com.sungard.poc.pricingengine.generators.RNG.{GaussianRandom}
import com.sungard.poc.pricingengine.calculate.StockPriceSeries
import com.sungard.poc.pricingengine.generators.RNG

/**
 * An interface that given a price series, i.e. a statistical distribution based on observed stock
 *    price data, it returns a random number generator based on that observed statistical distribution.
 *    By default, it returns a Gaussian generator with the observed mean return and standard deviation
 *
 * User: bposerow
 * Date: 2/2/14
 * Time: 8:52 PM
 */
trait StockReturnRandomProvider extends RandomProvider {
  /**
   *
   * @param priceSeries  a statistical distribution based on observed stock
   *    price data
   * @return A random number generator that is generated from that statistical distribution
   */
  def random(priceSeries : StockPriceSeries): RNG =
            GaussianRandom(priceSeries.meanReturn, priceSeries.stdDevReturn)
}
