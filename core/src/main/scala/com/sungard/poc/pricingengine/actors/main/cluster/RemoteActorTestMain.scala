package com.sungard.poc.pricingengine.actors.main.cluster

import akka.actor.{Props, ActorSystem, Actor}
import com.sungard.poc.pricingengine.actors.StockPortfolioValuationActor.GetPortfolioValuation
import com.typesafe.config.ConfigFactory

/**
 * Created by admin on 6/19/14.
 */

class RemoteActor extends Actor {
     def receive : Receive = {
       case GetPortfolioValuation(portfolio) => {
           println("Faking out getting portfolio valuation for portfolio" + portfolio)
       }
     }
}

object RemoteActorTestMain {
  def main(args : Array[String]) : Unit = {
    val system =
      ActorSystem("LookupSystem", ConfigFactory.parseString( s"""
                                        akka.remote.netty.tcp.port= 2558
                                        akka.remote.netty.tcp.hostname= "127.0.0.1"

                                        akka.actor.provider = "akka.remote.RemoteActorRefProvider"

                                        akka.remote.enabled-transports = ["akka.remote.netty.tcp"]

                                        akka.remote.log-sent-messages = on
                                        akka.remote.log-received-messages = on
                               """))
    val actor = system.actorOf(Props[RemoteActor], "ValuationActor")

    println("Actor = " + actor)
  }
}
