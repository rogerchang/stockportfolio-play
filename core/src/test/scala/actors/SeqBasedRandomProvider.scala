package actors

import com.sungard.poc.pricingengine.pricing.RandomProvider
import com.sungard.poc.pricingengine.calculate.StockPriceSeries
import com.sungard.poc.pricingengine.generators.RNG

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 3/19/14
 * Time: 11:02 PM
 * To change this template use File | Settings | File Templates.
 */
trait SeqBasedRandomProvider extends RandomProvider {
  this : SeqProvider[Double] =>

  def random(priceSeries : StockPriceSeries): RNG = {
    new RNG() {

      def nextDouble : (Double, RNG) = {
        val nextElem = seq(0)
        seq = seq.tail
        (nextElem, this)
      }
    }
  }
}