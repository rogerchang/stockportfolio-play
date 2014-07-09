package actors

import akka.testkit.{TestProbe, ImplicitSender, TestKit}
import akka.actor.{Props, Actor, ActorRef, ActorSystem}
import org.scalatest.{Matchers, WordSpecLike}
import com.sungard.poc.pricingengine.actors.{AskTimeoutProvider, StockPortfolioValuationActor, StockPortfolioGeneratorActor}
import com.sungard.poc.pricingengine.portfolio.StockPortfolio
import com.sungard.poc.pricingengine.actors.StockPortfolioValuationActor.{PortfolioValued, GetPortfolioValuation}
import com.sungard.poc.pricingengine.actors.StockDirectoryActor.{StockPricerRetrieved, GetStockPricer}
import com.sungard.poc.pricingengine.actors.StockPricerActor.{CurrentStockQuote, RetrieveStockQuote}
import com.sungard.poc.pricingengine.pricing.StockQuote

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 2/23/14
 * Time: 10:58 PM
 * To change this template use File | Settings | File Templates.
 */
class StockPortfolioValuationActorSpec extends TestKit(ActorSystem("stockPortfolioGeneratorSpec"))
         with WordSpecLike with Matchers with ImplicitSender {
      val stockDirectory = TestProbe()

      def makeActor() : ActorRef = {
         system.actorOf(StockPortfolioValuationActor.props(stockDirectory.ref))
      }

      private object TestPricer {
          def apply(ticker : String, price : Double) = {
              new TestPricer(ticker, price)
          }

        def props(ticker : String, price : Double) : Props = Props(TestPricer(ticker, price))

      }

      private class TestPricer(ticker : String, price : Double) extends Actor {
        def receive: Receive = {
          case RetrieveStockQuote => {
               sender ! CurrentStockQuote(StockQuote(ticker, price))
          }
        }
      }

  "Stock portfolio valuation actor" should {
    "value a multi stock portfolio with correct interactions" in {
        val stockPortfolioValuationActor = makeActor()
        val testPortfolio = StockPortfolio(Map("IBM" -> "40", "AAPL" -> "60"))
        stockPortfolioValuationActor !  GetPortfolioValuation(testPortfolio)

        val ibmPricer = system.actorOf(TestPricer.props("IBM", 40.0))
        val aaplPricer = system.actorOf(TestPricer.props("AAPL", 450.0))


      //val applPricer = TestProbe()
        stockDirectory.expectMsg(GetStockPricer("IBM"))
        stockDirectory.reply(StockPricerRetrieved("IBM", ibmPricer))
        stockDirectory.expectMsg(GetStockPricer("AAPL"))
        stockDirectory.reply(StockPricerRetrieved("AAPL", aaplPricer))

        //ibmPricer.expectMsg(GetStockQuote("IBM"))
        //ibmPricer.expectMsg(GetStockQuote("AAPL"))
        //ibmPricer.reply(StockQuote("IBM", 40.0))
        //ibmPricer.reply(StockQuote("AAPL", 450.0))

        expectMsg(PortfolioValued(testPortfolio, 28600.0))
      }
    }
}
