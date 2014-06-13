package com.sungard.poc.pricingengine.stock_data

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 10/22/13
 * Time: 10:37 PM
 * To change this template use File | Settings | File Templates.
 */
object StockDataFields extends Enumeration {
     type StockDataFields = Value
     val Ticker, Ask, Bid, MarketCap, Volume, TradeDate, Low, High, Mean = Value
}
