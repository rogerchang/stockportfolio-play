package com.sungard.poc.pricingengine.actors.main.cluster

import com.sungard.poc.pricingengine.actors.{StockDataRetrieverActor, StockDirectoryActor}
import akka.actor.Props
import com.sungard.poc.pricingengine.actors.StockDataRetrieverActor.{DefaultStockDataRetrieverProvider, ClusteredInitialized}

/**
 * Created by admin on 6/15/14.
 */
object ClusteredDataRetrieverMain {
  def main(args : Array[String]) : Unit = {
    val system = ClusteredMainUtils.createClusteredSystem(args,
      null, "dataRetriever")
    system.actorOf(Props(new StockDataRetrieverActor with ClusteredInitialized
      with DefaultStockDataRetrieverProvider {
    }), "StockDataRetriever")

    readLine()
  }
}
