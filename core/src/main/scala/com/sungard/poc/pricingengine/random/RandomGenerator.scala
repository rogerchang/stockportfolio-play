package com.sungard.poc.pricingengine.random

import scala.util.Random

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 11/11/13
 * Time: 10:19 PM
 * To change this template use File | Settings | File Templates.
 */
class RandomGenerator(val mean : Double, val stdDev : Double, val random : Random) extends RandomState {
    /*
    def getGaussianVals(mean : Double, stdDev : Double) = {
         getGaussianRandoms(mean, stdDev)(random)._2
     }
     */
}
