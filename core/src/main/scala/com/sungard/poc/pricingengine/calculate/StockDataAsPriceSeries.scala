package com.sungard.poc.pricingengine.calculate

import com.sungard.poc.pricingengine.stock_data.StockData

/**
 * Implicit conversion of StockData, which simply contains historical data for a stock, into a price series,
 *    which allows statistical measures to be calculated from this historical data
 *
 * User: bposerow
 * Date: 3/10/14
 * Time: 4:03 PM
 */
object StockDataAsPriceSeries {
  /**
   * Implicit conversion to a series of closing prices from which statistical measures can be calculated
   *
   * @param stockData  A container for historical price data for the stock
   * @return
   */
      implicit def convertStockDataToPriceSeries(stockData : StockData) = {
           // Note that the close prices are selected to form the price series
           StockPriceSeries(stockData.stockPriceRows.map(historicalRow => historicalRow.close))
      }
}
