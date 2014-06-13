package controllers

import play.api._
import play.api.mvc._
import com.test._
import play.api.libs.json.{JsValue, Json}
import actors.{StockPortfolioWebSocketActor, Price}
import play.api.mvc.WebSocket.FrameFormatter
import play.api.Play.current
import play.libs.Akka
import akka.actor.ActorSelection
import com.sungard.poc.pricingengine.actors.ServiceLookupActor.GetStockPortfolioValuer
import com.sungard.poc.pricingengine.actors.ServiceLookupActor

object Application extends Controller {

  def index = Action {
    val hello = new Hello()
    val test = hello.printHello()
    Ok(views.html.index(test))
  }

  /**
   * Display the stock portfolio page
   */
  def stockPortfolio() = Action { implicit request =>
    Ok(views.html.stockPortfolio())
  }

  def stockPortfolioJs() = Action { implicit request =>
    Ok(views.js.stockPortfolio(request))
  }

  implicit val pricingFormat = Json.format[Price]
  implicit val pricingFrameFormatter = FrameFormatter.jsonFrame[Price]

  def socket = WebSocket.acceptWithActor[String, Price] { request => out =>
    StockPortfolioWebSocketActor.props(out)
  }

  /*
  def pricePortfolio(portfolio: String) = WebSocket.async[JsValue] { request =>
    val system = Akka.system()
    ServiceLookupActor.createServiceLookupActor(system)
    var serviceLookupActor: ActorSelection = ServiceLookupActor.getServiceLookupActor(system)
    serviceLookupActor ! GetStockPortfolioValuer
  }
  */
}