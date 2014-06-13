package com.sungard.poc.pricingengine.actors

import com.sungard.poc.pricingengine.pricing.StockQuoteGenerator
import com.sungard.poc.pricingengine.calculate.StockPriceSeries
import com.sungard.poc.pricingengine.stock_data.StockData
import com.sungard.poc.pricingengine.calculate.StockDataAsPriceSeries._

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 3/10/14
 * Time: 3:52 PM
 * To change this template use File | Settings | File Templates.
 */
trait StockQuoteGeneratorProvider {
      def createStockQuoteGenerator(ticker : String, priceSeries : StockPriceSeries) : StockQuoteGenerator


      def stockQuoteGenerator(ticker : String, stockData : StockData) : StockQuoteGenerator = {
           createStockQuoteGenerator(ticker, stockData)
      }
}
