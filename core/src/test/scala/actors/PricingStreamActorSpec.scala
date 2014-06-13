package actors

import akka.testkit.{TestProbe, ImplicitSender, TestKit}
import akka.actor.ActorSystem
import org.scalatest.{Matchers, WordSpecLike}
import com.sungard.poc.pricingengine.actors.StockDataRetrieverActor.{StockDataRetrieved, RetrieveStockData}
import com.sungard.poc.pricingengine.actors.{PricingEventSubscription, PricingEvent, PricingStreamEventBus, PricingStreamActor}
import scala.concurrent.duration._
import com.sungard.poc.pricingengine.portfolio.StockPortfolio
import com.sungard.poc.pricingengine.actors.PricingStreamActor.RegisterPricingStream
import com.sungard.poc.pricingengine.actors.StockPortfolioValuationActor.{PortfolioValued, GetPortfolioValuation}
import akka.routing.Routees

/**
 * Created by admin on 4/16/14.
 */
class PricingStreamActorSpec extends TestKit(ActorSystem("pricingStreamActorSpec"))
with WordSpecLike with Matchers with ImplicitSender {
  private val stockValuationActor = TestProbe()

  def createPricingStreamActor = {
      val pricingEventBus = new PricingStreamEventBus(100);
      system.actorOf(PricingStreamActor.props(stockValuationActor.ref, pricingEventBus, 500.milliseconds))
  }



  "Stock data retriever actor" should {
    "retrieve the expected StockData" in {
      val pricingStreamActor = createPricingStreamActor
      val testPortfolio = StockPortfolio(List(("IBM", 0.4), ("AAPL", 0.6)), 100)
      val prices = List(30.0, 20.0, 15.0, 25.0, 22.5)
      pricingStreamActor ! RegisterPricingStream(testActor, testPortfolio)
      var subscription: PricingEventSubscription = null;

      expectMsgPF() {
        case p@PricingEventSubscription(testActor, _, testPortfolio) => {
          subscription = p;
        }
      }

      for (i <- 0 to prices.length-1) {
        within(750.milliseconds) {
          stockValuationActor.expectMsg(GetPortfolioValuation(testPortfolio))
          stockValuationActor.reply(PortfolioValued(testPortfolio, prices(i)))
          this.expectMsgPF() {
            case PricingEvent(_, portfolio, portfolioValueMsg) => {
              assert(portfolio == testPortfolio)
              assert(portfolioValueMsg.value == prices(i))
            }
            case _ => fail("Unexpected message")
          }
        }
      }
      subscription.close;
      stockValuationActor.expectNoMsg();
      this.expectNoMsg(1.second)
    }
  }
}
