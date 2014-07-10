package actors

import akka.actor.{Props, Actor, ActorRef}
import scala.util.Random
import com.sungard.poc.pricingengine.portfolio.StockPortfolio
import com.sungard.poc.pricingengine.actors.{PricingEventSubscription, ServiceLookupActor, PricingEvent}
import play.libs.Akka
import com.sungard.poc.pricingengine.actors.ServiceLookupActor.{StockPriceStreamerReturned, GetStockPriceStreamer}
import com.sungard.poc.pricingengine.actors.PricingStreamActor.RegisterPricingStream

case class StockPortfolioValue(stockportfolio: StockPortfolio, value: Double)
//case class StockPortfolio(elements: Map[String, Integer]) //try to get this one to work

object StockPortfolioWebSocketActor {
    def props(out: ActorRef, serviceLookup: ActorRef) = Props(new StockPortfolioWebSocketActor(out, serviceLookup))
}

class StockPortfolioWebSocketActor(out: ActorRef, serviceLookup: ActorRef) extends Actor {

    var currentPortfolio: StockPortfolio = _
    var subscription: PricingEventSubscription = _

    def receive = {
        case stockPortfolio: StockPortfolio =>
            println("received stockPortfolio")
            println(stockPortfolio)
            currentPortfolio = stockPortfolio

            //stop current streaming
            if (subscription != null)
                subscription.close()
            serviceLookup ! GetStockPriceStreamer

        case stockPriceStreamer: StockPriceStreamerReturned =>
            println("received stockPriceStreamer")
            stockPriceStreamer.stockPriceStreamer ! RegisterPricingStream(self, currentPortfolio)
        case pricingEventSubscription: PricingEventSubscription =>
            subscription = pricingEventSubscription
        case pricingEvent: PricingEvent =>
            println("received pricingEvent")
            println(pricingEvent)
            out ! StockPortfolioValue(pricingEvent.portfolioValueMsg.stockPortfolio, pricingEvent.portfolioValueMsg.value)
    }
}