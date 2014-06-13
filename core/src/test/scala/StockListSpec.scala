import com.sungard.poc.pricingengine.stock_data.StockList
import org.scalatest.FlatSpec

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 1/22/14
 * Time: 10:34 PM
 * To change this template use File | Settings | File Templates.
 */
class StockListSpec extends FlatSpec {
  "Stock list" should "have reasonable stocks" in {
        val theStocks = StockList("symbol_list").stocks
        assert(! theStocks.isEmpty)
        assert(theStocks.contains("AAPL"))
  }
}
