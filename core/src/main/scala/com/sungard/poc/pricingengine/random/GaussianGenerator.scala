package com.sungard.poc.pricingengine.random

import scala._
import scala.Some

/**
 * Allows generation of doubles according to a Gaussian distribution.
 *
 * User: bposerow
 * Date: 1/5/14
 * Time: 8:25 PM
 */
trait GaussianGenerator {
  /**
   * Left abstract, return a double according to a Gaussian distribution with mean and standard dev 1
   *
   * @return  Some(x) if we want to return x from the distribution, None if we want to stop returning
   *             values from the distribution
   */
     def nextGaussian : Option[Double]

  /**
   * Converts the values returned from a Gaussian distribution with mean and standard dev 1 and converts it
   *    to match arbitrary means and standard deviations
   *
   * @param mean  Mean of the distribution from which we want to return doubles
   * @param stdDev  Standard deviation of the distribution from which we want to return doubles
   * @return  A double from the Gaussian distribution with the specified mean and standard dev
   */
     def nextNormalizedGaussian(mean : Double, stdDev : Double) = {
         nextGaussian.map(_ * stdDev + mean)
     }

  /**
   * Provides a potentially infinite stream of values from a Gaussian distribution with specified mean
   *    and standard deviation
   *
   * @param mean
   * @param stdDev
   * @return
   */
     def normalizedGaussianStream(mean : Double, stdDev : Double) : Stream[Double] = {
       nextNormalizedGaussian(mean, stdDev) match {
         case Some(value) => value #:: normalizedGaussianStream(mean, stdDev)
         // If we return the None option, we stop returning values from the stream
         case None => Stream.empty
       }
     }
}
