package com.sungard.poc.pricingengine.actors.cluster

import akka.remote.testkit.{MultiNodeSpec, MultiNodeConfig}
import com.sungard.poc.pricingengine.cluster.{ClusterUtils, ClusterNodeParameters}
import akka.actor.{Props, ActorPath}
import akka.cluster.ClusterEvent.MemberUp
import akka.cluster.Cluster
import org.scalatest.{Matchers, BeforeAndAfterAll, WordSpecLike}
import akka.testkit.ImplicitSender
import language.postfixOps
import scala.concurrent.duration._
import com.sungard.poc.pricingengine.actors.{StockDirectoryRouterActor, StockDirectoryActor}
import com.sungard.poc.pricingengine.actors.StockDataRetrieverActor.ClusteredInitialized
import com.sungard.poc.pricingengine.actors.StockDirectoryActor.{StockPricerRetrieved, GetStockPricer}
import com.sungard.poc.pricingengine.actors.cluster.ClusteredStockDataRetrieverActorSpecConfig._
import akka.cluster.ClusterEvent.MemberUp
import com.sungard.poc.pricingengine.actors.StockPricerActor.{RetrieveStockQuote, CurrentStockQuote}
import scala.util.Random

/**
 * Created by admin on 5/24/14.
 */

object ClusteredStockDirectoryActorSpecConfig extends MultiNodeConfig {
  // register the named roles (nodes) of the test
  val stockDirectory1 = role("stockDirectory1")
  val stockDirectory2 = role("stockDirectory2")
  val stockDirectory3 = role("stockDirectory3")
  val stockDirectoryRouter = role("stockDirectoryRouter")
  val priceRequester = role("priceRequester")

  val stockDirectoryParam1 = ClusterNodeParameters(host = "127.0.0.1", port = 2551,
    role = "stockDirectory")
  val stockDirectoryParam2 = ClusterNodeParameters(host = "127.0.0.1", port = 2552,
    role = "stockDirectory")
  val stockDirectoryParam3 = ClusterNodeParameters(host = "127.0.0.1", port = 0,
    role = "stockDirectory")
  val stockDirectoryRouterParam = ClusterNodeParameters(host = "127.0.0.1", port = 0,
    role = "stockDirectory", configName = "stock_directory_cluster_router")
  val stockPricerParam = ClusterNodeParameters(host = "127.0.0.1", port = 0, role = "stockPricer")
  val pricerRequesterParam = ClusterNodeParameters(host = "127.0.0.1", port = 0, role = "priceRequester")

  nodeConfig(stockDirectory1) {
    ClusterUtils.config(stockDirectoryParam1)
  }

  nodeConfig(stockDirectory2) {
    ClusterUtils.config(stockDirectoryParam2)
  }

  nodeConfig(stockDirectory3) {
    ClusterUtils.config(stockDirectoryParam3)
  }

  nodeConfig(stockDirectoryRouter) {
    ClusterUtils.config(stockDirectoryRouterParam)
  }

  nodeConfig(priceRequester) {
    ClusterUtils.config(pricerRequesterParam)
  }
}

class ClusteredStockDirectoryActorSpecMultiJvmNode1 extends ClusteredStockDirectoryActorSpec
class ClusteredStockDirectoryActorSpecMultiJvmNode2 extends ClusteredStockDirectoryActorSpec
class ClusteredStockDirectoryActorSpecMultiJvmNode3 extends ClusteredStockDirectoryActorSpec
class ClusteredStockDirectoryActorSpecMultiJvmNode4 extends ClusteredStockDirectoryActorSpec
class ClusteredStockDirectoryActorSpecMultiJvmNode5 extends ClusteredStockDirectoryActorSpec

import ClusteredStockDirectoryActorSpecConfig._

abstract class ClusteredStockDirectoryActorSpec
    extends MultiNodeSpec(ClusteredStockDirectoryActorSpecConfig)
  with WordSpecLike with Matchers with BeforeAndAfterAll with ImplicitSender
    with ClusterTestable with MultiNodeTestable {

  "Clustered stock directory" must {
    "receive routed request and then route to appropriate pricer" in within(30 seconds) {
      Cluster(system).subscribe(testActor, classOf[MemberUp])

      runOn(stockDirectory1) {
        // this will only run on the 'first' node
        join(node(stockDirectory1), stockDirectory1)
        system.actorOf(StockDirectoryActor.clusteredProps(), "StockDirectory")
        //Thread.sleep(1000)
      }

      runOn(stockDirectory2) {
        // this will only run on the 'first' node
        join(node(stockDirectory2), stockDirectory1)
        system.actorOf(StockDirectoryActor.clusteredProps(), "StockDirectory")
        //Thread.sleep(1000)
      }

      runOn(stockDirectory3) {
        // this will only run on the 'first' node
        join(node(stockDirectory3), stockDirectory1)
        system.actorOf(StockDirectoryActor.clusteredProps(), "StockDirectory")
        //Thread.sleep(1000)
      }

      // this will run on all nodes
      // use barrier to coordinate test steps
      testConductor.enter("stock-directories-started")

      runOn(stockDirectoryRouter) {
        join(node(stockDirectoryRouter), stockDirectory1)
        system.actorOf(StockDirectoryRouterActor.clusterProps(), "StockDirectoryRouterActor")
      }

      testConductor.enter("stock-directory-router-started")

      runOn(priceRequester) {
        join(node(priceRequester), stockDirectory1)

        val directoryRouter = system.actorSelection(node(stockDirectoryRouter) / "user" / "StockDirectoryRouterActor")

        var stockToPort = Map[String, Int]()

        val stockList = List("IBM", "AAPL", "MS", "GOOG", "JPM", "GS")

        /*
        for (i <- 1 to 1) {
          awaitAssert {
            val randomStock = stockList(new Random(System.currentTimeMillis).nextInt(stockList.length))
            directoryRouter ! GetStockPricer(randomStock)
            val stockPricer = this.expectMsgType[StockPricerRetrieved].stockPricer
            assert(stockPricer.path.elements.exists(_.equals(randomStock)))
            val observedPort: Int = stockPricer.path.address.port.get
            assert(stockToPort.getOrElse(randomStock, -1) == observedPort || (stockToPort = stockToPort.updated(randomStock, observedPort)) != null)
            //assert(stockPricer.path.contains("IBM"))
          }


        }

        assert(stockToPort.values.exists(_ == 2551))
        assert(stockToPort.values.exists(_ == 2552))
        assert(stockToPort.values.exists(port => port != 2551 && port != 2552))
        */

        for (i <- 1 to 20) {
          awaitAssert ({
              val randomStock = stockList(new Random(System.currentTimeMillis).nextInt(stockList.length))
              directoryRouter ! GetStockPricer(randomStock)
              val stockPricer = this.expectMsgType[StockPricerRetrieved](10.seconds).stockPricer
              assert(stockPricer.path.elements.exists(_.equals(randomStock)))
              val observedPort: Int = stockPricer.path.address.port.get
              stockToPort.getOrElse(randomStock, -1) == observedPort || (stockToPort = stockToPort.updated(randomStock, observedPort)) != null
            }, 10.seconds)
         }

        assert(stockToPort.values.toSet.size > 1)
      }
      testConductor.enter("done-1")
    }
  }
}
