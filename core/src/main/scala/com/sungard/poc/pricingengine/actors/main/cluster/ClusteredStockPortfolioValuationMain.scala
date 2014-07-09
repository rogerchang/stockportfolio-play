package com.sungard.poc.pricingengine.actors.main.cluster

import com.sungard.poc.pricingengine.actors.{StockPortfolioValuationActor, StockDirectoryRouterActor, StockDataRetrieverActor, StockDirectoryActor}
import akka.actor.Props
import com.sungard.poc.pricingengine.actors.StockDataRetrieverActor.ClusteredInitialized
import com.sungard.poc.pricingengine.cluster.ClusterNodeParameters

/**
 * Created by admin on 6/15/14.
 */
object ClusteredStockPortfolioValuationMain {
  def main(args : Array[String]) : Unit = {

      val system = ClusteredMainUtils.createClusterClientSystem(args, "StockPortfolioValuationSystem")

      val clusterSystem = args(2)

      var contactPoints : Seq[ClusterNodeParameters] = List[ClusterNodeParameters]()

      if (args.length > 4) {
           contactPoints ++= Seq[ClusterNodeParameters](ClusterNodeParameters(host = args(3), port = args(4).toInt, clusterName = clusterSystem))
      }

      if (args.length > 6) {
        contactPoints ++= Seq[ClusterNodeParameters](ClusterNodeParameters(host = args(5), port = args(6).toInt, clusterName = clusterSystem))
      }

      val actor = system.actorOf(
        StockPortfolioValuationActor.propsWithCluster(contactPoints,
          system), "ValuationActor")

       println("Actor started: " + actor)

       // Just prevent me from dying
       val lock = new Object()
       lock.synchronized {
         lock.wait();
       }

       println(actor + ": I'm dying!!!!!!")
    }

}
