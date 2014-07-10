package controllers

import play.api._
import play.api.libs.json.Json
import play.api.libs.json.JsValue
import play.api.mvc._
import com.test._
import play.api.libs.json.{JsValue, Json}
import actors.{StockPortfolioValue, StockPortfolioWebSocketActor}
import play.api.mvc.Controller
import play.api.mvc.WebSocket
import play.api.mvc.WebSocket.FrameFormatter
import play.api.mvc.WebSocket.FrameFormatter
import play.api.Play.current
import com.sungard.poc.pricingengine.portfolio.StockPortfolio
import play.libs.Akka
import com.sungard.poc.pricingengine.actors.ServiceLookupActor
import play.libs.Akka

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

    implicit val inEventFormat = Json.format[StockPortfolio]
    implicit val outEventFormat = Json.format[StockPortfolioValue]

    implicit val inEventFrameFormatter = FrameFormatter.jsonFrame[StockPortfolio]
    implicit val outEventFrameFormatter = FrameFormatter.jsonFrame[StockPortfolioValue]

    val system = Akka.system()
    val serviceLookup = ServiceLookupActor.createServiceLookupActor(system)

    def socket = WebSocket.acceptWithActor[StockPortfolio, StockPortfolioValue] { request => out =>
        StockPortfolioWebSocketActor.props(out, serviceLookup)
    }
}