package actors.cluster

import org.scalatest.{Matchers, WordSpecLike, FlatSpec}
import com.sungard.poc.pricingengine.cluster.{ClusterUtils, ClusterNodeParameters}
import akka.testkit.{ImplicitSender, TestKit}
import akka.actor.ActorSystem
import com.sungard.poc.pricingengine.actors.StockDirectoryRouterActor
import com.sungard.poc.pricingengine.actors.StockDirectoryActor.GetStockPricer


/**
 * Created by admin on 5/18/14.
 */
class ClusterUtilTest extends FlatSpec with Matchers {
  /*
  "Basic config request" should "generate correct config" in {
      val sampleParam =  ClusterNodeParameters(host = "127.0.0.1", port = 0,
        role = "dataRetriever")
      val config = ClusterUtils.config(sampleParam)
      assert(config.getInt("client_app.min.portfolio.stocks") == 2)
   }
   */


  "Clustered router config" should "be successfully parsed" in {
      val sampleParam = ClusterNodeParameters(host = "127.0.0.1", port = 0,
        role = "stockDirectory", configName = "stock_directory_cluster_router")
      val config = ClusterUtils.config(sampleParam)
      val newSystem = ActorSystem("clusterConfigTest", config)
      val actor = newSystem.actorOf(StockDirectoryRouterActor.clusterProps(), "StockDirectoryRouterActor")
      actor ! GetStockPricer("IBM")
      Thread.sleep(3000)
   }
}
