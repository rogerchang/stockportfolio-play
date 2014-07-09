package actors

import akka.actor.{Props, Actor, ActorRef}
import scala.util.Random
import com.sungard.poc.pricingengine.portfolio.StockPortfolio
import com.sungard.poc.pricingengine.actors.{ServiceLookupActor, PricingEvent}
import play.libs.Akka
import com.sungard.poc.pricingengine.actors.ServiceLookupActor.{StockPriceStreamerReturned, GetStockPriceStreamer}
import com.sungard.poc.pricingengine.actors.PricingStreamActor.RegisterPricingStream

case class StockPortfolioValue(value: Double)
//case class StockPortfolio(elements: Map[String, Integer]) //try to get this one to work

object StockPortfolioWebSocketActor {
  def props(out: ActorRef) = Props(new StockPortfolioWebSocketActor(out))
}

class StockPortfolioWebSocketActor(out: ActorRef) extends Actor {

  var currentPortfolio: StockPortfolio = _

  def receive = {
    case msg: String =>
      out ! StockPortfolioValue(Random.nextDouble())

    case stockPortfolio: StockPortfolio =>
      println("received stockPortfolio")
      println(stockPortfolio)
      currentPortfolio = stockPortfolio

      //start the process of subscribing
      val system = Akka.system()
      val serviceLookup = ServiceLookupActor.createServiceLookupActor(system)
      serviceLookup ! GetStockPriceStreamer

    case stockPriceStreamer: StockPriceStreamerReturned =>
      println("received stockPriceStreamer")
      stockPriceStreamer.stockPriceStreamer ! RegisterPricingStream(self, currentPortfolio)

    case pricingEvent: PricingEvent =>
      println("received pricingEvent")
      println(pricingEvent)
      out ! StockPortfolioValue(pricingEvent.portfolioValueMsg.value)
  }
}