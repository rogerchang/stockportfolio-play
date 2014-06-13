package com.sungard.poc.pricingengine.random

import scala.util.Random

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 1/22/14
 * Time: 11:20 PM
 * To change this template use File | Settings | File Templates.
 */
case class SkewedRandom(dict : Map[Int, (Double, Double)] = Map())(implicit random : Random) {
    def addRandom(value : Int, probability : Double) = {
         val totalProb = dict.values.map(_._2).max
         SkewedRandom(dict + (value -> (totalProb, totalProb + probability)))
    }

    def nextRandom = {
        val maxKey = dict.keys.max
        val randomNum = random.nextDouble()

        dict.find(_ match {
          case (key, (startPct, endPct)) => {
              randomNum >= startPct && randomNum < endPct
          }
        }) match {
          case Some((key, _)) => key
          case None => random.nextInt(maxKey) + 1
        }
    }

}
