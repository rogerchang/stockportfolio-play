import com.sungard.poc.pricingengine.calculate.StockPriceSeries
import com.sungard.poc.pricingengine.generators.RNG
import com.sungard.poc.pricingengine.pricing.{RandomProvider, StockQuoteGenerator}
import org.scalatest.FlatSpec

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 2/2/14
 * Time: 8:33 PM
 * To change this template use File | Settings | File Templates.
 */
class StockQuoteGeneratorSpec  extends FlatSpec {
      trait SeqBasedRandomProvider extends RandomProvider {

        def random(priceSeries : StockPriceSeries): RNG = {
          new RNG() {
            var list = List(0.25, 0.5, -0.1, -0.5, 0.5)

            def nextDouble : (Double, RNG) = {
                  val nextElem = list(0)
                  list = list.tail
                  (nextElem, this)
            }
          }
        }
      }

     /* class TestStockQuoteGenerator(ticker : String, priceSeries : StockPriceSeries) extends StockQuoteGenerator(ticker, priceSeries)
                   with
      */

      "Generated stock quotes" should "have reasonable values" in {
            val generator = new StockQuoteGenerator("IBM", StockPriceSeries(Seq(12.0, 13.2, 5.7))) with SeqBasedRandomProvider
            val sampleQuotes = generator.getStockQuotes.take(5).toList
              //case Some(quotes) => {
              //    val sampleQuotes = quotes.take(5).toList
            sampleQuotes.foreach[Unit](quote => "IBM".equals(quote.ticker))
            assert(sampleQuotes.map(_.value).sameElements(Seq(12.0, 15.0, 22.5, 20.25, 10.125)))
              //}
              //case None => fail("Should have found quotes")
            //}
      }
}
