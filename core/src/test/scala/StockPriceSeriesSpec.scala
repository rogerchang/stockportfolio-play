import com.sungard.poc.pricingengine.calculate.StockPriceSeries
import com.sungard.poc.pricingengine.random.GaussianGenerator
import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 1/7/14
 * Time: 11:08 PM
 * To change this template use File | Settings | File Templates.
 */
object StockPriceSeriesSpec {
    private val TOLERANCE = 0.001

    /*
    private trait StockPriceSeriesProvider {
         def stockPriceSeries
    }

    private trait SimpleStockPriceSeriesProvider extends StockPriceSeriesProvider {
        override def stockPriceSeries = new StockPriceSeries(List(4.5, 2.0, 3.5, 7.5, 2.5, 5.0, 10.0))
    }
    */

    class TestGaussianGenerator(var gaussianVals : Seq[Double]) extends GaussianGenerator {
            override def nextGaussian : Option[Double] = {
                gaussianVals match {
                  case head :: tail => {
                    gaussianVals = tail
                    Some(head)
                  }
                  case _ => None
                }
            }
    }


    trait StockPriceSeriesBehavior {  this: FlatSpec =>
            private def matchesWithinTolerance(actualVal : Double, expectedVal : Double) : Unit = {
                 assert((actualVal - expectedVal).abs < TOLERANCE)
            }

            def shouldHaveMeanReturn(stockPriceSeries : StockPriceSeries, meanReturn : Double) = {
                   matchesWithinTolerance(stockPriceSeries.meanReturn, meanReturn)
            }

            def shouldHaveStdDev(stockPriceSeries : StockPriceSeries, stdDev : Double) = {
                  matchesWithinTolerance(stockPriceSeries.stdDevReturn, stdDev)
            }

            def shouldHaveExpectedReturns(stockPriceSeries : StockPriceSeries, firstReturn : Double, lastReturn : Double) = {
                  matchesWithinTolerance(stockPriceSeries.returns.head, firstReturn)
                  matchesWithinTolerance(stockPriceSeries.returns.last, lastReturn)
                  assert(stockPriceSeries.returns.size === (stockPriceSeries.prices.size - 1))
            }

            /*
            def shouldHaveExpectedPrices(stockPriceSeries : StockPriceSeries,
                                         gaussianGenerator : GaussianGenerator,
                                         expectedPrices : Seq[Double]) = {
                 val randomPricesOption = stockPriceSeries.getRandomPrices/*(expectedPrices.length)*/(gaussianGenerator)
                 randomPricesOption.map(randomPricesStream => randomPricesStream.take(expectedPrices.length).zip(expectedPrices).foreach[Unit](
                   tuple => matchesWithinTolerance(tuple._1, tuple._2)))
            }
            */
    }
}

import StockPriceSeriesSpec._


@RunWith(classOf[JUnitRunner])
class StockPriceSeriesSpec extends FlatSpec with StockPriceSeriesBehavior {
   val gaussianGenerator : GaussianGenerator = new TestGaussianGenerator(List(0.2, -0.4, 1.3, 0.8, -1.2, 0.5))

   val stockPriceSeries = new StockPriceSeries(List(10.0, 15.0, 7.5, 12.5, 2.5))
   val expectedPrices = List(10.0, 10.0 * ((0.6285 * 0.2) + -0.0333333),
                            10.0 * 0.1)

    // 0.5  -0.5 0.66666  -0.8
   "Normal stock pricing series" should behave like shouldHaveMeanReturn(stockPriceSeries, -0.033333333333333)
   it should behave like shouldHaveExpectedReturns(stockPriceSeries, 0.5, -0.8)
   it should behave like shouldHaveStdDev(stockPriceSeries, 0.6285)
   /*
   it should behave like shouldHaveExpectedPrices(stockPriceSeries, gaussianGenerator, Seq(10.0,
  10.923667,
  7.8133352469889,
  13.9367765627527,
  20.4796290643056,
  4.3512392045171,
  5.57357496276067))
  */
}
