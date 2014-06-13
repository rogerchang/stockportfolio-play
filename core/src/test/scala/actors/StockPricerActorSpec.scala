package actors

import akka.testkit.{TestProbe, ImplicitSender, TestKit}
import akka.actor._
import org.scalatest.{Matchers, WordSpecLike}
import com.sungard.poc.pricingengine.actors.{ServiceLookupActor, StockPricerActor, StockQuoteGeneratorProvider}
import com.sungard.poc.pricingengine.calculate.StockPriceSeries
import com.sungard.poc.pricingengine.pricing.{StockQuote, StockQuoteGenerator}
import com.sungard.poc.pricingengine.actors.StockPricerActor._
import com.sungard.poc.pricingengine.actors.StockDataRetrieverActor.{StockDataRetrieved, RetrieveStockData}
import com.sungard.poc.pricingengine.stock_data.{HistoricalStockPriceRow, StockData}
import com.sungard.poc.pricingengine.calculate.StockDataAsPriceSeries._
import org.joda.time.DateTime
import com.sungard.poc.pricingengine.actors.ServiceLookupActor.{DataRetrieverReturned, GetDataRetriever}
import com.sungard.poc.pricingengine.actors.StockDataRetrieverActor.RetrieveStockData
import com.sungard.poc.pricingengine.stock_data.HistoricalStockPriceRow
import com.sungard.poc.pricingengine.pricing.StockQuote
import com.sungard.poc.pricingengine.calculate.StockPriceSeries
import com.sungard.poc.pricingengine.stock_data.StockData
import com.sungard.poc.pricingengine.actors.StockDataRetrieverActor.StockDataRetrieved
import com.sungard.poc.pricingengine.actors.ServiceLookupActor.DataRetrieverReturned
import com.sungard.poc.pricingengine.actors.StockDataRetrieverActor.RetrieveStockData
import com.sungard.poc.pricingengine.stock_data.StockData
import com.sungard.poc.pricingengine.actors.StockPricerActor.CurrentStockQuote
import com.sungard.poc.pricingengine.stock_data.HistoricalStockPriceRow
import com.sungard.poc.pricingengine.actors.StockDataRetrieverActor.StockDataRetrieved
import com.sungard.poc.pricingengine.actors.ServiceLookupActor.DataRetrieverReturned
import com.sungard.poc.pricingengine.pricing.StockQuote
import com.sungard.poc.pricingengine.calculate.StockPriceSeries

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 3/19/14
 * Time: 10:53 PM
 * To change this template use File | Settings | File Templates.
 */
class StockPricerActorSpec extends TestKit(ActorSystem("stockPricerActorSpec"))
        with WordSpecLike with Matchers with ImplicitSender{
      val ibmStockData = StockData("IBM", List(
          HistoricalStockPriceRow(DateTime.parse("2013-11-01"), 20.5, 31.0, 15.0, 22.5),
          HistoricalStockPriceRow(DateTime.parse("2013-12-15"), 24.5, 27.0, 21.0, 21.0),
          HistoricalStockPriceRow(DateTime.parse("2013-12-30"), 40.0, 45.0, 35.0, 37.0)))

       private trait TestStockQuoteGeneratorProvider extends StockQuoteGeneratorProvider {
         def createStockQuoteGenerator(ticker : String, priceSeries : StockPriceSeries) : StockQuoteGenerator = {
             new StockQuoteGenerator(ticker, priceSeries)
                    with SeqBasedRandomProvider with SeqProvider[Double] {
                var seq : Seq[Double] = List(0.25, 0.5, -0.1, -0.5, 0.5)
           }
         }
       }

       private class TestDataRetrieverActor extends Actor {
            def receive : Receive = {
                case RetrieveStockData(ticker : String, originator : ActorRef) => {
                    sender ! StockDataRetrieved(ibmStockData, originator)
                }
            }
       }

       private class TestLookup extends Actor {
           def receive : Receive = {
             case GetDataRetriever => {
                val dataRetrieverActor = system.actorOf(Props(new TestDataRetrieverActor()))
                sender ! DataRetrieverReturned(dataRetrieverActor)
             }
           }
       }

       //val dataRetrieverProbe = TestProbe()

       def createPricerActor(ticker : String) : ActorRef = {
            val testLookupActor = system.actorOf(Props(new TestLookup()), "testLookup")
            val stockPricerDataRetriever = system.actorOf(Props(
                        new DefaultStockPricerDataRetriever(
                          system.actorSelection("/user/testLookup"), ticker)), "dataRetrieverActor")

            system.actorOf(Props(new StockPricerActor(ticker,
                    stockPricerDataRetriever)
                            with TestStockQuoteGeneratorProvider), "pricerActor")
       }

      "Stock pricer actor" should {
        "successfully return stock prices" in {
            val stockPricerActor = createPricerActor("IBM")
            stockPricerActor ! Initialize
            //expectMsgType[DataRetrieverReturned]
            stockPricerActor ! RetrieveStockQuote
            val priceSeries : StockPriceSeries = ibmStockData
            val firstPrice = priceSeries.prices.head
            val retStockQuote = this.expectMsgType[CurrentStockQuote].stockQuote
            assert(retStockQuote.ticker === "IBM")
            assert(retStockQuote.value === firstPrice)
            stockPricerActor ! RetrieveStockQuote
            expectMsg(CurrentStockQuote(StockQuote("IBM", firstPrice * 1.25)))
            stockPricerActor ! RetrieveStockQuote
            expectMsg(CurrentStockQuote(StockQuote("IBM", firstPrice * 1.25 * 1.5)))
            stockPricerActor ! RetrieveStockQuote
            expectMsg(CurrentStockQuote(StockQuote("IBM", firstPrice * 1.25 * 1.5 * 0.9)))
        }
      }

}
