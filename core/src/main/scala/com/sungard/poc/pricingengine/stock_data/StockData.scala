package com.sungard.poc.pricingengine.stock_data

import java.util.Date

/**
 * Simple structure with a stock ticker symbol and a list of historical price data for the stock
 *
 * User: bposerow
 * Date: 10/18/13
 * Time: 10:45 PM
 */
case class StockData(symbol : String, stockPriceRows : List[HistoricalStockPriceRow])
