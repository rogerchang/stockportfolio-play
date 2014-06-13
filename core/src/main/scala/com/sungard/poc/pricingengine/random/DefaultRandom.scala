package com.sungard.poc.pricingengine.random

import scala.util.Random

/**
 * Adds some implicits into scope, specifically a couple of randomizer objects that are useful.  In
 *    this case, I put the scala.util.Random and a random Gaussian generator into scope.
 *
 * User: bposerow
 * Date: 1/22/14
 * Time: 11:44 PM
 */
object DefaultRandom {
    implicit def random = new Random(System.currentTimeMillis())

    implicit def randomGaussianGenerator = RandomGaussianGenerator()
}
