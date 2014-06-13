package com.sungard.poc.pricingengine.random

import scalaz._
import Scalaz._
import scala.util.Random

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 11/11/13
 * Time: 9:48 PM
 * To change this template use File | Settings | File Templates.
 */
trait RandomState {
    //def getGaussianRandom(mean : Double, stdDev : Double, )

    /*
    def getGaussianRandom(mean : Double, stdDev : Double) : State[Random, Double] = {
          gets[Random, Double] {
            rnd : Random =>   rnd.nextGaussian() * stdDev + mean
          }
    }
    */

    /*
       def freqSum(dice: (Int, Int)) = state[Map[Int,Int], Int]{ freq =>
  val s = dice._1 + dice._2
  val tuple = s -> (freq.getOrElse(s, 0) + 1)
  (freq + tuple, s)
}
     */

    /*
    def getGaussianRandomStateStream(mean : Double, stdDev : Double) : Stream[State[Random, Double]] = {
        (getGaussianRandom(mean, stdDev) #:: getGaussianRandomStateStream(mean, stdDev))
    }


    def getGaussianRandomStream(mean : Double, stdDev : Double) : State[Random, Stream[Double]] = {
        type StateRandom[x] = State[Random,x]
        getGaussianRandomStream(mean, stdDev).sequence[StateRandom, Stream[Double]]
    }
    */


   /*
   def addToGaussianRandomStream(nextVal : Double) = State[(Random, Stream[Double]), Double]
   {tuple => ((tuple._1, nextVal #:: tuple._2), nextVal) }
   */

    /*
    def getGaussianRandoms(mean : Double, stdDev : Double) : State[(Random, Stream[Double]), Double] = {
        getGaussianRandomStream(mean, stdDev)
    }
    */
}
