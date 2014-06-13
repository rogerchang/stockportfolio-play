import com.github.nscala_time.time.Imports._
import com.sungard.poc.pricingengine.stock_data.HistoricalStockPriceRow
import com.sungard.poc.pricingengine.stock_data.yahoo.YahooStockDataRetriever
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 10/28/13
 * Time: 11:01 PM
 * To change this template use File | Settings | File Templates.
 */
object YahooStockDataRetrieverSpec {
      trait StockDataRetrieverFixture {
        val stockDataRetriever = new YahooStockDataRetriever();
      }

  trait ParsedFieldsBehavior extends StockDataRetrieverFixture { this: FlatSpec =>
    def checkFieldsValid(row : HistoricalStockPriceRow, date : DateTime) = {
        assert(row.close > 0.0)
        assert(row.high > 0.0)
        assert(row.low > 0.0)
        assert(row.open > 0.0)
        assert(date.equals(row.date))
    }

    def validParsingOfTheFile(ticker : String, startDate : DateTime, endDate : DateTime, numDays : Int) = {
      val stockData = stockDataRetriever.retrieve(ticker, startDate, endDate)

      it should "have some stock data" in {
          assert(stockData != null && ! stockData.stockPriceRows.isEmpty)
      }

      it should "have correct stock ticker" in {
          assert(ticker === stockData.symbol);
      }

      it should "have correct number of values" in {
         assert(stockData.stockPriceRows.length === numDays)
      }

      it should "have points for data at boundary" in {
         assert(startDate.equals(stockData.stockPriceRows(0).date))
         assert(endDate.equals(stockData.stockPriceRows.last.date))
      }

      it should "have reasonable data values for start date" in {
         val firstPoint = stockData.stockPriceRows(0)
         checkFieldsValid(firstPoint, startDate)
      }
    }
  }
}

import YahooStockDataRetrieverSpec._


@RunWith(classOf[JUnitRunner])
class YahooStockDataRetrieverSpec extends FlatSpec with ParsedFieldsBehavior {
      def ibmSingleDay() = {
        ("IBM", new DateTime("2013-10-28"), new DateTime("2013-10-28"))
      }

     def aaplMultipleDay() = {
      ("AAPL", new DateTime("2013-10-28"), new DateTime("2013-10-30"))
    }

  "Single day ticker retrieve" should behave like  validParsingOfTheFile(ibmSingleDay()._1, ibmSingleDay()._2,
                      ibmSingleDay()._3, 1)

  "Multi day ticker retrieve" should behave like  validParsingOfTheFile(aaplMultipleDay()._1, aaplMultipleDay()._2,
    aaplMultipleDay()._3, 3)
}
