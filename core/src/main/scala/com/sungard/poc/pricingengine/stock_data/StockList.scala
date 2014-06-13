package com.sungard.poc.pricingengine.stock_data

import scala.io.Source
import com.sungard.poc.pricingengine.io.CsvParser

/**
 * Represents a set of available stock tickers, in this case from a file containing them
 *
 * User: bposerow
 * Date: 1/22/14
 * Time: 10:05 PM
 */
object StockList {
  /**
   * Allows us to construct our stock list from a CSV file.  This is a bit flawed because it is
   *    hardcoded to the format of a file format I happened to include in this project with a list of
   *    all NASDAQ symbols.  This format happens to have all of the symbols in the first column of a pipe
   *    delimited file, so the logic is as such here.
   *
   * @param fileName  File name relative to the classpath
   * @return
   */
   def apply(fileName : String) : StockList = {
       val parsedLines = new CsvParser {}.parse(Source.fromURL(getClass()
         .getResource(fileName)), "|", true)
       StockList(parsedLines.map(arr => arr(0)).toSet)
   }

  /**
   * Construct the stock list from the NASDAQ stock list file I happen to have on the classpath of this project
   *
   * @return
   */
   def apply() : StockList = this("/symbol_list")

  /**
   * Put the stock list in scope
   */
   implicit val stockList = StockList()
}

/**
 *
 * @param stocks Set of stock tickers in the list
 */
case class StockList(val stocks : Set[String])
