package com.sungard.poc.pricingengine.stock_data.yahoo

import com.sungard.poc.pricingengine.io.{URLComponents, CsvParser}
import com.sungard.poc.pricingengine.stock_data.{HistoricalStockPriceRow, StockData, StockDataRetriever}
import com.github.nscala_time.time.Imports._
import org.joda.time.DateTime.Property
import java.net.URL

/**
 * An implementation of a StockDataRetriever that is able to retrieve data from Yahoo Finance and return
 *    a StockData object, which represents the historical price information for a given stock ticker
 *
 * User: bposerow
 * Date: 10/17/13
 * Time: 8:51 PM
 */

// Simply an enumeration of Year, Month, and Day, this is helpful in extracting out the different fields
//   of the date as they are represented in Yahoo Finance
private[yahoo] object YahooDateFields extends Enumeration {
  type StockDataFields = Value
  val Year, Month, Day = Value
}

object YahooStockDataRetriever {
     import YahooDateFields._
     private val DATE_FORMAT = DateTimeFormat.forPattern("yyyy-MM-dd")

     // Base URL for getting Yahoo finance historical data in CSV format
     private val URL_PREFIX = "http://ichart.finance.yahoo.com/table.csv"

     // These are some of the GET variable names and values that must be provided to Yahoo finance
     //   to get the data that we want

     // Consider the FORMAT_VAL and FORMAT_VAL as sort of magic values that get the data in the desired format;
     //    this is part of hidden API of Yahoo finance
     private val FORMAT_VAR = "g"
     private val FORMAT_VAL = "d"

     // This is the variable which allows us to specify ticker value
     private val TICKER_VAR = "s"

     // GET variables that represent the month, day, and year fields, respectively, for the start date for
     //    which we want to get historical stock price data
     private val BEFORE_DATE_VARS = Map(Month -> "a", Day -> "b", Year -> "c")

    // GET variables that represent the month, day, and year fields, respectively, for the end date for
    //    which we want to get historical stock price data
    private val AFTER_DATE_VARS = Map(Month -> "d", Day -> "e", Year -> "f")

    // Just a utility conversion that is useful for dealing with Joda time library
    private implicit def asSeq(dtProperty : Property) = Seq(dtProperty.get().toString())

    // Extract month and convert it the way that Yahoo Finance wants it
    private def convertMonth(dt : DateTime) = {
        Seq((dt.monthOfYear().get() - 1).toString())
    }

    // Given a stock ticker, start date, and end date, form the URL (which is created via an
    //    implicit conversion from the URLComponents object)
    private def makeUrl(ticker : String, startDate : DateTime, endDate : DateTime)  = {
        URLComponents(prefix = YahooStockDataRetriever.URL_PREFIX,
          getParams = Map(
            FORMAT_VAR -> Seq(FORMAT_VAL),
            TICKER_VAR  -> Seq(ticker),
            BEFORE_DATE_VARS(Year) -> startDate.year(),
            BEFORE_DATE_VARS(Month) -> convertMonth(startDate),
            BEFORE_DATE_VARS(Day) -> startDate.dayOfMonth(),
            AFTER_DATE_VARS(Year) -> endDate.year(),
            AFTER_DATE_VARS(Month) -> convertMonth(endDate),
            AFTER_DATE_VARS(Day) -> endDate.dayOfMonth()
        ))

  }
}

/**
 * This class extends from CSVParser which gives us the ability to parse the CSV formatted result
 *    from Yahoo Finance
 */
class YahooStockDataRetriever extends CsvParser with StockDataRetriever {
        import YahooStockDataRetriever._
        import com.sungard.poc.pricingengine.io.URLConverter._

  /**
   * Retrieves the historical stock prices for a given ticker
   *
    * @param ticker  The stock ticker symbol
   * @param startDate   The start date for which we are going to obtain historical stock prices for the ticker
   * @param endDate   The end date for which we are going to obtain historical stock prices
   * @return
   */
        def retrieve(ticker : String, startDate : DateTime, endDate : DateTime) : StockData = {
              // makeURL returns URLComponents which is implicitly converted to a URL
              val url : URL = makeUrl(ticker, startDate, endDate)

              // Parse the results from the CSV results returned from Yahoo finance.  The results
              //    are in the format of date, open price, high price, low price, close price, so
              //    extract each of these fields and put in a HistoricalStockPriceRow, which represents
              //    the high, open, low, and close prices for a particular day
              val stockPriceRows = parse(url, skipHeaders = true)
                .map(fields => HistoricalStockPriceRow(
                date = DATE_FORMAT.parseDateTime(fields(0)),
                open = fields(1).toDouble,
                high = fields(2).toDouble,
                low = fields(3).toDouble,
                close = fields(4).toDouble
              )).toList.reverse
              // Roll up all of this historical price info into a StockData object
              StockData(symbol = ticker, stockPriceRows = stockPriceRows)
        }
}
