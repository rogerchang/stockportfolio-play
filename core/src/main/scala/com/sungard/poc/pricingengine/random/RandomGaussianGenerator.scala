package com.sungard.poc.pricingengine.random

import scala.util.Random
import com.sungard.poc.pricingengine.random.DefaultRandom._

/**
 * A Gaussian distribution that returns values using Scala's random Gaussian distribution.
 *
 * User: bposerow
 * Date: 1/5/14
 * Time: 8:42 PM
 */

object RandomGaussianGenerator {
    def apply() = new RandomGaussianGenerator()
}

/**
 *
 *
 * @param random  Scala Random object that is implicitly in scope
 */
class RandomGaussianGenerator(implicit random : Random) extends GaussianGenerator {
  /**
   * Note each Gaussian value is simply returned from the Scala.Random nextGaussian function
   *
   * @return  Some(x) if we want to return x from the distribution, None if we want to stop returning
   *          values from the distribution
   */
  def nextGaussian: Option[Double] = Some(random.nextGaussian())
}
