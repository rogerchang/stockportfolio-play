package actors

import akka.testkit.{ImplicitSender, TestKit}
import akka.actor.{ActorSelection, ActorSystem}
import org.scalatest.{Matchers, BeforeAndAfterAll, WordSpecLike}
import com.sungard.poc.pricingengine.actors._
import com.sungard.poc.pricingengine.actors.StockPortfolioGeneratorActor.{PortfolioGenerated, GeneratePortfolio}
import com.sungard.poc.pricingengine.actors.StockPortfolioValuationActor.{PortfolioValued, GetPortfolioValuation}
import com.sungard.poc.pricingengine.actors.ServiceLookupActor._
import com.sungard.poc.pricingengine.actors.StockPortfolioValuationActor.GetPortfolioValuation
import com.sungard.poc.pricingengine.actors.StockPortfolioValuationActor.PortfolioValued
import com.sungard.poc.pricingengine.actors.ServiceLookupActor.StockDirectoryReturned
import com.sungard.poc.pricingengine.actors.StockPortfolioGeneratorActor.PortfolioGenerated
import com.sungard.poc.pricingengine.actors.PricingStreamActor.RegisterPricingStream
import com.sungard.poc.pricingengine.portfolio.StockPortfolio
import com.sungard.poc.pricingengine.actors.StockPortfolioValuationActor.GetPortfolioValuation
import com.sungard.poc.pricingengine.actors.StockPortfolioValuationActor.PortfolioValued
import com.sungard.poc.pricingengine.actors.ServiceLookupActor.StockPriceStreamerReturned
import com.sungard.poc.pricingengine.actors.ServiceLookupActor.StockPortfolioValuerReturned
import com.sungard.poc.pricingengine.actors.PricingStreamActor.RegisterPricingStream
import com.sungard.poc.pricingengine.portfolio.StockPortfolio
import com.sungard.poc.pricingengine.actors.StockPortfolioGeneratorActor.PortfolioGenerated
import com.typesafe.config.ConfigFactory

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 3/25/14
 * Time: 10:13 PM
 * To change this template use File | Settings | File Templates.
 */
class StockPricingEndToEndSpec extends TestKit(ActorSystem("StockPricingEndToEndSpec",
  ConfigFactory.parseString("""
                               akka {
                                  actor {
                                      deployment {
                                          /ServiceLookup/StockDirectoryRouterActor/stockDirectoryRouter {
                                             router = consistent-hashing-pool
                                             nr-of-instances = 10
                                             virtual-nodes-factor = 10
                                          }
                                     }
                                  }
                               }
                            """).withFallback(ConfigFactory.load())

))
    with WordSpecLike with Matchers with ImplicitSender with BeforeAndAfterAll {

  ServiceLookupActor.createServiceLookupActor(system)

  "Randomly generated portfolio" should {
    "be successfully priced" in {

      var serviceLookupActor : ActorSelection = ServiceLookupActor.getServiceLookupActor(system)

      serviceLookupActor ! GetStockPortfolioValuer
      val stockPortfolioValuationActor = expectMsgType[StockPortfolioValuerReturned].portfolioValuer

      val portfolioGeneratorActor = system.actorOf(StockPortfolioGeneratorActor.props())

        portfolioGeneratorActor ! GeneratePortfolio
        val stockPortfolio = expectMsgType[PortfolioGenerated].portfolio
        stockPortfolioValuationActor ! GetPortfolioValuation(stockPortfolio)
        val portfolioValuedMessage = expectMsgType[PortfolioValued]
        assert(portfolioValuedMessage.value > 0 && portfolioValuedMessage.stockPortfolio == stockPortfolio)
    }
  }

  "Specific portfolio" should {
    "show stream prices correctly" in {
      var serviceLookupActor : ActorSelection = ServiceLookupActor.getServiceLookupActor(system)
      serviceLookupActor !  GetStockPriceStreamer
      val stockPriceStreamerActor = expectMsgType[StockPriceStreamerReturned].stockPriceStreamer
      val testPortfolio = StockPortfolio(Map("IBM" -> "40", "AAPL" -> "60"))
      stockPriceStreamerActor ! RegisterPricingStream(testActor, testPortfolio)
      val subscription = expectMsgType[PricingEventSubscription]

      var lastValue = 0.0;

      for (i <- 0 to 10) {
          val priceEvt = expectMsgType[PricingEvent]
          assert(priceEvt.portfolio == testPortfolio)
          assert(priceEvt.portfolioValueMsg.value > 0 && priceEvt.portfolioValueMsg.value != lastValue)
          lastValue = priceEvt.portfolioValueMsg.value
      }

      subscription.close()

      expectNoMsg()
    }
  }

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }
}
