package com.sungard.poc.pricingengine.portfolio

import com.sungard.poc.pricingengine.random.{RandomGaussianGenerator}
import com.sungard.poc.pricingengine.stock_data.StockList
import scala.util.Random
import com.sungard.poc.pricingengine.generators.{RNG, Gen}
import com.sungard.poc.pricingengine.generators.RNG.ScalaRandom

/**
 * Represents an infinite stream of stock portfolios that are randomly generated.  By randomly
 *   generated, I mean portfolios that:
 *
 *   a)  Have a random number of stock tickers constituting them between minStocks and maxSTocks
 *   b)  Each of those stock tickers are randomly generated from the complete list of stock tickers
 *   c)  Generate a random number of total shares of stock in the portfolio between minShares and maxShares
 *
 * User: bposerow
 * Date: 1/22/14
 * Time: 11:16 PM
 */

/**
 *
 * @param minStocks  The minimum number of stock tickers that can be in my randomly generated stock portfolio
 * @param maxStocks  The maximum number of stock tickers that can be in my randomly generated stock portfolio
 * @param minShares  The minimum number of total shares of stock contained within the portfolio
 * @param maxShares  The maximum number of total shares of stock contained within the portfolio
 * @param stockList  The list of stock tickers, should be made available to this class implicitly
 */
class RandomStockPortfolioStream(minStocks : Integer, maxStocks : Integer,
                                         minShares : Integer, maxShares : Integer)
                                        (implicit val stockList : StockList)
                                       extends StockPortfolioStream {
   // Note this is making use of a Generator, which is a monad that is similar to what is available in ScalaCheck
   //  library.  The purpose of the generator is to randomly generate values appropriate to the types requested.
   //  In this case,
   val portfolioGenerator = for {
              // Choose a number between min and max num of stock tickers that will be the number of stock
              //    tickers in our portfolio
              portfolioSize <- Gen.choose(minStocks, maxStocks)
              // Randomly permute the list of stocks
              randomizedStocks <- Gen.permutedList(stockList.stocks.toList)
              // By taking the chosen number of stock tickers from the head of our randomly permuted list, we
              //    will be effectively be taking portfolioSize random stock tickers
              selectedStocks  = randomizedStocks.take(portfolioSize)
              // Choose a random double for each of our stock tickers, that will be used to determine which percentage
              //   of the portfolio is represented by each stock
              rawProportionVals <- Gen.listOfN(portfolioSize, Gen.choose(0.0, 100.0))
              // Normalize those random values so that they add up to 1 and therefore give random proportions
              //   of each stock in the portfolio
              proportions = rawProportionVals.map(_.toString)
              // Choose a random value for the number of shares in the portfolio
              numShares <- Gen.choose(minShares, maxShares)
            } yield (StockPortfolio(selectedStocks.zip(proportions).toMap))

  /**
   * Just generate a random size for the portfolio
   */
   val portfolioSizeGenerator = for {
      portfolioSize <- Gen.choose(minStocks, maxStocks)
   }  yield portfolioSize;

    def nextPortfolio: Option[StockPortfolio] = {
        nextPortfolio(ScalaRandom())
    }

  /**
   * Run the generator for generating random portfolios by giving it a random number generator and
   *    hence producing a random portfolio according to the rules described above
   *
   * @param rng A random number generator
   * @return
   */
    def nextPortfolio(rng : RNG) = {
      Some(portfolioGenerator.sample.run(rng)._1)
    }

    def nextPortfolioSize() = {
       portfolioSizeGenerator.sample.run(ScalaRandom())._1
    }
}
