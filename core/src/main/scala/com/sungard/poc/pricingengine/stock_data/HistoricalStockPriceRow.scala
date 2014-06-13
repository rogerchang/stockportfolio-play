package com.sungard.poc.pricingengine.stock_data

import java.util.Date
import org.joda.time.DateTime

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 10/18/13
 * Time: 10:52 PM
 * To change this template use File | Settings | File Templates.
 */
case class HistoricalStockPriceRow(date : DateTime, open : Double, high : Double, low : Double, close : Double)
