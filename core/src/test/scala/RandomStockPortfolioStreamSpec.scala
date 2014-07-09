import com.sungard.poc.pricingengine.portfolio.RandomStockPortfolioStream
import com.sungard.poc.pricingengine.stock_data.StockList
import org.scalatest.fixture

import org.scalatest.FlatSpec
import com.sungard.poc.pricingengine.stock_data.StockList.stockList

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 1/27/14
 * Time: 10:30 PM
 * To change this template use File | Settings | File Templates.
 */
class RandomStockPortfolioStreamSpec extends FlatSpec {
  "Single portfolio" should "have reasonable values" in {
       val portfolioStream = new RandomStockPortfolioStream(1, 5, 10, 20)
       val stockList = StockList()



      val portfolio = portfolioStream.nextPortfolio
       portfolio match {
           case Some(actualPortfolio) => {
                 assert(! actualPortfolio.portfolioElements.isEmpty)
                 actualPortfolio.portfolioElements.map {
                   case (stock, shares) =>  {
                        assert(stockList.stocks.contains(stock))
                        assert(shares.toInt >= 0.0)
                   }
                 }
           }
           case _ => fail("Did not find a portfolio")
       }

       assert(portfolioStream.nextPortfolioSize() > 0)

  }
}
