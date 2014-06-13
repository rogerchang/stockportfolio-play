package com.sungard.poc.pricingengine.stock_data

import com.github.nscala_time.time.Imports._
import org.joda.time.{DurationFieldType, ReadableDuration}

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 10/18/13
 * Time: 10:43 PM
 * To change this template use File | Settings | File Templates.
 */
trait StockDataRetriever {
  def retrieve(ticker : String, startDate : DateTime, endDate : DateTime) : StockData
  def retrieve(ticker : String, daysBack : Integer) : StockData = {
       retrieve(ticker, DateTime.now.withFieldAdded(DurationFieldType.days(), -daysBack),
         DateTime.now)
  }
}
