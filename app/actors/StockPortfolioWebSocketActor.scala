package actors

import akka.actor.{Props, Actor, ActorRef}
import scala.util.Random

case class Price(price: Double)

object StockPortfolioWebSocketActor {
  def props(out: ActorRef) = Props(new StockPortfolioWebSocketActor(out))
}

class StockPortfolioWebSocketActor(out: ActorRef) extends Actor {

  def receive = {
    case msg: String =>
      out ! Price(Random.nextDouble())
    //            out ! ("I received your message: " + msg)
  }
}