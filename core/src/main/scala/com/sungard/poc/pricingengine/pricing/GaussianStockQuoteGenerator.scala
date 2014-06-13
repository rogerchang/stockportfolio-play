package com.sungard.poc.pricingengine.pricing

import com.sungard.poc.pricingengine.calculate.StockPriceSeries

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 2/2/14
 * Time: 8:57 PM
 * To change this template use File | Settings | File Templates.
 */
class GaussianStockQuoteGenerator (ticker : String, priceSeries : StockPriceSeries)
        extends StockQuoteGenerator(ticker, priceSeries) with StockReturnRandomProvider {

}
