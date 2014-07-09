package com.sungard.poc.pricingengine.actors.main.cluster

import com.sungard.poc.pricingengine.actors.StockDirectoryRouterActor

/**
 * Created by admin on 6/15/14.
 */
object ClusteredStockDirectoryRouterMain {
     def main(args : Array[String]) : Unit = {
         val system = ClusteredMainUtils.createClusteredSystem(args,
           "stock_directory_cluster_router", "stockDirectoryRouter")
         system.actorOf(StockDirectoryRouterActor.clusterProps(), "StockDirectoryRouterActor")

         readLine()
     }
}
