package com.sungard.poc.pricingengine.actors.main.cluster

import com.sungard.poc.pricingengine.actors.{StockDirectoryActor, StockDirectoryRouterActor}

/**
 * Created by admin on 6/15/14.
 */
object ClusteredStockDirectoryMain {
  def main(args : Array[String]) : Unit = {
    val system = ClusteredMainUtils.createClusteredSystem(args,
      null, "stockDirectory")
    system.actorOf(StockDirectoryActor.clusteredProps(), "StockDirectory")

    readLine()
  }
}
