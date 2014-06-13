package com.sungard.poc.pricingengine.pricing

import com.sungard.poc.pricingengine.calculate.StockPriceSeries
import com.sungard.poc.pricingengine.random.GaussianGenerator
import com.sungard.poc.pricingengine.generators.{RNG, Gen}

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 1/31/14
 * Time: 10:43 PM
 * To change this template use File | Settings | File Templates.
 */

trait RandomProvider {
    def random(priceSeries : StockPriceSeries) : RNG
}

class StockQuoteGenerator(ticker : String, priceSeries : StockPriceSeries) {
  this : RandomProvider =>
   private val stockQuoteGenerator = (Gen.scanLeft(Gen.stream(Gen.uniform), priceSeries.prices.head)
    ((nextPrice, theReturn) => {
      if (theReturn > -1.0) nextPrice *(1.0 + theReturn) else 0.0
    }
  )).map(_.map(StockQuote(ticker, _)))


   def getStockQuotes : Seq[StockQuote] = {
      stockQuoteGenerator.sample.run(random(priceSeries))._1
  }
}
