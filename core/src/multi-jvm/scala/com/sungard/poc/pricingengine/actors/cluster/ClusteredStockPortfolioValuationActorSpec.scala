package com.sungard.poc.pricingengine.actors.cluster

import akka.remote.testkit.{MultiNodeSpec, MultiNodeConfig}
import com.sungard.poc.pricingengine.cluster.{ClusterClientAccessible, ClusterUtils, ClusterNodeParameters}
import org.scalatest.{Matchers, BeforeAndAfterAll, WordSpecLike}
import akka.testkit.ImplicitSender
import language.postfixOps
import scala.concurrent.duration._
import akka.cluster.ClusterEvent.MemberUp
import com.sungard.poc.pricingengine.actors._
import akka.cluster.Cluster
import akka.actor.{Props, Status, ActorSystem}
import com.sungard.poc.pricingengine.actors.StockPortfolioValuationActor.{PortfolioValued, GetPortfolioValuation}
import com.sungard.poc.pricingengine.portfolio.StockPortfolio
import com.sungard.poc.pricingengine.actors.StockDirectoryActor.StockPricerRetrieved
import com.sungard.poc.pricingengine.actors.StockDataRetrieverActor.ClusteredInitialized
import com.sungard.poc.pricingengine.actors.StockPortfolioValuationActor.GetPortfolioValuation
import akka.cluster.ClusterEvent.MemberUp
import com.sungard.poc.pricingengine.portfolio.StockPortfolio
import com.sungard.poc.pricingengine.calculate.StockPriceSeries
import com.sungard.poc.pricingengine.pricing.StockQuoteGenerator
import actors.{SeqBasedRandomProvider, SeqProvider}
import com.sungard.poc.pricingengine.stock_data.{HistoricalStockPriceRow, StockData, StockDataRetriever}
import org.joda.time.DateTime
import com.sungard.poc.pricingengine

/**
 * Created by admin on 6/1/14.
 */
object ClusteredStockPortfolioValuationActorSpecConfig extends MultiNodeConfig {
  val stockDirectory1 = role("stockDirectory1")
  val stockDirectory2 = role("stockDirectory2")
  val stockDirectoryRouter1 = role("stockDirectoryRouter1")
  val stockDirectoryRouter2 = role("stockDirectoryRouter2")
  val dataRetriever = role("dataRetriever")
  val valuation = role("valuation")

  //val seedHostPorts = List("127.0.0.1:2551", "127.0.0.1:2552")
  val seedHostPorts = List[String]()

  val dataRetrieverParam = pricingengine.cluster.ClusterNodeParameters(host = "127.0.0.1", port = 0,
    role = "dataRetriever",
    clusterName="ClusteredStockPortfolioValuationActorSpec",
    seedHostPorts = seedHostPorts)
  val stockDirectory1Param = ClusterNodeParameters(host = "127.0.0.1", port = 2553,
    role = "stockDirectory",
    clusterName="ClusteredStockPortfolioValuationActorSpec",
    seedHostPorts = seedHostPorts)
  val stockDirectory2Param = ClusterNodeParameters(host = "127.0.0.1", port = 2554,
    role = "stockDirectory",
    clusterName="ClusteredStockPortfolioValuationActorSpec",
    seedHostPorts = seedHostPorts)
  val stockDirectoryRouter1Param = ClusterNodeParameters(host = "127.0.0.1", port = 2551,
    role = "stockDirectoryRouter", configName = "stock_directory_cluster_router",
    clusterName="ClusteredStockPortfolioValuationActorSpec",
    seedHostPorts = seedHostPorts)
  val stockDirectoryRouter2Param = ClusterNodeParameters(host = "127.0.0.1", port = 2552,
    role = "stockDirectoryRouter", configName = "stock_directory_cluster_router",
    clusterName="ClusteredStockPortfolioValuationActorSpec",
    seedHostPorts = seedHostPorts)

  nodeConfig(dataRetriever) {
    ClusterUtils.config(dataRetrieverParam)
  }

  nodeConfig(stockDirectory1) {
    ClusterUtils.config(stockDirectory1Param)
  }

  nodeConfig(stockDirectory2) {
    ClusterUtils.config(stockDirectory2Param)
  }

  nodeConfig(stockDirectoryRouter1) {
    ClusterUtils.config(stockDirectoryRouter1Param)
  }

  nodeConfig(stockDirectoryRouter2) {
    ClusterUtils.config(stockDirectoryRouter2Param)
  }

  nodeConfig(valuation) {
    ClusterClientAccessible.config("127.0.0.1", 2555)
  }

}

class ClusteredStockPortfolioValuationActorSpecMultiJvmNode1 extends ClusteredStockPortfolioValuationActorSpec
class ClusteredStockPortfolioValuationActorSpecMultiJvmNode2 extends ClusteredStockPortfolioValuationActorSpec
class ClusteredStockPortfolioValuationActorSpecMultiJvmNode3 extends ClusteredStockPortfolioValuationActorSpec
class ClusteredStockPortfolioValuationActorSpecMultiJvmNode4 extends ClusteredStockPortfolioValuationActorSpec
class ClusteredStockPortfolioValuationActorSpecMultiJvmNode5 extends ClusteredStockPortfolioValuationActorSpec
class ClusteredStockPortfolioValuationActorSpecMultiJvmNode6 extends ClusteredStockPortfolioValuationActorSpec


import ClusteredStockPortfolioValuationActorSpecConfig._

abstract class ClusteredStockPortfolioValuationActorSpec
  extends MultiNodeSpec(ClusteredStockPortfolioValuationActorSpecConfig)
  with WordSpecLike with Matchers with BeforeAndAfterAll with ImplicitSender
  with ClusterTestable with MultiNodeTestable {

  val ibmStockData = StockData("IBM", List(
    HistoricalStockPriceRow(DateTime.parse("2013-11-01"), 1.0, 31.0, 15.0, 22.5),
    HistoricalStockPriceRow(DateTime.parse("2013-12-01"), 24.5, 27.0, 21.0, 21.0),
    HistoricalStockPriceRow(DateTime.parse("2014-02-01"), 40.0, 45.0, 35.0, 37.0)))


  trait TestStockQuoteGeneratorProvider extends StockQuoteGeneratorProvider {
    def createStockQuoteGenerator(ticker: String, priceSeries: StockPriceSeries): StockQuoteGenerator = {
      new StockQuoteGenerator(ticker, priceSeries)
        with SeqBasedRandomProvider with SeqProvider[Double] {
        var seq: Seq[Double] = List(0.25, 0.5, -0.1, -0.5, 0.5)
      }
    }
  }

  trait TestStockDataRetrieverProvider extends StockDataRetrieverProvider {
    val dataRetriever: StockDataRetriever = {
      new StockDataRetriever {
        def retrieve(ticker : String, startDate : DateTime, endDate : DateTime) : StockData = {
          ibmStockData
        }
      }
    }
  }

  "Portfolio valuation actor" must {
    "be able to contact stock directory router in cluster" in within(30 seconds) {
      runOn(stockDirectoryRouter1, stockDirectoryRouter2, stockDirectory1, stockDirectory2, dataRetriever) {
        Cluster(system).subscribe(testActor, classOf[MemberUp])
      }

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

      testConductor.enter("stock-directory-started")

      runOn(dataRetriever) {
        // this will only run on the 'first' node
        join(node(dataRetriever), stockDirectory1)
        system.actorOf(Props(new StockDataRetrieverActor with ClusteredInitialized
          with TestStockDataRetrieverProvider {
        }), "StockDataRetriever")
      }
      testConductor.enter("data-retriever-started")

      runOn(stockDirectoryRouter1) {
        // this will only run on the 'first' node
        join(node(stockDirectoryRouter1), stockDirectory1)
        system.actorOf(StockDirectoryRouterActor.clusterProps(), "StockDirectoryRouterActor")
        //Thread.sleep(1000)
      }

      runOn(stockDirectoryRouter2) {
        // this will only run on the 'first' node
        join(node(stockDirectoryRouter2), stockDirectory1)
        system.actorOf(StockDirectoryRouterActor.clusterProps(), "StockDirectoryRouterActor")
        //Thread.sleep(1000)
      }

      testConductor.enter("stock-directory-routers-started")

      runOn(valuation) {
        val valuationActor = system.actorOf(
           StockPortfolioValuationActor.propsWithCluster(List(stockDirectoryRouter1Param, stockDirectoryRouter2Param),
            system), "ValuationActor")
        val testPortfolio = StockPortfolio(List(("IBM", 0.4), ("AAPL", 0.6)), 100)

        Thread.sleep(2000)

        for (i <- 1 to 20) {
          awaitAssert {
            valuationActor ! GetPortfolioValuation(testPortfolio)
            //println("Found an error message")
            //expectMsgType[Status.Failure](30.seconds).cause.printStackTrace()
            expectMsgType[PortfolioValued](30.seconds)
          }
        }
      }

      testConductor.enter("done-1")
    }
  }
}

