package com.sungard.poc.pricingengine.actors.main.cluster

import akka.actor._
import com.sungard.poc.pricingengine.actors.StockPortfolioGeneratorActor
import com.sungard.poc.pricingengine.actors.StockDataRetrieverActor.RetrieveStockData
import com.sungard.poc.pricingengine.actors.StockPortfolioGeneratorActor.{PortfolioGenerated, GeneratePortfolio}
import com.sungard.poc.pricingengine.actors.StockPortfolioValuationActor.{PortfolioValued, GetPortfolioValuation}
import com.sungard.poc.pricingengine.portfolio.StockPortfolio
import com.sungard.poc.pricingengine.actors.StockPortfolioValuationActor.PortfolioValued
import com.sungard.poc.pricingengine.actors.StockPortfolioGeneratorActor.PortfolioGenerated
import scala.Some
import akka.actor.Identify
import com.typesafe.config.ConfigFactory
import akka.event.Logging
import akka.util.Timeout
import scala.concurrent.duration._

/**
 * Created by admin on 6/15/14.
 */

object PortfolioSubmitterMain {
  case object GetStockPortfolio


  def main(args : Array[String]) : Unit = {
    val host = args(0)
    val port = args(1).toInt
    val actorSystem = args(2)
    //val host = "127.0.0.1"
    //val port = 2558
    //val actorSystem = "LookupSystem"

    implicit val askTimeout = Timeout(5.second)

    val system = ActorSystem(actorSystem, ConfigFactory.parseString( s"""
                                        akka.actor.provider = "akka.remote.RemoteActorRefProvider"

                                        akka.remote.enabled-transports = ["akka.remote.netty.tcp"]

                                        akka.remote.log-sent-messages = on
                                        akka.remote.log-received-messages = on
                               """).withFallback(ConfigFactory.load("application")))
    val portfolioGeneratorActor = system.actorOf(StockPortfolioGeneratorActor.props(),
      "PortfolioGeneratorActor")

    println("Sending message back to actor path " + s"akka.tcp://$actorSystem@$host:$port/user/ValuationActor")

    val path = s"akka.tcp://$actorSystem@$host:$port/user/ValuationActor"

    val portfolioValuationActor =
      system.actorSelection(path)

    println("Found portfolio valuation actor at " + portfolioValuationActor)

    val portfolioHandlerActor = system.actorOf(Props(new Actor {
      private val log = Logging(context.system, this)

      var thePortfolio : StockPortfolio = null

      def receive: Receive = {
        case GetStockPortfolio => {
          portfolioGeneratorActor ! GeneratePortfolio
        }
        case PortfolioGenerated(portfolio) => {
           log.info("Generated portfolio " + portfolio)
           thePortfolio = portfolio

           for (i <- 1 to 50) {
             portfolioValuationActor ! GetPortfolioValuation(portfolio)
           }

           //portfolioValuationActor ! Identify(path)

        }

        case ActorIdentity(`path`, Some(actor)) => {
              log.info("Actor identity returned")
              actor ! GetPortfolioValuation(thePortfolio)
        }

        case PortfolioValued(portfolio, value) => {
           log.info("Portfolio " + portfolio + " was valued as " + value)
        }

        case m => log.info("Received random message " + m)
      }
    }));

    portfolioHandlerActor ! GetStockPortfolio

    readLine();
  }
}
