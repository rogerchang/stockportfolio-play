package com.sungard.poc.pricingengine.calculate

/**
 * Library of implicit conversions
 *
 * User: bposerow
 * Date: 1/23/14
 * Time: 10:42 PM
 */
class NumberConversions {
  /**
   * Convert an integer to a double implicitly
   *
   * @param int
   * @return
   */
     implicit def convertToDouble(int : Int) = {
         int.toDouble
     }
}
